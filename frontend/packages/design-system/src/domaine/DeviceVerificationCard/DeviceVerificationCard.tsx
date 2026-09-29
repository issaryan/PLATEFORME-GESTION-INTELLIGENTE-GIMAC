import type { Status } from '../../lib/types'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Hash } from '../../affichage/Hash/Hash'
import { Card } from '../../conteneurs/Card/Card'
import { Button } from '../../actions/Button/Button'
import { PinField } from '../../saisie/PinField/PinField'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export type DeviceState = 'verified' | 'pending' | 'unverified' | 'revoked'

const MAP: Record<DeviceState, [Status, CleLibelle]> = {
  verified: ['ok', 'terminal.verifie'],
  pending: ['info', 'terminal.enCours'],
  unverified: ['warn', 'terminal.nonVerifie'],
  revoked: ['crit', 'terminal.revoque'],
}

export interface DeviceVerificationCardProps {
  name: string
  id: string
  /**
   * Empreinte de la clé du terminal. Le contrat publié la nommait « key », nom réservé par
   * React et donc jamais transmis au composant : elle est portée ici par « fingerprint ».
   */
  fingerprint?: string
  lastSeen: string
  platform: string
  state?: DeviceState
  /** Code court affiché pendant la vérification. */
  code?: string
}

// Carte d'un terminal (§7.7, profil DES-02 et console CON-09).
export function DeviceVerificationCard(p: DeviceVerificationCardProps) {
  const t = useLibelle()
  const st = p.state || 'unverified'
  const m = MAP[st]
  return (
    <Card
      title={p.name}
      actions={<StatusBadge status={m[0]} label={t(m[1])} icon="smartphone" />}
      footer={
        st === 'revoked'
          ? null
          : [
              <Button key={1} variant="danger" size="sm">{t('terminal.revoquer')}</Button>,
              st !== 'verified' ? <Button key={2} variant="primary" size="sm" icon="key-round">{t('terminal.verifierCode')}</Button> : null,
            ]
      }
    >
      <KeyValueList
        items={[
          { key: t('terminal.identifiant'), value: p.id, mono: true },
          { key: t('terminal.empreinte'), value: <Hash value={p.fingerprint || ''} /> },
          { key: t('terminal.activite'), value: p.lastSeen },
          { key: t('terminal.plateforme'), value: p.platform },
        ]}
      />
      {st === 'pending' && p.code ? (
        <div style={{ marginTop: 'var(--gm-space-4)' }}>
          <PinField label={t('terminal.code')} value={p.code} help={t('terminal.validite')} />
        </div>
      ) : null}
    </Card>
  )
}
