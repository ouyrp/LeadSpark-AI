import {
  ArrowUpRight,
  BarChart3,
  Bot,
  Building2,
  CalendarClock,
  PhoneCall,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

const metrics = [
  { label: "今日待跟进", value: "12", change: "+3", icon: CalendarClock },
  { label: "高分线索", value: "8", change: "+18%", icon: Sparkles },
  { label: "本周触达", value: "46", change: "+11%", icon: PhoneCall },
  { label: "月度成交额", value: "12.8万", change: "+24%", icon: BarChart3 },
];

const leads = [
  {
    company: "华东智造科技有限公司",
    industry: "智能制造",
    region: "上海",
    score: 91,
    grade: "S",
    signal: "近期招聘销售运营负责人",
  },
  {
    company: "云启软件服务有限公司",
    industry: "企业服务",
    region: "杭州",
    score: 86,
    grade: "A",
    signal: "官网新增渠道合作页面",
  },
  {
    company: "远航供应链管理有限公司",
    industry: "物流供应链",
    region: "苏州",
    score: 78,
    grade: "B",
    signal: "发布数字化升级新闻",
  },
];

const tasks = [
  "优先电话触达 S 级线索，并在通话后补充客户异议",
  "复盘制造业话术模板，保留回复率最高的开场",
  "检查 3 天未跟进的 A 级客户，生成二次跟进任务",
];

export default function Home() {
  return (
    <main className="min-h-screen bg-mist text-ink">
      <aside className="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-200 bg-white px-5 py-6 lg:block">
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded bg-leaf text-white">
            <Bot size={22} />
          </div>
          <div>
            <p className="text-lg font-semibold">LeadSpark AI</p>
            <p className="text-xs text-slate-500">智能主动获客</p>
          </div>
        </div>

        <nav className="mt-10 space-y-1 text-sm">
          {["工作台", "线索中心", "客户画像", "销售任务", "商机管理", "数据分析", "系统设置"].map(
            (item, index) => (
              <div
                key={item}
                className={`rounded px-3 py-2 ${
                  index === 0 ? "bg-leaf text-white" : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                {item}
              </div>
            ),
          )}
        </nav>
      </aside>

      <section className="lg:pl-64">
        <header className="border-b border-slate-200 bg-white px-5 py-4 lg:px-8">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-2xl font-semibold tracking-normal">销售工作台</h1>
              <p className="mt-1 text-sm text-slate-500">聚焦高分线索、跟进任务和 AI 推荐动作。</p>
            </div>
            <button className="inline-flex h-10 items-center justify-center gap-2 rounded bg-coral px-4 text-sm font-medium text-white">
              <Sparkles size={16} />
              生成今日策略
            </button>
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
                  <div className="mt-4 flex items-end justify-between">
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
                <button className="inline-flex items-center gap-1 text-sm font-medium text-leaf">
                  查看全部
                  <ArrowUpRight size={16} />
                </button>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[680px] border-collapse text-sm">
                  <thead className="bg-slate-50 text-left text-slate-500">
                    <tr>
                      <th className="px-4 py-3 font-medium">企业</th>
                      <th className="px-4 py-3 font-medium">行业</th>
                      <th className="px-4 py-3 font-medium">地区</th>
                      <th className="px-4 py-3 font-medium">评分</th>
                      <th className="px-4 py-3 font-medium">意图信号</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leads.map((lead) => (
                      <tr key={lead.company} className="border-t border-slate-100">
                        <td className="px-4 py-4 font-medium">{lead.company}</td>
                        <td className="px-4 py-4 text-slate-600">{lead.industry}</td>
                        <td className="px-4 py-4 text-slate-600">{lead.region}</td>
                        <td className="px-4 py-4">
                          <span className="rounded bg-leaf px-2 py-1 text-xs font-semibold text-white">
                            {lead.grade} {lead.score}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-slate-600">{lead.signal}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="rounded border border-slate-200 bg-white p-4">
              <div className="flex items-center gap-2">
                <ShieldCheck size={18} className="text-leaf" />
                <h2 className="font-semibold">AI 优化建议</h2>
              </div>
              <div className="mt-4 space-y-3">
                {tasks.map((task, index) => (
                  <div key={task} className="rounded border border-slate-200 p-3">
                    <div className="flex gap-3">
                      <span className="grid h-6 w-6 shrink-0 place-items-center rounded bg-mist text-xs font-semibold text-leaf">
                        {index + 1}
                      </span>
                      <p className="text-sm leading-6 text-slate-700">{task}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
