# language: fr
Fonctionnalité: Administration des comptes par le super administrateur et le RSSI (D-62)
  Aucun compte n'est créé par inscription libre : le super administrateur ou le RSSI le crée depuis
  la console, attribue les profils et exige, au besoin, la mise à jour du mot de passe ou du second
  facteur. Les actes sensibles exigent une réauthentification forte FIDO2 (IAM-07).

  Scénario: Le super administrateur crée un compte et envoie l'invitation
    Étant donné le "super administrateur" authentifié à deux facteurs
    Quand il crée le compte "agent.recette" avec le profil "ROLE_CHARGE_DDRM"
    Alors la réponse a le statut 201
    Et le compte attend son premier accès
    Et le journal d'administration du compte contient l'acte "CREATION"

  Scénario: Le RSSI exige la mise à jour du mot de passe et la configuration du second facteur
    Étant donné le "RSSI" authentifié à deux facteurs
    Et un compte existant avec le profil "ROLE_SNA"
    Quand il exige la mise à jour du mot de passe et la configuration du second facteur
    Alors la réponse a le statut 200
    Et le compte doit accomplir les actions "MOT_DE_PASSE,SECOND_FACTEUR"

  Scénario: Le RSSI ne peut attribuer un profil privilégié
    Étant donné le "RSSI" authentifié à deux facteurs
    Et un compte existant avec le profil "ROLE_SNA"
    Quand il attribue le profil "ROLE_SUPER_ADMIN" avec une réauthentification forte
    Alors la réponse a le statut 403
    Et le code d'erreur est "IAM_PRIVILEGED_PROFILE"

  Scénario: La suppression d'un compte exige la réauthentification forte
    Étant donné le "super administrateur" authentifié à deux facteurs
    Et un compte existant avec le profil "ROLE_SNA"
    Quand il supprime le compte sans réauthentification
    Alors la réponse a le statut 428
    Quand il supprime le compte avec une réauthentification forte
    Alors la réponse a le statut 204
    Et le journal d'administration du compte contient l'acte "SUPPRESSION"

  Scénario: Nul ne désactive son propre compte
    Étant donné le "RSSI" authentifié à deux facteurs
    Quand il désactive son propre compte
    Alors la réponse a le statut 403
    Et le code d'erreur est "IAM_SELF_ADMINISTRATION"

  Scénario: Un agent sans permission d'administration n'accède pas aux comptes
    Étant donné le "chargé DDRM" authentifié à deux facteurs
    Quand il consulte la liste des comptes
    Alors la réponse a le statut 403
