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
│   ├── index/                       工作台（统计、快捷入口、最近档案）
│   ├── archive/list/                档案列表（状态筛选、下拉刷新、分页）
│   ├── archive/detail/              档案详情（元数据、附件预览、审核记录）
│   ├── push/list/ + push/detail/    政策推送
│   ├── borrow/                      可借阅档案 / 我的借阅 + 申请
│   ├── mine/                        个人中心（改名、改密、退出）
│   └── fill/                        档案填报（骨架，后续完善）
```

## 本地联调

1. 后端启动（8080）：`cd associationWechatApply && mvn spring-boot:run`
2. 用微信开发者工具「导入项目」，目录选择本文件夹 `miniapp`
3. AppID 可选「测试号」或「无 AppID（游客模式）」
4. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
5. 账号登录：`A001 / 123456`（商协会管理员）或 `admin / 123456`

> 真机预览时把 `config.js` 的 `localhost` 改成电脑局域网 IP（如 `http://192.168.1.10:8080/api`），并确保手机与电脑同一网络。
