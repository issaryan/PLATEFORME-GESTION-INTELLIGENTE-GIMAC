// Inventaire des composants du système de conception publié (généré depuis ses fiches) :
// groupe, dimensions de la carte d'aperçu et texte de la fiche du composant.

export interface EntreePlan {
  nom: string
  groupe: string
  hauteur: number
  largeur: number | null
  description: string
}

export const GROUPES = ["Actions", "Saisie", "Affichage", "Navigation", "Conteneurs", "Retours", "Dialogues", "Données", "Enveloppe", "Domaine", "CRUD"] as const

export const PLAN: EntreePlan[] = [
  {
    "nom": "Button",
    "groupe": "Actions",
    "hauteur": 112,
    "largeur": null,
    "description": "Bouton d'action : un verbe à l'infinitif et son objet. Variantes `primary` (fond `gm-color-action`, texte inverse, un seul par zone), `secondary` (surface, bordure et texte action), `danger` (bordure et texte `gm-color-status-crit-fg`, toujours suivi d'un ConfirmDialog), `ghost` (texte action sans bordure, actions de faible importance dans les tableaux). Tailles `md` 40 px (défaut), `sm` 32 px (lignes de tableau), `lg` 48 px (portail mobile).\n\n**Le consommateur fournit** `children` (verbe + objet), `variant`, `size`, `icon?`, `loading?`, `disabled?` avec `disabledReason` (affiché en infobulle : un bouton n'est jamais désactivé sans explication), `onClick`, `type`.\n\n**États** : repos, survol (action-hover pour primary, action-subtle pour les autres), focus (anneau 2 px `gm-color-focus`), désactivé (opacité 0,5, curseur not-allowed), chargement (spinner 16 px à gauche, libellé conservé, `aria-busy`).\n\n**Interdits** : plus d'un `primary` par zone ; un `danger` sans confirmation ; un bouton qui ne dit que « OK », « Valider » ou « Soumettre » ; une cible de moins de 40 × 40 px."
  },
  {
    "nom": "IconButton",
    "groupe": "Actions",
    "hauteur": 72,
    "largeur": null,
    "description": "Bouton carré de 40 px (32 px en `sm`) avec pictogramme seul ; `label` est obligatoire et devient l'`aria-label` et l'infobulle. Mêmes variantes que Button ; `ghost` dans les barres et les lignes de tableau. Ne jamais l'employer pour une action à effet sans ConfirmDialog."
  },
  {
    "nom": "Link",
    "groupe": "Actions",
    "hauteur": 56,
    "largeur": null,
    "description": "Lien texte en `gm-color-action`, souligné au survol et au focus. `external` ajoute `rel=\"noopener\"` et une nouvelle fenêtre ; `mono` affiche une référence d'objet (ticket, liaison) en chasse fixe. Un lien navigue ; il ne déclenche jamais une action à effet (c'est un Button)."
  },
  {
    "nom": "SplitButton",
    "groupe": "Actions",
    "hauteur": 120,
    "largeur": null,
    "description": "Action principale + menu d'actions secondaires ouvert par la flèche de 32 px (ArrowDown au clavier, `aria-haspopup=\"menu\"`). Employé quand plusieurs verbes s'appliquent au même objet (« Signer le quitus » et « Refuser avec motif »). Un seul SplitButton primaire par zone."
  },
  {
    "nom": "Checkbox",
    "groupe": "Saisie",
    "hauteur": 120,
    "largeur": null,
    "description": "Case carrée de 18 px, libellé cliquable, angles droits. CheckboxGroup pour une sélection multiple avec état indéterminé ; RadioGroup pour un choix exclusif de 2 à 5 options ; Switch pour une préférence binaire à effet immédiat (thème, densité, canal), jamais pour une action à effet."
  },
  {
    "nom": "CheckboxGroup",
    "groupe": "Saisie",
    "hauteur": 200,
    "largeur": null,
    "description": "Groupe de cases avec « Tout sélectionner » à l'état indéterminé quand une partie seulement est cochée. `fieldset` et `legend` natifs. Pour un choix exclusif, RadioGroup ; pour une préférence immédiate, Switch."
  },
  {
    "nom": "DateField",
    "groupe": "Saisie",
    "hauteur": 420,
    "largeur": 800,
    "description": "Saisie de date au clavier au format JJ/MM/AAAA, calendrier accessible en popover (navigation clavier, `aria-pressed` sur le jour choisi), fuseau affiché (Afrique centrale, UTC+1), `min`, `max`, raccourcis « aujourd'hui », « J-7 », « ce mois ». DateTimeField ajoute l'heure locale ; DateRangeField prend un début et une fin et surligne la plage. Les horodatages sont toujours affichés en heure locale avec l'UTC au survol.\n\n**Le consommateur fournit** `name`, `label`, `value` (chaîne JJ/MM/AAAA), `onChange`, `min?`, `max?`, `required?`, `help?`, `error?` ; pour DateRangeField `from`, `to` ; pour DateTimeField `date`, `time`."
  },
  {
    "nom": "FileDropzone",
    "groupe": "Saisie",
    "hauteur": 380,
    "largeur": 880,
    "description": "Dépôt de pièce par glisser ou clic : types et taille maximale annoncés, barre de progression, puis résultat de la chaîne de contrôle affiché par MaskingResult (acceptée, masquée avec le nombre de séquences, refusée avec l'explication et l'action corrective). Le refus est explicite : antivirus, masquage, type non admis. Aucune donnée de carte n'est jamais affichée ; le client refuse en amont une séquence de 13 à 19 chiffres vérifiant la formule de Luhn.\n\n**Le consommateur fournit** `name`, `label`, `accept`, `maxSize`, `file?` (`{ name, size }`), `state` (`idle | uploading | done`), `progress?`, `masking?` (`{ result: accepted | masked | rejected, count?, text }`)."
  },
  {
    "nom": "MultiSelect",
    "groupe": "Saisie",
    "hauteur": 120,
    "largeur": null,
    "description": "Sélection multiple par jetons (Tag) avec retrait au clavier (Retour arrière retire le dernier jeton) et à la souris. Employé pour les services souscrits, les destinataires d'une notification, les étiquettes d'un document. Au-delà de 10 options, la saisie filtre la liste comme SearchSelect."
  },
  {
    "nom": "NumberField",
    "groupe": "Saisie",
    "hauteur": 96,
    "largeur": null,
    "description": "Nombre avec séparateur de milliers à l'affichage, saisie brute, `min`, `max`, `step`, unité en suffixe (XAF, %, min). Chiffres tabulaires. Le montant n'est jamais recalculé côté client."
  },
  {
    "nom": "PinField",
    "groupe": "Saisie",
    "hauteur": 120,
    "largeur": null,
    "description": "Saisie à usage unique de 6 cases (code court de vérification d'un terminal, code de réexamen) : une case par chiffre, `inputMode=\"numeric\"`, `autocomplete=\"one-time-code\"` sur la première, passage automatique à la case suivante, chasse fixe. Jamais pour un mot de passe : l'authentification passe par le fournisseur d'identité et la clé de sécurité."
  },
  {
    "nom": "SearchInput",
    "groupe": "Saisie",
    "hauteur": 64,
    "largeur": null,
    "description": "Recherche avec délai de 300 ms, effacement, raccourci `/` ; la recherche globale de la barre supérieure (Ctrl+K) ouvre la CommandPalette. Les résultats sont groupés par type d'objet."
  },
  {
    "nom": "SearchSelect",
    "groupe": "Saisie",
    "hauteur": 300,
    "largeur": null,
    "description": "Liste avec recherche dès 10 options (institutions, collaborateurs) : `combobox` accessible, chargement asynchrone (`loading`), affichage code + libellé, création interdite (le message d'absence le dit). Les options viennent du service (`options: () => Promise<Option[]>` dans le descripteur).\n\n**Le consommateur fournit** `name`, `label`, `value`, `query`, `onQuery`, `onChange`, `options` (`{ value, code, label }`), `open`, `loading?`."
  },
  {
    "nom": "Select",
    "groupe": "Saisie",
    "hauteur": 96,
    "largeur": null,
    "description": "Liste fermée jusqu'à 10 options, `select` natif accessible. Au-delà de 10 options, SearchSelect (recherche, chargement asynchrone, affichage code + libellé, création interdite). Pour 2 à 5 choix exclusifs, RadioGroup."
  },
  {
    "nom": "SignatureField",
    "groupe": "Saisie",
    "hauteur": 340,
    "largeur": null,
    "description": "Bloc de réauthentification forte FIDO2 utilisé par SignDialog pour toute action signée (quitus, clôture, scellement, purge, transition de statut, relève de quart) : pictogramme pen-line, texte « Confirmez avec votre clé de sécurité », rappel de l'action et de l'empreinte de ce qui sera signé (Hash), puis résultat horodaté. États `idle`, `waiting`, `done`, `error`. Le client n'accède jamais à un jeton ; la signature est vérifiée par le service."
  },
  {
    "nom": "Switch",
    "groupe": "Saisie",
    "hauteur": 80,
    "largeur": null,
    "description": "Préférence binaire immédiate (canal de notification, thème, densité) ; `role=\"switch\"`. Jamais pour une action à effet : celle-ci passe par un Button et une confirmation."
  },
  {
    "nom": "TextArea",
    "groupe": "Saisie",
    "hauteur": 140,
    "largeur": null,
    "description": "Texte long : hauteur automatique de 3 à 12 lignes, compteur quand `maxLength` existe, même anatomie FieldWrapper que TextField. Toute zone de texte libre passe par le contrôle de masquage côté service ; le client avertit avant envoi si une séquence de 13 à 19 chiffres vérifiant la formule de Luhn est présente."
  },
  {
    "nom": "TextField",
    "groupe": "Saisie",
    "hauteur": 340,
    "largeur": null,
    "description": "Champ texte court avec l'anatomie FieldWrapper : étiquette permanente au-dessus (`gm-type-body-strong`), marque « obligatoire », aide sous le champ (`gm-type-small`, text-muted), erreur sous le champ (status-crit-fg avec pictogramme, liée par `aria-describedby`), bordure 1 px `gm-color-border-strong`, 2 px `gm-color-action` au focus, hauteur 40 px, rayon 0. Pas de texte indicatif (`placeholder`) : l'étiquette est permanente. Variante `mono` pour les identifiants. Validation de format côté client seulement ; les erreurs métier viennent du service.\n\n**Le consommateur fournit** `name`, `label`, `value`, `onChange`, `required?`, `help?`, `error?`, `disabled?`, `readOnly?`, `mono?`, `maxLength?`, `autoComplete?`."
  },
  {
    "nom": "Avatar",
    "groupe": "Affichage",
    "hauteur": 56,
    "largeur": null,
    "description": "Avatar d'une personne, cercle (`gm-radius-avatar`, la seule forme arrondie du système, §5.3) : photo de profil recadrée si le compte en a une (adresse versionnée `/bff/<couche>/iam/avatars/{id}?v=<version>`, docs/lot1/mon-compte.md), initiales sur fond `gm-color-action-subtle` à défaut ou si l'image ne se charge pas. Tailles `xs` 20 px (jetons de destinataires), `sm` 24 px (fil, tableaux), `md` 32 px (défaut), `lg` 40 px (menu utilisateur), `xl` 96 px (Mon compte, fiche d'un compte). Employé dans le menu utilisateur, la messagerie (conversations, fil, destinataires) et la console (liste et fiche des comptes).\n\n**Le consommateur fournit** `name`, `src?` (adresse de la photo), `size?`, `decorative?` (nom déjà écrit à côté).\n\n**Interdits** : ombre, liseré coloré, photo non recadrée ; une photo sans repli sur les initiales."
  },
  {
    "nom": "CorrelationIdChip",
    "groupe": "Affichage",
    "hauteur": 56,
    "largeur": null,
    "description": "Identifiant de corrélation en chasse fixe avec copie et, dans la console, lien vers le registre d'audit. Affiché dans tout message d'erreur, dans le pied de page et dans la chronologie : c'est la seule information technique montrée à l'utilisateur."
  },
  {
    "nom": "EmptyState",
    "groupe": "Affichage",
    "hauteur": 300,
    "largeur": null,
    "description": "État vide d'un écran, d'une liste ou d'une carte : pictogramme 24 px sur motif de trame (240 × 160 px), titre `gm-type-h2`, explication, action principale. Le texte change selon la cause : aucune donnée, filtre sans résultat, droits insuffisants. Un écran sans état vide n'est pas fini."
  },
  {
    "nom": "KeyValueList",
    "groupe": "Affichage",
    "hauteur": 160,
    "largeur": null,
    "description": "Liste clé-valeur en deux colonnes : clé en `gm-color-text-secondary`, valeur en `gm-color-text-primary` ; `mono` pour les identifiants (copie au clic). C'est le rendu des champs `inDetail` d'un descripteur dans ResourceDetail."
  },
  {
    "nom": "MaskingResult",
    "groupe": "Affichage",
    "hauteur": 220,
    "largeur": null,
    "description": "Résultat du contrôle de masquage d'une pièce ou d'un champ libre, renvoyé par le service : `accepted`, `masked` (n séquences masquées), `rejected` avec explication et action corrective. Toujours affiché avant la confirmation du dépôt (Stepper) et dans le tiroir de la pièce."
  },
  {
    "nom": "Money",
    "groupe": "Affichage",
    "hauteur": 56,
    "largeur": null,
    "description": "Montant formaté avec espace fine et devise ISO après le nombre (1 250 000 XAF), chiffres tabulaires, aligné à droite en colonne (`gm-type-number`). DateTime affiche l'horodatage local (UTC+1) au format 28/09/2026 14:05 avec l'UTC en infobulle ; variante relative rafraîchie (« il y a 4 min »)."
  },
  {
    "nom": "Sparkline",
    "groupe": "Affichage",
    "hauteur": 140,
    "largeur": 420,
    "description": "Mini-courbe (tuile de mesure, ligne de tableau de supervision) : sans axe ni grille, trait de 2 px dans la série `gm-serie-1` à `gm-serie-4` (ordre fixe validé en clair et en sombre) ou `gm-serie-autre` pour une courbe de contexte, point final cerclé de la surface, lavis d'aire à 10 % en option. Largeur fluide, hauteur fixe ; une mesure absente (`null`) laisse un trou, jamais un zéro. `role=\"img\"` avec un résumé accessible (« Processeur : de 12 % à 34 % ») ; la valeur courante reste lisible hors du graphique.\n\n**Le consommateur fournit** `points` (valeurs dans l'ordre du temps), `label`, `unit?`, `serie?` (0 à 3 ou `autre`), `aire?`, `min?`, `max?`, `hauteur?`."
  },
  {
    "nom": "ProgressRing",
    "groupe": "Affichage",
    "hauteur": 160,
    "largeur": 960,
    "description": "Anneau de couverture (certification, mise en service) avec pourcentage au centre, teinte `gm-color-action`, statut porté par le libellé (StatusBadge), jamais par la couleur de l'anneau. `role=\"img\"` avec libellé accessible. CoverageBar est la barre par service à quatre paliers (inclus, en essai, certifié, en service) avec légende textuelle."
  },
  {
    "nom": "SeverityBadge",
    "groupe": "Affichage",
    "hauteur": 56,
    "largeur": null,
    "description": "Sévérité S1 à S4 d'un incident, résultat de la qualification par la matrice impact × urgence (calculée par le service). Puce à bordure : S1 critique, S2 à surveiller, S3 et S4 neutres. Jamais « priorité P1 à P5 » ni « criticité »."
  },
  {
    "nom": "Skeleton",
    "groupe": "Affichage",
    "hauteur": 120,
    "largeur": null,
    "description": "Blocs `gm-color-surface-sunken` animés en opacité pendant le chargement, à la forme du contenu attendu (dix lignes pour un tableau, une carte pour une StatCard). Jamais de spinner plein écran."
  },
  {
    "nom": "StatCard",
    "groupe": "Affichage",
    "hauteur": 168,
    "largeur": null,
    "description": "Carte d'indicateur (DS-GIMAC-01 §7.3) : surface plate, bordure de 1 px, angles droits, aucune ombre ni pictogramme décoratif. Libellé (`gm-type-small`), valeur (`gm-type-stat` : 32 px, 600, chiffres tabulaires), unité ; ligne de contexte : statut porté par le pictogramme réservé (§5.5) et le libellé, colorés, jamais par un fond ni une bande ; tendance (flèche et variation), seuil (« seuil 95 % »), `context` libre (échéance, période). `onClick` ouvre le détail ou l'écran filtré : bordure d'action et cartouche au survol, anneau de focus, Entrée et Espace. Rangée de 4 à 6 cartes en tête d'un tableau de bord (gabarit G1), grille régulière `gm-stat-grid`.\n\nTuile de mesure : avec `serie`, une mini-courbe (Sparkline) de l'évolution récente s'affiche sous la valeur.\n\n**Le consommateur fournit** `label`, `value`, `unit?`, `trend?`, `threshold?`, `context?`, `status?`, `statusLabel?`, `onClick?`, `serie?`, `serieLibelle?`.\n\n**Interdits** : un pictogramme décoratif ; un statut porté par la seule couleur."
  },
  {
    "nom": "StatusBadge",
    "groupe": "Affichage",
    "hauteur": 72,
    "largeur": null,
    "description": "Puce de 24 px : pictogramme 16 px + libellé `gm-type-small-strong` sur fond de statut. `status` : `ok`, `warn`, `crit`, `info`, `neutral` ; le pictogramme réservé est appliqué automatiquement (circle-check, triangle-alert, octagon-alert, info). Les libellés viennent du dictionnaire de statuts du descripteur de ressource, en minuscules, accordés.\n\nUn statut = pictogramme + libellé + couleur : retirer la couleur ne doit rien enlever au sens. Le fond de puce est la seule surface colorée admise pour un statut ; jamais une ligne ni une carte entière."
  },
  {
    "nom": "Tag",
    "groupe": "Affichage",
    "hauteur": 56,
    "largeur": null,
    "description": "Étiquette neutre (catégorie, type de projet, filtre actif) ; retirable dans les barres de filtres avec `onRemove`. Un Tag ne porte jamais un statut : c'est le rôle de StatusBadge."
  },
  {
    "nom": "Timeline",
    "groupe": "Affichage",
    "hauteur": 200,
    "largeur": null,
    "description": "Chronologie verticale : événement, horodatage local (UTC au survol), acteur (rôle et nom), identifiant de corrélation en chasse fixe, pièce liée. Alimentée par l'endpoint `timeline` du descripteur ; c'est l'onglet Chronologie de tout tiroir et de toute fiche."
  },
  {
    "nom": "Breadcrumb",
    "groupe": "Navigation",
    "hauteur": 56,
    "largeur": null,
    "description": "Fil d'Ariane à 3 niveaux au plus, dernier élément non cliquable (`aria-current=\"page\"`), en `gm-type-small` dans la barre supérieure."
  },
  {
    "nom": "Pagination",
    "groupe": "Navigation",
    "hauteur": 64,
    "largeur": null,
    "description": "Pagination côté serveur : taille de page 25, 50, 100 ; total ; page courante. Jamais de défilement infini dans les listes de travail ; jamais plus de 100 lignes rendues."
  },
  {
    "nom": "Stepper",
    "groupe": "Navigation",
    "hauteur": 72,
    "largeur": null,
    "description": "Assistant en étapes (gabarit G4) : numéro, libellé, état `todo`, `current`, `done`, `error` ; navigation retour permise ; résumé avant confirmation ou signature. Un formulaire de plus de 8 champs est découpé en étapes ; un brouillon local est conservé jusqu'à soumission."
  },
  {
    "nom": "Tabs",
    "groupe": "Navigation",
    "hauteur": 72,
    "largeur": null,
    "description": "Onglets d'une fiche ou d'un tiroir ; l'onglet actif est souligné de 2 px `gm-color-action` ; navigation clavier par flèches ; l'onglet actif est dans l'URL (`?onglet=pieces`). Les onglets d'une fiche viennent du descripteur de ressource."
  },
  {
    "nom": "Card",
    "groupe": "Conteneurs",
    "hauteur": 220,
    "largeur": null,
    "description": "Surface `gm-color-surface` avec bordure 1 px `gm-color-border`, en-tête (titre `gm-type-h2`, actions), corps à 16 px de marge interne, pied optionnel. Pas d'ombre, angles droits. Panel est le cartouche sur `gm-color-surface-sunken` pour les résumés et les filtres."
  },
  {
    "nom": "Banner",
    "groupe": "Retours",
    "hauteur": 200,
    "largeur": null,
    "description": "Bandeau pleine largeur sous la barre supérieure : `info` (fond `gm-color-action-subtle`), `warn`, `crit` (fond de statut, filet gauche 4 px). Persistant jusqu'à acquittement pour les niveaux urgent et critique ; `role=\"alert\"` pour crit. Le rouge n'est jamais en aplat de plus de 48 px : le bandeau critique est un fond status-crit-bg avec un filet status-crit-fg."
  },
  {
    "nom": "InlineAlert",
    "groupe": "Retours",
    "hauteur": 140,
    "largeur": null,
    "description": "Message contextuel dans une carte ou un formulaire : information, à surveiller, critique. L'état d'erreur d'un écran est un InlineAlert critique avec l'identifiant de corrélation et une action « Réessayer ». Un message d'erreur dit ce qui s'est passé, pourquoi, et quoi faire."
  },
  {
    "nom": "Toast",
    "groupe": "Retours",
    "hauteur": 64,
    "largeur": null,
    "description": "Retour d'action non bloquant (« Opération close », « Pièce déposée »), 6 secondes, empilé en bas à droite sur `gm-color-surface-raised`. Jamais pour une erreur bloquante : celle-ci est un InlineAlert ou un Dialog."
  },
  {
    "nom": "CommandPalette",
    "groupe": "Dialogues",
    "hauteur": 520,
    "largeur": 900,
    "description": "Recherche globale Ctrl+K (application du département et console, pas sur le portail) : objets (institution, liaison, ticket, suspens, pièce), écrans et actions, résultats groupés, référence en chasse fixe et statut de l'objet, navigation clavier (flèches, Entrée, Échap). Les résultats viennent du service de recherche et respectent les permissions du jeton : un objet non autorisé n'apparaît pas."
  },
  {
    "nom": "ConfirmDialog",
    "groupe": "Dialogues",
    "hauteur": 700,
    "largeur": 900,
    "description": "Confirmation à deux étapes pour toute action à effet (`confirm: \"confirm\"` dans le descripteur) : rappel de l'objet (KeyValueList), conséquences, champ de motif si requis (10 caractères au moins, consigné dans la chronologie et le registre d'audit), case « je confirme » pour une action irréversible, bouton principal libellé avec le verbe et l'objet (jamais « OK »). `role=\"alertdialog\"`. Le bouton reste désactivé, avec la raison en infobulle, tant que les conditions ne sont pas réunies ; une variante `danger` porte le bouton destructeur."
  },
  {
    "nom": "Dialog",
    "groupe": "Dialogues",
    "hauteur": 380,
    "largeur": 900,
    "description": "Fenêtre modale centrée de 560 px (720 px en `lg`) sur le voile `gm-color-overlay` : titre `gm-type-h2`, angles droits, ouverture en fondu (opacité seule, 200 ms, supprimée sous `prefers-reduced-motion`), description, corps, pied d'actions. Piège de focus, fermeture par Échap et par le voile, retour du focus au déclencheur à la fermeture, `aria-modal` et `aria-labelledby`. Aucune ombre : la fenêtre est séparée de la page par le voile et une bordure de 1 px. Un dialogue ne contient jamais un tableau ni un formulaire long (c'est un Drawer ou un Stepper).\n\n**Le consommateur fournit** `title`, `description?`, `children`, `footer` (au plus un bouton principal), `onClose`, `size?`, `open?` ; `inline` confine le voile au conteneur parent (catalogue, aperçus)."
  },
  {
    "nom": "Drawer",
    "groupe": "Dialogues",
    "hauteur": 620,
    "largeur": 900,
    "description": "Tiroir de détail de 480 px (640 px en `lg`) ouvert à droite sans quitter la liste (gabarit G2) : en-tête (type, référence en chasse fixe, statut, « ouvrir en pleine page », fermeture), titre, onglets Détail, Chronologie, Pièces, Audit, corps, pied avec les actions permises par le rôle. Piège de focus, Échap, l'URL porte l'objet ouvert (`?detail=TCK-000318`). C'est le tiroir de ResourceList ; son contenu vient du descripteur."
  },
  {
    "nom": "Menu",
    "groupe": "Dialogues",
    "hauteur": 300,
    "largeur": null,
    "description": "Menu d'actions (« Plus », colonne d'actions d'un tableau, SplitButton) : navigation clavier par flèches, séparateurs, raccourcis, actions dangereuses en bas avec pictogramme et couleur critique ; une action non permise est désactivée avec la raison en infobulle (jamais retirée sans explication quand l'utilisateur a le droit de la voir). `role=\"menu\"` et `menuitem` natifs."
  },
  {
    "nom": "NotificationCenter",
    "groupe": "Dialogues",
    "hauteur": 540,
    "largeur": null,
    "description": "Panneau de 400 px ouvert depuis la cloche de la barre supérieure : onglets « Non lues » et « Toutes », regroupement par niveau (critique, urgent, important, information), chaque notification avec pictogramme, objet, texte, horodatage relatif, lien vers l'objet en chasse fixe, bouton d'acquittement pour les niveaux urgent et critique. Une notification non lue porte un filet gauche `gm-color-action`. Les niveaux urgent et critique s'affichent aussi en Banner persistant jusqu'à acquittement."
  },
  {
    "nom": "Popover",
    "groupe": "Dialogues",
    "hauteur": 300,
    "largeur": null,
    "description": "Contenu riche ancré à un déclencheur (calendrier, filtres avancés, sélection de colonnes) sur `gm-color-surface-raised` avec bordure 1 px, sans ombre ; fermeture par Échap et clic à l'extérieur, focus rendu au déclencheur. Tooltip est l'infobulle au survol et au focus (300 ms, texte court, jamais de contenu essentiel ni d'action)."
  },
  {
    "nom": "SignDialog",
    "groupe": "Dialogues",
    "hauteur": 600,
    "largeur": 900,
    "description": "Confirmation avec réauthentification forte FIDO2 (`confirm: \"sign\"`) pour les quitus, clôtures, scellements, purges, transitions de statut et relèves de quart : rappel de l'objet, empreinte de ce qui sera signé (Hash), SignatureField, résultat horodaté. Le dialogue ne peut être fermé pendant la signature ; à l'issue, le résultat (jeton d'horodatage, référence d'audit) est affiché avant de continuer. Le client n'accède jamais à un jeton ; la vérification est faite par le service."
  },
  {
    "nom": "CrudDialog",
    "groupe": "Dialogues",
    "hauteur": 520,
    "largeur": 900,
    "description": "Modale CRUD standard (D-63) : toute création, modification, suppression, action sensible ou lecture de détail déclenchée depuis une fiche ou une liste s'ouvre dans cette fenêtre, sans quitter l'écran, adaptée à son contexte. Assemblage de Dialog (voile, focus piégé, Échap, `aria-modal`, titre, sous-titre de contexte et description), d'un formulaire natif et d'un pied d'actions standard : « Annuler » et l'action principale, reliée au formulaire par l'attribut `form` (Entrée dans un champ soumet), avec état de chargement.\n\n**Contextes** (`context`) : `create` (large, pictogramme « + ») ; `edit` (large) ; `delete` (moyenne, mise en garde « action définitive », case « irréversible », bouton danger, `alertdialog`) ; `sensitive` (moyenne, motif obligatoire de 10 caractères, réauthentification FIDO2 annoncée avec `reauth`, `alertdialog`) ; `confirm` (moyenne, `alertdialog`) ; `detail` (lecture seule, seul « Fermer » au pied). Tailles `sm` (440 px), `md` (560 px), `lg` (720 px).\n\n**Garde-fous** : pendant l'envoi (`submitting`), ni Échap, ni le voile, ni « Annuler » ne ferment ; un formulaire modifié (`dirty`) demande de confirmer l'abandon de la saisie. Le refus du service s'affiche en tête du corps (InlineAlert critique avec l'identifiant de corrélation). ConfirmDialog et SignDialog en sont des spécialisations ; ResourceForm s'y présente avec `presentation=\"dialog\"`.\n\n**Le consommateur fournit** `context`, `title`, `subject?`, `description?`, `onSubmit(motif)`, `onClose`, `submitLabel` (verbe et objet), `submitIcon?`, `submitting?`, `submitDisabled?` avec `submitDisabledReason`, `dirty?`, `error?`, `object?`, `consequences?`, `reason?`, `irreversible?`, `reauth?`, `danger?`, `extraActions?`, `size?`, `children` (champs du catalogue).\n\n**Interdits** : une saisie de modification en pleine page depuis une fiche ; un bouton principal « OK » ; plus d'un bouton principal ; une suppression sans contexte `delete`."
  },
  {
    "nom": "AuditProofViewer",
    "groupe": "Données",
    "hauteur": 560,
    "largeur": null,
    "description": "Visualisation d'une preuve d'inclusion du registre d'audit (CON-04, gabarit G9) : feuille (événement), chemin d'audit niveau par niveau (côté gauche ou droit), racine de période, jeton d'horodatage, verdict de vérification avec identifiant de corrélation, téléchargement de la preuve au format ouvert. La vérification est recalculée par le service ; l'interface affiche les empreintes (Hash tronqué, copie) et le verdict."
  },
  {
    "nom": "AgendaGrid",
    "groupe": "Données",
    "hauteur": 640,
    "largeur": 1100,
    "description": "Grille d'agenda (D-113) : vues jour, semaine et mois. Colonne des heures, ligne « toute la journée », événements placés à leur heure, chevauchements côte à côte, ligne de l'heure actuelle ; une case libre crée un événement à cette heure. Mois : trois événements par jour puis « + n autres », jours hors du mois atténués. Chaque catégorie prend une teinte de la palette de données portée par une pastille, toujours doublée de son libellé ; une invitation en attente est en contour pointillé.\n\n**Le consommateur fournit** `vue`, `jours`, `evenements` (`{ id, titre, debut, fin, journeeEntiere?, categorie, couleur, lieu?, enAttente? }`), `aujourdhui`, `maintenant?`, `heureDebut?`, `heureFin?`, les fonctions de fuseau `jourDe`, `minutesDe`, `heure`, `libelleJour`, et `onOuvrir`, `onCreer?`, `onVoirJour?`."
  },
  {
    "nom": "CalendarGrid",
    "groupe": "Données",
    "hauteur": 420,
    "largeur": 1180,
    "description": "Grille d'astreinte hebdomadaire (DES-18) : jours × plages (jour, soir, nuit), cases par opérateur avec chef de quart marqué, plages non couvertes signalées (fond `gm-color-status-warn-bg` avec pictogramme et libellé), demandes d'échange en contour pointillé, paramètres de génération visibles (repos, nuits consécutives, effectif), publication par le chef du DES (SignDialog). Vue tableau équivalente : la grille est elle-même un tableau accessible."
  },
  {
    "nom": "ChartCard",
    "groupe": "Données",
    "hauteur": 760,
    "largeur": 980,
    "description": "Carte de graphique (cockpits, tableaux de bord) : courbe, barres ou jauge en SVG, palette de données `gm-data-1` à `gm-data-8` dans l'ordre fixe (validée pour la vision des couleurs en clair et en sombre), légende toujours présente dès deux séries, étiquettes directes en fin de courbe, seuil en pointillé critique, infobulle au survol (réticule sur les courbes, par barre sur les barres), onglet **Tableau** équivalent, export contrôlé. Une seule échelle par graphique, jamais deux axes. Le texte des graphiques reste en encre (jamais dans la couleur de série) ; les statuts sur un graphique passent par StatusBadge.\n\n**Le consommateur fournit** `title`, `subtitle?`, `type` (`line | bar | gauge`), `labels`, `series` (`{ name, data[] }`, au plus huit), `unit?`, `threshold?`, `min?` et `max?` (domaine : une courbe de disponibilité part de 90 %, pas de 0), `status?`, `width?`, `height?`."
  },
  {
    "nom": "SerieTemporelle",
    "groupe": "Données",
    "hauteur": 1200,
    "largeur": 980,
    "description": "Courbe temporelle des métriques (supervision, tableaux de bord) : une seule échelle, axe du temps en heures ou en dates selon l'étendue, ou en jours ou en mois pour les comptes des tableaux de bord (`granularite`), largeur adaptative, trou pour une mesure absente. Quatre séries d'identité au plus (`gm-serie-1` à `gm-serie-4`, ordre fixe validé pour la vision des couleurs), les suivantes en gris et regroupées en « Autres (n) » dans la légende. Réticule et infobulle listant toutes les séries à l'instant visé, à la souris comme au clavier (flèches, Début, Fin, Échap) ; seuil ; lavis d'aire pour une série unique ; légende dès deux séries ; onglet **Tableau** équivalent ; rechargement sans flash (image précédente atténuée). L'export n'apparaît que si `onExporter` est fourni. Les filtres de période se placent au-dessus des graphiques, jamais dans la carte.\n\n**Le consommateur fournit** `title`, `subtitle?`, `series` (`{ nom, points: { t, v }[] }`), `unit?`, `format?`, `min?`, `max?`, `seuil?`, `aire?`, `hauteur?`, `status?`, `chargement?`, `rechargement?`, `vide?`, `onExporter?`, `actions?`, `granularite?` (`instant` par défaut, `jour`, `mois`)."
  },
  {
    "nom": "BarresHorizontales",
    "groupe": "Données",
    "hauteur": 420,
    "largeur": 980,
    "description": "Barres horizontales de répartition (comptes par profil, liaisons par étape, causes d'anomalies) : une seule teinte (comparaison de grandeurs, pas d'identités), barres de 20 px à bout arrondi côté valeur, carrées sur la ligne de base, valeur au bout de la barre, survol par barre, libellés longs à gauche. Tri décroissant par défaut ou ordre fourni (étapes d'un parcours) ; au-delà de `maximum`, les plus petites catégories sont regroupées en « Autres (n) ». Une catégorie peut ouvrir la liste filtrée (`onClick`). Onglet **Tableau** équivalent.\n\n**Le consommateur fournit** `title`, `subtitle?`, `elements` (`{ libelle, valeur, cle?, onClick? }`), `unit?`, `format?`, `maximum?`, `trier?`, `vide?`, `actions?`."
  },
  {
    "nom": "DagView",
    "groupe": "Données",
    "hauteur": 620,
    "largeur": 1100,
    "description": "Graphe orienté acyclique des opérations d'une liaison (gabarit G6) : nœuds OperationNode disposés par phase, liens orthogonaux, chemin critique en `gm-color-action`, états (éligible, verrouillée, close, hors périmètre, en retard), zoom, et **vue liste équivalente** obligatoire (même contenu en DataTable). Le graphe est un `role=\"img\"` décrit ; toute l'information est disponible dans la liste. La disposition (phases, dépendances) vient du référentiel d'opérations."
  },
  {
    "nom": "DataTable",
    "groupe": "Données",
    "hauteur": 420,
    "largeur": 1040,
    "description": "Le composant central des listes (gabarit G2, ResourceList). Anatomie : barre de filtres persistante, barre d'outils (sélection, actions groupées, densité, colonnes, export contrôlé), en-tête sur fond `gm-color-action` en texte inverse avec tri par colonne, lignes de 44 px (36 px en densité compacte) alternées sur `gm-color-surface-sunken`, colonne d'actions à droite (boutons ghost ou menu), pagination serveur. Types de colonnes : `text`, `mono`, `number`, `money`, `date`, `datetime`, `status`, `severity`, `progress`, `tags`, `link`, `actions`, `custom`. Colonnes numériques alignées à droite en chiffres tabulaires.\n\n**Quatre états** via `state` : `loading` (squelette), `empty` (EmptyState avec cause), `error` (InlineAlert avec identifiant de corrélation et réessai), `ok`. `liveUpdates` surligne 1 200 ms les lignes mises à jour par le canal poussé. Les filtres sont dans l'URL et mémorisés par écran et par utilisateur."
  },
  {
    "nom": "DiffView",
    "groupe": "Données",
    "hauteur": 360,
    "largeur": 1100,
    "description": "Comparaison de deux versions (référentiel d'opérations, règle, rôle, route, matrice) en côte à côte ou unifié : lignes ajoutées (fond de statut nominal), retirées (fond critique, barrées), modifiées (fond à surveiller), numéros de ligne, chasse fixe. Toujours accompagnée d'une légende textuelle ; la couleur ne porte jamais seule la différence."
  },
  {
    "nom": "FilterBar",
    "groupe": "Données",
    "hauteur": 170,
    "largeur": 1080,
    "description": "Barre de filtres persistante en tête d'une liste (gabarit G2) : recherche, filtres typés (sélection, statut, institution, service, plage de dates), « réinitialiser », enregistrement de filtres favoris, puces des filtres actifs retirables. Les filtres sont dans l'URL et mémorisés par écran et par utilisateur. Alimentée par les champs `filterable` du descripteur de ressource."
  },
  {
    "nom": "GanttView",
    "groupe": "Données",
    "hauteur": 470,
    "largeur": 1180,
    "description": "Jalons et chemin critique d'un projet (DES-24) : barre par jalon avec avancement réalisé, chemin critique en contour critique, marge hachurée, ligne « aujourd'hui », probabilité de tenue calculée par le service (en rouge sous 50 %). Vue tableau équivalente obligatoire. Aucun dégradé, aucune ombre ; les barres sont des aplats `gm-color-action`."
  },
  {
    "nom": "KanbanBoard",
    "groupe": "Données",
    "hauteur": 680,
    "largeur": 1260,
    "description": "Tableau Kanban (DES-23) : colonnes avec limite d'en-cours affichée (« 4/5 »), cartes TicketCard glissables et déplaçables au clavier, refus au-delà de la limite avec demande de dérogation (motif, approbateur distinct, 72 h). Une colonne en dépassement porte une bordure critique et un avis ; le bouton d'ajout est désactivé avec la raison. Vocabulaire imposé : tableau, tâche, colonne, limite d'en-cours."
  },
  {
    "nom": "RiskMatrix",
    "groupe": "Données",
    "hauteur": 420,
    "largeur": 1100,
    "description": "Matrice des risques 5 × 5 (probabilité × impact) d'un projet ou du portefeuille (DES-24) : trois vues (brut, maîtrise, résiduel), risques positionnés par leur clé, zone (faible, modéré, élevé, critique) portée par le libellé de la cellule et par StatusBadge dans la liste, jamais par un aplat de fond. Vue tableau équivalente. La cotation vient du service."
  },
  {
    "nom": "StateMachineBar",
    "groupe": "Données",
    "hauteur": 72,
    "largeur": null,
    "description": "Barre d'états d'un ticket, d'un suspens, d'un dossier ou d'une institution : états parcourus, état courant (fond `gm-color-action`), transitions permises (texte action) renvoyées par le service dans `transitionsPermises` ; l'interface ne les recalcule jamais."
  },
  {
    "nom": "TreeView",
    "groupe": "Données",
    "hauteur": 340,
    "largeur": null,
    "description": "Arbre accessible (`role=\"tree\"`, `treeitem`, `group`, niveaux annoncés) pour les référentiels d'opérations (tronc commun et branches par type de projet), les rôles et permissions, les catalogues de types de pièces (gabarit G5, volet gauche). Nœud avec pictogramme, code en chasse fixe, libellé, compteur ou StatusBadge ; sélection et dépliage au clavier."
  },
  {
    "nom": "WallBoard",
    "groupe": "Données",
    "hauteur": 440,
    "largeur": 1180,
    "description": "Tableau mural des liaisons supervisées (gabarit G7, DES-17, 2560 px) : tuiles d'état par liaison (référence, nom, dernière mesure en `gm-type-stat-wall`, alertes), filet supérieur de statut, aucune interaction, rafraîchissement poussé (`aria-live=\"polite\"`), mode sombre par défaut. Le statut est toujours doublé d'un libellé."
  },
  {
    "nom": "SideNav",
    "groupe": "Enveloppe",
    "hauteur": 576,
    "largeur": 320,
    "description": "Navigation latérale de 264 px, repliable à 64 px (§6.2) : fond uni (bleu institutionnel en clair, gris anthracite du thème en sombre : `gm-color-nav-fond`), aucun dégradé ; logo GIMAC seul en blanc, sans drapeaux, sans cartouche ni nom de couche (D-66). Entrées par module avec pictogramme et libellé ; entrée active marquée par une teinte de fond (`gm-color-nav-actif`) et un texte et un pictogramme blancs, sans filet vertical (D-66) ; survol et focus en blanc. Les entrées non autorisées sont absentes du DOM, pas grisées. Sous 1280 px, elle devient un tiroir. Dans la coquille applicative, elle occupe toute la hauteur disponible et défile seule."
  },
  {
    "nom": "TopBar",
    "groupe": "Enveloppe",
    "hauteur": 200,
    "largeur": 1120,
    "description": "Barre supérieure de 56 px, fixe, commune aux trois couches (§6.1) : logo (couleur en clair, blanc en sombre), recherche globale Ctrl+K (pas sur le portail), horloge locale (UTC au survol), indicateur du canal poussé (point vert ou gris avec libellé), notifications, bouton « Préférences » (globe) ouvrant un menu surgissant ancré (langue FR, EN, ES et thème ; Échap, clic extérieur, focus rendu au bouton) sans déformer la page, menu utilisateur ancré à l'avatar (photo ou initiales, chevron) : en-tête avec avatar de 40 px, nom, fonction (et institution sur le portail), puis les entrées fournies (Mon compte, Se déconnecter) ; Échap, clic extérieur, focus rendu ; les deux menus surgissants s'excluent. Ni nom de couche, ni bandeau d'environnement (D-66)."
  },
  {
    "nom": "AlertRow",
    "groupe": "Domaine",
    "hauteur": 160,
    "largeur": 1080,
    "description": "Ligne d'alerte de supervision (DES-16, tableau de bord) : référence, sévérité, liaison, message et horodatage, délai d'acquittement restant (décompte, en rouge sous 5 minutes), actions « Acquitter » (ConfirmDialog court) et « Ouvrir un ticket » pré-rempli. Le filet gauche prend la couleur de statut ; le fond reste `gm-color-surface`."
  },
  {
    "nom": "Messagerie",
    "groupe": "Domaine",
    "hauteur": 480,
    "largeur": null,
    "description": "Messagerie de travail entre acteurs du GIMAC (D-64) : ConversationList (la plus récente en tête, non-lus en pastille, conversation ouverte marquée), MessageThread (messages du plus ancien au plus récent, « Vous » pour les siens, étiquettes de liaison en liens, pièces jointes téléchargeables, messages plus anciens à la demande) et MessageComposer (Entrée pour envoyer, Maj + Entrée pour aller à la ligne, cinq pièces au plus, retirables).\n\n**Photos et fond du fil (docs/lot1/mon-compte.md, D-66)** : avatar (photo ou initiales) de l'interlocuteur d'un échange direct (`avatar` de ConversationItem) et de l'auteur de chaque message (`avatar` de ThreadMessage) ; la zone des messages (et l'état vide de la page) porte le motif « trame » de §5.6 (classe `gm-trame-fil`, masque `assets/motif/trame-masque.svg`, jeton `gm-color-motif-fil` : brand-accent à 6 %, 7 % en sombre), décoratif, retiré sous `prefers-reduced-transparency` et en couleurs forcées ; bulles, ligne d'auteur, en-tête, saisie et liste des conversations restent sur fond plein : aucun texte posé sur le motif."
  },
  {
    "nom": "ChatPanel",
    "groupe": "Domaine",
    "hauteur": 460,
    "largeur": null,
    "description": "Salon de messagerie chiffrée (DES-29, cellule de crise) : liste des messages avec accusés (remis, lu), pièces jointes chiffrées, mentions, indicateur de terminal vérifié. L'envoi vers un salon comptant un terminal non vérifié est refusé et expliqué. Aucun message n'est conservé côté client hors de la session ; le salon d'une crise est archivé et scellé à la clôture."
  },
  {
    "nom": "ConferenceControls",
    "groupe": "Domaine",
    "hauteur": 100,
    "largeur": 1080,
    "description": "Commandes de la conférence audio et vidéo locale (cellule de crise, salons) : micro, caméra, partage d'écran, nombre de participants, consentement à l'enregistrement (l'enregistrement n'est possible qu'après consentement recueilli et est signalé en permanence), quitter. Un contrôle coupé passe en variante `danger` avec libellé explicite : jamais la couleur seule."
  },
  {
    "nom": "CrisisRoomHeader",
    "groupe": "Domaine",
    "hauteur": 200,
    "largeur": 1080,
    "description": "En-tête de la salle de crise (DES-20, gabarit G8) : référence, niveau, ouverture, titre, quorum atteint ou non, membres attendus avec acquittement de convocation (et suppléance), accès au salon chiffré et à la conférence. Le filet gauche critique est le seul usage du rouge ; le fond reste `gm-color-surface`."
  },
  {
    "nom": "DecisionToTaskRow",
    "groupe": "Domaine",
    "hauteur": 180,
    "largeur": 1080,
    "description": "Ligne de traçabilité d'une réunion (DES-25) : décision consignée, tâche engendrée (ou bouton de création), responsable, échéance, état. Le taux d'exécution des décisions est calculé par le service. Une décision sans tâche est signalée ; la tâche créée apparaît dans le Kanban avec son origine typée."
  },
  {
    "nom": "DeviceVerificationCard",
    "groupe": "Domaine",
    "hauteur": 380,
    "largeur": 920,
    "description": "Carte d'un terminal (profil DES-02, console CON-09) : nom, identifiant, empreinte de clé (Hash), dernière activité, plateforme, état de vérification (vérifié, vérification en cours, non vérifié, révoqué), saisie du code court (PinField) pendant la vérification, révocation avec ConfirmDialog. Le terminal d'archivage de la console est présenté avec la même carte."
  },
  {
    "nom": "FourElementsChecklist",
    "groupe": "Domaine",
    "hauteur": 320,
    "largeur": null,
    "description": "Liste de contrôle de la règle des quatre éléments à la clôture d'une opération (écran DES-10) : responsable, preuve, horodatage, validation, chacun avec son état ; l'élément manquant indique l'action attendue. La clôture n'est possible que lorsque les quatre sont présents et que le validateur est distinct du responsable (le bouton est alors absent, avec rappel de la règle). La signature passe par SignDialog."
  },
  {
    "nom": "InstitutionHeader",
    "groupe": "Domaine",
    "hauteur": 160,
    "largeur": 1040,
    "description": "En-tête de la fiche d'une institution membre (gabarit G3) et de l'espace de l'institution sur le portail : code participant en chasse fixe, catégorie (Tag), pays, dénomination en `gm-type-display` (l'unique h1), sigle, statut (StatusBadge, dictionnaire à six états : candidat, en homologation, actif, à régulariser, suspendu, retiré), conformité documentaire, mode d'adhésion, actions permises par le rôle (au plus une principale, deux secondaires, le reste dans « Plus »). Composé de Tag, StatusBadge et Button."
  },
  {
    "nom": "LiaisonProgress",
    "groupe": "Domaine",
    "hauteur": 300,
    "largeur": null,
    "description": "Avancement d'une liaison (fiche de liaison, portail) : deux ProgressRing (certification, mise en service), phase courante et version du référentiel d'opérations, CoverageBar par service souscrit (inclus, en essai, certifié, en service). Les pourcentages sont calculés par le service d'homologation, jamais côté client."
  },
  {
    "nom": "LogbookEntryList",
    "groupe": "Domaine",
    "hauteur": 340,
    "largeur": null,
    "description": "Main courante chaînée d'une vacation ou d'une cellule de crise (gabarit G8) : entrées horodatées avec acteur (rôle), type, texte et empreinte de chaînage (chaque entrée scelle la précédente) ; saisie en bas, sans modification ni suppression possibles. Les entrées automatiques (acquittement, transition) apparaissent avec leur type."
  },
  {
    "nom": "OperationNode",
    "groupe": "Domaine",
    "hauteur": 200,
    "largeur": 1060,
    "description": "Nœud du graphe d'une liaison (DagView) : code en chasse fixe, libellé, à qui l'opération incombe, état (éligible, verrouillée, close, hors périmètre, en retard), et les quatre éléments de la règle des quatre éléments sous forme de quatre pictogrammes (responsable, preuve, horodatage, validation) chacun présent ou manquant. Un filet gauche `gm-color-action` marque le chemin critique. La vue liste équivalente du graphe reprend les mêmes colonnes."
  },
  {
    "nom": "PdcaCard",
    "groupe": "Domaine",
    "hauteur": 340,
    "largeur": null,
    "description": "Fiche d'action PDCA (cockpits, projets) ouverte depuis un indicateur dégradé : phase courante (planifier, réaliser, vérifier, agir), gardes de passage (une phase ne se ferme pas sans son livrable), responsable, échéance. Le passage à la phase suivante est bloqué tant que la garde n'est pas levée, avec l'explication en infobulle du bouton."
  },
  {
    "nom": "PolicyMatrix",
    "groupe": "Domaine",
    "hauteur": 300,
    "largeur": 940,
    "description": "Matrice rôles × permissions par domaine (console CON-01) : une ligne par permission (clé en chasse fixe et libellé), une colonne par rôle, cellule accordée ou refusée toujours accompagnée d'un nom accessible ; une modification en attente d'approbation à quatre yeux est marquée (sablier) jusqu'à la signature d'un second administrateur. Les gardes d'interface masquent, le serveur refuse."
  },
  {
    "nom": "QuitusCircuit",
    "groupe": "Domaine",
    "hauteur": 240,
    "largeur": null,
    "description": "Circuit des quitus d'un dossier de mise en service (DES-11) dans l'ordre imposé : technique, puis sécurité et conformité, puis direction générale ; pour chacun le signataire (rôle), l'état (à venir, en attente de signature, signé, refusé), l'horodatage et le motif d'un refus. Un quitus ne peut être signé avant le précédent ; la signature passe par SignDialog avec l'empreinte du socle d'évaluation."
  },
  {
    "nom": "ReconciliationSummary",
    "groupe": "Domaine",
    "hauteur": 380,
    "largeur": 940,
    "description": "Résumé d'une journée de rapprochement (DES-21) : journée, institution, état, états importés par source (avec nombre de lignes et empreinte de l'import), écarts par typologie avec nombre, montant et état. L'import se fait en Stepper (source, institution, journée, fichier, contrôle de format et de masquage, empreinte). Aucun calcul n'est fait côté client."
  },
  {
    "nom": "RootPublicationCard",
    "groupe": "Domaine",
    "hauteur": 340,
    "largeur": null,
    "description": "Racine de période du registre d'audit (console CON-04, gabarit G9) : racine, jeton d'horodatage, référence de publication, nombre d'événements couverts, date ; actions « Vérifier la racine » (verdict affiché en StatusBadge) et « Télécharger la preuve ». AuditProofViewer (à venir) présente la preuve d'inclusion d'un événement (feuille, chemin, racine, jeton, verdict)."
  },
  {
    "nom": "SeverityMatrix",
    "groupe": "Domaine",
    "hauteur": 360,
    "largeur": null,
    "description": "Matrice impact × urgence 5 × 5 pour la qualification d'un incident (DES-14). La sévérité S1 à S4 de chaque cellule est définie par le référentiel (console CON-07) et calculée par le service ; l'interface affiche la cellule sélectionnée et le marqueur réglementaire. La couleur n'est portée que par la puce de sévérité, jamais par le fond des cellules."
  },
  {
    "nom": "ShiftHandoverPanel",
    "groupe": "Domaine",
    "hauteur": 420,
    "largeur": null,
    "description": "Rapport de relève de quart (DES-19) : contenu généré depuis la main courante, signature sortante, contresignature entrante, visa du chef du DES, horodatage, dépôt scellé. La responsabilité n'est transférée qu'à la contresignature entrante ; l'InlineAlert le rappelle. Chaque signature passe par SignDialog."
  },
  {
    "nom": "SlaClock",
    "groupe": "Domaine",
    "hauteur": 120,
    "largeur": 1080,
    "description": "Horloge d'engagement d'un incident : délai imputable écoulé, engagement, gel en attente de tiers (le délai ne court pas), dépassement. Le statut est porté par StatusBadge (dans l'engagement, échéance proche à 80 %, dépassé, gelé) ; la barre reprend `gm-color-action` et `gm-color-status-crit-fg` en dépassement. Les durées viennent du service, en minutes."
  },
  {
    "nom": "SuspensCard",
    "groupe": "Domaine",
    "hauteur": 340,
    "largeur": null,
    "description": "Carte d'un suspens de rapprochement (DES-22, portail POR-05) : référence, typologie, ancienneté J+1, J+5, J+30 en StatusBadge, état de l'automate à six états, journée, institution, identifiants pivots en chasse fixe, montant, responsable, attestation déposée. La clôture n'est possible que sur attestation déposée puis validée par un second acteur (SignDialog, bouton absent pour le déposant). Le rappel « la plateforme ne réinjecte rien » est permanent."
  },
  {
    "nom": "TicketCard",
    "groupe": "Domaine",
    "hauteur": 220,
    "largeur": 920,
    "description": "Carte de ticket pour le Kanban et les listes en cartes du portail : référence en chasse fixe, sévérité (incident) ou type (demande), objet, institution, état (StatusBadge) et échéance d'engagement (en rouge et gras si dépassée). Glissable et déplaçable au clavier dans KanbanBoard. TicketStateBar est l'automate à sept états du ticket (nouveau, qualifié, en cours, en attente de tiers, résolu, clos, rouvert) ; les transitions permises viennent du service."
  },
  {
    "nom": "ResourceDelete",
    "groupe": "CRUD",
    "hauteur": 520,
    "largeur": 900,
    "description": "Suppression ou archivage universel : jamais de suppression physique d'un objet réglementaire ; quand le descripteur expose `archive` (ou n'expose pas `delete`), le dialogue archive et le dit. Motif obligatoire, conséquences listées, case d'irréversibilité pour une vraie suppression, signature (SignDialog) si l'action l'exige."
  },
  {
    "nom": "ResourceDetail",
    "groupe": "CRUD",
    "hauteur": 560,
    "largeur": 1100,
    "description": "Fiche à onglets universelle (gabarit G3) : en-tête (type, référence, titre, statut, actions permises), StateMachineBar depuis le dictionnaire de statuts et `transitionsPermises` renvoyées par le service, champs `inDetail` en KeyValueList, onglets du descripteur (chacun rend en général un `ResourceList` embarqué avec `fixedFilters`), chronologie, audit. En mode `drawer`, seule la partie détail est rendue dans le tiroir de ResourceList."
  },
  {
    "nom": "ResourceForm",
    "groupe": "CRUD",
    "hauteur": 760,
    "largeur": 980,
    "description": "Création ou modification universelle : champs `inForm` (`true`, `\"create\"` ou `\"edit\"`) rendus par `kind` avec les composants de saisie du catalogue, validation de format côté client, erreurs métier renvoyées par le service mappées sous les champs (avec message de synthèse en haut du formulaire et liens vers les champs), brouillon local jusqu'à soumission, découpage en étapes (Stepper) quand le formulaire dépasse 8 champs, confirmation ou signature selon l'action. Aucune règle métier côté client.\n\n**Le consommateur fournit** `descriptor`, `mode` (`create | edit`), `initial?`, `steps?` (`{ label, fields[] }`), `errors?` (champ → message), `serverError?`, `onSubmit`, `onCancel`."
  },
  {
    "nom": "ResourceList",
    "groupe": "CRUD",
    "hauteur": 760,
    "largeur": 1180,
    "description": "Composant universel de liste (gabarit G2) : lit un descripteur de ressource et rien d'autre. Compose le titre et les actions de page (une principale au plus), FilterBar (champs `filterable`), DataTable (champs `inList`, colonne d'actions de ligne selon `isAllowed` et les permissions), Drawer de détail (ResourceDetail en mode tiroir, chronologie, audit), ConfirmDialog ou SignDialog selon `confirm` de l'action, rafraîchissement poussé (`liveTopic`). Quatre états via `state`. Une page « Institutions membres » se réduit à `<ResourceList descriptor={institutionDescriptor} />`.\n\n**Le consommateur fournit** `descriptor`, `rows` et `total` (ou les hooks de données), `state`, `permissions?` (celles du jeton), `defaultQuery?`, `embedded?` (dans un onglet), `fixedFilters?` (par exemple `institutionId`)."
  },
  {
    "nom": "QrCode",
    "groupe": "Affichage",
    "hauteur": 220,
    "largeur": null,
    "description": "Code QR calculé dans le navigateur (D-65) pour configurer l'application d'authentification : URI `otpauth://` jamais envoyée à un service tiers (bibliothèque d'encodage embarquée), correction d'erreur M, zone de silence de quatre modules, un seul tracé SVG net à toute échelle. Modules sombres sur fond blanc dans les deux thèmes (la lecture par un téléphone l'exige) ; image nommée (`role=\"img\"`). Toujours accompagné du secret en clair pour la saisie manuelle.\n\n**Le consommateur fournit** `value`, `label`, `size?` (192 px par défaut)."
  },
  {
    "nom": "OneTimeSecret",
    "groupe": "Affichage",
    "hauteur": 200,
    "largeur": 560,
    "description": "Secret affiché une seule fois (mot de passe provisoire d'un compte, D-62), assemblage de Banner (avertissement `warn`), d'une puce `gm-chip` en chasse fixe et d'un Button de copie ; la confirmation de copie est annoncée (`role=status`).\n\n**Le consommateur fournit** `label` (nature du secret), `value`, `warning?` (avertissement propre), `onCopy?` (par défaut, presse-papiers du navigateur).\n\n**Interdits** : réafficher un secret déjà communiqué ; le stocker dans le navigateur."
  },
  {
    "nom": "AvatarCropper",
    "groupe": "Saisie",
    "hauteur": 420,
    "largeur": 620,
    "description": "Recadrage carré d'une photo de profil (docs/lot1/mon-compte.md §4), assemblage d'une image, d'un curseur de zoom et de Button : cadre carré à angles droits avec guide circulaire de l'avatar, image déplacée à la souris, au doigt ou au clavier (flèches, Maj pour aller plus vite ; + et − pour le zoom ; 0 recentre), aperçus ronds à 96, 40 et 24 px, bouton « Recentrer ». `recadrerImage` calcule l'image recadrée dans le navigateur (canevas, WebP sinon PNG) ; `zoneRecadree` donne la zone retenue.\n\n**Le consommateur fournit** `src` (adresse data: de l'image choisie), `label`, `value` (`zoom` de 1 à 4, `x` et `y` de −1 à 1), `onChange`, `size?` (240 px), `name?`.\n\n**Interdits** : voile ou ombre autour du cadre ; envoi de l'image d'origine sans recadrage."
  },
  {
    "nom": "PermissionPicker",
    "groupe": "Saisie",
    "hauteur": 260,
    "largeur": null,
    "description": "Composition d'un profil (console, D-62) : un CheckboxGroup par domaine de permissions (HOM, CER, PAR, IAM, PLATFORM), chacun avec « Tout sélectionner » ; un changement conserve les autres domaines. Compteur de la sélection annoncé.\n\n**Le consommateur fournit** `label`, `groups` (clé, libellé, options), `value`, `onChange`, `readOnly?`, `error?`.\n\n**Interdits** : proposer les permissions internes des services (INTERNAL_*)."
  },
  {
    "nom": "LiveStatGroup",
    "groupe": "Données",
    "hauteur": 220,
    "largeur": null,
    "description": "Indicateurs en temps réel (supervision de la plateforme, D-61) : StatCard alimentées par un flux SSE, état de la connexion (`connecting`, `live`, `lost`) en StatusBadge annoncé, valeurs dans une zone `aria-live` polie, heure de la dernière mise à jour.\n\n**Le consommateur fournit** `title`, `state`, `items` (propriétés de StatCard et `key`), `updatedAt?`, `level?`.\n\n**Interdits** : masquer la perte du flux ; animer les valeurs."
  },
  {
    "nom": "PageHeader",
    "groupe": "Enveloppe",
    "hauteur": 160,
    "largeur": null,
    "description": "En-tête d'écran (gabarits G1 à G3, §6.3, D-63) : à plat, filet inférieur de 1 px. À gauche, lien de retour éventuel, rubrique en texte secondaire, titre unique `h1` (`gm-type-display`), une ligne de description, ligne de contexte (référence, statut) ; à droite, alignés sur la ligne du titre, la personne connectée et ses profils en texte simple (« Nom · Profil »), puis les actions (une principale au plus).\n\n**Le consommateur fournit** `title`, `subtitle?`, `overline?`, `user?` (`name`, `roles?`), `meta?`, `actions?`, `back?`, `id?`.\n\n**Interdits** : plusieurs `h1` dans un écran ; un en-tête composé à la main ; avatar, puces ou formule d'accueil dans l'en-tête."
  },
  {
    "nom": "PageSection",
    "groupe": "Conteneurs",
    "hauteur": 220,
    "largeur": null,
    "description": "Section titrée d'un tableau de bord ou d'une fiche (D-58, D-63) : région nommée par son titre (`aria-labelledby`), titre `h2` (ou `h3`) et actions sur une ligne, contenu en colonne aux espacements des jetons.\n\n**Le consommateur fournit** `title`, `actions?` (lien « Voir tout »), `level?`, `children`."
  },
  {
    "nom": "StatGrid",
    "groupe": "Données",
    "hauteur": 200,
    "largeur": null,
    "description": "Grille d'indicateurs d'un tableau de bord par acteur (D-58, D-63) : StatCard de même hauteur, renvoyées à la ligne, dans un groupe nommé. Une carte cliquable ouvre la liste filtrée correspondante. Pour des valeurs poussées en temps réel, LiveStatGroup.\n\n**Le consommateur fournit** `label`, `items` (propriétés de StatCard et `key`).\n\n**Interdits** : calculer un compteur dans l'interface ; les valeurs viennent du service."
  },
  {
    "nom": "LinkList",
    "groupe": "Navigation",
    "hauteur": 160,
    "largeur": 560,
    "description": "Liste de liens vers des fiches (« Mes dossiers en cours », « Mes tâches », raccourcis ; D-63) : un Link par ligne, complément (référence, date) et StatusBadge alignés à droite, filet de 1 px entre les lignes ; pagination toujours visible (D-66), 5 lignes par page par défaut, côté client ou côté serveur (`total` et `onPage`) ; texte explicite quand elle est vide.\n\n**Le consommateur fournit** `label`, `items` (`key`, `href`, `label`, `meta?`, `status?`, `statusLabel?`), `empty?`, `pageSize?`, `page?`, `total?`, `onPage?`, `onPageSize?`, `pagination?`.\n\n**Interdits** : un lien qui déclenche une action à effet (c'est un Button) ; une liste longue sans pagination."
  },
  {
    "nom": "Note",
    "groupe": "Conteneurs",
    "hauteur": 120,
    "largeur": 560,
    "description": "Note d'aide (D-63) : explication, règle ou rappel posé dans une section, en cartouche (`gm-color-surface-sunken`, bordure de 1 px, angles droits) avec le pictogramme `info` ; jamais un paragraphe à nu, jamais une bande latérale. Pour un état ou un refus, InlineAlert.\n\n**Le consommateur fournit** `children`, `title?`, `icon?` (`null` pour aucun).\n\n**Interdits** : y porter un statut (c'est un StatusBadge ou une InlineAlert)."
  },
  {
    "nom": "SettingsGrid",
    "groupe": "Données",
    "hauteur": 220,
    "largeur": null,
    "description": "Grille de réglages en lecture (politique de sécurité, configuration ; D-63) : une cartouche par réglage (bordure de 1 px, angles droits), libellé discret, valeur mise en valeur avec son unité, aide (plancher, plage admise) en dessous ; état d'une valeur booléenne porté par le pictogramme de statut et le libellé (« activée »). Colonnes régulières.\n\n**Le consommateur fournit** `label`, `items` (`key`, `label`, `value`, `unit?`, `hint?`, `status?`).\n\n**Interdits** : y modifier une valeur (l'édition passe par CrudDialog)."
  },
  {
    "nom": "ActionBar",
    "groupe": "Actions",
    "hauteur": 96,
    "largeur": null,
    "description": "Rangée de boutons d'un pied de carte ou de formulaire, ou d'une barre de section (D-63) : alignement `end` (défaut), `between` ou `start` ; groupe nommé quand `label` est fourni.\n\n**Le consommateur fournit** `children` (Button), `align?`, `label?`.\n\n**Interdits** : plus d'un bouton `primary` dans la rangée."
  },
  {
    "nom": "ToastRegion",
    "groupe": "Retours",
    "hauteur": 120,
    "largeur": 560,
    "description": "Emplacement des retours d'action (Toast) : fixe en bas à droite de la fenêtre, au-dessus du contenu et sous les dialogues ; rien n'est rendu sans retour. L'aperçu montre le Toast qu'il accueille, avec son bouton de fermeture.\n\n**Le consommateur fournit** `children` (un ou plusieurs Toast).\n\n**Interdits** : un retour qui masque une décision à prendre (c'est un dialogue)."
  }
]
