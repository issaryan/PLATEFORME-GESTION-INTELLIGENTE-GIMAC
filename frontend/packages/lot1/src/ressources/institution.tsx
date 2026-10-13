import { Hash, InlineAlert, Link, StatusBadge, Tag, type Option } from '@gimac/design-system'
import { sha256Js, type ResourceDescriptorLot1 } from '@gimac/socle'
import type { Institution, PieceAdhesion, StatutInstitution, TransitionStatut } from '../modele/participant'
import type { Services } from '../services/ports'
import { conformite, etatInstitution, etatPiece, etatTransition } from '../dictionnaires'

// Descripteurs du registre des institutions (DES-05 à DES-07 ; PAR-01 à PAR-04, D-57). Libellés : clés
// i18n. Les actions n'agissent que par ctx.services ; les règles (prérequis, maker-checker, séparation
// des pouvoirs) sont décidées par MS-PARTICIPANT, dont l'interface affiche les refus.

type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string
export type OptionsInstitution = {
  t: Traduire
  /** HOM_LIAISON_OPEN et HOM_PROJECT_DESIGNATE réunies (chef du DES, chef de la DDRM) : ouverture au cadrage. */
  ouvertureLiaison?: boolean
  /**
   * Sujet de la session (DDR-01) : maker-checker, l'auteur d'une proposition (ou son premier approbateur) ne se voit
   * offrir ni l'approbation ni le refus ; MS-PARTICIPANT les refuse de toute façon (403 PAR_SEPARATION_OF_DUTIES).
   */
  moi?: string
  /** Groupes déclarés au registre (D-104) : valeurs du filtre de la liste. */
  groupes?: string[]
}

/**
 * DDR-26 : empreinte SHA-256 de ce qui est signé (la proposition tranchée : institution, transition, statuts, motif, auteur
 * et instant de la proposition), jamais l'identifiant de la proposition.
 */
export function empreinteProposition(r: TransitionStatut): string {
  const contenu = JSON.stringify([r.institutionId ?? null, r.id, r.de, r.vers, r.motif, r.demandeurId, r.demandeeLe])
  return Array.from(sha256Js(new TextEncoder().encode(contenu)), (o) => o.toString(16).padStart(2, '0')).join('')
}
/** Statuts depuis lesquels une liaison s'ouvre (étape 3, D-57) ; MS-HOMOLOGATION revérifie. */
const OUVRABLES: StatutInstitution[] = ['ADHERENT', 'EN_HOMOLOGATION']

const I = '/bff/des/participant/institutions'
/**
 * DDR-17 : sur la fiche, la ligne porte l'état du dossier (proposition en attente, prérequis de l'adhésion) ; une proposition
 * vouée au refus du service (PAR_TRANSITION_PENDING, PAR_PREREQUISITES_MISSING) n'est pas offerte. Dans la liste, sans
 * dossier, le service tranche.
 */
type LigneInstitution = Institution & { propositionEnAttente?: boolean; prerequisAdhesionReunis?: boolean }
const proposable = (r: LigneInstitution) => !r.propositionEnAttente
const TERMINAUX: StatutInstitution[] = ['RETIRE', 'CLOS_SANS_SUITE']

export const paysOptions: Option[] = ['CM', 'CF', 'CG', 'GA', 'GQ', 'TD'].map((v) => ({ value: v, label: `pays.${v}` }))
export const categorieOptions: Option[] = [
  'BANQUE', 'MICROFINANCE', 'ETABLISSEMENT_PAIEMENT', 'EMETTEUR_MONNAIE_ELECTRONIQUE', 'TRESOR_PUBLIC', 'AGREGATEUR', 'FINTECH', 'EDITEUR_LOGICIEL',
].map((v) => ({ value: v, label: `institution.categorie.${v}` }))
export const modeOptions: Option[] = ['DIRECT', 'INDIRECT'].map((v) => ({ value: v, label: `institution.mode.${v}` }))

