# Rôles composés par fonctionnalité (D-90)

Demande de KOAGNE ISSA RYAN : la matrice des rôles doit être configurable jusqu'aux fonctions du système ;
les droits sont empaquetés dans des rôles, et les rôles donnent accès aux fonctionnalités de la barre
latérale. Aujourd'hui un profil se compose de permissions brutes (codes `HOM_…`, `PIL_…`, D-62) et la barre
latérale suit déjà les permissions (D-79) ; D-90 rend cette composition lisible et pilotée par fonctionnalité.

## 1. Catalogue des fonctionnalités

Arborescence **Module > Fonctionnalité > Droit** : module (Certification, Participants, Mon activité,
Processus d'unité, Messagerie, Documents, Administration, Exploitation…), fonctionnalité = une entrée de la
barre latérale ou un écran (Référentiel des opérations, Liaisons, Tâches…), droit = action sur la
fonctionnalité (consulter, créer, modifier, valider, signer, publier, exporter, administrer) relié à **une
permission**. Source unique : le registre des fonctionnalités du frontend (`fonctionnalites.ts`, D-79),
complété des droits ; un script le vérifie contre les permissions du domaine (toute permission appartient à
au moins un droit du catalogue, sinon échec du contrôle de contrat). MS-IAM sert le catalogue
(`GET /configuration/fonctionnalites`, `IAM_ACCOUNT_READ`) depuis un fichier généré et versionné, avec les
libellés fr, en, es.

## 2. Composer un profil par fonctionnalité

L'écran « Profils et permissions » propose, pour un profil, l'arbre du catalogue avec une case par droit
(cocher une fonctionnalité coche « consulter » ; un droit d'écriture coche la consultation), un **aperçu de
la barre latérale** du profil, et la liste des permissions brutes en vue avancée. L'enregistrement envoie la
composition de permissions (route D-62 existante, motif et FIDO2) : aucune nouvelle règle serveur ; un droit
privilégié (`IAM_*`, `PLATFORM_*`) rend le profil privilégié (D-81). Comparaison de deux profils côte à côte.

**Réalisation de l'interface (frontend).** Tant que `GET /configuration/fonctionnalites` n'est pas servi, le
catalogue est **local** : `packages/lot1/src/droits.ts` dérive Module > Fonctionnalité > Droit du registre
`fonctionnalites.ts` (plus les écrans hors barre latérale : dossier de certification, quitus, attestations,
registre, portail), libellés fr, en, es dans le catalogue `droits.*`. Un test unitaire vérifie que chaque
permission connue de l'interface (hors `INTERNAL_*`) appartient à au moins un droit. La fiche d'un profil est une
grande modale à quatre onglets : **Fonctionnalités** (arbre à cases à trois états ; cocher une fonctionnalité
coche sa consultation, la décocher retire tous ses droits ; déplier montre chaque droit avec sa nature et sa
permission ; un droit d'écriture coche la consultation, retirer la consultation retire l'écriture ; aperçu de la
barre latérale avec les entrées ajoutées et retirées), **Droits** (permissions brutes par domaine, vue avancée),
**Pouvoirs** (ligne de la matrice, lien vers elle) et **Organisation** (titulaires, unité, n+1, lien vers
l'organigramme). L'enregistrement passe par la route de composition existante (motif et clé de sécurité).
« Comparer deux profils » présente les droits des deux profils côte à côte (option « Seulement les
différences ») et leurs barres latérales.

## 3. Profils, fonctions et matrice des pouvoirs

Les trois niveaux restent distincts et se configurent au même endroit : **droits** (ce qu'un profil peut faire
et voir, D-90), **pouvoirs** (sur qui, avec quelle portée, D-81), **organisation** (qui est chef de qui,
D-86). La fiche d'un profil montre les trois onglets.
