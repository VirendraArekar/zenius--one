import {
  BarChart3,
  Blocks,
  Boxes,
  Briefcase,
  FileText,
  FolderKanban,
  HardDrive,
  Headphones,
  Landmark,
  LayoutDashboard,
  Mail,
  Megaphone,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  UserCheck,
  Users,
  Wallet,
  Workflow,
  type LucideIcon,
} from 'lucide-react'

const ICON_MAP: Record<string, LucideIcon> = {
  LayoutDashboard,
  Users,
  TrendingUp,
  Megaphone,
  Headphones,
  Landmark,
  Boxes,
  UserCheck,
  Briefcase,
  Wallet,
  FolderKanban,
  MessageSquare,
  Mail,
  HardDrive,
  FileText,
  BarChart3,
  Workflow,
  Blocks,
  Sparkles,
  ShieldCheck,
}

export function ModuleIcon({ name, className = 'h-4 w-4' }: { name: string; className?: string }) {
  const Icon = ICON_MAP[name] || LayoutDashboard
  return <Icon className={className} aria-hidden="true" />
}

