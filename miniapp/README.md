# 小程序配置与导入

## 1. 安装微信开发者工具

从官方下载 macOS（Apple Silicon）稳定版并登录：

https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html

## 2. 安装依赖

```bash
cd miniapp
pnpm install
```

## 3. 项目配置

```bash
cp project.config.example.json project.config.json
```

`project.config.json` 已加入 `.gitignore`，不会提交个人 AppID。配置中已包含：

```json
"setting": {
  "useCompilerPlugins": ["typescript"],
  "urlCheck": false
}
```

- `useCompilerPlugins: ["typescript"]`：编译 `.ts` 页面必需
- `urlCheck: false`：本地开发不校验合法域名

## 4. 导入项目

微信开发者工具 → 「导入项目」：

| 项目 | 填写 |
| --- | --- |
| 目录 | `/Users/catalina/Workspace/module/mall-mini/miniapp` |
| AppID | 选择「测试号」或填写自有 AppID |
| 项目名称 | 优选商城 |
| 后端服务 | 不使用云服务 |

示例配置使用 `touristappid`，无 AppID 时可直接使用测试号。

## 5. 运行

1. 服务端需运行在 `http://localhost:8080`
2. 详情 → 本地设置，勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
3. 点击「编译」，首页加载轮播图、分类与样例商品
4. 「我的」→「开发登录（测试用）」完成登录，测试购物车、地址、下单与模拟支付

TDesign 目前未在页面中使用，不需要执行「构建 npm」。后续使用 TDesign 时，再执行「工具 → 构建 npm」。

## 6. 真机预览

真机不能访问 Mac 的 `localhost`：

1. 将 `utils/config.ts` 的 `apiBaseUrl` 改为 `http://<Mac 局域网 IP>:8080/api/v1`
2. 手机与 Mac 连接同一 Wi-Fi
3. 示例图片 URL 仍指向 `localhost:8080`，真机上会显示占位图；需要真机图片时同步替换图片域名
4. 生产环境替换为 HTTPS 域名，并关闭「不校验合法域名」

## 7. 生产环境

- 替换 `utils/config.ts` 中的 `apiBaseUrl`
- `project.config.json` 使用真实小程序 AppID
- 配置微信登录所需的 `WECHAT_APP_ID` / `WECHAT_APP_SECRET`（仅服务端）
- 不要在仓库提交 AppSecret、证书或真实 AppID
