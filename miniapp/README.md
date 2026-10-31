# 小程序端（原生微信小程序）

## 技术栈

- 语言：JavaScript（ES6+）+ WXML（模板）+ WXSS（样式）+ JSON（配置）
- 框架：微信原生小程序（不使用 uni-app / Taro，无需 npm 构建，开发者工具直接运行）
- 网络：`wx.request` 封装（`utils/request.js`），JWT 放在 Authorization 头，401 自动回登录页
- 附件：`wx.downloadFile` 带 token 下载后 `wx.openDocument` 预览

## 目录结构

```
miniapp/
├── app.js / app.json / app.wxss     全局入口、页面注册与 tabBar、主题样式
├── config.js                        BASE_URL（默认 http://localhost:8080/api）
├── utils/
│   ├── request.js                   wx.request / uploadFile / downloadFile 封装
│   ├── auth.js                      登录态、角色判断
│   ├── format.js                    时间、状态文案、文件大小
│   └── api.js                       后端接口封装
├── pages/
│   ├── login/                       账号登录 + 微信登录入口
│   ├── index/                       首页（按角色显示统计与快捷入口）
│   ├── archive/list/                档案列表（状态筛选、下拉刷新、分页）
│   ├── archive/detail/              详情（元数据、附件预览、审核记录、审核/修改入口）
│   ├── fill/                        填报/修改档案（分类级联、密级期限、拍照或选文件上传）
│   ├── push/list/ + push/detail/    政策推送
│   ├── borrow/                      可借阅档案 / 本协会借阅记录 / 审批
│   ├── task/list/ + task/detail/    专项补录任务
│   └── mine/                        个人中心（改名、改密、退出）
```

## 角色与界面差异

| 功能 | 商协会管理员（ASSOC） | 主管部门操作员（ADMIN） | 主管领导（LEADER） |
|---|---|---|---|
| 首页 | 本会档案统计，填报/推送/借阅/补录入口 | 待审核档案、借阅待审批、审核入口 | 档案查阅、借阅记录、推送、补录（只读） |
| 档案列表 | 本会档案，默认全部 | 全量档案，默认「待审核」 | 全量档案，只读 |
| 档案详情 | 草稿/退回可「修改档案」 | 待审核可「通过并归档 / 退回修改」 | 只读 |
| 填报档案 | 可填报、拍照或选文件上传 | 不可用（后端拒绝） | 不可用 |
| 借阅 | 可借阅档案 + 本协会借阅记录 + 申请 | 全部借阅记录 + 通过/拒绝/登记归还 | 全部借阅记录（只读） |
| 专项补录 | 查看任务与说明 | 查看 | 查看 |

> 权限由后端 `@RequireRole` 与行级隔离兜底，前端只是按角色隐藏入口。

## 本地联调

1. 后端启动（8080）：`cd associationWechatApply && mvn spring-boot:run`
2. 用微信开发者工具「导入项目」，目录选择本文件夹 `miniapp`
3. AppID 可选「测试号」或「无 AppID（游客模式）」
4. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
5. 账号登录：`A001 / 123456`（商协会管理员）或 `admin / 123456`

> 真机预览时把 `config.js` 的 `localhost` 改成电脑局域网 IP（如 `http://192.168.1.10:8080/api`），并确保手机与电脑同一网络。

## 真机调试（手机上登录没反应时先看这里）

1. **后端地址不能是 localhost**：手机上的 `localhost` 指手机自己。登录页底部点「服务器：…」改成电脑的局域网 IP，
   例如 `http://192.168.1.10:8080/api`（在电脑上 `ipconfig` 看「无线局域网适配器 WLAN」的 IPv4）；
   「我的」页也有同一个「服务器地址」入口，登录前改也可以。
2. **手机与电脑连同一个 Wi-Fi**，且电脑防火墙放行 8080（首次启动 Java 时选择「允许访问」）。
3. **真机调试面板里勾选「不校验合法域名」**（「真机调试」窗口的设置里有这个开关）；预览/体验版则要求 HTTPS + 已备案域名。
4. 请求失败时小程序会弹「无法连接服务器」并显示当前地址，按提示核对 IP 与端口；同一会话只弹一次，之后用 toast。
5. 验证后端能否被手机访问：手机浏览器打开 `http://<电脑IP>:8080/api/auth/me`，返回 JSON（401 未登录）说明网络已通。

## 微信绑定与一键登录

**流程**：先用账号密码登录 → 「我的」→「微信绑定」→ 成功后再退出，即可用「微信一键登录」。

| 接口 | 说明 |
|---|---|
| `POST /api/auth/wechat/bind` | 需登录，body `{ code }`；code 由 `wx.login()` 获取，后端换 openid 后写入 `sys_user.wx_openid` |
| `POST /api/auth/wechat/unbind` | 需登录，清空当前账号的 openid |
| `GET /api/auth/wechat/status` | 返回 `{ bound: true/false }`，用于「我的」页显示状态 |
| `POST /api/auth/wechat-login` | 免登录，body `{ code }`；openid 已绑定则直接签发 token |

**配置**（`.env`）：

```
ZSSX_WECHAT_APPID=你的小程序AppID
ZSSX_WECHAT_SECRET=你的小程序AppSecret
```

- 未配置或仍是占位值（`wx0000...`）时，绑定/登录会明确提示「未配置微信小程序 AppID/Secret」，不会静默失败；
- **游客模式（touristappid）无法换取真实 openid**，微信登录需要用真实 AppID 或测试号；
- 一个微信只能绑定一个账号，重复绑定会提示「该微信已绑定其他账号」；
- 绑定与解绑都会写入审计日志。

