"""Tests de creer-comptes-personnels.py : création multi-profils, alignement, suppression (Keycloak simulé)."""
import contextlib
import importlib.util
import io
import json
import os
import pathlib
import sys
import tempfile
import unittest
import urllib.parse
from unittest import mock

ICI = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location("creer_comptes", os.path.join(ICI, "creer-comptes-personnels.py"))
cc = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cc)

ADMIN = "http://kc/admin/realms/gimac"


class KeycloakSimule:
    """Domaine minimal : utilisateurs, rôles du domaine et leurs attributions."""

    def __init__(self, utilisateurs=None):
        self.utilisateurs = {}
        self.roles = {}
        self.suite = 0
        for u in utilisateurs or []:
            self.ajouter(u["username"], u.get("roles", []), u.get("attributes", {}))

    def ajouter(self, nom, roles=(), attributs=None):
        self.suite += 1
        uid = f"id-{self.suite}"
        self.utilisateurs[uid] = {"id": uid, "username": nom, "attributes": dict(attributs or {})}
        self.roles[uid] = list(roles)
        return uid

    def par_nom(self, nom):
        return next((u for u in self.utilisateurs.values() if u["username"] == nom), None)

    def appel(self, methode, url, jeton=None, corps=None, formulaire=None):
        if url.endswith("/protocol/openid-connect/token"):
            return 200, {"access_token": "jeton"}, {}
        chemin = url[len(ADMIN):]
        if methode == "GET" and chemin.startswith("/users?"):
            nom = urllib.parse.parse_qs(chemin.split("?", 1)[1])["username"][0]
            u = self.par_nom(nom)
            return 200, [u] if u else [], {}
        if methode == "POST" and chemin == "/users":
            uid = self.ajouter(corps["username"], (), corps.get("attributes"))
            self.utilisateurs[uid]["creation"] = corps
            return 201, None, {"Location": f"{ADMIN}/users/{uid}"}
        if chemin.startswith("/roles/"):
            return 200, {"name": chemin.rsplit("/", 1)[1]}, {}
        partie = chemin.split("/")
        uid = partie[2] if len(partie) > 2 else None
        if chemin.endswith("/role-mappings/realm"):
            if methode == "GET":
                return 200, [{"name": r} for r in self.roles[uid]], {}
            if methode == "POST":
                self.roles[uid] += [r["name"] for r in corps]
                return 204, None, {}
            if methode == "DELETE":
                retires = {r["name"] for r in corps}
                self.roles[uid] = [r for r in self.roles[uid] if r not in retires]
                return 204, None, {}
        if chemin.endswith("/reset-password"):
            self.utilisateurs[uid]["mot_de_passe_fixe"] = True
            return 204, None, {}
        if methode == "GET" and chemin == f"/users/{uid}":
            return 200, json.loads(json.dumps(self.utilisateurs[uid])), {}
        if methode == "PUT" and chemin == f"/users/{uid}":
            self.utilisateurs[uid]["attributes"] = corps["attributes"]
            return 204, None, {}
        if methode == "DELETE" and chemin == f"/users/{uid}":
            del self.utilisateurs[uid]
            return 204, None, {}
        raise AssertionError(f"appel inattendu : {methode} {url}")


