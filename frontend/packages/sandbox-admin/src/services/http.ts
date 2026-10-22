import { http } from '@gimac/socle'
import type { Espace, Ouverture, PageEspaces } from '../modele'
import type { ServiceEspaces, ServicesSandbox } from './ports'

// SBX-ESPACE par la passerelle (D-118, D-122) : route des-sbx-espace, /bff/des/sbx-espace/(.*) réécrit en
// /api/v1/console/$1 ; contrôle OPA SBX_HAB_CONSULTATION en lecture, SBX_HAB_ADMINISTRATION en écriture.
export const BASE = '/bff/des/sbx-espace/espaces'
const e = encodeURIComponent
const cle = (c: string) => ({ 'Idempotency-Key': c })

export const espacesHttp: ServiceEspaces = {
  lister: (q) => http.get<PageEspaces>(BASE, { query: q }),
  consulter: (code) => http.get<Espace>(`${BASE}/${e(code)}`),
  ouvrir: (demande, c) => http.post<Ouverture>(BASE, { body: demande, entetes: cle(c) }),
  agir: (code, acte, c) => http.post<Espace>(`${BASE}/${e(code)}/${acte}`, { body: {}, entetes: cle(c) }),
}

export const servicesSandboxHttp = (): ServicesSandbox => ({ espaces: espacesHttp })
