import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Calendar,
  CheckCircle2,
  Clock,
  Compass,
  ExternalLink,
  LocateFixed,
  MapPin,
  Search,
  Target,
  UserCheck,
  XCircle,
} from 'lucide-react'
import {
  useCancelLeaveMutation,
  useCheckInAttendanceMutation,
  useCheckOutAttendanceMutation,
  useCreateEmployeeMutation,
  useCreateGoalMutation,
  useDecideLeaveMutation,
  useGetAttendanceQuery,
  useGetEmployeesQuery,
  useGetGoalsQuery,
  useGetLeavesQuery,
  useSubmitLeaveMutation,
  useUpdateGoalProgressMutation,
} from '@/app/api/peopleApi'
import { useAppSelector } from '@/app/hooks'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

type HrTab = 'attendance' | 'leaves' | 'employees' | 'goals'

const HQ_LAT = 40.7128
const HQ_LNG = -74.006

function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRad = (d: number) => (d * Math.PI) / 180
  const R = 6371
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function formatDuration(minutes: number): string {
  const hrs = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${hrs}h ${String(mins).padStart(2, '0')}m`
}

export function HrWorkspacePage() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const [tab, setTab] = useState<HrTab>('attendance')
  const [feedback, setFeedback] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)
  const [nowClock, setNowClock] = useState(() => new Date())

  // Employees state
  const [empSearch, setEmpSearch] = useState('')
  const [deptFilter, setDeptFilter] = useState('ALL')
  const [selectedEmpId, setSelectedEmpId] = useState<string>('')
  const [empName, setEmpName] = useState('')
  const [empEmail, setEmpEmail] = useState('')
  const [empDesignation, setEmpDesignation] = useState('')
  const [empDept, setEmpDept] = useState('Engineering')
  const [empSalary, setEmpSalary] = useState('75000')

  // Geo-Attendance state
  const [attEmpId, setAttEmpId] = useState('')
  const [attMode, setAttMode] = useState('OFFICE')
  const [attSearch, setAttSearch] = useState('')
  const [attModeFilter, setAttModeFilter] = useState('ALL')
  const [gpsLat, setGpsLat] = useState<number>(HQ_LAT)
  const [gpsLng, setGpsLng] = useState<number>(HQ_LNG)
  const [gpsAccuracy, setGpsAccuracy] = useState<number | null>(null)
  const [gpsSource, setGpsSource] = useState<string>('HQ Office Geofence Preset')
  const [locating, setLocating] = useState(false)

  // Leave Management state
  const todayIso = new Date().toISOString().slice(0, 10)
  const [leaveEmpId, setLeaveEmpId] = useState('')
  const [leaveType, setLeaveType] = useState('ANNUAL')
  const [leaveStart, setLeaveStart] = useState(todayIso)
  const [leaveEnd, setLeaveEnd] = useState(todayIso)
  const [leaveReason, setLeaveReason] = useState('')
  const [leaveStatusFilter, setLeaveStatusFilter] = useState('ALL')

  // Goals state
  const [goalEmpId, setGoalEmpId] = useState('')
  const [goalTitle, setGoalTitle] = useState('')
  const [goalMetric, setGoalMetric] = useState('')

  const skip = !accessToken || !user

  const { data: employees = [] } = useGetEmployeesQuery(undefined, { skip })
  const { data: attendance = [] } = useGetAttendanceQuery(undefined, { skip })
  const { data: leaves = [] } = useGetLeavesQuery(undefined, { skip })
  const { data: goals = [] } = useGetGoalsQuery(undefined, { skip })

  const [createEmployee] = useCreateEmployeeMutation()
  const [checkIn, { isLoading: checkingIn }] = useCheckInAttendanceMutation()
  const [checkOut, { isLoading: checkingOut }] = useCheckOutAttendanceMutation()
  const [submitLeave, { isLoading: submittingLeave }] = useSubmitLeaveMutation()
  const [decideLeave] = useDecideLeaveMutation()
  const [cancelLeave] = useCancelLeaveMutation()
  const [createGoal] = useCreateGoalMutation()
  const [updateGoalProgress] = useUpdateGoalProgressMutation()

  useEffect(() => {
    const id = setInterval(() => setNowClock(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  const empMap = useMemo(() => {
    const m = new Map<string, (typeof employees)[number]>()
    for (const e of employees) m.set(e.id, e)
    return m
  }, [employees])

  // Auto-select logged-in employee if email matches, otherwise first employee
  const defaultEmpId = useMemo(() => {
    if (employees.length === 0) return ''
    const self = employees.find((e) => e.workEmail.toLowerCase() === (user?.email || '').toLowerCase())
    return self?.id || employees[0].id
  }, [employees, user?.email])

  const activeAttEmpId = attEmpId || defaultEmpId
  const activeLeaveEmpId = leaveEmpId || defaultEmpId
  const activeGoalEmpId = goalEmpId || defaultEmpId
  const inspectedEmployee = empMap.get(selectedEmpId) || empMap.get(defaultEmpId) || null

  const captureBrowserGps = () => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      setFeedback({ type: 'err', text: 'Browser GPS unavailable; using Office Geofence coordinates.' })
      return
    }
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setGpsLat(Number(pos.coords.latitude.toFixed(6)))
        setGpsLng(Number(pos.coords.longitude.toFixed(6)))
        setGpsAccuracy(Math.round(pos.coords.accuracy))
        setGpsSource('Live Browser GPS')
        setLocating(false)
        setFeedback({ type: 'ok', text: `GPS Locked: ${pos.coords.latitude.toFixed(5)}, ${pos.coords.longitude.toFixed(5)}` })
      },
      () => {
        setLocating(false)
        setGpsLat(HQ_LAT)
        setGpsLng(HQ_LNG)
        setGpsAccuracy(null)
        setGpsSource('HQ Office Geofence Preset (Permission Declined)')
        setFeedback({ type: 'err', text: 'Location permission declined; reverted to Office HQ preset.' })
      },
      { enableHighAccuracy: true, timeout: 6000 }
    )
  }

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Sign In to Access People & HR</CardTitle>
          <CardDescription>Manage employee directory, GPS attendance, leave workflows, and KPIs.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const todayPunches = attendance.filter((a) => a.workDate === todayIso)
  const activeEmpTodayPunch = todayPunches.find((a) => a.employeeId === activeAttEmpId)
  const pendingLeavesCount = leaves.filter((l) => l.status === 'PENDING').length
  const avgGoalPct = goals.length > 0 ? Math.round(goals.reduce((sum, g) => sum + g.progressPercent, 0) / goals.length) : 0

  const leaveEmpObj = empMap.get(activeLeaveEmpId)
  const computedLeaveDays = (() => {
    if (!leaveStart || !leaveEnd) return 0
    const s = new Date(leaveStart)
    const e = new Date(leaveEnd)
    if (Number.isNaN(s.getTime()) || Number.isNaN(e.getTime()) || e < s) return 0
    return Math.floor((e.getTime() - s.getTime()) / 86400000) + 1
  })()

  const departments = Array.from(new Set(employees.map((e) => e.departmentName || 'General')))

  const filteredEmployees = employees.filter((e) => {
    const q = empSearch.trim().toLowerCase()
    const matchText =
      !q ||
      e.fullName.toLowerCase().includes(q) ||
      e.employeeCode.toLowerCase().includes(q) ||
      e.workEmail.toLowerCase().includes(q) ||
      e.designation.toLowerCase().includes(q)
    const matchDept = deptFilter === 'ALL' || (e.departmentName || 'General') === deptFilter
    return matchText && matchDept
  })

  const filteredAttendance = attendance.filter((a) => {
    const emp = empMap.get(a.employeeId)
    const q = attSearch.trim().toLowerCase()
    const matchText =
      !q ||
      (emp?.fullName.toLowerCase().includes(q) ?? false) ||
      (emp?.employeeCode.toLowerCase().includes(q) ?? false) ||
      (a.locationLabel?.toLowerCase().includes(q) ?? false)
    const matchMode = attModeFilter === 'ALL' || a.workMode === attModeFilter
    return matchText && matchMode
  })

  const filteredLeaves = leaves.filter((l) => leaveStatusFilter === 'ALL' || l.status === leaveStatusFilter)

  const distanceKm = haversineKm(gpsLat, gpsLng, HQ_LAT, HQ_LNG)
  const insideGeofence = distanceKm <= 1.0

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">People, Geo-Attendance & Leave Operations</h1>
          <p className="text-sm text-muted-foreground">
            Live GPS check-in/out punches, automated leave balance tracking, employee profiles & OKR scorecards
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to="/recruitment">Recruitment ATS</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/payroll">Payroll Runs</Link>
          </Button>
        </div>
      </div>

      {/* Live Workforce KPI Strip */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Active Workforce</CardTitle>
            <UserCheck className="h-4 w-4 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{employees.length}</div>
            <p className="text-xs text-muted-foreground">{departments.length} departments</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Checked In Today</CardTitle>
            <MapPin className="h-4 w-4 text-emerald-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{todayPunches.length}</div>
            <p className="text-xs text-muted-foreground">
              {todayPunches.filter((a) => !a.checkOutAt).length} currently clocked in
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Pending Leave Approvals</CardTitle>
            <Calendar className="h-4 w-4 text-amber-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{pendingLeavesCount}</div>
            <p className="text-xs text-muted-foreground">{leaves.length} total requests</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">OKR Goal Attainment</CardTitle>
            <Target className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{avgGoalPct}%</div>
            <p className="text-xs text-muted-foreground">Across {goals.length} active goals</p>
          </CardContent>
        </Card>
      </div>

      {feedback && (
        <div
          className={`flex items-center justify-between rounded-xl border px-4 py-2.5 text-xs ${
            feedback.type === 'err'
              ? 'border-destructive/40 bg-destructive/10 text-destructive'
              : 'border-emerald-300 bg-emerald-50 text-emerald-800'
          }`}
        >
          <span>{feedback.text}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        <Button size="sm" variant={tab === 'attendance' ? 'default' : 'outline'} onClick={() => setTab('attendance')}>
          <Clock className="mr-1.5 h-3.5 w-3.5" />
          Geo Attendance ({attendance.length})
        </Button>
        <Button size="sm" variant={tab === 'leaves' ? 'default' : 'outline'} onClick={() => setTab('leaves')}>
          <Calendar className="mr-1.5 h-3.5 w-3.5" />
          Leave Management ({leaves.length})
        </Button>
        <Button size="sm" variant={tab === 'employees' ? 'default' : 'outline'} onClick={() => setTab('employees')}>
          <UserCheck className="mr-1.5 h-3.5 w-3.5" />
          Employees ({employees.length})
        </Button>
        <Button size="sm" variant={tab === 'goals' ? 'default' : 'outline'} onClick={() => setTab('goals')}>
          <Target className="mr-1.5 h-3.5 w-3.5" />
          Performance OKRs ({goals.length})
        </Button>
      </div>

      {/* TAB 1: GEO-LOCATION CHECK-IN / CHECK-OUT & ATTENDANCE */}
      {tab === 'attendance' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-4 border-indigo-200/70">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base">GPS Time & Attendance Punch</CardTitle>
                  <CardDescription>{nowClock.toDateString()}</CardDescription>
                </div>
                <div className="rounded-xl bg-slate-950 px-3 py-1.5 font-mono text-sm font-bold text-emerald-400">
                  {nowClock.toLocaleTimeString()}
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">Employee</label>
                <select
                  value={activeAttEmpId}
                  onChange={(e) => setAttEmpId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  {employees.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.employeeCode} · {e.fullName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-muted-foreground">Work Mode</label>
                <div className="grid grid-cols-2 gap-2">
                  {(['OFFICE', 'REMOTE'] as const).map((m) => (
                    <Button
                      key={m}
                      type="button"
                      size="sm"
                      variant={attMode === m ? 'default' : 'outline'}
                      onClick={() => setAttMode(m)}
                    >
                      {m === 'OFFICE' ? 'Office Premises' : 'Remote / Field'}
                    </Button>
                  ))}
                </div>
              </div>

              {/* GPS Coordinate Capture Card */}
              <div className="rounded-xl border bg-slate-50/90 p-3 text-xs space-y-2 dark:bg-muted/30">
                <div className="flex items-center justify-between">
                  <span className="flex items-center gap-1.5 font-semibold">
                    <Compass className="h-3.5 w-3.5 text-indigo-600" /> Geo-Coordinates
                  </span>
                  <Badge variant={insideGeofence ? 'success' : 'info'}>
                    {insideGeofence ? 'Inside HQ Geofence' : `${distanceKm.toFixed(1)} km from HQ`}
                  </Badge>
                </div>
                <p className="font-mono text-[11px] text-slate-700 dark:text-slate-300">
                  Lat: {gpsLat.toFixed(5)} · Lng: {gpsLng.toFixed(5)}
                  {gpsAccuracy != null ? ` (±${gpsAccuracy}m)` : ''}
                </p>
                <p className="text-[11px] text-muted-foreground">{gpsSource}</p>
                <div className="flex flex-wrap gap-1.5 pt-1">
                  <Button type="button" size="sm" variant="outline" onClick={captureBrowserGps} disabled={locating}>
                    <LocateFixed className="mr-1 h-3 w-3" />
                    {locating ? 'Locating...' : 'Capture Live GPS'}
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    onClick={() => {
                      setGpsLat(HQ_LAT)
                      setGpsLng(HQ_LNG)
                      setGpsAccuracy(12)
                      setGpsSource('HQ Office Geofence Preset')
                    }}
                  >
                    HQ Preset
                  </Button>
                  <a
                    href={`https://maps.google.com/?q=${gpsLat},${gpsLng}`}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1 px-2 py-1 text-[11px] text-primary hover:underline"
                  >
                    Map <ExternalLink className="h-3 w-3" />
                  </a>
                </div>
              </div>

              {/* Active Punch Status & Check-in / Check-out Buttons */}
              {activeEmpTodayPunch && !activeEmpTodayPunch.checkOutAt ? (
                <div className="space-y-3 rounded-xl border border-emerald-300 bg-emerald-50/70 p-3 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-emerald-800">Clocked In Today</span>
                    <Badge variant="success">
                      {formatDuration(
                        Math.max(
                          0,
                          Math.floor((nowClock.getTime() - new Date(activeEmpTodayPunch.checkInAt).getTime()) / 60000)
                        )
                      )}{' '}
                      elapsed
                    </Badge>
                  </div>
                  <p className="text-emerald-700">
                    Since {new Date(activeEmpTodayPunch.checkInAt).toLocaleTimeString()} ·{' '}
                    {activeEmpTodayPunch.locationLabel || activeEmpTodayPunch.workMode}
                  </p>
                  <Button
                    type="button"
                    className="w-full bg-rose-600 text-white hover:bg-rose-500"
                    disabled={checkingOut}
                    onClick={async () => {
                      try {
                        const out = await checkOut({
                          employeeId: activeAttEmpId,
                          latitude: gpsLat,
                          longitude: gpsLng,
                        }).unwrap()
                        setFeedback({
                          type: 'ok',
                          text: `Checked out (${formatDuration(out.workedMinutes)}) at GPS ${gpsLat.toFixed(4)}, ${gpsLng.toFixed(4)}`,
                        })
                      } catch (err: unknown) {
                        const msg = (err as { data?: { message?: string } })?.data?.message || 'Check-out failed'
                        setFeedback({ type: 'err', text: msg })
                      }
                    }}
                  >
                    {checkingOut ? 'Recording Geo Check-Out...' : 'Check Out with Geo-Location'}
                  </Button>
                </div>
              ) : (
                <Button
                  type="button"
                  className="w-full"
                  disabled={!activeAttEmpId || checkingIn || Boolean(activeEmpTodayPunch?.checkOutAt)}
                  onClick={async () => {
                    try {
                      await checkIn({
                        employeeId: activeAttEmpId,
                        workMode: attMode,
                        latitude: gpsLat,
                        longitude: gpsLng,
                        locationLabel: `${attMode === 'REMOTE' ? 'Remote' : 'Office'} (${gpsLat.toFixed(4)}, ${gpsLng.toFixed(4)})`,
                      }).unwrap()
                      setFeedback({
                        type: 'ok',
                        text: `Checked in ${empMap.get(activeAttEmpId)?.fullName || ''} at GPS ${gpsLat.toFixed(4)}, ${gpsLng.toFixed(4)}`,
                      })
                    } catch (err: unknown) {
                      const msg =
                        (err as { data?: { message?: string } })?.data?.message ||
                        'Employee already checked in today or validation error.'
                      setFeedback({ type: 'err', text: msg })
                    }
                  }}
                >
                  {activeEmpTodayPunch?.checkOutAt
                    ? 'Shift Completed Today'
                    : checkingIn
                    ? 'Punching In...'
                    : 'Check In with Geo-Location'}
                </Button>
              )}
            </CardContent>
          </Card>

          <Card className="lg:col-span-8">
            <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardTitle>Geo-Attendance Ledger</CardTitle>
                <CardDescription>Live shift punches, GPS verification links, and overtime tracking</CardDescription>
              </div>
              <div className="flex flex-wrap gap-2">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                  <Input
                    placeholder="Filter employee or location..."
                    value={attSearch}
                    onChange={(e) => setAttSearch(e.target.value)}
                    className="h-8 w-44 pl-8 text-xs"
                  />
                </div>
                {(['ALL', 'OFFICE', 'REMOTE'] as const).map((m) => (
                  <Button
                    key={m}
                    size="sm"
                    variant={attModeFilter === m ? 'default' : 'outline'}
                    onClick={() => setAttModeFilter(m)}
                  >
                    {m}
                  </Button>
                ))}
              </div>
            </CardHeader>
            <CardContent className="space-y-2.5 max-h-[540px] overflow-y-auto">
              {filteredAttendance.map((a) => {
                const emp = empMap.get(a.employeeId)
                return (
                  <div
                    key={a.id}
                    className="flex flex-col gap-2 rounded-xl border p-3 text-xs sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-sm">{emp?.fullName || 'Employee'}</span>
                        {emp && <Badge variant="outline">{emp.employeeCode}</Badge>}
                        <Badge variant={a.workMode === 'REMOTE' ? 'info' : 'secondary'}>{a.workMode}</Badge>
                      </div>
                      <p className="text-muted-foreground">
                        {a.workDate} · In: {new Date(a.checkInAt).toLocaleTimeString()}
                        {a.checkOutAt ? ` · Out: ${new Date(a.checkOutAt).toLocaleTimeString()}` : ' · Active Shift'}
                        {' · '}Worked: <span className="font-medium text-foreground">{formatDuration(a.workedMinutes)}</span>
                        {a.overtimeMinutes > 0 && ` (+${a.overtimeMinutes}m OT)`}
                      </p>
                      <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
                        <span className="inline-flex items-center gap-1">
                          <MapPin className="h-3 w-3 text-indigo-500" />
                          {a.locationLabel ||
                            (a.latitude != null && a.longitude != null
                              ? `${a.latitude.toFixed(4)}, ${a.longitude.toFixed(4)}`
                              : 'Office GPS')}
                        </span>
                        {a.latitude != null && a.longitude != null && (
                          <a
                            href={`https://maps.google.com/?q=${a.latitude},${a.longitude}`}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-0.5 text-primary hover:underline"
                          >
                            In Pin <ExternalLink className="h-2.5 w-2.5" />
                          </a>
                        )}
                        {a.checkOutLatitude != null && a.checkOutLongitude != null && (
                          <a
                            href={`https://maps.google.com/?q=${a.checkOutLatitude},${a.checkOutLongitude}`}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-0.5 text-emerald-600 hover:underline"
                          >
                            Out Pin <ExternalLink className="h-2.5 w-2.5" />
                          </a>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {!a.checkOutAt ? (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={async () => {
                            try {
                              await checkOut({
                                employeeId: a.employeeId,
                                latitude: gpsLat,
                                longitude: gpsLng,
                              }).unwrap()
                              setFeedback({
                                type: 'ok',
                                text: `Checked out ${emp?.fullName || ''} with GPS (${gpsLat.toFixed(4)}, ${gpsLng.toFixed(4)})`,
                              })
                            } catch (err: unknown) {
                              const msg = (err as { data?: { message?: string } })?.data?.message || 'Check-out failed'
                              setFeedback({ type: 'err', text: msg })
                            }
                          }}
                        >
                          Check Out (GPS)
                        </Button>
                      ) : (
                        <Badge variant="success">Completed</Badge>
                      )}
                    </div>
                  </div>
                )
              })}
            </CardContent>
          </Card>
        </div>
      )}

      {/* TAB 2: FULL LEAVE MANAGEMENT */}
      {tab === 'leaves' && (
        <div className="space-y-6">
          {/* Employee Balance Entitlements */}
          <div className="grid gap-3 sm:grid-cols-4">
            <Card className="bg-indigo-50/40 dark:bg-indigo-950/20">
              <CardContent className="pt-4">
                <p className="text-xs text-muted-foreground">Annual Leave Remaining</p>
                <p className="mt-1 text-2xl font-bold text-indigo-700 dark:text-indigo-400">
                  {leaveEmpObj?.annualLeaveBalance ?? 18} days
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4">
                <p className="text-xs text-muted-foreground">Sick Leave Quota</p>
                <p className="mt-1 text-2xl font-bold">12 days</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4">
                <p className="text-xs text-muted-foreground">Casual / Personal</p>
                <p className="mt-1 text-2xl font-bold">6 days</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-4">
                <p className="text-xs text-muted-foreground">Approved Requests (Team)</p>
                <p className="mt-1 text-2xl font-bold text-emerald-600">
                  {leaves.filter((l) => l.status === 'APPROVED').length}
                </p>
              </CardContent>
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-12">
            <Card className="lg:col-span-4">
              <CardHeader>
                <CardTitle className="text-base">Apply for Leave</CardTitle>
                <CardDescription>Auto-calculates requested days & checks leave balance</CardDescription>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault()
                    if (!activeLeaveEmpId) return
                    try {
                      const created = await submitLeave({
                        employeeId: activeLeaveEmpId,
                        leaveType,
                        startDate: leaveStart,
                        endDate: leaveEnd,
                        reason: leaveReason || undefined,
                      }).unwrap()
                      setLeaveReason('')
                      setFeedback({
                        type: 'ok',
                        text: `Submitted ${created.leaveType} leave (${created.daysCount} day(s)) for ${empMap.get(activeLeaveEmpId)?.fullName || ''}.`,
                      })
                    } catch (err: unknown) {
                      const msg = (err as { data?: { message?: string } })?.data?.message || 'Failed to apply leave'
                      setFeedback({ type: 'err', text: msg })
                    }
                  }}
                  className="space-y-3"
                >
                  <div>
                    <label className="mb-1 block text-xs font-medium text-muted-foreground">Employee</label>
                    <select
                      value={activeLeaveEmpId}
                      onChange={(e) => setLeaveEmpId(e.target.value)}
                      className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                    >
                      {employees.map((e) => (
                        <option key={e.id} value={e.id}>
                          {e.employeeCode} · {e.fullName} ({e.annualLeaveBalance}d left)
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-medium text-muted-foreground">Leave Category</label>
                    <select
                      value={leaveType}
                      onChange={(e) => setLeaveType(e.target.value)}
                      className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                    >
                      <option value="ANNUAL">Annual / Vacation</option>
                      <option value="SICK">Sick / Medical</option>
                      <option value="CASUAL">Casual Leave</option>
                      <option value="WFH">Work From Home</option>
                      <option value="COMP_OFF">Compensatory Off</option>
                      <option value="UNPAID">Unpaid Leave</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="mb-1 block text-xs font-medium text-muted-foreground">From</label>
                      <Input type="date" value={leaveStart} onChange={(e) => setLeaveStart(e.target.value)} required />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-muted-foreground">To</label>
                      <Input type="date" value={leaveEnd} onChange={(e) => setLeaveEnd(e.target.value)} required />
                    </div>
                  </div>
                  <div className="rounded-lg border bg-slate-50 px-3 py-2 text-xs dark:bg-muted/30">
                    Duration: <span className="font-semibold">{computedLeaveDays} day(s)</span>
                    {leaveType === 'ANNUAL' &&
                      leaveEmpObj &&
                      computedLeaveDays > leaveEmpObj.annualLeaveBalance && (
                        <span className="ml-2 font-medium text-destructive">
                          (Exceeds {leaveEmpObj.annualLeaveBalance}d balance)
                        </span>
                      )}
                  </div>
                  <Input
                    placeholder="Reason / handover note..."
                    value={leaveReason}
                    onChange={(e) => setLeaveReason(e.target.value)}
                  />
                  <Button type="submit" className="w-full" disabled={submittingLeave || computedLeaveDays <= 0}>
                    {submittingLeave ? 'Submitting...' : 'Apply Leave'}
                  </Button>
                </form>
              </CardContent>
            </Card>

            <Card className="lg:col-span-8">
              <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <CardTitle>Leave Requests & Approval Queue</CardTitle>
                  <CardDescription>Approve, reject, or cancel leave applications dynamically</CardDescription>
                </div>
                <div className="flex flex-wrap gap-1">
                  {(['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'] as const).map((st) => (
                    <Button
                      key={st}
                      size="sm"
                      variant={leaveStatusFilter === st ? 'default' : 'outline'}
                      onClick={() => setLeaveStatusFilter(st)}
                    >
                      {st}
                    </Button>
                  ))}
                </div>
              </CardHeader>
              <CardContent className="space-y-2.5 max-h-[500px] overflow-y-auto">
                {filteredLeaves.length === 0 ? (
                  <p className="py-8 text-center text-xs text-muted-foreground">
                    No leave requests match filter {leaveStatusFilter}.
                  </p>
                ) : (
                  filteredLeaves.map((l) => {
                    const emp = empMap.get(l.employeeId)
                    return (
                      <div
                        key={l.id}
                        className="flex flex-col gap-2 rounded-xl border p-3 text-xs sm:flex-row sm:items-center sm:justify-between"
                      >
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="font-semibold text-sm">{emp?.fullName || 'Employee'}</span>
                            <Badge variant="outline">{l.leaveType}</Badge>
                            <span className="font-medium text-indigo-600">{l.daysCount} day(s)</span>
                          </div>
                          <p className="mt-0.5 text-muted-foreground">
                            {l.startDate} → {l.endDate} · {l.reason || 'No note provided'}
                          </p>
                        </div>
                        <div className="flex flex-wrap items-center gap-1.5">
                          <Badge
                            variant={
                              l.status === 'APPROVED'
                                ? 'success'
                                : l.status === 'PENDING'
                                ? 'warning'
                                : 'secondary'
                            }
                          >
                            {l.status}
                          </Badge>
                          {l.status === 'PENDING' && (
                            <>
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={async () => {
                                  await decideLeave({ id: l.id, approved: true }).unwrap()
                                  setFeedback({ type: 'ok', text: `Approved leave for ${emp?.fullName || 'employee'}.` })
                                }}
                              >
                                <CheckCircle2 className="mr-1 h-3.5 w-3.5 text-emerald-600" /> Approve
                              </Button>
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={async () => {
                                  await decideLeave({ id: l.id, approved: false }).unwrap()
                                  setFeedback({ type: 'ok', text: `Rejected leave for ${emp?.fullName || 'employee'}.` })
                                }}
                              >
                                <XCircle className="mr-1 h-3.5 w-3.5 text-rose-600" /> Reject
                              </Button>
                              <Button
                                size="sm"
                                variant="ghost"
                                onClick={async () => {
                                  await cancelLeave(l.id).unwrap()
                                  setFeedback({ type: 'ok', text: 'Leave request cancelled.' })
                                }}
                              >
                                Cancel
                              </Button>
                            </>
                          )}
                        </div>
                      </div>
                    )
                  })
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* TAB 3: DYNAMIC EMPLOYEE DIRECTORY & PROFILE INSPECTOR */}
      {tab === 'employees' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-8">
            <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <CardTitle>Employee Directory ({filteredEmployees.length})</CardTitle>
                <CardDescription>Click any employee row to inspect leave balances, GPS punches & OKRs</CardDescription>
              </div>
              <div className="flex flex-wrap gap-2">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
                  <Input
                    placeholder="Search name, code, email..."
                    value={empSearch}
                    onChange={(e) => setEmpSearch(e.target.value)}
                    className="h-8 w-44 pl-8 text-xs"
                  />
                </div>
                <select
                  value={deptFilter}
                  onChange={(e) => setDeptFilter(e.target.value)}
                  className="h-8 rounded-md border border-input bg-background px-2.5 text-xs"
                >
                  <option value="ALL">All Departments</option>
                  {departments.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </div>
            </CardHeader>
            <CardContent className="space-y-2 max-h-[520px] overflow-y-auto">
              {filteredEmployees.map((e) => (
                <button
                  key={e.id}
                  type="button"
                  onClick={() => setSelectedEmpId(e.id)}
                  className={`flex w-full items-center justify-between rounded-xl border p-3 text-left text-xs transition-colors ${
                    inspectedEmployee?.id === e.id ? 'border-indigo-500 bg-indigo-50/40' : 'hover:bg-muted/40'
                  }`}
                >
                  <div>
                    <p className="font-semibold text-sm">
                      <span className="mr-2 font-mono text-xs text-muted-foreground">{e.employeeCode}</span>
                      {e.fullName}
                    </p>
                    <p className="text-muted-foreground">
                      {e.designation} · {e.departmentName || 'General'} · {e.workEmail}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="hidden font-mono text-xs sm:inline">${Number(e.baseMonthlySalary).toLocaleString()}/mo</span>
                    <Badge variant="outline">{e.annualLeaveBalance}d PTO</Badge>
                    <Badge variant={e.status === 'ACTIVE' ? 'success' : 'secondary'}>{e.status}</Badge>
                  </div>
                </button>
              ))}
            </CardContent>
          </Card>

          <div className="space-y-6 lg:col-span-4">
            {inspectedEmployee && (
              <Card className="border-indigo-200/80">
                <CardHeader>
                  <CardTitle className="text-base">{inspectedEmployee.fullName}</CardTitle>
                  <CardDescription>
                    {inspectedEmployee.employeeCode} · {inspectedEmployee.designation}
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-2 text-xs">
                  <div className="flex justify-between border-b pb-1.5">
                    <span className="text-muted-foreground">Department</span>
                    <span className="font-medium">{inspectedEmployee.departmentName || 'General'}</span>
                  </div>
                  <div className="flex justify-between border-b pb-1.5">
                    <span className="text-muted-foreground">Joined</span>
                    <span className="font-medium">{inspectedEmployee.joiningDate}</span>
                  </div>
                  <div className="flex justify-between border-b pb-1.5">
                    <span className="text-muted-foreground">Monthly Salary</span>
                    <span className="font-mono font-semibold">
                      ${Number(inspectedEmployee.baseMonthlySalary).toLocaleString()}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Annual Leave Balance</span>
                    <Badge variant="success">{inspectedEmployee.annualLeaveBalance} days</Badge>
                  </div>
                </CardContent>
              </Card>
            )}

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Onboard Employee</CardTitle>
              </CardHeader>
              <CardContent>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault()
                    try {
                      await createEmployee({
                        fullName: empName,
                        workEmail: empEmail,
                        designation: empDesignation,
                        departmentName: empDept,
                        baseMonthlySalary: Number(empSalary),
                      }).unwrap()
                      setEmpName('')
                      setEmpEmail('')
                      setEmpDesignation('')
                      setFeedback({ type: 'ok', text: 'Employee onboarded successfully.' })
                    } catch (err: unknown) {
                      const msg = (err as { data?: { message?: string } })?.data?.message || 'Failed to onboard employee'
                      setFeedback({ type: 'err', text: msg })
                    }
                  }}
                  className="space-y-2.5"
                >
                  <Input placeholder="Full Name" value={empName} onChange={(e) => setEmpName(e.target.value)} required />
                  <Input type="email" placeholder="Work Email" value={empEmail} onChange={(e) => setEmpEmail(e.target.value)} required />
                  <Input placeholder="Designation" value={empDesignation} onChange={(e) => setEmpDesignation(e.target.value)} required />
                  <Input placeholder="Department" value={empDept} onChange={(e) => setEmpDept(e.target.value)} />
                  <Input type="number" placeholder="Base Monthly Salary" value={empSalary} onChange={(e) => setEmpSalary(e.target.value)} required />
                  <Button type="submit" className="w-full">Onboard Employee</Button>
                </form>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* TAB 4: DYNAMIC PERFORMANCE OKR TRACKER */}
      {tab === 'goals' && (
        <div className="grid gap-6 lg:grid-cols-12">
          <Card className="lg:col-span-8">
            <CardHeader>
              <CardTitle>Performance KPIs & OKR Progress</CardTitle>
              <CardDescription>Update employee goal progress dynamically</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 max-h-[520px] overflow-y-auto">
              {goals.map((g) => {
                const emp = empMap.get(g.employeeId)
                return (
                  <div key={g.id} className="space-y-2 rounded-xl border p-3.5 text-xs">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="font-semibold text-sm">{g.title}</p>
                        <p className="text-muted-foreground">
                          {emp?.fullName || 'Employee'} · Target: {g.targetMetric}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant={g.progressPercent >= 100 ? 'success' : 'info'}>
                          {g.progressPercent}% · {g.reviewStatus}
                        </Badge>
                      </div>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                      <div
                        className="h-full rounded-full bg-indigo-600 transition-all"
                        style={{ width: `${g.progressPercent}%` }}
                      />
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() =>
                          void updateGoalProgress({ id: g.id, progressPercent: Math.max(0, g.progressPercent - 10) })
                        }
                      >
                        -10%
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() =>
                          void updateGoalProgress({ id: g.id, progressPercent: Math.min(100, g.progressPercent + 10) })
                        }
                      >
                        +10%
                      </Button>
                      {g.progressPercent < 100 && (
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => void updateGoalProgress({ id: g.id, progressPercent: 100 })}
                        >
                          Complete (100%)
                        </Button>
                      )}
                    </div>
                  </div>
                )
              })}
            </CardContent>
          </Card>

          <Card className="lg:col-span-4">
            <CardHeader>
              <CardTitle className="text-base">Assign OKR Goal</CardTitle>
            </CardHeader>
            <CardContent>
              <form
                onSubmit={async (e) => {
                  e.preventDefault()
                  if (!activeGoalEmpId) return
                  await createGoal({
                    employeeId: activeGoalEmpId,
                    title: goalTitle,
                    targetMetric: goalMetric,
                    progressPercent: 0,
                  }).unwrap()
                  setGoalTitle('')
                  setGoalMetric('')
                  setFeedback({ type: 'ok', text: 'Performance goal assigned.' })
                }}
                className="space-y-2.5"
              >
                <select
                  value={activeGoalEmpId}
                  onChange={(e) => setGoalEmpId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  {employees.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.employeeCode} · {e.fullName}
                    </option>
                  ))}
                </select>
                <Input placeholder="Goal Title" value={goalTitle} onChange={(e) => setGoalTitle(e.target.value)} required />
                <Input placeholder="Target Metric" value={goalMetric} onChange={(e) => setGoalMetric(e.target.value)} required />
                <Button type="submit" className="w-full">Create Goal</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
