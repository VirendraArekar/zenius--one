import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Play, Power, Workflow } from 'lucide-react'
import {
  useCreateWorkflowMutation,
  useGetWorkflowExecutionsQuery,
  useGetWorkflowsQuery,
  useToggleWorkflowMutation,
  useTriggerWorkflowMutation,
} from '@/app/api/intelligenceApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

export function AutomationWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [name, setName] = useState('')
  const [triggerEvent, setTriggerEvent] = useState('CRM_DEAL_WON')
  const [conditionExpr, setConditionExpr] = useState('amount>1000')
  const [actionType, setActionType] = useState('CREATE_NOTIFICATION')
  const [sampleAmount, setSampleAmount] = useState('2500')
  const [feedback, setFeedback] = useState<string | null>(null)

  const skip = !accessToken || !user
  const { data: workflows = [] } = useGetWorkflowsQuery(undefined, { skip })
  const { data: executions = [] } = useGetWorkflowExecutionsQuery(undefined, { skip })

  const [createWorkflow] = useCreateWorkflowMutation()
  const [toggleWorkflow] = useToggleWorkflowMutation()
  const [triggerWorkflow] = useTriggerWorkflowMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Workflow Automation & RPA</CardTitle>
          <CardDescription>Configure event triggers, condition rules, notifications, and audit executions.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    try {
      const wf = await createWorkflow({
        name: name.trim(),
        triggerEvent,
        conditionExpr: conditionExpr.trim() || undefined,
        actionType,
      }).unwrap()
      setName('')
      setFeedback(`Created workflow ${wf.name}`)
    } catch {
      setFeedback('Failed to create workflow')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Workflow Automation & RPA</h1>
          <p className="text-sm text-muted-foreground">
            Event-driven workflows, conditional branching, automated notifications, and execution telemetry
          </p>
        </div>
        <Button variant="outline" asChild>
          <Link to="/analytics">Analytics & BI</Link>
        </Button>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Workflow className="h-4 w-4 text-primary" /> Create Automation Rule
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreate} className="space-y-3">
              <Input placeholder="Workflow name" value={name} onChange={(e) => setName(e.target.value)} />
              <select
                aria-label="Trigger Event"
                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                value={triggerEvent}
                onChange={(e) => setTriggerEvent(e.target.value)}
              >
                <option value="CRM_LEAD_CREATED">CRM_LEAD_CREATED</option>
                <option value="CRM_DEAL_WON">CRM_DEAL_WON</option>
                <option value="SUPPORT_TICKET_CREATED">SUPPORT_TICKET_CREATED</option>
                <option value="INVOICE_OVERDUE">INVOICE_OVERDUE</option>
                <option value="MANUAL_SCHEDULE">MANUAL_SCHEDULE</option>
              </select>
              <Input
                placeholder="Condition (e.g. amount>1000)"
                value={conditionExpr}
                onChange={(e) => setConditionExpr(e.target.value)}
              />
              <select
                aria-label="Action Type"
                className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                value={actionType}
                onChange={(e) => setActionType(e.target.value)}
              >
                <option value="CREATE_NOTIFICATION">CREATE_NOTIFICATION</option>
                <option value="CALL_WEBHOOK">CALL_WEBHOOK</option>
                <option value="CREATE_CRM_ACTIVITY">CREATE_CRM_ACTIVITY</option>
                <option value="CREATE_PROJECT_TASK">CREATE_PROJECT_TASK</option>
                <option value="RPA_SYNC">RPA_SYNC</option>
              </select>
              <Button type="submit" className="w-full">
                Save Workflow
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-base">Active Workflows ({workflows.length})</CardTitle>
            <div className="flex items-center gap-2 text-xs">
              <span>Test amount:</span>
              <Input
                type="number"
                className="h-8 w-24"
                value={sampleAmount}
                onChange={(e) => setSampleAmount(e.target.value)}
              />
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            {workflows.map((wf) => (
              <div key={wf.id} className="flex flex-col gap-2 rounded-lg border p-3 text-sm sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-semibold">{wf.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {wf.triggerEvent} · If {wf.conditionExpr || 'ALWAYS'} → {wf.actionType} · Runs: {wf.runCount}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={wf.enabled ? 'default' : 'secondary'}>
                    {wf.enabled ? 'ENABLED' : 'PAUSED'}
                  </Badge>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => toggleWorkflow({ id: wf.id, enabled: !wf.enabled })}
                  >
                    <Power className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    size="sm"
                    onClick={async () => {
                      const exec = await triggerWorkflow({
                        id: wf.id,
                        payload: { amount: Number(sampleAmount) || 0 },
                      }).unwrap()
                      setFeedback(`Execution ${exec.status}: ${exec.outputSummary}`)
                    }}
                  >
                    <Play className="mr-1 h-3.5 w-3.5" /> Trigger
                  </Button>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Execution History ({executions.length})</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {executions.map((ex) => (
            <div key={ex.id} className="flex items-center justify-between rounded border px-3 py-2 text-xs">
              <div>
                <span className="font-medium">{ex.outputSummary}</span>
                <span className="ml-2 text-muted-foreground">({ex.durationMs}ms)</span>
              </div>
              <Badge variant={ex.status === 'SUCCESS' ? 'default' : 'secondary'}>{ex.status}</Badge>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}

