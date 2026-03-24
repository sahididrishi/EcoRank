import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { formatCents } from '@/lib/format'

interface DataPoint {
  date: string
  revenue: number
}

export default function RevenueChart({ data }: { data: DataPoint[] }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6">
      <h3 className="text-sm font-semibold text-gray-900">Revenue Over Time</h3>
      <div className="mt-4 h-72">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis dataKey="date" tick={{ fontSize: 12 }} stroke="#9ca3af" />
            <YAxis
              tickFormatter={(v: number) => formatCents(v)}
              tick={{ fontSize: 12 }}
              stroke="#9ca3af"
            />
            <Tooltip
              formatter={(value: number) => [formatCents(value), 'Revenue']}
              contentStyle={{ borderRadius: '0.5rem', border: '1px solid #e5e7eb' }}
            />
            <Line
              type="monotone"
              dataKey="revenue"
              stroke="#16a34a"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
