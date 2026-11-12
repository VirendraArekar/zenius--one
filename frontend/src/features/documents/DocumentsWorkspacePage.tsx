import { useState } from 'react'
import { Link } from 'react-router-dom'
import { FileText, FolderOpen, Scale, Signature } from 'lucide-react'
import {
  useCreateContractMutation,
  useCreateFileMutation,
  useCreateFolderMutation,
  useGetContractsQuery,
  useGetFileDetailQuery,
  useGetFilesQuery,
  useGetFoldersQuery,
  useGetSignaturesQuery,
  useRequestSignatureMutation,
  useSignDocumentMutation,
  useUpdateContractStatusMutation,
  useUpdateFileMutation,
} from '@/app/api/workplaceApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type DocsTab = 'drive' | 'signatures' | 'legal'

export function DocumentsWorkspacePage({ initialTab = 'drive' }: { initialTab?: DocsTab }) {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<DocsTab>(initialTab)
  const [selectedFolderId, setSelectedFolderId] = useState<string>('')
  const [selectedFileId, setSelectedFileId] = useState<string>('')
  const [feedback, setFeedback] = useState<string | null>(null)

  const [folderName, setFolderName] = useState('')
  const [docTitle, setDocTitle] = useState('')
  const [docType, setDocType] = useState('WORD')
  const [docBody, setDocBody] = useState('')
  const [editBody, setEditBody] = useState('')

  const [signerName, setSignerName] = useState('')
  const [signerEmail, setSignerEmail] = useState('')

  const [ctrTitle, setCtrTitle] = useState('')
  const [ctrCounterparty, setCtrCounterparty] = useState('')
  const [ctrType, setCtrType] = useState('MSA')
  const [ctrValue, setCtrValue] = useState('50000')

  const skip = !accessToken || !user

  const { data: folders = [] } = useGetFoldersQuery(undefined, { skip })
  const activeFolderId = selectedFolderId || folders[0]?.id || ''
  const { data: files = [] } = useGetFilesQuery(activeFolderId || undefined, { skip })
  const activeFileId = selectedFileId || files[0]?.id || ''
  const { data: fileDetail } = useGetFileDetailQuery(activeFileId, { skip: skip || !activeFileId })
  const { data: signatures = [] } = useGetSignaturesQuery(undefined, { skip })
  const { data: contracts = [] } = useGetContractsQuery(undefined, { skip })

  const [createFolder] = useCreateFolderMutation()
  const [createFile] = useCreateFileMutation()
  const [updateFile] = useUpdateFileMutation()
  const [requestSignature] = useRequestSignatureMutation()
  const [signDocument] = useSignDocumentMutation()
  const [createContract] = useCreateContractMutation()
  const [updateContractStatus] = useUpdateContractStatusMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access WorkDrive, Office & Legal</CardTitle>
          <CardDescription>Manage shared folders, Office revisions, e-Signatures, and legal contracts.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleCreateFolder = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!folderName.trim()) return
    try {
      const f = await createFolder({ name: folderName.trim(), shared: true }).unwrap()
      setSelectedFolderId(f.id)
      setFolderName('')
      setFeedback(`Created folder ${f.name}`)
    } catch {
      setFeedback('Failed to create folder')
    }
  }

  const handleCreateFile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!docTitle.trim()) return
    try {
      const created = await createFile({
        folderId: activeFolderId || undefined,
        title: docTitle.trim(),
        docType,
        contentBody: docBody,
      }).unwrap()
      setSelectedFileId(created.file.id)
      setEditBody(created.contentBody)
      setDocTitle('')
      setDocBody('')
      setFeedback(`Created document ${created.file.title} (v1)`)
    } catch {
      setFeedback('Failed to create document')
    }
  }

  const handleSaveRevision = async () => {
    if (!activeFileId) return
    try {
      const updated = await updateFile({
        id: activeFileId,
        contentBody: editBody || fileDetail?.contentBody || '',
      }).unwrap()
      setFeedback(`Saved revision v${updated.file.currentVersion}`)
    } catch {
      setFeedback('Failed to save revision')
    }
  }

  const handleRequestSignature = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!activeFileId || !signerName.trim() || !signerEmail.trim()) return
    try {
      await requestSignature({
        fileId: activeFileId,
        signerName: signerName.trim(),
        signerEmail: signerEmail.trim(),
      }).unwrap()
      setSignerName('')
      setSignerEmail('')
      setFeedback('Signature request dispatched')
    } catch {
      setFeedback('Failed to create signature request')
    }
  }

  const handleSign = async (signatureId: string) => {
    try {
      const signed = await signDocument(signatureId).unwrap()
      setFeedback(`Digitally signed (${signed.signatureHash?.slice(0, 12)}...)`)
    } catch {
      setFeedback('Failed to sign document')
    }
  }

  const handleCreateContract = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!ctrTitle.trim() || !ctrCounterparty.trim()) return
    const today = new Date().toISOString().slice(0, 10)
    const nextYear = new Date(Date.now() + 365 * 86400000).toISOString().slice(0, 10)
    try {
      const created = await createContract({
        title: ctrTitle.trim(),
        counterpartyName: ctrCounterparty.trim(),
        contractType: ctrType,
        valueAmount: Number(ctrValue) || 0,
        effectiveDate: today,
        expirationDate: nextYear,
        documentId: activeFileId || undefined,
      }).unwrap()
      setCtrTitle('')
      setCtrCounterparty('')
      setFeedback(`Created contract ${created.contractNumber}`)
    } catch {
      setFeedback('Failed to create contract')
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">WorkDrive, Office & Legal CLM</h1>
        <p className="text-sm text-muted-foreground">
          Cloud storage folders, revision-controlled Office documents, cryptographic e-Signatures, and legal contracts
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

      <div className="flex gap-2 border-b pb-2">
        <Button variant={tab === 'drive' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('drive')}>
          <FolderOpen className="mr-1.5 h-4 w-4" /> WorkDrive & Office ({files.length})
        </Button>
        <Button variant={tab === 'signatures' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('signatures')}>
          <Signature className="mr-1.5 h-4 w-4" /> e-Signatures ({signatures.length})
        </Button>
        <Button variant={tab === 'legal' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('legal')}>
          <Scale className="mr-1.5 h-4 w-4" /> Legal Contracts ({contracts.length})
        </Button>
      </div>

      {tab === 'drive' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle>Folders</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <form onSubmit={handleCreateFolder} className="flex gap-2">
                  <Input
                    placeholder="New folder"
                    value={folderName}
                    onChange={(e) => setFolderName(e.target.value)}
                  />
                  <Button type="submit" size="sm">
                    Create
                  </Button>
                </form>
                <div className="space-y-1">
                  {folders.map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      onClick={() => setSelectedFolderId(f.id)}
                      className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm ${
                        f.id === activeFolderId ? 'bg-primary text-primary-foreground' : 'hover:bg-muted'
                      }`}
                    >
                      <span>{f.name}</span>
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>New Office Document</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleCreateFile} className="space-y-3">
                  <Input placeholder="Document title" value={docTitle} onChange={(e) => setDocTitle(e.target.value)} />
                  <select
                    aria-label="Document Type"
                    className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                    value={docType}
                    onChange={(e) => setDocType(e.target.value)}
                  >
                    <option value="WORD">WORD (Writer)</option>
                    <option value="SHEET">SHEET (Spreadsheet)</option>
                    <option value="SLIDE">SLIDE (Presentation)</option>
                    <option value="NOTE">NOTE</option>
                  </select>
                  <textarea
                    className="min-h-[80px] w-full rounded-md border border-input bg-background p-2 text-sm"
                    placeholder="Initial content..."
                    value={docBody}
                    onChange={(e) => setDocBody(e.target.value)}
                  />
                  <Button type="submit" className="w-full">
                    Create Document
                  </Button>
                </form>
              </CardContent>
            </Card>
          </div>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileText className="h-5 w-5" /> Documents & Live Revision Editor
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-2">
                {files.map((file) => (
                  <Button
                    key={file.id}
                    size="sm"
                    variant={file.id === activeFileId ? 'default' : 'outline'}
                    onClick={() => {
                      setSelectedFileId(file.id)
                      setEditBody('')
                    }}
                  >
                    {file.title} (v{file.currentVersion})
                  </Button>
                ))}
              </div>

              {fileDetail && (
                <div className="space-y-3 rounded-lg border p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-semibold">{fileDetail.file.title}</p>
                      <p className="text-xs text-muted-foreground">
                        {fileDetail.file.docType} · v{fileDetail.file.currentVersion} · Retention:{' '}
                        {fileDetail.file.retentionPolicy}
                      </p>
                    </div>
                    <Button size="sm" onClick={handleSaveRevision}>
                      Save New Revision
                    </Button>
                  </div>
                  <textarea
                    className="min-h-[140px] w-full rounded-md border border-input bg-background p-3 text-sm"
                    value={editBody || fileDetail.contentBody}
                    onChange={(e) => setEditBody(e.target.value)}
                  />
                  <div className="space-y-1 text-xs text-muted-foreground">
                    <p className="font-medium text-foreground">Revision History ({fileDetail.revisions.length})</p>
                    {fileDetail.revisions.map((r) => (
                      <div key={r.id}>
                        v{r.revisionNumber} by {r.editorName}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'signatures' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>Request e-Signature</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleRequestSignature} className="space-y-3">
                <select
                  aria-label="Document"
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                  value={activeFileId}
                  onChange={(e) => setSelectedFileId(e.target.value)}
                >
                  {files.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.title} (v{f.currentVersion})
                    </option>
                  ))}
                </select>
                <Input placeholder="Signer name" value={signerName} onChange={(e) => setSignerName(e.target.value)} />
                <Input
                  type="email"
                  placeholder="signer@company.com"
                  value={signerEmail}
                  onChange={(e) => setSignerEmail(e.target.value)}
                />
                <Button type="submit" className="w-full" disabled={!activeFileId}>
                  Send Signature Request
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Signature Envelope Queue</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {signatures.map((sig) => (
                <div key={sig.id} className="flex items-center justify-between rounded-lg border p-3 text-sm">
                  <div>
                    <p className="font-medium">
                      {sig.signerName} ({sig.signerEmail})
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {sig.signatureHash ? `SHA-256: ${sig.signatureHash.slice(0, 20)}...` : 'Awaiting cryptographic sign'}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={sig.status === 'SIGNED' ? 'default' : 'secondary'}>{sig.status}</Badge>
                    {sig.status !== 'SIGNED' && (
                      <Button size="sm" onClick={() => handleSign(sig.id)}>
                        Sign Now
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'legal' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>Create Legal Contract</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateContract} className="space-y-3">
                <Input placeholder="Contract title" value={ctrTitle} onChange={(e) => setCtrTitle(e.target.value)} />
                <Input
                  placeholder="Counterparty"
                  value={ctrCounterparty}
                  onChange={(e) => setCtrCounterparty(e.target.value)}
                />
                <select
                  aria-label="Contract Type"
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                  value={ctrType}
                  onChange={(e) => setCtrType(e.target.value)}
                >
                  <option value="MSA">MSA</option>
                  <option value="NDA">NDA</option>
                  <option value="SLA">SLA</option>
                  <option value="SOW">SOW</option>
                </select>
                <Input
                  type="number"
                  placeholder="Value amount"
                  value={ctrValue}
                  onChange={(e) => setCtrValue(e.target.value)}
                />
                <Button type="submit" className="w-full">
                  Draft Contract
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Contract Lifecycle Repository</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {contracts.map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-lg border p-3 text-sm">
                  <div>
                    <p className="font-medium">
                      {c.contractNumber} — {c.title}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {c.counterpartyName} · {c.contractType} · ${Number(c.valueAmount).toLocaleString()}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge>{c.status}</Badge>
                    {c.status === 'DRAFT' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => updateContractStatus({ id: c.id, status: 'APPROVED' })}
                      >
                        Approve
                      </Button>
                    )}
                    {c.status === 'APPROVED' && (
                      <Button size="sm" onClick={() => updateContractStatus({ id: c.id, status: 'EXECUTED' })}>
                        Execute
                      </Button>
                    )}
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

