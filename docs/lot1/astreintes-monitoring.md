# Supervision des liaisons, astreintes et relève de quart (D-114)

> **Recentrage D-116 (20/10/2026), à lire d'abord.** Le module ne surveille rien par lui-même : il **génère les
> programmes** de monitoring (vacations en salle) et d'astreinte (niveau 2) selon les contraintes que fixe le
> responsable, **conserve les rapports** que chacun dépose en les rattachant à son créneau, permet d'**envoyer le rapport
> de fin** au supérieur ou à la personne choisie, et permet aux **chefs de consulter** l'activité des personnes
> programmées et leurs rapports. Chaque matin, l'équipe de nuit dépose avant 8 h 00 le **rapport de monitoring**
> (bilan des 24 h). Le module n'a aucun lien avec l'homologation ni la certification des liaisons. Les §3 et §4
> (liaisons supervisées, alertes) restent dans le service mais sortent de la navigation ; le §10 décrit l'ajout.

Périmètre : BF-17 et BF-ADMIN du REC, UC-EXP-02 et UC-EXP-05, récits MON-01 à MON-15 du carnet v2.0,
algorithmes 6.44 (alertes), 6.45 (grille) et 6.46 (relève) du CC, décisions D-12 (relève signée), D-14 (délais
5, 15 et 60 min) et D-15 (port `IntegerProgramSolver`, adaptateur OR-Tools CP-SAT, 300 s). Service : MS-MONITORING
(`ms-monitoring`, port 8089), joint par la passerelle sous `/bff/des/monitoring/**` (réécrit en `/api/v1/**`).
Écrans : paquet `@gimac/exploitation` de l'application du département, sous `/des/exploitation/...`.

Livraison par tranches verticales (D-60) :

| Tranche | Contenu | Récits |
|---|---|---|
| 1 | Liaisons supervisées, alertes, acquittement dans le délai, escalade, vacation en cours | MON-01 à MON-05, MON-13 (lecture) |
| 2 | Grille d'astreinte CP-SAT, plages non couvertes, échanges, publication signée | MON-09, MON-10, MON-12 |
| 3 | Main courante chaînée, relève signée par le sortant et l'entrant, visa, scellement | MON-14, MON-15 |

## 1. Conventions

- Corps JSON, dates ISO-8601 en UTC (`2026-10-19T06:00:00Z`), jours `AAAA-MM-JJ`, semaine ISO `AAAA-Www`.
- Page : `{ contenu: [], total, page, taille }`, `page` à partir de 0, `taille` 20 par défaut, 100 au plus.
- Personne : `{ id, nom }`, `id` étant le sujet du jeton (compte de MS-IAM), `nom` le nom affiché (annuaire de
  MS-IAM, à défaut l'identifiant).
- Refus : `application/problem+json` `{ code, message, details, correlationId }`. Le `code` (`MON_*`) est stable et
  traduit par l'interface ; `message` est rendu dans la langue de la requête (`Accept-Language` : fr, en, es ;
  fr par défaut), à partir des clés `erreur.<code>` des catalogues `i18n/messages_{fr,en,es}.properties`.
- Actes engageants (publication de la grille, signatures de la relève) : en-tête `X-Reauth-Assertion`, assertion
  FIDO2 de réauthentification (IAM-07) vérifiée et consommée par MS-IAM pour le sujet et la finalité
  (`PUBLICATION_GRILLE`, `SIGNATURE_RELEVE`). Absente : 428 `MON_REAUTH_EXIGEE` ; refusée : 403
  `MON_REAUTH_REFUSEE` ; MS-IAM injoignable : 503 `MON_REAUTH_INDISPONIBLE`.
- Toute route `/api/v1/**` exige un jeton à deux facteurs (D-61) ; `/api/v1/interne/**` un jeton de service.

## 2. Permissions (domaine Keycloak `gimac`, profils configurables, D-86)

| Permission | Objet | Profils livrés |
|---|---|---|
| `MON_SUPERVISION_READ` | Lire liaisons, alertes, vacations, main courante, rapports, grilles et échanges ; proposer ou accepter un échange de ses propres plages | Chef du DES, production monétique, pôles réseau et système, administrateur système, super administrateur, RSSI |
| `MON_ALERT_ACK` | Acquitter une alerte (et en plus : être de la vacation en cours ou en être le chef de quart) | Chef du DES, production monétique, pôles réseau et système, administrateur système |
| `MON_SHIFT_OPERATOR` | Qualification d'opérateur en salle (niveau 1) : éligible à la grille, tient la main courante, signe la relève | Production monétique, pôles réseau et système, administrateur système |
| `MON_ONCALL_L2` | Qualification d'astreinte joignable (niveau 2) | Chef du DES, production monétique, pôles réseau et système |
| `MON_SHIFT_LEAD` | Habilitation de chef de quart : désignable chef de quart, vise la relève | Chef du DES, production monétique |
| `MON_ROSTER_MANAGE` | Générer la grille, approuver ou rejeter un échange | Chef du DES |
| `MON_ROSTER_PUBLISH` | Publier la grille (signature du chef du DES) ; destinataire du dernier palier d'escalade | Chef du DES |

Les qualifications sont lues dans MS-IAM au moment de la génération (`GET /api/v1/interne/collaborateurs?permission=`),
comme le prévoit le CC (« compétences lues dans MS-IAM à la génération »).

