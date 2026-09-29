import { t } from 'i18next'
import { StatusBadge } from '@gimac/design-system'
import type { ResourceActionLot1, ResourceDescriptorLot1 } from '@gimac/socle'
import type { PortailLiaison, PortailOperation } from '../modele/portail'
import type { Services } from '../services/ports'
import { etatLiaison, etatOperation } from '../dictionnaires'

// Descripteurs du portail (POR-03, HOM-23, HOM-24, CER-13) : la projection ne déclare aucun champ interne
// (motif d'état interne, chef de projet) ; l'absence est structurelle.

export const portailLiaisonDescriptor: ResourceDescriptorLot1<PortailLiaison, Services> = {
  key: 'portailLiaison',
  label: { singular: 'portail.liaison', plural: 'portail.liaisons', gender: 'f' },
  idField: 'id',
  titleField: 'typeProjet',
  statusField: 'etat',
  endpoints: { list: '/bff/portail/homologation/liaisons', get: '/bff/portail/homologation/liaisons/{id}' },
  fields: [
    { name: 'id', label: 'portail.reference', kind: 'mono', inList: true },
    // Libellé déjà traduit par la page (titre de la fiche) : voir avecTypeProjetLisible.
    { name: 'typeProjet', label: 'portail.typeProjet', kind: 'text', inList: true, inDetail: true },
    { name: 'etat', label: 'portail.etat', kind: 'status', inList: true, inDetail: true },
    { name: 'phaseCourante', label: 'portail.phase', kind: 'text', inList: true, inDetail: true },
    { name: 'versionNumero', label: 'portail.version', kind: 'number', inDetail: true },
  ],
  actions: [
    {
      key: 'ouvrir', label: 'portail.action.ouvrir', icon: 'arrow-right', variant: 'primary', scope: 'row', permission: 'HOM_PORTAL_READ', confirm: 'none',
      run: ({ row, navigate }) => navigate(`/portail/liaisons/${row.id}`),
    },
  ],
  tabs: [
    { key: 'a-faire', label: 'portail.onglet.aFaire' },
    { key: 'parcours', label: 'portail.onglet.parcours' },
    { key: 'resultats', label: 'portail.onglet.resultats' },
    { key: 'couvertures', label: 'portail.onglet.couvertures' },
    { key: 'certification', label: 'portail.onglet.certification' },
  ],
  statusDictionary: etatLiaison,
  permissions: { list: 'HOM_PORTAL_READ', read: 'HOM_PORTAL_READ' },
  liveTopic: 'homologation',
}

/** Présentation : le type de projet affiché (titre de la fiche, colonne de liste) dans la langue courante. */
export const avecTypeProjetLisible = (l: PortailLiaison, traduire: (cle: string) => string): PortailLiaison => ({ ...l, typeProjet: traduire(`typeProjet.${l.typeProjet}`) })

export const portailOperationDescriptor: ResourceDescriptorLot1<PortailOperation, Services> = {
  key: 'portailOperation',
  label: { singular: 'portail.operation', plural: 'portail.operations', gender: 'f' },
  idField: 'code',
  titleField: 'libelle',
  statusField: 'etat',
  endpoints: { list: '/bff/portail/homologation/liaisons/{liaisonId}/a-faire', get: '/bff/portail/homologation/liaisons/{liaisonId}/a-faire/{id}' },
  fields: [
    { name: 'code', label: 'portail.operation.code', kind: 'mono', inList: true, inDetail: true },
    { name: 'libelle', label: 'portail.operation.libelle', kind: 'text', inList: true, inDetail: true },
    { name: 'etat', label: 'portail.operation.etat', kind: 'status', inList: true, inDetail: true },
    {
      name: 'echeance', label: 'portail.operation.echeance', kind: 'custom', inList: true, inDetail: true,
      render: (v, r) => (r.joursRestants !== null && r.joursRestants < 0
        ? <StatusBadge status="warn" label={t('portail.echeanceDepassee', { n: -r.joursRestants, date: String(v ?? '') })} />
        : String(v ?? '')),
    },
    { name: 'preuveAttendue', label: 'portail.preuveAttendue', kind: 'text', inDetail: true },
  ],
  actions: [
    {
      key: 'ouvrirDepot', label: 'portail.action.deposer', icon: 'upload', variant: 'primary', scope: 'row', permission: 'HOM_PORTAL_SUBMIT', confirm: 'none',
      isAllowed: (r) => r.actionsPermises.includes('DEPOSER_PREUVE'),
      run: ({ row, navigate }) => navigate(`/portail/liaisons/${row.liaisonId}/operations/${row.id}/preuve`),
    },
  ],
  statusDictionary: etatOperation,
  permissions: { list: 'HOM_PORTAL_READ', read: 'HOM_PORTAL_READ', update: 'HOM_PORTAL_SUBMIT' },
  liveTopic: 'homologation',
}

/** Dépôt confirmé d'une preuve : la ligne porte le fichier choisi ; contrôle de contenu par MS-DOCUMENT. */
export const actionDeposerPreuvePortail: ResourceActionLot1<PortailOperation & { fichier: File | null }, Services> = {
  key: 'deposer', label: 'portail.action.confirmerDepot', icon: 'upload', variant: 'primary', scope: 'page', permission: 'HOM_PORTAL_SUBMIT', confirm: 'confirm',
  isAllowed: (r) => r.actionsPermises.includes('DEPOSER_PREUVE') && !!r.fichier,
  consequences: (r) => <p style={{ margin: 0 }}>{t('portail.depot.consequence', { fichier: r.fichier?.name ?? '' })}</p>,
  run: async ({ row, services }) => {
    if (row.fichier) await services.portail.deposerPreuve(row.id, row.fichier, row.preuveAttendue)
  },
}
