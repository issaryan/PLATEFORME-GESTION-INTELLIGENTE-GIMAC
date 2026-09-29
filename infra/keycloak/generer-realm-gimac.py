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
    "HOM": ["REFERENTIEL_READ", "REFERENTIEL_EDIT", "REFERENTIEL_PUBLISH", "REFERENTIEL_MIGRATE", "LIAISON_READ", "LIAISON_LIST",
            "LIAISON_OPEN", "LIAISON_UNFREEZE", "OPERATION_ASSIGN", "EVIDENCE_SUBMIT", "OPERATION_VALIDATE", "OPERATION_REOPEN",
            "GRID_EDIT", "REPLAY_ADMIN", "SCENARIO_QUALIFY", "DASHBOARD_READ", "DEADLINE_CONFIG", "EVIDENCE_FILE_EXPORT",
            "PORTAL_READ", "PORTAL_SUBMIT"],
    "CER": ["CIRCUIT_READ", "QUITUS_TECHNIQUE_SIGN", "QUITUS_SECURITE_SIGN", "QUITUS_CONFORMITE_SIGN", "QUITUS_DG_SIGN",
            "DOSSIER_READ", "DOSSIER_VERIFY", "CHANGE_RECORD_LINK", "GOLIVE_REPORT_SUBMIT", "ATTESTATION_ISSUE", "REGISTER_READ",
            "REGISTER_EXPORT", "ATTESTATION_STATUS_REQUEST", "ATTESTATION_STATUS_APPROVE", "PORTAL_READ"],
    # Registre des institutions et étapes 1 et 2 du parcours (D-57).
    "PAR": ["INSTITUTION_READ", "INSTITUTION_CREATE", "INSTITUTION_EDIT", "ADHESION_INSTRUCT", "TRANSITION_REQUEST",
            "TRANSITION_APPROVE"],
    "INTERNAL": ["HOMOLOGATION_READ", "CERTIFICATION_READ", "PARTICIPANT_READ", "DOCUMENT_READ", "IAM_READ"],
}
TOUTES = [f"{d}_{p}" for d, ps in PERMISSIONS.items() for p in ps]

LECTURE_DES = ["HOM_REFERENTIEL_READ", "HOM_LIAISON_READ", "HOM_LIAISON_LIST", "HOM_DASHBOARD_READ", "CER_CIRCUIT_READ",
               "CER_DOSSIER_READ", "CER_REGISTER_READ", "PAR_INSTITUTION_READ"]
# Profils par fonction (D-58). Chaque acteur n'a que les actions de sa fonction ; le rôle
# CONDUCTEUR_PROJET habilite à conduire un projet, le projet lui étant confié par désignation.
PROFILS = {
    "ROLE_INGENIEUR_HOMOLOGATION": LECTURE_DES + ["HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH", "HOM_LIAISON_OPEN",
        "HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_OPERATION_REOPEN", "HOM_GRID_EDIT",
        "HOM_REPLAY_ADMIN", "HOM_SCENARIO_QUALIFY", "HOM_EVIDENCE_FILE_EXPORT", "CER_DOSSIER_VERIFY", "CER_GOLIVE_REPORT_SUBMIT",
        "CER_ATTESTATION_ISSUE"],
    "ROLE_CHEF_DES": LECTURE_DES + ["HOM_REFERENTIEL_MIGRATE", "HOM_LIAISON_UNFREEZE", "HOM_OPERATION_VALIDATE",
        "HOM_DEADLINE_CONFIG", "HOM_EVIDENCE_FILE_EXPORT", "CER_QUITUS_TECHNIQUE_SIGN", "CER_ATTESTATION_ISSUE",
        "CER_REGISTER_EXPORT", "CER_ATTESTATION_STATUS_REQUEST"],
    "ROLE_RSSI": LECTURE_DES + ["CER_QUITUS_SECURITE_SIGN"],
    "ROLE_CONFORMITE": LECTURE_DES + ["CER_QUITUS_CONFORMITE_SIGN", "CER_CHANGE_RECORD_LINK"],
    "ROLE_DG": LECTURE_DES + ["CER_QUITUS_DG_SIGN", "CER_ATTESTATION_STATUS_APPROVE"],
    "ROLE_CORRESPONDANT": ["HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT", "CER_PORTAL_READ"],
    # DDRM : le chargé instruit et propose, le chef prononce (maker-checker, algorithme 6.25).
    "ROLE_CHARGE_DDRM": LECTURE_DES + ["PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT",
        "PAR_TRANSITION_REQUEST"],
    "ROLE_CHEF_DDRM": LECTURE_DES + ["PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT",
        "PAR_TRANSITION_REQUEST", "PAR_TRANSITION_APPROVE"],
    # Service Normes et Agréments, production monétique, pôles réseau et système (CR-CERT §3.6).
    "ROLE_SNA": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE"],
    "ROLE_PRODUCTION_MONETIQUE": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT"],
    "ROLE_POLE_RESEAU": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT"],
    "ROLE_POLE_SYSTEME": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT"],
    "ROLE_CONDUCTEUR_PROJET": LECTURE_DES + ["HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_EVIDENCE_FILE_EXPORT"],
}
# Institutions fictives du jeu de données de référence (D-30).
UTILISATEURS = [
    ("test-ingenieur-homologation", "Ingénieur", "Homologation", "ROLE_INGENIEUR_HOMOLOGATION", None),
    ("test-chef-des", "Chef", "DES", "ROLE_CHEF_DES", None),
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
    "homologation-service": ["INTERNAL_PARTICIPANT_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_IAM_READ"],
    "certification-service": ["INTERNAL_HOMOLOGATION_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_IAM_READ"],
    "participant-service": ["INTERNAL_DOCUMENT_READ", "INTERNAL_IAM_READ"],
}

