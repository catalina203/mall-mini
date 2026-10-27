# 物流接入操作手册

> 状态：**待接入设计文档**。当前仓库只在订单上保存「物流公司 + 物流单号」，没有物流轨迹查询、订阅推送、多包裹和自动确认收货。
>
> 目标：需要真实物流能力时，按本文完成承运商适配、轨迹查询、后台发货、用户端物流详情和自动确认收货。
>
> 原则：物流供应商通过适配器隔离；外部接口失败不能阻塞订单主流程；轨迹信息可缓存；用户只能查看自己的订单物流。

---

## 1. 当前基线

| 能力 | 状态 | 位置 |
| --- | --- | --- |
| 管理员发货 | 已实现，填写物流公司 + 单号 | `AdminOrderController#ship`、`OrderService#ship` |
| 订单物流字段 | 已实现 | `mall_order.logistics_company`、`logistics_no` |
| 状态流转 | 已实现 | `PENDING_SHIPMENT → PENDING_RECEIPT` |
| 物流轨迹 | **未实现** | 无 |
| 物流公司字典 | **未实现** | 前端自由文本 |
| 多包裹 | **未实现** | 单订单单号 |
| 自动确认收货 | **未实现** | 只有超时取消未支付订单 |
| 电子面单 | **未实现** | 可选 |

当前发货接口：

```text
POST /api/v1/admin/orders/{orderNo}/ship
{
  "logisticsCompany": "顺丰速运",
  "logisticsNo": "SF1234567890"
}
```

问题：

- 物流公司是自由文本，无法稳定映射到供应商编码；
- 无法查询轨迹，用户只能看到单号；
- 订单长期停留在「待收货」，没有自动完成；
- 多商品分包裹场景无法表达。

---

## 2. 方案选型

| 方案 | 优点 | 缺点 | 适用 |
| --- | --- | --- | --- |
| 聚合服务（快递100） | 覆盖快递公司多、支持订阅推送、电子面单 | 按查询/订阅计费，需处理供应商签名 | 首期推荐 |
| 聚合服务（快递鸟） | 覆盖广、即时查询 + 订阅、文档成熟 | 同上 | 首期可选 |
| 顺丰丰桥 | 顺丰直连、时效与服务稳定 | 只覆盖顺丰 | 顺丰单量占比高时 |
| 菜鸟开放平台 | 阿里生态、仓配一体 | 接入与资质要求较高 | 有仓配需求时 |

建议：

1. 首期用**一家聚合服务**打通「查询 + 订阅推送」，避免同时维护多家；
2. 定义稳定的 `LogisticsGateway` 接口，后续可加顺丰直连；
3. 电子面单是独立能力，等发货量稳定后再接。

参考入口：

- 快递100：<https://api.kuaidi100.com/>
- 快递鸟：<https://www.kdniao.com/>
- 顺丰丰桥：<https://open.sf-express.com/>
- 菜鸟开放平台：<https://open.taobao.com/>

> 具体接口路径、签名算法、计费以供应商当前文档为准。本文只约定内部适配层，不把某家字段写进订单核心业务。

---

## 3. 目标架构

```text
Admin / User API
      |
LogisticsService
  ├─ 归属与状态校验
  ├─ 轨迹缓存 / 限流
  ├─ 状态映射
  └─ LogisticsGateway（接口）
        ├─ Kuaidi100Gateway
        ├─ KdniaoGateway
        ├─ SfGateway（可选）
        └─ StubLogisticsGateway（仅 dev）
```

边界：

- `OrderService` 只负责订单状态与发货动作；
- `LogisticsService` 负责供应商调用、轨迹缓存、状态映射；
- 供应商 DTO 不进入 Controller 返回值；
- 外部调用不放在数据库长事务中；
- dev 环境没有凭证时使用 `StubLogisticsGateway`，并明确标记「模拟物流」。

---

## 4. 数据模型

新增 Flyway `V2__logistics.sql`（与微信支付迁移可合并到同一版本，按项目习惯拆分）：

