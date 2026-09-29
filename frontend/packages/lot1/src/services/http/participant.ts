import { http, P } from '@gimac/socle'
import type { FiltreInstitutions } from '../../modele/participant'
import type { ServiceParticipant } from '../ports'

// Implémentation HTTP de ServiceParticipant : routes de MS-PARTICIPANT exposées par la passerelle sous
// /bff/des/participant et réécrites vers /api/v1 (D-57). Les règles (prérequis, maker-checker,
// séparation des pouvoirs) sont décidées par le service ; l'interface affiche ses refus.
const I = `${P.desParticipant}/institutions`

const requete = (f: FiltreInstitutions) => ({
  // Pagination de l'interface à partir de 1 ; celle du service à partir de 0.
  page: Math.max(f.page - 1, 0),
  taille: f.taille,
  q: f.recherche || undefined,
  mesDossiers: f.mesDossiers || undefined,
  statut: f.statut && f.statut.length ? f.statut.join(',') : undefined,
})

/** Empreinte SHA-256 du fichier, en hexadécimal minuscule, calculée dans le navigateur. */
export async function empreinteSha256(fichier: Blob): Promise<string> {
  const d = await crypto.subtle.digest('SHA-256', await fichier.arrayBuffer())
  return Array.from(new Uint8Array(d), (b) => b.toString(16).padStart(2, '0')).join('')
}

export const participantHttp: ServiceParticipant = {
  listerInstitutions: (f) => http.get(I, { query: requete(f) }),
  indicateurs: () => http.get(`${I}/indicateurs`),
  dossier: (id) => http.get(`${I}/${id}`),
  prerequis: (id, etat) => http.get(`${I}/${id}/prerequis`, { query: { etat } }),
  enregistrer: (c) => http.post(I, { body: c }),
  modifier: (id, c) => http.put(`${I}/${id}`, { body: c }),
  definirConditions: (id, c) => http.put(`${I}/${id}/conditions-adhesion`, { body: c }),
  deposerPiece: async (id, code, fichier) => {
    // 1. Dépôt dans MS-DOCUMENT (stockage WORM) ; 2. rattachement de la référence et de l'empreinte,
    // que MS-PARTICIPANT revérifie auprès du registre documentaire.
    const corps = new FormData()
    corps.set('fichier', fichier)
    corps.set('finalite', 'ADHESION')
    corps.set('reference', `${id}/${code}`)
    const document = await http.post<{ id: string; sha256: string }>(`${P.desDocument}/documents`, { body: corps })
    const sha256 = document.sha256 || (await empreinteSha256(fichier))
    return http.post(`${I}/${id}/pieces/${encodeURIComponent(code)}/depot`, { body: { documentId: document.id, documentSha256: sha256, nomFichier: fichier.name } })
  },
  validerPiece: (id, code) => http.post(`${I}/${id}/pieces/${encodeURIComponent(code)}/validation`),
  rejeterPiece: (id, code, motif) => http.post(`${I}/${id}/pieces/${encodeURIComponent(code)}/rejet`, { body: { motif } }),
  proposerTransition: (id, vers, motif, pieceDecisionId) => http.post(`${I}/${id}/transitions`, { body: { vers, motif, pieceDecisionId } }),
  approuverTransition: (id, t, assertion) => http.post(`${I}/${id}/transitions/${t}/approbation`, { reauthAssertion: assertion }),
  rejeterTransition: (id, t, motif) => http.post(`${I}/${id}/transitions/${t}/rejet`, { body: { motif } }),
}
