import { useState } from 'react'
import { useRefundOrder } from '@/hooks/useOrders'
import ConfirmDialog from '@/components/common/ConfirmDialog'
import type { OrderStatus } from '@/types/api'

const refundableStatuses: OrderStatus[] = ['PAID', 'QUEUED', 'FULFILLED']

export default function RefundButton({ orderId, status }: { orderId: number; status: OrderStatus }) {
  const [open, setOpen] = useState(false)
  const refund = useRefundOrder()

  if (!refundableStatuses.includes(status)) return null

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
      >
        Refund
      </button>
      <ConfirmDialog
        open={open}
        title="Refund Order"
        message="This will refund the payment and remove the rank from the player. This action cannot be undone."
        confirmLabel="Refund"
        danger
        loading={refund.isPending}
        onConfirm={() => {
          refund.mutate(orderId, {
            onSuccess: () => setOpen(false),
          })
        }}
        onCancel={() => setOpen(false)}
      />
    </>
  )
}
