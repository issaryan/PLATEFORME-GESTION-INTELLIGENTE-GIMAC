# Domaine Keycloak « gimac »

`gimac-realm.json` est produit par `generer-realm-gimac.py` à partir du realm de développement généré par JHipster ; ne pas l'éditer à la main, relancer le script.

Contenu : permissions du lot 1 (`HOM_*`, `CER_*`, `INTERNAL_*`), six profils composites (`ROLE_INGENIEUR_HOMOLOGATION`, `ROLE_CHEF_DES`, `ROLE_RSSI`, `ROLE_CONFORMITE`, `ROLE_DG`, `ROLE_CORRESPONDANT`), sept utilisateurs de test (`test-*`, les correspondants portant `institution_id`), clients `tester-cli`, `homologation-service` et `certification-service`, claim `institution_id` dans le jeton.

Aucun secret : mots de passe et secrets de client sont des variables `${GIMAC_...}` résolues par Keycloak à l'import, à partir du `.env` local créé par `gimac-dev.sh`. Jeton d'un utilisateur de test : `source infra/docker-compose/scripts/token.sh test-chef-des`.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création du domaine gimac pour le lot 1 (D-55) |
