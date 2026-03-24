import { Outlet, Link } from 'react-router-dom'

export default function StoreLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-4">
          <Link to="/store" className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-brand-600" />
            <span className="text-lg font-bold text-gray-900">EcoRank Store</span>
          </Link>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
