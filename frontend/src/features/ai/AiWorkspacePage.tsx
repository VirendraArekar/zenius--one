import { useState } from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Bot, CheckCircle2, Send, ShieldAlert, Sparkles, XCircle } from 'lucide-react'
import {
  type RagCitationDto,
  useApproveProposalMutation,
  useGetAiAgentsQuery,
  useGetAiInsightsQuery,
  useGetConversationDetailQuery,
  useGetConversationsQuery,
  useGetKnowledgeDocsQuery,
  useGetProposalsQuery,
  useIngestKnowledgeMutation,
  useRejectProposalMutation,
  useSearchKnowledgeMutation,
  useSendAiChatMutation,
} from '@/app/api/aiApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type AiTab = 'assistant' | 'rag' | 'insights' | 'approvals'

export function AiWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<AiTab>('assistant')
  const [selectedAgent, setSelectedAgent] = useState('ASSISTANT')
  const [selectedConvId, setSelectedConvId] = useState('')
  const [prompt, setPrompt] = useState('')

  const [docTitle, setDocTitle] = useState('')
  const [docSource, setDocSource] = useState('POLICY')
  const [docText, setDocText] = useState('')
  const [ragQuery, setRagQuery] = useState('')
  const [searchResults, setSearchResults] = useState<RagCitationDto[]>([])
  const [feedback, setFeedback] = useState<string | null>(null)

  const skip = !accessToken || !user
  const { data: agents = [] } = useGetAiAgentsQuery(undefined, { skip })
  const { data: insights = [] } = useGetAiInsightsQuery(undefined, { skip })
  const { data: knowledgeDocs = [] } = useGetKnowledgeDocsQuery(undefined, { skip })
  const { data: conversations = [] } = useGetConversationsQuery(undefined, { skip })
  const activeConvId = selectedConvId || conversations[0]?.id || ''
  const { data: convDetail } = useGetConversationDetailQuery(activeConvId, { skip: skip || !activeConvId })
  const { data: proposals = [] } = useGetProposalsQuery(undefined, { skip })

  const [sendChat] = useSendAiChatMutation()
  const [ingestKnowledge] = useIngestKnowledgeMutation()
  const [searchKnowledge] = useSearchKnowledgeMutation()
  const [approveProposal] = useApproveProposalMutation()
  const [rejectProposal] = useRejectProposalMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Zenius AI Copilot</CardTitle>
          <CardDescription>
            Converse with 11 specialized enterprise agents, search RAG policies, and approve AI-proposed workflows.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleChat = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!prompt.trim()) return
    try {
      const res = await sendChat({
        conversationId: selectedConvId || undefined,
        agentCode: selectedAgent,
        message: prompt.trim(),
      }).unwrap()
      setSelectedConvId(res.conversationId)
      setPrompt('')
      if (res.proposal) {
        setFeedback(`HITL proposal queued: ${res.proposal.summary}`)
      }
    } catch {
      setFeedback('Failed to reach Zenius AI')
    }
  }

  const handleIngest = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!docTitle.trim() || !docText.trim()) return
    try {
      const saved = await ingestKnowledge({
        title: docTitle.trim(),
        sourceType: docSource,
        contentText: docText.trim(),
      }).unwrap()
      setDocTitle('')
      setDocText('')
      setFeedback(`Indexed "${saved.title}" into ${saved.chunkCount} RAG vector chunks`)
    } catch {
      setFeedback('Failed to ingest document')
    }
  }

  const handleSearchRag = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!ragQuery.trim()) return
    try {
      const hits = await searchKnowledge({ query: ragQuery.trim(), topK: 5 }).unwrap()
      setSearchResults(hits)
    } catch {
      setFeedback('Failed to search RAG index')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight">
            <Sparkles className="h-6 w-6 text-primary" /> Zenius AI Intelligence
          </h1>
          <p className="text-sm text-muted-foreground">
            Permission-aware Global Copilot, 11 specialized enterprise agents, tenant RAG knowledge base, and HITL governance
          </p>
        </div>
        <Badge variant="outline">12 Agents Ready</Badge>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="flex flex-wrap gap-2 border-b pb-2">
        <Button variant={tab === 'assistant' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('assistant')}>
          <Bot className="mr-1.5 h-4 w-4" /> Agents & Chat
        </Button>
        <Button variant={tab === 'rag' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('rag')}>
          <BookOpen className="mr-1.5 h-4 w-4" /> RAG Knowledge ({knowledgeDocs.length})
        </Button>
        <Button variant={tab === 'insights' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('insights')}>
          <ShieldAlert className="mr-1.5 h-4 w-4" /> Predictive Insights ({insights.length})
        </Button>
        <Button variant={tab === 'approvals' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('approvals')}>
          <CheckCircle2 className="mr-1.5 h-4 w-4" /> HITL Action Queue ({proposals.filter((p) => p.status === 'PENDING').length})
        </Button>
      </div>

      {tab === 'assistant' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Specialized Agents ({agents.length})</CardTitle>
            </CardHeader>
            <CardContent className="max-h-[420px] space-y-1.5 overflow-y-auto">
              {agents.map((ag) => (
                <button
                  key={ag.code}
                  type="button"
                  onClick={() => {
                    setSelectedAgent(ag.code)
                    setSelectedConvId('')
                  }}
                  className={`block w-full rounded-lg border p-2.5 text-left text-xs ${
                    selectedAgent === ag.code ? 'border-primary bg-primary/10' : 'hover:bg-muted'
                  }`}
                >
                  <div className="flex items-center justify-between font-semibold">
                    <span>{ag.name}</span>
                    <Badge variant="outline">{ag.code}</Badge>
                  </div>
                  <p className="mt-1 text-muted-foreground">{ag.description}</p>
                </button>
              ))}
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-base">Agent Console — {selectedAgent}</CardTitle>
                <CardDescription>Try: &quot;Automate escalation notification when high priority ticket arrives&quot;</CardDescription>
              </div>
              <Button size="sm" variant="outline" onClick={() => setSelectedConvId('')}>
                New Thread
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="max-h-72 space-y-3 overflow-y-auto rounded-lg border p-3">
                {(convDetail?.messages ?? []).length === 0 ? (
                  <p className="text-xs text-muted-foreground">Ask a question grounded in your organization data and RAG documents.</p>
                ) : (
                  convDetail?.messages.map((m) => (
                    <div
                      key={m.id}
                      className={`rounded-lg p-3 text-sm ${
                        m.role === 'USER' ? 'ml-8 bg-primary text-primary-foreground' : 'mr-8 bg-muted'
                      }`}
                    >
                      <p className="text-[10px] font-semibold opacity-75">{m.role}</p>
                      <p className="mt-0.5">{m.content}</p>
                      {m.citations.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-1">
                          {m.citations.map((c) => (
                            <Badge key={`${c.documentId}-${c.chunkIndex}`} variant="secondary" className="text-[10px]">
                              Cite: {c.documentTitle} ({c.score})
                            </Badge>
                          ))}
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>

              <form onSubmit={handleChat} className="flex gap-2">
                <Input
                  placeholder={`Message ${selectedAgent} Agent...`}
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                />
                <Button type="submit">
                  <Send className="mr-1.5 h-4 w-4" /> Ask
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'rag' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Ingest Knowledge Document</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleIngest} className="space-y-3">
                <Input placeholder="Policy or KB title" value={docTitle} onChange={(e) => setDocTitle(e.target.value)} />
                <select
                  aria-label="Source Type"
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                  value={docSource}
                  onChange={(e) => setDocSource(e.target.value)}
                >
                  <option value="POLICY">POLICY</option>
                  <option value="KB">KB</option>
                  <option value="CONTRACT">CONTRACT</option>
                  <option value="GENERAL">GENERAL</option>
                </select>
                <textarea
                  className="min-h-[120px] w-full rounded-md border border-input bg-background p-2 text-sm"
                  placeholder="Paste text to chunk & index for semantic RAG retrieval..."
                  value={docText}
                  onChange={(e) => setDocText(e.target.value)}
                />
                <Button type="submit" className="w-full">
                  Index into RAG Store
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Semantic Knowledge Search</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <form onSubmit={handleSearchRag} className="flex gap-2">
                <Input placeholder="Search RAG chunks..." value={ragQuery} onChange={(e) => setRagQuery(e.target.value)} />
                <Button type="submit" size="sm">
                  Search
                </Button>
              </form>
              <div className="space-y-2">
                {searchResults.map((hit) => (
                  <div key={`${hit.documentId}-${hit.chunkIndex}`} className="rounded-lg border p-2.5 text-xs">
                    <div className="flex items-center justify-between font-semibold">
                      <span>{hit.documentTitle}</span>
                      <Badge variant="secondary">Score: {hit.score}</Badge>
                    </div>
                    <p className="mt-1 text-muted-foreground">{hit.snippet}</p>
                  </div>
                ))}
              </div>
              <div className="border-t pt-3">
                <p className="mb-2 text-xs font-semibold">Indexed Documents ({knowledgeDocs.length})</p>
                {knowledgeDocs.map((d) => (
                  <div key={d.id} className="flex items-center justify-between py-1 text-xs">
                    <span>{d.title}</span>
                    <Badge variant="outline">{d.chunkCount} chunks</Badge>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'insights' && (
        <div className="grid gap-4 sm:grid-cols-2">
          {insights.map((ins) => (
            <Card key={ins.title}>
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                  <Badge variant="outline">{ins.domain}</Badge>
                  <Badge variant={ins.severity === 'HIGH' ? 'warning' : 'secondary'}>{ins.severity}</Badge>
                </div>
                <CardTitle className="mt-2 text-base">{ins.title}</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">{ins.summary}</CardContent>
            </Card>
          ))}
        </div>
      )}

      {tab === 'approvals' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Human-in-the-Loop Action Proposals</CardTitle>
            <CardDescription>AI-suggested workflows and mutations never execute without explicit approval.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {proposals.length === 0 ? (
              <p className="text-xs text-muted-foreground">No AI proposals queued yet.</p>
            ) : (
              proposals.map((p) => (
                <div key={p.id} className="flex flex-col gap-2 rounded-lg border p-3 text-sm sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-semibold">
                      [{p.agentCode}] {p.actionType}
                    </p>
                    <p className="text-xs text-muted-foreground">{p.summary}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge>{p.status}</Badge>
                    {p.status === 'PENDING' && (
                      <>
                        <Button size="sm" onClick={() => approveProposal(p.id)}>
                          <CheckCircle2 className="mr-1 h-3.5 w-3.5" /> Approve &amp; Execute
                        </Button>
                        <Button size="sm" variant="outline" onClick={() => rejectProposal(p.id)}>
                          <XCircle className="mr-1 h-3.5 w-3.5" /> Reject
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
