# Recette DEMO-SBX-12 — raccordement de la Sandbox GIMAC

## Portée et méthode

Cette recette consolide les preuves visuelles existantes de l'administration de la Sandbox GIMAC dans Moabi : **12 contrôles et 64 captures**, chacun en thème clair et sombre. Les captures sont produites par Chromium/Playwright sur les applications construites avec `VITE_SERVICES=simule`, en `fr-FR`, fuseau Africa/Douala, 1440 × 900. Les institutions 99001 à 99005, comptes, campagnes et événements sont fictifs.

Les scripts rejouables sont `frontend/e2e/captures/exi-homologation/capturer.mjs` (DES, port 5441) et `frontend/e2e/captures/exi-iam/capturer.mjs` (console, port 5431). Chaque étape attend le résultat visible avant la capture. Cette recette démontre les rendus et gestes simulés, pas le comportement de serveurs Sandbox ou GIMAC réels.

## Douze contrôles documentés

| Contrôle | Récit et résultat visible | Captures |
|---|---|---|
| DEMO-SBX-12.01 | Espaces des institutions : liste puis fiche d'un espace | `captures/exi/homologation/01-espaces-liste-clair.png`, `11-espaces-liste-sombre.png`, `05-espaces-fiche-clair.png`, `15-espaces-fiche-sombre.png` |
| DEMO-SBX-12.02 | Ouverture : dialogue, saisie contrôlée puis espace ouvert | `captures/exi/homologation/02-espaces-ouverture-dialogue-clair.png`, `03-espaces-ouverture-saisie-clair.png`, `04-espaces-ouverture-faite-clair.png`, et les trois équivalents `12` à `14` en sombre |
| DEMO-SBX-12.03 | Suspension : motif puis état suspendu | `captures/exi/homologation/06-espaces-suspension-dialogue-clair.png`, `07-espaces-suspendu-clair.png`, `16-espaces-suspension-dialogue-sombre.png`, `17-espaces-suspendu-sombre.png` |
| DEMO-SBX-12.04 | Fermeture : confirmation affichée avant l'acte | `captures/exi/homologation/08-espaces-fermeture-dialogue-clair.png`, `18-espaces-fermeture-dialogue-sombre.png` |
| DEMO-SBX-12.05 | Suspension consécutive au changement de statut au registre | `captures/exi/homologation/09-espaces-suspendu-par-le-registre-clair.png`, `19-espaces-suspendu-par-le-registre-sombre.png` |
| DEMO-SBX-12.06 | Lecture par le chef du DES, sans action d'administration | `captures/exi/homologation/10-espaces-lecteur-chef-des-clair.png`, `20-espaces-lecteur-chef-des-sombre.png` |
| DEMO-SBX-12.07 | US-EXI-17 : résultats d'une campagne, pagination des exécutions, scénarios non rapprochés, indisponibilité et absence de campagne | `captures/exi/homologation/exi-17/` (10 fichiers) |
| DEMO-SBX-12.08 | US-EXI-06 : périmètres suspendus et comptes protégés | `captures/exi/iam/exi-06/` (4 fichiers) |
| DEMO-SBX-12.09 | US-EXI-04 : état des mandats de partenaire | `captures/exi/iam/exi-04/` (4 fichiers) |
| DEMO-SBX-12.10 | US-EXI-24 : demande d'accès de prestataire, enregistrement, approbation et refus d'auto-approbation | `captures/exi/iam/exi-24/` (12 fichiers) |
| DEMO-SBX-12.11 | US-EXI-23 : compte verrouillé après cinq échecs, motif de déblocage puis compte débloqué | `captures/exi/iam/exi-23/` (6 fichiers) |
| DEMO-SBX-12.12 | US-EXI-10 : registre de la Sandbox, filtre par institution, export CSV et rebut motivé | `captures/exi/audit/exi-10/` (8 fichiers) |

Les douze contrôles sont documentés comme conformes aux résultats visibles attendus dans ces captures. Ils ne couvrent pas les critères serveur qui ne produisent aucun écran.

## Compléments intégrés le 27/10/2026, hors captures initiales

- **Introspection/cloisonnement** : tests MS-IAM et MS-GATEWAY de la décision sans cache, de la suspension, des mandats et du jeton réduit ; aucune capture supplémentaire.
- **Sagas OUVERTURE/MATRICE et réexamen US-EXI-23** : `ComptesSandboxSagaServiceTest`, `ComptesSandboxSagaIT`, `ReexamensAccesServiceTest`, `ReexamensAccesIT`, `ReexamenAccesTest` et `reexamens.test.tsx` ; aucune capture du nouvel onglet.
- **Supervision D-121** : écran `/des/sandbox/supervision` et quatre lectures OPA de SBX-TRACE, couverts par 46 tests de `@gimac/sandbox-admin`, 592 tests de MS-GATEWAY et 213 tests OPA ; aucune capture du nouvel écran.
- **US-EXI-25 / D-125** : `SocleEvaluationSandboxReleveTest`, `ReleveSandboxIT.exi25EmpreinteBilanEtSocle`, `SocleEvaluationSandboxTest`, `BilanSandboxConsumerTest` et `BilanSandboxIT.circuitComplet` ; fonction serveur sans capture.

Les validations et leurs couvertures sont synthétisées dans le README racine. Elles ne doivent pas être extrapolées en recette de production : les assemblages spécifiques de recette restent à résorber, l'AC mTLS locale n'est pas livrée, le Compose complet n'a pas été démarré pour cette intégration et aucune preuve de serveurs GIMAC réels n'existe.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-27 | - | Consolidation DEMO-SBX-12 : 12 contrôles, 64 captures existantes et distinction explicite des compléments validés sans nouvelles captures |
