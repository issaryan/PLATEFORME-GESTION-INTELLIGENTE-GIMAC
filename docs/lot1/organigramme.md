# Organigramme : affecter des personnes, affichage ergonomique, PDF (D-102)

Demande de KOAGNE ISSA RYAN (09/10/2026) : « Dans l'organigramme on doit donc pouvoir également affecter des personnes à
des directions ou des services, et télécharger l'organigramme en PDF ou l'afficher de manière ergonomique (le PDF doit
aussi être très beau). »

Complète D-76 (unités, rattachement, `docs/lot1/mon-activite.md` §1), D-81 (chef d'unité, matrice des pouvoirs,
`docs/lot1/pouvoirs-hierarchiques.md`) et D-86 §3 (écran Organisation › Organigramme, `docs/lot1/profils-organisation.md`).
L'écran reste celui de D-86 (`/console/organigramme`, `/des/administration/organigramme`), enrichi ; aucun nouvel écran.

## 1. Décision

1. Une personne s'affecte explicitement à une unité (direction, département, service, pôle, cellule), seule ou à
   plusieurs, par une route dédiée de MS-IAM, motif obligatoire et journalisé ; le n+1 est proposé automatiquement.
2. L'organigramme s'affiche en organigramme descendant (traits de liaison, branches repliables, zoom, déplacement,
   recherche, plein écran, panneau de l'unité) ; la vue Liste reste ; sur petit écran, arbre vertical indenté.
3. L'organigramme se télécharge en PDF composé par la plateforme à partir de ses seules données (jamais d'un HTML fourni
   par le client), mis en page par le moteur Chromium de Gotenberg derrière MS-DOCUMENT.

## 2. Affecter des personnes à une unité (MS-IAM)

| Méthode | Chemin (MS-IAM, sous `/bff/{des,console}/iam`) | Droits |
|---|---|---|
| POST | `/api/v1/organisation/unites/{code}/affectations` | `IAM_ACCOUNT_EDIT`, ou chef (`PIL_ACTIVITY_OWN`, D-81) dans son périmètre |
| DELETE | `/api/v1/organisation/unites/{code}/affectations/{id}?motif=…` | `IAM_ACCOUNT_EDIT` |

**Corps de l'affectation** : `{ comptes: [id…], responsableId?: id | null, motif }`.
- `comptes` : 1 à 50 identifiants (doublons ignorés), sinon 400 `ORG_AFFECTATION_INVALIDE`.
- `motif` : obligatoire, 3 à 500 caractères après retrait des espaces, sinon 400 `IAM_REASON_REQUIRED`.
- `responsableId` **absent** : n+1 résolu pour chaque personne ; `null` : aucun n+1 ; identifiant : ce n+1 pour toutes,
  sauf pour la personne qui est elle-même ce n+1 (elle reçoit le n+1 résolu).
- **n+1 résolu** (`ReglesOrganisation.responsableParDefaut`) : le chef de l'unité ; si l'unité n'a pas de chef, ou si la
  personne est ce chef, le chef de l'unité parente la plus proche qui en a un et qui n'est pas la personne ; aucun au
  sommet.

