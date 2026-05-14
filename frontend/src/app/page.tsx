import {
  ArrowUpRight,
  BarChart3,
  Building2,
  CalendarClock,
  CheckCircle2,
  PhoneCall,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { apiGet } from "@/lib/api";
import { AppShell } from "@/components/AppShell";
import Link from "next/link";

type PageResult<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};

type Lead = {
  id: number;
  companyName: string;
  industry: string;
  region: string;
  status: string;
  score: number;
  grade: string;
  latestSignal: string;
  nextFollowUpAt?: string;
};

type Task = {
  id: number;
  leadId: number;
  companyName: string;
  taskType: string;
  status: string;
  title: string;
  dueAt: string;
};

type Workbench = {
  todayTasks: number;
  overdueTasks: number;
  newHighScoreLeads: number;
  weeklyTouches: number;
  monthlyDealAmount: number | string;
  sourceStats: Array<{ source: string; count: number; avgScore: number }>;
};

type ImportTask = {
  taskId: number;
  taskType: string;
  sourceType: string;
  status: string;
  totalRows: number;
  successRows: number;
  failedRows: number;
  duplicateRows: number;
  createdAt: string;
};

async function loadWorkbench() {
  try {
    const [workbench, leads, tasks, importTasks] = await Promise.all([
      apiGet<Workbench>("/api/v1/analytics/workbench"),
      apiGet<PageResult<Lead>>("/api/v1/leads?page=1&pageSize=6"),
      apiGet<Task[]>("/api/v1/tasks?status=PENDING&limit=6"),
      apiGet<ImportTask[]>("/api/v1/import-tasks"),
    ]);
    return { workbench, leads: leads.items, tasks, importTasks, offline: false };
  } catch {
    return {
      workbench: {
        todayTasks: 0,
        overdueTasks: 0,
        newHighScoreLeads: 0,
        weeklyTouches: 0,
        monthlyDealAmount: 0,
        sourceStats: [],
      },
      leads: [],
      tasks: [],
      importTasks: [],
      offline: true,
    };
  }
}

