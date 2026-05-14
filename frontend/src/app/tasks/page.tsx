import { AppShell } from "@/components/AppShell";
import { apiGet, apiPatch, apiPost } from "@/lib/api";
import { CheckCircle2, Clock3, ListChecks, Plus, Search } from "lucide-react";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

type Task = {
  id: number;
  leadId: number;
  companyId: number;
  companyName: string;
  taskType: string;
  status: string;
  title: string;
  dueAt: string;
  completedAt?: string;
  result?: string;
};

type SearchParams = {
  status?: string;
};

async function createTask(formData: FormData) {
  "use server";

  await apiPost("/api/v1/tasks", {
    leadId: Number(text(formData, "leadId")),
    taskType: text(formData, "taskType"),
    title: text(formData, "title"),
    dueAt: text(formData, "dueAt") || null,
  });
  revalidatePath("/tasks");
  revalidatePath("/");
  redirect("/tasks");
}

async function completeTask(formData: FormData) {
  "use server";

  const taskId = text(formData, "taskId");
  await apiPatch(`/api/v1/tasks/${taskId}/complete`, {
    result: text(formData, "result") || "DONE",
  });
  revalidatePath("/tasks");
  revalidatePath("/");
}

function text(formData: FormData, key: string) {
  const value = formData.get(key);
  return typeof value === "string" ? value.trim() : "";
}

async function loadTasks(searchParams: SearchParams) {
  const params = new URLSearchParams({
    limit: "80",
  });
  if (searchParams.status) {
    params.set("status", searchParams.status);
  }
  try {
    return {
      tasks: await apiGet<Task[]>(`/api/v1/tasks?${params.toString()}`),
      offline: false,
    };
  } catch {
    return { tasks: [], offline: true };
  }
}

export default async function TasksPage({
  searchParams,
}: {
  searchParams?: Promise<SearchParams>;
}) {
  const resolvedSearchParams = (await searchParams) ?? {};
  const { tasks, offline } = await loadTasks(resolvedSearchParams);
  const pendingCount = tasks.filter((task) => task.status === "PENDING").length;
  const doneCount = tasks.filter((task) => task.status === "DONE").length;
  const overdueCount = tasks.filter((task) => task.status === "PENDING" && new Date(task.dueAt).getTime() < Date.now()).length;

  return (
    <AppShell active="销售任务">
      <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">任务中心</h1>
            <p className="mt-1 text-sm text-slate-500">安排首次触达、二次跟进和销售动作，形成稳定执行节奏。</p>
          </div>
          {offline ? (
            <span className="rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              后端未连接
            </span>
          ) : (
            <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              共 {tasks.length} 个任务
            </span>
          )}
        </div>
      </header>

      <div className="space-y-6 px-5 py-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-3">
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">待处理</p>
              <Clock3 size={20} className="text-leaf" />
            </div>
            <strong className="mt-3 block text-3xl font-semibold">{pendingCount}</strong>
          </div>
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">已完成</p>
              <CheckCircle2 size={20} className="text-leaf" />
            </div>
            <strong className="mt-3 block text-3xl font-semibold">{doneCount}</strong>
          </div>
          <div className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-500">已逾期</p>
              <ListChecks size={20} className="text-leaf" />
            </div>
            <strong className="mt-3 block text-3xl font-semibold">{overdueCount}</strong>
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
          <div className="rounded border border-slate-200 bg-white">
            <div className="flex flex-col gap-3 border-b border-slate-200 px-4 py-3 md:flex-row md:items-center md:justify-between">
              <div className="flex items-center gap-2">
                <ListChecks size={18} className="text-leaf" />
                <h2 className="font-semibold">任务列表</h2>
              </div>
              <form className="flex flex-col gap-2 sm:flex-row">
                <select
                  name="status"
                  defaultValue={resolvedSearchParams.status ?? ""}
                  className="h-10 rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
                >
                  <option value="">全部状态</option>
                  <option value="PENDING">PENDING</option>
                  <option value="DONE">DONE</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>
                <button className="inline-flex h-10 items-center justify-center gap-2 rounded bg-leaf px-4 text-sm font-medium text-white">
                  <Search size={16} />
                  筛选
                </button>
              </form>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[920px] border-collapse text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">任务</th>
                    <th className="px-4 py-3 font-medium">企业</th>
                    <th className="px-4 py-3 font-medium">类型</th>
                    <th className="px-4 py-3 font-medium">状态</th>
                    <th className="px-4 py-3 font-medium">到期时间</th>
                    <th className="px-4 py-3 font-medium">动作</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.map((task) => (
                    <tr key={task.id} className="border-t border-slate-100 align-top">
                      <td className="px-4 py-4">
                        <p className="font-medium">{task.title}</p>
                        {task.result ? <p className="mt-1 text-xs text-slate-500">结果：{task.result}</p> : null}
                      </td>
                      <td className="px-4 py-4 text-slate-600">
                        <p>{task.companyName}</p>
                        <p className="mt-1 text-xs text-slate-500">线索 {task.leadId}</p>
                      </td>
                      <td className="px-4 py-4 text-slate-600">{task.taskType}</td>
                      <td className="px-4 py-4">
                        <span className="rounded bg-mist px-2 py-1 text-xs font-semibold text-leaf">{task.status}</span>
                      </td>
                      <td className="px-4 py-4 text-slate-600">{new Date(task.dueAt).toLocaleString("zh-CN")}</td>
                      <td className="px-4 py-4">
                        {task.status === "PENDING" ? (
                          <form action={completeTask} className="flex gap-2">
                            <input type="hidden" name="taskId" value={task.id} />
                            <input type="hidden" name="result" value="DONE" />
                            <button className="inline-flex h-9 items-center justify-center gap-2 rounded border border-slate-200 px-3 text-sm font-medium text-leaf hover:bg-slate-50">
                              <CheckCircle2 size={15} />
                              完成
                            </button>
                          </form>
                        ) : (
                          <span className="text-sm text-slate-400">已处理</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {tasks.length === 0 ? <p className="px-4 py-5 text-sm text-slate-500">暂无任务</p> : null}
            </div>
          </div>

          <form action={createTask} className="rounded border border-slate-200 bg-white p-4">
            <div className="flex items-center gap-2">
              <Plus size={18} className="text-leaf" />
              <h2 className="font-semibold">新增任务</h2>
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
                name="taskType"
                defaultValue="CALL"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              >
                <option value="CALL">电话</option>
                <option value="WECHAT">微信</option>
                <option value="EMAIL">邮件</option>
                <option value="MEETING">会议</option>
              </select>
              <input
                name="title"
                required
                placeholder="任务标题"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              />
              <input
                name="dueAt"
                type="datetime-local"
                className="h-10 w-full rounded border border-slate-200 px-3 text-sm outline-none focus:border-leaf"
              />
            </div>
            <button className="mt-4 inline-flex h-10 w-full items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
              <Plus size={16} />
              创建任务
            </button>
          </form>
        </section>
      </div>
    </AppShell>
  );
}
