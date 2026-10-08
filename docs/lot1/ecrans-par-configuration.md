# Écrans pilotés par la configuration, aucun codage en dur (D-94)

Demande de KOAGNE ISSA RYAN : les écrans sont dynamiques en fonction de la définition dynamique des rôles par le
RSSI ou le super administrateur ; les fonctionnalités sont des vues qui apparaissent ou disparaissent selon les
rôles, droits et accès qu'ils définissent, en fonction des fonctions du backend ; si le référentiel métier ou les
procédures changent, la plateforme ne doit pas redemander un codage en dur.

## 1. Principe

- **Le backend expose des fonctions atomiques** : les permissions. Aucune règle (Java, OPA, interface) ne cite un
  nom de profil ; les qualités d'un profil (privilégié, externe ou d'institution) se déduisent de ses permissions ou
  d'un attribut du profil (`gimac_externe`), modifiable.
- **Les rôles sont des compositions dynamiques** de permissions (D-62, D-81, D-86, D-90, D-92), décidées par le RSSI
  et le super administrateur (les profils privilégiés restant réservés à `IAM_ROLE_PRIVILEGED_GRANT`).
- **Les écrans dérivent de la configuration servie par MS-IAM** et des permissions du jeton ; les actions dans un
  écran dérivent de `actionsPermises` envoyé par le service métier.
- **Les procédures sont des données versionnées** (D-87, D-89, D-91, D-93) : aucun écran ne teste un rang, un code
  d'étape, une fonction ou un type de projet.

## 2. Catalogue des fonctionnalités servi et configurable (MS-IAM)

`GET /configuration/fonctionnalites` (tout compte interne : pour construire sa navigation) et
`PUT /configuration/fonctionnalites/{cle}` (`IAM_PROFILE_EDIT`, motif, clé de sécurité, journal
`CONFIGURATION_FONCTIONNALITE_MODIFIEE`). Une fonctionnalité : `{ cle, application: des | console | portail,
libelle, groupe, ordre, icone, visible, permissions: { tous: [..], unDe: [..] }, widgets: [..], droits: [ { cle,
nature: CONSULTER | CREER | MODIFIER | VALIDER | SIGNER | PUBLIER | EXPORTER | ADMINISTRER, permission, libelle } ] }`.
Les valeurs de départ viennent du registre actuel (généré à la construction et chargé au démarrage si absent) ;
toute modification est conservée en base et survit aux déploiements. Le code ne garde que la correspondance
`cle → composant et chemin`. La barre latérale, les gardes de route, la console, le tableau de bord, l'éditeur de
profil par fonctionnalité (D-90) et l'aperçu de la barre latérale dérivent de ce catalogue (fin des trois copies
divergentes du registre).

## 3. Règles figées à remplacer

- **MS-IAM** : « dernier super administrateur » → dernier porteur actif de `IAM_ROLE_PRIVILEGED_GRANT` ; rattachement
  à une institution selon l'attribut externe du profil ; lien d'invitation selon les permissions de console ; un
  profil est privilégié dès qu'il porte une permission `IAM_*` ou `PLATFORM_*`, qu'il soit livré ou créé.
- **MS-HOMOLOGATION** : pilote du référentiel = porteur de `HOM_REFERENTIEL_EDIT` (annuaire par permission) ;
  conducteur de projet = porteur d'une permission dédiée (`HOM_PROJECT_CONDUCT`, donnée au profil conducteur par
  défaut) ; fonctions de l'utilisateur servies par le serveur (`GET /moi/fonctions`) ; compteurs du tableau d'acteur
  décrits en données.
- **MS-CERTIFICATION** : circuit de signature du PV et visas lus dans les signatures configurées du type de projet
  (D-91) au lieu de `RoleSignature` figé.
