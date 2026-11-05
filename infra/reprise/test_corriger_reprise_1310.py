#!/usr/bin/env python3
"""Tests de infra/reprise/corriger-reprise-1310.py : propositions et fichiers d'import, sur des données fictives.

    python3 -m unittest -q infra/reprise/test_corriger_reprise_1310.py
"""
import csv
import importlib.util
import io
import json
import pathlib
import tempfile
import unittest

ICI = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("corriger", ICI / "corriger-reprise-1310.py")
C = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(C)

INSTITUTIONS = [
    ("99001", "u1", "Banque fictive", "ACTIF", "EMETTEUR_MONNAIE_ELECTRONIQUE", "CM"),
    ("99002", "u2", "Opérateur fictif", "EN_HOMOLOGATION", "EMETTEUR_MONNAIE_ELECTRONIQUE", "CM"),
    ("99003", "u3", "Microfinance fictive", "EN_HOMOLOGATION", "BANQUE", "GQ"),
    ("99004", "u4", "Sans relevé", "ACTIF", "BANQUE", "CM"),
]
LIAISONS = [
    ("REPRISE-99001-MOBILE", "u1", "PAIEMENT_MOBILE", "CLOSE", "Payway production : membre actif au 13/10/2026"),
    ("REPRISE-99002-MOBILE", "u2", "PAIEMENT_MOBILE", "EN_HOMOLOGATION", "PayWay test : membre paramétré"),
    ("2026-99002-01", "u2", "PAIEMENT_MOBILE", "EN_HOMOLOGATION", "Portail sandbox ONGOING : 1/2 réussis"),
    ("2026-99003-01", "u3", "PAIEMENT_MOBILE", "CLOSE", "Portail sandbox COMPLETED : 2/2 réussis"),
    ("REPRISE-99004-CARTE", "u4", "CARTE_INTERBANCAIRE", "CLOSE", C.ORIGINE_PRODUCTION),
]
RELEVE = """== ORGANISATIONS (reference|nom|type|email|domaines|async|simule|cree)
99001|Banque fictive|MNO||banque.invalid|false|false|2025-01-01
99002|Opérateur fictif|||||false|true|2025-01-01
99003|Microfinance fictive|MICROFINANCE|||false|false|2025-01-01
== MEMBRES (code|nom|type|pays|services declares)
99001|Banque fictive|BANK|Cameroon|
99002|OPERATEUR|BANK|Cameroon|
99003|Microfinance fictive 2|MICROFINANCE|Gabon|
== PAYS HORS CEMAC
"""


