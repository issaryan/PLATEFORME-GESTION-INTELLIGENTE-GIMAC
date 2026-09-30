# Parcours de la liaison, étapes 4 à 10 : partage entre MS-HOMOLOGATION et MS-CERTIFICATION

Ce document fixe la répartition des responsabilités, les échanges entre les deux services et les
événements qui les relient. Il complète `docs/lot1/correspondance.md` (routes attendues par l'interface)
et s'appuie sur les décisions D-56 à D-60 de REC-GIMAC-01.

**État (30/09/2026).** Les deux parts sont livrées ; les écarts assumés de MS-HOMOLOGATION au simulateur
sont listés dans `ms-homologation/README.md`. Règles OPA à permission unique des gestes du parcours dans
`lot1_authz.rego` ; le client `homologation-service` porte `INTERNAL_DOCUMENT_DEPOSIT` (livrables du portail).

**Spécification exécutable.** Le comportement attendu est celui du serveur de substitution de
l'interface, qui passe ses tests : `frontend/packages/lot1/src/services/simule/parcours.ts` (règles, refus,
compteurs, liens), `frontend/packages/lot1/src/modele/parcours.ts` (représentations),
`frontend/packages/lot1/src/modele/referentielCR.ts` (référentiel de l'annexe A : 36 rangs du tronc et
les branches carte C1 à C9, mobile M1 à M12, délégataire D1 à D34, porteurs, prédécesseurs, grains,
plateformes, attributs `cle`, `secret`, `valeurReference`, `oubliSansSymptome`, `pivot`), et les tests
`services/simule/{parcours,parcours-contrat,decision,parcours-portail,cadrage}.test.ts`. Les chemins et
corps HTTP sont ceux de `frontend/packages/lot1/src/services/http/parcours.ts`. En cas de doute, le
simulateur fait foi pour la règle ; le service est la seule source des décisions (actions permises,
compteurs, couvertures).

## 1. Répartition

| Domaine | Service | Routes (préfixe service `/api/v1`) |
|---|---|---|
| Dossier numérique, étapes à six éléments, opérations instanciées du référentiel, RACI + E, preuves, livrables, validation (FIDO2), qualités, ressources (rang 6), blocages, couvertures (certification et mise en service), constat de couverture (rang 17), services supplémentaires (rang 35), fiche de suivi et constat de mise en service (rangs 32, 33), tableaux de bord par fonction et pilotage, intervenants, portail (liaisons, dossier, tâches, coordonnées techniques rang 12, livrables, fiche de suivi) | **MS-HOMOLOGATION** | `/liaisons/{id}/dossier`, `/liaisons/{id}/parcours/operations`, `/liaisons/{id}/ressources`, `/parcours/operations/{id}/{livrable,preuves,validation,intervenants}`, `/liaisons/{id}/blocages`, `/blocages/{id}/levee`, `/liaisons/{id}/couverture/constat`, `/liaisons/{id}/services-supplementaires`, `/tableaux-de-bord/{acteur,pilotage}`, `/intervenants`, `/liaisons/{id}/fiche-suivi{,/remise,/constat}`, `/portail/liaisons…`, `/portail/taches`, `/portail/operations/{id}/livrable` |
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
| `certification.pv-established.v1` (sujet `certification.proces-verbal`) | CER → HOM | `{liaisonId, etabliPar, empreinte}` | Rang 18 clos avec la preuve (procès-verbal) |
| `certification.pv-signed.v1` | CER → HOM | `{liaisonId, signatures:[{fonction, signataire, signeLe}], empreinte}` | Rang 19 clos, statut `CERTIFIEE` |
| `certification.change-record-constituted.v1` (sujet `certification.approbation`) | CER → HOM | `{liaisonId, documentRef, nom, empreinte}` | Rang 20 clos |
| `certification.change-record-referenced.v1` | CER → HOM | `{liaisonId, reference}` | Rang 26 clos s'il est ouvert |
| `certification.attestations-archived.v1` | CER → HOM | `{liaisonId, attestations:[{type, documentRef}]}` | Rangs 21 et 22 clos |
| `certification.security-approved.v1` | CER → HOM | `{liaisonId, approuveeLe}` | Rangs 23 à 25 clos, statut `APPROUVEE_SECURITE` |

MS-HOMOLOGATION tient une **projection** de l'état des verrous par liaison (`etat_pv`, `etat_approbation`,
attestations à vérifier, visas attendus) alimentée par ces événements : ses tableaux de bord (compteurs
`pvASigner`, `pvAEtablir`, `dossiersChangement`, `attestationsAVerifier`, `approbationsEnAttente`,
`visasAttendus`…) et les tâches du portail (`pvASigner`, `attestationsAFournir`) s'en servent, sans
appel synchrone. Les événements portent `institutionid` (extension CloudEvents) pour le canal poussé du
portail ; aucun ne porte de secret ni de contenu de pièce.

## 4. Invariants communs

- Toute action est décidée par le service et reflétée dans `actionsPermises` ; le validateur (V) n'est
  jamais le responsable (R) d'une même opération (`HOM_VALIDATOR_IS_RESPONSIBLE`).
- Clés : une opération `cle` ne porte que des codes de contrôle (4 à 8 hexadécimaux) ; C1 porte la
  cérémonie (date, trois porteurs, trois codes) ; une opération `secret` ne porte qu'une trace ; aucune
  valeur de clé ni de secret n'est jamais acceptée ni conservée.
- Signatures (procès-verbal, visas, validation) : assertion FIDO2 vérifiée par MS-IAM
  (`/api/v1/reauthentification`, en-tête `X-Reauth-Assertion`), puis cachet horodaté (D-35) ; une signature
  est immuable.
- Portail : cloisonné à l'institution du jeton (`institution_id`) ; projection sans nom interne (les
  personnes du GIMAC apparaissent par leur fonction) ; 404 pour une liaison d'une autre institution.
- Codes de refus : ceux du simulateur (`HOM_*`, `CER_*`), en `application/problem+json` avec `code`.