export default async function Home() {
  const { workbench, leads, tasks, importTasks, offline } = await loadWorkbench();
  const metrics = [
    { label: "今日待跟进", value: workbench.todayTasks, change: `${workbench.overdueTasks} 个逾期`, icon: CalendarClock },
    { label: "高分线索", value: workbench.newHighScoreLeads, change: "80 分以上", icon: Sparkles },
    { label: "本周触达", value: workbench.weeklyTouches, change: "近 7 天", icon: PhoneCall },
    { label: "打开商机", value: `¥${Number(workbench.monthlyDealAmount).toLocaleString("zh-CN")}`, change: "进行中", icon: BarChart3 },
  ];

  return (
    <AppShell active="工作台">
        <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-2xl font-semibold tracking-normal">销售工作台</h1>
              <p className="mt-1 text-sm text-slate-500">从企业数据、意图信号、AI 评分到跟进任务的真实链路。</p>
            </div>
            <div className="flex items-center gap-2">
              {offline ? (
                <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
                  后端未连接
                </span>
              ) : (
                <span className="inline-flex items-center gap-2 rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
                  <CheckCircle2 size={16} />
                  数据已同步
                </span>
              )}
              <button className="inline-flex h-10 items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
                <Sparkles size={16} />
                生成今日策略
              </button>
            </div>
          </div>
        </header>

        <div className="space-y-6 px-5 py-6 lg:px-8">
          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {metrics.map((metric) => {
              const Icon = metric.icon;
              return (
                <div key={metric.label} className="rounded border border-slate-200 bg-white p-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-slate-500">{metric.label}</span>
                    <Icon className="text-leaf" size={20} />
                  </div>
                  <div className="mt-4 flex items-end justify-between gap-3">
                    <strong className="text-3xl font-semibold">{metric.value}</strong>
                    <span className="text-sm font-medium text-leaf">{metric.change}</span>
                  </div>
                </div>
              );
            })}
          </section>

          <section className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
            <div className="rounded border border-slate-200 bg-white">
              <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
                <div className="flex items-center gap-2">
                  <Building2 size={18} className="text-leaf" />
                  <h2 className="font-semibold">高优先级线索</h2>
                </div>
                <Link href="/leads" className="inline-flex items-center gap-1 text-sm font-medium text-leaf">
                  查看全部
                  <ArrowUpRight size={16} />
                </Link>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] border-collapse text-sm">
                  <thead className="bg-slate-50 text-left text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">企业</th>
                      <th className="px-4 py-3 font-medium">行业</th>
                      <th className="px-4 py-3 font-medium">地区</th>
                      <th className="px-4 py-3 font-medium">评分</th>
                      <th className="px-4 py-3 font-medium">状态</th>
                      <th className="px-4 py-3 font-medium">意图信号</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leads.map((lead) => (
                      <tr key={lead.id} className="border-t border-slate-100">
                        <td className="px-4 py-4 font-medium">{lead.companyName}</td>
                        <td className="px-4 py-4 text-slate-600">{lead.industry}</td>
                        <td className="px-4 py-4 text-slate-600">{lead.region}</td>
                        <td className="px-4 py-4">
                          <span className="rounded bg-leaf px-2 py-1 text-xs font-semibold text-white">
                            {lead.grade} {lead.score}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-slate-600">{lead.status}</td>
                        <td className="max-w-[260px] px-4 py-4 text-slate-600">{lead.latestSignal || "暂无"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="rounded border border-slate-200 bg-white p-4">
              <div className="flex items-center gap-2">
                <ShieldCheck size={18} className="text-leaf" />
                <Link href="/tasks" className="font-semibold hover:text-leaf">
                  待跟进任务
                </Link>
              </div>
              <div className="mt-4 space-y-3">
                {tasks.map((task, index) => (
                  <div key={task.id} className="rounded border border-slate-200 p-3">
                    <div className="flex gap-3">
                      <span className="grid h-6 w-6 shrink-0 place-items-center rounded bg-mist text-xs font-semibold text-leaf">
                        {index + 1}
                      </span>
                      <div>
                        <p className="text-sm font-medium text-slate-800">{task.title}</p>
                        <p className="mt-1 text-xs text-slate-500">
                          {task.companyName} · {task.taskType} · {new Date(task.dueAt).toLocaleString("zh-CN")}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
                {tasks.length === 0 ? <p className="text-sm text-slate-500">暂无待办任务</p> : null}
              </div>
            </div>
          </section>

          <section className="rounded border border-slate-200 bg-white">
            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
              <div className="flex items-center gap-2">
                <BarChart3 size={18} className="text-leaf" />
                <h2 className="font-semibold">最近导入任务</h2>
              </div>
              <span className="text-sm text-slate-500">采集信号进入线索池</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] border-collapse text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">任务</th>
                    <th className="px-4 py-3 font-medium">来源</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">总数</th>
                    <th className="px-4 py-3 font-medium">成功</th>
                    <th className="px-4 py-3 font-medium">重复</th>
                    <th className="px-4 py-3 font-medium">失败</th>
                  </tr>
                </thead>
                <tbody>
                  {importTasks.slice(0, 5).map((task) => (
                    <tr key={task.taskId} className="border-t border-slate-100">
                      <td className="px-4 py-4 font-medium">{task.taskId}</td>
                      <td className="px-4 py-4 text-slate-600">{task.sourceType}</td>
                      <td className="px-4 py-4">
                        <span className="rounded bg-mist px-2 py-1 text-xs font-semibold text-leaf">
                          {task.status}
                        </span>
                      </td>
                      <td className="px-4 py-4 text-slate-600">{task.totalRows}</td>
                      <td className="px-4 py-4 text-slate-600">{task.successRows}</td>
                      <td className="px-4 py-4 text-slate-600">{task.duplicateRows}</td>
                      <td className="px-4 py-4 text-slate-600">{task.failedRows}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {importTasks.length === 0 ? <p className="px-4 py-5 text-sm text-slate-500">暂无导入任务</p> : null}
            </div>
          </section>
        </div>
    </AppShell>
  );
}
