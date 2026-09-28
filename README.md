# Plateforme de suivi des activités du DES du GIMAC

Plateforme du Département Exploitation et Supervision (DES) du GIMAC, groupement interbancaire monétique de la CEMAC. Elle **lit, référence et trace** les activités du département : homologation et certification des liaisons, exploitation et incidents, rapprochement, pilotage, reporting et collaboration. Elle n'écrit jamais dans le commutateur, le simulateur de certification ni la gestion des changements, ne traite aucune donnée de carte et ne comporte aucune composante d'intelligence artificielle (D-27).

## Documents de référence

| Document | Rôle |
|---|---|
| `CCT-GIMAC-01_v1.1` | Cahier des charges techniques : besoins BF et BNF, seuils contractuels |
| `CA-GIMAC-01_v1.1` | Cahier d'analyse : acteurs, cas d'utilisation, règles de gestion RG |
| `CC-GIMAC-01_v1.1` | Cahier de conception : 14 microservices, événements, algorithmes, thème |
| `CP-GIMAC-01_v2.0` | Carnet de produit : 313 récits, 17 épopées, feuille de route S0 à S12 |
| `REC-GIMAC-01_Reconciliation.md` | **Référentiel d'implémentation** : décisions D-01 à D-51, prime sur les quatre cahiers pour les points qu'il tranche |

## Organisation du dépôt

| Dossier | Contenu |
|---|---|
| `ms-gateway` … `ms-collaboration` | Les 14 microservices JHipster (Java 21, Spring Boot 3), un JDL par service |
| `frontend/` | Les trois couches de présentation React : application du département, portail des participants, console d'administration et d'audit |
| `infra/docker-compose/` | Environnement local : Docker Compose généré par JHipster et complément du projet |
| `infra/kubernetes/` | Déclarations Kubernetes générées par JHipster |
| `infra/kubernetes-gimac/` | Complément Kustomize du projet : politiques réseau, sécurité, frontend |

Chaque dossier a son propre README, avec un journal des mises à jour.

## Pile technique

Java 21, Spring Boot 3 et JHipster 9.4.0 ; Kafka et CloudEvents ; PostgreSQL, un schéma par service ; Keycloak (OIDC) ; Valkey ; Ceph RGW pour le stockage WORM (D-50) ; OpenBao ; Open Policy Agent ; React 19, TypeScript et Vite (D-51) ; Prometheus, Grafana, OpenTelemetry, Loki, Tempo ; Kubernetes et Kyverno.

## Démarrer en local

```
infra/docker-compose/gimac-dev.sh verifier          # prérequis et ports
infra/docker-compose/gimac-dev.sh demarrer --socle  # infrastructure et frontend, sans les 14 services
infra/docker-compose/gimac-dev.sh images            # images Jib des 14 services
infra/docker-compose/gimac-dev.sh demarrer          # tout
```

Les secrets locaux sont générés dans `infra/docker-compose/.env`, jamais versionné.

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
- Prochaine étape : sprint 0 du carnet (gabarit hexagonal SOC-01, chaîne de livraison SOC-02, contrôles QUA-01, QUA-02, QUA-09, QUA-10).

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
