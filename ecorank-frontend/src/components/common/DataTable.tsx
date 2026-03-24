import type { ReactNode } from 'react'
import LoadingSpinner from './LoadingSpinner'

export interface Column<T> {
  key: string
  header: string
  render: (item: T) => ReactNode
  className?: string
}

interface Props<T> {
  columns: Column<T>[]
  data: T[]
  loading?: boolean
  page?: number
  totalPages?: number
  onPageChange?: (page: number) => void
  onRowClick?: (item: T) => void
  keyExtractor: (item: T) => string | number
  emptyMessage?: string
}

export default function DataTable<T>({
  columns,
  data,
  loading,
  page,
  totalPages,
  onPageChange,
  onRowClick,
  keyExtractor,
  emptyMessage = 'No data found.',
}: Props<T>) {
  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center text-sm text-gray-500">
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((item) => (
              <tr
                key={keyExtractor(item)}
                onClick={() => onRowClick?.(item)}
                className={onRowClick ? 'cursor-pointer hover:bg-gray-50' : ''}
              >
                {columns.map((col) => (
                  <td key={col.key} className={col.className ?? 'px-4 py-3 text-sm text-gray-900'}>
                    {col.render(item)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
      {totalPages != null && totalPages > 1 && onPageChange && (
        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3">
          <button
            onClick={() => onPageChange(page! - 1)}
            disabled={page === 0}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-gray-600">
            Page {(page ?? 0) + 1} of {totalPages}
          </span>
          <button
            onClick={() => onPageChange(page! + 1)}
            disabled={page === totalPages - 1}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
