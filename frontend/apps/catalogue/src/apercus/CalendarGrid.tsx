import { CalendarGrid, type ShiftOperator } from '@gimac/design-system'

const op = (n: string, o?: Partial<ShiftOperator>): ShiftOperator => ({ name: n, ...(o || {}) })

export default function Apercu() {
  return (
    <CalendarGrid
      uncovered={2}
      grid={{
        jour: [[op('Chef de quart A', { lead: true }), op('Opérateur B')], [op('Chef de quart A', { lead: true }), op('Opérateur B')], [op('Chef de quart C', { lead: true }), op('Opérateur D')], [op('Chef de quart C', { lead: true }), op('Opérateur D')], [op('Chef de quart A', { lead: true }), op('Opérateur E')], [op('Chef de quart C', { lead: true })], [op('Chef de quart C', { lead: true })]],
        soir: [[op('Chef de quart C', { lead: true }), op('Opérateur E')], [op('Chef de quart C', { lead: true }), op('Opérateur E', { swap: true })], [op('Chef de quart A', { lead: true }), op('Opérateur B')], [op('Chef de quart A', { lead: true }), op('Opérateur B')], [], [op('Chef de quart A', { lead: true }), op('Opérateur D')], [op('Chef de quart A', { lead: true }), op('Opérateur D')]],
        nuit: [[op('Chef de quart F', { lead: true }), op('Opérateur D')], [op('Chef de quart F', { lead: true }), op('Opérateur D')], [op('Chef de quart F', { lead: true }), op('Opérateur E')], [], [op('Chef de quart F', { lead: true }), op('Opérateur B')], [op('Chef de quart F', { lead: true }), op('Opérateur E')], [op('Chef de quart C', { lead: true }), op('Opérateur B')]],
      }}
    />
  )
}
