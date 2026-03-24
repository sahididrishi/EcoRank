import { useState, useEffect } from 'react'
import { usePlayerSearch } from '@/hooks/usePlayers'
import { useNavigate } from 'react-router-dom'

export default function PlayerSearch() {
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const navigate = useNavigate()
  const { data: players, isLoading } = usePlayerSearch(debouncedQuery)

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300)
    return () => clearTimeout(timer)
  }, [query])

  return (
    <div>
      <input
        type="text"
        placeholder="Search players by username..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
      />
      {debouncedQuery.length >= 2 && (
        <div className="mt-2 rounded-xl border border-gray-200 bg-white">
          {isLoading ? (
            <p className="px-4 py-3 text-sm text-gray-500">Searching...</p>
          ) : !players || players.length === 0 ? (
            <p className="px-4 py-3 text-sm text-gray-500">No players found.</p>
          ) : (
            <div className="divide-y divide-gray-100">
              {players.map((player) => (
                <button
                  key={player.minecraftUuid}
                  onClick={() => navigate(`/players/${player.minecraftUuid}`)}
                  className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-gray-50"
                >
                  <img
                    src={`https://crafatar.com/avatars/${player.minecraftUuid}?size=32&overlay`}
                    alt={player.username}
                    className="h-8 w-8 rounded"
                  />
                  <span className="text-sm font-medium text-gray-900">{player.username}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
