import type { ReactNode } from 'react'
import type { Couche } from './http/prefixes'
import type { Session } from './session/session'
import type { SessionDemo } from './session/demo'
import { adresseApplication, adresseConnexionApplication, applicationApresRefus, applicationDe } from './session/aiguillage'

// Habillage d'un produit hébergé sur le socle (D-118) : la Sandbox GIMAC garde la connexion, la session, le premier
// accès et l'enveloppe de la plateforme, avec son décor, son logo, son nom, son pied et sa règle d'aiguillage. Tout
// est facultatif : sans habillage, le rendu et le comportement de la plateforme restent exactement les mêmes.

/** Logo sur fond clair et sur fond sombre (thème sombre, navigation latérale). */
export type LogoHabillage = { clair: string; sombre: string; alt?: string }

export type HabillageConnexion = {
  /** Fond de toute la page, à la place des cernes du bois. */
  fond?: ReactNode
  /** Marque à côté de la carte d'accès, à la place du nom Moabi ; null la retire et centre la carte. */
  marque?: ReactNode | null
  logo?: LogoHabillage
  /** Titre de l'onglet ; NOM_PLATEFORME à défaut. */
  nom?: string
  /** Mention du pied (couche et classification) ; chaîne vide : seule l'heure reste. */
  mention?: string
}

export type HabillageEnveloppe = {
  logo?: LogoHabillage
  /** Bandeau sous la barre supérieure (mention du produit, mode d'exécution). */
  bandeau?: ReactNode
  /** Pied de page : désignation du produit et classification (voir Footer). */
  pied?: { plateforme?: string; classification?: string }
}

/** Règle d'aiguillage entre applications (D-79) ; celle de la plateforme à défaut. */
export type ReglesAiguillage = {
  /** Application où le compte a sa place. */
  applicationDe: (s: Pick<Session, 'institutionId' | 'permissions' | 'restreinte'>, couche: Couche) => Couche
  /** Application à proposer après un refus de couche (CNX_COUCHE_REFUSEE). */
  apresRefus: (couche: Couche) => Couche
  /** Tableau de bord d'une application, la session de démonstration jointe. */
  adresse: (couche: Couche, demo: SessionDemo | null) => string
  /** Écran de connexion d'une application, identifiant prérempli. */
  adresseConnexion: (couche: Couche, o: { identifiant?: string; depuis: Couche }) => string
}

export const AIGUILLAGE_PLATEFORME: ReglesAiguillage = {
  applicationDe,
  apresRefus: applicationApresRefus,
  adresse: (couche, demo) => adresseApplication(couche, undefined, demo),
  adresseConnexion: adresseConnexionApplication,
}
