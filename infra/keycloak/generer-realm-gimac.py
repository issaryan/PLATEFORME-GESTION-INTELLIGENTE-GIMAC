#!/usr/bin/env python3
"""Domaine Keycloak « gimac » du lot 1 (PI-GIMAC-01, convention transversale et §0.6, §0.7).

Dérivé du realm de développement généré par JHipster (infra/docker-compose/realm-config),
il y ajoute les permissions du lot, un rôle composite par profil, les utilisateurs de test,
le claim institution_id, le client tester-cli et les clients de service. Aucun secret n'est
écrit : mots de passe et secrets sont des variables ${...} résolues par Keycloak à l'import,
à partir du .env local (BNF-07, D-44).

    python3 infra/keycloak/generer-realm-gimac.py
"""
import copy
import json
import pathlib
import uuid

RACINE = pathlib.Path(__file__).resolve().parents[2]
SOURCE = RACINE / "infra/docker-compose/realm-config/jhipster-realm.json"
CIBLE = RACINE / "infra/keycloak/gimac-realm.json"

PERMISSIONS = {
    "HOM": ["PROJECT_DESIGNATE", "REFERENTIEL_READ", "REFERENTIEL_EDIT", "REFERENTIEL_PUBLISH", "REFERENTIEL_MIGRATE", "LIAISON_READ", "LIAISON_LIST",
            "LIAISON_OPEN", "LIAISON_UNFREEZE", "OPERATION_ASSIGN", "EVIDENCE_SUBMIT", "OPERATION_VALIDATE", "OPERATION_REOPEN",
            "GRID_EDIT", "REPLAY_ADMIN", "SCENARIO_QUALIFY", "DASHBOARD_READ", "DEADLINE_CONFIG", "EVIDENCE_FILE_EXPORT",
            "PORTAL_READ", "PORTAL_SUBMIT",
            # Parcours de la liaison, étapes 4 à 10 (D-56 à D-59) : conduite des étapes.
            "STEP_MANAGE"],
    "CER": ["CIRCUIT_READ", "QUITUS_TECHNIQUE_SIGN", "QUITUS_SECURITE_SIGN", "QUITUS_CONFORMITE_SIGN", "QUITUS_DG_SIGN",
            "DOSSIER_READ", "DOSSIER_VERIFY", "CHANGE_RECORD_LINK", "GOLIVE_REPORT_SUBMIT", "ATTESTATION_ISSUE", "REGISTER_READ",
            "REGISTER_EXPORT", "ATTESTATION_STATUS_REQUEST", "ATTESTATION_STATUS_APPROVE", "PORTAL_READ",
            # Verrous de la décision (D-56) : procès-verbal établi par le conducteur, vérification de la sécurité, signature du participant.
            "PV_ESTABLISH", "SECURITY_VERIFY", "PORTAL_SIGN"],
    # Registre des institutions et étapes 1 et 2 du parcours (D-57).
    "PAR": ["INSTITUTION_READ", "INSTITUTION_CREATE", "INSTITUTION_EDIT", "ADHESION_INSTRUCT", "TRANSITION_REQUEST",
            "TRANSITION_APPROVE"],
    # Administration des comptes et de la plateforme depuis la console (D-62).
    "IAM": ["ACCOUNT_READ", "ACCOUNT_CREATE", "ACCOUNT_EDIT", "ACCOUNT_ROLES", "ACCOUNT_STATUS", "ACCOUNT_DELETE",
            "ACCOUNT_SECURITY", "JOURNAL_READ", "PROFILE_EDIT", "ROLE_PRIVILEGED_GRANT"],
    "PLATFORM": ["SUPERVISION_READ", "CONFIG_READ", "CONFIG_EDIT", "SECURITY_POLICY_READ", "SECURITY_POLICY_EDIT",
                 "MESSAGING_EDIT"],
    # Messagerie instantanée de travail entre acteurs internes (D-64, docs/lot1/messagerie.md).
    "COL": ["MESSAGE_READ", "MESSAGE_SEND"],
    "INTERNAL": ["HOMOLOGATION_READ", "CERTIFICATION_READ", "PARTICIPANT_READ", "DOCUMENT_READ", "DOCUMENT_DEPOSIT", "IAM_READ"],
}
TOUTES = [f"{d}_{p}" for d, ps in PERMISSIONS.items() for p in ps]

LECTURE_DES = ["HOM_REFERENTIEL_READ", "HOM_LIAISON_READ", "HOM_LIAISON_LIST", "HOM_DASHBOARD_READ", "CER_CIRCUIT_READ",
               "CER_DOSSIER_READ", "CER_REGISTER_READ", "PAR_INSTITUTION_READ"]
