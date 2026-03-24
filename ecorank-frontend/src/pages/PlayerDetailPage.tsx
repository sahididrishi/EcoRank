import { useParams, useNavigate } from 'react-router-dom'
import { usePlayer, usePlayerOrders } from '@/hooks/usePlayers'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import DataTable from '@/components/common/DataTable'
import type { Column } from '@/components/common/DataTable'
import StatusBadge from '@/components/common/StatusBadge'
import type { Order } from '@/types/api'
import { formatCents, formatDate, formatRelative } from '@/lib/format'

export default function PlayerDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const { data: player, isLoading: playerLoading } = usePlayer(uuid!)
  const { data: orders, isLoading: ordersLoading } = usePlayerOrders(uuid!)

  if (playerLoading || !player) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  const orderColumns: Column<Order>[] = [
    { key: 'id', header: 'ID', render: (o) => `#${o.id}` },
    { key: 'product', header: 'Product', render: (o) => o.productName },
    { key: 'amount', header: 'Amount', render: (o) => formatCents(o.amountCents) },
    { key: 'status', header: 'Status', render: (o) => <StatusBadge status={o.status} /> },
    { key: 'date', header: 'Date', render: (o) => formatRelative(o.createdAt) },
  ]

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white p-6">
        <img
          src={`https://crafatar.com/avatars/${player.minecraftUuid}?size=64&overlay`}
          alt={player.username}
          className="h-16 w-16 rounded-lg"
        />
        <div>
          <h2 className="text-xl font-bold text-gray-900">{player.username}</h2>
          <p className="text-sm text-gray-500">{player.minecraftUuid}</p>
          <p className="text-sm text-gray-500">Joined {formatDate(player.createdAt)}</p>
        </div>
      </div>

      <div>
        <h3 className="mb-3 text-sm font-semibold text-gray-900">Order History</h3>
        <DataTable
          columns={orderColumns}
          data={orders ?? []}
          loading={ordersLoading}
          keyExtractor={(o) => o.id}
          onRowClick={(o) => navigate(`/orders/${o.id}`)}
          emptyMessage="No orders for this player."
        />
      </div>
    </div>
  )
}
