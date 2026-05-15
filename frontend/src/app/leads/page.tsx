import { AppShell } from "@/components/AppShell";
import { apiGet, apiPost } from "@/lib/api";
import { Brain, Building2, Plus, Search, Sparkles } from "lucide-react";
import Link from "next/link";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

type PageResult<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};

type Lead = {
  id: number;
  companyId: number;
  companyName: string;
  industry: string;
  region: string;
  scale: string;
  source: string;
  status: string;
  score: number;
  grade: string;
  scoreReason: string;
  latestSignal: string;
  nextFollowUpAt?: string;
  createdAt: string;
};

type SearchParams = {
  keyword?: string;
  status?: string;
};

async function createLead(formData: FormData) {
  "use server";

  await apiPost("/api/v1/leads", {
    companyName: text(formData, "companyName"),
    industry: text(formData, "industry"),
    region: text(formData, "region"),
    scale: text(formData, "scale"),
    website: text(formData, "website"),
    source: "MANUAL",
    intentSignal: text(formData, "intentSignal"),
  });
  revalidatePath("/leads");
  revalidatePath("/");
  redirect("/leads");
}

async function scoreLead(formData: FormData) {
  "use server";

  const leadId = text(formData, "leadId");
  await apiPost(`/api/v1/ai/leads/${leadId}/score`);
  revalidatePath("/leads");
  revalidatePath("/");
}

function text(formData: FormData, key: string) {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

async function loadLeads(searchParams: SearchParams) {
  const params = new URLSearchParams({
    page: "1",
    pageSize: "50",
  });
  if (searchParams.keyword) {
    params.set("keyword", searchParams.keyword);
  }
  if (searchParams.status) {
    params.set("status", searchParams.status);
  }

  try {
    return {
      data: await apiGet<PageResult<Lead>>(`/api/v1/leads?${params.toString()}`),
      offline: false,
    };
  } catch {
    return {
      data: { items: [], page: 1, pageSize: 50, total: 0 },
      offline: true,
    };
  }
}

export default async function LeadsPage({
  searchParams,
}: {
  searchParams?: Promise<SearchParams>;
}) {
  const resolvedSearchParams = (await searchParams) ?? {};
  const { data, offline } = await loadLeads(resolvedSearchParams);
  const topScoreCount = data.items.filter((lead) => lead.score >= 80).length;
  const pendingCount = data.items.filter((lead) => lead.status === "NEW").length;

  return (
    <AppShell active="线索中心">
      <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">线索中心</h1>
            <p className="mt-1 text-sm text-slate-500">管理企业画像、意图信号、评分和下一步动作。</p>
          </div>
          {offline ? (
            <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              后端未连接
            </span>
          ) : (
            <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              共 {data.total} 条线索
            </span>
          )}
        </div>
      </header>

      <div className="space-y-6 px-5 py-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-3">
          <div className="rounded border border-slate-200 bg-white p-4">
            <p className="text-sm text-slate-500">线索总数</p>
            <strong className="mt-3 block text-3xl font-semibold">{data.total}</strong>
          </div>
          <div className="rounded border border-slate-200 bg-white p-4">
            <p className="text-sm text-slate-500">高分线索</p>
            <strong className="mt-3 block text-3xl font-semibold">{topScoreCount}</strong>
          </div>
          <div className="rounded border border-slate-200 bg-white p-4">
            <p className="text-sm text-slate-500">新线索</p>
            <strong className="mt-3 block text-3xl font-semibold">{pendingCount}</strong>
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
          <div className="rounded border border-slate-200 bg-white">
            <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-3 xl:flex-row xl:items-center xl:justify-between">
              <div className="flex items-center gap-2">
                <Building2 size={18} className="text-leaf" />
                <h2 className="font-semibold">线索列表</h2>
              </div>
              <form className="flex flex-col gap-2 sm:flex-row">
                <input
                  name="keyword"
                  defaultValue={resolvedSearchParams.keyword}
                  placeholder="搜索企业/行业/地区"
                  className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
                />
                <select
                  name="status"
                  defaultValue={resolvedSearchParams.status ?? ""}
                  className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
                >
                  <option value="">全部状态</option>
                  <option value="NEW">NEW</option>
                  <option value="TOUCHING">TOUCHING</option>
                  <option value="FOLLOWING">FOLLOWING</option>
                  <option value="QUALIFIED">QUALIFIED</option>
                </select>
                <button className="inline-flex h-10 items-center justify-center gap-2 rounded bg-leaf px-4 text-sm font-medium text-white">
                  <Search size={16} />
                  筛选
                </button>
              </form>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[980px] border-collapse text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">企业</th>
                    <th className="px-4 py-3 font-medium">画像</th>
                    <th className="px-4 py-3 font-medium">评分</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">意图信号</th>
                    <th className="px-4 py-3 font-medium">动作</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((lead) => (
                    <tr key={lead.id} className="border-t border-slate-100 align-top">
                      <td className="px-4 py-4">
                        <p className="font-medium">{lead.companyName}</p>
                        <p className="mt-1 text-xs text-slate-500">{lead.source}</p>
                      </td>
                      <td className="px-4 py-4 text-slate-600">
                        <p>{lead.industry}</p>
                        <p className="mt-1 text-xs text-slate-500">
                          {lead.region} · {lead.scale}
                        </p>
                      </td>
                      <td className="px-4 py-4">
                        <span className="rounded bg-leaf px-2 py-1 text-xs font-semibold text-white">
                          {lead.grade} {lead.score}
                        </span>
                        <p className="mt-2 max-w-[220px] text-xs leading-5 text-slate-500">{lead.scoreReason}</p>
                      </td>
                      <td className="px-4 py-4 text-slate-600">{lead.status}</td>
                      <td className="max-w-[260px] px-4 py-4 leading-6 text-slate-600">{lead.latestSignal || "暂无"}</td>
                      <td className="px-4 py-4">
                        <form action={scoreLead}>
                          <input type="hidden" name="leadId" value={lead.id} />
                          <div className="flex flex-wrap gap-2">
                            <Link
                              href={`/profiles?leadId=${lead.id}`}
                              className="inline-flex h-9 items-center justify-center rounded border border-slate-200 px-3 text-sm font-medium text-leaf hover:bg-slate-50"
                            >
                              查看画像
                            </Link>
                            <button className="inline-flex h-9 items-center justify-center gap-2 rounded border border-slate-200 px-3 text-sm font-medium text-leaf hover:bg-slate-50">
                              <Brain size={15} />
                              重新评分
                            </button>
                          </div>
                        </form>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {data.items.length === 0 ? <p className="px-4 py-5 text-sm text-slate-500">暂无线索</p> : null}
            </div>
          </div>

          <form action={createLead} className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center gap-2">
              <Plus size={18} className="text-leaf" />
              <h2 className="font-semibold">新增线索</h2>
            </div>
            <div className="mt-4 space-y-3">
              <input name="companyName" required placeholder="企业名称" className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
              <input name="industry" placeholder="行业" className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
              <input name="region" placeholder="地区" className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
              <input name="scale" placeholder="规模" className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
              <input name="website" placeholder="官网" className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
              <textarea
                name="intentSignal"
                placeholder="意图信号，例如：近期招聘销售运营负责人"
                className="min-h-28 w-full rounded border border-slate-200 px-3 py-2 text-sm leading-6 outline-none focus:border-leaf"
              />
            </div>
            <button className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
              <Sparkles size={16} />
              创建并评分
            </button>
          </form>
        </section>
      </div>
    </AppShell>
  );
}
