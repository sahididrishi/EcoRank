import { useState, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getStoreProducts } from '@/api/store'
import { createCheckout } from '@/api/store'
import { formatCents } from '@/lib/format'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function CheckoutPage() {
  const { slug } = useParams()
  const [playerName, setPlayerName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const idempotencyKey = useRef(crypto.randomUUID())

  const { data: products, isLoading } = useQuery({
    queryKey: ['store-products'],
    queryFn: getStoreProducts,
  })

  const product = products?.find((p) => p.slug === slug)

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!product) {
    return <p className="py-12 text-center text-gray-500">Product not found.</p>
  }

  async function handleCheckout(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { checkoutUrl } = await createCheckout({
        productSlug: product!.slug,
        playerName,
        idempotencyKey: idempotencyKey.current,
      })
      window.location.href = checkoutUrl
    } catch {
      idempotencyKey.current = crypto.randomUUID()
      setError('Failed to create checkout session. Please try again.')
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <div className="rounded-xl border border-gray-200 bg-white p-6">
        <h2 className="text-xl font-bold text-gray-900">{product.name}</h2>
        {product.description && <p className="mt-2 text-sm text-gray-600">{product.description}</p>}
        <p className="mt-4 text-3xl font-bold text-brand-600">{formatCents(product.priceCents)}</p>

        <form onSubmit={handleCheckout} className="mt-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Minecraft Username</label>
            <input
              type="text"
              required
              minLength={3}
              maxLength={16}
              pattern="[a-zA-Z0-9_]+"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              placeholder="Enter your username"
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
          {error && <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
          >
            {loading ? 'Redirecting to payment...' : 'Proceed to Payment'}
          </button>
        </form>
      </div>
    </div>
  )
}
