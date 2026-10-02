# REC-GIMAC-01 — Note de réconciliation des livrables

| Élément | Valeur |
|---|---|
| Projet | Plateforme de suivi des activités du Département Exploitation et Supervision (DES), GIMAC |
| Référence | REC-GIMAC-01 v1.0 (28/09/2026) |
| Documents réconciliés | CCT-GIMAC-01 v1.1, CA-GIMAC-01 v1.1, CC-GIMAC-01 v1.1, CP-GIMAC-01 v1.0 |
| Établi par | KOAGNE ISSA RYAN, Ingénieur en Informatique et Systèmes d'information |
| Statut | Référentiel d'implémentation. Décisions D-01 à D-31, y compris les propositions [P], **acceptées par KOAGNE ISSA RYAN le 28/09/2026** ; validation formelle du Product Owner (chef du DES) attendue en revue d'itération |
| Rôle | Base unique à partir de laquelle le code est écrit ; prime sur les quatre documents pour les points qu'elle tranche |
| Révision | **v1.1 (28/09/2026)** : ajout des décisions D-32 à D-46 (§4.4), complétées par D-47 à D-55, pour réconcilier **CP-GIMAC-01 v2.0**, qui remplace CP v1.0, et fixer l'infrastructure et le frontend ; aucune disposition de la v1.0 n'est retirée |

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
| Priorités, estimations, ordonnancement, à compter de la révision v1.1 | **CP v2.0**, dans les mêmes limites (D-32, D-33) |
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

### 4.4 Réconciliation avec CP-GIMAC-01 v2.0, infrastructure et frontend (révision v1.1)

Décisions ajoutées le 28/09/2026 à la réception du carnet de produit v2.0 (313 récits, 85 features, 17 épopées, 1575 points). Elles complètent les décisions D-01 à D-31, qui restent toutes en vigueur.

- **D-32 [R] Carnet de référence.** CP-GIMAC-01 v2.0 remplace CP v1.0 pour les priorités, les estimations et l'ordonnancement.
  - Toutes les dispositions de la présente note qui visent CP v1.0 (D-04, D-07, §7) s'appliquent à la v2.0 dans les mêmes termes.
  - Les récits transverses TRV-xx de la v1.0 sont repris par les épopées SOC, IHM et QUA de la v2.0 ; les identifiants de la v2.0 font foi dans le code, les commits et les procès-verbaux.
- **D-33 [R] Règles de gestion dans CP v2.0.** Le carnet v2.0 ne numérote plus les RG et renvoie aux « RG du domaine N ». D-04 s'applique : le rattachement exact se lit au §5, et le CA reste la seule source des numéros RG.
- **D-34 [R] Périodicité de la revue des accès.** La revue est **au moins trimestrielle** (RG-66, CA ; CCT : « revues trimestrielles des habilitations »).
  - La mention « au moins semestrielle » de IAM-10, IAM-18 et de l'annexe B du CP (BNF-03) est caduque.
  - La périodicité est un paramètre dont la valeur maximale est de 3 mois ; une valeur plus courte est admise.
- **D-35 [R] Signature des quitus.** D-22 s'applique : aucune clé personnelle n'est détenue par la plateforme. La formule « signée par le coffre de clés en mon nom » de CER-02 à CER-05 est interprétée en deux actes distincts :
  1. **Signature personnelle** : le signataire produit une assertion WebAuthn dont le défi est l'empreinte SHA-256 de la décision canonique (liaison, empreinte du socle d'évaluation, décision, motif, signataire, instant). L'assertion complète (identifiant de l'authentificateur, données d'authentification, données client, signature) est conservée avec la décision et vérifiée par MS-IAM (`verifyAssertion`). Elle seule engage la responsabilité du signataire.
  2. **Cachet de la plateforme** : la clé de signature de la plateforme, conservée dans le coffre, contresigne l'ensemble (décision et assertion), puis l'autorité d'horodatage RFC 3161 horodate le cachet. Le cachet atteste l'intégrité, jamais le consentement.
  - Le coffre ne signe jamais au nom d'une personne.
- **D-36 [R] Élévation ponctuelle.** D-20 est maintenue : le carnet v2.0 ne décrit que la suppléance de 72 h (IAM-06), mais l'élévation ponctuelle de 60 min reste exigée par RG-01 et UC-SOC-01.
  - IAM-06 couvre les deux types de `Delegation` ; ses critères d'acceptation sont complétés par un scénario `ELEVATION_JIT` : demande par l'agent, approbation par un superviseur, durée plafonnée à 60 min, renouvellement uniquement par une nouvelle demande.
  - Les événements `iam.delegation-granted.v1` et `iam.delegation-ended.v1` du CP sont adoptés (le CC ne nomme pas ces faits) ; leur charge utile porte le type de délégation.
  - IAM-06 est réestimé lors de l'affinage du sprint concerné.
- **D-37 [R] Nommage des événements du carnet v2.0.** D-19 est maintenue. Le carnet v2.0 cite environ 140 sujets absents du CC. Ils sont traités ainsi :
  1. **Même fait, autre nom** : le nom du CC fait foi. Correspondances établies :

     | Nom du CP v2.0 | Nom retenu (CC) |
     |---|---|
     | `gateway.decision-recorded.v1` | `gateway.access-decided.v1` |
     | `iam.role-assignment-changed.v1` | `iam.role-granted.v1` et `iam.role-revoked.v1` |
     | `participant.institution-status-changed.v1` | `participant.status-changed.v1` |
     | `document.consulted.v1` | `document.document-consulted.v1` |
     | `document.integrity-violated.v1` | `document.integrity-alert.v1` |
     | `document.document-quarantined.v1` | `document.quarantined.v1` |
     | `homologation.referential-published.v1` | `homologation.catalog-published.v1` |
     | `homologation.coverage-changed.v1` | `homologation.coverage-updated.v1` |
     | `incident.ticket-status-changed.v1` | `incident.ticket-transitioned.v1` |
     | `incident.ticket-escalated.v1` | `incident.incident-escalated.v1` |
     | `incident.crisis-quorum-reached.v1` | `incident.crisis-constituted.v1` |
     | `collaboration.room-created.v1` | `collaboration.room-opened.v1` |
     | `rapprochement.suspense-opened.v1` | `rapprochement.suspens-opened.v1` |
     | `rapprochement.suspense-closed.v1` | `rapprochement.suspens-closed.v1` |
     | `rapprochement.suspense-status-changed.v1` | `rapprochement.suspens-assigned.v1` et `rapprochement.suspens-contested.v1` pour ces deux transitions |
     | `rapprochement.statement-rejected.v1` | `rapprochement.import-rejected.v1` |
     | `rapprochement.reconciliation-completed.v1` | `rapprochement.completed.v1` |
     | `reporting.report-generated.v1` | `reporting.report-published.v1` |
     | `reporting.declared-statement-imported.v1` | `reporting.statement-imported.v1` |
     | `pilotage.action-plan-phase-changed.v1` | `pilotage.action-sheet-changed.v1` |

  2. **Fait nouveau** : le sujet du CP est adopté s'il respecte le format `domaine.agrégat-événement.v1`, en reprenant le nom d'agrégat du CC (par exemple `rapprochement.suspens-…` et non `suspense-…`, `pilotage.action-sheet-…` et non `action-plan-…`).
  3. **Source unique** : le registre des schémas (SOC-03) est initialisé selon ces deux règles et devient la liste de référence des sujets ; tout sujet absent du registre est refusé à la publication.
  4. MS-AUDIT consomme tous les sujets, y compris les ajouts (D-19).