**Contrôles, tous faits avant la première modification** (rien n'est modifié si une seule personne est refusée) :
- unité inconnue : 404 `ORG_UNITE_INTROUVABLE` ; unité inactive : 422 `ORG_UNITE_INCONNUE` ;
- par personne : compte inconnu (`IAM_ACCOUNT_NOT_FOUND`), compte d'institution (`ORG_COMPTE_EXTERNE`), compte désactivé
  (`ORG_COMPTE_INACTIF`), puis les règles habituelles du rattachement (`ORG_RESPONSABLE_INVALIDE`, `ORG_CYCLE`) ; tout
  refus donne **422 `ORG_AFFECTATION_REFUSEE`**, `details = { unite, refus: [{ id, code, message }] }` ;
- **chef** (l'appelant ne porte pas `IAM_ACCOUNT_EDIT`) : `PouvoirsService.verifierAffectationParChef`, mêmes règles que le
  rattachement d'un subordonné (`rattacherSubordonne`, D-81 §3) : portée de `RATTACHEMENT_MODIFIER` non nulle, unité
  cible parmi les unités qu'il dirige (sous-unités comprises), chaque personne dans le périmètre de cette portée, n+1
  visé égal au n+1 actuel, au chef ou à un compte couvert ; sinon 403 `ORG_HORS_PERIMETRE` ; lui-même : 403
  `IAM_SELF_ADMINISTRATION`.

**Réponse 200** : `[{ id, nomAffiche, avant: { unite, responsableId }, apres: { unite, responsableId } }]`, dans l'ordre
de la demande.

**Journal et événements** : chaque personne passe par `OrganisationService.rattacher` : entrée
`COMPTE_RATTACHEMENT_MODIFIE` (motif ; détails `avant: …, après: …, affectation=CODE`, plus
`pouvoir=RATTACHEMENT_MODIFIER portee=…` pour un chef) et événement `iam.account-administered.v1` sur le sujet
`iam.account`, une fois par personne, dans la même transaction (outbox).

**Retrait** (`DELETE …/affectations/{id}?motif=`, motif en paramètre comme la suppression d'un compte, mêmes bornes) :
l'unité de la personne devient nulle ; son n+1 est retiré s'il était celui que l'unité lui donnait par défaut, gardé
sinon. Personne non rattachée à cette unité : 409 `ORG_AFFECTATION_ABSENTE` ; personne chef de l'unité : 409
`ORG_CHEF_UNITE` (désigner d'abord un autre chef). Journal `COMPTE_RATTACHEMENT_MODIFIE` (`retrait=CODE`). Réponse : le
résultat de la personne. Le retrait reste réservé à l'administration (la règle D-81 du chef exige une unité cible).

## 3. Motif des gestes existants

Les gestes de l'organigramme transmettent désormais leur motif (l'écart assumé de `profils-organisation.md` §3 est levé) :
`PUT /comptes/{id}/rattachement` accepte `motif` (facultatif, 1000 caractères au plus) porté par
`COMPTE_RATTACHEMENT_MODIFIE` ; `POST /organisation/unites` et `PUT /organisation/unites/{code}` acceptent `motif`
(facultatif) porté par `CONFIGURATION_UNITE_ORGANISATIONNELLE`. Sans motif, rien ne change.

## 4. Organigramme en PDF

### 4.1 Route publique (MS-IAM)

`GET /api/v1/organisation/organigramme.pdf?racine=CODE&format=A4|A3&orientation=paysage|portrait&membres=true|false`
(sous `/bff/{des,console}/iam/organisation`), mêmes lecteurs que `GET /unites` : `PIL_ACTIVITY_OWN` ou `IAM_ACCOUNT_READ`.
- Valeurs par défaut : toute l'organisation, A3, paysage, sans annexe. Format ou orientation inconnus : 400
  `ORG_PDF_PARAMETRE` ; racine inconnue : 404 `ORG_UNITE_INTROUVABLE`.
- Réponse 200 `application/pdf`, `Content-Disposition: attachment; filename="organigramme-moabi-AAAA-MM-JJ.pdf"` (date
  de Douala), `Cache-Control: no-store`.
- Moteur absent, injoignable, saturé ou délai dépassé (90 s) : **503 `ORG_PDF_INDISPONIBLE`** (« Le moteur de mise en page
  PDF est momentanément indisponible ; réessayer dans quelques instants »). Adaptateur de substitution (développement
  sans MS-DOCUMENT, `gimac.adapters.rendu-pdf=stub`, valeur par défaut) : même 503, jamais de faux document.

### 4.2 Composition (MS-IAM, `DocumentOrganigramme`, `MiseEnPageArbre`)

Document HTML autonome construit à partir des données de MS-IAM (unités actives du périmètre, comptes internes actifs,
libellés des profils, photos des chefs) ; tout texte des données est échappé ; aucun script ; polices et images en data
URI (aucune ressource externe).
- **Page 1** : en-tête aux logos de Moabi (houppier et nom, `organigramme/moabi-logo.svg`) et du GIMAC
  (`organigramme/gimac-logo-couleur.png`), surtitre « Groupement Interbancaire Monétique de l'Afrique Centrale », titre
  « Organigramme », périmètre, nombre d'unités et de personnes, date d'édition (« Édité le 9 octobre 2026 à 14:32 (heure
  de Douala) »), double filet bleu et vert ; organigramme descendant vectoriel mis à l'échelle de la zone utile ; légende
  des types d'unité, du chef (photo ou initiales) et de l'effectif.
- **Cartes** : liseré haut à la couleur du type (direction `#15569E`, bleu GIMAC ; département `#1F80C9` ; service
  `#0B6938` ; pôle `#6A4FB8` ; cellule `#8A5A14`), type et effectif (personnes de l'unité et de ses sous-unités), libellé
  en Ubuntu, chef mis en avant (photo de profil ou initiales, nom, fonction ; « Chef non désigné » sinon). Mise en page :
  sous-unités côte à côte, centrées, traits coudés ; deux feuilles ou plus sous une même unité empilées en colonne (épine
  verticale) pour garder la largeur ; plusieurs directions côte à côte.
- **Pages de détail** : quand l'échelle de la vue d'ensemble descend sous 0,62 (organisation large, A4), chaque branche
  principale qui a des sous-unités a sa page.
- **Annexe** (`membres=true`) : « Membres par unité », une section par unité (chemin des unités parentes), tableau Nom
  (photo ou initiales, badge « Chef d'unité », chef en tête), Fonction · profils, n+1 ; « Sans unité » pour toute
  l'organisation ; deux colonnes en A3 paysage.
- **Pied de page** de chaque page : « Moabi · Organigramme · page x sur y » et « Document confidentiel · usage interne ».
- Polices de la charte embarquées : Ubuntu 500 et 700 (titres), Inter 400, 500 et 600 (texte), sous-ensembles latin et
  latin étendu (paquets @fontsource 5.3.0, licences OFL et UFL copiées dans `ms-iam/src/main/resources/organigramme/polices`) ;
  repli Liberation Sans puis DejaVu Sans. Marges : 12 mm, 16 mm en bas pour le pied de page.

### 4.3 Route interne et moteur (MS-DOCUMENT, Gotenberg)

MS-IAM n'appelle jamais le moteur directement : MS-DOCUMENT en reste le seul client (réseau `conversion` du Compose,
politique réseau Kubernetes `conversion-documents`).
- `POST /api/v1/interne/rendus/pdf` (MS-DOCUMENT), jeton de service portant **`INTERNAL_DOCUMENT_RENDER`** (client
  `iam-service`, `infra/keycloak/generer-realm-gimac.py`), jamais relayée par la passerelle ; corps JSON
  `{ html, piedDePage, format: A4|A3, orientation: PAYSAGE|PORTRAIT }`, 4 Mio au plus (413 `DOC_RENDU_TROP_VOLUMINEUX`) ;
  réponse `application/pdf`. Rien n'est conservé.
- **Contrôle du document** (`RenduPdfService`, défense en profondeur contre la falsification de requêtes côté serveur) :
  refus 400 `DOC_RENDU_INVALIDE` de tout élément actif ou de chargement (`script`, `iframe`, `frame`, `object`, `embed`,
  `link`, `base`, `portal`, `applet`, `meta http-equiv`, gestionnaires `on…=`, `javascript:`) et de toute adresse autre
  que `data:` ou une ancre dans `src`, `href`, `xlink:href`, `srcset`, `poster`, `action`, `formaction`, `background`,
  `url()` et `@import`. Moteur : refus 422 `DOC_RENDU_REFUSE`, indisponible 503 `DOC_RENDU_INDISPONIBLE`.
- **Gotenberg** `8.37.0` (`conversion-documents`, Compose et Kubernetes) : routes Chromium ouvertes avec
  `--chromium-disable-javascript=true`, `--chromium-allow-list=^file:///tmp/.*` (seuls ses fichiers temporaires),
  `--chromium-deny-private-ips=true`, `--chromium-deny-public-ips=true`, `--chromium-max-queue-size=10` ; le reste
  inchangé (moteurs PDF, webhooks, téléchargement d'URL désactivés, aucun accès sortant). Mémoire portée à 1,5 Go
  (Chromium et LibreOffice). Appel : `POST /forms/chromium/convert/html`, parties `index.html` et `footer.html`, papier et
  marges en pouces, `printBackground=true`.
- **Réglages** : MS-IAM `gimac.adapters.rendu-pdf` (`real` en production, `application-prod.yml`) et
  `gimac.services.document.url` (`GIMAC_SERVICES_DOCUMENT_URL`, `http://document:8083` par défaut en production, Compose et
  Kubernetes) ; MS-DOCUMENT réutilise `gimac.apercu.url`, `delai` et `delai-connexion` (D-83).

## 5. Interface

- **Organigramme descendant** : listes imbriquées dont les traits sont tracés en CSS ; carte d'unité (type et code au
  liseré de la couleur du type, libellé, chef avec photo ou initiales, effectif), bouton de menu (Affecter des personnes,
  Désigner le chef, Sous-unité, Renommer, Déplacer, Télécharger la branche en PDF ; lecteur : PDF seulement), bouton de
  repli sous chaque unité qui a des sous-unités (nombre de sous-unités masquées). Barre d'outils : recherche d'une
  personne ou d'une unité (résultats au clavier, Entrée prend le premier), zoom (30 à 160 %), ajuster à l'écran, tout
  replier, tout déplier, plein écran (Échap pour quitter). Zone à défilement dans les deux sens, déplacement en faisant
  glisser le fond. À l'ouverture : zoom ajusté sans descendre sous 75 %, racine centrée.
- **Recherche** : l'unité trouvée (ou celle de la personne) est dépliée, choisie, mise en évidence et amenée au centre ;
  la personne est mise en évidence dans le panneau ; annonce dans la région `aria-live`.
- **Panneau de l'unité** : actions (Affecter des personnes, Renommer, Déplacer, Sous-unité, Désactiver), code, type,
  parente, effectif, case « Chef d'unité » (dépôt d'un compte), membres en arbre des n+1 : photo, nom, fonction et profil,
  n+1, boutons Pouvoirs, n+1, Unité (changer d'unité), Retirer de l'unité (pas pour le chef).
- **Affecter des personnes** (grande modale) : recherche dans l'annuaire des comptes internes actifs (nom, identifiant,
  fonction ; unité actuelle affichée ; membres de l'unité non sélectionnables), cases à cocher (50 au plus), n+1 proposé
  automatiquement (nom et unité du chef annoncés), choisi ou aucun, motif obligatoire (10 caractères au moins, comme
  toute modale motivée) ; refus d'ensemble restitué personne par personne dans la modale.
- **Télécharger en PDF** (en-tête de l'écran et menu d'une carte) : périmètre (toute l'organisation ou une branche),
  format A3 ou A4, orientation, annexe des membres ; fichier `organigramme-moabi-AAAA-MM-JJ.pdf`.
- **Gestes existants conservés** (glisser-déposer, vue Liste au clavier, confirmation motivée) ; leur motif part au
  serveur (§3). Petit écran (720 px) : arbre vertical indenté, zoom masqué, panneaux sous l'organigramme. Clair et sombre :
  jetons `--gm-*` seulement (couleurs de type éclaircies en sombre).
- **Services simulés** : mêmes règles et mêmes refus que MS-IAM (affectation, retrait, chef limité aux unités qu'il
  dirige). Aucun moteur de mise en page en démonstration : le PDF téléchargé est un **exemple de démonstration** produit
  par la chaîne réelle (MS-IAM, MS-DOCUMENT, Gotenberg) sur le jeu de démonstration de la console, filigrane
  « DÉMONSTRATION » et pied de page « Démonstration · données fictives »
  (`frontend/packages/administration/src/assets/organigramme-demonstration.pdf`) ; la modale le dit. Il ne suit pas les
  changements faits pendant la démonstration. Régénération : `DocumentOrganigrammeTest.ecrireDemonstration` (MS-IAM) puis
  `ConvertisseurHtmlGotenbergReelTest` (MS-DOCUMENT), variables `GIMAC_ORGANIGRAMME_ESSAI_DOSSIER` et
  `GIMAC_APERCU_ESSAI_URL`.

## 6. Passerelle, OPA, domaine Keycloak

- Passerelle : la route `iam-organisation` couvre déjà les nouveaux chemins (aucune route ajoutée ; aucun délai global
  n'est imposé, la mise en page prend quelques secondes).
- OPA (`lot1_authz.rego`, copie Kubernetes par `infra/kubernetes-gimac/synchroniser-partages.sh`) : `POST
  /bff/console/iam/organisation/unites/{code}/affectations` `IAM_ACCOUNT_EDIT` ; règle `affectation_unite` pour le
  département (`IAM_ACCOUNT_EDIT` ou `PIL_ACTIVITY_OWN`, jamais un compte d'institution) ; `DELETE
  …/affectations/{id}` `IAM_ACCOUNT_EDIT` (département et console) ; `GET …/organigramme.pdf` `PIL_ACTIVITY_OWN`
  (département) ou `IAM_ACCOUNT_READ` (console) ; le portail n'a aucune règle.
- Domaine Keycloak : permission `INTERNAL_DOCUMENT_RENDER` donnée au compte de service `iam-service`
  (`gimac-realm.json`, `gimac-realm-production.json`, `domaine-gimac.json` régénérés).

## 7. Tests

- MS-IAM : `ReglesOrganisation.responsableParDefaut` et `AffectationsServiceTest` (affectation, n+1, refus d'ensemble,
  chef, retrait), `OrganigrammePdfServiceTest`, `DocumentOrganigrammeTest`, `MiseEnPageArbreTest`, `RenduPdfDocumentTest`
  (fausse route interne), `OrganisationRoutesTest` (droits, formes, 422 détaillé, 503), `IamRoutesSecurityTest`,
  intégration `OrganigrammeIT` (PostgreSQL, journal, outbox, PDF au port simulé).
- MS-DOCUMENT : `RenduPdfServiceTest` (contrôles), `ConvertisseurHtmlGotenbergTest` (faux moteur),
  `RenduInterneResourceTest` (jeton de service), `ConvertisseurHtmlGotenbergReelTest` (moteur réel, à la demande).
- Passerelle : `RoutesPresentationTest` ; OPA : `lot1_authz_test.rego` (affectations, retrait, PDF).
- Frontend : `Organigramme.test.tsx`, `services/simule/organigramme.test.ts`, `services/http/organisation.test.ts`,
  `ProfilsOrganisation.test.tsx` (motif transmis) ; bout en bout `frontend/e2e/specs/organigramme.spec.ts`.
- Captures `frontend/e2e/captures/d102/` : écran (vue d'ensemble, zoom ajusté, recherche, affectation, panneau des membres,
  plein écran, téléchargement, petit écran ; clair et sombre) et PDF réels (`pdf-a3-paysage-vue-ensemble`,
  `pdf-a3-paysage-annexe-membres`, `pdf-a4-paysage-branche-des`, `pdf-a4-portrait-detail-branche`, `pdf-demonstration`).

Compléments de la validation d'intégration (09/10/2026) :

- Accessibilité (audit axe-core, `accessibilite.spec.ts`, WCAG 2.2 AA) : le champ de recherche ne porte `aria-controls`
  que lorsque la liste des résultats est affichée (aucun `aria-expanded`, non admis sur un champ de recherche) ; les
  étiquettes de type d'unité passent au bleu 600 (département) et au violet éclairci en thème sombre (pôle), le code de
  l'unité au texte secondaire, pour un contraste d'au moins 4,5:1 ; les boutons de repli gardent au moins 24 px à l'écran
  quel que soit le zoom (facteur transmis en variable `--gm-org-zoom`) et ne sont plus recouverts par la carte.
- Scénario « gestes complémentaires » (`organigramme.spec.ts`) : retrait motivé d'un membre, recherche sans résultat,
  agrandissement, rattachement, chef désigné, n+1 changé, sous-unité créée, déplacée, désactivée puis réactivée,
  affectation de deux personnes (n+1 choisi puis aucun), PDF d'une branche en A4 portrait sans annexe.

## 8. Écarts et suites

- Le retrait d'une personne par un chef n'est pas ouvert (règle D-81 : unité cible obligatoire) ; l'interface du chef
  (« Mon équipe ») ne propose pas encore l'affectation, la route l'admet (§2).
- L'organigramme de démonstration du département (« Mon activité », `ORGANISATION_DEMO` de `@gimac/lot1`) et l'annuaire
  simulé de l'administration gardent des identifiants distincts ; l'organigramme et son PDF de démonstration s'appuient
  sur le second.
