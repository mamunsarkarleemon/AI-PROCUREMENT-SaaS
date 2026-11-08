import {
  BarChart3,
  Boxes,
  ClipboardList,
  Factory,
  FileText,
  Handshake,
  Inbox,
  LayoutDashboard,
  Mail,
  Package,
  ShieldCheck,
  ShoppingCart,
  Store,
  Tags,
  UserCircle,
  Users,
  Wallet,
  type LucideIcon,
} from 'lucide-react'

/** The backend sends icon names (from the menus table and dashboard cards); map them to components. */
const ICONS: Record<string, LucideIcon> = {
  BarChart3,
  Boxes,
  ClipboardList,
  Factory,
  FileText,
  Handshake,
  Inbox,
  LayoutDashboard,
  Mail,
  Package,
  ShieldCheck,
  ShoppingCart,
  Store,
  Tags,
  UserCircle,
  Users,
  Wallet,
}

export function Icon({ name, size = 18 }: { name: string; size?: number }) {
  const Cmp = ICONS[name] ?? LayoutDashboard
  return <Cmp size={size} aria-hidden="true" />
}
