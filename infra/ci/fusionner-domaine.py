#!/usr/bin/env python3
"""Domaine gimac à jour, comptes et administration conservés (Keycloak persistant du serveur, deployer-serveur.sh).

    fusionner-domaine.py <gimac-realm.json du dépôt> <export kc.sh du domaine en service> [<domaine du dépôt déployé la fois précédente>] > fusion.json

Le résultat est le domaine du dépôt dont les utilisateurs sont ceux de l'export (identifiants,
mots de passe hachés, codes TOTP, attributs), plus les utilisateurs du dépôt absents de l'export.
Les comptes de service viennent toujours du dépôt (leurs rôles suivent les clients).
Avec GIMAC_SANS_COMPTES_TEST=1 (serveur de démonstration, deployer-serveur.sh), les comptes de test
du générateur (identifiant « test-… », generer-realm-gimac.py) sont écartés, du dépôt comme de l'export :
ils ne reviennent pas après leur suppression sur le serveur.

Rôles du domaine (profils et permissions, D-62, D-81, D-86) : fusion à trois voies entre le domaine du dépôt
déployé la fois précédente (base), le domaine en service (administration faite dans la plateforme) et le
nouveau domaine du dépôt. Pour chaque rôle :
- composition = (composition en service - ce que le dépôt a retiré depuis la base) + ce que le dépôt a
  ajouté depuis la base ; une recomposition faite dans la plateforme est donc conservée, et une permission
  ajoutée par une évolution du dépôt est appliquée ;
- description et attributs (libellé `gimac_libelle`…) : la valeur en service si elle diffère de la base
  (modifiée dans la plateforme), sinon celle du dépôt ;
- un rôle absent du dépôt et de la base mais présent en service a été créé dans la plateforme (profil créé,
  D-81) : il est conservé ; un rôle présent dans la base et retiré du dépôt est retiré.
Sans base (premier déploiement de cette règle), composition = en service + dépôt (rien n'est perdu ; un
retrait fait dans la plateforme d'une permission encore présente dans le dépôt est alors rétabli une fois).
Les rôles et groupes disparus sont retirés des comptes, sans quoi l'import échouerait.

Politique de sécurité (PLATFORM_SECURITY_POLICY_EDIT, console) : politique de mot de passe (passwordPolicy),
paramètres OTP (otpPolicy*), durées de session (ssoSession*) et protection contre la force brute. Fusion à
trois voies par paramètre : la valeur en service si elle diffère de la base (modifiée dans la console), sinon
celle du dépôt. Sans base, la valeur en service est conservée (rien n'est perdu) ; un paramètre absent du
domaine en service prend la valeur du dépôt.
"""
import json
import os
import sys


# Comptes de test du générateur (generer-realm-gimac.py, UTILISATEURS), écartés sur le serveur.
PREFIXE_TEST = "test-"

# Paramètres du domaine administrés depuis la console (écran Politique de sécurité de MS-IAM).
PARAMETRES_POLITIQUE = (
    "passwordPolicy",
    "ssoSessionIdleTimeout", "ssoSessionMaxLifespan",
    "ssoSessionIdleTimeoutRememberMe", "ssoSessionMaxLifespanRememberMe",
    "bruteForceProtected", "permanentLockout", "maxTemporaryLockouts", "bruteForceStrategy",
    "maxFailureWaitSeconds", "minimumQuickLoginWaitSeconds", "waitIncrementSeconds",
    "quickLoginCheckMilliSeconds", "maxDeltaTimeSeconds", "failureFactor",
)
ABSENT = object()


def parametres_politique(*domaines):
    noms = set(PARAMETRES_POLITIQUE)
    for d in domaines:
        noms.update(k for k in (d or {}) if k.startswith("otpPolicy"))
    return sorted(noms)


