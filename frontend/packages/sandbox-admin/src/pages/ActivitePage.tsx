import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { FilterBar, PageHeader, PageSection } from '@gimac/design-system'
import { useParticipantsActifs } from '../activite/hooks'
import { lireVue, PERIODES } from '../activite/filtres'
import { INTERFACES } from '../activite/modele'
import { ActiviteParInterface, CasDefavorables, CodesRejet, Indicateurs, TauxCampagnes, VerdictsParStatut } from '../activite/Graphiques'
import { Certification, Delais } from '../activite/Tableaux'

// « Activité des tests » (D-121) : tests effectués par les participants dans la sandbox (SBX-LECTURE), graphes et
// tableaux sur la même tranche (période, participant, interface), et leur suite dans le DES (MS-HOMOLOGATION).

export default function ActivitePage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const vue = lireVue(params, new Date())
  // Liste des participants du filtre : page bornée de l'agrégat, sur la période seule.
  const choix = useParticipantsActifs({ du: vue.filtre.du, au: vue.filtre.au }, 0, 100)
  const changer = (cle: string, valeur: string) => {
    const suivants = new URLSearchParams(params)
    if (valeur) suivants.set(cle, valeur)
    else suivants.delete(cle)
    setParams(suivants, { replace: true })
  }
  const actifs = [
    ...(vue.filtre.participant ? [{ key: 'participant', label: `${t('sbx.activite.participant')} : ${vue.filtre.participant}` }] : []),
    ...(vue.filtre.interface ? [{ key: 'interface', label: `${t('sbx.activite.interface')} : ${t(`sbx.activite.interface.${vue.filtre.interface}`)}` }] : []),
  ]
  return <div className="gm-col">
    <PageHeader title={t('sbx.activite.titre')} subtitle={t('sbx.activite.sousTitre')} />
    <FilterBar
      query={vue.filtre.participant ?? ''}
      onQuery={(q) => { if (q === '' || /^[0-9]{5}$/.test(q)) changer('participant', q) }}
      filters={[
        { key: 'periode', label: t('sbx.activite.periode'), value: String(vue.periode), options: PERIODES.map((p) => ({ value: String(p), label: t(`sbx.activite.periode.${p}`) })) },
        { key: 'participant', label: t('sbx.activite.participant'), value: vue.filtre.participant ?? '', options: (choix.data?.elements ?? []).map((p) => ({ value: p.codeParticipant, label: p.codeParticipant })) },
        { key: 'interface', label: t('sbx.activite.interface'), value: vue.filtre.interface ?? '', options: INTERFACES.map((i) => ({ value: i, label: t(`sbx.activite.interface.${i}`) })) },
      ]}
      onChange={changer}
      active={actifs}
      onRemove={(cle) => changer(cle, '')}
      onReset={() => setParams(new URLSearchParams(), { replace: true })}
    />
    <Indicateurs vue={vue} />
    <PageSection title={t('sbx.activite.titreActivite')}>
      <ActiviteParInterface vue={vue} />
    </PageSection>
    <div className="gm-graphiques-grille">
      <VerdictsParStatut vue={vue} />
      <TauxCampagnes vue={vue} />
      <CasDefavorables vue={vue} />
      <CodesRejet vue={vue} />
    </div>
    <Certification vue={vue} />
    <Delais vue={vue} />
  </div>
}
