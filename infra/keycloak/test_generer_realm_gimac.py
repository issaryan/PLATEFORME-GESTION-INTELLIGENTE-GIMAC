"""Tests du générateur du domaine Keycloak « gimac » : profils selon le CR-CERT-GIMAC-01 v1.7 (missions, ouverture
des liaisons par la DDRM, agent de la sécurité des SI) et cohérence des fichiers générés avec le générateur.

    python3 -m unittest -q infra/keycloak/test_generer_realm_gimac.py
"""
import importlib.util
import json
import pathlib
import unittest

ICI = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("generer_realm_gimac", ICI / "generer-realm-gimac.py")
G = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(G)


class ProfilsCrV17(unittest.TestCase):
    def test_ouverture_des_liaisons_reservee_a_la_ddrm(self):
        self.assertEqual([n for n, ps in G.PROFILS.items() if "HOM_LIAISON_OPEN" in ps], ["ROLE_CHEF_DDRM"])
        # Les deux chefs désignent le conducteur (rang 6) ; le chef du DES garde la certification.
        for chef in ("ROLE_CHEF_DES", "ROLE_CHEF_DDRM"):
            self.assertIn("HOM_PROJECT_DESIGNATE", G.PROFILS[chef])
        self.assertIn("CER_QUITUS_TECHNIQUE_SIGN", G.PROFILS["ROLE_CHEF_DES"])

    def test_agent_ssi(self):
        ps = G.PROFILS["ROLE_AGENT_SSI"]
        for p in ("CER_SECURITY_VERIFY", "HOM_EVIDENCE_SUBMIT", "HOM_LIAISON_READ", "COL_MESSAGE_SEND", "PIL_ACTIVITY_OWN"):
            self.assertIn(p, ps)
        self.assertEqual([p for p in ps if p.startswith(("IAM_", "PLATFORM_", "CER_QUITUS_", "PIL_TEAM"))], [])
        self.assertNotIn("ROLE_AGENT_SSI", G.RESPONSABLES)
        self.assertNotIn("ROLE_AGENT_SSI", G.PROFILS_EXTERNES)
        self.assertIn(("test-agent-ssi", "Agent", "Sécurité des SI", "ROLE_AGENT_SSI", None), G.UTILISATEURS)
        self.assertEqual(G.PRESENTATIONS["ROLE_AGENT_SSI"][0], "Agent de la sécurité des SI")

    def test_perimetre_de_lecture_approuve_par_le_rssi(self):
        # CC-SBX-GIMAC-02 §D.8 et UC-LEC-10 : le périmètre du compte de service est approuvé par le RSSI
        # (SBX_LEC_VALIDATION), profil interne ; aucun profil externe ne porte de validation (RG-05).
        self.assertEqual([n for n, ps in G.PROFILS.items() if "SBX_LEC_VALIDATION" in ps], ["ROLE_RSSI"])
        self.assertNotIn("ROLE_RSSI", G.PROFILS_EXTERNES)
        for cible in (G.CIBLE, G.CIBLE_PRODUCTION):
            realm = json.loads(cible.read_text(encoding="utf-8"))
            rssi = next(r for r in realm["roles"]["realm"] if r["name"] == "ROLE_RSSI")
            self.assertIn("SBX_LEC_VALIDATION", rssi["composites"]["realm"])

    def test_developpeur_execute_les_cas_affectes(self):
        # CA-SBX-GIMAC-02 UC-CER-04 : développeur et testeur sont les acteurs principaux de l'exécution d'un cas.
        for profil in ("ROLE_SBX_DEVELOPPEUR", "ROLE_SBX_TESTEUR"):
            self.assertIn("SBX_CER_EXECUTION", G.PROFILS[profil])
            self.assertIn("SBX_CER_CONSULTATION", G.PROFILS[profil])

    def test_missions_selon_le_cr(self):
        m = {k: v[1] for k, v in G.PRESENTATIONS.items()}
        self.assertIn("Coordonnateur fonctionnel", m["ROLE_CHEF_DDRM"])
        self.assertIn("Coordonnateur technique", m["ROLE_CHEF_DES"])
        self.assertNotIn("ouvre les liaisons", m["ROLE_CHEF_DES"])
        self.assertIn("conduit les essais service par service", m["ROLE_SNA"])
        self.assertIn("émet la demande de changement", m["ROLE_PRODUCTION_MONETIQUE"])
        self.assertIn("lien d'essai", m["ROLE_POLE_RESEAU"])
        self.assertIn("répertoires de dépôt", m["ROLE_POLE_SYSTEME"])
        self.assertIn("coordination opérationnelle", m["ROLE_CONDUCTEUR_PROJET"])
        for libelle, mission in G.PRESENTATIONS.values():
            self.assertTrue(1 <= len(libelle) <= 80 and len(mission) <= 500)
            self.assertNotIn("\u2014", libelle + mission)


class FichiersGeneres(unittest.TestCase):
    """Les fichiers versionnés sont ceux que produit le générateur (jamais édités à la main)."""

    def test_realm_et_catalogue_frontend(self):
        realm = json.loads(G.CIBLE.read_text(encoding="utf-8"))
        roles = {r["name"]: r for r in realm["roles"]["realm"]}
        self.assertEqual(roles["ROLE_AGENT_SSI"]["description"], G.PRESENTATIONS["ROLE_AGENT_SSI"][1])
        self.assertEqual(set(roles["ROLE_AGENT_SSI"]["composites"]["realm"]) - {"ROLE_USER"}, set(G.PROFILS["ROLE_AGENT_SSI"]))
        self.assertNotIn("HOM_LIAISON_OPEN", roles["ROLE_CHEF_DES"]["composites"]["realm"])
        front = json.loads(G.CIBLE_FRONTEND.read_text(encoding="utf-8"))
        self.assertEqual([p["code"] for p in front["profils"]], list(G.PROFILS))
        prod = json.loads(G.CIBLE_PRODUCTION.read_text(encoding="utf-8"))
        self.assertIn("ROLE_AGENT_SSI", {r["name"] for r in prod["roles"]["realm"]})
        self.assertFalse([u for u in prod["users"] if u["username"].startswith("test-")])


if __name__ == "__main__":
    unittest.main()
