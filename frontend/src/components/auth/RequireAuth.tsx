import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAppSelector } from '@/app/hooks'

/**
 * Route guard enforcing authentication before rendering any Zenius One application
 * layout, sidebar navigation, or business module workspace.
 */
export function RequireAuth() {
  const { accessToken, user } = useAppSelector((s) => s.auth)
  const location = useLocation()

  if (!accessToken || !user) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }

  return <Outlet />
}

