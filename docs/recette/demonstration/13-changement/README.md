# DEMO-SBX 13 : changement complet, signature FIDO2 et PDF final réel

## Récit et définition de fini

Le processus d'unité `DEMANDE_CHANGEMENT`, distinct de la certification, est lancé par la passerelle Moabi avec un
compte réel de la recette. La référence datée est allouée par PostgreSQL et les trois premières étapes du demandeur
sont terminées. Le responsable `demo.admin2.gimac` dépose ensuite le livrable `DOSSIER_CHANGEMENT` par dépôt
fragmenté dans MS-DOCUMENT et termine l'étape 4. L'agent `demo.certificateur` renseigne les cinq champs SSI, termine
l'étape 5 et appose la signature `DECISION_SSI`, ce qui clôt l'instance.

Les deux acteurs enrôlent chacun une clé depuis « Mon compte ». Le test utilise les authentificateurs virtuels CTAP2
natifs de Chromium/CDP, avec vérification utilisateur, dans le contexte sécurisé `http://localhost:4200`. Les appels
`navigator.credentials.create` et `navigator.credentials.get` restent ceux de l'application : aucune attestation ou
assertion n'est fabriquée ou interceptée par le test. MS-IAM vérifie l'assertion avec la finalité
`SIGNATURE_PROCESSUS` avant que MS-PILOTAGE n'enregistre la signature.

Le bouton « Télécharger le PDF » appelle `GET /bff/des/pilotage/processus/instances/{id}/pdf`. MS-PILOTAGE transmet
le HTML contrôlé à MS-DOCUMENT, qui le convertit avec le Gotenberg 8.37.0 de la recette. Le PDF intermédiaire sert de
livrable réel ; le PDF final reprend l'état `CLOSE`, les champs SSI, la signature et son empreinte.

## Exécution vérifiée le 30/10/2026

```bash
cd frontend
GIMAC_RECETTE_ENV="../infra/recette/.local/demo12.env" \
  npx playwright test -c e2e/demonstration/playwright.config.ts \
  e2e/demonstration/etapes/13-changement-reel.spec.ts
```

Résultat : **1 test réussi**. Le scénario vérifie les codes HTTP, la référence
`CHG-2026-10-30-000013`, les transitions des cinq étapes, les métadonnées et le SHA-256 du livrable, les champs SSI,
l'état `A_SIGNER`, la réponse de signature, l'état `SIGNEE`, l'avancement 5/5, l'instance `CLOSE` et les octets
`%PDF-`. L'historique porte aussi l'empreinte signée. Une assertion interdit le débordement horizontal de la fiche et
la capture finale replace le conteneur interne en haut afin de montrer l'état et l'avancement.

Preuves, chacune en thème clair et sombre :

- `01-demande-changement-reelle-*` : fiche réelle, trois étapes terminées et responsable désigné ;
- `02-cle-responsable-enrolee-*` et `04-cle-ssi-enrolee-*` : clés enrôlées dans « Mon compte » pour les deux acteurs ;
- `03-livrable-pdf-depose-*` : PDF déposé par le responsable et empreinte SHA-256 du document ;
- `05-champs-ssi-renseignes-*` et `06-decision-ssi-a-signer-*` : champs enregistrés et étape prête à signer ;
- `07-signature-empreinte-controlee-*` et `08-signature-fido2-confirmee-*` : empreinte attendue puis confirmation de
  la signature par la clé ;
- `09-instance-close-signee-*` : état `Close`, avancement 5/5, référence et bouton de téléchargement de l'instance close.

`CHG-2026-10-30-000013.pdf` est un PDF A4 réel de deux pages, de 43 463 octets. Son extraction texte confirme `CLOSE`,
`ACCEPTEE`, `SIGNEE`, le signataire « Ingénieur Certificateur démonstration » et l'empreinte
`58d4b0b71b38f1c582785fa83d28b6d6c7b49a56a474e06000cae34b73d4c385`. Le SHA-256 du fichier PDF final est
`476e23be181fffb6808837f850092a4e6cf4ade9a933fb0b41cf5fcb417d85e9`.

Corrections rendues nécessaires par cette prolongation : le RP ID WebAuthn de MS-IAM vaut `localhost` au lieu de
l'adresse IP publique, et les origines `localhost` restent admises même lorsque des origines publiques sont configurées.

## Limites explicites

Cette preuve utilise de vrais services locaux, mais des clés FIDO2 virtuelles propres au test et non des clés physiques
de production. Elle ne constitue ni une recette de production ni une connexion aux serveurs GIMAC. La signature est une
signature métier vérifiée par MS-IAM et matérialisée dans le PDF ; le conteneur PDF n'est pas lui-même signé
cryptographiquement. Le Gotenberg local, son AC et les comptes restent propres au poste.

## Reprise dans le dépôt de la plateforme (04/11/2026)

Preuve propre à Moabi (MS-PILOTAGE, MS-DOCUMENT et MS-IAM ; écrans du DES seulement), retirée du dépôt de la Sandbox
GIMAC et reprise ici à l'identique : captures, PDF et récit ci-dessus datent du 30/10/2026. Le scénario est
`frontend/e2e/demonstration/etapes/13-changement-reel.spec.ts`, avec le harnais `frontend/e2e/demonstration/`
(`outils.ts`, `playwright.config.ts`) ; le fichier `.env` de la recette reste celui du dépôt de la sandbox :

```bash
cd frontend
GIMAC_RECETTE_ENV="<dépôt de la sandbox>/infra/recette/.local/demo12.env" GIMAC_DEMO_COMPTE_RSSI=<compte RSSI> \
  npx playwright test -c e2e/demonstration/playwright.config.ts e2e/demonstration/etapes/13-changement-reel.spec.ts
```

Depuis le 04/11/2026, deux points diffèrent des preuves du 30/10, qui ne sont pas rejouées : la référence suit le
format de la fiche, `CHG-AAAA-N` (D-127 point 5), et seul le RSSI signe l'étape SSI (D-130), si bien que l'agent SSI
`demo.certificateur` ne convient plus ; un compte de démonstration portant `ROLE_RSSI` est attendu dans
`GIMAC_DEMO_COMPTE_RSSI`.