- **D-38 [R] Reprise du simulateur (complément à D-16).** L'interrogation planifiée reste le mécanisme obligatoire. Le scénario 2 de HOM-13 (reprise sur notification) est admis comme **accélérateur facultatif** :
  - le point de réception n'accepte que des connexions mTLS du simulateur et ne retient que l'identifiant de campagne ;
  - il déclenche une lecture ciblée de la campagne ; le contenu de la notification n'est jamais pris pour un résultat ;
  - il est désactivé par défaut et activé par configuration quand le simulateur sait émettre ces notifications.
  - La non-intrusion est préservée : la plateforme reçoit et lit, elle n'écrit jamais dans le simulateur.
- **D-39 [R] Entités de MS-GATEWAY.** Les entités du carnet v2.0 sont réparties selon leur nature :

  | Nature | Entités |
  |---|---|
  | Configuration persistée (schéma PostgreSQL du service) | `Route`, `PresentationLayer`, `ApiVersion`, `ContextPolicy`, `RateLimitQuota`, `MaskingRule`, `InspectionRule`, `SecurityHeaderPolicy` |
  | Événement publié par l'outbox, sans table métier | `GatewayDecision`, publiée sous `gateway.access-decided.v1` ; le journal des décisions est celui de MS-AUDIT |
  | État volatil dans le cache Valkey | `InternalToken`, `Revocation` (projection de `iam.session-revoked.v1`), `PushSubscription` et curseurs du canal poussé, compteurs de débit |
  | État d'exécution Resilience4j | `CircuitBreaker` ; ses seuils sont portés par `Route` ou `ApiVersion` |
  | Attribut du jeton, non une entité | `CertificateBinding` : revendication `cnf` / `x5t#S256` du jeton interne (RFC 8705) |

  - `ms-gateway.jdl` est complété par les huit entités persistées avant le premier sprint de l'épopée GW.
- **D-40 [R] Charge des sprints.** Le plan nivelé du CP v2.0 (15 sprints à 110 points) est la base d'engagement de l'équipe. La feuille de route S0 à S12 fixe l'ordre des dépendances.
  - Un sprint de la feuille de route dont la charge dépasse la vélocité (S1 247, S2 308, S4 236 points) est réparti selon le plan nivelé, sans changer l'ordre des dépendances.
  - Un récit de 13 points est découpé avant son sprint (§7, maintenu).
- **D-41 [R] Seuil de couverture.** Les deux seuils s'appliquent et sont bloquants :
  - **80 % des branches du domaine** (BNF-15, contractuel) ;
  - **80 % des lignes des couches domaine et application** (QUA-01).
  - Le code généré et la configuration sont exclus par une liste revue, publiée dans le rapport de couverture.
- **D-42 [P] Forge et chaîne de livraison.** GitLab CE auto-hébergé (D-25) reste la forge cible des environnements INT, QUAL et PROD.
  - Pendant la phase DEV, hors site et sans aucune donnée réelle, le dépôt privé GitHub `issaryan/PLATEFORME-GESTION-INTELLIGENTE-GIMAC` sert de forge de développement.
  - La chaîne est écrite en scripts portables (`scripts/ci/`), appelés par GitHub Actions pendant la phase DEV puis par GitLab CI ; la bascule se fait par miroir du dépôt lors de SOC-14.
  - Aucun secret, aucune donnée réelle et aucune donnée de carte ne transitent par GitHub.
- **D-43 [R] Version de JHipster.** La version cible est **9.4.0**, celle qu'annoncent les JDL.
  - Le socle (MS-GATEWAY, MS-IAM, MS-AUDIT) est déjà généré en 9.4.0.
  - Les onze autres services, générés en 8.11.0, sont régénérés en 9.4.0 au moment de leur passage au gabarit hexagonal (SOC-01), avant leur premier sprint. Aucune version mixte n'entre en recette.
- **D-44 [R] Secrets de développement versionnés.** Les secrets par défaut générés par JHipster (clé `jwtSecretKey`, secrets clients du realm Keycloak de développement, mot de passe du magasin TLS de développement) figurent dans l'historique du dépôt privé.
  - Ils sont **déclarés compromis** et ne sont jamais réutilisés dans un autre environnement.
  - Ils sont remplacés par une injection à l'exécution : fichier `.env` non versionné en DEV, coffre OpenBao à partir d'INT (SOC-06).
  - L'historique n'est pas réécrit : ces valeurs n'ont jamais protégé de donnée réelle, et la réécriture casserait la traçabilité des commits. La ligne de base du contrôle de secrets (QUA-09) les liste comme constats historiques connus.
  - Tout jeton d'accès à la forge divulgué hors d'un coffre est révoqué par son titulaire.
- **D-45 [P] Infrastructure.**
  - **DEV (D-24, SOC-13)** : une description Docker Compose dans `infra/dev`, démarrée par une seule commande, avec vérification préalable des prérequis. Elle comprend :

    | Rôle | Composant (code source ouvert, utilisable hors ligne) |
    |---|---|
    | Base relationnelle | PostgreSQL 16, une base et un rôle par service |
    | Bus d'événements | Kafka en mode KRaft |
    | Registre des schémas | Apicurio Registry (Apache-2.0) |
    | Cache | Valkey |
    | Dépôt objet WORM | MinIO avec verrouillage d'objets |
    | Coffre | OpenBao |
    | Fournisseur d'identité | Keycloak |
    | Politiques d'accès | Open Policy Agent |
    | Simulacres des tiers | WireMock pour le simulateur de certification, la gestion des changements et l'autorité d'horodatage ; Mailpit pour la messagerie |
    | Observabilité (profil facultatif) | OpenTelemetry Collector, Prometheus, Grafana, Loki, Tempo |

  - **INT, QUAL et PROD (SOC-07)** : déclarations Kubernetes versionnées dans `infra/k8s` (Kustomize), avec :
    - un espace de noms par segment du CC 5.6.1 : `gimac-dmz`, puis `gimac-socle`, `gimac-referentiels`, `gimac-exploitation`, `gimac-pilotage` pour la zone applicative, et `gimac-admin` pour la zone d'administration ;
    - une politique réseau de refus par défaut, entrant et sortant, dans chaque espace de noms, puis une matrice de flux autorisés explicite ;
    - le profil de sécurité des pods `restricted` (conteneurs non root, sans privilège) ;
    - un contrôle d'admission Kyverno (Apache-2.0) qui exige des images signées.
  - La zone de persistance reste hors de la grappe, sur des machines dédiées, comme le prévoit le CC 5.6.1.
  - La zone d'exploitation ne porte aucun composant de la plateforme : elle ne contient que les postes clients.
- **D-46 [P] Frontend.** Les trois couches de présentation (§1, D7) partagent un seul espace de travail `frontend/` :

  | Élément | Choix |
  |---|---|
  | Organisation | Espaces de travail npm : applications `departement`, `portail`, `console` ; paquets partagés `theme` (jetons du CC 6.2), `ui` (bibliothèque de composants, IHM-02), `shell` (enveloppe commune, IHM-01), `i18n` |
  | Langage et outillage | React 18.3, TypeScript en mode strict, Vite |
  | État serveur | TanStack Query, sans persistance dans le navigateur |
  | Langues | i18next, catalogues fr et en complets (BNF-17) |
  | Composants | Bibliothèque propre, sur primitives accessibles Radix UI lorsque nécessaire ; pictogrammes linéaires Lucide ; police auto-hébergée Inter avec chiffres à chasse fixe |
  | Thème | Palette, typographie et grille du CC 6.2 exprimées en jetons CSS ; angles droits, bordures de 1 px, aucune ombre ni dégradé ; mode sombre du CC 6.2.2 |
  | Tests | Vitest et Testing Library, axe-core pour WCAG 2.2 AA, Playwright pour les parcours et la non-régression visuelle, catalogue Storybook (IHM-02, IHM-18) |
  | Authentification | Modèle BFF de MS-GATEWAY : cookie de session `HttpOnly`, `Secure`, `SameSite=Strict` ; aucun jeton ni donnée métier dans le navigateur (RG-58) ; verrouillage à 15 min d'inactivité (RG-56) |
  | Déploiement | Fichiers statiques servis par un conteneur nginx non privilégié, un par couche, dans la zone de la couche |

  - JHipster reste configuré sans client (`skipClient`) : aucun frontal n'est généré dans les microservices.
  - Les dépendances npm sont servies par un registre interne en INT, QUAL et PROD, conformément à BNF-08 et BNF-09.

