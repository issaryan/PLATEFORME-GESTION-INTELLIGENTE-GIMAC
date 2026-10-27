# Infrastructure de la plateforme Moabi

| Dossier | Rôle | Produit par |
|---|---|---|
| `docker-compose/` | Environnement local DEV (D-24, SOC-13) | `jhipster docker-compose` + complément `docker-compose.gimac.yml` |
| `kubernetes/` | Déclarations Kubernetes des 14 services et de leurs dépendances | `jhipster kubernetes` (jamais modifié à la main) |
| `kubernetes-gimac/` | Complément Kustomize : réseau, sécurité, cache, frontend | Écrit pour le projet (D-49) |
| `ci/` | Chaîne CI/CD : validation de l'infrastructure, du backend et du frontend, images, déploiement (D-61) | Écrit pour le projet ; appelé par `.github/workflows/ci.yml` |

Principe (D-48) : ce que JHipster sait générer est régénéré, jamais édité ; les ajouts du projet vivent dans des fichiers séparés, pour qu'une régénération ne les écrase pas. Les générateurs réécrivent la version JHipster dans les `.yo-rc.json` des services en 8.11.0 : ces modifications sont annulées.

## Sandbox GIMAC : règles OPA et limites de validation

La politique `docker-compose/opa/politiques/lot1_authz.rego` et sa copie `kubernetes-gimac/partages/opa/lot1_authz.rego` sont identiques. Elles ferment le repli DES sur les routes SBX, n'ouvrent que `/bff/des/sbx-espace` et les quatre lectures D-121 de SBX-TRACE sous `SBX_SUP_CONSULTATION`, et protègent les écritures de réexamen US-EXI-23 par `IAM_ACCOUNT_CREATE` (ouverture) et `IAM_ACCOUNT_ROLES` (décision, clôture). Les autres règles SBX appliquent institution, second facteur, fonction du module et refus par défaut. `opa test infra/docker-compose/opa/politiques` a été exécuté le 27/10/2026 : **213/213**.

Le domaine généré attribue `INTERNAL_IAM_SBX_SAGA` au seul `sbx-espace` et `INTERNAL_IAM_SBX_INTROSPECT` à `gateway-service` ; les secrets restent injectés par l'environnement. La liaison des jetons au certificat client est un réglage de déploiement : l'AC mTLS locale n'est pas livrée.