## 3. Liaisons supervisées (MON-01, MON-07, MON-16)

`GET /liaisons?etat=&recherche=&page=&taille=` (MON_SUPERVISION_READ) → Page de :
```jsonc
{ "id": "uuid", "code": "LIA-2026-017", "libelle": "Établissement de paiement A, liaison mobile",
  "portee": "INSTITUTION" | "PLATEFORME", "institutionId": "uuid" | null, "type": "liaison de paiement mobile",
  "etat": "NOMINALE" | "DEGRADEE" | "CRITIQUE" | "INJOIGNABLE" | "MAINTENANCE", "depuis": "ISO", "derniereMesure": "ISO" | null,
  "periodeMesureSecondes": 60, "maintenanceJusqua": "ISO" | null, "plateformeParente": { "id", "code" } | null,
  "alertesOuvertes": 2 }
```
Tri : état le plus grave d'abord (INJOIGNABLE, CRITIQUE, DEGRADEE, MAINTENANCE, NOMINALE), puis code.
`GET /liaisons/{id}` → la même vue ; 404 `MON_LIAISON_INTROUVABLE`.

## 4. Alertes (algorithme 6.44, MON-02 à MON-06)

### 4.1 Lecture

`GET /alertes?etat=&gravite=&liaison=&page=&taille=` → Page de :
```jsonc
{ "id": "uuid", "reference": "ALR-2026-04417", "liaison": { "id", "code", "libelle" }, "source": "prometheus", "regle": "latence_p95",
  "gravite": "MINEURE" | "MAJEURE" | "CRITIQUE", "etat": "OUVERTE" | "ACQUITTEE" | "RESOLUE",
  "premiereOccurrence": "ISO", "derniereOccurrence": "ISO", "occurrences": 37,
  "delaiMinutes": 5, "echeance": "ISO" | null, "horsDelai": false, "palierEscalade": 0, "escaladeLe": "ISO" | null,
  "racine": { "id", "reference" } | null, "symptomes": 3,
  "acquittement": { "par": { "id", "nom" }, "le": "ISO", "commentaire": "…", "ticketDemande": true, "horsDelai": false } | null,
  "ticketId": "uuid" | null, "resolueLe": "ISO" | null }
```
Sans filtre d'état : alertes OUVERTE puis ACQUITTEE puis RESOLUE ; à état égal, échéance la plus proche d'abord.
`GET /alertes/{id}` ; `GET /politiques-alerte` → `[ { gravite, delaiMinutes, niveauNotification, canalUrgent, ticket:
"OBLIGATOIRE" | "A_APPRECIATION" | "FACULTATIF" } ]` (tableau 6.48 ; valeurs de départ 5, 15 et 60 min, D-14).

### 4.2 Réception (route interne)

