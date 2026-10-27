# 微信登录与微信支付接入操作手册

> 状态：**待接入设计文档**。当前仓库已实现微信登录的服务端调用与小程序入口，但未用真实 AppID 联调；微信支付只有 `MockPaymentGateway`，真实支付、回调、退款、对账均未实现。
>
> 目标：需要真实接入时，按本文步骤即可完成小程序登录、JSAPI/小程序支付、支付回调、关单与退款基础链路。
>
> 原则：AppSecret、商户私钥、APIv3 Key 只放服务端；客户端支付结果不可信；所有回调必须验签、验金额、幂等处理。

---

## 1. 当前代码基线

| 能力 | 状态 | 位置 |
| --- | --- | --- |
| 微信登录（code2session） | 已实现，未真实联调 | `server/.../auth/service/AuthService.java`、`AuthController.java` |
| 小程序 `wx.login` 入口 | 已实现 | `miniapp/pages/profile/index.ts` |
| 登录配置 | 已存在 | `WECHAT_APP_ID`、`WECHAT_APP_SECRET` |
| 开发登录 | 已实现，仅 dev | `APP_DEV_LOGIN_ENABLED` |
| 支付抽象 | 已实现 | `payment/gateway/PaymentGateway.java` |
| 模拟支付 | 已实现，仅 dev/test | `MockPaymentGateway.java` |
| 支付记录 | 已实现 | `payment_record` 表、`PaymentService.java` |
| 订单支付状态流转 | 已实现 | `MallOrderMapper#transitionToPaid` |
| 真实微信支付 | **未实现** | 需要新增 `WechatPayGateway`、回调、关单 |
| 退款 / 对账 | **未实现** | 需要新增退款单与账单任务 |

当前 `PaymentService#pay` 是**同步支付**：调用网关成功后立即把订单改为 `PENDING_SHIPMENT/PAID`。真实微信支付必须拆成两段：

1. 服务端下单拿 `prepay_id`，返回小程序调起支付参数；
2. 微信异步回调到达后，服务端验签、验金额、幂等更新订单。

**不能**在小程序 `wx.requestPayment` 成功回调里直接相信客户端。客户端只负责刷新订单状态。

---

## 2. 接入前准备

### 2.1 账号与资质

- [ ] 小程序为已认证的正式 AppID（不能是 `touristappid` 或测试号）。
- [ ] 小程序类目支持电商/实物商品销售，完成备案与隐私保护指引。
- [ ] 已申请微信支付商户号，并完成与小程序 AppID 的绑定。
- [ ] 商户号已开通「JSAPI 支付 / 小程序支付」权限。
- [ ] 已完成商户主体认证、结算账户配置。
- [ ] 生产域名已完成 ICP 备案，并配置 HTTPS。
- [ ] 小程序后台已配置 `request` 合法域名、`uploadFile`/`downloadFile` 域名。
- [ ] 微信支付商户平台已配置支付回调域名（必须 HTTPS，不能带参数）。

### 2.2 证书与密钥

在微信支付商户平台获取：

| 材料 | 用途 | 存放位置 |
| --- | --- | --- |
| 商户号 `mchid` | 下单、回调校验 | 服务端配置 |
| 商户 API 证书序列号 | 请求签名 | 服务端配置 |
| 商户 API 私钥 `apiclient_key.pem` | 请求签名 | 服务端文件/密钥管理，权限 600 |
| APIv3 密钥（32 字节） | 回调报文解密 | 服务端密钥管理 |
| 微信支付公钥 ID / 公钥文件 | 验签回调与应答 | 服务端文件/密钥管理 |
| 小程序 AppID | 下单、回调校验 | 服务端 + 小程序配置 |
| 小程序 AppSecret | code2session | **仅服务端** |

> 新商户优先使用「微信支付公钥」模式；老商户仍可能使用「平台证书」模式。两者验签实现不同，以商户平台当前指引为准：
> <https://pay.weixin.qq.com/doc/v3/merchant/4024350132>

### 2.3 环境变量设计

在 `AppProperties` 中新增 `WechatPay` 配置组，变量建议如下（不要直接提交真实值）：

