# language: fr
# Étape 2 du parcours (CR-CERT rang 2, D-57) et PAR-02, PAR-03 : dossier d'adhésion, prononcé en
# maker-checker (chargé de la DDRM puis chef de la DDRM), clôture sans suite.
Fonctionnalité: Adhésion d'un candidat

  Contexte:
    Étant donné un candidat "Banque Adhésion" enregistré par la DDRM

  Scénario: Nominal : adhésion prononcée sur un dossier complet
    Étant donné toutes ses pièces obligatoires déposées et validées
    Et ses conditions d'adhésion directes arrêtées avec le code participant "BQ-ADH-001"
    Quand le chargé de la DDRM propose l'adhésion avec le motif "Dossier complet"
    Et le chef de la DDRM approuve la proposition après réauthentification forte
    Alors l'institution est à l'état "ADHERENT"
    Et l'événement "participant.status-changed.v1" est émis sur le sujet "participant.status"

  Scénario: Erreur : dossier incomplet
    Quand le chargé de la DDRM propose l'adhésion avec le motif "Dossier complet"
    Alors la réponse a le statut 409 et le code "PAR_PREREQUISITES_MISSING"
    Et la réponse restitue le prérequis manquant "PIECE_STATUTS"

  Scénario: Erreur : l'auteur de la proposition ne peut l'approuver
    Étant donné toutes ses pièces obligatoires déposées et validées
    Et ses conditions d'adhésion directes arrêtées avec le code participant "BQ-ADH-002"
    Quand le chargé de la DDRM propose l'adhésion avec le motif "Dossier complet"
    Et le chargé de la DDRM tente d'approuver sa propre proposition
    Alors la réponse a le statut 403 et le code "PAR_SEPARATION_OF_DUTIES"

  Scénario: Alternatif : proposition rejetée
    Quand le chargé de la DDRM propose la clôture sans suite avec le motif "Projet abandonné"
    Et le chef de la DDRM rejette la proposition avec le motif "Candidat relancé"
    Alors l'institution est à l'état "CANDIDAT"

  Scénario: Alternatif : clôture sans suite
    Quand le chargé de la DDRM propose la clôture sans suite avec le motif "Projet abandonné"
    Et le chef de la DDRM approuve la proposition après réauthentification forte
    Alors l'institution est à l'état "CLOS_SANS_SUITE"
