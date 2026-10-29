import type { ChampFormulaire, Intervenant, SaisieProcessus } from '../../modele/processus'

const pos = (x: number, y: number) => ({ x, y })

/** Modèle simulé de la fiche RFC, identique au provisionnement MS-PILOTAGE. */
export function definitionChangementDemo(): SaisieProcessus {
  const demandeur: Intervenant = { mode: 'LANCEUR' }
  const responsable: Intervenant = { mode: 'AU_LANCEMENT', unite: 'SSI', remplacement: 'ROLE_PRODUCTION_MONETIQUE' }
  const ssi: Intervenant = { mode: 'PROFIL', profil: 'ROLE_AGENT_SSI', unite: 'SSI' }
  const champ = (code: string, libelle: string, type: 'TEXTE' | 'TEXTE_LONG' | 'LISTE', choix: string[] = []): ChampFormulaire => ({ code, libelle, type, choix, obligatoire: true, aide: null, typesPieces: [] })
  return {
    code: 'DEMANDE_CHANGEMENT', unite: 'SSI', libelle: 'Demande de changement', description: 'Fiche de demande de changement GIMAC.', categorie: 'Exploitation', delaiCibleJours: 10,
    referenceModele: 'CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}',
    initiateurs: { regles: ['TOUT_LE_GIMAC'], unites: [], profils: [], permissions: [], personnes: [], initiateurIntervenant: true },
    formulaire: { champs: [
      champ('TYPE_DEMANDEUR', 'Unité du demandeur', 'LISTE', ['SUPPORT_MONETIQUE', 'AUTRE_DES']), champ('ELEMENT_CONFIGURATION', 'Élément de configuration (AMA, BD…)', 'TEXTE'),
      champ('IDENTIFIANT_EC', 'Identifiant EC', 'TEXTE'), champ('PRIORITE', 'Priorité', 'LISTE', ['NORMAL', 'URGENT']),
      champ('DECLENCHEUR', 'Déclencheur', 'LISTE', ['CLIENT', 'ERREUR_CONNUE', 'PROBLEME', 'PROJET']), champ('DEMANDEUR_CONTACT', 'Contact du demandeur', 'TEXTE'),
      champ('RAISON', 'Raison détaillée du changement', 'TEXTE_LONG'), champ('ELEMENT_IMPACTE', 'EC impactés', 'TEXTE_LONG'),
      champ('DESCRIPTION_IMPACT', 'Description de l’impact', 'TEXTE_LONG'), champ('RESPONSABLE_CONTACT', 'Contact du responsable de mise en œuvre', 'TEXTE'),
      champ('PLAN_TEST', 'Description du plan de test', 'TEXTE_LONG'), champ('PLAN_RETOUR', 'Description du plan de retour', 'TEXTE_LONG'),
    ] },
    etapes: [
      { code: 'IDENTIFICATION', libelle: '1. Identification du changement', action: 'Vérifier l’identification.', intervenant: demandeur, validateur: null, predecesseurs: [], delaiJours: 1, livrables: [], signatures: [], champs: [], position: pos(0, 0) },
      { code: 'RAISON', libelle: '2. Raison du changement', action: 'Vérifier la raison.', intervenant: demandeur, validateur: null, predecesseurs: ['IDENTIFICATION'], delaiJours: 1, livrables: [], signatures: [], champs: [], position: pos(220, 0) },
      { code: 'IMPACT', libelle: '3. Impact du changement', action: 'Vérifier l’impact.', intervenant: demandeur, validateur: null, predecesseurs: ['RAISON'], delaiJours: 1, livrables: [], signatures: [], champs: [], position: pos(440, 0) },
      { code: 'IMPLEMENTATION', libelle: '4. Détail d’implémentation', action: 'Confirmer les plans et déposer le dossier PDF.', intervenant: responsable, validateur: null, predecesseurs: ['IMPACT'], delaiJours: 2,
        livrables: [{ code: 'DOSSIER_CHANGEMENT', libelle: 'Dossier de changement PDF', typesPieces: ['application/pdf'], obligatoire: true, modele: null }], signatures: [], champs: [], position: pos(660, 0) },
      { code: 'SSI', libelle: '5. Décision de la Sécurité du Système d’Information', action: 'Planifier, évaluer et signer la décision unique.', intervenant: ssi, validateur: null, predecesseurs: ['IMPLEMENTATION'], delaiJours: 2,
        livrables: [], signatures: [{ code: 'DECISION_SSI', libelle: 'Décision SSI', signataire: ssi, porte: ['DOSSIER_CHANGEMENT'], ordre: 1 }],
        champs: [
          { code: 'DATE_DEPLOIEMENT', libelle: 'Date de déploiement', type: 'DATE', choix: [], obligatoire: true },
          { code: 'DATE_REVUE', libelle: 'Date de revue', type: 'DATE', choix: [], obligatoire: true },
          { code: 'EVALUATION', libelle: 'Évaluation du changement', type: 'TEXTE', choix: [], obligatoire: true },
          { code: 'DECISION', libelle: 'Décision', type: 'CHOIX', choix: ['ACCEPTEE', 'REFUSEE'], obligatoire: true },
          { code: 'DESCRIPTION', libelle: 'Description de la décision', type: 'TEXTE', choix: [], obligatoire: true },
        ], position: pos(880, 0) },
    ],
  }
}
