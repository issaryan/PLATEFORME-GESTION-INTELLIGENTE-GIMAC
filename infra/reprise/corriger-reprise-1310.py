#!/usr/bin/env python3
"""Correctifs de la reprise de l'existant du 13 au 15/10/2026 (D-107) : propositions, puis fichiers d'import.

Aucune écriture ici. L'outil lit les exports en lecture seule de MS-PARTICIPANT (`institutions.csv` : code_membre, id,
nom, statut, categorie, pays) et de MS-HOMOLOGATION (`liaisons.csv` : reference_origine, institution_id,
institution_nom, type_projet, etat, date_ouverture, origine_etat), et le relevé du portail de l'ancienne sandbox
(`referentiels-portail.txt`). Deux commandes :

    # 1. propositions : une ligne par donnée à corriger, avec la valeur proposée quand elle se déduit des sources
    python3 infra/reprise/corriger-reprise-1310.py propositions --institutions institutions.csv --liaisons liaisons.csv \\
        --portail <dossier portail de l'audit> --sortie propositions.csv

    # 2. fichiers : les seules lignes retenues par l'utilisateur (decisions.csv : nature;cle;champ;valeur) deviennent
    #    des fichiers d'import de infra/reprise/importer-reprise.py, joués d'abord en simulation
    python3 infra/reprise/corriger-reprise-1310.py fichiers --institutions institutions.csv --liaisons liaisons.csv \\
        --decisions decisions.csv --sortie correctifs/
    python3 infra/reprise/importer-reprise.py institutions correctifs/institutions.json          # simulation
    python3 infra/reprise/importer-reprise.py liaisons correctifs/liaisons.json                  # simulation

Les routes de reprise sont idempotentes : une institution par son code membre, une liaison par sa référence d'origine ;
rejouer les mêmes fichiers rend « inchangée ». Ce qu'aucune route ne permet est proposé sans fichier d'import
(action « sans route ») : clore sans suite une liaison, redater une opération déjà faite, revenir sur une mise en
production.
"""
import argparse
import csv
import json
import pathlib
import re
import sys

CATEGORIES = {"BANK": "BANQUE", "MICROFINANCE": "MICROFINANCE", "MNO": "EMETTEUR_MONNAIE_ELECTRONIQUE", "MTC": "PARTENAIRE_REMITTANCE",
              "MERCHANTS AGGREGATOR": "AGREGATEUR", "BILLER": "FACTURIER"}
PAYS = {"CAMEROON": "CM", "RCA": "CF", "CONGO B": "CG", "GABON": "GA", "GUINEA": "GQ", "CHAD": "TD"}
COLONNES = ("nature", "cle", "champ", "valeur_actuelle", "valeur_proposee", "action", "motif")
# origineEtat : 100 caractères au plus (docs/exploitation/reprise-des-donnees.md §9).
ORIGINE_PRODUCTION = "Payway production au 13/10/2026 ; date réelle de production inconnue (date de reprise)"
CHAMPS_INSTITUTION = {"categorie", "pays", "nom"}
CHAMPS_LIAISON = {"origineEtat", "intitule"}


class ErreurCorrectif(Exception):
    """Fichier absent, illisible ou incohérent."""


def lire_csv(chemin, colonnes, separateur=","):
    try:
        with open(chemin, encoding="utf-8-sig", newline="") as f:
            lignes = list(csv.DictReader(f, delimiter=separateur))
    except OSError as e:
        raise ErreurCorrectif(f"fichier illisible : {chemin} ({e})")
    manquantes = sorted(set(colonnes) - set(lignes[0] if lignes else []))
    if manquantes:
        raise ErreurCorrectif(f"colonnes absentes de {chemin} : {', '.join(manquantes)}")
    return lignes


def normaliser(t):
    return " ".join(str(t or "").split()).upper()


