import { keepPreviousData, useMutation, useQueryClient } from '@tanstack/react-query'
import type { ApiError } from '@gimac/socle'
import type { EntreeActivite, EntreeEvenement, EntreeRapport, EntreeTache, EtatTache, ReponseInvitation, StatutActivite, VueRapports, VueTaches } from '../modele/pilotage'
import { useServices } from '../services/ServicesProvider'
import type { Services } from '../services/ports'
import { useLecture } from './base'

// Données de « Mon activité » (D-76). Toutes les clés commencent par le sujet « pilotage » : un geste
// invalide l'ensemble (journée, agenda, tâches, rapports, équipe et synthèse du tableau de bord), qui se
// recoupent (une activité reportée, une tâche terminée, un rapport soumis changent plusieurs écrans).
export const clesPilotage = {
  tout: ['pilotage'] as const,
  agenda: (du: string, au: string, collaborateur?: string) => ['pilotage', 'agenda', du, au, collaborateur ?? ''] as const,
  activites: (du: string, au: string, collaborateur?: string) => ['pilotage', 'activites', du, au, collaborateur ?? ''] as const,
  taches: (q: object) => ['pilotage', 'taches', q] as const,
  tache: (id: string) => ['pilotage', 'tache', id] as const,
  rapports: (q: object) => ['pilotage', 'rapports', q] as const,
  rapport: (id: string) => ['pilotage', 'rapport', id] as const,
  groupe: (id: string) => ['pilotage', 'groupe', id] as const,
  equipe: (jour: string) => ['pilotage', 'equipe', jour] as const,
  synthese: (jour: string) => ['pilotage', 'synthese', jour] as const,
  unites: () => ['organisation', 'unites'] as const,
  organisation: () => ['organisation', 'moi'] as const,
}

export const useAgenda = (du: string, au: string, collaborateur?: string) =>
  useLecture(clesPilotage.agenda(du, au, collaborateur), (s) => s.pilotage.agenda(du, au, collaborateur || undefined), { placeholderData: keepPreviousData })
export const useActivites = (du: string, au: string, collaborateur?: string) =>
  useLecture(clesPilotage.activites(du, au, collaborateur), (s) => s.pilotage.activites(du, au, collaborateur || undefined))
export const useTaches = (q: { vue: VueTaches; etat?: EtatTache; page?: number; taille?: number }, actif = true) =>
  useLecture(clesPilotage.taches(q), (s) => s.pilotage.taches(q), { enabled: actif, placeholderData: keepPreviousData })
/** Suivi d'un groupe de tâches confiées à plusieurs personnes (D-81 §6). */
export const useGroupe = (id: string | undefined) => useLecture(clesPilotage.groupe(id ?? ''), (s) => s.pilotage.groupe(id!), { enabled: !!id })
export const useTache = (id: string | undefined) => useLecture(clesPilotage.tache(id ?? ''), (s) => s.pilotage.tache(id!), { enabled: !!id })
export const useRapports = (q: { vue: VueRapports; page?: number; taille?: number }, actif = true) =>
  useLecture(clesPilotage.rapports(q), (s) => s.pilotage.rapports(q), { enabled: actif, placeholderData: keepPreviousData })
export const useRapport = (id: string | undefined) => useLecture(clesPilotage.rapport(id ?? ''), (s) => s.pilotage.rapport(id!), { enabled: !!id })
export const useEquipe = (jour: string, actif = true) => useLecture(clesPilotage.equipe(jour), (s) => s.pilotage.equipe(jour), { enabled: actif })
export const useSynthese = (jour: string) => useLecture(clesPilotage.synthese(jour), (s) => s.pilotage.synthese(jour))
/** Catalogue des unités : libellés des codes (rarement modifié). */
export const useUnites = () => useLecture(clesPilotage.unites(), (s) => s.pilotage.unites(), { staleTime: 300_000 })
export const useOrganisation = () => useLecture(clesPilotage.organisation(), (s) => s.pilotage.organisation(), { staleTime: 60_000 })

/** Geste de « Mon activité » : service puis relecture de tout le sujet. */
function useGeste<V, R>(agir: (s: Services, v: V) => Promise<R>) {
  const s = useServices()
  const qc = useQueryClient()
  return useMutation<R, ApiError, V>({ mutationFn: (v) => agir(s, v), onSuccess: () => qc.invalidateQueries({ queryKey: clesPilotage.tout }) })
}

