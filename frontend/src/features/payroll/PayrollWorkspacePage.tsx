import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Wallet } from 'lucide-react'
import { useGetPayRunsQuery, useGetPayslipsQuery, useProcessPayRunMutation } from '@/app/api/peopleApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable'
import { Input } from '@/components/ui/Input'

export function PayrollWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [payMonth, setPayMonth] = useState('2026-11')
  const [feedback, setFeedback] = useState<string | null>(null)

  const skip = !accessToken || !user

  const { data: runs = [] } = useGetPayRunsQuery(undefined, { skip })
  const { data: payslips = [] } = useGetPayslipsQuery(undefined, { skip })
  const [processRun, { isLoading }] = useProcessPayRunMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access Payroll</CardTitle>
          <CardDescription>Run monthly payroll with PF, ESI, and TDS statutory deductions.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const payslipCols: DataTableColumn<(typeof payslips)[number]>[] = [
    { key: 'month', header: 'Month', render: (p) => <span className="font-mono text-xs">{p.payMonth}</span> },
    { key: 'emp', header: 'Employee', render: (p) => <span className="text-xs font-semibold">{p.employeeName}</span> },
    { key: 'gross', header: 'Gross', render: (p) => <span className="font-mono text-xs">${Number(p.grossPay).toLocaleString()}</span> },
    { key: 'pf', header: 'PF', render: (p) => <span className="font-mono text-xs">${Number(p.pfDeduction).toLocaleString()}</span> },
    { key: 'tds', header: 'TDS', render: (p) => <span className="font-mono text-xs">${Number(p.tdsTaxDeduction).toLocaleString()}</span> },
    { key: 'net', header: 'Net Pay', render: (p) => <span className="font-mono text-xs font-bold text-emerald-600">${Number(p.netPay).toLocaleString()}</span> },
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Payroll & Statutory Compliance</h1>
          <p className="text-sm text-muted-foreground">Automated monthly payroll runs, PF/ESI/TDS deductions, and payslips</p>
        </div>
        <form
          onSubmit={async (e) => {
            e.preventDefault()
            const run = await processRun({ payMonth }).unwrap()
            setFeedback(`Processed ${run.payMonth} payroll for ${run.employeesCount} employees.`)
          }}
          className="flex gap-2"
        >
          <Input value={payMonth} onChange={(e) => setPayMonth(e.target.value)} placeholder="YYYY-MM" className="w-32" required />
          <Button type="submit" disabled={isLoading}>
            <Wallet className="h-4 w-4" />
            <span>{isLoading ? 'Processing...' : 'Run Payroll'}</span>
          </Button>
        </form>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {runs.map((r) => (
          <Card key={r.id}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">{r.payMonth} Payroll</CardTitle>
                <Badge variant="success">{r.status}</Badge>
              </div>
              <CardDescription>{r.employeesCount} employees paid</CardDescription>
            </CardHeader>
            <CardContent className="text-xs space-y-1">
              <p>Gross: ${Number(r.totalGross).toLocaleString()}</p>
              <p>Deductions: ${Number(r.totalDeductions).toLocaleString()}</p>
              <p className="font-bold text-sm">Net Disbursed: ${Number(r.totalNetPay).toLocaleString()}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="space-y-2">
        <h2 className="text-lg font-semibold">Generated Payslips</h2>
        <DataTable columns={payslipCols} data={payslips} rowKey={(p) => p.id} emptyMessage="Run payroll to generate employee payslips." />
      </div>
    </div>
  )
}

