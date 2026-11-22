FROM oven/bun:1

WORKDIR /app

COPY package.json bun.lock bunfig.toml ./
RUN bun install --frozen-lockfile

COPY . .

ENV HOST=0.0.0.0
EXPOSE 3000

CMD ["bun", "run", "dev"]
