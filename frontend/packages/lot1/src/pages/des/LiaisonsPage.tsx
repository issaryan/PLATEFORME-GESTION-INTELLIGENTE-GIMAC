import { useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, ResourceList, Tabs } from '@gimac/design-system'
import type { ListQuery } from '../../modele/commun'
import type { Liaison, LiaisonPilotage } from '../../modele/homologation'
import { useLiaisons, useOptionsCollaborateurs, useOptionsInstitutions, usePilotageLiaisons } from '../../hooks/liaisons'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerLiaisonDescriptor, creerPilotageDescriptor } from '../../ressources/liaison'
import { etatListe } from './liaisons/communs'
import { FormulaireChefDeProjet, FormulaireOuverture } from './liaisons/formulaires'

// DES-08 Liaisons et parcours (/des/liaisons, gabarit G2 ; HOM-04, HOM-20) : liste et tiroir, onglet de
// pilotage, ouverture d'une liaison et désignation du chef de projet. Composée du catalogue ; les
// données viennent des hooks, jamais d'un appel direct.
export default function LiaisonsPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const onglet = params.get('onglet') === 'pilotage' ? 'pilotage' : 'liste'
  const formulaire = params.get('formulaire')
  const [requete] = useState<ListQuery>({ page: 1, taille: 25 })

  const institutions = useOptionsInstitutions()
  const collaborateurs = useOptionsCollaborateurs()
  const options = useMemo(() => ({ t, institutions: institutions.data ?? [], collaborateurs: collaborateurs.data ?? [] }), [t, institutions.data, collaborateurs.data])
  const liaisonLot = useMemo(() => creerLiaisonDescriptor(options), [options])
  const pilotageLot = useMemo(() => creerPilotageDescriptor(options), [options])

  // Les formulaires sont portés par l'adresse (?formulaire=), comme l'onglet (?onglet=).
  const definir = useRef(setParams)
  definir.current = setParams
  const pilotes = useMemo(
    () => ({ openForm: (mode: 'create' | 'edit', row: Liaison | null) => definir.current(mode === 'create' ? { formulaire: 'ouvrir' } : { formulaire: 'chef', liaison: row?.id ?? '' }) }),
    [],
  )
  const liaison = useDescripteurDS(liaisonLot, pilotes)
  const pilotage = useDescripteurDS(pilotageLot, pilotes as unknown as { openForm: (m: 'create' | 'edit', r: LiaisonPilotage) => void })

  const liste = useLiaisons(requete)
  const tableau = usePilotageLiaisons({ ...requete, tri: 'operationsEnRetard,desc' })
  const fermer = () => setParams(onglet === 'pilotage' ? { onglet } : {})

  if (formulaire === 'ouvrir') return <FormulaireOuverture descripteur={liaison} onFermer={fermer} />
  const cible = formulaire === 'chef' ? liste.data?.contenu.find((l) => l.id === params.get('liaison')) : undefined
  if (cible) return <FormulaireChefDeProjet descripteur={liaison} liaison={cible} onFermer={fermer} />

  const ouvrir = liaison.actions.find((a) => a.key === 'ouvrir')
  const lignes = liste.data?.contenu ?? []
  const lignesPilotage = tableau.data?.contenu ?? []
  return (
    <div className="gm-col">
      <div className="gm-resource-head">
        <h1 className="gm-heading-display">{t('liaisons.titre')}</h1>
        {ouvrir ? (
          <Button variant="primary" icon="plus" onClick={() => void ouvrir.run?.({ row: null, descriptor: liaison })}>
            {ouvrir.label}
          </Button>
        ) : null}
      </div>
      <Tabs
        items={[{ key: 'liste', label: t('liaisons.onglet.liste') }, { key: 'pilotage', label: t('liaisons.onglet.pilotage') }]}
        value={onglet}
        onChange={(k: string) => setParams(k === 'liste' ? {} : { onglet: k })}
      />
      {onglet === 'liste' ? (
        <ResourceList descriptor={liaison} rows={lignes} total={liste.data?.total} embedded {...etatListe(liste, lignes)} />
      ) : (
        <ResourceList descriptor={pilotage} rows={lignesPilotage} total={tableau.data?.total} embedded {...etatListe(tableau, lignesPilotage)} />
      )}
    </div>
  )
}
