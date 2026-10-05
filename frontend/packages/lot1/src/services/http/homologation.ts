import { http, P } from '@gimac/socle'
import type { EvenementChronologie, ListQuery, Option, Page } from '../../modele/commun'
import type { ServiceHomologation } from '../ports'

// Implémentation HTTP de ServiceHomologation : chemins de la cartographie des contrats (PI-GIMAC-01),
// exposés par la passerelle sous /bff/des/homologation et réécrits vers /api/v1 du service.
const H = P.desHomologation
const R = `${H}/referentiels`
const L = `${H}/liaisons`
const O = `${H}/operations`

/** Instant ISO-8601 en « jj/mm/aaaa hh:mm » (Afrique centrale) ; une valeur absente ou déjà lisible est gardée. */
export const horodatage = (iso: string | null | undefined): string => {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Douala' }).format(d).replace(',', '')
}
const requete = (q: ListQuery) => ({ page: q.page, taille: q.taille, tri: q.tri, recherche: q.recherche, ...(q.filtres ?? {}) })
/** Liste des liaisons (LiaisonsCadrageResource) : pagination du service à partir de 0, filtres vides omis. */
const requeteLiaisons = (q: ListQuery) => {
  const f = Object.fromEntries(Object.entries(q.filtres ?? {}).filter(([, v]) => v !== undefined && v !== '' && v !== false))
  return { page: Math.max(q.page - 1, 0), taille: q.taille, tri: q.tri, recherche: q.recherche || undefined, ...f }
}
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
  // Pilote facultatif : omis, le créateur pilote le brouillon.
  creerBrouillon: (entree) => http.post(R, { body: entree.piloteId ? entree : { typeProjet: entree.typeProjet, libelle: entree.libelle } }),
  deriverBrouillon: (versionSourceId) => http.post(R, { body: { versionSourceId } }),
  publierReferentiel: (id) => http.post(`${R}/${id}/publication`),
  migrerLiaisons: (id, liaisonsRetenues) => http.post(`${R}/${id}/migrations`, { body: { liaisonsRetenues } }),
  // Chronologie servie avec des instants ISO-8601 : affichés à la minute, à l'heure de Douala.
  historiqueReferentiel: async (id) =>
    (await http.get<EvenementChronologie[]>(`${R}/${id}/historique`)).map((e) => ({ ...e, at: horodatage(e.at), actor: e.actor ?? undefined, correlationId: e.correlationId ?? undefined })),

  // Modélisateur (D-77, §4) : graphe étendu, catalogue, opérations, positions, arcs et pilote du brouillon.
  grapheReferentiel: (id) => http.get(`${R}/${id}/graphe`),
  catalogueReferentiels: () => http.get(`${R}/catalogue`),
  creerOperation: (id, operation) => http.post(`${R}/${id}/operations`, { body: operation }),
  enregistrerOperation: (id, code, operation) => http.put(`${R}/${id}/operations/${encodeURIComponent(code)}`, { body: operation }),
  supprimerOperation: (id, code) => http.delete(`${R}/${id}/operations/${encodeURIComponent(code)}`),
  placerOperations: (id, positions) => http.put(`${R}/${id}/positions`, { body: positions }),
  arcReferentiel: (id, arc) => http.put(`${R}/${id}/arcs`, { body: arc }),
  designerPilote: (id, compteId) => http.put(`${R}/${id}/pilote`, { body: { compteId } }),

  listerLiaisons: (q) => http.get(L, { query: requeteLiaisons(q) }),
  tableauDeBordLiaisons: (q) => http.get(`${H}/tableau-de-bord/liaisons`, { query: requete(q) }),
  lireLiaison: (id) => http.get(`${L}/${id}`),
  historiqueLiaison: (id) => http.get(`${L}/${id}/historique`),
  ouvrirLiaison: (entree) => http.post(L, { body: entree }),
  designerChefDeProjet: (id, chefDeProjetId, motif) => http.put(`${L}/${id}/chef-de-projet`, { body: motif ? { chefDeProjetId, motif } : { chefDeProjetId } }),
  conducteurs: () => http.get(`${L}/conducteurs`),
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

  // Annuaire de la plateforme (comptes internes actifs, D-64) : seuls les profils qui peuvent porter une
  // opération sont proposés ; le service refait le contrôle à l'attribution.
  rechercherCollaborateurs: (recherche) =>
    http
      .get<{ contenu: { id: string; nom: string; profils?: string[] }[] }>(`${P.desCollaboration}/messagerie/annuaire`, { query: { recherche: recherche || undefined, page: 0, taille: 100 } })
      .then((p): Option[] => p.contenu.flatMap((c) => {
        const role = (c.profils ?? []).find((r) => r === 'ROLE_INGENIEUR_HOMOLOGATION' || r === 'ROLE_CHEF_DES')
        return role ? [{ value: c.id, label: c.nom, hint: `role.${role.replace(/^ROLE_/, '')}` }] : []
      })),
}