```bash
# 微信登录
WECHAT_APP_ID=
WECHAT_APP_SECRET=

# 微信支付
WECHAT_PAY_ENABLED=false
WECHAT_PAY_MCH_ID=
WECHAT_PAY_MCH_SERIAL_NO=
WECHAT_PAY_PRIVATE_KEY_PATH=/run/secrets/apiclient_key.pem
WECHAT_PAY_API_V3_KEY=
WECHAT_PAY_PUBLIC_KEY_ID=
WECHAT_PAY_PUBLIC_KEY_PATH=/run/secrets/wechatpay_public_key.pem
WECHAT_PAY_NOTIFY_URL=https://api.example.com/api/v1/payment/wechat/notify
```

配置要求：

- `WECHAT_PAY_ENABLED=false` 时，支付接口返回明确的「支付未开通」错误，**禁止回退到模拟支付**。
- 生产启动校验：`prod` profile 下若 `ENABLED=true` 但关键参数缺失，直接启动失败。
- 私钥与公钥文件不进入 Git，不写入日志，不打进容器镜像的公开层。
- APIv3 Key 轮换时保留旧 Key 一个回调重试周期，避免历史通知解密失败。

### 2.4 依赖

使用微信支付官方 Java SDK：

```xml
<dependency>
    <groupId>com.github.wechatpay-apiv3</groupId>
    <artifactId>wechatpay-java</artifactId>
    <version>以 Maven Central 最新稳定版为准</version>
</dependency>
```

落地前先核对版本与 Java 17 / Spring Boot 3.3 兼容性：

```bash
# 查看可用版本
curl -s https://repo1.maven.org/maven2/com/github/wechatpay-apiv3/wechatpay-java/maven-metadata.xml
```

官方仓库：<https://github.com/wechatpay-apiv3/wechatpay-java>

---

## 3. 微信登录接入

### 3.1 已有流程

```text
小程序 wx.login()
  -> 获得临时 code
  -> POST /api/v1/auth/wechat-login { code }
  -> 服务端 GET https://api.weixin.qq.com/sns/jscode2session
  -> 获得 openid / unionid
  -> 按 openid 查找或创建 mall_user
  -> 签发 JWT
```

服务端实现位置：

- `AuthController#wechatLogin`
- `AuthService#wechatLogin`
- `WechatLoginRequest`

### 3.2 配置并启动

```bash
cd /Users/catalina/Workspace/module/mall-mini/server

export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export SPRING_PROFILES_ACTIVE=dev
export DB_URL='jdbc:mysql://localhost:3307/mall_mini?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME=mallmini
export DB_PASSWORD=mallmini_dev

# 真实微信登录
export WECHAT_APP_ID='wx你的小程序AppID'
export WECHAT_APP_SECRET='你的AppSecret'

mvn spring-boot:run
```

小程序侧：

1. `miniapp/project.config.json` 的 `appid` 改为正式 AppID；
2. `miniapp/utils/config.ts` 的 `apiBaseUrl` 指向可访问的服务端；
3. 微信开发者工具中勾选「不校验合法域名」（仅本地），生产必须配置合法域名；
4. 进入「我的」→「微信登录」，观察服务端日志与数据库 `mall_user`。

### 3.3 验收项

- [ ] 新用户首次登录自动创建 `mall_user`，`open_id` 唯一。
- [ ] 老用户再次登录复用同一用户，不重复建号。
- [ ] 无效 code 返回 401，不泄露 AppSecret。
- [ ] 微信接口超时/500 返回 503，不把用户创建成半成品。
- [ ] 禁用用户登录返回 403。
- [ ] 小程序重启后 JWT 仍可访问 `/api/v1/me`。
- [ ] 生产环境 `APP_DEV_LOGIN_ENABLED=false`。
- [ ] AppSecret 未出现在小程序包、前端日志、Git 历史中。

### 3.4 常见错误码

| errcode | 含义 | 处理 |
| --- | --- | --- |
| `40029` | code 无效 | code 一次性、5 分钟有效；重新 `wx.login` |
| `40125` | AppSecret 无效 | 检查服务端环境变量 |
| `40013` | AppID 无效 | 检查 AppID 与小程序是否一致 |
| `45011` | 频率限制 | 登录接口限流，稍后重试 |
| `40226` | 高风险用户 | 按微信风控提示处理，不要绕过 |

