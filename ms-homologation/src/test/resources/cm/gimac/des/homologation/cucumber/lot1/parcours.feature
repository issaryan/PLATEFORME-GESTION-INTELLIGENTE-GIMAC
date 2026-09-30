# language: fr
Fonctionnalité: Parcours de la liaison, étapes 4 à 10 : du cadrage au constat de la couverture (D-56 à D-59)
  Le référentiel de l'annexe A du compte rendu de cadrage est instancié pour la liaison à son ouverture :
  tronc commun amont, branche du type de projet, tronc commun aval. Chaque acteur réalise les opérations
  de sa fonction ; le validateur n'est jamais le responsable ; une clé ne porte que son code de contrôle.
  Le constat de la couverture ouvre le procès-verbal de certification tenu par MS-CERTIFICATION, dont les
  événements font progresser la liaison jusqu'à l'approbation de la sécurité.

  Contexte:
    Étant donné une liaison "CARTE_INTERBANCAIRE" ouverte au cadrage pour l'adhérent "Banque Recette" qui a souscrit "RETRAIT" et "PAIEMENT"

  Scénario: Parcours nominal : cadrage, lancement, ateliers, intégration, essais, constat de la couverture
    Alors le parcours compte 43 opérations instanciées du référentiel de l'annexe A
    Et la liaison est à l'étape "CADRAGE" au statut "CADREE"
    Quand la DDRM dépose le livrable du rang "3"
    Et le chef du DES affecte l'ingénieur Normes et Agréments aux ressources du projet
    Alors l'opération de rang "6" est "FAITE"
    Quand les livrables sont déposés jusqu'au rang "12"
    Alors la liaison est à l'étape "INTEGRATION" au statut "EN_INTEGRATION"
    Quand le participant communique les coordonnées d'essai "essai.exemple" et "8443" sur le portail
    Et les opérations sont prouvées et validées jusqu'au rang "15"
    Et le participant dépose le document de paramétrage sur le portail
    Alors la liaison est à l'étape "TESTS" au statut "EN_TEST"
    Quand les essais sont prouvés et validés
    Et l'ingénieur Normes et Agréments constate la couverture
    Alors la couverture de certification constatée vaut 100 %
    Et l'événement "homologation.coverage-established.v1" est prêt pour MS-CERTIFICATION
    Et le conducteur de projet voit le procès-verbal à établir sur son tableau de bord

  Scénario: Le validateur n'est jamais le responsable de l'opération
    Quand la DDRM dépose la preuve du rang "3"
    Et l'ingénieur Normes et Agréments est désigné responsable du rang "3"
    Alors sa validation du rang "3" est refusée avec le statut 409 et le code "HOM_VALIDATOR_IS_RESPONSIBLE"
    Et la validation du rang "3" sans réauthentification est refusée avec le statut 428 et le code "IAM_REAUTH_REQUIRED"

  Scénario: Blocage daté puis levée qui rouvre les opérations
    Quand le conducteur de projet déclare un blocage "Pièce du participant manquante" avec la prochaine action "Relancer le participant"
    Alors l'opération de rang "3" est "BLOQUEE"
    Et la liaison figure parmi les projets bloqués du pilotage
    Quand le conducteur de projet lève le blocage
    Alors l'opération de rang "3" est "A_FAIRE"

  Scénario: Verrous tenus par MS-CERTIFICATION : liaison certifiée puis approuvée par la sécurité
    Étant donné une liaison dont la couverture a été constatée
    Quand MS-CERTIFICATION publie le procès-verbal établi puis signé des deux parties
    Alors la liaison est à l'étape "APPROBATION_SECURITE" au statut "CERTIFIEE"
    Quand MS-CERTIFICATION publie le dossier de changement, l'archivage des attestations et l'approbation de la sécurité
    Alors la liaison est à l'étape "APPROBATION_SECURITE" au statut "APPROUVEE_SECURITE"

  Plan du scénario: Clés et valeurs de référence : aucune valeur de clé, une valeur relevée
    Étant donné une liaison approuvée par la sécurité
    Quand une preuve est déposée pour le rang "<rang>" avec <complement>
    Alors le dépôt est refusé avec le statut 422 et le code "<code>"

    Exemples:
      | rang | complement                             | code                          |
      | C1   | aucune cérémonie                       | HOM_KEY_CEREMONY_INCOMPLETE   |
      | C1   | une valeur de clé en clair             | HOM_EVIDENCE_FIELD_REFUSED    |

  Scénario: Portail : le participant ne voit que les liaisons de son institution, sans nom interne
    Alors le correspondant de l'adhérent voit sa liaison et son dossier sans nom interne
    Et le correspondant d'une autre institution ne trouve pas la liaison
