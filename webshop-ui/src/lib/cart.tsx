import { createContext, useContext, useState, type ReactNode } from "react";
import type { Product } from "./api";

export type CartLine = { product: Product; quantity: number };
type CartCtx = { lines: CartLine[]; add: (p: Product) => void; setQty: (id: string, q: number) => void; clear: () => void; total: number; count: number };
const Ctx = createContext<CartCtx | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>([]);
  const add = (p: Product) => setLines((l) => l.some((x) => x.product.id === p.id)
    ? l.map((x) => x.product.id === p.id ? { ...x, quantity: x.quantity + 1 } : x)
    : [...l, { product: p, quantity: 1 }]);
  const setQty = (id: string, q: number) => setLines((l) => q <= 0 ? l.filter((x) => x.product.id !== id) : l.map((x) => x.product.id === id ? { ...x, quantity: q } : x));
  const total = lines.reduce((s, l) => s + l.product.price * l.quantity, 0);
  const count = lines.reduce((s, l) => s + l.quantity, 0);
  return <Ctx.Provider value={{ lines, add, setQty, clear: () => setLines([]), total, count }}>{children}</Ctx.Provider>;
}
export function useCart() { const c = useContext(Ctx); if (!c) throw new Error("CartProvider fehlt"); return c; }
