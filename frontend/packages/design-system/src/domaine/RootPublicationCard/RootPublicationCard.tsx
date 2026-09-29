import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Hash } from '../../affichage/Hash/Hash'
import { Card } from '../../conteneurs/Card/Card'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

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
  const t = useLibelle()
  const ok = p.verdict === 'ok'
  return (
    <Card
      title={t('preuve.racine', { periode: p.period })}
      actions={p.verdict ? <StatusBadge status={ok ? 'ok' : 'crit'} label={ok ? t('racine.verifiee') : t('racine.nonConcordante')} icon={ok ? 'shield-check' : 'octagon-alert'} /> : null}
      footer={[
        <Button key={1} variant="secondary" icon="download">{t('preuve.telecharger')}</Button>,
        <Button key={2} variant="primary" icon="shield-check">{t('racine.verifier')}</Button>,
      ]}
    >
      <KeyValueList
        items={[
          { key: t('racine.racine'), value: <Hash value={p.root} /> },
          { key: t('preuve.jeton'), value: <Hash value={p.tst} /> },
          { key: t('racine.reference'), value: p.publication, mono: true },
          { key: t('racine.evenements'), value: <span className="gm-num">{p.count}</span> },
          { key: t('racine.publiee'), value: p.at },
        ]}
      />
    </Card>
  )
}