export function creerInstitutionDescriptor({ t, ouvertureLiaison, groupes }: OptionsInstitution): ResourceDescriptorLot1<Institution, Services> {
  return {
    key: 'institution',
    label: { singular: 'institution.singulier', plural: 'institution.pluriel', gender: 'f' },
    idField: 'id',
    titleField: 'nom',
    statusField: 'statut',
    endpoints: { list: I, get: `${I}/{id}`, create: I, update: `${I}/{id}` },
    fields: [
      {
        name: 'nom', label: 'institution.nom', kind: 'text', required: true, inList: true, inDetail: true, inForm: true, sortable: true,
        render: (_v, r) => <Link href={`/des/institutions/${r.id}`}>{r.nom}</Link>,
      },
      { name: 'sigle', label: 'institution.sigle', kind: 'text', inDetail: true, inForm: true },
      // D-104 : filiales. Chaque filiale est une institution à part entière ; le groupe (ou la maison mère) les rassemble.
      {
        name: 'groupe', label: 'institution.groupe', kind: 'text', inList: true, inDetail: true, inForm: true, help: 'institution.groupe.aide',
        filterable: !!groupes?.length, options: (groupes ?? []).map((g) => ({ value: g, label: g })),
        render: (_v, r) => r.groupe ?? <span className="gm-field-help">{t('institution.groupe.aucun')}</span>,
      },
      { name: 'categorie', label: 'institution.categorie', kind: 'select', required: true, options: categorieOptions, inList: true, inDetail: true, inForm: true,
        render: (_v, r) => t(`institution.categorie.${r.categorie}`) },
      { name: 'pays', label: 'institution.pays', kind: 'select', required: true, options: paysOptions, inList: true, inDetail: true, inForm: true,
        render: (_v, r) => t(`pays.${r.pays}`) },
      { name: 'statut', label: 'institution.statut', kind: 'status', inList: true, inDetail: true, filterable: true },
      { name: 'codeParticipant', label: 'institution.codeParticipant', kind: 'mono', inList: true, inDetail: true },
      { name: 'codeIdentification', label: 'institution.codeIdentification', kind: 'mono', inDetail: true, inForm: true, help: 'institution.codeIdentification.aide' },
      { name: 'codeBanqueBeac', label: 'institution.codeBanqueBeac', kind: 'mono', inDetail: true, inForm: true, help: 'institution.codeBanqueBeac.aide' },
      // DDR-04 : modifiable aussi sur la fiche (le service l'accepte à la modification).
      // DDR-12 : hors de la liste (le tableau tient dans la largeur à 1440 px), sur la fiche et dans le formulaire.
      { name: 'datePriseDeContact', label: 'institution.datePriseDeContact', kind: 'date', inDetail: true, inForm: true, sortable: true },
      { name: 'originePriseDeContact', label: 'institution.origine', kind: 'text', inDetail: true, inForm: true, help: 'institution.origine.aide' },
      { name: 'projetEnvisage', label: 'institution.projetEnvisage', kind: 'textarea', inDetail: true, inForm: true, width: 2 },
      {
        name: 'chargeDdrm', label: 'institution.chargeDdrm', kind: 'custom', inList: true, inDetail: true,
        render: (_v, r) => r.chargeDdrm?.nom ?? <span className="gm-field-help">{t('institution.chargeDdrm.aucun')}</span>,
      },
      {
        name: 'conformite', label: 'institution.conformite', kind: 'custom', inDetail: true,
        render: (_v, r) => <StatusBadge status={conformite[r.conformite]?.status ?? 'neutral'} label={t(conformite[r.conformite]?.label ?? r.conformite)} />,
      },
      { name: 'creeLe', label: 'institution.creeLe', kind: 'datetime', inDetail: true },
    ],
    actions: [
      { key: 'enregistrer', label: 'institution.action.enregistrer', icon: 'plus', variant: 'primary', scope: 'page', permission: 'PAR_INSTITUTION_CREATE', confirm: 'none', run: (ctx) => ctx.openForm('create') },
      {
        key: 'proposerAdhesion', label: 'institution.action.proposerAdhesion', icon: 'check', variant: 'primary', scope: 'row', permission: 'PAR_TRANSITION_REQUEST', confirm: 'confirm', reason: true,
        isAllowed: (r) => r.statut === 'CANDIDAT' && proposable(r) && (r as LigneInstitution).prerequisAdhesionReunis !== false,
        consequences: () => <InlineAlert level="info">{t('institution.adhesion.consequence')}</InlineAlert>,
        run: (ctx) => ctx.services.participant.proposerTransition(ctx.row.id, 'ADHERENT', ctx.motif ?? ''),
      },
      ...(ouvertureLiaison
        ? [{
            key: 'ouvrirLiaison', label: 'cadrage.action.ouvrir', icon: 'route', variant: 'primary' as const, scope: 'row' as const, permission: 'HOM_LIAISON_OPEN', confirm: 'none' as const,
            isAllowed: (r: Institution) => OUVRABLES.includes(r.statut),
            run: (ctx: { row: Institution; navigate: (to: string) => void }) => ctx.navigate(`/des/institutions/${ctx.row.id}?formulaire=liaison`),
          }]
        : []),
      {
        key: 'modifier', label: 'institution.action.modifier', icon: 'pen-line', scope: 'row', permission: 'PAR_INSTITUTION_EDIT', confirm: 'none',
        isAllowed: (r) => !TERMINAUX.includes(r.statut), run: (ctx) => ctx.openForm('edit'),
      },
      {
        key: 'proposerCloture', label: 'institution.action.proposerCloture', icon: 'x', variant: 'danger', scope: 'row', permission: 'PAR_TRANSITION_REQUEST', confirm: 'confirm', reason: true,
        isAllowed: (r) => (r.statut === 'CANDIDAT' || r.statut === 'ADHERENT') && proposable(r),
        consequences: () => <InlineAlert level="warn">{t('institution.cloture.consequence')}</InlineAlert>,
        run: (ctx) => ctx.services.participant.proposerTransition(ctx.row.id, 'CLOS_SANS_SUITE', ctx.motif ?? ''),
      },
    ],
    statusDictionary: etatInstitution,
    permissions: { list: 'PAR_INSTITUTION_READ', read: 'PAR_INSTITUTION_READ', create: 'PAR_INSTITUTION_CREATE', update: 'PAR_INSTITUTION_EDIT' },
    liveTopic: 'participant',
  }
}

