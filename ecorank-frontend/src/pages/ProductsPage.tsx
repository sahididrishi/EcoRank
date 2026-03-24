import { useNavigate } from 'react-router-dom'
import { useProducts, useToggleProductActive } from '@/hooks/useProducts'
import DataTable from '@/components/common/DataTable'
import type { Column } from '@/components/common/DataTable'
import type { Product } from '@/types/api'
import { formatCents } from '@/lib/format'
import clsx from 'clsx'

export default function ProductsPage() {
  const navigate = useNavigate()
  const { data: products, isLoading } = useProducts()
  const toggleActive = useToggleProductActive()

  const columns: Column<Product>[] = [
    { key: 'name', header: 'Name', render: (p) => <span className="font-medium">{p.name}</span> },
    { key: 'slug', header: 'Slug', render: (p) => <span className="text-gray-500">{p.slug}</span> },
    { key: 'price', header: 'Price', render: (p) => formatCents(p.priceCents) },
    { key: 'rankGroup', header: 'Rank', render: (p) => p.rankGroup ?? '-' },
    {
      key: 'active',
      header: 'Status',
      render: (p) => (
        <button
          onClick={(e) => {
            e.stopPropagation()
            toggleActive.mutate({ id: p.id, active: !p.active })
          }}
          className={clsx(
            'rounded-full px-2.5 py-0.5 text-xs font-medium',
            p.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600',
          )}
        >
          {p.active ? 'Active' : 'Inactive'}
        </button>
      ),
    },
  ]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">All Products</h2>
        <button
          onClick={() => navigate('/products/new')}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          Add Product
        </button>
      </div>
      <DataTable
        columns={columns}
        data={products ?? []}
        loading={isLoading}
        keyExtractor={(p) => p.id}
        onRowClick={(p) => navigate(`/products/${p.id}/edit`)}
        emptyMessage="No products yet. Create your first product."
      />
    </div>
  )
}
