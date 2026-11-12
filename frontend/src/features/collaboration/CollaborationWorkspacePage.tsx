import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CheckCheck, Hash, Mail, MailOpen, Reply, Send, Star, Video } from 'lucide-react'
import {
  useCreateChannelMutation,
  useGetChannelMessagesQuery,
  useGetChannelsQuery,
  useGetMailQuery,
  useGetMeetingsQuery,
  useMarkMailReadMutation,
  usePostMessageMutation,
  useReplyMailMutation,
  useScheduleMeetingMutation,
  useSendMailMutation,
  useToggleMailStarMutation,
} from '@/app/api/workplaceApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type CollabTab = 'channels' | 'mail' | 'meetings'
type MailFolder = 'INBOX' | 'STARRED' | 'SENT'

export function CollaborationWorkspacePage({ initialTab = 'channels' }: { initialTab?: CollabTab }) {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<CollabTab>(initialTab)
  const [selectedChannelId, setSelectedChannelId] = useState<string>('')
  const [mailFolder, setMailFolder] = useState<MailFolder>('INBOX')
  const [selectedMailId, setSelectedMailId] = useState<string>('')
  const [replyText, setReplyText] = useState('')
  const [feedback, setFeedback] = useState<string | null>(null)

  const [channelName, setChannelName] = useState('')
  const [messageBody, setMessageBody] = useState('')

  const [recipientEmail, setRecipientEmail] = useState('')
  const [mailSubject, setMailSubject] = useState('')
  const [mailBody, setMailBody] = useState('')

  const [meetingTitle, setMeetingTitle] = useState('')
  const [meetingDuration, setMeetingDuration] = useState('30')

  const skip = !accessToken || !user

  const { data: channels = [] } = useGetChannelsQuery(undefined, { skip })
  const activeChannelId = selectedChannelId || channels[0]?.id || ''
  const { data: messages = [] } = useGetChannelMessagesQuery(activeChannelId, { skip: skip || !activeChannelId })
  const { data: mail = [] } = useGetMailQuery(mailFolder, { skip })
  const activeMail = mail.find((m) => m.id === selectedMailId) || mail[0] || null
  const { data: meetings = [] } = useGetMeetingsQuery(undefined, { skip })

  const [createChannel] = useCreateChannelMutation()
  const [postMessage] = usePostMessageMutation()
  const [sendMail] = useSendMailMutation()
  const [markMailRead] = useMarkMailReadMutation()
  const [toggleMailStar] = useToggleMailStarMutation()
  const [replyMail] = useReplyMailMutation()
  const [scheduleMeeting] = useScheduleMeetingMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Mail & Collaboration</CardTitle>
          <CardDescription>Team channels, business email mailboxes, and video meetings.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleCreateChannel = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!channelName.trim()) return
    try {
      const ch = await createChannel({ name: channelName.trim(), privateChannel: false }).unwrap()
      setSelectedChannelId(ch.id)
      setChannelName('')
      setFeedback(`Created channel #${ch.name}`)
    } catch {
      setFeedback('Failed to create channel')
    }
  }

  const handlePostMessage = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!activeChannelId || !messageBody.trim()) return
    try {
      await postMessage({ channelId: activeChannelId, body: messageBody.trim() }).unwrap()
      setMessageBody('')
    } catch {
      setFeedback('Failed to post message')
    }
  }

  const handleSendMail = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!recipientEmail.trim() || !mailSubject.trim() || !mailBody.trim()) return
    try {
      await sendMail({
        recipientEmail: recipientEmail.trim(),
        subject: mailSubject.trim(),
        body: mailBody.trim(),
        labels: 'WORK',
      }).unwrap()
      setRecipientEmail('')
      setMailSubject('')
      setMailBody('')
      setFeedback('Email sent')
    } catch {
      setFeedback('Failed to send email')
    }
  }

  const handleScheduleMeeting = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!meetingTitle.trim()) return
    try {
      const m = await scheduleMeeting({
        title: meetingTitle.trim(),
        durationMinutes: Number(meetingDuration) || 30,
      }).unwrap()
      setMeetingTitle('')
      setFeedback(`Scheduled meeting: ${m.meetingLink}`)
    } catch {
      setFeedback('Failed to schedule meeting')
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Email & Team Collaboration</h1>
        <p className="text-sm text-muted-foreground">
          Unified workspace for business mail, real-time team channels, and scheduled video meetings
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
        <Button variant={tab === 'channels' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('channels')}>
          <Hash className="mr-1.5 h-4 w-4" /> Team Channels ({channels.length})
        </Button>
        <Button variant={tab === 'mail' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('mail')}>
          <Mail className="mr-1.5 h-4 w-4" /> Business Mail ({mail.length})
        </Button>
        <Button variant={tab === 'meetings' ? 'default' : 'ghost'} size="sm" onClick={() => setTab('meetings')}>
          <Video className="mr-1.5 h-4 w-4" /> Meetings ({meetings.length})
        </Button>
      </div>

      {tab === 'channels' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>Channels</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <form onSubmit={handleCreateChannel} className="flex gap-2">
                <Input placeholder="new-channel" value={channelName} onChange={(e) => setChannelName(e.target.value)} />
                <Button type="submit" size="sm">
                  Add
                </Button>
              </form>
              <div className="space-y-1">
                {channels.map((ch) => (
                  <button
                    key={ch.id}
                    type="button"
                    onClick={() => setSelectedChannelId(ch.id)}
                    className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm ${
                      ch.id === activeChannelId ? 'bg-primary text-primary-foreground' : 'hover:bg-muted'
                    }`}
                  >
                    <span># {ch.name}</span>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Channel Stream</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="max-h-72 space-y-2 overflow-y-auto rounded-lg border p-3">
                {messages.length === 0 ? (
                  <p className="text-xs text-muted-foreground">No messages in this channel yet.</p>
                ) : (
                  messages.map((m) => (
                    <div key={m.id} className="rounded bg-muted/50 p-2 text-sm">
                      <span className="font-semibold">{m.senderName}: </span>
                      <span>{m.body}</span>
                    </div>
                  ))
                )}
              </div>
              <form onSubmit={handlePostMessage} className="flex gap-2">
                <Input
                  placeholder="Write a message to channel..."
                  value={messageBody}
                  onChange={(e) => setMessageBody(e.target.value)}
                />
                <Button type="submit" disabled={!activeChannelId}>
                  Send
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'mail' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-4">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Send className="h-4 w-4 text-primary" /> Compose Email
              </CardTitle>
              <CardDescription>Delivers inside org mailbox, triggers real-time notifications & optional SMTP relay.</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSendMail} className="space-y-3">
                <Input
                  type="email"
                  placeholder="recipient@company.com"
                  value={recipientEmail}
                  onChange={(e) => setRecipientEmail(e.target.value)}
                />
                <Input placeholder="Subject" value={mailSubject} onChange={(e) => setMailSubject(e.target.value)} />
                <textarea
                  className="min-h-[120px] w-full rounded-md border border-input bg-background p-2.5 text-sm"
                  placeholder="Write your email..."
                  value={mailBody}
                  onChange={(e) => setMailBody(e.target.value)}
                />
                <Button type="submit" className="w-full">
                  Send Email
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card className="lg:col-span-8">
            <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardTitle>Mailbox · {mailFolder}</CardTitle>
                <CardDescription>{mail.filter((m) => !m.read).length} unread in view</CardDescription>
              </div>
              <div className="flex gap-1.5">
                {(['INBOX', 'STARRED', 'SENT'] as MailFolder[]).map((folder) => (
                  <Button
                    key={folder}
                    size="sm"
                    variant={mailFolder === folder ? 'default' : 'outline'}
                    onClick={() => {
                      setMailFolder(folder)
                      setSelectedMailId('')
                    }}
                  >
                    {folder === 'INBOX' ? 'Inbox' : folder === 'STARRED' ? 'Starred' : 'Sent'}
                  </Button>
                ))}
              </div>
            </CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-5">
              <div className="space-y-2 md:col-span-2 max-h-[420px] overflow-y-auto pr-1">
                {mail.length === 0 ? (
                  <p className="rounded-lg border border-dashed p-6 text-center text-xs text-muted-foreground">
                    No emails in {mailFolder}.
                  </p>
                ) : (
                  mail.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => {
                        setSelectedMailId(item.id)
                        if (!item.read) {
                          void markMailRead({ id: item.id, read: true })
                        }
                      }}
                      className={`w-full rounded-lg border p-3 text-left text-sm transition-colors ${
                        activeMail?.id === item.id
                          ? 'border-primary bg-primary/5'
                          : !item.read
                          ? 'border-l-4 border-l-primary bg-slate-50 font-medium'
                          : 'hover:bg-muted/40'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate font-semibold">{item.subject}</span>
                        {item.starred && <Star className="h-3.5 w-3.5 shrink-0 fill-amber-400 text-amber-500" />}
                      </div>
                      <p className="mt-0.5 truncate text-xs text-muted-foreground">
                        {item.senderEmail} → {item.recipientEmail}
                      </p>
                    </button>
                  ))
                )}
              </div>

              <div className="rounded-xl border bg-slate-50/60 p-4 md:col-span-3">
                {!activeMail ? (
                  <p className="py-12 text-center text-xs text-muted-foreground">Select a message to read or reply.</p>
                ) : (
                  <div className="space-y-4">
                    <div className="flex flex-wrap items-start justify-between gap-2 border-b pb-3">
                      <div>
                        <h3 className="text-base font-semibold">{activeMail.subject}</h3>
                        <p className="mt-1 text-xs text-muted-foreground">
                          From <span className="font-medium text-foreground">{activeMail.senderEmail}</span> to{' '}
                          <span className="font-medium text-foreground">{activeMail.recipientEmail}</span>
                        </p>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => void toggleMailStar(activeMail.id)}
                          title="Star message"
                        >
                          <Star className={`h-3.5 w-3.5 ${activeMail.starred ? 'fill-amber-400 text-amber-500' : ''}`} />
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => void markMailRead({ id: activeMail.id, read: !activeMail.read })}
                          title={activeMail.read ? 'Mark unread' : 'Mark read'}
                        >
                          {activeMail.read ? <MailOpen className="h-3.5 w-3.5" /> : <CheckCheck className="h-3.5 w-3.5" />}
                        </Button>
                        {activeMail.labels && <Badge variant="outline">{activeMail.labels}</Badge>}
                      </div>
                    </div>
                    <div className="min-h-[120px] whitespace-pre-wrap text-sm leading-relaxed text-slate-700">
                      {activeMail.body}
                    </div>
                    <form
                      onSubmit={async (e) => {
                        e.preventDefault()
                        if (!replyText.trim()) return
                        try {
                          await replyMail({ id: activeMail.id, body: replyText.trim() }).unwrap()
                          setReplyText('')
                          setFeedback('Reply sent and recipient notified.')
                        } catch {
                          setFeedback('Failed to send reply')
                        }
                      }}
                      className="space-y-2 border-t pt-3"
                    >
                      <textarea
                        className="min-h-[70px] w-full rounded-md border border-input bg-white p-2 text-sm"
                        placeholder={`Reply to ${activeMail.senderEmail}...`}
                        value={replyText}
                        onChange={(e) => setReplyText(e.target.value)}
                      />
                      <Button type="submit" size="sm">
                        <Reply className="mr-1.5 h-3.5 w-3.5" /> Send Reply
                      </Button>
                    </form>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {tab === 'meetings' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>Schedule Video Meeting</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleScheduleMeeting} className="space-y-3">
                <Input
                  placeholder="Meeting title"
                  value={meetingTitle}
                  onChange={(e) => setMeetingTitle(e.target.value)}
                />
                <Input
                  type="number"
                  min={15}
                  placeholder="Duration (minutes)"
                  value={meetingDuration}
                  onChange={(e) => setMeetingDuration(e.target.value)}
                />
                <Button type="submit" className="w-full">
                  Schedule Meeting
                </Button>
              </form>
            </CardContent>
          </Card>
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Upcoming Meetings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {meetings.map((m) => (
                <div key={m.id} className="flex items-center justify-between rounded-lg border p-3 text-sm">
                  <div>
                    <p className="font-medium">{m.title}</p>
                    <p className="text-xs text-muted-foreground">
                      Organizer: {m.organizerName} · {m.durationMinutes} min
                    </p>
                  </div>
                  <Badge variant="secondary">{m.meetingLink}</Badge>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