def fusionner_politique(domaine, en_service, base):
    """Fusion à trois voies des paramètres de la politique de sécurité ; renvoie les paramètres conservés."""
    conserves = []
    for cle in parametres_politique(domaine, en_service, base):
        s = en_service.get(cle, ABSENT)
        if s is ABSENT:
            continue
        if base is None:
            garder = True
        else:
            garder = s != base.get(cle, ABSENT)
        if garder and s != domaine.get(cle, ABSENT):
            domaine[cle] = s
            conserves.append(cle)
    return conserves


def chemins(groupes, parent=""):
    for g in groupes:
        chemin = f"{parent}/{g['name']}"
        yield chemin
        yield from chemins(g.get("subGroups", []), chemin)


def roles_domaine(d):
    return {r["name"]: r for r in (d or {}).get("roles", {}).get("realm", [])}


def composition(r):
    return set(((r or {}).get("composites") or {}).get("realm", []))


def fusionner_roles(domaine, en_service, base):
    depot = roles_domaine(domaine)
    service = roles_domaine(en_service)
    precedent = roles_domaine(base) if base is not None else None

    resultat = {}
    for nom, r in depot.items():
        r = json.loads(json.dumps(r))
        s = service.get(nom)
        if s is not None:
            b = precedent.get(nom) if precedent is not None else None
            if precedent is None:
                compo = composition(s) | composition(r)
            else:
                compo = (composition(s) - (composition(b) - composition(r))) | (composition(r) - composition(b))
            if compo or r.get("composite"):
                r["composite"] = bool(compo)
                r.setdefault("composites", {})["realm"] = sorted(compo)
            if b is not None and s.get("description") != b.get("description"):
                r["description"] = s.get("description")
            attributs = dict(r.get("attributes") or {})
            for cle, valeur in (s.get("attributes") or {}).items():
                if b is not None and valeur != (b.get("attributes") or {}).get(cle):
                    attributs[cle] = valeur
            if attributs:
                r["attributes"] = attributs
        resultat[nom] = r

    # Rôles créés dans la plateforme : en service, inconnus du dépôt et de la base.
    for nom, s in service.items():
        if nom in resultat or (precedent is not None and nom in precedent):
            continue
        if precedent is None and not (s.get("attributes") or {}).get("gimac_profil_cree"):
            continue
        s = json.loads(json.dumps(s))
        s.pop("id", None)
        s.pop("containerId", None)
        resultat[nom] = s

    # Compositions limitées aux rôles qui existent encore.
    for r in resultat.values():
        if r.get("composites", {}).get("realm"):
            r["composites"]["realm"] = [c for c in r["composites"]["realm"] if c in resultat]
            r["composite"] = bool(r["composites"]["realm"] or r["composites"].get("client"))
    domaine.setdefault("roles", {})["realm"] = list(resultat.values())
    return set(resultat)


def main(depot, export, precedent=None):
    domaine = json.load(open(depot, encoding="utf-8"))
    en_service = json.load(open(export, encoding="utf-8"))
    base = json.load(open(precedent, encoding="utf-8")) if precedent else None
    roles = fusionner_roles(domaine, en_service, base)
    conserves = fusionner_politique(domaine, en_service, base)
    if conserves:
        # Noms des paramètres seulement, jamais leurs valeurs.
        print("Politique de sécurité conservée (modifiée dans la console) : " + ", ".join(conserves), file=sys.stderr)
    roles_clients = {c: {r["name"] for r in liste} for c, liste in domaine.get("roles", {}).get("client", {}).items()}
    groupes = set(chemins(domaine.get("groups", [])))

    sans_test = os.environ.get("GIMAC_SANS_COMPTES_TEST") == "1"

    def compte_test(u):
        return sans_test and not u.get("serviceAccountClientId") and u.get("username", "").startswith(PREFIXE_TEST)

    domaine["users"] = [u for u in domaine.get("users", []) if not compte_test(u)]
    utilisateurs = {}
    for u in en_service.get("users", []):
        if u.get("serviceAccountClientId") or compte_test(u):
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
    if len(sys.argv) not in (3, 4):
        sys.exit(__doc__)
    main(*sys.argv[1:])
