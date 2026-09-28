# Infrastructure de la plateforme DES

| Dossier | Rôle | Produit par |
|---|---|---|
| `docker-compose/` | Environnement local DEV (D-24, SOC-13) | `jhipster docker-compose` + complément `docker-compose.gimac.yml` |
| `kubernetes/` | Déclarations Kubernetes des 14 services et de leurs dépendances | `jhipster kubernetes` (jamais modifié à la main) |
| `kubernetes-gimac/` | Complément Kustomize : réseau, sécurité, cache, frontend | Écrit pour le projet (D-49) |

Principe (D-48) : ce que JHipster sait générer est régénéré, jamais édité ; les ajouts du projet vivent dans des fichiers séparés, pour qu'une régénération ne les écrase pas. Les générateurs réécrivent la version JHipster dans les `.yo-rc.json` des services en 8.11.0 : ces modifications sont annulées.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 9f2250a | Docker Compose généré et complément du projet |
| 2026-09-28 | 932d192 | Ports réglables, contrôle des ports, santé de Keycloak |
| 2026-09-28 | c0b91ee | Kubernetes généré et complément Kustomize |
| 2026-09-28 | 7dcca58 | Ceph RGW remplace MinIO |
| 2026-09-28 | b20a049 | Couches de présentation dans Docker Compose et Kubernetes |
| 2026-09-28 | — | Ajout de ce README |
