import { ApiError, http } from '@gimac/socle'
import type { Activite, CodeRetour, FiltreActivite, LiaisonCertification, LigneCampagne, LigneCas, LigneParticipant, PageStatistiques, Synthese } from './modele'
import type { ServiceActivite } from './ports'

// SBX-LECTURE par la passerelle (D-121) : route des-sbx-lecture, cinq lectures exactes sous
// /bff/des/sbx-lecture/statistiques réécrites en /api/v1/console/statistiques ; contrôle OPA SBX_SUP_CONSULTATION.
// La chaîne de certification lit MS-HOMOLOGATION sous /bff/des/homologation/liaisons/{id} (HOM_LIAISON_READ).
export const BASE_STATISTIQUES = '/bff/des/sbx-lecture/statistiques'
export const BASE_LIAISONS = '/bff/des/homologation/liaisons'
/** Codes de retour des échanges, lus dans SBX-TRACE (route des-sbx-trace, même permission SBX_SUP_CONSULTATION). */
export const CODES_RETOUR = '/bff/des/sbx-trace/statistiques/codes-retour'

const criteres = (f: FiltreActivite) => ({ du: f.du, au: f.au, participant: f.participant, interface: f.interface })

export const activiteHttp: ServiceActivite = {
  synthese: (f) => http.get<Synthese>(`${BASE_STATISTIQUES}/synthese`, { query: criteres(f) }),
  activite: (f, granularite) => http.get<Activite>(`${BASE_STATISTIQUES}/activite`, { query: { ...criteres(f), granularite } }),
  participants: (f, p) => http.get<PageStatistiques<LigneParticipant>>(`${BASE_STATISTIQUES}/participants`, { query: { ...criteres(f), ...p } }),
  campagnes: (f, tri, p) => http.get<PageStatistiques<LigneCampagne>>(`${BASE_STATISTIQUES}/campagnes`, { query: { ...criteres(f), tri, ...p } }),
  cas: (f, p) => http.get<PageStatistiques<LigneCas>>(`${BASE_STATISTIQUES}/cas`, { query: { ...criteres(f), ...p } }),
  // Route absente (SBX-TRACE antérieure au contrat exchange-captured.v1 complet) : source indisponible, pas une erreur.
  codesRetour: (f, p) =>
    http.get<PageStatistiques<CodeRetour>>(CODES_RETOUR, { query: { ...criteres(f), ...p } }).catch((e: unknown) => {
      if (e instanceof ApiError && e.status === 404) return null
      throw e
    }),
  liaison: (id) => http.get<LiaisonCertification>(`${BASE_LIAISONS}/${encodeURIComponent(id)}`),
}
