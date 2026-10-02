import { http, P } from '@gimac/socle'
import type { Envoi, NouvelleConversation } from '../../modele/messagerie'
import type { ServiceMessagerie } from '../ports'

// Implémentation HTTP de la messagerie (D-64, docs/lot1/messagerie.md) : MS-COLLABORATION derrière la
// passerelle, /bff/{des,console}/collaboration/messagerie réécrit vers /api/v1/messagerie. L'envoi est un
// corps multipart : la partie JSON « message » et jusqu'à cinq parties « fichiers ».

function multipart(message: object, fichiers: File[]): FormData {
  const corps = new FormData()
  corps.set('message', new Blob([JSON.stringify(message)], { type: 'application/json' }))
  fichiers.forEach((f) => corps.append('fichiers', f, f.name))
  return corps
}

const envoi = (e: Envoi) => ({ contenu: e.contenu, etiquettes: e.etiquettes })
const nouvelle = (c: NouvelleConversation) => ({ ...envoi(c), destinataires: c.destinataires, titre: c.titre, liaisonId: c.liaisonId, ...(c.groupe ? { groupe: true } : {}) })

/** Messagerie d'une couche interne : le DES, ou la console (RSSI, super administrateur). */
export function messagerieHttpPour(couche: 'des' | 'console'): ServiceMessagerie {
  const M = `${couche === 'console' ? P.consoleCollaboration : P.desCollaboration}/messagerie`
  const PRESENCE = couche === 'console' ? P.consolePresence : P.desPresence
  return {
    // Pagination de l'interface à partir de 0, comme le service.
    annuaire: (recherche, page = 0, taille = 20) => http.get(`${M}/annuaire`, { query: { recherche: recherche || undefined, page, taille } }),
    // Liste, conversation et fil sont aussi relus par le canal poussé et le sondage de repli : appels de
    // fond, la session d'inactivité n'étant prolongée que par l'activité de l'utilisateur (D-65 §6, D-72).
    conversations: (q) => http.get(`${M}/conversations`, { fond: true, query: { page: q.page ?? 0, taille: q.taille ?? 20, recherche: q.recherche || undefined, liaisonId: q.liaisonId } }),
    conversation: (id) => http.get(`${M}/conversations/${encodeURIComponent(id)}`, { fond: true }),
    messages: (id, avant) => http.get(`${M}/conversations/${encodeURIComponent(id)}/messages`, { fond: true, query: { avant, taille: 50 } }),
    demarrer: (c) => http.post(`${M}/conversations`, { body: multipart(nouvelle(c), c.fichiers) }),
    envoyer: (id, e) => http.post(`${M}/conversations/${encodeURIComponent(id)}/messages`, { body: multipart(envoi(e), e.fichiers) }),
    marquerLu: (id) => http.post(`${M}/conversations/${encodeURIComponent(id)}/lu`),
    // Compteur périodique : appel de fond, qui ne prolonge pas la session d'inactivité (D-65 §6).
    nonLus: () => http.get(`${M}/non-lus`, { fond: true }),
    telechargerPiece: (id) => http.blob(`${M}/pieces/${encodeURIComponent(id)}`, '*/*'),
    renommerGroupe: (id, titre) => http.patch(`${M}/conversations/${encodeURIComponent(id)}`, { body: { titre } }),
    ajouterMembres: (id, membres) => http.post(`${M}/conversations/${encodeURIComponent(id)}/membres`, { body: { membres } }),
    retirerMembre: (id, membreId) => http.delete(`${M}/conversations/${encodeURIComponent(id)}/membres/${encodeURIComponent(membreId)}`),
    // Présence et saisie (D-72) : appels de fond, comme les relectures du canal.
    presence: () => http.get(PRESENCE, { fond: true }),
    signalerSaisie: (id) => http.post(`${M}/conversations/${encodeURIComponent(id)}/saisie`, { fond: true }),
  }
}

export const messagerieHttp: ServiceMessagerie = messagerieHttpPour('des')
