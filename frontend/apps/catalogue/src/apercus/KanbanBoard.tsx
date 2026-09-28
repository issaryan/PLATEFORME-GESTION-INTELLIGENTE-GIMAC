import { KanbanBoard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <KanbanBoard
      label="Tableau du DES"
      columns={[
        { key: 'todo', label: 'À faire', cards: [
          { id: 'TSK-2026-0412', type: 'décision', title: 'Relances documentaires, lot 3', institution: 'Conformité', status: 'neutral', statusLabel: 'à faire', due: '10/10/2026' },
          { id: 'TSK-2026-0415', type: 'incident', title: 'Rapport circonstancié TCK-000318', institution: 'Chef de quart', status: 'neutral', statusLabel: 'à faire', due: '01/10/2026' }] },
        { key: 'doing', label: 'En cours', limit: 3, cards: [
          { id: 'TSK-2026-0401', type: 'projet', title: 'Révision du seuil de rejet LIA-2025-102', institution: 'Chef du DES', status: 'info', statusLabel: 'en cours', due: '02/10/2026' },
          { id: 'TSK-2026-0398', type: 'décision', title: 'Mise à jour de la matrice de sévérité', institution: 'RSSI', status: 'info', statusLabel: 'en cours', due: '30/09/2026' },
          { id: 'TSK-2026-0390', type: 'audit', title: 'Réexamen des accès, campagne T3', institution: 'Console', status: 'info', statusLabel: 'en cours', due: '27/09/2026', late: true },
          { id: 'TSK-2026-0388', type: 'projet', title: 'Plan de tests DagView', institution: 'Homologation', status: 'info', statusLabel: 'en cours', due: '04/10/2026' }] },
        { key: 'review', label: 'À vérifier', limit: 2, cards: [
          { id: 'TSK-2026-0380', type: 'incident', title: "Clôture TCK-000302, période d'observation", institution: 'Opérateur', status: 'warn', statusLabel: 'en attente de validation', due: '29/09/2026' }] },
        { key: 'done', label: 'Fait', cards: [] },
      ]}
    />
  )
}
