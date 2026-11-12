import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { KeyRound, Lock, ShieldCheck, Sparkles } from 'lucide-react'
import { useLoginMutation, useVerifyMfaLoginMutation } from '@/app/api/adminApi'
import { useAppDispatch } from '@/app/hooks'
import { setSession } from '@/app/slices/authSlice'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

const mfaSchema = z.object({
  code: z.string().regex(/^\d{6}$/, 'Enter the 6-digit authenticator code'),
})

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useAppDispatch()
  const [login, { isLoading }] = useLoginMutation()
  const [verifyMfa, { isLoading: verifyingMfa }] = useVerifyMfaLoginMutation()
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const redirectTarget = ((location.state as { from?: string } | null)?.from) || '/'

  const loginForm = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const mfaForm = useForm<z.infer<typeof mfaSchema>>({
    resolver: zodResolver(mfaSchema),
    defaultValues: { code: '' },
  })

  const onLoginSubmit = async (values: z.infer<typeof loginSchema>) => {
    setErrorMsg(null)
    try {
      const result = await login(values).unwrap()
      if (result.mfaRequired && result.mfaToken) {
        setMfaToken(result.mfaToken)
        return
      }
      if (result.session) {
        dispatch(
          setSession({
            accessToken: result.session.accessToken,
            refreshToken: result.session.refreshToken,
            user: result.session.user,
          })
        )
        navigate(redirectTarget, { replace: true })
      }
    } catch {
      setErrorMsg('Invalid email or password, or backend gateway is unreachable.')
    }
  }

  const onMfaSubmit = async (values: z.infer<typeof mfaSchema>) => {
    if (!mfaToken) return
    setErrorMsg(null)
    try {
      const session = await verifyMfa({ mfaToken, code: values.code }).unwrap()
      dispatch(
        setSession({
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          user: session.user,
        })
      )
      navigate(redirectTarget, { replace: true })
    } catch {
      setErrorMsg('Invalid or expired MFA verification code.')
    }
  }

  const fillDemoCredentials = () => {
    loginForm.setValue('email', 'admin@zeniusone.local', { shouldValidate: true })
    loginForm.setValue('password', 'Zenius@2026!', { shouldValidate: true })
  }

  return (
    <div className="grid min-h-screen bg-background lg:grid-cols-2">
      {/* Branded Enterprise Showcase Panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-slate-950 via-indigo-950 to-blue-950 p-10 text-white lg:flex">
        <div className="absolute -right-24 -top-24 h-80 w-80 rounded-full bg-blue-500/20 blur-3xl" />
        <div className="absolute -bottom-24 -left-24 h-80 w-80 rounded-full bg-indigo-500/20 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 text-lg font-bold backdrop-blur">
            Z
          </div>
          <div>
            <p className="text-base font-bold tracking-tight">Zenius One</p>
            <p className="text-xs text-blue-200/80">Enterprise Business Operating System · v1.0 GA</p>
          </div>
        </div>

        <div className="relative space-y-6">
          <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-medium text-blue-100">
            <Sparkles className="h-3.5 w-3.5" />
            <span>Microservices Gateway · 20 Unified Enterprise Modules</span>
          </div>
          <h1 className="text-4xl font-bold leading-tight tracking-tight">
            One Intelligent Operating System for Your Entire Organization.
          </h1>
          <p className="max-w-lg text-sm leading-relaxed text-slate-300">
            Sign in with SSO &amp; MFA to unlock CRM, Finance, Billing, Inventory, HR, Payroll, Projects, Collaboration,
            WorkDrive, Analytics BI, Low-Code Creator, Workflow Automation, and Zenius AI.
          </p>
          <div className="grid grid-cols-3 gap-3 pt-2 text-xs">
            <div className="rounded-xl border border-white/10 bg-white/5 p-3 backdrop-blur">
              <p className="text-lg font-bold">5 Services</p>
              <p className="text-slate-300">Spring Cloud Mesh</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/5 p-3 backdrop-blur">
              <p className="text-lg font-bold">20 Modules</p>
              <p className="text-slate-300">SSO &amp; RBAC Gated</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/5 p-3 backdrop-blur">
              <p className="text-lg font-bold">12 AI Agents</p>
              <p className="text-slate-300">RAG &amp; HITL Safe</p>
            </div>
          </div>
        </div>

        <p className="relative flex items-center gap-2 text-xs text-slate-400">
          <Lock className="h-3.5 w-3.5" />
          Protected by JWT rotation, TOTP MFA, and tenant-isolated RBAC policies
        </p>
      </div>

      {/* Authentication Form */}
      <div className="flex items-center justify-center p-4 sm:p-8">
        <Card className="animate-fadeSlideIn w-full max-w-md border-border/80 shadow-lg">
          <CardHeader className="space-y-2 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-lg font-bold text-primary-foreground shadow-sm">
              Z
            </div>
            <CardTitle className="text-2xl">
              {mfaToken ? 'Two-Factor Verification' : 'Sign in to Zenius One'}
            </CardTitle>
            <CardDescription>
              {mfaToken
                ? 'Enter the 6-digit TOTP code from your authenticator app'
                : 'Authenticate to unlock your organization apps and AI workspace'}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {errorMsg && (
              <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
                {errorMsg}
              </div>
            )}

            {!mfaToken ? (
              <form onSubmit={loginForm.handleSubmit(onLoginSubmit)} className="space-y-3.5">
                <div>
                  <label className="mb-1 block text-xs font-medium">Work Email</label>
                  <Input
                    type="email"
                    placeholder="admin@zeniusone.local"
                    {...loginForm.register('email')}
                  />
                  {loginForm.formState.errors.email && (
                    <p className="mt-1 text-xs text-destructive">
                      {loginForm.formState.errors.email.message}
                    </p>
                  )}
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium">Password</label>
                  <Input type="password" placeholder="••••••••" {...loginForm.register('password')} />
                  {loginForm.formState.errors.password && (
                    <p className="mt-1 text-xs text-destructive">
                      {loginForm.formState.errors.password.message}
                    </p>
                  )}
                </div>
                <Button type="submit" className="w-full" disabled={isLoading}>
                  {isLoading ? 'Signing in...' : 'Sign In'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="w-full text-xs"
                  onClick={fillDemoCredentials}
                >
                  <KeyRound className="h-3.5 w-3.5" />
                  <span>Use Demo Admin (admin@zeniusone.local)</span>
                </Button>
              </form>
            ) : (
              <form onSubmit={mfaForm.handleSubmit(onMfaSubmit)} className="space-y-3">
                <div>
                  <label className="mb-1 block text-xs font-medium">6-Digit TOTP Code</label>
                  <Input placeholder="123456" maxLength={6} {...mfaForm.register('code')} />
                  {mfaForm.formState.errors.code && (
                    <p className="mt-1 text-xs text-destructive">
                      {mfaForm.formState.errors.code.message}
                    </p>
                  )}
                </div>
                <Button type="submit" className="w-full" disabled={verifyingMfa}>
                  <ShieldCheck className="h-4 w-4" />
                  <span>{verifyingMfa ? 'Verifying...' : 'Verify & Continue'}</span>
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  className="w-full"
                  onClick={() => setMfaToken(null)}
                >
                  Back to password login
                </Button>
              </form>
            )}

            <div className="border-t pt-4 text-center text-xs text-muted-foreground">
              <p>
                New organization?{' '}
                <Link to="/register" className="font-medium text-primary hover:underline">
                  Create an enterprise workspace
                </Link>
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
