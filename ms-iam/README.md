<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-IAM : plateforme DES du GIMAC

Identités, rôles à cinq niveaux (D-31), délégations (élévation 60 min et suppléance 72 h, D-20, D-36), authentification forte FIDO2, revue trimestrielle des accès.

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D1 |
| Épopée du carnet (CP v2.0) | IAM |
| Port | 8081 |
| Paquet Java | `cm.gimac.des.iam` |
| Entités du JDL (10) | Collaborator, Role, Permission, RoleGrant, Delegation, UserSession, Assignment, Availability, AccessReview, AccessReviewDecision |
| Règles de gestion (REC-GIMAC-01 §5) | RG-01, RG-02, RG-57, RG-65, RG-66, RG-67, RG-74 |
| Modèle | `ms-iam.jdl` |

## État

Squelette JHipster 9.4.0 (CRUD généré réservé à l'administration technique, `ROLE_ADMIN`) et, en architecture hexagonale (`domain`, `application`, `infrastructure`, `web.rest.lot1`), l'administration complète des comptes et de l'authentification depuis la plateforme (D-62) et la réauthentification forte FIDO2 (IAM-07). Keycloak reste le moteur d'authentification ; nul n'ouvre sa console.

### Administration des comptes et de la plateforme (D-62)

- **Annuaire** (port `application/port/out/Annuaire`) : adaptateur réel `infrastructure/idp/KeycloakAnnuaire` (API d'administration, compte de service `iam-service` du domaine `gimac`, rôles `manage-users`, `view-users`, `query-users`, `query-groups`, `manage-realm`, `view-realm`), choisi par `gimac.adapters.annuaire=real` (profil prod) ; adaptateur en mémoire `AnnuaireEnMemoire` en développement et en test (comptes de démonstration `super.admin`, `rssi`). Panne de Keycloak : erreur 503 `IAM_IDP_UNAVAILABLE`, jamais de valeur par défaut.
- **Règles** (`domain/service/ReglesAdministration`, `PolitiqueMotDePasse`, services purs) : identifiant `^[a-z][a-z0-9._-]{2,49}$`, courriel unique, profils connus seulement, profils privilégiés (tout profil, livré ou créé, dont la composition porte une permission `IAM_*` ou `PLATFORM_*`, D-94) attribués par le seul détenteur de `IAM_ROLE_PRIVILEGED_GRANT`, nul ne s'administre soi-même, le dernier compte actif portant `IAM_ROLE_PRIVILEGED_GRANT` par la composition de ses profils ne peut être désactivé, supprimé ni perdre cette permission (409 `IAM_LAST_SUPER_ADMIN`, D-94), institution réservée aux profils externes (attribut `gimac_externe`, à défaut une permission `*_PORTAL_*`) et obligatoire pour eux, motif obligatoire des actes, planchers de la politique de sécurité (12 caractères au moins, OTP 6 ou 8 chiffres sur 30 à 60 s, sessions 5 à 120 min et 1 à 24 h, protection contre la force brute obligatoire, 3 à 10 échecs).
- **Journal d'administration** (`journal_administration`, insertion seule, déclencheur PostgreSQL) et événements d'audit par l'outbox, dans la transaction de l'acte.
- **Premier accès** : étapes restantes calculées (`MOT_DE_PASSE`, `SECOND_FACTEUR`, `FICHE`, `CHARTE`, `VERIFIER_COURRIEL`) ; toutes sont conduites sur la plateforme depuis D-65 (voir ci-dessous), jamais par une page de Keycloak.
- **Réauthentification forte FIDO2** (`ReauthentificationService`, webauthn4j) : enrôlement des clés depuis « Mon compte », défi à usage unique lié au sujet et à la finalité (2 min), vérification pour le service qui porte la décision (compteur de signatures mis à jour, assertion jamais rejouable, résultat publié vers l'audit). Actes d'administration sensibles : finalité `ADMIN_SENSITIVE` (suppression, profils, politique, composition d'un profil), 428 sans assertion, 403 `IAM_REAUTH_INVALID` si elle est refusée. Finalité libre (`^[A-Z][A-Z_]{2,59}$`), liée au défi : signature d'une étape de processus d'unité, `SIGNATURE_PROCESSUS`, vérifiée par MS-PILOTAGE (jeton `pilotage-service`, D-87).

### Connexion et configuration des comptes sur la plateforme (D-65)

Contrat : `docs/lot1/connexion.md` §3. L'utilisateur ne voit jamais une page de Keycloak ; MS-IAM tient l'état du compte, envoie les courriels et conduit l'assistant de premier accès.

- **Actions exigées** tenues dans l'attribut Keycloak `gimac_actions_requises` (jamais les actions requises de Keycloak, qui bloqueraient l'octroi direct). Étapes restantes = actions exigées, plus `FICHE` et `CHARTE` tant que la fiche n'est pas complète, plus `SECOND_FACTEUR` tant qu'aucun code TOTP n'est configuré. Les anciens noms (`UPDATE_PASSWORD`, `CONFIGURE_TOTP`, `UPDATE_PROFILE`, `VERIFY_EMAIL`) sont encore lus.
- **État de connexion** pour la passerelle : `{actif, secondFacteur, actionsRequises}`.
- **Liens par courriel** (`LiensCourriel`, table `jeton_lien`) : 32 octets aléatoires en base64url, seule l'empreinte SHA-256 est conservée avec la finalité (`INVITATION` 24 h, `REINITIALISATION` 30 min), le compte, la couche, l'échéance et la date d'usage ; usage unique, un seul lien ouvert par compte ; un mot de passe refusé par la politique ne consomme pas le lien. Suivre le lien prouve la boîte aux lettres : le courriel devient vérifié, les actions `MOT_DE_PASSE` et `VERIFIER_COURRIEL` sont retirées ; une réinitialisation ferme les sessions.
- **Courriels de la plateforme** (`CourrielsPlateforme`, gabarits `i18n/courriels*.properties` en français, anglais et espagnol selon la langue du compte) : invitation, réinitialisation, rappel des actions. Lien construit sur `gimac.iam.liens.{des,portail,console}` (portail pour un compte d'institution, console si ses profils composent l'une des permissions de `gimac.iam.liens.permissions-console`, département sinon, D-94). Envoi par le port `ServiceCourriel` : adaptateur `CourrielSmtp` (`gimac.adapters.courriel=smtp`, profil prod) avec les paramètres de messagerie de la console (D-62) — le mot de passe SMTP, que Keycloak ne restitue jamais, est gardé chiffré par MS-IAM (table `parametre_chiffre`) ; adaptateur en mémoire `CourrielsEnMemoire` en développement et en test.
- **Mot de passe** : changement par `POST /moi/mot-de-passe` (actuel vérifié par l'octroi direct du client `gimac-connexion`, force brute comprise ; politique contrôlée par MS-IAM puis par Keycloak, refus ramenés aux violations `LONGUEUR`, `MAJUSCULE`, `MINUSCULE`, `CHIFFRE`, `SPECIAL`, `IDENTIFIANT`, `COURRIEL`, `HISTORIQUE`). Mot de passe provisoire de l'administration non temporaire pour Keycloak, plus l'action `MOT_DE_PASSE`.
- **Code TOTP** : `POST /moi/second-facteur/totp/enrolement` tire un secret de 20 octets (base 32), gardé chiffré (AES-256-GCM, table `enrolement_totp`) 10 min, et rend l'URI `otpauth://totp/GIMAC%20DES:{identifiant}?…` selon la politique OTP du domaine ; `POST /moi/second-facteur/totp` vérifie le code (RFC 6238, tolérance d'un pas), puis l'enregistre dans Keycloak par l'extension `gimac-totp` (jeton du compte de service `iam-service`, code vérifié à nouveau par Keycloak) ; un code existant est remplacé, l'action `SECOND_FACTEUR` retirée. Remplacer un code déjà configuré exige la réauthentification FIDO2 si une clé est enrôlée (finalité `ACCOUNT_SENSITIVE`, en-tête `X-Reauth-Assertion`). Réinitialisation par l'administration : suppression des identifiants OTP plus l'action `SECOND_FACTEUR`.
- **Création et invitation** : courriel de la plateforme (lien vers le nouveau mot de passe, 24 h) au lieu de `execute-actions-email` ; une panne de la messagerie n'annule pas la création (`invitationEnvoyee` faux, acte `INVITATION_ECHEC` tracé).
- **Traces** : chaque acte (création, invitation, nouveau mot de passe, mot de passe oublié, changement de mot de passe, code TOTP, première configuration) entre au journal d'administration et part vers l'audit (`iam.account-administered.v1`). Jamais de mot de passe, de jeton, de secret ni de code dans les journaux, les événements ou la représentation textuelle des requêtes.
- **Paramètres** : `gimac.iam.liens.*` (`GIMAC_URL_DES`, `GIMAC_URL_PORTAIL`, `GIMAC_URL_CONSOLE`), `gimac.iam.connexion.client-secret` (`GIMAC_SECRET_CONNEXION`), `gimac.iam.connexion.clients-internes` (défaut `gateway-service`), `gimac.iam.chiffrement.cle` (`GIMAC_IAM_CLE_CHIFFREMENT`, 32 octets en base 64 ; absente : clé éphémère, à fournir en production).

### « Mon compte » : photo de profil, mot de passe, alerte de connexion (docs/lot1/mon-compte.md)

- **Photo de profil** (`domain/service/TraitementAvatar`, service pur ; `AvatarService` ; `AvatarResource`) : type réel reconnu par les octets (PNG, JPEG, WebP — lecture WebP par TwelveMonkeys `imageio-webp`), 2 Mo au plus (au-delà de 3 Mo, la requête multipart est refusée avant lecture, même code), dimensions 64 à 4 096 px lues dans l'en-tête avant décodage, recadrage carré centré, réduction par moitiés jusqu'à 256 × 256, réencodage PNG (pixels seuls : EXIF, position GPS, commentaires et profils disparaissent ; l'original n'est jamais gardé). Table `avatar_compte` (compte, image, type, version = 16 caractères hexadécimaux du SHA-256, mise à jour). Codes : `IAM_AVATAR_TYPE`, `IAM_AVATAR_TOO_LARGE`, `IAM_AVATAR_DIMENSIONS`, `IAM_AVATAR_UNREADABLE` (400), `IAM_AVATAR_NOT_FOUND` (404), `IAM_AVATAR_FORBIDDEN` (403 : un correspondant ne lit que sa photo). Lecture en `Cache-Control: private, max-age=86400, immutable` avec `ETag`. Chaque remplacement ou retrait entre au journal d'administration (`AVATAR_REMPLACEE`, `AVATAR_RETIREE`) et part vers l'audit (`iam.avatar-changed.v1`, sujet `iam.avatar`, sans l'image).
- **`avatarVersion`** dans `GET /moi` (et `compte`), la liste et la fiche des comptes de la console, l'annuaire interne (`/interne/collaborateurs`), et par lot pour MS-COLLABORATION (`GET /interne/collaborateurs/avatars?ids=`, 200 identifiants au plus).
- **Mot de passe** : `fermerAutresSessions` facultatif ; MS-IAM ferme les sessions Keycloak autres que celle du jeton (claim `sid`) et répond avec l'en-tête `X-Gimac-Fermer-Autres-Sessions: 1`, que la passerelle consomme pour fermer ses autres sessions. Courriel d'information (« mot de passe modifié », lien vers « Mon compte » de la couche) après tout changement ; son échec n'annule pas le changement.
- **Alerte « nouvel appareil »** : `POST /interne/comptes/{id}/alerte-connexion` (jeton de service de `gateway-service`) → courriel dans la langue du compte (appareil, adresse, date UTC, lien vers les appareils connectés), acte `ALERTE_NOUVEL_APPAREIL` au journal ; rien pour un compte inactif.

### Organisation : unités et rattachement des comptes (D-76, docs/lot1/mon-activite.md §1)

- **Catalogue** `unite_organisationnelle` (Liquibase `20261005090000_iam_organisation.xml`, jeu initial DG, DES, SNA, PROD_MONETIQUE, PROD_INFO, POLE_RESEAU, POLE_SYSTEME, DDRM, SSI, CONFORMITE) ; port `CatalogueUnites`, adaptateur JPA `CatalogueUnitesJpa`. Règles (`domain/service/ReglesOrganisation`, service pur) : code `^[A-Z][A-Z0-9_]{1,31}$`, libellé 1 à 120, type `DIRECTION|DEPARTEMENT|SERVICE|POLE|CELLULE`, direction sans parent (400 `ORG_UNITE_INVALIDE`), code unique (409 `ORG_UNITE_EXISTANTE`), parent connu et actif (422 `ORG_UNITE_INCONNUE`), sans cycle (409 `ORG_CYCLE`), pas de désactivation avec des sous-unités actives (409 `ORG_UNITE_SOUS_UNITES_ACTIVES`). Chaque création ou modification entre au journal (`CONFIGURATION_UNITE_ORGANISATIONNELLE`).
- **Rattachement** : attributs Keycloak `unite` et `responsable_id` (profil utilisateur, administration seule) ; `unite` et `responsableId` dans la fiche des comptes (console, « Mon compte ») et dans l'annuaire interne. Refus : 422 `ORG_COMPTE_EXTERNE` (compte d'institution), 422 `ORG_UNITE_INCONNUE` (unité inconnue ou inactive ; l'unité actuelle, même désactivée, peut être conservée), 422 `ORG_RESPONSABLE_INVALIDE` (responsable absent, inactif, d'institution ou titulaire lui-même), 409 `ORG_CYCLE`. Journal `COMPTE_RATTACHEMENT_MODIFIE` (avant, après).
- **Lectures** : rattachement `{id, nomAffiche, unite, chemin, responsable, chaine (10 au plus), equipeDirecte}` ; équipe récursive (recherche Keycloak par attribut `q=responsable_id:X`, niveau par niveau, sans cycle, 500 comptes au plus) `[{id, nomAffiche, fonction, unite, responsableId, niveau, actif}]`.

### Pouvoirs des chefs sur leurs subordonnés (D-81, docs/lot1/pouvoirs-hierarchiques.md §2 à §5)

- **Responsable d'une unité** : colonne `responsable_id` de `unite_organisationnelle` (compte interne actif, sinon 422 `ORG_RESPONSABLE_INVALIDE`) ; `responsableId` dans `Unite`, `POST /organisation/unites` et `PUT /organisation/unites/{code}` (absent du corps : conservé ; `null` : retiré). `unitesDirigees` (unités dont le compte est responsable, sous-unités comprises) dans le rattachement (`/organisation/moi`, `/interne/organisation/{id}`).
- **Périmètre** (`OrganisationService.perimetre`) : `AUCUNE` rien ; `DIRECTS` comptes dont il est le n+1 (niveau 1) ; `EQUIPE` sa chaîne (niveau 1, 2…) ; `UNITES` équipe puis membres de ses unités dirigées hors de sa chaîne (niveau 0, recherche Keycloak `q=unite:X`) ; `TOUTES` (D-92) tous les comptes internes actifs (niveau 0, triés par nom) ; jamais le compte lui-même, 500 au plus. `unites` : unités dirigées pour `UNITES`, toutes les unités du catalogue (actives ou non) pour `TOUTES`, vide sinon.
- **Matrice des pouvoirs** (`domain/pouvoir`, `ReglesPouvoirs`, `PouvoirsService`, tables `pouvoir_profil` et `profil_demandable`, adaptateur `MatricePouvoirsJdbc`) : portée par profil et par action (absente : `AUCUNE`) ; pouvoirs d'un compte = portée la plus large de ses profils, profils demandables réunis puis restreints aux profils attribuables non privilégiés (aucune escalade, même si la base en portait) ; compte inactif ou d'institution : aucun pouvoir. Portées `AUCUNE` < `DIRECTS` < `EQUIPE` < `UNITES` < `TOUTES` ; `TOUTES` (D-92, docs/lot1/referentiels-metier-admin.md) admise seulement pour `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` (422 `ORG_PORTEE_NON_ADMISE`, contrainte `ck_pouvoir_profil_portee_toutes`, Liquibase `20261006200000_iam_portee_toutes.xml`) ; `GET /pouvoirs/matrice` expose `porteesAdmises` par action ; les subordonnés de `/pouvoirs/moi` restent calculés au plus à `UNITES`. Réglage : profils demandables connus (400 `IAM_PROFILE_UNKNOWN`), jamais privilégiés (422 `ORG_PROFIL_NON_DEMANDABLE`), journal `MATRICE_POUVOIRS_MODIFIEE` (avant, après) et `iam.configuration-changed.v1` (objet `MATRICE_POUVOIRS`).
- **Jeu par défaut** (Liquibase `20261006090000_iam_pouvoirs_hierarchiques.xml`) : chef du DES, chef de la DDRM, DG, RSSI, conformité, responsable de service : `TACHE_CONFIER` et `TACHE_SUIVRE` `UNITES`, `ACTIVITE_LIRE` et `RAPPORT_VISER` `EQUIPE`, `RATTACHEMENT_MODIFIER` `DIRECTS`, `COMPTE_DEMANDER` `UNITES`, `PROFIL_DEMANDER` et `STATUT_DEMANDER` `EQUIPE` ; tous les autres profils : `AUCUNE`. Profils demandables (profils non privilégiés du domaine de chaque responsable ; jamais super administrateur, RSSI, administrateur système, ni correspondant d'institution, les demandes ne créant que des comptes internes) :

  | Responsable | Profils demandables |
  |---|---|
  | `ROLE_DG` | tous les profils internes non privilégiés : `ROLE_CHEF_DES`, `ROLE_CHEF_DDRM`, `ROLE_CONFORMITE`, `ROLE_RESPONSABLE_SERVICE`, `ROLE_COLLABORATEUR`, `ROLE_INGENIEUR_HOMOLOGATION`, `ROLE_SNA`, `ROLE_PRODUCTION_MONETIQUE`, `ROLE_POLE_RESEAU`, `ROLE_POLE_SYSTEME`, `ROLE_CHARGE_DDRM`, `ROLE_CONDUCTEUR_PROJET` |
  | `ROLE_CHEF_DES` | `ROLE_RESPONSABLE_SERVICE`, `ROLE_COLLABORATEUR`, `ROLE_INGENIEUR_HOMOLOGATION`, `ROLE_SNA`, `ROLE_PRODUCTION_MONETIQUE`, `ROLE_POLE_RESEAU`, `ROLE_POLE_SYSTEME`, `ROLE_CONDUCTEUR_PROJET` |
  | `ROLE_CHEF_DDRM` | `ROLE_RESPONSABLE_SERVICE`, `ROLE_COLLABORATEUR`, `ROLE_CHARGE_DDRM`, `ROLE_CONDUCTEUR_PROJET` |
  | `ROLE_RESPONSABLE_SERVICE` | `ROLE_COLLABORATEUR`, `ROLE_SNA`, `ROLE_PRODUCTION_MONETIQUE`, `ROLE_POLE_RESEAU`, `ROLE_POLE_SYSTEME`, `ROLE_INGENIEUR_HOMOLOGATION` |
  | `ROLE_RSSI`, `ROLE_CONFORMITE` | `ROLE_COLLABORATEUR` (un profil de quitus ou de sécurité se demande à la DG) |

- **Demandes à l'administration** (`domain/demande`, `ReglesDemandes`, `DemandesService`, table `demande_administration`, index unique partiel `(type, cle_cible) WHERE etat = 'EN_ATTENTE'`, clé = compte visé ou `identifiant:<identifiant proposé>`) : motif 5 à 500 (400 `ORG_DEMANDE_INVALIDE`, aussi pour un contenu mal formé ou sans effet) ; portée de `COMPTE_DEMANDER` / `PROFIL_DEMANDER` / `STATUT_DEMANDER` (403 `ORG_HORS_PERIMETRE` ; création : n+1 proposé = le demandeur ou un compte de son périmètre, unité proposée = la sienne, celle d'un compte de son périmètre ou une unité dirigée en portée `UNITES`, puis règles du rattachement) ; profils proposés (attribués, ajoutés ou retirés) dans ses profils demandables (422 `ORG_PROFIL_NON_DEMANDABLE`) ; rattachement à une institution vérifié dès la soumission (compte créé par demande = compte interne sans institution, profils après l'acte cohérents avec l'institution du compte visé : 400 `IAM_INSTITUTION_REQUIRED` ou `IAM_INSTITUTION_INTERNAL_ACCOUNT`) ; une seule demande en attente par cible et par type (409 `ORG_DEMANDE_EXISTANTE`) ; identifiant ou courriel déjà pris (409 `IAM_ACCOUNT_CONFLICT`). Acceptation : permission de l'acte (`IAM_ACCOUNT_CREATE`, `IAM_ACCOUNT_ROLES`, `IAM_ACCOUNT_STATUS`, sinon 403 `ORG_DEMANDE_INTERDITE`), réauthentification FIDO2 (`ADMIN_SENSITIVE`, 428 / 403), pas sa propre demande (403 `IAM_SELF_ADMINISTRATION`), état `EN_ATTENTE` (409 `ORG_DEMANDE_TRAITEE`) ; l'acte passe par `AdministrationComptesService` (création avec invitation par courriel, profils privilégiés sous `IAM_ROLE_PRIVILEGED_GRANT`, dernier super administrateur, journal) et `OrganisationService.rattacher` ; la création acceptée renseigne `cible` avec le compte créé. Journal `DEMANDE_SOUMISE`, `DEMANDE_ACCEPTEE`, `DEMANDE_REFUSEE`, `DEMANDE_ANNULEE` (cible : la demande). Version optimiste : deux décisions concurrentes ne passent pas.
- **Profils créés** (`ProfilsService`, `ReglesProfils`, table `profil_cree`, `CatalogueProfils`) : code `^ROLE_[A-Z][A-Z0-9_]{2,40}$` (400 `IAM_PROFIL_CODE_INVALIDE`), unique (409 `IAM_PROFIL_EXISTANT`), description 1 à 500, libellé facultatif 1 à 80 (D-86) ; permissions connues, `INTERNAL_*` interdites (422 `IAM_PROFIL_PERMISSION_INTERNE`), `IAM_*` / `PLATFORM_*` : profil privilégié, réservé à `IAM_ROLE_PRIVILEGED_GRANT` (403 `IAM_PRIVILEGED_PROFILE`) ; `COL_MESSAGE_READ`, `COL_MESSAGE_SEND`, `PIL_ACTIVITY_OWN`, `NOT_INBOX_READ` ajoutées d'office (et à chaque recomposition). Rôle composite Keycloak (description, permissions, `ROLE_USER`, attribut `gimac_profil_cree`). La marque « créé » qui fait autorité est la table `profil_cree` : elle s'écrit dans la transaction du journal (un refus de Keycloak annule l'inscription), se lit sans appel au fournisseur d'identité à chaque contrôle des profils attribuables et ne dépend pas d'un attribut modifiable hors de la plateforme ; l'attribut du rôle n'est qu'un repère lisible dans le realm. Attribuables = configuration ∪ profils créés ; privilégiés = configuration ∪ profils créés privilégiés. Suppression : profil livré 409 `IAM_PROFIL_SYSTEME`, inconnu 404, avec titulaires 409 `IAM_PROFIL_TITULAIRES` ; il quitte la matrice. Journal `CONFIGURATION_PROFIL_CREE`, `CONFIGURATION_PROFIL_SUPPRIME`.

### Profils et organisation configurables (D-86, docs/lot1/profils-organisation.md §2 et §4)

- **Libellé et description** (`ProfilsService.modifier`, `ReglesProfils.libelle` / `description`) : libellé 1 à 80, description 0 à 500 (espaces retirés, 400 `IAM_PROFIL_INVALIDE`), motif obligatoire (400 `IAM_REASON_REQUIRED`), profil attribuable (404 `IAM_PROFILE_NOT_FOUND`), privilégié réservé à `IAM_ROLE_PRIVILEGED_GRANT` (403 `IAM_PRIVILEGED_PROFILE`), code immuable. Rangés dans l'attribut `gimac_libelle` et la description du rôle Keycloak ; journal `CONFIGURATION_PROFIL_MODIFIE` (cible : le code ; détails `avant: libelle=… ; description=…, après: …`) et `iam.configuration-changed.v1` (objet `PROFIL_MODIFIE`). Libellé absent : le code en tient lieu.
- **Trace** (table `profil_presentation`, adaptateur `PresentationsProfilsJdbc`) : date et auteur de la dernière modification (libellé, description, composition, création) rendus par `GET /configuration/profils` (`modifieLe`, `modifiePar: { id, nomAffiche }`, nuls si jamais modifié) ; libellé et description fixés depuis la console, que `RestaurationPresentationsProfils` réapplique au démarrage s'ils diffèrent de Keycloak (réimport du domaine par `infra/ci/fusionner-domaine.py`, qui remplace les rôles par ceux du dépôt) ; idempotent, sans effet si Keycloak est indisponible.
- **Jeu initial** : `infra/keycloak/generer-realm-gimac.py` donne à chaque profil livré son libellé et sa mission (fin de « Profil du lot 1 ») ; la fusion du domaine les applique au serveur.
- **Libellés exposés** : `libelle` dans chaque profil (`GET`, `POST`, `PUT /configuration/profils…`) et chaque ligne de la matrice ; `libellesProfils` (`{ CODE: libellé }` des profils demandables) dans `/pouvoirs/moi` et `/interne/pouvoirs/{id}`.
- **Matrice** : actions `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` (Liquibase `20261006170000_iam_profils_organisation.xml` : contrainte élargie, `UNITES` pour les six profils responsables, `ON CONFLICT DO NOTHING`).
- **Unité par son code** (`OrganisationService.unite`, D-87) : `{ code, libelle, type, parent, active, responsableId }`, active ou non ; le moteur de processus y lit le chef d'une unité, même sans membre ; unité inconnue 404 `ORG_UNITE_INTROUVABLE`.
- **Membres d'une unité** (`OrganisationService.membresUnite`, moteur de processus D-87) : comptes internes actifs rattachés à l'unité (et à ses sous-unités, actives ou non, par défaut), filtrés par profil, triés par nom, 500 au plus ; unité inconnue 404 `ORG_UNITE_INTROUVABLE`.

### Routes (`/api/v1`)

| Route | Exposée sous | Permission |
|---|---|---|
| `GET /comptes`, `/comptes/indicateurs`, `/comptes/{id}`, `/comptes/{id}/sessions` | `/bff/console/iam` | `IAM_ACCOUNT_READ` |
| `GET /comptes/journal` | `/bff/console/iam` | `IAM_JOURNAL_READ` |
| `POST /comptes` | `/bff/console/iam` | `IAM_ACCOUNT_CREATE` |
| `PUT /comptes/{id}` | `/bff/console/iam` | `IAM_ACCOUNT_EDIT` |
| `PUT /comptes/{id}/rattachement` (`{unite, responsableId}`, réponse : la fiche du compte) | `/bff/{des,console}/iam` | `IAM_ACCOUNT_EDIT` |
| `GET /organisation/unites[?actives=true]` | `/bff/{des,console}/iam` | `PIL_ACTIVITY_OWN` ou `IAM_ACCOUNT_READ` |
| `POST /organisation/unites` (201), `PUT /organisation/unites/{code}` | `/bff/{des,console}/iam` | `IAM_ACCOUNT_EDIT` |
| `GET /organisation/moi` | `/bff/des/iam` | `PIL_ACTIVITY_OWN` |
| `GET /interne/organisation/{id}`, `GET /interne/organisation/{id}/equipe` | jamais relayée | jeton de service `INTERNAL_IAM_READ` (MS-PILOTAGE, règles d'équipe) |
| `PUT /comptes/{id}/profils` (FIDO2) | `/bff/console/iam` | `IAM_ACCOUNT_ROLES` |
| `POST /comptes/{id}/activation`, `/desactivation` | `/bff/console/iam` | `IAM_ACCOUNT_STATUS` |
| `DELETE /comptes/{id}?motif=` (FIDO2) | `/bff/console/iam` | `IAM_ACCOUNT_DELETE` |
| `POST /comptes/{id}/actions-requises`, `/second-facteur/reinitialisation`, `/mot-de-passe-provisoire`, `/sessions/fermeture` | `/bff/console/iam` | `IAM_ACCOUNT_SECURITY` |
| `GET` / `PUT` (FIDO2) `/configuration/politique-securite` | `/bff/console/iam` | `PLATFORM_SECURITY_POLICY_READ` / `_EDIT` |
| `GET` / `PUT /configuration/messagerie`, `POST /configuration/messagerie/test` | `/bff/console/iam` | `PLATFORM_MESSAGING_EDIT` |
| `GET /configuration/profils`, `/configuration/permissions` | `/bff/console/iam` | `IAM_ACCOUNT_READ` |
| `PUT /configuration/profils/{code}/permissions` (FIDO2) | `/bff/console/iam` | `IAM_PROFILE_EDIT` |
| `PUT /configuration/profils/{code}` (`{ libelle, description, motif }`, FIDO2 ; réponse : le profil) | `/bff/{des,console}/iam` | `IAM_PROFILE_EDIT` (privilégié : `IAM_ROLE_PRIVILEGED_GRANT`) |
| `GET /interne/organisation/unites/{code}` | jamais relayée | jeton de service `INTERNAL_IAM_READ` (MS-PILOTAGE, chef d'une unité, D-87) |
| `GET /interne/organisation/unites/{code}/membres?profil=&sousUnites=true` | jamais relayée | jeton de service `INTERNAL_IAM_READ` (MS-PILOTAGE, processus d'unité) |
| `GET /pouvoirs/matrice` | `/bff/{des,console}/iam` | `IAM_ACCOUNT_READ` |
| `PUT /pouvoirs/matrice/{profil}` (FIDO2) | `/bff/{des,console}/iam` | `IAM_PROFILE_EDIT` |
| `GET /pouvoirs/moi` | `/bff/{des,console}/iam` | `PIL_ACTIVITY_OWN` |
| `PUT /pouvoirs/subordonnes/{id}/rattachement` (`{ unite, responsableId, motif? }`, action `RATTACHEMENT_MODIFIER`) | `/bff/des/iam` | `PIL_ACTIVITY_OWN` |
| `GET /interne/pouvoirs/{id}`, `GET /interne/organisation/{id}/perimetre?portee=` | jamais relayée | jeton de service `INTERNAL_IAM_READ` |
| `POST /demandes` (201) | `/bff/{des,console}/iam` | `PIL_ACTIVITY_OWN` (+ matrice) |
| `GET /demandes?vue=miennes\|a-traiter&etat=&page=&taille=`, `GET /demandes/{id}` | `/bff/{des,console}/iam` | `PIL_ACTIVITY_OWN` ou `IAM_ACCOUNT_READ` (`a-traiter` et demande d'autrui : `IAM_ACCOUNT_READ`) |
| `POST /demandes/{id}/annulation` | `/bff/{des,console}/iam` | `PIL_ACTIVITY_OWN`, demandeur seul |
| `POST /demandes/{id}/acceptation` (FIDO2) | `/bff/{des,console}/iam` | `IAM_ACCOUNT_CREATE`, `IAM_ACCOUNT_ROLES` ou `IAM_ACCOUNT_STATUS` (celle de l'acte) |
| `POST /demandes/{id}/refus` | `/bff/{des,console}/iam` | `IAM_ACCOUNT_READ` |
| `POST /configuration/profils` (201, FIDO2), `DELETE /configuration/profils/{code}?motif=` (204, FIDO2) | `/bff/{des,console}/iam` | `IAM_PROFILE_EDIT` |
| `PUT /moi/avatar` (multipart `image`), `DELETE /moi/avatar`, `GET /moi/avatar` | `/bff/{des,portail,console}/iam` | titulaire à deux facteurs |
| `GET /avatars/{compteId}?v=` | `/bff/{des,console}/iam` | tout compte interne à deux facteurs ; un correspondant, sa propre photo seulement |
| `GET /moi`, `POST /moi/premiere-configuration`, `POST /moi/mot-de-passe`, `POST /moi/second-facteur/totp/enrolement`, `POST /moi/second-facteur/totp` | `/bff/{des,portail,console}/iam` | tout utilisateur ; `acr` 1 admis (session restreinte du premier accès, D-65) |
| `PUT /moi`, `/moi/actions`, `/moi/sessions`, `/moi/cles-securite` | `/bff/{des,portail,console}/iam` | tout utilisateur à deux facteurs |
| `POST /reauthentification/defi` | `/bff/{des,portail,console}/iam` | tout utilisateur à deux facteurs |
| `GET /interne/comptes/{id}/etat-connexion`, `POST /interne/connexion/mot-de-passe-oublie` (202 toujours), `POST /interne/connexion/nouveau-mot-de-passe` (204 ; 400 `CNX_JETON_INVALIDE`, `IAM_PASSWORD_POLICY`, `IAM_PASSWORD_REUSED`) | jamais relayée | jeton de service `INTERNAL_IAM_READ` émis pour `gateway-service` (claim `azp`) seulement (D-65) |
| `POST /interne/reauthentifications/verification` | jamais relayée | jeton de service `INTERNAL_IAM_READ` |
| `GET /interne/collaborateurs/{id}`, `GET /interne/collaborateurs?profil=` | jamais relayée | jeton de service `INTERNAL_IAM_READ` (annuaire IAM-04 : profils, rattachement, activité ; conducteurs de projet, D-58) |
| `POST /interne/comptes/{id}/alerte-connexion` (204) | jamais relayée | jeton de service `INTERNAL_IAM_READ` émis pour `gateway-service` seulement |
| `GET /interne/collaborateurs/avatars?ids=` | jamais relayée | jeton de service `INTERNAL_IAM_READ` : versions des photos d'un lot de comptes |
| `GET /interne/collaborateurs?recherche=&page=&taille=` | jamais relayée | jeton de service `INTERNAL_IAM_READ` : comptes internes actifs (sans institution), filtrés par nom, courriel ou fonction, page `{contenu, total, page, taille}` ; destinataires de la messagerie de MS-COLLABORATION (D-64) |

Erreurs : ProblemDetail `application/problem+json` (`code`, `message`, `details`, `correlationId` ; `violations` au premier niveau pour la politique des mots de passe). Codes D-65 : `CNX_JETON_INVALIDE`, `IAM_PASSWORD_POLICY`, `IAM_PASSWORD_REUSED`, `IAM_PASSWORD_INVALID`, `IAM_TOTP_CODE_INVALID`, `IAM_TOTP_ENROLMENT_EXPIRED`, `IAM_SMTP_FAILED`. Sécurité (D-61, D-65) : `/api/v1/**` exige un jeton à deux facteurs (`acr` 2 ou 3), sauf les cinq routes de l'assistant de premier accès qui admettent `acr` 1 ; `/api/v1/interne/**` un jeton de service ; le CRUD généré `/api/**` le rôle `ROLE_ADMIN`.

### Événements (outbox, CloudEvents)

`iam.account-administered.v1` (sujet `iam.account`), `iam.configuration-changed.v1` (`iam.configuration`), `iam.reauthentication-verified.v1` (`iam.reauthentication`), `iam.avatar-changed.v1` (`iam.avatar` ; `compteId`, `identifiant`, `action` REMPLACEE ou RETIREE, `version`, `acteurId`, jamais l'image), `iam.request-submitted.v1` et `iam.request-decided.v1` (`iam.request`, clé = demande ; D-81 : `{demandeId, type, etat, demandeurId, cibleId, destinataires, le}` et `{demandeId, type, etat, demandeurId, cibleId, acteurId, destinataires: [demandeurId], le}` ; destinataires de la soumission : comptes actifs dont un profil porte `IAM_ACCOUNT_CREATE` ou `IAM_ACCOUNT_ROLES`, sauf le demandeur ; ni contenu ni motif).

## Tests : définition de fini (D-61)

| Type | Tests |
|---|---|
| Unitaires | `ReglesPouvoirsTest`, `ReglesDemandesEtProfilsTest`, `PouvoirsServiceTest` (D-81 : portées, périmètres, unités dirigées, escalade interdite, demandes, profils créés), `TraitementAvatarTest` (signatures, WebP, EXIF/GPS et commentaires supprimés, recadrage, limites), `AvatarServiceTest`, `TotpEtJetonsTest` (vecteurs de la RFC 6238, base 32, jetons, violations), `ConnexionServiceTest`, `ChiffrementSecretsTest`, `CourrielsTest`, `KeycloakAnnuaireTest`, `ReglesAdministrationTest`, `PolitiqueMotDePasseTest`, `AdministrationComptesServiceTest`, `MonCompteEtConfigurationTest`, `JournalActesTest`, `ReauthentificationServiceTest` (authentificateur FIDO2 émulé, webauthn4j-test : enrôlement, assertion, non-rejeu, sujet et finalité liés, expiration) |
| Sécurité | `IamRoutesSecurityTest` (toutes les routes : 401, 403 sans second facteur, 403 sans la permission, accès avec elle, 428 et 403 de la réauthentification, routes internes ; D-65 : `acr` 1 admis sur les seules routes de l'assistant, routes internes de la connexion réservées à `gateway-service`, formes problem+json), `TechnicalStructureTest` |
| Intégration | `PouvoirsHierarchiquesIT` (D-81 de bout en bout : matrice sous FIDO2 et journal, pouvoirs et périmètres, demandes 403 / 422 / 409, acceptation qui crée le compte, refus, annulation, `iam.request-*` relayés, profils créés et supprimés), `MonCompteAvatarIT` (photo JPEG avec GPS réencodée sans métadonnées, WebP, refus par les octets, versions dans `/moi`, la console et l'annuaire, journal et `iam.avatar-changed.v1` sans l'image, retrait ; fermeture des autres sessions et courriel d'information ; alerte « nouvel appareil »), `ConnexionPlateformeIT` (D-65 de bout en bout : invitation, nouveau mot de passe, assistant en `acr` 1, code TOTP, mot de passe oublié en espagnol, journal sans secret), `ParcoursAdministrationIT` (PostgreSQL et Kafka par Testcontainers, clé FIDO2 émulée de bout en bout), `KeycloakAnnuaireIT` (vrai Keycloak 26.7.4, domaine `gimac` généré et extension `gimac-totp` : attributs du profil utilisateur, actions de la plateforme, mot de passe vérifié par `gimac-connexion` et refus de la politique, code TOTP enregistré, profils, profils créés et supprimés, membres d'une unité, politique, messagerie, erreurs), CRUD généré |
| Recette | `RecetteGherkinIT` : `administration-comptes.feature`, `premier-acces.feature` (dont trois scénarios D-65 : lien d'invitation à usage unique, code TOTP en session restreinte, routes fermées en session restreinte) |

Seuils JaCoCo (80 %) : à `test`, chaque classe de la tranche en lignes et en méthodes, l'ensemble en branches ; à `verify`, tout le service en lignes et en méthodes hors MapStruct. `KeycloakAnnuaireIT` démarre Keycloak par Testcontainers (`quay.io/keycloak/keycloak:26.7.4-0`, attente sur la sonde `/health/ready`, 10 min au plus sous charge) avec le jar de l'extension `gimac-totp` (construit au besoin par le Maven Wrapper) ou utilise un Keycloak déjà lancé : `GIMAC_KEYCLOAK_IT_URL`, `GIMAC_KEYCLOAK_IT_SECRET` (secret du client `iam-service`), `GIMAC_KEYCLOAK_IT_SECRET_CONNEXION` (secret de `gimac-connexion`).

## Lancer

- Service seul, en développement : `./mvnw` (dépendances : `docker compose -f src/main/docker/services.yml up -d`).
- Avec toute la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

Copié et adapté (paquet `cm.gimac.des.iam`) ; chaque bean est inactif tant que sa propriété ne vaut pas `true`.

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `config/DecodeurJwtInterne` | `audit-service/src/main/java/com/afriland/audit/config/DecodeurJwtInterne.java` | sans bean à activer |
| `config/KafkaErrorHandlingConfiguration` | `audit-service/src/main/java/com/afriland/audit/config/KafkaErrorHandlingConfiguration.java` | `gimac.kafka.rebut.actif` |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/src/main/java/com/afriland/notifications/infrastructure/logging/PiiMaskingLogConverter.java` | sans bean à activer |
| `adapter/outbound/port/KeycloakAdminPort` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/KeycloakAdminPort.java` | sans bean à activer |
| `adapter/outbound/port/GroupeAgenceKc` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/GroupeAgenceKc.java` | sans bean à activer |
| `adapter/outbound/port/IdentityProviderInfo` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/IdentityProviderInfo.java` | sans bean à activer |
| `adapter/outbound/port/ProfilKeycloak` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/ProfilKeycloak.java` | sans bean à activer |
| `adapter/outbound/port/SessionInfo` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/SessionInfo.java` | sans bean à activer |
| `adapter/outbound/port/UtilisateurRealm` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/UtilisateurRealm.java` | sans bean à activer |
| `adapter/outbound/keycloak/KeycloakAdminAdapter` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/keycloak/KeycloakAdminAdapter.java` | `gimac.iam.keycloak-admin.actif` |
| `config/KeycloakConfig` | `utilisateurs-service/src/main/java/com/afriland/users/config/KeycloakConfig.java` | `gimac.iam.keycloak-admin.actif` |
| `config/KeycloakProperties` | `utilisateurs-service/src/main/java/com/afriland/users/config/KeycloakProperties.java` | sans bean à activer |
| `config/KeycloakClientRegistrationConfig` | `utilisateurs-service/src/main/java/com/afriland/users/config/KeycloakClientRegistrationConfig.java` | `gimac.iam.keycloak-admin.actif` |
| `adapter/outbound/securite/ServiceTokenProvider` | `administration-service/src/main/java/com/afriland/admin/adapter/outbound/cognitive/ServiceTokenProvider.java` | `gimac.securite.jeton-service.actif` |

Écarts comblés (D-53) : `ValidateurLiaisonCertificat` et option du décodeur exigeant la liaison au certificat (RFC 8705) ; masquage des journaux limité aux numéros vérifiant Luhn, en 6+4 (RG-52).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | c1b7131 | Génération JHipster 9.4.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 11 classes reprises d'afriland : GroupeAgenceKc, IdentityProviderInfo, KeycloakAdminAdapter, KeycloakAdminPort, KeycloakClientRegistrationConfig, KeycloakConfig, KeycloakProperties, ProfilKeycloak, ServiceTokenProvider, SessionInfo, UtilisateurRealm (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 101 tests unitaires et 375 tests d'intégration réussis, image Jib construite ; `KeycloakProperties` déplacée dans l'adaptateur (test d'architecture) ; `List.of` (contrôle de modernité) (D-54). |
| 2026-09-29 | — | Permissions métier (HOM_*, CER_*, INTERNAL_*) acceptées comme autorités, en plus de ROLE_* (D-55). |
| 2026-09-29 | — | Administration des comptes et de l'authentification depuis la plateforme (D-62) : annuaire Keycloak, premier accès, « Mon compte », politique de sécurité, messagerie, profils, journal ; réauthentification forte FIDO2 (IAM-07). 170 tests unitaires et de sécurité, 396 d'intégration et de recette réussis ; seuils JaCoCo atteints. |
| 2026-09-29 | — | Ports sortants `application/port/out` versionnés : la règle `out/` du `.gitignore` généré par JHipster les ignorait, le dépôt distant ne compilait pas ; exception `!src/**/out/` ajoutée et port `Annuaire` reconstitué d'après `KeycloakAnnuaire` et `AnnuaireEnMemoire` |
| 2026-09-30 | — | D-65 : connexion et configuration des comptes sur la plateforme (état de connexion, mot de passe oublié et nouveau mot de passe par lien à usage unique, courriels fr/en/es envoyés par MS-IAM, changement de mot de passe, enrôlement TOTP par l'extension `gimac-totp`, actions dans `gimac_actions_requises`, `acr` 1 limité à l'assistant) ; tables `jeton_lien`, `enrolement_totp`, `parametre_chiffre`. Résultats : voir le rapport de la tranche. |
| 2026-09-30 | — | Annuaire interne de la messagerie (D-64) : `GET /api/v1/interne/collaborateurs?recherche=&page=&taille=` (comptes internes actifs, recherche par nom, courriel ou fonction, pagination), la liste `?profil=` inchangée ; tests unitaire, de sécurité et d'intégration (`CollaborateursInternesIT`). |
| 2026-10-01 | — | « Mon compte » (docs/lot1/mon-compte.md) : photo de profil (`PUT`/`DELETE`/`GET /moi/avatar`, `GET /avatars/{compteId}`, type par les octets, 2 Mo, 64 à 4 096 px, carré 256 × 256 réencodé en PNG sans EXIF ni GPS, table `avatar_compte`, `iam.avatar-changed.v1`), `avatarVersion` dans `/moi`, la console et l'annuaire (et par lot : `/interne/collaborateurs/avatars`), `fermerAutresSessions` et courriel d'information au changement de mot de passe, alerte « nouvel appareil » (`/interne/comptes/{id}/alerte-connexion`, `gateway-service`) ; WebP lu par TwelveMonkeys `imageio-webp` 3.12.0. `./mvnw -ntp -B verify` : 238 tests unitaires et de sécurité, 407 d'intégration et de recette réussis ; couverture fusionnée hors MapStruct 87,8 % en lignes, 87,6 % en méthodes ; code nouveau ou modifié 98,1 % en lignes, 84,8 % en branches. |
| 2026-10-05 | - | Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1) : catalogue `unite_organisationnelle` et jeu initial, routes `/organisation/unites`, `/organisation/moi`, `PUT /comptes/{id}/rattachement`, `/interne/organisation/{id}` et `/{id}/equipe` ; attributs Keycloak `unite` et `responsable_id` ; `unite` et `responsableId` dans la fiche des comptes, « Mon compte » et l'annuaire interne. `./mvnw -q verify` : 256 tests unitaires et de sécurité, 410 d'intégration et de recette réussis. |
| 2026-10-06 | — | Rattachement à une institution réservé au correspondant de participant et obligatoire pour lui (`ReglesAdministration.verifierRattachement`, création, fiche, profils) : un agent saisi avec l'institution « GIMAC » disparaissait de la messagerie ; 257 tests, seuils atteints |
| 2026-10-06 | — | Tableaux de bord en graphiques (lot 5) : `GET /api/v1/comptes/indicateurs` rend aussi `creationsParJour` et `actesParJour`, 30 jours denses au fuseau de Douala ; tests d'intégration `OrganisationIT` et `CollaborateursInternesIT` alignés sur la règle du correspondant (compte rattaché à une institution) |
| 2026-10-06 | - | Pouvoirs des chefs (D-81, docs/lot1/pouvoirs-hierarchiques.md §2 à §5) : responsable d'unité et unités dirigées, périmètres, matrice des pouvoirs et jeu par défaut, `/pouvoirs/*`, `/interne/pouvoirs/{id}`, `/interne/organisation/{id}/perimetre`, demandes à l'administration et `iam.request-*`, création et suppression de profils ; tables `pouvoir_profil`, `profil_demandable`, `demande_administration`, `profil_cree`. `./mvnw verify` : 277 tests unitaires et de sécurité, 415 d'intégration et de recette réussis (dont `KeycloakAnnuaireIT` face au vrai Keycloak), seuils JaCoCo atteints. |
| 2026-10-06 | - | Référentiels métier, tous domaines (D-92) : portée `TOUTES` de la matrice (processus d'unité seulement, 422 `ORG_PORTEE_NON_ADMISE`, contrainte de la base), `porteesAdmises`, périmètre `TOUTES` ; `REF_METIER_ADMIN` permission ordinaire non privilégiée (substitut de l'annuaire). Tests `ReglesPouvoirsTest`, `PouvoirsServiceTest`, `DemandesEtProfilsServiceTest`, `PouvoirsDemandesRoutesTest`, `OrganisationRoutesTest`, `ReferentielsMetierAdminIT`. |
| 2026-10-06 | - | Profils et organisation configurables (D-86, docs/lot1/profils-organisation.md) : `PUT /configuration/profils/{code}` (libellé `gimac_libelle` et description du rôle, FIDO2, journal `CONFIGURATION_PROFIL_MODIFIE`), `libelle`, `modifieLe`, `modifiePar` dans les profils, `libelle` dans la matrice, `libellesProfils` dans les pouvoirs, actions `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER`, `/interne/organisation/unites/{code}/membres`, table `profil_presentation` et réapplication au démarrage. `./mvnw verify` : 290 tests unitaires et de sécurité, 420 d'intégration et de recette réussis (dont `KeycloakAnnuaireIT` face au vrai Keycloak), seuils JaCoCo atteints. |
| 2026-10-13 | D-105 | Profil `ROLE_AGENT_SSI` (agent de la sécurité des SI) attribuable ; changelog `20261013120000_iam_agent_ssi.xml` : demandable par le RSSI et la direction générale ; annuaire en mémoire aligné |
| 2026-10-13 | D-107 | D-107 : fonctionnalité `repriseExistant` (droit `REF_REPRISE_IMPORT`, sans écran) dans `fonctionnalites-initiales.json` |
| 2026-10-19 | D-114 | Catalogue des fonctionnalités : `monitoringSupervision`, `monitoringAstreintes`, `monitoringReleve` (groupe `monitoring`, prédicat `MON_SUPERVISION_READ`) |
| 2026-10-19 | D-115 | `ReglesSandbox` : permissions `SBX_<MODULE>_<FONCTION>`, validation des verdicts refusée à tout profil externe (422 `IAM_SBX_VALIDATION_RESERVEE`) à la création, à la recomposition et au passage en externe ; `GET /configuration/permissions?prefixe=` ; six profils `ROLE_SBX_*` attribuables et externes ; module sandbox du catalogue des fonctionnalités |
| 2026-10-20 | D-115 | Administration déléguée de la sandbox (`AdministrationDelegueeService`, `ReglesAdministrationDeleguee`) : comptes bornés à l'institution du jeton, rôles délégables, jamais sur soi-même, RG-15 ; clé d'idempotence des étapes de saga (table `etape_idempotente`, changeset `20261019180000_iam_sandbox_delegation`) |
<!-- GIMAC:FIN -->
| 2026-10-20 | - | D-116 : catalogue des fonctionnalités, entrées monitoringAujourdhui, monitoringRapports, monitoringActivite ; supervision et relève passent en visible: false |

# iam

This application was generated using JHipster 9.4.0, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v9.4.0](https://www.jhipster.tech/documentation-archive/v9.4.0).

This is a "microservice" application intended to be part of a microservice architecture, please refer to the [Doing microservices with JHipster][] page of the documentation for more information.

## Project Structure

Node is required for generation and recommended for development. `package.json` is always generated for a better development experience with prettier, commit hooks, scripts and so on.

In the project root, JHipster generates configuration files for tools like git, prettier, eslint, husky, and others that are well known and you can find references in the web.

`/src/*` structure follows default Java structure.

- `.yo-rc.json` - Yeoman configuration file
  JHipster configuration is stored in this file at `generator-jhipster` key. You may find `generator-jhipster-*` for specific blueprints configuration.
- `.yo-resolve` (optional) - Yeoman conflict resolver
  Allows to use a specific action when conflicts are found skipping prompts for files that matches a pattern. Each line should match `[pattern] [action]` with pattern been a [Minimatch](https://github.com/isaacs/minimatch#minimatch) pattern and action been one of skip (default if omitted) or force. Lines starting with `#` are considered comments and are ignored.
- `.jhipster/*.json` - JHipster entity configuration files
- `/src/main/docker` - Docker configurations for the application and services that the application depends on

## Development

To start your application in the dev profile, run:

```bash
./mvnw
```

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### OAuth 2.0 / OpenID Connect

Congratulations! You've selected an excellent way to secure your JHipster application. If you're not sure what OAuth and OpenID Connect (OIDC) are, please see [What the Heck is OAuth?](https://developer.okta.com/blog/2017/06/21/what-the-heck-is-oauth)

To log in to your app, you'll need to have [Keycloak](https://keycloak.org) up and running. The JHipster Team has created a Docker container for you that has the default users and roles. Start Keycloak using the following command.

```bash
docker compose -f src/main/docker/keycloak.yml up
```

The security settings in `src/main/resources/config/application.yml` are configured for this image.

```yaml
spring:
  ...
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: http://localhost:9080/realms/jhipster
        registration:
          oidc:
            client-id: web_app
            client-secret: web_app
            scope: openid,profile,email
```

Some of Keycloak configuration is now done in build time and the other part before running the app, here is the [list](https://www.keycloak.org/server/all-config) of all build and configuration options.

Before moving to production, please make sure to follow this [guide](https://www.keycloak.org/server/configuration) for better security and performance.

Also, you should never use `start-dev` nor `KC_DB=dev-file` in production.

When using Kubernetes, importing should be done using init-containers (with a volume when using `db=dev-file`).

### Okta

If you'd like to use Okta instead of Keycloak, it's pretty quick.

First, you'll need to create a free developer account at <https://developer.okta.com/signup/>. After doing so, you'll get your own Okta domain, that has a name like `https://dev-123456.okta.com`.

Modify `src/main/resources/config/application.yml` to use your Okta settings.

```yaml
spring:
  ...
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: https://{yourOktaDomain}/oauth2/default
        registration:
          oidc:
            client-id: {clientId}
            client-secret: {clientSecret}
security:
```

Create an OIDC App in Okta to get a `{clientId}` and `{clientSecret}`. To do this, log in to your Okta Developer account and navigate to **Applications** > **Add Application**. Click **Web** and click the **Next** button. Give the app a name you’ll remember, specify `http://localhost:8080` as a Base URI, and `http://localhost:8080/login/oauth2/code/oidc` as a Login Redirect URI. Click **Done**, then Edit and add `http://localhost:8080` as a Logout redirect URI. Copy and paste the client ID and secret into your `application.yml` file.

Create a `ROLE_ADMIN` and `ROLE_USER` group and add users into them. Modify e2e tests to use this account when running integration tests.

Navigate to **API** > **Authorization Servers**, click the **Authorization Servers** tab and edit the default one. Click the **Claims** tab and **Add Claim**. Name it "groups", and include it in the ID Token. Set the value type to "Groups" and set the filter to be a Regex of `.*`.

After making these changes, you should be good to go! If you have any issues, please post them to [Stack Overflow](https://stackoverflow.com/questions/tagged/jhipster). Make sure to tag your question with "jhipster" and "okta".

### Auth0

If you'd like to use [Auth0](https://auth0.com/) instead of Keycloak, follow the configuration steps below:

- Create a free developer account at [Sign Up - Auth0](https://auth0.com/signup). After successful sign-up, your account will be associated with a unique domain like `dev-xxx.us.auth0.com`
- Create a new application of type `Regular Web Applications`. Switch to the `Settings` tab, and configure your application settings like:
  - Allowed Callback URLs: `http://localhost:8080/login/oauth2/code/oidc`
  - Allowed Logout URLs: `http://localhost:8080/`
- Navigate to **User Management** > **Roles** and create new roles named `ROLE_ADMIN`, and `ROLE_USER`.
- Navigate to **User Management** > **Users** and create a new user account. Click on the **Role** tab to assign roles to the newly created user account.
- Navigate to **Auth Pipeline** > **Rules** and create a new Rule. Choose `Empty rule` template. Provide a meaningful name like `JHipster claims` and replace `Script` content with the following and Save.

```javascript
function (user, context, callback) {
  user.preferred_username = user.email;
  const roles = (context.authorization || {}).roles;

  function prepareCustomClaimKey(claim) {
    return `https://www.jhipster.tech/${claim}`;
  }

  const rolesClaim = prepareCustomClaimKey('roles');

  if (context.idToken) {
    context.idToken[rolesClaim] = roles;
  }

  if (context.accessToken) {
    context.accessToken[rolesClaim] = roles;
  }

  callback(null, user, context);
}
```

- In your `JHipster` application, modify `src/main/resources/config/application.yml` to use your Auth0 application settings:

```yaml
spring:
  ...
  security:
    oauth2:
      client:
        provider:
          oidc:
            # make sure to include the ending slash!
            issuer-uri: https://{your-auth0-domain}/
        registration:
          oidc:
            client-id: {clientId}
            client-secret: {clientSecret}
            scope: openid,profile,email
jhipster:
  ...
  security:
    oauth2:
      audience:
        - https://{your-auth0-domain}/api/v2/
```

## Building for production

### Packaging as jar

To build the final jar and optimize the iam application for production, run:

```bash
./mvnw -Pprod clean verify
```

To ensure everything worked, run:

```bash
java -jar target/*.jar
```

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

```bash
./mvnw -Pprod,war clean verify
```

### JHipster Control Center

JHipster Control Center can help you manage and control your application(s). You can start a local control center server (accessible on http://localhost:7419) with:

```bash
docker compose -f src/main/docker/jhipster-control-center.yml up
```

## Testing

### Spring Boot tests

To launch your application's tests, run:

```bash
./mvnw verify
```

## Others

### Code quality using Sonar

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```bash
docker compose -f src/main/docker/sonar.yml up -d
```

Note: we have turned off forced authentication redirect for UI in [src/main/docker/sonar.yml](src/main/docker/sonar.yml) for out of the box experience while trying out SonarQube, for real use cases turn it back on.

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven plugin.

Then, run a Sonar analysis:

```bash
./mvnw -Pprod clean verify sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase since Sonar properties are loaded from the sonar-project.properties file.

```bash
./mvnw initialize sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

Additionally, Instead of passing `sonar.password` and `sonar.login` as CLI arguments, these parameters can be configured from [sonar-project.properties](sonar-project.properties) as shown below:

```bash
sonar.login=admin
sonar.password=admin
```

For more information, refer to the [Code quality page][].

### Docker Compose support

JHipster generates a number of Docker Compose configuration files in the [src/main/docker/](src/main/docker/) folder to launch required third party services.

For example, to start required services in Docker containers, run:

```bash
docker compose -f src/main/docker/services.yml up -d
```

To stop and remove the containers, run:

```bash
docker compose -f src/main/docker/services.yml down
```

[Spring Docker Compose Integration](https://docs.spring.io/spring-boot/reference/features/dev-services.html) is enabled by default. It's possible to disable it in `application.yml`:

```yaml
spring:
  ...
  docker:
    compose:
      enabled: false
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a Docker image of your app by running:

```bash
npm run java:docker
```

Or build an arm64 Docker image when using an arm64 processor OS, i.e., Apple Silicon chips (M*), running:

```bash
npm run java:docker:arm64
```

Then run:

```bash
docker compose -f src/main/docker/app.yml up -d
```

For more information refer to [Docker and Docker-Compose](https://www.jhipster.tech/documentation-archive/v9.4.0/docker-compose/), this page also contains information on the Docker Compose sub-generator (`jhipster docker-compose`), which is able to generate Docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration](https://www.jhipster.tech/documentation-archive/v9.4.0/setting-up-ci/) page for more information.

## References

- [JHipster Homepage and latest documentation](https://www.jhipster.tech/)
- [JHipster 9.4.0 archive](https://www.jhipster.tech/documentation-archive/v9.4.0)
- [Doing microservices with JHipster](https://www.jhipster.tech/documentation-archive/v9.4.0/microservices-architecture/)
- [Using JHipster in development](https://www.jhipster.tech/documentation-archive/v9.4.0/development/)
- [Using Docker and Docker-Compose](https://www.jhipster.tech/documentation-archive/v9.4.0/docker-compose)
- [Using JHipster in production](https://www.jhipster.tech/documentation-archive/v9.4.0/production/)
- [Running tests page](https://www.jhipster.tech/documentation-archive/v9.4.0/running-tests/)
- [Code quality page](https://www.jhipster.tech/documentation-archive/v9.4.0/code-quality/)
- [Setting up Continuous Integration](https://www.jhipster.tech/documentation-archive/v9.4.0/setting-up-ci/)
- [Node.js](https://nodejs.org/)
- [NPM](https://www.npmjs.com/)

## Écrans pilotés par la configuration (D-94)

Voir docs/lot1/ecrans-par-configuration.md, section « Mise en œuvre » : catalogue des fonctionnalités servi et
configurable (`GET` et `PUT /api/v1/configuration/fonctionnalites`, table `fonctionnalite`, valeurs de départ
`config/fonctionnalites-initiales.json`), attribut externe des profils (`gimac_externe`, `PUT
/api/v1/configuration/profils/{code}` avec `externe`), qualités des profils et des comptes déduites des permissions.
