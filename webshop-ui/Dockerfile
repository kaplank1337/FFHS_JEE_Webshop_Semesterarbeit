FROM node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# The repo's Vite config selects Nitro's Node server for this Docker build.
ENV DOCKER_BUILD=true \
    VITE_API_BASE_URL=/api \
    VITE_ORDER_WS_URL=/ws/orders
RUN npm run build

FROM node:22-alpine AS runtime
ENV NODE_ENV=production HOST=0.0.0.0 PORT=3000
WORKDIR /app
COPY --from=build --chown=node:node /app/.output ./
USER node
EXPOSE 3000
CMD ["node", "/app/server/index.mjs"]
