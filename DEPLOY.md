# 部署手册（给 Agent / 运维看的）

> 目标：拿到这个仓库后，**不需要读代码**，照着本文就能把「智慧农业农产品溯源管理系统」跑起来并验证可用。
> 每一步都给出了命令和**预期结果**；结果不符时看文末「排错」。

---

## 0. 一眼看懂

| 项 | 值 |
|---|---|
| 后端 | Spring Boot 3.3 · Java 17+ · MyBatis-Plus · JWT，打包为 `backend/target/agri-trace.jar`，默认端口 **8080** |
| 前端 | Vue 3 + Vite + Element Plus + ECharts，打包为 `frontend/dist/` 静态文件 |
| 数据库 | MySQL 8.0（MariaDB 10.6+ 也可），库名 **agri_trace**，字符集 utf8mb4 |
| 接口前缀 | 所有接口都在 `/api/**` 下；前端通过 Nginx（或 Vite 代理）把 `/api` 转给后端 |
| 建表脚本 | `sql/schema.sql`（会 `DROP TABLE` 后重建！）→ `sql/data.sql`（演示数据） |
| 健康检查 | `GET /api/health` → `{"code":200,"msg":"success","data":{"status":"UP"}}` |
| 默认账号 | `admin` / `farmer` / `inspector` / `logistics`，密码都是 `123456` |

```
.
├── backend/              Spring Boot 后端（Dockerfile 在里面）
├── frontend/             Vue3 前端（Dockerfile、nginx.conf 在里面）
├── sql/schema.sql        建库建表
├── sql/data.sql          演示数据
├── deploy/nginx.conf     手动部署用的 Nginx 配置
├── deploy/agri-trace.service  手动部署用的 systemd 服务
├── docker-compose.yml    一键部署
└── .env.example          docker compose 的环境变量模板
```

**选哪种方式：**

- 机器上有 Docker → 用 **方式 A**（最省事，3 条命令）。
- 没有 Docker，是一台普通 Linux 服务器 → 用 **方式 B**。
- 只是本地开发 / 调试 → 用 **方式 C**。

---

## 方式 A：Docker Compose 一键部署（推荐）

**前置：** Docker 20+，Docker Compose v2（`docker compose version` 能输出版本号）。80 端口空闲（或在 `.env` 里改 `WEB_PORT`）。

```bash
cd <仓库根目录>
cp .env.example .env          # 按需修改密码、JWT_SECRET、端口
docker compose up -d --build  # 首次构建约 3~8 分钟（要下载 Maven / npm 依赖）
docker compose ps             # 三个容器都应是 running；mysql 显示 (healthy)
```

做了什么：

1. `mysql` 容器第一次启动时（数据卷为空）自动执行 `sql/schema.sql` 和 `sql/data.sql`。
2. `backend` 等 MySQL 健康后启动，通过环境变量连库。
3. `frontend` 是 Nginx：托管静态文件，并把 `/api/` 反向代理到 `backend:8080`。

验证（把 `localhost` 换成服务器 IP；改过 `WEB_PORT` 的要加端口）：

```bash
curl -s http://localhost/api/health
# 预期：{"code":200,"msg":"success","data":{"status":"UP"}}
```

然后执行文末的「验收清单」。

常用操作：

```bash
docker compose logs -f backend      # 看后端日志
docker compose restart backend      # 重启后端
docker compose down                 # 停止（保留数据）
docker compose down -v              # 停止并删除数据库数据卷（下次启动会重新导入演示数据）
```

> 注意：`sql/*.sql` 只在**数据卷为空**时执行。改了 SQL 想重新导入，要 `docker compose down -v` 后再 `up`。

---

## 方式 B：手动部署到 Linux 服务器

以下以 Ubuntu 22.04/24.04 为例，其它发行版把 `apt` 换成对应包管理器即可。

### B1. 安装依赖

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven nginx mysql-server
# Node.js 18+（构建前端用；只在构建机上需要）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - && sudo apt install -y nodejs
java -version   # 预期 17 或更高
node -v         # 预期 v18 或更高
```

### B2. 初始化数据库

```bash
# 给 root 设密码（如果已经有密码可跳过；下面假设密码是 root，请按实际修改）
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root'; FLUSH PRIVILEGES;"

mysql -uroot -proot < sql/schema.sql
mysql -uroot -proot < sql/data.sql

