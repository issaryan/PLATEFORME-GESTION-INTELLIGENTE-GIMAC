# Identité du logiciel Moabi (D-101)

Demande de KOAGNE ISSA RYAN (09/10/2026) : le haut de la page (onglet du navigateur) montrait encore l'icône de Vite,
l'outil de construction de l'interface. Les propositions tirées du logo du GIMAC n'ont pas été retenues : l'identité doit
être celle du logiciel lui-même, belle et déclinable. Parmi quatre directions (cernes, houppier, monogramme, feuille),
le houppier et la feuille sont retenus, employés selon les circonstances. L'onglet porte simplement « Moabi ».

## 1. Deux symboles, un usage chacun

- **Houppier**, symbole principal : couronne du moabi aplatie en parasol, trois strates (jeune pousse, canopée, forêt),
  fût et contreforts en bois de cœur. Logo avec le nom, écran de connexion, documents, icônes d'application (180, 192,
  512 px).
- **Feuille**, marque compacte : limbe obovale, nervures évidées (masque SVG, transparentes sur tout fond). Icône
  d'onglet (16 à 48 px), point du i du nom, petits repères.
- Nom : « moabi » en minuscules, Ubuntu 500 vectorisé, feuille à la place du point du i.

## 2. Fichiers

- `frontend/packages/design-system/src/assets/logo/moabi/` : `moabi-symbole*.svg`, `moabi-feuille*.svg`, `moabi-logo*.svg`
  (clair, sombre, blanc, noir).
- `frontend/apps/<application>/public/` : `favicon.svg` (feuille, teinte adaptée au thème du navigateur), `favicon.ico`
  (16, 32, 48 px), `apple-touch-icon.png` (180 px), `icone-192.png`, `icone-512.png`, `site.webmanifest` (nom « Moabi »,
  couleur de l'application). Tuiles : département `#12352B`, portail `#1C4E73`, console `#2B2E35`, catalogue `#6E3A1E`.
- `index.html` des quatre applications : titre « Moabi », liens d'icônes, manifeste, `theme-color`. Les écrans ne
  modifient plus le titre de l'onglet (connexion, notifications, mon compte).
- Le manifeste et les icônes sont servis par l'origine de l'application (`default-src 'self'` de la CSP suffit).

## 3. Documents

- IG-GIMAC-01 v1.3 : charte graphique, sous-section « Identité du logiciel Moabi » (symboles, déclinaisons, couleurs,
  onglet, tuiles, règles d'usage) ; le logo du GIMAC reste la marque de l'institution.
- DS-GIMAC-01 §2.4 : fichiers, couleurs, tailles et règles.

## 4. Hors périmètre

Le logo du GIMAC reste dans la barre latérale et la barre du haut ; son remplacement par le logo Moabi n'est pas décidé.
