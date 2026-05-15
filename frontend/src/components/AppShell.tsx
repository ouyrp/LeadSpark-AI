import { Bot } from "lucide-react";
import Link from "next/link";

const navItems = [
  { label: "工作台", href: "/" },
  { label: "线索中心", href: "/leads" },
  { label: "客户画像", href: "/profiles" },
  { label: "销售任务", href: "/tasks" },
  { label: "商机管理", href: "/opportunities" },
  { label: "数据分析", href: "/" },
  { label: "系统设置", href: "/" },
];

export function AppShell({
  active,
  children,
}: Readonly<{
  active: string;
  children: React.ReactNode;
}>) {
  return (
    <main className="min-h-screen bg-mist text-ink">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-200 bg-white px-5 py-6 lg:block">
        <Link href="/" className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded bg-leaf text-white">
            <Bot size={22} />
          </div>
          <div>
            <p className="text-lg font-semibold">LeadSpark AI</p>
            <p className="text-xs text-slate-500">智能主动获客</p>
          </div>
        </Link>

        <nav className="mt-10 space-y-1 text-sm">
          {navItems.map((item) => {
            const selected = item.label === active;
            return (
              <Link
                key={`${item.label}-${item.href}`}
                href={item.href}
                className={`block rounded px-3 py-2 ${
                  selected ? "bg-leaf text-white" : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      </aside>

      <section className="lg:pl-64">{children}</section>
    </main>
  );
}
