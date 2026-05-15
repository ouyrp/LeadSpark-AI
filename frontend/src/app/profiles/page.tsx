import { AppShell } from "@/components/AppShell";
import { apiGet, apiPost } from "@/lib/api";
import {
  Building2,
  CalendarClock,
  CheckCircle2,
  ClipboardList,
  MessageSquareText,
  PhoneCall,
  Sparkles,
  Target,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { revalidatePath } from "next/cache";

type PageResult<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};

type LeadSummary = {
  id: number;
  companyName: string;
  industry: string;
  region: string;
  scale: string;
  source: string;
  status: string;
  score: number;
  grade: string;
  latestSignal: string;
};

type LeadDetail = {
  lead: LeadSummary & {
    companyId: number;
    website?: string;
    description?: string;
    scoreReason: string;
    lastFollowUpAt?: string;
    nextFollowUpAt?: string;
    createdAt: string;
  };
  signals: Array<{
    id: number;
    signalType: string;
    signalSource: string;
    content: string;
    signalTime?: string;
    weight: number;
  }>;
  contacts: Array<{
    id: number;
    name?: string;
    title?: string;
    department?: string;
    confidence: number;
    source?: string;
  }>;
  tasks: Array<{
    id: number;
    taskType: string;
    status: string;
    title: string;
    dueAt: string;
    result?: string;
  }>;
  followUps: Array<{
    id: number;
    channel: string;
    content: string;
    result: string;
    nextAction?: string;
    nextFollowUpAt?: string;
    createdAt: string;
  }>;
  opportunities: Array<{
    id: number;
    stage: string;
    amount?: number | string;
    probability: number;
    expectedCloseDate?: string;
    status: string;
  }>;
  recommendations: Array<{
    id: number;
    recommendationType: string;
    content: string;
    confidence?: number;
    createdAt: string;
  }>;
};

type SearchParams = {
  leadId?: string;
};

async function createFollowUp(formData: FormData) {
  "use server";

  const leadId = text(formData, "leadId");
  await apiPost(`/api/v1/leads/${leadId}/follow-ups`, {
    channel: text(formData, "channel") || "CALL",
    content: text(formData, "content"),
    result: text(formData, "result") || "INTERESTED",
    nextAction: text(formData, "nextAction"),
    nextFollowUpAt: text(formData, "nextFollowUpAt") || null,
  });
  revalidatePath("/profiles");
  revalidatePath("/leads");
  revalidatePath("/");
}

async function generatePitch(formData: FormData) {
  "use server";

  const leadId = text(formData, "leadId");
  await apiPost(`/api/v1/ai/leads/${leadId}/pitch`);
  revalidatePath("/profiles");
}

function text(formData: FormData, key: string) {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

async function loadProfiles(leadId?: string) {
  try {
    const leads = await apiGet<PageResult<LeadSummary>>("/api/v1/leads?page=1&pageSize=30");
    const selectedLeadId = leadId ?? String(leads.items[0]?.id ?? "");
    const detail = selectedLeadId ? await apiGet<LeadDetail>(`/api/v1/leads/${selectedLeadId}`) : null;
    return { leads: leads.items, detail, selectedLeadId, offline: false };
  } catch {
    return { leads: [], detail: null, selectedLeadId: "", offline: true };
  }
}

function money(value: LeadDetail["opportunities"][number]["amount"]) {
  return `¥${Number(value ?? 0).toLocaleString("zh-CN")}`;
}

function dateTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "未设置";
}