> `session_key` 当前未落库。若后续要做手机号解密、敏感数据解密，再引入会话管理；不要把 `session_key` 返回给客户端。

### 3.5 可选：手机号授权

当前 `mall_user.phone` 字段存在，但未接入手机号授权。需要时：

1. 小程序使用 `<button open-type="getPhoneNumber">` 获取 `code`；
2. 服务端获取并缓存 `access_token`：
   `GET https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=...&secret=...`
3. 调用：
   `POST https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=...`
   body：`{"code":"..."}`；
4. 校验返回的 `phone_info.phoneNumber`，更新当前登录用户；
5. `access_token` 缓存 7000 秒左右，禁止每次请求重新获取；
6. 手机号展示与日志必须脱敏。

### 3.6 Android 端说明

Android 不能复用小程序 `wx.login`。真实微信登录需要：

- 微信开放平台创建「移动应用」，获取 AppID；
- 集成微信 OpenSDK for Android；
- 配置包名、应用签名、Universal Links（如涉及）；
- 客户端拿到 `code` 后调服务端新增的移动应用登录接口；
- 服务端使用移动应用 AppID/Secret 调用 `sns/oauth2/access_token`（不是小程序的 `jscode2session`）。

当前 Android 仍使用开发登录，接入真实微信登录属于独立任务，不要与小程序配置混用。

---

## 4. 微信支付接入（JSAPI / 小程序支付）

### 4.1 目标链路

```text
小程序                服务端                         微信支付
  |  POST /pay           |                              |
  |--------------------->| 校验订单/金额/归属           |
  |                     | 创建 payment_record PENDING  |
  |                     | POST /v3/pay/transactions/jsapi
  |                     |----------------------------->|
  |                     |<-----------------------------|
  |                     |  保存 prepay_id              |
  |<--------------------|  返回调起支付参数             |
  | wx.requestPayment   |                              |
  |--------------------------------------------------->|
  |                     |<--- 支付成功回调（异步）------|
  |                     | 验签/解密/验金额/幂等         |
  |                     | 订单 PENDING_PAYMENT -> PENDING_SHIPMENT
  |                     | 支付记录 SUCCESS             |
  |  轮询/刷新订单       |                              |
  |--------------------->|                              |
```

### 4.2 必须改造的代码

| 改造点 | 当前 | 目标 |
| --- | --- | --- |
| `PaymentGateway` | 只有同步 `pay()` | 增加 `prepay()`、`close()`、`parseNotify()` |
| `PaymentRequest` | orderId/orderNo/amount | 增加 openid、商品描述、客户端 IP、过期时间 |
| `PaymentResult` | success/transactionNo/message | 拆成「预支付结果」与「回调结果」 |
| `PaymentService#pay` | 调网关后立即置为已支付 | 只创建预支付并返回参数；回调才置为已支付 |
| `PaymentController` | 返回支付结果 | 返回 `wx.requestPayment` 参数 |
| 回调接口 | 无 | 新增公开回调接口，验签 + 幂等 |
| 关单 | 无 | 取消/超时前调用微信关单 |
| 退款 | 无 | 新增退款单、退款回调 |
| 对账 | 无 | 每日账单下载与差异核对 |

建议接口设计：

```java
public interface PaymentGateway {
    String channel();

    PaymentInitResult prepay(PaymentRequest request);

    void close(String outTradeNo);

    PaymentNotification parseNotification(String body, Map<String, String> headers);
}
```

```java
public record PaymentRequest(
        Long orderId,
        String orderNo,
        String outTradeNo,
        BigDecimal amount,
        String description,
        String payerOpenId,
        String clientIp,
        LocalDateTime expireAt
) {}
```

```java
public record PaymentInitResult(
        boolean success,
        boolean paidImmediately,
        String prepayId,
        Map<String, String> payParams,
        String message
) {}
```

> 保留 `MockPaymentGateway` 的 `paidImmediately=true` 行为，便于本地继续使用模拟支付；真实网关返回 `payParams`，由小程序调起。

### 4.3 数据模型调整

新增 Flyway `V2__wechat_pay.sql`，只做**向后兼容**的增量字段：

