import { http, P } from '@gimac/socle'
import type { ListQuery } from '../../modele/commun'
import type { NatureQuitus } from '../../modele/certification'
import type { ServiceCertification } from '../ports'

// Implémentation HTTP de ServiceCertification (CER-01 à CER-15), sous /bff/des/certification.
const C = P.desCertification
const NATURE: Record<NatureQuitus, string> = { TECHNIQUE: 'technique', SECURITE: 'securite', CONFORMITE: 'conformite', DIRECTION_GENERALE: 'direction-generale' }
const requete = (q: ListQuery) => ({ page: q.page, taille: q.taille, tri: q.tri, recherche: q.recherche, ...(q.filtres ?? {}) })

export const certificationHttp: ServiceCertification = {
  dossierParLiaison: async (liaisonId) => {
    const l = await http.get<{ contenu: { id: string }[] }>(`${C}/dossiers`, { query: { liaisonId, taille: 1 } })
    return l.contenu[0] ? http.get(`${C}/dossiers/${l.contenu[0].id}`) : null
  },
  lireDossier: (id) => http.get(`${C}/dossiers/${id}`),
  historiqueDossier: (id) => http.get(`${C}/dossiers/${id}/historique`),
  circuit: (id) => http.get(`${C}/circuits/${id}`),
  signerQuitus: (circuitId, nature, decision, assertion) =>
    http.post(`${C}/circuits/${circuitId}/quitus/${NATURE[nature]}`, { body: decision, reauthAssertion: assertion }),
  conditions: (id) => http.get(`${C}/dossiers/${id}/conditions`),
  verifierDossier: (id, condition) => http.post(`${C}/dossiers/${id}/verification`, { body: condition ? { condition } : {} }),
  ficheDeChangement: (id) => http.get(`${C}/dossiers/${id}/fiche-de-changement`),
  lierFicheDeChangement: (id, reference) => http.put(`${C}/dossiers/${id}/fiche-de-changement`, { body: { reference } }),
  procesVerbal: (id) => http.get(`${C}/dossiers/${id}/proces-verbal`),
  deposerProcesVerbal: (id, fichier, heureBascule) => {
    const body = new FormData()
    body.set('fichier', fichier)
    body.set('heureBascule', heureBascule)
    return http.post(`${C}/dossiers/${id}/proces-verbal`, { body })
  },
  horodatages: (id) => http.get(`${C}/dossiers/${id}/horodatages`),
  emettreAttestation: (id) => http.post(`${C}/dossiers/${id}/attestation`),
  lireAttestation: (numero) => http.get(`${C}/attestations/${numero}/fiche`),
  telechargerAttestation: (numero) => http.blob(`${C}/attestations/${numero}`, 'application/pdf'),
  demanderStatut: (numero, e) => {
    const body = new FormData()
    body.set('typeDemande', e.typeDemande)
    body.set('motif', e.motif)
    body.set('piece', e.piece)
    if (e.conditionLevee) body.set('conditionLevee', e.conditionLevee)
    return http.post(`${C}/attestations/${numero}/demandes-de-statut`, { body })
  },
  approuverStatut: (demandeId, motif, assertion) =>
    http.post(`${C}/demandes-de-statut/${demandeId}/approbation`, { body: { motif }, reauthAssertion: assertion }),
  registre: (q) => http.get(`${C}/registre`, { query: requete(q) }),
  exporterRegistre: (q) => http.blob(`${C}/registre/export?${new URLSearchParams(Object.entries(requete(q)).filter(([, v]) => v !== undefined).map(([k, v]) => [k, String(v)])).toString()}`, 'text/csv'),
}
