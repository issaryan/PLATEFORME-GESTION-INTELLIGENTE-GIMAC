# DS-GIMAC-01 : Système de conception et guide d'interface de la plateforme intégrée du DES

Version 1.1, 1er octobre 2026 (dérogations D-66 de REC-GIMAC-01, signalées « D-66 » ci-dessous ; version 1.0 du 28 septembre 2026). Établi par KOAGNE ISSA RYAN, Ingénieur, et NGOUNOU STEVE DELORS, Ingénieur, pour le Groupement Interbancaire Monétique de l'Afrique Centrale (GIMAC), Département Exploitation et Supervision (DES).

Documents amont : cahier de charges techniques CCT-GIMAC-01 v1.1 (besoins BF-26 à BF-28, BNF-13, BNF-17, BNF-18), cahier d'analyse CA-GIMAC-01 v1.1 (domaine 7), cahier de conception CC-GIMAC-01 v1.1 (section 6.2 thème, section 6.3 cartographie des écrans), carnet de produit CP-GIMAC-01 v2.0 (épopée IHM, récits d'écran de chaque microservice).

---

## 0. Mode d'emploi du document

### 0.1 Objet

Ce document pense l'interface de la plateforme de A à Z : l'identité visuelle et la charte graphique, les couleurs, la typographie, les espacements, les icônes, les motifs, l'enveloppe applicative, le catalogue complet des composants, le principe de composition par composants réutilisables (dont le CRUD universel en quatre composants), les gabarits d'écran, la cartographie exhaustive des écrans des trois couches de présentation, les règles d'accessibilité, d'internationalisation, de performance et de sécurité côté client, et enfin les prompts destinés à l'agent codeur, page par page et composant par composant.

Il est la référence unique du frontend. Toute maquette, tout composant, tout écran et tout prompt de développement s'y conforment ; un écart est une demande de changement, pas une initiative.

### 0.2 Lecteurs

Le concepteur d'interface (maquettes), l'équipe de réalisation, l'agent codeur qui reçoit les prompts du chapitre 12, le responsable de produit (chef du DES) qui valide les maquettes, le RSSI pour les règles de sécurité côté client, et le testeur pour les critères de revue.

### 0.3 Conventions

- Les jetons de conception (design tokens) sont préfixés `--gm-` (GIMAC), comme dans les autres produits du groupement, et exposés en propriétés CSS personnalisées. Un composant ne consomme jamais une couleur, un espacement ou une durée en dur : il consomme un jeton sémantique. Toute valeur en dur est un motif de refus en revue.
- Les tailles sont exprimées en pixels CSS (px) pour les dimensions de composant et en `rem` pour la typographie (base 16 px), le texte courant de l'interface étant à 14 px (0,875 rem) pour supporter la densité des écrans de supervision.
- Les noms de composants sont en PascalCase (`DataTable`), les jetons en kebab-case (`--gm-color-text-primary`), les routes en kebab-case français (`/institutions/{id}/pieces-legales`).
- Le vocabulaire d'interface est celui du chapitre 13 ; il est imposé.
- Ce document ne contient aucun nom de personne, aucune adresse, aucun secret, aucune donnée réelle d'institution.

### 0.4 Principes directeurs (les sept règles)

1. **Sobriété institutionnelle.** L'interface est un outil de travail réglementé, pas une vitrine : angles droits, bordures fines, aucune ombre portée, aucun dégradé, aucune animation d'ambiance.
2. **Lisibilité en salle de supervision.** Lecture prolongée, de jour comme de nuit, sur poste et sur mur d'écrans : contrastes élevés, chiffres à chasse fixe, mode sombre dérivé de la même palette.
3. **Aucune information portée par la seule couleur.** Tout statut est accompagné d'un libellé et d'un pictogramme ; toute visualisation dispose d'une vue en tableau.
4. **Composants, pas de pages.** Un écran est une composition de composants du catalogue, paramétrés par des descripteurs ; aucune logique métier dans une page.
5. **Quatre états par écran.** Chargement, vide, erreur, nominal ; un écran qui n'a pas ses quatre états n'est pas fini.
6. **La couche de présentation ne sait rien.** Elle ne détient ni état durable, ni secret, ni règle métier ; elle appelle la passerelle et affiche ce que les services décident ; les gardes d'interface masquent, le serveur refuse.
7. **Ancrage régional sans folklore.** L'appartenance à l'Afrique centrale s'exprime par la typographie, un motif géométrique discret, les six drapeaux du logo et le vocabulaire, jamais par des couleurs criardes ni des illustrations stéréotypées.

---

## 1. Identité et positionnement visuel

### 1.1 Trois ancrages

**GIMAC.** La plateforme porte l'identité du groupement : le logo, son bleu, la coche verte, les six drapeaux de la CEMAC (Cameroun, République centrafricaine, Congo, Gabon, Guinée équatoriale, Tchad). Le bleu du logo est la couleur d'action ; le vert du logo est réservé aux états nominaux et aux validations, ce qui prolonge naturellement le sens de la coche.

**Afrique centrale.** L'ancrage régional est exprimé par trois moyens maîtrisés : la famille typographique de titrage Ubuntu, humaniste, ouverte, dont le nom porte une philosophie africaine du lien (« je suis parce que nous sommes »), en résonance avec la vocation interbancaire du groupement ; un motif géométrique de trame, inspiré des tissages et textiles d'Afrique centrale (lignes brisées et losanges alignés sur la grille de 8 px), employé à faible contraste sur les pages d'accueil, les états vides et l'écran de connexion, jamais derrière du texte courant ; une palette d'accent « terre » (ocre et latérite) réservée aux moments de marque (connexion, états vides, en-têtes de rapport), jamais aux statuts.

**Technologie.** Le caractère technologique tient à la précision : grille de 8 px, chiffres tabulaires, identifiants en chasse fixe, tableaux denses, transitions courtes, mode sombre, absence de décoration. La plateforme doit donner la sensation d'un instrument de mesure fiable.

### 1.2 Ce que l'interface n'est pas

Elle n'est pas une application grand public : pas de coins arrondis, pas d'ombres, pas de dégradés, pas d'illustrations de personnages, pas d'emojis, pas d'animations décoratives, pas de couleurs de statut en aplat de fond, pas de rouge ou d'orange sur de grandes surfaces.

---

## 2. Logo

### 2.1 Versions

| Version | Fichier | Usage |
|---|---|---|
| Couleur, fond clair | `logo/gimac-couleur.svg` (source vectorielle), `gimac-couleur@2x.png` | Page de connexion, en-tête des rapports PDF/A-3, portail des participants |
| Monochrome bleu | `logo/gimac-bleu.svg` | Barre supérieure des trois couches sur fond clair |
| Monochrome blanc | `logo/gimac-blanc.svg` | Barre supérieure en mode sombre, bandeaux bleus |
| Symbole seul (la coche et le « G ») | `logo/gimac-symbole.svg` | Favicon, icône d'application, avatar système des notifications, 16 à 48 px |

Le logo original (`GIMAC-LOGO.png`, 1024 × 341) est la référence colorimétrique ; la vectorisation est réalisée à partir de lui sans modification des proportions.

### 2.2 Zone de protection et tailles

- Zone de protection : au moins la hauteur du « G » sur les quatre côtés ; aucun élément (texte, bordure, pictogramme) ne pénètre cette zone.
- Taille minimale du logo complet : 120 px de large à l'écran, 30 mm à l'impression. En dessous, utiliser le symbole seul.
- Dans la barre supérieure : hauteur 28 px (logo monochrome), aligné à gauche, suivi du nom de la couche.
- Favicon : symbole seul, 32 × 32 et 16 × 16 ; icône d'application 192 × 192 et 512 × 512.

### 2.3 Interdits

Ne jamais déformer, incliner, recolorer, ombrer, contourner, animer le logo ; ne jamais le placer sur le motif de trame à plus de 8 % d'opacité ; ne jamais le placer sur une photographie ; ne jamais réordonner les drapeaux dans la version couleur ; dans la barre latérale, le logo s'emploie seul, sans les drapeaux (D-66).

---

## 3. Couleurs

### 3.1 Primitives

Les primitives sont les valeurs brutes ; les composants ne les consomment jamais directement (voir 3.2). Les rapports de contraste indiqués sont mesurés selon WCAG 2.2 sur fond blanc `#FFFFFF`, sur fond de page `#F5F6F8` et sur fond de cartouche `#EAF0F7`.

**Bleu GIMAC (couleur d'action et d'identité)**

| Jeton primitif | Hex | Contraste sur blanc / page / cartouche | Rôle |
|---|---|---|---|
| `--gm-blue-900` | `#0F3D70` | 11,2 | Texte des titres en mode clair, survol des boutons principaux |
| `--gm-blue-700` | `#15569E` | 7,35 / 6,80 / 6,41 | Titres, en-têtes de tableaux, liens, bouton principal (texte blanc : 7,35) |
| `--gm-blue-600` | `#006EB7` | 5,37 / 4,96 / 4,68 | Bleu du logo ; icônes actives, élément de navigation actif, bordure de focus |
| `--gm-blue-500` | `#1F80C9` | 4,21 (texte grand uniquement) | Graphiques, jauges |
| `--gm-blue-300` | `#7FB3E6` | 7,88 sur fond sombre | Texte d'action en mode sombre |
| `--gm-blue-100` | `#D6E6F7` | | Fond des lignes sélectionnées, fond des puces d'information |
| `--gm-blue-50` | `#EAF0F7` | | Fond de cartouche, lignes alternées, panneaux latéraux |

**Vert GIMAC (nominal, validé)**

| Jeton | Hex | Contraste | Rôle |
|---|---|---|---|
| `--gm-green-700` | `#0B6938` | 6,79 / 6,28 / 5,92 | Pictogramme et libellé « conforme », « en service », « acquittée », « clos » ; coche du logo |
| `--gm-green-600` | `#1E7F4B` | 5,01 | Graphiques |
| `--gm-green-300` | `#6FCF97` | 9,17 sur sombre | Statut nominal en mode sombre |
| `--gm-green-50` | `#E6F2EA` | | Fond d'une puce de statut nominal (avec texte 700) |

**Ocre et terre (attention, et accents de marque)**

| Jeton | Hex | Contraste | Rôle |
|---|---|---|---|
| `--gm-ochre-700` | `#8A5A14` | 5,91 / 5,47 / 5,15 | Pictogramme et libellé « à surveiller », « à régulariser », « en attente », « échéance proche » |
| `--gm-ochre-300` | `#F2C14E` | 10,38 sur sombre | Statut « à surveiller » en mode sombre |
| `--gm-ochre-50` | `#F8F0E0` | | Fond d'une puce « à surveiller » |
| `--gm-earth-700` | `#7A3E1D` | 8,29 | Accent de marque (latérite) : filet de la page de connexion, en-tête des rapports, motif de trame |
| `--gm-earth-100` | `#F1E4DA` | | Fond du motif de trame sur la page de connexion et les états vides |

**Rouge sobre (critique)**

| Jeton | Hex | Contraste | Rôle |
|---|---|---|---|
| `--gm-red-700` | `#9E2626` | 7,61 / 7,04 / 6,63 | Pictogramme et libellé « non conforme », « S1 », « seuil franchi », « rejeté », bouton destructeur (bordure et texte) |
| `--gm-red-300` | `#F08A8A` | 7,22 sur sombre | Statut critique en mode sombre |
| `--gm-red-50` | `#F9E5E5` | | Fond d'une puce critique et des messages d'erreur de champ |

Le rouge n'est jamais employé en aplat de fond sur plus de 48 px de hauteur (un bandeau critique est un fond `--gm-red-50` avec un filet gauche de 4 px `--gm-red-700`).

**Neutres**

| Jeton | Hex | Contraste sur blanc | Rôle |
|---|---|---|---|
| `--gm-grey-900` | `#14161A` | 18,1 | Texte principal, valeurs des indicateurs |
| `--gm-grey-700` | `#5C6268` | 6,17 | Texte secondaire, libellés, pictogrammes inactifs |
| `--gm-grey-600` | `#6B7280` | 4,83 | Texte d'aide, texte désactivé (avec pictogramme) |
| `--gm-grey-400` | `#B4B9C0` | | Bordures des champs, séparateurs forts |
| `--gm-grey-300` | `#D5D8DC` | | Bordures des tableaux, séparateurs |
| `--gm-grey-200` | `#E7E9EC` | | Lignes de grille des graphiques, pistes des jauges |
| `--gm-grey-100` | `#F5F6F8` | | Fond de page |
| `--gm-white` | `#FFFFFF` | | Surfaces (cartes, tableaux, champs, tiroirs) |

**Neutres du mode sombre**

| Jeton | Hex | Rôle |
|---|---|---|
| `--gm-dark-900` | `#181A1E` | Fond de page sombre |
| `--gm-dark-800` | `#22252B` | Surfaces sombres (cartes, tableaux) |
| `--gm-dark-700` | `#2C3037` | Surfaces surélevées (tiroirs, dialogues), cartouches |
| `--gm-dark-500` | `#4A4F58` | Bordures |
| `--gm-dark-200` | `#A7ADB5` | Texte secondaire (7,71) |
| `--gm-dark-50` | `#ECEEF1` | Texte principal (14,99) |

### 3.2 Couche sémantique (la seule consommée par les composants)

