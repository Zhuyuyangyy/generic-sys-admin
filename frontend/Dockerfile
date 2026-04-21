# ========== Frontend Dockerfile - Multi-stage build ==========
# 阶段一：构建
FROM node:20-alpine AS builder

WORKDIR /app

# 只复制 package.json 和 lock 文件，利用 Docker 缓存
COPY package*.json ./

# 安装依赖
RUN npm ci --prefer-offline --no-audit --legacy-peer-deps

# 复制源码
COPY . .

# 构建（生产环境变量）
RUN npm run build

# ========== 阶段二：运行（Nginx） ==========
FROM nginx:alpine

# 安装常用工具（用于调试）
RUN apk add --no-cache curl

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx 运行用户（默认 nginx）
# 注意：nginx alpine 镜像使用 nginx 用户而非 root

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]