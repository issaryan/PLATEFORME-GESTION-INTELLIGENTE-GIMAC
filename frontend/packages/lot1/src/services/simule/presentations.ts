// Présentation des profils livrés (D-86, docs/lot1/profils-organisation.md §2) : libellé (attribut gimac_libelle du
// rôle) et mission (description), reprises de infra/keycloak/generer-realm-gimac.py pour les services simulés. Les
// services réels les servent par GET /configuration/profils, la matrice et GET /pouvoirs/moi.

export type PresentationProfil = { libelle: string; description: string }

export const PRESENTATIONS_PROFILS: Record<string, PresentationProfil> = {
  ROLE_INGENIEUR_HOMOLOGATION: { libelle: 'Ingénieur d’homologation', description: 'Conduit les opérations d’homologation des liaisons (référentiel, preuves, validation) et prépare la certification.' },
  ROLE_CHEF_DES: { libelle: 'Chef du DES', description: 'Dirige le Département Exploitation et Supervision : ouvre les liaisons, désigne les conducteurs de projet et signe le quitus technique.' },
  ROLE_SUPER_ADMIN: { libelle: 'Super administrateur', description: 'Administre la plateforme, les comptes et les profils ; seul habilité à attribuer un profil privilégié.' },
  ROLE_RSSI: { libelle: 'Sécurité des SI', description: 'Responsable de la sécurité des systèmes d’information : vérifie la sécurité des liaisons, signe le quitus sécurité et administre les comptes.' },
  ROLE_CONFORMITE: { libelle: 'Conformité', description: 'Contrôle la conformité des dossiers de certification, rattache les changements et signe le quitus conformité.' },
  ROLE_DG: { libelle: 'Direction générale', description: 'Direction générale du GIMAC : signe le quitus final et approuve les changements de statut des attestations.' },
  ROLE_CORRESPONDANT: { libelle: 'Correspondant du participant', description: 'Représente une institution membre sur le portail : suit l’homologation de sa liaison, dépose les livrables et signe.' },
  ROLE_CHARGE_DDRM: { libelle: 'Chargé de la DDRM', description: 'Instruit les dossiers des institutions membres (adhésion, transitions) et propose les décisions au chef de la DDRM.' },
  ROLE_CHEF_DDRM: { libelle: 'Chef de la DDRM', description: 'Dirige la DDRM : prononce les décisions sur les institutions membres, ouvre les liaisons et désigne les conducteurs de projet.' },
  ROLE_SNA: { libelle: 'Normes et Agréments', description: 'Service Normes et Agréments : vérifie le respect des normes, dépose et valide les preuves d’homologation.' },
  ROLE_PRODUCTION_MONETIQUE: { libelle: 'Production monétique', description: 'Met en production les liaisons homologuées : preuves, rattachement des changements et rapport de mise en production.' },
  ROLE_POLE_RESEAU: { libelle: 'Pôle réseau', description: 'Réalise les opérations réseau des liaisons et en dépose les preuves d’homologation.' },
  ROLE_POLE_SYSTEME: { libelle: 'Pôle système', description: 'Réalise les opérations système des liaisons et en dépose les preuves d’homologation.' },
  ROLE_COLLABORATEUR: { libelle: 'Collaborateur', description: 'Employé du GIMAC sans fonction métier dans la plateforme : messagerie, Mon activité et notifications.' },
  ROLE_ADMIN_SYSTEME: { libelle: 'Administrateur système', description: 'Exploite l’infrastructure de la plateforme (supervision, opérations Docker et Kubernetes) sans administrer les comptes.' },
  ROLE_RESPONSABLE_SERVICE: { libelle: 'Responsable de service', description: 'Dirige un service, un pôle ou une cellule : confie et suit les tâches de son équipe, lit ses activités et vise ses rapports.' },
  ROLE_CONDUCTEUR_PROJET: { libelle: 'Conducteur de projet', description: 'Conduit le projet de liaison qui lui est confié : affecte les opérations, valide les preuves et établit le procès-verbal.' },
}
