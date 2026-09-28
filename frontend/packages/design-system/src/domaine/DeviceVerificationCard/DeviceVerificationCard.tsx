import type { Status } from '../../lib/types'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Hash } from '../../affichage/Hash/Hash'
import { Card } from '../../conteneurs/Card/Card'
import { Button } from '../../actions/Button/Button'
import { PinField } from '../../saisie/PinField/PinField'

export type DeviceState = 'verified' | 'pending' | 'unverified' | 'revoked'

const MAP: Record<DeviceState, [Status, string]> = {
  verified: ['ok', 'vérifié'],
  pending: ['info', 'vérification en cours'],
  unverified: ['warn', 'non vérifié'],
  revoked: ['crit', 'révoqué'],
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
  const st = p.state || 'unverified'
  const m = MAP[st]
  return (
    <Card
      title={p.name}
      actions={<StatusBadge status={m[0]} label={m[1]} icon="smartphone" />}
      footer={
        st === 'revoked'
          ? null
          : [
              <Button key={1} variant="danger" size="sm">Révoquer le terminal</Button>,
              st !== 'verified' ? <Button key={2} variant="primary" size="sm" icon="key-round">Vérifier avec un code court</Button> : null,
            ]
      }
    >
      <KeyValueList
        items={[
          { key: 'Identifiant', value: p.id, mono: true },
          { key: 'Empreinte de clé', value: <Hash value={p.fingerprint || ''} /> },
          { key: 'Dernière activité', value: p.lastSeen },
          { key: 'Plateforme', value: p.platform },
        ]}
      />
      {st === 'pending' && p.code ? (
        <div style={{ marginTop: 'var(--gm-space-4)' }}>
          <PinField label="Code court affiché sur l'autre terminal" value={p.code} help="Valable 10 minutes" />
        </div>
      ) : null}
    </Card>
  )
}
