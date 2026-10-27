# @gimac/administration : administration de la plateforme

Paquet React des écrans d'administration communs à la console et, selon les droits, à l'application du département (D-62, D-69). Les adaptateurs HTTP passent par la passerelle ; les adaptateurs simulés servent uniquement les tests et les démonstrations (`VITE_SERVICES=simule`).

## Sandbox GIMAC

`/console/sandbox` rassemble les périmètres suspendus, comptes protégés, mandats de partenaire, accès de prestataire, réexamens des accès et registre d'audit. Les décisions restent contrôlées par MS-IAM, la passerelle et OPA ; l'interface ne duplique aucune habilitation.

- **US-EXI-23** : la fiche d'un compte affiche le verrouillage et propose son déblocage motivé aux seuls administrateurs GIMAC habilités. L'onglet « Réexamen des accès » présente les campagnes trimestrielles, les profils et permissions effectives (dont `SBX_*`), les décisions `MAINTENU`, `REDUIT`, `REVOQUE` et le scellement. Les appels HTTP utilisent `/bff/console/iam/comptes/reexamens` ; l'ouverture exige `IAM_ACCOUNT_CREATE`, les décisions et la clôture `IAM_ACCOUNT_ROLES`.
- **Sagas** : l'interface ne rejoue pas elle-même une saga OUVERTURE ou MATRICE ; MS-IAM garantit l'idempotence des étapes appelées par SBX-ESPACE. MATRICE préserve les profils non SBX.
- **Preuves visuelles** : DEMO-SBX-12 est consigné dans `docs/recette/sandbox.md`. Les 34 captures produites depuis ce paquet couvrent EXI-04, EXI-06, EXI-23, EXI-24 et le registre EXI-10, dans `docs/recette/captures/exi/iam/` et `docs/recette/captures/exi/audit/`. Le nouvel onglet de réexamen n'a pas de capture ajoutée.

Le contrat fonctionnel détaillé n'est pas recopié ici : voir `docs/lot1/sandbox-existant-adapte.md` §8.

## Vérification

```bash
npm run lint -w @gimac/administration
npm test -w @gimac/administration
npm run couverture -w @gimac/administration
```

Exécution du 27/10/2026 : 23 fichiers et 189 tests réussis. Couverture globale : 95,25 % des instructions, 85,70 % des branches, 94,38 % des fonctions et 96,44 % des lignes. Le seuil global reste à 80 % ; les fichiers Sandbox et de réexamen EXI sont soumis à 98 % sur les quatre métriques. Ces tests ne démarrent ni le Compose complet ni un serveur GIMAC réel.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-27 | - | Création du README ; US-EXI-23 : réexamen trimestriel, décisions et scellement, routes OPA ; synthèse DEMO-SBX-12 et validation Vitest |
