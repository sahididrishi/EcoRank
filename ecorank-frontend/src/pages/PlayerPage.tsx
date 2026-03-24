import PlayerSearch from '@/components/players/PlayerSearch'

export default function PlayerPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h2 className="text-lg font-semibold text-gray-900">Player Lookup</h2>
      <PlayerSearch />
    </div>
  )
}
