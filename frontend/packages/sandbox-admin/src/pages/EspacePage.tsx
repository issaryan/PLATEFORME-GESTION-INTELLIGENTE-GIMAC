import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router'
import { PageHeader } from '@gimac/design-system'
import { EspaceInstitution } from '../composants/EspaceInstitution'

// Espace d'une institution (/des/sandbox/espaces/:code) : fiche et actes de la console (UC-INS-09).
export default function EspacePage() {
  const { t } = useTranslation()
  const { code = '' } = useParams()
  return (
    <div className="gm-col">
      <PageHeader title={t('sbx.espace.titre', { code })} back={{ href: '/des/sandbox/espaces', label: t('sbx.espaces.titre') }} />
      <EspaceInstitution codeParticipant={code} ouvrable titre={t('sbx.espace.fiche')} />
    </div>
  )
}
