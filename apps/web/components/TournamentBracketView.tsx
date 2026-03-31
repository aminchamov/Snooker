import type { TournamentMatchRow, TournamentRow } from "@/lib/types";

type Props = {
  tournament: TournamentRow;
  matches: TournamentMatchRow[];
  playerNameById: Map<number, string>;
};

const CARD_HEIGHT = 74;
const CARD_GAP = 14;
const UNIT_HEIGHT = CARD_HEIGHT + CARD_GAP;
const CONNECTOR_WIDTH = 56;

type RoundBucket = {
  roundNumber: number;
  items: TournamentMatchRow[];
};

function toRoundBuckets(matches: TournamentMatchRow[], totalRounds: number): RoundBucket[] {
  const out: RoundBucket[] = [];
  for (let round = 1; round <= totalRounds; round++) {
    const items = matches
      .filter((m) => m.round_number === round)
      .sort((a, b) => a.bracket_position - b.bracket_position);
    out.push({ roundNumber: round, items });
  }
  return out;
}

function roundLabel(roundNumber: number, totalRounds: number): { short: string; long: string } {
  if (roundNumber === totalRounds) return { short: "FINAL", long: "FINAL" };
  if (roundNumber === totalRounds - 1) return { short: "SF", long: "Semi-Final" };
  if (roundNumber === totalRounds - 2) return { short: "QF", long: "Quarter-Final" };
  if (roundNumber === 1 && totalRounds >= 5) return { short: "R1", long: "Round of 32" };
  if (roundNumber === 1 && totalRounds === 4) return { short: "R1", long: "Round of 16" };
  return { short: `R${roundNumber}`, long: `Round ${roundNumber}` };
}

function getMatchNumber(match: TournamentMatchRow, totalPlayerCount: number): number {
  let number = 0;
  for (let round = 1; round < match.round_number; round++) {
    number += Math.floor(totalPlayerCount / 2 ** round);
  }
  number += match.bracket_position + 1;
  return number;
}

function playerName(playerId: number | null, playerNameById: Map<number, string>, fallback: string): string {
  if (!playerId) return fallback;
  return playerNameById.get(playerId) ?? `#${playerId}`;
}

function stateClass(state: string): string {
  if (state === "completed" || state === "bye") return "is-completed";
  if (state === "ready") return "is-ready";
  return "is-pending";
}

function BracketConnector({
  sourceMatchCount,
  sourceSlotHeight,
  isLeft
}: {
  sourceMatchCount: number;
  sourceSlotHeight: number;
  isLeft: boolean;
}) {
  const totalHeight = Math.max(sourceSlotHeight * Math.max(sourceMatchCount, 1), UNIT_HEIGHT);
  const pairCount = Math.floor(sourceMatchCount / 2);

  return (
    <svg
      className="bracket-connector"
      width={CONNECTOR_WIDTH}
      height={totalHeight}
      viewBox={`0 0 ${CONNECTOR_WIDTH} ${totalHeight}`}
      aria-hidden="true"
    >
      {sourceMatchCount < 2 ? (
        <path
          d={`M 0 ${totalHeight / 2} L ${CONNECTOR_WIDTH} ${totalHeight / 2}`}
          className="bracket-connector-path"
        />
      ) : (
        <>
          {Array.from({ length: pairCount }).map((_, pairIndex) => {
            const slot = totalHeight / sourceMatchCount;
            const topCenterY = pairIndex * 2 * slot + slot / 2;
            const bottomCenterY = topCenterY + slot;
            const midY = (topCenterY + bottomCenterY) / 2;
            const joinX = isLeft ? CONNECTOR_WIDTH * 0.45 : CONNECTOR_WIDTH * 0.55;
            const fromX = isLeft ? 0 : CONNECTOR_WIDTH;
            const toX = isLeft ? CONNECTOR_WIDTH : 0;
            return (
              <g key={`connector-${pairIndex}`}>
                <path d={`M ${fromX} ${topCenterY} L ${joinX} ${topCenterY}`} className="bracket-connector-path" />
                <path d={`M ${fromX} ${bottomCenterY} L ${joinX} ${bottomCenterY}`} className="bracket-connector-path" />
                <path d={`M ${joinX} ${topCenterY} L ${joinX} ${bottomCenterY}`} className="bracket-connector-path" />
                <path d={`M ${joinX} ${midY} L ${toX} ${midY}`} className="bracket-connector-path" />
              </g>
            );
          })}
        </>
      )}
    </svg>
  );
}

