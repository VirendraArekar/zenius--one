import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Briefcase, UserPlus } from 'lucide-react'
import {
  useAddCandidateMutation,
  useCreateJobMutation,
  useGetCandidatesQuery,
  useGetJobsQuery,
  useHireCandidateMutation,
} from '@/app/api/peopleApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

export function RecruitmentWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [feedback, setFeedback] = useState<string | null>(null)

  const [jobTitle, setJobTitle] = useState('')
  const [jobDept, setJobDept] = useState('Engineering')
  const [jobLoc, setJobLoc] = useState('Bengaluru')

  const [candJobId, setCandJobId] = useState('')
  const [candName, setCandName] = useState('')
  const [candEmail, setCandEmail] = useState('')
  const [candSkills, setCandSkills] = useState('')

  const skip = !accessToken || !user

  const { data: jobs = [] } = useGetJobsQuery(undefined, { skip })
  const { data: candidates = [] } = useGetCandidatesQuery(undefined, { skip })
  const [createJob] = useCreateJobMutation()
  const [addCandidate] = useAddCandidateMutation()
  const [hireCandidate] = useHireCandidateMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Recruitment ATS</CardTitle>
          <CardDescription>Manage job openings, candidates, and onboarding to HR.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Recruitment ATS</h1>
          <p className="text-sm text-muted-foreground">Job openings, applicant pipeline, and one-click HR onboarding</p>
        </div>
        <Button variant="outline" asChild>
          <Link to="/hr">Open HR Directory →</Link>
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

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Briefcase className="h-4 w-4 text-primary" />
              <span>Open Positions ({jobs.length})</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <form
              onSubmit={async (e) => {
                e.preventDefault()
                await createJob({ title: jobTitle, department: jobDept, location: jobLoc, openingsCount: 1 }).unwrap()
                setJobTitle('')
                setFeedback('Job opening posted.')
              }}
              className="flex flex-wrap gap-2"
            >
              <Input placeholder="Role Title" value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} className="max-w-48" required />
              <Input placeholder="Department" value={jobDept} onChange={(e) => setJobDept(e.target.value)} className="max-w-36" />
              <Input placeholder="Location" value={jobLoc} onChange={(e) => setJobLoc(e.target.value)} className="max-w-36" />
              <Button type="submit">Post Job</Button>
            </form>
            <div className="space-y-2">
              {jobs.map((j) => (
                <div key={j.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <span className="font-mono font-bold mr-2">{j.jobCode}</span>
                    <span className="font-semibold">{j.title}</span>
                    <p className="text-muted-foreground">{j.department} · {j.location}</p>
                  </div>
                  <Badge variant="success">{j.status}</Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="h-4 w-4 text-primary" />
              <span>Candidates ({candidates.length})</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <form
              onSubmit={async (e) => {
                e.preventDefault()
                const jobId = candJobId || jobs[0]?.id
                if (!jobId) return
                await addCandidate({
                  jobId,
                  fullName: candName,
                  email: candEmail,
                  skills: candSkills,
                  experienceYears: 3,
                }).unwrap()
                setCandName('')
                setCandEmail('')
                setCandSkills('')
                setFeedback('Candidate added to pipeline.')
              }}
              className="space-y-2"
            >
              <select value={candJobId} onChange={(e) => setCandJobId(e.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm">
                {jobs.map((j) => (
                  <option key={j.id} value={j.id}>{j.title}</option>
                ))}
              </select>
              <div className="grid grid-cols-2 gap-2">
                <Input placeholder="Candidate Name" value={candName} onChange={(e) => setCandName(e.target.value)} required />
                <Input type="email" placeholder="Email" value={candEmail} onChange={(e) => setCandEmail(e.target.value)} required />
              </div>
              <div className="flex gap-2">
                <Input placeholder="Skills" value={candSkills} onChange={(e) => setCandSkills(e.target.value)} />
                <Button type="submit">Add</Button>
              </div>
            </form>

            <div className="space-y-2">
              {candidates.map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-lg border p-3 text-xs">
                  <div>
                    <p className="font-semibold">{c.fullName} ({c.email})</p>
                    <p className="text-muted-foreground">{c.skills || 'Applicant'}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={c.stage === 'HIRED' ? 'success' : 'info'}>{c.stage}</Badge>
                    {c.stage !== 'HIRED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={async () => {
                          const res = await hireCandidate({ id: c.id, baseMonthlySalary: 85000 }).unwrap()
                          setFeedback(`Hired ${c.fullName} as ${res.employeeCode} in HR!`)
                        }}
                      >
                        Hire to HR
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