mysql -uroot -proot --default-character-set=utf8mb4 agri_trace -e "SELECT username, real_name FROM sys_user;"
# 预期：5 行，中文姓名正常显示（admin 系统管理员 / farmer 王建国 ……）
```

> 更安全的做法是单独建账号：
> `CREATE USER 'agri'@'localhost' IDENTIFIED BY '强密码'; GRANT ALL ON agri_trace.* TO 'agri'@'localhost';`
> 然后把下面的 `DB_USER` / `DB_PASSWORD` 换成它。

### B3. 构建并运行后端

```bash
cd backend
mvn -B -DskipTests package          # 产物：target/agri-trace.jar
sudo mkdir -p /opt/agri-trace && sudo cp target/agri-trace.jar /opt/agri-trace/
cd ..
```

先前台试跑一次（确认能连上库）：

```bash
DB_HOST=127.0.0.1 DB_USER=root DB_PASSWORD=root \
JWT_SECRET='换成至少32个字符的随机串-xxxxxxxxxxxxxxxx' \
java -jar /opt/agri-trace/agri-trace.jar
# 看到 "Started AgriTraceApplication" 即成功；另开终端：
curl -s http://127.0.0.1:8080/api/health
```

确认无误后 Ctrl+C，改用 systemd 常驻：

```bash
sudo cp deploy/agri-trace.service /etc/systemd/system/
sudo nano /etc/systemd/system/agri-trace.service    # 改 DB_PASSWORD、JWT_SECRET
sudo systemctl daemon-reload
sudo systemctl enable --now agri-trace
systemctl status agri-trace --no-pager              # 预期 active (running)
journalctl -u agri-trace -f                         # 看日志
```

### B4. 构建并发布前端

```bash
cd frontend
npm ci            # 或 npm install
npm run build     # 产物：dist/
sudo mkdir -p /var/www/agri-trace
sudo cp -r dist/* /var/www/agri-trace/
cd ..
```

### B5. 配置 Nginx

```bash
sudo cp deploy/nginx.conf /etc/nginx/conf.d/agri-trace.conf
# 如果 /etc/nginx/sites-enabled/default 也监听 80，先删掉它：
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
curl -s http://127.0.0.1/api/health     # 预期 code 200
```

`deploy/nginx.conf` 的要点（自己写配置时别漏）：

- `location /api/ { proxy_pass http://127.0.0.1:8080; ... }` —— **不要**在 `proxy_pass` 末尾加 `/`，否则 `/api` 前缀会被去掉。
- `location / { try_files $uri $uri/ /index.html; }` —— 前端是 history 路由，没有这行时刷新 `/trace/xxx` 会 404。
- 传 `X-Forwarded-For`，后端用它记录扫码 IP。

---

## 方式 C：本地开发

```bash
# 1. 数据库（同 B2）
mysql -uroot -p < sql/schema.sql && mysql -uroot -p < sql/data.sql

# 2. 后端（默认连 localhost:3306，root/root；密码不同就带上环境变量）
cd backend && DB_PASSWORD=你的密码 mvn spring-boot:run

# 3. 前端（另开终端；Vite 会把 /api 代理到 http://localhost:8080）
cd frontend && npm install && npm run dev
# 打开 http://localhost:5173
```

后端不在本机时：`VITE_API_TARGET=http://192.168.1.10:8080 npm run dev`。

---

## 环境变量

### 后端（`backend/src/main/resources/application.yml` 里读取）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_HOST` | `localhost` | MySQL 地址 |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `agri_trace` | 库名，需与 `sql/schema.sql` 一致 |
| `DB_USER` | `root` | |
| `DB_PASSWORD` | `root` | |
| `JWT_SECRET` | 开发用默认串 | **生产必须改**，至少 32 个字符；改了之后已登录的用户需要重新登录 |
| `JWT_EXPIRE_HOURS` | `24` | 登录有效期（小时） |
| `CORS_ORIGINS` | `*` | 允许跨域的来源，逗号分隔。走 Nginx 同域部署时用不到 |

也可以用命令行参数覆盖：`java -jar agri-trace.jar --DB_PASSWORD=xxx`。

### 前端（构建时读取，可选）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_BASE` | `/api` | 接口前缀。前后端不同域部署时可设为 `https://api.example.com/api`（同时设置后端 `CORS_ORIGINS`） |
| `VITE_API_TARGET` | `http://localhost:8080` | 仅 `npm run dev` 时的代理目标 |

### docker compose（`.env`）

`MYSQL_ROOT_PASSWORD`、`JWT_SECRET`、`WEB_PORT`、`MYSQL_PORT`，见 `.env.example`。

---

## 验收清单（部署完逐条执行）

以下 `BASE` 为站点地址，例如 `http://localhost` 或 `http://服务器IP`。

```bash
BASE=http://localhost

# 1. 健康检查
curl -s $BASE/api/health
# → {"code":200,...,"data":{"status":"UP"}}

# 2. 登录拿 token
TOKEN=$(curl -s $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
echo $TOKEN    # → 一长串 eyJ... 开头的字符

# 3. 带 token 访问看板
curl -s $BASE/api/dashboard/overview -H "Authorization: Bearer $TOKEN" | head -c 200
# → {"code":200,"msg":"success","data":{"cards":{"baseCount":6,"productCount":6,"batchCount":8,...

# 4. 不带 token 应被拒绝
curl -s $BASE/api/batches
# → {"code":401,"msg":"请先登录","data":null}

# 5. 消费者扫码接口（无需登录）
curl -s $BASE/api/public/trace/T6QK8M3NXA2P | head -c 120
# → {"code":200,"msg":"success","data":{"batch":{"id":1,"batchNo":"B20251208-001",...

# 6. 前端页面
curl -s -o /dev/null -w "%{http_code}\n" $BASE/              # → 200
curl -s -o /dev/null -w "%{http_code}\n" $BASE/trace/T6QK8M3NXA2P   # → 200（history 路由回落正常）
```

浏览器里再确认：

- 打开 `$BASE/login`，点「管理员」演示账号 → 登录后进入数据看板，有图表。
- 打开 `$BASE/trace/T6QK8M3NXA2P`（手机尺寸也可）→ 显示「奉节脐橙」完整溯源时间线。

---

## 业务规则速查（验证功能时会用到）

批次状态自动流转，不满足条件的操作后端会直接返回提示：

```
GROWING 种植中 ──(农事记录登记「采收」)──▶ HARVESTED 已采收
HARVESTED ──(录入质检：合格)──▶ INSPECTED 质检合格   ──(登记发运)──▶ SHIPPING 运输中 ──(确认到达)──▶ ON_SALE 已上市
          └─(录入质检：不合格)─▶ REJECTED 质检不合格（不能发运、不能赋码）
```

- 只有「种植中 / 已采收」的批次能增改农事记录；进入质检后农事记录锁定。
- 只有质检合格（INSPECTED / SHIPPING / ON_SALE）的批次能发运、能生成溯源码；一批一码。
- 角色权限：`ADMIN` 全部；`FARMER` 基地/产品/批次/农事/赋码；`INSPECTOR` 质检；`LOGISTICS` 物流。只读查询所有登录用户都可以。

演示数据里的溯源码：`T6QK8M3NXA2P`（奉节脐橙，已上市）、`T9HD4W7RBC5E`（花椒，运输中）、`T3VZ6P8KYM4F`（永川秀芽，质检合格）、`T7LJ2C9TGW8H`（涪陵青菜头，已上市）。

---

## 排错

| 现象 | 原因 / 处理 |
|---|---|
| 后端启动报 `Communications link failure` | MySQL 没启动或地址不对。检查 `DB_HOST`/`DB_PORT`，`mysql -h$DB_HOST -uroot -p` 能否连上。Docker 方式下等 mysql 变成 healthy。 |
| 报 `Access denied for user 'root'@'...'` | 密码不对，或 root 只允许 socket 登录（Ubuntu 默认）。按 B2 给 root 设密码，或建专用账号。 |
| 报 `Public Key Retrieval is not allowed` | 连接串里已带 `allowPublicKeyRetrieval=true`；如果自己改过 URL，加回去。 |
| 报 `Unknown database 'agri_trace'` | 没执行 `sql/schema.sql`。 |
| 后端启动报 `app.jwt.secret 至少要 32 个字符` | `JWT_SECRET` 太短，换一个更长的。 |
| 页面上中文显示成 `???` | 导入 SQL 时字符集不对。重新执行：`mysql --default-character-set=utf8mb4 -uroot -p < sql/schema.sql`（会清空数据），再导 `data.sql`。 |
| 执行 `data.sql` 报 `Duplicate entry` | 库里已有数据。`data.sql` 只能在刚执行完 `schema.sql` 的空库上跑一次。 |
| 执行 `data.sql` 报 `WITH RECURSIVE` 语法错误 | 数据库版本太低。需要 MySQL 8.0+ 或 MariaDB 10.2+。 |
| 登录后马上又跳回登录页 | token 校验失败：多实例部署时各实例 `JWT_SECRET` 不一致，或系统时间严重不准。 |
| 刷新页面 404 | Nginx 少了 `try_files $uri $uri/ /index.html;`。 |
| 页面能开，接口全部 404 / 502 | Nginx `/api/` 没转发到后端，或后端没起来。`curl 127.0.0.1:8080/api/health` 先确认后端。 |
| 二维码扫出来的地址是 `localhost` | 二维码内容取浏览器当前地址（`location.origin`）。请用对外域名/IP 打开后台再下载标签。 |
| `docker compose up` 构建很慢或失败 | 依赖要从 Maven Central / npm 下载。国内服务器可给 Docker 配镜像加速，或在 `backend/Dockerfile` 前加 Maven 镜像设置、`frontend` 里设 `npm config set registry https://registry.npmmirror.com`。 |
| 端口被占用 | 改 `.env` 的 `WEB_PORT` / `MYSQL_PORT`，或 `SERVER_PORT`（手动部署时同步改 Nginx 里的 `proxy_pass`）。 |

---

## 给 Agent 的执行要点

1. 先判断环境：`docker compose version` 成功 → 方式 A；否则方式 B。
2. `sql/schema.sql` 会 **DROP 并重建所有表**。目标库已有业务数据时**不要**执行，先问人。
3. 生产环境一定要改 `JWT_SECRET` 和数据库密码；不要把 `.env` 提交到仓库（已在 `.gitignore` 中）。
4. 部署完按「验收清单」逐条执行，全部符合预期才算完成；把访问地址和默认账号告诉用户，并提醒登录后修改默认密码（右上角 → 个人中心）。
