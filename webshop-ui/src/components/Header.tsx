import { Link, useNavigate } from "@tanstack/react-router";
import { GlassWater, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";

export function Header() {
  const { token, user, logout } = useAuth();
  const { count, clear } = useCart();
  const nav = useNavigate();
  const link = "rounded-md px-3 py-2 text-sm font-medium text-muted-foreground hover:text-foreground";
  const active = { className: "rounded-md px-3 py-2 text-sm font-medium bg-secondary text-secondary-foreground" };
  return (
    <header className="sticky top-0 z-20 border-b bg-background/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4">
        <Link to={token ? "/shop" : "/"} className="flex items-center gap-2 font-bold text-lg">
          <span className="grid h-9 w-9 place-items-center rounded-full bg-primary text-primary-foreground"><GlassWater className="h-5 w-5" /></span>
          Durstlöscher
        </Link>
        {token && (
          <nav className="flex gap-1">
            <Link to="/shop" className={link} activeProps={active}>Shop {count > 0 && <span className="ml-1 rounded-full bg-accent px-2 text-xs text-accent-foreground">{count}</span>}</Link>
            <Link to="/orders" className={link} activeProps={active}>Bestellungen</Link>
            <Link to="/account" className={link} activeProps={active}>Konto</Link>
          </nav>
        )}
        <div className="ml-auto flex items-center gap-2">
          {user && <span className="hidden text-sm text-muted-foreground sm:inline">Hallo, {user.firstName}</span>}
          {token && <Button variant="outline" size="sm" onClick={() => { logout(); clear(); nav({ to: "/", replace: true }); }}><LogOut className="mr-1 h-4 w-4" />Abmelden</Button>}
        </div>
      </div>
    </header>
  );
}
