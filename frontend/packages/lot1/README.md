# @gimac/lot1

Interface du lot 1 (PI-GIMAC-01) et de la certification de bout en bout (D-56 à D-61) : tableau de bord par acteur (DES-01), registre des institutions et étapes 1 et 2 du parcours (DES-05 à DES-07, tranche DDRM), étape 3, cadrage d'une liaison (D-57, D-58), écrans DES-08 à DES-12 de l'application du département et POR-03 du portail.

## Découplage des composants et des appels

```
pages (compositions du design system) ──> hooks de données ──> ports (interfaces) ──> implémentation
                                                                                      ├─ http    : passerelle /bff (réel)
                                                                                      └─ simule  : serveur fictif en mémoire
```

- **Composants** : uniquement ceux de `@gimac/design-system`, purement visuels.
- **Ports** (`src/services/ports.ts`) : `ServiceParticipant` (registre, dossier d'adhésion, maker-checker), `ServiceHomologation`, `ServiceCertification`, `ServicePortail`, `ServiceReauthentification`. Seul contrat entre les écrans et les données.
- **Implémentations** : `services/http` (chemins de la cartographie des contrats du plan) et `services/simule` (données de développement fictives, latence, refus normalisés). L'application choisit : simulée par défaut, réelle avec `VITE_SERVICES=http`.
- **Hooks** (`src/hooks`) : seuls à appeler les services, par injection (`ServicesProvider`) ; clés de requête `[sujet, ressource, …]` invalidées par le canal poussé.
- **Descripteurs** (`src/ressources`) : déclaratifs, libellés en clés i18n, actions agissant par `ctx.services` ; convertis pour le catalogue par `useDescripteurDS`. `ExecuteurActions` porte confirmation, signature FIDO2, motif et retours.
- **Adaptateurs** (`src/adaptateurs`) : fonctions pures des réponses des services vers les propriétés des composants.
- **i18n** (`src/i18n`) : un catalogue FR, EN, ES par écran.

Aucune règle métier n'est évaluée côté client : transitions, permissions, couvertures et impartialité viennent du service.

**Matrice des rôles (D-58, D-61).** Les pages lisent les permissions de la session (`usePermissions` du socle, alimenté par `/api/session` après une connexion à deux facteurs) et les passent aux composants du catalogue : une action non permise est absente, pas grisée. Le tableau de bord (`TableauDeBordPage`) ne compose que les sections que ces permissions ouvrent : propositions à trancher pour le chef de la DDRM, dossiers en cours pour le chargé, pilotage des liaisons pour l'ingénieur d'homologation, raccourcis vers les écrans permis.

**Tranche DDRM.** `InstitutionsPage` (liste paginée côté serveur, filtre de statut, « Mes dossiers »), `InstitutionPage` (fiche, dossier d'adhésion pièce par pièce, conditions d'adhésion, prérequis, propositions de statut avec approbation signée), formulaires de prise de contact, de conditions et de dépôt (MS-DOCUMENT puis rattachement de la référence et de l'empreinte).

**Étape 3, cadrage (D-57, D-58).** Contrat de MS-HOMOLOGATION (`LiaisonsCadrageResource`, passerelle `/bff/des/homologation`) :

| Route | Port (`ServiceHomologation`) | Écran |
|---|---|---|
| `GET /liaisons?etat=&typeProjet=&institutionId=&recherche=&mesProjets=&page=&taille=` (page à partir de 0) | `listerLiaisons` | Liste, filtre « Mes projets », onglet « Liaisons » de la fiche de l'institution, tuiles des tableaux de bord |
| `GET /liaisons/{id}` (LiaisonVue : `perimetre`, `intervenants`, `phaseCourante` = `CADRAGE`) | `lireLiaison` | Fiche : résumé du cadrage sous l'en-tête (`ResumeCadrage`) |
| `GET /liaisons/conducteurs` | `conducteurs` | Choix du conducteur de projet |
| `POST /liaisons` | `ouvrirLiaison` | « Ouvrir une liaison (cadrage) » sur la fiche d'une institution adhérente ou en homologation (`FormulaireCadrage`), réservé à qui détient `HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE` |
| `PUT /liaisons/{id}/chef-de-projet` `{chefDeProjetId, motif?}` | `designerChefDeProjet` | « Désigner le conducteur » (`HOM_PROJECT_DESIGNATE` et `actionsPermises`) |

Refus traduits en FR, EN, ES (statut de l'institution interpolé) : `HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE`, `HOM_LIAISON_DUPLICATE`, `HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED`, `HOM_LIAISON_CARRIER_NOT_BANK`, `HOM_REFERENTIAL_NONE_PUBLISHED`, `HOM_PROJECT_DESIGNATION_FORBIDDEN`, `HOM_PARTICIPANT_UNAVAILABLE`, `HOM_IAM_UNAVAILABLE`. Après l'ouverture, la liste et la fiche de l'institution sont relues : MS-PARTICIPANT la fait passer d'elle-même en homologation (`homologation.liaison-opened.v1`), transition marquée « automatique » dans ses propositions de statut. Le serveur simulé reproduit ces règles et la bascule (pont entre les serveurs fictifs de MS-HOMOLOGATION et de MS-PARTICIPANT). « Mes projets » est activé d'emblée pour un conducteur de projet qui ne désigne pas ; tableaux de bord : liaisons en cadrage (chefs du DES et de la DDRM), mes projets (conducteur).

**Interface par composition (D-63).** Les écrans n'emploient que des composants du système de conception : en-têtes (`PageHeader`), sections et tuiles des tableaux de bord (`PageSection`, `StatGrid`), listes de liens (`LinkList`), rangées de boutons (`ActionBar`), retours d'action (`ToastRegion`) ; aucun style en ligne dans les écrans de la tranche DDRM et du cadrage.

```
npm run test -w @gimac/lot1
npm run couverture -w @gimac/lot1   # 80 % bloquants sur tout le paquet et sur les tranches DDRM et cadrage (D-61)
npm run lint -w @gimac/lot1
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création : ports, implémentations HTTP et simulée, hooks, descripteurs, écrans du lot 1 |
| 2026-09-29 | — | Tranche DDRM : port `ServiceParticipant` (HTTP et simulé), hooks, descripteurs, tableau de bord par acteur, registre et dossier numérique des institutions, formulaires ; permissions de la session passées aux composants ; 121 tests, couverture de la tranche au-dessus de 80 % (D-61) |
| 2026-09-29 | — | Étape 3, cadrage (D-57, D-58) : port et services HTTP et simulés au contrat de `LiaisonsCadrageResource` (`conducteurs`, `designerChefDeProjet(id, chefDeProjetId, motif?)`, filtres du service, pagination à partir de 0, recherche d'institutions retirée), ouverture depuis la fiche de l'institution, résumé du cadrage, désignation motivée, « Mes projets », tuiles par acteur, transitions automatiques, i18n FR, EN, ES ; lignes du dossier rattachées à l'institution par l'interface (les vues de MS-PARTICIPANT ne la portent pas : l'approbation visait `/institutions//transitions/…`) ; conditions d'adhésion et attribution d'une opération par liste fermée (la liste avec recherche ne permettait aucun choix) ; audit D-63 (composites du système de conception) ; 198 tests, couverture de tout le paquet au-dessus de 80 % (D-61) |
| 2026-09-29 | — | Parcours de la liaison, socle (D-56 à D-59) : modèle des étapes 3 à 10, référentiel de l'annexe A du CR v1.4 (rangs 1 à 35, C1 à C10, M1 à M17, D1 à D34), matrice des 15 profils par fonction, ports `ServiceParcours`, `ServiceDecision` et `ServicePortailParcours` (HTTP : routes réelles de l'étape 3 ; simulé : liaisons fictives à chaque étape), hooks, vocabulaire FR, EN, ES ; profil de démonstration par `?profil=ROLE_…` |
| 2026-09-29 | — | Parcours de la liaison, tests (D-61) : contrats HTTP des trois ports (méthodes, chemins, paramètres, corps, réauthentification, dépôt en deux temps, ApiError), serveur simulé (progression 3 à 10 et règle des six éléments, R/C/V/I/E, preuves sans secret, blocages datés, verrous 1 et 2, visas, fiche de suivi, service supplémentaire, tableaux par fonction, pilotage, portail, chaque refus), hooks (méthode appelée, clés invalidées), matrice des rôles et référentiel de l'annexe A ; corrections du simulé : état partagé entre instances, rang 19 non clos quand le participant signe en second, rangs 21 et 22 jamais clos, blocages de même identifiant sur deux liaisons, tâches et blocages d'autres institutions au portail, actions offertes que le service refuse, contournement du code de contrôle par un livrable, validateur devenu responsable, conducteur non habilité à l'ouverture, places R et V non pourvues à la première désignation, refus du portail levé hors promesse ; 315 tests, couverture du paquet au-dessus de 80 % |
| 2026-09-29 | — | Tests du contrat HTTP et du service simulé du parcours (24 tests, service simulé couvert à 98 % en lignes) ; nouvelles liaisons ouvertes avant le rang 3 |
| 2026-10-13 | D-104 | cadrage avec intitulé et référence d'origine, refus `HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE` traduit, services simulés alignés ; groupe des institutions et filtre par groupe |
