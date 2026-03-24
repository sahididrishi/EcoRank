import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getStoreProducts } from '@/api/store'
import { formatCents } from '@/lib/format'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function StorefrontPage() {
  const navigate = useNavigate()
  const { data: products, isLoading } = useQuery({
    queryKey: ['store-products'],
    queryFn: getStoreProducts,
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <div>
      <h2 className="text-2xl font-bold text-gray-900">Server Store</h2>
      <p className="mt-1 text-gray-600">Purchase ranks and perks for the server.</p>
      <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {products?.map((product) => (
          <div
            key={product.id}
            className="flex flex-col rounded-xl border border-gray-200 bg-white p-6 transition-shadow hover:shadow-md"
          >
            {product.imageUrl && (
              <img
                src={product.imageUrl}
                alt={product.name}
                className="mb-4 h-40 w-full rounded-lg object-cover"
              />
            )}
            <h3 className="text-lg font-semibold text-gray-900">{product.name}</h3>
            {product.description && (
              <p className="mt-2 flex-1 text-sm text-gray-600">{product.description}</p>
            )}
            <div className="mt-4 flex items-center justify-between">
              <span className="text-2xl font-bold text-brand-600">
                {formatCents(product.priceCents)}
              </span>
              <button
                onClick={() => navigate(`/store/checkout/${product.slug}`)}
                className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
              >
                Buy Now
              </button>
            </div>
          </div>
        ))}
        {(!products || products.length === 0) && (
          <div className="col-span-full py-12 text-center text-gray-500">
            No products available right now.
          </div>
        )}
      </div>
    </div>
  )
}
