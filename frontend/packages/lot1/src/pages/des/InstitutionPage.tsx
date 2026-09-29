import { useMemo, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { Button, DataTable, InlineAlert, KeyValueList, ResourceDetail, ResourceList, StatusBadge, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { DossierInstitution, Institution, Prerequis } from '../../modele/participant'
import { useDossierInstitution, useInstitutions } from '../../hooks/participant'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerInstitutionDescriptor, creerPieceDescriptor, creerTransitionDescriptor } from '../../ressources/institution'
import { Donnees } from './liaisons/communs'
import { FormulaireConditions, FormulaireDepot, FormulaireInstitution } from './institutions/formulaires'

// DES-06 Dossier numérique d'une institution (/des/institutions/:id, gabarit G3 ; PAR-01 à PAR-04,
// D-57) : fiche, dossier d'adhésion (pièces), conditions d'adhésion, prérequis, propositions de
// transition. L'onglet et les formulaires sont portés par l'adresse (?onglet=, ?formulaire=, ?piece=).

const vide = (t: (k: string) => string, v: ReactNode) => (v === null || v === undefined || v === '' ? <span className="gm-field-help">{t('commun.nonRenseigne')}</span> : v)

function OngletPieces({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [, setParams] = useSearchParams()
  const definir = useRef(setParams)
  definir.current = setParams
  const lot = useMemo(() => creerPieceDescriptor({ t }), [t])
  const pilotes = useMemo(() => ({ openForm: (_m: 'create' | 'edit', row: { code: string }) => definir.current({ onglet: 'pieces', piece: row.code }) }), [])
  const descripteur = useDescripteurDS(lot, pilotes)
  return <ResourceList descriptor={descripteur} rows={dossier.pieces} total={dossier.pieces.length} permissions={permissions} embedded state={dossier.pieces.length ? 'ok' : 'empty'} />
}

function OngletConditions({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [, setParams] = useSearchParams()
  const a = dossier.adhesion
  const modifiable = dossier.institution.statut === 'CANDIDAT' && aPermission(permissions, 'PAR_INSTITUTION_EDIT')
  return (
    <div className="gm-col">
      <KeyValueList
        items={[
          { key: t('institution.codeParticipant'), value: vide(t, a.codeParticipant), mono: true },
          { key: t('adhesion.mode'), value: vide(t, a.mode ? t(`institution.mode.${a.mode}`) : null) },
          { key: t('adhesion.compteReglement'), value: vide(t, a.compteReglement), mono: true },
          { key: t('adhesion.compteCantonnement'), value: vide(t, a.compteCantonnement), mono: true },
          { key: t('adhesion.dateAdhesion'), value: vide(t, a.dateAdhesion) },
          { key: t('adhesion.motifCloture'), value: vide(t, a.motifCloture) },
        ]}
      />
      {modifiable ? (
        <div>
          <Button variant="secondary" icon="pen-line" onClick={() => setParams({ onglet: 'conditions', formulaire: 'conditions' })}>
            {t('adhesion.action.definirConditions')}
          </Button>
        </div>
      ) : null}
    </div>
  )
}

function OngletPrerequis({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const manquants = dossier.prerequisAdhesion.filter((p) => !p.satisfait).length
  const colonnes: Column<Prerequis>[] = [
    { key: 'libelle', header: t('prerequis.libelle') },
    {
      key: 'satisfait', header: t('prerequis.etat'),
      render: (p) => <StatusBadge status={p.satisfait ? 'ok' : 'warn'} label={t(p.satisfait ? 'prerequis.satisfait' : 'prerequis.manquant')} />,
    },
    { key: 'reference', header: t('prerequis.reference'), type: 'mono', render: (p) => p.reference ?? '' },
  ]
  return (
    <div className="gm-col">
      {dossier.institution.statut === 'CANDIDAT' ? (
        <InlineAlert level={manquants ? 'warn' : 'info'}>{manquants ? t('prerequis.manquants', { count: manquants }) : t('prerequis.complet')}</InlineAlert>
      ) : null}
      <DataTable columns={colonnes} rows={dossier.prerequisAdhesion} rowKey={(p) => p.code} state={dossier.prerequisAdhesion.length ? 'ok' : 'empty'} emptyText={t('prerequis.aucun')} />
    </div>
  )
}

function OngletTransitions({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const lot = useMemo(() => creerTransitionDescriptor({ t }), [t])
  const descripteur = useDescripteurDS(lot)
  return <ResourceList descriptor={descripteur} rows={dossier.transitions} total={dossier.transitions.length} permissions={permissions} embedded state={dossier.transitions.length ? 'ok' : 'empty'} />
}

function Dossier({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const onglet = params.get('onglet') ?? 'detail'
  const definir = useRef(setParams)
  definir.current = setParams
  const marraines = useInstitutions({ page: 1, taille: 100, statut: ['ACTIF'] })
  const lot = useMemo(() => {
    const d = creerInstitutionDescriptor({ t })
    return {
      ...d,
      tabs: [
        { key: 'pieces', label: 'institution.onglet.pieces', render: () => <OngletPieces dossier={dossier} /> },
        { key: 'conditions', label: 'institution.onglet.conditions', render: () => <OngletConditions dossier={dossier} /> },
        { key: 'prerequis', label: 'institution.onglet.prerequis', render: () => <OngletPrerequis dossier={dossier} /> },
        { key: 'transitions', label: 'institution.onglet.transitions', render: () => <OngletTransitions dossier={dossier} /> },
      ],
    }
  }, [t, dossier])
  const pilotes = useMemo(() => ({ openForm: () => definir.current({ formulaire: 'modifier' }), openTab: (key: string) => definir.current({ onglet: key }) }), [])
  const descripteur = useDescripteurDS(lot, pilotes)
  const fermer = () => setParams(onglet === 'detail' ? {} : { onglet })

  const formulaire = params.get('formulaire')
  if (formulaire === 'modifier') return <FormulaireInstitution descripteur={descripteur} institution={dossier.institution} onFermer={() => setParams({})} />
  if (formulaire === 'conditions') {
    const options = (marraines.data?.contenu ?? []).filter((i) => i.id !== dossier.institution.id && i.modeAdhesion === 'DIRECT').map((i) => ({ value: i.id, label: i.nom, hint: i.codeParticipant ?? '' }))
    return <FormulaireConditions institution={dossier.institution} marraines={options} onFermer={fermer} />
  }
  const piece = dossier.pieces.find((p) => p.code === params.get('piece'))
  if (piece) return <FormulaireDepot institutionId={dossier.institution.id} piece={piece} onFermer={() => setParams({ onglet: 'pieces' })} />

  const ligne = { ...dossier.institution, transitionsPermises: dossier.transitionsPermises } as Institution
  return (
    <ResourceDetail
      key={onglet}
      descriptor={descripteur}
      row={ligne}
      mode="page"
      tab={onglet}
      permissions={permissions}
      audit={false}
      onTabChange={(k) => setParams(k === 'detail' ? {} : { onglet: k })}
      onAction={(a, row) => void a.run?.({ row, descriptor: descripteur })}
    />
  )
}

export default function InstitutionPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const dossier = useDossierInstitution(id)
  return (
    <div className="gm-col">
      <Donnees requete={dossier} vide={t('erreur.introuvable')}>
        {(d) => <Dossier dossier={d} />}
      </Donnees>
    </div>
  )
}
