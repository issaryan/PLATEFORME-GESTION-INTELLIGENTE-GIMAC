import { P, type ResourceDescriptorLot1 } from '@gimac/socle'
import type { ReferentielVersion } from '../modele/homologation'
import type { Services } from '../services/ports'
import { etatReferentiel } from '../dictionnaires'
import { ImpactPublication, OngletComparaison, OngletControle, OngletMigration, OngletOperations } from '../pages/des/compositions/referentiel'

// DES-12 : version d'un référentiel d'opérations (HOM-01 à HOM-03). Les actions ne sont proposées que si
// actionsPermises les contient : l'interface ne sait pas qu'une version publiée est figée, elle constate
// que PUBLIER ou MODIFIER est absent.
const R = `${P.desHomologation}/referentiels`
const TYPES = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE']

export const referentielOperationsDescriptor: ResourceDescriptorLot1<ReferentielVersion, Services> = {
  key: 'referentielOperations',
  label: { singular: 'referentiel.singulier', plural: 'referentiel.pluriel', gender: 'm' },
  idField: 'id',
  titleField: 'libelle',
  statusField: 'etat',
  endpoints: { list: R, get: `${R}/{id}`, create: R },
  fields: [
    { name: 'typeProjet', label: 'referentiel.typeProjet', kind: 'select', required: true, inList: true, inDetail: true, inForm: 'create', filterable: true, options: TYPES.map((v) => ({ value: v, label: `typeProjet.${v}` })) },
    { name: 'numero', label: 'referentiel.numero', kind: 'number', inList: true, inDetail: true, readOnly: true },
    { name: 'libelle', label: 'referentiel.libelle', kind: 'text', required: true, inList: true, inDetail: true, inForm: true },
    { name: 'etat', label: 'referentiel.etat', kind: 'status', inList: true, inDetail: true, filterable: true },
    { name: 'dateCreation', label: 'commun.dateCreation', kind: 'datetime', inDetail: true, readOnly: true },
    { name: 'datePublication', label: 'referentiel.datePublication', kind: 'datetime', inList: true, inDetail: true, readOnly: true },
    { name: 'versionSourceId', label: 'referentiel.versionSource', kind: 'mono', inDetail: true, readOnly: true },
  ],
  actions: [
    { key: 'creer', label: 'referentiel.action.creer', icon: 'plus', variant: 'primary', scope: 'page', permission: 'HOM_REFERENTIEL_EDIT', confirm: 'none', run: ({ openForm }) => openForm('create') },
    {
      key: 'deriver', label: 'referentiel.action.deriver', icon: 'copy', scope: 'row', permission: 'HOM_REFERENTIEL_EDIT', confirm: 'confirm',
      isAllowed: (r) => r.actionsPermises.includes('DERIVER'),
      run: async ({ row, services, navigate }) => {
        const v = await services.homologation.deriverBrouillon(row.id)
        navigate(`?version=${v.id}`)
      },
    },
    { key: 'controler', label: 'referentiel.action.controler', icon: 'shield-check', scope: 'row', permission: 'HOM_REFERENTIEL_EDIT', confirm: 'none', isAllowed: (r) => r.actionsPermises.includes('CONTROLER'), run: ({ openTab }) => openTab('controle') },
    {
      key: 'publier', label: 'referentiel.action.publier', icon: 'check', variant: 'primary', scope: 'row', permission: 'HOM_REFERENTIEL_PUBLISH', confirm: 'confirm',
      isAllowed: (r) => r.actionsPermises.includes('PUBLIER'),
      consequences: (r) => <ImpactPublication versionId={r.versionActiveId ?? r.id} />,
      run: async ({ row, services, invalidate }) => {
        await services.homologation.publierReferentiel(row.id)
        invalidate()
      },
    },
    { key: 'simuler', label: 'referentiel.action.simulerMigration', icon: 'route', scope: 'row', permission: 'HOM_REFERENTIEL_MIGRATE', confirm: 'none', isAllowed: (r) => r.actionsPermises.includes('MIGRER'), run: ({ openTab }) => openTab('migration') },
  ],
  tabs: [
    { key: 'operations', label: 'referentiel.onglet.operations', render: (r) => <OngletOperations versionId={r.id} /> },
    { key: 'comparaison', label: 'referentiel.onglet.comparaison', render: (r) => <OngletComparaison version={r} /> },
    { key: 'controle', label: 'referentiel.onglet.controle', permission: 'HOM_REFERENTIEL_EDIT', render: (r) => <OngletControle versionId={r.id} /> },
    { key: 'migration', label: 'referentiel.onglet.migration', render: (r) => <OngletMigration version={r} /> },
  ],
  timeline: (r) => `${R}/${r.id}/historique`,
  statusDictionary: etatReferentiel,
  permissions: { list: 'HOM_REFERENTIEL_READ', read: 'HOM_REFERENTIEL_READ', create: 'HOM_REFERENTIEL_EDIT', update: 'HOM_REFERENTIEL_EDIT' },
  liveTopic: 'homologation',
}
