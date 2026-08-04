# ============================================================================
# frontend/Dockerfile — TICKET-I126
# ============================================================================
# WHAT:    Multi-stage Docker build for the React UI.
# HOW:     Stage 1: node builds the SPA. Stage 2: nginx serves /dist.
# WHY:     Smaller final image (no Node in prod) + nginx handles SPA routing
#          + reverse-proxies /api and /actuator to the backend service.
# OBSERVE: `docker build .` succeeds; `docker run -p 5173:80 <image>` serves
#          the UI, and /trades survives a hard refresh.
# ============================================================================

# ---- Stage 1: build -------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# Lockfile first so node_modules caches independently of source changes.
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Stage 2: nginx serve -------------------------------------------------
# The SPA is static at runtime, so the final image needs a file server, not
# Node. nginx-alpine is ~25 MB.
FROM nginx:1.27-alpine

# Custom server block: SPA fallback to index.html + /api/ and /actuator/
# reverse-proxy to the backend container. Replaces nginx's default site.
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
