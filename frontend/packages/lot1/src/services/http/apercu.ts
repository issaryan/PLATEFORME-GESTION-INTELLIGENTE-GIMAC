import { http, P } from '@gimac/socle'
import type { ServiceApercu } from '../ports'

// Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md §2 et §4.4) : le contenu déjà
// téléchargé part tel quel vers MS-DOCUMENT, qui le convertit en PDF. Le nom n'est transmis que pour son
// extension (X-Nom-Fichier, encodé en URI) ; la couche est celle de l'application (DES ou console).
export function apercuHttp(couche: 'des' | 'console'): ServiceApercu {
  const route = `${couche === 'console' ? P.consoleDocument : P.desDocument}/apercus`
  return {
    convertir: (source, nom) => http.post<Blob>(route, {
      body: source,
      contentType: source.type || 'application/octet-stream',
      entetes: { 'X-Nom-Fichier': encodeURIComponent(nom) },
      accept: 'application/pdf, application/problem+json',
      binaire: true,
    }),
  }
}
