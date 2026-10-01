import { connexionSimulee, type CompteConnexionDemo, type ServiceConnexion, type Session } from '@gimac/socle'
import { PROFILS_DEMO } from './navigation'

// Comptes de démonstration de la connexion simulée de la console (D-65) : super administrateur et RSSI,
// un compte au premier accès, un compte désactivé et un correspondant (refusé sur cette couche). Mot de
// passe : MOT_DE_PASSE_DEMO ; tout code de six chiffres est accepté (socle, connexion/simule.ts).
const session = (role: string, identifiant = PROFILS_DEMO[role].identifiant): Session => {
  const p = PROFILS_DEMO[role]
  return { sujet: p.sujet, nom: p.nom, identifiant, roles: [role], permissions: p.permissions, institutionId: '', acr: '2', doubleFacteur: true }
}

export const COMPTES_DEMO: CompteConnexionDemo[] = [
  ...Object.keys(PROFILS_DEMO).map((role): CompteConnexionDemo => ({ identifiant: PROFILS_DEMO[role].identifiant, couches: ['des', 'console'], role, session: session(role) })),
  { identifiant: 'nouvel.admin', couches: ['des', 'console'], etat: 'PREMIER_ACCES', role: 'ROLE_RSSI', session: session('ROLE_RSSI', 'nouvel.admin') },
  { identifiant: 'compte.desactive', couches: ['des', 'console'], etat: 'INACTIF', role: 'ROLE_RSSI', session: session('ROLE_RSSI', 'compte.desactive') },
  { identifiant: 'correspondant-a', couches: ['portail'], role: 'ROLE_RSSI', session: session('ROLE_RSSI', 'correspondant-a') },
]

export const connexionDemo = (): ServiceConnexion => connexionSimulee({ comptes: COMPTES_DEMO })
