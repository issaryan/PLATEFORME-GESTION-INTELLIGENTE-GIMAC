# Console d'administration et d'audit

Couche de présentation `@gimac/app-console` (D-46, CC 6.3.1).

| Élément | Valeur |
|---|---|
| Public | administrateurs, RSSI et auditeurs |
| Zone réseau | zone d'administration hors bande (D-17) |
| Authentification | certificat client, facteur FIDO2 et sessions courtes |
| Port de développement | 4202 |
| Image | `frontend-console` (`docker build --build-arg APPLICATION=console` dans `frontend/`) |

Généré par `create-vite` (gabarit `react-ts`). La page de démonstration de Vite est remplacée par une page d'accueil provisoire en français, sur les jetons de `@gimac/theme`, en attendant l'enveloppe commune (IHM-01).

```
npm run dev:console      # depuis frontend/
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Génération `create-vite`, page d'accueil provisoire, thème, relais /api vers la passerelle |
| 2026-09-28 | — | Remplacement du README du gabarit Vite par ce README |
