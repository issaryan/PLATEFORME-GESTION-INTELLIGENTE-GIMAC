# Chaîne d'intégration et de déploiement continus (CI/CD)

Ce dossier contient toute la logique de validation de la plateforme Moabi : infrastructure,
backend (14 microservices JHipster) et frontend (couches de présentation). Les scripts
s'exécutent à l'identique sur un poste de développement et dans GitHub Actions. La définition
de la chaîne, `.github/workflows/ci.yml`, se limite à installer les outils et à appeler ces
scripts : GitHub n'exécute un workflow que s'il se trouve dans `.github/workflows`.

## Définition de « terminé » (D-61)

Une modification n'est intégrée à `main` que si la chaîne est entièrement verte :

| Exigence D-61 | Contrôle | Seuil bloquant |
|---|---|---|
| Tests unitaires | Surefire (backend), Vitest (frontend) | tout test en échec |
| Tests d'intégration | Failsafe avec Testcontainers (PostgreSQL, Kafka, S3…) | tout test en échec |
| Tests d'acceptation | scénarios Cucumber (backend), Playwright par acteur (frontend) | tout scénario en échec |
| Tests de sécurité | gitleaks (secrets), Trivy (vulnérabilités), politiques OPA, contrôles BNF-07 | secret en clair, vulnérabilité HIGH/CRITICAL corrigeable |
| Couverture du code | JaCoCo (backend), Vitest v8 (frontend) | < 80 % (lignes, méthodes/fonctions, branches) |
| Couverture de bout en bout | Playwright + nyc sur les constructions instrumentées | < 80 % |
| Qualité | Checkstyle, Modernizer, tsc, oxlint, shellcheck, hadolint | toute erreur |

## Étapes de la chaîne

```
préparation ──┬── infra ─────┐
              ├── backend ×N ┤   (sur main seulement)
              ├── frontend ──┼── images ×16 ── déploiement (environnement « recette »)
              └── sécurité ──┘
```

| Tâche | Script | Contenu |
|---|---|---|
| Préparation | `valider-backend.sh --lister-json` | liste des microservices munis de tests (matrice du backend), nom du registre |
| Infrastructure | `valider-infra.sh` | voir ci-dessous |
| Backend | `valider-backend.sh <service>` | `./mvnw -ntp -B clean verify` : compilation, Checkstyle, tests unitaires, d'intégration et d'acceptation, Modernizer, seuils JaCoCo du `pom.xml` ; rapports publiés en artefacts |
| Frontend | `valider-frontend.sh --e2e` | `npm ci`, `npm run lint`, `npm run couverture`, `npm run build`, contrôle des paquets construits (aucune donnée fictive du simulateur), `npm run e2e:couverture` ; rapports de couverture publiés |
| Contrat des routes | `valider-contrat.sh` | chaque appel HTTP du frontend a sa route de passerelle, une règle OPA qui le laisse passer et un contrôleur dont le `@PreAuthorize` admet la permission d'OPA (`contrat/routes.mjs`, écarts admis dans `contrat/ecarts-connus.txt`, tableau dans `docs/lot1/correspondance.md`) |
| Sécurité | `valider-securite.sh` + Trivy | secrets dans l'arborescence et dans les commits nouveaux ; vulnérabilités des dépendances Maven et npm ; configuration Docker et Kubernetes (informatif) |
| Images | `construire-images.sh --pousser <cible>` | microservices par Jib (profil `prod`), frontend (3 images) et Keycloak par Dockerfile ; publication dans GHCR et signature cosign |
| Déploiement | `deployer-recette.sh <liste>` | `kubectl apply -k` de `infra/kubernetes-gimac` avec les images de ce commit, référencées par empreinte, puis attente des déploiements |

### Validation de l'infrastructure

`valider-infra.sh` mène tous les contrôles, puis affiche un bilan (réussi, ignoré, échec) :

1. **Scripts** : `shellcheck -S warning` et `bash -n` sur tous les `.sh` de `infra`
   (hors `infra/kubernetes`, généré par JHipster et jamais modifié à la main, D-48).
2. **Docker Compose** : `docker compose config -q` sur `docker-compose.yml` seul, puis fusionné
   avec `docker-compose.gimac.yml` comme le fait `gimac-dev.sh`. Les secrets exigés
   (`${VAR:?…}`) reçoivent une valeur factice : le `.env` n'est jamais versionné (BNF-07).
3. **OPA** : `opa check --strict`, `opa fmt` (format canonique), `opa test` et couverture des
   politiques par leurs tests ≥ 80 %.
4. **Kubernetes** : `kustomize build infra/kubernetes-gimac`, absence de `Secret` et d'image
   `latest` dans le rendu, `kubeconform -strict` (schémas Kubernetes 1.33 et catalogue des CRD ;
   une ressource sans schéma connu est signalée sans bloquer).
5. **Supervision** : `promtool check config`, `promtool check rules`, `amtool check-config`.
6. **Dockerfiles** : `hadolint --failure-threshold warning`.
7. **Domaine Keycloak** : `generer-realm-gimac.py` est rejoué dans une copie ; le résultat doit
   être identique à `infra/keycloak/gimac-realm.json` et à `gimac-realm-production.json` (sans compte de
   test, retenu par `--build-arg DOMAINE=gimac-realm-production.json`) versionnés. Aucun client confidentiel ne
   doit porter de secret en clair (seulement des variables `${…}`).
   Les tests de `fusionner-domaine.py` (`test_fusionner_domaine.py` : rôles, comptes, politique de sécurité)
   s'exécutent dans cette section.
8. **JSON et YAML** : syntaxe des JSON, `yamllint` (règles de `yamllint.yml`).
9. **Chaîne** : `actionlint` sur `.github/workflows`.

