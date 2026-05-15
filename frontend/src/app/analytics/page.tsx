import { AppShell } from "@/components/AppShell";
import { apiGet } from "@/lib/api";
import { BarChart3, CircleDollarSign, ClipboardList, RadioTower, Sparkles, TrendingUp } from "lucide-react";

type WorkbenchAnalytics = {
  todayTasks: number;
  overdueTasks: number;
  newHighScoreLeads: number;
  weeklyTouches: number;
  monthlyDealAmount: number | string;
  scoreBuckets: Array<{ grade: string; count: number }>;
  sourceStats: Array<{ source: string; count: number; avgScore: number }>;
  statusStats: Array<{ status: string; count: number }>;
  taskStats: Array<{ status: string; count: number }>;
  opportunityFunnel: Array<{
    stage: string;
    status: string;
    count: number;
    amount: number | string;
    avgProbability?: number;
  }>;
  leadTrend: Array<{ date: string; count: number }>;
};

async function loadAnalytics() {
  try {
    return {
      data: await apiGet<WorkbenchAnalytics>("/api/v1/analytics/workbench"),
      offline: false,
    };
  } catch {
    return {
      data: {
        todayTasks: 0,
        overdueTasks: 0,
        newHighScoreLeads: 0,
        weeklyTouches: 0,
        monthlyDealAmount: 0,
        scoreBuckets: [],
        sourceStats: [],
        statusStats: [],
        taskStats: [],
        opportunityFunnel: [],
        leadTrend: [],
      },
      offline: true,
    };
  }
}

function money(value: number | string) {
  return `¥${Number(value ?? 0).toLocaleString("zh-CN")}`;
}

function total(items: Array<{ count: number }>) {
  return items.reduce((sum, item) => sum + Number(item.count ?? 0), 0);
}

