// Descripteur d'exemple « institution membre » (DS-GIMAC-01 §8.4) et données fictives de développement.
// Aucune donnée réelle : les institutions sont nommées « Établissement de paiement A », « Banque B »…
import { StatusBadge, type Option, type ResourceDescriptor, type Status } from '@gimac/design-system'

export interface Institution {
  codeParticipant: string
  denomination: string
  sigle: string
  categorie: string
  modeAdhesion: string
  pays: string
  banqueReglement?: string
  banqueMarraine?: string
  statut: string
  etatConformite: string
  dateAdhesion: string
  transitionsPermises: string[]
  transitionEnAttente?: { demandeeParMoi: boolean } | null
}

const paysCemac: Option[] = [
  { value: 'CM', label: 'Cameroun' },
  { value: 'CF', label: 'République centrafricaine' },
  { value: 'CG', label: 'Congo' },
  { value: 'GA', label: 'Gabon' },
  { value: 'GQ', label: 'Guinée équatoriale' },
  { value: 'TD', label: 'Tchad' },
]
const categorieOptions: Option[] = [
  { value: 'BANQUE', label: 'Banque' },
  { value: 'EP', label: 'Établissement de paiement' },
  { value: 'EMF', label: 'Établissement de microfinance' },
]
const institutions: Option[] = [
  { value: 'INST-0017', label: 'Banque B' },
  { value: 'INST-0061', label: 'Banque D' },
  { value: 'INST-0093', label: 'Banque E' },
]

const CONFORMITE: Record<string, [Status, string]> = {
  CONFORME: ['ok', 'conforme'],
  A_REGULARISER: ['warn', 'à régulariser'],
  NON_CONFORME: ['crit', 'non conforme'],
  EN_COURS: ['neutral', 'en cours'],
}

