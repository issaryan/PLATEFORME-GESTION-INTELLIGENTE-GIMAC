# language: fr
Fonctionnalité: Premier accès et « Mon compte » (D-62)
  À sa première connexion, le titulaire d'un compte créé par l'administration complète sa fiche,
  accepte la charte et configure ses facteurs d'authentification depuis la plateforme, sans passer
  par la console du fournisseur d'identité.

  Contexte:
    Étant donné un compte créé par le super administrateur avec le profil "ROLE_CHARGE_DDRM"

  Scénario: Le titulaire termine la configuration de son compte
    Quand le titulaire consulte son compte
    Alors les étapes restantes comprennent "FICHE"
    Quand il renseigne le téléphone "+237677000000", la langue "fr" et accepte la charte
    Alors la réponse a le statut 200
    Et sa fiche est complète

  Scénario: La charte doit être acceptée
    Quand il renseigne le téléphone "+237677000000", la langue "fr" sans accepter la charte
    Alors la réponse a le statut 400
    Et le code d'erreur est "IAM_CHARTER_REQUIRED"

  Scénario: Le titulaire enrôle une clé de sécurité puis se réauthentifie
    Quand le titulaire enrôle une clé de sécurité nommée "Clé du bureau"
    Alors la réponse a le statut 200
    Et son compte compte 1 clé de sécurité
    Et une assertion pour la finalité "STATUS_APPROVE" est reconnue valide par la route interne

  Scénario: Le titulaire demande à changer son mot de passe
    Quand le titulaire demande l'action "MOT_DE_PASSE"
    Alors la réponse a le statut 200
    Et le compte doit accomplir les actions "MOT_DE_PASSE"

  Scénario: D-65 — le titulaire fixe son mot de passe par le lien de l'invitation, sans page Keycloak
    Quand le titulaire suit le lien de son invitation avec le mot de passe "Premier-Mot-2026!"
    Alors la réponse a le statut 204
    Et la passerelle lit pour le compte les actions "SECOND_FACTEUR,FICHE,CHARTE"
    Quand le titulaire suit à nouveau le même lien avec le mot de passe "Autre-Mot-2026!"
    Alors la réponse a le statut 400
    Et le code d'erreur est "CNX_JETON_INVALIDE"

  Scénario: D-65 — le titulaire configure son code TOTP dans l'assistant de premier accès
    Quand le titulaire, en session restreinte, configure son code TOTP avec un code juste
    Alors la réponse a le statut 204
    Et la passerelle lit pour le compte un second facteur configuré

  Scénario: D-65 — la session restreinte n'ouvre aucune autre route
    Quand le titulaire, en session restreinte, consulte ses sessions
    Alors la réponse a le statut 403
