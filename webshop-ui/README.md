# Webshop

A webshop for browsing and ordering drinks, with user registration, login, and account deletion.

After logging in, users can place drink orders, update them, or delete them. The application also supports creating and deleting user accounts. It is built with TanStack Start and TypeScript.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/4ba1263f-b998-5f38-859e-793a4ab65b80).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm.

```sh
cd webshop-ui
npm install
cp .env.example .env.local
npm run dev
```

The UI listens on `http://localhost:3000`. By default it calls the locally
debugged GlassFish Gateway at `http://localhost:8080/api-gateway-1.0.0/api`.
If the Gateway runs in Docker instead, set `VITE_API_BASE_URL=http://localhost:8080/api`
in `.env.local`.

## Run with Docker Compose

From the repository root, start the complete stack, including the UI:

```sh
docker compose up --build -d
```

Open `http://localhost:3000`. In Compose the UI uses the host-published Gateway
at `http://localhost:8080/api`. Stop the stack with `docker compose down`.

To start only the UI container while using a separately running local Gateway:

```sh
VITE_API_BASE_URL=http://localhost:8080/api-gateway-1.0.0/api docker compose up --build webshop-ui
```

The environment variable selects the local GlassFish context root for this
command. The default Compose setting (`http://localhost:8080/api`) is used when
the full Docker backend stack is started.
