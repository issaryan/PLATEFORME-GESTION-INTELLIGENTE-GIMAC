import { http } from '@gimac/socle'
import type { Activite, FiltreActivite, LiaisonCertification, LigneCampagne, LigneCas, LigneParticipant, PageStatistiques, Synthese } from './modele'
import type { ServiceActivite } from './ports'

// SBX-LECTURE par la passerelle (D-121) : route des-sbx-lecture, cinq lectures exactes sous
// /bff/des/sbx-lecture/statistiques réécrites en /api/v1/console/statistiques ; contrôle OPA SBX_SUP_CONSULTATION.
// La chaîne de certification lit MS-HOMOLOGATION sous /bff/des/homologation/liaisons/{id} (HOM_LIAISON_READ).
export const BASE_STATISTIQUES = '/bff/des/sbx-lecture/statistiques'
export const BASE_LIAISONS = '/bff/des/homologation/liaisons'

const criteres = (f: FiltreActivite) => ({ du: f.du, au: f.au, participant: f.participant, interface: f.interface })

export const activiteHttp: ServiceActivite = {
  synthese: (f) => http.get<Synthese>(`${BASE_STATISTIQUES}/synthese`, { query: criteres(f) }),
  activite: (f, granularite) => http.get<Activite>(`${BASE_STATISTIQUES}/activite`, { query: { ...criteres(f), granularite } }),
  participants: (f, p) => http.get<PageStatistiques<LigneParticipant>>(`${BASE_STATISTIQUES}/participants`, { query: { ...criteres(f), ...p } }),
  campagnes: (f, tri, p) => http.get<PageStatistiques<LigneCampagne>>(`${BASE_STATISTIQUES}/campagnes`, { query: { ...criteres(f), tri, ...p } }),
  cas: (f, p) => http.get<PageStatistiques<LigneCas>>(`${BASE_STATISTIQUES}/cas`, { query: { ...criteres(f), ...p } }),
  // Source indisponible : SBX-TRACE ne reçoit pas encore codeRetour (exchange-captured.v1 à corriger côté modules).
  codesRetour: async () => null,
  liaison: (id) => http.get<LiaisonCertification>(`${BASE_LIAISONS}/${encodeURIComponent(id)}`),
}
