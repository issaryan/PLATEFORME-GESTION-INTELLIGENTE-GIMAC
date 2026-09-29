# language: fr
# CP-GIMAC-01 v2.0, PAR-01 ; étape 1 du parcours (CR-CERT rang 1). D-57 et D-58 : la prise de
# contact est enregistrée par la DDRM, qui porte le dossier jusqu'au prononcé de l'adhésion.
Fonctionnalité: PAR-01 Enregistrer une institution candidate

  Contexte:
    Étant donné un agent authentifié à deux facteurs avec le profil "CHARGE_DDRM"

  Plan du scénario: Nominal : enregistrement d'un candidat de chaque catégorie
    Quand il enregistre la prise de contact de "<nom>" de catégorie "<categorie>"
    Alors la fiche est créée à l'état "CANDIDAT" avec son dossier de pièces attendues
    Et l'événement "participant.institution-registered.v1" est émis sur le sujet "participant.institution"

    Exemples:
      | nom                    | categorie                     |
      | Banque B               | BANQUE                        |
      | Microfinance M         | MICROFINANCE                  |
      | Établissement A        | ETABLISSEMENT_PAIEMENT        |
      | Émetteur E             | EMETTEUR_MONNAIE_ELECTRONIQUE |
      | Trésor public          | TRESOR_PUBLIC                 |
      | Agrégateur G           | AGREGATEUR                    |
      | Fintech F              | FINTECH                       |
      | Éditeur de logiciel L  | EDITEUR_LOGICIEL              |

  Scénario: Erreur : code déjà attribué
    Étant donné une institution enregistrée avec le code d'identification "BQBBCMCX"
    Quand il enregistre une autre institution avec le code d'identification "BQBBCMCX"
    Alors la réponse a le statut 409 et le code "PAR_CODE_ALREADY_USED"

  Scénario: Sécurité : un profil sans la permission ne peut enregistrer
    Étant donné un agent authentifié à deux facteurs avec le profil "SNA"
    Quand il enregistre la prise de contact de "Banque Z" de catégorie "BANQUE"
    Alors la réponse a le statut 403
