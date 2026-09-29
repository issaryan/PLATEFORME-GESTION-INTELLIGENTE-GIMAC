import { t } from 'i18next'
import { Hash, StatusBadge } from '@gimac/design-system'
import type { ResourceActionLot1, ResourceDescriptorLot1 } from '@gimac/socle'
import type { Attestation, Dossier, EntreeRegistre, NatureQuitus } from '../modele/certification'
import type { Services } from '../services/ports'
import { etatAttestation, etatDossier, etatScellement } from '../dictionnaires'
import { separerDesignations } from '../adaptateurs/certification'
import { enregistrerFichier } from './fichier'

// Descripteurs de DES-11 (dossier de mise en service, CER-01 à CER-15) : déclaratifs ; les actions
// n'agissent que par ctx.services ; transitions et actions permises viennent du service.

export const dossierDescriptor: ResourceDescriptorLot1<Dossier, Services> = {
  key: 'dossierMiseEnService',
  label: { singular: 'dossier.singulier', plural: 'dossier.pluriel', gender: 'm' },
  idField: 'id',
  titleField: 'institutionNom',
  statusField: 'etat',
  endpoints: { list: '/bff/des/certification/dossiers', get: '/bff/des/certification/dossiers/{id}' },
  fields: [
    { name: 'institutionNom', label: 'dossier.institution', kind: 'text', inDetail: true },
    { name: 'liaisonId', label: 'dossier.liaison', kind: 'mono', inDetail: true },
    { name: 'etat', label: 'dossier.etat', kind: 'status', inDetail: true },
    { name: 'cycle', label: 'dossier.cycle', kind: 'number', inDetail: true },
    { name: 'socleEmpreinte', label: 'dossier.socle', kind: 'custom', inDetail: true, render: (v) => <Hash value={String(v)} /> },
    {
      name: 'etatScellement', label: 'dossier.scellement', kind: 'custom', inDetail: true,
      render: (v) => (v ? <StatusBadge status={etatScellement[String(v)]?.status ?? 'neutral'} label={t(etatScellement[String(v)]?.label ?? String(v))} /> : t('dossier.scellement.aucun')),
    },
    { name: 'dateOuverture', label: 'dossier.dateOuverture', kind: 'date', inDetail: true },
    { name: 'dateAutorisation', label: 'dossier.dateAutorisation', kind: 'custom', inDetail: true, render: (v) => (v ? String(v) : t('commun.nonDisponible')) },
    { name: 'referenceFicheChangement', label: 'fiche.reference', kind: 'mono', inDetail: true },
    { name: 'numeroAttestation', label: 'attestation.numero', kind: 'custom', inDetail: true, render: (v) => (v ? <span className="gm-mono">{String(v)}</span> : t('attestation.aucune')) },
  ],
  actions: [
    {
      key: 'verifier', label: 'dossier.action.verifier', icon: 'refresh', scope: 'row', permission: 'CER_DOSSIER_VERIFY', confirm: 'none',
      isAllowed: (r) => r.actionsPermises.includes('VERIFIER'),
      run: ({ row, services }) => services.certification.verifierDossier(row.id),
    },
    {
      key: 'relireFiche', label: 'fiche.action.relire', icon: 'refresh', scope: 'row', permission: 'CER_CHANGE_RECORD_LINK', confirm: 'none',
      isAllowed: (r) => r.actionsPermises.includes('RELIRE_FICHE') && !!r.referenceFicheChangement,
      run: ({ row, services }) => services.certification.lierFicheDeChangement(row.id, row.referenceFicheChangement ?? ''),
    },
    {
      key: 'emettreAttestation', label: 'attestation.action.emettre', icon: 'shield-check', variant: 'primary', scope: 'row',
      permission: 'CER_ATTESTATION_ISSUE', confirm: 'confirm', isAllowed: (r) => r.actionsPermises.includes('EMETTRE_ATTESTATION'),
      consequences: () => <p style={{ margin: 0 }}>{t('attestation.emettre.consequence')}</p>,
      run: ({ row, services }) => services.certification.emettreAttestation(row.id),
    },
  ],
  tabs: [
    { key: 'circuit', label: 'dossier.onglet.circuit' },
    { key: 'completude', label: 'dossier.onglet.completude' },
    { key: 'fiche', label: 'dossier.onglet.fiche' },
    { key: 'procesVerbal', label: 'dossier.onglet.procesVerbal' },
    { key: 'horodatages', label: 'dossier.onglet.horodatages' },
    { key: 'attestation', label: 'dossier.onglet.attestation' },
    { key: 'registre', label: 'dossier.onglet.registre' },
  ],
  timeline: (r) => `/bff/des/certification/dossiers/${r.id}/historique`,
  statusDictionary: etatDossier,
  permissions: { list: 'CER_DOSSIER_READ', read: 'CER_DOSSIER_READ', update: 'CER_CHANGE_RECORD_LINK' },
  liveTopic: 'certification',
}

