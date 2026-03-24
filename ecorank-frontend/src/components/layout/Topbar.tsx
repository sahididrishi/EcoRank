import { useAuth } from '@/context/AuthContext'

export default function Topbar({ title }: { title: string }) {
  const { logout } = useAuth()

  return (
    <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6">
      <h1 className="text-lg font-semibold text-gray-900">{title}</h1>
      <button
        onClick={logout}
        className="rounded-lg px-3 py-1.5 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-100 hover:text-gray-900"
      >
        Logout
      </button>
    </header>
  )
}
