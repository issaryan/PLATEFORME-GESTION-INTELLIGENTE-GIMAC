import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Skeleton } from '@gimac/design-system'
import { deconnecter } from './deconnexion'

// Route <couche>/deconnexion de l'espace personnel (D-62) : déconnexion par la passerelle, une seule fois.
export function PageDeconnexion({ agir = deconnecter }: { agir?: () => Promise<void> }) {
  const { t } = useTranslation()
  const lance = useRef(false)
  useEffect(() => {
    if (lance.current) return
    lance.current = true
    void agir()
  }, [agir])
  return (
    <div className="gm-col" role="status" aria-live="polite">
      <span>{t('deconnexion.enCours')}</span>
      <Skeleton height={8} />
    </div>
  )
}
