# language: fr
Fonctionnalité: Étape 3, cadrage : ouverture d'une liaison sur un adhérent (HOM-04 adapté par D-57 et D-58)
  La liaison s'ouvre au cadrage sur une institution dont l'adhésion a été prononcée. Un chef de
  département désigne le conducteur de projet parmi les détenteurs du rôle CONDUCTEUR_PROJET. Le
  périmètre contractualisé est dérivé des services souscrits ; l'ouverture est publiée vers
  MS-PARTICIPANT, qui fait passer l'adhérent à l'état « en homologation ».

  Contexte:
    Étant donné une version publiée du référentiel de cadrage "PAIEMENT_MOBILE"
    Et un chef de département authentifié à deux facteurs

  Scénario: Nominal : ouverture d'une liaison sur un adhérent
    Étant donné l'institution "Établissement de paiement Recette" à l'état "ADHERENT" qui a souscrit "PAIEMENT_MOBILE"
    Quand le chef de département ouvre une liaison "PAIEMENT_MOBILE" en désignant le conducteur "Aminata Ngono"
    Alors la liaison est créée à l'état "OUVERTE" en phase "CADRAGE"
    Et son périmètre contractualisé comprend "PAIEMENT_MOBILE" et "TRANSVERSE"
    Et l'événement "homologation.liaison-opened.v1" est prêt pour MS-PARTICIPANT

  Scénario: Le conducteur désigné retrouve la liaison dans ses projets
    Étant donné l'institution "Établissement de paiement Projets" à l'état "ADHERENT" qui a souscrit "PAIEMENT_MOBILE"
    Quand le chef de département ouvre une liaison "PAIEMENT_MOBILE" en désignant le conducteur "Aminata Ngono"
    Alors la conductrice "Aminata Ngono" voit la liaison parmi ses projets
    Et le conducteur "Paul Essomba" ne la voit pas parmi ses projets

  Scénario: Redésignation du conducteur par le chef de département
    Étant donné l'institution "Établissement de paiement Relève" à l'état "ADHERENT" qui a souscrit "PAIEMENT_MOBILE"
    Et le chef de département a ouvert une liaison "PAIEMENT_MOBILE" en désignant le conducteur "Aminata Ngono"
    Quand le chef de département désigne le conducteur "Paul Essomba" avec le motif "Réaffectation"
    Alors le conducteur de la liaison est "Paul Essomba"

  Plan du scénario: Refus : institution non éligible, liaison en double ou conducteur non habilité
    Étant donné <situation>
    Quand l'ouverture de la liaison est demandée
    Alors l'ouverture est refusée avec le code "<code>"

    Exemples:
      | situation                                                           | code                                     |
      | une institution à l'état "CANDIDAT"                                 | HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE     |
      | un adhérent qui a déjà une liaison "PAIEMENT_MOBILE" active         | HOM_LIAISON_DUPLICATE                    |
      | un adhérent et un conducteur sans le rôle CONDUCTEUR_PROJET         | HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED |

  Scénario: Un ingénieur sans droit de désignation n'ouvre pas de liaison
    Étant donné l'institution "Établissement de paiement Droits" à l'état "ADHERENT" qui a souscrit "PAIEMENT_MOBILE"
    Quand un ingénieur d'homologation sans droit de désignation ouvre une liaison "PAIEMENT_MOBILE"
    Alors l'ouverture est refusée avec le code "HOM_PROJECT_DESIGNATION_FORBIDDEN"