# Double facteur (D-61) : niveaux d'assurance du flux navigateur. Niveau 1 : mot de passe ;
# niveau 2 : mot de passe puis code à usage unique (TOTP). Keycloak pose le niveau atteint dans
# le claim acr (« 1 » ou « 2 ») ; les clients des couches de présentation exigent le niveau 2.
FLUX_NAVIGATEUR = "navigateur-gimac"
CLIENTS_NIVEAU_2 = ["web_app", "tester-cli"]


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
    realm["supportedLocales"] = ["fr", "en"]
    realm["defaultLocale"] = "fr"
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
            cs["protocolMappers"].append({
                "id": uid("mapper:institution_id"), "name": "institution_id", "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-attribute-mapper", "consentRequired": False,
                "config": {"user.attribute": "institution_id", "claim.name": "institution_id", "jsonType.label": "String",
                           "id.token.claim": "true", "access.token.claim": "true", "userinfo.token.claim": "true"},
            })

    for c in realm["clients"]:
        if c["clientId"] == "internal" and "jhipster" not in c.get("defaultClientScopes", []):
            c["defaultClientScopes"].append("jhipster")
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
    for client_id in SERVICES:
        realm["clients"].append(client_confidentiel(client_id, "GIMAC_SECRET_" + client_id.upper().replace("-", "_"), service=True))

    utilisateurs = [u for u in realm["users"] if not u["username"].startswith("service-account-")]
    for login, prenom, nom, profil, institution in UTILISATEURS:
        u = {
            "id": uid("user:" + login), "username": login, "enabled": True, "emailVerified": True,
            "firstName": prenom, "lastName": nom, "email": f"{login}@gimac.test",
            "credentials": [{"type": "password", "value": "${GIMAC_MOT_DE_PASSE_TEST}", "temporary": False}],
            "realmRoles": ["default-roles-gimac", profil], "groups": ["/Users"],
            "attributes": {"langKey": ["fr"], "login": [login]},
            # Second facteur enrôlé à la première connexion (D-61).
            "requiredActions": ["CONFIGURE_TOTP"],
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
    realm["users"] = utilisateurs

    # Identifiants internes retirés : le realm JHipster est importé dans la même base Keycloak,
    # ses identifiants de rôles, clients et portées y existent déjà. Keycloak les régénère.
    def sans_identifiants(noeud, racine=False):
        if isinstance(noeud, dict):
            return {k: sans_identifiants(v) for k, v in noeud.items() if racine and k == "id" or k not in ("id", "containerId")}
        if isinstance(noeud, list):
            return [sans_identifiants(v) for v in noeud]
        return noeud
    realm = sans_identifiants(realm, racine=True)

    texte = json.dumps(realm, ensure_ascii=False, indent=2)
    assert "default-roles-jhipster" not in texte
    assert all(x in texte for x in ('"browserFlow": "navigateur-gimac"', '"minimum.acr.value": "2"', "PAR_TRANSITION_APPROVE"))
    CIBLE.write_text(texte + "\n", encoding="utf-8")
    print(f"{CIBLE.relative_to(RACINE)} : {len(TOUTES)} permissions, {len(PROFILS)} profils, {len(UTILISATEURS)} utilisateurs de test")


if __name__ == "__main__":
    main()
