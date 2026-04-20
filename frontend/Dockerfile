# ========== Frontend Dockerfile ==========
FROM node:20-alpine AS builder

WORKDIR /app

# 复制 package.json 和依赖
COPY package*.json ./
RUN npm ci --prefer-offline --no-audit

# 复制源码
COPY . .

# 构建（指定生产环境）
RUN npm run build

# ========== 运行阶段 ==========
FROM nginx:alpine

# 复制 nginx 配置
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
