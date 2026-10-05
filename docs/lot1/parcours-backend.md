# Parcours de la liaison, étapes 4 à 10 : partage entre MS-HOMOLOGATION et MS-CERTIFICATION

Ce document fixe la répartition des responsabilités, les échanges entre les deux services et les
événements qui les relient. Il complète `docs/lot1/correspondance.md` (routes attendues par l'interface)
et s'appuie sur les décisions D-56 à D-60 de REC-GIMAC-01.

**État (08/10/2026).** Les deux parts sont livrées. Depuis D-77 (`docs/lot1/modelisateur-processus.md`),
MS-HOMOLOGATION n'a plus qu'**un seul moteur** : le parcours d'une liaison est instancié depuis la version
publiée du référentiel à laquelle la liaison est rattachée, et les gestes, jalons, déclencheurs,
particularités et livrables sont lus sur chaque opération (§5). Règles OPA à permission unique des gestes du
parcours dans `lot1_authz.rego` ; le client `homologation-service` porte `INTERNAL_DOCUMENT_DEPOSIT` (livrables
du portail) et `INTERNAL_PILOTAGE_TASK_CREATE` (tâches de MS-PILOTAGE).

**Ce qui fait foi.** Le **référentiel publié** fait foi : chaque type de projet a sa version en vigueur
(DES-12, modélisateur), et chaque liaison suit la version à laquelle elle est rattachée (migration explicite,
`HOM_REFERENTIEL_MIGRATE`). Ni le fichier `parcours_referentiel_cr.csv` ni le serveur de substitution de
l'interface ne font plus foi : le CSV ne sert qu'à la reprise des données qui a créé la **version 1** de chaque
type (fidèle à l'annexe A du CR-CERT-GIMAC-01 v1.4), et le simulateur n'est qu'un outil de démonstration. La
version 1 compte, par type : le tronc commun amont et aval, rangs 3 à 35 (33 opérations ; 32 en paiement
mobile, qui n'a pas le rang 5 ; les rangs 1 et 2 sont tenus par MS-PARTICIPANT et jamais instanciés), et la
branche du type : carte interbancaire C1 à C10 (10 opérations, 43 au total), paiement mobile M1 à M17
(17 opérations, 49 au total), délégataire D1 à D34 (34 opérations, 67 au total). Le rang 27 a pour
prédécesseur explicite la dernière opération de la branche (C10, M17, D34). Les chemins et corps HTTP sont
ceux de `frontend/packages/lot1/src/services/http/parcours.ts` ; le service est la seule source des
décisions (actions permises, compteurs, couvertures).

## 1. Répartition

| Domaine | Service | Routes (préfixe service `/api/v1`) |
|---|---|---|
| Dossier numérique, étapes à six éléments, opérations instanciées du référentiel publié, RACI + E, preuves, livrables, validation (FIDO2), qualités, ressources (geste `AFFECTER_RESSOURCES`), blocages, couvertures (certification et mise en service), constat de couverture (geste `CONSTATER_COUVERTURE`), services supplémentaires, fiche de suivi et constat de mise en service (geste `FICHE_SUIVI`), tableaux de bord par fonction et pilotage, intervenants, portail (liaisons, dossier, tâches, coordonnées techniques (geste `COORDONNEES_TECHNIQUES`), livrables, fiche de suivi), modélisateur des référentiels et tâches de MS-PILOTAGE (D-77) | **MS-HOMOLOGATION** | `/liaisons/{id}/dossier`, `/liaisons/{id}/parcours/operations`, `/liaisons/{id}/ressources`, `/parcours/operations/{id}/{livrable,preuves,validation,intervenants}`, `/liaisons/{id}/blocages`, `/blocages/{id}/levee`, `/liaisons/{id}/couverture/constat`, `/liaisons/{id}/services-supplementaires`, `/tableaux-de-bord/{acteur,pilotage}`, `/intervenants`, `/liaisons/{id}/fiche-suivi{,/remise,/constat}`, `/portail/liaisons…`, `/portail/taches`, `/portail/operations/{id}/livrable` |
| Verrou 1 : procès-verbal de certification (rangs 18, 19 : établissement à 100 % de couverture, signatures GIMAC et participant avec FIDO2) ; verrou 2 : approbation de la sécurité (rangs 20 à 26 : dossier de changement, fiche de changement lue jamais refaite, attestations PCI DSS / ISO 27001 / GSMA / COBAC selon l'activité, vérification motivée et archivage, visas optionnels conformité et DG) ; portail (signature du procès-verbal, dépôt des attestations) | **MS-CERTIFICATION** | `/liaisons/{id}/proces-verbal{,/signature-gimac}`, `/liaisons/{id}/approbation-securite{,/dossier,/fiche,/fiche/lecture,/attestations/{type}/verification,/archivage,/visas/{nature}}`, `/portail/liaisons/{id}/proces-verbal{,/signature}`, `/portail/liaisons/{id}/approbation-securite`, `/portail/liaisons/{id}/attestations/{type}` |

Les routes de la passerelle (`/bff/{des,portail}/{homologation,certification}/**`) et les règles OPA
existent déjà pour les deux services (réécriture vers `/api/v1`, `/api/v1/portail` pour le portail) ;
toute route nouvelle est vérifiée dans `RoutesPresentationTest` et `lot1_authz.rego`.

## 2. Appels synchrones (jeton de service, `/api/v1/interne/**`, permissions `INTERNAL_*`)

- MS-CERTIFICATION → MS-HOMOLOGATION `GET /api/v1/interne/liaisons/{id}/contexte-certification`
  (`INTERNAL_HOMOLOGATION_READ`) : `{ liaisonId, reference, institutionId, institutionNom, typeProjet,
  fintech, statut, couvertureCertification: {taux, numerateur, denominateur}, couvertureConstatee: bool,
  servicesCouverts: [code], conducteurId, conducteurNom }`. Sert à établir le procès-verbal (100 % exigés,
  constat fait), à déterminer les attestations exigées et le cloisonnement du portail.

## 3. Événements (outbox, CloudEvents 1.0, consommateurs idempotents)

| Type | Émetteur → consommateur | Donnée | Effet côté consommateur |
|---|---|---|---|
| `homologation.coverage-established.v1` (sujet `homologation.liaison`) | HOM → CER | `{liaisonId, taux, servicesCouverts}` | Procès-verbal établissable (`ETABLIR` si 100 %, sinon `ETABLIR_BLOQUE`) |
| `certification.pv-established.v1` (sujet `certification.proces-verbal`) | CER → HOM | `{liaisonId, etabliPar, empreinte}` | Opérations de ce déclencheur closes avec la preuve (procès-verbal) ; rang 18 en version 1 |
| `certification.pv-signed.v1` | CER → HOM | `{liaisonId, signatures:[{fonction, signataire, signeLe}], empreinte}` | Opérations de ce déclencheur (et du procès-verbal établi) closes ; jalon `CERTIFIEE` (rang 19 en version 1) |
| `certification.change-record-constituted.v1` (sujet `certification.approbation`) | CER → HOM | `{liaisonId, documentRef, nom, empreinte}` | Opérations de ce déclencheur closes (rang 20) |
| `certification.change-record-referenced.v1` | CER → HOM | `{liaisonId, reference}` | Opérations de ce déclencheur closes si elles sont ouvertes (rang 26) ; à l'approbation, une telle opération ouverte est close par la fiche déjà référencée |
| `certification.attestations-archived.v1` | CER → HOM | `{liaisonId, attestations:[{type, documentRef}]}` | Opérations de ce déclencheur closes (rangs 21 et 22) |
| `certification.security-approved.v1` | CER → HOM | `{liaisonId, approuveeLe}` | Opérations de ce déclencheur closes (rangs 23 à 25), jalon `APPROUVEE_SECURITE` (porté par le rang 25) |

MS-HOMOLOGATION tient une **projection** de l'état des verrous par liaison (`etat_pv`, `etat_approbation`,
attestations à vérifier, visas attendus) alimentée par ces événements : ses tableaux de bord (compteurs
`pvASigner`, `pvAEtablir`, `dossiersChangement`, `attestationsAVerifier`, `approbationsEnAttente`,
`visasAttendus`…) et les tâches du portail (`pvASigner`, `attestationsAFournir`) s'en servent, sans
appel synchrone. Les événements portent `institutionid` (extension CloudEvents) pour le canal poussé du
portail ; aucun ne porte de secret ni de contenu de pièce.

## 4. Invariants communs

- Toute action est décidée par le service et reflétée dans `actionsPermises` ; le validateur (V) n'est
  jamais le responsable (R) d'une même opération (`HOM_VALIDATOR_IS_RESPONSIBLE`).
- Clés : une opération `cle` ne porte que des codes de contrôle (4 à 8 hexadécimaux) ; une opération de
  particularité `CEREMONIE_CLES` (C1 en version 1) porte la cérémonie (date, trois porteurs, trois codes) ; une opération `secret` ne porte qu'une trace ; aucune
  valeur de clé ni de secret n'est jamais acceptée ni conservée.
- Signatures (procès-verbal, visas, validation) : assertion FIDO2 vérifiée par MS-IAM
  (`/api/v1/reauthentification`, en-tête `X-Reauth-Assertion`), puis cachet horodaté (D-35) ; une signature
  est immuable.
- Portail : cloisonné à l'institution du jeton (`institution_id`) ; projection sans nom interne (les
  personnes du GIMAC apparaissent par leur fonction) ; 404 pour une liaison d'une autre institution.
- Codes de refus : ceux du simulateur (`HOM_*`, `CER_*`), en `application/problem+json` avec `code`.

## 5. Moteur unique (D-77)

- **Instanciation.** À l'ouverture, la liaison est rattachée à la version publiée de son type ; ses opérations
  (hors étapes 1 et 2) sont instanciées avec leur modèle recopié dans `parcours_operation` (libellé, geste,
  jalon, déclencheur, particularité, livrables, délai, responsable de référence, `version_id`) ;
  `parcours_liaison` porte `version_id` et `version_numero` (`version_cr` ne subsiste que pour l'historique).
  Qualités : R, V, C, I, E de l'opération, chaque fonction tenue par la personne désignée.
- **Échéances.** Au plus tôt sur le graphe : maximum des échéances des prédécesseurs plus le délai de
  l'opération, depuis la date de démarrage, en jours calendaires (auparavant : un jour de décalage puis cumul
  des délais dans l'ordre du référentiel).
- **Règles lues sur l'opération.** Geste (`DEPOSER_LIVRABLE`, `DEPOSER_PREUVE`, `CONSTATER_COUVERTURE`,
  `AFFECTER_RESSOURCES`, `COORDONNEES_TECHNIQUES`, `FICHE_SUIVI`, `EVENEMENT_EXTERNE`, `AUCUN` : point de passage
  clos dès qu'il devient éligible), jalon (statut atteint à la clôture : `APTE_CERTIFICATION`, `CERTIFIEE`,
  `APPROUVEE_SECURITE`, `EN_PRODUCTION`), déclencheur (sujet de MS-CERTIFICATION qui clôt l'opération),
  particularité (`ENTREE_ESSAIS`, `FIN_ESSAIS`, `CEREMONIE_CLES`, `SERVICE_SUPPLEMENTAIRE`), livrables (un
  livrable déposé par une fonction externe se dépose depuis le portail). Onglet du dossier : décision pour une
  opération à déclencheur, mise en service pour la fiche de suivi et le service supplémentaire.
- **Migration** d'une liaison vers la nouvelle version publiée : différentiel appliqué au parcours (ajoutées
  verrouillées puis éligibilité recalculée ; retirées supprimées si non commencées, sinon sans objet, une
  opération faite restant faite ; modifiées et non faites : modèle et qualités mis à jour) ; échéances
  replanifiées.
- **Tâches de MS-PILOTAGE** (`POST /api/v1/interne/taches`, `/cloture`, `/reouverture`, jeton
  `homologation-service`) : au conducteur à l'ouverture et à la désignation (« Affecter les intervenants de la
  liaison »), à R quand l'opération devient éligible, à V quand la validation est demandée ; terminées ou
  rouvertes avec l'opération. Demandes écrites dans la transaction du geste (`demande_tache_pilotage`) et
  relayées avec réessai : MS-PILOTAGE indisponible ne bloque jamais le geste.
