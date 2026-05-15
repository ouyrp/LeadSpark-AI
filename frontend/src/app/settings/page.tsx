import { AppShell } from "@/components/AppShell";
import { apiGet, apiPost } from "@/lib/api";
import { CheckCircle2, Database, Play, RadioTower, Settings, ShieldCheck, UploadCloud } from "lucide-react";
import { revalidatePath } from "next/cache";

type SourceConfig = {
  enabled: boolean;
  configured: boolean;
  hasApiKey: boolean;
};

type CollectionConfig = {
  enabled: boolean;
  cron: string;
  tenantId: number;
  keywords: string[];
  sources: Record<string, SourceConfig>;
};

type JobList = {
  items: Array<{
    id: number;
    status: string;
    trigger_type: string;
    keyword_count: number;
    collected_count: number;
    deduped_count: number;
    error_message?: string;
    started_at: string;
    finished_at?: string;
  }>;
};

type SignalList = {
  items: Array<{
    id: number;
    keyword: string;
    company_name: string;
    source_type: string;
    source_name: string;
    title: string;
    confidence: number;
    created_at: string;
  }>;
};

type ImportTask = {
  taskId: number;
  sourceType: string;
  status: string;
  totalRows: number;
  successRows: number;
  duplicateRows: number;
  failedRows: number;
  createdAt: string;
};

async function runCollection() {
  "use server";

  await apiPost("/api/v1/competitor-collections/run");
  revalidatePath("/settings");
  revalidatePath("/analytics");
  revalidatePath("/");
}

async function importSignals() {
  "use server";

  await apiPost("/api/v1/import-tasks/competitor-signals", {
    limit: 100,
    minConfidence: 0,
  });
  revalidatePath("/settings");
  revalidatePath("/leads");
  revalidatePath("/analytics");
  revalidatePath("/");
}

async function loadSettings() {
  try {
    const [config, jobs, signals, importTasks] = await Promise.all([
      apiGet<CollectionConfig>("/api/v1/competitor-collections/config"),
      apiGet<JobList>("/api/v1/competitor-collections/jobs"),
      apiGet<SignalList>("/api/v1/competitor-collections/signals"),
      apiGet<ImportTask[]>("/api/v1/import-tasks"),
    ]);
    return { config, jobs: jobs.items, signals: signals.items, importTasks, offline: false };
  } catch {
    return {
      config: {
        enabled: false,
        cron: "",
        tenantId: 0,
        keywords: [],
        sources: {},
      },
      jobs: [],
      signals: [],
      importTasks: [],
      offline: true,
    };
  }
}

function dateTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN") : "未完成";
}

