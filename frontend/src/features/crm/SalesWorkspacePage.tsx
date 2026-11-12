import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  Calculator,
  CheckCircle2,
  FileCheck2,
  Package,
  Search,
  ShoppingCart,
} from 'lucide-react'
import {
  useConvertQuoteToOrderMutation,
  useCreateProductMutation,
  useCreateQuoteMutation,
  useGetAccountsQuery,
  useGetProductsQuery,
  useGetQuotesQuery,
  useGetSalesOrdersQuery,
} from '@/app/api/crmApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type SalesTab = 'quotes' | 'orders' | 'products'

export function SalesWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<SalesTab>('quotes')
  const [feedback, setFeedback] = useState<string | null>(null)

  // Filters & Search
  const [quoteSearch, setQuoteSearch] = useState('')
  const [quoteStatusFilter, setQuoteStatusFilter] = useState('ALL')
  const [orderSearch, setOrderSearch] = useState('')
  const [prodSearch, setProdSearch] = useState('')
  const [prodCategoryFilter, setProdCategoryFilter] = useState('ALL')

  // Product form
  const [sku, setSku] = useState('')
  const [prodName, setProdName] = useState('')
  const [prodCat, setProdCat] = useState('SAAS_LICENSE')
  const [unitPrice, setUnitPrice] = useState('1000')
  const [taxRate, setTaxRate] = useState('18')

  // Dynamic Quote form
  const [quoteSubject, setQuoteSubject] = useState('')
  const [quoteAccountId, setQuoteAccountId] = useState('')
  const [quoteItemName, setQuoteItemName] = useState('Zenius Cloud Module Pack')
  const [quoteQty, setQuoteQty] = useState('5')
  const [quotePrice, setQuotePrice] = useState('450')
  const [quoteDiscount, setQuoteDiscount] = useState('10')
  const [quoteTax, setQuoteTax] = useState('18')

  const skip = !accessToken || !user

  const { data: productsRes } = useGetProductsQuery(undefined, { skip })
  const { data: quotesRes } = useGetQuotesQuery(undefined, { skip })
  const { data: ordersRes } = useGetSalesOrdersQuery(undefined, { skip })
  const { data: accountsRes } = useGetAccountsQuery(undefined, { skip })

  const [createProduct, { isLoading: creatingProd }] = useCreateProductMutation()
  const [createQuote, { isLoading: creatingQuote }] = useCreateQuoteMutation()
  const [convertQuoteToOrder, { isLoading: converting }] = useConvertQuoteToOrderMutation()

  const products = productsRes?.data ?? []
  const quotes = quotesRes?.data ?? []
  const orders = ordersRes?.data ?? []
  const accounts = accountsRes?.data ?? []

  // Dynamic Quote live calculation
  const qtyNum = Math.max(0, Number(quoteQty) || 0)
  const priceNum = Math.max(0, Number(quotePrice) || 0)
  const discPct = Math.min(100, Math.max(0, Number(quoteDiscount) || 0))
  const taxPct = Math.max(0, Number(quoteTax) || 0)

  const rawSubtotal = qtyNum * priceNum
  const discountAmount = (rawSubtotal * discPct) / 100
  const discountedSubtotal = rawSubtotal - discountAmount
  const taxAmount = (discountedSubtotal * taxPct) / 100
  const calculatedGrandTotal = discountedSubtotal + taxAmount

  const accountMap = useMemo(() => {
    const m = new Map<string, string>()
    for (const a of accounts) m.set(a.id, a.name)
    return m
  }, [accounts])

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Sales Commercials</CardTitle>
          <CardDescription>Manage products, customer quotes, and confirmed sales orders.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const filteredQuotes = quotes.filter((q) => {
    const matchText =
      !quoteSearch ||
      q.subject.toLowerCase().includes(quoteSearch.toLowerCase()) ||
      q.quoteNumber.toLowerCase().includes(quoteSearch.toLowerCase()) ||
      (accountMap.get(q.accountId ?? '') || '').toLowerCase().includes(quoteSearch.toLowerCase())
    const matchStatus = quoteStatusFilter === 'ALL' || q.status === quoteStatusFilter
    return matchText && matchStatus
  })

  const filteredOrders = orders.filter((o) => {
    return (
      !orderSearch ||
      o.orderNumber.toLowerCase().includes(orderSearch.toLowerCase()) ||
      o.subject.toLowerCase().includes(orderSearch.toLowerCase())
    )
  })

  const filteredProducts = products.filter((p) => {
    const matchText =
      !prodSearch ||
      p.name.toLowerCase().includes(prodSearch.toLowerCase()) ||
      p.sku.toLowerCase().includes(prodSearch.toLowerCase())
    const matchCat = prodCategoryFilter === 'ALL' || p.category === prodCategoryFilter
    return matchText && matchCat
  })

  const totalOrderValue = orders.reduce((sum, o) => sum + Number(o.grandTotal || 0), 0)
  const totalPipelineQuotes = quotes.reduce((sum, q) => sum + Number(q.grandTotal || 0), 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Sales Commercials & Quote-to-Cash</h1>
          <p className="text-sm text-muted-foreground">
            Multi-currency price lists, real-time quote calculators, tax automation, and 1-click sales order conversion
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to="/crm">CRM Pipelines</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/finance">Invoicing & Billing</Link>
          </Button>
        </div>
      </div>

      {/* KPI Commercial Strip */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Active Quotes</CardTitle>
            <FileCheck2 className="h-4 w-4 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{quotes.length}</div>
            <p className="text-xs text-muted-foreground">${totalPipelineQuotes.toLocaleString()} quoted value</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Confirmed Orders</CardTitle>
            <ShoppingCart className="h-4 w-4 text-emerald-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{orders.length}</div>
            <p className="text-xs text-muted-foreground">${totalOrderValue.toLocaleString()} booked revenue</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Catalog SKUs</CardTitle>
            <Package className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{products.length}</div>
            <p className="text-xs text-muted-foreground">Active catalog products</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Conversion Ratio</CardTitle>
            <CheckCircle2 className="h-4 w-4 text-amber-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {quotes.length > 0 ? `${Math.round((orders.length / quotes.length) * 100)}%` : '—'}
            </div>
            <p className="text-xs text-muted-foreground">Quote to Confirmed Order rate</p>
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
        <Button size="sm" variant={tab === 'quotes' ? 'default' : 'outline'} onClick={() => setTab('quotes')}>
          <FileCheck2 className="mr-1.5 h-3.5 w-3.5" />
          Quotes ({quotes.length})
        </Button>
        <Button size="sm" variant={tab === 'orders' ? 'default' : 'outline'} onClick={() => setTab('orders')}>
          <ShoppingCart className="mr-1.5 h-3.5 w-3.5" />
          Sales Orders ({orders.length})
        </Button>
        <Button size="sm" variant={tab === 'products' ? 'default' : 'outline'} onClick={() => setTab('products')}>
          <Package className="mr-1.5 h-3.5 w-3.5" />
          Products Catalog ({products.length})
        </Button>
      </div>

      {/* TAB 1: DYNAMIC QUOTES */}
      {tab === 'quotes' && (
        <div className="grid gap-6 lg:grid-cols-12">
          {/* Dynamic Interactive Quote Generator */}
          <Card className="lg:col-span-5 border-indigo-200/80">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base">Interactive Quote Builder</CardTitle>
                  <CardDescription>Live real-time line calculation & tax engine</CardDescription>
                </div>
                <Calculator className="h-4 w-4 text-indigo-600" />
              </div>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  if (!quoteSubject.trim()) return
                  try {
                    const created = await createQuote({
                      accountId: quoteAccountId || undefined,
                      subject: quoteSubject.trim(),
                      lines: [
                        {
                          itemName: quoteItemName.trim(),
                          quantity: qtyNum,
                          unitPrice: priceNum,
                          discountPercent: discPct,
                          taxRate: taxPct,
                        },
                      ],
                    }).unwrap()
                    setQuoteSubject('')
                    setFeedback(`Generated quote ${created.quoteNumber} · Grand Total: $${Number(created.grandTotal).toLocaleString()}`)
                  } catch {
                    setFeedback('Failed to generate quote')
                  }
                }}
                className="space-y-3"
              >
                <div>
                  <label className="mb-1 block text-xs font-medium text-muted-foreground">Customer Account</label>
                  <select
                    value={quoteAccountId}
                    onChange={(e) => setQuoteAccountId(e.target.value)}
                    className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                  >
                    <option value="">Select Account (Optional)</option>
                    {accounts.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.name}
                      </option>
                    ))}
                  </select>
                </div>

                <Input
                  placeholder="Proposal Subject (e.g. Annual Enterprise SaaS Pack)"
                  value={quoteSubject}
                  onChange={(e) => setQuoteSubject(e.target.value)}
                  required
                />

                <Input
                  placeholder="Line Item Description"
                  value={quoteItemName}
                  onChange={(e) => setQuoteItemName(e.target.value)}
                  required
                />

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="mb-1 block text-xs font-medium text-muted-foreground">Quantity</label>
                    <Input type="number" min={1} value={quoteQty} onChange={(e) => setQuoteQty(e.target.value)} required />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-medium text-muted-foreground">Unit Price ($)</label>
                    <Input type="number" min={0} value={quotePrice} onChange={(e) => setQuotePrice(e.target.value)} required />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="mb-1 block text-xs font-medium text-muted-foreground">Discount (%)</label>
                    <Input type="number" min={0} max={100} value={quoteDiscount} onChange={(e) => setQuoteDiscount(e.target.value)} />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-medium text-muted-foreground">GST / Tax (%)</label>
                    <Input type="number" min={0} value={quoteTax} onChange={(e) => setQuoteTax(e.target.value)} />
                  </div>
                </div>

                {/* Real-time calculated price breakdown */}
                <div className="rounded-xl border bg-slate-50/90 p-3 text-xs space-y-1.5 dark:bg-muted/30">
                  <div className="flex justify-between text-muted-foreground">
                    <span>Gross Subtotal</span>
                    <span className="font-mono">${rawSubtotal.toFixed(2)}</span>
                  </div>
                  {discountAmount > 0 && (
                    <div className="flex justify-between text-emerald-600">
                      <span>Discount ({discPct}%)</span>
                      <span className="font-mono">-${discountAmount.toFixed(2)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-muted-foreground">
                    <span>Tax ({taxPct}%)</span>
                    <span className="font-mono">+${taxAmount.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between border-t pt-1.5 font-bold text-sm text-foreground">
                    <span>Estimated Total</span>
                    <span className="font-mono text-indigo-600">${calculatedGrandTotal.toFixed(2)}</span>
                  </div>
                </div>

                <Button type="submit" className="w-full" disabled={creatingQuote || !quoteSubject.trim()}>
                  {creatingQuote ? 'Generating...' : 'Create Commercial Quote'}
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Quotes List with Dynamic Actions */}
          <Card className="lg:col-span-7">
            <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardTitle>Quotes Directory ({filteredQuotes.length})</CardTitle>
                <CardDescription>Advance quote status or convert to confirmed sales order</CardDescription>
              </div>
              <div className="flex flex-wrap gap-2">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                  <Input
                    placeholder="Search quotes..."
                    value={quoteSearch}
                    onChange={(e) => setQuoteSearch(e.target.value)}
                    className="h-8 w-36 pl-8 text-xs"
                  />
                </div>
                {(['ALL', 'DRAFT', 'SENT', 'ACCEPTED'] as const).map((s) => (
                  <Button
                    key={s}
                    size="sm"
                    variant={quoteStatusFilter === s ? 'default' : 'outline'}
                    onClick={() => setQuoteStatusFilter(s)}
                  >
                    {s}
                  </Button>
                ))}
              </div>
            </CardHeader>
            <CardContent className="space-y-3 max-h-[560px] overflow-y-auto">
              {filteredQuotes.map((q) => {
                const accountName = accountMap.get(q.accountId ?? '')
                return (
                  <div
                    key={q.id}
                    className="flex flex-col gap-2 rounded-xl border p-3.5 text-xs transition-colors hover:bg-muted/30 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-sm">{q.subject}</span>
                        <Badge variant="outline">{q.quoteNumber}</Badge>
                        <Badge
                          variant={
                            q.status === 'ACCEPTED'
                              ? 'success'
                              : q.status === 'SENT'
                              ? 'info'
                              : 'secondary'
                          }
                        >
                          {q.status}
                        </Badge>
                      </div>
                      <p className="text-muted-foreground">
                        {accountName ? `Account: ${accountName} · ` : ''}Valid: {q.validUntil || 'Standard 30 Days'}
                      </p>
                      <p className="font-mono text-xs">
                        Subtotal: ${Number(q.subtotal).toLocaleString()} · Tax: ${Number(q.taxTotal).toLocaleString()} ·{' '}
                        <span className="font-bold text-indigo-600">${Number(q.grandTotal).toLocaleString()}</span>
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      <Button
                        size="sm"
                        variant="default"
                        disabled={converting}
                        onClick={async () => {
                          try {
                            const so = await convertQuoteToOrder(q.id).unwrap()
                            setFeedback(`Converted quote into Sales Order ${so.orderNumber}!`)
                            setTab('orders')
                          } catch {
                            setFeedback('Failed to convert quote')
                          }
                        }}
                      >
                        <ShoppingCart className="mr-1 h-3.5 w-3.5" /> Convert Order
                      </Button>
                    </div>
                  </div>
                )
              })}
            </CardContent>
          </Card>
        </div>
      )}

      {/* TAB 2: CONFIRMED SALES ORDERS */}
      {tab === 'orders' && (
        <Card>
          <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <CardTitle>Sales Orders & Invoicing Readiness ({filteredOrders.length})</CardTitle>
              <CardDescription>Confirmed customer commitments ready for fulfillment and AR invoicing</CardDescription>
            </div>
            <div className="relative">
              <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
              <Input
                placeholder="Search orders..."
                value={orderSearch}
                onChange={(e) => setOrderSearch(e.target.value)}
                className="h-8 w-48 pl-8 text-xs"
              />
            </div>
          </CardHeader>
          <CardContent className="space-y-3 max-h-[540px] overflow-y-auto">
            {filteredOrders.length === 0 ? (
              <p className="py-8 text-center text-xs text-muted-foreground">No orders match search.</p>
            ) : (
              filteredOrders.map((o) => (
                <div
                  key={o.id}
                  className="flex flex-col gap-2 rounded-xl border p-3.5 text-xs sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-sm">{o.subject}</span>
                      <Badge variant="outline">{o.orderNumber}</Badge>
                      <Badge variant="success">{o.status}</Badge>
                    </div>
                    <p className="mt-1 text-muted-foreground">Date: {o.orderDate}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-sm font-bold text-emerald-600">
                      ${Number(o.grandTotal).toLocaleString()}
                    </span>
                    <Button size="sm" variant="outline" asChild>
                      <Link to="/finance">
                        <span>Invoice</span>
                        <ArrowRight className="ml-1 h-3 w-3" />
                      </Link>
                    </Button>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      )}

      {/* TAB 3: PRODUCTS CATALOG */}
      {tab === 'products' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-4">
            <CardHeader>
              <CardTitle className="text-base">Add Product SKU</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  if (!sku.trim() || !prodName.trim()) return
                  await createProduct({
                    sku: sku.trim(),
                    name: prodName.trim(),
                    category: prodCat,
                    unitPrice: Number(unitPrice),
                    taxRate: Number(taxRate),
                  }).unwrap()
                  setSku('')
                  setProdName('')
                  setFeedback('Product SKU added to catalog.')
                }}
                className="space-y-2.5"
              >
                <Input placeholder="SKU (e.g. ZNS-CLOUD-01)" value={sku} onChange={(e) => setSku(e.target.value)} required />
                <Input placeholder="Product Name" value={prodName} onChange={(e) => setProdName(e.target.value)} required />
                <select
                  value={prodCat}
                  onChange={(e) => setProdCat(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="SAAS_LICENSE">SaaS License</option>
                  <option value="PROFESSIONAL_SERVICES">Professional Services</option>
                  <option value="HARDWARE">Hardware Appliance</option>
                  <option value="SUPPORT_TIER">Support Tier</option>
                </select>
                <div className="grid grid-cols-2 gap-2">
                  <Input type="number" placeholder="Unit Price ($)" value={unitPrice} onChange={(e) => setUnitPrice(e.target.value)} required />
                  <Input type="number" placeholder="Tax Rate (%)" value={taxRate} onChange={(e) => setTaxRate(e.target.value)} required />
                </div>
                <Button type="submit" className="w-full" disabled={creatingProd}>
                  {creatingProd ? 'Saving...' : 'Add Product'}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-8">
            <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardTitle>Catalog Items ({filteredProducts.length})</CardTitle>
              </div>
              <div className="flex gap-2">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                  <Input
                    placeholder="Search SKU or name..."
                    value={prodSearch}
                    onChange={(e) => setProdSearch(e.target.value)}
                    className="h-8 w-40 pl-8 text-xs"
                  />
                </div>
                <select
                  value={prodCategoryFilter}
                  onChange={(e) => setProdCategoryFilter(e.target.value)}
                  className="h-8 rounded-md border border-input bg-background px-2 text-xs"
                >
                  <option value="ALL">All Categories</option>
                  <option value="SAAS_LICENSE">SaaS License</option>
                  <option value="PROFESSIONAL_SERVICES">Services</option>
                  <option value="HARDWARE">Hardware</option>
                  <option value="SUPPORT_TIER">Support</option>
                </select>
              </div>
            </CardHeader>
            <CardContent className="space-y-2 max-h-[500px] overflow-y-auto">
              {filteredProducts.map((p) => (
                <div key={p.id} className="flex items-center justify-between rounded-xl border p-3 text-xs">
                  <div>
                    <p className="font-semibold text-sm">
                      <span className="font-mono mr-2 text-xs text-muted-foreground">{p.sku}</span>
                      {p.name}
                    </p>
                    <p className="text-muted-foreground">Category: {p.category} · Tax: {p.taxRate}%</p>
                  </div>
                  <div className="text-right">
                    <span className="font-mono text-sm font-bold text-indigo-600">${Number(p.unitPrice).toLocaleString()}</span>
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
