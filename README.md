# LeadSpark AI

LeadSpark AI 是一个面向 B2B 主动获客的智能销售系统。当前仓库已经按详细设计搭建了第一版工程骨架：

- `backend`：Java 21 + Spring Boot 3.x + MySQL + Flyway。
- `frontend`：Next.js + React + TypeScript + Tailwind CSS。
- `docker-compose.yml`：MySQL、Redis、API、Web 的本地编排。
- `docs`：需求、架构、详细设计和技术选型文档目前位于仓库根目录。

## 本地开发

### 1. 启动基础设施

```bash
colima start
docker-compose up -d mysql redis
```

### 2. 启动后端

```bash
cd backend
mvn -s maven-setting.xml -Dmaven.repo.local=.m2/repository spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

健康检查：

```text
GET http://localhost:8080/api/v1/health
```

Swagger：

```text
http://localhost:8080/swagger-ui.html
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev -- --hostname 127.0.0.1 --port 3000
```

前端默认地址：

```text
http://localhost:3000
```

## Docker 启动

```bash
colima start
docker-compose up --build
```

服务端口：

```text
Web:   http://localhost:3000
API:   http://localhost:8080
MySQL: localhost:3306
Redis: localhost:6379
```

## 当前已搭建内容

后端：

- Spring Boot 启动工程。
- 统一 API 响应结构。
- 全局异常处理。
- 基础 Security 配置。
- 健康检查接口。
- 登录接口占位。
- 线索列表、详情、创建接口，已接入 MySQL。
- AI 评分和话术接口，已保存推荐结果。
- 工作台数据接口，已从线索、任务、跟进、商机表聚合。
- 销售任务、跟进记录、商机创建和查询接口。
- 线索导入任务、导入错误记录、采集信号转线索接口。
- Flyway MySQL 初始化脚本。
- 同类产品企业信号每日采集任务。

前端：

- Next.js 应用骨架。
- Tailwind CSS 配置。
- 工作台首页，已接入真实后端 API。
- 线索中心页面：支持筛选、查看评分、手动新增线索、触发 AI 重新评分。
- 任务中心页面：支持筛选任务、创建任务、完成任务。
- 商机管理页面：支持查看商机、创建商机、推进阶段和更新赢单状态。
- 客户画像页面：聚合企业档案、联系人、意图信号、跟进记录、任务、商机和 AI 建议。
- 数据分析页面：展示线索增长、评分分布、来源效率、任务负荷和商机漏斗。
- 系统设置页面：展示采集配置、数据源状态、采集任务、导入任务，并支持手动采集和信号导入。
- 指标卡片。
- 高分线索表。
- 待跟进任务面板。
- 最近导入任务面板。

## 下一步建议

1. 完成真实登录、JWT、用户和租户表落库。
2. 引入 MyBatis-Plus 实体、Mapper、Service，替换当前 JdbcTemplate 原型实现。
3. 接入真实 AI Provider 适配层。
4. 增加 CSV/Excel 文件上传解析和字段映射。
5. 接入真实 AI Provider、企业数据 API 和权限体系。

## 后端依赖下载说明

如果本机全局 Maven 配置的镜像不可用，可以使用项目内置配置：

```bash
cd backend
mvn -s maven-setting.xml -Dmaven.repo.local=.m2/repository test
```

当前 `backend/maven-setting.xml` 强制使用 Maven Central，并配置了本机代理 `127.0.0.1:7890`。如果本机代理端口不同，需要同步修改该文件里的 `<proxies>`。

## 同类产品企业数据采集任务

系统内置一个每日定时任务，用于围绕励消云、探迹、销售易、纷享销客等同类产品关键词采集公开企业信号。

默认配置：

```yaml
leadspark:
  competitor-collection:
    enabled: true
    cron: 0 30 2 * * *
```

手动触发：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/competitor-collections/run
```

查看任务：

```bash
curl http://127.0.0.1:8080/api/v1/competitor-collections/jobs
```

查看采集信号：

```bash
curl http://127.0.0.1:8080/api/v1/competitor-collections/signals
```

把未导入的采集信号转成销售线索：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/import-tasks/competitor-signals \
  -H 'Content-Type: application/json' \
  -d '{"limit":100,"minConfidence":0}'
```

查看导入任务：

```bash
curl http://127.0.0.1:8080/api/v1/import-tasks
```

当前默认使用 `MockCompetitorDataProvider`，只生成占位数据。后续接入企查查、天眼查、搜索 API、新闻 API 或内部数据源时，实现 `CompetitorDataProvider` 即可。采集范围应限定在公开网页、授权 API 和企业自有数据内，不应绕过登录、验证码、robots 或平台访问限制。

已内置的数据源 Provider：

- `MockCompetitorDataProvider`：本地占位数据，默认开启。
- `QichachaCompetitorDataProvider`：企查查授权 API，默认关闭。
- `TianyanchaCompetitorDataProvider`：天眼查授权 API，默认关闭。
- `SearchApiCompetitorDataProvider`：通用搜索 API，默认关闭。
- `InternalCompanySourceProvider`：内部企业数据表，默认开启。
- `RecruitmentApiCompetitorDataProvider`：招聘数据 API，用于识别销售、增长、获客、CRM 等岗位需求信号，默认关闭。
- `BiddingApiCompetitorDataProvider`：招投标/采购公告 API，用于识别 CRM、营销、获客、销售系统采购需求，默认关闭。
- `NewsApiCompetitorDataProvider`：新闻/舆情 API，用于识别融资、扩张、数字化转型等事件，默认关闭。
- `WebsiteApiCompetitorDataProvider`：官网、案例页、产品页聚合 API，用于识别竞品客户和行业案例，默认关闭。

相关环境变量：

```bash
COMPETITOR_COLLECTION_ENABLED=true
COMPETITOR_COLLECTION_CRON="0 30 2 * * *"
COMPETITOR_SOURCE_MOCK_ENABLED=true

QICHACHA_ENABLED=false
QICHACHA_BASE_URL=
QICHACHA_API_KEY=

TIANYANCHA_ENABLED=false
TIANYANCHA_BASE_URL=
TIANYANCHA_API_KEY=

SEARCH_API_ENABLED=false
SEARCH_API_BASE_URL=
SEARCH_API_API_KEY=

INTERNAL_COMPANY_SOURCE_ENABLED=true

RECRUITMENT_API_ENABLED=false
RECRUITMENT_API_BASE_URL=
RECRUITMENT_API_API_KEY=

BIDDING_API_ENABLED=false
BIDDING_API_BASE_URL=
BIDDING_API_API_KEY=

NEWS_API_ENABLED=false
NEWS_API_BASE_URL=
NEWS_API_API_KEY=

WEBSITE_API_ENABLED=false
WEBSITE_API_BASE_URL=
WEBSITE_API_API_KEY=
```

外部 API 返回会被统一解析为企业信号，并落库到：

```text
competitor_company_signal
```

内部数据源可先写入：

```text
internal_company_source
```

每日任务会读取内部数据源中匹配关键词的企业，再统一落到 `competitor_company_signal`，后续可以继续扩展为自动转线索或进入 AI 评分流程。
