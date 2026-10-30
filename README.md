# 中山商协会档案资料收集与服务系统

毕业设计项目：面向中山市商协会的“一协一档”数字化档案库，实现六大类档案（登记、治理、会员、活动、财务、党建）线上填报、多级审核、电子归档、检索借阅、可视化统计与政策精准推送。依据同目录《需求规格说明书》（ZSSX-SRS-2026-001，V1.0）开发。

## 项目结构

```
associationWechatApply/
├── src/                    # 后端：Spring Boot 3.5 + MyBatis-Plus + MySQL + JWT
│   ├── main/java/com/wechatapply/associationwechatapply/
│   │   ├── common/         # 统一响应 R / 错误码 / 全局异常 / JWT / SHA-256 / 档案编码
│   │   ├── config/         # Web MVC（跨域+拦截器白名单）/ MyBatis-Plus（分页+公共字段填充）
│   │   ├── framework/      # @RequireRole 角色鉴权 / LoginContext 行级隔离 / @AuditLog 审计切面
│   │   ├── entity/         # 10 个核心实体（SRS 5.2 数据表）
│   │   ├── mapper/         # MyBatis-Plus Mapper
│   │   ├── dto/            # 请求对象（登录/填报/审核/借阅）
│   │   ├── service/impl/   # 认证、档案、文件存储（哈希固证）、借阅、统计
│   │   └── controller/     # /api/** 接口（按 SRS 表 4-1 权限矩阵标注角色）
│   ├── main/resources/
│   │   ├── application.yml # 数据库/JWT/存储/微信配置
│   │   └── sql/            # schema.sql（建表）+ init-data.sql（预置账号与六类分类）
│   └── test/               # JUnit：档案编码规则、哈希工具（SRS 8.1）
├── web-admin/              # Web 管理后台：Vue3 + Element Plus + ECharts（SRS 2.2）
│   └── src/{api,router,store,layout,views}   # 登录/看板/档案/审核归档/借阅/补录/推送/名录/审计
└── miniapp/                # 微信小程序填报端：工作台/填报/消息/我的（SRS 7.2）
```

## 角色与初始账号（init-data.sql）

| 账号 | 口令 | 角色 | 数据范围 |
|------|------|------|----------|
| admin | 123456 | 主管部门操作员 | 全局 |
| leader | 123456 | 主管领导（只读） | 全局只读 |
| A001 / A002 / A003 | 123456 | 商协会管理员 | 仅本会 |

## 快速启动

**Java 环境**：JDK 17（项目编译目标为 17，Spring Boot 3.5 支持 17–24；IDEA 中把 Project SDK 与 Language level 都设为 17）。

1. **数据库**：MySQL 8.0 执行 `src/main/resources/sql/schema.sql`，再执行 `init-data.sql`；
   数据库口令等敏感配置写在项目根目录的 `.env` 中：复制 `.env.example` 为 `.env` 填写本机值
   （`.env` 已被 .gitignore 忽略，不会入库）。
2. **后端**：`mvn spring-boot:run`（默认 8080 口）。
   附件内容保存在数据库 `biz_attachment.file_content`，导出/导入数据库即可带走附件；
   `zssx.storage.root` 仅用于兼容迁移到数据库之前的历史磁盘附件。
3. **管理后台**：`cd web-admin && npm install && npm run dev`（5173 口，已配置 /api 代理到 8080）。
4. **小程序**：微信开发者工具导入 `miniapp/`，将 `project.config.json` 的 appid 替换为真实 AppID；
   详情设置中勾选“不校验合法域名”以便本地联调（`utils/request.js` 的 BASE_URL 指向本机 8080）。

## 批量导入与模板（SRS FR-04-04）

档案管理页（商协会管理员）提供 **批量导入** 与紧邻的 **下载模板**：

- 模板为 xlsx，六列固定：`分类编码 / 题名 / 年度 / 责任者 / 密级 / 保管期限`，附两行示例；
- 导入时逐行校验：分类是否存在且启用、题名非空、年度为 4 位、密级填 `公开/内部`、保管期限填 `永久/定期30年/定期10年`，
  并按分类的元数据字典校验必填项；
- 可选“存草稿”或“提交送审”；
- 返回回执：总行数、成功数、失败数与逐行失败原因（行号与 Excel 行号一致），单次上限 500 行；
- 实现只用 JDK 自带的 ZIP + XML 解析（`SimpleXlsx`），不引入 POI 等额外依赖。

## 安全配置（重要）

