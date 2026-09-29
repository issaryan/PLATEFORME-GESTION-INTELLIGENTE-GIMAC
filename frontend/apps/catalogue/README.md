# Catalogue vivant (@gimac/app-catalogue)

Écran de démonstration `/catalogue` du DS-GIMAC-01 (§12.2), hors production : chaque composant de `@gimac/design-system` est affiché un à un, avec sa fiche et son aperçu, en thème clair ou sombre. Il n'est pas embarqué dans l'image de production.

```
npm run dev:catalogue      # depuis frontend/, http://localhost:4203
```

- `src/apercus/<Composant>.tsx` : un aperçu par composant ; ajouter le fichier suffit à l'afficher.
- `src/fondations/` : couverture, couleurs, typographie, espacements, logo, motif, pictogrammes.
- `src/plan.ts` : inventaire des composants du système publié (groupe, dimensions, fiche).
- `src/fabriques/` : données fictives de développement (« Établissement de paiement A », « Banque B »).

Sous la largeur prévue d'une carte, l'aperçu est réduit à l'échelle pour rester entier. `npm run test -w @gimac/app-catalogue` rend chaque aperçu et l'audite avec axe, en clair et en sombre.

Navigation : liste à gauche, boutons Précédent et Suivant, Alt + flèche haut ou bas ; l'adresse porte le composant affiché (`#/Button`).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 6eca1ce | Création du catalogue vivant |
| 2026-09-29 | — | Mise à l'échelle des aperçus sur écran étroit, audit axe de tous les aperçus |
| 2026-09-29 | — | Sélecteur de langue FR, EN, ES des aperçus |
| 2026-09-29 | — | Aperçus des composites `OneTimeSecret`, `PermissionPicker` et `LiveStatGroup` (D-62), audités par axe en clair et en sombre |
