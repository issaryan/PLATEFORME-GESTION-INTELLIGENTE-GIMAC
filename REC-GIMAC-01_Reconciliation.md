# REC-GIMAC-01 — Note de réconciliation des livrables

| Élément | Valeur |
|---|---|
| Projet | Plateforme de suivi des activités du Département Exploitation et Supervision (DES), GIMAC |
| Référence | REC-GIMAC-01 v1.0 (28/09/2026) |
| Documents réconciliés | CCT-GIMAC-01 v1.1, CA-GIMAC-01 v1.1, CC-GIMAC-01 v1.1, CP-GIMAC-01 v1.0 |
| Établi par | KOAGNE ISSA RYAN, Ingénieur en Informatique et Systèmes d'information |
| Statut | Référentiel d'implémentation. Décisions D-01 à D-31, y compris les propositions [P], **acceptées par KOAGNE ISSA RYAN le 28/09/2026** ; validation formelle du Product Owner (chef du DES) attendue en revue d'itération |
| Rôle | Base unique à partir de laquelle le code est écrit ; prime sur les quatre documents pour les points qu'elle tranche |

**Légende des décisions**
- **[R]** : réconciliation déduite des documents eux-mêmes, par application de la règle de préséance (§0).
- **[P]** : proposition nouvelle, qui comble un vide des documents et doit être validée explicitement.

---

## 0. Règle de préséance

Quand deux documents divergent, la source qui fait foi dépend de la nature de l'information :

| Nature de l'information | Source qui fait foi |
|---|---|
| Périmètre, besoins (BF, BNF), seuils contractuels (métriques SMART), contraintes réglementaires | **CCT v1.1**, complété par les ajouts ratifiés au §2 |
| Acteurs, cas d'utilisation (UC), règles de gestion (RG) | **CA v1.1**, seule source des numéros RG |
| Découpage en services, technologies, états, événements, algorithmes, valeurs de paramètres | **CC v1.1** |
| Priorités (MoSCoW), estimations, ordonnancement | **CP v1.0**, sauf sa colonne RG, déclarée nulle (D-04) |
| Tout point tranché ci-dessous | **REC-GIMAC-01** |

---

## 1. Référentiel d'architecture retenu

**Chaîne de découpage.** Les 7 domaines d'analyse du CA et les 17 modules du CCT sont projetés sur **14 microservices** : c'est l'unité d'implémentation, de dépôt de code et de déploiement. La mention « six groupes » de la figure 3.1 du CCT est caduque.

| Domaine d'analyse (CA) | Microservices (CC) |
|---|---|
| D1 Socle transverse, identités et sécurité | MS-IAM, MS-AUDIT, MS-DOCUMENT, MS-NOTIFICATION |
| D2 Référentiels et gestion des participants | MS-PARTICIPANT |
| D3 Homologation et certification | MS-HOMOLOGATION (suivi et preuves), MS-CERTIFICATION (décision), séparés au sens d'ISO/IEC 17065 |
| D4 Exploitation, monitoring et incidents | MS-INCIDENT, MS-MONITORING, MS-RAPPROCHEMENT |
| D5 Pilotage et cycle décisionnel | MS-PILOTAGE, MS-REPORTING |
| D6 Collaboration souveraine | MS-COLLABORATION |
| D7 Présentation et passerelle | MS-GATEWAY et les trois frontaux (application du département, portail des participants, console d'administration et d'audit) |

**Pile technique de référence (CC, chapitre 4).**
- Backend : Java 21 et Spring Boot 3, générés avec JHipster.
- Frontend : React 18 et TypeScript.
- Bus d'événements : Kafka, avec enveloppe CloudEvents 1.0.
- Identité : Keycloak (OIDC) et annuaire Samba AD (LDAPS).
- Données : PostgreSQL 16 (Patroni), Liquibase, stockage objet WORM MinIO.
- Politiques d'accès : Open Policy Agent. Résilience : Resilience4j.
- Collaboration : Matrix Synapse et LiveKit.
- Les choix restés ouverts dans le CC sont tranchés en D-25.

---

## 2. Réconciliation des besoins fonctionnels

Le CCT définit 28 besoins numérotés (BF-01 à BF-28) plus BF-ADMIN. BF-29 est ratifié par D-05. Les identifiants signalés *(nouveau)* sont attribués par la présente note (D-06).

Colonne « Livraison » : It = itération de la première phase (semaines 3 à 10), V2 et V3 = vagues suivantes.

