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
| `src/modele.ts` | `Compte`, `Moi`, `SessionCompte`, étapes de l'assistant (`MOT_DE_PASSE`, `SECOND_FACTEUR`, `FICHE`, `CHARTE`, noms historiques ramenés), changement de mot de passe, enrôlement TOTP, langues |
| `src/services/ports.ts` | `ServiceMonCompte` : moi, première configuration, fiche, mot de passe, code TOTP (enrôlement, confirmation), sessions, clés de sécurité |
| `src/services/http.ts` | Routes `/bff/{des,portail,console}/iam/moi`, `/moi/premiere-configuration`, `/moi/mot-de-passe`, `/moi/second-facteur/totp[/enrolement]` (réauthentification `ACCOUNT_SENSITIVE` sur 428), `/moi/sessions[/{id}]`, `/moi/cles-securite[/defi|/{id}]` ; enrôlement par `enrolerCleSecurite` du socle |
| `src/services/simule.ts`, `simulateur.ts` | Substitut de MS-IAM (mêmes formes, mêmes codes de refus : `IAM_PHONE_INVALID`, `IAM_CHARTER_REQUIRED`…) ; `creerSimulateur` réutilisé par la console |
| `src/contexte.tsx`, `src/hooks.ts` | Injection du service (`CompteProvider`) et hooks `useMoi`, `useMesCles`, `useEnrolerCle`… |
| `src/GardePremierAcces.tsx` | Après `GardeSession` : lit `/moi` ; tant que `etapesRestantes` n'est pas vide, fournit l'assistant à placer dans l'enveloppe (sans navigation) à la place de toutes les pages ; MS-IAM injoignable : pages servies (mode dégradé) |
| `src/pages/PremierAccesPage.tsx` | Assistant (Stepper, D-65) : mot de passe définitif (politique rappelée, violations), code TOTP (code QR local, secret sur demande, code de vérification), fiche (téléphone obligatoire, langue, fonction), charte ; dans la session restreinte, connexion finale avec le nouveau mot de passe et un code |
| `src/pages/MonComptePage.tsx` | Fiche personnelle, mot de passe, code TOTP, clés de sécurité, sessions : toute modification en modale CRUD standard (`src/composants/ModaleSaisie.tsx`, seul point d'emploi de `CrudDialog`) |
| `src/composants/` | `useSaisieMotDePasse`, `useSaisieTotp` (champs et envoi partagés par l'assistant et les modales), `QrCode` (bibliothèque `uqr` embarquée, à promouvoir au système de conception) |
| `src/i18n.ts` | Libellés FR, EN, ES (`premierAcces.*`, `compte.*`, profils, codes d'erreur IAM) |

D-65 : tout se fait sur la plateforme, aucune page du fournisseur d'identité n'est ouverte (contrat `docs/lot1/connexion.md`). Service simulé : mot de passe actuel `Gimac-Demo-2026!`, tout code TOTP de six chiffres accepté ; le mot de passe choisi et la fin du premier accès sont transmis à la connexion simulée du socle, en mémoire de la page.

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