# Profils par fonction (D-58). Chaque acteur n'a que les actions de sa fonction ; le rôle
# CONDUCTEUR_PROJET habilite à conduire un projet, le projet lui étant confié par désignation.
PROFILS = {
    "ROLE_INGENIEUR_HOMOLOGATION": LECTURE_DES + ["HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH",
        "HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_OPERATION_REOPEN", "HOM_GRID_EDIT",
        "HOM_REPLAY_ADMIN", "HOM_SCENARIO_QUALIFY", "HOM_EVIDENCE_FILE_EXPORT", "CER_DOSSIER_VERIFY", "CER_GOLIVE_REPORT_SUBMIT",
        "CER_ATTESTATION_ISSUE"],
    # Chefs de département : ouvrent la liaison au cadrage et désignent le conducteur de projet (D-58, rang 6).
    "ROLE_CHEF_DES": LECTURE_DES + ["HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "HOM_REFERENTIEL_MIGRATE", "HOM_LIAISON_UNFREEZE", "HOM_OPERATION_VALIDATE",
        "HOM_DEADLINE_CONFIG", "HOM_EVIDENCE_FILE_EXPORT", "CER_QUITUS_TECHNIQUE_SIGN", "CER_ATTESTATION_ISSUE",
        "CER_REGISTER_EXPORT", "CER_ATTESTATION_STATUS_REQUEST", "HOM_STEP_MANAGE"],
    # Super administrateur et RSSI administrent la plateforme depuis la console (D-62) ; seul le
    # super administrateur attribue un profil privilégié et règle la journalisation des services.
    "ROLE_SUPER_ADMIN": LECTURE_DES + ["ROLE_ADMIN"] + [f"IAM_{p}" for p in PERMISSIONS["IAM"]]
        + [f"PLATFORM_{p}" for p in PERMISSIONS["PLATFORM"]],
    "ROLE_RSSI": LECTURE_DES + ["CER_QUITUS_SECURITE_SIGN", "CER_SECURITY_VERIFY", "HOM_EVIDENCE_SUBMIT"] + [f"IAM_{p}" for p in PERMISSIONS["IAM"] if p != "ROLE_PRIVILEGED_GRANT"]
        + ["PLATFORM_SUPERVISION_READ", "PLATFORM_CONFIG_READ", "PLATFORM_SECURITY_POLICY_READ", "PLATFORM_SECURITY_POLICY_EDIT",
           "PLATFORM_MESSAGING_EDIT"],
    "ROLE_CONFORMITE": LECTURE_DES + ["CER_QUITUS_CONFORMITE_SIGN", "CER_CHANGE_RECORD_LINK"],
    "ROLE_DG": LECTURE_DES + ["CER_QUITUS_DG_SIGN", "CER_ATTESTATION_STATUS_APPROVE"],
    "ROLE_CORRESPONDANT": ["HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT", "CER_PORTAL_READ", "CER_PORTAL_SIGN"],
    # DDRM : le chargé instruit et propose, le chef prononce (maker-checker, algorithme 6.25).
    "ROLE_CHARGE_DDRM": LECTURE_DES + ["PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT",
        "PAR_TRANSITION_REQUEST", "HOM_STEP_MANAGE"],
    "ROLE_CHEF_DDRM": LECTURE_DES + ["HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT",
        "PAR_TRANSITION_REQUEST", "PAR_TRANSITION_APPROVE", "HOM_STEP_MANAGE"],
    # Service Normes et Agréments, production monétique, pôles réseau et système (CR-CERT §3.6).
    "ROLE_SNA": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "CER_DOSSIER_VERIFY"],
    "ROLE_PRODUCTION_MONETIQUE": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "CER_CHANGE_RECORD_LINK", "CER_GOLIVE_REPORT_SUBMIT"],
    "ROLE_POLE_RESEAU": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT"],
    "ROLE_POLE_SYSTEME": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT"],
    "ROLE_CONDUCTEUR_PROJET": LECTURE_DES + ["HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_EVIDENCE_FILE_EXPORT",
        "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE", "CER_PV_ESTABLISH"],
}
# Messagerie de travail (D-64) : tous les profils internes du GIMAC, jamais le correspondant d'un
# participant (profil externe, porteur d'une institution).
PROFILS_EXTERNES = {"ROLE_CORRESPONDANT"}
MESSAGERIE = ["COL_MESSAGE_READ", "COL_MESSAGE_SEND"]
for _profil in PROFILS:
    if _profil not in PROFILS_EXTERNES:
        PROFILS[_profil] = PROFILS[_profil] + MESSAGERIE
