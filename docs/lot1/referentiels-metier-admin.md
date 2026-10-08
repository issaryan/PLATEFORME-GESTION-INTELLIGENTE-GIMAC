# Administration des référentiels métier, tous domaines (D-92)

Demande de KOAGNE ISSA RYAN : le super administrateur et l'ingénieur chargé de l'homologation ont tous deux le
CRUD complet des référentiels métier de tous les domaines : tous les secteurs du GIMAC, chaque branche (unités,
services, pôles, cellules) et l'homologation.

## 1. Permission

`REF_METIER_ADMIN` (« Administration des référentiels métier, tous domaines ») : `ROLE_SUPER_ADMIN` et
`ROLE_INGENIEUR_HOMOLOGATION` seulement. Elle ne remplace pas les droits par unité : un chef garde, par la matrice
des pouvoirs (D-81, D-86 : `PROCESSUS_DEFINIR`), l'administration des processus de ses propres unités. Elle n'est
pas privilégiée au sens d'IAM (elle n'ouvre ni les comptes ni la plateforme).

## 2. Portée

| Référentiel | Avec `REF_METIER_ADMIN` |
|---|---|
| Processus d'unité (D-87), toutes les unités, actives ou non | lire tout ; créer pour n'importe quelle unité ; modifier, contrôler, publier (levée motivée), nouvelle version, retirer ; supprimer un brouillon ; lancer une instance ; suivre, suspendre, reprendre, annuler, réaffecter une instance |
| Référentiel d'homologation (D-77, D-89) | lecture complète ; créer, dériver, modifier, contrôler, publier, migrer, supprimer un brouillon, étapes, export |
| Types de projet et fonctions (D-91) | créer, modifier, activer, désactiver |

« Supprimer » s'applique aux brouillons ; une version publiée ne se supprime jamais (traçabilité, liaisons et
instances qui s'y rattachent) : elle se **retire**, ce qui l'enlève des choix sans effacer l'historique. Chaque
acte est journalisé avec l'acteur et le motif.

## 2 bis. Une fonction attribuable à tout rôle, pas un privilège figé

La règle n'est jamais écrite pour deux profils : tout se décide dans la configuration.
- **Composition des rôles (D-62, D-90)** : `REF_METIER_ADMIN` est un droit du catalogue des fonctionnalités
  (module « Référentiels métier », fonctionnalité « Administration de tous les domaines »). L'administration le
  coche ou le décoche pour n'importe quel profil, livré ou créé (D-81 §5, D-86), avec motif et clé de sécurité
  comme toute composition ; le super administrateur et l'ingénieur d'homologation le portent seulement **par
  défaut**. Un profil créé peut le recevoir dès sa création.
- **Matrice des pouvoirs (D-81, D-86 §4)** : nouvelle portée `TOUTES` (toutes les unités, actives ou non),
  admise seulement pour `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` (422 `ORG_PORTEE_NON_ADMISE` ailleurs). Une
  ligne de matrice à `TOUTES` produit, pour ces actions, le même effet que `REF_METIER_ADMIN` sur les processus
  d'unité. Ordre des portées : `AUCUNE` < `DIRECTS` < `EQUIPE` < `UNITES` < `TOUTES`.
- Les deux voies sont journalisées (composition : `CONFIGURATION_PROFIL_*` ; matrice : `MATRICE_POUVOIRS_MODIFIEE`).

## 3. Interface

Pour ces deux profils, le catalogue des processus d'unité propose toutes les unités (arbre des secteurs et
branches, filtre, recherche) et la création dans n'importe quelle unité ; la barre latérale de l'application
interne montre « Processus de l'unité » et « Référentiels d'opérations » ; un bandeau rappelle « Administration
de tous les domaines ».

## 4. Mise en œuvre

### 4.1 MS-PILOTAGE (processus d'unité)

Les droits sont calculés par `AccesProcessus` (ms-pilotage) à partir du seul jeton et de la matrice des
pouvoirs ; aucun nom de profil n'y figure. `IAM_PROFILE_EDIT` (administration des profils) n'ouvre plus les
processus de toutes les unités depuis D-94 (docs/lot1/ecrans-par-configuration.md §6).

