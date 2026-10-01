#!/usr/bin/env python3
"""Domaine gimac à jour, comptes conservés (Keycloak persistant du serveur, deployer-serveur.sh).

    fusionner-domaine.py <gimac-realm.json du dépôt> <export kc.sh du domaine en service> > fusion.json

Le résultat est le domaine du dépôt dont les utilisateurs sont ceux de l'export (identifiants,
mots de passe hachés, codes TOTP, attributs), plus les utilisateurs du dépôt absents de l'export.
Les comptes de service viennent toujours du dépôt (leurs rôles suivent les clients). Les rôles et
groupes disparus du dépôt sont retirés des comptes, sans quoi l'import échouerait.
"""
import json
import sys


def chemins(groupes, parent=""):
    for g in groupes:
        chemin = f"{parent}/{g['name']}"
        yield chemin
        yield from chemins(g.get("subGroups", []), chemin)


def main(depot, export):
    domaine = json.load(open(depot, encoding="utf-8"))
    en_service = json.load(open(export, encoding="utf-8"))
    roles = {r["name"] for r in domaine.get("roles", {}).get("realm", [])}
    roles_clients = {c: {r["name"] for r in liste} for c, liste in domaine.get("roles", {}).get("client", {}).items()}
    groupes = set(chemins(domaine.get("groups", [])))

    utilisateurs = {}
    for u in en_service.get("users", []):
        if u.get("serviceAccountClientId"):
            continue
        u["realmRoles"] = [r for r in u.get("realmRoles", []) if r in roles]
        u["clientRoles"] = {c: [r for r in rs if r in roles_clients.get(c, set())]
                            for c, rs in u.get("clientRoles", {}).items() if c in roles_clients}
        u["groups"] = [g for g in u.get("groups", []) if g in groupes]
        utilisateurs[u["username"]] = u
    for u in domaine.get("users", []):
        utilisateurs.setdefault(u["username"], u)
    domaine["users"] = list(utilisateurs.values())
    json.dump(domaine, sys.stdout, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
