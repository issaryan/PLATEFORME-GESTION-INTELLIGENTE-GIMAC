import { http, P } from '@gimac/socle'
import type { ListQuery, Option, Page } from '../../modele/commun'
import type { ServiceHomologation } from '../ports'

// Implémentation HTTP de ServiceHomologation : chemins de la cartographie des contrats (PI-GIMAC-01),
// exposés par la passerelle sous /bff/des/homologation et réécrits vers /api/v1 du service.
const H = P.desHomologation
const R = `${H}/referentiels`
const L = `${H}/liaisons`
const O = `${H}/operations`

const requete = (q: ListQuery) => ({ page: q.page, taille: q.taille, tri: q.tri, recherche: q.recherche, ...(q.filtres ?? {}) })
const depot = (fichier: File, typePiece: string) => {
  const body = new FormData()
  body.set('fichier', fichier)
  body.set('typePiece', typePiece)
  return body
}

export const homologationHttp: ServiceHomologation = {
  listerReferentiels: () => http.get(R),
  lireReferentiel: (id) => http.get(`${R}/${id}`),
  operationsReferentiel: (id) => http.get(`${R}/${id}/operations`),
  // Contrôle à blanc et simulation : sans effet, lus par POST (HOM-02, HOM-03).
  controlerReferentiel: (id) => http.post(`${R}/${id}/controle`),
  simulerMigration: (id) => http.post(`${R}/${id}/migrations/simulation`),
  liaisonsImpactees: async (versionId) => {
    const p = await http.get<Page<unknown>>(L, { query: { versionId, actives: true, taille: 1 } })
    return { total: p.total }
  },
  creerBrouillon: (entree) => http.post(R, { body: entree }),
  deriverBrouillon: (versionSourceId) => http.post(R, { body: { versionSourceId } }),
  publierReferentiel: (id) => http.post(`${R}/${id}/publication`),
  migrerLiaisons: (id, liaisonsRetenues) => http.post(`${R}/${id}/migrations`, { body: { liaisonsRetenues } }),
  historiqueReferentiel: (id) => http.get(`${R}/${id}/historique`),

  listerLiaisons: (q) => http.get(L, { query: requete(q) }),
  tableauDeBordLiaisons: (q) => http.get(`${H}/tableau-de-bord/liaisons`, { query: requete(q) }),
  lireLiaison: (id) => http.get(`${L}/${id}`),
  historiqueLiaison: (id) => http.get(`${L}/${id}/historique`),
  ouvrirLiaison: (entree) => http.post(L, { body: entree }),
  designerChefDeProjet: (id, chefDeProjetId) => http.put(`${L}/${id}/chef-de-projet`, { body: { chefDeProjetId } }),
  degelerLiaison: (id, motif) => http.post(`${L}/${id}/degel`, { body: { motif } }),
  exporterDossierDePreuves: (id) => http.post(`${L}/${id}/dossier-de-preuves`),
  dernierExportPreuves: (id) => http.get(`${L}/${id}/dossier-de-preuves`),
  grapheLiaison: (id) => http.get(`${L}/${id}/graphe`),
  couverturesLiaison: (id) => http.get(`${L}/${id}/couvertures`),

  listerOperations: (liaisonId, filtres) => http.get(`${L}/${liaisonId}/operations`, { query: filtres }),
  lireOperation: (id) => http.get(`${O}/${id}`),
  preuvesOperation: (id) => http.get(`${O}/${id}/preuves`),
  attributionsOperation: (id) => http.get(`${O}/${id}/attributions`),
  grilleOperation: (id) => http.get(`${O}/${id}/grille`),
  renseignerGrille: (id, reponses) => http.put(`${O}/${id}/grille`, { body: { reponses } }),
  attribuerOperation: (id, responsableId, motif) => http.put(`${O}/${id}/responsable`, { body: { responsableId, motif } }),
  rouvrirOperation: (id, motif) => http.post(`${O}/${id}/reouverture`, { body: { motif } }),
  elementsDeCloture: (id) => http.get(`${O}/${id}/elements-de-cloture`),
  deposerPreuve: (id, fichier, typePiece) => http.post(`${O}/${id}/preuves`, { body: depot(fichier, typePiece) }),
  validerOperation: (id, entree, assertion) => http.post(`${O}/${id}/validation`, { body: entree, reauthAssertion: assertion }),

  campagnesLiaison: (liaisonId) => http.get(`${L}/${liaisonId}/campagnes`),
  scenariosInconnus: (liaisonId) => http.get(`${L}/${liaisonId}/scenarios-inconnus`),
  qualifierScenario: (id, qualification, motif) => http.post(`${H}/scenarios-inconnus/${id}/qualification`, { body: { qualification, motif } }),
  etatReprise: () => http.get(`${H}/reprise/etat`),

  rechercherInstitutions: (recherche) =>
    http
      .get<{ contenu: { id: string; code: string; denomination: string }[] }>('/bff/des/participant/institutions', { query: { recherche } })
      .then((p): Option[] => p.contenu.map((i) => ({ value: i.id, label: i.denomination, hint: i.code }))),
  rechercherCollaborateurs: (recherche) =>
    http
      .get<{ contenu: { id: string; nom: string; role: string }[] }>('/bff/des/iam/collaborateurs', { query: { recherche, roles: 'INGENIEUR_HOMOLOGATION,CHEF_DES' } })
      .then((p): Option[] => p.contenu.map((c) => ({ value: c.id, label: c.nom, hint: `role.${c.role}` }))),
}
