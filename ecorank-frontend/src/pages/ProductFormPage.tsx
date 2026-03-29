import { useParams, useNavigate } from 'react-router-dom'
import { useProduct, useCreateProduct, useUpdateProduct } from '@/hooks/useProducts'
import ProductForm from '@/components/products/ProductForm'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import type { CreateProductRequest } from '@/types/api'

export default function ProductFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = id !== undefined && id !== 'new'
  const { data: product, isLoading } = useProduct(isEdit ? Number(id) : 0)
  const create = useCreateProduct()
  const update = useUpdateProduct()

  function handleSubmit(data: CreateProductRequest) {
    if (isEdit) {
      update.mutate(
        { id: Number(id), req: data },
        { onSuccess: () => navigate('/products') },
      )
    } else {
      create.mutate(data, { onSuccess: () => navigate('/products') })
    }
  }

  if (isEdit && (isLoading || !product)) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h2 className="text-lg font-semibold text-gray-900">
        {isEdit ? 'Edit Product' : 'New Product'}
      </h2>
      <div className="mt-4 rounded-xl border border-gray-200 bg-white p-6">
        <ProductForm
          key={id}
          initial={product}
          onSubmit={handleSubmit}
          loading={create.isPending || update.isPending}
        />
      </div>
    </div>
  )
}