```sql
ALTER TABLE payment_record
    ADD COLUMN out_trade_no   VARCHAR(32)  NULL COMMENT '微信商户订单号',
    ADD COLUMN prepay_id      VARCHAR(64)  NULL COMMENT '预支付会话标识',
    ADD COLUMN payer_openid   VARCHAR(128) NULL COMMENT '支付用户 openid',
    ADD COLUMN trade_type     VARCHAR(32)  NULL COMMENT 'JSAPI/APP 等',
    ADD COLUMN success_time   DATETIME(3)  NULL COMMENT '微信支付成功时间',
    ADD COLUMN notify_at      DATETIME(3)  NULL COMMENT '回调到达时间',
    ADD COLUMN raw_status     VARCHAR(32)  NULL COMMENT '微信交易状态',
    ADD UNIQUE KEY uk_payment_transaction (transaction_no),
    ADD UNIQUE KEY uk_payment_out_trade_no (out_trade_no);
```

说明：

- MySQL 唯一索引允许多个 `NULL`，历史数据不受影响。
- 上线后回填 `out_trade_no = order_no`。
- 微信要求 `out_trade_no` 在同一商户号下唯一。若同一订单需要重新预支付，使用 `orderNo + "-R2"` 这类后缀（总长 ≤ 32），不要覆盖已关闭单号。
- `transaction_no` 存微信 `transaction_id`，唯一索引用于回调幂等兜底。
- 退款单独建 `refund_record`，不要复用 `payment_record` 的字段。

### 4.4 服务端实现步骤

**Step 1：配置类**

在 `AppProperties` 增加 `WechatPay` 子配置，并在 `application.yml` 绑定环境变量。生产启动校验关键项。

**Step 2：实现 `WechatPayGateway`**

使用官方 SDK 构建 `RSAAutoCertificateConfig` 或公钥模式配置。下单请求关键字段：

| 字段 | 值 |
| --- | --- |
| `appid` | 小程序 AppID |
| `mchid` | 商户号 |
| `description` | 商品描述（可用「优选商城-订单尾号」） |
| `out_trade_no` | 本地支付单号 |
| `notify_url` | HTTPS 回调地址 |
| `time_expire` | 订单取消时间，RFC3339 带时区 |
| `amount.total` | **分**，`payable_amount × 100`，必须大于 0 |
| `amount.currency` | `CNY` |
| `payer.openid` | 当前用户 openid，不能为空 |
| `scene_info.payer_client_ip` | 客户端真实 IP |

响应只取 `prepay_id`。`prepay_id` 有效期 2 小时，过期需重新下单。

**Step 3：改造 `PaymentService`**

- 校验订单属于当前用户、状态为 `PENDING_PAYMENT`、`payment_status=UNPAID`；
- 已支付直接返回订单，不再调用微信；
- 已取消/已完成直接报错；
- 先写 `payment_record(PENDING, out_trade_no, channel=WECHAT)`，再调用微信预下单；
- 预下单失败把记录置 `FAILED`，返回明确错误；
- 预下单成功保存 `prepay_id`，返回调起参数；
- **不在此处修改订单为已支付**。

**Step 4：生成小程序调起参数**

```text
签名串 = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + "prepay_id=" + prepayId + "\n"
paySign = Base64(RSA-SHA256(签名串, 商户API私钥))
```

返回：

```json
{
  "timeStamp": "1414561699",
  "nonceStr": "5K8264ILTKCH16CQ2502SI8ZNMTM67VS",
  "package": "prepay_id=wx201410272009395522657a690389285100",
  "signType": "RSA",
  "paySign": "..."
}
```

**Step 5：支付回调（核心）**

新增公开接口，例如：

```text
POST /api/v1/payment/wechat/notify
```

处理顺序：

1. 读取 `Wechatpay-Signature`、`Wechatpay-Timestamp`、`Wechatpay-Nonce`、`Wechatpay-Serial`；
2. 用微信支付公钥/平台证书验签，时间戳偏差不超过 5 分钟；
3. 用 APIv3 Key 对 `resource` 做 AES-256-GCM 解密；
4. 校验 `appid`、`mchid` 与本地配置一致；
5. 校验 `out_trade_no` 存在、`amount.total` 等于本地应付金额（分）；
6. 仅处理 `trade_state=SUCCESS`；
7. 幂等：
   - 若 `transaction_id` 已存在且订单已支付，直接返回成功；
   - 条件更新 `mall_order`：`WHERE status='PENDING_PAYMENT' AND payment_status='UNPAID'`；
   - 更新 `payment_record` 为 `SUCCESS`，写 `transaction_id`、`success_time`；
   - 写 `order_status_log`（`PAYMENT`，`PENDING_PAYMENT → PENDING_SHIPMENT`）；
