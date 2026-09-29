import { useState, type ReactNode } from 'react'
import { Banner } from '../../conteneurs/Banner/Banner'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface OneTimeSecretProps {
  /** Nature du secret, titre de l'avertissement (« Mot de passe provisoire »). */
  label: string
  value: string
  /** Avertissement propre ; par défaut, affichage unique et transmission par un canal sûr. */
  warning?: ReactNode
  /** Copie ; par défaut, presse-papiers du navigateur. */
  onCopy?: (value: string) => Promise<void> | void
}

// Secret affiché une seule fois (mot de passe provisoire…), composé de Banner, puce gm-chip et Button :
// avertissement, valeur en chasse fixe, copie et confirmation annoncée (extension D-62).
export function OneTimeSecret(p: OneTimeSecretProps) {
  const t = useLibelle()
  const [copie, setCopie] = useState<'idle' | 'ok' | 'ko'>('idle')
  const copier = async () => {
    try {
      await (p.onCopy ? p.onCopy(p.value) : navigator.clipboard.writeText(p.value))
      setCopie('ok')
    } catch {
      setCopie('ko')
    }
  }
  return (
    <div className="gm-col" role="group" aria-label={p.label}>
      <Banner level="warn" title={p.label}>{p.warning ?? t('secret.unique')}</Banner>
      <div className="gm-row">
        <span className="gm-chip" data-testid="secret-valeur">{p.value}</span>
        <Button size="sm" icon="copy" onClick={() => void copier()}>{t('secret.copier')}</Button>
      </div>
      <span className="gm-field-help" role="status" data-testid="secret-copie">{copie === 'ok' ? t('secret.copie') : copie === 'ko' ? t('secret.copieImpossible') : ''}</span>
    </div>
  )
}
