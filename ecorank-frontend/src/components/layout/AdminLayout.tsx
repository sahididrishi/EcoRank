import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'

const titles: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/products': 'Products',
  '/orders': 'Orders',
  '/players': 'Players',
}

function getTitle(pathname: string): string {
  for (const [prefix, title] of Object.entries(titles)) {
    if (pathname.startsWith(prefix)) return title
  }
  return 'EcoRank'
}

export default function AdminLayout() {
  const location = useLocation()
  const title = getTitle(location.pathname)

  return (
    <div className="min-h-screen bg-gray-50">
      <Sidebar />
      <div className="pl-64">
        <Topbar title={title} />
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
