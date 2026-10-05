import { useTranslation } from 'react-i18next'
import { PageHeader } from '@gimac/design-system'
import { SectionSupervision } from '../composants/supervision'

// Supervision de la plateforme (D-61, D-79) : l'état en temps réel, les services, les serveurs et les alertes
// du tableau de bord de la console, servis aussi dans l'application interne pour PLATFORM_SUPERVISION_READ.
export default function SupervisionPage() {
  const { t } = useTranslation()
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <PageHeader title={t('console.supervision.titre')} subtitle={t('infra.widget.supervision.texte')} />
      <SectionSupervision />
    </div>
  )
}
