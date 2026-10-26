# MallMini Android 客户端

原生 Android 客户端，使用 Kotlin + Jetpack Compose，对接现有 MallMini REST API。

## 功能范围（MVP）

- 首页：轮播图、分类入口、推荐商品
- 分类：一级分类、二级分类筛选、分页
- 搜索：关键词搜索
- 商品详情：图片、价格、库存、SKU、数量、加入购物车、立即购买
- 购物车：勾选、全选、数量、删除、清空、合计
- 收货地址：列表、新增、编辑、删除、设默认
- 结算：地址、商品、金额明细、提交订单
- 订单：状态筛选、详情、取消、模拟支付、确认收货
- 个人中心：开发登录、服务器地址、退出登录

## 技术栈

- Kotlin 2.0 / Jetpack Compose / Material 3
- Navigation Compose
- Retrofit + Gson + OkHttp
- Coil（含 SVG 解码）
- minSdk 26（Android 8.0）/ targetSdk 35

## 构建环境

| 组件 | 要求 |
| --- | --- |
| JDK | 17 |
| Android SDK | platform 35、build-tools 35.0.0、platform-tools |
| Gradle | 使用项目自带 wrapper 8.10.2 |
| Android Studio | 可选；命令行即可构建 |

当前机器 SDK 路径：

```text
/opt/homebrew/share/android-commandlinetools
```

`local.properties` 已写入 `sdk.dir`，该文件不提交仓库。

## 构建 APK

```bash
cd /Users/catalina/Workspace/module/mall-mini/android
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME
./gradlew :app:assembleDebug
```

产物：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

当前为 **debug 签名**，仅用于开发测试，不可直接上架应用商店。

## 安装到手机

### USB / ADB

1. 手机开启开发者选项与 USB 调试。
2. 连接 Mac，允许 USB 调试。
3. 安装 platform-tools（已安装）：

```bash
export PATH="/opt/homebrew/share/android-commandlinetools/platform-tools:$PATH"
adb devices
adb install -r /Users/catalina/Workspace/module/mall-mini/android/app/build/outputs/apk/debug/app-debug.apk
```

### 直接传输 APK

把 `app-debug.apk` 发送到手机（邮件、AirDrop、文件传输、网盘均可），在手机上允许「安装未知来源应用」后点击安装。已确认邮件传输安装可行。

## 连接服务端

1. Mac 启动 MySQL 与服务端（见根目录 README）。
2. 手机与 Mac 连接同一个 Wi-Fi。
3. App 内「我的 → 服务器设置」填写：

```text
http://<Mac 局域网 IP>:8080/api/v1
```

当前默认值：`http://192.168.2.7:8080/api/v1`（在 `app/build.gradle.kts` 的 `DEFAULT_API_BASE_URL` 修改）。

服务端必须监听 `0.0.0.0:8080`（默认），并允许 Mac 防火墙入站。

> 当前 debug 包允许 HTTP 明文请求，便于局域网调试；生产必须使用 HTTPS，并移除 `usesCleartextTraffic`。

## 登录

Android 端暂未接入微信 SDK，使用明确标记的开发登录：

1. 「我的」输入 openId（默认 `android-dev-user`）和昵称。
2. 点击「开发登录（测试）」。
3. 登录后可加购、下单、模拟支付。

生产应接入企业账号体系或微信开放平台 SDK，并单独验收。

## 支付

使用服务端开发环境模拟支付：

- 订单详情点击「模拟支付（开发环境）」
- 二次确认，不会产生真实扣款
- 生产环境服务端会禁用模拟支付

真实微信支付需接入微信支付 Android SDK/服务端 API v3 与回调验签，当前未实现。

## 项目结构

```text
android/app/src/main/java/com/mallmini/android/
├── MainActivity.kt
├── MallMiniApplication.kt        # Coil SVG ImageLoader
├── data/
│   ├── ApiConfig.kt              # API 地址与 localhost 图片重写
│   ├── ApiService.kt
│   ├── MallRepository.kt
│   ├── Models.kt
│   └── Preferences.kt
└── ui/
    ├── MallApp.kt                # 导航与底部栏
    ├── MallViewModel.kt
    ├── components/
    ├── screens/
    └── theme/
```

## 验证记录

已在 Android 35 模拟器完成：

- 安装与启动
- 首页 API 数据加载
- 开发登录
- 商品详情、SKU 选择、加入购物车
- 购物车、结算、提交订单
- 模拟支付
- 管理员发货后确认收货

管理员发货在 Mac 浏览器打开 `http://localhost:5173`，使用初始化时设置的 `APP_ADMIN_INIT_*` 账号密码。仓库不内置固定密码。

APK 已通过邮件安装到真实 Android 手机；完整交易流程在 Android 35 模拟器验证。

## 已知限制

- 未接入微信登录与真实微信支付
- 无推送、离线缓存、图片上传
- 收货地址为手填省市区，未接三级联动
- 首页分类入口暂未定位到具体分类
- Debug 包允许明文 HTTP，生产需切换 HTTPS
- 未做平板/横屏适配
