import { useState, useEffect } from 'react'
import type { Product, CreateProductRequest } from '@/types/api'

interface Props {
  initial?: Product
  onSubmit: (data: CreateProductRequest) => void
  loading?: boolean
}

function toSlug(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}

export default function ProductForm({ initial, onSubmit, loading }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [slug, setSlug] = useState(initial?.slug ?? '')
  const [price, setPrice] = useState(initial ? (initial.priceCents / 100).toFixed(2) : '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [rankGroup, setRankGroup] = useState(initial?.rankGroup ?? '')
  const [category, setCategory] = useState(initial?.category ?? '')
  const [imageUrl, setImageUrl] = useState(initial?.imageUrl ?? '')
  const [autoSlug, setAutoSlug] = useState(!initial)

  useEffect(() => {
    if (autoSlug) setSlug(toSlug(name))
  }, [name, autoSlug])

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const priceCents = Math.round(parseFloat(price) * 100)
    if (isNaN(priceCents) || priceCents < 1) return
    onSubmit({
      name,
      slug,
      priceCents,
      description: description || undefined,
      rankGroup: rankGroup || undefined,
      category: category || undefined,
      imageUrl: imageUrl || undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Name</label>
          <input
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">Slug</label>
          <input
            type="text"
            required
            value={slug}
            onChange={(e) => {
              setSlug(e.target.value)
              setAutoSlug(false)
            }}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Price ($)</label>
          <input
            type="number"
            required
            step="0.01"
            min="0.01"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">LuckPerms Group</label>
          <input
            type="text"
            value={rankGroup}
            onChange={(e) => setRankGroup(e.target.value)}
            placeholder="e.g. vip"
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700">Description</label>
        <textarea
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Category</label>
          <input
            type="text"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">Image URL</label>
          <input
            type="url"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
      </div>
      <div className="flex justify-end">
        <button
          type="submit"
          disabled={loading}
          className="rounded-lg bg-brand-600 px-6 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-50"
        >
          {loading ? 'Saving...' : initial ? 'Update Product' : 'Create Product'}
        </button>
      </div>
    </form>
  )
}
