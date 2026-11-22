export type User = { id: string; username: string; email: string; firstName: string; lastName: string; role: string; createdAt: string; updatedAt: string };
export type Product = { id: string; name: string; description?: string; category: string; price: number; stockQuantity: number; active: boolean };
export type OrderItem = { id?: string; productId: string; productName?: string; quantity: number; unitPrice?: number; totalPrice?: number; lineTotal?: number };
export type Order = { id: string; userId: string; status: OrderStatus; totalPrice: number; items: OrderItem[]; createdAt: string; updatedAt: string };
export const ORDER_STATUSES = ["CREATED", "CONFIRMED", "SHIPPING", "SHIPPED", "DELIVERED", "CANCELLED"] as const;
export type OrderStatus = (typeof ORDER_STATUSES)[number];

// The browser calls the Gateway directly. Override per environment because the
// local GlassFish context root differs from the Docker ROOT.war context root.
export const GATEWAY_URL = (
  import.meta.env["VITE_API_BASE_URL"] || "http://localhost:8080/api-gateway-1.0.0/api"
).replace(/\/$/, "");

// WebSocket is hosted by the Order-Service; REST traffic still goes through the Gateway.
export const ORDER_STATUS_WS_URL = import.meta.env["VITE_ORDER_WS_URL"]
  || "ws://localhost:11080/order-service-1.0.0/ws/orders";

export class ApiError extends Error { constructor(public status: number, msg: string) { super(msg); } }

const messages: Record<number, string> = {
  400: "Ungültige Eingabe", 401: "Nicht angemeldet oder falsche Zugangsdaten", 403: "Keine Berechtigung",
  404: "Nicht gefunden", 409: "Username oder E-Mail bereits vergeben",
};

async function request<T>(url: string, opts: { method?: string; body?: unknown; token?: string | null } = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(url, {
      method: opts.method ?? "GET",
      headers: {
        Accept: "application/json",
        ...(opts.body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(opts.token ? { Authorization: `Bearer ${opts.token}` } : {}),
      },
      body: opts.body !== undefined ? JSON.stringify(opts.body) : null,
    });
  } catch {
    throw new ApiError(0, "Backend nicht erreichbar (läuft der Server? CORS aktiviert?)");
  }
  if (!res.ok) {
    let detail = "";
    try { const t = await res.text(); try { const j = JSON.parse(t); detail = j.message || j.error || t; } catch { detail = t; } } catch {}
    throw new ApiError(res.status, detail || messages[res.status] || `Fehler ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

const gw = (p: string) => GATEWAY_URL.replace(/\/$/, "") + p;

export const api = {
  register: (b: { username: string; email: string; password: string; firstName: string; lastName: string }) =>
    request<User>(gw("/users/auth/register"), { method: "POST", body: b }),
  login: (b: { username: string; password: string }) =>
    request<{ accessToken: string; tokenType: string; expiresIn: number }>(gw("/users/auth/login"), { method: "POST", body: b }),
  me: (token: string) => request<User>(gw("/users/me"), { token }),
  deleteMe: (token: string) => request<void>(gw("/users/me"), { method: "DELETE", token }),
  products: () => request<Product[]>(gw("/products")),
  orders: (token: string) => request<Order[]>(gw("/orders"), { token }),
  createOrder: (token: string, items: { productId: string; quantity: number }[]) =>
    request<Order>(gw("/orders"), { method: "POST", body: { items }, token }),
  updateStatus: (token: string, id: string, status: OrderStatus) =>
    request<Order>(gw(`/orders/${id}/status`), { method: "PUT", body: { status }, token }),
  deleteOrder: (token: string, id: string) => request<void>(gw(`/orders/${id}`), { method: "DELETE", token }),
};

export const CATEGORY_LABELS: Record<string, string> = {
  WATER: "Wasser", SOFT_DRINKS: "Softdrinks", JUICE: "Saft", BEER: "Bier", COFFEE: "Kaffee",
};
export const STATUS_LABELS: Record<OrderStatus, string> = {
  CREATED: "Erstellt", CONFIRMED: "Erstellt", SHIPPING: "Wird geliefert", SHIPPED: "Wird geliefert", DELIVERED: "Geliefert", CANCELLED: "Storniert",
};
export const chf = (n: number) => `${Number(n ?? 0).toFixed(2)}`;
