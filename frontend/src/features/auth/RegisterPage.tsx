import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRegisterMutation } from '@/app/api/adminApi'
import { useAppDispatch } from '@/app/hooks'
import { setSession } from '@/app/slices/authSlice'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

const registerSchema = z.object({
  fullName: z.string().min(2, 'Full name is required'),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  organizationName: z.string().min(2, 'Organization name is required'),
  baseCurrency: z.string().min(3).max(10),
  timezone: z.string().min(2),
})

export function RegisterPage() {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const [registerAccount, { isLoading }] = useRegisterMutation()
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const form = useForm<z.infer<typeof registerSchema>>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      password: '',
      organizationName: '',
      baseCurrency: 'USD',
      timezone: 'UTC',
    },
  })

  const onSubmit = async (values: z.infer<typeof registerSchema>) => {
    setErrorMsg(null)
    try {
      const session = await registerAccount(values).unwrap()
      dispatch(
        setSession({
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          user: session.user,
        })
      )
      navigate('/')
    } catch {
      setErrorMsg('Registration failed. Email or organization slug may already be in use.')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <Card className="w-full max-w-lg">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground font-bold text-lg">
            Z
          </div>
          <CardTitle className="text-xl">Provision Your Zenius One Organization</CardTitle>
          <CardDescription>
            Creates your SSO identity, multi-tenant workspace, HQ branch, and default RBAC roles
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {errorMsg && (
            <div className="rounded-lg border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
              {errorMsg}
            </div>
          )}

          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-medium">Full Name</label>
                <Input placeholder="Aarav Mehta" {...form.register('fullName')} />
                {form.formState.errors.fullName && (
                  <p className="mt-1 text-xs text-destructive">
                    {form.formState.errors.fullName.message}
                  </p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium">Work Email</label>
                <Input type="email" placeholder="aarav@company.com" {...form.register('email')} />
                {form.formState.errors.email && (
                  <p className="mt-1 text-xs text-destructive">
                    {form.formState.errors.email.message}
                  </p>
                )}
              </div>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium">Password (min 8 chars)</label>
              <Input type="password" placeholder="••••••••••••" {...form.register('password')} />
              {form.formState.errors.password && (
                <p className="mt-1 text-xs text-destructive">
                  {form.formState.errors.password.message}
                </p>
              )}
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium">Organization Name</label>
              <Input placeholder="Acme Global Pvt Ltd" {...form.register('organizationName')} />
              {form.formState.errors.organizationName && (
                <p className="mt-1 text-xs text-destructive">
                  {form.formState.errors.organizationName.message}
                </p>
              )}
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs font-medium">Base Currency</label>
                <Input placeholder="USD" {...form.register('baseCurrency')} />
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium">Timezone</label>
                <Input placeholder="Asia/Kolkata" {...form.register('timezone')} />
              </div>
            </div>

            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? 'Provisioning Workspace...' : 'Create Organization & Sign In'}
            </Button>
          </form>

          <p className="border-t pt-4 text-center text-xs text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-primary hover:underline">
              Sign in
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

