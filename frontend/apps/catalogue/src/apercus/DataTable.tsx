import { Button, DataTable, type Column } from '@gimac/design-system'

interface Ligne {
  code: string
  nom: string
  pays: string
  statut: { status: 'ok' | 'warn' | 'crit' | 'info' | 'neutral'; label: string }
  conformite: { status: 'ok' | 'warn' | 'crit' | 'info' | 'neutral'; label: string }
  echeance: string
}

const cols: Column<Ligne>[] = [
  { key: 'code', header: 'Code', type: 'mono', width: 120, sortable: true },
  { key: 'nom', header: 'Dénomination', sortable: true },
  { key: 'pays', header: 'Pays' },
  { key: 'statut', header: 'Statut', type: 'status' },
  { key: 'conformite', header: 'Conformité documentaire', type: 'status' },
  { key: 'echeance', header: 'Prochaine échéance', type: 'datetime' },
  { key: 'actions', header: '', type: 'actions', render: () => <Button size="sm" variant="ghost">Ouvrir</Button> },
]
const rows: Ligne[] = [
  { code: 'INST-0042', nom: 'Établissement de paiement A', pays: 'Cameroun', statut: { status: 'ok', label: 'actif' }, conformite: { status: 'ok', label: 'conforme' }, echeance: '14/10/2026' },
  { code: 'INST-0017', nom: 'Banque B', pays: 'Gabon', statut: { status: 'ok', label: 'actif' }, conformite: { status: 'warn', label: 'à régulariser' }, echeance: '02/10/2026' },
  { code: 'INST-0088', nom: 'Établissement de microfinance C', pays: 'Tchad', statut: { status: 'info', label: 'en homologation' }, conformite: { status: 'neutral', label: 'en cours' }, echeance: '30/11/2026' },
  { code: 'INST-0061', nom: 'Banque D', pays: 'Congo', statut: { status: 'crit', label: 'suspendu' }, conformite: { status: 'crit', label: 'non conforme' }, echeance: '21/09/2026' },
]

export default function Apercu() {
  return (
    <DataTable
      columns={cols}
      rows={rows}
      rowKey={(r) => r.code}
      sort={{ key: 'nom', dir: 'asc' }}
      total={1284}
      page={1}
      pageSize={25}
      state="ok"
      toolbar={[<span key={1}>4 institutions sur cette page</span>, <span key={2} className="gm-grow" />, <Button key={3} size="sm" variant="ghost" icon="download">Exporter (contrôlé)</Button>]}
    />
  )
}