def portail(dossier):
    """Organisations et membres du relevé : {code: (nom, type, pays)} pour chaque section."""
    try:
        texte = (pathlib.Path(dossier) / "referentiels-portail.txt").read_text(encoding="utf-8")
    except OSError as e:
        raise ErreurCorrectif(f"relevé du portail illisible : {dossier} ({e})")
    organisations, membres, section = {}, {}, None
    for ligne in texte.splitlines():
        if ligne.startswith("== ORGANISATIONS"):
            section = organisations
        elif ligne.startswith("== MEMBRES"):
            section = membres
        elif ligne.startswith("=="):
            section = None
        elif section is not None and re.match(r"^\d{5}\|", ligne):
            c = ligne.split("|")
            section[c[0]] = (c[1].strip(), c[2].strip(), c[3].strip() if section is membres and len(c) > 3 else "")
    return organisations, membres


def propositions_institutions(institutions, organisations, membres):
    lignes = []
    for i in sorted(institutions, key=lambda x: x["code_membre"]):
        code = i["code_membre"]
        org, mem = organisations.get(code), membres.get(code)
        co = CATEGORIES.get(normaliser(org[1])) if org else None
        cm = CATEGORIES.get(normaliser(mem[1])) if mem else None
        if co and cm and co != cm:
            lignes.append(("institution", code, "categorie", i["categorie"], "", "à décider (DDRM)",
                           f"organisations « {org[1]} », members « {mem[1]} » : la reprise a retenu {i['categorie']}"))
        elif co and co == cm and co != i["categorie"]:
            lignes.append(("institution", code, "categorie", i["categorie"], co, "proposé", "organisations et members du portail concordent, la reprise diffère"))
        elif (co or cm) and not (co and cm) and (co or cm) != i["categorie"]:
            # Une seule source : members classe « BANK » des opérateurs (41002 Moov Gabon) ; jamais proposé d'office.
            lignes.append(("institution", code, "categorie", i["categorie"], co or cm, "à vérifier (une source)",
                           f"seul {'organisations' if co else 'members'} donne une catégorie, différente de la reprise"))
        pays = PAYS.get(normaliser(mem[2])) if mem else None
        if pays and pays != i["pays"]:
            lignes.append(("institution", code, "pays", i["pays"], pays, "proposé",
                           f"members « {mem[2]} » ; la reprise a déduit le pays du premier chiffre du code"))
        if org and org[0] and normaliser(org[0]) != normaliser(i["nom"]):
            lignes.append(("institution", code, "nom", i["nom"], org[0], "à décider (DDRM)", "nom des organisations du portail différent du registre"))
    return lignes


def propositions_liaisons(liaisons):
    lignes = []
    par_institution = {}
    for l in liaisons:
        par_institution.setdefault(l["institution_id"], []).append(l)
    for l in sorted(liaisons, key=lambda x: x["reference_origine"]):
        ref, etat, origine = l["reference_origine"], l["etat"], l["origine_etat"]
        if ref.startswith("REPRISE-"):
            if origine.startswith("Payway production") and origine != ORIGINE_PRODUCTION:
                lignes.append(("liaison", ref, "origineEtat", origine, ORIGINE_PRODUCTION, "proposé",
                               "liaison par défaut en production : la date du 13/10/2026 est celle de la reprise, pas un fait"))
            elif etat != "CLOSE":
                lignes.append(("liaison", ref, "statut", etat, "CLOSE_SANS_SUITE", "sans route",
                               "liaison par défaut sans projet connu (PayWay test) : à clore ou à requalifier ; aucune route de clôture sans suite"))
            if len(par_institution[l["institution_id"]]) > 1:
                autres = sorted(x["reference_origine"] for x in par_institution[l["institution_id"]] if x is not l)
                lignes.append(("liaison", ref, "doublon", "", "", "à décider (DES)",
                               f"liaison par défaut à côté de {', '.join(autres)} : risque de rattachement des campagnes reprises"))
        if etat == "CLOSE" and origine.startswith("Portail sandbox COMPLETED"):
            lignes.append(("liaison", ref, "statut", "EN_PRODUCTION", "CERTIFIEE", "sans route",
                           "production déduite d'une certification terminée sur le portail : à vérifier dans PayWay production"))
    return lignes


def ecrire_propositions(chemin, lignes):
    with open(chemin, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";")
        w.writerow(COLONNES)
        w.writerows(lignes)