8. 立即返回 HTTP 200：`{"code":"SUCCESS","message":"成功"}`；
9. 失败返回非 2xx：`{"code":"FAIL","message":"原因"}`，让微信重试。

注意：

- 回调处理中不要执行下单、发货、短信等耗时外部调用；
- 不要在长事务里做网络请求；
- 金额不等、商户号不符、签名失败一律拒绝并告警；
- 已取消订单收到支付成功回调：不要静默丢弃，应记录异常并触发退款/人工处理。

**Step 6：关单与超时**

- 用户取消订单前，先调用微信关单 API；关单成功后再本地取消并回补库存；
- 定时任务取消超时订单时，同样先关单；
- 关单返回「订单已支付」时，不能取消，应查询订单并走支付成功流程；
- 外部调用放在事务提交后（`TransactionSynchronization` / 事件 / 独立任务），避免长事务。

**Step 7：主动查询补偿**

网络超时、回调丢失时，定时任务按 `payment_record(PENDING)` 调「商户订单号查单」：

- `SUCCESS`：按回调逻辑幂等入账；
- `CLOSED/REVOKED/PAYERROR`：关闭本地支付单；
- 查单失败：指数退避重试并告警。

### 4.5 小程序端改造

`miniapp/services/payment.ts` 当前直接调 `/pay`。改造后：

```ts
const params = await payOrder(orderNo)

await new Promise<void>((resolve, reject) => {
  wx.requestPayment({
    ...params,
    success: () => resolve(),
    fail: (err) => reject(err),
  })
})

// 不要本地改订单状态，重新拉取详情
const order = await getOrder(orderNo)
```

处理要点：

- 用户取消支付：提示「已取消支付」，订单仍为待付款；
- 支付失败：展示原因，不重复创建支付单；
- 支付成功后订单状态可能因回调延迟仍是待付款：轮询 3–5 次或提示稍后刷新；
- 禁止用 `wx.requestPayment` 的 success 直接调用「标记已支付」接口。

### 4.6 退款（基础版）

真实退款需要新增：

- `refund_record`：退款单号、支付单号、订单号、金额、原因、状态、微信退款单号、时间；
- `POST /api/v1/admin/orders/{orderNo}/refund`：管理员发起，校验金额 ≤ 已支付 − 已退款；
- 调微信退款 API（`/v3/refund/domestic/refunds`），`out_refund_no` 唯一；
- 退款回调 `/api/v1/payment/wechat/refund-notify`：验签、验金额、幂等；
- 退款成功后更新 `payment_record`/`refund_record`，订单支付状态 `REFUNDED` 或增加 `PARTIALLY_REFUNDED`；
- 库存回补策略必须显式定义：仅「退货入库」后退回库存，仅退款不自动回补；
- 退款失败进入人工处理队列，不能假装成功。

完整售后状态机见 [`docs/roadmap.md`](./roadmap.md)。

### 4.7 对账

- 每日下载交易账单（`/v3/bill/tradebill`）与资金账单；
- 以 `out_trade_no` / `transaction_id` 与本地 `payment_record` 比对；
- 差异分类：本地有微信无、微信有本地无、金额不一致、状态不一致；
- 差异进入告警与人工核对，不自动改历史订单；
- 对账任务需幂等，可重复执行同一天账单。

### 4.8 测试与验收

单元测试：

- [ ] 预下单金额转分正确，0 元订单被拒绝。
- [ ] 非本人订单不能支付。
- [ ] 已取消/已完成订单不能支付。
- [ ] 重复预下单返回同一有效 `prepay_id`（或明确的新支付单策略）。
- [ ] 回调签名错误被拒绝。
- [ ] 回调金额不一致被拒绝。
- [ ] 重复回调只入账一次。
- [ ] 并发回调只有一个成功流转订单。
- [ ] 已支付订单再次支付直接返回当前订单。

联调测试（真实环境，建议 1 分钱商品）：

