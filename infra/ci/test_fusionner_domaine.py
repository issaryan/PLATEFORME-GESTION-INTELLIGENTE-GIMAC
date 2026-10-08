#!/usr/bin/env python3
"""Tests de fusionner-domaine.py (fusion à trois voies du domaine Keycloak), lancés par valider-infra.sh.

    python3 -m unittest infra/ci/test_fusionner_domaine.py
"""
import contextlib
import importlib.util
import io
import json
import os
import tempfile
import unittest

ICI = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("fusionner_domaine", os.path.join(ICI, "fusionner-domaine.py"))
fd = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(fd)


def role(nom, compo=(), **autres):
    r = {"name": nom, "composite": bool(compo), **autres}
    if compo:
        r["composites"] = {"realm": list(compo)}
    return r


def domaine(roles, users=(), **parametres):
    return {"realm": "gimac", "roles": {"realm": roles, "client": {}}, "groups": [], "users": list(users), **parametres}


PERMS = [role("A"), role("B"), role("C")]


class FusionDomaine(unittest.TestCase):
    def fusion(self, depot, service, base=None):
        with tempfile.TemporaryDirectory() as d:
            chemins = []
            for nom, contenu in (("depot", depot), ("service", service), ("base", base)):
                if contenu is None:
                    continue
                p = os.path.join(d, nom + ".json")
                with open(p, "w", encoding="utf-8") as f:
                    json.dump(contenu, f)
                chemins.append(p)
            sortie, erreurs = io.StringIO(), io.StringIO()
            with contextlib.redirect_stdout(sortie), contextlib.redirect_stderr(erreurs):
                fd.main(*chemins)
            self.journal = erreurs.getvalue()
            return json.loads(sortie.getvalue())

    @staticmethod
    def roles(d):
        return {r["name"]: r for r in d["roles"]["realm"]}

    def test_recomposition_conservee_et_ajout_du_depot_applique(self):
        base = domaine(PERMS + [role("ROLE_X", ["A"])])
        service = domaine(PERMS + [role("ROLE_X", ["A", "B"])])
        depot = domaine(PERMS + [role("ROLE_X", ["A", "C"])])
        r = self.roles(self.fusion(depot, service, base))
        self.assertEqual(sorted(r["ROLE_X"]["composites"]["realm"]), ["A", "B", "C"])

    def test_retrait_du_depot_applique(self):
        base = domaine(PERMS + [role("ROLE_X", ["A", "B"])])
        service = domaine(PERMS + [role("ROLE_X", ["A", "B"])])
        depot = domaine(PERMS + [role("ROLE_X", ["A"])])
        r = self.roles(self.fusion(depot, service, base))
        self.assertEqual(r["ROLE_X"]["composites"]["realm"], ["A"])

    def test_profil_cree_conserve_profil_retire_du_depot_supprime(self):
        base = domaine(PERMS + [role("ROLE_ANCIEN", ["A"])])
        service = domaine(PERMS + [role("ROLE_ANCIEN", ["A"]), role("ROLE_CREE", ["B"], id="x", containerId="y")])
        depot = domaine(PERMS)
        r = self.roles(self.fusion(depot, service, base))
        self.assertIn("ROLE_CREE", r)
        self.assertNotIn("id", r["ROLE_CREE"])
        self.assertNotIn("ROLE_ANCIEN", r)

    def test_sans_base_union_des_compositions(self):
        service = domaine(PERMS + [role("ROLE_X", ["B"])])
        depot = domaine(PERMS + [role("ROLE_X", ["A"])])
        r = self.roles(self.fusion(depot, service))
        self.assertEqual(sorted(r["ROLE_X"]["composites"]["realm"]), ["A", "B"])

    def test_comptes_conserves_roles_disparus_retires(self):
        compte = {"username": "awa", "realmRoles": ["ROLE_X", "ROLE_DISPARU"], "groups": [], "clientRoles": {}}
        service = domaine(PERMS + [role("ROLE_X", ["A"])], users=[compte, {"username": "service-account-x", "serviceAccountClientId": "x"}])
        depot = domaine(PERMS + [role("ROLE_X", ["A"])], users=[{"username": "demo"}])
        d = self.fusion(depot, service, domaine(PERMS + [role("ROLE_X", ["A"]), role("ROLE_DISPARU")]))
        comptes = {u["username"]: u for u in d["users"]}
        self.assertEqual(sorted(comptes), ["awa", "demo"])
        self.assertEqual(comptes["awa"]["realmRoles"], ["ROLE_X"])

    def test_politique_modifiee_dans_la_console_conservee(self):
        base = domaine(PERMS, passwordPolicy="length(12)", otpPolicyDigits=6, otpPolicyPeriod=30,
                       ssoSessionIdleTimeout=1800, failureFactor=5, bruteForceProtected=True)
        service = domaine(PERMS, passwordPolicy="length(14) and digits(1)", otpPolicyDigits=8, otpPolicyPeriod=30,
                          ssoSessionIdleTimeout=900, failureFactor=5, bruteForceProtected=True)
        depot = domaine(PERMS, passwordPolicy="length(12)", otpPolicyDigits=6, otpPolicyPeriod=60,
                        ssoSessionIdleTimeout=1800, failureFactor=3, bruteForceProtected=True)
        d = self.fusion(depot, service, base)
        # Modifiés dans la console : valeur en service.
        self.assertEqual(d["passwordPolicy"], "length(14) and digits(1)")
        self.assertEqual(d["otpPolicyDigits"], 8)
        self.assertEqual(d["ssoSessionIdleTimeout"], 900)
        # Inchangés en service, modifiés dans le dépôt : valeur du dépôt.
        self.assertEqual(d["otpPolicyPeriod"], 60)
        self.assertEqual(d["failureFactor"], 3)
        # Le journal nomme les paramètres conservés, jamais leurs valeurs.
        self.assertIn("passwordPolicy", self.journal)
        self.assertNotIn("length(14)", self.journal)

    def test_politique_sans_base_valeur_en_service(self):
        service = domaine(PERMS, passwordPolicy="length(16)", otpPolicyAlgorithm="HmacSHA256")
        depot = domaine(PERMS, passwordPolicy="length(12)", otpPolicyAlgorithm="HmacSHA1", maxDeltaTimeSeconds=43200)
        d = self.fusion(depot, service)
        self.assertEqual(d["passwordPolicy"], "length(16)")
        self.assertEqual(d["otpPolicyAlgorithm"], "HmacSHA256")
        # Absent du domaine en service : valeur du dépôt.
        self.assertEqual(d["maxDeltaTimeSeconds"], 43200)

    def test_parametres_hors_politique_suivent_le_depot(self):
        base = domaine(PERMS, displayName="Ancien", accessTokenLifespan=300)
        service = domaine(PERMS, displayName="Modifié", accessTokenLifespan=600)
        depot = domaine(PERMS, displayName="Nouveau", accessTokenLifespan=300)
        d = self.fusion(depot, service, base)
        self.assertEqual(d["displayName"], "Nouveau")
        self.assertEqual(d["accessTokenLifespan"], 300)


if __name__ == "__main__":
    unittest.main()
