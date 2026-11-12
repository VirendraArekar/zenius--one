import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Building2,
  KeyRound,
  Layers,
  MapPin,
  ScrollText,
  Shield,
  ShieldCheck,
  Trash2,
  UserPlus,
  Users,
} from 'lucide-react'
import {
  useAddMemberMutation,
  useArchiveBranchMutation,
  useArchiveDepartmentMutation,
  useCreateApiKeyMutation,
  useCreateBranchMutation,
  useCreateDepartmentMutation,
  useCreateOrganizationMutation,
  useCreateRoleMutation,
  useDisableMfaMutation,
  useEnableMfaMutation,
  useGetApiKeysQuery,
  useGetAuditLogsQuery,
  useGetBranchesQuery,
  useGetDepartmentsQuery,
  useGetMembersQuery,
  useGetOrganizationsQuery,
  useGetPermissionsQuery,
  useGetRolesQuery,
  useGetSessionsQuery,
  useRevokeApiKeyMutation,
  useRevokeSessionMutation,
  useSetupMfaMutation,
  useSwitchOrganizationMutation,
} from '@/app/api/adminApi'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { setSession, setUserProfile } from '@/app/slices/authSlice'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable'
import { Input } from '@/components/ui/Input'

type AdminTab = 'orgs' | 'members' | 'branches' | 'departments' | 'roles' | 'security' | 'audit'

