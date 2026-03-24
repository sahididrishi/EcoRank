import { useParams } from 'react-router-dom'
import { useOrder } from '@/hooks/useOrders'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StatusBadge from '@/components/common/StatusBadge'
import RefundButton from '@/components/orders/RefundButton'
import { formatCents, formatDateTime } from '@/lib/format'

export default function OrderDetailPage() {
  const { id } = useParams()
  const { data: order, isLoading } = useOrder(Number(id))

  if (isLoading || !order) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  const fields = [
    { label: 'Order ID', value: `#${order.id}` },
    { label: 'Player', value: order.playerName },
    { label: 'Player UUID', value: order.playerUuid },
    { label: 'Product', value: order.productName },
    { label: 'Amount', value: formatCents(order.amountCents) },
    { label: 'Created', value: formatDateTime(order.createdAt) },
    { label: 'Fulfilled', value: order.fulfilledAt ? formatDateTime(order.fulfilledAt) : '-' },
  ]

  return (
    <div className="mx-auto max-w-2xl">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">Order #{order.id}</h2>
        <div className="flex items-center gap-3">
          <StatusBadge status={order.status} />
          <RefundButton orderId={order.id} status={order.status} />
        </div>
      </div>
      <div className="mt-4 rounded-xl border border-gray-200 bg-white">
        <dl className="divide-y divide-gray-100">
          {fields.map((f) => (
            <div key={f.label} className="flex justify-between px-6 py-3">
              <dt className="text-sm text-gray-500">{f.label}</dt>
              <dd className="text-sm font-medium text-gray-900">{f.value}</dd>
            </div>
          ))}
        </dl>
      </div>
    </div>
  )
}
