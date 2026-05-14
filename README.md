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
- 线索列表、详情、创建接口占位。
- AI 评分和话术接口占位。
- 工作台数据接口占位。
- Flyway MySQL 初始化脚本。

前端：

- Next.js 应用骨架。
- Tailwind CSS 配置。
- 工作台首页。
- 指标卡片。
- 高分线索表。
- AI 优化建议面板。

## 下一步建议

1. 完成真实登录、JWT、用户和租户表落库。
2. 引入 MyBatis-Plus 实体、Mapper、Service。
3. 完成线索导入和导入任务表。
4. 接入真实 AI Provider 适配层。
5. 把前端工作台接入真实 API。

## 后端依赖下载说明

如果本机全局 Maven 配置的镜像不可用，可以使用项目内置配置：

```bash
cd backend
mvn -s maven-setting.xml -Dmaven.repo.local=.m2/repository test
```

当前 `backend/maven-setting.xml` 强制使用 Maven Central，并配置了本机代理 `127.0.0.1:7890`。如果本机代理端口不同，需要同步修改该文件里的 `<proxies>`。