Les validations OPA, Maven et Vitest ne remplacent pas un déploiement. Pour cette intégration, le Compose complet n'a pas été démarré, les assemblages spécifiques de recette restent à résorber dans l'assemblage courant et aucune preuve de connexion à de vrais serveurs GIMAC n'existe.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 9f2250a | Docker Compose généré et complément du projet |
| 2026-09-28 | 932d192 | Ports réglables, contrôle des ports, santé de Keycloak |
| 2026-09-28 | c0b91ee | Kubernetes généré et complément Kustomize |
| 2026-09-28 | 7dcca58 | Ceph RGW remplace MinIO |
| 2026-09-28 | b20a049 | Couches de présentation dans Docker Compose et Kubernetes |
| 2026-09-28 | — | Ajout de ce README |
| 2026-09-29 | — | Lot 1 : `infra/keycloak` (domaine gimac) et `infra/kafka` (sujets du lot) |
| 2026-09-29 | — | `infra/ci` : chaîne CI/CD (validation, images signées, déploiement en recette) |
| 2026-10-05 | — | Publication HTTPS (D-75) : port retenu 8463 (`GIMAC_PORT_HTTPS=8463`), noms `moabi`, `moabi-portail`, `moabi-console`.gimac-afr.org ; procédure complétée (chemins du certificat ANTIC, contrôle du relais avant DNS) |
| 2026-10-05 | — | Relais HTTPS de Moabi installé sur le nginx du serveur 214 (port 8463, trois noms) et vérifié ; en attente du DNS OVH et du pare-feu |
| 2026-10-06 | — | `kubernetes-gimac/exploitation-rbac.yaml` : lecture des événements, jeux de répliques, jobs et cronjobs de l'espace de noms et des nœuds du cluster pour le compte de service de la passerelle |
| 2026-10-08 | — | Publication HTTPS : noms internes par fichier hosts (décision de la DSI du 07/10) ; serveur fait, postes à configurer (`docs/exploitation/publication-https.md` §2 bis) |
| 2026-10-09 | — | `ci/paquet-serveur.sh`, `ci/deployer-serveur.sh --images`, cible `frontend-serveur` de `ci/construire-images.sh` : serveur 214 déployé depuis les images de la CI, sans code source (`docs/exploitation/deploiement-serveur-images.md`) ; `.env` du serveur modifié sans rompre son lien vers `~/.config/gimac-des` |
| 2026-10-09 | - | `ci/livrer-serveur.sh` : livraison du serveur 214 depuis un poste (images par SSH, paquet sans code) ; `deployer-serveur.sh --images` accepte des images déjà présentes sur le serveur (`docs/exploitation/livraison-serveur.md`) |
| 2026-10-12 | - | `docker-compose/docker-compose.serveur.yml` : volumes nommés `<service>_donnees` pour les 14 bases des microservices du serveur 214 (au lieu de volumes anonymes perdus à la suppression des conteneurs) |
| 2026-10-12 | - | `ci/deployer-serveur.sh` : seuil d'espace libre (`GIMAC_DISQUE_MIN_GO`, 10 Go) et ménage des images et du cache de plus de 3 jours après démarrage ; Prometheus plafonné à 3 Go (`docker-compose.gimac.yml`) |
| 2026-10-13 | - | `ci/fusionner-domaine.py` : option `GIMAC_SANS_COMPTES_TEST=1` (comptes `test-…` écartés), activée par `ci/deployer-serveur.sh` |
| 2026-10-13 | - | `keycloak/creer-comptes-personnels.py` : `profils`, `telephone`, `aligner`, `--supprimer` |
| 2026-10-13 | D-107 | D-107 : `reprise/importer-reprise.py` (reprise des institutions et des liaisons, simulation par défaut, `--reel`), exemples fictifs et tests joués par `ci/valider-infra.sh` ; OPA : règles de reprise |
| 2026-10-19 | D-114 | OPA : règles de `/bff/des/monitoring` par chemin et permission `MON_*`, jamais pour un compte d'institution ; Keycloak : permissions `MON_*` des profils ; Compose et Kubernetes : MS-MONITORING relié à MS-IAM et MS-DOCUMENT |
| 2026-10-19 | D-115 | Keycloak : soixante permissions `SBX_*`, six profils externes `ROLE_SBX_*`, fonctions SBX du GIMAC ajoutées aux profils existants ; `ci/valider-infra.sh` : 60 s par test OPA |
| 2026-10-20 | D-115 | OPA : routes `/bff/portail/sbx-habilitations` sous `SBX_HAB_ADMINISTRATION`, institution exigée, et leurs tests |
| 2026-10-20 | D-116 | OPA : rapports de monitoring, rapports quotidiens, activité de l'équipe et contraintes de génération sous `/bff/des/monitoring` |
| 2026-10-20 | - | D-117 : règles OPA de /grilles/creneaux (PUT, MON_ROSTER_MANAGE) et /grilles/{id}/export (MON_SUPERVISION_READ) ; INTERNAL_DOCUMENT_RENDER donné à monitoring-service ; domaine Keycloak régénéré |
| 2026-10-22 | D-120 | OPA : routes de la Sandbox GIMAC (`/bff/{console,portail}/sbx-*`, `/bff/des/sbx-espace`), permissions `SBX_<MODULE>_<FONCTION>`, repli du département fermé à `/bff/des/sbx-*` |
| 2026-10-22 | D-119 | Keycloak : client `sbx-espace` (`INTERNAL_IAM_SBX_SAGA`, `INTERNAL_PARTICIPANT_READ`), `INTERNAL_IAM_SBX_INTROSPECT` à `gateway-service`, secret `GIMAC_SECRET_SBX_ESPACE` (Compose, gimac-dev.sh, Kubernetes) ; OPA : lectures `/bff/console/iam/sandbox/**` (IAM_ACCOUNT_READ) |
| 2026-10-22 | D-120 | OPA : catalogue des types de pièces de la sandbox depuis la console (SBX_TRA_CONSULTATION, SBX_TRA_ADMINISTRATION, comptes du GIMAC) |
| 2026-10-27 | D-121 | OPA : quatre lectures SBX-TRACE et écritures de réexamen US-EXI-23, politiques Compose/Kubernetes identiques, 213 tests réussis ; limites AC mTLS locale, assemblages de recette, Compose complet et serveurs réels documentées |
