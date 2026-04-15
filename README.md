# WeChat

基于 Spring Boot 3、MyBatis、MySQL、WebSocket 的即时聊天项目，支持大厅聊天、好友私聊、群聊、头像上传、文件发送、好友申请、消息分页加载和 Docker 部署。

## 功能概览

- 用户注册、登录、退出登录
- Token + HttpOnly Cookie 登录态，默认 6 小时过期
- 聊天大厅、好友私聊、群聊
- 好友申请、同意/拒绝申请、好友备注
- 头像上传、聊天文件上传
- 聊天图片缩略图预览与弹窗查看原图
- 聊天记录按 `beforeId` 滚动分页加载
- 上传文件按内容去重
- 聊天文件与头像文件分目录存储
- 定时清理 24 小时前的聊天文件

## 技术栈

- Java 17
- Spring Boot 3.3.x
- Spring Security
- MyBatis
- MySQL 8
- WebSocket
- Docker / Docker Compose

## 运行要求

- JDK 17
- Maven Wrapper
- MySQL 8.x

应用默认端口：

```text
20935
```

## 本地运行

先准备数据库连接环境变量：

```powershell
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="we_chat"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root"
$env:APP_AUTH_TOKEN_SECRET="change-this-secret"
.\mvnw.cmd spring-boot:run
```

也可以直接打包运行：

```powershell
.\mvnw.cmd clean package
java -jar .\target\wechat-*.jar
```

## 关键配置

应用主要读取以下环境变量：

```text
SERVER_PORT
MYSQL_HOST
MYSQL_PORT
MYSQL_DATABASE
MYSQL_USERNAME
MYSQL_PASSWORD
MYSQL_TIMEZONE
MYSQL_USE_SSL
APP_AUTH_TOKEN_SECRET
APP_UPLOAD_DIR
APP_CHAT_CLEANUP_CRON
```

`application.yml` 中上传目录的推荐写法如下：

```yaml
app:
  storage:
    upload-dir: /app/uploads
    chat-cleanup-cron: ${APP_CHAT_CLEANUP_CRON:0 0 * * * *}
```

## 上传文件说明

默认上传根目录为：

```text
/app/uploads
```

其中会自动分成两个子目录：

```text
/app/uploads/avatar
/app/uploads/chat
```

说明：

- 头像保存在 `avatar` 目录
- 聊天附件保存在 `chat` 目录
- 相同内容文件会基于 SHA-256 做去重
- 聊天文件目录下超过 24 小时的文件会按定时任务清理
- 头像目录不会被自动清理

## Docker 部署

### 1. 准备 `.env`

先复制模板：

```powershell
Copy-Item .env.example .env
```

### 2. 主 `docker-compose.yml`

默认只启动应用，不内置 MySQL：

```yaml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: ${APP_IMAGE:-wechat:local}
    restart: unless-stopped
    ports:
      - "${APP_PORT:-20935}:20935"
    environment:
      SERVER_PORT: 20935
      APP_UPLOAD_DIR: /app/uploads
      APP_AUTH_TOKEN_SECRET: ${APP_AUTH_TOKEN_SECRET:?set APP_AUTH_TOKEN_SECRET in .env}
      MYSQL_HOST: ${MYSQL_HOST:-127.0.0.1}
      MYSQL_PORT: ${MYSQL_PORT:-3306}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-we_chat}
      MYSQL_USERNAME: ${MYSQL_USERNAME:-root}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-root}
      MYSQL_TIMEZONE: ${MYSQL_TIMEZONE:-Asia/Shanghai}
      MYSQL_USE_SSL: ${MYSQL_USE_SSL:-false}
      APP_CHAT_CLEANUP_CRON: ${APP_CHAT_CLEANUP_CRON:-0 0 * * * *}
    volumes:
      - /home/app/files:/app/uploads
```

### 3. 可选 `docker-compose.mysql.yml`

只有你明确需要本地 MySQL 容器时才叠加：

```yaml
services:
  mysql:
    image: mysql:8.4
    restart: unless-stopped
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    ports:
      - "${MYSQL_EXPOSE_PORT:-3306}:3306"
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE:-we_chat}
      MYSQL_USER: ${MYSQL_USERNAME:-wechat}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-wechat_password}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root_password}
      TZ: ${MYSQL_TIMEZONE:-Asia/Shanghai}
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "-p${MYSQL_ROOT_PASSWORD:-root_password}"]
      interval: 10s
      timeout: 5s
      retries: 10

volumes:
  mysql-data:
```

### 4. `.env.example`

