import { useMemo, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { Banner, InlineAlert, ResourceDetail } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { Liaison } from '../../modele/homologation'
import { useConducteurs, useEtatReprise, useHistoriqueLiaison, useLiaison } from '../../hooks/liaisons'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerLiaisonDescriptor } from '../../ressources/liaison'
import { Donnees } from './liaisons/communs'
import { FormulaireChefDeProjet } from './liaisons/formulaires'
import { ResumeCadrage } from './liaisons/cadrage'
import { OngletGraphe, OngletOperations, OngletPerimetre, OngletPreuves, OngletResultats } from './liaisons/onglets'

// DES-09 Vue d'une liaison (/des/liaisons/:id, gabarits G3 et G6 ; HOM-05 à HOM-19) : fiche à onglets
// (graphe, périmètre, opérations, résultats du simulateur, preuves), chronologie et bandeau de reprise.
// Sous l'en-tête, le cadrage (étape 3, D-57) : phase, périmètre, intervenants, conducteur de projet,
// que HOM_PROJECT_DESIGNATE permet de désigner. L'onglet ouvert est porté par l'adresse (?onglet=).

/** Bandeau de reprise suspendue (HOM-14) : affiché tant que le disjoncteur du simulateur n'est pas fermé. */
function BandeauReprise() {
  const { t } = useTranslation()
  const reprise = useEtatReprise()
  const r = reprise.data
  return r && r.etatDisjoncteur !== 'FERME' ? <Banner level="warn">{t('reprise.suspendue', { depuis: r.depuis ?? '' })}</Banner> : null
}

function FicheLiaison({ liaison, onglet }: { liaison: Liaison; onglet: string }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const designation = permissions === undefined || aPermission(permissions, 'HOM_PROJECT_DESIGNATE')
  const conducteurs = useConducteurs(designation)
  const historique = useHistoriqueLiaison(liaison.id)
  const definir = useRef(setParams)
  definir.current = setParams
  const lot = useMemo(() => {
    const d = creerLiaisonDescriptor({ t, conducteurs: (conducteurs.data ?? []).map((c) => ({ value: c.id, label: c.nom })) })
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
  }, [t, conducteurs.data])
  const pilotes = useMemo(
    () => ({
      openForm: () => definir.current({ formulaire: 'chef' }),
      openTab: (key: string) => definir.current({ onglet: key }),
    }),
    [],
  )
  const descripteur = useDescripteurDS(lot, pilotes)

  if (params.get('formulaire') === 'chef' && designation && liaison.actionsPermises.includes('DESIGNER_CHEF_DE_PROJET')) {
    return <FormulaireChefDeProjet descripteur={descripteur} liaison={liaison} onFermer={() => setParams({})} />
  }
  return (
    <div className="gm-col">
      {params.get('ouverte') === '1' ? <InlineAlert level="info">{t('cadrage.ouverte', { id: liaison.id, institution: liaison.institutionNom })}</InlineAlert> : null}
      <ResourceDetail
        key={onglet}
        descriptor={descripteur}
        row={liaison}
        mode="page"
        tab={onglet}
        permissions={permissions}
        timeline={historique.data ?? []}
        summary={<ResumeCadrage liaison={liaison} />}
        onAction={(a, row) => void a.run?.({ row, descriptor: descripteur })}
      />
    </div>
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