- **Interface** : suppression de `FONCTION_DU_ROLE`, `fonctionsDe`, des tests de `ROLE_*`, de la table `PROFILS`
  hors démonstration, des libellés de profil i18n (libellés du serveur), des listes de fonctions, d'étapes et de
  rangs figées (affectation, attestations, questions de pilotage, filtres d'étapes) ; une « vue spécialisée » d'une
  opération est un attribut de l'opération (`vue`), pas son rang.

## 4. Sécurité et déploiement

- Routes `/services/**` de la passerelle fermées ou soumises à OPA ; `ROLE_ADMIN` n'ouvre plus le CRUD brut des
  services aux personnes (correctif immédiat, déployé à part).
- Fusion à trois voies du domaine Keycloak partout : `mettre-a-jour.sh` avec base, Kubernetes, et conservation de la
  politique de mot de passe et OTP réglée dans la plateforme.
- Politique OPA morte `gimac.rego` supprimée.

## 5. Preuve

Un test par règle remplacée (un profil créé quelconque reçoit la permission et obtient l'effet ; un profil livré qui
la perd ne l'a plus), et un E2E où le RSSI renomme, regroupe, réordonne, masque une fonctionnalité et change la
permission qui l'ouvre : la barre latérale et les gardes d'un utilisateur suivent sans nouvelle construction.

## 6. Mise en œuvre

### 6.1 MS-IAM : aucune règle sur un nom de profil

- **Profil privilégié** : tout profil, livré ou créé, dont la composition porte une permission `IAM_*` ou
  `PLATFORM_*` (`ReglesProfils.privilegie`, `CatalogueProfils.privilegies` lu dans la composition servie par
  l'annuaire). La liste `gimac.iam.profils.privilegies` est supprimée. Recomposer un profil livré en profil
  privilégié exige `IAM_ROLE_PRIVILEGED_GRANT` (403 `IAM_PRIVILEGED_PROFILE`).
- **Dernier super administrateur** : dernier compte actif portant `IAM_ROLE_PRIVILEGED_GRANT` par la composition de
  ses profils. Désactiver, supprimer, retirer les profils d'un tel compte, ou recomposer un profil
  (`PUT /configuration/profils/{code}/permissions`) de sorte qu'aucun compte actif ne la porte plus : 409
  `IAM_LAST_SUPER_ADMIN` (code inchangé, `details.permission`).
- **Profil externe** (compte d'institution, portail) : attribut `gimac_externe` du rôle (`true` ou `false`), lu par
  l'annuaire (Keycloak et substitut) ; à défaut d'attribut, règle de repli : le profil porte une permission
  `*_PORTAL_*`. Une institution n'est admise que si tous les profils du compte sont externes, et elle est
  obligatoire dès que l'un l'est (400 `IAM_INSTITUTION_INTERNAL_ACCOUNT`, `IAM_INSTITUTION_REQUIRED`).
  - Valeur de départ : `gimac.iam.profils.externes-initiaux: [ROLE_CORRESPONDANT]` ; au démarrage,
    `InitialisationProfilsExternes` pose `gimac_externe=true` sur ces profils s'ils n'ont pas encore l'attribut
    (jamais d'écrasement d'une valeur posée, à vrai ou à faux), puis réapplique les valeurs fixées depuis la console.
  - Modification : `PUT /api/v1/configuration/profils/{code}` accepte `externe` (facultatif, absent : inchangé), avec
    motif et clé de sécurité ; refus 409 `IAM_PROFIL_EXTERNE_TITULAIRES` si le profil a des titulaires ; journal
    `CONFIGURATION_PROFIL_MODIFIE` complété de `externe=avant/après` ; valeur gardée dans
    `profil_presentation.externe` et réappliquée après un réimport du domaine.
  - Vue `Profil` (`GET /configuration/profils`, réponses des `POST` et `PUT`) : nouveau champ `externe` (booléen).
- **Lien d'invitation et courriels** : console si les profils du compte composent l'une des permissions de
  `gimac.iam.liens.permissions-console` (par défaut `IAM_ACCOUNT_READ`, `PLATFORM_SUPERVISION_READ`,
  `PLATFORM_CONFIG_READ`, `PLATFORM_OPS_READ`, `PLATFORM_SECURITY_POLICY_READ`, `PLATFORM_MESSAGING_EDIT`, comme
  `PERMISSIONS_CONSOLE` de l'interface), portail pour un compte d'institution, département sinon.

### 6.2 MS-IAM : catalogue des fonctionnalités

- Table `fonctionnalite` (Liquibase `20261007090000_iam_ecrans_configuration.xml`) : définition au format servi
  (JSON), `modifie_le` nul pour une valeur de départ.
- Valeurs de départ : `ms-iam/src/main/resources/config/fonctionnalites-initiales.json` (objet
  `{ "fonctionnalites": [ … ] }` ou tableau seul), chargé au démarrage : clé absente ajoutée ; valeur de départ jamais
  modifiée mise à jour (ou retirée si le registre ne la porte plus) ; modification faite dans la plateforme jamais
  écrasée.
- `GET /api/v1/configuration/fonctionnalites?application=des|console|portail` : tout compte interne authentifié
  (jeton utilisateur sans `institution_id`, sinon 403 `IAM_COMPTE_INTERNE_EXIGE`) ; tableau trié par application,
  ordre et clé ; application inconnue : 400 `IAM_FONCTIONNALITE_INVALIDE`.
- `PUT /api/v1/configuration/fonctionnalites/{cle}` : `IAM_PROFILE_EDIT`, en-tête `X-Reauth-Assertion` (428 sans),
  corps = la fonctionnalité sans trace (clé facultative, égale au chemin) et `motif`. Refus : 404
  `IAM_FONCTIONNALITE_INTROUVABLE`, 400 `IAM_FONCTIONNALITE_INVALIDE` (`details.champ`), 422
  `IAM_FONCTIONNALITE_APPLICATION_IMMUABLE`, 400 `IAM_PERMISSION_UNKNOWN` (permission absente de
  `GET /configuration/permissions`), 422 `IAM_FONCTIONNALITE_PERMISSION_INTERNE`. Journal
  `CONFIGURATION_FONCTIONNALITE_MODIFIEE` (avant, après : champs modifiés seulement) et événement de configuration
  `FONCTIONNALITE_MODIFIEE`. Sans changement, rien n'est écrit.

```json
{
  "cle": "liaisons", "application": "des", "module": "homologation", "libelle": "nav.liaisons",
  "groupe": "homologation", "ordre": 110, "icone": "route", "visible": true,
  "permissions": { "tous": [], "unDe": ["HOM_LIAISON_LIST"] },
  "widgets": [ { "cle": "cockpit", "permissions": { "tous": ["HOM_DASHBOARD_READ"], "unDe": ["HOM_PROJECT_DESIGNATE"] } } ],
  "droits": [ { "cle": "HOM_LIAISON_LIST", "nature": "CONSULTER", "permission": "HOM_LIAISON_LIST", "libelle": "droits.permission.HOM_LIAISON_LIST" } ],
  "modifieLe": null, "modifiePar": null
}
```

  Champs ajoutés au contrat : `module` (regroupement de l'éditeur de profil par fonctionnalité, D-90), `modifieLe`
  et `modifiePar` (trace). `groupe` nul : fonctionnalité hors barre latérale (droits seulement). `libelle` : clé de
  traduction actuelle ou texte français. `ordre` : rang dans l'application (les groupes suivent l'ordre de leur
  première fonctionnalité).
- Transcription du registre (53 fonctionnalités : 32 `des`, 18 `console`, 3 `portail`), dont `referentielsMetierAdmin`
  (module `referentielsMetier`, « Administration de tous les domaines », droit `ADMINISTRER` sur `REF_METIER_ADMIN`).
  Clés de la console préfixées (`consoleComptes`…) car la clé est unique toutes applications confondues.
- Incohérences relevées entre les copies, règle retenue (la plus restrictive cohérente avec le serveur) :
  - matrice des pouvoirs : registre et navigation de la console `IAM_PROFILE_EDIT`, gardes de route des deux
    `App.tsx` `IAM_ACCOUNT_READ`, serveur lecture `IAM_ACCOUNT_READ` et écriture `IAM_PROFILE_EDIT` : les deux exigées
    (`tous`) ;
  - « Mes projets » : ouvert par `CER_PV_ESTABLISH` mais droit de consultation `HOM_LIAISON_LIST` : les deux exigées ;
  - « Console » depuis l'application interne : `PLATFORM_CONFIG_READ`, alors que la console s'ouvre avec l'une des
    `PERMISSIONS_CONSOLE` : gardé `PLATFORM_CONFIG_READ` ;
  - navigation de la console sans droits D-90 (les droits ne sont portés que par les fonctionnalités `des` et `portail`).

### 6.3 MS-PILOTAGE

L'ancien repli « `IAM_PROFILE_EDIT` = toutes les unités » est retiré de `AccesProcessus` (définir, lire, catalogue,
`auTitreDe`, mention du journal) : seuls `REF_METIER_ADMIN`, la matrice (portées `TOUTES`, `UNITES`…) et
l'appartenance à l'unité décident. Un RSSI avec `IAM_PROFILE_EDIT` seul ne voit plus les processus des autres
unités (`AccesProcessusTest`, `ProcessusDefinitionIT`). Aucun nom de profil n'est testé dans le code de production
(seuls les bouchons de démonstration en citent).

### 6.4 Consignes pour les autres composants

- Générateur du domaine (`infra/keycloak/generer-realm-gimac.py`) : ajouter l'attribut aux profils externes, par
  exemple `{"gimac_libelle": [PRESENTATIONS[n][0]], **({"gimac_externe": ["true"]} if n in PROFILS_EXTERNES else {})}`.
- OPA : ouvrir `GET /bff/{des,console}/iam/configuration/fonctionnalites` à toute session interne et
  `PUT /bff/{des,console}/iam/configuration/fonctionnalites/{cle}` à `IAM_PROFILE_EDIT`.
- Interface : construire barre latérale, gardes, console, tableau de bord et éditeur D-90 depuis `GET` (filtre
  `visible`, prédicat `tous`/`unDe`, widgets) ; garder seulement `cle → composant et chemin` ; ajouter les clés i18n
  `droits.module.referentielsMetier` (le libellé servi est sinon du texte) ; afficher et modifier `externe` dans la
  vue Profil ; régénérer `config/fonctionnalites-initiales.json` depuis le registre.

### 6.5 MS-HOMOLOGATION (07/10/2026)

- **Pilote du référentiel** : porteur de `HOM_REFERENTIEL_EDIT` ou `REF_METIER_ADMIN`
  (`ReferentialLifecycleService.PERMISSIONS_PILOTE`) ; désignation contrôlée et liste `pilotes` du catalogue du
  modélisateur lues par permission (`PorteursPermission`). La propriété `gimac.homologation.referentiel.profils-pilote`
  et la constante `PROFILS_EDITION` sont supprimées.
- **Conducteur** : porteur de la nouvelle permission `HOM_PROJECT_CONDUCT` (« Conduite des projets d'homologation »,
  donnée par défaut à `ROLE_CONDUCTEUR_PROJET` dans `generer-realm-gimac.py`, domaines régénérés : 81 permissions).
  Ouverture et redésignation : 409 `HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED` sinon ; `GET /liaisons/conducteurs` liste
  les porteurs. `LiaisonOpeningPolicy.PROFIL_CONDUCTEUR` est supprimé. Aucune route n'exige la permission : pas de
  règle OPA (seule la route de renvoi D-93 en reçoit une).
- **Annuaire par permission** : MS-IAM ne sert pas encore cette lecture (seul `?profil=` existe). **À ajouter côté
  MS-IAM** : `GET /api/v1/interne/collaborateurs?permission=<PERMISSION>` (jeton de service `INTERNAL_IAM_READ`) →
  tableau `Collaborateur` (comme `?profil=`) des comptes actifs dont un profil compose la permission. MS-HOMOLOGATION
  l'appelle déjà (`IamCollaboratorAdapter.parPermission`) ; tant que la réponse n'est pas une liste (route non servie)
  ou sur un 4xx, repli transitoire sur `gimac.homologation.permissions-repli.<PERMISSION>` (profils du domaine
  généré), en configuration et non dans le code. Route désormais servie par MS-IAM (§6.6) : le repli ne joue plus
  que si MS-IAM répond par un 4xx. **Repli supprimé le 08/10/2026** : propriété `gimac.homologation.permissions-repli` et code
  retirés ; MS-IAM injoignable ou en refus : 503 `HOM_IAM_UNAVAILABLE` (`effet-reel-referentiels.md` §8.8).
- **Fonctions de l'appelant** : `GET /api/v1/moi/fonctions` (passerelle `/bff/des/homologation/moi/fonctions`, tout
  compte authentifié au second facteur) → `[{ code, libelle, externe, profils }]` : fonctions du catalogue D-91 que
  confèrent les profils du jeton (un chef de département tient aussi `CHEFS_DEPARTEMENT`), `profils` : ceux du jeton
  qui la confèrent. Remplace `FONCTION_DU_ROLE` et `fonctionsDe` de l'interface.
- **Compteurs du tableau d'acteur en données** : `config/tableau-acteur-compteurs.json` (ou fichier externe :
  `gimac.homologation.tableau.compteurs`), liste de `{ cle, libelle, fonctions, portee: LIAISON|OPERATION|TACHE,
  mesure: NOMBRE|ATTESTATIONS_DEPOSEES|ATTESTATIONS_A_FOURNIR, liaison: { etapesCourantes, conducteurADesigner,
  mesProjets, etatsPv, etatsApprobation, etatsFiche, fintech, visaAttendu }, operation: { etats, engagee, etapes,
  etapesVersion, gestes, jalons, declencheurs, particularites, segments, responsables, fonctionsR, cle,
  oubliSansSymptome, premiereDeLEtape, vues }, sla, ton, lien, lienUnique: OPERATION|DOSSIER:<onglet>|TACHE }`. Les
  32 compteurs codés par fonction y sont transcrits (seul écart : `essaisEnCours` pointe la liste des liaisons en essai).
  Le compteur servi gagne `libelle` et `fonction`. « Mes tâches » : opérations où la personne est R ou V désignée,
  consultée sollicitée, signataire attendue, ou porteuse d'une fonction à affecter (tâche à prendre).
- Voir `effet-reel-referentiels.md` §8 pour le RACI imposé, les livrables, la vue des opérations et les événements.

### 6.6 MS-IAM : collaborateurs par permission et conservation de la configuration

**Porteurs d'une permission** (D-94 §3, demandé au §6.5).

- `GET /api/v1/interne/collaborateurs?permission=<PERMISSION>` (`CollaborateursInternesResource.parPermission`), jeton
  de service `INTERNAL_IAM_READ` comme toutes les routes `/api/v1/interne/**` (403 pour un jeton utilisateur, 401 sans
  jeton), jamais relayée par la passerelle : aucune règle OPA.
- Réponse : tableau `Collaborateur`, même forme que `?profil=` et que l'adaptateur `IamCollaboratorAdapter` de
  MS-HOMOLOGATION : `{ id, nomAffiche, profils, institutionId, actif, fonction, avatarVersion, unite, responsableId }`,
  sans courriel ni donnée de sécurité ; trié par nom affiché puis identifiant ; liste complète, non paginée.
  Permission inconnue, blanche ou portée par aucun profil : `[]` (200).
- Règle (`PorteursPermission`, `ReglesProfils.profilsPortant`) : comptes **internes** (sans institution) et **actifs**
  dont un profil compose la permission. La composition est lue dans Keycloak à chaque appel
  (`Annuaire.compositions`) : profils créés (D-81) et recompositions faites dans la console sont pris en compte
  sans redémarrage. Trois lectures de l'annuaire quel que soit le nombre de comptes (compositions, profils par compte,
  comptes actifs allégés), sans lecture du second facteur. Aucun nom de profil dans le code.
- Paramètre `unite` : non servi, MS-HOMOLOGATION ne l'envoie pas (les membres d'une unité ont leur route,
  `/api/v1/interne/organisation/unites/{code}/membres`). `?profil=` et `?permission=` ensemble : `?profil=` l'emporte.
