import type { ApiError } from '@gimac/socle'
import type { PersonneActivite, Unite } from '../../modele/pilotage'
import {
  ACTIONS_POUVOIR, AUCUN_POUVOIR, MOTIF_DEMANDE, PORTEES, rangPortee, type ActionPouvoir, type ActionsPouvoir, type Demande, type EntreePouvoirs, type MesPouvoirs,
  type NouvelleDemande, type PouvoirsProfil, type Portee, type Subordonne,
} from '../../modele/pouvoirs'
import type { ServicePouvoirs } from '../ports'
import { ANNUAIRE } from './messagerie'
import { ORGANISATION_DEMO, UNITES_DEMO } from './pilotage'
import { PRESENTATIONS_PROFILS } from './presentations'

// MS-IAM simulé pour les pouvoirs hiérarchiques (D-81, docs/lot1/pouvoirs-hierarchiques.md) : matrice des
// pouvoirs, périmètre de chacun, demandes adressées à l'administration. Un même magasin peut servir l'application
// interne (mes pouvoirs, mes demandes) et l'administration simulée (matrice, demandes à traiter) d'une page ; ses
// règles ne sont qu'une imitation de celles du service, avec les mêmes codes de refus.

type Attendre = <T>(v: T | (() => T)) => Promise<T>
type Refus = (status: number, code: string, message: string) => ApiError

/** Responsables d'unité de l'organisation de démonstration (§2). */
export const RESPONSABLES_UNITES_DEMO: Record<string, string> = {
  DG: 'u-dg', DES: 'U-001', PROD_INFO: 'u-resp-info', DDRM: 'U-020', SSI: 'u-rssi', CONFORMITE: 'u-conf',
}

/** Profils des responsables (§3) : portées par défaut ; les autres profils n'ont aucun pouvoir. */
export const PROFILS_RESPONSABLES = ['ROLE_CHEF_DES', 'ROLE_CHEF_DDRM', 'ROLE_DG', 'ROLE_RSSI', 'ROLE_CONFORMITE', 'ROLE_RESPONSABLE_SERVICE']

/** Profils privilégiés : jamais demandables (§3). */
export const PROFILS_PRIVILEGIES = ['ROLE_SUPER_ADMIN', 'ROLE_RSSI', 'ROLE_ADMIN_SYSTEME']

/** Profils du domaine (lignes de la matrice de démonstration). */
export const PROFILS_DOMAINE = [
  'ROLE_CHEF_DES', 'ROLE_CHEF_DDRM', 'ROLE_DG', 'ROLE_RSSI', 'ROLE_CONFORMITE', 'ROLE_RESPONSABLE_SERVICE', 'ROLE_CHARGE_DDRM', 'ROLE_CONDUCTEUR_PROJET',
  'ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_SNA', 'ROLE_PRODUCTION_MONETIQUE', 'ROLE_POLE_RESEAU', 'ROLE_POLE_SYSTEME', 'ROLE_COLLABORATEUR', 'ROLE_SUPER_ADMIN',
  'ROLE_ADMIN_SYSTEME',
]

/** Portées par défaut des responsables (§3). */
export const PORTEES_RESPONSABLES: ActionsPouvoir = {
  TACHE_CONFIER: 'UNITES', TACHE_SUIVRE: 'UNITES', ACTIVITE_LIRE: 'EQUIPE', RAPPORT_VISER: 'EQUIPE', RATTACHEMENT_MODIFIER: 'DIRECTS', COMPTE_DEMANDER: 'UNITES',
  PROFIL_DEMANDER: 'EQUIPE', STATUT_DEMANDER: 'EQUIPE', PROCESSUS_DEFINIR: 'UNITES', PROCESSUS_LANCER: 'UNITES',
}

/** Profils qu'un responsable propose par défaut pour un subordonné (aucun profil privilégié). */
export const DEMANDABLES_DEFAUT = [
  'ROLE_COLLABORATEUR', 'ROLE_POLE_RESEAU', 'ROLE_POLE_SYSTEME', 'ROLE_SNA', 'ROLE_PRODUCTION_MONETIQUE', 'ROLE_CONDUCTEUR_PROJET', 'ROLE_INGENIEUR_HOMOLOGATION',
  'ROLE_CHARGE_DDRM', 'ROLE_RESPONSABLE_SERVICE',
]

