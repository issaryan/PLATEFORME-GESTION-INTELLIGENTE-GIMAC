import { useMemo, type ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { EmptyState, ResourceDetail } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { Dossier } from '../../modele/certification'
import { useDossierDeLiaison, useHistoriqueDossier } from '../../hooks/certification'
import { dossierDescriptor } from '../../ressources/dossier'
import { useDescripteurDS } from '../../ressources/descripteur'
import { OngletAttestation, OngletCompletude, OngletFiche, OngletHorodatages, OngletProcesVerbal, OngletRegistre } from './dossier/onglets'
import { ResumeDecision } from './parcours/decision/ResumeDecision'

// DES-11 Dossier de mise en service (gabarit G3, CER-08 à CER-15) : composition de ResourceDetail et des
// onglets ; aucune règle ni aucun appel ici, les hooks de données portent tout accès aux services.
// Modèle hybride (D-56) : le circuit des quatre quitus disparaît ; l'onglet « Décision » résume le
// procès-verbal de certification et l'approbation de la sécurité et conduit au dossier de la liaison, où
// ils se signent. Une ancienne adresse ?onglet=circuit ouvre cet onglet.

const RENDUS: Record<string, (d: Dossier) => ReactNode> = {
  decision: (d) => <ResumeDecision liaisonId={d.liaisonId} />,
  completude: (d) => <OngletCompletude dossier={d} />,
  fiche: (d) => <OngletFiche dossier={d} />,
  procesVerbal: (d) => <OngletProcesVerbal dossier={d} />,
  horodatages: (d) => <OngletHorodatages dossier={d} />,
  attestation: (d) => <OngletAttestation dossier={d} />,
  registre: () => <OngletRegistre />,
}

/** Onglets du descripteur : « circuit » (quatre quitus) devient « decision » (D-56). */
const ongletDe = (cle: string | null) => (cle === 'circuit' ? 'decision' : cle ?? undefined)

function FicheDossier({ dossier }: { dossier: Dossier }) {
  const [recherche, setRecherche] = useSearchParams()
  const base = useDescripteurDS(dossierDescriptor)
  const { t } = useTranslation()
  const descripteur = useMemo(
    () => ({
      ...base,
      tabs: base.tabs?.map((o) => (o.key === 'circuit' ? { key: 'decision', label: t('decision.onglet'), render: RENDUS.decision } : { ...o, render: RENDUS[o.key] })),
    }),
    [base, t],
  )
  const historique = useHistoriqueDossier(dossier.id)
  return (
    <ResourceDetail
      descriptor={descripteur}
      row={dossier}
      mode="page"
      tab={ongletDe(recherche.get('onglet'))}
      onTabChange={(k) => setRecherche(k === 'detail' ? {} : { onglet: k }, { replace: true })}
      timeline={historique.data ?? []}
      onAction={(a, row) => a.run?.({ row: row ?? dossier, descriptor: descripteur })}
    />
  )
}

export default function DossierMiseEnServicePage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const q = useDossierDeLiaison(id)
  return (
    <EtatsRequete
      etat={q}
      isEmpty={(d) => d === null}
      emptyTitle={t('dossier.aucun', { liaison: id })}
      errorTitle={t('commun.erreur.titre')}
      retryLabel={t('commun.reessayer')}
    >
      {(d) => (d ? <FicheDossier dossier={d} /> : <EmptyState title={t('dossier.aucun', { liaison: id })} />)}
    </EtatsRequete>
  )
}
