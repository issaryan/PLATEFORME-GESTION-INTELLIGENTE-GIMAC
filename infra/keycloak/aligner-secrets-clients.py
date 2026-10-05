#!/usr/bin/env python3
"""Aligne le secret de chaque client confidentiel du domaine gimac sur le .env du serveur (deployer-serveur.sh).

Les secrets des clients sont des variables ${GIMAC_SECRET_...} du gimac-realm.json, résolues à l'import.
Sur le serveur, le domaine est réimporté depuis un export fusionné (comptes conservés) : un client ajouté
au dépôt (pilotage-service, notification-service, D-76 et D-80) s'est retrouvé avec un secret différent de
celui que le .env donne au service, d'où invalid_client_credentials. Ce script lit, pour chaque client du
dépôt dont le secret est une variable, la valeur du .env, la compare au secret enregistré dans Keycloak
(API d'administration) et la remplace si elle diffère. Idempotent ; aucun secret n'est affiché.
"""
import importlib.util
import json
import pathlib
import urllib.parse

# Accès au .env et appels HTTP : mêmes fonctions que creer-comptes-personnels.py (amorçage des comptes).
_spec = importlib.util.spec_from_file_location("comptes", pathlib.Path(__file__).resolve().parent / "creer-comptes-personnels.py")
_comptes = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_comptes)
env, appel = _comptes.env, _comptes.appel

RACINE = pathlib.Path(__file__).resolve().parents[2]
DOMAINE_FICHIER = RACINE / "infra/keycloak/gimac-realm.json"
DOMAINE = "gimac"


def main():
    e = env()
    base = f"http://localhost:{e.get('GIMAC_PORT_KEYCLOAK', '9080')}"
    _, jeton, _ = appel("POST", f"{base}/realms/master/protocol/openid-connect/token", formulaire={
        "grant_type": "password", "client_id": "admin-cli", "username": "gimac-admin",
        "password": e["GIMAC_KEYCLOAK_ADMIN_PASSWORD"]})
    jeton = jeton["access_token"]
    admin = f"{base}/admin/realms/{DOMAINE}"
    clients = json.loads(DOMAINE_FICHIER.read_text(encoding="utf-8"))["clients"]
    ecarts = 0
    for c in clients:
        secret = c.get("secret", "")
        if not (secret.startswith("${") and secret.endswith("}")):
            continue
        variable = secret[2:-1]
        valeur = e.get(variable)
        if not valeur:
            print(f"{c['clientId']} : {variable} absent du .env, ignoré")
            continue
        _, trouves, _ = appel("GET", f"{admin}/clients?clientId={urllib.parse.quote(c['clientId'])}", jeton)
        if not trouves:
            print(f"{c['clientId']} : absent du domaine, ignoré")
            continue
        cid = trouves[0]["id"]
        _, actuel, _ = appel("GET", f"{admin}/clients/{cid}/client-secret", jeton)
        if (actuel or {}).get("value") == valeur:
            print(f"{c['clientId']} : conforme")
            continue
        _, representation, _ = appel("GET", f"{admin}/clients/{cid}", jeton)
        representation["secret"] = valeur
        appel("PUT", f"{admin}/clients/{cid}", jeton, corps=representation)
        ecarts += 1
        print(f"{c['clientId']} : secret réaligné sur le .env")
    print(f"Clients réalignés : {ecarts}")


if __name__ == "__main__":
    main()