/** Ligne de la matrice par défaut d'un profil, avec son libellé (D-86 §5 : le code à défaut). */
export const pouvoirsParDefaut = (profil: string, libelle = PRESENTATIONS_PROFILS[profil]?.libelle ?? profil): PouvoirsProfil => PROFILS_RESPONSABLES.includes(profil)
  ? { profil, libelle, actions: { ...PORTEES_RESPONSABLES }, profilsDemandables: [...DEMANDABLES_DEFAUT] }
  : { profil, libelle, actions: { ...AUCUN_POUVOIR }, profilsDemandables: [] }

type Membre = { id: string; nomAffiche: string; fonction: string; unite: string | null; responsableId: string | null; actif: boolean }

export type MagasinPouvoirs = {
  matrice: PouvoirsProfil[]
  demandes: Demande[]
  unites: Unite[]
  organisation: Membre[]
  personne(id: string): PersonneActivite
  profilsDe(id: string): string[]
  definirProfils(id: string, profils: string[]): void
  pouvoirsDe(id: string): MesPouvoirs
  /** La portée de l'action du chef couvre-t-elle ce compte ? */
  couvre(chef: string, action: ActionPouvoir, cible: string): boolean
  /** La portée de l'action du chef couvre-t-elle cette unité (UNITES et unité dirigée) ? */
  couvreUnite(chef: string, action: ActionPouvoir, code: string): boolean
  /** Unité et ses sous-unités. */
  sousUnites(code: string): string[]
  /** Unités dirigées par un compte, sous-unités comprises. */
  unitesDirigees(id: string): string[]
  /** Vérifie et enregistre une demande ; refus du contrat (§4). */
  deposer(refus: Refus, demandeur: string, d: NouvelleDemande): Demande
  /** Ligne de la matrice, refus IAM_PROFILE_NOT_FOUND pour un profil inconnu. */
  modifierMatrice(refus: Refus, profil: string, e: EntreePouvoirs): PouvoirsProfil
  ajouterProfil(code: string, libelle?: string): void
  retirerProfil(code: string): void
  /** Libellé d'un profil (D-86) : repris par la ligne de la matrice et par libellesProfils de GET /pouvoirs/moi. */
  renommerProfil(code: string, libelle: string): void
}

const il = (jours: number, heure = '09:00') => {
  const d = new Date(Date.now() - jours * 86_400_000)
  return `${d.toISOString().slice(0, 10)}T${heure}:00+01:00`
}

