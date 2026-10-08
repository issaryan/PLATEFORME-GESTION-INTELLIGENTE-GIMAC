import { depotsHttp, http, P, televerser } from '@gimac/socle'
import type { ServicePilotage } from '../ports'

// Implémentation HTTP de « Mon activité » (D-76, docs/lot1/mon-activite.md) : MS-PILOTAGE derrière la
// passerelle, /bff/des/pilotage/** réécrit vers /api/v1/** ; organisation de MS-IAM sous
// /bff/des/iam/organisation. Documents des rapports : dépôts fragmentés de MS-DOCUMENT (D-74), finalité
// RAPPORT_ACTIVITE, référence : l'identifiant du rapport.

/** Préfixe de MS-PILOTAGE pour la couche interne. */
export const PILOTAGE = '/bff/des/pilotage'
const ORGANISATION = `${P.desIam}/organisation`
const un = (racine: string, id: string) => `${PILOTAGE}/${racine}/${encodeURIComponent(id)}`

export const pilotageHttp: ServicePilotage = {
  agenda: (du, au, collaborateur) => http.get(`${PILOTAGE}/agenda`, { query: { du, au, collaborateur } }),
  creerEvenement: (e) => http.post(`${PILOTAGE}/agenda`, { body: e }),
  modifierEvenement: (id, e) => http.put(un('agenda', id), { body: e }),
  supprimerEvenement: (id) => http.delete(un('agenda', id)),
  repondre: (id, reponse) => http.post(`${un('agenda', id)}/reponse`, { body: { reponse } }),

  activites: (du, au, collaborateur) => http.get(`${PILOTAGE}/activites`, { query: { du, au, collaborateur } }),
  creerActivite: (a) => http.post(`${PILOTAGE}/activites`, { body: a }),
  modifierActivite: (id, a) => http.put(un('activites', id), { body: a }),
  changerStatut: (id, statut, reporteeAu) => http.post(`${un('activites', id)}/statut`, { body: reporteeAu ? { statut, reporteeAu } : { statut } }),
  supprimerActivite: (id) => http.delete(un('activites', id)),

  taches: (q) => http.get(`${PILOTAGE}/taches`, { query: { vue: q.vue, etat: q.etat, page: q.page ?? 0, taille: q.taille ?? 20 } }),
  tache: (id) => http.get(un('taches', id)),
  creerTache: (t) => http.post(`${PILOTAGE}/taches`, { body: t }),
  creerGroupe: (t) => http.post(`${PILOTAGE}/taches`, { body: t }),
  modifierTache: (id, t) => http.put(un('taches', id), { body: t }),
  transition: (id, etat, motif) => http.post(`${un('taches', id)}/transition`, { body: motif ? { etat, motif } : { etat } }),
  // Tâches confiées (D-81 §6) : groupe, prise d'une tâche à prendre, attribution, relance.
  groupe: (groupeId) => http.get(un('taches/groupes', groupeId)),
  prendre: (id) => http.post(`${un('taches', id)}/prise`),
  attribuer: (id, responsableId) => http.post(`${un('taches', id)}/attribution`, { body: { responsableId } }),
  relancer: (id) => http.post(`${un('taches', id)}/relance`),

  rapports: (q) => http.get(`${PILOTAGE}/rapports`, { query: { vue: q.vue, type: q.type, etat: q.etat, page: q.page ?? 0, taille: q.taille ?? 20 } }),
  rapport: (id) => http.get(un('rapports', id)),
  preremplir: (type, periode) => http.post(`${PILOTAGE}/rapports/preremplissage`, { body: { type, periode } }),
  creerRapport: (r) => http.post(`${PILOTAGE}/rapports`, { body: r }),
  modifierRapport: (id, r) => http.put(un('rapports', id), { body: r }),
  soumettre: (id, destinataireId) => http.post(`${un('rapports', id)}/soumission`, destinataireId ? { body: { destinataireId } } : undefined),
  destinatairesRapport: (id) => http.get(`${un('rapports', id)}/destinataires`),
  viser: (id, commentaire) => http.post(`${un('rapports', id)}/visa`, { body: { commentaire } }),
  documentRapport: (id) => http.blob(`${un('rapports', id)}/document`, '*/*'),
  deposerDocument: (fichier, rapportId, o) => televerser(depotsHttp('des'), fichier, { finalite: 'RAPPORT_ACTIVITE', reference: rapportId }, o),

  equipe: (jour) => http.get(`${PILOTAGE}/equipe`, { query: { jour } }),
  synthese: (jour) => http.get(`${PILOTAGE}/moi/synthese`, { query: { jour } }),

  unites: () => http.get(`${ORGANISATION}/unites`),
  organisation: () => http.get(`${ORGANISATION}/moi`),
}