- **D-47 [P] Distribution de MinIO.** L'éditeur de MinIO ne publie plus d'image de conteneur officielle ; le code reste sous licence AGPL-3.0.
  - En DEV, l'image communautaire `pgsty/minio`, reconstruite depuis les sources, est utilisée avec une version épinglée.
  - À partir d'INT, l'image est construite depuis les sources étiquetées de MinIO par la chaîne de livraison (SOC-02), signée et déposée dans le registre interne ; aucune image tierce non reconstruite n'entre en recette.
  - Le port de stockage objet reste le protocole S3 avec verrouillage d'objets : un remplacement de MinIO (par exemple Ceph RGW) ne modifierait que la configuration, jamais le code des services. Le choix est réexaminé avec la Production Informatique avant la mise en production.
- **D-48 [R] Configuration de déploiement générée par JHipster.** Pour rester cohérente avec les services, la configuration de déploiement n'est pas écrite à la main ; elle précise D-45 sans la retirer :
  - **Docker Compose** : `infra/docker-compose/docker-compose.yml` est produit par `jhipster docker-compose` à partir de `infra/docker-compose/.yo-rc.json` (14 services, une base PostgreSQL et un cache par service, Kafka, Keycloak, Prometheus, Alertmanager, Grafana). Il n'est jamais modifié à la main : on le régénère.
  - **Éléments propres au projet** : ils sont ajoutés dans `infra/docker-compose/docker-compose.gimac.yml`, fusionné au lancement par `gimac-dev.sh` : Valkey à la place de Redis (D-25), registre des schémas, MinIO WORM, OpenBao, OPA, simulacres des tiers, messagerie de test, collecteur OpenTelemetry, Loki et Tempo, et suppression des mots de passe d'administration par défaut (D-44).
  - **Images des services** : construites par Jib selon la configuration JHipster de chaque service (`./mvnw -Pprod verify jib:dockerBuild`) ; aucun Dockerfile n'est écrit à la main.
  - **Kubernetes** : les déclarations de `infra/k8s` sont produites par `jhipster kubernetes` ; les politiques réseau par zone, le profil de sécurité des pods et le contrôle d'admission de D-45 y sont ajoutés comme compléments Kustomize, sans modifier les fichiers générés.
  - Un générateur JHipster qui réécrit des métadonnées de service (par exemple la version dans `.yo-rc.json`) sans régénérer le code voit ces modifications annulées.
- **D-49 [R] Déclarations Kubernetes générées et complément du projet.** Elles précisent D-45 et D-48 sans les retirer :
  - **Espace de noms** : `jhipster kubernetes` génère un espace de noms unique, `gimac-des`, et des adresses de service internes à cet espace. Les segments de D-45 (DMZ, socle, référentiels, exploitation, pilotage) sont donc portés par l'étiquette `gimac.cm/segment` des pods plutôt que par des espaces de noms distincts ; la séparation physique des zones (DMZ, administration, persistance) reste assurée par le réseau et les nœuds (CC 5.6.1).
  - **Complément** : `infra/kubernetes-gimac` (Kustomize) s'appuie sur `infra/kubernetes` sans le modifier. Il ajoute la politique de refus par défaut et la matrice des flux par segment, un cache Valkey partagé (SOC-11), le profil de sécurité des pods, le contrôle Kyverno des signatures d'images, l'épinglage des images non versionnées, et corrige les défauts de schéma du rendu généré (valeurs d'environnement numériques de Kafka, arguments de Keycloak).
  - **Secrets** : les Secrets générés par JHipster (mots de passe des bases et de Grafana) sont retirés par le complément. Leurs valeurs, présentes dans `infra/kubernetes`, sont déclarées compromises (D-44). Les secrets sont créés au déploiement par `creer-secrets.sh`, puis délivrés par OpenBao à partir d'INT (SOC-06).
  - **Profil de sécurité des pods** : `baseline` est imposé ; `restricted` est audité et signalé jusqu'au durcissement des pods générés (SOC-07), puis imposé.
  - **Signature des images** : la politique Kyverno est en mode Audit jusqu'à la signature des images par la chaîne (SOC-02), puis en mode Enforce.
  - **Persistance** : les bases PostgreSQL et Kafka générées dans la grappe servent l'environnement INT. En QUAL et PROD, la zone de persistance reste hors de la grappe (CC 5.6.1) : un complément dédié remplacera ces déploiements par des services externes (SOC-04, SOC-07).
- **D-50 [P] Dépôt objet WORM : Ceph RGW remplace MinIO.** Cette décision remplace D-47 et la ligne « Dépôt objet WORM » de D-45, qui restent consignées pour l'historique ; la mention de MinIO au §1 se lit désormais « stockage objet S3 avec verrouillage d'objets ».
  - **Motif** : MinIO n'a plus de distribution officielle maintenue (D-47). Parmi les solutions libres, Garage n'implémente pas le verrouillage d'objets S3 ; SeaweedFS l'accepte mais n'appliquait pas le mode conformité à la suppression (anomalies seaweedfs#8350 et #11333, corrigées récemment et encore jeunes). **Ceph RGW** (LGPL, fondation Linux) applique les modes gouvernance et conformité, la version d'objets, la rétention par défaut et la conservation légale ; son WORM a été évalué conforme aux exigences d'archivage électronique SEC 17a-4(f), FINRA 4511(c) et CFTC 1.31, et il offre la réplication entre sites (multisite) exigée par le site de secours.
  - **Vérifié en DEV le 28/09/2026** : compartiment créé avec verrouillage, rétention COMPLIANCE par défaut, suppression d'une version verrouillée refusée même avec contournement de gouvernance, raccourcissement de rétention refusé, données conservées après redémarrage.
  - **DEV** : Ceph mono-nœud (moniteur, gestionnaire, un OSD BlueStore sur fichier, passerelle RGW) dans un seul conteneur de l'image officielle `quay.io/ceph/ceph`, amorcé par `infra/docker-compose/stockage-objet/ceph-mononoeud.sh`. Réplication 1 et authentification interne désactivée : configuration interdite hors DEV. Rétention par défaut GOUVERNANCE d'un jour pour garder le poste nettoyable.
  - **INT, QUAL, PROD** : grappe Ceph de la zone de persistance, déployée avec la Production Informatique (au moins trois nœuds, réplication 3, multisite vers le site de secours), rétention COMPLIANCE de 10 ans (D-11).
  - **Clients** : les services n'utilisent que le protocole S3 (SDK AWS ou compatible) ; l'initialisation des compartiments utilise l'AWS CLI. Aucun client propre à un éditeur n'est admis.
