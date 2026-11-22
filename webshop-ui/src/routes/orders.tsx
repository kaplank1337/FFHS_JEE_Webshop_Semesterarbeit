import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { RequireAuth } from "@/components/RequireAuth";
import { api, chf, orderStatusWebSocketUrl, STATUS_LABELS, type Order, type OrderStatus } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/orders")({
  head: () => ({ meta: [{ title: "Meine Bestellungen – Durstlöscher" }, { name: "description", content: "Bestellungen ansehen, Status ändern oder löschen." }, { property: "og:title", content: "Meine Bestellungen – Durstlöscher" }, { property: "og:description", content: "Bestellungen ansehen, Status ändern oder löschen." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: () => <RequireAuth><Orders /></RequireAuth>,
});

function Orders() {
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const { data, isLoading, error } = useQuery({ queryKey: ["orders", token], queryFn: () => api.orders(token!) });

  useEffect(() => {
    if (!token) return;

    let stopped = false;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let socket: WebSocket | undefined;

    const connect = () => {
      if (stopped) return;
      socket = new WebSocket(orderStatusWebSocketUrl());
      socket.onopen = () => socket?.send(JSON.stringify({ token }));
      socket.onmessage = (message) => {
        try {
          const event = JSON.parse(message.data) as { type?: string; orderId?: string; status?: OrderStatus };
          if (event.type !== "ORDER_STATUS_CHANGED" || !event.orderId || !event.status) return;
          queryClient.setQueryData<Order[]>(["orders", token], (current) =>
            current?.map((order) => order.id === event.orderId ? { ...order, status: event.status! } : order),
          );
        } catch {
          // Ignore malformed or non-JSON frames; the initial REST query remains authoritative.
        }
      };
      socket.onclose = (event) => {
        if (!stopped && event.code !== 1008) reconnectTimer = setTimeout(connect, 2_000);
      };
      socket.onerror = () => socket?.close();
    };

    connect();
    return () => {
      stopped = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      socket?.close();
    };
  }, [token, queryClient]);

  const orders = [...(data ?? [])].sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""));
  return (
    <div className="space-y-5">
      <h1 className="text-3xl font-bold">Meine Bestellungen</h1>
      {error && <Alert variant="destructive"><AlertDescription>{(error as Error).message}</AlertDescription></Alert>}
      {isLoading && <p className="text-muted-foreground">Lädt…</p>}
      {!isLoading && !error && orders.length === 0 && (
        <Card><CardContent className="py-10 text-center text-muted-foreground">Noch keine Bestellungen. <Link to="/shop" className="font-medium text-primary underline">Zum Shop</Link></CardContent></Card>
      )}
      <div className="space-y-4">{orders.map((o) => <OrderCard key={o.id} o={o} />)}</div>
    </div>
  );
}

function OrderCard({ o }: { o: Order }) {
  const { token } = useAuth();
  const qc = useQueryClient();
  const delivered = o.status === "DELIVERED";
  const refresh = () => qc.invalidateQueries({ queryKey: ["orders"] });
  const del = useMutation({ mutationFn: () => api.deleteOrder(token!, o.id), onSuccess: () => { toast.success("Bestellung gelöscht"); refresh(); }, onError: (e) => toast.error(e.message) });
  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-center gap-3 space-y-0">
        <div className="flex-1">
          <CardTitle className="text-base">Bestellung #{o.id.slice(0, 8)}</CardTitle>
          <p className="text-sm text-muted-foreground">{o.createdAt ? new Date(o.createdAt).toLocaleString("de-CH") : ""}</p>
        </div>
        <Badge variant={o.status === "CANCELLED" ? "destructive" : "secondary"}>{STATUS_LABELS[o.status] ?? o.status}</Badge>
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="outline"
              size="icon"
              disabled={delivered}
              title={delivered ? "Gelieferte Bestellungen können nicht gelöscht werden" : "Bestellung löschen"}
              aria-label={delivered ? "Gelieferte Bestellung kann nicht gelöscht werden" : "Bestellung löschen"}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader><AlertDialogTitle>Bestellung löschen?</AlertDialogTitle><AlertDialogDescription>Das kann nicht rückgängig gemacht werden.</AlertDialogDescription></AlertDialogHeader>
            <AlertDialogFooter><AlertDialogCancel>Abbrechen</AlertDialogCancel><AlertDialogAction onClick={() => del.mutate()}>Löschen</AlertDialogAction></AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </CardHeader>
      <CardContent>
        <ul className="divide-y text-sm">
          {o.items?.map((it, i) => (
            <li key={it.id ?? i} className="flex justify-between py-2">
              <span>{it.quantity} × {it.productName ?? it.productId}</span>
              <span className="text-muted-foreground">{chf(it.totalPrice ?? it.lineTotal ?? (it.unitPrice ?? 0) * it.quantity)}</span>
            </li>
          ))}
        </ul>
        <div className="mt-2 flex justify-between border-t pt-2 font-bold"><span>Total</span><span>{chf(o.totalPrice)}</span></div>
      </CardContent>
    </Card>
  );
}
