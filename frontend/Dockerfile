# =============================================
# Vue.js Frontend Dockerfile
# Multi-stage build for optimized production image
# =============================================

# Stage 1: Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for dependency caching
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Runtime stage with Nginx
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