/** Quitus visé par une signature : ligne construite au clic, à partir du circuit renvoyé par le service. */
export type QuitusCible = { circuitId: string; nature: NatureQuitus; socleEmpreinte: string; libelle: string }

// La décision (favorable ou défavorable, désignations) se saisit dans le dialogue de signature ; elle
// est conservée ici le temps de la signature, par quitus visé, puis transmise au service.
export type DecisionSaisie = { favorable: boolean; designations: string[] }
export const decisionsEnCours = new Map<string, DecisionSaisie>()
export const cleDecision = (c: Pick<QuitusCible, 'circuitId' | 'nature'>) => `${c.circuitId}:${c.nature}`

export function actionSignerQuitus(consequences: (c: QuitusCible) => React.ReactNode): ResourceActionLot1<QuitusCible, Services> {
  return {
    key: 'signerQuitus', label: 'quitus.action.signer', icon: 'pen-line', variant: 'primary', scope: 'row', permission: 'CER_QUITUS_SIGN',
    confirm: 'sign', reason: true, sign: { purpose: 'QUITUS_SIGN', empreinte: (c) => c.socleEmpreinte },
    consequences,
    run: async ({ row, motif, assertion, services }) => {
      const d = decisionsEnCours.get(cleDecision(row)) ?? { favorable: true, designations: [] }
      await services.certification.signerQuitus(
        row.circuitId, row.nature,
        { decisionFavorable: d.favorable, motif: motif ?? '', socleEmpreinte: row.socleEmpreinte, ...separerDesignations(d.favorable ? [] : d.designations) },
        assertion ?? '',
      )
      decisionsEnCours.delete(cleDecision(row))
    },
  }
}

export const actionApprouverStatut: ResourceActionLot1<Attestation, Services> = {
  key: 'approuverStatut', label: 'statut.action.approuver', icon: 'pen-line', variant: 'primary', scope: 'row', permission: 'CER_ATTESTATION_STATUS_APPROVE',
  confirm: 'sign', reason: true, sign: { purpose: 'STATUS_APPROVE', empreinte: (a) => a.demandeEnAttente?.empreinte ?? '' },
  isAllowed: (a) => a.actionsPermises.includes('APPROUVER_STATUT'),
  run: ({ row, motif, assertion, services }) => services.certification.approuverStatut(row.demandeEnAttente?.id ?? '', motif ?? '', assertion ?? ''),
}

export const registreDescriptor: ResourceDescriptorLot1<EntreeRegistre, Services> = {
  key: 'registre',
  label: { singular: 'registre.singulier', plural: 'registre.pluriel', gender: 'f' },
  idField: 'numero',
  titleField: 'institutionNom',
  statusField: 'etat',
  endpoints: { list: '/bff/des/certification/registre', get: '/bff/des/certification/attestations/{id}' },
  fields: [
    { name: 'numero', label: 'attestation.numero', kind: 'mono', inList: true, inDetail: true, sortable: true },
    { name: 'institutionNom', label: 'dossier.institution', kind: 'text', inList: true, inDetail: true, filterable: true },
    { name: 'typeProjet', label: 'registre.typeProjet', kind: 'custom', inList: true, inDetail: true, render: (v) => t(`typeProjet.${String(v)}`) },
    { name: 'services', label: 'registre.services', kind: 'custom', inList: true, inDetail: true, render: (v) => (v as string[]).join(', ') },
    { name: 'dateEmission', label: 'registre.dateEmission', kind: 'date', inList: true, inDetail: true, sortable: true },
    { name: 'echeanceRenouvellement', label: 'registre.echeance', kind: 'date', inList: true, inDetail: true, sortable: true },
    { name: 'etat', label: 'registre.etat', kind: 'status', inList: true, inDetail: true, filterable: true },
  ],
  actions: [
    {
      key: 'exporter', label: 'registre.action.exporter', icon: 'download', scope: 'page', permission: 'CER_REGISTER_EXPORT', confirm: 'confirm',
      consequences: () => <p style={{ margin: 0 }}>{t('registre.exporter.consequence')}</p>,
      run: async ({ services }) => {
        const f = await services.certification.exporterRegistre({ page: 1, taille: 1000 })
        enregistrerFichier(f.blob, 'registre-attestations.csv')
      },
    },
  ],
  statusDictionary: etatAttestation,
  permissions: { list: 'CER_REGISTER_READ', read: 'CER_REGISTER_READ' },
  liveTopic: 'certification',
}
