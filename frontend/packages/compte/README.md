# @gimac/compte

« Mon compte » et premier accès (décision D-62), communs aux trois couches : application du département (`/des`), portail des participants (`/portail`) et console (`/console`). Aucun utilisateur n'ouvre la console du fournisseur d'identité : les comptes sont créés par le super administrateur ou le RSSI, puis chaque titulaire complète le sien depuis la plateforme.

Paquet distinct du socle parce qu'il porte des écrans, des hooks de données et un port propre (même découpage que `@gimac/lot1`), alors que `@gimac/socle` reste limité aux briques transverses ; l'assistant WebAuthn d'enrôlement est, lui, dans le socle, à côté de la réauthentification (`socle/src/reauth/enrolement.ts`).

## Découplage

```
GardePremierAcces, PremierAccesPage, MonComptePage ──> hooks (react-query) ──> ServiceMonCompte (port)
                                                                              ├─ monCompteHttp(couche) : /bff/<couche>/iam/moi
                                                                              └─ monCompteSimule()     : serveur fictif en mémoire
```

| Fichier | Contenu |
|---|---|
| `src/modele.ts` | `Compte`, `Moi`, `SessionCompte`, actions requises du fournisseur d'identité, langues |
| `src/services/ports.ts` | `ServiceMonCompte` : moi, première configuration, fiche, action en libre-service, sessions, clés de sécurité, reconnexion |
| `src/services/http.ts` | Routes `/bff/{des,portail,console}/iam/moi`, `/moi/premiere-configuration`, `/moi/actions`, `/moi/sessions[/{id}]`, `/moi/cles-securite[/defi|/{id}]` ; enrôlement par `enrolerCleSecurite` du socle ; reconnexion par la passerelle |
| `src/services/simule.ts`, `simulateur.ts` | Substitut de MS-IAM (mêmes formes, mêmes codes de refus : `IAM_PHONE_INVALID`, `IAM_CHARTER_REQUIRED`…) ; `creerSimulateur` réutilisé par la console |
| `src/contexte.tsx`, `src/hooks.ts` | Injection du service (`CompteProvider`) et hooks `useMoi`, `useMesCles`, `useEnrolerCle`… |
| `src/GardePremierAcces.tsx` | Après `GardeSession` : lit `/moi` ; tant que `etapesRestantes` n'est pas vide, fournit l'assistant à placer dans l'enveloppe (sans navigation) à la place de toutes les pages ; MS-IAM injoignable : pages servies (mode dégradé) |
| `src/pages/PremierAccesPage.tsx` | Assistant (Stepper) : fiche (téléphone obligatoire, langue fr, en ou es, fonction, charte d'usage), clé de sécurité facultative, puis actions requises du fournisseur d'identité (mot de passe, second facteur, courriel, profil) expliquées et exécutées après reconnexion (`Continuer`) |
| `src/pages/MonComptePage.tsx` | Fiche personnelle, mot de passe et second facteur (action inscrite puis reconnexion), clés de sécurité (liste, enrôlement, suppression), sessions (fermer une, fermer toutes) |
| `src/i18n.ts` | Libellés FR, EN, ES (`premierAcces.*`, `compte.*`, profils, codes d'erreur IAM) |

Reconnexion : `POST /api/logout` (session de la passerelle close) puis `/oauth2/authorization/oidc` ; le fournisseur d'identité exécute alors les actions requises sur ses pages aux couleurs du GIMAC.

Composants : uniquement ceux de `@gimac/design-system` (Stepper, Card, Panel, TextField, Select, Checkbox avec erreur, KeyValueList, DataTable, ConfirmDialog, InlineAlert).

```
npm run test -w @gimac/compte
npm run couverture -w @gimac/compte   # 80 % bloquants (D-61)
npm run lint -w @gimac/compte
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création (D-62) : port, services HTTP et simulé, hooks, garde de premier accès, assistant, page Mon compte, FR/EN/ES ; 25 tests, couverture 98,5 % des instructions, 85,6 % des branches |
