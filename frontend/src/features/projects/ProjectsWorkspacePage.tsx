import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  CheckSquare,
  Clock,
  FolderKanban,
  Layers,
  Plus,
  Search,
} from 'lucide-react'
import {
  useCreateProjectMutation,
  useCreateSprintMutation,
  useCreateTaskMutation,
  useGetProjectsQuery,
  useGetSprintsQuery,
  useGetTasksQuery,
  useGetTimeLogsQuery,
  useLogTimeMutation,
  useUpdateTaskStatusMutation,
} from '@/app/api/workplaceApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type ProjectTab = 'board' | 'projects' | 'sprints' | 'timelogs'

export function ProjectsWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<ProjectTab>('board')
  const [selectedProjectId, setSelectedProjectId] = useState<string>('')
  const [selectedSprintId, setSelectedSprintId] = useState<string>('ALL')
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL')
  const [taskSearch, setTaskSearch] = useState('')
  const [feedback, setFeedback] = useState<string | null>(null)

  const [projName, setProjName] = useState('')
  const [projClient, setProjClient] = useState('')
  const [projBudget, setProjBudget] = useState('25000')

  const [sprintName, setSprintName] = useState('')
  const [sprintGoal, setSprintGoal] = useState('')

  const [taskTitle, setTaskTitle] = useState('')
  const [taskPriority, setTaskPriority] = useState('MEDIUM')
  const [taskPoints, setTaskPoints] = useState('3')
  const [taskAssignee, setTaskAssignee] = useState('')

  const [logTaskId, setLogTaskId] = useState('')
  const [logMinutes, setLogMinutes] = useState('60')
  const [logNotes, setLogNotes] = useState('')

  const skip = !accessToken || !user

  const { data: projects = [] } = useGetProjectsQuery(undefined, { skip })
  const activeProjectId = selectedProjectId || projects[0]?.id || ''
  const { data: sprints = [] } = useGetSprintsQuery(activeProjectId, { skip: skip || !activeProjectId })
  const { data: tasks = [] } = useGetTasksQuery(activeProjectId || undefined, { skip })
  const { data: timeLogs = [] } = useGetTimeLogsQuery(undefined, { skip })

  const [createProject] = useCreateProjectMutation()
  const [createSprint] = useCreateSprintMutation()
  const [createTask] = useCreateTaskMutation()
  const [updateTaskStatus] = useUpdateTaskStatusMutation()
  const [logTime] = useLogTimeMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Project Management</CardTitle>
          <CardDescription>Manage projects, Scrum sprints, Kanban boards, and billable time logs.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const columns = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'] as const

  const advanceStatus = (current: string): string | null => {
    const idx = columns.indexOf(current as (typeof columns)[number])
    if (idx >= 0 && idx < columns.length - 1) return columns[idx + 1]
    return null
  }

  const regressStatus = (current: string): string | null => {
    const idx = columns.indexOf(current as (typeof columns)[number])
    if (idx > 0) return columns[idx - 1]
    return null
  }

  const activeProject = projects.find((p) => p.id === activeProjectId)

  const filteredTasks = tasks.filter((t) => {
    const matchSearch =
      !taskSearch ||
      t.title.toLowerCase().includes(taskSearch.toLowerCase()) ||
      t.taskKey.toLowerCase().includes(taskSearch.toLowerCase()) ||
      (t.assigneeName || '').toLowerCase().includes(taskSearch.toLowerCase())
    const matchSprint = selectedSprintId === 'ALL' || t.sprintId === selectedSprintId
    const matchPriority = priorityFilter === 'ALL' || t.priority === priorityFilter
    return matchSearch && matchSprint && matchPriority
  })

  const completedCount = tasks.filter((t) => t.status === 'DONE').length
  const sprintProgress = tasks.length > 0 ? Math.round((completedCount / tasks.length) * 100) : 0
  const totalLoggedMinutes = timeLogs.reduce((acc, l) => acc + l.minutesLogged, 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Agile Project & Timesheet Hub</h1>
          <p className="text-sm text-muted-foreground">
            Scrum sprints, dynamic interactive Kanban columns, billable timesheets & story point tracking
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to="/collaboration">Team Channels</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/analytics">Sprint Velocity BI</Link>
          </Button>
        </div>
      </div>

      {/* KPI Project Strip */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Active Projects</CardTitle>
            <FolderKanban className="h-4 w-4 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{projects.length}</div>
            <p className="text-xs text-muted-foreground">{activeProject?.name || 'Selected project'}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Sprint Completion</CardTitle>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{sprintProgress}%</div>
            <p className="text-xs text-muted-foreground">
              {completedCount} of {tasks.length} tasks completed
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Total Story Points</CardTitle>
            <Layers className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{tasks.reduce((sum, t) => sum + (t.storyPoints || 1), 0)} pts</div>
            <p className="text-xs text-muted-foreground">Across {tasks.length} backlog items</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Logged Hours</CardTitle>
            <Clock className="h-4 w-4 text-amber-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{(totalLoggedMinutes / 60).toFixed(1)} hrs</div>
            <p className="text-xs text-muted-foreground">{timeLogs.length} verified work entries</p>
          </CardContent>
        </Card>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-xl border border-indigo-200 bg-indigo-50/70 px-4 py-2 text-xs text-indigo-900 dark:bg-indigo-950/30 dark:text-indigo-200">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        <Button size="sm" variant={tab === 'board' ? 'default' : 'outline'} onClick={() => setTab('board')}>
          <FolderKanban className="mr-1.5 h-3.5 w-3.5" />
          Kanban Board ({tasks.length})
        </Button>
        <Button size="sm" variant={tab === 'sprints' ? 'default' : 'outline'} onClick={() => setTab('sprints')}>
          <Layers className="mr-1.5 h-3.5 w-3.5" />
          Sprints ({sprints.length})
        </Button>
        <Button size="sm" variant={tab === 'projects' ? 'default' : 'outline'} onClick={() => setTab('projects')}>
          <CheckSquare className="mr-1.5 h-3.5 w-3.5" />
          Projects ({projects.length})
        </Button>
        <Button size="sm" variant={tab === 'timelogs' ? 'default' : 'outline'} onClick={() => setTab('timelogs')}>
          <Clock className="mr-1.5 h-3.5 w-3.5" />
          Timesheets ({timeLogs.length})
        </Button>
      </div>

      {/* TAB 1: INTERACTIVE KANBAN BOARD */}
      {tab === 'board' && (
        <div className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap items-center gap-2">
              <select
                value={activeProjectId}
                onChange={(e) => setSelectedProjectId(e.target.value)}
                className="h-9 rounded-xl border border-input bg-background px-3 text-xs font-semibold"
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.projectCode} · {p.name}
                  </option>
                ))}
              </select>

              <select
                value={selectedSprintId}
                onChange={(e) => setSelectedSprintId(e.target.value)}
                className="h-9 rounded-xl border border-input bg-background px-3 text-xs"
              >
                <option value="ALL">All Sprints</option>
                {sprints.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>

              <select
                value={priorityFilter}
                onChange={(e) => setPriorityFilter(e.target.value)}
                className="h-9 rounded-xl border border-input bg-background px-3 text-xs"
              >
                <option value="ALL">All Priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <div className="relative">
                <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                <Input
                  placeholder="Filter tasks..."
                  value={taskSearch}
                  onChange={(e) => setTaskSearch(e.target.value)}
                  className="h-8 w-44 pl-8 text-xs"
                />
              </div>
            </div>
          </div>

          {/* Interactive Kanban Columns */}
          <div className="grid gap-4 md:grid-cols-4">
            {columns.map((col) => {
              const colTasks = filteredTasks.filter((t) => t.status === col)
              const colTitle = col.replace('_', ' ')
              return (
                <div key={col} className="flex flex-col rounded-2xl border bg-slate-50/70 p-3 dark:bg-card">
                  <div className="mb-3 flex items-center justify-between">
                    <span className="font-semibold text-xs tracking-wider uppercase text-muted-foreground">
                      {colTitle}
                    </span>
                    <Badge variant={col === 'DONE' ? 'success' : 'secondary'}>{colTasks.length}</Badge>
                  </div>

                  <div className="flex-1 space-y-2.5 min-h-[320px] max-h-[580px] overflow-y-auto pr-1">
                    {colTasks.length === 0 ? (
                      <p className="rounded-xl border border-dashed p-6 text-center text-xs text-muted-foreground">
                        No tasks in {colTitle}
                      </p>
                    ) : (
                      colTasks.map((t) => {
                        const nextSt = advanceStatus(t.status)
                        const prevSt = regressStatus(t.status)
                        return (
                          <div
                            key={t.id}
                            className="space-y-2 rounded-xl border bg-white p-3 text-xs shadow-xs transition-shadow hover:shadow-md dark:bg-slate-900"
                          >
                            <div className="flex items-start justify-between gap-2">
                              <span className="font-mono text-[10px] text-muted-foreground">{t.taskKey}</span>
                              <Badge
                                variant={
                                  t.priority === 'URGENT'
                                    ? 'warning'
                                    : t.priority === 'HIGH'
                                    ? 'default'
                                    : 'outline'
                                }
                                className="text-[9px]"
                              >
                                {t.priority}
                              </Badge>
                            </div>
                            <p className="font-medium text-foreground">{t.title}</p>
                            <div className="flex items-center justify-between text-[11px] text-muted-foreground">
                              <span>{t.assigneeName || 'Unassigned'}</span>
                              <span className="font-mono font-semibold">{t.storyPoints} pts</span>
                            </div>

                            {/* 1-Click Interactive State Movement */}
                            <div className="flex items-center justify-between border-t pt-2 gap-1">
                              {prevSt ? (
                                <button
                                  type="button"
                                  onClick={() => void updateTaskStatus({ id: t.id, status: prevSt })}
                                  className="inline-flex items-center gap-0.5 rounded px-1.5 py-0.5 text-[10px] text-muted-foreground hover:bg-muted"
                                  title={`Move back to ${prevSt}`}
                                >
                                  <ArrowLeft className="h-3 w-3" /> Back
                                </button>
                              ) : (
                                <span />
                              )}

                              {/* Quick Log 30m */}
                              <button
                                type="button"
                                onClick={async () => {
                                  await logTime({ taskId: t.id, minutesLogged: 30, billable: true }).unwrap()
                                  setFeedback(`Logged 30m for ${t.taskKey}`)
                                }}
                                className="text-[10px] text-indigo-600 hover:underline"
                              >
                                +30m
                              </button>

                              {nextSt && (
                                <button
                                  type="button"
                                  onClick={() => void updateTaskStatus({ id: t.id, status: nextSt })}
                                  className="inline-flex items-center gap-0.5 rounded px-2 py-0.5 text-[10px] font-semibold text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-950"
                                  title={`Advance to ${nextSt}`}
                                >
                                  Next <ArrowRight className="h-3 w-3" />
                                </button>
                              )}
                            </div>
                          </div>
                        )
                      })
                    )}
                  </div>
                </div>
              )
            })}
          </div>

          {/* Quick Create Task */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Create Backlog Item for {activeProject?.name || 'Project'}</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  if (!activeProjectId || !taskTitle.trim()) return
                  await createTask({
                    projectId: activeProjectId,
                    sprintId: selectedSprintId !== 'ALL' ? selectedSprintId : undefined,
                    title: taskTitle.trim(),
                    priority: taskPriority,
                    storyPoints: Number(taskPoints) || 1,
                    assigneeName: taskAssignee.trim() || undefined,
                  }).unwrap()
                  setTaskTitle('')
                  setTaskAssignee('')
                  setFeedback('Task created on board.')
                }}
                className="grid gap-2 sm:grid-cols-5"
              >
                <Input
                  className="sm:col-span-2"
                  placeholder="Task title..."
                  value={taskTitle}
                  onChange={(e) => setTaskTitle(e.target.value)}
                  required
                />
                <select
                  value={taskPriority}
                  onChange={(e) => setTaskPriority(e.target.value)}
                  className="h-9 rounded-md border border-input bg-background px-3 text-xs"
                >
                  <option value="LOW">Low Priority</option>
                  <option value="MEDIUM">Medium Priority</option>
                  <option value="HIGH">High Priority</option>
                  <option value="URGENT">Urgent</option>
                </select>
                <Input
                  type="number"
                  placeholder="Points"
                  value={taskPoints}
                  onChange={(e) => setTaskPoints(e.target.value)}
                  min={1}
                  max={21}
                />
                <Button type="submit">
                  <Plus className="mr-1.5 h-3.5 w-3.5" /> Add Task
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {/* TAB 2: SPRINTS */}
      {tab === 'sprints' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-4">
            <CardHeader>
              <CardTitle className="text-base">Start New Sprint</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  if (!activeProjectId || !sprintName.trim()) return
                  await createSprint({
                    projectId: activeProjectId,
                    name: sprintName.trim(),
                    goal: sprintGoal.trim() || undefined,
                  }).unwrap()
                  setSprintName('')
                  setSprintGoal('')
                  setFeedback('Sprint created.')
                }}
                className="space-y-3"
              >
                <Input placeholder="Sprint Name (e.g. Sprint 4)" value={sprintName} onChange={(e) => setSprintName(e.target.value)} required />
                <Input placeholder="Sprint Goal" value={sprintGoal} onChange={(e) => setSprintGoal(e.target.value)} />
                <Button type="submit" className="w-full">Create Sprint</Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-8">
            <CardHeader>
              <CardTitle>Sprints in {activeProject?.name || 'Project'}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5 max-h-[500px] overflow-y-auto">
              {sprints.map((s) => (
                <div key={s.id} className="flex items-center justify-between rounded-xl border p-3 text-xs">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm">{s.name}</span>
                      <Badge variant="success">{s.status}</Badge>
                    </div>
                    <p className="mt-1 text-muted-foreground">{s.goal || 'No goal set'} · {s.startDate} → {s.endDate}</p>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      {/* TAB 3: PROJECTS */}
      {tab === 'projects' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-4">
            <CardHeader>
              <CardTitle className="text-base">Create Project</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  if (!projName.trim()) return
                  const p = await createProject({
                    name: projName.trim(),
                    clientName: projClient.trim() || undefined,
                    budgetAmount: Number(projBudget) || 0,
                  }).unwrap()
                  setSelectedProjectId(p.id)
                  setProjName('')
                  setProjClient('')
                  setFeedback(`Project ${p.projectCode} created.`)
                }}
                className="space-y-3"
              >
                <Input placeholder="Project Name" value={projName} onChange={(e) => setProjName(e.target.value)} required />
                <Input placeholder="Client Name" value={projClient} onChange={(e) => setProjClient(e.target.value)} />
                <Input type="number" placeholder="Budget Amount ($)" value={projBudget} onChange={(e) => setProjBudget(e.target.value)} />
                <Button type="submit" className="w-full">Create Project</Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-8">
            <CardHeader>
              <CardTitle>Portfolio Projects ({projects.length})</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5 max-h-[500px] overflow-y-auto">
              {projects.map((p) => (
                <div
                  key={p.id}
                  className={`flex items-center justify-between rounded-xl border p-3 text-xs ${
                    activeProjectId === p.id ? 'border-indigo-500 bg-indigo-50/40' : ''
                  }`}
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm">{p.name}</span>
                      <Badge variant="outline">{p.projectCode}</Badge>
                      <Badge variant="success">{p.status}</Badge>
                    </div>
                    <p className="mt-1 text-muted-foreground">Client: {p.clientName || 'Internal'} · Budget: ${Number(p.budgetAmount).toLocaleString()}</p>
                  </div>
                  <Button size="sm" variant="outline" onClick={() => setSelectedProjectId(p.id)}>
                    View Board
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      {/* TAB 4: TIMESHEETS */}
      {tab === 'timelogs' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-4">
            <CardHeader>
              <CardTitle className="text-base">Log Work Time</CardTitle>
              <CardDescription>Record billable engineering or delivery hours against any task</CardDescription>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  const targetTask = logTaskId || tasks[0]?.id
                  if (!targetTask) return
                  await logTime({
                    taskId: targetTask,
                    minutesLogged: Math.max(5, Number(logMinutes) || 60),
                    billable: true,
                    notes: logNotes.trim() || 'Logged from Timesheets console',
                  }).unwrap()
                  setLogNotes('')
                  setFeedback('Billable time entry logged.')
                }}
                className="space-y-3"
              >
                <select
                  value={logTaskId || tasks[0]?.id || ''}
                  onChange={(e) => setLogTaskId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  {tasks.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.taskKey} · {t.title}
                    </option>
                  ))}
                </select>
                <Input
                  type="number"
                  min={5}
                  placeholder="Minutes (e.g. 60)"
                  value={logMinutes}
                  onChange={(e) => setLogMinutes(e.target.value)}
                  required
                />
                <Input
                  placeholder="Work description / deliverables"
                  value={logNotes}
                  onChange={(e) => setLogNotes(e.target.value)}
                />
                <Button type="submit" className="w-full" disabled={tasks.length === 0}>
                  <Clock className="mr-1.5 h-4 w-4" />
                  Log Billable Time
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-8">
            <CardHeader>
              <CardTitle>Billable Time Entries ({timeLogs.length})</CardTitle>
              <CardDescription>Verified engineering, architecture & support time allocation</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2.5 max-h-[500px] overflow-y-auto">
              {timeLogs.map((l) => (
                <div key={l.id} className="flex items-center justify-between rounded-xl border p-3 text-xs">
                  <div>
                    <p className="font-semibold text-sm">{l.memberName}</p>
                    <p className="text-muted-foreground">{l.workDate} · {l.notes || 'No description'}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-indigo-600">{l.minutesLogged} mins</span>
                    <Badge variant={l.billable ? 'success' : 'outline'}>{l.billable ? 'Billable' : 'Internal'}</Badge>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
