import { connexionSimulee, type CompteConnexionDemo, type ServiceConnexion } from '@gimac/socle'
import { PROFILS_DEMO, sessionDemo } from './profils'

// Comptes de démonstration de la connexion simulée de l'application du département (D-65) : un compte
// par profil interne (identifiants de l'annuaire simulé), un compte au premier accès, un compte désactivé
// et un correspondant (refusé sur cette couche). Mot de passe : MOT_DE_PASSE_DEMO ; tout code de six
// chiffres est accepté (docs du socle, connexion/simule.ts).
export const COMPTES_DEMO: CompteConnexionDemo[] = [
  ...Object.entries(PROFILS_DEMO).map(([role, p]): CompteConnexionDemo => ({ identifiant: p.identifiant, couches: ['des', 'console'], role, session: sessionDemo(role) })),
  { identifiant: 'nouvel.agent', couches: ['des', 'console'], etat: 'PREMIER_ACCES', role: 'ROLE_CHARGE_DDRM', session: { ...sessionDemo('ROLE_CHARGE_DDRM'), identifiant: 'nouvel.agent' } },
  { identifiant: 'compte.desactive', couches: ['des', 'console'], etat: 'INACTIF', role: 'ROLE_SNA', session: sessionDemo('ROLE_SNA') },
  { identifiant: 'correspondant-a', couches: ['portail'], role: 'ROLE_CHEF_DDRM', session: sessionDemo('ROLE_CHEF_DDRM') },
]

export const connexionDemo = (): ServiceConnexion => connexionSimulee({ comptes: COMPTES_DEMO })