def ecrire(dossier):
    d = pathlib.Path(dossier)
    with open(d / "institutions.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(("code_membre", "id", "nom", "statut", "categorie", "pays"))
        w.writerows(INSTITUTIONS)
    with open(d / "liaisons.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(("reference_origine", "institution_id", "institution_nom", "type_projet", "etat", "date_ouverture", "origine_etat"))
        w.writerows((r, i, "x", t, e, "2026-10-13 00:00:00", o) for r, i, t, e, o in LIAISONS)
    (d / "portail").mkdir()
    (d / "portail" / "referentiels-portail.txt").write_text(RELEVE, encoding="utf-8")
    return d


class CorrigerTest(unittest.TestCase):
    def setUp(self):
        self.t = tempfile.TemporaryDirectory()
        self.d = ecrire(self.t.name)

    def tearDown(self):
        self.t.cleanup()

    def args(self, commande, *extra):
        return [commande, "--institutions", str(self.d / "institutions.csv"), "--liaisons", str(self.d / "liaisons.csv"), *extra]

    def propositions(self):
        sortie = io.StringIO()
        code = C.main(self.args("propositions", "--portail", str(self.d / "portail"), "--sortie", str(self.d / "p.csv")), sortie)
        with open(self.d / "p.csv", encoding="utf-8") as f:
            return code, list(csv.DictReader(f, delimiter=";")), sortie.getvalue()

    def test_propositions(self):
        code, lignes, texte = self.propositions()
        self.assertEqual(code, 0)
        par = {(l["cle"], l["champ"]): l for l in lignes}
        self.assertEqual(par[("99001", "categorie")]["action"], "à décider (DDRM)")
        self.assertEqual(par[("99002", "categorie")]["action"], "à vérifier (une source)")
        self.assertEqual(par[("99003", "categorie")]["valeur_proposee"], "MICROFINANCE")
        self.assertEqual(par[("99003", "categorie")]["action"], "proposé")
        self.assertEqual((par[("99003", "pays")]["valeur_actuelle"], par[("99003", "pays")]["valeur_proposee"]), ("GQ", "GA"))
        self.assertEqual(par[("99002", "nom")]["action"], "à décider (DDRM)") if ("99002", "nom") in par else self.assertNotIn(("99001", "nom"), par)
        self.assertEqual(par[("REPRISE-99001-MOBILE", "origineEtat")]["valeur_proposee"], C.ORIGINE_PRODUCTION)
        self.assertLessEqual(len(C.ORIGINE_PRODUCTION), 100)
        self.assertEqual(par[("REPRISE-99002-MOBILE", "statut")]["action"], "sans route")
        self.assertIn("2026-99002-01", par[("REPRISE-99002-MOBILE", "doublon")]["motif"])
        self.assertEqual(par[("2026-99003-01", "statut")]["valeur_proposee"], "CERTIFIEE")
        self.assertNotIn(("REPRISE-99004-CARTE", "origineEtat"), par)
        self.assertNotIn("99004", {l["cle"] for l in lignes})
        self.assertIn("aucune écriture", texte)

    def test_fichiers_des_seules_decisions(self):
        (self.d / "decisions.csv").write_text(
            "nature;cle;champ;valeur\n"
            "institution;99003;pays;GA\n"
            "institution;99003;categorie;MICROFINANCE\n"
            "liaison;REPRISE-99001-MOBILE;origineEtat;\"" + C.ORIGINE_PRODUCTION + "\"\n"
            "liaison;REPRISE-99002-MOBILE;statut;CLOSE_SANS_SUITE\n"
            "institution;99009;pays;GA\n",
            encoding="utf-8",
        )
        sortie = io.StringIO()
        code = C.main(self.args("fichiers", "--decisions", str(self.d / "decisions.csv"), "--sortie", str(self.d / "c")), sortie)
        self.assertEqual(code, 1)
        i = json.loads((self.d / "c" / "institutions.json").read_text())
        self.assertEqual(i["institutions"], [{"codeMembre": "99003", "id": "u3", "nom": "Microfinance fictive", "pays": "GA", "categorie": "MICROFINANCE", "statut": "EN_HOMOLOGATION"}])
        l = json.loads((self.d / "c" / "liaisons.json").read_text())
        self.assertEqual(l["liaisons"], [{"referenceOrigine": "REPRISE-99001-MOBILE", "codeMembre": "99001", "typeProjet": "PAIEMENT_MOBILE", "origineEtat": C.ORIGINE_PRODUCTION}])
        self.assertIn("REPRISE-99002-MOBILE;statut : décision sans route", sortie.getvalue())
        self.assertIn("99009;pays", sortie.getvalue())

    def test_fichiers_vides_et_erreurs(self):
        (self.d / "decisions.csv").write_text("nature;cle;champ;valeur\ninstitution;99001;nom;Banque fictive SA\n", encoding="utf-8")
        sortie = io.StringIO()
        self.assertEqual(C.main(self.args("fichiers", "--decisions", str(self.d / "decisions.csv"), "--sortie", str(self.d / "c")), sortie), 0)
        self.assertFalse((self.d / "c" / "liaisons.json").exists())
        self.assertIn("liaisons.json : 0 ligne(s)", sortie.getvalue())
        self.assertEqual(C.main(["propositions", "--institutions", "/absent.csv", "--liaisons", "x", "--portail", "x", "--sortie", "x"], io.StringIO()), 2)
        (self.d / "mauvais.csv").write_text("a,b\n1,2\n", encoding="utf-8")
        self.assertEqual(C.main(["propositions", "--institutions", str(self.d / "mauvais.csv"), "--liaisons", "x", "--portail", "x", "--sortie", "x"], io.StringIO()), 2)
        self.assertEqual(C.main(self.args("propositions", "--portail", "/absent", "--sortie", str(self.d / "p.csv")), io.StringIO()), 2)


if __name__ == "__main__":
    unittest.main()