export default async function ProfilesPage({
  searchParams,
}: {
  searchParams?: Promise<SearchParams>;
}) {
  const resolvedSearchParams = (await searchParams) ?? {};
  const { leads, detail, selectedLeadId, offline } = await loadProfiles(resolvedSearchParams.leadId);
  const lead = detail?.lead;
  const openOpportunityAmount =
    detail?.opportunities
      .filter((item) => item.status === "OPEN")
      .reduce((sum, item) => sum + Number(item.amount ?? 0), 0) ?? 0;

  return (
    <AppShell active="客户画像">
      <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">客户画像</h1>
            <p className="mt-1 text-sm text-slate-500">聚合企业档案、意图信号、联系人、跟进和商机。</p>
          </div>
          {offline ? (
            <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              后端未连接
            </span>
          ) : (
            <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              可查看 {leads.length} 个客户
            </span>
          )}
        </div>
      </header>

      <div className="grid gap-6 px-5 py-6 xl:grid-cols-[320px_1fr] lg:px-8">
        <aside className="rounded border border-slate-200 bg-white">
          <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-3">
            <Building2 size={18} className="text-leaf" />
            <h2 className="font-semibold">客户列表</h2>
          </div>
          <div className="max-h-[760px] overflow-y-auto p-2">
            {leads.map((item) => {
              const selected = String(item.id) === selectedLeadId;
              return (
                <Link
                  key={item.id}
                  href={`/profiles?leadId=${item.id}`}
                  className={`block rounded px-3 py-3 text-sm ${
                    selected ? "bg-leaf text-white" : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <span className="font-medium">{item.companyName}</span>
                    <span className={selected ? "text-white" : "text-leaf"}>
                      {item.grade}
                      {item.score}
                    </span>
                  </div>
                  <p className={`mt-1 text-xs ${selected ? "text-white/80" : "text-slate-500"}`}>
                    {item.industry} · {item.region}
                  </p>
                </Link>
              );
            })}
            {leads.length === 0 ? <p className="px-3 py-4 text-sm text-slate-500">暂无客户</p> : null}
          </div>
        </aside>

        {lead ? (
          <div className="space-y-6">
            <section className="grid gap-4 md:grid-cols-4">
              <div className="rounded border border-slate-200 bg-white p-4">
                <p className="text-sm text-slate-500">画像评分</p>
                <strong className="mt-3 block text-3xl font-semibold">
                  {lead.grade} {lead.score}
                </strong>
              </div>
              <div className="rounded border border-slate-200 bg-white p-4">
                <p className="text-sm text-slate-500">意图信号</p>
                <strong className="mt-3 block text-3xl font-semibold">{detail.signals.length}</strong>
              </div>
              <div className="rounded border border-slate-200 bg-white p-4">
                <p className="text-sm text-slate-500">打开商机金额</p>
                <strong className="mt-3 block text-3xl font-semibold">{money(openOpportunityAmount)}</strong>
              </div>
              <div className="rounded border border-slate-200 bg-white p-4">
                <p className="text-sm text-slate-500">下次跟进</p>
                <strong className="mt-3 block text-base font-semibold">{dateTime(lead.nextFollowUpAt)}</strong>
              </div>
            </section>

            <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
              <div className="rounded border border-slate-200 bg-white p-5">
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <div>
                    <h2 className="text-xl font-semibold">{lead.companyName}</h2>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                      {lead.industry} · {lead.region} · {lead.scale}
                    </p>
                    <p className="mt-2 text-sm leading-6 text-slate-500">{lead.description || lead.scoreReason}</p>
                  </div>
                  <span className="rounded bg-mist px-3 py-2 text-sm font-semibold text-leaf">{lead.status}</span>
                </div>
                <div className="mt-5 grid gap-3 text-sm md:grid-cols-3">
                  <p className="rounded bg-slate-50 px-3 py-2">来源：{lead.source}</p>
                  <p className="rounded bg-slate-50 px-3 py-2">官网：{lead.website || "未录入"}</p>
                  <p className="rounded bg-slate-50 px-3 py-2">最后跟进：{dateTime(lead.lastFollowUpAt)}</p>
                </div>
              </div>

              <form action={createFollowUp} className="rounded border border-slate-200 bg-white p-5">
                <input type="hidden" name="leadId" value={lead.id} />
                <div className="flex items-center gap-2">
                  <PhoneCall size={18} className="text-leaf" />
                  <h2 className="font-semibold">记录跟进</h2>
                </div>
                <div className="mt-4 grid gap-3">
                  <select name="channel" defaultValue="CALL" className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf">
                    <option value="CALL">电话</option>
                    <option value="WECHAT">微信</option>
                    <option value="EMAIL">邮件</option>
                    <option value="MEETING">会议</option>
                  </select>
                  <textarea
                    name="content"
                    required
                    placeholder="跟进内容"
                    className="min-h-24 rounded border border-slate-200 px-3 py-2 text-sm leading-6 outline-none focus:border-leaf"
                  />
                  <select name="result" defaultValue="INTERESTED" className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf">
                    <option value="INTERESTED">有兴趣</option>
                    <option value="NO_RESPONSE">未接通</option>
                    <option value="NOT_NOW">暂不考虑</option>
                  </select>
                  <input name="nextAction" placeholder="下一步动作" className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
                  <input name="nextFollowUpAt" type="datetime-local" className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf" />
                </div>
                <button className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
                  <CheckCircle2 size={16} />
                  保存跟进
                </button>
              </form>
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <Panel title="意图信号" icon={<Target size={18} className="text-leaf" />}>
                {detail.signals.map((item) => (
                  <TimelineItem key={item.id} title={`${item.signalType} · ${item.weight}`} meta={item.signalSource}>
                    {item.content}
                  </TimelineItem>
                ))}
                {detail.signals.length === 0 ? <EmptyText /> : null}
              </Panel>

              <Panel title="联系人" icon={<UserRound size={18} className="text-leaf" />}>
                {detail.contacts.map((item) => (
                  <div key={item.id} className="rounded border border-slate-200 p-3 text-sm">
                    <div className="flex items-center justify-between gap-3">
                      <p className="font-medium">{item.name || "未知联系人"}</p>
                      <span className="text-leaf">{item.confidence}%</span>
                    </div>
                    <p className="mt-1 text-slate-500">
                      {item.department || "未知部门"} · {item.title || "未知职务"} · {item.source || "未知来源"}
                    </p>
                  </div>
                ))}
                {detail.contacts.length === 0 ? <EmptyText /> : null}
              </Panel>

              <Panel title="跟进记录" icon={<MessageSquareText size={18} className="text-leaf" />}>
                {detail.followUps.map((item) => (
                  <TimelineItem key={item.id} title={`${item.channel} · ${item.result}`} meta={dateTime(item.createdAt)}>
                    {item.content}
                    {item.nextAction ? `；下一步：${item.nextAction}` : ""}
                  </TimelineItem>
                ))}
                {detail.followUps.length === 0 ? <EmptyText /> : null}
              </Panel>

              <Panel title="销售任务" icon={<ClipboardList size={18} className="text-leaf" />}>
                {detail.tasks.map((item) => (
                  <TimelineItem key={item.id} title={`${item.taskType} · ${item.status}`} meta={dateTime(item.dueAt)}>
                    {item.title}
                  </TimelineItem>
                ))}
                {detail.tasks.length === 0 ? <EmptyText /> : null}
              </Panel>

              <Panel title="商机" icon={<CalendarClock size={18} className="text-leaf" />}>
                {detail.opportunities.map((item) => (
                  <TimelineItem key={item.id} title={`${item.stage} · ${item.status}`} meta={`${money(item.amount)} · ${item.probability}%`}>
                    预计成交：{item.expectedCloseDate || "未设置"}
                  </TimelineItem>
                ))}
                {detail.opportunities.length === 0 ? <EmptyText /> : null}
              </Panel>

              <Panel title="AI 建议" icon={<Sparkles size={18} className="text-leaf" />}>
                <form action={generatePitch}>
                  <input type="hidden" name="leadId" value={lead.id} />
                  <button className="mb-3 inline-flex h-9 items-center justify-center gap-2 rounded border border-slate-200 px-3 text-sm font-medium text-leaf hover:bg-slate-50">
                    <Sparkles size={15} />
                    生成触达话术
                  </button>
                </form>
                {detail.recommendations.map((item) => (
                  <TimelineItem key={item.id} title={item.recommendationType} meta={`${item.confidence ?? 0}%`}>
                    {item.content}
                  </TimelineItem>
                ))}
                {detail.recommendations.length === 0 ? <EmptyText /> : null}
              </Panel>
            </section>
          </div>
        ) : (
          <div className="rounded border border-slate-200 bg-white p-8 text-sm text-slate-500">暂无可查看的客户画像</div>
        )}
      </div>
    </AppShell>
  );
}

function Panel({
  title,
  icon,
  children,
}: Readonly<{
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}>) {
  return (
    <section className="rounded border border-slate-200 bg-white">
      <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-3">
        {icon}
        <h2 className="font-semibold">{title}</h2>
      </div>
      <div className="space-y-3 p-4">{children}</div>
    </section>
  );
}

function TimelineItem({
  title,
  meta,
  children,
}: Readonly<{
  title: string;
  meta: string;
  children: React.ReactNode;
}>) {
  return (
    <div className="rounded border border-slate-200 p-3 text-sm">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <p className="font-medium">{title}</p>
        <span className="text-xs text-slate-500">{meta}</span>
      </div>
      <p className="mt-2 leading-6 text-slate-600">{children}</p>
    </div>
  );
}

function EmptyText() {
  return <p className="text-sm text-slate-500">暂无记录</p>;
}
