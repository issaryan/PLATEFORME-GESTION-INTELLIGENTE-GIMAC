import { http } from '@gimac/socle'
import type { Alerte, Disponibilite, ElementSupervise, Espace, EtatSante, MesureService, Ouverture, PageEspaces, PageSupervision } from '../modele'
import { activiteHttp } from '../activite/http'
import type { ServiceEspaces, ServiceSupervision, ServicesSandbox } from './ports'

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

export const BASE_SUPERVISION = '/bff/des/sbx-trace'

export const supervisionHttp: ServiceSupervision = {
  etatSante: () => http.get<EtatSante>(`${BASE_SUPERVISION}/etat-sante/detail`),
  elements: (q) => http.get<PageSupervision<ElementSupervise>>(`${BASE_SUPERVISION}/elements-supervises`, { query: q }),
  alertes: (q) => http.get<PageSupervision<Alerte>>(`${BASE_SUPERVISION}/alertes`, { query: q }),
  disponibilites: (q) => http.get<PageSupervision<Disponibilite>>(`${BASE_SUPERVISION}/disponibilites`, { query: q }),
  services: (q) => http.get<PageSupervision<MesureService>>(`${BASE_SUPERVISION}/services`, { query: q }),
}

export const servicesSandboxHttp = (): ServicesSandbox => ({ espaces: espacesHttp, supervision: supervisionHttp, activite: activiteHttp })
