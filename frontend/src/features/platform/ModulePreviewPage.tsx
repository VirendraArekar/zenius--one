import { Link } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, Clock } from 'lucide-react'
import { useGetModuleByCodeQuery } from '@/app/api/systemApi'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { ModuleIcon } from '@/components/layout/ModuleIcon'

export function ModulePreviewPage({ code }: { code: string }) {
  const { data: mod, isLoading, isError } = useGetModuleByCodeQuery(code)

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Loading module metadata from backend...</p>
  }

  if (isError || !mod) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Module Unavailable</CardTitle>
          <CardDescription>Could not load module `{code}` from `/api/v1/system/modules/{code}`.</CardDescription>
        </CardHeader>
        <CardContent>
          <Button variant="outline" asChild>
            <Link to="/">
              <ArrowLeft className="h-4 w-4" />
              <span>Return Home</span>
            </Link>
          </Button>
        </CardContent>
      </Card>
    )
  }

  const isActive = mod.status === 'ACTIVE'

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link to="/apps">
          <ArrowLeft className="h-4 w-4" />
          <span>Back to All Apps</span>
        </Link>
      </Button>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <ModuleIcon name={mod.iconName} className="h-6 w-6" />
              </span>
              <div>
                <CardTitle className="text-xl">{mod.name}</CardTitle>
                <CardDescription>{mod.tagline}</CardDescription>
              </div>
            </div>
            <Badge variant={isActive ? 'success' : 'warning'}>
              {isActive ? 'ACTIVE' : `PLANNED — PHASE ${mod.phaseNumber}`}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm leading-relaxed text-muted-foreground">{mod.description}</p>

          <div className="grid gap-3 rounded-lg border bg-muted/40 p-4 sm:grid-cols-3 text-xs">
            <div>
              <p className="text-muted-foreground">Bounded Context</p>
              <p className="mt-0.5 font-mono font-semibold">{mod.boundedContext}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Category</p>
              <p className="mt-0.5 font-semibold">{mod.category}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Target Milestone</p>
              <p className="mt-0.5 font-semibold">Phase {mod.phaseNumber}</p>
            </div>
          </div>

          <div className="flex items-start gap-2.5 rounded-lg border border-dashed p-4 text-xs text-muted-foreground">
            {isActive ? (
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
            ) : (
              <Clock className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
            )}
            <p>
              {isActive
                ? 'This module is active in the current build.'
                : `Per Zenius One strict delivery rules, business screens and mock records for ${mod.name} are intentionally withheld until Phase ${mod.phaseNumber} backend entities, Flyway migrations, authorization, and integration tests are built.`}
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

