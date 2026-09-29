import { Hash } from '@gimac/design-system'
import type { ResourceDescriptorLot1 } from '@gimac/socle'
import type { CampaignResult, ScenarioInconnu } from '../modele/homologation'
import type { Services } from '../services/ports'
import { verdictCampagne } from '../dictionnaires'

// Résultats du simulateur (HOM-13) : lecture seule structurelle, aucune action ni champ de formulaire.
export const campagneDescriptor: ResourceDescriptorLot1<CampaignResult, Services> = {
  key: 'campagne',
  label: { singular: 'campagne.singulier', plural: 'campagne.pluriel', gender: 'm' },
  idField: 'executionId',
  titleField: 'idScenario',
  statusField: 'verdict',
  endpoints: { list: '/bff/des/homologation/liaisons/{liaisonId}/campagnes', get: '/bff/des/homologation/liaisons/{liaisonId}/campagnes/{id}' },
  fields: [
    { name: 'idScenario', label: 'campagne.scenario', kind: 'mono', inList: true, inDetail: true, filterable: true },
    { name: 'operationCode', label: 'campagne.operation', kind: 'mono', inList: true, inDetail: true },
    { name: 'verdict', label: 'campagne.verdict', kind: 'status', inList: true, inDetail: true, filterable: true },
    { name: 'dateExecution', label: 'campagne.dateExecution', kind: 'datetime', inList: true, inDetail: true, sortable: true },
    { name: 'versionScenario', label: 'campagne.versionScenario', kind: 'mono', inList: true, inDetail: true },
    { name: 'referenceTrace', label: 'campagne.referenceTrace', kind: 'mono', inList: true, inDetail: true },
    { name: 'empreinteReponse', label: 'campagne.empreinte', kind: 'custom', inDetail: true, render: (v) => <Hash value={String(v)} /> },
    { name: 'dateReprise', label: 'campagne.dateReprise', kind: 'datetime', inDetail: true },
  ],
  actions: [],
  statusDictionary: verdictCampagne,
  permissions: { list: 'HOM_LIAISON_READ', read: 'HOM_LIAISON_READ' },
  liveTopic: 'homologation',
}

// Scénarios inconnus (HOM-15) : seule action de l'onglet, la qualification motivée.
export const scenarioInconnuDescriptor: ResourceDescriptorLot1<ScenarioInconnu, Services> = {
  key: 'scenarioInconnu',
  label: { singular: 'scenario.singulier', plural: 'scenario.pluriel', gender: 'm' },
  idField: 'id',
  titleField: 'idScenario',
  endpoints: { list: '/bff/des/homologation/liaisons/{liaisonId}/scenarios-inconnus', get: '/bff/des/homologation/scenarios-inconnus/{id}' },
  fields: [
    { name: 'idScenario', label: 'campagne.scenario', kind: 'mono', inList: true, inDetail: true },
    { name: 'dateReception', label: 'scenario.dateReception', kind: 'datetime', inList: true, inDetail: true },
    { name: 'qualification', label: 'scenario.qualification', kind: 'select', inList: true, inDetail: true, options: [{ value: 'A_DECLARER', label: 'scenario.qualification.A_DECLARER' }, { value: 'HORS_PARCOURS', label: 'scenario.qualification.HORS_PARCOURS' }] },
  ],
  actions: [
    {
      key: 'declarer', label: 'scenario.action.declarer', icon: 'plus', scope: 'row', permission: 'HOM_SCENARIO_QUALIFY', confirm: 'confirm', reason: true,
      isAllowed: (r) => r.actionsPermises.includes('QUALIFIER'), run: (ctx) => ctx.services.homologation.qualifierScenario(ctx.row.id, 'A_DECLARER', ctx.motif ?? ''),
    },
    {
      key: 'horsParcours', label: 'scenario.action.horsParcours', icon: 'x', scope: 'row', permission: 'HOM_SCENARIO_QUALIFY', confirm: 'confirm', reason: true,
      isAllowed: (r) => r.actionsPermises.includes('QUALIFIER'), run: (ctx) => ctx.services.homologation.qualifierScenario(ctx.row.id, 'HORS_PARCOURS', ctx.motif ?? ''),
    },
  ],
  permissions: { list: 'HOM_LIAISON_READ', read: 'HOM_LIAISON_READ' },
  liveTopic: 'homologation',
}
