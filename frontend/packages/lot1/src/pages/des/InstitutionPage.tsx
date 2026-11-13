import { useMemo, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { ActionBar, Button, DataTable, EmptyState, Hash, InlineAlert, KeyValueList, ResourceDetail, ResourceList, StatusBadge, Timeline, type Column, type TimelineEvent } from '@gimac/design-system'
import { useFormatDate } from './parcours/communs'
import { etatPiece } from '../../dictionnaires'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import type { DossierInstitution, Institution, PieceAdhesion, Prerequis } from '../../modele/participant'
import { useDossierInstitution, useInstitutions } from '../../hooks/participant'
import { useDescripteurDS } from '../../ressources/descripteur'
import { creerInstitutionDescriptor, creerPieceDescriptor, creerTransitionDescriptor } from '../../ressources/institution'
import { Donnees } from './liaisons/communs'
import { FormulaireConditions, FormulaireDepot, FormulaireInstitution } from './institutions/formulaires'
import { useOuvertureLiaison } from './parcours/ouverture'
import { FormulaireCadrage, OngletLiaisonsInstitution, STATUTS_OUVERTURE } from './institutions/cadrage'

// DES-06 Dossier numérique d'une institution (/des/institutions/:id, gabarit G3 ; PAR-01 à PAR-04,
// D-57) : fiche, dossier d'adhésion (pièces), conditions d'adhésion, prérequis, propositions de
// transition, liaisons ; ouverture d'une liaison au cadrage (étape 3, D-57, D-58) par un chef de département.
// L'onglet et les formulaires sont portés par l'adresse (?onglet=, ?formulaire=, ?piece=) ; chaque formulaire
// s'ouvre dans une fenêtre modale sur la fiche, qui reste affichée dessous.

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
  // Les vues du service ne portent pas l'institution : l'interface la rattache pour les actions de ligne.
  const lignes = useMemo(() => dossier.pieces.map((p) => ({ ...p, institutionId: dossier.institution.id })), [dossier])
  // DDR-13 : le tiroir d'une pièce montre sa chronologie (dépôt, examen, motif), le fichier déposé et son empreinte.
  const formatDate = useFormatDate()
  const evenements = (p: PieceAdhesion): TimelineEvent[] => [
    ...(p.deposeeLe ? [{ event: t('piece.chrono.deposee', { fichier: p.nomFichier ?? '' }), at: formatDate(p.deposeeLe) }] : []),
    ...(p.examineeLe && p.etat === 'VALIDEE' ? [{ event: t('piece.chrono.validee'), at: formatDate(p.examineeLe) }] : []),
    ...(p.examineeLe && p.etat === 'REJETEE' ? [{ event: t('piece.chrono.rejetee', { motif: p.motifRejet ?? '' }), at: formatDate(p.examineeLe) }] : []),
  ].reverse()
  const tiroir = (onglet: string, p: PieceAdhesion) => {
    if (onglet === 'detail') return <ResourceDetail descriptor={descripteur} row={p} mode="drawer" permissions={permissions} />
    if (onglet === 'pieces') {
      return p.nomFichier
        ? <KeyValueList items={[{ key: t('piece.fichier'), value: p.nomFichier }, { key: t('piece.empreinte'), value: p.documentSha256 ? <Hash value={p.documentSha256} /> : t('commun.nonRenseigne') }]} />
        : <EmptyState title={t('piece.chrono.aucunFichier')} text={t('piece.chrono.aucunFichier.texte')} />
    }
    const l = evenements(p)
    return l.length ? <Timeline events={l} /> : <EmptyState title={t('piece.chrono.aucun')} text={t('piece.chrono.aucun.texte')} />
  }
  return <ResourceList descriptor={descripteur} rows={lignes} total={lignes.length} permissions={permissions} embedded state={lignes.length ? 'ok' : 'empty'} drawerRender={tiroir} />
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
        <ActionBar align="start">
          <Button variant="secondary" icon="pen-line" onClick={() => setParams({ onglet: 'conditions', formulaire: 'conditions' })}>
            {t('adhesion.action.definirConditions')}
          </Button>
        </ActionBar>
      ) : null}
    </div>
  )
}