| BF | Besoin réconcilié | Prio. CCT | Service propriétaire | UC (CA) | RG (CA) | Épopées (CP) | Livraison |
|---|---|---|---|---|---|---|---|
| BF-01 | Fédération d'identité : OIDC vers l'IdP, aucun mot de passe local, second facteur pour les comptes à privilèges | E | MS-IAM, MS-GATEWAY | SOC-01, PRE-01 | 24, 65 | IAM-E1, GW-E1 | It1 |
| BF-02 | Habilitations à 5 niveaux, RBAC projeté en RLS ; délégations (D-20) ; revue des accès | E | MS-IAM | SOC-01, SOC-05 | 01, 02, 03 | IAM-E2, IAM-E4 | It1 (revue en It4) |
| BF-03 | Annuaire des collaborateurs synchronisé (LDAPS), historique immuable des affectations | E | MS-IAM | SOC-05 | 66, 67 | IAM-E3 | It1, It2 |
| BF-ADMIN | Congés et absences avec justificatif, validation hiérarchique, alimentation de la grille d'astreinte | E | MS-IAM (produit la disponibilité), MS-MONITORING (la consomme) | SOC-06 *(nouveau)* | 74 *(nouveau)* | IAM-09 | It2 |
| BF-04 | GED WORM : antivirus, masquage, SHA-256, versions sans écrasement, journal de consultation | E | MS-DOCUMENT | SOC-02 | 04, 05, 06, 16 | DOC-E1 à E3 | It2 |
| BF-05 | Notifications : dans la plateforme, canal poussé, courriel, SMS en extension ; urgence, préférences, trace de remise | E | MS-NOTIFICATION | SOC-04 | 62, 63, 64 | NOT-E1 à E3 | It2 (SMS en V2) |
| BF-06 | Registre d'audit en ajout seul, arbre de Merkle (RFC 9162), racine publiée hors plateforme (D-26) | E | MS-AUDIT | SOC-03 | 07, 08, 09 | AUD-E1 à E3 | It1 |
| BF-07 | Fiche institutionnelle : code ISO 9362, catégorie, statut, banque de règlement, conventions, services, correspondants, annuaire d'escalade | E | MS-PARTICIPANT | REF-01, REF-02, REF-04 | 10 à 14, 70, 71 | PAR-E1, E2, E4 | It2 |
| BF-08 | Veille des échéances J-90, J-60, J-30, J-0 et dégradation encadrée de la conformité (D-13) | I | MS-PARTICIPANT | REF-03 | 16, 68, 69 | PAR-E3 | It2 |
| BF-09 | Référentiel d'opérations versionné (tronc commun et branches) en graphe acyclique ; instanciation par tri de Kahn | E | MS-HOMOLOGATION | HOM-01 | 26, 27, 28 | HOM-E1 | It3 |
| BF-10 | Périmètre contractualisé, couverture de certification (γ) et de mise en service (µ) par service | E | MS-HOMOLOGATION | HOM-01 | 72 | HOM-07 | It4 |
| BF-11 | Règle des quatre éléments, **pour les opérations d'homologation seulement** (D-09) | E | MS-HOMOLOGATION | HOM-03 | 32, 33 | HOM-E2 | It3 |
| BF-12 | Reprise en lecture seule des résultats du simulateur, par interrogation périodique (D-16) | E | MS-HOMOLOGATION | HOM-02 | 29, 30, 31 | HOM-E3 | It4 |
| BF-13 | Quatre quitus, fiche de changement approuvée (lue, jamais écrite), attestation signée | E | MS-CERTIFICATION | HOM-04 | 34, 73 | CER-E1 à E3 | It4 |
| BF-14 | Dépôt de pièces depuis le portail : antivirus et masquage avant ingestion | I | MS-DOCUMENT, MS-HOMOLOGATION | PRE-03 | 59, 60 | DOC-08, HOM-05, HOM-E4 | It4 |
| BF-15 | Tickets de tous types sur l'automate à 7 états (D-08), délais d'engagement, escalade | E | MS-INCIDENT | EXP-01 | 36, 37, 38 | INC-E1 | V2 (D-07) |
| BF-16 | Qualification S1 à S4 (matrice 5×5), escalade, critères de clôture d'incident, rapport circonstancié (D-09, D-10) | E | MS-INCIDENT | EXP-01 | 36, 37, 38 | INC-E2, INC-E3 | V2 (D-07) |
| BF-17 | Monitoring des liaisons, alertes, grille d'astreinte par programmation linéaire en nombres entiers (D-15), relève de quart signée (D-12) | E | MS-MONITORING | EXP-02, EXP-05 *(nouveau)* | 17, 18, 39 | MON-E1 à E3 | It2 |
| BF-18 | Cellule de crise S1 : main courante horodatée, clôture sur rapport post-incident signé (D-14) | I | MS-INCIDENT (cellule), MS-COLLABORATION (salon) | EXP-03, COL-01, COL-04 | 40, 41, 42, 22, 53, 75 | INC-E4, COL-E3 | V2, V3 |
| BF-19 | Rapprochement par identifiants pivots, suspens tracés jusqu'à l'attestation ; aucune écriture en production | I | MS-RAPPROCHEMENT | EXP-04 | 43, 44, 45 | RAP-E1, RAP-E2 | V2 |
| BF-20 | Kanban avec limites d'en-cours, tâches à responsable unique, liens vers ticket, opération ou décision | E | MS-PILOTAGE | PIL-02 | 20, 49 | PIL-E1 | V3 (D-07) |
| BF-21 | Projets (PERT/CPM, jalons) et risques (matrice 5×5) | I | MS-PILOTAGE | PIL-03 | 46, 50 | PIL-E2 | V3 |
| BF-22 | Réunions et PDCA : décisions saisies de façon structurée puis converties en tâches (D-27) | I | MS-PILOTAGE | PIL-04, PIL-01 | 47, 51 | PIL-E3 | V3 |
| BF-23 | Magasin décisionnel en étoile, indicateurs et seuils, cockpits DG et DES, rapports hebdomadaire et mensuel | E | MS-REPORTING | PIL-01 | 21, 48 | REP-E1 à E3 | It3 (mensuel en V3) |
| BF-24 | Messagerie chiffrée de bout en bout ; archivage par un terminal d'archivage déclaré (D-28) | I | MS-COLLABORATION | COL-02 | 22, 52 | COL-E1 | V3 |
| BF-25 | Visioconférence WebRTC locale (SFU), chiffrement SFrame, latence inférieure à 50 ms en critère d'acceptation | I | MS-COLLABORATION | COL-03 | 23, 54, 55 | COL-E2 | V3 |
| BF-26 | Application web du département : SPA sans état, BFF, cookies HttpOnly | E | MS-GATEWAY et frontal | PRE-02 | 56, 58 | GW-E1, GW-E4, TRV-E3 | It1 (squelette) |
| BF-27 | Portail des participants en DMZ, cloisonnement strict par institution | I | MS-GATEWAY et frontal | PRE-03 | 59, 60 | GW-13, TRV-12 et récits « portail » | It4 |
| BF-28 | Console d'administration et d'audit : FIDO2, zone d'administration (D-17) | E | MS-GATEWAY, MS-IAM, MS-AUDIT et frontal | PRE-04 | 57, 61 | GW-14, AUD-08, IAM-10 | It4 |
| BF-29 | Canal poussé temps réel vers les trois couches (sous-besoin de BF-05) | I (CA) | MS-GATEWAY | PRE-05 *(nouveau)* | — | GW-12 | It1 |

