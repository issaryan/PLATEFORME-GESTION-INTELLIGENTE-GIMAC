import { useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { ResourceList, Tabs, type ListQuery } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { FiltreInstitutions, Institution, StatutInstitution } from '../../modele/participant'
import { useInstitutions } from '../../hooks/participant'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerInstitutionDescriptor } from '../../ressources/institution'
import { etatListe } from './liaisons/communs'
import { FormulaireInstitution } from './institutions/formulaires'

// DES-05 Registre des institutions (/des/institutions, gabarit G2 ; PAR-01, PAR-17) et DES-07 prise de
// contact (?formulaire=nouvelle). Liste paginée côté serveur, filtrée par statut, recherche libre ;
// l'onglet « Mes dossiers » (chargé de la DDRM) restreint aux dossiers qu'il porte.
export default function InstitutionsPage() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const onglet = params.get('onglet') === 'mesDossiers' ? 'mesDossiers' : 'tous'
  const statutInitial = params.get('statut') as StatutInstitution | null
  const [requete, setRequete] = useState<FiltreInstitutions>({ page: 1, taille: 25, statut: statutInitial ? [statutInitial] : undefined })
  const filtre = useMemo(() => ({ ...requete, mesDossiers: onglet === 'mesDossiers' }), [requete, onglet])

  const lot = useMemo(() => creerInstitutionDescriptor({ t }), [t])
  const definir = useRef(setParams)
  definir.current = setParams
  const pilotes = useMemo(() => ({ openForm: (mode: 'create' | 'edit') => mode === 'create' && definir.current({ formulaire: 'nouvelle' }) }), [])
  const descripteur = useDescripteurDS(lot, pilotes)
  const liste = useInstitutions(filtre)

  if (params.get('formulaire') === 'nouvelle') return <FormulaireInstitution descripteur={descripteur} onFermer={() => setParams({})} />

  const lignes: Institution[] = liste.data?.contenu ?? []
  const surRequete = (q: ListQuery) =>
    setRequete({ page: q.page, taille: q.pageSize, recherche: q.q, statut: q.filters?.statut ? [q.filters.statut as StatutInstitution] : undefined })
  return (
    <div className="gm-col">
      {aPermission(permissions, 'PAR_ADHESION_INSTRUCT') ? (
        <Tabs
          items={[{ key: 'tous', label: t('institutions.onglet.tous') }, { key: 'mesDossiers', label: t('institutions.onglet.mesDossiers') }]}
          value={onglet}
          onChange={(k: string) => setParams(k === 'tous' ? {} : { onglet: k })}
        />
      ) : null}
      <ResourceList
        descriptor={descripteur}
        rows={lignes}
        total={liste.data?.total}
        permissions={permissions}
        defaultQuery={statutInitial ? { filters: { statut: statutInitial } } : undefined}
        onQuery={surRequete}
        {...etatListe(liste, lignes)}
      />
    </div>
  )
}