```sql
CREATE TABLE logistics_company (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code          VARCHAR(32)     NOT NULL COMMENT '内部编码，如 SF',
    name          VARCHAR(64)     NOT NULL COMMENT '展示名称，如 顺丰速运',
    provider_code VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '供应商编码',
    enabled       TINYINT(1)      NOT NULL DEFAULT 1,
    sort_order    INT             NOT NULL DEFAULT 0,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_logistics_company_code (code),
    KEY idx_logistics_company_enabled_sort (enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物流公司字典';

CREATE TABLE order_shipment (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id       BIGINT UNSIGNED NOT NULL,
    order_no       VARCHAR(32)     NOT NULL,
    company_code   VARCHAR(32)     NOT NULL,
    company_name   VARCHAR(64)     NOT NULL,
    tracking_no    VARCHAR(64)     NOT NULL,
    status         VARCHAR(32)     NOT NULL DEFAULT 'CREATED',
    last_track_at  DATETIME(3)     NULL,
    last_track_desc VARCHAR(255)   NOT NULL DEFAULT '',
    delivered_at   DATETIME(3)     NULL,
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_shipment_company_tracking (company_code, tracking_no),
    KEY idx_shipment_order (order_id),
    KEY idx_shipment_status (status),
    KEY idx_shipment_delivered (delivered_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单包裹';

CREATE TABLE logistics_track (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    shipment_id  BIGINT UNSIGNED NOT NULL,
    track_time   DATETIME(3)     NOT NULL,
    status       VARCHAR(32)     NOT NULL,
    description  VARCHAR(255)    NOT NULL,
    location     VARCHAR(128)    NOT NULL DEFAULT '',
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_track_shipment_time (shipment_id, track_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物流轨迹';
```

兼容策略：

- 保留 `mall_order.logistics_company/logistics_no` 作为「主包裹」冗余字段，避免一次性改所有接口；
- 发货时写入 `order_shipment`，同时更新旧字段；
- 后续多包裹时，以 `order_shipment` 为准；
- 若需要按商品拆包，再增加 `order_shipment_item(shipment_id, order_item_id, quantity)`，不要提前设计复杂波次。

种子数据（可选，dev）：

```sql
INSERT INTO logistics_company (code, name, provider_code, sort_order) VALUES
('SF', '顺丰速运', 'shunfeng', 10),
('ZTO', '中通快递', 'zhongtong', 20),
('YTO', '圆通速递', 'yuantong', 30),
('JD', '京东物流', 'jd', 40);
```

> 供应商编码必须以其开放平台当前编码表为准，不要凭经验写死。

---

## 5. 接口契约

### 5.1 管理端

```text
GET  /api/v1/admin/logistics/companies          物流公司下拉
POST /api/v1/admin/orders/{orderNo}/ship        发货（可扩展 packages）
POST /api/v1/admin/shipments/{id}/refresh       手动刷新轨迹
GET  /api/v1/admin/orders/{orderNo}/shipments   订单包裹与轨迹
```

发货请求扩展（保持旧字段兼容）：

```json
{
  "logisticsCompany": "顺丰速运",
  "logisticsNo": "SF1234567890",
  "companyCode": "SF"
}
```

多包裹版本：

```json
{
  "packages": [
    { "companyCode": "SF", "trackingNo": "SF1234567890" },
    { "companyCode": "ZTO", "trackingNo": "781234567890" }
  ]
}
```

规则：

- 至少一个包裹；
- `companyCode + trackingNo` 全局唯一；
- 同一订单重复发货返回当前结果，不重复写包裹；
- 全部包裹创建成功后订单才进入 `PENDING_RECEIPT`；
- 任一包裹失败整单回滚数据库写入，外部已创建的电子面单走补偿。

### 5.2 用户端

```text
GET /api/v1/me/orders/{orderNo}/shipments
```

返回：

```json
{
  "orderNo": "MM...",
  "status": "PENDING_RECEIPT",
  "shipments": [
    {
      "companyName": "顺丰速运",
      "trackingNo": "SF1234567890",
      "status": "IN_TRANSIT",
      "lastTrackAt": "2026-10-26T10:00:00",
      "lastTrackDesc": "快件已到达深圳转运中心",
      "tracks": [
        { "time": "2026-10-26T10:00:00", "status": "IN_TRANSIT", "description": "快件已到达深圳转运中心" }
      ]
    }
  ]
}
```

规则：

- 必须校验订单属于当前用户；
- 只允许已发货/待收货/已完成/售后中的订单查询；
- 待付款/已取消订单不返回物流；
- 轨迹按时间倒序或正序统一，前端只做展示；
- 物流单号可展示，收件人手机号必须脱敏。

### 5.3 供应商回调（如使用订阅推送）

```text
POST /api/v1/logistics/callback/{provider}
```

要求：

- 校验供应商签名/Token；
- 幂等：同一 `trackingNo + trackTime + description` 不重复插入；
- 快速返回供应商要求的响应格式；
- 不在回调里做发货、退款等业务动作；
- 失败记录原始请求摘要（脱敏）并告警。

---

## 6. 分步接入

### Step 0：准备供应商账号

