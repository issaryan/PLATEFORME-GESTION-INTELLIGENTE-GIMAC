import { useMemo } from 'react'
import { useSession, type SourceRecherche } from '@gimac/socle'
import { useServices } from './services/ServicesProvider'
import { PERM } from './permissions'

const PAR_SOURCE = 5

/**
 * Sources de la recherche globale (Ctrl+K) du lot 1, selon les permissions de la session : liaisons
 * (référence, institution), institutions membres (nom, sigle) et conversations de la messagerie. Chaque
 * source interroge le service par sa recherche paginée (5 premiers résultats) ; la passerelle et les
 * services refont le contrôle des droits. « espace » est la racine de la couche (/des ou /console).
 */
export function useSourcesRecherche(permissions: string[] | undefined, espace = '/des'): SourceRecherche[] {
  const services = useServices()
  const moi = useSession()?.sujet
  const p = permissions ?? []
  const liaisons = espace === '/des' && p.includes(PERM.liaisonLister)
  const institutions = espace === '/des' && p.includes(PERM.institutionLire)
  const messagerie = p.includes(PERM.messageLire)
  return useMemo(() => {
    const sources: SourceRecherche[] = []
    if (liaisons) {
      sources.push({
        cle: 'liaisons',
        chercher: async (q) => (await services.parcours.listerLiaisons({ page: 1, taille: PAR_SOURCE, recherche: q })).contenu.map((l) => ({
          cle: l.id, type: 'liaison' as const, code: l.reference, libelle: l.institutionNom, detail: l.conducteurNom ?? undefined, href: `${espace}/parcours/${encodeURIComponent(l.id)}`,
        })),
      })
    }
    if (institutions) {
      sources.push({
        cle: 'institutions',
        chercher: async (q) => (await services.participant.listerInstitutions({ page: 1, taille: PAR_SOURCE, recherche: q })).contenu.map((i) => ({
          cle: i.id, type: 'institution' as const, code: i.sigle ?? undefined, libelle: i.nom, detail: i.pays, href: `${espace}/institutions/${encodeURIComponent(i.id)}`,
        })),
      })
    }
    if (messagerie) {
      sources.push({
        cle: 'conversations',
        chercher: async (q) => (await services.messagerie.conversations({ page: 0, taille: PAR_SOURCE, recherche: q })).contenu.map((c) => ({
          cle: c.id, type: 'conversation' as const, libelle: c.titre ?? c.membres.filter((m) => m.id !== moi).map((m) => m.nom).join(', '), detail: c.liaison?.reference,
          href: `${espace}/messagerie?conversation=${encodeURIComponent(c.id)}`,
        })),
      })
    }
    return sources
  }, [services, moi, espace, liaisons, institutions, messagerie])
}
