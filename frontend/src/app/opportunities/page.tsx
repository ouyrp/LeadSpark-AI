import { AppShell } from "@/components/AppShell";
import { apiGet, apiPatch, apiPost } from "@/lib/api";
import { BarChart3, CheckCircle2, CircleDollarSign, Plus, TrendingUp } from "lucide-react";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

type Opportunity = {
  id: number;
  leadId: number;
  companyId: number;
  companyName: string;
  stage: string;
  amount?: number | string;
  probability: number;
  expectedCloseDate?: string;
  status: string;
  lostReason?: string;
  createdAt: string;
};

async function createOpportunity(formData: FormData) {
  "use server";

  await apiPost("/api/v1/opportunities", {
    leadId: Number(text(formData, "leadId")),
    stage: text(formData, "stage") || "QUALIFIED",
    amount: Number(text(formData, "amount") || "0"),
    probability: Number(text(formData, "probability") || "50"),
    expectedCloseDate: text(formData, "expectedCloseDate") || null,
  });
  revalidatePath("/opportunities");
  revalidatePath("/leads");
  revalidatePath("/");
  redirect("/opportunities");
}

async function updateOpportunity(formData: FormData) {
  "use server";

  const id = text(formData, "id");
  await apiPatch(`/api/v1/opportunities/${id}`, {
    stage: text(formData, "stage"),
    probability: Number(text(formData, "probability") || "50"),
    status: text(formData, "status") || "OPEN",
    lostReason: text(formData, "lostReason") || null,
  });
  revalidatePath("/opportunities");
  revalidatePath("/leads");
  revalidatePath("/");
}

function text(formData: FormData, key: string) {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

async function loadOpportunities() {
  try {
    return {
      opportunities: await apiGet<Opportunity[]>("/api/v1/opportunities"),
      offline: false,
    };
  } catch {
    return { opportunities: [], offline: true };
  }
}

function money(value: Opportunity["amount"]) {
  return `¥${Number(value ?? 0).toLocaleString("zh-CN")}`;
}

export default async function OpportunitiesPage() {
  const { opportunities, offline } = await loadOpportunities();
  const openItems = opportunities.filter((item) => item.status === "OPEN");
  const openAmount = openItems.reduce((sum, item) => sum + Number(item.amount ?? 0), 0);
  const weightedAmount = openItems.reduce((sum, item) => sum + Number(item.amount ?? 0) * (item.probability / 100), 0);

  return (
    <AppShell active="商机管理">
      <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">商机管理</h1>
            <p className="mt-1 text-sm text-slate-500">把高意向线索推进为商机，跟踪金额、阶段和赢单概率。</p>
          </div>
          {offline ? (
            <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              后端未连接
            </span>
          ) : (
            <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              共 {opportunities.length} 个商机
            </span>
          )}
        </div>
      </header>

      <div className="space-y-6 px-5 py-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-3">
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">打开商机</p>
              <CircleDollarSign size={20} className="text-leaf" />
            </div>
            <strong className="mt-3 block text-3xl font-semibold">{openItems.length}</strong>
          </div>
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">打开金额</p>
              <BarChart3 size={20} className="text-leaf" />
            </div>
            <strong className="mt-3 block text-3xl font-semibold">{money(openAmount)}</strong>
          </div>
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">加权金额</p>
              <TrendingUp size={20} className="text-leaf" />
            </div>
            <strong className="mt-3 block text-3xl font-semibold">{money(Math.round(weightedAmount))}</strong>
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
          <div className="rounded border border-slate-200 bg-white">
            <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-3">
              <CircleDollarSign size={18} className="text-leaf" />
              <h2 className="font-semibold">商机列表</h2>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[1020px] border-collapse text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">企业</th>
                    <th className="px-4 py-3 font-medium">阶段</th>
                    <th className="px-4 py-3 font-medium">金额</th>
                    <th className="px-4 py-3 font-medium">概率</th>
                    <th className="px-4 py-3 font-medium">预计成交</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">推进</th>
                  </tr>
                </thead>
                <tbody>
                  {opportunities.map((item) => (
                    <tr key={item.id} className="border-t border-slate-100 align-top">
                      <td className="px-4 py-4">
                        <p className="font-medium">{item.companyName}</p>
                        <p className="mt-1 text-xs text-slate-500">线索 {item.leadId}</p>
                      </td>
                      <td className="px-4 py-4 text-slate-600">{item.stage}</td>
                      <td className="px-4 py-4 font-medium">{money(item.amount)}</td>
                      <td className="px-4 py-4 text-slate-600">{item.probability}%</td>
                      <td className="px-4 py-4 text-slate-600">{item.expectedCloseDate || "未设置"}</td>
                      <td className="px-4 py-4">
                        <span className="rounded bg-mist px-2 py-1 text-xs font-semibold text-leaf">{item.status}</span>
                      </td>
                      <td className="px-4 py-4">
                        <form action={updateOpportunity} className="grid min-w-[280px] gap-2 sm:grid-cols-[1fr_88px_80px_auto]">
                          <input type="hidden" name="id" value={item.id} />
                          <select
                            name="stage"
                            defaultValue={item.stage}
                            className="h-9 rounded border border-slate-200 px-2 text-sm outline-none focus:border-leaf"
                          >
                            <option value="QUALIFIED">QUALIFIED</option>
                            <option value="DEMO">DEMO</option>
                            <option value="PROPOSAL">PROPOSAL</option>
                            <option value="NEGOTIATION">NEGOTIATION</option>
                            <option value="CLOSED">CLOSED</option>
                          </select>
                          <input
                            name="probability"
                            defaultValue={item.probability}
                            inputMode="numeric"
                            className="h-9 rounded border border-slate-200 px-2 text-sm outline-none focus:border-leaf"
                          />
                          <select
                            name="status"
                            defaultValue={item.status}
                            className="h-9 rounded border border-slate-200 px-2 text-sm outline-none focus:border-leaf"
                          >
                            <option value="OPEN">OPEN</option>
                            <option value="WON">WON</option>
                            <option value="LOST">LOST</option>
                          </select>
                          <button className="inline-flex h-9 items-center justify-center gap-1 rounded border border-slate-200 px-3 text-sm font-medium text-leaf hover:bg-slate-50">
                            <CheckCircle2 size={15} />
                            更新
                          </button>
                        </form>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {opportunities.length === 0 ? <p className="px-4 py-5 text-sm text-slate-500">暂无商机</p> : null}
            </div>
          </div>

          <form action={createOpportunity} className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center gap-2">
              <Plus size={18} className="text-leaf" />
              <h2 className="font-semibold">新建商机</h2>
            </div>
            <div className="mt-4 space-y-3">
              <input
                name="leadId"
                required
                inputMode="numeric"
                placeholder="线索 ID"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              />
              <select
                name="stage"
                defaultValue="QUALIFIED"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              >
                <option value="QUALIFIED">QUALIFIED</option>
                <option value="DEMO">DEMO</option>
                <option value="PROPOSAL">PROPOSAL</option>
                <option value="NEGOTIATION">NEGOTIATION</option>
              </select>
              <input
                name="amount"
                inputMode="decimal"
                placeholder="预计金额"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              />
              <input
                name="probability"
                inputMode="numeric"
                placeholder="赢单概率"
                defaultValue="50"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              />
              <input
                name="expectedCloseDate"
                type="date"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              />
            </div>
            <button className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
              <Plus size={16} />
              创建商机
            </button>
          </form>
        </section>
      </div>
    </AppShell>
  );
}
