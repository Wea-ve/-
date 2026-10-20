# =============================================
# 农产品全链路溯源管理平台 - 多阶段构建
# 构建：docker build -t nongye:latest .
# 运行：docker compose up -d
# =============================================

# ---------- 阶段一：构建前端 Vue3 SPA ----------
FROM node:20-alpine AS frontend-builder
WORKDIR /fe
# 使用国内 npm 镜像加速
RUN npm config set registry https://registry.npmmirror.com
# 先拷贝依赖清单，利用 Docker 层缓存
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# 产物输出到 /fe-dist（覆盖 vite.config.js 中的 outDir）
RUN npm run build -- --outDir /fe-dist --emptyOutDir

# ---------- 阶段二：构建后端 Spring Boot jar ----------
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app
# 阿里云 Maven 镜像加速
COPY docker/maven-settings.xml /root/.m2/settings.xml
# 先拷 pom 拉依赖，利用 Docker 层缓存
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
# 将前端产物合入静态资源目录，一起打进 jar
COPY --from=frontend-builder /fe-dist src/main/resources/static/app
RUN mvn -q package -DskipTests

# ---------- 阶段三：精简运行镜像 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=backend-builder /app/target/*.jar app.jar
# 上传文件（检测报告/头像/二维码）挂载卷持久化
VOLUME ["/app/uploads"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