# Institutions fictives du jeu de données de référence (D-30).
UTILISATEURS = [
    ("test-ingenieur-homologation", "Ingénieur", "Homologation", "ROLE_INGENIEUR_HOMOLOGATION", None),
    ("test-chef-des", "Chef", "DES", "ROLE_CHEF_DES", None),
    ("test-super-admin", "Super", "Administrateur", "ROLE_SUPER_ADMIN", None),
    ("test-rssi", "Responsable", "Sécurité", "ROLE_RSSI", None),
    ("test-conformite", "Responsable", "Conformité", "ROLE_CONFORMITE", None),
    ("test-dg", "Direction", "Générale", "ROLE_DG", None),
    ("test-correspondant-a", "Correspondant", "Établissement de paiement A", "ROLE_CORRESPONDANT", "INST-0042"),
    ("test-correspondant-b", "Correspondant", "Banque B", "ROLE_CORRESPONDANT", "INST-0043"),
    ("test-charge-ddrm", "Chargé", "DDRM", "ROLE_CHARGE_DDRM", None),
    ("test-chef-ddrm", "Chef", "DDRM", "ROLE_CHEF_DDRM", None),
    ("test-sna", "Agent", "Normes et Agréments", "ROLE_SNA", None),
    ("test-production-monetique", "Agent", "Production monétique", "ROLE_PRODUCTION_MONETIQUE", None),
    ("test-pole-reseau", "Agent", "Pôle réseau", "ROLE_POLE_RESEAU", None),
    ("test-pole-systeme", "Agent", "Pôle système", "ROLE_POLE_SYSTEME", None),
    ("test-conducteur-projet", "Conducteur", "Projet", "ROLE_CONDUCTEUR_PROJET", None),
]
SERVICES = {
    # Livrables du portail déposés pour le correspondant dans MS-DOCUMENT (étapes 4 à 10).
    "homologation-service": ["INTERNAL_PARTICIPANT_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT", "INTERNAL_IAM_READ"],
    # Attestations du participant déposées pour son compte dans MS-DOCUMENT (verrou 2).
    "certification-service": ["INTERNAL_HOMOLOGATION_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT", "INTERNAL_IAM_READ"],
    "participant-service": ["INTERNAL_DOCUMENT_READ", "INTERNAL_IAM_READ"],
    # Messagerie (D-64) : annuaire des comptes internes (MS-IAM), dépôt et lecture des pièces jointes (MS-DOCUMENT).
    "collaboration-service": ["INTERNAL_IAM_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT"],
    # MS-IAM administre le realm par l'API d'administration (D-62) : rôles de realm-management ci-dessous.
    "iam-service": [],
    # Connexion sur la plateforme (D-65) : la passerelle lit l'état de connexion du compte sur MS-IAM.
    "gateway-service": ["INTERNAL_IAM_READ"],
    # Supervision de la passerelle : lecture des points /management des services (ROLE_ADMIN).
    "gateway-supervision": ["ROLE_ADMIN"],
}
ROLES_CLIENT_SERVICES = {
    "iam-service": {"realm-management": ["manage-users", "view-users", "query-users", "query-groups", "manage-realm",
                                         "view-realm"]},
}

# Profil utilisateur déclaratif (Keycloak 26) : les attributs de la plateforme sont déclarés, modifiables
# par l'administration seulement ; l'utilisateur les renseigne par « Mon compte » (MS-IAM), jamais par Keycloak.
ADMIN_SEUL = {"view": ["admin"], "edit": ["admin"]}
PROFIL_UTILISATEUR = {
    "unmanagedAttributePolicy": "ADMIN_EDIT",
    "attributes": [
        {"name": "username", "displayName": "${username}", "permissions": {"view": ["admin", "user"], "edit": ["admin"]},
         "validations": {"length": {"min": 3, "max": 255}, "username-prohibited-characters": {}, "up-username-not-idn-homograph": {}}},
        {"name": "email", "displayName": "${email}", "required": {"roles": ["user"]},
         "permissions": {"view": ["admin", "user"], "edit": ["admin"]}, "validations": {"email": {}, "length": {"max": 255}}},
        {"name": "firstName", "displayName": "${firstName}", "required": {"roles": ["user"]},
         "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]},
         "validations": {"length": {"max": 255}, "person-name-prohibited-characters": {}}},
        {"name": "lastName", "displayName": "${lastName}", "required": {"roles": ["user"]},
         "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]},
         "validations": {"length": {"max": 255}, "person-name-prohibited-characters": {}}},
        {"name": "institution_id", "displayName": "Institution", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "phone_number", "displayName": "Téléphone", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "fonction", "displayName": "Fonction", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "profil_complet", "displayName": "Premier accès terminé", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "langKey", "displayName": "Langue", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "login", "displayName": "Identifiant", "permissions": ADMIN_SEUL, "multivalued": False},
        # Actions exigées tenues par la plateforme (D-65), jamais par les actions requises de Keycloak,
        # qui bloqueraient l'octroi direct : MOT_DE_PASSE, SECOND_FACTEUR, FICHE, CHARTE, VERIFIER_COURRIEL.
        {"name": "gimac_actions_requises", "displayName": "Actions exigées par la plateforme", "permissions": ADMIN_SEUL,
         "multivalued": True},
    ],
    "groups": [],
}

