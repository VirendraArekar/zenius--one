import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ArrowRightLeft, Building2, CheckCircle2, PieChart as PieChartIcon, Plus, Trash2, TrendingUp, Users } from 'lucide-react'
import { getStageColor, chartTooltipStyle } from '@/components/ui/chartThemes'
import {
  useArchiveLeadMutation,
  useCompleteActivityMutation,
  useConvertLeadMutation,
  useCreateAccountMutation,
  useCreateActivityMutation,
  useCreateContactMutation,
  useCreateDealMutation,
  useCreateLeadMutation,
  useGetAccountsQuery,
  useGetActivitiesQuery,
  useGetContactsQuery,
  useGetCrmDashboardQuery,
  useGetDealsQuery,
  useGetLeadsQuery,
  useGetPipelinesQuery,
  useImportLeadsCsvMutation,
  useMoveDealStageMutation,
} from '@/app/api/crmApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type CrmTab = 'dashboard' | 'leads' | 'pipeline' | 'accounts' | 'contacts' | 'activities'

export function CrmWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<CrmTab>('dashboard')
  const [feedback, setFeedback] = useState<string | null>(null)

  // Lead form
  const [leadSearch, setLeadSearch] = useState('')
  const [leadName, setLeadName] = useState('')
  const [leadEmail, setLeadEmail] = useState('')
  const [leadCompany, setLeadCompany] = useState('')
  const [leadSource, setLeadSource] = useState('WEBSITE')
  const [leadValue, setLeadValue] = useState('')
  const [csvImportText, setCsvImportText] = useState('')

  // Deal form
  const [dealTitle, setDealTitle] = useState('')
  const [dealAmount, setDealAmount] = useState('')

  // Account form
  const [accName, setAccName] = useState('')
  const [accIndustry, setAccIndustry] = useState('')
  const [accStage, setAccStage] = useState('PROSPECT')

  // Contact form
  const [contactFirst, setContactFirst] = useState('')
  const [contactLast, setContactLast] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [contactAccountId, setContactAccountId] = useState('')

  // Activity form
  const [actType, setActType] = useState('CALL')
  const [actSubject, setActSubject] = useState('')
  const [actAccountId, setActAccountId] = useState('')

  const skip = !accessToken || !user

  const { data: dashboard } = useGetCrmDashboardQuery(undefined, { skip })
  const { data: leadsRes } = useGetLeadsQuery({ q: leadSearch || undefined }, { skip })
  const { data: pipelines = [] } = useGetPipelinesQuery(undefined, { skip })
  const { data: dealsRes } = useGetDealsQuery(undefined, { skip })
  const { data: accountsRes } = useGetAccountsQuery(undefined, { skip })
  const { data: contactsRes } = useGetContactsQuery(undefined, { skip })
  const { data: activitiesRes } = useGetActivitiesQuery(undefined, { skip })

  const [createLead] = useCreateLeadMutation()
  const [convertLead] = useConvertLeadMutation()
  const [archiveLead] = useArchiveLeadMutation()
  const [importCsv] = useImportLeadsCsvMutation()
  const [createDeal] = useCreateDealMutation()
  const [moveDealStage] = useMoveDealStageMutation()
  const [createAccount] = useCreateAccountMutation()
  const [createContact] = useCreateContactMutation()
  const [createActivity] = useCreateActivityMutation()
  const [completeActivity] = useCompleteActivityMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Sales & CRM</CardTitle>
          <CardDescription>
            CRM records are isolated per organization. Sign in or create a workspace to manage
            leads, pipelines, accounts, contacts, and quotes.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex gap-3">
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/register">Create Organization</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const defaultPipeline = pipelines[0]
  const leads = leadsRes?.data ?? []
  const deals = dealsRes?.data ?? []
  const accounts = accountsRes?.data ?? []
  const contacts = contactsRes?.data ?? []
  const activities = activitiesRes?.data ?? []

  const handleCreateLead = async (e: React.FormEvent) => {
    e.preventDefault()
    await createLead({
      fullName: leadName,
      email: leadEmail || undefined,
      companyName: leadCompany || undefined,
      source: leadSource,
      estimatedValue: leadValue ? Number(leadValue) : undefined,
    }).unwrap()
    setLeadName('')
    setLeadEmail('')
    setLeadCompany('')
    setLeadValue('')
    setFeedback('Lead created with automated score.')
  }

  const handleConvertLead = async (id: string, name: string) => {
    await convertLead({ id, createDeal: true, dealTitle: `${name} Opportunity` }).unwrap()
    setFeedback(`Converted lead "${name}" into Account, Contact, and Deal.`)
  }

  const handleCreateDeal = async (e: React.FormEvent) => {
    e.preventDefault()
    await createDeal({ title: dealTitle, amount: Number(dealAmount || 0) }).unwrap()
    setDealTitle('')
    setDealAmount('')
    setFeedback('Deal added to pipeline.')
  }

  const handleMoveStage = async (dealId: string, stageId: string, isClosedLost: boolean) => {
    let lostReason: string | undefined
    if (isClosedLost) {
      lostReason = 'Budget or timing mismatch'
    }
    await moveDealStage({ id: dealId, stageId, lostReason }).unwrap()
    setFeedback('Deal stage & weighted forecast updated.')
  }

  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault()
    await createAccount({
      name: accName,
      industry: accIndustry || undefined,
      lifecycleStage: accStage,
    }).unwrap()
    setAccName('')
    setAccIndustry('')
    setFeedback('Account created.')
  }

  const handleCreateContact = async (e: React.FormEvent) => {
    e.preventDefault()
    await createContact({
      accountId: contactAccountId || undefined,
      firstName: contactFirst,
      lastName: contactLast,
      email: contactEmail || undefined,
    }).unwrap()
    setContactFirst('')
    setContactLast('')
    setContactEmail('')
    setFeedback('Contact created.')
  }

  const handleCreateActivity = async (e: React.FormEvent) => {
    e.preventDefault()
    const targetId = actAccountId || accounts[0]?.id
    if (!targetId) {
      setFeedback('Create an Account first to attach CRM activities.')
      return
    }
    await createActivity({
      activityType: actType,
      subject: actSubject,
      relatedEntityType: 'ACCOUNT',
      relatedEntityId: targetId,
    }).unwrap()
    setActSubject('')
    setFeedback('Activity logged.')
  }

  const tabs: { id: CrmTab; label: string }[] = [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'leads', label: `Leads (${leads.length})` },
    { id: 'pipeline', label: `Pipeline (${deals.length})` },
    { id: 'accounts', label: `Accounts (${accounts.length})` },
    { id: 'contacts', label: `Contacts (${contacts.length})` },
    { id: 'activities', label: `Activities (${activities.length})` },
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">CRM & Revenue Pipeline</h1>
          <p className="text-sm text-muted-foreground">
            Multi-tenant Lead-to-Cash workspace connected to <code>/api/v1/crm/*</code>
          </p>
        </div>
        <Button variant="outline" asChild>
          <Link to="/sales">Open Quote-to-Cash & Products →</Link>
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

      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        {tabs.map((t) => (
          <Button
            key={t.id}
            size="sm"
            variant={tab === t.id ? 'default' : 'outline'}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </Button>
        ))}
      </div>

      {tab === 'dashboard' && (
        <div className="space-y-6 animate-enter-slide">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Card className="card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Total Leads</CardTitle>
                <Users className="h-4 w-4 text-indigo-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">{dashboard?.totalLeads ?? 0}</div>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {dashboard?.convertedLeads ?? 0} converted to accounts
                </p>
              </CardContent>
            </Card>
            <Card className="card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Open Pipeline</CardTitle>
                <TrendingUp className="h-4 w-4 text-cyan-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">
                  ${Number(dashboard?.openPipelineAmount ?? 0).toLocaleString()}
                </div>
                <p className="mt-0.5 text-xs text-muted-foreground">{dashboard?.openDeals ?? 0} active deals</p>
              </CardContent>
            </Card>
            <Card className="card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Weighted Forecast</CardTitle>
                <TrendingUp className="h-4 w-4 text-emerald-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                  ${Number(dashboard?.weightedForecast ?? 0).toLocaleString()}
                </div>
                <p className="mt-0.5 text-xs text-muted-foreground">Probability-adjusted revenue</p>
              </CardContent>
            </Card>
            <Card className="card-motion">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Accounts & Contacts</CardTitle>
                <Building2 className="h-4 w-4 text-violet-500" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">{dashboard?.totalAccounts ?? 0}</div>
                <p className="mt-0.5 text-xs text-muted-foreground">{dashboard?.totalContacts ?? 0} contacts</p>
              </CardContent>
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2 card-motion">
              <CardHeader>
                <CardTitle>Pipeline Funnel by Stage</CardTitle>
                <CardDescription>Live deal amounts and progression across sales stages</CardDescription>
              </CardHeader>
              <CardContent className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={dashboard?.stageBreakdown ?? []}>
                    <CartesianGrid strokeDasharray="3 3" opacity={0.2} className="stroke-border" />
                    <XAxis dataKey="stageName" fontSize={11} stroke="currentColor" opacity={0.7} />
                    <YAxis fontSize={11} stroke="currentColor" opacity={0.7} />
                    <Tooltip contentStyle={chartTooltipStyle} cursor={{ fill: 'rgba(99, 102, 241, 0.08)' }} />
                    <Bar dataKey="totalAmount" radius={[6, 6, 0, 0]} animationDuration={800}>
                      {(dashboard?.stageBreakdown ?? []).map((entry, idx) => (
                        <Cell key={`stage-cell-${idx}`} fill={getStageColor(entry.stageName, idx)} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            <Card className="card-motion">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <PieChartIcon className="h-4 w-4 text-primary" />
                  Stage Share
                </CardTitle>
                <CardDescription>Deal volume proportion</CardDescription>
              </CardHeader>
              <CardContent className="h-72">
                {(dashboard?.stageBreakdown ?? []).length === 0 ? (
                  <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
                    No active deals in pipeline
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Tooltip contentStyle={chartTooltipStyle} />
                      <Pie
                        data={dashboard?.stageBreakdown ?? []}
                        dataKey="dealCount"
                        nameKey="stageName"
                        cx="50%"
                        cy="50%"
                        innerRadius={45}
                        outerRadius={75}
                        paddingAngle={4}
                        animationDuration={800}
                      >
                        {(dashboard?.stageBreakdown ?? []).map((entry, idx) => (
                          <Cell key={`pie-stage-${idx}`} fill={getStageColor(entry.stageName, idx)} />
                        ))}
                      </Pie>
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {tab === 'leads' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader className="flex flex-row items-center justify-between gap-3">
              <div>
                <CardTitle>Leads</CardTitle>
                <CardDescription>Duplicate email guarded & automatically scored</CardDescription>
              </div>
              <Input
                value={leadSearch}
                onChange={(e) => setLeadSearch(e.target.value)}
                placeholder="Search leads..."
                className="max-w-xs"
              />
            </CardHeader>
            <CardContent className="space-y-2.5">
              {leads.length === 0 && (
                <p className="text-xs text-muted-foreground">No leads yet. Create or import leads on the right.</p>
              )}
              {leads.map((lead) => (
                <div key={lead.id} className="flex flex-col gap-2 rounded-lg border p-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold">{lead.fullName}</span>
                      <Badge variant={lead.status === 'CONVERTED' ? 'success' : 'info'}>
                        {lead.status}
                      </Badge>
                      <Badge variant="outline">Score {lead.leadScore}</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {[lead.companyName, lead.email, lead.source].filter(Boolean).join(' · ')}
                    </p>
                  </div>
                  <div className="flex items-center gap-1.5">
                    {lead.status !== 'CONVERTED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleConvertLead(lead.id, lead.fullName)}
                      >
                        <ArrowRightLeft className="h-3.5 w-3.5" />
                        <span>Convert</span>
                      </Button>
                    )}
                    <Button
                      size="icon"
                      variant="ghost"
                      aria-label="Archive lead"
                      onClick={() => archiveLead(lead.id)}
                    >
                      <Trash2 className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>New Lead</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleCreateLead} className="space-y-2.5">
                  <Input placeholder="Full Name" value={leadName} onChange={(e) => setLeadName(e.target.value)} required />
                  <Input type="email" placeholder="Email" value={leadEmail} onChange={(e) => setLeadEmail(e.target.value)} />
                  <Input placeholder="Company Name" value={leadCompany} onChange={(e) => setLeadCompany(e.target.value)} />
                  <select
                    value={leadSource}
                    onChange={(e) => setLeadSource(e.target.value)}
                    className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                  >
                    <option value="WEBSITE">Website</option>
                    <option value="REFERRAL">Referral</option>
                    <option value="PARTNER">Partner</option>
                    <option value="OUTBOUND">Outbound</option>
                  </select>
                  <Input
                    type="number"
                    placeholder="Estimated Value ($)"
                    value={leadValue}
                    onChange={(e) => setLeadValue(e.target.value)}
                  />
                  <Button type="submit" className="w-full">
                    <Plus className="h-4 w-4" />
                    <span>Create Lead</span>
                  </Button>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Bulk CSV Import</CardTitle>
                <CardDescription>Format: fullName,email,phone,company,source,value</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2">
                <textarea
                  rows={3}
                  value={csvImportText}
                  onChange={(e) => setCsvImportText(e.target.value)}
                  placeholder="Jane Doe,jane@acme.io,+1-555-0100,Acme Inc,WEBSITE,25000"
                  className="w-full rounded-md border border-input bg-background p-2 text-xs"
                />
                <Button
                  size="sm"
                  variant="outline"
                  className="w-full"
                  onClick={async () => {
                    if (!csvImportText.trim()) return
                    const res = await importCsv({ csv: csvImportText }).unwrap()
                    setCsvImportText('')
                    setFeedback(`Imported ${res.imported} leads (${res.skipped} skipped).`)
                  }}
                >
                  Import CSV
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {tab === 'pipeline' && (
        <div className="space-y-4">
          <Card>
            <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
              <form onSubmit={handleCreateDeal} className="flex flex-1 flex-wrap gap-2">
                <Input
                  placeholder="New Deal Title"
                  value={dealTitle}
                  onChange={(e) => setDealTitle(e.target.value)}
                  className="max-w-xs"
                  required
                />
                <Input
                  type="number"
                  placeholder="Amount ($)"
                  value={dealAmount}
                  onChange={(e) => setDealAmount(e.target.value)}
                  className="max-w-40"
                  required
                />
                <Button type="submit">Add Deal</Button>
              </form>
            </CardContent>
          </Card>

          <div className="grid gap-4 overflow-x-auto md:grid-cols-3 xl:grid-cols-6">
            {(defaultPipeline?.stages ?? []).map((stage) => {
              const stageDeals = deals.filter((d) => d.stageId === stage.id)
              return (
                <div key={stage.id} className="rounded-xl border bg-muted/30 p-3 space-y-2.5 min-w-52">
                  <div className="flex items-center justify-between border-b pb-2">
                    <div>
                      <p className="text-xs font-semibold">{stage.name}</p>
                      <p className="text-[10px] text-muted-foreground">{stage.probability}% prob</p>
                    </div>
                    <Badge variant="secondary">{stageDeals.length}</Badge>
                  </div>
                  <div className="space-y-2">
                    {stageDeals.map((deal) => (
                      <div key={deal.id} className="rounded-lg border bg-card p-2.5 shadow-sm space-y-2">
                        <p className="text-xs font-semibold">{deal.title}</p>
                        <p className="text-xs font-mono text-primary">
                          ${Number(deal.amount).toLocaleString()}
                        </p>
                        <select
                          value={deal.stageId}
                          onChange={(e) => {
                            const nextStage = defaultPipeline?.stages.find((s) => s.id === e.target.value)
                            handleMoveStage(
                              deal.id,
                              e.target.value,
                              Boolean(nextStage?.closed && !nextStage?.won)
                            )
                          }}
                          className="h-7 w-full rounded border bg-background px-1.5 text-[11px]"
                        >
                          {(defaultPipeline?.stages ?? []).map((s) => (
                            <option key={s.id} value={s.id}>
                              {s.name}
                            </option>
                          ))}
                        </select>
                      </div>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {tab === 'accounts' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Company Accounts</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {accounts.map((acc) => (
                <div key={acc.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <p className="text-sm font-semibold">{acc.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {[acc.industry, acc.billingCity, acc.billingCountry].filter(Boolean).join(' · ') || 'Enterprise Account'}
                    </p>
                  </div>
                  <Badge variant="outline">{acc.lifecycleStage}</Badge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>New Account</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateAccount} className="space-y-2.5">
                <Input placeholder="Company Name" value={accName} onChange={(e) => setAccName(e.target.value)} required />
                <Input placeholder="Industry" value={accIndustry} onChange={(e) => setAccIndustry(e.target.value)} />
                <select
                  value={accStage}
                  onChange={(e) => setAccStage(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="PROSPECT">Prospect</option>
                  <option value="CUSTOMER">Customer</option>
                  <option value="PARTNER">Partner</option>
                </select>
                <Button type="submit" className="w-full">Create Account</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'contacts' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Customer Contacts</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {contacts.map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <p className="text-sm font-semibold">
                      {c.firstName} {c.lastName}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {[c.email, c.jobTitle].filter(Boolean).join(' · ') || 'Contact'}
                    </p>
                  </div>
                  {c.segment && <Badge variant="secondary">{c.segment}</Badge>}
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>New Contact</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateContact} className="space-y-2.5">
                <Input placeholder="First Name" value={contactFirst} onChange={(e) => setContactFirst(e.target.value)} required />
                <Input placeholder="Last Name" value={contactLast} onChange={(e) => setContactLast(e.target.value)} required />
                <Input type="email" placeholder="Email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} />
                <select
                  value={contactAccountId}
                  onChange={(e) => setContactAccountId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">No Account</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
                </select>
                <Button type="submit" className="w-full">Create Contact</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'activities' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Sales Activities & Tasks</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {activities.map((act) => (
                <div key={act.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">{act.activityType}</Badge>
                      <span className="text-sm font-semibold">{act.subject}</span>
                    </div>
                    <p className="text-xs text-muted-foreground">{act.relatedEntityType}</p>
                  </div>
                  {act.status === 'COMPLETED' ? (
                    <Badge variant="success">Completed</Badge>
                  ) : (
                    <Button size="sm" variant="outline" onClick={() => completeActivity(act.id)}>
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      <span>Complete</span>
                    </Button>
                  )}
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Log Activity</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateActivity} className="space-y-2.5">
                <select
                  value={actType}
                  onChange={(e) => setActType(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="CALL">Call</option>
                  <option value="MEETING">Meeting</option>
                  <option value="TASK">Task</option>
                  <option value="EMAIL">Email</option>
                </select>
                <Input placeholder="Subject" value={actSubject} onChange={(e) => setActSubject(e.target.value)} required />
                <select
                  value={actAccountId}
                  onChange={(e) => setActAccountId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
                </select>
                <Button type="submit" className="w-full">Log Activity</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

