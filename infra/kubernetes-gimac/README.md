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
| `creer-secrets.sh` | Création des secrets au déploiement (puis OpenBao, SOC-06) |

## Déployer

```
./creer-secrets.sh
kubectl apply -k .
```

Prérequis : contrôleur d'entrée NGINX dans l'espace de noms `ingress-nginx`, opérateur Kyverno, images dans `registre.gimac.local/des`.

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