export default async function AnalyticsPage() {
  const { data, offline } = await loadAnalytics();
  const leadTotal = total(data.statusStats);
  const taskTotal = total(data.taskStats);
  const maxTrend = Math.max(...data.leadTrend.map((item) => Number(item.count)), 1);
  const maxSource = Math.max(...data.sourceStats.map((item) => Number(item.count)), 1);
  const maxScoreBucket = Math.max(...data.scoreBuckets.map((item) => Number(item.count)), 1);

  return (
    <AppShell active="数据分析">
      <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">数据分析</h1>
            <p className="mt-1 text-sm text-slate-500">跟踪线索质量、来源效率、任务负荷和商机漏斗。</p>
          </div>
          {offline ? (
            <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              后端未连接
            </span>
          ) : (
            <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              分析数据已同步
            </span>
          )}
        </div>
      </header>

      <div className="space-y-6 px-5 py-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="高分线索" value={data.newHighScoreLeads} note="80 分以上" icon={<Sparkles size={20} />} />
          <MetricCard label="本周触达" value={data.weeklyTouches} note="近 7 天" icon={<RadioTower size={20} />} />
          <MetricCard label="今日待办" value={data.todayTasks} note={`${data.overdueTasks} 个逾期`} icon={<ClipboardList size={20} />} />
          <MetricCard label="打开商机" value={money(data.monthlyDealAmount)} note="进行中金额" icon={<CircleDollarSign size={20} />} />
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
          <Panel title="线索增长趋势" icon={<TrendingUp size={18} className="text-leaf" />}>
            <div className="flex h-56 items-end gap-2 border-b border-slate-200 px-1 pb-3">
              {data.leadTrend.map((item) => (
                <div key={item.date} className="flex h-full flex-1 flex-col justify-end gap-2">
                  <div
                    className="min-h-2 rounded-t bg-leaf"
                    style={{ height: `${Math.max((Number(item.count) / maxTrend) * 100, 4)}%` }}
                    title={`${item.date}: ${item.count}`}
                  />
                  <span className="truncate text-center text-xs text-slate-500">{item.date.slice(5)}</span>
                </div>
              ))}
              {data.leadTrend.length === 0 ? <p className="self-center text-sm text-slate-500">暂无趋势数据</p> : null}
            </div>
          </Panel>

          <Panel title="评分分布" icon={<BarChart3 size={18} className="text-leaf" />}>
            <div className="space-y-3">
              {data.scoreBuckets.map((item) => (
                <ProgressRow key={item.grade} label={`${item.grade} 级`} value={item.count} max={maxScoreBucket} />
              ))}
              {data.scoreBuckets.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>
        </section>

        <section className="grid gap-6 xl:grid-cols-2">
          <Panel title="来源效率" icon={<RadioTower size={18} className="text-leaf" />}>
            <div className="space-y-3">
              {data.sourceStats.map((item) => (
                <div key={item.source} className="rounded border border-slate-200 p-3">
                  <div className="flex items-center justify-between gap-3 text-sm">
                    <p className="font-medium">{item.source}</p>
                    <span className="text-slate-500">{item.count} 条 · 均分 {Number(item.avgScore ?? 0).toFixed(1)}</span>
                  </div>
                  <div className="mt-3 h-2 rounded bg-slate-100">
                    <div className="h-2 rounded bg-leaf" style={{ width: `${Math.max((item.count / maxSource) * 100, 4)}%` }} />
                  </div>
                </div>
              ))}
              {data.sourceStats.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>

          <Panel title="线索状态" icon={<Sparkles size={18} className="text-leaf" />}>
            <div className="grid gap-3 sm:grid-cols-2">
              {data.statusStats.map((item) => (
                <StatusTile key={item.status} label={item.status} value={item.count} total={leadTotal} />
              ))}
              {data.statusStats.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>

          <Panel title="任务负荷" icon={<ClipboardList size={18} className="text-leaf" />}>
            <div className="grid gap-3 sm:grid-cols-2">
              {data.taskStats.map((item) => (
                <StatusTile key={item.status} label={item.status} value={item.count} total={taskTotal} />
              ))}
              {data.taskStats.length === 0 ? <EmptyText /> : null}
            </div>
          </Panel>

          <Panel title="商机漏斗" icon={<CircleDollarSign size={18} className="text-leaf" />}>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] border-collapse text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-3 py-3 font-medium">阶段</th>
                    <th className="px-3 py-3 font-medium">状态</th>
                    <th className="px-3 py-3 font-medium">数量</th>
                    <th className="px-3 py-3 font-medium">金额</th>
                    <th className="px-3 py-3 font-medium">平均概率</th>
                  </tr>
                </thead>
                <tbody>
                  {data.opportunityFunnel.map((item) => (
                    <tr key={`${item.stage}-${item.status}`} className="border-t border-slate-100">
                      <td className="px-3 py-3 font-medium">{item.stage}</td>
                      <td className="px-3 py-3 text-slate-600">{item.status}</td>
                      <td className="px-3 py-3 text-slate-600">{item.count}</td>
                      <td className="px-3 py-3 text-slate-600">{money(item.amount)}</td>
                      <td className="px-3 py-3 text-slate-600">{Number(item.avgProbability ?? 0).toFixed(1)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {data.opportunityFunnel.length === 0 ? <p className="px-3 py-4 text-sm text-slate-500">暂无商机数据</p> : null}
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
      <div className="p-4">{children}</div>
    </section>
  );
}

function ProgressRow({ label, value, max }: Readonly<{ label: string; value: number; max: number }>) {
  return (
    <div>
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium">{label}</span>
        <span className="text-slate-500">{value}</span>
      </div>
      <div className="mt-2 h-2 rounded bg-slate-100">
        <div className="h-2 rounded bg-coral" style={{ width: `${Math.max((value / max) * 100, 4)}%` }} />
      </div>
    </div>
  );
}

function StatusTile({ label, value, total: itemTotal }: Readonly<{ label: string; value: number; total: number }>) {
  const percent = itemTotal > 0 ? Math.round((value / itemTotal) * 100) : 0;
  return (
    <div className="rounded border border-slate-200 p-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-medium">{label}</p>
        <span className="text-sm text-leaf">{percent}%</span>
      </div>
      <strong className="mt-3 block text-2xl font-semibold">{value}</strong>
    </div>
  );
}

function EmptyText() {
  return <p className="text-sm text-slate-500">暂无数据</p>;
}
