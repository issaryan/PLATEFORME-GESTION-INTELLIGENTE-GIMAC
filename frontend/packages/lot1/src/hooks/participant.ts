import type { Candidature, ConditionsAdhesion, FiltreInstitutions } from '../modele/participant'
import { useEffet, useLecture } from './base'
import { cles } from './cles'

// Hooks de données du registre des institutions et du tableau de bord de la DDRM (DES-01, DES-05 à
// DES-07) : seuls points d'appel du port ServiceParticipant.

export const useInstitutions = (f: FiltreInstitutions, actif = true) => useLecture(cles.institutions(f), (s) => s.participant.listerInstitutions(f), { enabled: actif })
export const useIndicateursDdrm = (actif = true) => useLecture(cles.indicateursDdrm(), (s) => s.participant.indicateurs(), { enabled: actif })
export const useDossierInstitution = (id: string) => useLecture(cles.institution(id), (s) => s.participant.dossier(id))

export const useEnregistrerInstitution = () => useEffet((s, c: Candidature) => s.participant.enregistrer(c), [cles.participant])
export const useModifierInstitution = () => useEffet((s, v: { id: string; c: Candidature }) => s.participant.modifier(v.id, v.c), [cles.participant])
export const useDefinirConditions = () => useEffet((s, v: { id: string; c: ConditionsAdhesion }) => s.participant.definirConditions(v.id, v.c), [cles.participant])
export const useDeposerPiece = () =>
  useEffet((s, v: { id: string; code: string; fichier: File }) => s.participant.deposerPiece(v.id, v.code, v.fichier), [cles.participant])