- Tests : `PorteursPermissionTest` (règle, recomposition, profil créé), `IamRoutesSecurityTest`
  (`collaborateursParPermission` : jetons, forme), `CollaborateursInternesIT` (`parPermission`, de bout en bout avec
  l'annuaire en mémoire), `KeycloakAnnuaireIT` (`porteursPermission` : vrai Keycloak, profil créé puis recomposé,
  compte externe et compte désactivé exclus).

**Comptes d'une institution** (D-93 §11, destinataires des notifications d'une institution chez MS-NOTIFICATION).

- `GET /api/v1/interne/collaborateurs?institution=<id>` (`CollaborateursInternesResource.parInstitution`), jeton de
  service `INTERNAL_IAM_READ`. Même forme que `?permission=` (tableau `Collaborateur`, `institutionId` renseigné), trié
  par nom affiché, 500 comptes au plus. Comptes **actifs** dont l'attribut `institution_id` vaut exactement `<id>`
  (recherche Keycloak `q=institution_id:<id>`, `Annuaire.parInstitution`) ; profils complétés par la lecture des
  profils par compte. Institution inconnue ou blanche : `[]`. `?profil=` ou `?permission=` présent : ces routes
  l'emportent. Contrat de `IamComptesInstitutionAdapter` (MS-NOTIFICATION) respecté : `id`, `institutionId`, `actif`.
