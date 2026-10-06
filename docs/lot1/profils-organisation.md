# Profils, permissions et organisation entièrement configurables (D-86)

Demande de KOAGNE ISSA RYAN : les profils et les permissions doivent être dynamiques, configurables et
éditables (créer, lire, modifier, supprimer), adaptables à l'évolution du GIMAC ; on doit pouvoir définir qui
est subordonné à qui, qui est chef de qui, qui peut donner des tâches à qui. Arbitrage : **les profils livrés
se modifient mais ne se suppriment pas** ; seuls les profils créés par l'administration se suppriment (D-81 §5).
Complète D-62 (composition des profils), D-76 (unités, rattachements) et D-81 (matrice des pouvoirs,
responsable d'unité, profils créés).

## 1. Ce qui est déjà couvert (rappel)

| Besoin | Où |
|---|---|
| Composer un profil (ajouter, retirer des permissions), effet immédiat sur les titulaires | D-62 |
| Créer, supprimer un profil (créé, sans titulaire) | D-81 §5 |
| Qui peut confier des tâches à qui, suivre, lire, viser, demander, et avec quelle portée | D-81 §3 (matrice) |
| Unités : créer, modifier, désactiver, hiérarchie | D-76 §1.1 |
| Chef d'unité, n+1 de chaque compte | D-81 §2, D-76 §1.2 |

## 2. Profil : libellé, description, nature (MS-IAM)

Chaque profil (livré ou créé) porte un **libellé** (1..80) et une **description** (0..500) modifiables, en
plus de sa composition. Le code (`ROLE_…`) est immuable. `GET /configuration/profils` rend
`{ code, libelle, description, permissions, privilegie, cree, titulaires, modifieLe, modifiePar }`.

| Méthode | Chemin | Permission |
|---|---|---|
| PUT | `/configuration/profils/{code}` (`{ libelle, description, motif }`) | `IAM_PROFILE_EDIT` + FIDO2 |

Règles : un profil privilégié ne se modifie que par `IAM_ROLE_PRIVILEGED_GRANT` ; un profil livré ne se
supprime pas (409 `IAM_PROFIL_SYSTEME`, inchangé). Journal `CONFIGURATION_PROFIL_MODIFIE` (avant, après).
Le libellé est rangé dans l'attribut `gimac_libelle` du rôle Keycloak et repris partout où un profil est
affiché (listes, fiches, matrice, sélecteurs), au lieu des libellés figés de l'interface. Le jeu initial
reprend les libellés actuels (« Direction générale », « Pôle réseau »…) et remplace la description « Profil
du lot 1 » par la mission du profil.

## 3. Organigramme (interface, sur les routes existantes)

Nouvel écran **Organisation › Organigramme** (administration ; lecture pour tout porteur de
`IAM_ACCOUNT_READ`) :
- arbre des unités (créer, renommer, déplacer par glisser-déposer sous une autre unité, désactiver), chaque
  carte montrant le chef d'unité et l'effectif ;
- dans une unité : ses membres et leur n+1 sous forme d'arbre ; glisser un compte sur un autre en fait son
  n+1, le déposer sur une unité l'y rattache ; désigner le chef d'unité ;
- panneau « Pouvoirs » d'un compte : ses profils, et pour chaque action de la matrice sa portée effective et
  les personnes couvertes (qui il peut charger de tâches, qui il suit…), lien vers la ligne de la matrice ;
- tout geste ouvre une modale de confirmation (motif ; FIDO2 pour les gestes déjà soumis à réauthentification).
Les mêmes gestes restent possibles par les formulaires existants. Équivalent liste accessible au clavier.

**Réalisation de l'interface (frontend).**
- *Profils et permissions* : colonnes Libellé (libellé du serveur, code en dessous), Description, Nature,
  Titulaires, Permissions, Modifié (date et auteur, « non modifié » sinon). Bouton « Libellé » : modale
  « Modifier le profil » (libellé 1..80 obligatoire, description 0..500, contrôles annoncés avant l'envoi), puis
  motif et clé de sécurité, `PUT /configuration/profils/{code}`. Un profil livré n'a jamais de bouton
  « Supprimer » ; un profil créé ne l'a que sans titulaire. La création accepte un libellé facultatif.
- *Libellés* : partout où un profil est affiché (comptes, fiche d'un compte, matrice, demandes, organigramme,
  sélecteurs), l'interface prend le libellé du serveur et ne retombe sur son catalogue fr, en, es que si le
  libellé manque ou vaut le code. « Mon équipe » (couche `des`) lit les libellés des profils demandables dans
  `libellesProfils` de `GET /pouvoirs/moi` (champ facultatif, même repli).
- *Organigramme* (`/console/organigramme` et `/des/administration/organigramme`) : arbre des unités (chef et
  effectif sur chaque carte), panneau de l'unité (code, type, parente, effectif, case « Chef d'unité » où
  déposer un compte, membres en arbre des n+1), panneau « Pouvoirs » d'un compte. Glisser-déposer HTML5 natif ;
  une unité ne se dépose ni sur elle-même ni sur une sous-unité. La vue **Liste** offre les mêmes gestes au
  clavier (Déplacer, Désigner le chef, n+1, Unité) par des modales de choix ; une région annoncée (`aria-live`)
  décrit la saisie et la cible. Les comptes sont lus par pages de 100.
- *Écarts assumés* : les routes d'organisation actuelles ne reçoivent pas de motif ; la modale de
  confirmation le demande (motif de 10 caractères au moins) et l'aide le dit, mais il n'est pas transmis.
  Rattachement : `PUT /comptes/{id}/rattachement` reçoit toujours `unite` et `responsableId` (l'autre valeur
  reste inchangée) ; déplacement et chef d'unité : `PUT /organisation/unites/{code}` avec l'unité entière.
  Le panneau « Pouvoirs » calcule la portée effective (la plus large des profils du compte) et les personnes
  couvertes à partir de la matrice et de l'organisation affichées ; MS-IAM reste seul juge à chaque geste.

## 4. Matrice des pouvoirs : actions des processus d'unité

La matrice (D-81 §3) gagne deux actions, pour D-87 : `PROCESSUS_DEFINIR` (créer et publier les processus de
l'unité ; portée par défaut des responsables : `UNITES`) et `PROCESSUS_LANCER` (lancer un processus d'une
unité couverte ; défaut des responsables : `UNITES`). Tout membre d'une unité lance les processus publiés de
son unité sans pouvoir particulier.

## 5. Mise en œuvre (MS-IAM, domaine Keycloak, OPA)

Précisions apportées par la réalisation, sans changer le sens des §2 et §4.

- **Libellé et description** : `PUT /configuration/profils/{code}`, corps `{ libelle, description, motif }`. Libellé 1 à 80
  caractères et description 0 à 500 après retrait des espaces de tête et de fin (`description` absente ou `null` : vide),
  sinon 400 `IAM_PROFIL_INVALIDE` (détail `champ`) ; **motif obligatoire** comme pour la recomposition (400
  `IAM_REASON_REQUIRED`) ; profil inconnu 404 `IAM_PROFILE_NOT_FOUND` ; profil privilégié sans
  `IAM_ROLE_PRIVILEGED_GRANT` 403 `IAM_PRIVILEGED_PROFILE` ; réauthentification FIDO2 (`X-Reauth-Assertion`, 428 sans,
  403 si refusée). Réponse 200 : le profil, même forme que la liste. Journal `CONFIGURATION_PROFIL_MODIFIE` (cible : le
  code ; détails `avant: libelle=… ; description=…, après: libelle=… ; description=…`) et événement
  `iam.configuration-changed.v1` (objet `PROFIL_MODIFIE`).
- **Forme d'un profil** (`GET /configuration/profils`, réponses de `POST`, `PUT /configuration/profils/{code}` et
  `PUT …/permissions`) : `{ code, libelle, description, permissions, privilegie, titulaires, cree, modifieLe,
  modifiePar: { id, nomAffiche } }`. `libelle` vaut le code quand l'attribut `gimac_libelle` est absent ; `modifieLe` et
  `modifiePar` (dernière modification depuis la console : libellé, description, composition ou création) sont `null` pour
  un profil jamais modifié.