export const useCreerEvenement = () => useGeste((s, e: EntreeEvenement) => s.pilotage.creerEvenement(e))
export const useModifierEvenement = () => useGeste((s, v: { id: string; e: EntreeEvenement }) => s.pilotage.modifierEvenement(v.id, v.e))
export const useSupprimerEvenement = () => useGeste((s, id: string) => s.pilotage.supprimerEvenement(id))
export const useRepondre = () => useGeste((s, v: { id: string; reponse: Exclude<ReponseInvitation, 'EN_ATTENTE'> }) => s.pilotage.repondre(v.id, v.reponse))

export const useCreerActivite = () => useGeste((s, a: EntreeActivite) => s.pilotage.creerActivite(a))
export const useModifierActivite = () => useGeste((s, v: { id: string; a: EntreeActivite }) => s.pilotage.modifierActivite(v.id, v.a))
export const useChangerStatut = () => useGeste((s, v: { id: string; statut: StatutActivite; reporteeAu?: string }) => s.pilotage.changerStatut(v.id, v.statut, v.reporteeAu))
export const useSupprimerActivite = () => useGeste((s, id: string) => s.pilotage.supprimerActivite(id))
/** Réordonner : deux activités voisines échangent leur rang. */
export const useEchangerActivites = () =>
  useGeste(async (s, v: { a: { id: string } & EntreeActivite; b: { id: string } & EntreeActivite }) => {
    await s.pilotage.modifierActivite(v.a.id, { jour: v.a.jour, titre: v.a.titre, description: v.a.description, dureeMinutes: v.a.dureeMinutes, ordre: v.b.ordre })
    await s.pilotage.modifierActivite(v.b.id, { jour: v.b.jour, titre: v.b.titre, description: v.b.description, dureeMinutes: v.b.dureeMinutes, ordre: v.a.ordre })
  })

export const useCreerTache = () => useGeste((s, t: EntreeTache) => s.pilotage.creerTache(t))
export const useModifierTache = () => useGeste((s, v: { id: string; t: EntreeTache }) => s.pilotage.modifierTache(v.id, v.t))
export const useCreerGroupe = () => useGeste((s, t: EntreeTache & { responsables: string[] }) => s.pilotage.creerGroupe(t))
export const usePrendre = () => useGeste((s, id: string) => s.pilotage.prendre(id))
export const useAttribuer = () => useGeste((s, v: { id: string; responsableId: string }) => s.pilotage.attribuer(v.id, v.responsableId))
export const useRelancer = () => useGeste((s, id: string) => s.pilotage.relancer(id))
export const useTransition = () => useGeste((s, v: { id: string; etat: EtatTache; motif?: string }) => s.pilotage.transition(v.id, v.etat, v.motif))

export const usePreremplir = () => {
  const s = useServices()
  return useMutation<{ contenu: string }, ApiError, { type: EntreeRapport['type']; periode: string }>({ mutationFn: (v) => s.pilotage.preremplir(v.type, v.periode) })
}
export const useCreerRapport = () => useGeste((s, r: EntreeRapport) => s.pilotage.creerRapport(r))
export const useModifierRapport = () => useGeste((s, v: { id: string; r: EntreeRapport }) => s.pilotage.modifierRapport(v.id, v.r))
export const useSoumettre = () => useGeste((s, v: string | { id: string; destinataireId?: string }) => (typeof v === 'string' ? s.pilotage.soumettre(v) : s.pilotage.soumettre(v.id, v.destinataireId)))
/** Destinataire du rapport et choix possibles avant la soumission (§2.4). */
export const useDestinatairesRapport = (id: string | undefined, actif = true) =>
  useLecture([...clesPilotage.rapport(id ?? ''), 'destinataires'], (s) => s.pilotage.destinatairesRapport(id!), { enabled: actif && !!id })
export const useViser = () => useGeste((s, v: { id: string; commentaire: string }) => s.pilotage.viser(v.id, v.commentaire))
export const useDocumentRapport = () => {
  const s = useServices()
  return useMutation<{ blob: Blob; empreinte: string | null }, ApiError, string>({ mutationFn: (id) => s.pilotage.documentRapport(id) })
}
