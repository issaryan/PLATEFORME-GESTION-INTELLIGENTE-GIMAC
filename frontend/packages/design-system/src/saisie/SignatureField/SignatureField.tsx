import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { Hash } from '../../affichage/Hash/Hash'

export interface SignatureFieldProps {
  state?: 'idle' | 'waiting' | 'done' | 'error'
  /** Action signée, rappelée dans le texte. */
  action?: string
  /** Empreinte de ce qui sera signé. */
  hash?: string
  at?: string
  onSign?: () => void
}

// Bloc de réauthentification FIDO2 employé par SignDialog (§7.2).
export function SignatureField(p: SignatureFieldProps) {
  const st = p.state || 'idle'
  return (
    <div className={cx('gm-signature', 'gm-signature-' + st)} role="group" aria-label="Réauthentification forte">
      <div className="gm-signature-icon">
        <Icon name={st === 'done' ? 'shield-check' : st === 'error' ? 'circle-x' : 'pen-line'} size={24} />
      </div>
      <div style={{ flex: 1 }}>
        <div className="gm-banner-title">
          {st === 'done' ? 'Signature confirmée' : st === 'error' ? "La clé de sécurité n'a pas répondu" : 'Confirmez avec votre clé de sécurité'}
        </div>
        <div className="gm-field-help">
          {st === 'waiting'
            ? 'Touchez votre clé de sécurité dans les 60 secondes.'
            : st === 'done'
              ? 'Horodatée le ' + (p.at || '28/09/2026 14:05') + ", jeton d'horodatage joint."
              : st === 'error'
                ? 'Vérifiez que la clé est branchée puis réessayez.'
                : "L'action « " + (p.action || 'Signer le quitus') + ' » sera signée avec l\'empreinte affichée ci-dessous.'}
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
        <Button variant="primary" icon="key-round">Utiliser ma clé de sécurité</Button>
      ) : st === 'error' ? (
        <Button variant="secondary" icon="refresh">Réessayer</Button>
      ) : null}
    </div>
  )
}
