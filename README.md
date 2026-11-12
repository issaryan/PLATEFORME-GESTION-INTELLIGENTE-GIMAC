# Moabi, plateforme de suivi des activités du GIMAC

Plateforme du Département Exploitation et Supervision (DES) du GIMAC, groupement interbancaire monétique de la CEMAC. Elle **lit, référence et trace** les activités du département : homologation et certification des liaisons, exploitation et incidents, rapprochement, pilotage, reporting et collaboration. Elle n'écrit jamais dans le commutateur, le simulateur de certification ni la gestion des changements, ne traite aucune donnée de carte et ne comporte aucune composante d'intelligence artificielle (D-27).

## Documents de référence

| Document | Rôle |
|---|---|
| `CCT-GIMAC-01_v1.1` | Cahier des charges techniques : besoins BF et BNF, seuils contractuels |
| `CA-GIMAC-01_v1.1` | Cahier d'analyse : acteurs, cas d'utilisation, règles de gestion RG |
| `CC-GIMAC-01_v1.1` | Cahier de conception : 14 microservices, événements, algorithmes, thème |
| `CP-GIMAC-01_v2.0` | Carnet de produit : 313 récits, 17 épopées, feuille de route S0 à S12 |
| `REC-GIMAC-01_Reconciliation.md` | **Référentiel d'implémentation** : décisions D-01 à D-64, prime sur les quatre cahiers pour les points qu'il tranche |

## Organisation du dépôt

| Dossier | Contenu |
|---|---|
| `ms-gateway` … `ms-collaboration` | Les 14 microservices JHipster (Java 21), un JDL par service |
| `frontend/` | Les trois couches de présentation React : application du département, portail des participants, console d'administration et d'audit |
| `infra/docker-compose/` | Environnement local : Docker Compose généré par JHipster et complément du projet |
| `infra/kubernetes/` | Déclarations Kubernetes générées par JHipster |
| `infra/kubernetes-gimac/` | Complément Kustomize du projet : politiques réseau, sécurité, frontend |
| `infra/keycloak/` | Domaine Keycloak `gimac` du lot 1 (permissions, profils, utilisateurs de test) |
| `infra/kafka/` | Création des sujets Kafka du lot 1 et de leurs files de rebut |
| `docs/lot1/` | Plan d'implémentation du lot 1, système de conception, tableau de correspondance, contrats de service du parcours (`parcours-backend.md`) et de la messagerie (`messagerie.md`) |
| `infra/ci/` et `.github/workflows/ci.yml` | Chaîne d'intégration et de déploiement continus : validation de l'infrastructure, du backend, du frontend et de la sécurité, images signées, déploiement en recette |

Chaque dossier a son propre README, avec un journal des mises à jour.

## Pile technique

Java 21 ; JHipster 9.4.0 et Spring Boot 4 pour les services régénérés (passerelle, IAM, audit, participant, homologation, certification, collaboration), JHipster 8.11 et Spring Boot 3 pour les sept autres jusqu'à leur régénération (D-43) ; Kafka et CloudEvents ; PostgreSQL, un schéma par service ; Keycloak (OIDC) ; Valkey ; Ceph RGW pour le stockage WORM (D-50) ; OpenBao ; Open Policy Agent ; React 19, TypeScript et Vite (D-51) ; Prometheus, Grafana, OpenTelemetry, Loki, Tempo ; Kubernetes et Kyverno.

## Démarrer en local

```
infra/docker-compose/gimac-dev.sh verifier          # prérequis et ports
infra/docker-compose/gimac-dev.sh demarrer --socle  # infrastructure et frontend, sans les 14 services
infra/docker-compose/gimac-dev.sh images            # images Jib des 14 services
infra/docker-compose/gimac-dev.sh demarrer          # tout
infra/docker-compose/mettre-a-jour.sh              # après un pull : ne reconstruit que ce qui a changé
```

Les secrets locaux sont générés dans `infra/docker-compose/.env`, jamais versionné.

Comptes nominatifs (un par tableau de bord) : décrire les comptes dans `infra/keycloak/local/comptes.json` (non versionné), puis `python3 infra/keycloak/creer-comptes-personnels.py` ; les mots de passe provisoires sont écrits dans `infra/keycloak/local/mots-de-passe-provisoires.txt`. Première connexion : nouveau mot de passe, enrôlement TOTP, puis premier accès de la plateforme. Keycloak de développement perd ses comptes quand son conteneur est recréé : relancer le script.

## Intégration continue

