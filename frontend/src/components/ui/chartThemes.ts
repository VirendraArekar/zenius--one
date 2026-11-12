export const VIBRANT_PALETTE = [
  '#6366f1', // Indigo / Iris
  '#06b6d4', // Cyan / Electric Sky
  '#10b981', // Emerald / Mint
  '#f59e0b', // Amber / Gold
  '#ec4899', // Rose / Fuchsia
  '#8b5cf6', // Violet / Purple
  '#3b82f6', // Ocean Blue
  '#14b8a6', // Teal / Seafoam
  '#f97316', // Coral Orange
  '#d946ef', // Neon Magenta
]

export const STAGE_COLOR_MAP: Record<string, string> = {
  Prospect: '#3b82f6',
  Qualification: '#06b6d4',
  'Needs Analysis': '#8b5cf6',
  'Value Proposition': '#6366f1',
  Proposal: '#f59e0b',
  Negotiation: '#f97316',
  'Closed Won': '#10b981',
  'Closed Lost': '#ef4444',
}

export function getStageColor(stageName: string, index = 0): string {
  return STAGE_COLOR_MAP[stageName] ?? VIBRANT_PALETTE[index % VIBRANT_PALETTE.length]
}

export const chartTooltipStyle = {
  backgroundColor: 'rgba(15, 23, 42, 0.92)',
  borderColor: 'rgba(99, 102, 241, 0.3)',
  borderRadius: '0.75rem',
  boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.3)',
  color: '#f8fafc',
  padding: '8px 12px',
  fontSize: '12px',
}

