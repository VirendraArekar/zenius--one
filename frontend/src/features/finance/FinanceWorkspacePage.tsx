import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CreditCard, DollarSign, FileText, Landmark, Receipt, TrendingUp } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import {
  useCancelSubscriptionMutation,
  useCreateFinBillMutation,
  useCreateFinInvoiceMutation,
  useCreatePlanMutation,
  useCreateSubscriptionMutation,
  useGetFinAccountsQuery,
  useGetFinBillsQuery,
  useGetFinInvoicesQuery,
  useGetFinancialReportsQuery,
  useGetPlansQuery,
  useGetSubscriptionsQuery,
  usePayFinBillMutation,
  useRecordInvoicePaymentMutation,
} from '@/app/api/erpApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable'
import { Input } from '@/components/ui/Input'
import { chartTooltipStyle } from '@/components/ui/chartThemes'

type FinTab = 'reports' | 'invoices' | 'bills' | 'coa' | 'billing'

export function FinanceWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<FinTab>('reports')
  const [feedback, setFeedback] = useState<string | null>(null)

  const [invCustomer, setInvCustomer] = useState('')
  const [invSubtotal, setInvSubtotal] = useState('1000')
  const [invGst, setInvGst] = useState('18')
  const [invSearch, setInvSearch] = useState('')
  const [invStatusFilter, setInvStatusFilter] = useState('ALL')

  const [billVendor, setBillVendor] = useState('')
  const [billSubtotal, setBillSubtotal] = useState('500')
  const [billTds, setBillTds] = useState('10')

  const [planCode, setPlanCode] = useState('')
  const [planName, setPlanName] = useState('')
  const [planPrice, setPlanPrice] = useState('99')
  const [subPlanId, setSubPlanId] = useState('')
  const [subCustomer, setSubCustomer] = useState('')
  const [subEmail, setSubEmail] = useState('')

  const skip = !accessToken || !user

  const { data: reports } = useGetFinancialReportsQuery(undefined, { skip })
  const { data: accounts = [] } = useGetFinAccountsQuery(undefined, { skip })
  const { data: invoicesRes } = useGetFinInvoicesQuery(undefined, { skip })
  const { data: billsRes } = useGetFinBillsQuery(undefined, { skip })
  const { data: plans = [] } = useGetPlansQuery(undefined, { skip })
  const { data: subscriptions = [] } = useGetSubscriptionsQuery(undefined, { skip })

  const [createInvoice] = useCreateFinInvoiceMutation()
  const [recordInvoicePayment] = useRecordInvoicePaymentMutation()
  const [createBill] = useCreateFinBillMutation()
  const [payBill] = usePayFinBillMutation()
  const [createPlan] = useCreatePlanMutation()
  const [createSubscription] = useCreateSubscriptionMutation()
  const [cancelSubscription] = useCancelSubscriptionMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Finance & Billing</CardTitle>
          <CardDescription>Manage double-entry ledgers, GST/TDS invoices, vendor bills, and subscriptions.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const invoices = invoicesRes?.data ?? []
  const bills = billsRes?.data ?? []

  const trialCols: DataTableColumn<NonNullable<typeof reports>['trialBalance'][number]>[] = [
    { key: 'code', header: 'Code', render: (r) => <span className="font-mono text-xs font-semibold">{r.code}</span> },
    { key: 'name', header: 'Account', render: (r) => <span className="text-xs font-medium">{r.name}</span> },
    { key: 'type', header: 'Type', render: (r) => <Badge variant="outline">{r.accountType}</Badge> },
    { key: 'debit', header: 'Debit', render: (r) => <span className="font-mono text-xs">${Number(r.debit).toLocaleString()}</span> },
    { key: 'credit', header: 'Credit', render: (r) => <span className="font-mono text-xs">${Number(r.credit).toLocaleString()}</span> },
    { key: 'net', header: 'Net Balance', render: (r) => <span className="font-mono text-xs font-semibold">${Number(r.netBalance).toLocaleString()}</span> },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Finance, Accounting & Subscriptions</h1>
        <p className="text-sm text-muted-foreground">
          Double-entry GL, Accounts Receivable/Payable, India GST/TDS, and recurring subscription billing
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
        <Card className="card-motion">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Revenue</CardTitle>
            <DollarSign className="h-4 w-4 text-emerald-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-foreground">${Number(reports?.totalRevenue ?? 0).toLocaleString()}</div>
            <p className="mt-0.5 text-xs text-muted-foreground">Net Profit: ${Number(reports?.netProfit ?? 0).toLocaleString()}</p>
          </CardContent>
        </Card>
        <Card className="card-motion">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">AR Outstanding</CardTitle>
            <FileText className="h-4 w-4 text-indigo-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-foreground">${Number(reports?.arOutstanding ?? 0).toLocaleString()}</div>
            <p className="mt-0.5 text-xs text-muted-foreground">Uncollected receivables</p>
          </CardContent>
        </Card>
        <Card className="card-motion">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">AP Outstanding</CardTitle>
            <Receipt className="h-4 w-4 text-amber-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-foreground">${Number(reports?.apOutstanding ?? 0).toLocaleString()}</div>
            <p className="mt-0.5 text-xs text-muted-foreground">Vendor bills due</p>
          </CardContent>
        </Card>
        <Card className="card-motion">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">India GST & TDS</CardTitle>
            <Landmark className="h-4 w-4 text-cyan-500" />
          </CardHeader>
          <CardContent>
            <div className="text-lg font-bold text-foreground">GST: ${Number(reports?.totalGstCollected ?? 0).toLocaleString()}</div>
            <p className="mt-0.5 text-xs text-muted-foreground">TDS Deducted: ${Number(reports?.totalTdsDeducted ?? 0).toLocaleString()}</p>
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        {(['reports', 'invoices', 'bills', 'coa', 'billing'] as FinTab[]).map((t) => (
          <Button key={t} size="sm" variant={tab === t ? 'default' : 'outline'} onClick={() => setTab(t)} className="transition-all hover:scale-105">
            {t === 'coa' ? 'CHART OF ACCOUNTS' : t === 'billing' ? 'SUBSCRIPTIONS' : t.toUpperCase()}
          </Button>
        ))}
      </div>

      {tab === 'reports' && (
        <div className="space-y-6 animate-enter-slide">
          <Card className="card-motion">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-primary" />
                Financial Liquidity & Revenue Spectrum
              </CardTitle>
              <CardDescription>Live real-time position across revenue, receivables, payables, and taxes</CardDescription>
            </CardHeader>
            <CardContent className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={[
                    { name: 'Revenue', amount: Number(reports?.totalRevenue ?? 0), color: '#10b981' },
                    { name: 'Net Profit', amount: Number(reports?.netProfit ?? 0), color: '#06b6d4' },
                    { name: 'AR (Receivable)', amount: Number(reports?.arOutstanding ?? 0), color: '#6366f1' },
                    { name: 'AP (Payable)', amount: Number(reports?.apOutstanding ?? 0), color: '#f59e0b' },
                    { name: 'GST Collected', amount: Number(reports?.totalGstCollected ?? 0), color: '#8b5cf6' },
                  ]}
                >
                  <CartesianGrid strokeDasharray="3 3" opacity={0.2} />
                  <XAxis dataKey="name" fontSize={11} stroke="currentColor" opacity={0.7} />
                  <YAxis fontSize={11} stroke="currentColor" opacity={0.7} />
                  <Tooltip contentStyle={chartTooltipStyle} />
                  <Bar dataKey="amount" radius={[5, 5, 0, 0]} animationDuration={800}>
                    {[
                      { name: 'Revenue', color: '#10b981' },
                      { name: 'Net Profit', color: '#06b6d4' },
                      { name: 'AR (Receivable)', color: '#6366f1' },
                      { name: 'AP (Payable)', color: '#f59e0b' },
                      { name: 'GST Collected', color: '#8b5cf6' },
                    ].map((entry, idx) => (
                      <Cell key={`fin-bar-${idx}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Live Trial Balance</h2>
              <Badge variant="success">
                Debits ${Number(reports?.totalDebits ?? 0).toLocaleString()} = Credits ${Number(reports?.totalCredits ?? 0).toLocaleString()}
              </Badge>
            </div>
            <DataTable columns={trialCols} data={reports?.trialBalance ?? []} rowKey={(r) => r.accountId} />
          </div>
        </div>
      )}

      {tab === 'invoices' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <CardTitle>Accounts Receivable — Customer Invoices ({invoices.length})</CardTitle>
              <div className="flex flex-wrap gap-1.5">
                <Input
                  placeholder="Search invoice or customer..."
                  value={invSearch}
                  onChange={(e) => setInvSearch(e.target.value)}
                  className="h-8 w-40 text-xs"
                />
                {(['ALL', 'SENT', 'OVERDUE', 'PAID'] as const).map((st) => (
                  <Button
                    key={st}
                    size="sm"
                    variant={invStatusFilter === st ? 'default' : 'outline'}
                    onClick={() => setInvStatusFilter(st)}
                  >
                    {st}
                  </Button>
                ))}
              </div>
            </CardHeader>
            <CardContent className="space-y-2.5 max-h-[500px] overflow-y-auto">
              {invoices
                .filter((inv) => {
                  const q = invSearch.trim().toLowerCase()
                  const matchText =
                    !q ||
                    inv.invoiceNumber.toLowerCase().includes(q) ||
                    inv.customerName.toLowerCase().includes(q)
                  const matchStatus = invStatusFilter === 'ALL' || inv.status === invStatusFilter
                  return matchText && matchStatus
                })
                .map((inv) => (
                  <div key={inv.id} className="flex items-center justify-between rounded-lg border p-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold">{inv.invoiceNumber}</span>
                        <span className="text-sm font-semibold">{inv.customerName}</span>
                        <Badge variant={inv.status === 'PAID' ? 'success' : inv.status === 'OVERDUE' ? 'warning' : 'info'}>
                          {inv.status}
                        </Badge>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        Total: ${Number(inv.totalAmount).toLocaleString()} (GST ${Number(inv.gstAmount).toLocaleString()}) · Balance Due: ${Number(inv.balanceDue).toLocaleString()}
                      </p>
                    </div>
                    {inv.balanceDue > 0 && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={async () => {
                          await recordInvoicePayment({ id: inv.id, amount: inv.balanceDue }).unwrap()
                          setFeedback(`Recorded payment for ${inv.invoiceNumber}.`)
                        }}
                      >
                        Mark Paid
                      </Button>
                    )}
                  </div>
                ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Issue Customer Invoice</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createInvoice({
                    customerName: invCustomer,
                    subtotal: Number(invSubtotal),
                    gstRate: Number(invGst),
                  }).unwrap()
                  setInvCustomer('')
                  setFeedback('Invoice created and posted to General Ledger.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Customer Name" value={invCustomer} onChange={(e) => setInvCustomer(e.target.value)} required />
                <Input type="number" placeholder="Subtotal" value={invSubtotal} onChange={(e) => setInvSubtotal(e.target.value)} required />
                <Input type="number" placeholder="GST Rate (%)" value={invGst} onChange={(e) => setInvGst(e.target.value)} />
                <div className="rounded-lg border bg-slate-50 p-2.5 text-xs space-y-1 dark:bg-muted/30">
                  <div className="flex justify-between text-muted-foreground">
                    <span>Tax ({Number(invGst) || 0}%)</span>
                    <span className="font-mono">${(((Number(invSubtotal) || 0) * (Number(invGst) || 0)) / 100).toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between font-bold">
                    <span>Total Invoice Amount</span>
                    <span className="font-mono text-indigo-600">
                      ${((Number(invSubtotal) || 0) * (1 + (Number(invGst) || 0) / 100)).toFixed(2)}
                    </span>
                  </div>
                </div>
                <Button type="submit" className="w-full">Create & Post Invoice</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'bills' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Accounts Payable — Vendor Bills</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5">
              {bills.map((b) => (
                <div key={b.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold">{b.billNumber}</span>
                      <span className="text-sm font-semibold">{b.vendorName}</span>
                      <Badge variant={b.status === 'PAID' ? 'success' : 'warning'}>{b.status}</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Payable: ${Number(b.totalAmount).toLocaleString()} (TDS ${Number(b.tdsAmount).toLocaleString()}) · Due: ${Number(b.balanceDue).toLocaleString()}
                    </p>
                  </div>
                  {b.balanceDue > 0 && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={async () => {
                        await payBill({ id: b.id, amount: b.balanceDue }).unwrap()
                        setFeedback(`Paid vendor bill ${b.billNumber}.`)
                      }}
                    >
                      Pay Bill
                    </Button>
                  )}
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Record Vendor Bill</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  await createBill({
                    vendorName: billVendor,
                    subtotal: Number(billSubtotal),
                    tdsRate: Number(billTds),
                  }).unwrap()
                  setBillVendor('')
                  setFeedback('Vendor bill recorded and posted to General Ledger.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="Vendor Name" value={billVendor} onChange={(e) => setBillVendor(e.target.value)} required />
                <Input type="number" placeholder="Subtotal" value={billSubtotal} onChange={(e) => setBillSubtotal(e.target.value)} required />
                <Input type="number" placeholder="TDS Rate (%)" value={billTds} onChange={(e) => setBillTds(e.target.value)} />
                <Button type="submit" className="w-full">Save Vendor Bill</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'coa' && (
        <Card>
          <CardHeader>
            <CardTitle>Chart of Accounts</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {accounts.map((acc) => (
              <div key={acc.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                <div>
                  <span className="font-mono font-bold mr-2">{acc.code}</span>
                  <span className="font-medium">{acc.name}</span>
                </div>
                <Badge variant="outline">{acc.accountType}</Badge>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {tab === 'billing' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-4 w-4 text-primary" />
                <span>Subscriptions ({subscriptions.length})</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2.5">
              {subscriptions.map((s) => (
                <div key={s.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <p className="font-semibold">
                      <span className="font-mono mr-2">{s.subscriptionNumber}</span>
                      {s.customerName} — {s.planName} (${s.planPrice})
                    </p>
                    <p className="text-muted-foreground">{s.customerEmail} · Renews: {s.currentPeriodEnd}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={s.status === 'ACTIVE' ? 'success' : s.status === 'TRIALING' ? 'info' : 'secondary'}>
                      {s.status}
                    </Badge>
                    {s.status !== 'CANCELLED' && (
                      <Button size="sm" variant="ghost" onClick={() => cancelSubscription(s.id)}>
                        Cancel
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Create Plan</CardTitle>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault()
                    await createPlan({ code: planCode, name: planName, price: Number(planPrice), trialDays: 14 }).unwrap()
                    setPlanCode('')
                    setPlanName('')
                    setFeedback('Subscription plan created.')
                  }}
                  className="space-y-2"
                >
                  <Input placeholder="Code (e.g. PRO)" value={planCode} onChange={(e) => setPlanCode(e.target.value)} required />
                  <Input placeholder="Plan Name" value={planName} onChange={(e) => setPlanName(e.target.value)} required />
                  <Input type="number" placeholder="Price" value={planPrice} onChange={(e) => setPlanPrice(e.target.value)} required />
                  <Button type="submit" className="w-full">Save Plan</Button>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">New Subscription</CardTitle>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault()
                    if (!subPlanId && !plans[0]?.id) return
                    await createSubscription({
                      planId: subPlanId || plans[0].id,
                      customerName: subCustomer,
                      customerEmail: subEmail,
                    }).unwrap()
                    setSubCustomer('')
                    setSubEmail('')
                    setFeedback('Subscription activated.')
                  }}
                  className="space-y-2"
                >
                  <select
                    value={subPlanId}
                    onChange={(e) => setSubPlanId(e.target.value)}
                    className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                  >
                    {plans.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} (${p.price})
                      </option>
                    ))}
                  </select>
                  <Input placeholder="Customer Name" value={subCustomer} onChange={(e) => setSubCustomer(e.target.value)} required />
                  <Input type="email" placeholder="Customer Email" value={subEmail} onChange={(e) => setSubEmail(e.target.value)} required />
                  <Button type="submit" variant="outline" className="w-full">Subscribe</Button>
                </form>
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  )
}

