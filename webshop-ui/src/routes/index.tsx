import { createFileRoute, Navigate, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/")({
  head: () => ({ meta: [{ title: "Anmelden – Durstlöscher" }, { name: "description", content: "Melde dich an oder erstelle ein Konto, um Getränke zu bestellen." }, { property: "og:title", content: "Anmelden – Durstlöscher" }, { property: "og:description", content: "Melde dich an oder erstelle ein Konto, um Getränke zu bestellen." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: AuthPage,
});

function AuthPage() {
  const { token } = useAuth();
  const [tab, setTab] = useState("login");
  const [prefill, setPrefill] = useState("");
  if (token) return <Navigate to="/shop" replace />;
  return (
    <div className="grid items-center gap-10 py-6 md:grid-cols-2">
      <div className="space-y-4">
        <p className="inline-block rounded-full bg-accent px-3 py-1 text-sm font-medium text-accent-foreground">Frisch gekühlt</p>
        <h1 className="text-5xl font-extrabold leading-tight tracking-tight">Getränke bestellen,<br /><span className="text-primary">ohne Schleppen.</span></h1>
        <p className="max-w-md text-lg text-muted-foreground">Wasser, Saft, Softdrinks, Bier und Cold Brew – melde dich an und stelle deine Bestellung zusammen.</p>
      </div>
      <Card className="shadow-lg">
        <Tabs value={tab} onValueChange={setTab}>
          <CardHeader>
            <TabsList className="grid w-full grid-cols-2"><TabsTrigger value="login">Anmelden</TabsTrigger><TabsTrigger value="register">Registrieren</TabsTrigger></TabsList>
          </CardHeader>
          <CardContent>
            <TabsContent value="login"><LoginForm prefill={prefill} /></TabsContent>
            <TabsContent value="register"><RegisterForm onDone={(u) => { setPrefill(u); setTab("login"); }} /></TabsContent>
          </CardContent>
        </Tabs>
      </Card>
    </div>
  );
}

function LoginForm({ prefill }: { prefill: string }) {
  const { login } = useAuth();
  const nav = useNavigate();
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    try { await login(String(f.get("username")), String(f.get("password"))); toast.success("Willkommen zurück!"); nav({ to: "/shop" }); }
    catch (err) { toast.error((err as Error).message); }
    finally { setBusy(false); }
  }
  return (
    <form onSubmit={submit} className="space-y-4" key={prefill}>
      <CardTitle className="text-xl">Willkommen zurück</CardTitle>
      <div className="space-y-1"><Label htmlFor="lu">Username</Label><Input id="lu" name="username" defaultValue={prefill} required /></div>
      <div className="space-y-1"><Label htmlFor="lp">Passwort</Label><Input id="lp" name="password" type="password" required /></div>
      <Button className="w-full" disabled={busy}>{busy ? "Anmelden…" : "Anmelden"}</Button>
    </form>
  );
}

function RegisterForm({ onDone }: { onDone: (username: string) => void }) {
  const [busy, setBusy] = useState(false);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget)) as { username: string; email: string; password: string; firstName: string; lastName: string };
    setBusy(true);
    try {
      await api.register({ username: f.username, email: f.email, password: f.password, firstName: f.firstName, lastName: f.lastName });
      toast.success("Konto erstellt – bitte anmelden");
      onDone(f.username);
    } catch (err) { toast.error((err as Error).message); }
    finally { setBusy(false); }
  }
  return (
    <form onSubmit={submit} className="space-y-4">
      <div><CardTitle className="text-xl">Neues Konto</CardTitle><CardDescription>Dauert keine Minute.</CardDescription></div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1"><Label htmlFor="fn">Vorname</Label><Input id="fn" name="firstName" required /></div>
        <div className="space-y-1"><Label htmlFor="ln">Nachname</Label><Input id="ln" name="lastName" required /></div>
      </div>
      <div className="space-y-1"><Label htmlFor="ru">Username</Label><Input id="ru" name="username" minLength={3} maxLength={50} required /></div>
      <div className="space-y-1"><Label htmlFor="re">E-Mail</Label><Input id="re" name="email" type="email" required /></div>
      <div className="space-y-1"><Label htmlFor="rp">Passwort (min. 8 Zeichen)</Label><Input id="rp" name="password" type="password" minLength={8} required /></div>
      <Button className="w-full" disabled={busy}>{busy ? "Wird erstellt…" : "Konto erstellen"}</Button>
    </form>
  );
}