export default async function SettingsPage() {
  const { config, jobs, signals, importTasks, offline } = await loadSettings();
  const sourceItems = Object.entries(config.sources);
  const enabledSources = sourceItems.filter(([, source]) => source.enabled).length;
  const configuredSources = sourceItems.filter(([, source]) => source.configured).length;

  return (
    <AppShell active="系统设置">
      <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">系统设置</h1>
            <p className="mt-1 text-sm text-slate-500">管理采集任务、数据源状态和导入链路。</p>
          </div>
          {offline ? (
            <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              后端未连接
            </span>
          ) : (
            <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              配置已同步
            </span>
          )}
        </div>
      </header>

      <div className="space-y-6 px-5 py-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-4">
          <MetricCard label="租户" value={config.tenantId || "-"} note="当前演示租户" icon={<Settings size={20} />} />
          <MetricCard label="采集任务" value={config.enabled ? "启用" : "停用"} note={config.cron || "未配置"} icon={<RadioTower size={20} />} />
          <MetricCard label="关键词" value={config.keywords.length} note="同类产品和场景词" icon={<Database size={20} />} />
          <MetricCard label="数据源" value={`${configuredSources}/${enabledSources}`} note="已配置/已启用" icon={<ShieldCheck size={20} />} />
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.85fr_1.15fr]">
          <div className="rounded border border-slate-200 bg-white p-5">
            <div className="flex items-center gap-2">
              <RadioTower size={18} className="text-leaf" />
              <h2 className="font-semibold">采集控制</h2>
            </div>
            <div className="mt-4 grid gap-3">
              <form action={runCollection}>
                <button className="inline-flex h-10 w-full items-center justify-center gap-2 rounded bg-leaf px-4 text-sm font-medium text-white">
                  <Play size={16} />
                  立即运行采集
                </button>
              </form>
              <form action={importSignals}>
                <button className="inline-flex h-10 w-full items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
                  <UploadCloud size={16} />
                  导入未转线索信号
                </button>
              </form>
            </div>
            <div className="mt-5 rounded bg-slate-50 p-3 text-sm leading-6 text-slate-600">
              当前每日任务会在 cron 表达式指定时间执行。手动采集只触发合规 Provider，不会绕过登录、验证码或平台访问限制。
            </div>
          </div>

          <Panel title="关键词">
            <div className="flex flex-wrap gap-2">
              {config.keywords.map((keyword) => (
                <span key={keyword} className="rounded bg-mist px-3 py-2 text-sm font-medium text-leaf">
                  {keyword}
                </span>
              ))}
              {config.keywords.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>
        </section>

        <section className="grid gap-6 xl:grid-cols-2">
          <Panel title="数据源状态">
            <div className="grid gap-3 sm:grid-cols-2">
              {sourceItems.map(([name, source]) => (
                <div key={name} className="rounded border border-slate-200 p-3">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-medium">{name}</p>
                    <span className={`rounded px-2 py-1 text-xs font-semibold ${source.enabled ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                      {source.enabled ? "启用" : "停用"}
                    </span>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2 text-xs">
                    <Badge active={source.configured}>接口已配置</Badge>
                    <Badge active={source.hasApiKey}>密钥已配置</Badge>
                  </div>
                </div>
              ))}
              {sourceItems.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>

          <Panel title="最近采集任务">
            <div className="space-y-3">
              {jobs.slice(0, 5).map((job) => (
                <div key={job.id} className="rounded border border-slate-200 p-3 text-sm">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <p className="font-medium">{job.id}</p>
                    <span className="rounded bg-mist px-2 py-1 text-xs font-semibold text-leaf">{job.status}</span>
                  </div>
                  <p className="mt-2 text-slate-500">
                    {job.trigger_type} · 关键词 {job.keyword_count} · 采集 {job.collected_count} · 新增 {job.deduped_count}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">{dateTime(job.started_at)} - {dateTime(job.finished_at)}</p>
                </div>
              ))}
              {jobs.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>

          <Panel title="最近导入任务">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] border-collapse text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-3 py-3 font-medium">任务</th>
                    <th className="px-3 py-3 font-medium">来源</th>
                    <th className="px-3 py-3 font-medium">成功</th>
                    <th className="px-3 py-3 font-medium">重复</th>
                    <th className="px-3 py-3 font-medium">失败</th>
                  </tr>
                </thead>
                <tbody>
                  {importTasks.slice(0, 6).map((task) => (
                    <tr key={task.taskId} className="border-t border-slate-100">
                      <td className="px-3 py-3 font-medium">{task.taskId}</td>
                      <td className="px-3 py-3 text-slate-600">{task.sourceType}</td>
                      <td className="px-3 py-3 text-slate-600">{task.successRows}</td>
                      <td className="px-3 py-3 text-slate-600">{task.duplicateRows}</td>
                      <td className="px-3 py-3 text-slate-600">{task.failedRows}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {importTasks.length === 0 ? <p className="px-3 py-4 text-sm text-slate-500">暂无导入任务</p> : null}
            </div>
          </Panel>

          <Panel title="最近采集信号">
            <div className="space-y-3">
              {signals.slice(0, 6).map((signal) => (
                <div key={signal.id} className="rounded border border-slate-200 p-3 text-sm">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <p className="font-medium">{signal.company_name}</p>
                    <span className="text-leaf">{signal.confidence}</span>
                  </div>
                  <p className="mt-1 text-slate-500">{signal.keyword} · {signal.source_type} · {signal.source_name}</p>
                  <p className="mt-2 leading-6 text-slate-600">{signal.title}</p>
                </div>
              ))}
              {signals.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>
        </section>
      </div>
    </AppShell>
  );
}

function MetricCard({
  label,
  value,
  note,
  icon,
}: Readonly<{
  label: string;
  value: number | string;
  note: string;
  icon: React.ReactNode;
}>) {
  return (
    <div className="rounded border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">{label}</p>
        <span className="text-leaf">{icon}</span>
      </div>
      <strong className="mt-3 block text-3xl font-semibold">{value}</strong>
      <p className="mt-2 text-sm text-slate-500">{note}</p>
    </div>
  );
}

function Panel({ title, children }: Readonly<{ title: string; children: React.ReactNode }>) {
  return (
    <section className="rounded border border-slate-200 bg-white">
      <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-3">
        <CheckCircle2 size={18} className="text-leaf" />
        <h2 className="font-semibold">{title}</h2>
      </div>
      <div className="p-4">{children}</div>
    </section>
  );
}

function Badge({ active, children }: Readonly<{ active: boolean; children: React.ReactNode }>) {
  return (
    <span className={`rounded px-2 py-1 ${active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
      {children}
    </span>
  );
}

function EmptyText() {
  return <p className="text-sm text-slate-500">暂无数据</p>;
}