```env
# This file is a local/server deployment template.
# It is not uploaded to the Docker image registry.
# Copy it to .env and adjust the values before running docker compose.

APP_PORT=20935
APP_IMAGE=registry.example.com/personal/wechat:latest
APP_AUTH_TOKEN_SECRET=change-this-secret

# External MySQL by default.
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=we_chat
MYSQL_USERNAME=root
MYSQL_PASSWORD=root
MYSQL_TIMEZONE=Asia/Shanghai
MYSQL_USE_SSL=false

# Upload path mapping.
HOST_UPLOAD_DIR=/home/app/files

# Optional cleanup schedule.
APP_CHAT_CLEANUP_CRON=0 0 * * * *

# Only used when you also load docker-compose.mysql.yml.
MYSQL_ROOT_PASSWORD=root_password
MYSQL_EXPOSE_PORT=3306

# Optional image push settings.
DOCKER_REGISTRY=registry.example.com
DOCKER_REPOSITORY=personal/wechat
DOCKER_IMAGE_TAG=latest
```

## Docker 启动方式

### 使用外部 MySQL

```powershell
docker compose up -d --build
```

这种方式下：

- 不会启动 MySQL 容器
- 应用直接连接 `.env` 中配置的数据库
- 数据库地址不会写死在镜像里

### 使用可选内置 MySQL

```powershell
docker compose -f docker-compose.yml -f docker-compose.mysql.yml up -d --build
```

如果应用需要连接这个 compose 里的 MySQL，请把 `.env` 里的连接改成：

```env
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DATABASE=we_chat
MYSQL_USERNAME=wechat
MYSQL_PASSWORD=wechat_password
```

## 上传目录映射到服务器

如果你希望容器里的上传文件持久化到服务器目录 `/home/app/files`，推荐这样配置：

```yaml
services:
  app:
    environment:
      APP_UPLOAD_DIR: /app/uploads
    volumes:
      - /home/app/files:/app/uploads
```

对应关系是：

- 服务器目录：`/home/app/files`
- 容器内目录：`/app/uploads`
- 应用实际读写目录：`/app/uploads`

建议先在服务器上创建目录：

```bash
mkdir -p /home/app/files
```

## 构建镜像

```powershell
docker build -t wechat:local .
```

## 推送镜像到远程仓库

### 使用脚本推送

```powershell
$env:DOCKER_REGISTRY="registry.example.com"
$env:DOCKER_REPOSITORY="personal/wechat"
$env:DOCKER_IMAGE_TAG="latest"
docker login $env:DOCKER_REGISTRY
.\scripts\push-image.ps1
```

### 使用 Maven Jib

```powershell
$env:DOCKER_USERNAME="your-username"
$env:DOCKER_PASSWORD="your-password"
.\mvnw.cmd "-Ddocker.registry=registry.example.com/personal" "-Djib.to.image=registry.example.com/personal/wechat:latest" jib:build
```

## Docker 仓库与配置文件的关系

上传到 Docker 镜像仓库的只有镜像本身，不会自动包含这些部署文件：

- `docker-compose.yml`
- `docker-compose.mysql.yml`
- `.env`
- `.env.example`

这些文件仍然应该保存在：

- Git 仓库
- 或服务器部署目录

标准部署流程通常是：

1. 服务器保留 `docker-compose.yml` 和 `.env`
2. 服务器执行 `docker compose pull`
3. 服务器执行 `docker compose up -d`

## 数据库初始化

项目自带 [schema.sql](./src/main/resources/schema.sql)，应用启动时会执行表结构初始化。

如果你的数据库账号没有建表或改表权限，建议提前手动执行 `schema.sql`。

## 常见问题

### 1. 为什么重启后头像或附件丢失

通常是上传目录没有做宿主机挂载，或者 `APP_UPLOAD_DIR` 指向了容器临时目录。请确认：

- 容器内写入路径是 `/app/uploads`
- `docker-compose.yml` 已配置 `volumes`
- 宿主机目录存在且权限正确

### 2. 为什么上传图片后重复请求头像

项目已经对 `/uploads/**` 设置了浏览器缓存头。若浏览器仍旧频繁请求，先强刷缓存后再验证。

### 3. 为什么表情写入 MySQL 失败

数据库和表需要使用 `utf8mb4`。如果是存量数据库，请确认表字段和连接参数已经按 `utf8mb4` 配好。

## 开发建议

- 生产环境务必修改 `APP_AUTH_TOKEN_SECRET`
- 生产环境不要使用 README 中的示例数据库密码
- 推荐把数据库和上传目录都放到容器外部

