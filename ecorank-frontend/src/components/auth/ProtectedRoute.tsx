import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />
}
