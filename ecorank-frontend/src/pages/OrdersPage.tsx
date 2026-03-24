import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useOrders } from '@/hooks/useOrders'
import DataTable from '@/components/common/DataTable'
import type { Column } from '@/components/common/DataTable'
import StatusBadge from '@/components/common/StatusBadge'
import type { Order, OrderStatus } from '@/types/api'
import { formatCents, formatRelative } from '@/lib/format'

const statuses: (OrderStatus | '')[] = ['', 'PENDING_PAYMENT', 'PAID', 'QUEUED', 'FULFILLED', 'REFUNDED', 'FAILED']

export default function OrdersPage() {
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const navigate = useNavigate()
  const { data, isLoading } = useOrders(page, 20, statusFilter || undefined)

  const columns: Column<Order>[] = [
    { key: 'id', header: 'ID', render: (o) => `#${o.id}` },
    { key: 'player', header: 'Player', render: (o) => <span className="font-medium">{o.playerName}</span> },
    { key: 'product', header: 'Product', render: (o) => o.productName },
    { key: 'amount', header: 'Amount', render: (o) => formatCents(o.amountCents) },
    { key: 'status', header: 'Status', render: (o) => <StatusBadge status={o.status} /> },
    { key: 'date', header: 'Date', render: (o) => formatRelative(o.createdAt) },
  ]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">Orders</h2>
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value)
            setPage(0)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        >
          {statuses.map((s) => (
            <option key={s} value={s}>
              {s || 'All Statuses'}
            </option>
          ))}
        </select>
      </div>
      <DataTable
        columns={columns}
        data={data?.content ?? []}
        loading={isLoading}
        page={page}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setPage}
        keyExtractor={(o) => o.id}
        onRowClick={(o) => navigate(`/orders/${o.id}`)}
        emptyMessage="No orders found."
      />
    </div>
  )
}