- **敏感项统一放 `.env`**：项目根目录的 `.env` 由 `application.yml` 通过
  `spring.config.import: optional:file:./.env[.properties]` 加载；仓库只提交 `.env.example`，`.env` 已被忽略。
- **`.env` 可配置项**：`SERVER_PORT`、`ZSSX_DB_URL`、`ZSSX_DB_USERNAME`、`ZSSX_DB_PASSWORD`、`ZSSX_JWT_SECRET`、
  `ZSSX_JWT_EXPIRE_MINUTES`、`ZSSX_STORAGE_ROOT`、`ZSSX_URL_EXPIRE_MINUTES`、`ZSSX_WECHAT_APPID`、
  `ZSSX_WECHAT_SECRET`、`ZSSX_CORS_ORIGINS`。
- **生成密钥**：`openssl rand -base64 48` 的输出填到 `ZSSX_JWT_SECRET`。
- **演示账号**：`admin / leader / A001…` 口令统一 123456（`init-data.sql` 存的是 BCrypt 哈希，不是明文），
  部署到公网前必须逐一改密。
- **跨域**：默认只放行 `http://localhost:5173` 与 `http://127.0.0.1:5173`，部署时用 `ZSSX_CORS_ORIGINS` 指定实际域名。
- **鉴权**：除 `/api/auth/login`、`/api/auth/register`、`/api/auth/wechat-login` 外，所有接口都要求 JWT；
  管理类接口用 `@RequireRole` 限定角色，数据范围（本会/全局）在服务层按 `assocCode` 隔离，不依赖前端隐藏按钮。

## 接口约定（SRS 7.1）

- RESTful，前缀 `/api`，响应统一 `{code, message, data}`；除登录外均需 `Authorization: Bearer <JWT>`。
- 错误码分段：1xxx 用户权限、3xxx 档案、5xxx 借阅、9999 系统错误。
- 行级隔离在后端数据访问层强制实施（商协会用户仅本会数据），不依赖前端。

## 活跃度评估（SRS FR-08 / 表 3-11）

`GET /api/stats/activity` 实时计算、`POST /api/stats/activity/evaluate` 计算并写回商协会主档（仅主管部门），
另有定时任务每周一 02:00 自动重算（`zssx.activity.cron` 可配置）。

| 指标 | 权重 | 数据口径（归一化后 0–100） |
|---|---|---|
| 会员规模指数 | 25% | 会员类（C）已归档条目数 / 3 × 100，封顶 100 |
| 活动频次指数 | 25% | 活动类（D）已归档条目数 / 6 × 100，封顶 100 |
| 填报完整及时率 | 20% | 有已归档档案的大类数 / 6 × 100 |
| 年检达标 | 20% | 存在 A02（年检年报材料）归档记 100，否则 0 |
| 审核一次通过率 | 10% | (1 − 退回次数 / 审核次数) × 100，无审核记录按 100 |

综合得分 S = Σ(指标得分 × 权重)，S ≥ 80 示范型、60 ≤ S < 80 成长型、S < 60 休眠型。
归一化口径集中在 `ActivityScoreCalculator`，调整考核要求时改这一个类即可。

## 测试

```bash
cd associationWechatApply
mvn -Dtest=ArchiveReviewTest test   # “通过并归档”专项：档号生成、撞号重试、附件哈希、退回校验
mvn test                            # 全部单元测试（上下文测试需 ZSSX_RUN_CTX_TEST=true）
```

`ArchiveReviewTest` 覆盖以下场景，改动归档逻辑后先跑它：

| 用例 | 说明 |
|---|---|
| 档号跳过已删除记录 | 已删除档案仍占用唯一索引，新档号必须取“已用最大流水号 + 1” |
| 撞号自动重试 | 并发下唯一键冲突时重新取号，最多 3 次 |
| 附件哈希校验 | 内容与登记哈希不一致时中止归档，不落库 |
| 退回意见必填 | 退回但未填意见时拒绝 |
| 仅待审核可审核 | 已归档/草稿再次审核被拒绝 |
| 档号格式 | `ZSA-协会-大类+中类-年度-6位流水` |

## 框架阶段待完善清单

- [ ] 微信 `jscode2session` 真实调用（现以 mock openid 联调，见 `AuthServiceImpl.fetchOpenid`）
- [x] 附件流式下载与在线打开（`FileController.download`，内容随数据库保存）
- [ ] 活跃度评估加权评分计算与定时任务（指标与权重见 SRS 表 3-11）
- [ ] Excel 批量导入（模板下载/校验回执）、统计图表缓存、订阅消息通道
- [ ] 数据库每日增量/每周全量定时备份任务（SRS FR-10-05）
