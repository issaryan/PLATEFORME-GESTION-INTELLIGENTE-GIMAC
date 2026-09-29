# @gimac/socle

Socle d'interface commun aux trois couches (PI-GIMAC-01, fragment F0). Aucun écran métier ici : les briques que chaque lot consomme.

| Dossier | Contenu |
|---|---|
| `src/http/` | `client.ts`, seul point d'appel réseau vers la passerelle (`/bff/<couche>/<service>`) ; témoin de session HttpOnly envoyé seul, jeton anti-CSRF recopié dans `X-XSRF-TOKEN` sur les méthodes qui modifient, assertion FIDO2 dans `X-Reauth-Assertion` sans rejeu, erreurs normalisées en `ApiError` avec identifiant de corrélation ; `prefixes.ts` (`P`) ; `errors.ts` |
| `src/live/` | Canal poussé SSE : reprise depuis le dernier événement, invalidation des requêtes du sujet, reconnexion exponentielle plafonnée |
| `src/reauth/` | Réauthentification FIDO2 (défi MS-IAM, `navigator.credentials.get`) ; `ReauthCancelled` si l'utilisateur annule |
| `src/ressource/` | Types des descripteurs du lot (`ResourceDescriptorLot1`, `ActionContext` portant les services injectés), fabrique `dictionnaire` |
| `src/requete/` | `QueryBoundary` et `EtatsRequete` : les quatre états (chargement, vide, erreur avec corrélation et réessai, nominal) |
| `src/i18n/` | Catalogues **français, anglais, espagnol** (langues de la zone CEMAC), fusion avec ceux des lots |
| `src/enveloppe/` | Enveloppe commune (barre supérieure, navigation latérale filtrée par permissions, bandeau de perte du canal), préférences d'affichage (thème, langue) |
| `src/connexion/` | Écran de connexion TRA-01 : mosaïque tissée prolongeant la couverture du système de conception, sans aucun champ de saisie |

Tout le rendu vient de `@gimac/design-system` ; le mode sombre et le mode clair découlent des jetons.

```
npm run test -w @gimac/socle   # client HTTP (F0 §B), catalogues FR/EN/ES
npm run lint -w @gimac/socle
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création : client HTTP corrigé après revue du F0, canal poussé, FIDO2, quatre états, i18n FR/EN/ES, enveloppe, connexion |