---

## 3. Réconciliation des besoins non fonctionnels

| BNF | Valeur retenue pour l'implémentation | Décision |
|---|---|---|
| BNF-01 | Tout appel porte un jeton ; la passerelle émet un jeton interne réduit de 15 min (RFC 8693) | [R] |
| BNF-02 | mTLS en TLS 1.3 entre services, certificats X.509 courts délivrés par la PKI interne, jeton lié au certificat (RFC 8705) | [R] |
| BNF-03 | Conteneurs non root (CCT) **et** RLS en base (CA) : les deux formulations s'appliquent | [R] |
| BNF-04 | Un cluster PostgreSQL, un schéma et un rôle par service, requêtes inter-schémas bloquées (CC 5.2.3) | [R] |
| BNF-05 | Hachage des feuilles `SHA256(0x00‖d)`, des nœuds `SHA256(0x01‖g‖d)`, racine de période horodatée (RFC 3161) et publiée | [R] |
| BNF-06 | Synchrone : REST pour les frontaux, gRPC en interne. Asynchrone : Kafka « au moins une fois » avec consommateurs idempotents. Couvert par TRV-01 et TRV-04 | [R] |
| BNF-07 | Détection de secrets bloquante dans la chaîne de livraison ; secrets injectés au démarrage depuis le coffre | [R] |
| BNF-08 | Images OCI, composants à code source ouvert uniquement. Conséquences : Snyk est retiré et Redis remplacé (D-25) | [R] |
| BNF-09 | Aucun CDN ni dépôt public, polices et bibliothèques servies localement, registre d'artefacts interne | [R] |
| BNF-10 | Disjoncteur, réessais et délais d'attente par service ; dégradation gracieuse testée | [R] |
| BNF-11 | Identifiant de corrélation sur 100 % des flux, sondes de santé, métriques par service | [R] |
| BNF-12 | Temps de réponse p95 inférieur à 2 000 ms, pour 50 opérateurs internes et les correspondants de plus de 120 institutions | [R] |
| BNF-13 | Inactivité 15 min (RG-56, PCI-DSS 8.2.8) ; délai absolu de **12 h** avec avertissement **2 min** avant expiration ; WCAG 2.2 AA | [R] pour l'inactivité, [P] pour le reste |
| BNF-14 | RPO inférieur à 15 min, RTO inférieur à 1 h, test de restauration trimestriel | [R] |
| BNF-15 | Couverture d'au moins 80 % des branches du code métier ; contrat OpenAPI 3.0 par service | [R] |
| BNF-16 | Aucune donnée de carte ; masquage 6+4 (Luhn, 13 à 19 chiffres) au dépôt, à l'import et à l'affichage ; journaux 12 mois dont 3 en ligne ; preuves 10 ans (D-11) | [R] |
| BNF-17 | Frontal sans état ; aucun jeton dans le LocalStorage ; cookies `HttpOnly`, `Secure`, `SameSite=Strict` | [R] |
| BNF-18 | Français et anglais pour les interfaces, les exports et les messages d'erreur | [R] |
| **BNF-19** | **Disponibilité de la plateforme de 99,9 % par an** : ratifiée comme exigence, car présente dans le CCT (tableau 5.2) et le CC (5.6.2) sans code BNF | [R] |

---

## 4. Décisions de réconciliation

### 4.1 Méthode, structure et traçabilité

