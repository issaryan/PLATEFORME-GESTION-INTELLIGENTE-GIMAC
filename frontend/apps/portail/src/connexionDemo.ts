import { connexionSimulee, type CompteConnexionDemo, type ServiceConnexion, type Session } from '@gimac/socle'
import { PROFILS, PROFILS_INTERNES_DEMO } from '@gimac/lot1'

// Comptes de démonstration de la connexion simulée du portail des participants (D-65) : le correspondant
// de l'institution de démonstration, un correspondant au premier accès, un compte désactivé et un compte
// interne (refusé sur cette couche). Tout compte interne de démonstration (communs aux trois applications, D-79)
// est refusé ici (CNX_COUCHE_REFUSEE) et conduit vers l'écran de connexion de l'application interne. Mot de passe :
// MOT_DE_PASSE_DEMO ; tout code de six chiffres est accepté (socle, connexion/simule.ts).
export function sessionCorrespondant(identifiant = 'correspondant-a'): Session {
  return { sujet: 'demo-correspondant', nom: 'Correspondant A', identifiant, roles: ['ROLE_CORRESPONDANT'], permissions: PROFILS.ROLE_CORRESPONDANT, institutionId: 'INST-0042', acr: '2', doubleFacteur: true }
}

export const COMPTES_DEMO: CompteConnexionDemo[] = [
  { identifiant: 'correspondant-a', couches: ['portail'], role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant() },
  { identifiant: 'nouveau.correspondant', couches: ['portail'], etat: 'PREMIER_ACCES', role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant('nouveau.correspondant') },
  { identifiant: 'compte.desactive', couches: ['portail'], etat: 'INACTIF', role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant('compte.desactive') },
  ...Object.entries(PROFILS_INTERNES_DEMO).map(([role, p]): CompteConnexionDemo => ({
    identifiant: p.identifiant, couches: ['des', 'console'], role,
    session: { sujet: p.sujet, nom: p.nom, identifiant: p.identifiant, roles: [role], permissions: p.permissions, institutionId: '', acr: '2', doubleFacteur: true },
  })),
]

export const connexionDemo = (): ServiceConnexion => connexionSimulee({ comptes: COMPTES_DEMO })
