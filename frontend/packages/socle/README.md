# @gimac/socle

Socle d'interface commun aux trois couches (PI-GIMAC-01, fragment F0). Aucun écran métier ici : les briques que chaque lot consomme.

| Dossier | Contenu |
|---|---|
| `src/http/` | `client.ts`, seul point d'appel réseau vers la passerelle (`/bff/<couche>/<service>`) ; témoin de session HttpOnly envoyé seul, jeton anti-CSRF recopié dans `X-XSRF-TOKEN` sur les méthodes qui modifient, assertion FIDO2 dans `X-Reauth-Assertion` sans rejeu, erreurs normalisées en `ApiError` avec identifiant de corrélation ; `prefixes.ts` (`P`) ; `errors.ts` |
| `src/live/` | Canal poussé SSE : reprise depuis le dernier événement, invalidation des requêtes du sujet, reconnexion exponentielle plafonnée |
| `src/reauth/` | Réauthentification FIDO2 (`webauthn.ts` : défi `POST /bff/<couche>/iam/reauthentification/defi` `{purpose}`, `navigator.credentials.get`, en-tête `X-Reauth-Assertion`, finalité `ADMIN_SENSITIVE` des actes d'administration ; `refusReauth` classe 428, `IAM_REAUTH_INVALID`, `IAM_NO_SECURITY_KEY` et l'annulation) ; enrôlement d'une clé (`enrolement.ts` : options de `/moi/cles-securite/defi`, `navigator.credentials.create`, attestation en base64url) ; `ReauthCancelled` si l'utilisateur annule |
| `src/ressource/` | Types des descripteurs du lot (`ResourceDescriptorLot1`, `ActionContext` portant les services injectés), fabrique `dictionnaire` |
| `src/requete/` | `QueryBoundary` et `EtatsRequete` : les quatre états (chargement, vide, erreur avec corrélation et réessai, nominal) |
| `src/i18n/` | Catalogues **français, anglais, espagnol** (langues de la zone CEMAC), fusion avec ceux des lots |
| `src/enveloppe/` | Enveloppe commune (barre supérieure, navigation latérale filtrée par permissions, bandeau de perte du canal, espace personnel « Mon compte » et « Se déconnecter » avec `espacePersonnel`), préférences d'affichage (thème, langue) |
| `src/session/` | Session de la passerelle (`/api/session`) : matrice des rôles et niveau d'assurance ; `GardeSession` refuse l'accès sans second facteur (D-61) ; `useSession`, `usePermissions`, `aPermission` ; `deconnecter` (`POST /api/logout` puis `/connexion`), `surNonAutorise` (401, `CNX_SESSION_EXPIREE`), `SessionActive` et `SurveillanceInactivite` (fermeture après 30 minutes d'inactivité, avertissement à 28, `BroadcastChannel`, D-65), session de démonstration (`demo.ts`) et `PageDeconnexion` (D-62) |
| `src/connexion/` | Connexion sur la plateforme (D-65) : `PageConnexion` (identifiant et mot de passe, code, mot de passe oublié, nouveau mot de passe), port `ServiceConnexion`, `connexionHttp` (`/api/connexion/**`, CSRF), `connexionSimulee` (comptes de démonstration, mêmes états et codes), `ConnexionProvider`, politique des mots de passe |

Tout le rendu vient de `@gimac/design-system` ; le mode sombre et le mode clair découlent des jetons.

```
Couches : `des`, `portail`, `console` (`Couche`) ; `prefixeIam(couche)` donne `/bff/<couche>/iam` ; le client HTTP offre `delete`. `LangueComposants` aligne la langue des composants du catalogue sur celle d'i18next.

```
npm run test -w @gimac/socle   # client HTTP (F0 §B), session, réauthentification et enrôlement, déconnexion, catalogues FR/EN/ES
npm run couverture -w @gimac/socle   # 80 % bloquants (D-61) sur http, session et reauth
npm run lint -w @gimac/socle
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création : client HTTP corrigé après revue du F0, canal poussé, FIDO2, quatre états, i18n FR/EN/ES, enveloppe, connexion |
| 2026-09-29 | — | Session et garde à double facteur (D-61), préfixes du registre et de la gestion documentaire, pied de page GIMAC dans l'enveloppe ; 24 tests, couverture au-dessus de 80 % |
| 2026-09-29 | — | D-62 : couche `console`, préfixe IAM par couche, `http.delete`, finalité `ADMIN_SENSITIVE` et `refusReauth`, enrôlement WebAuthn (`enrolement.ts`), déconnexion et reconnexion par la passerelle, espace personnel de l'enveloppe, `LangueComposants` ; 40 tests, couverture de http, session et reauth au-dessus de 80 % |
| 2026-09-30 | — | D-65 : connexion sur la plateforme (`src/connexion/`), session restreinte, fermeture après inactivité, appels de fond `X-Gimac-Fond`, 401 `CNX_SESSION_EXPIREE` ; couverture étendue à `src/connexion/` (92 tests) |
| 2026-10-13 | - | D-106 : `permissionsEffectives` et `PERMISSIONS_EQUIVALENTES` (miroir d'OPA : `REF_METIER_ADMIN` vaut les permissions `HOM_REFERENTIEL_*`), appliqués par `SessionProvider` |
| 2026-10-20 | D-118 | `src/habillage.ts` : `HabillageConnexion` (fond, marque ou `null` pour une carte centrée, logo, nom de l'onglet, mention du pied), `HabillageEnveloppe` (logo, bandeau, pied), `ReglesAiguillage` et `AIGUILLAGE_PLATEFORME`, la règle par défaut. Propriétés facultatives de `PageConnexion`, `Enveloppe` et `Aiguillage`, une vérification par propriété (`src/habillage.test.tsx`, 14 tests). |
| 2026-10-22 | - | Thème suivi d'un onglet à l'autre : un changement de `gm.theme` fait dans un autre onglet (événement storage) s'applique sans recharger ; 191 tests |
