interface Props {
  title: string
  description: string
  action?: { label: string; onClick: () => void }
}

export default function EmptyState({ title, description, action }: Props) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-gray-300 bg-white py-12">
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={1} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
      </svg>
      <h3 className="mt-4 text-sm font-semibold text-gray-900">{title}</h3>
      <p className="mt-1 text-sm text-gray-500">{description}</p>
      {action && (
        <button
          onClick={action.onClick}
          className="mt-4 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
        >
          {action.label}
        </button>
      )}
    </div>
  )
}