# Double facteur (D-61) : niveaux d'assurance du flux navigateur. Niveau 1 : mot de passe ;
# niveau 2 : mot de passe puis code à usage unique (TOTP). Keycloak pose le niveau atteint dans
# le claim acr (« 1 » ou « 2 ») ; les clients des couches de présentation exigent le niveau 2.
FLUX_NAVIGATEUR = "navigateur-gimac"
# D-65 : web_app ne passe plus par le navigateur (octroi direct à deux facteurs, acr posé par mappeur) ;
# seul tester-cli (contrôles cURL des développeurs, autorisation par appareil) garde le flux navigateur.
CLIENTS_NIVEAU_2 = ["tester-cli"]

# Connexion sur la plateforme (D-65, docs/lot1/connexion.md §4) : la passerelle soumet identifiant,
# mot de passe et code à Keycloak par l'octroi direct, serveur à serveur ; aucune page de Keycloak.
FLUX_MOT_DE_PASSE = "direct-gimac-mot-de-passe"
FLUX_DEUX_FACTEURS = "direct-gimac-deux-facteurs"
CLIENT_CONNEXION = "gimac-connexion"
# Actions exigées des comptes de test : second facteur à configurer sur la plateforme au premier accès.
ACTIONS_TEST = ["SECOND_FACTEUR"]
# Sessions (connexion.md §6) : 30 min d'inactivité, 10 h au plus, jeton d'accès de 5 min.
SESSIONS = {"ssoSessionIdleTimeout": 1800, "ssoSessionMaxLifespan": 36000, "accessTokenLifespan": 300}
# Protection contre la force brute (D-62, planchers) : elle s'applique aussi à l'octroi direct.
FORCE_BRUTE = {"bruteForceProtected": True, "permanentLockout": False, "failureFactor": 5, "waitIncrementSeconds": 900,
               "maxFailureWaitSeconds": 900, "maxDeltaTimeSeconds": 43200, "quickLoginCheckMilliSeconds": 1000,
               "minimumQuickLoginWaitSeconds": 60}
# Politique OTP du domaine, lue par MS-IAM et par l'extension gimac-totp pour vérifier le premier code.
# Politique des mots de passe au plancher de la plateforme (D-62), écrite dans la syntaxe de Keycloak
# comme MS-IAM l'écrit (PolitiqueMotDePasse.versChaine) ; la console la durcit ensuite.
POLITIQUE_MOTS_DE_PASSE = "length(12) and notUsername(undefined)"
POLITIQUE_OTP = {"otpPolicyType": "totp", "otpPolicyAlgorithm": "HmacSHA1", "otpPolicyDigits": 6, "otpPolicyPeriod": 30,
                 "otpPolicyLookAheadWindow": 1, "otpPolicyInitialCounter": 0, "otpPolicyCodeReusable": False}


def execution(authenticator, requirement, priority, config=None):
    e = {"authenticator": authenticator, "authenticatorFlow": False, "requirement": requirement, "priority": priority,
         "userSetupAllowed": False}
    if config:
        e["authenticatorConfig"] = config
    return e


def sous_flux(alias, requirement, priority):
    return {"flowAlias": alias, "authenticatorFlow": True, "requirement": requirement, "priority": priority,
            "userSetupAllowed": False}


def flux(alias, description, executions, top_level=False):
    return {"alias": alias, "description": description, "providerId": "basic-flow", "topLevel": top_level,
            "builtIn": False, "authenticationExecutions": executions}