- **D-01 [R] Démarche.** Le cycle en V couvre la conception documentaire, puis Scrum la réalisation, avec UML 2.5 en PlantUML (CCT §7.2). La démarche 2TUP citée au CC §4.1 est caduque et sera corrigée en CC v1.2.
- **D-02 [R] Unité de code.** Le dépôt est organisé par microservice (liste du §1). Chaque service est un module autonome : architecture hexagonale, schéma propre, contrat OpenAPI et contrat d'événements propres.
- **D-03 [P] Vue d'analyse par microservice.** Les tableaux des §2 et §5 rattachent chaque UC et chaque RG à un service ; ils constituent la vue d'analyse par microservice exigée par la méthodologie. Une refonte du CA par microservice (v1.2) n'est **pas** un préalable au code. Elle reste possible si l'encadrement l'exige.
- **D-04 [R] Numérotation des RG.** Le CA est la seule source des numéros RG.
  - La colonne RG du CP est **déclarée nulle** : ses numéros ne correspondent pas au CA (par exemple, RG-16 y désigne l'enregistrement, alors que RG-16 est l'immuabilité WORM dans le CA). Les récits restent tracés par BF, UC, épopée et algorithme ; le rattachement aux RG se lit au §5.
  - RG-19 n'existe pas dans le CA : le numéro est **réservé** et ne doit pas être réutilisé.
  - Nouveaux numéros :
    - **RG-74** : une disponibilité n'entre dans la grille qu'après validation hiérarchique ; son justificatif est déposé en GED à accès restreint (demandeur, valideur, RH).
    - **RG-75** : la « Règle A1 » de UC-COL-01 (escalade vers le suppléant après 10 min sans acquittement) est codifiée sous ce numéro.
  - Les références « C-08 », « A-04 » et « D-05 » du CP sont sans source et sont remplacées par les RG du §5.
- **D-05 [R] BF-29 ratifié.** Ce besoin, défini par le CA mais absent du CCT, est un sous-besoin de BF-05 porté par MS-GATEWAY (récit GW-12, priorité M). Il sera reporté en CCT v1.2.
- **D-06 [R] Définition de BF-ADMIN et nouveaux cas d'utilisation.** La définition du CCT fait foi. Pour que chaque besoin ait un cas d'utilisation, trois identifiants sont attribués :
  - **UC-SOC-06** « Déclarer et faire valider une disponibilité », pour BF-ADMIN ;
  - **UC-EXP-05** « Générer et publier la grille d'astreinte », pour BF-17 ;
  - **UC-PRE-05** « Recevoir les événements en temps réel », pour BF-29.
- **D-07 [R, à valider par le PO] Priorités contre plan de livraison.** Le CCT définit « Essentiel » comme « pour la phase 1 ». Or son propre plan (§2.5.2), le CC et le CP livrent BF-15, BF-16 et BF-20 (Essentiels) en vagues.
  - **Le plan de livraison fait foi** : la priorité MoSCoW du CP gouverne la réalisation.
  - « Essentiel » s'entend désormais comme « requis pour le service complet (phase 1 et vagues) ».

### 4.2 Règles métier

- **D-08 [R] Automates d'état.** Les énumérations du CC font foi. Les 4 états de BF-15 dans le CCT sont caducs.

  | Objet | États |
  |---|---|
  | Ticket | `OUVERT → QUALIFIE → EN_COURS ⇄ EN_ATTENTE_TIERS → RESOLU → CLOS`, avec `REOUVERT`. Pas de `CLOS` sans `RESOLU` (RG-36) ; délai gelé en attente de tiers (RG-37) |
  | Institution | `CANDIDAT, EN_HOMOLOGATION, ACTIF, A_REGULARISER, SUSPENDU, RETIRE` |
  | Liaison | `OUVERTE, EN_ESSAIS, PRETE_CERTIFICATION, CERTIFIEE, EN_SERVICE, GELEE` |
  | Opération | `VERROUILLEE, ELIGIBLE, EN_COURS, EN_VALIDATION, CLOSE, EN_ARBITRAGE`, **plus `HORS_PERIMETRE` et `ARCHIVEE`**, utilisés par les algorithmes 6.30 et 6.34 |
  | Dossier de mise en service | `OUVERT, QUITUS_EN_COURS, EN_CORRECTION, CERTIFIE, MISE_EN_SERVICE_AUTORISEE` |
  | Suspens | `OUVERT, AFFECTE, EN_CONCILIATION, CONTESTE, REGULARISATION_DECLAREE, CLOS` |
  | Cellule de crise | `CONVOQUEE, CONSTITUEE, CLOSE, ARCHIVEE` |

- **D-09 [R] Deux règles de clôture distinctes.**
  - BF-11 (quatre éléments : responsable, preuve WORM, horodatage, validation) ne s'applique **qu'aux opérations d'homologation**.
  - Un incident se clôt sur quatre **critères** (CCT §5.2.2, algorithme 6.43) : cause racine, plan de remédiation, test de succès, capitalisation.
  - Le **MTTR** se mesure de l'ouverture du ticket à la validation de ces critères, en temps réel, sans déduire les gels. Le délai imputable, lui, déduit les gels.