| Droit | Règle de l'unité (inchangée) | Toutes les unités, actives ou non |
|---|---|---|
| Définir : créer, modifier, contrôler, publier (levée motivée), nouvelle version, retirer, supprimer un brouillon | `PROCESSUS_DEFINIR` à `UNITES` sur une unité dirigée | `REF_METIER_ADMIN` ou `PROCESSUS_DEFINIR` à `TOUTES` |
| Lancer une instance | membre de l'unité, ou `PROCESSUS_LANCER` à `UNITES` sur une unité dirigée | `REF_METIER_ADMIN` ou `PROCESSUS_LANCER` à `TOUTES` |
| Piloter une instance : réaffecter, suspendre, reprendre, annuler | chef de l'unité, ou `TACHE_CONFIER` couvrant l'unité | `REF_METIER_ADMIN` ou `PROCESSUS_LANCER` à `TOUTES` |
| Lire définitions et instances, catalogue | membres, chefs, porteurs des droits ci-dessus | l'un des droits « toutes les unités » ci-dessus |

- Portée `TOUTES` : ajoutée à l'énumération (`AUCUNE` < `DIRECTS` < `EQUIPE` < `UNITES` < `TOUTES`) ; une
  valeur inconnue reste ignorée (`AUCUNE`) ; `TOUTES` sur une autre action (refusée par MS-IAM) est ramenée à
  `UNITES`.
- Création : n'importe quelle unité existante ; unité inconnue : 422 `PIL_PROCESSUS_UNITE_INCONNUE`.
- Catalogue `GET /api/v1/processus` sans `unite` : toutes les unités pour un porteur d'un droit « toutes les
  unités » ; un chef reste limité à ses unités (403 `PIL_PROCESSUS_INTERDIT` hors de celles-ci) ;
  `HOM_REFERENTIEL_EDIT` seul n'ouvre aucune autre unité.
- Nouvelle route `GET /api/v1/processus/portee` (passerelle `/bff/des/pilotage/processus/portee`,
  `PIL_ACTIVITY_OWN`) :

```json
{
  "portee": "TOUS_DOMAINES",
  "definirToutesUnites": true,
  "lancerToutesUnites": true,
  "auTitreDe": ["REF_METIER_ADMIN"],
  "unites": [],
  "unitesDefinissables": []
}
```

  `portee` vaut `TOUS_DOMAINES` ou `MES_UNITES` ; `auTitreDe` peut contenir `REF_METIER_ADMIN`,
  `PROCESSUS_DEFINIR:TOUTES`, `PROCESSUS_LANCER:TOUTES` ; `unites` (unités lisibles) et
  `unitesDefinissables` (unités où créer) ne sont remplies qu'en `MES_UNITES`.
- `droits` d'une version : `modifier`, `publier`, `lancer`, `retirer`, plus `supprimer` (brouillon) et
  `nouvelleVersion` (version publiée ou retirée). `droits` d'une instance et `moi.reaffecter` des étapes
  reflètent le pilotage ouvert par `REF_METIER_ADMIN` ou `PROCESSUS_LANCER` à `TOUTES`.
- Journal : quand l'acte n'est permis que par un droit « toutes les unités », le détail de la ligne d'historique
  se termine par « (au titre de REF_METIER_ADMIN) », « (au titre de PROCESSUS_DEFINIR portée TOUTES) »
  ou « (au titre de PROCESSUS_LANCER portée TOUTES) » (création,
  modification, suppression de brouillon, publication, retrait, nouvelle version, lancement, réaffectation,
  suspension, reprise, annulation). L'acteur et le motif restent tracés comme avant.
- Organisation simulée : compte `SUPER_ADMIN` (sans unité, sans pouvoir de matrice, profil `ROLE_SUPER_ADMIN`) ;
  l'ingénieur d'homologation `AGENT` n'a aucun pouvoir de matrice.
- Tests : `AccesProcessusTest` (unitaire), `ProcessusReferentielsMetierIT` (super administrateur et ingénieur
  d'homologation sur le pôle réseau, chef limité, chef de département avec `HOM_REFERENTIEL_EDIT` seul, profil
  créé recevant `REF_METIER_ADMIN`, matrice à `TOUTES` sans la permission), `AdaptateursReelsTest` (lecture de
  `TOUTES` dans la réponse de MS-IAM).

### MS-IAM (matrice des pouvoirs, périmètre, composition des profils)

- Portée `TOUTES` : `Portee` (ms-iam) devient `AUCUNE` < `DIRECTS` < `EQUIPE` < `UNITES` < `TOUTES`. Elle n'est
  admise que pour `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` (`ActionPouvoir.admet`) : `PUT
  /api/v1/pouvoirs/matrice/{profil}` avec `TOUTES` sur une autre action répond 422 `ORG_PORTEE_NON_ADMISE`
  (`details.actions` : actions refusées ; `details.portees` : action vers portée), rien n'est enregistré ni
  journalisé. La base le garantit aussi (Liquibase `20261006200000_iam_portee_toutes.xml` : contrainte
  `ck_pouvoir_profil_portee` élargie, nouvelle contrainte `ck_pouvoir_profil_portee_toutes`). Aucune ligne
  n'est ajoutée par défaut : l'administration règle `TOUTES` profil par profil (journal
  `MATRICE_POUVOIRS_MODIFIEE`, par exemple « après: PROCESSUS_DEFINIR=TOUTES PROCESSUS_LANCER=TOUTES
  demandables= »).
