import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search } from 'lucide-react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/Dialog'
import { Input } from '@/components/ui/Input'
import { Badge } from '@/components/ui/Badge'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { setCommandPaletteOpen } from '@/app/slices/uiSlice'
import { useGetModulesQuery } from '@/app/api/systemApi'
import { ModuleIcon } from './ModuleIcon'

export function CommandPalette() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const open = useAppSelector((s) => s.ui.commandPaletteOpen)
  const [query, setQuery] = useState('')

  const { data } = useGetModulesQuery({ q: query || undefined, page: 0, size: 25 }, { skip: !open })
  const modules = data?.data ?? []

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        dispatch(setCommandPaletteOpen(!open))
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [dispatch, open])

  const handleSelect = (path: string) => {
    dispatch(setCommandPaletteOpen(false))
    setQuery('')
    navigate(path)
  }

  return (
    <Dialog open={open} onOpenChange={(next) => dispatch(setCommandPaletteOpen(next))}>
      <DialogContent className="p-0 overflow-hidden">
        <DialogTitle className="sr-only">Zenius One Command Palette</DialogTitle>
        <div className="flex items-center border-b px-3 py-2">
          <Search className="mr-2 h-4 w-4 text-muted-foreground" />
          <Input
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search Zenius One applications, bounded contexts, or routes..."
            className="border-0 shadow-none focus-visible:ring-0"
          />
          <kbd className="rounded border bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
            ESC
          </kbd>
        </div>
        <div className="max-h-80 overflow-y-auto p-2">
          {modules.map((mod) => (
            <button
              key={mod.id}
              type="button"
              onClick={() => handleSelect(mod.routePath)}
              className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm hover:bg-accent"
            >
              <div className="flex items-center gap-3">
                <span className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/10 text-primary">
                  <ModuleIcon name={mod.iconName} />
                </span>
                <div>
                  <p className="font-medium">{mod.name}</p>
                  <p className="text-xs text-muted-foreground">{mod.tagline}</p>
                </div>
              </div>
              <Badge variant="outline">{mod.category}</Badge>
            </button>
          ))}
        </div>
      </DialogContent>
    </Dialog>
  )
}