- **D-10 [R] Rapport circonstancié.** Il est produit en **moins de 5 minutes** après la résolution d'un incident S1 ou S2 (CCT tableau 5.2 et §6.2.2) ; la valeur de 15 min du tableau 5.1 est caduque. La transmission éventuelle au régulateur (COBAC R-2024/01) reste un acte humain, hors plateforme.
- **D-11 [R] Rétention.**
  - Preuves, registre d'audit, rapports de quart, procès-verbaux et archives de crise : **10 ans**, WORM en mode conformité (RG-18, CC 5.6.3). La durée de 12 mois de RG-53 pour l'archive de crise est remplacée par 10 ans.
  - Journaux techniques : 12 mois, dont 3 en ligne (PCI-DSS 10.5).
  - Salons ordinaires : rétention paramétrée par type de salon.
- **D-12 [R] Relève de quart.** Signatures du sortant **et** de l'entrant, visa du chef de quart (RG-17), puis scellement WORM immédiat (RG-18).
- **D-13 [R] Dégradation de conformité (BF-08).** À J-0 sans régularisation, le système agit automatiquement mais sans jamais suspendre :
  - passage automatique en `A_REGULARISER` ;
  - gel des nouvelles ouvertures de liaisons (RG-68, HOM-10) ;
  - signalement de la non-conformité.
  - Le passage en `SUSPENDU` reste une décision humaine à double contrôle (maker-checker, RG-14), conformément au principe de non-intrusion (PAR-08).
- **D-14 [R] Cellule de crise.**
  - Partage des responsabilités : MS-INCIDENT porte la cellule (convocation, quorum de 4, main courante, clôture) ; MS-COLLABORATION porte le salon, la visioconférence et l'archive, qu'il ouvre sur `incident.crisis-opened.v1`.
  - La cellule passe `CLOSE` uniquement sur rapport post-incident signé (CCT BF-18), rédigé au plus tard **5 jours ouvrés** après la résolution (RG-42).
  - Temporisations paramétrables, avec les valeurs par défaut du CC :

    | Temporisation | Valeurs par défaut |
    |---|---|
    | Acquittement d'une notification S1 | 10 min, puis 30 min vers le suppléant (algorithme 6.23, RG-75) |
    | Acquittement d'une alerte | 5, 15 ou 60 min (algorithme 6.44) |
    | Contact de l'annuaire d'escalade | 15 min, réglable par contact (UC-REF-04) |

