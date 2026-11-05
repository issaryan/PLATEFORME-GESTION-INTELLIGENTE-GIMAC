import { ApiError } from '@gimac/socle'
import type { Page } from '../../modele/commun'
import type { Liaison, ModificationPerimetre, PerimetreProjet } from '../../modele/homologation'
import { permissionsDuProfil } from '../../domaine'
import { ANNUAIRE } from './messagerie'

// Périmètre du projet de certification simulé (D-133), aux règles de PerimetreLiaison de MS-HOMOLOGATION : modifiable par
// le gestionnaire (HOM_PERIMETRE_EDIT et créateur de la liaison), un super administrateur ou un chargé de l'implémentation
// des projets de la SNA (HOM_PERIMETRE_EDIT_TOUS) ; service ajouté parmi les services souscrits, TRANSVERSE toujours inclus,
// motif obligatoire, projet clos refusé ; chaque modification est tracée. Les gestionnaires sont désignés par leur rôle.

/** Gestionnaire des liaisons simulées : le compte de démonstration du chef de la DDRM, désigné par son rôle. */
export const GESTIONNAIRE_DEMO = { id: ANNUAIRE.find((p) => p.profils?.includes('ROLE_CHEF_DDRM'))?.id ?? 'U-CHEF-DDRM', nom: 'Chef de la DDRM' }
/** Services souscrits simulés : ceux du type, plus un service non encore inclus pour démontrer l'ajout. */
export const SOUSCRITS: Record<Liaison['typeProjet'], string[]> = {
  CARTE_INTERBANCAIRE: ['TRANSVERSE', 'RETRAIT', 'PAIEMENT', 'TRANSFERT', 'COMMERCE_ELECTRONIQUE'],
  PAIEMENT_MOBILE: ['TRANSVERSE', 'TRANSFERT', 'PAIEMENT', 'FACTURE', 'CASH_IN'],
  DELEGATAIRE: ['TRANSVERSE'],
}
const FERMES = new Set(['CLOSE', 'CERTIFIEE'])

type Utilisateur = { id: string; nom: string; profils?: string[] } | undefined
type Etat = { services: Map<string, boolean>; journal: ModificationPerimetre[]; gestionnaire: { id: string; nom: string } }

const refus = (status: number, code: string, message: string, details?: Record<string, unknown>) => new ApiError(status, code, message, `sim-perimetre-${Date.now().toString(36)}`, details)

export function magasinPerimetres(utilisateur: Utilisateur) {
  const etats = new Map<string, Etat>()
  const permissions = new Set((utilisateur?.profils ?? []).flatMap(permissionsDuProfil))
  const etat = (l: Liaison): Etat => {
    let e = etats.get(l.id)
    if (!e) {
      e = { services: new Map((l.perimetre ?? []).map((c) => [c, true])), journal: [], gestionnaire: GESTIONNAIRE_DEMO }
      etats.set(l.id, e)
    }
    return e
  }
  /** Permission qui autorise la modification ; sans profil (démonstration libre), celle du gestionnaire. */
  const permission = (e: Etat): string | null => {
    if (!utilisateur?.profils?.length) return 'HOM_PERIMETRE_EDIT'
    if (permissions.has('HOM_PERIMETRE_EDIT_TOUS')) return 'HOM_PERIMETRE_EDIT_TOUS'
    return permissions.has('HOM_PERIMETRE_EDIT') && utilisateur.id === e.gestionnaire.id ? 'HOM_PERIMETRE_EDIT' : null
  }
  const inclus = (e: Etat) => [...e.services].filter(([, v]) => v).map(([c]) => c).sort()
  const vue = (l: Liaison): PerimetreProjet => {
    const e = etat(l)
    const modifiable = permission(e) !== null && !FERMES.has(l.etat)
    return {
      liaisonId: l.id, version: e.journal.length + 1, gestionnaireId: e.gestionnaire.id, gestionnaireNom: e.gestionnaire.nom,
      services: [...e.services].sort(([a], [b]) => a.localeCompare(b)).map(([code, v]) => ({ code, libelle: code, etat: v ? 'A_ENGAGER' : 'HORS_PERIMETRE', inclus: v })),
      proposables: modifiable ? SOUSCRITS[l.typeProjet] : [], modifiable,
    }
  }
  return {
    lire: vue,
    modifier(l: Liaison, services: string[], motif: string): PerimetreProjet {
      const e = etat(l)
      const p = permission(e)
      if (!p) throw refus(403, 'HOM_PERIMETRE_GESTIONNAIRE_SEUL', 'Seuls le gestionnaire du projet, un super administrateur ou un chargé de l\'implémentation des projets de la SNA modifient le périmètre.')
      if (!motif.trim()) throw refus(422, 'HOM_PERIMETRE_MOTIF_REQUIS', 'Le motif de la modification est obligatoire.')
      if (FERMES.has(l.etat)) throw refus(409, 'HOM_PERIMETRE_LIAISON_CLOSE', 'Le périmètre d\'un projet clos ou certifié n\'est plus modifiable.')
      const apres = [...new Set(['TRANSVERSE', ...services])].sort()
      if (apres.length === 1) throw refus(422, 'HOM_PERIMETRE_VIDE', 'Le périmètre porte au moins un service à certifier.')
      const avant = inclus(e)
      const nonSouscrits = apres.filter((c) => !avant.includes(c) && !SOUSCRITS[l.typeProjet].includes(c))
      if (nonSouscrits.length) throw refus(422, 'HOM_PERIMETRE_SERVICE_NON_SOUSCRIT', 'Un service non souscrit ne peut entrer au périmètre.', { services: nonSouscrits })
      if (avant.join() === apres.join()) return vue(l)
      for (const c of e.services.keys()) e.services.set(c, apres.includes(c))
      for (const c of apres) e.services.set(c, true)
      e.journal.unshift({
        version: e.journal.length + 2, auteurId: utilisateur?.id ?? GESTIONNAIRE_DEMO.id, auteurNom: utilisateur?.nom ?? GESTIONNAIRE_DEMO.nom,
        permissionUtilisee: p, avant, apres, motif: motif.trim(), modifieLe: new Date().toISOString(),
      })
      l.perimetre = apres
      return vue(l)
    },
    historique(l: Liaison, page: number, taille: number): Page<ModificationPerimetre> {
      const j = etat(l).journal
      return { contenu: j.slice(page * taille, page * taille + taille), total: j.length }
    },
  }
}

/** Les trois méthodes du port (ServiceHomologation) servies par le magasin, avec la latence de la simulation. */
export function servicesPerimetre(m: ReturnType<typeof magasinPerimetres>, trouver: (id: string) => Liaison, attendre: <T>(v: () => T) => Promise<T>) {
  return {
    perimetreLiaison: (id: string) => attendre(() => m.lire(trouver(id))),
    modifierPerimetre: (id: string, services: string[], motif: string) => attendre(() => m.modifier(trouver(id), services, motif)),
    historiquePerimetre: (id: string, page: number, taille: number) => attendre(() => m.historique(trouver(id), page, taille)),
  }
}