| Jeton sémantique | Mode clair | Mode sombre | Usage |
|---|---|---|---|
| `--gm-color-bg-page` | grey-100 | dark-900 | Fond de page |
| `--gm-color-surface` | white | dark-800 | Cartes, tableaux, champs |
| `--gm-color-surface-raised` | white | dark-700 | Tiroirs, dialogues, menus |
| `--gm-color-surface-sunken` | blue-50 | dark-700 | Cartouches, lignes alternées, panneaux |
| `--gm-color-surface-selected` | blue-100 | `#1E3A5C` | Ligne ou carte sélectionnée |
| `--gm-color-border` | grey-300 | dark-500 | Bordures des tableaux et cartes |
| `--gm-color-border-strong` | grey-400 | `#5A606A` | Bordures des champs |
| `--gm-color-text-primary` | grey-900 | dark-50 | Texte courant |
| `--gm-color-text-secondary` | grey-700 | dark-200 | Libellés, texte secondaire |
| `--gm-color-text-muted` | grey-600 | `#8A9099` | Aide, désactivé |
| `--gm-color-text-inverse` | white | grey-900 | Texte sur fond d'action |
| `--gm-color-action` | blue-700 | blue-300 | Boutons principaux, liens, navigation active |
| `--gm-color-action-hover` | blue-900 | `#A9CDF0` | Survol |
| `--gm-color-action-subtle` | blue-50 | `#1E3A5C` | Fond de survol des lignes, puces d'information |
| `--gm-color-focus` | blue-600 | blue-300 | Anneau de focus (2 px, décalage 2 px) |
| `--gm-color-status-ok-fg` | green-700 | green-300 | Libellé et pictogramme nominal |
| `--gm-color-status-ok-bg` | green-50 | `#123B26` | Fond de puce nominale |
| `--gm-color-status-warn-fg` | ochre-700 | ochre-300 | Libellé et pictogramme à surveiller |
| `--gm-color-status-warn-bg` | ochre-50 | `#3D2E10` | Fond de puce à surveiller |
| `--gm-color-status-crit-fg` | red-700 | red-300 | Libellé et pictogramme critique |
| `--gm-color-status-crit-bg` | red-50 | `#3F1A1A` | Fond de puce critique |
| `--gm-color-status-info-fg` | blue-700 | blue-300 | Libellé et pictogramme d'information |
| `--gm-color-status-neutral-fg` | grey-700 | dark-200 | Statut neutre (brouillon, archivé, clos) |
| `--gm-color-brand-accent` | earth-700 | `#C98B63` | Filet de marque, en-têtes de rapport |
| `--gm-color-brand-pattern` | earth-100 | dark-700 | Fond du motif de trame |

Le mode sombre redéfinit exactement ces 25 jetons et rien d'autre. Il est activé par `data-theme="dark"` sur la racine, choisi par l'utilisateur (préférence mémorisée dans son profil, IAM-12) ; la préférence système n'est qu'une valeur initiale.

### 3.3 Règles d'usage

1. Une seule couleur d'action : le bleu. Le vert, l'ocre et le rouge sont des couleurs de statut, jamais des couleurs d'action ni de décoration.
2. Un statut = un pictogramme + un libellé + une couleur. Retirer la couleur ne doit rien enlever au sens.
3. Les couleurs de statut ne s'appliquent qu'au pictogramme, au libellé et à un fond de puce (hauteur 24 px) ou à un filet gauche de 4 px ; jamais au fond d'une carte, d'une ligne entière ou d'un écran.
4. Les accents de marque (terre) ne dépassent pas 5 % de la surface d'un écran et n'apparaissent jamais dans les écrans de travail denses (listes, fiches, tableaux de bord).
5. Les graphiques utilisent la palette de données de la section 3.4, jamais les couleurs de statut pour distinguer des séries.
6. Tout texte respecte 4,5 : 1 au minimum, 3 : 1 pour le texte de 24 px et plus ; les composants d'interface (bordures de champ, anneaux de focus) respectent 3 : 1.

### 3.4 Palette de données (graphiques)

Séries catégorielles, dans l'ordre fixe : `#15569E` (bleu), `#0B6938` (vert), `#8A5A14` (ocre), `#5C6268` (gris), `#7A3E1D` (terre), `#1F80C9` (bleu clair), `#3E9B36` (vert clair), `#9E6A15` (ambre). Au-delà de huit séries : regroupement « Autres ». Échelle séquentielle : une seule teinte, bleu-50 vers bleu-900. Échelle divergente : bleu-700, gris-300 au centre, terre-700. Les statuts sur un graphique (seuil franchi) emploient les couleurs de statut avec pictogramme et libellé. Mode sombre : mêmes teintes éclaircies d'un cran (300 au lieu de 700).

---

## 4. Typographie

### 4.1 Familles

| Rôle | Famille | Justification | Repli |
|---|---|---|---|
| Titrage et navigation | **Ubuntu** (libre, OFL, graisses 400, 500, 700) | Humaniste et géométrique, ouverte, excellente à 16 à 32 px ; son nom et son dessin portent l'ancrage africain sans folklore | `system-ui`, `Segoe UI`, sans-serif |
| Texte d'interface et données | **Inter** (libre, OFL, graisses 400, 500, 600) | Conçue pour l'écran, chiffres tabulaires (`tnum`), excellente lisibilité à 12 à 14 px, glyphes distincts (I, l, 1 ; O, 0) | `system-ui`, sans-serif |
| Identifiants, empreintes, code, montants alignés | **JetBrains Mono** (libre, OFL, 400, 500) | Chasse fixe lisible, zéro barré, ligatures désactivées | `ui-monospace`, `Consolas`, monospace |
| Documents PDF générés (rapports, attestations) | Times New Roman 12 (charte documentaire du groupement) | Continuité avec les cahiers | Liberation Serif |

Les polices sont auto-hébergées (fichiers `woff2` dans `/assets/fonts/`), la plateforme n'ayant aucun accès à Internet ; `font-display: swap` ; sous-ensembles latin et latin étendu.

### 4.2 Échelle typographique (14 rôles)

| Rôle | Jeton | Famille | Taille | Graisse | Interligne | Usage |
|---|---|---|---|---|---|---|
| Titre d'écran | `--gm-type-display` | Ubuntu | 28 px (1,75 rem) | 700 | 36 px | Un par écran |
| Titre de section | `--gm-type-h1` | Ubuntu | 22 px | 700 | 30 px | Sections d'un écran |
| Titre de carte | `--gm-type-h2` | Ubuntu | 18 px | 500 | 26 px | Cartes, panneaux, dialogues |
| Sous-titre | `--gm-type-h3` | Ubuntu | 16 px | 500 | 24 px | Groupes de champs |
| Corps | `--gm-type-body` | Inter | 14 px | 400 | 22 px | Texte courant, cellules |
| Corps accentué | `--gm-type-body-strong` | Inter | 14 px | 600 | 22 px | Libellés de clés, entêtes de colonnes |
| Petit | `--gm-type-small` | Inter | 12 px | 400 | 18 px | Aide, horodatages, légendes |
| Petit accentué | `--gm-type-small-strong` | Inter | 12 px | 600 | 18 px | Puces de statut, badges |
| Micro | `--gm-type-micro` | Inter | 11 px | 500, capitales espacées 0,04 em | 16 px | Étiquettes de section dans les tiroirs, nom de la couche |
| Valeur d'indicateur | `--gm-type-stat` | Inter | 32 px | 600, `tnum` | 40 px | Cartes d'indicateurs |
| Valeur d'indicateur mur d'écrans | `--gm-type-stat-wall` | Inter | 56 px | 600, `tnum` | 64 px | Tableau mural uniquement |
| Identifiant | `--gm-type-mono` | JetBrains Mono | 13 px | 400 | 20 px | Références, identifiants de corrélation, empreintes |
| Montant et chiffre en colonne | `--gm-type-number` | Inter | 14 px | 400, `tnum`, aligné à droite | 22 px | Colonnes numériques |
| Bouton | `--gm-type-button` | Inter | 14 px | 600 | 20 px | Libellés de boutons |

### 4.3 Règles de rédaction des libellés

1. Français correct, accents sur les capitales, ponctuation française (espace insécable avant « : », « ; », « ? », « ! »), guillemets « ».
2. Un bouton porte un verbe à l'infinitif et son objet : « Clore l'opération », « Signer le quitus », « Déposer une pièce » ; jamais « OK », « Valider », « Soumettre » seuls.
3. Un titre d'écran nomme l'objet, pas l'action : « Institutions membres », « Liaison LIA-2026-017 ».
4. Les statuts sont des adjectifs ou participes au féminin ou masculin selon l'objet, en minuscules dans les puces : « conforme », « à régulariser », « en attente de tiers ».
5. Les horodatages s'affichent en heure locale (Afrique centrale, UTC+1) au format `28/09/2026 14:05` avec l'UTC au survol ; les durées en « 2 h 15 min ».
6. Les montants s'affichent avec séparateur de milliers (espace fine) et devise en code ISO après le nombre : `1 250 000 XAF`.
7. Aucun jargon technique dans les messages destinés aux correspondants des institutions ; le vocabulaire du chapitre 13 est imposé.
8. Un message d'erreur dit ce qui s'est passé, pourquoi, et quoi faire : « La pièce a été refusée : elle contient un numéro de carte non masqué. Masquez le numéro puis déposez à nouveau. »
9. Pas de point final sur les libellés courts, un point sur les phrases complètes.
10. Les abréviations sont explicitées au premier usage dans l'écran (« RSSI », « GED ») sauf celles du vocabulaire imposé.

---

## 5. Espacement, grille, formes, mouvement, icônes, motifs

### 5.1 Espacement

Pas de base 4 px, grille de composition 8 px. Jetons : `--gm-space-1` 4 px, `-2` 8 px, `-3` 12 px, `-4` 16 px, `-5` 24 px, `-6` 32 px, `-7` 48 px, `-8` 64 px. Règles : 16 px de marge interne dans les cartes, 24 px entre sections, 8 px entre champs d'une même ligne, 16 px entre lignes de formulaire, 12 px de marge dans les cellules de tableau (hauteur de ligne 44 px, 36 px en densité compacte).

### 5.2 Grille et points de rupture

