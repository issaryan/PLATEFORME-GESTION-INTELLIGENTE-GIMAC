# Domaine Keycloak « gimac »

`gimac-realm.json` est produit par `generer-realm-gimac.py` à partir du realm de développement généré par JHipster ; ne pas l'éditer à la main, relancer le script.

Contenu : permissions (`HOM_*`, `CER_*`, `PAR_*`, `INTERNAL_*`) ; treize profils composites **par fonction** (D-58) : `ROLE_INGENIEUR_HOMOLOGATION`, `ROLE_CHEF_DES`, `ROLE_RSSI`, `ROLE_CONFORMITE`, `ROLE_DG`, `ROLE_CORRESPONDANT`, `ROLE_CHARGE_DDRM`, `ROLE_CHEF_DDRM`, `ROLE_SNA`, `ROLE_PRODUCTION_MONETIQUE`, `ROLE_POLE_RESEAU`, `ROLE_POLE_SYSTEME`, et le rôle dédié `ROLE_CONDUCTEUR_PROJET` (habilite à conduire tout projet ; le projet lui est confié par désignation) ; quatorze utilisateurs de test (`test-*`, les correspondants portant `institution_id`) ; clients `tester-cli`, `homologation-service`, `certification-service`, `participant-service` ; claim `institution_id` dans le jeton.

**Double facteur (D-61).** Flux navigateur `navigateur-gimac` à niveaux d'assurance : niveau 1 mot de passe, niveau 2 mot de passe puis code TOTP (enrôlé à la première connexion, action `CONFIGURE_TOTP`). Les clients `web_app` (connexion par la passerelle) et `tester-cli` exigent le niveau 2 (`minimum.acr.value`) ; Keycloak pose le niveau atteint dans le claim `acr`, contrôlé par OPA à la passerelle et par chaque service. `tester-cli` accepte l'autorisation par appareil (RFC 8628) pour les contrôles cURL. Le jeu de flux est à vérifier à l'import sur le poste de développement (Keycloak n'est pas disponible dans l'environnement distant).

Aucun secret : mots de passe et secrets de client sont des variables `${GIMAC_...}` résolues par Keycloak à l'import, à partir du `.env` local créé par `gimac-dev.sh`. Jeton d'un utilisateur de test : `source infra/docker-compose/scripts/token.sh test-chef-ddrm` (niveau 2, par le navigateur) ou `NIVEAU=1 source ...` (niveau 1, pour vérifier les refus).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création du domaine gimac pour le lot 1 (D-55) |
| 2026-09-29 | — | Permissions `PAR_*`, profils par fonction et rôle `CONDUCTEUR_PROJET` (D-58), client `participant-service`, flux à double facteur et niveau 2 exigé (D-61) |