function OngletPrerequis({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const manquants = dossier.prerequisAdhesion.filter((p) => !p.satisfait).length
  const referencePrerequis = (p: Prerequis) => {
    const r = p.reference
    if (!r) return <span className="gm-field-help">{t('commun.nonRenseigne')}</span>
    if (p.code === 'MODE_ADHESION') return t(`institution.mode.${r}`, { defaultValue: r })
    if (p.code.startsWith('PIECE_') && etatPiece[r]) return t(etatPiece[r].label)
    return <span className="gm-mono">{r}</span>
  }
  const colonnes: Column<Prerequis>[] = [
    { key: 'libelle', header: t('prerequis.libelle') },
    {
      key: 'satisfait', header: t('prerequis.etat'),
      render: (p) => <StatusBadge status={p.satisfait ? 'ok' : 'warn'} label={t(p.satisfait ? 'prerequis.satisfait' : 'prerequis.manquant')} />,
    },
    // DDR-14 : état d'une pièce attendue et mode d'adhésion nommés, jamais en code brut ; une référence documentaire reste en chasse fixe.
    { key: 'reference', header: t('prerequis.reference'), render: (p) => referencePrerequis(p) },
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
  // DDR-01 : le proposant ne se voit offrir ni l'approbation ni le refus de sa propre proposition.
  const moi = useSession()?.sujet
  const lot = useMemo(() => creerTransitionDescriptor({ t, moi }), [t, moi])
  const descripteur = useDescripteurDS(lot)
  const lignes = useMemo(() => dossier.transitions.map((x) => ({ ...x, institutionId: dossier.institution.id })), [dossier])
  return <ResourceList descriptor={descripteur} rows={lignes} total={lignes.length} permissions={permissions} embedded state={lignes.length ? 'ok' : 'empty'} />
}

function Dossier({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const onglet = params.get('onglet') ?? 'detail'
  const definir = useRef(setParams)
  definir.current = setParams
  const marraines = useInstitutions({ page: 1, taille: 100, statut: ['ACTIF'] })
  // Étape 3 : ouverture par un chef de département ou par le conducteur du projet (D-146) ; le service revérifie.
  const { peutOuvrir: ouverture } = useOuvertureLiaison()
  const lecteurLiaisons = aPermission(permissions, 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ')
  const lot = useMemo(() => {
    const d = creerInstitutionDescriptor({ t, ouvertureLiaison: ouverture })
    return {
      ...d,
      tabs: [
        { key: 'pieces', label: 'institution.onglet.pieces', render: () => <OngletPieces dossier={dossier} /> },
        { key: 'conditions', label: 'institution.onglet.conditions', render: () => <OngletConditions dossier={dossier} /> },
        { key: 'prerequis', label: 'institution.onglet.prerequis', render: () => <OngletPrerequis dossier={dossier} /> },
        { key: 'transitions', label: 'institution.onglet.transitions', render: () => <OngletTransitions dossier={dossier} /> },
        ...(lecteurLiaisons ? [{ key: 'liaisons', label: 'institution.onglet.liaisons', render: () => <OngletLiaisonsInstitution institution={dossier.institution} /> }] : []),
      ],
    }
  }, [t, dossier, ouverture, lecteurLiaisons])
  const pilotes = useMemo(() => ({ openForm: () => definir.current((p) => ({ ...Object.fromEntries(p), formulaire: 'modifier' })), openTab: (key: string) => definir.current({ onglet: key }) }), [])
  const descripteur = useDescripteurDS(lot, pilotes)
  const fermer = () => setParams(onglet === 'detail' ? {} : { onglet })

  // Formulaires et dépôt d'une pièce : fenêtres modales ouvertes sur la fiche, portées par l'adresse.
  const formulaire = params.get('formulaire')
  const piece = dossier.pieces.find((p) => p.code === params.get('piece'))
  const options = (marraines.data?.contenu ?? []).filter((i) => i.id !== dossier.institution.id && i.modeAdhesion === 'DIRECT').map((i) => ({ value: i.id, label: i.nom, hint: i.codeParticipant ?? '' }))
  const dialogue =
    formulaire === 'liaison' && ouverture && STATUTS_OUVERTURE.includes(dossier.institution.statut) ? (
      <FormulaireCadrage institution={dossier.institution} onFermer={fermer} />
    ) : formulaire === 'modifier' ? (
      <FormulaireInstitution descripteur={descripteur} institution={dossier.institution} onFermer={fermer} />
    ) : formulaire === 'conditions' ? (
      <FormulaireConditions institution={dossier.institution} adhesion={dossier.adhesion} marraines={options} onFermer={fermer} />
    ) : piece ? (
      <FormulaireDepot institutionId={dossier.institution.id} piece={piece} onFermer={() => setParams({ onglet: 'pieces' })} />
    ) : null

  // DDR-17 : état du dossier porté par la ligne, pour n'offrir que les propositions que le service peut accepter.
  const ligne = {
    ...dossier.institution, transitionsPermises: dossier.transitionsPermises,
    propositionEnAttente: dossier.transitions.some((x) => x.etat === 'PROPOSEE'),
    prerequisAdhesionReunis: dossier.prerequisAdhesion.every((p) => p.satisfait),
  } as Institution
  return (
    <>
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
      {dialogue}
    </>
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
