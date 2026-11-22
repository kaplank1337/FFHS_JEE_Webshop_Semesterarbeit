import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { RequireAuth } from "@/components/RequireAuth";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";

export const Route = createFileRoute("/account")({
  head: () => ({ meta: [{ title: "Mein Konto – Durstlöscher" }, { name: "description", content: "Profil ansehen und Konto löschen." }, { property: "og:title", content: "Mein Konto – Durstlöscher" }, { property: "og:description", content: "Profil ansehen und Konto löschen." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: () => <RequireAuth><Account /></RequireAuth>,
});

function Account() {
  const { user, token, logout } = useAuth();
  const { clear } = useCart();
  const nav = useNavigate();
  const [busy, setBusy] = useState(false);
  async function remove() {
    setBusy(true);
    try { await api.deleteMe(token!); clear(); logout(); toast.success("Konto gelöscht"); nav({ to: "/", replace: true }); }
    catch (e) { toast.error((e as Error).message); setBusy(false); }
  }
  const rows: [string, string | undefined][] = [["Username", user?.username], ["E-Mail", user?.email], ["Name", user ? `${user.firstName} ${user.lastName}` : undefined], ["Dabei seit", user?.createdAt ? new Date(user.createdAt).toLocaleDateString("de-CH") : undefined]];
  return (
    <div className="mx-auto max-w-xl space-y-6">
      <h1 className="text-3xl font-bold">Mein Konto</h1>
      <Card>
        <CardHeader><CardTitle>Profil</CardTitle></CardHeader>
        <CardContent><dl className="divide-y text-sm">{rows.map(([k, v]) => <div key={k} className="flex justify-between py-2"><dt className="text-muted-foreground">{k}</dt><dd className="font-medium">{v ?? "–"}</dd></div>)}</dl></CardContent>
      </Card>
      <Card className="border-destructive/40">
        <CardHeader><CardTitle className="text-destructive">Konto löschen</CardTitle><CardDescription>Dein Konto wird dauerhaft entfernt und du wirst abgemeldet.</CardDescription></CardHeader>
        <CardContent>
          <AlertDialog>
            <AlertDialogTrigger asChild><Button variant="destructive" disabled={busy}>Konto löschen</Button></AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader><AlertDialogTitle>Wirklich löschen?</AlertDialogTitle><AlertDialogDescription>Das kann nicht rückgängig gemacht werden.</AlertDialogDescription></AlertDialogHeader>
              <AlertDialogFooter><AlertDialogCancel>Abbrechen</AlertDialogCancel><AlertDialogAction onClick={remove}>Ja, löschen</AlertDialogAction></AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </CardContent>
      </Card>
    </div>
  );
}
