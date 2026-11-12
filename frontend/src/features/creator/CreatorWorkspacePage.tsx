import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Blocks, Database, Rocket } from 'lucide-react'
import {
  useCreateCreatorAppMutation,
  useCreateCreatorFormMutation,
  useGetCreatorAppsQuery,
  useGetCreatorFormsQuery,
  useGetCreatorRecordsQuery,
  usePublishCreatorAppMutation,
  useSubmitCreatorRecordMutation,
} from '@/app/api/intelligenceApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

export function CreatorWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [selectedAppId, setSelectedAppId] = useState('')
  const [selectedFormId, setSelectedFormId] = useState('')
  const [appName, setAppName] = useState('')
  const [appDesc, setAppDesc] = useState('')
  const [formName, setFormName] = useState('')
  const [fieldTitle, setFieldTitle] = useState('Item')
  const [qtyInput, setQtyInput] = useState('1')
  const [priceInput, setPriceInput] = useState('100')
  const [itemInput, setItemInput] = useState('')
  const [feedback, setFeedback] = useState<string | null>(null)

  const skip = !accessToken || !user
  const { data: apps = [] } = useGetCreatorAppsQuery(undefined, { skip })
  const activeAppId = selectedAppId || apps[0]?.id || ''
  const { data: forms = [] } = useGetCreatorFormsQuery(activeAppId || undefined, { skip })
  const activeFormId = selectedFormId || forms[0]?.id || ''
  const { data: records = [] } = useGetCreatorRecordsQuery(activeFormId, { skip: skip || !activeFormId })

  const [createApp] = useCreateCreatorAppMutation()
  const [publishApp] = usePublishCreatorAppMutation()
  const [createForm] = useCreateCreatorFormMutation()
  const [submitRecord] = useSubmitCreatorRecordMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Low-Code Creator</CardTitle>
          <CardDescription>Build custom applications, forms with formula fields, and live databases.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleCreateApp = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!appName.trim()) return
    try {
      const created = await createApp({
        name: appName.trim(),
        description: appDesc.trim() || undefined,
      }).unwrap()
      setSelectedAppId(created.id)
      setAppName('')
      setAppDesc('')
      setFeedback(`Created app ${created.appCode}: ${created.name}`)
    } catch {
      setFeedback('Failed to create app')
    }
  }

  const handleCreateForm = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!activeAppId || !formName.trim()) return
    try {
      const created = await createForm({
        appId: activeAppId,
        name: formName.trim(),
        fields: [
          { key: 'title', label: fieldTitle || 'Title', type: 'TEXT', required: true },
          { key: 'qty', label: 'Quantity', type: 'NUMBER', required: true },
          { key: 'unitPrice', label: 'Unit Price', type: 'NUMBER', required: true },
          { key: 'total', label: 'Computed Total', type: 'FORMULA', required: false, formula: 'qty * unitPrice' },
        ],
      }).unwrap()
      setSelectedFormId(created.id)
      setFormName('')
      setFeedback(`Created form ${created.formCode} with formula fields`)
    } catch {
      setFeedback('Failed to create form')
    }
  }

  const handleSubmitRecord = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!activeFormId || !itemInput.trim()) return
    try {
      const rec = await submitRecord({
        formId: activeFormId,
        data: {
          title: itemInput.trim(),
          qty: Number(qtyInput) || 1,
          unitPrice: Number(priceInput) || 0,
        },
      }).unwrap()
      setItemInput('')
      setFeedback(`Submitted record ${rec.recordNumber}`)
    } catch {
      setFeedback('Failed to submit record')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Low-Code Creator Studio</h1>
          <p className="text-sm text-muted-foreground">
            Build custom business apps, dynamic forms with server-evaluated formulas, and publish versioned releases
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
              <Blocks className="h-4 w-4 text-primary" /> Custom Applications
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <form onSubmit={handleCreateApp} className="space-y-2">
              <Input placeholder="App name" value={appName} onChange={(e) => setAppName(e.target.value)} />
              <Input placeholder="Description" value={appDesc} onChange={(e) => setAppDesc(e.target.value)} />
              <Button type="submit" className="w-full" size="sm">
                Create Low-Code App
              </Button>
            </form>
            <div className="space-y-2">
              {apps.map((app) => (
                <div
                  key={app.id}
                  className={`rounded-lg border p-3 text-xs ${
                    app.id === activeAppId ? 'border-primary bg-primary/5' : ''
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <button
                      type="button"
                      onClick={() => setSelectedAppId(app.id)}
                      className="text-left font-semibold text-sm"
                    >
                      {app.appCode} — {app.name}
                    </button>
                    <Badge variant={app.status === 'PUBLISHED' ? 'default' : 'secondary'}>
                      {app.status} v{app.appVersion}
                    </Badge>
                  </div>
                  <div className="mt-2 flex justify-end">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={async () => {
                        const res = await publishApp(app.id).unwrap()
                        setFeedback(`Published ${res.appCode} v${res.appVersion}`)
                      }}
                    >
                      <Rocket className="mr-1 h-3 w-3" /> Publish
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Form & Schema Builder</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <form onSubmit={handleCreateForm} className="space-y-2">
              <Input placeholder="Form name" value={formName} onChange={(e) => setFormName(e.target.value)} />
              <Input
                placeholder="Primary field label"
                value={fieldTitle}
                onChange={(e) => setFieldTitle(e.target.value)}
              />
              <Button type="submit" className="w-full" size="sm" disabled={!activeAppId}>
                Create Form + Formula Total
              </Button>
            </form>
            <div className="space-y-2">
              {forms.map((f) => (
                <button
                  key={f.id}
                  type="button"
                  onClick={() => setSelectedFormId(f.id)}
                  className={`block w-full rounded-lg border p-3 text-left text-xs ${
                    f.id === activeFormId ? 'border-primary bg-primary/5' : ''
                  }`}
                >
                  <p className="font-semibold text-sm">
                    {f.formCode} — {f.name}
                  </p>
                  <p className="text-muted-foreground">{f.fields.length} schema fields</p>
                </button>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Database className="h-4 w-4 text-primary" /> Custom Records ({records.length})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <form onSubmit={handleSubmitRecord} className="space-y-2">
              <Input
                placeholder="Record title / item"
                value={itemInput}
                onChange={(e) => setItemInput(e.target.value)}
              />
              <div className="grid grid-cols-2 gap-2">
                <Input type="number" min={1} value={qtyInput} onChange={(e) => setQtyInput(e.target.value)} />
                <Input type="number" min={0} value={priceInput} onChange={(e) => setPriceInput(e.target.value)} />
              </div>
              <Button type="submit" className="w-full" size="sm" disabled={!activeFormId}>
                Submit Record
              </Button>
            </form>
            <div className="space-y-2">
              {records.map((r) => (
                <div key={r.id} className="rounded-lg border p-2.5 text-xs">
                  <div className="flex items-center justify-between font-medium">
                    <span>{r.recordNumber}</span>
                    <span>{r.submittedBy}</span>
                  </div>
                  <pre className="mt-1 overflow-x-auto text-[11px] text-muted-foreground">
                    {JSON.stringify(r.data)}
                  </pre>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

