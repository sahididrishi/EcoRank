import { Link } from 'react-router-dom'

export default function CheckoutSuccessPage() {
  return (
    <div className="flex flex-col items-center py-16">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
        <svg className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
        </svg>
      </div>
      <h2 className="mt-6 text-2xl font-bold text-gray-900">Payment Received!</h2>
      <p className="mt-2 text-gray-600">Your rank will be applied within 30 seconds.</p>
      <p className="mt-1 text-sm text-gray-500">If you're online, you'll see a message in-game.</p>
      <Link
        to="/store"
        className="mt-8 rounded-lg bg-brand-600 px-6 py-2 text-sm font-medium text-white hover:bg-brand-700"
      >
        Back to Store
      </Link>
    </div>
  )
}
