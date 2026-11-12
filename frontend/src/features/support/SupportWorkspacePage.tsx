import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, CalendarClock, CheckCircle2, Headphones, MessageSquare, ShieldAlert, Star } from 'lucide-react'
import {
  useAddTicketCommentMutation,
  useCreateAppointmentMutation,
  useCreateKbArticleMutation,
  useCreateTicketMutation,
  useGetAppointmentsQuery,
  useGetKbArticlesQuery,
  useGetSlaPoliciesQuery,
  useGetSupportDashboardQuery,
  useGetTicketCommentsQuery,
  useGetTicketsQuery,
  useSubmitCsatMutation,
  useUpdateTicketStatusMutation,
  useVoteKbHelpfulMutation,
} from '@/app/api/growthAndSupportApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type SupTab = 'tickets' | 'kb' | 'field' | 'slas'

export function SupportWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<SupTab>('tickets')
  const [selectedTicketId, setSelectedTicketId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)

  const [subject, setSubject] = useState('')
  const [desc, setDesc] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [reqName, setReqName] = useState('')
  const [reqEmail, setReqEmail] = useState('')
  const [commentBody, setCommentBody] = useState('')

  const [kbTitle, setKbTitle] = useState('')
  const [kbCat, setKbCat] = useState('General')
  const [kbContent, setKbContent] = useState('')

  const [apptCustomer, setApptCustomer] = useState('')
  const [apptAddress, setApptAddress] = useState('')

  const skip = !accessToken || !user

  const { data: dashboard } = useGetSupportDashboardQuery(undefined, { skip })
  const { data: ticketsRes } = useGetTicketsQuery(undefined, { skip })
  const { data: comments = [] } = useGetTicketCommentsQuery(selectedTicketId ?? '', {
    skip: skip || !selectedTicketId,
  })
  const { data: slas = [] } = useGetSlaPoliciesQuery(undefined, { skip })
  const { data: kbRes } = useGetKbArticlesQuery(undefined, { skip })
  const { data: appointments = [] } = useGetAppointmentsQuery(undefined, { skip })

  const [createTicket] = useCreateTicketMutation()
  const [updateTicketStatus] = useUpdateTicketStatusMutation()
  const [addComment] = useAddTicketCommentMutation()
  const [submitCsat] = useSubmitCsatMutation()
  const [createKb] = useCreateKbArticleMutation()
  const [voteHelpful] = useVoteKbHelpfulMutation()
  const [createAppointment] = useCreateAppointmentMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Customer Support</CardTitle>
          <CardDescription>Manage tickets, SLAs, knowledge base articles, and field service.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const tickets = ticketsRes?.data ?? []
  const kbArticles = kbRes?.data ?? []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Customer Support & Field Service</h1>
        <p className="text-sm text-muted-foreground">
          Multi-channel ticketing, SLA tracking, knowledge base, CSAT, and technician dispatch
        </p>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Open Tickets</CardTitle>
            <Headphones className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.openTickets ?? 0}</div>
            <p className="text-xs text-muted-foreground">{dashboard?.totalTickets ?? 0} total tickets</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Average CSAT</CardTitle>
            <Star className="h-4 w-4 text-amber-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {dashboard?.averageCsat ? dashboard.averageCsat.toFixed(1) : '—'}
            </div>
            <p className="text-xs text-muted-foreground">{dashboard?.resolvedTickets ?? 0} resolved</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">KB Articles</CardTitle>
            <BookOpen className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.publishedKbArticles ?? 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Field Appointments</CardTitle>
            <CalendarClock className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.fieldAppointments ?? 0}</div>
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        {(['tickets', 'kb', 'field', 'slas'] as SupTab[]).map((t) => (
          <Button key={t} size="sm" variant={tab === t ? 'default' : 'outline'} onClick={() => setTab(t)}>
            {t.toUpperCase()}
          </Button>
        ))}
      </div>

      {tab === 'tickets' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Support Queue</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2.5">
                {tickets.map((t) => (
                  <div key={t.id} className="rounded-lg border p-3 space-y-2">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold">{t.ticketNumber}</span>
                        <span className="text-sm font-semibold">{t.subject}</span>
                        <Badge variant={t.status === 'RESOLVED' ? 'success' : 'info'}>{t.status}</Badge>
                        <Badge variant={t.priority === 'URGENT' || t.priority === 'HIGH' ? 'warning' : 'secondary'}>
                          {t.priority}
                        </Badge>
                      </div>
                      <div className="flex gap-1.5">
                        <Button size="sm" variant="outline" onClick={() => setSelectedTicketId(t.id)}>
                          <MessageSquare className="h-3.5 w-3.5" />
                          <span>Thread</span>
                        </Button>
                        {t.status !== 'RESOLVED' && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => updateTicketStatus({ id: t.id, status: 'RESOLVED', escalate: false })}
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            <span>Resolve</span>
                          </Button>
                        )}
                      </div>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Requester: {t.requesterName} ({t.requesterEmail}) · Channel: {t.channel}
                    </p>
                    {t.status === 'RESOLVED' && t.csatRating == null && (
                      <div className="flex items-center gap-2 text-xs">
                        <span>Record CSAT:</span>
                        {[5, 4, 3].map((score) => (
                          <Button
                            key={score}
                            size="sm"
                            variant="ghost"
                            onClick={() => submitCsat({ ticketId: t.id, rating: score })}
                          >
                            {score}★
                          </Button>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </CardContent>
            </Card>

            {selectedTicketId && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-base">Ticket Conversation</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="space-y-2">
                    {comments.map((c) => (
                      <div key={c.id} className="rounded-lg border bg-muted/40 p-2.5 text-xs">
                        <p className="font-semibold">{c.authorName}</p>
                        <p className="mt-1">{c.body}</p>
                      </div>
                    ))}
                  </div>
                  <form
                    onSubmit={async (e) => {
                      e.preventDefault()
                      await addComment({ ticketId: selectedTicketId, body: commentBody, internalNote: false }).unwrap()
                      setCommentBody('')
                    }}
                    className="flex gap-2"
                  >
                    <Input placeholder="Write a reply..." value={commentBody} onChange={(e) => setCommentBody(e.target.value)} required />
                    <Button type="submit">Send</Button>
                  </form>
                </CardContent>
              </Card>
            )}
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Create Ticket</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createTicket({
                    subject,
                    description: desc,
                    channel: 'WEB',
                    priority,
                    requesterName: reqName,
                    requesterEmail: reqEmail,
                  }).unwrap()
                  setSubject('')
                  setDesc('')
                  setReqName('')
                  setReqEmail('')
                  setFeedback('Ticket logged with SLA policy deadline.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Subject" value={subject} onChange={(e) => setSubject(e.target.value)} required />
                <textarea
                  rows={3}
                  placeholder="Issue description..."
                  value={desc}
                  onChange={(e) => setDesc(e.target.value)}
                  className="w-full rounded-md border border-input bg-background p-2 text-xs"
                  required
                />
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="URGENT">Urgent</option>
                </select>
                <Input placeholder="Customer Name" value={reqName} onChange={(e) => setReqName(e.target.value)} required />
                <Input type="email" placeholder="Customer Email" value={reqEmail} onChange={(e) => setReqEmail(e.target.value)} required />
                <Button type="submit" className="w-full">Open Ticket</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'kb' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Knowledge Base & FAQ</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {kbArticles.map((art) => (
                <div key={art.id} className="rounded-lg border p-3 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold">{art.title}</span>
                    <Badge variant="outline">{art.category}</Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">{art.content}</p>
                  <Button size="sm" variant="ghost" onClick={() => voteHelpful(art.id)}>
                    Helpful ({art.helpfulCount})
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Publish KB Article</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createKb({ title: kbTitle, category: kbCat, content: kbContent, published: true }).unwrap()
                  setKbTitle('')
                  setKbContent('')
                  setFeedback('Knowledge base article published.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Title" value={kbTitle} onChange={(e) => setKbTitle(e.target.value)} required />
                <Input placeholder="Category" value={kbCat} onChange={(e) => setKbCat(e.target.value)} required />
                <textarea
                  rows={4}
                  placeholder="Article body..."
                  value={kbContent}
                  onChange={(e) => setKbContent(e.target.value)}
                  className="w-full rounded-md border border-input bg-background p-2 text-xs"
                  required
                />
                <Button type="submit" className="w-full">Publish Article</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'field' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Field Service Appointments</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {appointments.map((a) => (
                <div key={a.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <p className="font-semibold">
                      <span className="font-mono mr-2">{a.appointmentNumber}</span>
                      {a.customerName}
                    </p>
                    <p className="text-muted-foreground">{a.serviceAddress}</p>
                  </div>
                  <Badge variant="info">{a.status}</Badge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Schedule Field Visit</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createAppointment({ customerName: apptCustomer, serviceAddress: apptAddress }).unwrap()
                  setApptCustomer('')
                  setApptAddress('')
                  setFeedback('Field service appointment scheduled.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Customer Name" value={apptCustomer} onChange={(e) => setApptCustomer(e.target.value)} required />
                <Input placeholder="Service Address" value={apptAddress} onChange={(e) => setApptAddress(e.target.value)} required />
                <Button type="submit" className="w-full">Schedule Dispatch</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'slas' && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <ShieldAlert className="h-4 w-4 text-primary" />
              <span>Organization SLA Policies</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {slas.map((s) => (
              <div key={s.id} className="rounded-lg border p-3 text-xs space-y-1">
                <p className="font-semibold">{s.name}</p>
                <Badge variant="outline">{s.priority}</Badge>
                <p className="text-muted-foreground">First Response: {s.firstResponseMinutes}m</p>
                <p className="text-muted-foreground">Resolution: {s.resolutionMinutes}m</p>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  )
}

