import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import { formatCents } from '@/lib/format'

interface DataPoint {
  name: string
  revenue: number
}

export default function TopProductsChart({ data }: { data: DataPoint[] }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6">
      <h3 className="text-sm font-semibold text-gray-900">Top Products</h3>
      <div className="mt-4 h-72">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ left: 80 }}>
            <XAxis type="number" tickFormatter={(v: number) => formatCents(v)} tick={{ fontSize: 12 }} stroke="#9ca3af" />
            <YAxis type="category" dataKey="name" tick={{ fontSize: 12 }} stroke="#9ca3af" width={80} />
            <Tooltip
              formatter={(value: number) => [formatCents(value), 'Revenue']}
              contentStyle={{ borderRadius: '0.5rem', border: '1px solid #e5e7eb' }}
            />
            <Bar dataKey="revenue" fill="#16a34a" radius={[0, 4, 4, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
