import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Beer, Coffee, Citrus, CupSoda, Droplets, Minus, Plus, ShoppingCart } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { RequireAuth } from "@/components/RequireAuth";
import { ApiError, api, CATEGORY_LABELS, chf, type Product } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";

export const Route = createFileRoute("/shop")({
  head: () => ({ meta: [{ title: "Getränkeshop – Durstlöscher" }, { name: "description", content: "Wähle Getränke aus und stelle deine Bestellung zusammen." }, { property: "og:title", content: "Getränkeshop – Durstlöscher" }, { property: "og:description", content: "Wähle Getränke aus und stelle deine Bestellung zusammen." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: () => <RequireAuth><Shop /></RequireAuth>,
});

const icons: Record<string, typeof Droplets> = { WATER: Droplets, SOFT_DRINKS: CupSoda, JUICE: Citrus, BEER: Beer, COFFEE: Coffee };

function Shop() {
  const { data, isLoading, error } = useQuery({ queryKey: ["products"], queryFn: api.products });
  const [cat, setCat] = useState<string | null>(null);
  const { add } = useCart();
  const list = (data ?? []).filter((p) => p.active !== false && (!cat || p.category === cat));
  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_340px]">
      <section className="space-y-5">
        <h1 className="text-3xl font-bold">Getränke</h1>
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant={cat === null ? "default" : "outline"} onClick={() => setCat(null)}>Alle</Button>
          {Object.entries(CATEGORY_LABELS).map(([k, v]) => <Button key={k} size="sm" variant={cat === k ? "default" : "outline"} onClick={() => setCat(k)}>{v}</Button>)}
        </div>
        {error && (
          <Alert variant="destructive">
            <AlertDescription>
              Produkte konnten nicht geladen werden: {(error as Error).message}
              {error instanceof ApiError && error.status === 404 && (
                <span className="mt-1 block text-xs">
                  Prüfe, ob das API-Gateway und der Order-Service laufen und die aktuelle Gateway-Version deployed ist.
                </span>
              )}
            </AlertDescription>
          </Alert>
        )}
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {isLoading && Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-48" />)}
          {list.map((p) => <ProductCard key={p.id} p={p} onAdd={() => { add(p); toast.success(`${p.name} im Warenkorb`); }} />)}
        </div>
      </section>
      <Cart />
    </div>
  );
}

function ProductCard({ p, onAdd }: { p: Product; onAdd: () => void }) {
  const Icon = icons[p.category] ?? CupSoda;
  const out = p.stockQuantity <= 0;
  return (
    <Card className="flex flex-col transition-shadow hover:shadow-md">
      <CardContent className="flex flex-1 flex-col gap-3 pt-6">
        <div className="flex items-start justify-between">
          <span className="grid h-12 w-12 place-items-center rounded-2xl bg-secondary text-secondary-foreground"><Icon className="h-6 w-6" /></span>
          <span className="text-xs font-medium text-muted-foreground">{CATEGORY_LABELS[p.category] ?? p.category}</span>
        </div>
        <div className="flex-1"><h3 className="font-semibold">{p.name}</h3>{p.description && <p className="text-sm text-muted-foreground">{p.description}</p>}</div>
        <p className="text-xs text-muted-foreground">{out ? "Ausverkauft" : `${p.stockQuantity} an Lager`}</p>
        <div className="flex items-center justify-between">
          <span className="text-xl font-bold">{chf(p.price)}</span>
          <Button size="sm" onClick={onAdd} disabled={out}><Plus className="mr-1 h-4 w-4" />In den Korb</Button>
        </div>
      </CardContent>
    </Card>
  );
}

function Cart() {
  const { lines, setQty, total, clear } = useCart();
  const { token } = useAuth();
  const nav = useNavigate();
  const [busy, setBusy] = useState(false);
  async function order() {
    setBusy(true);
    try {
      await api.createOrder(token!, lines.map((l) => ({ productId: l.product.id, quantity: l.quantity })));
      clear(); toast.success("Bestellung vom Backend bestätigt!"); nav({ to: "/orders" });
    } catch (e) { toast.error((e as Error).message); } finally { setBusy(false); }
  }
  return (
    <Card className="h-fit lg:sticky lg:top-24">
      <CardHeader><CardTitle className="flex items-center gap-2"><ShoppingCart className="h-5 w-5" />Warenkorb</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        {lines.length === 0 && <p className="text-sm text-muted-foreground">Noch leer – füge Getränke hinzu.</p>}
        {lines.map((l) => (
          <div key={l.product.id} className="flex items-center gap-2">
            <div className="flex-1 text-sm"><p className="font-medium">{l.product.name}</p><p className="text-muted-foreground">{chf(l.product.price)} × {l.quantity}</p></div>
            <Button size="icon" variant="outline" className="h-7 w-7" onClick={() => setQty(l.product.id, l.quantity - 1)}><Minus className="h-3 w-3" /></Button>
            <span className="w-6 text-center text-sm">{l.quantity}</span>
            <Button size="icon" variant="outline" className="h-7 w-7" onClick={() => setQty(l.product.id, l.quantity + 1)}><Plus className="h-3 w-3" /></Button>
          </div>
        ))}
        <div className="flex justify-between border-t pt-3 font-bold"><span>Total</span><span>{chf(total)}</span></div>
        <Button className="w-full" disabled={!lines.length || busy} onClick={order}>{busy ? "Wird bestellt…" : "Jetzt bestellen"}</Button>
      </CardContent>
    </Card>
  );
}