- `GET /api/v1/pouvoirs/matrice` expose la liste ordonnée `portees` (avec `TOUTES`) et, nouveau,
  `porteesAdmises` : pour chaque action, les portées que l'interface peut proposer.

```json
{
  "actions": ["TACHE_CONFIER", "...", "PROCESSUS_DEFINIR", "PROCESSUS_LANCER"],
  "portees": ["AUCUNE", "DIRECTS", "EQUIPE", "UNITES", "TOUTES"],
  "porteesAdmises": {
    "TACHE_CONFIER": ["AUCUNE", "DIRECTS", "EQUIPE", "UNITES"],
    "PROCESSUS_DEFINIR": ["AUCUNE", "DIRECTS", "EQUIPE", "UNITES", "TOUTES"],
    "PROCESSUS_LANCER": ["AUCUNE", "DIRECTS", "EQUIPE", "UNITES", "TOUTES"]
  },
  "profils": [{ "profil": "ROLE_X", "libelle": "...", "privilegie": false, "cree": true,
                "actions": { "PROCESSUS_DEFINIR": "TOUTES", "PROCESSUS_LANCER": "TOUTES", "TACHE_CONFIER": "AUCUNE" },
                "profilsDemandables": [] }]
}
```

- `GET /api/v1/pouvoirs/moi` et `GET /api/v1/interne/pouvoirs/{id}` rendent `TOUTES` dans `actions` quand une
  ligne de la matrice le porte ; un compte qui cumule plusieurs profils reçoit, action par action, la portée la
  plus large (`TOUTES` l'emporte sur `UNITES`). `subordonnes` reste calculé avec la portée la plus large
  plafonnée à `UNITES` : `TOUTES` vise les processus d'unité, pas les personnes.
- `GET /api/v1/interne/organisation/{id}/perimetre?portee=TOUTES` : `unites` = toutes les unités du catalogue,
  actives ou non, triées par code ; `comptes` = tous les comptes internes actifs sauf le compte lui-même,
  niveau 0, triés par nom, 500 au plus (les suivants dans l'ordre des noms sont omis, comme pour les autres
  portées ; l'adaptateur Keycloak lit les comptes par pages de 200, sans profils ni second facteur).
- `REF_METIER_ADMIN` est une permission ordinaire : elle ne commence ni par `IAM_` ni par `PLATFORM_`, donc
  `ReglesProfils.privilegie` ne la compte pas et aucun droit d'octroi privilégié n'est exigé. MS-IAM ne tient
  pas de catalogue propre des permissions : les permissions connues et leurs libellés viennent du realm
  (`GET /api/v1/configuration/permissions`) ; seul l'annuaire de substitution (développement, tests) la
  déclare. Un profil créé peut la recevoir dès sa création (`POST /api/v1/configuration/profils`), la
  composition d'un profil livré peut l'ajouter ou la retirer (`PUT
  /api/v1/configuration/profils/{code}/permissions`, journal `CONFIGURATION_PROFIL_*`). Aucun nom de profil
  n'est codé dans MS-IAM.
- Interface : dans l'éditeur de la matrice, proposer pour chaque case les seules portées de
  `porteesAdmises[action]` (l'option « Toutes les unités » n'apparaît donc que pour `PROCESSUS_DEFINIR` et
  `PROCESSUS_LANCER`) et afficher le 422 `ORG_PORTEE_NON_ADMISE` s'il survient ; dans la composition d'un
  profil, `REF_METIER_ADMIN` se coche comme toute fonctionnalité (module « Référentiels métier »), sans
  avertissement de profil privilégié.
- Tests : `ReglesPouvoirsTest` (ordre, admission par action, refus 422, cumul), `PouvoirsServiceTest`
  (exposition, réglage admis et refusé, pouvoirs, cumul, périmètre `TOUTES` avec unité inactive, comptes
  inactifs et externes exclus, limite de 500), `DemandesEtProfilsServiceTest` (profil créé avec
  `REF_METIER_ADMIN` par un administrateur sans octroi privilégié, ajout et retrait sur un profil livré),
  `PouvoirsDemandesRoutesTest` et `OrganisationRoutesTest` (formes JSON, 422), `ReferentielsMetierAdminIT`
  (PostgreSQL réel : contrainte de la base, journal, pouvoirs, cumul, périmètre, composition).