function RoundColumn({
  roundNumber,
  totalRounds,
  matches,
  playerNameById,
  playerCount,
  slotHeight,
  isFinal = false
}: {
  roundNumber: number;
  totalRounds: number;
  matches: TournamentMatchRow[];
  playerNameById: Map<number, string>;
  playerCount: number;
  slotHeight: number;
  isFinal?: boolean;
}) {
  const label = roundLabel(roundNumber, totalRounds);
  return (
    <div className="bracket-round-col">
      <div className="bracket-round-label">
        <span className="short">{label.short}</span>
        {label.short !== label.long ? <span className="long">{label.long}</span> : null}
      </div>
      <div className="bracket-round-body">
        {matches.map((match) => {
          const p1 = playerName(match.player1_id, playerNameById, match.state === "bye" ? "BYE" : "TBD");
          const p2 = playerName(match.player2_id, playerNameById, "TBD");
          const winner = match.winner_player_id ?? null;
          return (
            <div key={match.id} className="bracket-slot" style={{ height: `${slotHeight}px` }}>
              <article className={`bracket-match ${stateClass(match.state)} ${isFinal ? "is-final" : ""}`}>
                <div className="bracket-match-no">M{getMatchNumber(match, playerCount)}</div>
                <div className={`bracket-player ${winner === match.player1_id ? "is-winner" : ""}`}>
                  <span className="accent a1" />
                  <span className="name">{p1}</span>
                  {winner === match.player1_id ? <span className="win-mark">W</span> : null}
                </div>
                <div className="split" />
                <div className={`bracket-player ${winner === match.player2_id ? "is-winner" : ""}`}>
                  <span className="accent a2" />
                  <span className="name">{p2}</span>
                  {winner === match.player2_id ? <span className="win-mark">W</span> : null}
                </div>
                {match.state === "ready" ? <div className="bracket-ready">READY</div> : null}
              </article>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function TournamentBracketView({ tournament, matches, playerNameById }: Props) {
  const totalRounds = tournament.total_rounds;
  if (matches.length === 0 || totalRounds <= 0) {
    return <div className="empty">No bracket rows synced yet.</div>;
  }

  const rounds = toRoundBuckets(matches, totalRounds);
  const earlyRounds = rounds.filter((r) => r.roundNumber < totalRounds);
  const finalRound = rounds.find((r) => r.roundNumber === totalRounds)?.items ?? [];

  const leftRounds = earlyRounds.map((r) => {
    const half = Math.max(1, Math.floor(r.items.length / 2));
    return { roundNumber: r.roundNumber, items: r.items.slice(0, half) };
  });
  const rightRounds = earlyRounds.map((r) => {
    const half = Math.max(1, Math.floor(r.items.length / 2));
    return { roundNumber: r.roundNumber, items: r.items.slice(half) };
  });

  const championName =
    tournament.champion_player_id && playerNameById.get(tournament.champion_player_id)
      ? playerNameById.get(tournament.champion_player_id)
      : null;

  return (
    <div className="bracket-scroll">
      <div className="bracket-afcon">
        {leftRounds.map((round, index) => {
          const slotHeight = index === 0 ? UNIT_HEIGHT : UNIT_HEIGHT * 2 ** index;
          return (
            <div className="bracket-chunk" key={`left-${round.roundNumber}`}>
              <RoundColumn
                roundNumber={round.roundNumber}
                totalRounds={totalRounds}
                matches={round.items}
                playerNameById={playerNameById}
                playerCount={tournament.player_count}
                slotHeight={slotHeight}
              />
              <BracketConnector sourceMatchCount={round.items.length} sourceSlotHeight={slotHeight} isLeft />
            </div>
          );
        })}

        <div className="bracket-center">
          <div className="trophy">🏆</div>
          {championName ? <div className="champion-pill">Champion: {championName}</div> : null}
          <RoundColumn
            roundNumber={totalRounds}
            totalRounds={totalRounds}
            matches={finalRound}
            playerNameById={playerNameById}
            playerCount={tournament.player_count}
            slotHeight={UNIT_HEIGHT}
            isFinal
          />
        </div>

        {rightRounds
          .map((round, index) => ({ round, index }))
          .reverse()
          .map(({ round, index }) => {
            const slotHeight = index === 0 ? UNIT_HEIGHT : UNIT_HEIGHT * 2 ** index;
            return (
              <div className="bracket-chunk" key={`right-${round.roundNumber}`}>
                <BracketConnector sourceMatchCount={round.items.length} sourceSlotHeight={slotHeight} isLeft={false} />
                <RoundColumn
                  roundNumber={round.roundNumber}
                  totalRounds={totalRounds}
                  matches={round.items}
                  playerNameById={playerNameById}
                  playerCount={tournament.player_count}
                  slotHeight={slotHeight}
                />
              </div>
            );
          })}
      </div>
    </div>
  );
}
