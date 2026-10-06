import { depotsHttp, http, televerser } from '@gimac/socle'
import type { ServiceProcessus } from '../ports'
import { PILOTAGE } from './pilotage'

// Processus d'unité (D-87, docs/lot1/processus-unite.md §3 et §5.1) : MS-PILOTAGE derrière la passerelle,
// /bff/des/pilotage/processus/** réécrit vers /api/v1/processus/**. Livrables : dépôts fragmentés de MS-DOCUMENT
// (D-74), finalité PROCESSUS_UNITE, référence = identifiant de l'instance (comme les rapports D-76). Signature : assertion
// FIDO2 de finalité SIGNATURE_PROCESSUS dans l'en-tête X-Reauth-Assertion.

/** Préfixe des processus d'unité. */
export const PROCESSUS = `${PILOTAGE}/processus`
const definition = (id: string) => `${PROCESSUS}/${encodeURIComponent(id)}`
const instance = (id: string) => `${PROCESSUS}/instances/${encodeURIComponent(id)}`
const etape = (id: string, code: string) => `${instance(id)}/etapes/${encodeURIComponent(code)}`

export const processusHttp: ServiceProcessus = {
  lister: (q) => http.get(PROCESSUS, { query: { unite: q.unite || undefined, etat: q.etat || undefined, q: q.q?.trim() || undefined } }),
  lire: (id) => http.get(definition(id)),
  creer: (s) => http.post(PROCESSUS, { body: s }),
  modifier: (id, s) => http.put(definition(id), { body: s }),
  supprimer: (id) => http.delete(definition(id)),
  controler: (id) => http.post(`${definition(id)}/controle`),
  publier: (id, o) => http.post(`${definition(id)}/publication`, { body: { ...(o.motif?.trim() ? { motif: o.motif.trim() } : {}), ...(o.leverAvertissements ? { leverAvertissements: true } : {}) } }),
  nouvelleVersion: (id) => http.post(`${definition(id)}/nouvelle-version`),
  retirer: (id, motif) => http.post(`${definition(id)}/retrait`, { body: { motif } }),

  instances: (q) => http.get(`${PROCESSUS}/instances`, { query: { vue: q.vue, etat: q.etat || undefined, page: q.page ?? 0, taille: q.taille ?? 20 } }),
  lancer: (id, l) => http.post(`${definition(id)}/instances`, { body: l }),
  instance: (id) => http.get(instance(id)),
  televerserLivrable: (fichier, instanceId, o) => televerser(depotsHttp('des'), fichier, { finalite: 'PROCESSUS_UNITE', reference: instanceId }, o),
  deposerLivrable: (id, code, livrable, documentId) => http.post(`${etape(id, code)}/livrables`, { body: { livrable, documentId } }),
  saisirChamps: (id, code, valeurs) => http.put(`${etape(id, code)}/champs`, { body: { valeurs } }),
  signer: (id, code, signature, assertion) => http.post(`${etape(id, code)}/signature`, { body: { signature }, reauthAssertion: assertion }),
  terminer: (id, code) => http.post(`${etape(id, code)}/fin`),
  valider: (id, code, decision, motif) => http.post(`${etape(id, code)}/validation`, { body: { decision, ...(motif?.trim() ? { motif: motif.trim() } : {}) } }),
  reaffecter: (id, code, personneId, motif) => http.post(`${etape(id, code)}/reaffectation`, { body: { personneId, motif } }),
  suspendre: (id, motif) => http.post(`${instance(id)}/suspension`, { body: { motif } }),
  reprendre: (id, motif) => http.post(`${instance(id)}/reprise`, { body: motif?.trim() ? { motif: motif.trim() } : {} }),
  annuler: (id, motif) => http.post(`${instance(id)}/annulation`, { body: { motif } }),
}
