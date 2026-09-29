# language: fr
# PI-GIMAC-01, HOM-01 D. Événement renommé selon D-37 (homologation.catalog-published.v1, sujet
# homologation.catalog) ; son inscription au registre relève de MS-AUDIT et se vérifie en recette (E).
Fonctionnalité: HOM-01 Publication d'une version du référentiel d'opérations

  Contexte:
    Étant donné un utilisateur authentifié avec le rôle "INGENIEUR_HOMOLOGATION"

  Plan du scénario: Nominal : publication d'une version du référentiel
    Étant donné un brouillon de référentiel du type "<type>" dont chaque opération porte un code, un libellé, une phase, un segment, ses prédécesseurs, la partie à qui elle incombe, le service rattaché, la nature de couverture et la preuve attendue par catégorie de participant
    Quand l'ingénieur d'homologation demande la publication du brouillon
    Alors une version numérotée et datée est créée à l'état "PUBLIEE" et devient immuable
    Et l'événement "homologation.catalog-published.v1" est émis sur le sujet "homologation.catalog" à destination du registre d'audit

    Exemples:
      | type                |
      | CARTE_INTERBANCAIRE |
      | PAIEMENT_MOBILE     |
      | DELEGATAIRE         |

  Scénario: Alternatif : évolution d'une version publiée
    Étant donné une version publiée du référentiel "CARTE_INTERBANCAIRE"
    Quand un ingénieur tente de modifier l'opération "CI-AMONT-01" de cette version
    Alors la réponse a le statut 409 et le code "HOM_REFERENTIAL_FROZEN"
    Et la réponse propose la création d'un brouillon dérivé de cette version

  Scénario: Erreur : opération incomplète
    Étant donné un brouillon dont l'opération "PM-BR-04" n'indique ni la partie à qui elle incombe ni la preuve attendue
    Quand la publication est demandée
    Alors la réponse a le statut 409 et le code "HOM_REFERENTIAL_INCOMPLETE"
    Et la réponse restitue pour "PM-BR-04" les attributs manquants "partie" et "preuveAttendue"