- **D-51 [P] Initialisation et conteneurisation du frontend.** Précise D-46 sans la retirer :
  - **Génération** : les trois applications sont produites par le générateur officiel `create-vite` (gabarit `react-ts`), puis rattachées à l'espace de travail npm `frontend/` avec le paquet `@gimac/theme` (jetons du CC 6.2).
  - **Version de React** : **React 19.2**, celle du gabarit officiel et la ligne maintenue, remplace React 18.3 de D-46 et la mention « React 18 » du §1, qui restent consignées pour l'historique. Le projet n'utilise pas les composants serveur React : les failles propres à ce mécanisme ne le concernent pas.
  - **Image** : un Dockerfile commun (`frontend/Dockerfile`, argument `APPLICATION`) construit chaque couche avec Node 22 puis la sert par `nginx-unprivileged` sur le port 8080, sous un utilisateur non root, compatible avec un système de fichiers en lecture seule. C'est le seul fichier de conteneur écrit à la main : ni JHipster ni `docker init` ne couvrent un frontend séparé (D-48).
  - **Configuration nginx** : politique de sécurité du contenu stricte (`default-src 'self'`, `frame-ancestors 'none'`), `nosniff`, `no-referrer`, `index.html` jamais mis en cache ; `/api`, `/oauth2` et `/login` relayés vers MS-GATEWAY (BFF, RG-58), avec une résolution DNS différée pour que la couche démarre même sans passerelle.
  - **Docker Compose** : services `frontend-departement`, `frontend-portail` et `frontend-console` dans `docker-compose.gimac.yml`, ports 4200, 4201 et 4202 réglables par `.env`.
  - **Kubernetes** : `infra/kubernetes-gimac/frontend.yaml`, deux répliques par couche, profil `restricted`, segment `presentation` qui ne reçoit que le contrôleur d'entrée et ne joint que la passerelle ; l'étiquette `gimac.cm/zone` rappelle la zone d'origine (DMZ pour le portail, applicative pour l'application du département, administration pour la console).
- **D-52 [P] Reprise de code du projet afriland.** Le code du projet afriland (plateforme documentaire de KOAGNE ISSA RYAN, réalisée à titre personnel, même socle JHipster 8.11, Java 21, Kafka) est réutilisable librement ; la reprise est décidée le 28/09/2026 pour accélérer le sprint 0 et les premières itérations.
  - **Règles de reprise** : paquets renommés en `cm.gimac.des.<service>`, références à afriland retirées, et tout bean repris conditionné par une propriété `gimac.*.actif` à `false` par défaut ; le récit qui l'utilise l'active et le teste. Le code repris est un point de départ soumis à la revue et à la définition de fini du §6, jamais une livraison.
  - **Repris** : passerelle (jeton fantôme et introspection, jeton interne RS256, clés, mTLS sortant, limitation de débit sur Valkey, publication des décisions, propriétés typées) ; dans chaque service, validation du jeton interne, rejeu et file de rebut Kafka, masquage des numéros de carte et données personnelles dans les journaux ; MS-NOTIFICATION (ports et adaptateurs courriel et SMS, résilience, modèles Thymeleaf, WebSocket) ; MS-DOCUMENT (corrélation, fusion PDF) ; MS-AUDIT (JSON canonique RFC 8785, adaptateur de signature).
  - **Non repris** : le service Merkle d'afriland (CRUD sans RFC 9162), les adaptateurs MinIO et OpenTimestamps (contraires à D-50 et à l'horodatage RFC 3161), les composants liés aux entités métier d'afriland (quotas, journaux de requêtes, traces d'audit, règles de masquage). Le relais d'outbox est repris dans la bibliothèque partagée de SOC-05.
  - **Écarts à combler avant activation** : le jeton interne doit porter la revendication `cnf` / `x5t#S256` (RFC 8705, GW-05) ; les routes propres à afriland codées dans le filtre du jeton fantôme sont remplacées par les routes déclarées de GIMAC (GW-19) ; le masquage des journaux est complété par le contrôle de Luhn (6+4, RG-52).
- **D-53 [R] Écarts du code repris comblés, second inventaire d'afriland.** Complète D-52 sans la retirer :
  - **Liaison au certificat (RFC 8705, RG-25, GW-05)** : la passerelle pose dans chaque jeton interne `cnf.x5t#S256`, empreinte SHA-256 en base64url de son certificat client mTLS ; chaque service vérifie, dès que le mTLS est actif, que cette empreinte est celle du certificat présenté sur la connexion. Un jeton intercepté ne peut pas être rejoué par un autre appelant. Le mTLS sortant de la passerelle est limité à TLS 1.3, et les mots de passe de ses magasins n'ont plus de valeur par défaut (D-44).
  - **Routes** : la route propre à afriland est retirée du filtre du jeton fantôme ; l'expression des chemins exemptés de jeton, reprise non ancrée (un chemin contenant « /management » ou « /docs » passait sans authentification), est ancrée sur les seuls chemins techniques. Le point JWKS de la passerelle est repris.
  - **Masquage des journaux (RG-52, PCI-DSS 3.4)** : seule une suite de 13 à 19 chiffres vérifiant la formule de Luhn est masquée, en 6+4 ; les identifiants et horodatages restent lisibles.
  - **Second inventaire** : repris en plus l'administration Keycloak (MS-IAM : sessions, révocation globale, activation, actions requises, fédération ; IAM-03, IAM-07, IAM-13, IAM-15), le fournisseur de jeton de service par `client_credentials` (13 services, IAM-16) et l'exception de transition interdite (7 services à automate d'état).
  - **Non repris** : les adaptateurs d'événements du commutateur (dont un adaptateur « GIMAC ») lisent des échecs de transactions ISO 8583 avec numéro de carte masqué, contraires à la non-intrusion et à l'absence de donnée de carte (D-27, RG-30) ; le moteur de règles, l'import Excel, les sagas et l'extraction de texte des PDF sont propres au crédit d'afriland ; la supervision Docker et Prometheus d'administration n'a pas d'usage dans le périmètre.
- **D-54 [R] Socle vérifié avant le développement ; défauts de génération corrigés.** Constat du 29/09/2026 :
  - **Tests** : les 14 services passent leur suite complète, soit 1 356 tests unitaires et 5 476 tests d'intégration (Testcontainers), et leurs images Jib sont construites.
  - **Plateforme** : démarrée en entier par `gimac-dev.sh demarrer`, elle passe les 48 contrôles de `tester-socle.sh` : santé des 14 services et de l'infrastructure, jeton Keycloak, refus sans jeton, appel d'API authentifié via la passerelle vers quatre services, refus par défaut d'OPA, WORM Ceph en conformité, simulacres, en-têtes de sécurité des trois couches, collecte Prometheus des 14 services.
  - **Défauts du générateur JHipster corrigés** :
    - la passerelle n'avait aucune route vers les microservices, et la résolution `lb://` n'y fonctionne pas sans découverte de services : 13 routes déclarées vers `gimac.services.<service>` (localhost en DEV, nom du service en prod) ;
    - `@DecimalMax(value = "0")` produit pour `max(0.9)` sur le coefficient de maîtrise des risques (MS-PILOTAGE, formule 6.64) : rétabli à `0.9`, faute de quoi l'API aurait refusé tout coefficient non nul ;
    - le Docker Compose généré ne fait pas dépendre les services de Kafka : dépendance et délai de santé ajoutés au complément ;
    - tests d'intégration générés en conflit avec des champs uniques (auto-références, valeurs « modifiées » contenues dans la valeur par défaut, lien un-pour-un) : corrigés dans 9 classes de test.
  - **Activé** : masquage des journaux (numéros de carte vérifiant Luhn en 6+4, jetons, téléphones, courriels) dans les 14 services. **Restent inactifs** jusqu'à leurs récits : jeton fantôme et liaison au certificat (GW-04, GW-05, SOC-09), mTLS, administration Keycloak (IAM-03), jeton de service (IAM-16), file de rebut Kafka (aucun consommateur `@KafkaListener` encore).
  - **Passerelle** : la dépendance Redis réactive reprise d'afriland active une sonde de santé Redis ; la passerelle reçoit son Valkey (Docker Compose) ou le Valkey partagé (Kubernetes).
- **D-55 [R] Lot 1, Phase 0 : alignement du socle sur le plan PI-GIMAC-01 v1.0.** Le plan d'implémentation du lot 1 (module de gestion de la certification : MS-HOMOLOGATION, MS-CERTIFICATION, écrans DES-08 à DES-12 et POR-03) est le sprint backlog exécutable du lot ; il est versé dans `docs/lot1` avec le système de conception DS-GIMAC-01. Arbitrages :
  - **Modèle de données** : les JDL du plan (§0.2) remplacent ceux de MS-HOMOLOGATION et MS-CERTIFICATION établis d'après CC-GIMAC-01, qui ne portaient que du CRUD généré ; les deux services sont régénérés en JHipster 9.4.0 (D-43), sans cache, avec Cucumber. Paquets, noms de base et ports restent ceux du dépôt (le dépôt fait foi, PI §0.1).
  - **Tableau de correspondance** : `docs/lot1/correspondance.md` relie le plan au dépôt ; le plan n'est pas réécrit.
  - **Identifiants** : toutes les entités des deux services ont un identifiant UUID (`id UUID` ajouté au JDL), comme le suppose le plan pour ses chemins, ses événements et les références entre services.
  - **Sécurité** : domaine Keycloak `gimac` (permissions du lot, six profils, sept utilisateurs de test, claim `institution_id`, clients `tester-cli` et de service, secrets en variables d'environnement), dérivé par script du realm généré ; la passerelle et les services acceptent comme autorités les permissions en majuscules, en plus des rôles `ROLE_*` ; la passerelle soumet les routes `/bff/**` à OPA (filtre `OpaAuthorize`) et renvoie 401 sans jeton ; la politique du plan est corrigée pour exiger l'institution sur le portail.
  - **Messagerie** : outbox transactionnelle, relais CloudEvents, consommateur idempotent et file de rebut `.dlq` (rejeux 1 s, 5 s, 25 s) actifs dans les deux services.
  - **Écarts assumés** : `lb://` remplacé par les adresses directes (D-54) ; React 19 (D-51) ; signature des quitus selon D-35 ; noms d'événements tranchés récit par récit selon D-37 et consignés dans le tableau de correspondance.

### 4.5 Arbitrages de la certification d'après le compte rendu de cadrage (révision v1.2, 29/09/2026)

Source ajoutée : **CR-CERT-GIMAC-01 v1.4**, compte rendu d'entretiens et de cadrage de la certification (document amont du CCT, décrit l'existant), et la présentation de vision *Vision_GIMAC_Digitalisation* adressée à la DG. Les deux sont versés à la racine du dépôt. Arbitrages décidés par KOAGNE ISSA RYAN le 29/09/2026.

- **D-56 [R] Décision de mise en production : modèle hybride.** Le CR ne connaît que deux verrous ; le plan PI et le CC prévoient quatre quitus. Retenu :
  - **Verrou 1, obligatoire : procès-verbal de certification.** Établi par le conducteur de projet (rang 18), il n'est émis que si la couverture de certification du périmètre vaut 100 % (rang 17). Il est signé par le GIMAC et par le participant (rang 19), chaque signature après réauthentification forte (D-35). Il clôt l'étape 8 et fait passer la liaison à `CERTIFIEE`.
  - **Verrou 2, obligatoire : approbation du changement par la sécurité des SI.** Elle est lue depuis la fiche de changement du dispositif existant, référencée et jamais refaite (CR p.32, D-16, RG-34). La vérification et l'archivage des attestations du participant (rangs 21 à 23 : ISO 27001, PCI DSS ou GSMA selon l'activité, conformité COBAC si fintech) sont tracés. L'approbation fait passer la liaison à `APPROUVEE_SECURITE`, et la mise en production ne peut commencer qu'après.
  - **Visas optionnels : conformité et Direction Générale.** Ils sont paramétrables par type de projet (désactivés par défaut) et, s'ils sont activés, s'insèrent avant l'approbation de la sécurité avec les mêmes règles de signature, d'immuabilité et de caducité (CER-06, CER-07).
  - Le « quitus technique du chef du DES » du plan devient la **signature GIMAC du procès-verbal**, portée par le chef du DES ou son délégataire (le CR ne nomme pas le signataire).
  - Récits touchés : CER-01 à CER-07 sont réécrits selon ce modèle ; CER-08 à CER-15 (dossier de mise en service, procès-verbal de mise en service, scellement, attestation, registre, révocation) sont conservés.
