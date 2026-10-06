import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PageHeader } from '@gimac/design-system'
import { SectionSupervision } from '../composants/supervision'
import { GraphiquesPlateforme, SelecteurPeriode } from '../composants/graphiquesSupervision'
import type { PeriodeSerie } from '../modele/plateforme'

const CLE_PERIODE = 'gimac.supervision.periode'

function periodeMemorisee(): PeriodeSerie {
  try {
    const v = window.localStorage.getItem(CLE_PERIODE)
    return v === 'SIX_HEURES' || v === 'UN_JOUR' || v === 'SEPT_JOURS' ? v : 'UNE_HEURE'
  } catch {
    return 'UNE_HEURE'
  }
}

// Centre de contrôle (D-61, D-79) : la période choisie en tête gouverne tout l'écran ; état en temps réel
// (anneau de disponibilité, tendances par service et par conteneur, fiche d'un service au clic), puis les
// courbes de la plateforme, des services et des serveurs. Les tableaux de l'état instantané restent en place.
export default function SupervisionPage() {
  const { t } = useTranslation()
  const [periode, setPeriode] = useState<PeriodeSerie>(periodeMemorisee)
  const choisir = (p: PeriodeSerie) => {
    setPeriode(p)
    try {
      window.localStorage.setItem(CLE_PERIODE, p)
    } catch {
      // Stockage indisponible (navigation privée) : la période vaut pour la visite.
    }
  }
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <PageHeader title={t('console.nav.supervision')} subtitle={t('graphiques.page.sousTitre')} />
      <SelecteurPeriode valeur={periode} onChange={choisir} />
      <SectionSupervision periode={periode} />
      <GraphiquesPlateforme periode={periode} />
    </div>
  )
}
