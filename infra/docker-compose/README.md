# Environnement local (Docker Compose)

Environnement de développement de la plateforme en une commande (SOC-13, D-45, D-48). Voir aussi `README-DOCKER-COMPOSE.md`, produit par JHipster.

## Fichiers

| Fichier | Rôle |
|---|---|
| `.yo-rc.json` | Réponses du générateur `jhipster docker-compose` |
| `docker-compose.yml` | **Généré** : 14 services, PostgreSQL et cache par service, Kafka, Keycloak, Prometheus, Alertmanager, Grafana. Ne pas éditer : régénérer |
| `docker-compose.gimac.yml` | Complément du projet, fusionné au lancement |
| `gimac-dev.sh` | Commande unique : prérequis, secrets, images, démarrage, arrêt ; journaux conservés dans `.journaux-echec/` en cas d'échec |
| `scripts/token.sh` | Jeton d'un utilisateur de test du domaine gimac (à sourcer) |
| `scripts/verifier-phase0.sh` | Contrôles de sortie de la Phase 0 du lot 1 |
| `tester-socle.sh` | Vérification de bout en bout du socle démarré (48 contrôles) ; code de sortie = nombre d'échecs |
| `stockage-objet/` | Ceph RGW mono-nœud et création des compartiments WORM (D-50) |
| `opa/politiques/` | Politique d'accès de développement et ses tests |
| `simulacres/` | Simulacres WireMock du simulateur, de la gestion des changements et de l'autorité d'horodatage |
| `observabilite/` | Collecteur OpenTelemetry, Tempo, sources de données Grafana |
| `.env` | Secrets et ports locaux, créés par `gimac-dev.sh`, **jamais versionnés** |

## Complément du projet

- Valkey à la place de Redis (D-25) et mots de passe d'administration issus du `.env` (D-44).
- Ceph RGW (port 7480) avec compartiments `gimac-preuves`, `gimac-documents`, `gimac-archives`, `gimac-rapports`, verrouillage d'objets activé.
- Registre des schémas Apicurio (8880), OpenBao (8200), OPA (8181).
- Simulacres : simulateur (8190, lecture seule, écritures refusées), gestion des changements (8191), horodatage (8192), messagerie Mailpit (1025, 8025).
- OpenTelemetry (4317, 4318), Loki (3100), Tempo (3200).
- Couches de présentation : département (4200), portail (4201), console (4202).

## Commandes

```
./gimac-dev.sh verifier                 # prérequis et ports libres
./gimac-dev.sh demarrer --socle         # sans les 14 services
./gimac-dev.sh images                   # construit les images Jib des 14 services
./gimac-dev.sh demarrer                 # tout
./gimac-dev.sh etat | arreter | nettoyer
./tester-socle.sh                       # vérification de bout en bout
```

Un port déjà pris sur le poste est signalé avec la variable `GIMAC_PORT_*` à placer dans `.env`. En cas d'échec, tout ce qui a démarré est arrêté. Le projet Compose s'appelle `gimac-des`.

## Régénérer la base

```
cd infra/docker-compose && jhipster docker-compose --no-insight
git checkout -- ../../ms-*/.yo-rc.json   # annule la réécriture de version par le générateur
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 9f2250a | Génération JHipster, complément, `gimac-dev.sh` |
| 2026-09-28 | 932d192 | Ports réglables, contrôle des ports, projet `gimac-des`, santé de Keycloak |
| 2026-09-28 | 7dcca58 | Ceph RGW remplace MinIO ; clés S3 dans le `.env` |
| 2026-09-28 | b20a049 | Trois couches de présentation |
| 2026-09-28 | — | Ajout de ce README |
| 2026-09-29 | — | Services dépendants de Kafka, délai de santé élargi, Valkey de la passerelle, amorçage Ceph robuste sous charge, conteneurs d'initialisation hors attente de santé, journaux conservés sur échec, `tester-socle.sh` : 48/48 (D-54) |
| 2026-09-29 | — | Lot 1 : domaine Keycloak gimac importé, émetteur gimac pour tous les services, jeton de service des deux services du lot, caches Redis retirés (régénération), `scripts/token.sh` et `scripts/verifier-phase0.sh`, politique OPA du lot (D-55) |
