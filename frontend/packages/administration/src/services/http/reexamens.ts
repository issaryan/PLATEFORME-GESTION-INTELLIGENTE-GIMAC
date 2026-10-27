import { http } from '@gimac/socle'
import type { ServiceReexamens } from '../portsReexamens'

const BASE = '/bff/console/iam/comptes/reexamens'

export const reexamensHttp = (): ServiceReexamens => ({
  lister: () => http.get(BASE),
  ouvrir: () => http.post(`${BASE}/ouverture`, { body: {} }),
  lire: (id) => http.get(`${BASE}/${encodeURIComponent(id)}`),
  decider: (id, compteId, d) => http.post(`${BASE}/${encodeURIComponent(id)}/comptes/${encodeURIComponent(compteId)}/decision`, { body: d }),
  cloturer: (id) => http.post(`${BASE}/${encodeURIComponent(id)}/cloture`, { body: {} }),
})
