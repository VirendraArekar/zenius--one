import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Search } from 'lucide-react'
import { useGetModulesQuery } from '@/app/api/systemApi'
import { Badge } from '@/components/ui/Badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { ModuleIcon } from '@/components/layout/ModuleIcon'

const CATEGORIES = ['ALL', 'PLATFORM', 'REVENUE', 'CUSTOMER', 'FINANCE', 'OPERATIONS', 'PEOPLE', 'WORKPLACE', 'INTELLIGENCE']

export function AppsDirectoryPage() {
  const [category, setCategory] = useState('ALL')
  const [query, setQuery] = useState('')

  const { data, isLoading, isError } = useGetModulesQuery({
    category: category === 'ALL' ? undefined : category,
    q: query.trim() || undefined,
    page: 0,
    size: 50,
  })

  const modules = data?.data ?? []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Zenius One Application Directory</h1>
        <p className="text-sm text-muted-foreground">
          Live server-filtered catalog from <code>GET /api/v1/system/modules</code>
        </p>
      </div>

      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex flex-wrap gap-1.5">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setCategory(cat)}
              className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                category === cat
                  ? 'border-primary bg-primary text-primary-foreground'
                  : 'bg-card text-muted-foreground hover:bg-accent'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        <div className="relative w-full md:w-72">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Filter modules..."
            className="pl-9"
          />
        </div>
      </div>

      {isLoading && <p className="text-sm text-muted-foreground">Querying backend module registry...</p>}
      {isError && (
        <p className="text-sm text-destructive">Failed to load modules from `/api/v1/system/modules`.</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {modules.map((mod) => (
          <Link key={mod.id} to={mod.routePath} className="group">
            <Card className="h-full transition-all group-hover:border-primary/60 group-hover:shadow-md">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between gap-2">
                  <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <ModuleIcon name={mod.iconName} className="h-5 w-5" />
                  </span>
                  <Badge variant={mod.status === 'ACTIVE' ? 'success' : 'secondary'}>
                    {mod.status === 'ACTIVE' ? 'ACTIVE' : `PHASE ${mod.phaseNumber}`}
                  </Badge>
                </div>
                <CardTitle className="pt-2">{mod.name}</CardTitle>
                <CardDescription>{mod.tagline}</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-xs text-muted-foreground line-clamp-3">{mod.description}</p>
                <div className="flex items-center justify-between text-[11px] text-muted-foreground">
                  <span className="font-mono">ctx: {mod.boundedContext}</span>
                  <span>{mod.category}</span>
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}

