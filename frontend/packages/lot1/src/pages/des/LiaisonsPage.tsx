import { useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, PageHeader, ResourceList, Switch, Tabs, type ListQuery as RequeteListe } from '@gimac/design-system'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import type { ListQuery } from '../../modele/commun'
import type { Liaison, LiaisonPilotage } from '../../modele/homologation'
import { useConducteurs, useLiaisons, usePilotageLiaisons } from '../../hooks/liaisons'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerLiaisonDescriptor, creerPilotageDescriptor } from '../../ressources/liaison'
import { etatListe, mesProjetsParDefaut } from './liaisons/communs'
import { FormulaireChefDeProjet } from './liaisons/formulaires'

// DES-08 Liaisons et parcours (/des/liaisons, gabarit G2 ; HOM-04, HOM-20) : liste paginée et filtrée par
// le service (état, type de projet, recherche, « Mes projets »), onglet de pilotage, désignation du
// conducteur de projet. L'ouverture se fait depuis la fiche d'une institution (étape 3, D-57).

export default function LiaisonsPage() {
  const { t } = useTranslation()
  const session = useSession()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const onglet = params.get('onglet') === 'pilotage' ? 'pilotage' : 'liste'
  const formulaire = params.get('formulaire')
  const mesProjets = params.has('mesProjets') ? params.get('mesProjets') === '1' : mesProjetsParDefaut(session?.roles, permissions)
  const etatInitial = params.get('etat') ?? undefined
  const [requete, setRequete] = useState<ListQuery>({ page: 1, taille: 25, filtres: { etat: etatInitial } })
  const filtree = useMemo<ListQuery>(() => ({ ...requete, filtres: { ...requete.filtres, mesProjets: mesProjets || undefined } }), [requete, mesProjets])

  const designation = permissions === undefined || aPermission(permissions, 'HOM_PROJECT_DESIGNATE')
  const ouverture = aPermission(permissions, 'HOM_LIAISON_OPEN') && aPermission(permissions, 'HOM_PROJECT_DESIGNATE')
  const conducteurs = useConducteurs(designation)
  const options = useMemo(
    () => ({ t, ouverture, conducteurs: (conducteurs.data ?? []).map((c) => ({ value: c.id, label: c.nom })) }),
    [t, ouverture, conducteurs.data],
  )
  const liaisonLot = useMemo(() => creerLiaisonDescriptor(options), [options])
  const pilotageLot = useMemo(() => creerPilotageDescriptor(options), [options])

  // Les formulaires sont portés par l'adresse (?formulaire=), comme l'onglet (?onglet=).
  const definir = useRef(setParams)
  definir.current = setParams
  const pilotes = useMemo(() => ({ openForm: (_mode: 'create' | 'edit', row: Liaison | null) => definir.current({ formulaire: 'chef', liaison: row?.id ?? '' }) }), [])
  const liaison = useDescripteurDS(liaisonLot, pilotes)
  const pilotage = useDescripteurDS(pilotageLot, pilotes as unknown as { openForm: (m: 'create' | 'edit', r: LiaisonPilotage) => void })

  const liste = useLiaisons(filtree)
  const tableau = usePilotageLiaisons({ page: 1, taille: 25, tri: 'operationsEnRetard,desc' })
  const garder = (suite: Record<string, string>) => ({ ...(params.has('mesProjets') ? { mesProjets: params.get('mesProjets') ?? '' } : {}), ...suite })
  const fermer = () => setParams(garder(onglet === 'pilotage' ? { onglet } : {}))

  const cible = formulaire === 'chef' ? liste.data?.contenu.find((l) => l.id === params.get('liaison')) : undefined
  if (cible && designation) return <FormulaireChefDeProjet descripteur={liaison} liaison={cible} onFermer={fermer} />

  const ouvrir = liaison.actions.find((a) => a.key === 'ouvrir')
  const lignes = liste.data?.contenu ?? []
  const lignesPilotage = tableau.data?.contenu ?? []
  const surRequete = (q: RequeteListe) =>
    setRequete({
      page: q.page, taille: q.pageSize, tri: q.sort ? `${q.sort.key},${q.sort.dir}` : undefined, recherche: q.q || undefined,
      filtres: { etat: q.filters?.etat || undefined, typeProjet: q.filters?.typeProjet || undefined },
    })
  return (
    <div className="gm-col">
      <PageHeader
        title={t('liaisons.titre')}
        actions={ouvrir ? <Button variant="primary" icon="plus" onClick={() => void ouvrir.run?.({ row: null, descriptor: liaison })}>{ouvrir.label}</Button> : undefined}
      />
      <Tabs
        items={[{ key: 'liste', label: t('liaisons.onglet.liste') }, { key: 'pilotage', label: t('liaisons.onglet.pilotage') }]}
        value={onglet}
        onChange={(k: string) => setParams(garder(k === 'liste' ? {} : { onglet: k }))}
      />
      {onglet === 'liste' ? (
        <>
          <Switch label={t('liaisons.mesProjets')} checked={mesProjets} onChange={(v) => setParams({ mesProjets: v ? '1' : '0' })} />
          <ResourceList
            descriptor={liaison}
            rows={lignes}
            total={liste.data?.total}
            permissions={permissions}
            embedded
            defaultQuery={etatInitial ? { filters: { etat: etatInitial } } : undefined}
            onQuery={surRequete}
            {...etatListe(liste, lignes)}
          />
        </>
      ) : (
        <ResourceList descriptor={pilotage} rows={lignesPilotage} total={tableau.data?.total} permissions={permissions} embedded {...etatListe(tableau, lignesPilotage)} />
      )}
    </div>
  )
}