- **D-57 [R] Début du parcours : le participant, puis la liaison.**
  - **Étapes 1 et 2 dans MS-PARTICIPANT.** Prise de contact (rang 1) : le candidat est enregistré (`CANDIDAT`, « Prospect » du CR). Adhésion (rang 2) : le dossier administratif est collecté, pièce par pièce, et son complétude constatée ; l'adhésion est prononcée par la DDRM (`ADHERENT`). Un candidat ou un adhérent peut être **clos sans suite** (`CLOS_SANS_SUITE`), avec motif. Ces deux états complètent l'automate de D-08 ; `EN_HOMOLOGATION` est atteint à l'ouverture de la première liaison.
  - **Étapes 3 à 10 portées par la liaison (MS-HOMOLOGATION).** La liaison s'ouvre au cadrage (rang 3) sur un adhérent. Elle reprend les statuts du CR comme **étape courante** : Cadrée, En intégration, En test, Certifiée, Approuvée sécurité, En production ; plus Suspendue et Close sans suite. L'automate technique du plan (HOM-19) est conservé et projeté sur ces étapes.
  - Le **dossier numérique** du participant agrège les deux (identification, pilotage, périmètre, progression, blocages, preuves : CR Fig. 7.3). Un service supplémentaire rouvre le parcours au cadrage sur une **nouvelle liaison**, sans repasser par l'adhésion (rang 35).
