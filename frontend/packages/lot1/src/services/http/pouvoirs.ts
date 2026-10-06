import { http, P } from '@gimac/socle'
import type { ServicePouvoirs } from '../ports'

// Pouvoirs hiérarchiques (D-81, docs/lot1/pouvoirs-hierarchiques.md §3 et §4) : MS-IAM derrière la passerelle,
// /bff/des/iam/** réécrit vers /api/v1/**. Mes pouvoirs, demandes adressées à l'administration (vue « miennes »,
// pages à partir de 0) et rattachement d'un subordonné (RATTACHEMENT_MODIFIER).
const POUVOIRS = `${P.desIam}/pouvoirs`
const DEMANDES = `${P.desIam}/demandes`
const demande = (id: string) => `${DEMANDES}/${encodeURIComponent(id)}`

export const pouvoirsHttp: ServicePouvoirs = {
  moi: () => http.get(`${POUVOIRS}/moi`),
  demander: (d) => http.post(DEMANDES, { body: d }),
  demandes: (q) => http.get(DEMANDES, { query: { vue: 'miennes', etat: q.etat, page: q.page ?? 0, taille: q.taille ?? 20 } }),
  demande: (id) => http.get(demande(id)),
  annuler: (id) => http.post(`${demande(id)}/annulation`),
  // Route du responsable (D-81), et non celle de l'administration (/comptes/{id}/rattachement, 403 pour un chef) :
  // les deux valeurs partent toujours (une valeur nulle retire l'unité ou le n+1), le motif s'il est donné.
  rattacher: (id, r) => http.put(`${POUVOIRS}/subordonnes/${encodeURIComponent(id)}/rattachement`, {
    body: { unite: r.unite, responsableId: r.responsableId, ...(r.motif?.trim() ? { motif: r.motif.trim() } : {}) },
  }),
}