/** Pièces du dossier d'adhésion (CR-CERT rang 2) : dépôt, validation, rejet motivé. */
export function creerPieceDescriptor({ t }: OptionsInstitution): ResourceDescriptorLot1<PieceAdhesion, Services> {
  return {
    key: 'pieceAdhesion',
    label: { singular: 'piece.singulier', plural: 'piece.pluriel', gender: 'f' },
    idField: 'id',
    titleField: 'libelle',
    statusField: 'etat',
    endpoints: { list: `${I}/{id}`, get: `${I}/{id}` },
    fields: [
      { name: 'libelle', label: 'piece.libelle', kind: 'text', inList: true, inDetail: true },
      {
        name: 'obligatoire', label: 'piece.obligatoire', kind: 'custom', inList: true, inDetail: true,
        render: (_v, r) => (r.obligatoire ? <StatusBadge status="info" label={t('piece.obligatoire.oui')} icon={null} /> : <span className="gm-field-help">{t('piece.obligatoire.non')}</span>),
      },
      { name: 'etat', label: 'piece.etat', kind: 'status', inList: true, inDetail: true },
      { name: 'nomFichier', label: 'piece.fichier', kind: 'text', inList: true, inDetail: true },
      { name: 'documentSha256', label: 'piece.empreinte', kind: 'custom', inDetail: true, render: (_v, r) => (r.documentSha256 ? <Hash value={r.documentSha256} /> : null) },
      { name: 'deposeeLe', label: 'piece.deposeeLe', kind: 'datetime', inList: true, inDetail: true },
      { name: 'examineeLe', label: 'piece.examineeLe', kind: 'datetime', inDetail: true },
      { name: 'motifRejet', label: 'piece.motifRejet', kind: 'text', inDetail: true },
    ],
    actions: [
      {
        key: 'valider', label: 'piece.action.valider', icon: 'check', variant: 'primary', scope: 'row', permission: 'PAR_ADHESION_INSTRUCT', confirm: 'confirm',
        isAllowed: (r) => r.etat === 'RECUE',
        run: (ctx) => ctx.services.participant.validerPiece(ctx.row.institutionId ?? '', ctx.row.code),
      },
      {
        key: 'deposer', label: 'piece.action.deposer', icon: 'upload', scope: 'row', permission: 'PAR_ADHESION_INSTRUCT', confirm: 'none',
        isAllowed: (r) => r.etat !== 'VALIDEE', run: (ctx) => ctx.openForm('edit'),
      },
      {
        key: 'rejeter', label: 'piece.action.rejeter', icon: 'x', variant: 'danger', scope: 'row', permission: 'PAR_ADHESION_INSTRUCT', confirm: 'confirm', reason: true,
        isAllowed: (r) => r.etat === 'RECUE',
        run: (ctx) => ctx.services.participant.rejeterPiece(ctx.row.institutionId ?? '', ctx.row.code, ctx.motif ?? ''),
      },
    ],
    statusDictionary: etatPiece,
    permissions: { list: 'PAR_INSTITUTION_READ', read: 'PAR_INSTITUTION_READ', update: 'PAR_ADHESION_INSTRUCT' },
    liveTopic: 'participant',
  }
}