`POST /interne/alertes` (jeton de service, `INTERNAL_MONITORING_ALERT_INGEST`)
`{ source, liaison: "LIA-2026-017", regle, gravite, survenueLe? }` → `{ id, reference, creee: boolean }`.
- Empreinte `f = SHA-256(source ∥ liaison ∥ règle)`, la gravité n'y entrant pas ; une alerte non résolue de même
  empreinte vue depuis moins de 30 min absorbe le message (occurrences, dernière occurrence, gravité au plus haut ;
  une hausse de gravité recalcule l'échéance et republie `monitoring.alert-raised.v1`).
- Sinon création ; si la plateforme parente de la liaison porte une alerte ouverte levée moins de 10 min plus tôt,
  la nouvelle alerte en est un symptôme (`racine`), sans échéance ni notification propre.
- Racine : échéance = première occurrence + délai de la gravité ; `monitoring.alert-raised.v1`.
- Liaison inconnue : 422 `MON_LIAISON_INCONNUE` ; gravité inconnue : 400 `MON_REQUETE_INVALIDE`.

`POST /interne/alertes/resolution` `{ source, liaison, regle }` → `{ resolues: n }` : l'alerte et ses symptômes passent
RESOLUE (`monitoring.alert-resolved.v1`). Une alerte critique racine sans ticket n'est pas résolue à la main
(MON-06) ; la résolution par la source reste toujours admise.

### 4.3 Acquittement (MON-04, MON-06)

`POST /alertes/{id}/acquittement` (MON_ALERT_ACK) `{ commentaire (3 à 500), ticket: boolean }` → l'alerte.
- Réservé aux opérateurs et au chef de quart de la vacation en cours : sinon 403 `MON_HORS_VACATION`, tentative
  journalisée (`monitoring.alert-ack-refused.v1`, consommé par MS-AUDIT).
- Alerte critique racine sans ticket : 422 `MON_TICKET_OBLIGATOIRE`.
- Symptôme : 409 `MON_ALERTE_SYMPTOME` (on acquitte sa racine, qui couvre ses symptômes).
- Déjà acquittée : 409 `MON_ALERTE_DEJA_ACQUITTEE` ; résolue : 409 `MON_ALERTE_RESOLUE`.
- Après l'échéance : accepté et marqué hors délai (indicateurs).
- Effets : état ACQUITTEE pour l'alerte et ses symptômes, entrée automatique dans la main courante de la vacation
  (`ACQUITTEMENT_HORS_DELAI` si hors délai, sinon `ALERTE`), `monitoring.alert-acknowledged.v1`
  `{ alerteId, liaison, institutionId, gravite, ticketDemande, delaiEcouleSecondes, horsDelai, par }`, consommé par
  MS-INCIDENT pour ouvrir le ticket.

### 4.4 Escalade (MON-05)

Ordonnanceur toutes les 30 s (`gimac.monitoring.escalade.intervalle`). Pour chaque alerte racine OUVERTE :

| Palier | Déclenchement | Destinataires |
|---|---|---|
| 1 | échéance dépassée | chef de quart de la vacation en cours ; alerte marquée hors délai |
| 2 | échéance + délai de la gravité | responsable d'astreinte (niveau 2) de la grille publiée pour la plage courante |
| 3 | échéance + 2 × délai, ou palier 2 sans responsable publié | porteurs de `MON_ROSTER_PUBLISH` (chef du DES) ; absence de couverture signalée |

Chaque palier écrit une entrée dans la main courante, publie `monitoring.alert-escalated.v1`
`{ alerteId, palier, destinataires, planNonCouvert }` et `monitoring.notification-requested.v1`
`{ destinataires, niveau: CRITIQUE | URGENT | AVERTISSEMENT, objet, lien }` ; l'acquittement arrête l'escalade.

## 5. Vacations et main courante (MON-13, MON-14)

Plages : MATIN 06 h à 14 h, APRES_MIDI 14 h à 22 h, NUIT 22 h à 06 h (heure de la plateforme,
`gimac.monitoring.fuseau`, Africa/Douala par défaut). Les vacations sont instanciées à la publication de la grille.

`GET /vacations/en-cours` → `{ vacation: Vacation | null }` ;
`GET /vacations/{id}` → Vacation :
```jsonc
{ "id": "uuid", "reference": "VAC-2026-1019-M", "date": "2026-10-19", "plage": "MATIN", "debut": "ISO", "fin": "ISO",
  "operateurs": [ { "id", "nom" } ], "chefDeQuart": { "id", "nom" } | null, "enCours": true,
  "rapport": { "id", "etat" } | null, "precedente": { "id", "reference", "rapport": { "id", "etat" } | null } | null,
  "suivante": { "id", "reference" } | null,
  "responsable": "CETTE_VACATION" | "VACATION_PRECEDENTE" }
```
`responsable` vaut `VACATION_PRECEDENTE` tant que le rapport de la précédente n'est pas contresigné (MON-13, alternatif).

`GET /vacations/{id}/main-courante?page=&taille=` → Page d'entrées, plus récentes d'abord :
`{ id, rang, le, auteur: { id, nom }, nature, texte, reference, rectifie: "uuid" | null, empreinte, empreintePrecedente }`.
`POST /vacations/{id}/main-courante` (MON_SHIFT_OPERATOR, membre ou chef de la vacation **en cours**)
`{ nature: EVENEMENT | INTERVENTION | RETABLISSEMENT | MAINTENANCE | CONSIGNE | CORRECTION, texte (1 à 2000), reference?, rectifie? }`
→ 201. Chaînage : `empreinte = SHA-256(empreintePrecedente ∥ vacation ∥ rang ∥ le ∥ auteur ∥ nature ∥ texte ∥ reference)`, la
première entrée partant de 64 zéros ; la vacation y entre pour que deux chaînes recevant la même entrée automatique au
même instant restent distinctes, et `le` est arrondi à la microseconde (précision de la base). CORRECTION exige `rectifie` (entrée de la même vacation) ; l'entrée d'origine
reste inchangée. Hors vacation : 403 `MON_HORS_VACATION` ; vacation close : 409 `MON_VACATION_CLOSE`.
`GET /vacations/{id}/main-courante/verification` → `{ intacte, entrees, tete }` (recalcul de la chaîne).

## 6. Relève de quart signée et scellée (algorithme 6.46, D-12, MON-15)

`POST /vacations/{id}/rapport` (MON_SHIFT_OPERATOR, membre de la vacation) `{ consignes? }` → 201 Rapport BROUILLON ;
déjà généré : 409 `MON_RAPPORT_EXISTANT`. `GET /vacations/{id}/rapport`, `GET /rapports/{id}` → Rapport :
```jsonc
{ "id": "uuid", "reference": "RQ-2026-1019-M", "vacation": { "id", "reference", "plage", "date" },
  "etat": "BROUILLON" | "SIGNE_SORTANT" | "CONTRESIGNE" | "VISE" | "SCELLE",
  "contenu": { "alertesParGravite": { "CRITIQUE": 1, "MAJEURE": 0, "MINEURE": 2 }, "alertesOuvertes": [ { "reference", "gravite", "liaison" } ],
               "alertesHorsDelai": 0, "liaisons": { "NOMINALE": 12, "DEGRADEE": 1 }, "mainCourante": { "entrees": 7, "tete": "hex" } },
  "consignes": "…", "empreinteContenu": "hex (H1)", "empreinte": "hex (Hf)" | null,
  "documentRef": "…" | null, "horodatage": { "serie": "…" } | null,
  "signatures": [ { "role": "SORTANT" | "ENTRANT" | "VISA_CHEF_DE_QUART", "signataire": { "id", "nom" }, "le": "ISO", "defi": "hex", "reserves": "…" | null } ],
  "responsabiliteTransfereeLe": "ISO" | null, "scelleLe": "ISO" | null,
  "attendus": { "sortants": [ { "id", "nom" } ], "entrants": [ { "id", "nom" } ], "chefs": [ { "id", "nom" } ] } }
```
Circuit, chaque acte avec `X-Reauth-Assertion` (finalité `SIGNATURE_RELEVE`) :
1. `POST /rapports/{id}/signature` `{ consignes? }` : un opérateur de la vacation sortante (MON_SHIFT_OPERATOR). Le
   contenu est figé : `J` = sérialisation canonique (clés triées, RFC 8785), `H1 = SHA-256(J)` ; état SIGNE_SORTANT.
2. `POST /rapports/{id}/contresignature` `{ reserves? (500) }` : un opérateur ou le chef de la vacation suivante,
   distinct du sortant (sinon 422 `MON_SIGNATAIRE_IDENTIQUE`) ; défi `H2 = SHA-256(J ∥ σs)` ; état CONTRESIGNE ;
   la responsabilité de la salle est transférée à cet instant ; les réserves sont consignées dans la main courante
   de la vacation entrante (`RESERVE_ENTRANT`).
3. `POST /rapports/{id}/visa` (MON_SHIFT_LEAD) : chef de quart de la vacation sortante ou entrante, qui n'a signé
   ni comme sortant ni comme entrant (sinon 422 `MON_SIGNATAIRE_IDENTIQUE`) ; défi `H3 = SHA-256(J ∥ σs ∥ σe)` ;
   puis scellement immédiat (RG-18) : `Hf = SHA-256(J ∥ σs ∥ σe ∥ σc)`, dépôt dans MS-DOCUMENT par le port
   `DepotDocumentPort` (rétention 10 ans, D-11), jeton d'horodatage par le port `HorodatagePort` (RFC 3161), état
   SCELLE, `monitoring.shift-report-sealed.v1` `{ rapportId, vacationId, empreinte, documentRef, serieHorodatage }`.
Ordre non respecté : 409 `MON_ORDRE_SIGNATURES` ; signataire hors des attendus : 403 `MON_SIGNATAIRE_NON_ATTENDU`.
Un rapport scellé n'est jamais modifié.

## 7. Grille d'astreinte (algorithme 6.45, D-15, MON-09, MON-10, MON-12)

### 7.1 Modèle

Variables binaires `x[i,t,s,l]` (opérateur i, jour t, plage s, niveau l ∈ {1 en salle, 2 astreinte joignable}),
`z[i,t,s]` (chef de quart), entières `u[t,s,l]` et `uc[t,s]` (défaut de couverture), `E` (écart de charge).

| Contrainte | Valeur par défaut (`gimac.monitoring.grille.*`) |
|---|---|
| Effectif minimal par plage : niveau 1, niveau 2 | 2 et 1 |
| Un chef de quart habilité (MON_SHIFT_LEAD) par plage, présent au niveau 1 | oui |
| Compétences : niveau 1 MON_SHIFT_OPERATOR, niveau 2 MON_ONCALL_L2 | oui |
| Une affectation par jour et par personne | oui |
| Repos minimal de 11 h : (NUIT, MATIN), (NUIT, APRES_MIDI), (APRES_MIDI, MATIN) interdits sur deux jours consécutifs | 11 h |
| Nuits consécutives au plus | 3 |
| Plafond hebdomadaire (8 h au niveau 1, 2 h au niveau 2, γ = 0,25) | 48 h |
| Absences et congés validés dans MS-IAM | exclus |
| Dernier jour de la semaine précédente publiée repris, figé | oui |

Objectif : minimiser `Λ·Σu + α·E + Σ C·x` (Λ = 1 000 000 dominant, α = 10, C : pénibilité des nuits 3 et des fins
de semaine 2). Les coefficients sont entiers (heures comptées en quarts d'heure). Le modèle est construit par le
domaine (`FormulationGrille`) et transmis au port `IntegerProgramSolver` (programme linéaire en nombres entiers
générique) ; l'adaptateur `OrToolsCpSatSolver` le résout par CP-SAT, limite 300 s (`gimac.monitoring.grille.limite-secondes`),
8 fils au plus. La solution est contrôlée par un vérificateur indépendant (`VerificateurGrille`, Java pur), qui sert
aussi aux échanges.

### 7.2 Routes

`POST /grilles/generation` (MON_ROSTER_MANAGE) `{ semaine: "2026-W44" }` → 201 Grille (révision n+1, état GENEREE).
Aucun opérateur qualifié : 422 `MON_AUCUN_OPERATEUR` ; semaine mal formée : 400 `MON_REQUETE_INVALIDE` ;
semaine passée : 422 `MON_SEMAINE_PASSEE`. Solveur sans solution dans le délai : 422 `MON_GRILLE_SANS_SOLUTION`.

`GET /grilles?semaine=` → `[ { id, semaine, revision, etat, statutSolveur, nonCouvertes, genereeLe, publieeLe } ]` ;
`GET /grilles/publiee?semaine=` → Grille publiée (404 `MON_GRILLE_INTROUVABLE`) ; `GET /grilles/{id}` → Grille :
```jsonc
{ "id", "semaine", "revision", "etat": "GENEREE" | "PUBLIEE" | "REMPLACEE",
  "statutSolveur": "OPTIMALE" | "REALISABLE" | "IRREALISABLE" | "SANS_SOLUTION_DANS_LE_DELAI",
  "objectif": 1234, "ecart": 0.008, "dureeMs": 812, "genereeLe", "genereePar": { "id", "nom" }, "publieeLe", "publieePar",
  "jours": [ "2026-10-26", … ],
  "affectations": [ { "id", "date", "plage", "niveau": 1 | 2, "operateur": { "id", "nom" }, "chefDeQuart": false } ],
  "nonCouvertes": [ { "date", "plage", "besoin": "NIVEAU_1" | "NIVEAU_2" | "CHEF_DE_QUART", "manque": 1, "causes": [ "ABSENCES", "REPOS", "QUALIFICATION" ] } ],
  "violations": [ { "code": "REPOS" | "NUITS_CONSECUTIVES" | "PLAFOND" | "ABSENCE" | "QUALIFICATION" | "UNE_PAR_JOUR" | "CHEF", "operateurId", "date", "plage" } ],
  "charges": [ { "operateur": { "id", "nom" }, "heures": 40, "nuits": 2, "finsDeSemaine": 1 } ] }
```
`POST /grilles/{id}/publication` (MON_ROSTER_PUBLISH, `X-Reauth-Assertion` finalité `PUBLICATION_GRILLE`)
`{ accepterNonCouvertes?: boolean, motif?: string }` → Grille PUBLIEE. Plages non couvertes : refus 422
`MON_GRILLE_NON_COUVERTE` sauf `accepterNonCouvertes` avec un motif (10 à 500) ; grille déjà publiée ou remplacée :
409 `MON_GRILLE_NON_PUBLIABLE`. Effets : la publiée précédente de la semaine passe REMPLACEE, les vacations de la
semaine sont (ré)instanciées pour les plages futures, `monitoring.oncall-published.v1`
`{ grilleId, semaine, revision, affectations, nonCouvertes, motif }` et une demande de notification à chaque opérateur.

### 7.3 Échanges (MON-12)

`GET /grilles/{id}/echanges` → `[ Echange ]` ; `POST /grilles/{id}/echanges` (MON_SUPERVISION_READ, titulaire de
`affectationId`) `{ affectationId, affectationCibleId, motif? }` → 201 Echange PROPOSE. Grille non publiée : 409
`MON_GRILLE_NON_PUBLIEE` ; plage passée : 422 `MON_PLAGE_PASSEE` ; même personne : 422 `MON_ECHANGE_INVALIDE` ; grille
résultante en violation : 422 `MON_ECHANGE_CONTRAINTE` avec `details.violations`.
`POST /echanges/{id}/accord` | `/refus` (titulaire de la plage cible) ; `POST /echanges/{id}/approbation` | `/rejet`
(MON_ROSTER_MANAGE). L'approbation revérifie les contraintes, publie une révision n+1 de la grille avec l'échange
(la précédente REMPLACEE), met à jour les vacations concernées et publie `monitoring.oncall-swap-approved.v1`.
```jsonc
{ "id", "grilleId", "etat": "PROPOSE" | "ACCEPTE" | "REFUSE" | "APPROUVE" | "REJETE",
  "demandeur": { "id", "nom" }, "cible": { "id", "nom" },
  "affectation": { "id", "date", "plage", "niveau" }, "affectationCible": { "id", "date", "plage", "niveau" },
  "motif", "creeLe", "decideLe", "decidePar": { "id", "nom" } | null, "nouvelleGrilleId": "uuid" | null }
```

### 7.4 Absences (BF-ADMIN)

Les congés et absences **validés** de MS-IAM sont lus à la génération par le port `DisponibilitesPort`
(`GET /api/v1/interne/disponibilites?du=&au=` de MS-IAM, jeton de service `INTERNAL_IAM_READ`) et projetés dans la
table `operator_unavailability` (clé `availability_id`, idempotent) ; une absence annulée est retirée.

## 8. Passerelle et OPA

Route `des-monitoring` : `/bff/des/monitoring/**` → `${gimac.services.monitoring}/api/v1/**`, filtre OpaAuthorize,
corps limité à 64 Ko, en-tête `X-Reauth-Assertion` relayé. Comme pour Mon activité, seuls les chemins listés sont
relayés et jamais pour un compte d'institution ; `/bff/des/monitoring/interne/**` n'est jamais relayé.

| Méthode | Chemin sous `/bff/des/monitoring` | Permission |
|---|---|---|
| GET | `liaisons(/id)`, `alertes(/id)`, `politiques-alerte`, `vacations/en-cours`, `vacations/id(/main-courante(/verification)|/rapport)`, `rapports/id`, `grilles(/publiee|/id(/echanges))` | MON_SUPERVISION_READ |
| POST | `alertes/id/acquittement` | MON_ALERT_ACK |
| POST | `vacations/id/(main-courante|rapport)`, `rapports/id/(signature|contresignature)` | MON_SHIFT_OPERATOR |
| POST | `rapports/id/visa` | MON_SHIFT_LEAD |
| POST | `grilles/generation`, `echanges/id/(approbation|rejet)` | MON_ROSTER_MANAGE |
| POST | `grilles/id/publication` | MON_ROSTER_PUBLISH |
| POST | `grilles/id/echanges`, `echanges/id/(accord|refus)` | MON_SUPERVISION_READ |

## 9. Interface (couche `des`)

Paquet `@gimac/exploitation`, bâti sur le système de conception (D-63), sans style propre, libellés fr, en, es :

| Clé du catalogue (D-94) | Chemin | Écran |
|---|---|---|
| `monitoringSupervision` | `/des/exploitation/supervision` | État des liaisons (tuiles), alertes avec délai restant (`AlertRow`), acquittement en modale, bandeau de la vacation en cours |
| `monitoringAstreintes` | `/des/exploitation/astreintes` | Grille de la semaine (plages × jours), plages non couvertes, charges, génération, publication signée, échanges |
| `monitoringReleve` | `/des/exploitation/releve` | Vacation en cours, main courante, rapport de relève et circuit sortant, entrant, visa |

Groupe de la barre latérale `monitoring` (« Supervision et astreintes »), prédicat `MON_SUPERVISION_READ`.
Services simulés (`VITE_SERVICES=simule`) : même contrat, jeu fictif (opérateurs USR-0042 et suivants, liaisons
LIA-2026-0xx), refus normalisés.

## 10. Rapports de monitoring, contraintes et activité de l'équipe (D-116)

### 10.1 Rapports

Un rapport est un fichier déposé dans MS-DOCUMENT (dépôt fragmenté D-74, finalité `RAPPORT_MONITORING`, permission
MON_SUPERVISION_READ), puis rattaché par MS-MONITORING, qui vérifie la pièce (`GET /api/v1/interne/documents/{id}` :
finalité, déposant = auteur).

`POST /rapports-monitoring` (MON_SUPERVISION_READ) `{ nature: QUOTIDIEN | VACATION | ASTREINTE | FIN | AUTRE,
vacationId?, affectationId?, date?, titre (3 à 160), resume (0 à 2000), documentRef }` → 201 Rapport. Rattachement :
`vacationId` (membre ou chef de la vacation), `affectationId` (titulaire de l'affectation d'une grille publiée), ou
aucun pour AUTRE. QUOTIDIEN porte sur la journée `date` (par défaut la veille) et exige d'être de la vacation de nuit
qui se termine ce jour-là, ou chef. Refus : 403 `MON_RAPPORT_HORS_PROGRAMME`, 422 `MON_PIECE_INVALIDE`, 409
`MON_RAPPORT_QUOTIDIEN_EXISTANT` (un seul rapport quotidien par jour), 400 `MON_REQUETE_INVALIDE`.

```jsonc
{ "id", "nature", "titre", "resume", "date": "AAAA-MM-JJ" | null,
  "vacation": { "id", "reference", "plage", "date" } | null, "affectation": { "id", "date", "plage", "niveau" } | null,
  "auteur": { "id", "nom" }, "deposeLe": "ISO", "echeance": "ISO" | null, "enRetard": false,
  "document": { "id", "nom", "taille", "type" },
  "envois": [ { "destinataire": { "id", "nom" }, "le": "ISO", "message": "…" | null } ] }
```

`GET /rapports-monitoring?nature=&auteur=&du=&au=&page=&taille=` → Page, plus récents d'abord. Chacun voit ses rapports
et ceux qui lui ont été envoyés ; un chef (MON_SHIFT_LEAD ou MON_ROSTER_MANAGE) voit tous les rapports.
`GET /rapports-monitoring/{id}` (mêmes droits ; sinon 404) ; `GET /rapports-monitoring/{id}/fichier` (le fichier).

`POST /rapports-monitoring/{id}/envoi` (auteur) `{ destinataires: [compteId] (1 à 20), message? (500) }` → le rapport :
chaque destinataire est notifié (`monitoring.notification-requested.v1`, objet `RAPPORT_RECU`) et peut le lire.

### 10.2 Rapport quotidien de 8 h

Pour chaque jour J, le rapport QUOTIDIEN porte sur les 24 h de J-1 et revient à la vacation de NUIT de J-1 (22 h à
6 h). Échéance : J à 08 h 00 (`gimac.monitoring.rapport-quotidien.echeance`, heure de la plateforme). Un dépôt après
l'échéance est accepté et marqué en retard.

`GET /rapports-monitoring/quotidiens?du=&au=` (chefs ; un opérateur voit les siens) → `[ { date, vacation, attendus:
[Personne], rapport: { id, deposeLe, enRetard } | null, statut: DEPOSE | EN_RETARD | ATTENDU | SANS_VACATION } ]`.

Ordonnanceur : à 07 h 30, rappel aux membres de la vacation de nuit si rien n'est déposé ; dès l'échéance passée,
signalement aux porteurs de MON_ROSTER_MANAGE (une fois par jour).

### 10.3 Contraintes de génération

`GET /grilles/contraintes` (MON_SUPERVISION_READ) et `PUT /grilles/contraintes` (MON_ROSTER_MANAGE, motif)
`{ effectifSalle (1 à 10), effectifAstreinte (0 à 5), chefDeQuart (bool), reposHeures (8 à 24), nuitsMax (1 à 7),
plafondHeures (24 à 72), limiteSecondes (10 à 600) }` : valeurs utilisées par la génération suivante, journalisées
(`monitoring.constraints-changed.v1`) ; à défaut, celles de la configuration.

### 10.4 Activité de l'équipe

`GET /activite?du=&au=&personne=` (MON_SHIFT_LEAD ou MON_ROSTER_MANAGE ; 92 jours au plus) → `[ { personne,
creneaux: [ { date, plage, niveau, chefDeQuart, vacationId } ], rapports: n, quotidiensDeposes: n, quotidiensEnRetard: n,
quotidiensManquants: n } ]`, à partir des grilles publiées.

### 10.5 Écrans

Navigation du module : « Aujourd'hui » (qui est de service, rapport de 8 h, mes prochains créneaux, déposer un
rapport), « Programme » (grille, génération, contraintes, échanges), « Rapports » (déposer, filtrer, envoyer),
« Activité de l'équipe » (chefs). « Liaisons et alertes » et « Vacation et relève » quittent la navigation.

Écrans livrés (`@gimac/exploitation`) : `/des/exploitation/aujourdhui`, `/des/exploitation/astreintes` (« Programme
d'astreinte », panneau des contraintes en tête pour MON_ROSTER_MANAGE), `/des/exploitation/rapports` (onglets
« Rapports » et « Rapports de 8 h » ; liens des notifications `?rapport=<id>` et `?quotidien=<jour>`),
`/des/exploitation/activite`. Le dépôt enchaîne sur l'envoi, le supérieur (`/bff/des/iam/organisation/moi`) proposé
d'office, toute personne de l'annuaire de la messagerie ensuite. La pièce se consulte dans une modale avec le lecteur
de la messagerie : PDF, images et texte lus directement, documents bureautiques (docx, xlsx, odt…) convertis en PDF
par l'aperçu de MS-DOCUMENT (D-83, `POST /bff/des/document/apercus`).

## 11. Programme entièrement configurable, calendrier, téléchargement (D-117)

Le découpage n'est plus codé : un **modèle de créneaux** versionné, édité par la gestion du programme, décrit la
permanence. Les trois plages de D-114 (MATIN, APRES_MIDI, NUIT) ne sont plus que le modèle « historique », lu pour les
grilles enregistrées avant D-117 ; le modèle par défaut reprend la feuille du DES.

### 11.1 Modèle des créneaux

`GET /grilles/creneaux` (MON_SUPERVISION_READ) → `{ version, modifieLe, modifiePar, creneaux: [Creneau] }` ; version 0 :
modèle par défaut, jamais enregistré. `PUT /grilles/creneaux` (MON_ROSTER_MANAGE) `{ creneaux, motif (3 à 500) }` →
nouvelle version, événement `monitoring.roster-slots-changed.v1` ; 422 `MON_CRENEAUX_INVALIDES` avec
`details.erreurs` par champ (`creneaux[i].code`…), 400 `MON_REQUETE_INVALIDE` sur un champ illisible (heure, jour).

```jsonc
{ "code": "NUIT",                       // 2 à 40 : majuscules, chiffres, souligné ; unique
  "libelles": { "fr": "Nuit", "en": "Night", "es": "Noche" },   // fr obligatoire (80), en et es facultatifs (repli fr)
  "jours": [1, 2, 3, 4, 5, 6, 7],      // ISO, 1 = lundi
  "debut": "22:00", "fin": "08:00",    // la fin peut passer minuit (fin <= début : lendemain)
  "portee": "JOUR",                    // JOUR : une occurrence par jour ; SEMAINE : une seule pour la semaine
  "niveau": 1,                         // 1 de service, 2 joignable
  "effectifMin": 1, "effectifMax": 1,  // 0 à 10 ; maximum strict
  "permissions": [],                   // l'une est requise ; vide : MON_SHIFT_OPERATOR (1) ou MON_ONCALL_L2 (2)
  "eligibles": [],                     // personnes désignées (domaine) ; vide : toute personne qui a la permission
  "chefDeQuart": false,                // un chef de quart (MON_SHIFT_LEAD) exigé, niveau 1 seulement
  "rapportQuotidien": true,            // porte le rapport de 8 h (créneau quotidien)
  "heuresComptees": null,              // heures comptées au plafond ; null : durée (niveau 1) ou quart de la durée (2)
  "ordre": 20, "actif": true }
```

Modèle par défaut : `SOIREE` (lundi à vendredi, 16 h à 22 h, une ou deux personnes), `NUIT` (tous les jours, 22 h à
8 h, rapport de 8 h), `WE_MATINEE` et `WE_APRES_MIDI` (samedi et dimanche, 8 h à 12 h, 12 h à 16 h), quatre supports de
niveau 2 de la semaine `N2_RESEAU`, `N2_SYSTEME`, `N2_APPLICATION`, `N2_BASE_DONNEES` (lundi 8 h au lundi suivant 8 h, 10 h
comptées).

### 11.2 Contraintes générales

`GET /grilles/contraintes` et `PUT /grilles/contraintes` (MON_ROSTER_MANAGE, motif) `{ reposHeures (0 à 24), nuitsMax (1 à 7),
plafondHeures (8 à 84), limiteSecondes (10 à 600), echeanceRapport, rappelRapport ("HH:mm", rappel avant l'échéance) }`.
Effectifs et chef de quart relèvent de chaque créneau ; les colonnes de D-116 restent en base, sans effet.

### 11.3 Génération et vérification

Chaque grille fige la version du modèle qui l'a produite (`roster_slot_snapshot`) ; chaque affectation porte son
créneau, son début et sa fin (`roster_slot_assignment`), les entités JHipster restant inchangées. Le solveur et le
vérificateur raisonnent sur les intervalles réels : couverture (défaut pénalisé) et effectif maximal par occurrence,
chevauchement interdit sauf avec un support de la semaine, repos minimal entre deux créneaux quotidiens de tout niveau
(une affectation par jour du CC, généralisée), nuits consécutives (créneau quotidien qui passe minuit), plafond et
équité sur les heures comptées, pénibilité des nuits et des fins de semaine. Repos et chevauchement sont formulés en
cliques (à chaque début d'occurrence, les occurrences qui s'excluent : au plus une). Refus : 422 `MON_CRENEAUX_VIDES`
(aucun créneau actif), `MON_AUCUN_OPERATEUR` (personne d'éligible).

Vues : `GrilleVue.creneaux` (modèle de la grille), `AffectationVue { id, date, creneau, debut, fin, semaine, niveau,
operateur, chefDeQuart }`, `NonCouverteVue { date, creneau, debut, fin, besoin: EFFECTIF | CHEF_DE_QUART, manque,
causes }`, `ViolationVue { code (… CHEVAUCHEMENT, EFFECTIF_MAX), operateurId, date, creneau }`. À la publication, les
vacations des créneaux quotidiens de niveau 1 sont instanciées (`VAC-AAAA-MMJJ-CODE`, format de D-114 pour une plage
historique) ; sans chef de quart désigné sur un créneau qui l'exige, la personne qui publie tient la vacation (MON-13).

### 11.4 Rapport de 8 h

Le rapport d'une journée revient aux personnes affectées, dans le programme publié, aux créneaux de ce jour marqués
`rapportQuotidien` (la nuit, par défaut) ; échéance et rappel viennent des contraintes.

### 11.5 Téléchargement

`GET /grilles/{id}/export?format=pdf|xlsx&langue=fr|en|es` (MON_SUPERVISION_READ) → le fichier
(`programme-AAAA-Www-rN.pdf`), fidèle à la feuille : titre « Liste des agents en permanence du … au … », un bloc par
créneau quotidien (jour, personnes, chef de quart signalé), support de niveau 2 de la semaine, créneaux non couverts,
« Publié par X le Y » ou mention de projet, pied de page paginé. PDF imprimé par le moteur de MS-DOCUMENT
(`POST /api/v1/interne/rendus/pdf`, INTERNAL_DOCUMENT_RENDER donné à monitoring-service), 503 `MON_PDF_INDISPONIBLE` ;
classeur Excel écrit sans dépendance ; 400 sur un autre format.

### 11.6 Publication notifiée

Chaque personne programmée reçoit `GRILLE_PUBLIEE` (`GRILLE_REVISEE` pour une révision) avec `objetId` (la grille),
`le`, l'acteur et le lien `/des/exploitation/astreintes?semaine=AAAA-Www&vue=moi` : son programme de la semaine, puis
le programme complet et son jour.

### 11.7 Écrans

`/des/exploitation/astreintes` (« Programme d'astreinte et de monitoring ») : planning plutôt que grille horaire. Semaine :
un créneau par ligne (teinte, libellé, heures), un jour par colonne, personnes en pastilles (la mienne marquée, chef de
quart signalé, « à pourvoir » pour un manque), jours sans le créneau hachurés, créneau en cours encadré, supports de
niveau 2 de la semaine en bandeau. Jour : cartes horaires dans l'ordre (terminé, en cours, à venir ; nuit commencée la
veille ou finissant le lendemain). Mois : calendrier compact, un clic ouvre le jour. Légende des créneaux ; onglet « Tableau » sur le modèle de la feuille (un créneau par ligne, un jour
par colonne, supports de la semaine à part, charges) ; « Programme de » tout le monde ou moi ; « Mon jour » ; révisions ;
téléchargement PDF ou Excel dans la langue de l'interface ; « Créneaux du programme » (MON_ROSTER_MANAGE) : éditeur du
modèle, erreurs du service par champ. Adresse : `?jour=`, `?vue=jour|mois`, `?qui=moi`, `?affichage=tableau` ; lien de
notification `?semaine=&vue=moi`. « Aujourd'hui » affiche qui est de service à l'instant selon les créneaux (aucune
permanence aux heures ouvrables).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-19 | - | Contrat initial (D-114), tranches 1 à 3. |
| 2026-10-19 | - | Empreinte de la main courante : vacation incluse, instant à la microseconde (défauts trouvés par les tests d'intégration de la relève et de la grille). |
| 2026-10-20 | 5ebf7d0a | Recentrage D-116 : programmes et rapports, rapport quotidien de 8 h, contraintes paramétrables, activité de l'équipe (§10) |
| 2026-10-20 | 3f8b1db7 | §10.5 : écrans livrés, envoi après dépôt, consultation des pièces en modale (PDF, docx converti) |
| 2026-10-20 | a94e2cb1 | §11 (D-117) : modèle de créneaux configurable, contraintes générales, génération sur intervalles, rapport de 8 h porté par un créneau, téléchargement PDF et Excel, publication notifiée |
| 2026-10-20 | a94e2cb1 | §11.7 : écrans du programme configurable (calendrier, tableau, mon jour, éditeur des créneaux, téléchargement) |
| 2026-10-20 | - | §11.7 : le calendrier du programme devient un planning (semaine par créneau et par jour, jour en cartes, mois compact) |