def flux_double_facteur():
    """Flux navigateur à niveaux d'assurance (Keycloak « step-up authentication »)."""
    f = FLUX_NAVIGATEUR
    flux_ = [
        flux(f, "Navigateur GIMAC : cookie ou formulaires à niveaux d'assurance (D-61)", [
            execution("auth-cookie", "ALTERNATIVE", 10),
            sous_flux(f + " formulaires", "ALTERNATIVE", 20),
        ], top_level=True),
        flux(f + " formulaires", "Niveaux d'assurance 1 et 2", [
            sous_flux(f + " niveau 1", "CONDITIONAL", 10),
            sous_flux(f + " niveau 2", "CONDITIONAL", 20),
        ]),
        flux(f + " niveau 1", "Niveau 1 : identifiant et mot de passe", [
            execution("conditional-level-of-authentication", "REQUIRED", 10, "gimac-niveau-1"),
            execution("auth-username-password-form", "REQUIRED", 20),
        ]),
        flux(f + " niveau 2", "Niveau 2 : code à usage unique", [
            execution("conditional-level-of-authentication", "REQUIRED", 10, "gimac-niveau-2"),
            execution("auth-otp-form", "REQUIRED", 20),
        ]),
    ]
    configs = [
        {"alias": "gimac-niveau-1", "config": {"loa-condition-level": "1", "loa-max-age": "36000"}},
        {"alias": "gimac-niveau-2", "config": {"loa-condition-level": "2", "loa-max-age": "36000"}},
    ]
    return flux_, configs


def flux_octroi_direct():
    """Deux flux d'octroi direct (D-65) : mot de passe seul (acr 1), mot de passe et code obligatoire (acr 2)."""
    base = [execution("direct-grant-validate-username", "REQUIRED", 10),
            execution("direct-grant-validate-password", "REQUIRED", 20)]
    mot_de_passe = flux(FLUX_MOT_DE_PASSE, "Octroi direct GIMAC : identifiant et mot de passe (premier accès, acr 1)",
                        copy.deepcopy(base), top_level=True)
    deux_facteurs = flux(FLUX_DEUX_FACTEURS, "Octroi direct GIMAC : identifiant, mot de passe et code TOTP obligatoire (acr 2)",
                         copy.deepcopy(base) + [execution("direct-grant-validate-otp", "REQUIRED", 30)], top_level=True)
    # Identifiants propres (nouveaux flux) : les clients s'y lient par identifiant, que Keycloak remappe à l'import.
    mot_de_passe["id"] = uid("flow:" + FLUX_MOT_DE_PASSE)
    deux_facteurs["id"] = uid("flow:" + FLUX_DEUX_FACTEURS)
    return [mot_de_passe, deux_facteurs]


def mappeur_acr(niveau):
    """Niveau d'assurance posé en dur : l'octroi direct ne le calcule pas (acr 1 ou 2 selon le client)."""
    return {"name": "acr-gimac", "protocol": "openid-connect", "protocolMapper": "oidc-hardcoded-claim-mapper",
            "consentRequired": False,
            "config": {"claim.name": "acr", "claim.value": niveau, "jsonType.label": "String", "id.token.claim": "true",
                       "access.token.claim": "true", "userinfo.token.claim": "true", "introspection.token.claim": "true"}}


def client_octroi_direct(c, flux_id, niveau):
    """Client confidentiel à octroi direct seul, lié à son flux, acr en dur, sans portée acr par défaut."""
    c.update({"enabled": True, "publicClient": False, "clientAuthenticatorType": "client-secret",
              "standardFlowEnabled": False, "implicitFlowEnabled": False, "directAccessGrantsEnabled": True,
              "serviceAccountsEnabled": False, "frontchannelLogout": False, "redirectUris": [], "webOrigins": []})
    c["defaultClientScopes"] = [s for s in c.get("defaultClientScopes", []) if s != "acr"]
    c["authenticationFlowBindingOverrides"] = {"direct_grant": flux_id}
    c["protocolMappers"] = [m for m in c.get("protocolMappers", []) if m.get("name") != "acr-gimac"] + [mappeur_acr(niveau)]
    attributs = c.setdefault("attributes", {})
    attributs.pop("minimum.acr.value", None)
    attributs["access.token.lifespan"] = "300"
    return c


def uid(nom):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "gimac:" + nom))


def role(nom, description, composites=None):
    r = {"id": uid("role:" + nom), "name": nom, "description": description, "composite": bool(composites), "clientRole": False}
    if composites:
        r["composites"] = {"realm": composites}
    return r


def client_confidentiel(client_id, secret_var, directs=False, service=False, roles_service=None):
    c = {
        "id": uid("client:" + client_id), "clientId": client_id, "enabled": True, "publicClient": False,
        "clientAuthenticatorType": "client-secret", "secret": "${" + secret_var + "}",
        "standardFlowEnabled": False, "directAccessGrantsEnabled": directs, "serviceAccountsEnabled": service,
        "protocol": "openid-connect", "fullScopeAllowed": True,
        "defaultClientScopes": ["web-origins", "acr", "jhipster", "roles", "profile", "email"],
        "optionalClientScopes": ["offline_access"],
    }
    return c


