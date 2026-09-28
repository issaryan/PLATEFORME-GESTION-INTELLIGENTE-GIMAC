import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Hash } from '../../affichage/Hash/Hash'
import { Card } from '../../conteneurs/Card/Card'
import { Button } from '../../actions/Button/Button'

export interface RootPublicationCardProps {
  period: string
  root: string
  /** Jeton d'horodatage. */
  tst: string
  publication: string
  count: number
  at: string
  verdict?: 'ok' | 'mismatch'
}

// Racine de période du registre d'audit (§7.7, console CON-04).
export function RootPublicationCard(p: RootPublicationCardProps) {
  const ok = p.verdict === 'ok'
  return (
    <Card
      title={'Racine de période ' + p.period}
      actions={p.verdict ? <StatusBadge status={ok ? 'ok' : 'crit'} label={ok ? 'vérifiée' : 'non concordante'} icon={ok ? 'shield-check' : 'octagon-alert'} /> : null}
      footer={[
        <Button key={1} variant="secondary" icon="download">Télécharger la preuve</Button>,
        <Button key={2} variant="primary" icon="shield-check">Vérifier la racine</Button>,
      ]}
    >
      <KeyValueList
        items={[
          { key: 'Racine', value: <Hash value={p.root} /> },
          { key: "Jeton d'horodatage", value: <Hash value={p.tst} /> },
          { key: 'Référence de publication', value: p.publication, mono: true },
          { key: 'Événements couverts', value: <span className="gm-num">{p.count}</span> },
          { key: 'Publiée le', value: p.at },
        ]}
      />
    </Card>
  )
}