/** Magasin de démonstration : matrice par défaut, organisation, responsables d'unité et quelques demandes. */
export function magasinPouvoirs(): MagasinPouvoirs {
  const organisation: Membre[] = ORGANISATION_DEMO.map((m) => ({ ...m, actif: true }))
  const unites: Unite[] = UNITES_DEMO.map((u) => ({ ...u, responsableId: RESPONSABLES_UNITES_DEMO[u.code] ?? null }))
  const profils = new Map<string, string[]>()
  const matrice = PROFILS_DOMAINE.map((p) => pouvoirsParDefaut(p))
  const personne = (id: string): PersonneActivite => ({ id, nomAffiche: organisation.find((m) => m.id === id)?.nomAffiche ?? ANNUAIRE.find((p) => p.id === id)?.nom ?? id })
  const profilsDe = (id: string) => profils.get(id) ?? ANNUAIRE.find((p) => p.id === id)?.profils ?? []
  const libelleDe = (code: string) => matrice.find((l) => l.profil === code)?.libelle ?? PRESENTATIONS_PROFILS[code]?.libelle ?? code
  const sousUnites = (code: string): string[] => [code, ...unites.filter((u) => u.parent === code).flatMap((u) => sousUnites(u.code))]
  const unitesDirigees = (id: string) => [...new Set(unites.filter((u) => u.responsableId === id).flatMap((u) => sousUnites(u.code)))]
  const actionsDe = (id: string): ActionsPouvoir => {
    const lignes = matrice.filter((l) => profilsDe(id).includes(l.profil))
    return Object.fromEntries(ACTIONS_POUVOIR.map((a) => [a, PORTEES[Math.max(0, ...lignes.map((l) => rangPortee(l.actions[a])))] as Portee])) as ActionsPouvoir
  }
  /** Chaîne des responsables (niveau ≥ 1) puis membres des unités dirigées hors chaîne (niveau 0). */
  const perimetre = (id: string, portee: Portee): Subordonne[] => {
    const r = rangPortee(portee)
    const l: Subordonne[] = []
    const vers = (m: Membre, niveau: number): Subordonne => ({ id: m.id, nomAffiche: m.nomAffiche, fonction: m.fonction || null, unite: m.unite, responsableId: m.responsableId, niveau, actif: m.actif })
    const parcourir = (chef: string, niveau: number) => organisation.filter((m) => m.responsableId === chef).forEach((m) => {
      if (l.some((x) => x.id === m.id) || m.id === id || niveau > 10) return
      l.push(vers(m, niveau))
      if (r >= rangPortee('EQUIPE')) parcourir(m.id, niveau + 1)
    })
    if (r >= rangPortee('DIRECTS')) parcourir(id, 1)
    if (r >= rangPortee('UNITES')) {
      const dirigees = unitesDirigees(id)
      organisation.filter((m) => m.unite && dirigees.includes(m.unite) && m.id !== id && !l.some((x) => x.id === m.id)).forEach((m) => l.push(vers(m, 0)))
    }
    return l
  }
  const pouvoirsDe = (id: string): MesPouvoirs => {
    const actions = actionsDe(id)
    const large = PORTEES[Math.max(...ACTIONS_POUVOIR.map((a) => rangPortee(actions[a])))]
    const lignes = matrice.filter((l) => profilsDe(id).includes(l.profil))
    const demandables = [...new Set(lignes.flatMap((l) => l.profilsDemandables))].filter((p) => !PROFILS_PRIVILEGIES.includes(p)).sort()
    return {
      actions,
      profilsDemandables: demandables,
      subordonnes: perimetre(id, large),
      unitesDirigees: rangPortee(large) > 0 ? unitesDirigees(id) : [],
      libellesProfils: Object.fromEntries(demandables.map((p) => [p, libelleDe(p)])),
    }
  }
  const couvreCompte = (chef: string, action: ActionPouvoir, cible: string) => perimetre(chef, actionsDe(chef)[action]).some((s) => s.id === cible)
  const couvreUnite = (chef: string, action: ActionPouvoir, code: string) => actionsDe(chef)[action] === 'UNITES' && unitesDirigees(chef).includes(code)

  const demandes: Demande[] = [
    {
      id: 'DEM-0001', type: 'CREATION_COMPTE', demandeur: personne('U-001'), cible: null,
      contenu: { identifiant: 'i.fouda', prenom: 'Ingrid', nom: 'Fouda Ngono', email: 'i.fouda@gimac-afr.org', telephone: '+237 699 12 34 56', fonction: 'Technicienne réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', profils: ['ROLE_POLE_RESEAU'] },
      motif: 'Renfort du pôle réseau pour les liaisons ouvertes en 2026.', etat: 'EN_ATTENTE', decision: null, creeeLe: il(2),
    },
    {
      id: 'DEM-0002', type: 'PROFILS', demandeur: personne('u-resp-info'), cible: personne('u-collab'), contenu: { ajouter: ['ROLE_POLE_RESEAU'], retirer: [] },
      motif: 'Nadège assure désormais les dépôts de preuves du pôle réseau.', etat: 'EN_ATTENTE', decision: null, creeeLe: il(1, '14:20'),
    },
    {
      id: 'DEM-0003', type: 'STATUT', demandeur: personne('U-001'), cible: personne('u-prod'), contenu: { actif: false },
      motif: 'Départ annoncé à la fin du mois.', etat: 'REFUSEE',
      decision: { par: { id: 'u-super-admin', nomAffiche: 'Serge Mouelle Kombi' }, motif: 'Le départ n’est pas encore confirmé par les ressources humaines.', le: il(3, '16:05') }, creeeLe: il(5),
    },
    {
      id: 'DEM-0004', type: 'PROFILS', demandeur: personne('U-020'), cible: personne('sim-charge'), contenu: { ajouter: ['ROLE_CONDUCTEUR_PROJET'], retirer: [] },
      motif: 'Conduite du projet de la Banque de Libreville.', etat: 'ACCEPTEE',
      decision: { par: { id: 'u-super-admin', nomAffiche: 'Serge Mouelle Kombi' }, motif: null, le: il(6, '10:40') }, creeeLe: il(7),
    },
  ]
  let numero = demandes.length

  const deposer = (refus: Refus, demandeur: string, d: NouvelleDemande): Demande => {
    const motif = (d.motif ?? '').trim()
    if (motif.length < MOTIF_DEMANDE.min || motif.length > MOTIF_DEMANDE.max) throw refus(400, 'ORG_DEMANDE_INVALIDE', 'Motif obligatoire, 5 à 500 caractères.')
    const p = pouvoirsDe(demandeur)
    const hors = () => refus(403, 'ORG_HORS_PERIMETRE', 'Hors de votre périmètre selon la matrice des pouvoirs.')
    const nonDemandable = (l: string[]) => {
      if (l.some((x) => !p.profilsDemandables.includes(x))) throw refus(422, 'ORG_PROFIL_NON_DEMANDABLE', 'Ce profil ne peut pas être demandé.')
    }
    let cible: PersonneActivite | null = null
    let cle: string
    if (d.type === 'CREATION_COMPTE') {
      const c = d.contenu
      if (![c.identifiant, c.prenom, c.nom, c.email, c.fonction, c.unite, c.responsableId].every((x) => x?.trim()) || !c.profils.length) {
        throw refus(400, 'ORG_DEMANDE_INVALIDE', 'Identité, fonction, unité, responsable et au moins un profil sont obligatoires.')
      }
      if (p.actions.COMPTE_DEMANDER === 'AUCUNE' || !p.unitesDirigees.includes(c.unite)) throw hors()
      if (c.responsableId !== demandeur && !couvreCompte(demandeur, 'COMPTE_DEMANDER', c.responsableId)) throw hors()
      nonDemandable(c.profils)
      cle = `CREATION_COMPTE:${c.identifiant.trim().toLowerCase()}`
    } else {
      if (!couvreCompte(demandeur, d.type === 'PROFILS' ? 'PROFIL_DEMANDER' : 'STATUT_DEMANDER', d.cibleId)) throw hors()
      if (d.type === 'PROFILS') {
        if (!d.contenu.ajouter.length && !d.contenu.retirer.length) throw refus(400, 'ORG_DEMANDE_INVALIDE', 'Indiquez au moins un profil à ajouter ou à retirer.')
        nonDemandable([...d.contenu.ajouter, ...d.contenu.retirer])
      }
      cible = personne(d.cibleId)
      cle = `${d.type}:${d.cibleId}`
    }
    const cleDe = (x: Demande) => (x.type === 'CREATION_COMPTE' ? `CREATION_COMPTE:${(x.contenu as { identifiant: string }).identifiant.toLowerCase()}` : `${x.type}:${x.cible?.id}`)
    if (demandes.some((x) => x.etat === 'EN_ATTENTE' && cleDe(x) === cle)) throw refus(409, 'ORG_DEMANDE_EXISTANTE', 'Une demande de ce type est déjà en attente pour cette personne.')
    const n: Demande = {
      id: `DEM-${String(++numero).padStart(4, '0')}`, type: d.type, demandeur: personne(demandeur), cible, contenu: structuredClone(d.contenu), motif, etat: 'EN_ATTENTE',
      decision: null, creeeLe: new Date().toISOString(),
    }
    demandes.unshift(n)
    return n
  }

  return {
    matrice, demandes, unites, organisation, personne, profilsDe,
    definirProfils: (id, l) => void profils.set(id, [...l]),
    pouvoirsDe,
    couvre: couvreCompte,
    couvreUnite,
    sousUnites,
    unitesDirigees,
    deposer,
    modifierMatrice: (refus, profil, e) => {
      const l = matrice.find((x) => x.profil === profil)
      if (!l) throw refus(404, 'IAM_PROFILE_NOT_FOUND', 'Profil inconnu.')
      if (ACTIONS_POUVOIR.some((a) => !PORTEES.includes(e.actions?.[a]))) throw refus(400, 'IAM_POUVOIRS_INVALIDES', 'Une portée est attendue pour chaque action.')
      if (e.profilsDemandables.some((x) => PROFILS_PRIVILEGIES.includes(x))) throw refus(422, 'IAM_PROFIL_PRIVILEGIE_NON_DEMANDABLE', 'Un profil privilégié n’est jamais demandable.')
      Object.assign(l, { actions: { ...e.actions }, profilsDemandables: [...new Set(e.profilsDemandables)].sort() })
      return l
    },
    ajouterProfil: (code, libelle) => {
      if (!matrice.some((x) => x.profil === code)) matrice.push(pouvoirsParDefaut(code, libelle))
    },
    renommerProfil: (code, libelle) => {
      const l = matrice.find((x) => x.profil === code)
      if (l) l.libelle = libelle
    },
    retirerProfil: (code) => {
      const i = matrice.findIndex((x) => x.profil === code)
      if (i >= 0) matrice.splice(i, 1)
    },
  }
}

