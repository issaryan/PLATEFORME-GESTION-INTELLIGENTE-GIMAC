# Application du département

Couche de présentation `@gimac/app-departement` (D-46, CC 6.3.1).

| Élément | Valeur |
|---|---|
| Public | collaborateurs du DES et des directions |
| Zone réseau | zone applicative |
| Authentification | fournisseur d'identité et facteur FIDO2 |
| Port de développement | 4200 |
| Image | `frontend-departement` (`docker build --build-arg APPLICATION=departement` dans `frontend/`) |

Généré par `create-vite` (gabarit `react-ts`). La page de démonstration de Vite est remplacée par une page d'accueil provisoire en français, sur les jetons de `@gimac/theme`, en attendant l'enveloppe commune (IHM-01).

```
npm run dev:departement      # depuis frontend/
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Génération `create-vite`, page d'accueil provisoire, thème, relais /api vers la passerelle |
| 2026-09-28 | — | Remplacement du README du gabarit Vite par ce README |