- **Création** (D-81 §5) : `POST /configuration/profils` accepte `libelle` (facultatif, 1 à 80 ; absent : le code) ;
  description obligatoire, désormais 500 caractères au plus.
- **Stockage** : attribut `gimac_libelle` et description du rôle composite Keycloak (les autres attributs, dont
  `gimac_profil_cree`, sont conservés). MS-IAM garde en plus chaque modification (table `profil_presentation` : libellé et
  description fixés, date, auteur).
- **Jeu initial** : `infra/keycloak/generer-realm-gimac.py` porte le libellé (repris de l'interface) et la mission de chaque
  profil livré ; « Profil du lot 1 » a disparu de `gimac-realm.json` et `gimac-realm-production.json`. Sur le serveur, le
  domaine est réimporté par fusion (`infra/ci/fusionner-domaine.py`) quand `gimac-realm.json` change : les rôles du dépôt
  (attributs et descriptions compris) remplacent ceux en service, le jeu initial est donc appliqué. La même fusion
  écraserait une modification faite depuis la console : au démarrage, MS-IAM réapplique à Keycloak les libellés et
  descriptions fixés par l'administration qui en diffèrent (idempotent ; sans effet si Keycloak est indisponible, la
  réapplication se fera au démarrage suivant). Une recomposition seule (permissions) ne fixe ni libellé ni description.