def main():
    realm = json.loads(SOURCE.read_text(encoding="utf-8"))
    realm = copy.deepcopy(realm)
    realm["realm"] = "gimac"
    realm["id"] = "gimac"
    realm["displayName"] = "Plateforme DES du GIMAC (développement)"
    realm["internationalizationEnabled"] = True
    realm["supportedLocales"] = ["fr", "en", "es"]
    realm["defaultLocale"] = "fr"
    # D-62 : pages et courriels du fournisseur d'identité aux couleurs du GIMAC ; aucun compte
    # ouvert par inscription libre (création par le super administrateur ou le RSSI) ; mot de
    # passe oublié réinitialisable par courriel ; identifiant non modifiable par l'utilisateur.
    realm["loginTheme"] = "gimac"
    realm["emailTheme"] = "gimac"
    realm["registrationAllowed"] = False
    # D-65 : le mot de passe oublié se réinitialise sur la plateforme (courriel de MS-IAM), jamais par Keycloak.
    realm["resetPasswordAllowed"] = False
    realm["rememberMe"] = False
    realm["editUsernameAllowed"] = False
    realm["loginWithEmailAllowed"] = True
    # Messagerie de développement (Mailpit du Compose) ; reconfigurable depuis la console.
    realm["smtpServer"] = {"host": "messagerie", "port": "1025", "from": "des@gimac.test",
                           "fromDisplayName": "Plateforme DES du GIMAC", "auth": "false", "starttls": "false", "ssl": "false"}
    for r in realm["roles"]["realm"]:
        if r["name"] == "default-roles-jhipster":
            r["name"] = "default-roles-gimac"
    if "defaultRole" in realm:
        realm["defaultRole"]["name"] = "default-roles-gimac"

    realm["roles"]["realm"] += [role(p, "Permission du lot 1") for p in TOUTES]
    realm["roles"]["realm"] += [role(n, "Profil du lot 1", ["ROLE_USER"] + ps) for n, ps in PROFILS.items()]

    # Claim institution_id (RG-59), porté par le client scope jhipster déjà servi aux applications.
    for cs in realm["clientScopes"]:
        if cs["name"] == "jhipster":
            # D-65 : la passerelle ouvre la session à partir de l'octroi direct ; les rôles et permissions
            # figurent dans le jeton d'identité comme dans le jeton d'accès (acr et institution_id aussi).
            for m in cs["protocolMappers"]:
                if m["name"] == "roles":
                    m["config"]["id.token.claim"] = "true"
                    m["config"]["access.token.claim"] = "true"
            cs["protocolMappers"].append({
                "id": uid("mapper:institution_id"), "name": "institution_id", "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-attribute-mapper", "consentRequired": False,
                "config": {"user.attribute": "institution_id", "claim.name": "institution_id", "jsonType.label": "String",
                           "id.token.claim": "true", "access.token.claim": "true", "userinfo.token.claim": "true"},
            })

    for c in realm["clients"]:
        if c["clientId"] == "internal" and "jhipster" not in c.get("defaultClientScopes", []):
            c["defaultClientScopes"].append("jhipster")
        # Aucun secret hérité du realm JHipster (BNF-07) : le client de service « internal » reçoit une
        # variable résolue à l'import ; les clients publics n'en ont pas l'usage et les clients intégrés
        # (account, broker, realm-management) en reçoivent un de Keycloak.
        if c["clientId"] == "internal":
            c["secret"] = "${GIMAC_SECRET_INTERNAL}"
        else:
            c.pop("secret", None)
    realm["clients"].append(client_confidentiel("tester-cli", "GIMAC_SECRET_TESTER_CLI", directs=True))
    # Jetons de recette à deux facteurs pour les contrôles cURL : autorisation par appareil (RFC 8628),
    # qui passe par le flux navigateur ; le mot de passe direct ne donne que le niveau 1.
    realm["clients"][-1].setdefault("attributes", {})["oauth2.device.authorization.grant.enabled"] = "true"

    flux_, configs = flux_double_facteur()
    realm["authenticationFlows"] = [x for x in realm["authenticationFlows"] if not x["alias"].startswith(FLUX_NAVIGATEUR)] + flux_
    realm["authenticatorConfig"] = [x for x in realm.get("authenticatorConfig", []) if not x["alias"].startswith("gimac-niveau-")] + configs
    realm["browserFlow"] = FLUX_NAVIGATEUR
    for c in realm["clients"]:
        if c["clientId"] in CLIENTS_NIVEAU_2:
            c.setdefault("attributes", {})["minimum.acr.value"] = "2"

    # D-65 : octroi direct. gimac-connexion (mot de passe seul, acr 1, session restreinte du premier
    # accès) et web_app (mot de passe et code obligatoire, acr 2), confidentiels, sans flux navigateur.
    octroi = flux_octroi_direct()
    realm["authenticationFlows"] = [x for x in realm["authenticationFlows"] if not x["alias"].startswith("direct-gimac-")] + octroi
    flux_mdp, flux_2fa = (f["id"] for f in octroi)
    connexion = client_confidentiel(CLIENT_CONNEXION, "GIMAC_SECRET_CONNEXION", directs=True)
    connexion["description"] = "Connexion sur la plateforme, mot de passe seul (acr 1, premier accès) : passerelle et MS-IAM"
    realm["clients"].append(client_octroi_direct(connexion, flux_mdp, "1"))
    for c in realm["clients"]:
        if c["clientId"] == "web_app":
            client_octroi_direct(c, flux_2fa, "2")
            c["secret"] = "${GIMAC_SECRET_WEB_APP}"
            c["description"] = "Connexion sur la plateforme, mot de passe et code TOTP (acr 2) : passerelle"
    for client_id in SERVICES:
        realm["clients"].append(client_confidentiel(client_id, "GIMAC_SECRET_" + client_id.upper().replace("-", "_"), service=True))

    utilisateurs = [u for u in realm["users"] if not u["username"].startswith("service-account-")]
    for login, prenom, nom, profil, institution in UTILISATEURS:
        u = {
            "id": uid("user:" + login), "username": login, "enabled": True, "emailVerified": True,
            "firstName": prenom, "lastName": nom, "email": f"{login}@gimac.test",
            "credentials": [{"type": "password", "value": "${GIMAC_MOT_DE_PASSE_TEST}", "temporary": False}],
            "realmRoles": ["default-roles-gimac", profil], "groups": ["/Users"],
            # Second facteur configuré sur la plateforme au premier accès (D-61, D-65) : action tenue par
            # l'attribut gimac_actions_requises, aucune action requise de Keycloak (elle bloquerait l'octroi direct).
            "attributes": {"langKey": ["fr"], "login": [login], "gimac_actions_requises": list(ACTIONS_TEST)},
        }
        if institution:
            u["attributes"]["institution_id"] = [institution]
        utilisateurs.append(u)
    for client_id, roles in SERVICES.items():
        utilisateurs.append({
            "id": uid("user:service-account-" + client_id), "username": "service-account-" + client_id, "enabled": True,
            "serviceAccountClientId": client_id, "realmRoles": ["default-roles-gimac"] + roles,
        })
    for u in utilisateurs:
        u["realmRoles"] = ["default-roles-gimac" if r == "default-roles-jhipster" else r for r in u.get("realmRoles", [])]
        if u["username"] in ("admin", "user"):
            # Les empreintes du realm généré (mot de passe connu « admin », « user ») sont remplacées.
            u["credentials"] = [{"type": "password", "value": "${GIMAC_MOT_DE_PASSE_TEST}", "temporary": False}]
    for u in utilisateurs:
        u.pop("requiredActions", None)
    realm["users"] = utilisateurs
    realm.update(SESSIONS)
    realm.update(FORCE_BRUTE)
    realm.update(POLITIQUE_OTP)
    realm["passwordPolicy"] = POLITIQUE_MOTS_DE_PASSE
    for u in realm["users"]:
        client_id = u.get("serviceAccountClientId")
        if client_id in ROLES_CLIENT_SERVICES:
            u["clientRoles"] = ROLES_CLIENT_SERVICES[client_id]

    for composant in realm["components"]["org.keycloak.userprofile.UserProfileProvider"]:
        composant["config"] = {"kc.user.profile.config": [json.dumps(PROFIL_UTILISATEUR, ensure_ascii=False)]}

    # Identifiants internes retirés : le realm JHipster est importé dans la même base Keycloak,
    # ses identifiants de rôles, clients et portées y existent déjà. Keycloak les régénère.
    def sans_identifiants(noeud, racine=False):
        if isinstance(noeud, dict):
            return {k: sans_identifiants(v) for k, v in noeud.items() if racine and k == "id" or k not in ("id", "containerId")}
        if isinstance(noeud, list):
            return [sans_identifiants(v) for v in noeud]
        return noeud
    realm = sans_identifiants(realm, racine=True)
    # Les flux d'octroi direct gardent leur identifiant : les clients s'y lient par lui (remappé à l'import).
    for f in realm["authenticationFlows"]:
        if f["alias"] in (FLUX_MOT_DE_PASSE, FLUX_DEUX_FACTEURS):
            f["id"] = uid("flow:" + f["alias"])

    texte = json.dumps(realm, ensure_ascii=False, indent=2)
    assert "default-roles-jhipster" not in texte
    assert all(c.get("secret", "${").startswith("${") for c in realm["clients"]), "secret de client en clair"

    assert all(x in texte for x in ('"browserFlow": "navigateur-gimac"', '"minimum.acr.value": "2"', "PAR_TRANSITION_APPROVE", "ROLE_SUPER_ADMIN",
                                          "manage-realm", "unmanagedAttributePolicy", "COL_MESSAGE_SEND", "collaboration-service",
                                          '"loginTheme": "gimac"'))
    # D-65 : connexion sur la plateforme, jamais de page Keycloak.
    clients = {c["clientId"]: c for c in realm["clients"]}
    flux_ids = {f["alias"]: f.get("id") for f in realm["authenticationFlows"]}
    for client_id, flux_alias, niveau in ((CLIENT_CONNEXION, FLUX_MOT_DE_PASSE, "1"), ("web_app", FLUX_DEUX_FACTEURS, "2")):
        c = clients[client_id]
        assert not c["publicClient"] and c["directAccessGrantsEnabled"] and not c["standardFlowEnabled"], client_id
        assert not c.get("implicitFlowEnabled") and not c.get("serviceAccountsEnabled"), client_id
        assert c["secret"].startswith("${GIMAC_SECRET_"), client_id
        assert c["authenticationFlowBindingOverrides"] == {"direct_grant": flux_ids[flux_alias]}, client_id
        assert "acr" not in c["defaultClientScopes"] and "jhipster" in c["defaultClientScopes"], client_id
        assert "minimum.acr.value" not in c.get("attributes", {}), client_id
        assert [m["config"]["claim.value"] for m in c["protocolMappers"] if m["protocolMapper"] == "oidc-hardcoded-claim-mapper"
                and m["config"]["claim.name"] == "acr"] == [niveau], client_id
    assert clients[CLIENT_CONNEXION]["secret"] == "${GIMAC_SECRET_CONNEXION}"
    assert clients["web_app"]["secret"] == "${GIMAC_SECRET_WEB_APP}" and clients["gateway-service"]["secret"] == "${GIMAC_SECRET_GATEWAY_SERVICE}"
    jhipster = next(cs for cs in realm["clientScopes"] if cs["name"] == "jhipster")
    assert all(m["config"]["id.token.claim"] == m["config"]["access.token.claim"] == "true"
               for m in jhipster["protocolMappers"] if m["name"] in ("roles", "institution_id"))
    assert clients[CLIENT_CONNEXION]["attributes"]["access.token.lifespan"] == "300"
    otp = [e["authenticator"] for f in realm["authenticationFlows"] if f["alias"] == FLUX_DEUX_FACTEURS
           for e in f["authenticationExecutions"] if e["requirement"] == "REQUIRED"]
    assert otp == ["direct-grant-validate-username", "direct-grant-validate-password", "direct-grant-validate-otp"], otp
    assert "direct-grant-validate-otp" not in texte.split(FLUX_MOT_DE_PASSE, 1)[1].split('"topLevel"', 1)[0]
    comptes_service = {u.get("serviceAccountClientId"): u for u in realm["users"]}
    assert clients["gateway-service"]["serviceAccountsEnabled"] and "INTERNAL_IAM_READ" in comptes_service["gateway-service"]["realmRoles"]
    assert "manage-users" in comptes_service["iam-service"]["clientRoles"]["realm-management"]
    assert not any(u.get("requiredActions") for u in realm["users"]), "action requise Keycloak sur un compte"
    assert all(u["attributes"]["gimac_actions_requises"] == ACTIONS_TEST for u in realm["users"] if u["username"].startswith("test-"))
    assert all(realm[k] == v for k, v in {**SESSIONS, **FORCE_BRUTE, **POLITIQUE_OTP}.items())
    assert realm["passwordPolicy"] == POLITIQUE_MOTS_DE_PASSE
    assert realm["resetPasswordAllowed"] is False and "gimac_actions_requises" in texte
    CIBLE.write_text(texte + "\n", encoding="utf-8")
    print(f"{CIBLE.relative_to(RACINE)} : {len(TOUTES)} permissions, {len(PROFILS)} profils, {len(UTILISATEURS)} utilisateurs de test")


if __name__ == "__main__":
    main()