Toute la logique vit dans `infra/ci` (scripts exécutables à l'identique sur un poste et en CI) ;
`.github/workflows/ci.yml` installe les outils et enchaîne les étapes. Détail : `infra/ci/README.md`.

```
infra/ci/installer-outils.sh                       # shellcheck, opa, kustomize, kubeconform, promtool, amtool, hadolint, actionlint, gitleaks, yamllint
GIMAC_CI_STRICT=1 infra/ci/valider-infra.sh        # Compose, OPA (tests, couverture), Kubernetes, supervision, Dockerfiles, domaine Keycloak, YAML
infra/ci/valider-backend.sh [ms-…]                 # tests unitaires, d'intégration (Testcontainers) et de recette, seuils JaCoCo
infra/ci/valider-frontend.sh [--e2e]               # types, Vitest (≥ 80 %), construction, Playwright (≥ 80 %)
infra/ci/valider-securite.sh                       # secrets (gitleaks) et vulnérabilités (Trivy)
```

## Règles de travail

- Aucun secret, aucune clé et aucune donnée de carte dans le dépôt (BNF-07, D-44).
- Docker Compose, images des services et Kubernetes sont produits par les générateurs JHipster ; les compléments du projet sont dans des fichiers séparés (D-48).
- Le document de réconciliation évolue uniquement par ajout de décisions.
- Chaque modification met à jour le README du dossier concerné et le journal ci-dessous.

## État au 28/09/2026

- 14 microservices générés et compilés : le socle (gateway, iam, audit) en JHipster 9.4.0, les onze autres en 8.11.0, à régénérer (D-43).
- Environnement local vérifié : infrastructure, trois couches de présentation et Ceph RGW avec verrouillage d'objets en mode conformité.
- Déclarations Kubernetes validées, non déployées faute de grappe.
- Trois couches React initialisées et conteneurisées.
- 64 classes reprises du projet afriland (D-52) : jeton fantôme et mTLS de la passerelle, validation du jeton interne, file de rebut Kafka, masquage des journaux dans tous les services, canaux de notification, fusion PDF, JSON canonique. Inactives par défaut ; chaque récit les active. Écarts comblés (D-53) : jeton interne lié au certificat mTLS, chemins exemptés ancrés, masquage Luhn 6+4 ; administration Keycloak et jeton de service repris ensuite.
- **Lot 1 engagé (module de gestion de la certification)** : Phase 0 réalisée (D-55), voir `docs/lot1`.
- **Certification de bout en bout (D-56 à D-61)** : le compte rendu de cadrage CR-CERT-GIMAC-01 v1.4 est intégré ; réalisation par tranches verticales. Tranche DDRM livrée côté serveur : MS-PARTICIPANT régénéré en 9.4.0, étapes 1 et 2 du parcours (prise de contact, dossier d'adhésion, prononcé en maker-checker). Définition de fini : quatre types de tests (unitaires, intégration, sécurité, recette) et 80 % de couverture, bloquants ; accès par la matrice des rôles d'un jeton à deux facteurs.
- Prochaine étape : itération A du lot 1, puis sprint 0 du carnet (gabarit hexagonal SOC-01, chaîne de livraison SOC-02, contrôles QUA-01, QUA-02, QUA-09, QUA-10).

## État au 30/09/2026

- **Parcours de certification (étapes 3 à 10)** : interface complète par acteur (tableaux de bord « Mon travail » et pilotage consolidé, dossier numérique, verrous du procès-verbal et de la sécurité, mise en service, portail du participant), servie par un serveur de substitution qui passe ses tests ; backend des étapes 4 à 10 livré dans MS-HOMOLOGATION (dossier numérique, 96 opérations du référentiel de l'annexe A instanciées à l'ouverture, preuves, validation FIDO2, blocages, constats, fiche de suivi, tableaux de bord, portail) et MS-CERTIFICATION (verrous du procès-verbal et de l'approbation de la sécurité), reliés par événements selon `docs/lot1/parcours-backend.md`.
- **Messagerie instantanée de travail (D-64)** : interface livrée (`/des/messagerie` : annuaire, liaisons citées en liens, pièces jointes, non-lus) ; canal poussé servi par la passerelle (`/bff/{couche}/live`, alimenté par Kafka) ; MS-COLLABORATION régénéré en 9.4.0 et module de messagerie en cours.
- **Chaîne CI/CD** : infrastructure 35 contrôles sur 35, aucun secret en clair (gitleaks) ; sept services restés en JHipster 8.11 passés à Testcontainers 2 (Docker Engine 29 refusait l'API des versions 1.x).
- Interface : navigation latérale fixe aux couleurs du GIMAC, pagination visible sur toute liste, listes de choix interactives.

- **Socle vérifié le 29/09/2026 (D-54)** : 1 356 tests unitaires et 5 476 tests d'intégration réussis sur les 14 services ; plateforme complète démarrée ; `infra/docker-compose/tester-socle.sh` : 48 contrôles sur 48 réussis (santé, jeton Keycloak, API via la passerelle, OPA, WORM, simulacres, frontend, Prometheus).

## État au 27/10/2026 — intégration de la Sandbox GIMAC

Le contrat complet reste dans `docs/lot1/sandbox-existant-adapte.md` ; les README ne donnent que l'état intégré et ses preuves.

- **Introspection et cloisonnement** : MS-IAM décide à chaque requête, sans cache, de l'activité d'un jeton Sandbox (périmètre, mandat ou prestataire). La passerelle l'interroge avant de cloisonner l'institution effective et de réduire le jeton interne ; suspension et révocation ferment aussi les sessions. Le rétablissement restaure l'état propre de chaque compte.
- **Sagas et US-EXI-23** : les étapes OUVERTURE et MATRICE sont rejouables par clé d'idempotence ; MATRICE ne modifie que les profils SBX. Le réexamen trimestriel fige tous les accès, permissions SBX comprises, puis maintient, réduit ou révoque avant scellement. Le déblocage reste réservé au GIMAC.
- **US-EXI-25 / D-125** : MS-HOMOLOGATION propage l'empreinte contrôlée du bilan dans le socle d'évaluation. MS-CERTIFICATION ouvre un cycle seulement au changement, rend caducs les quitus favorables du circuit remplacé et refuse toute signature sur un ancien socle (`CER_SOCLE_OUTDATED`). Les événements historiques sans empreinte restent admis, sans effet sur le circuit.
- **Supervision D-121** : aucune adaptation de MS-MONITORING, MS-INCIDENT ou MS-REPORTING. L'écran `/des/sandbox/supervision` lit uniquement des ressources GET de SBX-TRACE sous `SBX_SUP_CONSULTATION` : les quatre de D-121, puis, par le complément du 05/11/2026, les mesures par service (`services`) et les statistiques des codes retour.
- **OPA** : les politiques Compose et Kubernetes sont identiques. Les règles couvrent le cloisonnement SBX, les quatre lectures D-121 et les écritures de réexamen (`IAM_ACCOUNT_CREATE`, `IAM_ACCOUNT_ROLES`), sans ouvrir les chemins voisins.
- **Recette DEMO-SBX-12** : douze contrôles visuels et 64 captures en clair et sombre sont consignés dans `docs/recette/sandbox.md`. Les compléments du 27/10 (sagas, réexamen, supervision et D-125) ont des preuves automatisées, mais pas de nouvelles captures.

Validations réellement exécutées et conservées dans les rapports du 27/10 :

| Périmètre | Résultat |
|---|---|
| MS-IAM | 458 tests unitaires et de sécurité (2 ignorés), 450 d'intégration et de recette ; seuils JaCoCo globaux et EXI 98 % |
| MS-GATEWAY | 592 tests unitaires ; seuils JaCoCo ; 213 tests OPA sur 213 |
| MS-HOMOLOGATION | `TESTCONTAINERS_RYUK_DISABLED=true ./mvnw clean verify` : 536 tests unitaires et 421 d'intégration ; seuils globaux et EXI 98 % |
| MS-CERTIFICATION | même commande : 219 tests unitaires et 236 d'intégration ; seuils globaux et EXI 98 % |
| `@gimac/administration` | 189 tests ; couverture globale 95,25 % instructions, 85,70 % branches, 94,38 % fonctions, 96,44 % lignes ; seuils EXI 98 % sur les fichiers concernés |
| `@gimac/sandbox-admin` | 46 tests ; 100 % instructions, fonctions et lignes, 99,55 % branches |

**Limites de preuve** : les assemblages spécifiques de recette restent à résorber dans l'assemblage courant ; l'autorité de certification mTLS locale n'est pas livrée ; le Compose complet n'a pas été démarré pour cette intégration ; aucune exécution ne démontre un raccordement à de vrais serveurs GIMAC. Les validations ci-dessus ne doivent donc pas être présentées comme une recette de production.

## État au 30/10/2026 — Demande de changement

- **Processus d'unité `DEMANDE_CHANGEMENT`** dans MS-PILOTAGE : unité SSI, ouvert à tout le GIMAC, référence automatique `CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}`, douze champs de lancement et cinq étapes (identification, raison, impact, implémentation, décision SSI). Le dossier PDF est obligatoire et la décision SSI est signée sur son empreinte.
- **Export PDF d'une instance** : `GET /api/v1/processus/instances/{id}/pdf`, avec les données du demandeur, les champs d'étapes et les signatures ; rendu par MS-DOCUMENT en production, adaptateur simulé hors production. Le bouton de téléchargement est disponible dans le suivi frontend.
- **Preuves locales** : tests ciblés MS-PILOTAGE et scénario d'intégration réussis avec seuils JaCoCo à 98 % ; 214 tests OPA sur 214 ; scénario Playwright complet et huit captures en clair et sombre. DEMO-SBX 13 a également vérifié le PDF Gotenberg réel de deux pages, la référence et trois transitions sur les services locaux. FIDO2 réel et les étapes 4 et 5 restent non prouvés ; aucun serveur GIMAC n'a été sollicité.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 4bade16 et précédents | Génération JHipster 8.11.0 de onze microservices |
| 2026-09-28 | ed4c1d5, c1b7131, 401e417 | Génération JHipster 9.4.0 du socle : gateway, iam, audit |
| 2026-09-28 | 5d9144c | Carnet de produit CP-GIMAC-01 v2.0 |
| 2026-09-28 | cc5cb20 | REC-GIMAC-01 v1.1 : décisions D-32 à D-46 |
| 2026-09-28 | 9f2250a, 932d192 | Docker Compose généré par JHipster, complément du projet, script `gimac-dev.sh` |
| 2026-09-28 | c0b91ee | Kubernetes généré par JHipster et complément Kustomize (D-49) |
| 2026-09-28 | 7dcca58 | Ceph RGW remplace MinIO (D-50) |
| 2026-09-28 | b20a049 | Frontend React : trois couches générées par create-vite et conteneurisées (D-51) |
| 2026-09-28 | — | README du projet et README de chaque dossier |
| 2026-09-28 | — | Reprise de code du projet afriland dans les 14 services (D-52) |
| 2026-09-28 | — | Écarts du code repris comblés et second inventaire d'afriland (D-53) |
| 2026-09-29 | — | Socle vérifié de bout en bout : tests des 14 services, plateforme complète, `tester-socle.sh` (D-54) |
| 2026-09-29 | — | Lot 1, Phase 0 : régénération de MS-HOMOLOGATION et MS-CERTIFICATION depuis le plan, domaine Keycloak gimac, OPA et routes /bff, messagerie, jeton de service (D-55) |
| 2026-09-29 | — | Arbitrages de la certification d'après le CR de cadrage (D-56 à D-60) ; MS-PARTICIPANT régénéré en 9.4.0 et tranche DDRM (étapes 1 et 2) ; définition de fini des tests et double facteur (D-61) |
| 2026-09-29 | — | Sécurité (profils par fonction, double facteur, OPA, `/api/session`) ; frontend de la tranche DDRM : tableau de bord par acteur, registre et dossier numérique des institutions, pied de page GIMAC, seuils de couverture |
| 2026-09-29 | — | Supervision de la plateforme en temps réel (passerelle), dépôt WORM et empreinte (MS-DOCUMENT), intégration verte de MS-PARTICIPANT |
| 2026-09-29 | — | Administration complète depuis la plateforme, sans console Keycloak (D-62) : MS-IAM (comptes, premier accès, « Mon compte », politique de sécurité, messagerie, profils, journal, réauthentification FIDO2), routes et règles OPA, thème GIMAC des pages Keycloak, domaine `gimac` en Compose et en Kubernetes |
| 2026-09-29 | — | Dépôt distant réparé : ports sortants `application/port/out` jamais versionnés (règle `out/` de JHipster), sept ports reconstitués, adaptateurs de MS-NOTIFICATION versionnés, modernizer de la passerelle, construction des images sous le seuil D-61 ; comptes nominatifs locaux (`infra/keycloak/creer-comptes-personnels.py`) |
| 2026-09-30 | 292c28c, 1a1ce09, 3ccf11f, 1025925 | Interface recalée après l'arrêt des agents (cloisonnement du portail, état simulé isolé) ; enveloppe fixe, navigation latérale à l'identité GIMAC, tableaux de bord épurés, pagination visible |
| 2026-09-30 | c913e71, ce6c88b | Chaîne CI/CD `infra/ci` ; plus aucun secret de client Keycloak en clair |
| 2026-09-30 | 4fa6dc8, f9cfef1 | Canal poussé de la passerelle et routes de la messagerie ; Testcontainers 2 dans sept services |
| 2026-09-30 | d07c74d, 0f46741 | Messagerie instantanée (D-64) : contrat et interface ; partage du backend des étapes 4 à 10 |
| 2026-10-01 | — | Serveur de démonstration 10.20.36.214 mis à jour à chaque poussée sur `main` (`.github/workflows/deploiement-serveur.yml`, exécuteur auto-hébergé `gimac-214`, `infra/ci/deployer-serveur.sh`) |
| 2026-10-02 | — | Keycloak persistant sur un poste de développement (surcouche activée par le `.env`), comme sur le serveur de démonstration |
| 2026-10-05 | — | `infra/docker-compose/mettre-a-jour.sh` : mise à jour et redéploiement d'un poste en une commande, sans perte des comptes |
| 2026-10-05 | — | Publication HTTPS (D-75) : port retenu 8463 (`GIMAC_PORT_HTTPS=8463`), noms `moabi`, `moabi-portail`, `moabi-console`.gimac-afr.org ; procédure complétée (chemins du certificat ANTIC, contrôle du relais avant DNS) |
| 2026-10-05 | — | Relais HTTPS de Moabi installé sur le nginx du serveur 214 (port 8463, trois noms) et vérifié ; en attente du DNS OVH et du pare-feu |
| 2026-10-06 | — | Comptes : institution réservée au correspondant de participant (précision de D-62) ; `mettre-a-jour.sh` prépare les nouveaux secrets avant de construire |
| 2026-10-06 | — | Supervision et tableaux de bord en graphiques, lot 1 : composants de graphiques de métriques du système de conception (D-63), palette de séries validée en clair et en sombre |
| 2026-10-06 | — | Supervision en graphiques, lot 2 : séries temporelles des métriques servies par la passerelle (liste fermée, sécurisée), rétention Prometheus explicite |
| 2026-10-06 | — | Supervision en graphiques, lot 3 : centre de contrôle dans la console du super administrateur et dans l'application interne |
| 2026-10-06 | — | Supervision en graphiques, lot 4a : courbes de processeur et de mémoire par charge de travail dans l'écran Infrastructure |
| 2026-10-06 | — | Exploitation Kubernetes depuis la console, lecture (lot 4b-1, partie serveur) : pods, événements, nœuds, tâches |
| 2026-10-06 | — | Exploitation Kubernetes depuis la console, lecture (lot 4b-1, écrans) : pods et événements par charge, nœuds du cluster, jobs et cronjobs |
| 2026-10-06 | — | Exploitation Kubernetes depuis la console : retour à la version précédente d'un déploiement (geste RETABLIR signé, lot 4b-2), image cible affichée dans la confirmation |
| 2026-10-06 | — | Tableaux de bord métier en graphiques (lot 5) : comptes par profil et activité quotidienne (MS-IAM), répartition des institutions et flux mensuel de la DDRM (MS-PARTICIPANT) |
| 2026-10-08 | — | Publication HTTPS : décision de la DSI du 07/10 (noms internes résolus par fichier hosts, certificat ANTIC importé sur le serveur), état constaté, marche à suivre pour les postes et architecture du déploiement (`docs/exploitation/publication-https.md` §2 bis) |
| 2026-10-09 | — | Serveur de démonstration sans code source : paquet de déploiement (`infra/ci/paquet-serveur.sh`) et images publiées par la CI, dont les trois interfaces aux adresses de `publication.env` ; workflow `deploiement-serveur-images.yml`, bascule par la variable `GIMAC_DEPLOIEMENT_SERVEUR` ; correctif du `.env` du serveur (secrets régénérés à chaque déploiement) (`docs/exploitation/deploiement-serveur-images.md`) |
| 2026-10-09 | - | Livraison du serveur de démonstration depuis un poste, sans code source ni GitHub Actions : `infra/ci/livrer-serveur.sh` (images construites sur le poste, envoyées par SSH avec le paquet de déploiement) ; ancien workflow du serveur en lancement manuel seulement (`docs/exploitation/livraison-serveur.md`) |
| 2026-10-12 | - | Serveur de démonstration : les 14 bases des microservices passent dans des volumes nommés (`docker-compose.serveur.yml`). Après la panne du 10-12/10, les conteneurs supprimés laissaient leurs bases dans des volumes anonymes orphelins et le redéploiement serait reparti sur des bases vides ; données du 10/10 reprises dans les nouveaux volumes |
| 2026-10-12 | - | Espace disque du serveur 214 (saturation du disque système à l'origine de l'arrêt du 10/10) : `deployer-serveur.sh` refuse de déployer sous 10 Go libres et retire en fin de déploiement les images et le cache de construction de plus de 3 jours ; Prometheus plafonné à 3 Go |
| 2026-10-13 | - | Serveur de démonstration : les comptes de test du générateur (`test-…`) sont écartés de la fusion du domaine Keycloak (`GIMAC_SANS_COMPTES_TEST=1` dans `deployer-serveur.sh`) ; supprimés du serveur, ils n'y reviennent plus |
| 2026-10-13 | - | `infra/keycloak/creer-comptes-personnels.py` : plusieurs profils par compte, téléphone, alignement des comptes existants (`aligner`) et suppression explicite (`--supprimer`), pour reprendre l'annuaire du personnel |
| 2026-10-13 | D-104 | plusieurs liaisons actives par institution, y compris du même type (intitulé libre, référence d'origine unique) ; groupe ou maison mère des institutions (filiales) |
| 2026-10-13 | - | D-106 : l'étape de rang 2 s'affiche « Procédure administrative » (code `ADHESION` inchangé) ; délai cible et délai maximal des processus internes, de leurs étapes et des opérations de certification (échéance dépassée au maximum, échéance proche à la cible) ; audit de qui voit et qui lance (public seul réduit à la version publiée, `REF_METIER_ADMIN` reconnu par l'interface, ouverture de liaison avec `HOM_PROJECT_DESIGNATE`), `docs/lot1/lancement-processus.md` §12 |
| 2026-10-13 | D-105 | CR-CERT-GIMAC-01 v1.7 fait foi : annexe A reprise en v1.7 (branche mobile à la Production monétique, jalon d'approbation sur le rang 24, intitulés complets), étapes « Cadrage et périmètre » et « Réunion de lancement », qualité C « Contributeur », ouverture des liaisons réservée à la DDRM, missions des profils, profil `ROLE_AGENT_SSI` |
| 2026-10-13 | D-107 | D-107 : nouveau projet d'une institution active sans changement de statut ; reprise de l'existant : permission `REF_REPRISE_IMPORT`, client `reprise-service`, routes de reprise des institutions (MS-PARTICIPANT) et des liaisons (MS-HOMOLOGATION) en simulation par défaut, outil `infra/reprise/importer-reprise.py` (`docs/exploitation/reprise-des-donnees.md` §3, §4, §9) |
| 2026-10-13 | - | Canal temps réel : invalidations regroupées (au plus une par clé toutes les 2 s), une rafale d'événements comme une reprise de données ne sature plus le navigateur (ERR_INSUFFICIENT_RESOURCES) ; fiche projet tolérante aux routes secondaires absentes (historique, dernier export de preuves, état de reprise) |
| 2026-10-13 | - | Fiche d'une liaison : l'adresse `/des/liaisons/:id` renvoie au dossier du parcours (`/des/parcours/:id`, routes réelles du service) ; l'ancienne fiche à onglets appelait des routes jamais construites (graphe, couvertures, campagnes, dossier de preuves), d'où « Le chargement a échoué » |
| 2026-10-13 | - | Supervision : la disponibilité des services est présentée dans une carte du gabarit des indicateurs, alignée sur leur rangée ; l'anneau (`ProgressRing`) n'est plus borné à la largeur de son cercle, son libellé et son badge ne débordent plus |
| 2026-10-19 | D-113 | Agenda en grille de calendrier (jour, semaine, mois, création depuis une case, catégories filtrables), Kanban personnel des tâches glissable et paginé par colonne |
| 2026-10-19 | D-112 | Dossier de la liaison : onglets « Graphe » (opérations par étape, état et dépendances) et « Planning » (Gantt des étapes) ; glisser une opération dans un autre couloir du modélisateur la rattache à cette étape ; étapes des brouillons D-59 reprises ; puces, nœuds du graphe et barres du Gantt sans débordement |
| 2026-10-19 | D-111 | Journaux supprimés après un jour sur le serveur (Loki et Tempo à 24 heures, purge quotidienne par l'exécuteur), journaux de conteneur bornés et redémarrage automatique des services par une surcouche d'exploitation générée |
| 2026-10-15 | D-110 | Avancement réel des projets pondéré par étape (MS-HOMOLOGATION, liste et dossier) ; essais repris de la sandbox ; requalification des 197 projets hors production (sandbox, PayWay test, rapports d'avancement) |
| 2026-10-15 | D-109 | Preuves, livrables, signatures, RACI et délais de chaque opération du CR v1.7 (carte, mobile, délégataire), exigés à la clôture, opérations reprises dispensées ; onglets « Livrables et preuves », « Signatures », « Chronologie » |
| 2026-10-19 | D-112 | « Mes actions de certification » (Liste des liaisons) : onglet « Liaisons suivies » en tableau paginé (25 par page ; liaison, statut, étape en cours, avancement, jalons), comme les deux autres onglets ; catalogues FR, EN, ES |
| 2026-10-19 | D-114 | Supervision des liaisons, grille d'astreinte (programme linéaire en nombres entiers, OR-Tools CP-SAT) et relève de quart signée puis scellée sur MS-MONITORING ; route `des-monitoring` de la passerelle et règles OPA ; écrans `@gimac/exploitation` (Supervision, Astreintes, Relève) ; FR, EN, ES ; tuiles murales, onglets et barres de défilement corrigés |
| 2026-10-19 | D-115 | Sandbox GIMAC, épopée EXI : principes du raccordement à la plateforme, soixante permissions `SBX_*` et six profils d'institution `ROLE_SBX_*`, validation des verdicts réservée au GIMAC dans MS-IAM, module « Sandbox GIMAC » du catalogue des droits (docs/lot1/sandbox-existant-adapte.md) |
| 2026-10-20 | D-115 | Sandbox GIMAC, US-EXI-03 : administration déléguée des comptes d'une institution dans MS-IAM (`/api/v1/administration-deleguee`, portail `/bff/portail/sbx-habilitations`), étapes de saga rejouables, dernier administrateur protégé |
| 2026-10-20 | D-116 | Recentrage du module d'astreintes et de monitoring : rapports rattachés au programme et envoyés au supérieur, rapport quotidien de 8 h (rappel, retard), contraintes de génération paramétrables, activité de l'équipe (serveur MS-MONITORING, finalité RAPPORT_MONITORING de MS-DOCUMENT, OPA) |
| 2026-10-20 | - | D-116, écrans : « Aujourd’hui » (rapport de 8 h, équipe de service, mes créneaux), « Rapports de monitoring » (dépôt fragmenté rattaché au programme, envoi au supérieur, consultation PDF ou docx en modale, rapport de 8 h jour par jour), « Activité de l’équipe », contraintes du programme ; supervision et relève hors navigation |
| 2026-10-20 | - | D-117, serveur : programme d’astreinte et de monitoring entièrement configurable (modèle de créneaux versionné, solveur et vérificateur sur intervalles, rapport de 8 h porté par un créneau), téléchargement PDF et Excel, publication notifiée à chaque personne programmée |
| 2026-10-20 | - | D-117, écrans : programme en calendrier (jour, semaine, mois) ou en tableau comme la feuille du DES, « Mon programme » et « Mon jour », éditeur des créneaux, téléchargement PDF et Excel, contraintes du rapport de 8 h |
| 2026-10-20 | - | D-118 : administration de la Sandbox GIMAC depuis la plateforme, résultats lus par MS-HOMOLOGATION (REC) |
| 2026-10-20 | - | Programme d’astreinte : calendrier refait en planning lisible (semaine, jour, mois) |
| 2026-10-22 | - | Exploitation : tests des rapports indépendants de la date du jour |
| 2026-10-22 | - | D-121 : la supervision de la sandbox ne passe pas par MS-MONITORING ; écran dans l'espace sandbox du DES (REC) |
| 2026-10-22 | - | Socle : thème synchronisé entre les onglets ouverts |
| 2026-10-22 | - | D-122, US-EXI-16 : MS-HOMOLOGATION publie l'ouverture, le gel et la clôture des liaisons pour la Sandbox GIMAC (`homologation.liaison-status-changed.v1`, version d'état croissante, code participant) |
| 2026-10-22 | - | D-122 : espace sandbox du DES (`@gimac/sandbox-admin`, « Espaces des institutions ») ; gel des liaisons sur changement de statut de l'institution (HOM-06) et republication des états pour la sandbox |
| 2026-10-20 | - | D-118 : habillage facultatif d'un produit hébergé sur le socle, pour la Sandbox GIMAC. Connexion : fond, marque, logo, nom de l'onglet, mention du pied. Enveloppe : logo, bandeau, pied. Le pied (`Footer`) accepte une désignation et une classification facultatives. Aiguillage : règle facultative sur `PageConnexion` et `Aiguillage`. Sans ces propriétés, le comportement de la plateforme est inchangé. |
| 2026-10-22 | D-120 | Sandbox GIMAC, US-EXI-07 : routes console et portail des onze services de la sandbox et `/bff/des/sbx-espace` dans MS-GATEWAY, règles OPA SBX, cloisonnement étendu aux mandats actifs (projection de `sbx-espace.scope-changed.v1`), jeton interne réduit, quotas, décisions journalisées vers MS-AUDIT |
| 2026-10-22 | D-120 | Sandbox GIMAC, US-EXI-20 : version de statut strictement croissante par institution dans `participant.status-changed.v1` (`versionStatutRegistre`, table `institution_version_statut`), schéma JSON du contrat |
| 2026-10-22 | - | Application du DES : seuil de couverture des fonctions de nouveau tenu (98,6 %) |
| 2026-10-22 | D-120 | Sandbox GIMAC, US-EXI-21 : fiche d'une institution (statut, version, correspondants actifs) et nomenclature des services par l'interface interne de MS-PARTICIPANT, refus journalisé des écritures des comptes de service sbx-* |
| 2026-10-22 | D-119 | Sandbox GIMAC, US-EXI-06 : suspension du périmètre d'une institution dans MS-IAM (indicateur superposé, sessions révoquées, connexion et introspection refusées, rétablissement à l'état antérieur), protection du dernier administrateur, désactivation des comptes inactifs de la sandbox, étapes de saga idempotentes ; client `sbx-espace` ; écran « Sandbox GIMAC » de la console (docs/lot1/sandbox-existant-adapte.md §8) |
| 2026-10-22 | D-120 | Sandbox GIMAC, US-EXI-08 : types de pièces et rétention de la sandbox dans MS-DOCUMENT, dépôt par SBX-CAMPAGNE, SBX-TRACE et SBX-ISO20022 (empreinte, déduplication, quarantaine), `document.document-sealed.v1` par outbox, pièce par empreinte |
| 2026-10-22 | D-119 | Sandbox GIMAC, US-EXI-04 : mandats de partenaire en délégations temporaires de MS-IAM (octroi borné, jeton délégué refusé dès la date de fin, révocation immédiate, prolongation) ; module SUP de la matrice SBX (`SBX_SUP_CONSULTATION`) ; onglet « Mandats de partenaire » de la console |
| 2026-10-23 | D-122 | Sandbox GIMAC, US-EXI-14, US-EXI-15, US-EXI-17 : MS-HOMOLOGATION relit le contrat de SBX-LECTURE (relève périodique et sur `sbx-lecture.campaign-published.v1`), pièces conservées par empreinte ; onglet « Résultats de la sandbox » du dossier de liaison du DES |
| 2026-10-23 | D-123 | Sandbox GIMAC, US-EXI-05 et US-EXI-24 : comptes de service `sbx-*` et matrice des appels synchrones dans le domaine Keycloak ; accès de prestataire de maintenance approuvé par le RSSI (MS-IAM, console) |
| 2026-10-23 | D-124 | Sandbox GIMAC, US-EXI-23, US-EXI-10, US-EXI-09 : verrouillage et déblocage des comptes depuis la console ; registre d'audit de la sandbox (MS-AUDIT) ; notifications de la sandbox (MS-NOTIFICATION, type `SANDBOX`) ; US-EXI-25 en attente d'une décision |
| 2026-10-27 | - | EXI : introspection MS-IAM et cloisonnement immédiat, sagas OUVERTURE/MATRICE, réexamen US-EXI-23, supervision D-121 et règles OPA ; DEMO-SBX-12 : 12 contrôles et 64 captures ; validations et limites de preuve actualisées |
| 2026-10-27 | D-125 | US-EXI-25 réalisé : socle d'évaluation par empreinte, cycle et caducité des quitus, refus des signatures sur ancien socle ; `clean verify` réussi sur MS-HOMOLOGATION et MS-CERTIFICATION |
| 2026-10-28 | - | Processus d'unité `DEMANDE_CHANGEMENT`, références automatiques et export PDF dans MS-PILOTAGE ; cycle simulé, téléchargement et captures clair/sombre dans le frontend ; habilitation de rendu et règle OPA documentées |
| 2026-10-29 | US-INS-07 | Matrice Sandbox globale et idempotente dans MS-IAM (`PUT /api/v1/interne/sandbox/matrice`) : recomposition auditée par profil, permissions hors SBX conservées, validation externe refusée et aucun second effet au rejeu |
| 2026-10-30 | - | DEMO-SBX 13 : PDF Gotenberg réel de deux pages, référence et transitions du processus de changement ; persistance PostgreSQL de la tâche avant son historique et sérialiseurs binaires du binder Kafka de MS-PILOTAGE |
| 2026-11-05 | D-121 | « Activité des tests » dans l'espace sandbox du DES : graphes des tests des participants (SBX-LECTURE, route des-sbx-lecture et règle OPA), chaîne sandbox, homologation et certification ; vue graphique de l'onglet sandbox du dossier de liaison (jauges, verdicts, réussite par service) |
| 2026-11-05 | D-133 | Périmètre du projet de certification fixé au GIMAC (gestionnaire, super administrateur, chargé de l'implémentation SNA) : MS-HOMOLOGATION, profil `ROLE_CHARGE_IMPLEMENTATION_SNA` et permissions `HOM_PERIMETRE_EDIT`, `HOM_PERIMETRE_EDIT_TOUS` au domaine Keycloak, OPA (deux copies), catalogue des fonctionnalités ; services des campagnes de la sandbox dérivés du périmètre. |
| 2026-11-04 | - | Sandbox, périmètre de lecture : `SBX_LEC_VALIDATION` portée par le seul `ROLE_RSSI` (CC-SBX-GIMAC-02 §D.8, UC-LEC-10), assertion du générateur et test ; domaines et catalogue régénérés, contrat §2.2 aligné ; la sandbox reçoit la même valeur (`MatriceRoles`) |
| 2026-11-05 | D-121, complément | Supervision de la sandbox : SBX-TRACE collecte et historise les mesures des onze services ; lectures DES `services` et `statistiques/codes-retour` ouvertes par la passerelle et OPA ; section « Services de la sandbox » (état, disponibilité 24 h, latences et tendance, débit, erreurs, files Kafka, rebuts) dans `@gimac/sandbox-admin` ; complément daté au REC |
| 2026-11-04 | D-123 | Liaison certificat et jeton vers SBX-LECTURE : jeton de `homologation-service` lié au certificat en production, demande de jeton et lecture en mTLS par paquet SSL dans MS-HOMOLOGATION, complément de D-123 au REC |
| 2026-11-05 | D-132 | Reprise des correspondants des institutions (MS-PARTICIPANT, `/api/v1/interne/reprise/correspondants` et route de console), passerelle, OPA, `infra/reprise/importer-reprise.py correspondants`, contrat `docs/exploitation/reprise-des-donnees.md` §3 bis ; préalable à la migration de l'ancienne sandbox. |
| 2026-11-05 | D-137 | Clôture sans suite des liaisons reprises sans projet réel (MS-HOMOLOGATION, `…/reprise/liaisons/clotures`, motif obligatoire, trace par événement, aucune suppression), passerelle, OPA, `importer-reprise.py clotures` ; `infra/reprise/corriger-reprise-1310.py` (propositions de correctifs de la reprise du 13/10). |
| 2026-11-05 | D-135 | Supervision de la sandbox depuis le DES (`SBX_SUP_CONSULTATION`) attribuée au chef du DES, à l'administrateur système et au super administrateur ; générateur du realm et domaine régénérés |
| 2026-11-05 | - | Barre latérale de l'application du DES en sections repliables (accordéons intégrés, section courante ouverte, état mémorisé, aria-expanded, pastille d'écran courant) ; section « Processus et demandes », institutions dans « Homologation » (changeset MS-IAM 20261105120000, groupe changé par un administrateur conservé) ; captures avant et après |
| 2026-11-05 | D-134 | `institution_id` : identifiant au registre côté plateforme, code participant côté sandbox, traduit une seule fois par MS-IAM (correspondance lue dans MS-PARTICIPANT) ; la passerelle ne relaie plus le jeton original |
| 2026-11-05 | D-133 | Compléments : écran du périmètre groupé par interface et par famille (table versionnée `services-perimetre.json`), référence d'origine publiée pour rattacher les campagnes reprises, contexte d'action du double rôle administrateur et intégrateur dans MS-GATEWAY. |
| 2026-11-06 | D-134 | `ConstructeursInjectionTest` dans les quatorze services : tout composant Spring, adaptateurs de prod compris (conditions non évaluées), a un constructeur `@Autowired` ou un constructeur sans argument dès qu'il en déclare plusieurs ; panne de MS-IAM du 06/11 sur le 214 |
| 2026-11-06 | D-140 | Liste paginée des membres du registre dans MS-PARTICIPANT pour la provision des comptes de test de SBX-DONNEES ; `INTERNAL_PARTICIPANT_READ` donnée à `sbx-donnees` |
| 2026-11-06 | D-141 | Sandbox : quatre comptes de test par membre (deux carte, deux mobile) dans un jeu INSTITUTION dédié, visible du seul membre ; dérogation à l'invariant réel/simulé limitée à ce jeu, validation de l'utilisateur attendue (REC D-141). |
| 2026-11-06 | D-134 | Jeton réduit de la sandbox finalisé en Docker : émetteur de la passerelle actif, clé RSA générée hors dépôt par `infra/docker-compose/scripts/generer-cle-passerelle.sh` (gimac-dev.sh, deployer-serveur.sh), JWKS ouvert aux services de la sandbox |
| 2026-11-06 | D-133, complément | REC : route console de SBX-ESPACE de rattachement d'un compte MS-IAM existant à l'espace ouvert de son institution (profils SBX, projection, espace personnel, idempotente), appelée par la migration de l'ancienne sandbox pour les collaborateurs injectés au format ancien. |
| 2026-11-06 | D-133, complément | MS-IAM : `ROLE_CHARGE_IMPLEMENTATION_SNA` livré attribuable (`iam.profils.attribuables`, interne, hors `externes-initiaux`) et recomposable par la matrice de la sandbox (`PROFILS_MATRICE`), pour que la publication de SBX-ESPACE, qui porte `SBX_CER_CONSULTATION` sur ce profil, ne soit plus refusée en 422 `IAM_SBX_PROFIL_INVALIDE` ; un profil inconnu reste refusé. À déployer avant SBX-ESPACE. |
| 2026-11-07 | D-143 | MS-PARTICIPANT : codes de service des souscriptions aux abréviations du TDR-SBX-GIMAC-02 D.1, préfixes `ISO.` et `CRT.` admis ; la nomenclature reste dérivée des abonnements actifs (D-120). |
| 2026-11-07 | D-143, complément | MS-HOMOLOGATION : codes de service à préfixe admis au service supplémentaire et à la reprise des liaisons ; souscriptions de test par l'institution de recette 99101. |

### Mise a jour du 02/11/2026 : Releve Sandbox GIMAC et integration documentaire
- MS-HOMOLOGATION : correction de DocumentRestAdapter pour transmettre service-account-homologation-service en cas de deposantId nul, securisation du scheduler de releve periodique par capture des exceptions, suppression du poller KafkaProducer parasite.
- MS-DOCUMENT : desactivation de Docker Compose au demarrage Spring Boot en profil prod, alignement du conteneur avec l image supportant la consultation d empreinte PiecesSandboxResource (D-120).
- Releve Sandbox de bout en bout : synchronisation effective des 197 liaisons, 5 973 executions retenues, 396 pieces probantes deposees et 5 821 scenarios non rapproches ; affichage complet et sans erreur de l onglet « Resultats de la sandbox » dans l interface de supervision du DES.
| 2026-11-05 | (ce commit) | Purge quotidienne du serveur 214 : images inutilisées de plus de 24 h, étiquetées ou non (et plus seulement sans étiquette), cache de construction, et échec visible du workflow sous 15 % d'espace libre sur la partition de Docker. |
| 2026-11-02 | - | MS-HOMOLOGATION : dépôt des pièces de la relève de la sandbox, planificateur de relève, poller `KafkaProducer` écarté ; MS-DOCUMENT et MS-HOMOLOGATION : Docker Compose de Spring Boot désactivé en profil `prod`. Les chiffres alors annoncés pour une relève de bout en bout (197 liaisons, 5 973 exécutions, 396 pièces) ne s'appuient sur aucun rapport conservé : ils sont retirés, les « Limites de preuve » restent valables |
| 2026-11-04 | D-126 à D-129 | Finition : sujets par défaut de MS-NOTIFICATION, isolement des tests de MS-AUDIT, `KafkaProducer` généré rétabli et déposant explicite de la relève (MS-HOMOLOGATION), `randomUUID` servi par l'application sous `script-src 'self'` ; REC : D-122 rétabli dans son texte, D-126 à D-129 ajoutés ; fichiers générés retouchés rétablis (requêtes dans des dépôts de l'infrastructure, champs reportés aux JDL) ; `PIL_PDF_INDISPONIBLE` traduit ; 214 tests OPA sur 214 |
| 2026-11-05 | D-130, D-131 | Demande de changement : RSSI seul signataire de l'étape SSI (D-130), référence au format de la fiche `CHG-{AAAA}-{N}`, nouvelle version publiée au démarrage à la place d'un modèle au gabarit daté (les instances ouvertes gardent leur version) ; D-127 à D-129 validées (D-131) ; preuve DEMO-SBX 13 reprise de la sandbox (`docs/recette/demonstration/13-changement/`) ; tous les codes `PIL_*` traduits en FR, EN et ES |
| 2026-11-06 | D-139 | Gabarit commun des PDF, au niveau de l'organigramme (étalon) : MS-DOCUMENT enveloppe le corps transmis par un service (champ `gabarit` de `/api/v1/interne/rendus/pdf`) de l'en-tête aux logos de Moabi et du GIMAC, du filet bleu à accent vert, des polices de la charte, d'un cartouche des données clés, d'un filigrane facultatif et du pied « page n sur N » avec référence, date et classification (fr, en, es), métadonnées PDF ; programme d'astreinte (MS-MONITORING) et PDF des instances de processus, dont la demande de changement (MS-PILOTAGE, paramètre `langue`), migrés ; captures avant et après dans `docs/recette/captures/pdf-standard/` |
| 2026-11-06 | D-139, complément | Mode démonstration : le programme d'astreinte et le PDF d'une instance servent un exemple rendu par la chaîne réelle au gabarit commun, marqué « Démonstration », en fr, en et es (comme l'organigramme), à la place du PDF Helvetica simulé ; filigrane réduit en A4 portrait |
| 2026-11-06 | D-139, finition | PDF d'une instance : valeurs des listes fermées traduites (« ACCEPTEE » devient « Acceptée », « Accepted », « Aceptada »), cases en oui ou non, périodes « début → fin », pièces par leur nom ; le programme d'astreinte n'avait aucune valeur brute ; captures et exemples de démonstration mis à jour |
| 2026-11-06 | - | Rapport de vérification des correctifs ISO 20022 annoncés par Paylogic (essais réels du 05/11 sur la façade de test) : `docs/paylogic/README.md`. |
| 2026-11-07 | - | Sandbox, mode plateforme de SBX-SIMULATION : `INTERNAL_SBX_ESPACE_PERSONNEL` donnée au compte de service `sbx-simulation` (espace personnel du lanceur d'un essai, route interne de SBX-ESPACE), par ajout ; realm, realm de production et domaine régénérés. |