- **D-15 [R pour le modèle, P pour l'outil] Grille d'astreinte.** Le CCT impose un programme linéaire en nombres entiers. Or Timefold (OptaPlanner) n'est pas un solveur de ce type et ne fournit pas d'écart d'optimalité (algorithme 6.45).
  - **Port `IntegerProgramSolver` conservé.**
  - **Adaptateur : Google OR-Tools (Apache-2.0, bibliothèques natives embarquables hors ligne), moteur CP-SAT ou SCIP, limite de 300 s.**
  - Timefold est retiré du tableau 4.2 du CC.
- **D-16 [R] Simulateur.** Lecture seule, **par interrogation planifiée** sous mTLS. La « notification d'achèvement entrante » évoquée au CC §6.1.7 n'est pas retenue en phase 1 ; elle reste une extension possible.
- **D-17 [R] Console.** Elle est accessible depuis la **zone d'administration hors bande**, sur des postes dédiés (CC 5.6.1). Les mentions « salle de contrôle » sont caduques. L'« Air-Gap » de la console désigne ce réseau d'administration sans accès Internet (RG-61).
- **D-18 [R] Aptitude et mise en service.**
  - Une liaison est apte à la certification quand la couverture γ vaut 1 pour chaque service inclus et que tout le tronc commun est clos (algorithme 6.33). Le seuil `τseuil` de UC-HOM-04 vaut **1**, non paramétrable, conformément à RG-13.
  - UC-HOM-01 est déclenché par `participant.status-changed.v1` vers `EN_HOMOLOGATION`, et non par l'enregistrement de l'institution.
- **D-19 [R] Événements.**
  - **Nommage** : le format du CC `domaine.agrégat-événement.v1` fait foi. Les noms du CA (`PermissionRevokedEvent`, `DossierMiseEnServiceApprouve`…) désignent les mêmes faits.
  - **Consommation** :
    - MS-AUDIT consomme **tous** les sujets ;
    - MS-NOTIFICATION consomme seulement `*.notification-requested.v1` ;
    - MS-REPORTING consomme les six domaines listés au CC p. 251.
  - **Sujets ajoutés à MS-RAPPROCHEMENT** : `suspens-contested`, `import-rejected`.
  - **Port synchrone d'urgence S1 de MS-NOTIFICATION** : ouvert à MS-INCIDENT, MS-AUDIT, MS-DOCUMENT, MS-MONITORING **et MS-PARTICIPANT** (algorithme 6.29).
- **D-20 [R] Deux sortes de délégation.** L'entité `Delegation` porte un type qui borne sa durée :

  | Type | Durée maximale | Source |
  |---|---|---|
  | `ELEVATION_JIT` (élévation ponctuelle demandée par l'agent et approuvée par un superviseur) | 60 min | RG-01, UC-SOC-01 |
  | `SUPPLEANCE` (remplacement) | 72 h | IAM-06 ; l'exemple d'une vacation de 8 h au CC en est un cas |

- **D-21 [P] Habilitations des correspondants externes.**
  - Les correspondants des institutions reçoivent une famille de rôles `PORTAIL`, limitée aux niveaux 1 (consultation) et 2 (dépôt et création), et restreinte par RLS à leur institution (RG-59).
  - Ils n'accèdent jamais aux niveaux 3 à 5.
- **D-31 [R] Échelle des niveaux d'habilitation.** Le CCT (BF-02) et le CC (§6.1.2.1) numérotent les cinq niveaux dans des sens opposés.
  - **L'échelle du CCT fait foi**, car elle est contractuelle. Du moins au plus privilégié : 1 Consultation, 2 Exécution et création, 3 Approbation, 4 Administration métier, 5 Audit et sécurité. Énumération `HabilitationLevel` : `CONSULTATION, EXECUTION, APPROBATION, ADMINISTRATION_METIER, AUDIT_SECURITE`.
  - Les catégories du CC (administrateur de la plateforme, responsable, opérateur de supervision, ingénieur d'homologation, lecteur ou auditeur) deviennent des **rôles**, chacun rattaché à un niveau de l'échelle.
  - Décision acceptée le 28/09/2026 avec le JDL de MS-IAM.

### 4.3 Technique et exploitation

- **D-22 [R] Clés détenues par la plateforme.** Aucune clé monétique, de PIN ou de HSM du commutateur. La plateforme détient uniquement :
  - les certificats mTLS de chaque service ;
  - les clés de signature des jetons (MS-GATEWAY, MS-IAM) ;
  - la clé de signature de la plateforme (attestations, rapports de quart, rapports périodiques, archives), conservée dans le coffre.

  Les signatures personnelles de quitus passent par une réauthentification WebAuthn : la clé reste sur l'authentificateur. Les clés de chiffrement de bout en bout restent sur les terminaux.
- **D-23 [R] Flux entrants.** En plus des cinq applications tierces, deux flux entrants passifs sont admis (CC tableau 5.4). Ce ne sont pas des applications tierces et la plateforme n'y écrit jamais :
  - la réception d'alertes des outils de supervision du groupement (mTLS) ;
  - le dépôt contrôlé des états importés.
- **D-24 [R] Environnements et dimensionnement.**
  - Environnements (CCT chapitre 9) :
    - **DEV** : VM isolées, Docker Compose ;
    - **INT** : VM d'intégration avec PKI et bus ;
    - **QUAL** : serveurs d'essai du GIMAC, pour la VABF ;
    - **PROD** : production, avec site de secours.
  - Le tableau 5.5 du CC donne le dimensionnement de la phase 1 ; les cardinalités de la figure 5.3 sont la cible de montée en charge.
  - L'énumération `NetworkZone` décrit l'**origine** d'une requête (`PORTAIL_DMZ`, `DEPARTEMENT` = zone d'exploitation, `ADMINISTRATION`), et non les cinq zones de déploiement.
- **D-25 [P] Choix techniques laissés ouverts.** Tous sont en code source ouvert et utilisables hors ligne, conformément à BNF-08 et BNF-09.

  | Rôle | Choix proposé |
  |---|---|
  | Passerelle | Spring Cloud Gateway (natif JHipster) |
  | WAF | Coraza avec les règles OWASP CRS, placé sur les mandataires inverses de la DMZ, en amont de la passerelle |
  | Fédération | OIDC (SAML non retenu) |
  | Tests de contrat | Spring Cloud Contract |
  | Orchestrateur | Kubernetes, distribution adaptée au hors-ligne, à arrêter avec la Production Informatique |
  | Coffre et PKI interne | OpenBao |
  | Antivirus | ClamAV |
  | Observabilité | OpenTelemetry, Prometheus, Grafana, Loki, Tempo |
  | Chaîne de livraison | GitLab CE auto-hébergé |
  | Analyse des dépendances | OWASP Dependency-Check et Trivy avec base hors ligne (remplacent **Snyk**, service SaaS incompatible avec l'isolement) |
  | Cache | **Valkey** (BSD, compatible Redis) : Redis 7.4 n'est plus sous licence libre |
  | Traversée WebRTC | coturn |

- **D-26 [R] Écriture d'audit.** « Interception synchrone » (BF-06) est réalisée ainsi : toute mutation d'état et son événement d'audit sont validés dans **la même transaction** (outbox transactionnelle). L'insertion dans l'arbre de Merkle est ensuite asynchrone et idempotente. Aucune mutation ne peut donc échapper au registre.
- **D-27 [R] Conversion des décisions en tâches (BF-22).** La « conversion sémantique » du CCT est **structurelle** : chaque décision est saisie comme un enregistrement typé (porteur, échéance), puis convertie de façon atomique (RG-51, algorithme 6.55). Aucun traitement du langage naturel n'est prévu. Le projet ne comporte **aucune composante d'IA** ; en ajouter une exigerait une demande de changement du CCT.
- **D-28 [R] Chiffrement de bout en bout et archivage (BF-24).** Le serveur ne voit jamais le clair. L'archivage des salons de crise et des salons liés à un ticket passe par un **terminal d'archivage déclaré et visible comme membre du salon**, qui scelle l'archive (algorithme 6.63).
- **D-29 [P] Données personnelles.**
  - Les justificatifs d'absence (données potentiellement médicales) sont stockés dans MS-DOCUMENT en type restreint (RG-74) : seuls le demandeur, le valideur et les RH y accèdent, et chaque lecture reste journalisée (BF-04).
  - Le cadre national de protection des données personnelles applicable est à confirmer par le RSSI.
- **D-30 [R] Jeu de données de référence** pour les tests et exemples ; il corrige les incohérences des diagrammes d'objets du CC.

  | Identifiant | Rôle ou objet |
  |---|---|
  | `USR-0007` | Chef du DES |
  | `USR-0008` | Chef de quart |
  | `USR-0011` | Responsable Normes et Agréments |
  | `USR-0012` | RSSI |
  | `USR-0013` | Suppléant DG |
  | `USR-0042` | Opérateur de supervision |
  | `INST-0042` | Établissement de paiement A |
  | `INST-0043` | Banque B (`BNK-B`) |
  | `TCK-000318` | Incident S1, cellule `CRI-2026-009`, salon `SAL-CRISE-2026-009` |

---

## 5. Règles de gestion par microservice

Numéros du CA, D-04 appliqué. Les règles ⟂ sont transverses : chaque service qui les touche les applique.

- **Transverses ⟂** :
  - RG-15 et RG-35 : consommateurs idempotents, table de déduplication ;
  - RG-24 : confiance nulle ;
  - RG-25 : mTLS en TLS 1.3 (RFC 8705) ;
  - RG-03 et RG-59 : RLS et cloisonnement par institution ;
  - RG-39 : horodatage RFC 3161 par autorité tierce.
- **MS-GATEWAY** :
  - RG-56 : verrouillage à 15 min d'inactivité et révocation du jeton ;
  - RG-58 : jetons hors du navigateur (BFF) ;
  - RG-60 : aucun accès de la DMZ aux segments internes hors passerelle ;
  - RG-61 : réseau d'administration sans Internet.
- **MS-IAM** :
  - RG-01 : élévation ponctuelle ≤ 60 min (voir D-20) ;
  - RG-02 : séparation des tâches ;
  - RG-57 : FIDO2 avec contrôle de l'AAGUID ;
  - RG-65 : aucun mot de passe local ;
  - RG-66 : revue des accès trimestrielle, approuvée par le chef du DES et scellée ;
  - RG-67 : historique immuable des affectations ;
  - RG-74 : disponibilité validée avant prise en compte.
- **MS-AUDIT** :
  - RG-07 : RFC 9162, préfixes 0x00 et 0x01 ;
  - RG-08 : racine de période publiée hors plateforme et horodatée ;
  - RG-09 : preuve logarithmique sans divulgation de la base.
- **MS-DOCUMENT** :
  - RG-04 et RG-16 : WORM en mode conformité, versions sans écrasement ;
  - RG-05 : SHA-256 obligatoire ;
  - RG-06 : conservation selon la matrice légale et journal de consultation.
- **MS-NOTIFICATION** :
  - RG-62 : niveau d'urgence et trace de remise obligatoires ;
  - RG-63 : préférences de canal respectées, sauf niveau critique en astreinte ;
  - RG-64 : aucune donnée sensible dans un courriel ou un SMS, seulement un lien ;
  - RG-71 : notifications d'escalade S1 et S2 signées.
- **MS-PARTICIPANT** :
  - RG-13 : passage à ACTIF seulement avec un quitus d'homologation à 100 %, appliqué à la réception de `certification.go-live-authorized.v1` ;
  - RG-10 : unicité du code banque BEAC ;
  - RG-11 : compte de règlement BEAC obligatoire pour un participant direct ;
  - RG-12 : convention de parrainage pour un participant indirect ;
  - RG-14 : levée de suspension en double signature DG et Risques ;
  - RG-68 : gel des ouvertures de liaisons en cas de forclusion ;
  - RG-69 : notification signée avec accusé de réception à J-60 et J-30 ;
  - RG-70 : au moins deux contacts d'astreinte 24/7 par participant.
- **MS-HOMOLOGATION** :
  - RG-26 : graphe acyclique ;
  - RG-27 : une opération est éligible si tous ses prédécesseurs sont résolus ;
  - RG-28 : validation du chemin critique ;
  - RG-29 : aucun recalcul ni saisie manuelle d'un résultat d'essai ;
  - RG-30 : aucune trame ni donnée de carte, seulement la référence de la trace ;
  - RG-31 : toute divergence entre référentiel et campagne est tracée ;
  - RG-32 : règle des quatre éléments ;
  - RG-33 : scellement immuable ;
  - RG-72 : un service hors périmètre contractualisé ne peut être certifié.
- **MS-CERTIFICATION** :
  - RG-34 : le comité des changements seul décide, la plateforme référence ;
  - RG-73 : quatre quitus, chacun par un rôle distinct.
- **MS-INCIDENT** :
  - RG-36 : pas de clôture sans passage par RESOLU ;
  - RG-37 : gel du délai en attente de tiers, avec justificatif ;
  - RG-38 : historique des transitions immuable ;
  - RG-40 : critères d'activation de crise non débrayables ;
  - RG-41 : main courante de crise immuable ;
  - RG-42 : rapport post-incident sous 5 jours ouvrés.
- **MS-MONITORING** :
  - RG-17 : double signature et visa du chef de quart ;
  - RG-18 : scellement WORM immédiat, 10 ans.
- **MS-RAPPROCHEMENT** :
  - RG-43 : écarts détectés par identifiants pivots, sans interpréter les états ;
  - RG-44 : aucune modification d'un suspens sans trace d'audit ;
  - RG-45 : clôture seulement sur attestation déposée en GED, aucune écriture en production.
- **MS-PILOTAGE** :
  - RG-20 : porteur unique nominatif ;
  - RG-46 : franchissement de jalon sur visa tripartite (Projets, Sécurité, DES) ;
  - RG-47 : procès-verbaux de comité scellés en WORM ;
  - RG-49 : blocage à la limite d'en-cours ;
  - RG-50 : risque résiduel recalculé à la clôture des actions ;
  - RG-51 : toute décision validée crée une tâche.
- **MS-REPORTING** :
  - RG-21 : consolidation automatique, sans ressaisie ;
  - RG-48 : indicateurs calculés sur un modèle étanche à la production.
- **MS-COLLABORATION** :
  - RG-22 : chiffrement de bout en bout ;
  - RG-23 : aucun relais cloud public ;
  - RG-52 : masquage des numéros de carte avant transmission d'un message ;
  - RG-53 : archive WORM, 10 ans (voir D-11) ;
  - RG-54 : chiffrement des flux médias ;
  - RG-55 : consentement explicite avant tout enregistrement ;
  - RG-75 : escalade vers le suppléant après 10 min.

---

## 6. Socle commun imposé à chaque microservice (définition de fini technique)

1. Architecture hexagonale : domaine pur, ports, adaptateurs ; aucune dépendance de framework dans le domaine.
2. Schéma et rôle PostgreSQL propres ; migrations Liquibase selon le patron Expand/Contract.
3. Outbox transactionnelle ; enveloppe CloudEvents ; identifiant de corrélation propagé ; consommateurs idempotents.
4. Toute mutation d'état émet un événement consommé par MS-AUDIT (D-26).
5. Contrat OpenAPI 3.0 publié et test de contrat ; au moins 80 % de couverture des branches du domaine.
6. mTLS entrant et sortant, conteneur non root, aucun secret dans le dépôt, aucun numéro de carte en clair.
7. Messages et erreurs en français et en anglais ; sondes de santé et métriques exposées.

---

## 7. Errata à reporter dans les documents (v1.2)

**CCT**
- Remplacer les auto-références « v1.0 » (§4.1, §7.2.1.1, matrice RACI) par « v1.1 ».
- Remplacer « six groupes » par « sept domaines » (figure 3.1).
- Harmoniser le délai du rapport COBAC à 5 min (tableau 5.1).
- Remplacer la fin de mesure du MTTR : « critères de clôture d'incident » au lieu de BF-11.
- Aligner les états de BF-15 sur D-08.
- Ajouter COBAC R-2024/01 au tableau 3.1.
- Ajouter BF-29 et BNF-19.
- Corriger le chiffre de participants (« plus de 120 ») et le volume de flux, qui diffèrent d'un chapitre à l'autre (31 788 Md contre 600 Md FCFA).

**CA**
- Remplacer « CCT v1.0 » par « v1.1 ».
- Définir BF-ADMIN dans le tableau 3.2.
- Ajouter UC-SOC-06, UC-EXP-05 et UC-PRE-05, ainsi que RG-74 et RG-75, et marquer RG-19 comme réservé.
- Ajouter l'autorité d'horodatage aux acteurs externes.
- Unifier les libellés « Chef, Directeur, Direction du DES ».

**CC**
- Remplacer 2TUP par cycle en V et Scrum (§4.1).
- Supprimer « anciennement CCT-GIMAC-02 ».
- Appliquer D-15, D-16, D-17, D-19, D-22, D-25 et D-31.
- Aligner les diagrammes d'objets sur D-30.
- Traduire « List of Figures / Tables ».
- Compléter les 13 maquettes.

**CP**
- Remplacer la colonne RG selon le §5.
- Découper les récits de 13 points avant leur itération (TRV-03, TRV-06 et TRV-07 sont en itération 1).
- Tracer BNF-06 (TRV-01, TRV-04).
- Corriger « afin de que » et « 1 récits ».
- Renuméroter le chapitre 5 (sous-sections 5.0.x).

**Tous les documents**
- Désignation des auteurs : « Ingénieur en Informatique et Systèmes d'information ».

---

*Prochaine étape : plan d'implémentation de l'itération 1 (socle : MS-GATEWAY, MS-IAM, MS-AUDIT, TRV-01 à TRV-07 et TRV-09), établi sur la base du présent référentiel.*