class CreerComptesPersonnels(unittest.TestCase):
    def lancer(self, keycloak, comptes=None, arguments=()):
        with tempfile.TemporaryDirectory() as d:
            local = pathlib.Path(d)
            if comptes is not None:
                (local / "comptes.json").write_text(json.dumps(comptes))
            env = local / ".env"
            env.write_text("GIMAC_PORT_KEYCLOAK=9080\nGIMAC_KEYCLOAK_ADMIN_PASSWORD=x\n")
            sortie = io.StringIO()
            with mock.patch.object(cc, "LOCAL", local), mock.patch.object(cc, "ENV", env), \
                    mock.patch.object(cc, "RACINE", local), mock.patch.object(cc, "appel", keycloak.appel), \
                    mock.patch.dict(os.environ, {"KEYCLOAK_URL": "http://kc"}), \
                    mock.patch.object(sys, "argv", ["creer-comptes-personnels.py", *arguments]), \
                    contextlib.redirect_stdout(sortie):
                cc.main()
            fichier = local / "mots-de-passe-provisoires.txt"
            lignes = fichier.read_text().splitlines() if fichier.exists() else []
            droits = oct(fichier.stat().st_mode & 0o777) if fichier.exists() else None
            return sortie.getvalue(), lignes, droits

    def test_creation_avec_plusieurs_profils_et_telephone(self):
        kc = KeycloakSimule()
        sortie, lignes, droits = self.lancer(kc, [{
            "identifiant": "martial.sossy", "email": "martial.sossy@gimac-afr.org", "prenom": "Martial",
            "nom": "SOSSY", "profils": ["ROLE_SNA", "ROLE_RESPONSABLE_SERVICE"], "fonction": "Chef SNA",
            "telephone": "+237 652364304"}])
        u = kc.par_nom("martial.sossy")
        self.assertEqual(kc.roles[u["id"]], ["ROLE_SNA", "ROLE_RESPONSABLE_SERVICE"])
        self.assertEqual(u["attributes"]["phone_number"], ["+237 652364304"])
        self.assertEqual(u["attributes"]["gimac_actions_requises"], cc.ACTIONS)
        self.assertTrue(u["mot_de_passe_fixe"])
        self.assertIn("créé : martial.sossy (ROLE_SNA, ROLE_RESPONSABLE_SERVICE)", sortie)
        self.assertEqual(len(lignes), 1)
        self.assertTrue(lignes[0].startswith("martial.sossy\tmartial.sossy@gimac-afr.org\tROLE_SNA,ROLE_RESPONSABLE_SERVICE\t"))
        self.assertEqual(droits, "0o600")

    def test_profil_unique_toujours_accepte_et_institution(self):
        kc = KeycloakSimule()
        self.lancer(kc, [{"identifiant": "c.b", "email": "c@b", "prenom": "C", "nom": "B",
                          "profil": "ROLE_CORRESPONDANT", "institution_id": "INST-1"}])
        u = kc.par_nom("c.b")
        self.assertEqual(kc.roles[u["id"]], ["ROLE_CORRESPONDANT"])
        self.assertEqual(u["attributes"]["institution_id"], ["INST-1"])

    def test_compte_existant_laisse_tel_quel_sans_aligner(self):
        kc = KeycloakSimule([{"username": "ryan", "roles": ["ROLE_SUPER_ADMIN"]}])
        sortie, lignes, _ = self.lancer(kc, [{"identifiant": "ryan", "profils": ["ROLE_SNA"]}])
        self.assertIn("déjà présent : ryan", sortie)
        self.assertEqual(kc.roles[kc.par_nom("ryan")["id"]], ["ROLE_SUPER_ADMIN"])
        self.assertEqual(lignes, [])

    def test_aligner_ramene_les_profils_et_garde_les_autres_roles(self):
        kc = KeycloakSimule([{"username": "otsimi.yarel", "roles": ["ROLE_CHARGE_DDRM", "offline_access"],
                              "attributes": {"fonction": ["CDDRM"], "locale": ["fr"]}}])
        sortie, _, _ = self.lancer(kc, [{"identifiant": "otsimi.yarel", "aligner": True,
                                         "profils": ["ROLE_PRODUCTION_MONETIQUE"], "fonction": "Back office",
                                         "telephone": "+237 1"}])
        u = kc.par_nom("otsimi.yarel")
        self.assertEqual(sorted(kc.roles[u["id"]]), ["ROLE_PRODUCTION_MONETIQUE", "offline_access"])
        self.assertEqual(u["attributes"]["fonction"], ["Back office"])
        self.assertEqual(u["attributes"]["phone_number"], ["+237 1"])
        self.assertEqual(u["attributes"]["locale"], ["fr"])
        self.assertIn("aligné : otsimi.yarel (+ROLE_PRODUCTION_MONETIQUE -ROLE_CHARGE_DDRM)", sortie)

    def test_aligner_sans_ecart_ne_dit_rien(self):
        kc = KeycloakSimule([{"username": "ryan", "roles": ["ROLE_SUPER_ADMIN"]}])
        sortie, _, _ = self.lancer(kc, [{"identifiant": "ryan", "aligner": True, "profils": ["ROLE_SUPER_ADMIN"]}])
        self.assertEqual(sortie.strip(), "")
        self.assertEqual(kc.roles[kc.par_nom("ryan")["id"]], ["ROLE_SUPER_ADMIN"])

    def test_suppression_explicite(self):
        kc = KeycloakSimule([{"username": "test-dg"}, {"username": "ryan"}])
        sortie, _, _ = self.lancer(kc, arguments=["--supprimer", "test-dg", "absent"])
        self.assertIsNone(kc.par_nom("test-dg"))
        self.assertIsNotNone(kc.par_nom("ryan"))
        self.assertIn("supprimé : test-dg", sortie)
        self.assertIn("absent : absent", sortie)

    def test_mot_de_passe_conforme_a_la_politique(self):
        for _ in range(50):
            m = cc.mot_de_passe()
            self.assertEqual(len(m), 16)
            self.assertTrue(any(c.isupper() for c in m) and any(c.islower() for c in m) and any(c.isdigit() for c in m))
            self.assertTrue(any(c in "!@#%*-_" for c in m))


if __name__ == "__main__":
    unittest.main()