- Tests : `AdministrationComptesServiceTest.comptesInstitution`, `IamRoutesSecurityTest.collaborateursParInstitution`,
  `CollaborateursInternesIT.parInstitution`, `KeycloakAnnuaireIT.parInstitution` (vrai Keycloak).

**Notifications du portail** (D-93 §11.4).

- OPA (`lot1_authz.rego`, règles de `regles`) : `GET /bff/portail/notification/(notifications(/compteur)?|preferences)`,
  `POST /bff/portail/notification/notifications(/{id})?/lecture` et `PUT /bff/portail/notification/preferences`, pour
  un compte d'institution (`institution_id` dans le jeton, règle `allow` du portail) à deux facteurs portant
  `NOT_INBOX_READ` ; tout autre chemin ou méthode sous `/bff/portail/notification` est refusé, un compte interne
  aussi. Tests `test_notifications_portail_*` ; copies `partages/opa` resynchronisées.
- Domaine : `NOT_INBOX_READ` ajoutée à `ROLE_CORRESPONDANT` (`generer-realm-gimac.py`, domaines régénérés ; substitut
  de MS-IAM aligné). Un domaine en service la reçoit au prochain déploiement par la fusion à trois voies (ajout du
  dépôt).
- Passerelle : route `notification-boite` étendue à `/bff/portail/notification/**` (vers `/api/v1/**` de
  MS-NOTIFICATION) ; canal poussé : la couche `PORTAIL` reçoit le sujet `notification` (`notification.inapp`), remis
  au seul destinataire (`data.destinataires`) ; l'institution de l'enveloppe n'est pas exigée pour ce sujet
  (notification personnelle, sans `institutionid`), mais l'abonné du portail doit porter une institution.