export function AdminConsolePage() {
  const dispatch = useAppDispatch()
  const { user, accessToken } = useAppSelector((s) => s.auth)
  const [activeTab, setActiveTab] = useState<AdminTab>('orgs')
  const [feedback, setFeedback] = useState<string | null>(null)

  // Organizations state
  const [newOrgName, setNewOrgName] = useState('')
  const [newOrgCurrency, setNewOrgCurrency] = useState('USD')

  // Branches state
  const [branchSearch, setBranchSearch] = useState('')
  const [branchCode, setBranchCode] = useState('')
  const [branchName, setBranchName] = useState('')
  const [branchCity, setBranchCity] = useState('')
  const [branchCountry, setBranchCountry] = useState('')

  // Departments state
  const [deptSearch, setDeptSearch] = useState('')
  const [deptCode, setDeptCode] = useState('')
  const [deptName, setDeptName] = useState('')
  const [deptDesc, setDeptDesc] = useState('')

  // Members state
  const [memberSearch, setMemberSearch] = useState('')
  const [memberName, setMemberName] = useState('')
  const [memberEmail, setMemberEmail] = useState('')
  const [memberPass, setMemberPass] = useState('')
  const [memberDesignation, setMemberDesignation] = useState('')
  const [memberRoleId, setMemberRoleId] = useState('')

  // Roles state
  const [roleCode, setRoleCode] = useState('')
  const [roleName, setRoleName] = useState('')
  const [selectedPermKeys, setSelectedPermKeys] = useState<string[]>(['org:read', 'notification:read'])

  // Security / MFA / API Keys state
  const [mfaSecretInfo, setMfaSecretInfo] = useState<{ secret: string; otpAuthUri: string } | null>(null)
  const [mfaCodeInput, setMfaCodeInput] = useState('')
  const [apiKeyName, setApiKeyName] = useState('')
  const [createdKeySecret, setCreatedKeySecret] = useState<string | null>(null)
  const [auditSearch, setAuditSearch] = useState('')

  const skipQueries = !accessToken || !user

  const { data: orgs = [] } = useGetOrganizationsQuery(undefined, { skip: skipQueries })
  const { data: branchesRes } = useGetBranchesQuery({ q: branchSearch || undefined }, { skip: skipQueries })
  const { data: deptsRes } = useGetDepartmentsQuery({ q: deptSearch || undefined }, { skip: skipQueries })
  const { data: membersRes } = useGetMembersQuery({ q: memberSearch || undefined }, { skip: skipQueries })
  const { data: roles = [] } = useGetRolesQuery(undefined, { skip: skipQueries })
  const { data: permissions = [] } = useGetPermissionsQuery(undefined, { skip: skipQueries })
  const { data: apiKeys = [] } = useGetApiKeysQuery(undefined, { skip: skipQueries })
  const { data: sessions = [] } = useGetSessionsQuery(undefined, { skip: skipQueries })
  const { data: auditRes } = useGetAuditLogsQuery({ q: auditSearch || undefined }, { skip: skipQueries })

  const [createOrg] = useCreateOrganizationMutation()
  const [switchOrg] = useSwitchOrganizationMutation()
  const [createBranch] = useCreateBranchMutation()
  const [archiveBranch] = useArchiveBranchMutation()
  const [createDept] = useCreateDepartmentMutation()
  const [archiveDept] = useArchiveDepartmentMutation()
  const [addMember] = useAddMemberMutation()
  const [createRole] = useCreateRoleMutation()
  const [setupMfa] = useSetupMfaMutation()
  const [enableMfa] = useEnableMfaMutation()
  const [disableMfa] = useDisableMfaMutation()
  const [revokeSession] = useRevokeSessionMutation()
  const [createApiKey] = useCreateApiKeyMutation()
  const [revokeApiKey] = useRevokeApiKeyMutation()

  if (!accessToken || !user) {
    return (
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>Authentication Required</CardTitle>
          <CardDescription>
            Sign in or provision an enterprise organization to manage multi-tenancy, employees,
            departments, branches, RBAC roles, MFA, API keys, and audit logs.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex gap-3">
          <Button asChild>
            <Link to="/login">Sign In</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link to="/register">Create Organization</Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const handleSwitchOrg = async (orgId: string) => {
    const session = await switchOrg(orgId).unwrap()
    dispatch(
      setSession({
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        user: session.user,
      })
    )
    setFeedback('Switched active organization context.')
  }

  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newOrgName.trim()) return
    const created = await createOrg({ name: newOrgName, baseCurrency: newOrgCurrency }).unwrap()
    setNewOrgName('')
    await handleSwitchOrg(created.id)
  }

  const handleCreateBranch = async (e: React.FormEvent) => {
    e.preventDefault()
    await createBranch({
      code: branchCode,
      name: branchName,
      city: branchCity || undefined,
      country: branchCountry || undefined,
    }).unwrap()
    setBranchCode('')
    setBranchName('')
    setBranchCity('')
    setBranchCountry('')
    setFeedback('Branch created.')
  }

  const handleCreateDept = async (e: React.FormEvent) => {
    e.preventDefault()
    await createDept({
      code: deptCode,
      name: deptName,
      description: deptDesc || undefined,
    }).unwrap()
    setDeptCode('')
    setDeptName('')
    setDeptDesc('')
    setFeedback('Department created.')
  }

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault()
    await addMember({
      fullName: memberName,
      email: memberEmail,
      temporaryPassword: memberPass || undefined,
      designation: memberDesignation || undefined,
      roleIds: memberRoleId ? [memberRoleId] : undefined,
    }).unwrap()
    setMemberName('')
    setMemberEmail('')
    setMemberPass('')
    setMemberDesignation('')
    setFeedback('Member added to active organization.')
  }

  const handleCreateRole = async (e: React.FormEvent) => {
    e.preventDefault()
    await createRole({
      code: roleCode,
      name: roleName,
      permissionKeys: selectedPermKeys,
    }).unwrap()
    setRoleCode('')
    setRoleName('')
    setFeedback('Custom role created.')
  }

  const togglePerm = (key: string) => {
    setSelectedPermKeys((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
    )
  }

  const handleEnableMfa = async (e: React.FormEvent) => {
    e.preventDefault()
    await enableMfa({ code: mfaCodeInput }).unwrap()
    dispatch(setUserProfile({ ...user, mfaEnabled: true }))
    setMfaSecretInfo(null)
    setMfaCodeInput('')
    setFeedback('TOTP MFA enabled.')
  }

  const handleDisableMfa = async (e: React.FormEvent) => {
    e.preventDefault()
    await disableMfa({ code: mfaCodeInput }).unwrap()
    dispatch(setUserProfile({ ...user, mfaEnabled: false }))
    setMfaCodeInput('')
    setFeedback('TOTP MFA disabled.')
  }

  const handleCreateApiKey = async (e: React.FormEvent) => {
    e.preventDefault()
    const created = await createApiKey({
      name: apiKeyName,
      scopes: ['org:read', 'branch:read', 'department:read', 'member:read'],
      expiresInDays: 90,
    }).unwrap()
    setApiKeyName('')
    setCreatedKeySecret(created.secretKey)
    setFeedback('API key generated. Copy the secret key now — it will not be shown again.')
  }

  const tabs: { id: AdminTab; label: string; icon: React.ReactNode }[] = [
    { id: 'orgs', label: 'Organizations', icon: <Building2 className="h-3.5 w-3.5" /> },
    { id: 'members', label: 'Members', icon: <Users className="h-3.5 w-3.5" /> },
    { id: 'branches', label: 'Branches', icon: <MapPin className="h-3.5 w-3.5" /> },
    { id: 'departments', label: 'Departments', icon: <Layers className="h-3.5 w-3.5" /> },
    { id: 'roles', label: 'Roles & RBAC', icon: <Shield className="h-3.5 w-3.5" /> },
    { id: 'security', label: 'Security & API Keys', icon: <KeyRound className="h-3.5 w-3.5" /> },
    { id: 'audit', label: 'Audit Logs', icon: <ScrollText className="h-3.5 w-3.5" /> },
  ]

  const auditColumns: DataTableColumn<(NonNullable<typeof auditRes>['data'])[number]>[] = [
    {
      key: 'createdAt',
      header: 'Timestamp',
      render: (r) => <span className="font-mono text-xs">{new Date(r.createdAt).toLocaleString()}</span>,
    },
    { key: 'actor', header: 'Actor', render: (r) => <span className="text-xs">{r.actorEmail ?? 'system'}</span> },
    { key: 'action', header: 'Action', render: (r) => <Badge variant="info">{r.action}</Badge> },
    { key: 'resource', header: 'Resource', render: (r) => <span className="text-xs">{r.resourceType}</span> },
    { key: 'summary', header: 'Summary', render: (r) => <span className="text-xs">{r.summary}</span> },
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Enterprise Administration</h1>
          <p className="text-sm text-muted-foreground">
            Signed in as <span className="font-medium text-foreground">{user.fullName}</span> ({user.email})
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="success">Multi-Tenant Isolated</Badge>
          {user.mfaEnabled && <Badge variant="info">MFA Active</Badge>}
        </div>
      </div>

      {feedback && (
        <div className="flex items-center justify-between rounded-lg border border-primary/30 bg-primary/10 px-4 py-2 text-xs text-primary">
          <span>{feedback}</span>
          <button type="button" onClick={() => setFeedback(null)} className="underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="flex flex-wrap gap-1.5 border-b pb-3">
        {tabs.map((tab) => (
          <Button
            key={tab.id}
            size="sm"
            variant={activeTab === tab.id ? 'default' : 'outline'}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.icon}
            <span>{tab.label}</span>
          </Button>
        ))}
      </div>

      {activeTab === 'orgs' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Your Organizations</CardTitle>
              <CardDescription>Switch active tenant workspace across Zenius One</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2.5">
              {orgs.map((org) => {
                const isActive = org.id === user.activeOrganizationId
                return (
                  <div
                    key={org.id}
                    className="flex items-center justify-between rounded-lg border p-3"
                  >
                    <div>
                      <p className="text-sm font-semibold">{org.name}</p>
                      <p className="text-xs text-muted-foreground">
                        slug: <code>{org.slug}</code> · {org.baseCurrency} · {org.timezone}
                      </p>
                    </div>
                    {isActive ? (
                      <Badge variant="success">Active</Badge>
                    ) : (
                      <Button size="sm" variant="outline" onClick={() => handleSwitchOrg(org.id)}>
                        Switch
                      </Button>
                    )}
                  </div>
                )
              })}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Provision New Organization</CardTitle>
              <CardDescription>Creates isolated tenant with default HQ, department, and RBAC roles</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateOrg} className="space-y-3">
                <div>
                  <label className="mb-1 block text-xs font-medium">Organization Name</label>
                  <Input
                    value={newOrgName}
                    onChange={(e) => setNewOrgName(e.target.value)}
                    placeholder="Zenius Europe GmbH"
                    required
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium">Base Currency</label>
                  <Input
                    value={newOrgCurrency}
                    onChange={(e) => setNewOrgCurrency(e.target.value)}
                    placeholder="EUR"
                  />
                </div>
                <Button type="submit">Create & Switch Organization</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'members' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader className="flex flex-row items-center justify-between gap-3">
              <div>
                <CardTitle>Organization Members</CardTitle>
                <CardDescription>Employees and RBAC assignments in active organization</CardDescription>
              </div>
              <Input
                value={memberSearch}
                onChange={(e) => setMemberSearch(e.target.value)}
                placeholder="Search members..."
                className="max-w-xs"
              />
            </CardHeader>
            <CardContent className="space-y-2">
              {(membersRes?.data ?? []).map((m) => (
                <div key={m.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <p className="text-sm font-semibold">{m.fullName}</p>
                    <p className="text-xs text-muted-foreground">
                      {m.email} {m.designation ? `· ${m.designation}` : ''}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-1">
                    {m.roles.map((r) => (
                      <Badge key={r.id} variant="secondary">
                        {r.code}
                      </Badge>
                    ))}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Add / Invite Member</CardTitle>
              <CardDescription>Requires <code>member:write</code> permission</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleAddMember} className="space-y-3">
                <Input
                  placeholder="Full Name"
                  value={memberName}
                  onChange={(e) => setMemberName(e.target.value)}
                  required
                />
                <Input
                  type="email"
                  placeholder="Email"
                  value={memberEmail}
                  onChange={(e) => setMemberEmail(e.target.value)}
                  required
                />
                <Input
                  type="password"
                  placeholder="Temp Password (new user)"
                  value={memberPass}
                  onChange={(e) => setMemberPass(e.target.value)}
                />
                <Input
                  placeholder="Designation"
                  value={memberDesignation}
                  onChange={(e) => setMemberDesignation(e.target.value)}
                />
                <select
                  value={memberRoleId}
                  onChange={(e) => setMemberRoleId(e.target.value)}
                  className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
                >
                  <option value="">Default (MEMBER)</option>
                  {roles.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name} ({r.code})
                    </option>
                  ))}
                </select>
                <Button type="submit" className="w-full">
                  <UserPlus className="h-4 w-4" />
                  <span>Add Member</span>
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'branches' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader className="flex flex-row items-center justify-between gap-3">
              <div>
                <CardTitle>Branches & Locations</CardTitle>
                <CardDescription>Physical offices and operating locations</CardDescription>
              </div>
              <Input
                value={branchSearch}
                onChange={(e) => setBranchSearch(e.target.value)}
                placeholder="Search branches..."
                className="max-w-xs"
              />
            </CardHeader>
            <CardContent className="space-y-2">
              {(branchesRes?.data ?? []).map((b) => (
                <div key={b.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-semibold">{b.code}</span>
                      <span className="text-sm font-semibold">{b.name}</span>
                      {b.headquarters && <Badge variant="info">HQ</Badge>}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {[b.city, b.country].filter(Boolean).join(', ') || 'No location metadata'}
                    </p>
                  </div>
                  <Button
                    size="icon"
                    variant="ghost"
                    aria-label="Archive branch"
                    onClick={() => archiveBranch(b.id)}
                  >
                    <Trash2 className="h-4 w-4 text-muted-foreground" />
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Branch</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateBranch} className="space-y-3">
                <Input placeholder="Code (e.g. BLR)" value={branchCode} onChange={(e) => setBranchCode(e.target.value)} required />
                <Input placeholder="Branch Name" value={branchName} onChange={(e) => setBranchName(e.target.value)} required />
                <Input placeholder="City" value={branchCity} onChange={(e) => setBranchCity(e.target.value)} />
                <Input placeholder="Country" value={branchCountry} onChange={(e) => setBranchCountry(e.target.value)} />
                <Button type="submit" className="w-full">Add Branch</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'departments' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader className="flex flex-row items-center justify-between gap-3">
              <div>
                <CardTitle>Departments</CardTitle>
                <CardDescription>Functional units within active organization</CardDescription>
              </div>
              <Input
                value={deptSearch}
                onChange={(e) => setDeptSearch(e.target.value)}
                placeholder="Search departments..."
                className="max-w-xs"
              />
            </CardHeader>
            <CardContent className="space-y-2">
              {(deptsRes?.data ?? []).map((d) => (
                <div key={d.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-semibold">{d.code}</span>
                      <span className="text-sm font-semibold">{d.name}</span>
                    </div>
                    <p className="text-xs text-muted-foreground">{d.description ?? 'No description'}</p>
                  </div>
                  <Button
                    size="icon"
                    variant="ghost"
                    aria-label="Archive department"
                    onClick={() => archiveDept(d.id)}
                  >
                    <Trash2 className="h-4 w-4 text-muted-foreground" />
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Department</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateDept} className="space-y-3">
                <Input placeholder="Code (e.g. ENG)" value={deptCode} onChange={(e) => setDeptCode(e.target.value)} required />
                <Input placeholder="Department Name" value={deptName} onChange={(e) => setDeptName(e.target.value)} required />
                <Input placeholder="Description" value={deptDesc} onChange={(e) => setDeptDesc(e.target.value)} />
                <Button type="submit" className="w-full">Add Department</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'roles' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Organization Roles</CardTitle>
              <CardDescription>System and custom RBAC roles</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {roles.map((role) => (
                <div key={role.id} className="rounded-lg border p-3 space-y-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-semibold">{role.name}</p>
                      <p className="text-xs text-muted-foreground">{role.code}</p>
                    </div>
                    <Badge variant={role.systemRole ? 'info' : 'secondary'}>
                      {role.systemRole ? 'SYSTEM' : 'CUSTOM'}
                    </Badge>
                  </div>
                  <div className="flex flex-wrap gap-1">
                    {role.permissionKeys.map((pk) => (
                      <Badge key={pk} variant="outline" className="font-mono text-[10px]">
                        {pk}
                      </Badge>
                    ))}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Create Custom Role</CardTitle>
              <CardDescription>Select fine-grained permissions</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateRole} className="space-y-3">
                <Input placeholder="Code (e.g. HR_MANAGER)" value={roleCode} onChange={(e) => setRoleCode(e.target.value)} required />
                <Input placeholder="Role Name" value={roleName} onChange={(e) => setRoleName(e.target.value)} required />
                <div className="max-h-56 overflow-y-auto rounded-lg border p-3 space-y-1.5">
                  {permissions.map((perm) => (
                    <label key={perm.id} className="flex items-center gap-2 text-xs">
                      <input
                        type="checkbox"
                        checked={selectedPermKeys.includes(perm.permissionKey)}
                        onChange={() => togglePerm(perm.permissionKey)}
                      />
                      <span className="font-mono font-medium">{perm.permissionKey}</span>
                      <span className="text-muted-foreground">— {perm.description}</span>
                    </label>
                  ))}
                </div>
                <Button type="submit">Create Custom Role</Button>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'security' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary" />
                <span>Multi-Factor Authentication (TOTP)</span>
              </CardTitle>
              <CardDescription>
                Status: {user.mfaEnabled ? 'Enabled' : 'Not enabled'}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {!user.mfaEnabled ? (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={async () => {
                      const res = await setupMfa().unwrap()
                      setMfaSecretInfo(res)
                    }}
                  >
                    Generate TOTP Secret
                  </Button>
                  {mfaSecretInfo && (
                    <form onSubmit={handleEnableMfa} className="space-y-2 rounded-lg border bg-muted/40 p-3">
                      <p className="text-xs font-medium">
                        Secret: <code className="font-mono">{mfaSecretInfo.secret}</code>
                      </p>
                      <p className="break-all text-[11px] text-muted-foreground">
                        URI: {mfaSecretInfo.otpAuthUri}
                      </p>
                      <Input
                        placeholder="Enter 6-digit code to enable"
                        maxLength={6}
                        value={mfaCodeInput}
                        onChange={(e) => setMfaCodeInput(e.target.value)}
                        required
                      />
                      <Button type="submit" size="sm">Verify & Enable MFA</Button>
                    </form>
                  )}
                </>
              ) : (
                <form onSubmit={handleDisableMfa} className="space-y-2">
                  <Input
                    placeholder="Enter 6-digit code to disable MFA"
                    maxLength={6}
                    value={mfaCodeInput}
                    onChange={(e) => setMfaCodeInput(e.target.value)}
                    required
                  />
                  <Button type="submit" variant="destructive" size="sm">Disable MFA</Button>
                </form>
              )}

              <div className="border-t pt-3">
                <h4 className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Active Refresh Sessions ({sessions.length})
                </h4>
                <div className="space-y-1.5">
                  {sessions.map((s) => (
                    <div key={s.id} className="flex items-center justify-between rounded border p-2 text-xs">
                      <span className="truncate max-w-xs">{s.userAgent || 'API Client'}</span>
                      <Button size="sm" variant="ghost" onClick={() => revokeSession(s.id)}>
                        Revoke
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Organization API Keys</CardTitle>
              <CardDescription>Scoped machine-to-machine credentials (<code>X-Api-Key</code>)</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <form onSubmit={handleCreateApiKey} className="flex gap-2">
                <Input
                  placeholder="Key name (e.g. BI Sync)"
                  value={apiKeyName}
                  onChange={(e) => setApiKeyName(e.target.value)}
                  required
                />
                <Button type="submit">Generate</Button>
              </form>
              {createdKeySecret && (
                <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-xs">
                  <p className="font-semibold">Copy Secret Key Now:</p>
                  <code className="break-all font-mono">{createdKeySecret}</code>
                </div>
              )}
              <div className="space-y-2">
                {apiKeys.map((k) => (
                  <div key={k.id} className="flex items-center justify-between rounded-lg border p-2.5 text-xs">
                    <div>
                      <p className="font-semibold">{k.name}</p>
                      <p className="font-mono text-muted-foreground">{k.keyPrefix}...</p>
                    </div>
                    {k.active ? (
                      <Button size="sm" variant="outline" onClick={() => revokeApiKey(k.id)}>
                        Revoke
                      </Button>
                    ) : (
                      <Badge variant="secondary">Revoked</Badge>
                    )}
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'audit' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Immutable Organization Audit Trail</h2>
            <Input
              value={auditSearch}
              onChange={(e) => setAuditSearch(e.target.value)}
              placeholder="Search audit events..."
              className="max-w-xs"
            />
          </div>
          <DataTable
            columns={auditColumns}
            data={auditRes?.data ?? []}
            rowKey={(r) => r.id}
            emptyMessage="No audit log entries found."
          />
        </div>
      )}
    </div>
  )
}

