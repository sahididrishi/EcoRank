import { useOrders } from '@/hooks/useOrders'
import StatusBadge from '@/components/common/StatusBadge'
import { formatCents, formatRelative } from '@/lib/format'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function RecentOrdersFeed() {
  const { data, isLoading } = useOrders(0, 10)

  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <LoadingSpinner />
      </div>
    )
  }

  const orders = data?.content ?? []

  return (
    <div className="rounded-xl border border-gray-200 bg-white">
      <div className="border-b border-gray-200 px-6 py-4">
        <h3 className="text-sm font-semibold text-gray-900">Recent Orders</h3>
      </div>
      <div className="divide-y divide-gray-100">
        {orders.length === 0 ? (
          <p className="px-6 py-8 text-center text-sm text-gray-500">No orders yet.</p>
        ) : (
          orders.map((order) => (
            <div key={order.id} className="flex items-center justify-between px-6 py-3">
              <div>
                <p className="text-sm font-medium text-gray-900">{order.playerName}</p>
                <p className="text-xs text-gray-500">{order.productName}</p>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-sm font-medium text-gray-900">
                  {formatCents(order.amountCents)}
                </span>
                <StatusBadge status={order.status} />
                <span className="text-xs text-gray-400">{formatRelative(order.createdAt)}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