def fichiers(institutions, liaisons, decisions):
    """Fichiers d'import des seules décisions retenues ; chaque fiche reprend les champs obligatoires de l'export."""
    par_code = {i["code_membre"]: i for i in institutions}
    par_ref = {l["reference_origine"]: l for l in liaisons}
    code_de = {i["id"]: i["code_membre"] for i in institutions}
    corrections_i, corrections_l, refus = {}, {}, []
    for d in decisions:
        nature, cle, champ, valeur = d["nature"].strip(), d["cle"].strip(), d["champ"].strip(), d["valeur"].strip()
        if nature == "institution" and champ in CHAMPS_INSTITUTION and cle in par_code and valeur:
            corrections_i.setdefault(cle, {})[champ] = valeur
        elif nature == "liaison" and champ in CHAMPS_LIAISON and cle in par_ref and valeur:
            corrections_l.setdefault(cle, {})[champ] = valeur
        else:
            refus.append(f"{nature};{cle};{champ} : décision sans route d'import, clé inconnue ou valeur vide")
    lot_i = []
    for code in sorted(corrections_i):
        i = par_code[code]
        fiche = {"codeMembre": code, "id": i["id"], "nom": i["nom"], "pays": i["pays"], "categorie": i["categorie"], "statut": i["statut"]}
        fiche.update(corrections_i[code])
        lot_i.append(fiche)
    lot_l = []
    for ref in sorted(corrections_l):
        l = par_ref[ref]
        lot_l.append({"referenceOrigine": ref, "codeMembre": code_de.get(l["institution_id"]), "typeProjet": l["type_projet"], **corrections_l[ref]})
    motif = "Correctif de la reprise du 13 au 15/10/2026, décision de l'utilisateur"
    return {"motif": motif, "institutions": lot_i}, {"liaisons": lot_l}, refus


def arguments(argv):
    p = argparse.ArgumentParser(description="Correctifs de la reprise du 13 au 15/10/2026 (aucune écriture)")
    s = p.add_subparsers(dest="commande", required=True)
    for nom in ("propositions", "fichiers"):
        c = s.add_parser(nom)
        c.add_argument("--institutions", required=True)
        c.add_argument("--liaisons", required=True)
        c.add_argument("--sortie", required=True)
        if nom == "propositions":
            c.add_argument("--portail", required=True)
        else:
            c.add_argument("--decisions", required=True)
    return p.parse_args(argv)


def main(argv=None, sortie=sys.stdout):
    a = arguments(sys.argv[1:] if argv is None else argv)
    try:
        institutions = lire_csv(a.institutions, ("code_membre", "id", "nom", "statut", "categorie", "pays"))
        liaisons = lire_csv(a.liaisons, ("reference_origine", "institution_id", "type_projet", "etat", "origine_etat"))
        if a.commande == "propositions":
            organisations, membres = portail(a.portail)
            lignes = propositions_institutions(institutions, organisations, membres) + propositions_liaisons(liaisons)
            ecrire_propositions(a.sortie, lignes)
            compte = {}
            for l in lignes:
                compte[(l[0], l[2], l[5])] = compte.get((l[0], l[2], l[5]), 0) + 1
            for (nature, champ, action), n in sorted(compte.items()):
                print(f"  {nature:<12}{champ:<13}{action:<26}{n:>5}", file=sortie)
            print(f"{len(lignes)} proposition(s) dans {a.sortie} ; aucune écriture.", file=sortie)
            return 0
        decisions = lire_csv(a.decisions, ("nature", "cle", "champ", "valeur"), ";")
        lot_i, lot_l, refus = fichiers(institutions, liaisons, decisions)
        racine = pathlib.Path(a.sortie)
        racine.mkdir(parents=True, exist_ok=True)
        for nom, contenu, n in (("institutions.json", lot_i, len(lot_i["institutions"])), ("liaisons.json", lot_l, len(lot_l["liaisons"]))):
            if n:
                (racine / nom).write_text(json.dumps(contenu, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
            print(f"  {nom} : {n} ligne(s)", file=sortie)
        for r in refus:
            print(f"  ! {r}", file=sortie)
        print("Fichiers écrits ; à jouer d'abord en simulation par importer-reprise.py.", file=sortie)
        return 1 if refus else 0
    except ErreurCorrectif as e:
        print(f"ERREUR : {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
