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
