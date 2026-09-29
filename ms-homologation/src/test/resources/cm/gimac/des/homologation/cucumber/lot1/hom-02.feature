# language: fr
# PI-GIMAC-01, HOM-02 D.
Fonctionnalité: HOM-02 Contrôle d'acyclicité et de cohérence du référentiel

  Contexte:
    Étant donné un utilisateur authentifié avec le rôle "INGENIEUR_HOMOLOGATION"

  Scénario: Nominal : graphe acyclique accepté
    Étant donné un brouillon dont les prédécesseurs forment un graphe orienté sans cycle
    Quand le contrôle de publication s'exécute
    Alors un ordre topologique de référence est calculé et enregistré avec la version
    Et la publication se poursuit jusqu'à l'état "PUBLIEE"

  Scénario: Alternatif : prédécesseur incohérent
    Étant donné un brouillon délégataire dont l'opération "DL-AMONT-03" du tronc commun amont déclare pour prédécesseur l'opération "DL-BR-02" de la branche délégataire
    Quand le contrôle de publication s'exécute
    Alors la publication est refusée avec l'anomalie "SEGMENT_INCOHERENT"
    Et les codes "DL-AMONT-03" et "DL-BR-02" sont restitués

  Scénario: Erreur : cycle détecté
    Étant donné trois opérations "DL-BR-01", "DL-BR-02" et "DL-BR-03" dont chacune désigne la suivante comme prédécesseur, la dernière désignant la première
    Quand le contrôle de publication s'exécute
    Alors la publication est refusée
    Et le cycle est restitué sous la forme de la suite ordonnée "DL-BR-01, DL-BR-02, DL-BR-03, DL-BR-01"
