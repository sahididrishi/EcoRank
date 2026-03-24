import clsx from 'clsx'
import type { OrderStatus } from '@/types/api'

const statusColors: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
  PAID: 'bg-blue-100 text-blue-800',
  QUEUED: 'bg-purple-100 text-purple-800',
  FULFILLED: 'bg-green-100 text-green-800',
  REFUNDED: 'bg-gray-100 text-gray-800',
  FAILED: 'bg-red-100 text-red-800',
}

const statusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Pending',
  PAID: 'Paid',
  QUEUED: 'Queued',
  FULFILLED: 'Fulfilled',
  REFUNDED: 'Refunded',
  FAILED: 'Failed',
}

export default function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        statusColors[status],
      )}
    >
      {statusLabels[status]}
    </span>
  )
}