- **Libellés ailleurs** : chaque ligne de `GET /pouvoirs/matrice` et la réponse de `PUT /pouvoirs/matrice/{profil}`
  gagnent `libelle` ; `GET /pouvoirs/moi` et `GET /api/v1/interne/pouvoirs/{id}` gagnent `libellesProfils`
  (`{ "ROLE_…": "libellé" }` des profils demandables). Les comptes (`profils: [codes]`) et les demandes gardent les codes :
  l'interface les traduit par `GET /configuration/profils` (administration) ou `libellesProfils` (chef).
- **Matrice** (§4) : actions `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER`, à la suite des huit actions de D-81 (ordre de
  `actions` dans `GET /pouvoirs/matrice`). Changelog `20261006170000_iam_profils_organisation.xml` : contrainte de la table
  `pouvoir_profil` élargie, `UNITES` pour le chef du DES, le chef de la DDRM, la DG, le RSSI, la conformité et le
  responsable de service (une ligne déjà présente n'est pas modifiée) ; les autres profils et les profils créés :
  `AUCUNE`.
- **Membres d'une unité** (moteur de processus, D-87) : `GET /api/v1/interne/organisation/unites/{code}/membres?profil=
  ROLE_X&sousUnites=true`, jeton de service `INTERNAL_IAM_READ`, jamais relayée par la passerelle →
  `[ { id, nomAffiche, fonction, unite, responsableId, actif, profils: [codes] } ]`. Comptes internes actifs rattachés à
  l'unité et, par défaut (`sousUnites=true`), à toutes ses sous-unités (actives ou non) ; `profil` facultatif ; triés par
  nom affiché, 500 au plus ; unité inconnue 404 `ORG_UNITE_INTROUVABLE`.
- **Départ ou désactivation d'une personne** (§2 de processus-unite.md) : MS-IAM n'émet pas d'événement
  `iam.account-status-changed`. L'activation et la désactivation (route d'administration ou demande `STATUT` acceptée)
  publient `iam.account-administered.v1` sur le sujet `iam.account` (clé : identifiant du compte), enveloppe CloudEvents
  (`type`, `subject` = compte, `source` `/gimac/des/iam`), `data` =
  `{ action: "ACTIVATION" | "DESACTIVATION", compteId, identifiant, acteurId, motif, details: [] }` ; la suppression
  d'un compte publie le même type avec `action: "SUPPRESSION"`. Le sujet n'est pas encore créé par
  `infra/kafka/creer-sujets-lot1.sh` (création automatique du courtier sinon).
- **OPA** : `PUT ^/bff/(des|console)/iam/configuration/profils/[^/]+$` exige `IAM_PROFILE_EDIT` (la recomposition
  `…/permissions` et la création restent inchangées). Passerelle : déjà relayé par `console-iam` et
  `des-iam-administration`.
- **Déploiement du serveur, fusion à trois voies des rôles** (`infra/ci/fusionner-domaine.py`) : au réimport du
  domaine, la composition, la description et les attributs de chaque profil combinent l'état en service
  (administration faite dans la plateforme) et ce que le dépôt a changé depuis le déploiement précédent
  (`~/.local/state/gimac-des/gimac-realm.precedent.json`) ; les profils créés dans la plateforme sont conservés.
  Une recomposition (D-62), un profil créé (D-81) ou un libellé modifié (D-86) survivent donc à un déploiement.