- MS-NOTIFICATION (relu, non modifié) : liste, compteur, lecture et préférences sont lus et écrits pour le seul `sub`
  du jeton (`Jetons.compte`, requêtes `...ByDestinataireId`, notification d'un autre compte : 404) ; aucun écart de
  comportement. Seules les javadocs (`BoiteNotificationsResource`, `Jetons.BOITE` : « tous les profils internes »)
  ne mentionnent pas encore le portail.

**Configuration modifiée dans la console conservée par les déploiements.** Keycloak est persistant ; une nouvelle
version de `gimac-realm.json` est réimportée par export, fusion (`infra/ci/fusionner-domaine.py`) et import. La fusion
est à trois voies entre la **base** (domaine du dépôt importé la fois précédente), le domaine **en service** et le
nouveau domaine du **dépôt** :

| Élément | Règle |
|---|---|
| Comptes (mots de passe hachés, codes TOTP, attributs) | Ceux du domaine en service ; comptes du dépôt absents ajoutés ; rôles et groupes disparus retirés |
| Composition d'un profil | (en service moins ce que le dépôt a retiré depuis la base) plus ce que le dépôt a ajouté |
| Description, attributs d'un profil (`gimac_libelle`, `gimac_externe`…) | Valeur en service si elle diffère de la base, sinon celle du dépôt |
| Profil créé dans la console | Conservé ; profil retiré du dépôt (présent dans la base) : retiré |
| Politique de mot de passe (`passwordPolicy`), OTP (`otpPolicy*`), durées de session (`ssoSession*`), force brute (`bruteForceProtected`, `failureFactor`, `waitIncrementSeconds`, `maxFailureWaitSeconds`, `permanentLockout`…) | Par paramètre : valeur en service si elle diffère de la base (modifiée par `PLATFORM_SECURITY_POLICY_EDIT`), sinon celle du dépôt ; paramètre absent du domaine en service : celle du dépôt |
| Autres paramètres du domaine, clients | Ceux du dépôt |

Sans base (premier passage de la règle sur une installation), la composition est l'union et la politique en service
est gardée : rien de ce qui a été fait dans la console n'est perdu, une évolution du dépôt sur un paramètre de
politique attend le déploiement suivant. Le journal de la fusion nomme les paramètres conservés, jamais leur valeur.

| Installation | Procédure | Base |
|---|---|---|
| Serveur de démonstration | `infra/ci/deployer-serveur.sh` (`actualiser_domaine`) | `~/.local/state/gimac-des/gimac-realm.precedent.json` |
| Poste de développement | `infra/docker-compose/mettre-a-jour.sh` (même logique, depuis le 07/10/2026) | `~/.local/state/gimac-des/gimac-realm.precedent.json`, écrite après un démarrage réussi |
| Kubernetes | `infra/kubernetes-gimac/actualiser-domaine-keycloak.sh` : Keycloak arrêté, Job `keycloak-domaine` (exporter, fusionner, importer), Keycloak relancé | volume `keycloak-domaine-etat` (`gimac-realm.precedent.json`), écrit par le Job après l'import |

Tests de la fusion : `infra/ci/test_fusionner_domaine.py` (rôles, comptes, politique), lancés par
`infra/ci/valider-infra.sh`. Exploitation : `docs/exploitation/reprise-des-donnees.md` §2 bis.
