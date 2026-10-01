import { connexionSimulee, type CompteConnexionDemo, type ServiceConnexion, type Session } from '@gimac/socle'
import { PROFILS } from '@gimac/lot1'

// Comptes de démonstration de la connexion simulée du portail des participants (D-65) : le correspondant
// de l'institution de démonstration, un correspondant au premier accès, un compte désactivé et un compte
// interne (refusé sur cette couche). Mot de passe : MOT_DE_PASSE_DEMO ; tout code de six chiffres est
// accepté (socle, connexion/simule.ts).
export function sessionCorrespondant(identifiant = 'correspondant-a'): Session {
  return { sujet: 'demo-correspondant', nom: 'Correspondant A', identifiant, roles: ['ROLE_CORRESPONDANT'], permissions: PROFILS.ROLE_CORRESPONDANT, institutionId: 'INST-0042', acr: '2', doubleFacteur: true }
}

export const COMPTES_DEMO: CompteConnexionDemo[] = [
  { identifiant: 'correspondant-a', couches: ['portail'], role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant() },
  { identifiant: 'nouveau.correspondant', couches: ['portail'], etat: 'PREMIER_ACCES', role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant('nouveau.correspondant') },
  { identifiant: 'compte.desactive', couches: ['portail'], etat: 'INACTIF', role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant('compte.desactive') },
  { identifiant: 'super.admin', couches: ['des', 'console'], role: 'ROLE_CORRESPONDANT', session: sessionCorrespondant('super.admin') },
]

export const connexionDemo = (): ServiceConnexion => connexionSimulee({ comptes: COMPTES_DEMO })
