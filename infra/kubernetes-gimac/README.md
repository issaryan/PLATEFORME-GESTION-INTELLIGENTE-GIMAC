# Complément Kubernetes du projet (Kustomize)

S'appuie sur `../kubernetes`, généré par JHipster, sans le modifier (D-48, D-49).

## Contenu

| Fichier | Rôle |
|---|---|
| `kustomization.yaml` | Assemblage : base générée, surcharges, étiquettes de segment, corrections de schéma, version de busybox |
| `politiques-reseau.yaml` | Refus par défaut et matrice des flux par segment : `dmz`, `socle`, `referentiels`, `exploitation`, `pilotage`, `persistance`, `identite`, `supervision`, `presentation` |
| `valkey.yaml` | Cache partagé (D-25, SOC-11) |
| `frontend.yaml` | Trois couches de présentation, profil `restricted`, limitées à la passerelle |
| `kyverno-images-signees.yaml` | Vérification des signatures d'images (Audit, puis Enforce avec SOC-02) |
| `retrait-secrets-generes.yaml` | Retrait des Secrets générés |
| `creer-secrets.sh` | Création des secrets au déploiement (puis OpenBao, SOC-06), dont `keycloak-admin` et `gimac-clients-keycloak` (secrets des clients de service) |
| `domaine-gimac/*.yaml` | Domaine Keycloak `gimac` (D-61, D-62) : émetteur des jetons des 14 services, clients de service (`iam-service`, `participant-service`, `homologation-service`, `certification-service`, `gateway-supervision`), annuaire réel de MS-IAM par l'entrée, Keycloak à l'image GIMAC (thème et domaine intégrés, administrateur d'amorçage lu dans un Secret) |

## Déployer

```
./creer-secrets.sh
kubectl apply -k .
```

Prérequis : contrôleur d'entrée NGINX dans l'espace de noms `ingress-nginx`, opérateur Kyverno, images dans `registre.gimac.local/des`, dont l'image Keycloak construite depuis `infra/keycloak/Dockerfile` (`registre.gimac.local/des/keycloak:26.7.4-gimac`).

## Vérifier sans grappe

```
kustomize build . | kubeconform -summary -ignore-missing-schemas
```

Dernier résultat : 130 objets, 111 valides, 19 ressources personnalisées non vérifiables sans leur schéma.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | c0b91ee | Création : segments, politiques réseau, Valkey, Kyverno, retrait des secrets, corrections de schéma |
| 2026-09-28 | b20a049 | Couches de présentation et segment `presentation` |
| 2026-09-28 | — | Ajout de ce README |
| 2026-09-29 | — | Passerelle raccordée au Valkey partagé (sonde Redis, limitation de débit) |
| 2026-09-29 | — | Domaine `gimac` pour tous les services, clients de service et image Keycloak GIMAC ; administrateur de Keycloak par Secret (D-62) ; `kustomize build` vérifié (130 objets) |
| 2026-09-29 | — | MS-DOCUMENT : point d'accès et clés du dépôt WORM par le Secret `gimac-stockage-objet`, fourni par l'exploitant (`GIMAC_S3_*` à l'appel de `creer-secrets.sh`) |
