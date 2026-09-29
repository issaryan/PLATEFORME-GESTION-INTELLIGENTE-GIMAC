import { useMemo, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { Banner, ResourceDetail } from '@gimac/design-system'
import type { Liaison } from '../../modele/homologation'
import { useEtatReprise, useHistoriqueLiaison, useLiaison, useOptionsCollaborateurs, useOptionsInstitutions } from '../../hooks/liaisons'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerLiaisonDescriptor } from '../../ressources/liaison'
import { Donnees } from './liaisons/communs'
import { FormulaireChefDeProjet } from './liaisons/formulaires'
import { OngletGraphe, OngletOperations, OngletPerimetre, OngletPreuves, OngletResultats } from './liaisons/onglets'

// DES-09 Vue d'une liaison (/des/liaisons/:id, gabarits G3 et G6 ; HOM-05 à HOM-19) : fiche à onglets
// (graphe, périmètre, opérations, résultats du simulateur, preuves), chronologie et bandeau de reprise.
// L'onglet ouvert est porté par l'adresse (?onglet=).

/** Bandeau de reprise suspendue (HOM-14) : affiché tant que le disjoncteur du simulateur n'est pas fermé. */
function BandeauReprise() {
  const { t } = useTranslation()
  const reprise = useEtatReprise()
  const r = reprise.data
  return r && r.etatDisjoncteur !== 'FERME' ? <Banner level="warn">{t('reprise.suspendue', { depuis: r.depuis ?? '' })}</Banner> : null
}

function FicheLiaison({ liaison, onglet }: { liaison: Liaison; onglet: string }) {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const institutions = useOptionsInstitutions()
  const collaborateurs = useOptionsCollaborateurs()
  const historique = useHistoriqueLiaison(liaison.id)
  const definir = useRef(setParams)
  definir.current = setParams
  const lot = useMemo(() => {
    const d = creerLiaisonDescriptor({ t, institutions: institutions.data ?? [], collaborateurs: collaborateurs.data ?? [] })
    return {
      ...d,
      tabs: [
        { key: 'graphe', label: 'liaison.onglet.graphe', render: (r: Liaison) => <OngletGraphe liaison={r} /> },
        { key: 'perimetre', label: 'liaison.onglet.perimetre', render: (r: Liaison) => <OngletPerimetre liaison={r} /> },
        { key: 'operations', label: 'liaison.onglet.operations', render: (r: Liaison) => <OngletOperations liaison={r} /> },
        { key: 'resultats', label: 'liaison.onglet.resultats', render: (r: Liaison) => <OngletResultats liaison={r} /> },
        { key: 'preuves', label: 'liaison.onglet.preuves', render: (r: Liaison) => <OngletPreuves liaison={r} /> },
      ],
      timeline: (r: Liaison) => `/bff/des/homologation/liaisons/${r.id}/historique`,
    }
  }, [t, institutions.data, collaborateurs.data])
  const pilotes = useMemo(
    () => ({
      openForm: () => definir.current({ formulaire: 'chef' }),
      openTab: (key: string) => definir.current({ onglet: key }),
    }),
    [],
  )
  const descripteur = useDescripteurDS(lot, pilotes)

  if (params.get('formulaire') === 'chef') {
    return <FormulaireChefDeProjet descripteur={descripteur} liaison={liaison} onFermer={() => setParams({})} />
  }
  return (
    <ResourceDetail
      key={onglet}
      descriptor={descripteur}
      row={liaison}
      mode="page"
      tab={onglet}
      timeline={historique.data ?? []}
      onAction={(a, row) => void a.run?.({ row, descriptor: descripteur })}
    />
  )
}

export default function LiaisonPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const [params] = useSearchParams()
  const liaison = useLiaison(id)
  return (
    <div className="gm-col">
      <BandeauReprise />
      <Donnees requete={liaison} vide={t('erreur.introuvable')}>
        {(l) => <FicheLiaison liaison={l} onglet={params.get('onglet') ?? 'detail'} />}
      </Donnees>
    </div>
  )
}