/** Port de l'application interne : mes pouvoirs, mes demandes, rattachement d'un subordonné. */
export function pouvoirsSimules(attendre: Attendre, refus: Refus, magasin: MagasinPouvoirs, moi: string): ServicePouvoirs {
  const sienne = (id: string) => {
    const d = magasin.demandes.find((x) => x.id === id)
    if (!d || d.demandeur.id !== moi) throw refus(404, 'ORG_DEMANDE_INTROUVABLE', 'Demande introuvable.')
    return d
  }
  return {
    moi: () => attendre(() => magasin.pouvoirsDe(moi)),
    demander: (d) => attendre(() => magasin.deposer(refus, moi, d)),
    demandes: (q) => attendre(() => {
      const page = q.page ?? 0
      const taille = q.taille ?? 20
      const l = magasin.demandes.filter((d) => d.demandeur.id === moi && (!q.etat || d.etat === q.etat))
      return { contenu: l.slice(page * taille, (page + 1) * taille), total: l.length, page, taille }
    }),
    demande: (id) => attendre(() => sienne(id)),
    annuler: (id) => attendre(() => {
      const d = sienne(id)
      if (d.etat !== 'EN_ATTENTE') throw refus(409, 'ORG_DEMANDE_TRAITEE', 'Cette demande n’est plus en attente.')
      d.etat = 'ANNULEE'
      return d
    }),
    rattacher: (id, r) => attendre(() => {
      if (!magasin.couvre(moi, 'RATTACHEMENT_MODIFIER', id)) throw refus(403, 'ORG_HORS_PERIMETRE', 'Hors de votre périmètre selon la matrice des pouvoirs.')
      if (r.unite && !magasin.unitesDirigees(moi).includes(r.unite)) throw refus(403, 'ORG_HORS_PERIMETRE', 'Unité hors de vos unités dirigées.')
      if (r.responsableId && r.responsableId !== moi && !magasin.pouvoirsDe(moi).subordonnes.some((s) => s.id === r.responsableId && s.niveau >= 1)) {
        throw refus(403, 'ORG_HORS_PERIMETRE', 'Responsable hors de votre équipe.')
      }
      if (r.responsableId === id) throw refus(422, 'ORG_RESPONSABLE_INVALIDE', 'Un compte n’est pas son propre responsable.')
      const m = magasin.organisation.find((x) => x.id === id)!
      // Comme MS-IAM : les deux valeurs sont reçues, une valeur nulle ou vide retire l'unité ou le n+1.
      Object.assign(m, { unite: r.unite || null, responsableId: r.responsableId || null })
      return { id, unite: m.unite, responsableId: m.responsableId }
    }),
  }
}