Les versions d'OPA, promtool et amtool sont celles des images du Compose : la configuration est
validée par l'outil qui l'exécutera.

## Exécution sur un poste

```bash
# Outils de validation, en versions épinglées (Linux x86_64)
infra/ci/installer-outils.sh                 # dans ~/.local/bin
export PATH="$HOME/.local/bin:$PATH"

infra/ci/valider-infra.sh                    # quelques secondes, sans conteneur
infra/ci/valider-securite.sh                 # gitleaks (et trivy s'il est installé)
infra/ci/valider-frontend.sh                 # lint, couverture, construction
infra/ci/valider-frontend.sh --e2e           # + tests de bout en bout Playwright
infra/ci/valider-contrat.sh                  # contrat des routes frontend, passerelle, OPA, contrôleurs
node infra/ci/contrat/routes.mjs --markdown  # tableau route par route
infra/ci/valider-backend.sh ms-iam           # un service (Docker requis pour Testcontainers)
infra/ci/valider-backend.sh                  # les 14 services, l'un après l'autre
infra/ci/valider-tout.sh                     # tout, dans l'ordre de la CI

infra/ci/construire-images.sh frontend       # images locales (registre.gimac.local/des/…)
infra/ci/deployer-recette.sh images.txt --simulation   # rendu de la surcouche de recette
```

Un outil absent du poste est **ignoré** avec un message clair ; en CI, `GIMAC_CI_STRICT=1`
transforme cette absence en échec. Sans démon Docker, `valider-backend.sh` saute les tests
d'intégration (`-DskipITs`) et le signale.

| Variable | Script | Effet |
|---|---|---|
| `GIMAC_CI_STRICT=1` | tous | un outil absent est un échec |
| `GIMAC_MAVEN_OBJECTIFS`, `GIMAC_MAVEN_OPTIONS` | backend | objectifs (défaut `clean verify`) et options Maven |
| `GIMAC_SANS_INTEGRATION=1` | backend | saute les tests d'intégration |
| `GIMAC_E2E=1`, `GIMAC_NPM_CI`, `GIMAC_CHROMIUM` | frontend | tests de bout en bout, installation, navigateur |
| `GIMAC_PLAGE_COMMITS` | sécurité | commits examinés par gitleaks (ex. `origin/main..HEAD`) |
| `GIMAC_REGISTRE`, `GIMAC_ETIQUETTE` | images | registre et étiquette des images |
| `GIMAC_VERSION_K8S` | infra | version de Kubernetes des schémas (défaut 1.33.0) |

## Publication, signature et déploiement

Sur un push de `main`, une fois toutes les validations réussies :

- les 16 cibles (`frontend` → 3 images, `keycloak`, 14 microservices) sont construites et
  publiées dans `ghcr.io/<propriétaire>/gimac-des/<nom>:<commit>` ;
- chaque image est signée par cosign, référencée par son empreinte :
  - **sans clé** (Sigstore), identité du workflow :
    émetteur `https://token.actions.githubusercontent.com`, sujet
    `https://github.com/<dépôt>/.github/workflows/ci.yml@refs/heads/main` ;
  - **par clé** si le secret `COSIGN_PRIVATE_KEY` (et `COSIGN_PASSWORD`) est défini : c'est la
    signature que vérifie `infra/kubernetes-gimac/kyverno-images-signees.yaml`, dont la clé
    publique est lue dans le secret `kyverno/cle-signature-images` ;
- la tâche `deploiement`, liée à l'environnement GitHub protégé `recette` (approbation
  configurable), applique `infra/kubernetes-gimac` avec ces images si le secret `KUBECONFIG`
  (contenu du fichier) est défini dans cet environnement ; sinon elle est sautée avec un avis.

Secrets à définir dans GitHub : `COSIGN_PRIVATE_KEY`, `COSIGN_PASSWORD` (dépôt, facultatifs),
`KUBECONFIG` (environnement `recette`). `GITHUB_TOKEN` suffit à publier dans GHCR.

## Fichiers

| Fichier | Rôle |
|---|---|
| `commun.sh` | fonctions partagées : sections, états, bilan, résumé GitHub |
| `installer-outils.sh` | installation des outils en versions épinglées |
| `valider-infra.sh`, `valider-backend.sh`, `valider-frontend.sh`, `valider-securite.sh`, `valider-contrat.sh` | validations |
| `contrat/routes.mjs`, `contrat/ecarts-connus.txt` | contrôle du contrat des routes et écarts admis |
| `valider-tout.sh` | toutes les validations sur le poste |
| `construire-images.sh`, `deployer-recette.sh` | livraison et déploiement continus |
| `deployer-serveur.sh`, `fusionner-domaine.py`, `test_fusionner_domaine.py` | serveur de démonstration (10.20.36.214) : images construites sur place, démarrage par `gimac-dev.sh` avec `docker-compose.serveur.yml`, Keycloak persistant (domaine modifié réimporté par fusion à trois voies en conservant comptes, mots de passe, TOTP, profils créés, recompositions et politique de sécurité modifiée dans la console ; même fusion dans `mettre-a-jour.sh` et le Job Kubernetes `keycloak-domaine` ; tests lancés par `valider-infra.sh`), comptes nominatifs, configurations montées (politiques OPA, simulacres, observabilité) suivies par empreinte : un service dont la configuration a changé est redémarré puis attendu sain ; lancé par `.github/workflows/deploiement-serveur.yml` sur l'exécuteur auto-hébergé `gimac-214` à chaque poussée sur `main` |
| `yamllint.yml`, `gitleaks.toml` | règles de yamllint et de gitleaks |
