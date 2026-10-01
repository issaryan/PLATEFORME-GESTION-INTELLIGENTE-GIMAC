#!/usr/bin/env python3
"""Comptes nominatifs de l'environnement local (amorçage, D-62).

Crée dans le domaine gimac les comptes décrits dans infra/keycloak/local/comptes.json, fichier
local jamais versionné (aucune donnée personnelle dans le dépôt). Chaque compte est créé comme
MS-IAM le fait depuis la console : mot de passe provisoire, profil, attributs de la plateforme,
premier accès à accomplir sur la plateforme (D-65 : attribut gimac_actions_requises, mot de passe provisoire
non temporaire pour Keycloak, changement du mot de passe et code TOTP dans l'assistant de premier accès).

L'amorçage passe par l'API d'administration de Keycloak parce que la route de MS-IAM exige un
jeton à deux facteurs qu'aucun compte ne détient encore ; les comptes suivants se créent depuis
la console de la plateforme par le super administrateur ou le RSSI.

    python3 infra/keycloak/creer-comptes-personnels.py

Les mots de passe provisoires sont écrits dans infra/keycloak/local/mots-de-passe-provisoires.txt
(droits 600). Keycloak de développement (dev-file) perd ses comptes quand son conteneur est recréé :
relancer alors le script. Un compte existant est laissé tel quel.

Format de comptes.json :
    [{"identifiant": "prenom.nom", "email": "...", "prenom": "...", "nom": "...",
      "profil": "ROLE_SUPER_ADMIN", "fonction": "...", "institution_id": null}]
"""
import json
import os
import pathlib
import secrets
import string
import sys
import urllib.error
import urllib.parse
import urllib.request

RACINE = pathlib.Path(__file__).resolve().parents[2]
LOCAL = RACINE / "infra/keycloak/local"
ENV = RACINE / "infra/docker-compose/.env"
DOMAINE = "gimac"
# Actions de la première connexion (MS-IAM, iam.premiere-connexion.actions). La vérification du
# courriel est omise : la messagerie de développement (Mailpit) ne délivre pas aux vraies adresses.
# Actions exigées par la plateforme (D-65), attribut gimac_actions_requises : jamais d'action requise
# de Keycloak, qui bloquerait l'octroi direct de la connexion sur la plateforme.
ACTIONS = ["MOT_DE_PASSE", "SECOND_FACTEUR", "FICHE", "CHARTE"]


def env():
    valeurs = {}
    for ligne in ENV.read_text().splitlines():
        if "=" in ligne and not ligne.startswith("#"):
            k, v = ligne.split("=", 1)
            valeurs[k] = v
    return valeurs


def appel(methode, url, jeton=None, corps=None, formulaire=None):
    donnees, entetes = None, {}
    if formulaire is not None:
        donnees = urllib.parse.urlencode(formulaire).encode()
        entetes["Content-Type"] = "application/x-www-form-urlencoded"
    elif corps is not None:
        donnees = json.dumps(corps).encode()
        entetes["Content-Type"] = "application/json"
    if jeton:
        entetes["Authorization"] = f"Bearer {jeton}"
    requete = urllib.request.Request(url, data=donnees, headers=entetes, method=methode)
    with urllib.request.urlopen(requete, timeout=30) as reponse:
        texte = reponse.read().decode()
        return reponse.status, (json.loads(texte) if texte else None), reponse.headers


def mot_de_passe():
    # 16 caractères, au-dessus du plancher de 12 (D-62), avec chaque classe représentée.
    classes = [string.ascii_uppercase, string.ascii_lowercase, string.digits, "!@#%*-_"]
    tirage = [secrets.choice(c) for c in classes] + [secrets.choice("".join(classes)) for _ in range(12)]
    secrets.SystemRandom().shuffle(tirage)
    return "".join(tirage)


def main():
    e = env()
    base = os.environ.get("KEYCLOAK_URL", f"http://localhost:{e.get('GIMAC_PORT_KEYCLOAK', '9080')}")
    comptes = json.loads((LOCAL / "comptes.json").read_text())
    _, jeton, _ = appel("POST", f"{base}/realms/master/protocol/openid-connect/token", formulaire={
        "grant_type": "password", "client_id": "admin-cli", "username": "gimac-admin",
        "password": e["GIMAC_KEYCLOAK_ADMIN_PASSWORD"]})
    jeton = jeton["access_token"]
    admin = f"{base}/admin/realms/{DOMAINE}"
    sortie = []
    for c in comptes:
        _, existants, _ = appel("GET", f"{admin}/users?exact=true&username={urllib.parse.quote(c['identifiant'])}", jeton)
        if existants:
            print(f"déjà présent : {c['identifiant']}")
            continue
        attributs = {"profil_complet": ["false"], "locale": ["fr"], "langKey": ["fr"], "gimac_actions_requises": ACTIONS}
        if c.get("fonction"):
            attributs["fonction"] = [c["fonction"]]
        if c.get("institution_id"):
            attributs["institution_id"] = [c["institution_id"]]
        _, _, entetes = appel("POST", f"{admin}/users", jeton, corps={
            "username": c["identifiant"], "email": c["email"], "firstName": c["prenom"], "lastName": c["nom"],
            "enabled": True, "emailVerified": True, "attributes": attributs})
        uid = entetes["Location"].rsplit("/", 1)[1]
        mdp = mot_de_passe()
        appel("PUT", f"{admin}/users/{uid}/reset-password", jeton, corps={"type": "password", "value": mdp, "temporary": False})
        _, role, _ = appel("GET", f"{admin}/roles/{c['profil']}", jeton)
        appel("POST", f"{admin}/users/{uid}/role-mappings/realm", jeton, corps=[role])
        sortie.append(f"{c['identifiant']}\t{c['email']}\t{c['profil']}\t{mdp}")
        print(f"créé : {c['identifiant']} ({c['profil']})")
    if sortie:
        fichier = LOCAL / "mots-de-passe-provisoires.txt"
        with open(os.open(fichier, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600), "a") as f:
            f.write("\n".join(sortie) + "\n")
        print(f"mots de passe provisoires : {fichier.relative_to(RACINE)}")


if __name__ == "__main__":
    try:
        main()
    except urllib.error.HTTPError as err:
        sys.exit(f"Keycloak a refusé l'appel : HTTP {err.code} {err.read().decode()[:300]}")
