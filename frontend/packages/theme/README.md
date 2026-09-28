# @gimac/theme

Jetons de conception du thème de la plateforme (CC-GIMAC-01 §6.2, récit IHM-02), exposés en variables CSS dans `jetons.css` :

- palette : bleu institutionnel, gris de structure, fonds, texte, trois teintes de statut toujours doublées d'un texte ou d'un pictogramme ;
- typographie : base 14 pt, titres 24, 18 et 16 pt, chiffres à chasse fixe ;
- grille de 8 pixels, bordures de 1 px, angles droits, aucune ombre ;
- mode sombre par `data-theme="sombre"` sur l'élément racine.

Usage : `import '@gimac/theme/jetons.css'` dans le point d'entrée de chaque couche.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Création des jetons à partir du CC 6.2 |
| 2026-09-28 | — | Ajout de ce README |
