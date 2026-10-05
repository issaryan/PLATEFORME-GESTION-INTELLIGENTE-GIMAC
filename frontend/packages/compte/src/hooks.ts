import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { urlAvatar, useConnexion, type ApiError, type CleSecurite } from '@gimac/socle'
import { useServiceCompte } from './contexte'
import type { Appareil, ChangementMotDePasse, ConfirmationTotp, FichePersonnelle, Moi, PageHistorique, PremiereConfiguration, RattachementCompte, SessionCompte } from './modele'

// Hooks de données de « Mon compte » : seuls points d'appel du port. Clés [compte, ressource].
export const clesCompte = {
  tout: ['compte'] as const,
  moi: ['compte', 'moi'] as const,
  sessions: ['compte', 'sessions'] as const,
  cles: ['compte', 'cles'] as const,
  appareils: ['compte', 'appareils'] as const,
  historique: (page: number, taille: number) => ['compte', 'historique', page, taille] as const,
  organisation: ['compte', 'organisation'] as const,
}

export const useMoi = () => {
  const s = useServiceCompte()
  return useQuery<Moi, ApiError>({ queryKey: clesCompte.moi, queryFn: () => s.moi() })
}
/** Rattachement organisationnel (D-76) : lu seulement si le compte en a un et si la couche le sert. */
export const useMonRattachement = (actif: boolean) => {
  const s = useServiceCompte()
  return useQuery<RattachementCompte, ApiError>({ queryKey: clesCompte.organisation, queryFn: () => s.organisation!(), enabled: actif && !!s.organisation })
}
export const useMesSessions = () => {
  const s = useServiceCompte()
  return useQuery<SessionCompte[], ApiError>({ queryKey: clesCompte.sessions, queryFn: () => s.sessions() })
}
export const useMesCles = () => {
  const s = useServiceCompte()
  return useQuery<CleSecurite[], ApiError>({ queryKey: clesCompte.cles, queryFn: () => s.cles() })
}

/** Action à effet : service puis invalidation de tout « Mon compte ». */
function useEffetCompte<V, R>(agir: (v: V) => Promise<R>) {
  const qc = useQueryClient()
  return useMutation<R, ApiError | Error, V>({ mutationFn: agir, onSuccess: () => qc.invalidateQueries({ queryKey: clesCompte.tout }) })
}

export const usePremiereConfiguration = () => {
  const s = useServiceCompte()
  return useEffetCompte((p: PremiereConfiguration) => s.premiereConfiguration(p))
}
export const useModifierFiche = () => {
  const s = useServiceCompte()
  return useEffetCompte((f: FichePersonnelle) => s.modifier(f))
}
/** Changement du mot de passe sur la plateforme (D-65). */
export const useChangerMotDePasse = () => {
  const s = useServiceCompte()
  return useEffetCompte((c: ChangementMotDePasse) => s.changerMotDePasse(c))
}
/** Nouveau secret TOTP et son URI otpauth (sans effet sur le compte avant confirmation). */
export const useEnrolerTotp = () => {
  const s = useServiceCompte()
  return useMutation({ mutationFn: () => s.enrolerTotp() })
}
/** Confirmation du code TOTP par un premier code de l'application d'authentification. */
export const useConfirmerTotp = () => {
  const s = useServiceCompte()
  return useEffetCompte((c: ConfirmationTotp) => s.confirmerTotp(c))
}
export const useFermerSession = () => {
  const s = useServiceCompte()
  return useEffetCompte((id: string) => s.fermerSession(id))
}
export const useFermerSessions = () => {
  const s = useServiceCompte()
  return useEffetCompte(() => s.fermerSessions())
}
export const useEnrolerCle = () => {
  const s = useServiceCompte()
  return useEffetCompte((nom: string) => s.enrolerCle(nom))
}
export const useSupprimerCle = () => {
  const s = useServiceCompte()
  return useEffetCompte((id: string) => s.supprimerCle(id))
}

/** Photo de profil du titulaire : adresse versionnée (/bff/<couche>/iam/avatars/{id}?v=…), undefined sans photo. */
export function useMonAvatar(): { nom: string; src: string | undefined; fonction: string | null } | null {
  const moi = useMoi()
  const { couche } = useConnexion()
  const c = moi.data?.compte
  if (!c) return null
  return { nom: c.nomAffiche, src: urlAvatar(couche, c.id, c.avatarVersion), fonction: c.fonction }
}
/** Remplace la photo (image déjà recadrée) ; /moi relu ensuite (nouvelle version). */
export const useChangerPhoto = () => {
  const s = useServiceCompte()
  return useEffetCompte((image: Blob) => s.changerPhoto(image))
}
export const useRetirerPhoto = () => {
  const s = useServiceCompte()
  return useEffetCompte(() => s.retirerPhoto())
}
/** Appareils connectés (sessions de la passerelle), rafraîchis à chaque retour sur la page. */
export const useAppareils = () => {
  const s = useServiceCompte()
  return useQuery<Appareil[], ApiError>({ queryKey: clesCompte.appareils, queryFn: () => s.appareils() })
}
export const useFermerAppareil = () => {
  const s = useServiceCompte()
  return useEffetCompte((id: string) => s.fermerAppareil(id))
}
export const useFermerAutresAppareils = () => {
  const s = useServiceCompte()
  return useEffetCompte(() => s.fermerAutresAppareils())
}
/** Historique des connexions, page à partir de 1 dans l'interface (0 pour le service). */
export const useHistoriqueConnexions = (page: number, taille: number) => {
  const s = useServiceCompte()
  return useQuery<PageHistorique, ApiError>({ queryKey: clesCompte.historique(page, taille), queryFn: () => s.historique(page - 1, taille), placeholderData: keepPreviousData })
}