/** Propositions de transition de statut (algorithme 6.25) : approbation signée, rejet motivé. */
export function creerTransitionDescriptor({ t, moi }: OptionsInstitution): ResourceDescriptorLot1<TransitionStatut, Services> {
  // DDR-01 : ni le proposant ni le premier approbateur ne tranchent la proposition.
  const tranchable = (r: TransitionStatut) => r.etat === 'PROPOSEE' && (!moi || (r.demandeurId !== moi && r.approbateurId !== moi))
  const statut = (s: StatutInstitution) => <StatusBadge status={etatInstitution[s]?.status ?? 'neutral'} label={t(`institution.statut.${s}`)} />
  return {
    key: 'transition',
    label: { singular: 'transition.singulier', plural: 'transition.pluriel', gender: 'f' },
    idField: 'id',
    titleField: 'motif',
    statusField: 'etat',
    endpoints: { list: `${I}/{id}`, get: `${I}/{id}` },
    fields: [
      { name: 'de', label: 'transition.de', kind: 'custom', inList: true, inDetail: true, render: (_v, r) => statut(r.de) },
      { name: 'vers', label: 'transition.vers', kind: 'custom', inList: true, inDetail: true, render: (_v, r) => statut(r.vers) },
      { name: 'motif', label: 'transition.motif', kind: 'text', inList: true, inDetail: true },
      { name: 'etat', label: 'transition.etat', kind: 'status', inList: true, inDetail: true },
      // Bascule appliquée par MS-PARTICIPANT seul (ouverture d'une liaison, échéance) : ni proposition ni approbation.
      { name: 'automatique', label: 'transition.mode', kind: 'custom', inList: true, inDetail: true,
        render: (_v, r) => <Tag>{t(r.automatique ? 'transition.mode.automatique' : 'transition.mode.proposee')}</Tag> },
      { name: 'demandeeLe', label: 'transition.demandeeLe', kind: 'datetime', inList: true, inDetail: true },
      // DDR-17 : proposant (« vous » pour l'utilisateur), dans le tableau et le tiroir.
      { name: 'demandeurId', label: 'transition.demandeur', kind: 'custom', inList: true, inDetail: true,
        render: (_v, r) => (r.automatique ? t('transition.demandeur.systeme') : moi && r.demandeurId === moi ? t('transition.demandeur.vous') : t('transition.demandeur.autre')) },
      { name: 'decideeLe', label: 'transition.decideeLe', kind: 'datetime', inDetail: true },
      { name: 'motifRejet', label: 'transition.motifRejet', kind: 'text', inDetail: true },
    ],
    actions: [
      {
        key: 'approuver', label: 'transition.action.approuver', icon: 'check', variant: 'primary', scope: 'row', permission: 'PAR_TRANSITION_APPROVE', confirm: 'sign',
        isAllowed: tranchable,
        sign: { purpose: 'STATUS_APPROVE', empreinte: empreinteProposition },
        consequences: (r) => <InlineAlert level="info">{t('transition.approbation.consequence', { vers: t(`institution.statut.${r.vers}`) })}</InlineAlert>,
        run: (ctx) => ctx.services.participant.approuverTransition(ctx.row.institutionId ?? '', ctx.row.id, ctx.assertion ?? ''),
      },
      {
        key: 'rejeter', label: 'transition.action.rejeter', icon: 'x', variant: 'danger', scope: 'row', permission: 'PAR_TRANSITION_APPROVE', confirm: 'confirm', reason: true,
        isAllowed: tranchable,
        run: (ctx) => ctx.services.participant.rejeterTransition(ctx.row.institutionId ?? '', ctx.row.id, ctx.motif ?? ''),
      },
    ],
    statusDictionary: etatTransition,
    permissions: { list: 'PAR_INSTITUTION_READ', read: 'PAR_INSTITUTION_READ', update: 'PAR_TRANSITION_APPROVE' },
    liveTopic: 'participant',
  }
}
