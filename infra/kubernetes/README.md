# Déclarations Kubernetes générées

Dossier **produit par `jhipster kubernetes`** à partir de `.yo-rc.json` : espace de noms `gimac-des`, 14 services, bases PostgreSQL, Kafka, Keycloak, Prometheus et Grafana, entrée NGINX sur `des.gimac.local`. Voir aussi `K8S-README.md`, produit par JHipster.

Ne pas éditer ces fichiers : le complément du projet est dans `../kubernetes-gimac`, qui s'appuie sur ce dossier (D-49). Les Secrets générés ici contiennent des valeurs déclarées compromises (D-44) ; le complément les retire.

## Régénérer

```
cd infra/kubernetes && jhipster kubernetes --no-insight
git checkout -- ../../ms-*/.yo-rc.json
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | c0b91ee | Génération JHipster 9.4.0 |
| 2026-09-28 | — | Ajout de ce README |