- [ ] 选择聚合服务商并完成企业认证；
- [ ] 获取 `customer` / `appKey` / `secret` / 回调 Token；
- [ ] 确认查询接口、订阅接口、电子面单接口的计费与频控；
- [ ] 配置回调域名（HTTPS）并加入白名单；
- [ ] 在测试环境用已知单号验证查询返回结构。

### Step 1：配置

在 `AppProperties` 增加：

```yaml
app:
  logistics:
    provider: ${APP_LOGISTICS_PROVIDER:stub}        # stub | kuaidi100 | kdniao
    enabled: ${APP_LOGISTICS_ENABLED:false}
    customer: ${APP_LOGISTICS_CUSTOMER:}
    app-key: ${APP_LOGISTICS_KEY:}
    secret: ${APP_LOGISTICS_SECRET:}
    callback-token: ${APP_LOGISTICS_CALLBACK_TOKEN:}
    timeout-ms: ${APP_LOGISTICS_TIMEOUT_MS:3000}
    cache-minutes: ${APP_LOGISTICS_CACHE_MINUTES:10}
  order:
    auto-confirm-enabled: ${APP_ORDER_AUTO_CONFIRM_ENABLED:true}
    auto-confirm-days: ${APP_ORDER_AUTO_CONFIRM_DAYS:7}
```

要求：

- `provider=stub` 仅允许 dev/test；生产启动校验拒绝；
- 未配置凭证时，发货仍可只保存单号，但轨迹接口返回「物流信息暂不可用」，不能报 500；
- 所有外部请求设置连接/读取超时，禁止无限等待。

### Step 2：迁移与字典

- 执行 `V2__logistics.sql`；
- 导入常用物流公司；
- 回填历史订单：为已有 `logistics_no` 的订单创建 `order_shipment`（可选，按需）；
- 管理后台发货下拉改为读取 `logistics_company`，不再自由输入。

### Step 3：适配器接口

```java
public interface LogisticsGateway {

    String provider();

    TrackingResult query(TrackingQuery query);

    default WaybillResult createWaybill(WaybillRequest request) {
        throw new UnsupportedOperationException("当前供应商未接入电子面单");
    }
}
```

```java
public record TrackingQuery(
        String companyCode,
        String providerCompanyCode,
        String trackingNo,
        String receiverPhoneLast4
) {}
```

```java
public record TrackingResult(
        boolean success,
        String state,
        String stateText,
        List<TrackNode> nodes,
        String message
) {}
```

实现：

- `StubLogisticsGateway`：dev 返回固定轨迹，页面明确标注「模拟物流」；
- `Kuaidi100Gateway` / `KdniaoGateway`：负责签名、请求、响应解析；
- 供应商异常转换为内部 `LogisticsException`，Controller 统一错误；
- 不把供应商原始 JSON 直接返回给前端。

### Step 4：发货改造

改造 `OrderService#ship`：

1. 校验订单状态 `PENDING_SHIPMENT`；
2. 校验物流公司启用、单号格式、同单号未被其他订单占用；
3. 写 `order_shipment` 与旧字段；
4. 订单条件更新 `PENDING_SHIPMENT → PENDING_RECEIPT`；
5. 写 `order_status_log` 与管理员操作日志；
6. 外部电子面单调用放在事务提交后；
7. 重复发货幂等返回当前结果。

事务边界：

- 数据库写入在一个事务内；
- 调供应商查询/面单不在事务内；
- 外部失败不回滚已成功的发货，进入补偿队列或人工处理。

### Step 5：轨迹查询与缓存

`LogisticsService`：

- 先查本地缓存：未签收 5–10 分钟，已签收 30 分钟以上；
- 缓存过期再调供应商；
- 用户手动刷新做频率限制（如每订单每分钟 1 次）；
- 查询失败返回最近一次缓存并附「更新中」，不要整页报错；
- 轨迹节点按 `trackTime` 去重写入。

### Step 6：订阅推送（推荐）

如果供应商支持订阅：

- 发货成功后调用订阅接口，绑定 `trackingNo + companyCode`；
- 回调到达后验签、解析、写轨迹、更新包裹状态；
- 处理重复推送；
- 订阅失败不影响发货，记录待重试；
- 定期检查长期无轨迹的包裹并告警。

若不支持订阅，使用定时任务轮询未签收包裹，注意供应商频控与成本。

### Step 7：自动确认收货

新增 `OrderAutoConfirmJob`：

```text
每 30 分钟扫描：
  status = PENDING_RECEIPT
  且存在已签收包裹
  且 delivered_at <= now - autoConfirmDays
  -> 条件更新 COMPLETED
  -> 写 SYSTEM 状态日志「超时自动确认收货」
```

