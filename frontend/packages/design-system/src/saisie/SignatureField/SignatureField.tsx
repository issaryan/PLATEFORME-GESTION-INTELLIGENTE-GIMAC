import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { Hash } from '../../affichage/Hash/Hash'
import { useLibelle } from '../../i18n/LangueProvider'

/**
 * État du bloc : error, la clé n'a pas répondu (réessayer) ; refused, la clé a répondu mais le service a refusé
 * l'acte (refus métier, aucune nouvelle tentative proposée : la demande doit d'abord être corrigée).
 */
export type EtatBlocSignature = 'idle' | 'waiting' | 'done' | 'error' | 'refused'

export interface SignatureFieldProps {
  state?: EtatBlocSignature
  /** Action signée, rappelée dans le texte. */
  action?: string
  /** Empreinte de ce qui sera signé. */
  hash?: string
  at?: string
  onSign?: () => void
  /** Bouton de signature désactivé (motif manquant), avec la raison en infobulle. */
  disabled?: boolean
  disabledReason?: string
}

/** DDR-15 : instant de la signature à l'heure de Douala (JJ/MM/AAAA HH:MM), jamais une date figée. */
const maintenantDouala = () => {
  const d = new Date(Date.now() + 3_600_000).toISOString()
  return `${d.slice(8, 10)}/${d.slice(5, 7)}/${d.slice(0, 4)} ${d.slice(11, 16)}`
}

// Bloc de réauthentification FIDO2 employé par SignDialog (§7.2).
export function SignatureField(p: SignatureFieldProps) {
  const t = useLibelle()
  const st = p.state || 'idle'
  const echec = st === 'error' || st === 'refused'
  return (
    <div className={cx('gm-signature', 'gm-signature-' + st, st === 'refused' && 'gm-signature-error')} role="group" aria-label={t('signature.label')}>
      <div className="gm-signature-icon">
        <Icon name={st === 'done' ? 'shield-check' : echec ? 'circle-x' : 'pen-line'} size={24} />
      </div>
      <div style={{ flex: 1 }}>
        <div className="gm-banner-title">
          {st === 'done' ? t('signature.confirmee') : st === 'refused' ? t('signature.refusee') : st === 'error' ? t('signature.erreur') : t('signature.confirmez')}
        </div>
        <div className="gm-field-help">
          {st === 'waiting'
            ? t('signature.toucher')
            : st === 'done'
              ? t('signature.horodatee', { at: p.at || maintenantDouala() })
              : st === 'refused'
                ? t('signature.refuseeAide')
                : st === 'error'
                  ? t('signature.verifier')
                  : t('signature.action', { action: p.action || t('signature.actionDefaut') })}
        </div>
        {p.hash ? (
          <div style={{ marginTop: 'var(--gm-space-2)' }}>
            <Hash value={p.hash} />
          </div>
        ) : null}
      </div>
      {st === 'waiting' ? (
        <span className="gm-spinner" aria-hidden="true" />
      ) : st === 'idle' ? (
        <Button variant="primary" icon="key-round" onClick={p.onSign} disabled={p.disabled} disabledReason={p.disabledReason}>{t('signature.utiliser')}</Button>
      ) : st === 'error' ? (
        <Button variant="secondary" icon="refresh" onClick={p.onSign}>{t('commun.reessayer')}</Button>
      ) : null}
    </div>
  )
}
