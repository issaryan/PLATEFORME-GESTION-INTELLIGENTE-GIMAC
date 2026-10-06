import { useCallback, useEffect, useRef } from 'react'
import { keepPreviousData, useInfiniteQuery, useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query'
import { initialiserPresence, surAccuseLecture, type ApiError } from '@gimac/socle'
import type { Conversation, Envoi, FilMessages, LectureMessage, Message, NouvelleConversation, PageMessagerie } from '../modele/messagerie'
import { useServices } from '../services/ServicesProvider'
import { useLecture } from './base'

// Données de la messagerie (D-64). Toutes les clés commencent par le sujet « collaboration » : un message
// reçu par le canal poussé invalide la liste, le fil ouvert et le compteur de non-lus.
export const clesMessagerie = {
  tout: ['collaboration'] as const,
  annuaire: (recherche: string) => ['collaboration', 'annuaire', recherche] as const,
  conversations: (q: { recherche?: string; liaisonId?: string; page?: number }) => ['collaboration', 'conversations', q] as const,
  conversation: (id: string) => ['collaboration', 'conversation', id] as const,
  fil: (id: string) => ['collaboration', 'conversation', id, 'messages'] as const,
  /** Détail « Lu par » (D-88) : sous le fil, relu avec lui. */
  lectures: (id: string, messageId: string) => ['collaboration', 'conversation', id, 'messages', messageId, 'lectures'] as const,
  nonLus: () => ['collaboration', 'non-lus'] as const,
}

/**
 * Repli du canal poussé : la liste et le fil ouverts sont relus toutes les 5 secondes tant que la page est
 * visible (jamais en arrière-plan), si un événement se perd (relais coupé, onglet resté sur un autre compte).
 */
export const SONDAGE_MESSAGERIE = 5_000

export const useAnnuaire = (recherche: string, actif = true) =>
  useLecture(clesMessagerie.annuaire(recherche), (s) => s.messagerie.annuaire(recherche, 0, 20), { enabled: actif, placeholderData: keepPreviousData })

export const useConversations = (q: { recherche?: string; liaisonId?: string; page?: number }) =>
  useLecture(clesMessagerie.conversations(q), (s) => s.messagerie.conversations({ ...q, taille: 20 }), { placeholderData: keepPreviousData, refetchInterval: SONDAGE_MESSAGERIE })

export const useConversation = (id: string | undefined) =>
  useLecture(clesMessagerie.conversation(id ?? ''), (s) => s.messagerie.conversation(id!), { enabled: !!id })

/** Compteur de non-lus de la navigation ; rafraîchi par le canal poussé, et toutes les minutes à défaut. */
export const useNonLus = (actif = true) =>
  useLecture(clesMessagerie.nonLus(), (s) => s.messagerie.nonLus(), { enabled: actif, refetchInterval: 60_000 })

/** Fil d'une conversation : la dernière page d'abord, les plus anciennes à la demande. */
export function useFil(id: string | undefined) {
  const s = useServices()
  return useInfiniteQuery<FilMessages, ApiError, FilMessages[], readonly unknown[], string | undefined>({
    queryKey: clesMessagerie.fil(id ?? ''),
    enabled: !!id,
    initialPageParam: undefined,
    queryFn: ({ pageParam }) => s.messagerie.messages(id!, pageParam),
    getNextPageParam: (derniere) => (derniere.plusAnciens ? derniere.contenu[0]?.id : undefined),
    select: (d) => d.pages,
    refetchInterval: SONDAGE_MESSAGERIE,
  })
}

/** Détail « Lu par » d'un de mes messages de groupe (D-88), lu à l'ouverture seulement. */
export const useLecturesMessage = (conversationId: string, messageId: string, actif = true) =>
  useLecture(clesMessagerie.lectures(conversationId, messageId), (s) => s.messagerie.lectures(conversationId, messageId), { enabled: actif })

/** Mon message envoyé jusqu'à `luJusquA` : lu par l'unique destinataire d'un échange direct. */
const luEnDirect = <T extends { envoyeLe: string; lecture?: LectureMessage | null }>(m: T, luJusquA: number): T =>
  m.lecture && m.lecture.etat !== 'LU' && Date.parse(m.envoyeLe) <= luJusquA ? { ...m, lecture: { etat: 'LU' as const, lus: m.lecture.destinataires, destinataires: m.lecture.destinataires } } : m

/**
 * Accusés de lecture reçus pour la conversation ouverte (D-88 §4) : en échange direct, mes messages envoyés
 * jusqu'à `luJusquA` passent à LU dans le fil et dans la liste, sans rien relire ; en groupe, l'événement ne dit
 * pas si ce lecteur était déjà compté : seuls le fil affiché et le détail « Lu par » ouvert sont relus. Jamais
 * de relecture de la liste ni du compteur de non-lus.
 */
export function useAccusesLecture(conversation: Pick<Conversation, 'id' | 'type'> | undefined) {
  const qc = useQueryClient()
  const id = conversation?.id
  const type = conversation?.type
  useEffect(() => {
    if (!id) return undefined
    return surAccuseLecture((a) => {
      if (a.conversationId !== id) return
      if (type === 'GROUPE') {
        void qc.invalidateQueries({ queryKey: clesMessagerie.fil(id) })
        return
      }
      const jusqua = Date.parse(a.luJusquA)
      qc.setQueryData<InfiniteData<FilMessages>>(clesMessagerie.fil(id), (d) => d && {
        ...d, pages: d.pages.map((p) => ({ ...p, contenu: p.contenu.map((m): Message => (m.moi ? luEnDirect(m, jusqua) : m)) })),
      })
      qc.setQueriesData<PageMessagerie<Conversation>>({ queryKey: ['collaboration', 'conversations'] }, (l) => l && {
        ...l, contenu: l.contenu.map((c) => (c.id === id && c.dernierMessage ? { ...c, dernierMessage: luEnDirect(c.dernierMessage, jusqua) } : c)),
      })
    })
  }, [id, type, qc])
}

function useEcriture<V, R>(agir: (v: V) => Promise<R>) {
  const qc = useQueryClient()
  return useMutation<R, ApiError, V>({ mutationFn: agir, onSuccess: () => qc.invalidateQueries({ queryKey: clesMessagerie.tout }) })
}

export function useDemarrer() {
  const s = useServices()
  return useEcriture((n: NouvelleConversation) => s.messagerie.demarrer(n))
}
export function useEnvoyer(conversationId: string) {
  const s = useServices()
  return useEcriture((e: Envoi) => s.messagerie.envoyer(conversationId, e))
}
export function useMarquerLu() {
  const s = useServices()
  return useEcriture((id: string) => s.messagerie.marquerLu(id))
}
export function useTelechargerPiece() {
  const s = useServices()
  return useMutation<{ blob: Blob; empreinte: string | null }, ApiError, string>({ mutationFn: (id) => s.messagerie.telechargerPiece(id) })
}

/** Repartage d'une pièce par son ticket (D-74) : message sans texte vers une autre conversation, même document. */
export function useTransferer() {
  const s = useServices()
  return useEcriture(({ conversationId, pieceId }: { conversationId: string; pieceId: string }) =>
    s.messagerie.envoyer(conversationId, { contenu: '', etiquettes: [], fichiers: [], transferts: [pieceId] }),
  )
}

/** Groupes de discussion (D-68) : nom, membres ajoutés, retrait ou départ. */
export function useRenommerGroupe(conversationId: string) {
  const s = useServices()
  return useEcriture((titre: string) => s.messagerie.renommerGroupe(conversationId, titre))
}
export function useAjouterMembres(conversationId: string) {
  const s = useServices()
  return useEcriture((membres: string[]) => s.messagerie.ajouterMembres(conversationId, membres))
}
export function useRetirerMembre(conversationId: string) {
  const s = useServices()
  return useEcriture((membreId: string) => s.messagerie.retirerMembre(conversationId, membreId))
}

/**
 * Présence des comptes internes (D-72) : état initial lu à l'ouverture de la messagerie et à chaque
 * (re)connexion du canal poussé (clé ['presence']), puis tenu à jour par ses signaux.
 */
export function usePresenceMessagerie() {
  const r = useLecture(['presence'], (s) => s.messagerie.presence())
  useEffect(() => {
    if (r.data) initialiserPresence(r.data)
  }, [r.data])
}

/** Un signal « en train d'écrire » au plus toutes les 3 secondes pendant la frappe (D-72). */
export const INTERVALLE_SAISIE_MS = 3_000

export function useSignalerSaisie(conversationId: string) {
  const s = useServices()
  const dernier = useRef(0)
  return useCallback(() => {
    const maintenant = Date.now()
    if (maintenant - dernier.current < INTERVALLE_SAISIE_MS) return
    dernier.current = maintenant
    s.messagerie.signalerSaisie(conversationId).catch(() => undefined)
  }, [s, conversationId])
}
