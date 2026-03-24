import { useDashboardStats } from '@/hooks/useStats'
import { formatCents } from '@/lib/format'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import RecentOrdersFeed from '@/components/stats/RecentOrdersFeed'

export default function DashboardPage() {
  const { data: stats, isLoading } = useDashboardStats()

  if (isLoading || !stats) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  const cards = [
    { label: 'Total Revenue', value: formatCents(stats.totalRevenueCents), color: 'bg-brand-50 text-brand-700' },
    { label: 'Total Orders', value: stats.totalOrders.toLocaleString(), color: 'bg-blue-50 text-blue-700' },
    { label: 'Total Players', value: stats.totalPlayers.toLocaleString(), color: 'bg-purple-50 text-purple-700' },
    { label: 'Pending Orders', value: stats.pendingOrders.toLocaleString(), color: 'bg-yellow-50 text-yellow-700' },
  ]

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-4 gap-4">
        {cards.map((card) => (
          <div key={card.label} className="rounded-xl border border-gray-200 bg-white p-5">
            <p className="text-sm text-gray-500">{card.label}</p>
            <p className="mt-1 text-2xl font-bold text-gray-900">{card.value}</p>
          </div>
        ))}
      </div>
      <RecentOrdersFeed />
    </div>
  )
}
