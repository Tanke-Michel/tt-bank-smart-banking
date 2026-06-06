# =============================================================
# Frontend — Dockerfile
# Multi-stage: Node.js build → Nginx static file serving
# React 19 + Vite 8 + TypeScript
# =============================================================

# ---- Stage 1: Build ----
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first to cache node_modules layer
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build (VITE_API_URL injected at build time via --build-arg)
COPY . .

# The API URL points to the API Gateway — injected at build time.
# Default: gateway container name on Docker network.
ARG VITE_API_URL=http://api-gateway:8080
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# ---- Stage 2: Runtime (Nginx) ----
FROM nginx:1.27-alpine

# Remove default Nginx config — replaced by ours
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom Nginx config (handles SPA routing)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx runs as root by default; set permissions for non-root
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