规则：

- 默认 7 天，可通过 `APP_ORDER_AUTO_CONFIRM_DAYS` 配置；
- 开关关闭时只保留用户手动确认；
- 条件更新保证幂等；
- 售后中的订单不自动完成；
- 多包裹时，全部签收后才开始计时，或按合同规则定义并写入文档。

### Step 8：前端

管理后台：

- 发货弹窗：物流公司下拉、单号校验、可选多包裹；
- 订单详情：包裹列表、轨迹时间线、刷新按钮；
- 异常件标记与筛选。

小程序 / Android：

- 订单详情增加「查看物流」；
- 轨迹时间线展示最新节点；
- 空轨迹、查询失败、未发货有明确状态；
- 不做假轨迹，演示数据必须标注「模拟」。

### Step 9：测试

单元测试：

- [ ] 发货状态非法被拒绝。
- [ ] 重复发货不重复写包裹。
- [ ] 物流单号重复被拒绝。
- [ ] 用户不能查他人订单物流。
- [ ] 轨迹去重与状态映射正确。
- [ ] 自动确认收货只执行一次。
- [ ] 供应商超时返回缓存/友好错误。
- [ ] 回调签名错误被拒绝。
- [ ] 回调重复不重复写轨迹。

联调：

- [ ] 真实单号查询返回轨迹。
- [ ] 订阅推送能到达并更新状态。
- [ ] 已签收包裹在配置天数后自动完成。
- [ ] 供应商故障时订单页面仍可打开。
- [ ] 管理后台发货、用户查看、自动完成闭环。

### Step 10：上线

- 生产 `provider` 不能是 `stub`；
- 凭证只放服务端密钥管理；
- 回调地址 HTTPS 且可公网访问；
- 配置超时、重试、频控、告警；
- 先灰度一个物流公司，再全量；
- 保留手动发货/手动确认收货作为降级手段。

---

## 7. 状态映射

内部状态建议：

| 内部状态 | 含义 |
| --- | --- |
| `CREATED` | 已发货，暂无轨迹 |
| `IN_TRANSIT` | 运输中 |
| `OUT_FOR_DELIVERY` | 派送中 |
| `DELIVERED` | 已签收 |
| `EXCEPTION` | 异常件 |
| `RETURNED` | 退回/退签 |

映射要求：

- 每家供应商状态码在适配器内转换为内部状态；
- 无法识别的状态保留 `IN_TRANSIT` 并记录原始状态；
- 已签收是终态，不被后续旧轨迹回退；
- 异常件不自动确认收货。

---

## 8. 异常与补偿

- 查询超时：返回缓存 + 「物流信息更新中」；
- 供应商限流：指数退避，熔断，避免拖垮订单页；
- 单号错误：后台提示「暂无轨迹」，允许管理员修正单号（需审计）；
- 包裹丢失/退回：进入异常件列表，人工处理；
- 回调丢失：定时轮询补偿；
- 订阅失败：记录 `subscription_status`，后台可重试；
- 所有补偿任务幂等、可重复执行。

---

## 9. 安全与合规

- 供应商请求必须签名，Secret 不入前端、不入日志；
- 回调验签 + 时间戳校验；
- 用户只能查自己的订单物流；
- 日志不记录完整手机号、详细地址；
- 物流单号在后台列表可脱敏展示；
- 电子面单包含收件人信息，打印/下载权限仅限必要角色；
- 供应商数据只用于履约，不对外共享。

---

## 10. 验收清单

- [ ] 物流公司字典可维护，发货只能选择启用项。
- [ ] 发货后订单进入待收货，旧字段与新包裹表一致。
- [ ] 用户订单详情可查看轨迹，越权返回 404。
- [ ] 轨迹有缓存，供应商故障不导致页面崩溃。
- [ ] 已签收包裹按配置天数自动确认收货，且只执行一次。
- [ ] 多包裹订单状态与展示正确。
- [ ] 回调/订阅重复通知幂等。
- [ ] 管理后台可刷新轨迹、处理异常件。
- [ ] 生产未启用 Stub，凭证未进入仓库。
- [ ] 发布检查清单中的物流相关项全部通过。

---

## 11. 电子面单（可选，后续）

电子面单是独立能力，接入前需要：

- 供应商开通电子面单账号；
- 配置网点、模板、打印机；
- 调用「取号」接口获取面单号与打印报文；
- 面单号回写 `order_shipment.tracking_no`；
- 打印失败可重打，但取号幂等；
- 收件人信息仅用于面单，禁止缓存到日志。

建议在「查询 + 订阅」稳定后再接，不要与首期物流查询一起上线。