- [ ] 小程序调起支付成功，订单变待发货。
- [ ] 用户取消支付，订单仍待付款。
- [ ] 支付成功后回调延迟，客户端轮询能恢复。
- [ ] 关单后无法继续支付。
- [ ] 重复点击支付不产生多笔成功支付。
- [ ] 退款 1 分钱成功，状态与金额正确。
- [ ] 断网/超时后主动查单能补偿。
- [ ] 对账无差异。

### 4.9 上线与降级

- 生产 `APP_PAYMENT_MOCK_ENABLED=false`、`APP_DEV_LOGIN_ENABLED=false`，启动校验必须通过。
- `WECHAT_PAY_ENABLED=false` 时，前端隐藏支付按钮或展示「支付暂未开通」，不能静默成功。
- 支付异常时保留订单待付款状态，允许用户重试或取消，不制造假支付。
- 密钥轮换、证书过期、回调积压要有监控与告警。
- 上线前按 [`docs/release-checklist.md`](./release-checklist.md) 第 2、4、7 节逐项核对。

---

## 5. 配置模板

`application.yml` 目标片段：

```yaml
app:
  wechat:
    app-id: ${WECHAT_APP_ID:}
    app-secret: ${WECHAT_APP_SECRET:}
  wechat-pay:
    enabled: ${WECHAT_PAY_ENABLED:false}
    mch-id: ${WECHAT_PAY_MCH_ID:}
    mch-serial-no: ${WECHAT_PAY_MCH_SERIAL_NO:}
    private-key-path: ${WECHAT_PAY_PRIVATE_KEY_PATH:}
    api-v3-key: ${WECHAT_PAY_API_V3_KEY:}
    public-key-id: ${WECHAT_PAY_PUBLIC_KEY_ID:}
    public-key-path: ${WECHAT_PAY_PUBLIC_KEY_PATH:}
    notify-url: ${WECHAT_PAY_NOTIFY_URL:}
```

生产环境只通过密钥管理/环境变量注入，不写入 `.env` 提交。

---

## 6. 常见故障排查

| 现象 | 排查 |
| --- | --- |
| `APPID_MCHID_NOT_MATCH` | 小程序 AppID 与商户号未绑定 |
| `SIGN_ERROR` | 证书序列号、私钥、签名串格式错误 |
| `OUT_TRADE_NO_USED` | 同一商户订单号重复下单，检查支付单复用策略 |
| 回调一直重试 | 本地未返回 200、验签失败、响应格式不对 |
| 回调解密失败 | APIv3 Key 错误或轮换未兼容 |
| 金额校验失败 | 元/分转换错误，或订单金额已变更 |
| 支付成功但订单未变 | 回调未到达/处理失败，用查单补偿 |
| `requestPayment:fail` | 小程序未绑定商户、参数签名错误、openid 不匹配 |

---

## 7. 官方文档

- 小程序支付产品介绍：<https://pay.weixin.qq.com/doc/v3/merchant/4012791894>
- 开发接入准备：<https://pay.weixin.qq.com/doc/v3/merchant/4015459512>
- JSAPI/小程序下单：<https://pay.weixin.qq.com/doc/v3/merchant/4012791897>
- 小程序调起支付：<https://pay.weixin.qq.com/doc/v3/merchant/4012791898>
- 支付成功回调：<https://pay.weixin.qq.com/doc/v3/merchant/4012791902>
- 关闭订单：<https://pay.weixin.qq.com/doc/v3/merchant/4012791901>
- 退款申请：<https://pay.weixin.qq.com/doc/v3/merchant/4012791903>
- 交易账单：<https://pay.weixin.qq.com/doc/v3/merchant/4012791907>
- 签名认证：<https://pay.weixin.qq.com/doc/v3/merchant/4012365342>
- 微信支付公钥/平台证书：<https://pay.weixin.qq.com/doc/v3/merchant/4024350132>
- 官方 Java SDK：<https://github.com/wechatpay-apiv3/wechatpay-java>
- 小程序登录 code2session：<https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html>
- 手机号快速验证：<https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-info/phone-number/getPhoneNumber.html>

> 微信接口与证书策略会更新。真实接入前以官方文档当前版本为准，不要直接照搬旧版 V2 的 MD5 签名方案。
