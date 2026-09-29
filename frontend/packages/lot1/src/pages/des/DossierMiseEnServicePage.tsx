import { useMemo, type ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { EmptyState, ResourceDetail } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { Dossier } from '../../modele/certification'
import { useDossierDeLiaison, useHistoriqueDossier } from '../../hooks/certification'
import { dossierDescriptor } from '../../ressources/dossier'
import { useDescripteurDS } from '../../ressources/descripteur'
import { OngletAttestation, OngletCircuit, OngletCompletude, OngletFiche, OngletHorodatages, OngletProcesVerbal, OngletRegistre } from './dossier/onglets'

// DES-11 Dossier de mise en service (gabarit G3, CER-01 à CER-15) : composition de ResourceDetail et des
// onglets ; aucune règle ni aucun appel ici, les hooks de données portent tout accès aux services.

const RENDUS: Record<string, (d: Dossier) => ReactNode> = {
  circuit: (d) => <OngletCircuit dossier={d} />,
  completude: (d) => <OngletCompletude dossier={d} />,
  fiche: (d) => <OngletFiche dossier={d} />,
  procesVerbal: (d) => <OngletProcesVerbal dossier={d} />,
  horodatages: (d) => <OngletHorodatages dossier={d} />,
  attestation: (d) => <OngletAttestation dossier={d} />,
  registre: () => <OngletRegistre />,
}

function FicheDossier({ dossier, onglet }: { dossier: Dossier; onglet?: string }) {
  const base = useDescripteurDS(dossierDescriptor)
  const descripteur = useMemo(() => ({ ...base, tabs: base.tabs?.map((o) => ({ ...o, render: RENDUS[o.key] })) }), [base])
  const historique = useHistoriqueDossier(dossier.id)
  return (
    <ResourceDetail
      descriptor={descripteur}
      row={dossier}
      mode="page"
      tab={onglet}
      timeline={historique.data ?? []}
      onAction={(a, row) => a.run?.({ row: row ?? dossier, descriptor: descripteur })}
    />
  )
}

export default function DossierMiseEnServicePage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const [recherche] = useSearchParams()
  const q = useDossierDeLiaison(id)
  return (
    <EtatsRequete
      etat={q}
      isEmpty={(d) => d === null}
      emptyTitle={t('dossier.aucun', { liaison: id })}
      errorTitle={t('commun.erreur.titre')}
      retryLabel={t('commun.reessayer')}
    >
      {(d) => (d ? <FicheDossier dossier={d} onglet={recherche.get('onglet') ?? undefined} /> : <EmptyState title={t('dossier.aucun', { liaison: id })} />)}
    </EtatsRequete>
  )
}