export const institutionDescriptor: ResourceDescriptor<Institution> = {
  key: 'institution',
  label: { singular: 'institution membre', plural: 'institutions membres', gender: 'f' },
  idField: 'codeParticipant',
  titleField: 'denomination',
  statusField: 'statut',
  endpoints: {
    list: '/bff/des/institutions',
    get: '/bff/des/institutions/{id}',
    create: '/bff/des/institutions',
    update: '/bff/des/institutions/{id}',
    archive: '/bff/des/institutions/{id}/archive',
  },
  fields: [
    { name: 'codeParticipant', label: 'Code participant', kind: 'mono', required: true, inList: true, inDetail: true, inForm: 'create', sortable: true, width: 130, help: 'Attribué par le GIMAC, immuable' },
    { name: 'denomination', label: 'Dénomination', kind: 'text', required: true, inList: true, inDetail: true, inForm: true, sortable: true, filterable: false },
    { name: 'sigle', label: 'Sigle', kind: 'text', inList: false, inDetail: true, inForm: true },
    { name: 'categorie', label: 'Catégorie', kind: 'select', required: true, options: categorieOptions, inList: true, inDetail: true, inForm: true, filterable: true },
    { name: 'modeAdhesion', label: "Mode d'adhésion", kind: 'radio', required: true, options: [{ value: 'DIRECT', label: 'Direct' }, { value: 'INDIRECT', label: 'Indirect' }], inDetail: true, inForm: true },
    { name: 'pays', label: 'Pays', kind: 'select', required: true, options: paysCemac, inList: true, inDetail: true, inForm: true, filterable: true },
    { name: 'banqueReglement', label: 'Banque de règlement', kind: 'searchSelect', options: institutions, inDetail: true, inForm: true },
    { name: 'banqueMarraine', label: 'Banque marraine', kind: 'searchSelect', options: institutions, inDetail: true, inForm: true, help: 'Obligatoire en adhésion indirecte' },
    { name: 'statut', label: 'Statut', kind: 'status', inList: true, inDetail: true, filterable: true },
    {
      name: 'etatConformite', label: 'Conformité documentaire', kind: 'status', inList: true, inDetail: true, filterable: true,
      render: (v) => {
        const m = CONFORMITE[String(v)] || ['neutral', String(v).toLowerCase()]
        return <StatusBadge status={m[0]} label={m[1]} />
      },
    },
    { name: 'dateAdhesion', label: "Date d'adhésion", kind: 'date', inDetail: true, inForm: 'edit', readOnly: true },
  ],
  actions: [
    { key: 'create', label: 'Enregistrer une institution', icon: 'landmark', variant: 'primary', scope: 'page', permission: 'PAR_INSTITUTION_CREATE', confirm: 'none' },
    { key: 'transition', label: 'Demander un changement de statut', icon: 'route', scope: 'row', permission: 'PAR_STATUS_REQUEST', confirm: 'confirm', reason: true, isAllowed: (r) => (r.transitionsPermises || []).length > 0, consequences: ['La demande sera approuvée par un autre utilisateur : le demandeur ne peut pas approuver sa propre demande.'] },
    { key: 'approve', label: 'Approuver la transition', icon: 'pen-line', scope: 'row', permission: 'PAR_STATUS_APPROVE', confirm: 'sign', isAllowed: (r) => !!r.transitionEnAttente && !r.transitionEnAttente.demandeeParMoi },
    { key: 'signal', label: 'Signaler une non-conformité', icon: 'octagon-alert', variant: 'danger', scope: 'row', permission: 'PAR_SIGNAL', confirm: 'confirm', reason: true, consequences: ['La plateforme ne suspend rien : la production monétique est notifiée et décide.'] },
    { key: 'export', label: 'Exporter (contrôlé)', icon: 'download', scope: 'page', permission: 'PAR_EXPORT', confirm: 'confirm', consequences: ["L'export est journalisé, plafonné et filigrané."] },
    { key: 'bulkRemind', label: 'Relancer les correspondants', icon: 'send', scope: 'bulk', permission: 'PAR_REMIND', confirm: 'confirm' },
  ],
  tabs: [
    { key: 'conventions', label: 'Conventions' },
    { key: 'services', label: 'Services souscrits' },
    { key: 'correspondants', label: 'Correspondants' },
    { key: 'pieces', label: 'Pièces légales et échéances' },
    { key: 'escalade', label: "Annuaire d'escalade" },
    { key: 'interactions', label: 'Interactions' },
  ],
  timeline: (r) => '/bff/des/institutions/' + r.codeParticipant + '/chronologie',
  statusDictionary: {
    CANDIDAT: { label: 'candidat', status: 'neutral' },
    EN_HOMOLOGATION: { label: 'en homologation', status: 'info' },
    ACTIF: { label: 'actif', status: 'ok' },
    A_REGULARISER: { label: 'à régulariser', status: 'warn' },
    SUSPENDU: { label: 'suspendu', status: 'crit' },
    RETIRE: { label: 'retiré', status: 'neutral' },
  },
  permissions: { list: 'PAR_INSTITUTION_LIST', read: 'PAR_INSTITUTION_READ', create: 'PAR_INSTITUTION_CREATE', update: 'PAR_INSTITUTION_UPDATE' },
  liveTopic: 'participant',
}

export const sampleInstitutions: Institution[] = [
  { codeParticipant: 'INST-0042', denomination: 'Établissement de paiement A', sigle: 'EPA', categorie: 'EP', modeAdhesion: 'INDIRECT', pays: 'CM', banqueReglement: 'INST-0017', banqueMarraine: 'INST-0017', statut: 'ACTIF', etatConformite: 'CONFORME', dateAdhesion: '12/03/2024', transitionsPermises: ['A_REGULARISER'], transitionEnAttente: null },
  { codeParticipant: 'INST-0017', denomination: 'Banque B', sigle: 'BB', categorie: 'BANQUE', modeAdhesion: 'DIRECT', pays: 'GA', statut: 'ACTIF', etatConformite: 'A_REGULARISER', dateAdhesion: '05/06/2019', transitionsPermises: [], transitionEnAttente: { demandeeParMoi: false } },
  { codeParticipant: 'INST-0088', denomination: 'Établissement de microfinance C', sigle: 'EMC', categorie: 'EMF', modeAdhesion: 'INDIRECT', pays: 'TD', statut: 'EN_HOMOLOGATION', etatConformite: 'EN_COURS', dateAdhesion: '', transitionsPermises: ['ACTIF'], transitionEnAttente: { demandeeParMoi: true } },
  { codeParticipant: 'INST-0061', denomination: 'Banque D', sigle: 'BD', categorie: 'BANQUE', modeAdhesion: 'DIRECT', pays: 'CG', statut: 'SUSPENDU', etatConformite: 'NON_CONFORME', dateAdhesion: '20/11/2021', transitionsPermises: ['ACTIF', 'RETIRE'] },
]
