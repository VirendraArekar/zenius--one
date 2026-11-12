import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import { DndProvider, useDrag, useDrop } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { GripVertical } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'

const workspaceSchema = z.object({
  organizationName: z.string().min(2, 'Organization name must be at least 2 characters'),
  domainSlug: z
    .string()
    .min(3, 'Slug must be at least 3 characters')
    .regex(/^[a-z0-9-]+$/, 'Lowercase letters, numbers, and hyphens only'),
  adminEmail: z.string().email('Enter a valid enterprise email'),
})

type WorkspaceFormValues = z.infer<typeof workspaceSchema>

interface PriorityItem {
  id: string
  label: string
  phase: string
}

const INITIAL_PRIORITIES: PriorityItem[] = [
  { id: 'auth', label: 'SSO & Multi-Tenant RBAC', phase: 'Phase 2' },
  { id: 'crm', label: 'Sales & CRM Pipelines', phase: 'Phase 3' },
  { id: 'finance', label: 'General Ledger & GST/TDS', phase: 'Phase 5' },
  { id: 'ai', label: 'Zenius AI & pgvector RAG', phase: 'Phase 9' },
]

function DraggablePriorityRow({
  item,
  index,
  moveItem,
}: {
  item: PriorityItem
  index: number
  moveItem: (from: number, to: number) => void
}) {
  const [{ isDragging }, dragRef] = useDrag({
    type: 'PRIORITY_ITEM',
    item: { index },
    collect: (monitor) => ({ isDragging: monitor.isDragging() }),
  })

  const [, dropRef] = useDrop({
    accept: 'PRIORITY_ITEM',
    hover: (dragged: { index: number }) => {
      if (dragged.index !== index) {
        moveItem(dragged.index, index)
        dragged.index = index
      }
    },
  })

  return (
    <div
      ref={(node) => {
        dragRef(dropRef(node))
      }}
      className={`flex cursor-grab items-center justify-between rounded-lg border bg-background px-3 py-2 text-sm ${
        isDragging ? 'opacity-50' : ''
      }`}
    >
      <div className="flex items-center gap-2">
        <GripVertical className="h-4 w-4 text-muted-foreground" />
        <span className="font-medium">{item.label}</span>
      </div>
      <Badge variant="outline">{item.phase}</Badge>
    </div>
  )
}

export function DesignSystemPage() {
  const [validatedPreview, setValidatedPreview] = useState<WorkspaceFormValues | null>(null)
  const [priorities, setPriorities] = useState<PriorityItem[]>(INITIAL_PRIORITIES)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<WorkspaceFormValues>({
    resolver: zodResolver(workspaceSchema),
    defaultValues: {
      organizationName: '',
      domainSlug: '',
      adminEmail: '',
    },
  })

  const movePriority = (from: number, to: number) => {
    setPriorities((prev) => {
      const next = [...prev]
      const [moved] = next.splice(from, 1)
      next.splice(to, 0, moved)
      return next
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Zenius One UI Design System</h1>
        <p className="text-sm text-muted-foreground">
          Phase 1 component primitives, Zod form validation, FullCalendar, and React DnD workbench
        </p>
      </div>

      {/* Buttons & Badges */}
      <Card>
        <CardHeader>
          <CardTitle>Design Tokens, Buttons & Status Badges</CardTitle>
          <CardDescription>Tailwind CSS + Shadcn UI CVA variants with dark/light parity</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <Button>Primary Action</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="outline">Outline</Button>
            <Button variant="ghost">Ghost</Button>
            <Button variant="destructive">Destructive</Button>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge>Default</Badge>
            <Badge variant="success">Operational</Badge>
            <Badge variant="warning">Pending Approval</Badge>
            <Badge variant="info">Phase 2 Ready</Badge>
            <Badge variant="outline">Tenant Isolated</Badge>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* React Hook Form + Zod */}
        <Card>
          <CardHeader>
            <CardTitle>Validated Form Pattern (React Hook Form + Zod)</CardTitle>
            <CardDescription>
              Client-side schema validation preview (persisted to backend in Phase 2 Identity)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit((values) => setValidatedPreview(values))} className="space-y-3">
              <div>
                <label className="mb-1 block text-xs font-medium">Organization Name</label>
                <Input placeholder="Acme Global Pvt Ltd" {...register('organizationName')} />
                {errors.organizationName && (
                  <p className="mt-1 text-xs text-destructive">{errors.organizationName.message}</p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium">Tenant Slug</label>
                <Input placeholder="acme-global" {...register('domainSlug')} />
                {errors.domainSlug && (
                  <p className="mt-1 text-xs text-destructive">{errors.domainSlug.message}</p>
                )}
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium">Admin Email</label>
                <Input placeholder="admin@acme.com" {...register('adminEmail')} />
                {errors.adminEmail && (
                  <p className="mt-1 text-xs text-destructive">{errors.adminEmail.message}</p>
                )}
              </div>
              <Button type="submit">Validate Schema</Button>
            </form>
            {validatedPreview && (
              <pre className="mt-3 rounded-lg bg-muted p-3 text-xs overflow-x-auto">
                {JSON.stringify(validatedPreview, null, 2)}
              </pre>
            )}
          </CardContent>
        </Card>

        {/* React DnD Workbench */}
        <Card>
          <CardHeader>
            <CardTitle>Drag-and-Drop Widget Stack (React DnD)</CardTitle>
            <CardDescription>Drag items to reorder rollout priorities</CardDescription>
          </CardHeader>
          <CardContent>
            <DndProvider backend={HTML5Backend}>
              <div className="space-y-2">
                {priorities.map((item, idx) => (
                  <DraggablePriorityRow
                    key={item.id}
                    item={item}
                    index={idx}
                    moveItem={movePriority}
                  />
                ))}
              </div>
            </DndProvider>
          </CardContent>
        </Card>
      </div>

      {/* FullCalendar Component */}
      <Card>
        <CardHeader>
          <CardTitle>Shared Enterprise Calendar Engine (FullCalendar)</CardTitle>
          <CardDescription>Reusable across CRM Meetings, HR Leave, Projects & Social Publishing</CardDescription>
        </CardHeader>
        <CardContent>
          <FullCalendar
            plugins={[dayGridPlugin]}
            initialView="dayGridMonth"
            height={420}
            events={[
              { title: 'Phase 1 Foundation Milestone', date: '2026-11-12' },
              { title: 'Phase 2 Identity & Multi-Tenancy', date: '2026-11-19' },
            ]}
          />
        </CardContent>
      </Card>
    </div>
  )
}