- **D-58 [R] Profils et conduite de projet.**
  - Les profils Keycloak restent **par fonction** : DDRM (chargé et chef de département), chef du DES, Normes et Agréments, production monétique, pôle réseau, pôle système, sécurité des SI, conformité, DG, correspondant de participant.
  - Un **rôle dédié `CONDUCTEUR_PROJET`** s'y ajoute. Il n'est pas propre à la certification : il habilite à conduire **tout projet** (une liaison de certification, un projet d'évolution de MS-PILOTAGE…). Le conducteur d'un projet donné est **désigné sur ce projet** par les chefs de département (rang 6), parmi les détenteurs de ce rôle ; il ne voit et n'agit en conducteur que sur les projets qui lui sont confiés.
  - **Précision (29/09/2026), étape 3.** La liaison est ouverte au cadrage par un chef de département (chef du DES ou de la DDRM), qui désigne le conducteur de projet dans le même acte (permissions `HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE`) ; l'ingénieur d'homologation n'ouvre plus de liaison. Le conducteur retrouve les liaisons qui lui sont confiées dans « Mes projets » ; la redésignation est motivée et tracée.
  - Chaque acteur dispose de **son tableau de bord** et des seules actions que lui confère sa fonction (CR §3.6, vision diapositive 14), avec un périmètre de visibilité (D-21, RG-59).
- **D-59 [R] Apports structurants du CR intégrés au modèle.**
  - **Workflow d'étape à six éléments** : responsable unique, actions, livrables, validation, preuve, date de clôture ; une étape n'est close que si les six sont réunis (CR §7.1). Qualités sur une tâche : R, C, V, I, E (CR p.19).
  - **Plateforme d'exécution** de chaque opération : switch interbancaire, switch délégataire, plateforme de paiement mobile, simulateur (CR p.38, p.54).
  - **Deux grains de preuve** : une pièce par phase en essai (le document de paramétrage, rang 15), une preuve par opération en production ; l'opération sait lequel elle attend (CR p.43).
  - **Blocage** : objet daté (motif, depuis le, prochaine action, échéance de levée), et non mention libre (CR annexe B).
  - **Aucun secret** : ni composante de clé, ni mot de passe ; seulement la trace et le code de contrôle de clé (CR p.34, p.54). Les preuves à valeurs de référence (adresse de point de terminaison, code membre, BIN) sont relisibles et comparables.
  - **Pilotage** : 4 compteurs (projets actifs, bloqués, tâches en retard, échéances proches), répartition par étape, délai moyen de certification, taux de respect des délais, et les 6 questions de l'annexe B comme critères d'acceptation.
  - **Référentiel réel** : l'annexe A du CR (rangs 1 à 35, C1 à C10, M1 à M17, D1 à D34) sert de jeu d'initialisation des référentiels. La branche mobile compte les **17** opérations listées ; les mentions « 21 » du CR sont une erreur de comptage.
- **D-60 [R] Méthode de réalisation : tranches verticales.** Chaque tranche livre, pour une étape du parcours, l'API métier, l'écran branché sur l'API réelle et le tableau de bord de chaque acteur concerné. Première tranche : la DDRM (étapes 1 à 3). Les tests d'intégration (Testcontainers) et les contrôles cURL s'exécutent sur le poste de développement ; l'environnement distant se limite à la compilation, aux tests unitaires et aux tests du frontend.

- **D-61 [R] Définition de fini des tests et accès par double facteur (29/09/2026).**
  - **Quatre types de tests obligatoires**, ceux que nomment les fiches du carnet de produit : **unitaires**, **d'intégration** (Testcontainers), **de sécurité** (sur chaque route : 401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec elle ; séparation des pouvoirs), **de recette** (scénarios Gherkin du carnet, Cucumber). Les tests de contrat du plan PI restent exigés pour les événements consommés par un autre service.
  - **Couverture d'au moins 80 %** de toutes les fonctions et de tous les services, sécurité comprise : JaCoCo bloque la construction. À `test` (sans Docker) : chaque classe écrite pour le métier à 80 % en lignes et en méthodes, l'ensemble à 80 % en branches. À `verify` (poste de développement) : unitaires et intégration fusionnés, tout le service à 80 % en lignes et en branches. Côté interface : seuils Vitest de 80 % et parcours Playwright par acteur. Un récit n'est pas fini sans ces quatre types et ces seuils.
  - **Accès par la matrice des rôles d'un jeton à deux facteurs.** L'accès à chaque tableau de bord et à chaque action se fait sur la matrice des rôles (permissions) portée par le JWT obtenu après une authentification à deux facteurs. Keycloak pose le niveau d'assurance (`acr`, LoA 2 : mot de passe et OTP ou FIDO2) ; la passerelle et chaque service refusent (403) un jeton utilisateur de niveau inférieur sur les routes métier ; l'interface n'affiche que les tableaux de bord et les actions permis par ce même jeton. Les décisions engageantes demandent en plus une réauthentification forte (IAM-07, en-tête `X-Reauth-Assertion`).
  - La recette Cucumber est renommée `RecetteGherkinIT` et s'exécute au `verify` avec les tests d'intégration.
  - **Précision du seuil de `verify` (29/09/2026).** Tout le service est tenu à 80 % en **lignes et en fonctions** (méthodes), hors implémentations MapStruct produites à la compilation (`*MapperImpl`, code absent des sources). Les **branches** restent exigées à 80 % sur le code écrit à la main (seuil de `test`). Motif : les branches non couvertes des services générés sont à plus de 80 % dans les implémentations MapStruct et les critères JHipster. Règle identique dans MS-PARTICIPANT, MS-IAM et MS-DOCUMENT.

  - **Interface (29/09/2026).** En plus des seuils Vitest, les parcours de bout en bout (Playwright, scénarios au format Gherkin quand c'est possible) couvrent chaque acteur et chaque couche ; leur couverture de code, instrumentée (istanbul), doit atteindre **80 %** en lignes, fonctions, branches et instructions, sinon la construction échoue.

- **D-64 [R] Messagerie instantanée de travail entre acteurs du GIMAC, premier incrément de MS-COLLABORATION (30/09/2026).** Les acteurs internes du GIMAC échangent des messages directs ou de groupe, comme dans une messagerie d'équipe ou un client de courriel.
  - **Destinataires choisis dans l'annuaire** de la plateforme (comptes internes actifs de MS-IAM, D-62) ; aucune adresse libre. Les participants externes n'ont pas accès à la messagerie dans cet incrément.
  - **Étiquette de projet** : un message peut citer une ou plusieurs liaisons (référence `LIA-AAAA-NNN`) ; l'étiquette est un lien qui ouvre directement le dossier de la liaison (`/des/parcours/{id}`), sous réserve des droits du lecteur, vérifiés à l'ouverture. Une conversation peut aussi être rattachée à une liaison.
  - **Pièces jointes** déposées dans MS-DOCUMENT (dépôt WORM, empreinte SHA-256), référencées par le message ; formats et taille bornés (PDF, images, bureautique ; 20 Mo) ; téléchargement réservé aux membres de la conversation, chaque lecture journalisée.
  - **Temps réel** : un nouveau message est poussé aux membres connectés par le canal poussé de la plateforme ; compteur de non-lus dans la barre supérieure et la navigation.
  - **Sécurité de cet incrément** : jeton à deux facteurs et permissions `COL_MESSAGE_*` ; seuls les membres lisent une conversation ; messages chiffrés en transit (TLS) et au repos (AES-256-GCM, clé hors base) ; RG-52 appliquée : tout numéro de carte (PAN) est masqué avant l'enregistrement ; un message envoyé est immuable (correction par nouveau message) ; conservation selon RG-53.
  - **Accès de tout membre interne, dans le DES et dans la console (01/10/2026).** Tout compte interne du GIMAC (sans institution dans le jeton, second facteur, session non restreinte) dispose de la messagerie, quel que soit son profil : les 13 profils internes portent `COL_MESSAGE_READ` et `COL_MESSAGE_SEND`, et OPA refuse la messagerie à tout jeton portant une institution (correspondants). Le RSSI et le super administrateur, qui travaillent dans la console, y retrouvent la même messagerie (entrée « Messagerie », pastille des non-lus, canal poussé `/bff/console/live` au sujet `collaboration`), servie par `/bff/console/collaboration/messagerie/**`. Dans la console, les liaisons citées s'affichent en texte (pas d'accès aux liaisons depuis cette couche). L'ancienne entrée « Messagerie » de la console, qui configurait le serveur d'envoi des courriels, devient « Courriels de la plateforme » (`/console/plateforme/courriels`).
  - **Écart assumé et borné** : le chiffrement de bout en bout (RG-22, D-28, BF-24) et l'archivage scellé par terminal déclaré restent exigés pour les salons de crise et les salons liés à un ticket (V3) ; la messagerie de travail de cet incrément n'en porte pas encore, le serveur voit le clair pour appliquer RG-52. Le RSSI confirme cet écart avant la mise en production.

- **D-63 [R] Interface par composition du système de conception (29/09/2026).** Tout élément d'interface est bâti sur les composants existants de `frontend/packages/design-system` et les jetons de `@gimac/theme`, définis depuis le départ. Un nouveau composant n'est qu'un assemblage de composants déjà établis : aucun widget ni style propre (couleurs, espacements, boutons, champs, tableaux, dialogues) hors du système de conception. Un assemblage réutilisable (tuile de mesure, assistant par étapes, grille de permissions…) entre dans le système de conception, avec ses tests et son entrée au catalogue (`apps/catalogue`).

- **D-62 [R] Administration complète depuis la plateforme, sans console Keycloak (29/09/2026).** Ni le super administrateur, ni le RSSI, ni un utilisateur n'ouvre la console d'administration de Keycloak : Keycloak reste le moteur d'authentification (mots de passe, TOTP, niveaux d'assurance), piloté par MS-IAM.
  - **Aucune inscription libre.** Tout compte est créé depuis la console par le super administrateur (`ROLE_SUPER_ADMIN`) ou le RSSI (`ROLE_RSSI`), avec invitation par courriel ou mot de passe provisoire. Ils gèrent le cycle de vie complet : fiche, profils, activation, suppression, actions exigées (mot de passe, second facteur, profil, vérification du courriel), réinitialisation du second facteur, fermeture des sessions. Seul le super administrateur attribue un profil privilégié (`IAM_ROLE_PRIVILEGED_GRANT`) ; nul ne s'administre soi-même ; le dernier super administrateur ne peut être retiré.
  - **Premier accès.** À la première connexion, l'utilisateur complète sa fiche (téléphone, langue, fonction), accepte la charte et configure son second facteur ; tant que ces étapes restent, aucun tableau de bord n'est servi. « Mon compte » permet ensuite de modifier sa fiche, de changer de mot de passe ou de second facteur, d'enrôler ses clés FIDO2 et de fermer ses sessions.
  - **Configuration de la plateforme depuis la console** : politique de sécurité (mots de passe, TOTP, sessions, force brute) sous des planchers non négociables (12 caractères, protection contre la force brute obligatoire…), messagerie des invitations, composition des profils en permissions. Chaque acte est motivé, journalisé (journal d'administration immuable) et publié vers l'audit (`iam.account-administered.v1`, `iam.configuration-changed.v1`). Les actes sensibles (suppression, profils, politique, composition d'un profil) exigent la réauthentification forte FIDO2 (IAM-07, finalité `ADMIN_SENSITIVE`).
  - **Supervision en temps réel** pour le super administrateur (et en lecture pour le RSSI) : état des services, des serveurs et des conteneurs, alertes, configuration masquée, niveaux de journalisation (MS-GATEWAY `/api/plateforme/**`).
  - **Pages du fournisseur d'identité aux couleurs du GIMAC** : connexion, second facteur, mot de passe à renouveler, configuration du TOTP et courriels d'invitation (thème `gimac`, `infra/keycloak/themes`). Le profil utilisateur de Keycloak déclare les attributs de la plateforme, modifiables par l'administration seulement (`institution_id`, téléphone, fonction, fin du premier accès).
  - Mise en œuvre : MS-IAM (`/api/v1/comptes`, `/api/v1/moi`, `/api/v1/configuration`, `/api/v1/reauthentification`), routes `/bff/{des,portail,console}/iam/moi` et `/bff/console/iam/**` de la passerelle, règles OPA correspondantes, console d'administration (`frontend/apps/console`).
  - **Complété par D-69** : comptes, journal, profils et politique de sécurité s'administrent aussi depuis l'application du département.

- **D-65 [R] Connexion et configuration des comptes entièrement sur la plateforme (30/09/2026).** Exigence de KOAGNE ISSA RYAN : l'utilisateur ne voit jamais une page de Keycloak, ni pour se connecter, ni pour configurer son compte. Complète D-62 et remplace son alinéa « Pages du fournisseur d'identité aux couleurs du GIMAC », conservé pour l'historique ; D-61 (double facteur, `acr` 2 ou 3 exigé) reste entier.
  - **Écran de connexion de la plateforme** : identifiant et mot de passe, puis code TOTP, saisis dans l'application de chaque couche. La passerelle (BFF) les soumet à Keycloak par l'octroi direct (serveur à serveur, jamais depuis le navigateur), pose le témoin de session HttpOnly et régénère l'identifiant de session. Aucun jeton n'atteint le navigateur. La délégation par redirection (`/oauth2/authorization/oidc`) est retirée.
  - **Keycloak, moteur invisible** : il vérifie le mot de passe et le code TOTP, applique la protection contre la force brute et émet les jetons (`acr` 2 seulement après le code). Deux flux d'octroi direct : mot de passe seul (client `gimac-connexion`, `acr` 1, session restreinte au premier accès) et mot de passe avec code obligatoire (client `web_app`, `acr` 2). Keycloak n'est pas exposé aux utilisateurs.
  - **Premier accès et actions exigées dans la plateforme** : changement du mot de passe provisoire, configuration du TOTP (code QR affiché par la plateforme, secret enregistré dans Keycloak par une extension `gimac-totp` après vérification d'un premier code), fiche, charte. Les actions exigées sont tenues par MS-IAM (attribut `gimac_actions_requises`), jamais par les actions requises de Keycloak, qui bloqueraient l'octroi direct.
  - **Courriels de la plateforme** : invitation et mot de passe oublié envoyés par MS-IAM, avec un lien vers la plateforme (jeton aléatoire à usage unique, conservé haché : 24 h pour une invitation, 30 min pour une réinitialisation).
  - **Mise en œuvre** : contrat dans `docs/lot1/connexion.md` (passerelle, MS-IAM, Keycloak, frontend).

- **D-71 [R] Tableau de bord d'administration dans le département, retour depuis la console, actions réservées (02/10/2026).** Demandes de KOAGNE ISSA RYAN ; complète D-69.
  - **Plus de listes de liens au tableau de bord** : le RSSI et le super administrateur y trouvent la gestion des utilisateurs de la console (barre d'actions, indicateurs des comptes cliquables vers la liste filtrée, comptes par profil pour le RSSI) et les derniers actes d'administration. Les raccourcis « Mes accès » ne paraissent que pour un profil sans aucune section ; sans tableau d'administration fourni, la section se réduit à une barre d'actions.
  - **Retour depuis la console** : entrée « Retour au département » en tête de la navigation de la console, vers le tableau de bord du département (`URL_DEPARTEMENT` à la construction, `GIMAC_URL_DES` sur le serveur).
  - **Actions et lectures réservées à leur permission** : « Nouveau brouillon » des référentiels (`HOM_REFERENTIEL_EDIT`), onglet de simulation de migration (`HOM_REFERENTIEL_MIGRATE`) et export du registre (`CER_REGISTER_EXPORT`) ne paraissent plus pour un simple lecteur, ce qui supprime les refus 403 constatés avec le super administrateur.

- **D-70 [R] Coquille et recherche globale (02/10/2026).** Demande de KOAGNE ISSA RYAN ; complète D-65 (coquille fixe) et DS-GIMAC-01 §6.1, §6.2 et §7.4.
  - **Barre latérale du haut jusqu'en bas** de la fenêtre ; la barre supérieure et le pied de page sont à sa droite, sur la largeur du contenu. La page ne défile toujours pas : seule la zone de contenu défile. Sous 1 280 px, la navigation reste un tiroir.
  - **Recherche globale** : le bouton « Rechercher » de la barre supérieure, Ctrl+K (Cmd+K) ou « / » hors saisie ouvrent la palette de recherche. Elle propose les écrans permis de la navigation, puis, dès deux caractères, les objets cherchés dans les services selon les permissions : liaisons (référence, institution), institutions membres, conversations de la messagerie, comptes (porteurs de `IAM_ACCOUNT_READ`). Les flèches parcourent, Entrée ou un clic ouvre l'objet, Échap ferme. Une source en échec n'empêche pas les autres. Département et console seulement ; ni le portail ni la session restreinte du premier accès n'ont de recherche. Le champ de recherche inerte de la barre supérieure est retiré.

- **D-69 [R] Administration des comptes dans l'application du département (02/10/2026).** Arbitrage de KOAGNE ISSA RYAN, à la suite d'un défaut signalé : depuis le département, « Comptes » ouvrait la console sur une autre origine, avec un autre tableau de bord et un autre thème (préférence d'affichage propre à chaque origine). Complète D-62.
  - **Écrans dans le département** : le RSSI et le super administrateur gèrent les comptes (liste, création, fiche et actes), le journal d'administration, les profils et leurs permissions et la politique de sécurité sous `/des/administration/**`, dans la coquille, la navigation et le thème du département. Mêmes règles qu'en console : actes motivés et journalisés, réauthentification forte pour les actes sensibles, séparation des pouvoirs, octroi d'un profil privilégié réservé au super administrateur.
  - **La console reste** l'outil de la plateforme : supervision en temps réel, configuration des services, niveaux de journalisation, serveur de courriels. Le département n'y mène que par le lien « Console de la plateforme » (permission `PLATFORM_CONFIG_READ`). Le serveur de courriels n'est pas joignable depuis le département.
  - **Mise en œuvre** : paquet `@gimac/administration` (écrans, services et libellés de l'administration, communs à la console et au département) ; route `des-iam-administration` de la passerelle (`/bff/des/iam/comptes/**`, `/bff/des/iam/configuration/{politique-securite,profils,permissions}`) ; règles OPA `/bff/(des|console)/iam/…` aux mêmes permissions ; profil de démonstration du super administrateur dans le département.

- **D-68 [R] Messagerie : annuaire, groupes de discussion, aperçu des pièces jointes (02/10/2026).** Demande de KOAGNE ISSA RYAN ; complète D-64.
  - **Annuaire** : on écrit à une personne choisie par son vrai nom parmi les comptes internes actifs du GIMAC (MS-IAM), cherchée par nom, fonction ou service, avec sa photo et sa fonction ; le champ à liste déroulante et l'objet du message direct sont retirés.
  - **Groupes de discussion** : groupe nommé (nom obligatoire, premier message facultatif), dont le créateur est l'administrateur. L'administrateur renomme le groupe, ajoute et retire des membres ; tout membre peut le quitter ; si l'administrateur part, le membre le plus ancien lui succède. 51 membres au plus. Chaque changement est consigné au fil par un message d'événement, sans contenu.
  - **Pièces jointes** : consultation dans une fenêtre d'aperçu (PDF, image, texte et CSV), téléchargement depuis cette fenêtre ; les autres formats invitent au téléchargement. La CSP de nginx admet les adresses `blob:` pour les images et les cadres, et elle seule.
  - **Mise en œuvre** : MS-COLLABORATION (migration `20261002090000_col_groupes`, trois routes : `PATCH /conversations/{id}`, `POST` et `DELETE /conversations/{id}/membres`), OPA (`COL_MESSAGE_SEND`), frontend `@gimac/lot1` et composant `AttachmentViewer` du système de conception ; contrat dans `docs/lot1/messagerie.md`.

- **D-67 [R] Nom de la plateforme « Moabi » et écran de connexion (01/10/2026).** Arbitrage de KOAGNE ISSA RYAN.
  - **Nom** : *Moabi*, arbre géant et pluricentenaire des forêts du Cameroun, du Gabon, du Congo et de la Guinée équatoriale ; il marque la collaboration et la durabilité. Il s'écrit sous le logo du GIMAC, qui reste la marque institutionnelle (titre des onglets : « Moabi · … »). Disponibilité comme marque à vérifier avant la mise en production.
  - **Écran de connexion (TRA-01)**, sur une seule page : carte d'accès à gauche posée sur les **cernes du bois** (coupe d'un tronc au trait fin couleur terre, un cerne sur dix en bleu, cœur caché par la carte, effacés vers la droite avant l'arbre), à filet tricolore bleu, vert, terre, avec le logo GIMAC seul (sans la bande des drapeaux), le titre « Connexion », les champs, « Mot de passe oublié ? » au niveau du libellé, un bouton pleine largeur et une note sur le code à six chiffres et la clé de sécurité ; nom « Moabi » en grand à droite, devant l'**arbre moabi** en filigrane (racines-contreforts, fût, houppier en parasol, feuilles vertes, une grappe sur cinq bleue ; masqué sous 1 024 px), **guilloché en mode clair, en contour en mode sombre**, avec une **feuille de moabi à la place du point du i**, souligné du filet tricolore et signé « GIMAC · Afrique centrale ». Aucune description de la plateforme sous le logo. Langue dans le menu surgissant du globe, bascule directe du thème ; couche, classification et heures de Douala et UTC en pied de page. La mosaïque de tuiles est retirée.

- **D-66 [R] Finition de l'interface, dérogations à DS-GIMAC-01 (01/10/2026).** Arbitrages de KOAGNE ISSA RYAN sur les écrans ; DS-GIMAC-01 reste la référence pour tout le reste (sobriété, rayon 0, aucune ombre ni dégradé, couche sémantique, typographie, mode sombre) et passe en version 1.1.
  - **Surbrillance sans filet vertical** : l'entrée de navigation active et tout élément mis en évidence sont marqués par une teinte de fond et un texte contrasté (blanc en mode sombre), jamais par un filet gauche de 4 px (remplace DS §5.3 et §6.2 sur ce point).
  - **Logo seul** dans la barre latérale, sans les drapeaux ni cartouche, version blanche sur la barre bleue ou sombre, couleur sur fond clair ; pas de nom de couche sous le logo ; pas de bandeau d'environnement (« PRODUCTION ») dans la console (remplace DS §2.3, dernier interdit, et §6.1).
  - **Barre latérale sombre en mode sombre**, texte actif blanc ; contraste ≥ 4,5:1 vérifié par test dans les deux thèmes.
  - **Sélecteur de langue** (FR, EN, ES) en menu surgissant ancré au bouton, sans déformer la page.
  - **Listes** : pagination toujours visible ; **gestes CRUD** dans la modale standard du système de conception (création, modification, suppression, action sensible, confirmation, détail).
  - **Aucun texte orphelin** : tout texte est porté par un composant stylé du catalogue (en-tête de page, description de section, note).
  - **Photo de profil** : l'avatar affiche la photo choisie par le titulaire (contrat docs/lot1/mon-compte.md), à défaut ses initiales ; il reste la seule forme ronde (remplace « jamais de photo » de DS §7.3).
  - **Fond texturé des discussions** : le motif de trame (DS §5.6) habille le fond du fil de la messagerie et son état vide, à opacité réduite et décliné en mode sombre ; les bulles, la saisie, l'en-tête du fil et la liste gardent un fond plein, de sorte qu'aucun texte ne repose sur le motif (dérogation encadrée à l'interdit « derrière un texte courant »).

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
8. Double seuil de couverture bloquant : 80 % des branches du domaine et 80 % des lignes des couches domaine et application (D-41).
9. Règles d'architecture vérifiées dans la chaîne (SOC-18) : dépendances entre couches, aucun montant en virgule flottante, suffixes de nommage.
10. Événements déclarés au registre des schémas sous le nom retenu par D-37.

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

**CP v2.0 (à reporter en v2.1)**
- Remplacer « au moins semestrielle » par « au moins trimestrielle » (IAM-10, IAM-18, annexe B), selon D-34.
- Reformuler la signature des quitus (CER-02 à CER-05, §3.6) selon D-35 : assertion WebAuthn personnelle puis cachet de la plateforme.
- Ajouter le scénario `ELEVATION_JIT` à IAM-06 (D-36).
- Appliquer la table de correspondance des sujets d'événements (D-37).
- Qualifier le scénario 2 de HOM-13 d'accélérateur facultatif (D-38).
- Ajouter les correspondances RG explicites du §5 aux fiches des récits (D-33).
- Remplacer « 80 % des lignes » par le double seuil de D-41 (QUA-01).

**Tous les documents**
- Désignation des auteurs : « Ingénieur en Informatique et Systèmes d'information ».

---

*Prochaine étape : plan d'implémentation de l'itération 1 (socle : MS-GATEWAY, MS-IAM, MS-AUDIT, TRV-01 à TRV-07 et TRV-09), établi sur la base du présent référentiel.*

*Révision v1.1 : prochaine étape, sprint 0 du CP v2.0 (SOC-01, SOC-02, SOC-07, SOC-13, SOC-15, SOC-18, QUA-01, QUA-02, QUA-08, QUA-09, QUA-10), puis infrastructure (D-45) et frontend (D-46).*
