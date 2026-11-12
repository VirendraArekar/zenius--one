import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Megaphone, Send, Tag, Users, Workflow } from 'lucide-react'
import {
  useCreateCampaignMutation,
  useCreateCouponMutation,
  useCreateFormMutation,
  useCreateJourneyMutation,
  useCreateSegmentMutation,
  useGetCampaignsQuery,
  useGetCouponsQuery,
  useGetFormsQuery,
  useGetJourneysQuery,
  useGetMarketingDashboardQuery,
  useGetSegmentsQuery,
  useGetSubscribersQuery,
  useLaunchCampaignMutation,
  useSubmitMarketingFormMutation,
  useUpsertSubscriberMutation,
} from '@/app/api/growthAndSupportApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type MktTab = 'campaigns' | 'subscribers' | 'journeys' | 'forms' | 'coupons'

export function MarketingWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<MktTab>('campaigns')
  const [feedback, setFeedback] = useState<string | null>(null)

  const [campName, setCampName] = useState('')
  const [campSubject, setCampSubject] = useState('')
  const [campBody, setCampBody] = useState('')
  const [campSegmentId, setCampSegmentId] = useState('')

  const [subEmail, setSubEmail] = useState('')
  const [subName, setSubName] = useState('')
  const [subTags, setSubTags] = useState('')

  const [segName, setSegName] = useState('')
  const [segTag, setSegTag] = useState('')

  const [journeyName, setJourneyName] = useState('')
  const [journeyTrigger, setJourneyTrigger] = useState('LEAD_CREATED')
  const [journeySteps, setJourneySteps] = useState('')

  const [formName, setFormName] = useState('')
  const [formTitle, setFormTitle] = useState('')
  const [testSubName, setTestSubName] = useState('')
  const [testSubEmail, setTestSubEmail] = useState('')

  const [couponCode, setCouponCode] = useState('')
  const [couponPct, setCouponPct] = useState('15')

  const skip = !accessToken || !user

  const { data: dashboard } = useGetMarketingDashboardQuery(undefined, { skip })
  const { data: campaignsRes } = useGetCampaignsQuery(undefined, { skip })
  const { data: subscribersRes } = useGetSubscribersQuery(undefined, { skip })
  const { data: segments = [] } = useGetSegmentsQuery(undefined, { skip })
  const { data: journeys = [] } = useGetJourneysQuery(undefined, { skip })
  const { data: forms = [] } = useGetFormsQuery(undefined, { skip })
  const { data: coupons = [] } = useGetCouponsQuery(undefined, { skip })

  const [createCampaign] = useCreateCampaignMutation()
  const [launchCampaign] = useLaunchCampaignMutation()
  const [upsertSubscriber] = useUpsertSubscriberMutation()
  const [createSegment] = useCreateSegmentMutation()
  const [createJourney] = useCreateJourneyMutation()
  const [createForm] = useCreateFormMutation()
  const [submitForm] = useSubmitMarketingFormMutation()
  const [createCoupon] = useCreateCouponMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Marketing Automation</CardTitle>
          <CardDescription>Manage email campaigns, segments, journeys, forms, and coupons.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const campaigns = campaignsRes?.data ?? []
  const subscribers = subscribersRes?.data ?? []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Marketing Automation</h1>
        <p className="text-sm text-muted-foreground">
          Campaign orchestration, segmentation, customer journeys, web forms, and promotions
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
            <CardTitle className="text-sm font-medium text-muted-foreground">Active Subscribers</CardTitle>
            <Users className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.subscribedCount ?? 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Campaigns Sent</CardTitle>
            <Megaphone className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.sentCampaigns ?? 0}</div>
            <p className="text-xs text-muted-foreground">{dashboard?.totalDispatched ?? 0} messages dispatched</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Form Captures</CardTitle>
            <Workflow className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.totalFormSubmissions ?? 0}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Active Coupons</CardTitle>
            <Tag className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{dashboard?.activeCoupons ?? 0}</div>
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        {(['campaigns', 'subscribers', 'journeys', 'forms', 'coupons'] as MktTab[]).map((t) => (
          <Button
            key={t}
            size="sm"
            variant={tab === t ? 'default' : 'outline'}
            onClick={() => setTab(t)}
          >
            {t.toUpperCase()}
          </Button>
        ))}
      </div>

      {tab === 'campaigns' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Email Campaigns</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5">
              {campaigns.map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold">{c.name}</span>
                      <Badge variant={c.status === 'SENT' ? 'success' : 'secondary'}>{c.status}</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {c.subject} · Sent: {c.sentCount}
                    </p>
                  </div>
                  {c.status === 'DRAFT' && (
                    <Button
                      size="sm"
                      onClick={async () => {
                        const launched = await launchCampaign(c.id).unwrap()
                        setFeedback(`Launched "${launched.name}" to ${launched.sentCount} subscribers.`)
                      }}
                    >
                      <Send className="h-3.5 w-3.5" />
                      <span>Launch</span>
                    </Button>
                  )}
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Campaign</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createCampaign({
                    segmentId: campSegmentId || undefined,
                    name: campName,
                    subject: campSubject,
                    bodyContent: campBody,
                  }).unwrap()
                  setCampName('')
                  setCampSubject('')
                  setCampBody('')
                  setFeedback('Campaign draft saved.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Campaign Name" value={campName} onChange={(e) => setCampName(e.target.value)} required />
                <select
                  value={campSegmentId}
                  onChange={(e) => setCampSegmentId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">All Active Subscribers</option>
                  {segments.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.matchingSubscribers})
                    </option>
                  ))}
                </select>
                <Input placeholder="Email Subject" value={campSubject} onChange={(e) => setCampSubject(e.target.value)} required />
                <textarea
                  rows={3}
                  placeholder="Email content..."
                  value={campBody}
                  onChange={(e) => setCampBody(e.target.value)}
                  className="w-full rounded-md border border-input bg-background p-2 text-xs"
                  required
                />
                <Button type="submit" className="w-full">Save Draft</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'subscribers' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Subscribers ({subscribers.length}) & Segments ({segments.length})</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-2">
                {segments.map((s) => (
                  <Badge key={s.id} variant="info">
                    {s.name}: {s.matchingSubscribers}
                  </Badge>
                ))}
              </div>
              <div className="space-y-2">
                {subscribers.map((sub) => (
                  <div key={sub.id} className="flex items-center justify-between rounded-lg border p-2.5 text-xs">
                    <div>
                      <p className="font-semibold">{sub.fullName || sub.email}</p>
                      <p className="text-muted-foreground">{sub.email} · {sub.tags || 'no tags'}</p>
                    </div>
                    <Badge variant="success">{sub.status}</Badge>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Add Subscriber</CardTitle>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault()
                    await upsertSubscriber({ email: subEmail, fullName: subName, tags: subTags }).unwrap()
                    setSubEmail('')
                    setSubName('')
                    setSubTags('')
                    setFeedback('Subscriber saved.')
                  }}
                  className="space-y-2"
                >
                  <Input type="email" placeholder="Email" value={subEmail} onChange={(e) => setSubEmail(e.target.value)} required />
                  <Input placeholder="Full Name" value={subName} onChange={(e) => setSubName(e.target.value)} />
                  <Input placeholder="Tags (comma-separated)" value={subTags} onChange={(e) => setSubTags(e.target.value)} />
                  <Button type="submit" className="w-full">Save Subscriber</Button>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Create Segment</CardTitle>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault()
                    await createSegment({ name: segName, filterTag: segTag || undefined }).unwrap()
                    setSegName('')
                    setSegTag('')
                    setFeedback('Audience segment created.')
                  }}
                  className="space-y-2"
                >
                  <Input placeholder="Segment Name" value={segName} onChange={(e) => setSegName(e.target.value)} required />
                  <Input placeholder="Match Tag (e.g. vip)" value={segTag} onChange={(e) => setSegTag(e.target.value)} />
                  <Button type="submit" variant="outline" className="w-full">Create Segment</Button>
                </form>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {tab === 'journeys' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Automated Customer Journeys</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {journeys.map((j) => (
                <div key={j.id} className="rounded-lg border p-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold">{j.name}</span>
                    <Badge variant="success">{j.triggerEvent}</Badge>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{j.stepsSummary}</p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>New Drip Journey</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createJourney({ name: journeyName, triggerEvent: journeyTrigger, stepsSummary: journeySteps }).unwrap()
                  setJourneyName('')
                  setJourneySteps('')
                  setFeedback('Customer journey activated.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Journey Name" value={journeyName} onChange={(e) => setJourneyName(e.target.value)} required />
                <Input placeholder="Trigger (e.g. LEAD_CREATED)" value={journeyTrigger} onChange={(e) => setJourneyTrigger(e.target.value)} required />
                <textarea
                  rows={3}
                  placeholder="Step 1: Welcome Email -> Wait 2d -> Follow-up"
                  value={journeySteps}
                  onChange={(e) => setJourneySteps(e.target.value)}
                  className="w-full rounded-md border border-input bg-background p-2 text-xs"
                  required
                />
                <Button type="submit" className="w-full">Activate Journey</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'forms' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Lead Capture Web Forms</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {forms.map((f) => (
                <div key={f.id} className="rounded-lg border p-3 space-y-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-semibold">{f.title}</p>
                      <p className="text-xs text-muted-foreground">
                        {f.name} · Submissions: {f.submissionsCount} · Auto-CRM Lead: {f.autoCreateLead ? 'Yes' : 'No'}
                      </p>
                    </div>
                    <Badge variant="success">{f.status}</Badge>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Input
                      placeholder="Test Full Name"
                      value={testSubName}
                      onChange={(e) => setTestSubName(e.target.value)}
                      className="max-w-44"
                    />
                    <Input
                      type="email"
                      placeholder="Test Email"
                      value={testSubEmail}
                      onChange={(e) => setTestSubEmail(e.target.value)}
                      className="max-w-48"
                    />
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={async () => {
                        if (!testSubName || !testSubEmail) return
                        await submitForm({ formId: f.id, fullName: testSubName, email: testSubEmail }).unwrap()
                        setTestSubName('')
                        setTestSubEmail('')
                        setFeedback('Form submitted — subscriber & CRM lead updated.')
                      }}
                    >
                      Test Submit
                    </Button>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Web Form</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createForm({ name: formName, title: formTitle, autoCreateLead: true }).unwrap()
                  setFormName('')
                  setFormTitle('')
                  setFeedback('Lead-capture form created.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Internal Name" value={formName} onChange={(e) => setFormName(e.target.value)} required />
                <Input placeholder="Public Form Title" value={formTitle} onChange={(e) => setFormTitle(e.target.value)} required />
                <Button type="submit" className="w-full">Create Form</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'coupons' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Promotional Coupons</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {coupons.map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <span className="font-mono text-sm font-bold">{c.code}</span>
                    <p className="text-xs text-muted-foreground">{c.discountPercent}% off · Redemptions: {c.redemptionCount}</p>
                  </div>
                  <Badge variant={c.active ? 'success' : 'secondary'}>{c.active ? 'ACTIVE' : 'INACTIVE'}</Badge>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Coupon</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createCoupon({ code: couponCode, discountPercent: Number(couponPct) }).unwrap()
                  setCouponCode('')
                  setFeedback('Coupon created.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Code (e.g. LAUNCH20)" value={couponCode} onChange={(e) => setCouponCode(e.target.value)} required />
                <Input type="number" placeholder="Discount %" value={couponPct} onChange={(e) => setCouponPct(e.target.value)} required />
                <Button type="submit" className="w-full">Create Coupon</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