- Application du département et console : largeur minimale 1280 px ; grille de 12 colonnes, gouttière 24 px ; contenu maximal 1600 px centré au-delà.
- Portail des participants : fluide dès 360 px ; 4 colonnes sous 640 px, 8 colonnes de 640 à 1024 px, 12 au-delà.
- Points de rupture : `--gm-bp-sm` 640, `--gm-bp-md` 1024, `--gm-bp-lg` 1280, `--gm-bp-xl` 1600, `--gm-bp-wall` 2560 (mur d'écrans).
- Densité : normale (par défaut) et compacte (tableaux de supervision, choix de l'utilisateur, mémorisé).

### 5.3 Formes et élévation

Rayon `--gm-radius` = 0 partout (angles droits) ; l'unique exception est l'avatar circulaire des utilisateurs. Aucune ombre portée : la hiérarchie des surfaces est rendue par les fonds (page, surface, surélevée) et des bordures de 1 px ; un tiroir ou un dialogue ouvert est séparé de la page par un voile `rgba(20, 22, 26, 0,45)`. Épaisseurs : 1 px pour les bordures, 2 px pour le focus et la bordure active des champs, 4 px pour les filets de statut des tableaux ; aucun filet vertical de surbrillance (D-66).

### 5.4 Mouvement

Durées : `--gm-motion-fast` 120 ms (survol, focus), `--gm-motion-base` 200 ms (ouverture d'un tiroir, d'un menu), `--gm-motion-slow` 320 ms (transition de page). Courbe : `cubic-bezier(0.2, 0, 0, 1)`. Aucune animation d'ambiance, aucune boucle ; les mises à jour poussées d'une cellule sont signalées par un fond `--gm-color-action-subtle` qui s'éteint en 1 200 ms. `prefers-reduced-motion` supprime toute transition sauf le changement d'opacité.

### 5.5 Iconographie

Un seul jeu : pictogrammes linéaires monochromes à trait de 1,5 px (jeu libre Lucide, auto-hébergé), tailles 16 px (dans le texte et les puces), 20 px (boutons, navigation), 24 px (en-têtes, états vides). Toujours accompagnés d'un libellé visible ou d'une infobulle et d'un nom accessible. Pictogrammes réservés (ne pas réaffecter) :

| Sens | Pictogramme |
|---|---|
| Nominal, conforme, clos | `circle-check` |
| À surveiller, échéance | `triangle-alert` |
| Critique, non conforme | `octagon-alert` |
| Information | `info` |
| Scellé, WORM, preuve | `lock` |
| Signature, quitus | `pen-line` |
| Horodatage | `clock` |
| Audit, registre | `scroll-text` |
| Institution | `landmark` |
| Liaison, parcours | `route` |
| Ticket | `ticket` |
| Alerte de monitoring | `bell-ring` |
| Astreinte, quart | `calendar-clock` |
| Suspens, rapprochement | `scale` |
| Tâche, Kanban | `kanban` |
| Réunion, décision | `gavel` |
| Cockpit, indicateur | `gauge` |
| Salon, message | `message-square-lock` |
| Conférence | `video` |
| Non-intrusion, lecture seule | `eye` |

### 5.6 Motif de trame (ancrage régional)

Le motif « trame » est un pavage géométrique de losanges et de chevrons sur la grille de 8 px (période 32 px), dessiné en SVG, trait de 1 px, couleur `--gm-color-brand-accent` à 8 % d'opacité sur `--gm-color-brand-pattern`. Il évoque les tissages d'Afrique centrale (bandes à motifs répétés) sans citer une ethnie ni une pièce précise. Emplois autorisés : bandeau gauche de la page de connexion (40 % de la largeur), fond des états vides (derrière le pictogramme, 240 × 160 px), en-tête des rapports PDF (bande de 12 mm), page d'accueil du portail (bandeau de bienvenue), fond du fil et de l'état vide de la messagerie à opacité réduite, bulles et saisie restant sur fond plein (D-66). Emplois interdits : derrière un texte courant, dans un tableau, dans un tiroir, sur le mur d'écrans.

Fichier : `assets/pattern/trame.svg` ; classe utilitaire `.gm-pattern` ; le motif est décoratif (`aria-hidden="true"`).

---

## 6. Enveloppe applicative

L'enveloppe (`AppShell`) est commune aux trois couches et n'est écrite qu'une fois. Elle est paramétrée par la couche (`layer: "des" | "portail" | "console"`).

### 6.1 Barre supérieure (`TopBar`, hauteur 56 px, fixe)

De gauche à droite : logo monochrome (28 px) et nom de la couche en micro-capitales (« APPLICATION DU DÉPARTEMENT », « PORTAIL DES PARTICIPANTS », « CONSOLE D'ADMINISTRATION ET D'AUDIT ») ; bouton de repli de la navigation ; fil d'Ariane ; zone centrale : recherche globale (`Ctrl+K`, application du département et console uniquement) ; à droite : horloge (heure locale, UTC au survol, mise à jour à la seconde), pastille de vacation en cours pour les opérateurs (« Quart de nuit, 22:00 à 06:00, chef de quart : … » en infobulle, sans nom dans la barre), indicateur de connexion au canal poussé (point vert ou gris avec libellé au survol), centre de notifications (cloche avec compteur), sélecteur de langue (FR/EN), menu utilisateur (avatar, nom, rôle, institution pour le portail, profil, thème clair ou sombre, densité, déconnexion).

Portail : pas de recherche globale ni de pastille de vacation ; nom de l'institution affiché à droite du nom de la couche. Console : pas de bandeau d'environnement (D-66). Le sélecteur de langue (FR, EN, ES) s'ouvre en menu surgissant ancré au bouton, sans déformer la page (D-66).

### 6.2 Navigation latérale (`SideNav`, 264 px, repliable à 64 px)

Entrées de premier niveau par module (voir la cartographie du chapitre 10), avec pictogramme et libellé ; entrée active marquée par une teinte de fond et un texte contrasté, blanc en mode sombre, sans filet vertical (D-66) ; barre latérale sombre en mode sombre ; logo seul en tête, sans drapeaux ni nom de couche (D-66) ; groupes repliables ; les entrées non autorisées par les permissions du jeton sont absentes du DOM, pas grisées. Sous 1280 px, la navigation devient un tiroir. Le bas de la navigation porte la version de la plateforme et la classification (« Usage interne, confidentiel »).

### 6.3 Zone de contenu

Titre d'écran (display) avec actions principales à droite (au plus une principale, deux secondaires, le reste dans un menu « Plus »), barre de filtres persistante quand l'écran est une liste, contenu, pied de page (référence de la plateforme, lien vers la documentation, identifiant de corrélation de la dernière requête).

### 6.4 Tiroir de détail (`Drawer`, 480 px, 640 px en large)

S'ouvre à droite sans quitter la liste ; en-tête (type, référence en chasse fixe, statut, bouton de fermeture, bouton « ouvrir en pleine page ») ; onglets (Détail, Chronologie, Pièces, Audit) ; pied avec les actions permises par le rôle. La chronologie affiche les transitions avec acteur, horodatage et identifiant de corrélation.

### 6.5 Centre de notifications (`NotificationCenter`)

Panneau de 400 px ouvert depuis la cloche ; onglets « Non lues », « Toutes » ; regroupement par niveau (critique, urgent, important, information) ; chaque notification : pictogramme, objet, horodatage relatif, lien vers l'objet, bouton d'acquittement pour les niveaux urgent et critique. Les niveaux urgent et critique s'affichent aussi en bandeau persistant (`Banner`) sous la barre supérieure, jusqu'à acquittement.

### 6.6 États globaux

Perte du canal poussé : indicateur gris et bandeau d'information « Mises à jour en temps réel interrompues, reconnexion en cours » ; session expirée : dialogue bloquant avec réauthentification ; maintenance : page dédiée ; erreurs 403, 404, 500 : pages dédiées avec identifiant de corrélation et retour.

---

## 7. Catalogue des composants

Chaque composant est décrit par son rôle, son anatomie, ses variantes, ses états, son contrat de propriétés (TypeScript), ses règles d'accessibilité et ses interdits. Tous les composants : n'emploient que des jetons sémantiques ; exposent `className` et `data-testid` ; sont documentés dans le catalogue vivant (Storybook) avec un exemple par variante et par état ; sont testés unitairement (rendu, accessibilité automatique, interactions clavier).

Les composants sont rangés en sept familles : actions, saisie, affichage, navigation, conteneurs et retours, données, domaine.

### 7.1 Actions

**Button**

- Variantes : `primary` (fond `--gm-color-action`, texte inverse), `secondary` (fond surface, bordure et texte action), `danger` (fond surface, bordure et texte critique ; confirmation obligatoire), `ghost` (texte action, sans bordure, pour les actions de faible importance dans les tableaux).
- Tailles : `md` 40 px de hauteur (par défaut), `sm` 32 px (dans les lignes de tableau), `lg` 48 px (portail sur mobile).
- États : repos, survol (fond `action-hover` pour primary, fond `action-subtle` pour les autres), focus (anneau 2 px), actif, désactivé (opacité 0,5, curseur `not-allowed`, jamais sans infobulle expliquant pourquoi), chargement (spinner 16 px à gauche, libellé conservé, bouton bloqué).
- Contrat :

```ts
type ButtonProps = {
  variant?: "primary" | "secondary" | "danger" | "ghost";
  size?: "sm" | "md" | "lg";
  icon?: IconName;            // pictogramme à gauche
  iconOnly?: boolean;         // exige aria-label
  loading?: boolean;
  disabled?: boolean;
  disabledReason?: string;    // affiché en infobulle si disabled
  type?: "button" | "submit";
  onClick?: (e: React.MouseEvent) => void;
  children: React.ReactNode;  // verbe + objet
};
```

- Accessibilité : élément `button` natif ; `aria-busy` en chargement ; libellé visible ; taille de cible 40 × 40 px au minimum.
- Interdits : plus d'un bouton `primary` par zone ; un `danger` sans `ConfirmDialog` ; un bouton qui ne dit que « OK ».

**IconButton** : bouton carré 40 px (32 px en `sm`) avec pictogramme seul et `aria-label` obligatoire ; infobulle au survol et au focus.

**SplitButton** : action principale + menu d'actions secondaires (flèche 32 px), navigation clavier `ArrowDown`.

**Link** : lien texte `--gm-color-action`, souligné au survol et au focus ; `external` ajoute le pictogramme et `rel="noopener"` ; les liens vers un objet (ticket, liaison) affichent la référence en chasse fixe.

### 7.2 Saisie

Tous les champs partagent l'anatomie `FieldWrapper` : étiquette permanente au-dessus (`body-strong`), marque « obligatoire » (astérisque et mention dans la légende du formulaire), aide optionnelle sous le champ (`small`, `text-muted`), message d'erreur sous le champ (`small`, `status-crit-fg`, pictogramme, `aria-describedby`), bordure 1 px `border-strong`, bordure 2 px `action` au focus, fond `surface`, hauteur 40 px, rayon 0.

```ts
type FieldProps<T> = {
  name: string; label: string; value: T; onChange: (v: T) => void;
  required?: boolean; help?: string; error?: string; disabled?: boolean; readOnly?: boolean;
  autoComplete?: string; placeholder?: never; // pas de texte indicatif : l'étiquette est permanente
};
```

| Composant | Rôle et règles |
|---|---|
| `TextField` | Texte court ; `maxLength` affiché en compteur quand il existe ; `inputMode` adapté ; variantes `mono` (identifiants) |
| `TextArea` | Texte long, hauteur auto de 3 à 12 lignes, compteur |
| `NumberField` | Nombres avec séparateur de milliers à l'affichage, saisie brute, `min`, `max`, `step`, unité en suffixe (XAF, %, min) |
| `DateField`, `DateTimeField`, `DateRangeField` | Saisie au clavier au format `JJ/MM/AAAA`, calendrier accessible en popover, fuseau affiché, `min`, `max`, raccourcis « aujourd'hui », « J-7 », « ce mois » |
| `Select` | Liste fermée jusqu'à 10 options, `listbox` natif accessible |
| `SearchSelect` | Liste avec recherche dès 10 options (institutions, collaborateurs), chargement asynchrone, création interdite, affichage code + libellé |
| `MultiSelect` | Sélection multiple par jetons (`Chip`) avec retrait au clavier |
| `Checkbox`, `CheckboxGroup` | Case carrée 18 px, libellé cliquable, état indéterminé pour les groupes |
| `RadioGroup` | Choix exclusif de 2 à 5 options ; au-delà, `Select` |
| `Switch` | Préférence binaire immédiate (canal, thème, densité) ; jamais pour une action à effet |
| `FileDropzone` | Dépôt par glisser ou clic ; types et taille maximale annoncés ; barre de progression ; résultat de la chaîne de contrôle affiché (`MaskingResult`) ; refus explicite |
| `SearchInput` | Recherche avec délai de 300 ms, effacement, raccourci `/` |
| `PinField` | Saisie à usage unique (code court de vérification de terminal, 6 cases) |
| `SignatureField` | Bloc de réauthentification FIDO2 (pictogramme `pen-line`, texte « Confirmez avec votre clé de sécurité »), utilisé par `SignDialog` |

Validation : côté client par schéma (zod) pour le format, jamais pour la règle métier ; les erreurs métier renvoyées par le service sont affichées sous le champ concerné (code d'erreur → champ) ou en bandeau de formulaire. Un formulaire long (plus de 8 champs) est découpé en étapes (`Stepper`) ; un formulaire ne perd jamais une saisie : brouillon local jusqu'à soumission.

### 7.3 Affichage

| Composant | Rôle et contrat |
|---|---|
| `StatCard` | Carte d'indicateur : libellé (`small`), valeur (`stat`, `tnum`), unité, tendance (flèche et variation en `small`), seuil (« seuil 95 % »), statut sur pictogramme et libellé uniquement ; `onClick` ouvre le détail. `{ label, value, unit?, trend?: {delta, direction}, threshold?, status?: "ok"|"warn"|"crit"|"neutral", loading }` |
| `StatusBadge` | Puce 24 px : pictogramme 16 px + libellé `small-strong`, fond de statut ; `{ status, label, icon? }` ; les libellés viennent d'un dictionnaire par objet (institution, liaison, ticket, suspens, alerte) |
| `SeverityBadge` | S1 à S4 : puce à bordure, S1 critique, S2 à surveiller, S3 et S4 neutres |
| `Tag` | Étiquette neutre (catégorie, type de projet) ; retirable dans les filtres |
| `Avatar` | Cercle (seule forme arrondie, §5.3), cinq tailles de 20 à 96 px ; photo de profil choisie par le titulaire (recadrée en carré, 256 px, sans métadonnées, docs/lot1/mon-compte.md), à défaut initiales sur fond `action-subtle` (D-66) |
| `KeyValueList` | Liste clé-valeur en deux colonnes (clé `text-secondary`, valeur `text-primary`), copie au clic pour les identifiants |
| `Timeline` | Chronologie verticale : point, horodatage, acteur (rôle et nom), événement, identifiant de corrélation en chasse fixe, pièce liée |
| `ProgressRing` | Anneau de couverture (certification, mise en service) avec pourcentage au centre, teinte action, statut sur le libellé |
| `CoverageBar` | Barre par service (inclus, en essai, certifié, en service) avec légende textuelle |
| `EmptyState` | Pictogramme 24 px sur motif de trame, titre, explication, action principale ; texte différent par cause (aucune donnée, filtre sans résultat, droits insuffisants) |
| `Skeleton` | Blocs gris animés en opacité pendant le chargement (jamais de spinner plein écran) |
| `CorrelationIdChip` | Identifiant de corrélation en chasse fixe avec copie et lien vers le registre d'audit (console) |
| `MaskingResult` | Résultat du contrôle de masquage d'une pièce : accepté, masqué (n séquences), refusé, avec explication |
| `Hash` | Empreinte SHA-256 tronquée (8 premiers et 8 derniers caractères) avec copie et vérification |
| `Money` | Montant formaté `1 250 000 XAF`, `tnum`, aligné à droite |
| `DateTime` | Horodatage local avec UTC en infobulle, variante relative (« il y a 4 min ») rafraîchie |

### 7.4 Navigation

| Composant | Rôle |
|---|---|
| `Tabs` | Onglets d'une fiche ; l'onglet actif est souligné de 2 px action ; navigation clavier flèches ; l'onglet est dans l'URL (`?onglet=pieces`) |
| `Stepper` | Assistant en étapes (dossier de mise en service, dépôt de pièce, enregistrement d'institution) : numéro, libellé, état (à faire, en cours, fait, en erreur), navigation retour permise, résumé avant confirmation |
| `Pagination` | Pagination côté serveur : taille de page (25, 50, 100), total, aller à la page ; jamais de défilement infini dans les listes de travail |
| `Breadcrumb` | Fil d'Ariane à 3 niveaux au plus, dernier élément non cliquable |
| `SideNav`, `TopBar` | Voir chapitre 6 |
| `CommandPalette` | Recherche globale `Ctrl+K` : objets (institution, liaison, ticket, suspens, pièce), écrans, actions ; résultats groupés |

### 7.5 Conteneurs et retours

| Composant | Rôle et règles |
|---|---|
| `Card` | Surface avec bordure 1 px, en-tête (titre `h2`, actions), corps, pied optionnel ; pas d'ombre |
| `Panel` | Cartouche sur fond `surface-sunken` pour les résumés et les filtres |
| `Drawer` | Tiroir latéral (voir 6.4) ; piège de focus ; fermeture `Échap` ; l'URL porte l'objet ouvert (`?detail=TCK-000318`) |
| `Dialog` | Fenêtre modale centrée 560 px (720 en large) ; titre `h2` ; piège de focus ; retour au déclencheur à la fermeture |
| `ConfirmDialog` | Confirmation à deux étapes pour toute action à effet : rappel de l'objet, des conséquences, champ de motif si requis, case « je confirme » pour les actions irréversibles, bouton principal libellé avec le verbe |
| `SignDialog` | Confirmation avec réauthentification FIDO2 (quitus, clôture, scellement, purge, transition de statut) : rappel de ce qui sera signé (empreinte affichée), `SignatureField`, résultat |
| `Banner` | Bandeau pleine largeur sous la barre : information (fond `action-subtle`), à surveiller, critique (fond de statut, filet gauche 4 px), persistant jusqu'à acquittement pour urgent et critique |
| `Toast` | Retour d'action non bloquant (« Opération close », « Pièce déposée »), 6 s, empilé en bas à droite, jamais pour une erreur bloquante |
| `InlineAlert` | Message contextuel dans une carte ou un formulaire |
| `Tooltip` | Infobulle au survol et au focus, 300 ms, texte court, jamais de contenu essentiel |
| `Popover` | Contenu riche ancré (calendrier, filtres avancés) |
| `Menu` | Menu d'actions (`Plus`), navigation clavier, séparateurs, actions dangereuses en bas avec pictogramme |

### 7.6 Données

**DataTable** (le composant central des listes)

- Anatomie : barre de filtres persistante (`FilterBar`), barre d'outils (sélection, actions groupées, densité, colonnes, export contrôlé), en-tête (fond `--gm-color-action`, texte inverse, tri par colonne, indicateur), lignes (hauteur 44 ou 36, alternance `surface-sunken`), colonne d'actions à droite (menu ou boutons `ghost`), ligne dépliable, pagination serveur, ligne de total optionnelle.
- Types de colonnes : `text`, `mono`, `number`, `money`, `date`, `datetime`, `status`, `severity`, `progress`, `tags`, `link`, `actions`, `custom`.
- Colonnes d'horodatage : locale et UTC au survol ; colonnes numériques alignées à droite en `tnum`.
- Sélection : case en première colonne, sélection de page, compteur.
- États : chargement (squelette de 10 lignes), vide (`EmptyState`), erreur (`InlineAlert` avec réessai), nominal.
- Filtres : dans l'URL ; persistants par écran et par utilisateur (mémorisés) ; puces de filtres actifs retirables.
- Contrat :

```ts
type Column<Row> = {
  key: keyof Row | string; header: string; type: ColumnType; width?: number;
  sortable?: boolean; align?: "left" | "right" | "center"; render?: (row: Row) => React.ReactNode;
  hiddenByDefault?: boolean;
};
type DataTableProps<Row> = {
  columns: Column<Row>[]; rows: Row[]; total: number; page: number; pageSize: number;
  sort?: { key: string; dir: "asc" | "desc" }; onChange: (q: ListQuery) => void;
  rowKey: (r: Row) => string; onRowOpen?: (r: Row) => void; selectable?: boolean;
  rowActions?: (r: Row) => Action[]; bulkActions?: Action[]; density?: "normal" | "compact";
  state: "loading" | "empty" | "error" | "ok"; error?: ApiError; emptyText?: string;
  expandable?: (r: Row) => React.ReactNode; liveUpdates?: boolean; // surlignage des lignes mises à jour par le canal poussé
};
```

**Autres composants de données**

| Composant | Rôle |
|---|---|
| `FilterBar` | Filtres typés (texte, sélection, plage de dates, statut, institution, service) ; recherche ; « réinitialiser » ; sauvegarde de filtres favoris |
| `TreeView` | Arbre (référentiel d'opérations en tronc commun et branches, rôles et permissions) |
| `DagView` | Graphe orienté acyclique des opérations d'une liaison : nœuds par phase, états (éligible, verrouillée, close, hors périmètre), chemin critique, zoom, vue liste équivalente |
| `KanbanBoard` | Colonnes avec limites d'en-cours affichées (« 4/5 »), cartes glissables et déplaçables au clavier, refus au-delà de la limite avec demande de dérogation |
| `GanttView` | Jalons et chemin critique d'un projet ; vue tableau équivalente |
| `CalendarGrid` | Grille d'astreinte hebdomadaire (jours × plages), cases par opérateur, plages non couvertes signalées, échange d'astreinte |
| `WallBoard` | Tableau mural des liaisons supervisées : tuiles d'état 2560 px, police `stat-wall`, rafraîchissement poussé, aucune interaction |
| `ChartCard` | Carte de graphique (courbe, barres, jauge, carte de chaleur) avec légende toujours présente pour deux séries et plus, infobulle, vue tableau, export |
| `RiskMatrix` | Matrice 5 × 5 (probabilité × impact) avec risques positionnés, teinte sur le libellé de zone |
| `AuditProofViewer` | Visualisation d'une preuve d'inclusion : feuille, chemin d'audit, racine, jeton d'horodatage, résultat de vérification |
| `StateMachineBar` | Barre d'états d'un ticket, d'un suspens, d'un dossier ou d'une institution : états parcourus, état courant, transitions permises |
| `DiffView` | Comparaison de deux versions (référentiel d'opérations, règle, rôle) |

### 7.7 Composants de domaine

| Composant | Composé de | Écran |
|---|---|---|
| `InstitutionHeader` | Nom, sigle, catégorie (`Tag`), statut (`StatusBadge`), conformité, pays (drapeau 16 px), actions | Fiche institution, portail |
| `LiaisonProgress` | `ProgressRing` × 2 (certification, mise en service), `CoverageBar` par service, phase courante | Liaison, portail |
| `OperationNode` | Nœud du `DagView` : code, libellé, à qui elle incombe, état, quatre éléments (quatre pictogrammes : responsable, preuve, horodatage, validation) | Liaison |
| `FourElementsChecklist` | Les quatre éléments avec leur état et l'action manquante | Clôture d'opération |
| `QuitusCircuit` | Les quatre quitus dans l'ordre imposé, signataire (rôle), état, horodatage, motif | Dossier de mise en service |
| `TicketCard`, `TicketStateBar` | Carte Kanban ou ligne de ticket ; automate à sept états | Tickets, incidents |
| `SeverityMatrix` | Matrice impact × urgence 5 × 5 pour la qualification | Incident |
| `SlaClock` | Délai imputable, engagement, gel en attente de tiers, dépassement | Incident |
| `AlertRow` | Alerte : liaison, gravité, horodatage, délai d'acquittement restant, acquitter, ouvrir un ticket | Monitoring |
| `ShiftHandoverPanel` | Rapport de quart : contenu, signatures (sortant, entrant, visa), horodatage, dépôt | Relève de quart |
| `LogbookEntryList` | Main courante chaînée (quart ou crise) avec saisie en bas | Vacation, cellule de crise |
| `SuspensCard` | Suspens : typologie, identifiants pivots (mono), montant, ancienneté, responsable, état, attestation | Rapprochement, portail |
| `ReconciliationSummary` | Journée, institution, états importés, écarts par typologie | Rapprochement |
| `DecisionToTaskRow` | Décision de réunion, tâche engendrée, responsable, échéance, état | Réunions |
| `PdcaCard` | Fiche d'action : phase courante (planifier, réaliser, vérifier, agir), gardes | Projets, cockpit |
| `CrisisRoomHeader` | Cellule de crise : niveau, quorum atteint ou non, membres présents, salon et conférence | Salle de crise |
| `ChatPanel` | Salon chiffré : liste des messages, accusés, pièces jointes chiffrées, mentions, indicateur de terminal vérifié | Salons |
| `ConferenceControls` | Micro, caméra, partage d'écran, consentement à l'enregistrement, participants | Conférence |
| `DeviceVerificationCard` | Terminal : identifiant, clés, état de vérification, code court | Profil, console |
| `PolicyMatrix` | Matrice rôles × permissions par domaine | Console |
| `RootPublicationCard` | Racine de période : racine, jeton d'horodatage, référence de publication, vérifier | Console d'audit |

---

## 8. Principe de composition et CRUD universel

### 8.1 Architecture du code client

```
src/
  app/                 // démarrage, routeur, fournisseurs (thème, i18n, requêtes, session)
  shell/               // AppShell, TopBar, SideNav, NotificationCenter, pages d'erreur
  design-system/       // jetons (tokens.css), primitives et composants du catalogue (chapitre 7)
  lib/                 // client HTTP (passerelle), erreurs normalisées, formatage, permissions, canal poussé
  resources/           // descripteurs de ressource (un fichier par agrégat métier)
  features/            // un dossier par module (institutions, liaisons, tickets, ...) : pages composées
  i18n/                // catalogues fr et en
  test/                // utilitaires de test, fabriques de données fictives
```

Règles : `design-system/` ne dépend de rien d'autre que `lib/`; `features/` ne contient que des compositions et des hooks de données ; aucune logique métier (une règle est appliquée par le service, l'interface l'affiche) ; aucun appel réseau hors `lib/http` ; aucune couleur ou taille en dur ; aucun `any`.

### 8.2 Le descripteur de ressource

Chaque agrégat métier exposé à l'interface (institution, convention, pièce légale, liaison, opération, ticket, alerte, suspens, tâche, réunion, indicateur, salon, rôle, route, ...) est décrit une seule fois par un descripteur typé. Les quatre composants universels du CRUD lisent ce descripteur et ne contiennent aucune connaissance de la ressource.

```ts
type FieldKind = "text" | "mono" | "textarea" | "number" | "money" | "date" | "datetime" | "daterange"
  | "select" | "searchSelect" | "multiSelect" | "checkbox" | "radio" | "switch" | "file" | "status" | "custom";

type FieldDescriptor<T> = {
  name: keyof T & string; label: string; kind: FieldKind;
  required?: boolean; help?: string; options?: () => Promise<Option[]> | Option[];
  validate?: ZodTypeAny;              // format uniquement
  inList?: boolean; inDetail?: boolean; inForm?: boolean | "create" | "edit";
  width?: number; sortable?: boolean; filterable?: boolean; readOnly?: boolean;
  permission?: string;                // permission requise pour voir le champ
  render?: (value: unknown, row: T) => React.ReactNode;
};

type ResourceAction<T> = {
  key: string; label: string; icon?: IconName; variant?: "primary" | "secondary" | "danger";
  scope: "row" | "bulk" | "page"; permission: string;
  confirm?: "none" | "confirm" | "sign";   // ConfirmDialog ou SignDialog
  reason?: boolean;                        // motif obligatoire
  isAllowed?: (row: T) => boolean;         // reflète les transitions permises renvoyées par le service
  run: (ctx: ActionContext<T>) => Promise<void>;
};

type ResourceDescriptor<T> = {
  key: string;                    // "institution"
  label: { singular: string; plural: string; gender: "m" | "f" };
  idField: keyof T & string; titleField: keyof T & string; statusField?: keyof T & string;
  endpoints: { list: string; get: string; create?: string; update?: string; delete?: string; };
  fields: FieldDescriptor<T>[];
  actions: ResourceAction<T>[];
  tabs?: { key: string; label: string; render: (row: T) => React.ReactNode }[]; // onglets du détail
  timeline?: (row: T) => string;  // endpoint de chronologie
  statusDictionary?: Record<string, { label: string; status: "ok" | "warn" | "crit" | "neutral" | "info" }>;
  permissions: { list: string; read: string; create?: string; update?: string; delete?: string };
  liveTopic?: string;             // sujet du canal poussé rafraîchissant les listes
};
```

### 8.3 Les quatre composants universels

| Composant | Rôle | Compose | Reçoit |
|---|---|---|---|
| `ResourceList<T>` | Liste paginée, filtrée, triée, avec actions de ligne et groupées, tiroir de détail, rafraîchissement poussé | `FilterBar`, `DataTable`, `Drawer`, `EmptyState` | `descriptor`, `defaultQuery?`, `embedded?` (dans un onglet), `fixedFilters?` (par exemple `institutionId`) |
| `ResourceDetail<T>` | Fiche à onglets : en-tête (titre, statut, actions), champs `inDetail` en `KeyValueList`, onglets du descripteur, chronologie, audit | `Tabs`, `KeyValueList`, `Timeline`, `StateMachineBar` | `descriptor`, `id`, `mode: "page" | "drawer"` |
| `ResourceForm<T>` | Création ou modification : champs `inForm`, validation de format, erreurs métier mappées, brouillon local, étapes si plus de 8 champs, confirmation ou signature selon l'action | `FieldWrapper` et champs, `Stepper`, `ConfirmDialog`, `SignDialog` | `descriptor`, `mode: "create" | "edit"`, `id?`, `initial?`, `onDone` |
| `ResourceDelete<T>` | Suppression ou archivage à confirmation (jamais de suppression physique d'un objet réglementaire : le descripteur expose `archive` plutôt que `delete` quand la rétention l'exige) | `ConfirmDialog` ou `SignDialog` | `descriptor`, `id`, `onDone` |

Ces quatre composants sont appelés avec le descripteur en paramètre et rien d'autre. Une page « Institutions membres » se réduit à :

```tsx
export default function InstitutionsPage() {
  return <ResourceList descriptor={institutionDescriptor} />;
}
```

et la fiche à :

```tsx
export default function InstitutionPage() {
  const { id } = useParams();
  return <ResourceDetail descriptor={institutionDescriptor} id={id!} mode="page" />;
}
```

Les hooks de données `useResourceList(descriptor, query)`, `useResource(descriptor, id)`, `useResourceMutation(descriptor, action)` encapsulent le client HTTP, le cache, l'invalidation par événement du canal poussé (`liveTopic`) et la gestion des erreurs normalisées. Aucun composant n'appelle `fetch`.

### 8.4 Exemple de descripteur : institution membre

```ts
export const institutionDescriptor: ResourceDescriptor<Institution> = {
  key: "institution",
  label: { singular: "institution membre", plural: "institutions membres", gender: "f" },
  idField: "id", titleField: "denomination", statusField: "statut",
  endpoints: { list: "/bff/des/institutions", get: "/bff/des/institutions/{id}", create: "/bff/des/institutions", update: "/bff/des/institutions/{id}" },
  fields: [
    { name: "codeParticipant", label: "Code participant", kind: "mono", required: true, inList: true, inDetail: true, inForm: "create", sortable: true },
    { name: "denomination", label: "Dénomination", kind: "text", required: true, inList: true, inDetail: true, inForm: true, sortable: true, filterable: true },
    { name: "sigle", label: "Sigle", kind: "text", inList: true, inDetail: true, inForm: true },
    { name: "categorie", label: "Catégorie", kind: "select", required: true, options: categorieOptions, inList: true, inDetail: true, inForm: true, filterable: true },
    { name: "modeAdhesion", label: "Mode d'adhésion", kind: "radio", required: true, options: [{ value: "DIRECT", label: "Direct" }, { value: "INDIRECT", label: "Indirect" }], inDetail: true, inForm: true },
    { name: "pays", label: "Pays", kind: "select", required: true, options: paysCemac, inList: true, inDetail: true, inForm: true, filterable: true },
    { name: "banqueReglement", label: "Banque de règlement", kind: "searchSelect", options: rechercheInstitutions, inDetail: true, inForm: true },
    { name: "banqueMarraine", label: "Banque marraine", kind: "searchSelect", options: rechercheInstitutions, inDetail: true, inForm: true, help: "Obligatoire en adhésion indirecte" },
    { name: "statut", label: "Statut", kind: "status", inList: true, inDetail: true, filterable: true },
    { name: "etatConformite", label: "Conformité documentaire", kind: "status", inList: true, inDetail: true, filterable: true },
    { name: "dateAdhesion", label: "Date d'adhésion", kind: "date", inDetail: true, inForm: "edit", readOnly: true },
  ],
  actions: [
    { key: "create", label: "Enregistrer une institution", icon: "landmark", variant: "primary", scope: "page", permission: "PAR_INSTITUTION_CREATE", confirm: "none", run: openCreateForm },
    { key: "transition", label: "Demander un changement de statut", icon: "route", scope: "row", permission: "PAR_STATUS_REQUEST", confirm: "confirm", reason: true, isAllowed: r => r.transitionsPermises.length > 0, run: openTransitionDialog },
    { key: "approve", label: "Approuver la transition", icon: "pen-line", scope: "row", permission: "PAR_STATUS_APPROVE", confirm: "sign", isAllowed: r => r.transitionEnAttente && !r.transitionEnAttente.demandeeParMoi, run: approveTransition },
    { key: "signal", label: "Signaler une non-conformité", icon: "octagon-alert", variant: "danger", scope: "row", permission: "PAR_SIGNAL", confirm: "confirm", reason: true, run: signalNonCompliance },
    { key: "export", label: "Exporter (contrôlé)", icon: "download", scope: "page", permission: "PAR_EXPORT", confirm: "confirm", run: exportControlled },
  ],
  tabs: [
    { key: "conventions", label: "Conventions", render: r => <ResourceList descriptor={conventionDescriptor} embedded fixedFilters={{ institutionId: r.id }} /> },
    { key: "services", label: "Services souscrits", render: r => <ResourceList descriptor={serviceSouscritDescriptor} embedded fixedFilters={{ institutionId: r.id }} /> },
    { key: "correspondants", label: "Correspondants", render: r => <ResourceList descriptor={correspondantDescriptor} embedded fixedFilters={{ institutionId: r.id }} /> },
    { key: "pieces", label: "Pièces légales et échéances", render: r => <ResourceList descriptor={pieceLegaleDescriptor} embedded fixedFilters={{ institutionId: r.id }} /> },
    { key: "escalade", label: "Annuaire d'escalade", render: r => <ResourceList descriptor={contactEscaladeDescriptor} embedded fixedFilters={{ institutionId: r.id }} /> },
    { key: "interactions", label: "Interactions", render: r => <ResourceList descriptor={interactionDescriptor} embedded fixedFilters={{ institutionId: r.id }} /> },
  ],
  timeline: r => `/bff/des/institutions/${r.id}/chronologie`,
  statusDictionary: {
    CANDIDAT: { label: "candidat", status: "neutral" }, EN_HOMOLOGATION: { label: "en homologation", status: "info" },
    ACTIF: { label: "actif", status: "ok" }, A_REGULARISER: { label: "à régulariser", status: "warn" },
    SUSPENDU: { label: "suspendu", status: "crit" }, RETIRE: { label: "retiré", status: "neutral" },
  },
  permissions: { list: "PAR_INSTITUTION_LIST", read: "PAR_INSTITUTION_READ", create: "PAR_INSTITUTION_CREATE", update: "PAR_INSTITUTION_UPDATE" },
  liveTopic: "participant",
};
```

### 8.5 Règles anti-spaghetti

1. Une page ne contient pas de JSX de formulaire ni de tableau : elle appelle un composant universel avec un descripteur, ou compose des composants de domaine.
2. Un descripteur ne contient pas de composant spécifique : les rendus particuliers passent par `render` en pointant vers un composant du catalogue.
3. Les transitions permises, les permissions effectives et les règles métier viennent du service (`transitionsPermises`, `actionsPermises` dans la réponse) ; l'interface ne les recalcule jamais.
4. Toute action à effet passe par `confirm: "confirm"` ou `"sign"` ; le composant universel refuse un descripteur dont une action `danger` n'a pas de confirmation.
5. Les composants de domaine (chapitre 7.7) sont les seuls à connaître le vocabulaire métier ; ils sont composés de primitives du catalogue.
6. Une fonctionnalité nouvelle commence par un descripteur et, si nécessaire, un composant de domaine ; jamais par une page.
7. Un composant est amélioré à un seul endroit ; l'amélioration profite à tous les écrans, et son catalogue vivant est mis à jour dans la même modification.

### 8.6 Client HTTP, session et canal poussé

- Toutes les requêtes passent par `lib/http` vers la passerelle sous `/bff/{couche}/...` ; le jeton de session est porté par un témoin `HttpOnly`, `Secure`, `SameSite=Strict` posé par la passerelle ; le client n'a jamais accès à un jeton ; un `401` déclenche la réauthentification ; un `403` affiche la page dédiée ; toute erreur est normalisée en `ApiError { code, message, details?, correlationId }` et l'identifiant de corrélation est affiché.
- Le canal poussé (`lib/live`) s'abonne aux sujets des écrans ouverts, reprend depuis le dernier événement à la reconnexion et invalide les caches concernés ; les lignes mises à jour sont surlignées 1 200 ms.
- Aucune donnée n'est conservée dans le stockage local hormis les préférences d'affichage (thème, densité, filtres favoris) ; les brouillons de formulaire sont en mémoire de session et purgés à la déconnexion.

---

## 9. Gabarits d'écran

La liste des gabarits est close : un écran emploie l'un des dix gabarits ci-dessous. Tout gabarit impose les quatre états (chargement, vide, erreur, nominal) et l'enveloppe du chapitre 6.

| Gabarit | Structure | Composants imposés | Exemples |
|---|---|---|---|
| G1 Tableau de bord | Titre, rangée de `StatCard` (4 à 6), grille de cartes (listes courtes, graphiques), bandeaux | `StatCard`, `ChartCard`, `DataTable` compact, `Banner` | Tableau de bord du DES, cockpits, espace de l'institution |
| G2 Liste et tiroir | Titre et action principale, `FilterBar`, `DataTable`, `Drawer` de détail | `ResourceList` | Institutions, tickets, alertes, suspens, pièces, événements d'audit |
| G3 Fiche à onglets | En-tête d'objet (`*Header`), `Tabs`, contenu d'onglet, chronologie | `ResourceDetail` | Institution, liaison, ticket, projet, salon |
| G4 Assistant en étapes | `Stepper`, une étape par écran, résumé, confirmation ou signature | `ResourceForm` en mode étapes | Enregistrement d'institution, dossier de mise en service, dépôt de pièce, import d'état |
| G5 Console de paramétrage | Deux volets : arbre ou liste à gauche, détail éditable à droite, versions, `DiffView` | `TreeView`, `ResourceForm`, `DiffView` | Rôles et permissions, routes et quotas, référentiels d'opérations, matrices, seuils, indicateurs |
| G6 Plan de travail | Surface interactive plein écran avec barre d'outils | `KanbanBoard`, `DagView`, `GanttView`, `CalendarGrid`, `RiskMatrix` | Kanban, liaison (graphe), projets, grille d'astreinte, risques |
| G7 Mur d'écrans | Tuiles plein écran sans navigation, police `stat-wall` | `WallBoard` | Tableau mural des liaisons |
| G8 Salle | Trois volets : contexte (en-tête et membres), flux (main courante ou messages), actions | `CrisisRoomHeader`, `LogbookEntryList`, `ChatPanel`, `ConferenceControls` | Salle de crise, salons, vacation |
| G9 Vérification | Formulaire de requête à gauche, résultat probant à droite (empreintes, jetons, verdict) | `AuditProofViewer`, `RootPublicationCard`, `Hash` | Vérification d'intégrité, contrôle d'une pièce |
| G10 Page simple | Une colonne centrée 640 px sur motif de trame | `Card`, `EmptyState` | Connexion, erreurs, maintenance, profil |

---

## 10. Cartographie exhaustive des écrans

Chaque écran est identifié (DES-xx, POR-xx, CON-xx, TRA-xx), rattaché à un module, un gabarit, un microservice de données, des permissions et des récits du carnet (CP-GIMAC-01 v2.0). Les routes sont indicatives. Toute page est composée selon le chapitre 8 : les descripteurs de ressource sont nommés.

### 10.1 Application web du département (`/des`)

| Réf. | Écran | Route | Gabarit | Données (BFF `/bff/des/...`) et descripteurs | Rôles | Récits |
|---|---|---|---|---|---|---|
| DES-01 | Tableau de bord du DES | `/des` | G1 | `indicateurs/mon-service`, `notifications/urgentes`, `tickets?sev=S1,S2`, `echeances/semaine`, `taches/miennes`, `liaisons/en-attente` | tous | IHM-16, REP-10, NOT-01 |
| DES-02 | Mon profil et mes disponibilités | `/des/profil` | G3 | `profil`, `affectations`, `delegations`, `disponibilites` (descripteur `disponibilite`), `preferences`, `terminaux` | tous | IAM-12, IAM-06, IAM-08, IAM-09, COL-06 |
| DES-03 | Bibliothèque des pièces | `/des/documents` | G2 | descripteur `document` ; dépôt en G4 ; `MaskingResult` | selon permissions | DOC-01, DOC-04, DOC-05, DOC-06 |
| DES-04 | Centre de notifications (pleine page) | `/des/notifications` | G2 | descripteur `notification` ; préférences | tous | NOT-01, NOT-04, NOT-15 |
| DES-05 | Registre des institutions | `/des/institutions` | G2 | descripteur `institution` | DES, conformité | PAR-01, PAR-17 |
| DES-06 | Fiche d'une institution | `/des/institutions/{id}` | G3 | `institution` et onglets (conventions, services, correspondants, pièces et échéances, escalade, interactions), `StateMachineBar` | DES, conformité | PAR-02 à PAR-14 |
| DES-07 | Enregistrer une institution | `/des/institutions/nouvelle` | G4 | `ResourceForm(institution, create)` en 3 étapes (identité, adhésion, correspondants) | chef du DES | PAR-01, PAR-07 |
| DES-08 | Liaisons et parcours | `/des/liaisons` | G2 | descripteur `liaison` ; tableau de bord des liaisons en onglet | homologation | HOM-04, HOM-20 |
| DES-09 | Vue d'une liaison | `/des/liaisons/{id}` | G3 + G6 | onglets : graphe (`DagView` + `OperationNode`), périmètre (`CoverageBar`), opérations (descripteur `operation`), résultats du simulateur, preuves, chronologie | homologation, participants (lecture) | HOM-05 à HOM-19 |
| DES-10 | Clore une opération | `/des/liaisons/{id}/operations/{op}/cloture` | G4 | `FourElementsChecklist`, `SignDialog` | homologation, validateur | HOM-09, HOM-10 |
| DES-11 | Dossier de mise en service | `/des/liaisons/{id}/mise-en-service` | G3 | `QuitusCircuit`, complétude (6 conditions), fiche de changement lue, procès-verbal, attestation | chef du DES, RSSI, conformité, DG | CER-01 à CER-12 |
| DES-12 | Référentiels d'opérations | `/des/referentiels/operations` | G5 | descripteur `referentielOperations`, `TreeView`, `DiffView`, publication | homologation | HOM-01 à HOM-03 |
| DES-13 | File des tickets | `/des/tickets` | G2 | descripteur `ticket` ; tiroir avec `TicketStateBar`, `SlaClock` | tous | INC-01 à INC-06, INC-09 |
| DES-14 | Ticket et incident | `/des/tickets/{id}` | G3 | onglets : détail, qualification (`SeverityMatrix`), chronologie, pièces, tâches liées, rapport | opérateurs, chef de quart | INC-07 à INC-17 |
| DES-15 | Tableau de bord des engagements | `/des/incidents/engagements` | G1 | indicateurs et listes | chef du DES | INC-12, INC-23 |
| DES-16 | Liaisons supervisées | `/des/monitoring` | G2 | descripteur `liaisonSupervisee`, `AlertRow` | opérateurs | MON-01 à MON-08, MON-19 |
| DES-17 | Tableau mural | `/des/monitoring/mur` | G7 | `WallBoard` | salle de supervision | MON-16 |
| DES-18 | Grille d'astreinte | `/des/astreintes` | G6 | `CalendarGrid`, génération, révision, échanges | chef du DES, opérateurs | MON-09 à MON-12 |
| DES-19 | Vacation et relève de quart | `/des/vacations/courante` | G8 | `LogbookEntryList`, `ShiftHandoverPanel`, `SignDialog` | opérateurs, chef de quart | MON-13 à MON-15 |
| DES-20 | Salle de crise | `/des/crises/{id}` | G8 | `CrisisRoomHeader`, main courante de crise, décisions vers tâches, `ChatPanel`, `ConferenceControls`, clôture | cellule de crise | INC-18 à INC-21, COL-09, COL-13 |
| DES-21 | Journées de rapprochement | `/des/rapprochement` | G2 | descripteur `rapprochement`, `ReconciliationSummary`, import en G4 | opérateur de rapprochement | RAP-01 à RAP-06, RAP-12 |
| DES-22 | Suspens | `/des/rapprochement/suspens` | G2 | descripteur `suspens`, `SuspensCard`, conciliation, attestation | opérateur, chef du DES | RAP-07 à RAP-11 |
| DES-23 | Tableaux Kanban | `/des/taches` | G6 | `KanbanBoard`, descripteur `tache` | tous | PIL-01 à PIL-08 |
| DES-24 | Portefeuille de projets | `/des/projets` | G2 + G6 | descripteur `projet`, `GanttView`, `RiskMatrix`, `PdcaCard` | chef du DES, RSSI | PIL-12 à PIL-17 |
| DES-25 | Réunions et décisions | `/des/reunions` | G2 + G3 | descripteur `reunion`, `DecisionToTaskRow` | secrétaire, chef du DES | PIL-09 à PIL-11 |
| DES-26 | Cockpit du DES | `/des/cockpit` | G1 | indicateurs, vues par service, seuils, comparaison | chef du DES, responsables | REP-10 à REP-13 |
| DES-27 | Cockpit de la Direction Générale | `/des/cockpit/direction` | G1 | indicateurs de synthèse, fiches d'action | DG | REP-09, PIL-17 |
| DES-28 | Rapports périodiques | `/des/rapports` | G2 | descripteur `rapportPeriodique`, visa | responsables, DG | REP-14, REP-15 |
| DES-29 | Salons et conférences | `/des/salons` | G8 | `ChatPanel`, liste des salons, `ConferenceControls` | tous | COL-01 à COL-05, COL-08, COL-11 |

### 10.2 Portail des participants (`/portail`)

| Réf. | Écran | Route | Gabarit | Données (`/bff/portail/...`) | Récits |
|---|---|---|---|---|---|
| POR-01 | Espace de l'institution | `/portail` | G1 | statut, conformité, échéances, liaisons, tickets, suspens, notifications | IHM-17, PAR-15, NOT-16 |
| POR-02 | Fiche et pièces légales | `/portail/institution` | G3 | fiche (lecture), proposition de correspondants, dépôt de pièces (G4) | PAR-15, PAR-16, DOC-10 |
| POR-03 | Ma liaison | `/portail/liaisons/{id}` | G3 | parcours (`DagView` en lecture), ce qui m'incombe, dépôt de preuve (G4), résultats, couvertures, dossier de mise en service, attestation | HOM-23, HOM-24, CER-13 |
| POR-04 | Mes tickets | `/portail/tickets` | G2 + G3 | descripteur `ticketPortail` | INC-05 |
| POR-05 | Mes suspens | `/portail/suspens` | G2 | descripteur `suspensPortail`, attestation (G4) | RAP-13 |
| POR-06 | Préférences et historique des notifications | `/portail/notifications` | G2 | notifications, préférences | NOT-16, NOT-04 |

Le portail est adapté au mobile dès 360 px : navigation en tiroir, tableaux en cartes empilées sous 640 px, boutons `lg`, dépôt de fichier depuis l'appareil.

### 10.3 Console d'administration et d'audit (`/console`)

| Réf. | Écran | Route | Gabarit | Données (`/bff/console/...`) | Récits |
|---|---|---|---|---|---|
| CON-01 | Rôles, permissions et délégations | `/console/habilitations` | G5 | `PolicyMatrix`, descripteurs `role`, `delegation`, `session` | IAM-04, IAM-06, IAM-17 |
| CON-02 | Campagne de réexamen des accès | `/console/habilitations/reexamen` | G4 + G2 | campagne, décisions, scellement | IAM-10, IAM-18 |
| CON-03 | Comptes de service et clés d'interface | `/console/habilitations/services` | G2 | descripteur `compteService` | IAM-16 |
| CON-04 | Journal d'audit et vérification d'intégrité | `/console/audit` | G2 + G9 | recherche, `AuditProofViewer`, `RootPublicationCard`, export | AUD-04 à AUD-08, AUD-14 |
| CON-05 | Rétention et purges | `/console/documents` | G2 | catalogue des types, purges à quatre yeux, intégrité, quarantaines | DOC-03, DOC-12 à DOC-16 |
| CON-06 | Routes, quotas et politiques | `/console/passerelle` | G5 | descripteurs `route`, `quota`, `politiqueAcces`, décisions récentes | GW-03, GW-18, GW-22, IAM-19 |
| CON-07 | Référentiels et matrices | `/console/referentiels` | G5 | matrice de sévérité, engagements, seuils de supervision, indicateurs, gabarits de notification, règles de destinataires | INC-22, MON-18, REP-05, NOT-02, NOT-09 |
| CON-08 | État des services et des files | `/console/exploitation` | G1 | quatorze services, retards, rebuts, disjoncteurs, reprises | MON-17, AUD-13, DOC-14 |
| CON-09 | Salons et terminaux | `/console/collaboration` | G2 | salons, terminaux vérifiés, terminal d'archivage | COL-10, COL-16 |
| CON-10 | Environnements et versions | `/console/systeme` | G1 | version, environnement, sauvegardes, restauration testée | SOC-15, SOC-16 |

### 10.4 Écrans transverses

| Réf. | Écran | Route | Gabarit | Contenu |
|---|---|---|---|---|
| TRA-01 | Connexion | `/connexion` | G10 | Bandeau gauche avec motif de trame et logo couleur ; bouton « Se connecter avec le fournisseur d'identité » ; mention de la couche ; aucun champ de mot de passe |
| TRA-02 | Réauthentification forte | dialogue | Dialog | `SignatureField` FIDO2, contexte de l'action |
| TRA-03 | Accès refusé (403) | `/acces-refuse` | G10 | Explication, permission manquante en langage clair, identifiant de corrélation, retour |
| TRA-04 | Page introuvable (404) | `*` | G10 | |
| TRA-05 | Erreur technique (500) | `/erreur` | G10 | Identifiant de corrélation, « réessayer », « signaler » (ouvre un ticket d'anomalie) |
| TRA-06 | Maintenance | `/maintenance` | G10 | Fenêtre annoncée, contact |
| TRA-07 | Session expirée | dialogue | Dialog | Réauthentification, brouillons conservés |
| TRA-08 | Sélecteur de langue et de thème | menu | Menu | FR/EN, clair/sombre, densité |

---

## 11. Accessibilité, internationalisation, performance, sécurité côté client

### 11.1 Accessibilité (WCAG 2.2 niveau AA)

- Contrastes : 4,5 : 1 texte, 3 : 1 grand texte et composants ; vérifiés par outil dans la chaîne de livraison (IHM-11).
- Clavier : tout est atteignable au clavier ; ordre de tabulation logique ; anneau de focus visible 2 px ; raccourcis documentés (`/` recherche, `Ctrl+K` palette, `Échap` fermer, `?` aide) ; pièges de focus dans les dialogues et tiroirs ; retour du focus au déclencheur.
- Lecteurs d'écran : rôles ARIA natifs d'abord ; `aria-live="polite"` pour les mises à jour poussées, `assertive` pour les bandeaux critiques ; tableaux avec en-têtes de colonne ; graphiques avec vue tableau et description.
- Cibles : 40 × 40 px au minimum ; 48 px sur le portail mobile.
- Mouvement : `prefers-reduced-motion` respecté ; aucune animation clignotante.
- Formulaires : étiquettes permanentes, erreurs liées par `aria-describedby`, message d'erreur de synthèse en haut du formulaire avec liens vers les champs.
- Contenu : langue déclarée (`lang="fr"` ou `"en"`), titres hiérarchiques, un `h1` par écran (le titre display).

### 11.2 Internationalisation

Toutes les chaînes dans `i18n/fr.json` et `i18n/en.json`, clés par composant et par écran (`institution.status.ACTIF`), pluriels par ICU, dates et nombres via `Intl` avec le fuseau Afrique centrale (UTC+1) et l'affichage UTC au survol ; bascule sans rechargement ; les données métier non traduisibles (dénominations, références) restent telles quelles ; les documents générés restent en français.

### 11.3 Budgets de performance

- Première peinture significative sous 1,5 s et interaction possible sous 2,5 s sur le réseau interne pour l'application du département ; sous 3 s sur une connexion mobile 3G pour le portail.
- Paquet initial sous 250 Ko compressé ; découpage par module (chargement paresseux des features) ; polices sous-ensemblées ; pictogrammes en sprite SVG.
- Listes : pagination serveur, pas de rendu de plus de 100 lignes ; virtualisation seulement pour le mur d'écrans et le graphe.
- Canal poussé : reconnexion exponentielle plafonnée à 30 s ; rafraîchissement d'un écran en moins d'une seconde après un événement.
- Mesures : indicateurs vitaux collectés (sans donnée personnelle) et publiés vers la pile d'observabilité.

### 11.4 Sécurité côté client

- Aucun jeton dans le stockage local ni en mémoire accessible : session par témoin `HttpOnly` posé par la passerelle (modèle BFF).
- Politique de contenu stricte (`default-src 'self'`, pas de script en ligne, polices et images locales, `frame-ancestors 'none'`), en-têtes de sécurité posés par la passerelle (GW-19).
- Aucune donnée de carte n'est jamais affichée ni saisie ; les champs libres passent par le contrôle de masquage côté service et l'interface affiche le résultat ; le client refuse en amont les séquences de 13 à 19 chiffres vérifiant la formule de Luhn dans les zones de texte (avertissement avant envoi).
- Les gardes d'interface (`RequirePermission`) masquent, le serveur refuse ; aucune décision de sécurité n'est prise côté client.
- Journaux du navigateur : aucune donnée personnelle ni identifiant de session ; l'identifiant de corrélation seulement.
- Dépendances : versions figées, audit des vulnérabilités dans la chaîne, aucune ressource distante (la plateforme est isolée).

### 11.5 Tests d'interface

Unitaires (composants du catalogue, hooks) ; accessibilité automatique sur chaque composant et chaque écran ; contrats avec la passerelle (schémas des réponses) ; parcours de bout en bout par rôle (connexion simulée, un parcours par écran principal) ; captures de référence des écrans en clair et en sombre ; revue d'ergonomie avec les utilisateurs à chaque sprint (IHM-18).

---

## 12. Prompts pour l'agent codeur

Les prompts ci-dessous sont à copier tels quels. Ils supposent que l'agent codeur a accès au dépôt du frontend, au présent document (placé à la racine sous `docs/DS-GIMAC-01.md`), au carnet de produit (`docs/CP-GIMAC-01.md`) et aux contrats de la passerelle (`docs/bff/*.yaml`). Chaque prompt de page se termine par les critères de fini ; l'agent ne déclare pas une page terminée sans les avoir démontrés.

### 12.1 Prompt système (à placer en tête de toute session)

```
Tu es l'agent de développement frontend de la plateforme intégrée du Département Exploitation et Supervision du GIMAC. Tu travailles dans un dépôt React 18 + TypeScript strict + Vite, avec React Router, TanStack Query, Zustand (état d'interface léger), react-hook-form + zod (formulaires), i18next (FR/EN), Vitest + Testing Library + axe (tests), Playwright (parcours), Storybook (catalogue vivant). Le style est porté par des jetons CSS (`--gm-*`) et des modules CSS ; aucune bibliothèque de composants tierce n'est employée pour le rendu, seules les primitives d'accessibilité (Radix) sont admises sous les composants du catalogue.

Règles absolues, sans exception :
1. Tu n'écris jamais une couleur, une taille, une durée ou une police en dur : tu consommes les jetons sémantiques de `design-system/tokens.css` (section 3.2 et 4.2 du DS-GIMAC-01).
2. Tu n'écris jamais de logique métier côté client : les transitions permises, les permissions effectives, les calculs et les règles viennent des réponses de la passerelle. L'interface masque, le serveur refuse.
3. Tu ne fais aucun appel réseau hors `lib/http` ; tu n'accèdes jamais à un jeton ; tu ne stockes rien dans le stockage local hormis les préférences d'affichage.
4. Une page est une composition : `ResourceList`, `ResourceDetail`, `ResourceForm`, `ResourceDelete` avec un descripteur, ou des composants de domaine du catalogue. Tu ne crées pas de tableau ni de formulaire spécifique dans une page.
5. Tout composant nouveau entre au catalogue (`design-system/`), avec sa story, ses tests unitaires, son test d'accessibilité et sa documentation ; il n'est jamais créé dans `features/`.
6. Chaque écran a quatre états démontrés : chargement (squelettes), vide (`EmptyState` avec cause), erreur (`InlineAlert` avec identifiant de corrélation et réessai), nominal.
7. Aucune information n'est portée par la seule couleur ; tout statut passe par `StatusBadge` (pictogramme + libellé) ; les graphiques ont une vue tableau.
8. Toute action à effet passe par `ConfirmDialog` (deux étapes) ou `SignDialog` (réauthentification FIDO2) selon le descripteur.
9. Angles droits, aucune ombre, aucun dégradé, aucune animation décorative, aucun emoji, aucune illustration de personnage.
10. Français correct dans les libellés (vocabulaire du chapitre 13 du DS-GIMAC-01), toutes les chaînes externalisées dans `i18n/`, accents sur les capitales, ponctuation française.
11. Aucune donnée réelle : les données de développement viennent des fabriques de `test/` (institutions fictives « Établissement de paiement A », « Banque B »), jamais d'un export de production.
12. Tu ne déclares une tâche finie qu'après : compilation TypeScript stricte sans `any`, tests unitaires et d'accessibilité verts, story à jour, critères Gherkin du récit démontrés, captures clair et sombre produites.

Avant de coder, tu lis la section du DS-GIMAC-01 qui concerne ta tâche et le récit du carnet de produit visé, puis tu annonces en cinq lignes ce que tu vas créer ou modifier (fichiers), et tu attends la confirmation si la tâche touche un composant du catalogue existant.
```

### 12.2 Prompt : initialiser le système de conception (sprint 0)

```
Objectif : créer `design-system/` conformément aux chapitres 3 à 7 du DS-GIMAC-01.

1. `tokens.css` : déclare toutes les primitives (3.1), la couche sémantique (3.2) en `:root` et sa redéfinition complète sous `[data-theme="dark"]` (exactement les 25 jetons), les espacements (5.1), les points de rupture (5.2), `--gm-radius: 0`, les durées et la courbe (5.4), l'échelle typographique (4.2) sous forme de classes utilitaires `.gm-type-*`.
2. `fonts.css` : `@font-face` pour Ubuntu (400, 500, 700), Inter (400, 500, 600, `font-feature-settings: "tnum"` sur `.gm-num`), JetBrains Mono (400, 500), fichiers `woff2` auto-hébergés, `font-display: swap`.
3. `reset.css` : réinitialisation minimale, `box-sizing: border-box`, focus visible global (anneau 2 px `--gm-color-focus`, décalage 2 px), `prefers-reduced-motion`.
4. `icons/` : sprite SVG des pictogrammes du 5.5 et composant `Icon` (`name`, `size: 16|20|24`, `label?`).
5. `pattern/trame.svg` et classe `.gm-pattern` (5.6).
6. Composants de la famille actions (7.1) puis saisie (7.2) avec `FieldWrapper`, puis affichage (7.3), navigation (7.4), conteneurs (7.5), en respectant les contrats TypeScript donnés. Chaque composant : fichier `.tsx`, module CSS, story avec une variante par état, test unitaire, test axe.
7. `Theme` : fournisseur qui pose `data-theme` et `data-density` depuis les préférences du profil.
8. Un écran de démonstration `/catalogue` (hors production) affichant tous les composants dans les deux thèmes.

Critères de fini : `tokens.css` passe le contrôle « aucune valeur en dur dans les composants » (script `scripts/check-tokens.mjs` à écrire : échoue si un module CSS contient un hexadécimal, un `px` de couleur ou une police nommée) ; contraste vérifié par test automatique sur `StatusBadge`, `Button`, `TextField` dans les deux thèmes ; Storybook démarre et affiche chaque composant ; rapport axe sans violation.
```

### 12.3 Prompt : créer ou modifier un composant du catalogue

```
Tâche : créer le composant <NomDuComposant> (famille <actions|saisie|affichage|navigation|conteneurs|données|domaine>), décrit à la section <7.x> du DS-GIMAC-01.

Livrables :
- `design-system/<famille>/<NomDuComposant>/<NomDuComposant>.tsx` avec le contrat de propriétés exactement tel que documenté (complète-le si la section ne le donne pas, en respectant les conventions : `className`, `data-testid`, jamais de `style` en ligne) ;
- `<NomDuComposant>.module.css` n'employant que des jetons `--gm-*` ;
- `<NomDuComposant>.stories.tsx` : une story par variante et par état (repos, survol, focus, désactivé, chargement, erreur, vide selon le composant), en clair et en sombre ;
- `<NomDuComposant>.test.tsx` : rendu, interactions clavier, rôles ARIA, test axe ;
- mise à jour de `design-system/index.ts` et de la documentation du composant (`README.md` de la famille : rôle, anatomie, quand l'employer, quand ne pas l'employer).

Contraintes : angles droits, aucune ombre ; hauteurs 40 px (32 en `sm`, 48 en `lg`) pour les contrôles ; cibles 40 × 40 px ; anneau de focus global ; aucun texte codé en dur (i18n) ; aucun appel réseau ; le composant ne connaît pas le métier sauf s'il appartient à la famille domaine, auquel cas il est composé de primitives du catalogue.

Si le composant existe déjà : ne le duplique pas, améliore-le à un seul endroit, mets à jour les stories et les tests, et liste les écrans impactés.
```

### 12.4 Prompt : créer une page (gabarit générique)

```
Tâche : créer l'écran <RÉF> « <Titre> », route `<route>`, gabarit <Gx>, décrit à la section 10 du DS-GIMAC-01, qui porte les récits <IDs> du carnet de produit.

1. Lis les fiches des récits <IDs> (critères Gherkin) et les contrats BFF `<fichiers>`.
2. Crée ou complète les descripteurs de ressource nécessaires dans `resources/` (section 8.2) : champs avec `kind`, `inList`, `inDetail`, `inForm`, actions avec `permission`, `confirm`, `isAllowed` reflétant `actionsPermises` de la réponse, `statusDictionary`, `liveTopic`.
3. Compose la page dans `features/<module>/<Page>.tsx` uniquement avec `ResourceList`, `ResourceDetail`, `ResourceForm`, `ResourceDelete` et les composants de domaine listés pour cet écran ; aucun JSX de tableau ou de formulaire spécifique.
4. Déclare la route et l'entrée de navigation (avec la permission requise pour l'afficher) dans `app/routes.tsx`.
5. Implémente les quatre états et le rafraîchissement par le canal poussé (`liveTopic`).
6. Ajoute les chaînes FR et EN.
7. Écris : un test de rendu par état, un test d'accessibilité axe, un parcours Playwright par critère Gherkin nominal du récit principal, des captures clair et sombre.

Critères de fini : les scénarios Gherkin des récits <IDs> sont démontrés (nominal, alternatif, erreur) ; la page respecte le gabarit ; aucune valeur en dur ; le titre display est unique ; le fil d'Ariane et l'entrée de navigation sont corrects ; le score axe est sans violation ; les captures sont jointes ; le compte rendu liste les fichiers créés et les descripteurs ajoutés.
```

### 12.5 Prompts par écran

Chaque prompt ci-dessous s'emploie avec le gabarit 12.4, en remplaçant les variables. Les spécificités sont précisées.

**DES-01 Tableau de bord du DES** (G1 ; IHM-16, REP-10, NOT-01)
Rangée de six `StatCard` alimentées par `GET /bff/des/indicateurs/mon-service` (délai moyen de résolution, respect des engagements, alertes non acquittées, liaisons en attente d'action, suspens ouverts, tâches en retard), chaque carte cliquable vers son écran filtré ; grille : notifications urgentes (liste de 5, acquittement), tickets S1 et S2 ouverts (`DataTable` compact), échéances de la semaine (institutions), mes tâches, liaisons en attente ; `Banner` critique persistant ; tout se rafraîchit par le canal poussé ; état vide par carte ; aucun graphique lourd sur cet écran.

**DES-02 Mon profil et mes disponibilités** (G3 ; IAM-12, IAM-06, IAM-08, IAM-09, COL-06)
Onglets : identité (lecture, rattachements immuables en `Timeline`), préférences (`Switch` thème, densité, langue, canaux de notification et plage horaire), délégations (demander : `ResourceForm(delegation)` avec motif, durée bornée à 72 h, approbateur ; liste), disponibilités (`ResourceList(disponibilite)`, déclaration avec justificatif via `FileDropzone`, état de validation), terminaux (`DeviceVerificationCard`, code court, révocation avec `ConfirmDialog`).

**DES-03 Bibliothèque des pièces** (G2 ; DOC-01, DOC-04 à DOC-06)
`ResourceList(document)` avec filtres type, institution, période, étiquette ; dépôt guidé en `Stepper` (fichier, type, institution, métadonnées, résultat de la chaîne de contrôle affiché par `MaskingResult` avant confirmation) ; tiroir : versions (lignée), empreinte (`Hash`), verrou de rétention, journal de consultation ; action « télécharger » journalisée ; refus explicite (antivirus, masquage) avec message et action corrective.

**DES-05 et DES-06 Registre et fiche d'une institution** (G2 et G3 ; PAR-01 à PAR-17)
Descripteur `institution` de la section 8.4 ; fiche : `InstitutionHeader`, `StateMachineBar` à six états, onglets ; transition en deux temps (demande avec motif puis approbation par un autre utilisateur via `SignDialog`, bouton d'approbation absent pour le demandeur avec rappel de la règle) ; signalement de non-conformité : `ConfirmDialog` rappelant que la plateforme ne suspend rien et notifie la production monétique.

**DES-07 Enregistrer une institution** (G4 ; PAR-01, PAR-07)
Trois étapes : identité (code, dénomination, sigle, catégorie, pays), adhésion (mode, banque de règlement, banque marraine obligatoire en indirect, services souscrits), correspondants (au moins un par fonction) ; résumé ; création à l'état candidat.

**DES-08 à DES-10 Liaisons, vue d'une liaison, clôture d'opération** (G2, G3 + G6, G4 ; HOM-04 à HOM-19)
Liste des liaisons avec type de projet, phase, couvertures (`ProgressRing` miniature), retards ; vue : en-tête (institution, type de projet, version du référentiel, chef de projet), onglet graphe (`DagView` avec `OperationNode` : quatre pictogrammes des quatre éléments, filtres par phase et par état, vue liste équivalente), onglet périmètre (`CoverageBar` par service), onglet résultats du simulateur (tableau en lecture seule, mention « repris du simulateur, jamais recalculé », scénarios inconnus signalés), onglet preuves, chronologie ; clôture : `FourElementsChecklist` (responsable, preuve, horodatage, validateur distinct), l'élément manquant indique l'action ; `SignDialog` ; le validateur ne peut pas être le responsable (bouton absent, rappel).

**DES-11 Dossier de mise en service** (G3 ; CER-01 à CER-12)
`QuitusCircuit` dans l'ordre imposé (technique, puis sécurité et conformité, puis direction générale) avec signataire (rôle), état, horodatage, motif ; six conditions de complétude affichées avec leur état ; fiche de changement lue (référence, état, date de lecture, bouton « relire ») ; procès-verbal ; attestation (téléchargement journalisé, `Hash`) ; signature par `SignDialog` avec empreinte du socle d'évaluation affichée ; refus avec motif obligatoire.

**DES-12 Référentiels d'opérations** (G5 ; HOM-01 à HOM-03)
Arbre tronc commun et branches par type de projet ; édition d'une version brouillon ; `DiffView` avec la version active ; contrôle d'acyclicité affiché avant publication ; publication avec `ConfirmDialog` et impact sur les liaisons en cours listé.

**DES-13 et DES-14 Tickets et incident** (G2, G3 ; INC-01 à INC-17)
Descripteur `ticket` (type, objet, institution, priorité, état, engagement, échéance) ; tiroir avec `TicketStateBar` (transitions permises renvoyées par le service), `SlaClock` (délai imputable, gel), pièces ; fiche : qualification par `SeverityMatrix` (impact × urgence, sévérité calculée par le service, marqueur réglementaire), escalade (paliers, contacts lus), remédiation (tâche liée), clôture : les quatre critères avec leur état, valideur distinct, période d'observation, rapport circonstancié (`Hash`) ; réouverture avec motif.

**DES-16 et DES-17 Liaisons supervisées et tableau mural** (G2, G7 ; MON-01 à MON-08, MON-16)
`AlertRow` avec délai d'acquittement restant (décompte), acquitter (`ConfirmDialog` court), ouvrir un ticket pré-rempli ; états injoignable et maintenance distincts ; tableau mural : tuiles par liaison (nom, état, dernière mesure, alertes), police `stat-wall`, aucun contrôle, rafraîchissement poussé, mode sombre par défaut.

**DES-18 Grille d'astreinte** (G6 ; MON-09 à MON-12)
`CalendarGrid` semaine × plages ; génération (paramètres visibles : repos, nuits consécutives, effectif) ; plages non couvertes signalées ; révision (nombre de changements affiché) ; publication par le chef du DES (`SignDialog`) ; échange entre opérateurs (demande, approbation) ; vue tableau équivalente.

**DES-19 Vacation et relève de quart** (G8 ; MON-13 à MON-15)
Volet contexte (vacation, chef de quart, opérateurs), flux (`LogbookEntryList` chaînée, saisie en bas), actions (`ShiftHandoverPanel` : générer le rapport, signature sortante, contresignature entrante, visa, dépôt ; responsabilité transférée seulement à la contresignature, indiqué clairement).

**DES-20 Salle de crise** (G8 ; INC-18 à INC-21, COL-09, COL-13)
`CrisisRoomHeader` (niveau, quorum, membres et acquittements, suppléance), main courante de crise, décisions converties en tâches (`DecisionToTaskRow`), `ChatPanel` du salon de crise, `ConferenceControls`, clôture (rapport post-incident, archive scellée : état de constitution, `Hash`, jeton d'horodatage).

**DES-21 et DES-22 Rapprochement et suspens** (G2 ; RAP-01 à RAP-13)
Import en `Stepper` (source, institution, journée, fichier, résultat du contrôle de format et de masquage, empreinte) ; `ReconciliationSummary` ; suspens : `SuspensCard`, automate à six états, conciliation (commentaires, pièces), contestation (ticket de litige), clôture sur attestation déposée puis validée par un second acteur (`SignDialog`, bouton absent pour le déposant) ; ancienneté J+1, J+5, J+30 en `StatusBadge` ; rappel « la plateforme ne réinjecte rien ».

**DES-23 Tableaux Kanban** (G6 ; PIL-01 à PIL-08)
`KanbanBoard` avec limites d'en-cours affichées ; refus au-delà de la limite avec dérogation (motif, approbateur distinct, 72 h) ; carte de tâche (origine typée avec lien, responsable, échéance, critères) ; vue « mes tâches » ; déplacement au clavier.

**DES-24 et DES-25 Projets, réunions** (G2 + G6, G2 + G3 ; PIL-09 à PIL-17)
Projet : jalons, `GanttView` avec chemin critique et marges, probabilité de tenue, `RiskMatrix` 5 × 5 (brut, maîtrise, résiduel), `PdcaCard` ; réunion : instance, ordre du jour, présences, compte rendu déposé (`Hash`), décisions → tâches avec taux d'exécution.

**DES-26 à DES-28 Cockpits et rapports** (G1, G2 ; REP-09 à REP-16)
`StatCard` et `ChartCard` (palette de données, légende, vue tableau, seuils à deux niveaux, tendance) ; filtres par période, institution, service ; comparaison de périodes ; favoris ; fiche d'action depuis un indicateur dégradé ; rapports : liste, visa (`SignDialog`), téléchargement journalisé, export contrôlé (agrégats, plafond, filigrane).

**DES-29 Salons et conférences** (G8 ; COL-01 à COL-05, COL-08, COL-11)
Liste des salons (quart, thème, incident, crise) ; `ChatPanel` (messages, accusés, pièces chiffrées, mentions, indicateur de terminal vérifié, refus d'envoi vers un terminal non vérifié expliqué) ; conférence locale (`ConferenceControls`, consentement à l'enregistrement, participants).

**POR-01 à POR-06 Portail** (IHM-07, IHM-17, PAR-15, PAR-16, HOM-23, HOM-24, CER-13, INC-05, RAP-13, NOT-16, DOC-10)
Enveloppe portail (nom de l'institution, pas de recherche globale) ; accueil G1 sur motif de trame (statut, conformité, échéances, liaison, tickets, suspens, notifications) ; toutes les listes cloisonnées à l'institution du jeton ; dépôts (pièces, preuves, attestations) en `Stepper` avec `MaskingResult` ; résultats du simulateur affichés tels quels ; adaptation mobile dès 360 px (cartes empilées, boutons `lg`, tiroir de navigation).

**CON-01 à CON-10 Console** (IAM-04, IAM-06, IAM-10, IAM-16 à IAM-19, AUD-04 à AUD-08, AUD-13, AUD-14, DOC-03, DOC-12 à DOC-16, GW-03, GW-18, GW-22, INC-22, MON-17, MON-18, REP-05, NOT-02, NOT-09, COL-10, COL-16, SOC-15, SOC-16)
Enveloppe console (bandeau d'environnement permanent, sessions courtes) ; `PolicyMatrix` rôles × permissions par domaine avec approbation à quatre yeux ; campagne de réexamen (G4 puis liste des décisions, scellement) ; audit : recherche multicritère, `AuditProofViewer` (feuille, chemin, racine, jeton, verdict), `RootPublicationCard`, export au format ouvert avec journalisation ; rétention et purges à quatre yeux ; routes, quotas, politiques avec `DiffView` et décisions récentes ; référentiels et matrices versionnés ; état des quatorze services, retards, rebuts, disjoncteurs, reprises ; salons et terminaux ; environnements et versions, sauvegardes et restauration testée.

**TRA-01 Connexion** (GW-01)
G10 avec bandeau gauche (40 %, motif de trame sur `--gm-color-brand-pattern`, logo couleur, nom de la couche) ; bouton principal « Se connecter avec le fournisseur d'identité » ; aucun champ de saisie ; message d'erreur de délégation avec identifiant de corrélation ; mention de classification.

### 12.6 Prompt : revue d'un écran ou d'un composant

```
Revois <fichier ou écran> contre la grille de revue du DS-GIMAC-01 (section 12.7). Pour chaque point : conforme, non conforme avec la ligne concernée et la correction à apporter, ou sans objet. Ne corrige rien sans avoir listé. Termine par un verdict : accepté, accepté sous réserve (liste), refusé (liste).
```

### 12.7 Grille de revue (30 points)

Jetons : 1 aucune valeur en dur ; 2 seule la couche sémantique est consommée ; 3 le mode sombre est correct sur captures. Structure : 4 gabarit déclaré ; 5 page composée sans JSX de tableau ni de formulaire ; 6 descripteur complet (permissions, confirmations, dictionnaire de statuts, `liveTopic`) ; 7 composant nouveau au catalogue avec story et tests. États : 8 chargement ; 9 vide avec cause ; 10 erreur avec identifiant de corrélation et réessai ; 11 nominal. Accessibilité : 12 axe sans violation ; 13 clavier complet ; 14 focus visible et retour au déclencheur ; 15 un `h1` ; 16 statuts avec pictogramme et libellé ; 17 vue tableau des graphiques ; 18 cibles 40 px. Contenu : 19 libellés conformes au vocabulaire ; 20 chaînes externalisées FR et EN ; 21 verbes + objets sur les boutons ; 22 horodatages locaux avec UTC ; 23 montants formatés. Sécurité : 24 aucun appel hors `lib/http` ; 25 aucun jeton ni donnée dans le stockage local ; 26 actions à effet confirmées ou signées ; 27 gardes d'interface sans décision de sécurité. Qualité : 28 TypeScript strict sans `any` ; 29 tests unitaires et parcours des critères Gherkin ; 30 captures clair et sombre jointes.

---

## 13. Vocabulaire d'interface imposé

| Terme retenu | Termes proscrits | Sens |
|---|---|---|
| institution membre | client, partenaire, banque (sauf catégorie) | Établissement adhérent au réseau |
| correspondant | contact, utilisateur externe | Personne d'une institution habilitée sur le portail |
| liaison | projet d'intégration, onboarding | Parcours de raccordement d'une institution |
| opération | tâche d'homologation, étape | Nœud du parcours d'une liaison |
| preuve | document, fichier (dans ce contexte) | Pièce probante d'une opération |
| règle des quatre éléments | validation complète | Responsable, preuve, horodatage, validation |
| quitus | approbation, go | Décision signée de mise en service |
| attestation de certification | certificat | Document PDF/A-3 signé |
| ticket | demande, requête, incident (sauf qualifié) | Toute sollicitation tracée |
| incident | panne, problème | Ticket qualifié par la matrice |
| sévérité S1 à S4 | priorité P1 à P5, criticité | Résultat de la qualification |
| alerte | alarme, notification (dans ce contexte) | Signal d'une sonde sur une liaison supervisée |
| acquitter | valider, confirmer une alerte | Prendre acte d'une alerte dans le délai |
| vacation, quart | shift, tour | Période de supervision |
| relève de quart | passation, handover | Transfert de responsabilité signé |
| grille d'astreinte | planning | Affectation hebdomadaire |
| rapprochement | réconciliation | Confrontation de deux états |
| suspens | écart, anomalie (dans ce contexte) | Écart suivi jusqu'à régularisation |
| régularisation attestée | apurement | Clôture d'un suspens sur attestation |
| cellule de crise | war room | Coordination d'un incident majeur |
| salon | canal, groupe, chat | Espace de messagerie chiffrée |
| conférence | réunion vidéo, visio | Session audio et vidéo locale |
| tableau, tâche, colonne, limite d'en-cours | board, card, WIP | Éléments du Kanban |
| fiche d'action | plan d'action (dans ce contexte) | Cycle PDCA |
| indicateur | KPI | Mesure du reporting |
| cockpit | dashboard (pour la DG et le DES) | Tableau de bord décisionnel |
| tableau de bord | dashboard | Accueil d'un utilisateur |
| registre d'audit | logs, journal (dans ce contexte) | Registre inaltérable |
| preuve d'inclusion, racine de période | hash, root | Éléments de vérification |
| console | back-office, admin | Console d'administration et d'audit |
| portail des participants | espace client, extranet | Couche externe |
| application du département | intranet, back-office | Couche interne |
| déposer | uploader, télécharger (sens montant) | Verser une pièce |
| télécharger | download | Récupérer une pièce |
| se connecter, se déconnecter | login, logout | Session |
| clé de sécurité | token, YubiKey | Authentificateur FIDO2 |

Règles complémentaires : pas d'anglicismes techniques dans l'interface (« BFF », « endpoint », « payload » restent dans le code) ; les acronymes admis sans explication : DES, GIMAC, COBAC, BEAC, RSSI, GED, PDF, UTC, XAF ; le nom des institutions apparaît tel qu'enregistré, jamais abrégé par l'interface ; les identifiants suivent les formats `INST-0042`, `LIA-2026-017`, `OP-…`, `TCK-000318`, `ALR-2026-04417`, `SUS-…`, `TSK-2026-0412`, `CRI-2026-009`, `RQ-2026-0927-N`, `DOC-2026-7B12`, toujours en chasse fixe.

---

## 14. Livrables attendus du chantier d'interface et ordre de réalisation

1. Sprint 0 : `design-system/` (prompt 12.2), enveloppe (`AppShell`, `TopBar`, `SideNav`, pages d'erreur, connexion), descripteurs des premières ressources, catalogue vivant en ligne sur l'environnement d'intégration.
2. Sprint 1 : composants universels du CRUD et hooks de données, centre de notifications, tiroir, tableaux, DES-01 (squelette), DES-02, TRA-01 à TRA-08.
3. Sprint 2 : DES-03 à DES-07, DES-16 à DES-19, DES-04.
4. Sprint 3 : DES-08 à DES-10, DES-12, DES-26 (première version), DES-28.
5. Sprint 4 : DES-11, POR-01 à POR-06, CON-01 à CON-10, revue d'ergonomie de la première phase.
6. Vagues 2 et 3 : DES-13 à DES-15, DES-20 à DES-25, DES-27, DES-29, mode sombre finalisé, accessibilité finale.

Chaque écran est maquetté avant d'être codé : la maquette est produite à partir des composants de ce document (dans l'outil de maquettage, avec la bibliothèque de composants synchronisée aux jetons), validée par le responsable de produit, puis intégrée au cahier de conception à l'emplacement réservé (section 6.3.5) ; c'est le prérequis de la définition de prêt des récits IHM.
