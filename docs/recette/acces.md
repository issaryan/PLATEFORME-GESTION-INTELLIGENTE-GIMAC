# Recette du lot ACC : matrice des accès

Recette du 8 octobre 2026, construction de recette en services simulés : application interne http://localhost:6100, portail http://localhost:6101, console http://localhost:6102 (Chromium sans tête, locale fr-FR, 1440 × 900 et 390 × 844). Validateur : lot ACC.

Bilan : **158 contrôles, 145 conformes, 13 en anomalie** (rattachés aux anomalies numérotées ci-dessous) ; matrice des entrées de barre latérale : 552 cases, 0 écart ; matrice des routes : 1275 ouvertures par adresse directe, 0 écart. 8 anomalies (ACC-01 à ACC-08) : 2 majeures, 6 mineures, aucune bloquante.

## 1. Périmètre

**Profils** : les 17 profils livrés du domaine « gimac » (infra/keycloak/generer-realm-gimac.py, PROFILS et PRESENTATIONS), chacun avec son compte de démonstration des services simulés :

| Abréviation | Profil | Compte de démonstration | Permissions (générateur) |
|---|---|---|---|
| ING | Ingénieur d'homologation (ROLE_INGENIEUR_HOMOLOGATION) | j.ekotto | 26 |
| CDES | Chef du DES (ROLE_CHEF_DES) | p.mbarga | 29 |
| SADM | Super administrateur (ROLE_SUPER_ADMIN) | super.admin | 35 |
| RSSI | Sécurité des SI (ROLE_RSSI) | h.kamga | 33 |
| CONF | Conformité (ROLE_CONFORMITE) | m.obame | 17 |
| DG | Direction générale (ROLE_DG) | j.ella | 17 |
| CHDR | Chargé de la DDRM (ROLE_CHARGE_DDRM) | f.abakar | 18 |
| CFDR | Chef de la DDRM (ROLE_CHEF_DDRM) | b.nguema | 25 |
| SNA | Normes et Agréments (ROLE_SNA) | r.biyong | 15 |
| PROD | Production monétique (ROLE_PRODUCTION_MONETIQUE) | s.ondoua | 17 |
| RES | Pôle réseau (ROLE_POLE_RESEAU) | i.mahamat | 14 |
| SYS | Pôle système (ROLE_POLE_SYSTEME) | c.ngo | 14 |
| COND | Conducteur de projet (ROLE_CONDUCTEUR_PROJET) | a.mahamat | 19 |
| COLL | Collaborateur (ROLE_COLLABORATEUR) | n.mvondo | 4 |
| RESP | Responsable de service (ROLE_RESPONSABLE_SERVICE) | g.ndzana | 8 |
| ASYS | Administrateur système (ROLE_ADMIN_SYSTEME) | admin.systeme | 8 |
| CORR | Correspondant du participant (ROLE_CORRESPONDANT, profil externe) | correspondant-a | 5 |

Profil externe : le correspondant (seul profil externe des services simulés, attribut `externe` du profil). Profil créé par l'administration : les services simulés permettent d'en créer un dans la console (« Nouveau profil »), mais **aucun compte de connexion de démonstration ne peut le porter** (les comptes de la connexion simulée sont figés dans apps/*/src/connexionDemo.ts) ; ce cas n'est donc pas vérifiable par une connexion en services simulés et reste à couvrir sur la passerelle.

**Attendu** : calculé par script (scratchpad `attendu.py`) à partir du catalogue des fonctionnalités de MS-IAM (ms-iam/src/main/resources/config/fonctionnalites-initiales.json : visible, permissions.tous et unDe, widgets, groupe) et des permissions de chaque profil exécutées depuis le générateur Keycloak (messagerie, Mon activité, responsables, référentiels compris). La copie simulée du catalogue (packages/lot1/src/services/simule/fonctionnalites-initiales.json) a été comparée au catalogue de MS-IAM : identique sur les 57 fonctionnalités (visible, permissions, widgets, ordre, groupe). Aiguillage D-79 attendu (packages/socle/src/session/aiguillage.ts) : un compte interne reste dans l'application interne quand il s'y connecte ; sur la console, il y reste s'il porte l'une des permissions de console (IAM_ACCOUNT_READ, PLATFORM_SUPERVISION_READ, PLATFORM_CONFIG_READ, PLATFORM_OPS_READ, PLATFORM_SECURITY_POLICY_READ, PLATFORM_MESSAGING_EDIT), sinon il est conduit vers l'application interne ; le portail refuse tout compte interne ; l'application interne et la console refusent le correspondant.

**Écrans et flux** :

1. Connexion de chaque profil sur les trois applications (identifiant, mot de passe, code), application d'arrivée, barre latérale, widgets du tableau de bord, captures du tableau de bord en clair et en sombre.
2. Ouverture par adresse directe de toutes les routes de frontend/apps/departement/src/App.tsx (44 routes, dont une inconnue), apps/console/src/App.tsx (20) et apps/portail/src/App.tsx (11) pour chaque profil, session ouverte dans l'onglet (gm.sessionDemo) : 75 adresses × 17 profils.
3. Liens profonds d'instance et de demande (D-98) avec « Processus de l'unité » masquée, sur une demande réelle lancée par la SNA vers le pôle réseau.
4. Effet du catalogue : masquer puis rétablir une fonctionnalité dans la console et dans l'application interne (motif et clé de sécurité simulée).
5. Affichage à 390 px pour 3 profils (COLL, SADM, CORR) ; contrôle de trois écrans en en-US ; ouverture d'une application avec une session existante qui n'y a pas sa place.

Les scripts Playwright de la recette sont dans le scratchpad du lot (lib.cjs, partA.cjs à partF.cjs, attendu.py, rapport.py) ; aucune erreur JavaScript, aucune erreur de console et aucune réponse réseau en erreur n'a été relevée pendant les connexions (partie A), les liens profonds (partie C) et les gestes du catalogue (partie D).

**Méthode du point 3** : le catalogue servi par MS-IAM simulé vit dans la mémoire de la page (packages/administration/src/services/simule/index.ts, copie des valeurs de départ) et repart à chaque rechargement, alors que l'état des processus survit au changement de profil (sessionStorage). Pour reproduire la configuration serveur « processus visible = faux » d'un profil à l'autre, un script d'initialisation de la recette modifie la copie des valeurs de départ (visible = faux pour la clé `processus`), exactement comme le ferait un PUT du super administrateur sur MS-IAM. Le geste réel de masquage par l'interface est vérifié au point 4.

**Méthode du point 4** : pour la même raison, l'effet d'un masquage n'est observable en services simulés que dans la page qui l'a fait ; le profil concerné est donc le super administrateur lui-même (porteur de PLATFORM_MESSAGING_EDIT pour « Courriels de la plateforme » et de PAR_INSTITUTION_READ pour « Institutions membres »).

## 2. Tableau des contrôles

Codes des profils : voir le tableau du périmètre. Les captures sont dans [captures/acces](captures/acces/) (numérotées dans l'ordre du parcours : 001 à 083 connexions et tableaux de bord, 101 à 114 échantillon des routes, 201 à 210 liens profonds, 301 à 321 catalogue, 401 à 423 mobile, langue et sessions existantes).

| N° | Écran ou flux | Profil | Geste | Résultat attendu | Résultat observé | Capture | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | Connexion, application interne | ING | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [001](captures/acces/001-ingenieur-homologation-des-tableau-de-bord-clair.png) | Conforme |
| 2 | Connexion, console | ING | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 3 | Connexion, portail | ING | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=j.ekotto ; identifiant prérempli « j.ekotto » |  | Conforme |
| 4 | Barre latérale interne | ING | Relevé des entrées | 14 entrées (catalogue × permissions) | 14 entrées, identiques | [001](captures/acces/001-ingenieur-homologation-des-tableau-de-bord-clair.png) | Conforme |
| 5 | Tableau de bord interne (widgets) | ING | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [001](captures/acces/001-ingenieur-homologation-des-tableau-de-bord-clair.png) [002](captures/acces/002-ingenieur-homologation-des-tableau-de-bord-sombre.png) | Conforme |
| 6 | Routes des trois applications par adresse directe | ING | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 18 refus sans donnée affichée |  | Conforme |
| 7 | Connexion, application interne | CDES | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [006](captures/acces/006-chef-des-des-tableau-de-bord-clair.png) | Conforme |
| 8 | Connexion, console | CDES | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 9 | Connexion, portail | CDES | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=p.mbarga ; identifiant prérempli « p.mbarga » |  | Conforme |
| 10 | Barre latérale interne | CDES | Relevé des entrées | 16 entrées (catalogue × permissions) | 16 entrées, identiques | [006](captures/acces/006-chef-des-des-tableau-de-bord-clair.png) | Conforme |
| 11 | Tableau de bord interne (widgets) | CDES | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Pilotage des projets de certification, Mon travail | maJournee, cockpit, monTravail | [006](captures/acces/006-chef-des-des-tableau-de-bord-clair.png) [007](captures/acces/007-chef-des-des-tableau-de-bord-sombre.png) | Conforme |
| 12 | Routes des trois applications par adresse directe | CDES | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 15 refus sans donnée affichée |  | Conforme |
| 13 | Connexion, application interne | SADM | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [011](captures/acces/011-super-admin-des-tableau-de-bord-clair.png) | Conforme |
| 14 | Connexion, console | SADM | Connexion sur :6102 | Console (permission de console) | Console :6102 | [013](captures/acces/013-super-admin-console-tableau-de-bord-clair.png) | Conforme |
| 15 | Connexion, portail | SADM | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=super.admin ; identifiant prérempli « super.admin » |  | Conforme |
| 16 | Barre latérale interne | SADM | Relevé des entrées | 28 entrées (catalogue × permissions) | 28 entrées, identiques | [011](captures/acces/011-super-admin-des-tableau-de-bord-clair.png) | Conforme |
| 17 | Tableau de bord interne (widgets) | SADM | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Gestion des utilisateurs (+ derniers actes), Supervision de la plateforme, Charges en erreur | maJournee, administration, administration+, supervision, infrastructure | [011](captures/acces/011-super-admin-des-tableau-de-bord-clair.png) [012](captures/acces/012-super-admin-des-tableau-de-bord-sombre.png) | Conforme |
| 18 | Barre latérale de la console | SADM | Relevé des entrées | 18 entrées | 18 entrées, identiques | [013](captures/acces/013-super-admin-console-tableau-de-bord-clair.png) | Conforme |
| 19 | Tableau de bord de la console | SADM | Relevé des régions, clair puis sombre | Gestion des utilisateurs, Charges en erreur, Supervision de la plateforme | gestionUtilisateurs, chargesEnErreur, supervision | [013](captures/acces/013-super-admin-console-tableau-de-bord-clair.png) [014](captures/acces/014-super-admin-console-tableau-de-bord-sombre.png) | Conforme |
| 20 | Routes des trois applications par adresse directe | SADM | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 3 refus sans donnée affichée |  | Conforme |
| 21 | Connexion, application interne | RSSI | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [016](captures/acces/016-rssi-des-tableau-de-bord-clair.png) | Conforme |
| 22 | Connexion, console | RSSI | Connexion sur :6102 | Console (permission de console) | Console :6102 | [018](captures/acces/018-rssi-console-tableau-de-bord-clair.png) | Conforme |
| 23 | Connexion, portail | RSSI | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=h.kamga ; identifiant prérempli « h.kamga » |  | Conforme |
| 24 | Barre latérale interne | RSSI | Relevé des entrées | 29 entrées (catalogue × permissions) | 29 entrées, identiques | [016](captures/acces/016-rssi-des-tableau-de-bord-clair.png) | Conforme |
| 25 | Tableau de bord interne (widgets) | RSSI | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Gestion des utilisateurs (+ derniers actes), Supervision de la plateforme, Charges en erreur, Mon travail | maJournee, administration, administration+, supervision, infrastructure, monTravail | [016](captures/acces/016-rssi-des-tableau-de-bord-clair.png) [017](captures/acces/017-rssi-des-tableau-de-bord-sombre.png) | Conforme |
| 26 | Barre latérale de la console | RSSI | Relevé des entrées | 18 entrées | 18 entrées, identiques | [018](captures/acces/018-rssi-console-tableau-de-bord-clair.png) | Conforme |
| 27 | Tableau de bord de la console | RSSI | Relevé des régions, clair puis sombre | Gestion des utilisateurs, Derniers actes d’administration, Politique de sécurité, Charges en erreur, Supervision de la plateforme | gestionUtilisateurs, actes, politique, chargesEnErreur, supervision | [018](captures/acces/018-rssi-console-tableau-de-bord-clair.png) [019](captures/acces/019-rssi-console-tableau-de-bord-sombre.png) | Conforme |
| 28 | Routes des trois applications par adresse directe | RSSI | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 1 refus sans donnée affichée |  | Conforme |
| 29 | Connexion, application interne | CONF | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [021](captures/acces/021-conformite-des-tableau-de-bord-clair.png) | Conforme |
| 30 | Connexion, console | CONF | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 31 | Connexion, portail | CONF | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=m.obame ; identifiant prérempli « m.obame » |  | Conforme |
| 32 | Barre latérale interne | CONF | Relevé des entrées | 15 entrées (catalogue × permissions) | 15 entrées, identiques | [021](captures/acces/021-conformite-des-tableau-de-bord-clair.png) | Conforme |
| 33 | Tableau de bord interne (widgets) | CONF | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [021](captures/acces/021-conformite-des-tableau-de-bord-clair.png) [022](captures/acces/022-conformite-des-tableau-de-bord-sombre.png) | Conforme |
| 34 | Routes des trois applications par adresse directe | CONF | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 16 refus sans donnée affichée |  | Conforme |
| 35 | Connexion, application interne | DG | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [026](captures/acces/026-dg-des-tableau-de-bord-clair.png) | Conforme |
| 36 | Connexion, console | DG | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 37 | Connexion, portail | DG | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=j.ella ; identifiant prérempli « j.ella » |  | Conforme |
| 38 | Barre latérale interne | DG | Relevé des entrées | 15 entrées (catalogue × permissions) | 15 entrées, identiques | [026](captures/acces/026-dg-des-tableau-de-bord-clair.png) | Conforme |
| 39 | Tableau de bord interne (widgets) | DG | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Pilotage des projets de certification, Mon travail | maJournee, cockpit, monTravail | [026](captures/acces/026-dg-des-tableau-de-bord-clair.png) [027](captures/acces/027-dg-des-tableau-de-bord-sombre.png) | Conforme |
| 40 | Routes des trois applications par adresse directe | DG | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 16 refus sans donnée affichée |  | Conforme |
| 41 | Connexion, application interne | CHDR | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [031](captures/acces/031-charge-ddrm-des-tableau-de-bord-clair.png) | Conforme |
| 42 | Connexion, console | CHDR | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 43 | Connexion, portail | CHDR | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=f.abakar ; identifiant prérempli « f.abakar » |  | Conforme |
| 44 | Barre latérale interne | CHDR | Relevé des entrées | 15 entrées (catalogue × permissions) | 15 entrées, identiques | [031](captures/acces/031-charge-ddrm-des-tableau-de-bord-clair.png) | Conforme |
| 45 | Tableau de bord interne (widgets) | CHDR | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Relations avec les membres (DDRM), Mon travail | maJournee, ddrm, monTravail | [031](captures/acces/031-charge-ddrm-des-tableau-de-bord-clair.png) [032](captures/acces/032-charge-ddrm-des-tableau-de-bord-sombre.png) | Conforme |
| 46 | Routes des trois applications par adresse directe | CHDR | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 17 refus sans donnée affichée |  | Conforme |
| 47 | Connexion, application interne | CFDR | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [036](captures/acces/036-chef-ddrm-des-tableau-de-bord-clair.png) | Conforme |
| 48 | Connexion, console | CFDR | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 49 | Connexion, portail | CFDR | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=b.nguema ; identifiant prérempli « b.nguema » |  | Conforme |
| 50 | Barre latérale interne | CFDR | Relevé des entrées | 16 entrées (catalogue × permissions) | 16 entrées, identiques | [036](captures/acces/036-chef-ddrm-des-tableau-de-bord-clair.png) | Conforme |
| 51 | Tableau de bord interne (widgets) | CFDR | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Relations avec les membres (DDRM), Pilotage des projets de certification, Mon travail | maJournee, ddrm, cockpit, monTravail | [036](captures/acces/036-chef-ddrm-des-tableau-de-bord-clair.png) [037](captures/acces/037-chef-ddrm-des-tableau-de-bord-sombre.png) | Conforme |
| 52 | Routes des trois applications par adresse directe | CFDR | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 15 refus sans donnée affichée |  | Conforme |
| 53 | Connexion, application interne | SNA | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [041](captures/acces/041-sna-des-tableau-de-bord-clair.png) | Conforme |
| 54 | Connexion, console | SNA | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 55 | Connexion, portail | SNA | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=r.biyong ; identifiant prérempli « r.biyong » | [043](captures/acces/043-sna-portail-refus-vers-interne-clair.png) | Conforme |
| 56 | Barre latérale interne | SNA | Relevé des entrées | 14 entrées (catalogue × permissions) | 14 entrées, identiques | [041](captures/acces/041-sna-des-tableau-de-bord-clair.png) | Conforme |
| 57 | Tableau de bord interne (widgets) | SNA | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [041](captures/acces/041-sna-des-tableau-de-bord-clair.png) [042](captures/acces/042-sna-des-tableau-de-bord-sombre.png) | Conforme |
| 58 | Routes des trois applications par adresse directe | SNA | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 18 refus sans donnée affichée |  | Conforme |
| 59 | Connexion, application interne | PROD | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [046](captures/acces/046-production-monetique-des-tableau-de-bord-clair.png) | Conforme |
| 60 | Connexion, console | PROD | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 61 | Connexion, portail | PROD | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=s.ondoua ; identifiant prérempli « s.ondoua » |  | Conforme |
| 62 | Barre latérale interne | PROD | Relevé des entrées | 14 entrées (catalogue × permissions) | 14 entrées, identiques | [046](captures/acces/046-production-monetique-des-tableau-de-bord-clair.png) | Conforme |
| 63 | Tableau de bord interne (widgets) | PROD | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [046](captures/acces/046-production-monetique-des-tableau-de-bord-clair.png) [047](captures/acces/047-production-monetique-des-tableau-de-bord-sombre.png) | Conforme |
| 64 | Routes des trois applications par adresse directe | PROD | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 18 refus sans donnée affichée |  | Conforme |
| 65 | Connexion, application interne | RES | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [051](captures/acces/051-pole-reseau-des-tableau-de-bord-clair.png) | Conforme |
| 66 | Connexion, console | RES | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 67 | Connexion, portail | RES | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=i.mahamat ; identifiant prérempli « i.mahamat » |  | Conforme |
| 68 | Barre latérale interne | RES | Relevé des entrées | 14 entrées (catalogue × permissions) | 14 entrées, identiques | [051](captures/acces/051-pole-reseau-des-tableau-de-bord-clair.png) | Conforme |
| 69 | Tableau de bord interne (widgets) | RES | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [051](captures/acces/051-pole-reseau-des-tableau-de-bord-clair.png) [052](captures/acces/052-pole-reseau-des-tableau-de-bord-sombre.png) | Conforme |
| 70 | Routes des trois applications par adresse directe | RES | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 18 refus sans donnée affichée |  | Conforme |
| 71 | Connexion, application interne | SYS | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [056](captures/acces/056-pole-systeme-des-tableau-de-bord-clair.png) | Conforme |
| 72 | Connexion, console | SYS | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 73 | Connexion, portail | SYS | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=c.ngo ; identifiant prérempli « c.ngo » |  | Conforme |
| 74 | Barre latérale interne | SYS | Relevé des entrées | 14 entrées (catalogue × permissions) | 14 entrées, identiques | [056](captures/acces/056-pole-systeme-des-tableau-de-bord-clair.png) | Conforme |
| 75 | Tableau de bord interne (widgets) | SYS | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [056](captures/acces/056-pole-systeme-des-tableau-de-bord-clair.png) [057](captures/acces/057-pole-systeme-des-tableau-de-bord-sombre.png) | Conforme |
| 76 | Routes des trois applications par adresse directe | SYS | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 18 refus sans donnée affichée |  | Conforme |
| 77 | Connexion, application interne | COND | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [061](captures/acces/061-conducteur-projet-des-tableau-de-bord-clair.png) | Conforme |
| 78 | Connexion, console | COND | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 79 | Connexion, portail | COND | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=a.mahamat ; identifiant prérempli « a.mahamat » |  | Conforme |
| 80 | Barre latérale interne | COND | Relevé des entrées | 15 entrées (catalogue × permissions) | 15 entrées, identiques | [061](captures/acces/061-conducteur-projet-des-tableau-de-bord-clair.png) | Conforme |
| 81 | Tableau de bord interne (widgets) | COND | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mon travail | maJournee, monTravail | [061](captures/acces/061-conducteur-projet-des-tableau-de-bord-clair.png) [062](captures/acces/062-conducteur-projet-des-tableau-de-bord-sombre.png) | Conforme |
| 82 | Routes des trois applications par adresse directe | COND | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 18 refus sans donnée affichée |  | Conforme |
| 83 | Connexion, application interne | COLL | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [066](captures/acces/066-collaborateur-des-tableau-de-bord-clair.png) | Conforme |
| 84 | Connexion, console | COLL | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 85 | Connexion, portail | COLL | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=n.mvondo ; identifiant prérempli « n.mvondo » |  | Conforme |
| 86 | Barre latérale interne | COLL | Relevé des entrées | 10 entrées (catalogue × permissions) | 10 entrées, identiques | [066](captures/acces/066-collaborateur-des-tableau-de-bord-clair.png) | Conforme |
| 87 | Tableau de bord interne (widgets) | COLL | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mes accès (raccourcis) | maJournee, mesAcces | [066](captures/acces/066-collaborateur-des-tableau-de-bord-clair.png) [067](captures/acces/067-collaborateur-des-tableau-de-bord-sombre.png) | Conforme |
| 88 | Routes des trois applications par adresse directe | COLL | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 29 refus sans donnée affichée |  | Conforme |
| 89 | Connexion, application interne | RESP | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [071](captures/acces/071-responsable-service-des-tableau-de-bord-clair.png) | Conforme |
| 90 | Connexion, console | RESP | Connexion sur :6102 | Message bref puis application interne | Conduit vers :6100/des/tableau-de-bord (message « Votre compte n’ouvre pas la console d’administration » relevé en 3.3 et sur la capture 423) |  | Conforme |
| 91 | Connexion, portail | RESP | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=g.ndzana ; identifiant prérempli « g.ndzana » |  | Conforme |
| 92 | Barre latérale interne | RESP | Relevé des entrées | 12 entrées (catalogue × permissions) | 12 entrées, identiques | [071](captures/acces/071-responsable-service-des-tableau-de-bord-clair.png) | Conforme |
| 93 | Tableau de bord interne (widgets) | RESP | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Mes accès (raccourcis) | maJournee, mesAcces | [071](captures/acces/071-responsable-service-des-tableau-de-bord-clair.png) [072](captures/acces/072-responsable-service-des-tableau-de-bord-sombre.png) | Conforme |
| 94 | Routes des trois applications par adresse directe | RESP | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 25 refus sans donnée affichée |  | Conforme |
| 95 | Connexion, application interne | ASYS | Connexion sur :6100 (identifiant, mot de passe, code) | Tableau de bord interne | :6100/des/tableau-de-bord | [076](captures/acces/076-admin-systeme-des-tableau-de-bord-clair.png) | Conforme |
| 96 | Connexion, console | ASYS | Connexion sur :6102 | Console (permission de console) | Console :6102 | [078](captures/acces/078-admin-systeme-console-tableau-de-bord-clair.png) | Conforme |
| 97 | Connexion, portail | ASYS | Connexion sur :6101 | Refus (CNX_COUCHE_REFUSEE), connexion interne proposée, identifiant prérempli | :6100/connexion?motif=aiguillage&depuis=portail&identifiant=admin.systeme ; identifiant prérempli « admin.systeme » |  | Conforme |
| 98 | Barre latérale interne | ASYS | Relevé des entrées | 15 entrées (catalogue × permissions) | 15 entrées, identiques | [076](captures/acces/076-admin-systeme-des-tableau-de-bord-clair.png) | Conforme |
| 99 | Tableau de bord interne (widgets) | ASYS | Relevé des régions du tableau de bord, clair puis sombre | Ma journée, Supervision de la plateforme, Charges en erreur | maJournee, supervision, infrastructure | [076](captures/acces/076-admin-systeme-des-tableau-de-bord-clair.png) [077](captures/acces/077-admin-systeme-des-tableau-de-bord-sombre.png) | Conforme |
| 100 | Barre latérale de la console | ASYS | Relevé des entrées | 9 entrées | 9 entrées, identiques | [078](captures/acces/078-admin-systeme-console-tableau-de-bord-clair.png) | Conforme |
| 101 | Tableau de bord de la console | ASYS | Relevé des régions, clair puis sombre | Charges en erreur, Supervision de la plateforme | chargesEnErreur, supervision | [078](captures/acces/078-admin-systeme-console-tableau-de-bord-clair.png) [079](captures/acces/079-admin-systeme-console-tableau-de-bord-sombre.png) | Conforme |
| 102 | Routes des trois applications par adresse directe | ASYS | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 36 refus sans donnée affichée |  | Conforme |
| 103 | Connexion, portail | CORR | Connexion sur :6101 | Tableau de bord du portail | :6101/portail/tableau-de-bord | [081](captures/acces/081-correspondant-portail-tableau-de-bord-clair.png) [082](captures/acces/082-correspondant-portail-tableau-de-bord-sombre.png) | Conforme |
| 104 | Connexion, application interne | CORR | Connexion sur :6100 | Refus, connexion du portail proposée | :6101/connexion?motif=aiguillage&depuis=des&identifiant=correspondant-a | [083](captures/acces/083-correspondant-interne-refus-vers-portail-clair.png) | Conforme |
| 105 | Connexion, console | CORR | Connexion sur :6102 | Refus, connexion du portail proposée | :6101/connexion?motif=aiguillage&depuis=console&identifiant=correspondant-a |  | Conforme |
| 106 | Barre latérale du portail | CORR | Relevé des entrées | Tableau de bord, Mes liaisons | Tableau de bord, Mes liaisons | [081](captures/acces/081-correspondant-portail-tableau-de-bord-clair.png) | Conforme |
| 107 | Tableau de bord du portail | CORR | Relevé des régions, clair puis sombre | Vue d’ensemble, attentes du GIMAC, liaisons | En un coup d'œil, Ce que le GIMAC attend de vous, Mes liaisons | [081](captures/acces/081-correspondant-portail-tableau-de-bord-clair.png) [082](captures/acces/082-correspondant-portail-tableau-de-bord-sombre.png) | Conforme |
| 108 | Routes des trois applications par adresse directe | CORR | Ouverture de 75 adresses | Permises affichées, autres refusées ou redirigées | 75 / 75 conformes ; 0 refus sans donnée affichée |  | Conforme |
| 109 | Lancer une demande | SNA | Lance « Demande de changement d’adresse IP » au pôle réseau | Instance créée, page de la demande | INS-0011 créée |  | Conforme |
| 110 | Barre latérale, « Processus de l’unité » masquée | RES | Configuration servie : processus visible = faux | Entrée absente | Absente | [201](captures/acces/201-pole-reseau-processus-masque-barre-laterale-clair.png) | Conforme |
| 111 | /des/processus (masquée) | RES | Adresse directe | Accès refusé | Accès refusé | [202](captures/acces/202-pole-reseau-processus-masque-route-refusee-clair.png) | Conforme |
| 112 | Lien profond /des/processus/instances/:id | RES (intervenant) | Adresse directe avec ?etape=ANALYSE | Instance affichée, action « Terminer l’étape » | « Recette ACC : poste de la salle de réunion », bouton Terminer présent : True | [203](captures/acces/203-pole-reseau-lien-profond-instance-clair.png) [204](captures/acces/204-pole-reseau-lien-profond-instance-sombre.png) | Conforme |
| 113 | Tâche reçue, lien « Ouvrir » | RES (intervenant) | Tâches > tâche > Ouvrir | Instance ouverte à l’étape | /des/processus/instances/INS-0011?etape=ANALYSE | [205](captures/acces/205-pole-reseau-tache-ouvrir-instance-clair.png) | Conforme |
| 114 | Lien profond /des/demandes/:id | SNA (initiatrice) | Adresse directe, processus masquée | Demande affichée | « Recette ACC : poste de la salle de réunion » | [206](captures/acces/206-sna-lien-profond-demande-clair.png) | Conforme |
| 115 | Lien profond /des/processus/instances/:id | SNA (initiatrice) | Adresse directe, processus masquée | Instance affichée | « Recette ACC : poste de la salle de réunion » |  | Conforme |
| 116 | Lien profond /des/processus/instances/:id | COLL (membre du pôle réseau) | Adresse directe, processus masquée | Lecture permise par l’unité destinataire (MS-PILOTAGE) | Instance affichée | [207](captures/acces/207-collaborateur-processus-hors-intervenants-clair.png) | Conforme |
| 117 | Lien profond /des/demandes/:id | COLL (membre du pôle réseau) | Adresse directe, processus masquée | Lecture permise par l’unité destinataire (MS-PILOTAGE) | Instance affichée | [208](captures/acces/208-collaborateur-demandes-hors-intervenants-clair.png) | Conforme |
| 118 | Lien profond /des/processus/instances/:id | CHDR (autre unité) | Adresse directe, processus masquée | Lecture refusée par le service, aucune donnée | Message « Instance introuvable », aucune donnée | [209](captures/acces/209-charge-ddrm-processus-hors-intervenants-clair.png) | Conforme |
| 119 | Lien profond /des/demandes/:id | CHDR (autre unité) | Adresse directe, processus masquée | Lecture refusée par le service, aucune donnée | Message « Instance introuvable », aucune donnée | [210](captures/acces/210-charge-ddrm-demandes-hors-intervenants-clair.png) | Conforme |
| 120 | Console : catalogue | SADM | État initial de la barre latérale | Entrée présente | Présente | [301](captures/acces/301-console-catalogue-avant-clair.png) | Conforme |
| 121 | Console : Profils et permissions > Fonctionnalités | SADM | Ouverture de l’onglet | Catalogue listé | Catalogue listé | [302](captures/acces/302-console-catalogue-liste-clair.png) | Conforme |
| 122 | Console : masquer consoleCourriels | SADM | Modifier > Visible désactivé > motif > clé de sécurité | Signature confirmée, entrée retirée, ligne « Masquée » | Signature confirmée, entrée retirée | [304](captures/acces/304-console-catalogue-masquee-clair.png) | Conforme |
| 123 | Console : /console/plateforme/courriels après masquage | SADM | Navigation directe | Accès refusé | Accès refusé | [305](captures/acces/305-console-catalogue-route-refusee-clair.png) | Conforme |
| 124 | Console : rétablir consoleCourriels | SADM | Modifier > Visible activé > signature | Entrée revenue | Revenue | [307](captures/acces/307-console-catalogue-retablie-clair.png) | Conforme |
| 125 | Console : /console/plateforme/courriels après rétablissement | SADM | Navigation directe | Écran affiché | Courriels de la plateforme |  | Conforme |
| 126 | Application interne : catalogue | SADM | État initial de la barre latérale | Entrée présente | Présente | [308](captures/acces/308-des-catalogue-avant-clair.png) | Conforme |
| 127 | Application interne : Profils et permissions > Fonctionnalités | SADM | Ouverture de l’onglet | Catalogue listé | Catalogue listé | [309](captures/acces/309-des-catalogue-liste-clair.png) | Conforme |
| 128 | Application interne : masquer institutions | SADM | Modifier > Visible désactivé > motif > clé de sécurité | Signature confirmée, entrée retirée, ligne « Masquée » | Signature confirmée, entrée retirée | [311](captures/acces/311-des-catalogue-masquee-clair.png) | Conforme |
| 129 | Application interne : /des/institutions après masquage | SADM | Navigation directe | Accès refusé | Accès refusé | [312](captures/acces/312-des-catalogue-route-refusee-clair.png) | Conforme |
| 130 | Application interne : rétablir institutions | SADM | Modifier > Visible activé > signature | Entrée revenue | Revenue | [314](captures/acces/314-des-catalogue-retablie-clair.png) | Conforme |
| 131 | Application interne : /des/institutions après rétablissement | SADM | Navigation directe | Écran affiché | Institutions membres |  | Conforme |
| 132 | Application interne : masquer liaisons | SADM | Modifier > Visible désactivé > signature | Signature confirmée | Refus « Permission inconnue » et message « La clé de sécurité n’a pas répondu » | [316](captures/acces/316-des-catalogue-liaisons-resultat-clair.png) | Anomalie (ACC-02, ACC-03) |
| 133 | Console : Profils et permissions, fiche Chef du DES | SADM | Ouverture de la fiche du profil | Composition du domaine (messagerie, ouverture des liaisons) | Messagerie 0 / 2 ; aperçu 14 entrées au lieu de 16 | [321](captures/acces/321-super-admin-console-profil-chef-des-clair.png) | Anomalie (ACC-02) |
| 134 | 390 px : /des/tableau-de-bord | COLL | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [401](captures/acces/401-collaborateur-mobile-tableau-de-bord-clair.png) | Conforme |
| 135 | 390 px : tiroir de navigation | COLL | Bouton de menu puis Échap | Tiroir ouvert sans débordement ; Échap le referme ; bouton nommé et aria-expanded | ouvert : oui ; largeur défilable : 390 px ; encore ouvert après Échap : oui ; nom « Replier la navigation », aria-expanded absent | [402](captures/acces/402-collaborateur-mobile-navigation-ouverte-clair.png) [403](captures/acces/403-collaborateur-mobile-tableau-de-bord-sombre.png) | Anomalie (ACC-07) |
| 136 | 390 px : /des/taches | COLL | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [404](captures/acces/404-collaborateur-mobile-taches-clair.png) | Conforme |
| 137 | 390 px : /des/demandes/lancer | COLL | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [405](captures/acces/405-collaborateur-mobile-demandes-lancer-clair.png) | Conforme |
| 138 | 390 px : /console/tableau-de-bord | SADM | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [406](captures/acces/406-super-admin-mobile-tableau-de-bord-clair.png) | Conforme |
| 139 | 390 px : tiroir de navigation | SADM | Bouton de menu puis Échap | Tiroir ouvert sans débordement ; Échap le referme ; bouton nommé et aria-expanded | ouvert : oui ; largeur défilable : 390 px ; encore ouvert après Échap : oui ; nom « Replier la navigation », aria-expanded absent | [407](captures/acces/407-super-admin-mobile-navigation-ouverte-clair.png) [408](captures/acces/408-super-admin-mobile-tableau-de-bord-sombre.png) | Anomalie (ACC-07) |
| 140 | 390 px : /console/comptes | SADM | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [409](captures/acces/409-super-admin-mobile-comptes-clair.png) | Conforme |
| 141 | 390 px : /console/profils?vue=fonctionnalites | SADM | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [410](captures/acces/410-super-admin-mobile-profils-vue-fonctionnalites-clair.png) | Conforme |
| 142 | 390 px : /portail/tableau-de-bord | CORR | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [411](captures/acces/411-correspondant-mobile-tableau-de-bord-clair.png) | Conforme |
| 143 | 390 px : tiroir de navigation | CORR | Bouton de menu puis Échap | Tiroir ouvert sans débordement ; Échap le referme ; bouton nommé et aria-expanded | ouvert : oui ; largeur défilable : 390 px ; encore ouvert après Échap : oui ; nom « Replier la navigation », aria-expanded absent | [412](captures/acces/412-correspondant-mobile-navigation-ouverte-clair.png) [413](captures/acces/413-correspondant-mobile-tableau-de-bord-sombre.png) | Anomalie (ACC-07) |
| 144 | 390 px : /portail/liaisons | CORR | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [414](captures/acces/414-correspondant-mobile-liaisons-clair.png) | Conforme |
| 145 | 390 px : /portail/liaisons/LIA-2026-022 | CORR | Ouverture à 390 px | Aucun débordement horizontal, barre latérale repliée | largeur défilable 390 px pour 390 px ; barre latérale repliée | [415](captures/acces/415-correspondant-mobile-liaisons-LIA-2026-022-clair.png) | Conforme |
| 146 | Langue en-US : 6100/des/tableau-de-bord | SADM | Navigateur en-US | Aucune clé de traduction affichée, interface en anglais | clés affichées : 0 ; libellés français restants : 0 ; en-tête « ROLE_SUPER_ADMIN » au lieu du libellé | [416](captures/acces/416-super-admin-en-US-des-tableau-de-bord.png) | Anomalie (ACC-04) |
| 147 | Langue en-US : 6102/console/tableau-de-bord | SADM | Navigateur en-US | Aucune clé de traduction affichée, interface en anglais | clés affichées : 0 ; libellés français restants : 0 | [417](captures/acces/417-super-admin-en-US-console-tableau-de-bord.png) | Conforme |
| 148 | Langue en-US : 6101/portail/tableau-de-bord | CORR | Navigateur en-US | Aucune clé de traduction affichée, interface en anglais | clés affichées : 0 ; libellés français restants : 0 | [418](captures/acces/418-correspondant-en-US-portail-tableau-de-bord.png) | Conforme |
| 149 | Langue en-US : 6100/des/administration/comptes | COLL | Navigateur en-US | Aucune clé de traduction affichée, interface en anglais | clés affichées : 0 ; libellés français restants : 0 | [419](captures/acces/419-collaborateur-en-US-des-administration-comptes.png) | Conforme |
| 150 | Session existante, :6101 | COLL | Ouverture du tableau de bord avec la session de l’onglet | Aiguillage vers l’application interne | Pas d’aiguillage : « Mon espace de certification », session « Correspondant A · Correspondant du participant, Établissement de paiement A » | [420](captures/acces/420-collaborateur-session-existante-6101-clair.png) | Anomalie (ACC-01) |
| 151 | Session existante, :6100 | CORR | Ouverture du tableau de bord avec la session de l’onglet | Aiguillage vers le portail | Pas d’aiguillage : « Tableau de bord », session « Bertrand Nguema Obiang · Chef de la DDRM » | [421](captures/acces/421-correspondant-session-existante-6100-clair.png) | Anomalie (ACC-01) |
| 152 | Session existante, :6102 | CORR | Ouverture du tableau de bord avec la session de l’onglet | Aiguillage vers le portail | Pas d’aiguillage : « Tableau de bord du super administrateur », session « Serge Mouelle Kombi · Super administrateur » | [422](captures/acces/422-correspondant-session-existante-6102-clair.png) | Anomalie (ACC-01) |
| 153 | Session existante, console | COLL | Ouverture de /console/comptes avec la session de l’onglet | Message bref puis application interne | Message « Votre compte n'ouvre pas la console d'administration  » puis :6100/des/tableau-de-bord | [423](captures/acces/423-collaborateur-session-existante-6102-clair.png) | Conforme |
| 154 | En-tête du tableau de bord interne | SADM | Lecture du profil affiché sous le nom | Super administrateur | Serge Mouelle Kombi · ROLE_SUPER_ADMIN | [011](captures/acces/011-super-admin-des-tableau-de-bord-clair.png) | Anomalie (ACC-04) |
| 155 | Identité du compte selon l’application | RSSI | h.kamga sur :6100 puis sur :6102 | Même personne dans les deux applications | Hervé Kamga Tchoupo (:6100), Carine Ebongue Ndjock (:6102) | [016](captures/acces/016-rssi-des-tableau-de-bord-clair.png) [018](captures/acces/018-rssi-console-tableau-de-bord-clair.png) | Anomalie (ACC-05) |
| 156 | Matrice des profils de l’interface (permissions.ts) | tous | Comparaison avec le générateur Keycloak exécuté | Mêmes permissions | 15 profils identiques ; ING sans HOM_REPLAY_ADMIN, CORR sans NOT_INBOX_READ |  | Anomalie (ACC-06) |
| 157 | Catalogue simulé (copie) / catalogue MS-IAM | tous | Comparaison des 57 fonctionnalités | Identiques | Identiques (visible, permissions, widgets, ordre, groupe) |  | Conforme |
| 158 | Catalogue des fonctionnalités, mise en page | SADM | Lecture du tableau | Icône à gauche du libellé | Icône au-dessus du libellé sur 4 lignes | [304](captures/acces/304-console-catalogue-masquee-clair.png) | Anomalie (ACC-08) |

## 3. Matrices des accès

### 3.1 Entrées de barre latérale × profil

V : entrée visible ; · : entrée absente ; ✗ : écart avec l'attendu ; n/a : application non ouverte au profil (aiguillé ailleurs). Comparaison faite sur les adresses des liens de la barre latérale (« Mon compte » et « Se déconnecter », communs, exclus). Les fonctionnalités du catalogue sans groupe (dossierCertification, quitus, attestations, registre, referentielsMetierAdmin, portail) ne sont pas des entrées de barre latérale et n'y apparaissent pour aucun profil, comme attendu.

| Application | Entrée | Prédicat du catalogue | ING | CDES | SADM | RSSI | CONF | DG | CHDR | CFDR | SNA | PROD | RES | SYS | COND | COLL | RESP | ASYS | CORR |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Application interne | Tableau de bord | tout compte | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Messagerie | l’une de : COL_MESSAGE_READ | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Notifications | l’une de : NOT_INBOX_READ | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Ma journée | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Agenda | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Tâches | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Rapports | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Mon équipe | l’une de : PIL_TEAM_READ | · | V | · | V | V | V | · | V | · | · | · | · | · | · | V | · | n/a |
| Application interne | Processus de l’unité | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Lancer une demande | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Mes demandes | l’une de : PIL_ACTIVITY_OWN | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Institutions membres | l’une de : PAR_INSTITUTION_READ | V | V | V | V | V | V | V | V | V | V | V | V | V | · | · | · | n/a |
| Application interne | Lancer une certification | l’une de : PAR_INSTITUTION_CREATE, PAR_ADHESION_INSTRUCT, HOM_LIAISON_OPEN | · | V | · | · | · | · | V | V | · | · | · | · | · | · | · | · | n/a |
| Application interne | Mes actions de certification | l’une de 19 permissions | V | V | V | V | V | V | V | V | V | V | V | V | V | · | · | · | n/a |
| Application interne | Liaisons et parcours | l’une de : HOM_LIAISON_LIST | V | V | V | V | V | V | V | V | V | V | V | V | V | · | · | · | n/a |
| Application interne | Mes projets | tous : HOM_LIAISON_LIST, CER_PV_ESTABLISH | · | · | · | · | · | · | · | · | · | · | · | · | V | · | · | · | n/a |
| Application interne | Référentiels d’opérations | l’une de : HOM_REFERENTIEL_READ | V | V | V | V | V | V | V | V | V | V | V | V | V | · | V | · | n/a |
| Application interne | Comptes utilisateurs | l’une de : IAM_ACCOUNT_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Demandes à traiter | l’une de : IAM_ACCOUNT_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Profils et permissions | l’une de : IAM_ACCOUNT_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Matrice des pouvoirs | tous : IAM_ACCOUNT_READ, IAM_PROFILE_EDIT | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Organisation | l’une de : IAM_ACCOUNT_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Organigramme | l’une de : IAM_ACCOUNT_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Journal d’administration | l’une de : IAM_JOURNAL_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Politique de sécurité | l’une de : PLATFORM_SECURITY_POLICY_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Supervision | l’une de : PLATFORM_SUPERVISION_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Application interne | Infrastructure | l’une de : PLATFORM_OPS_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Application interne | Configuration | l’une de : PLATFORM_CONFIG_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Application interne | Journalisation | l’une de : PLATFORM_CONFIG_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Application interne | Courriels de la plateforme | l’une de : PLATFORM_MESSAGING_EDIT | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Console de la plateforme | l’une de : PLATFORM_CONFIG_READ | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Console | Retour au département | tout compte | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Tableau de bord | tout compte | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Messagerie | l’une de : COL_MESSAGE_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Notifications | l’une de : NOT_INBOX_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Mon activité | l’une de : PIL_ACTIVITY_OWN | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Comptes | l’une de : IAM_ACCOUNT_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Demandes à traiter | l’une de : IAM_ACCOUNT_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Profils et permissions | l’une de : IAM_ACCOUNT_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Matrice des pouvoirs | tous : IAM_ACCOUNT_READ, IAM_PROFILE_EDIT | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Organisation | l’une de : IAM_ACCOUNT_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Organigramme | l’une de : IAM_ACCOUNT_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Journal d’administration | l’une de : IAM_JOURNAL_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Politique de sécurité | l’une de : PLATFORM_SECURITY_POLICY_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Centre de contrôle | l’une de : PLATFORM_SUPERVISION_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Infrastructure | l’une de : PLATFORM_OPS_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Configuration | l’une de : PLATFORM_CONFIG_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Journalisation | l’une de : PLATFORM_CONFIG_READ | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Courriels de la plateforme | l’une de : PLATFORM_MESSAGING_EDIT | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Portail | Tableau de bord | l’une de : HOM_PORTAL_READ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V |
| Portail | Mes liaisons | l’une de : HOM_PORTAL_READ | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V |

Résultat : 552 cases comparées, 0 écart.

### 3.2 Widgets des tableaux de bord × profil

Widgets de l'application interne : fonctionnalité ouverte et prédicat du widget (catalogue), « Mes accès » quand seul « Ma journée » s'affiche ; « Gestion des utilisateurs » s'accompagne des régions « Comptes par profil », « Activité des 30 derniers jours » et « Derniers actes d'administration ». Widgets de la console : règles de packages/administration/src/pages/TableauDeBordPage.tsx (comptes, actes et politique pour la posture de sécurité sans octroi privilégié, charges en erreur, supervision). Tous les tableaux de bord relevés sont identiques en clair et en sombre.

| Application | Widget | ING | CDES | SADM | RSSI | CONF | DG | CHDR | CFDR | SNA | PROD | RES | SYS | COND | COLL | RESP | ASYS | CORR |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Application interne | Ma journée | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | V | n/a |
| Application interne | Gestion des utilisateurs (+ derniers actes) | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | · | n/a |
| Application interne | Supervision de la plateforme | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Application interne | Charges en erreur | · | · | V | V | · | · | · | · | · | · | · | · | · | · | · | V | n/a |
| Application interne | Relations avec les membres (DDRM) | · | · | · | · | · | · | V | V | · | · | · | · | · | · | · | · | n/a |
| Application interne | Pilotage des projets de certification | · | V | · | · | · | V | · | V | · | · | · | · | · | · | · | · | n/a |
| Application interne | Mon travail | V | V | · | V | V | V | V | V | V | V | V | V | V | · | · | · | n/a |
| Application interne | Mes accès (raccourcis) | · | · | · | · | · | · | · | · | · | · | · | · | · | V | V | · | n/a |
| Console | Gestion des utilisateurs | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Derniers actes d’administration | n/a | n/a | · | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Politique de sécurité | n/a | n/a | · | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | · | n/a |
| Console | Charges en erreur | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |
| Console | Supervision de la plateforme | n/a | n/a | V | V | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | V | n/a |

### 3.3 Routes × profil (ouverture par adresse directe)

A : écran affiché ; R : page « Accès refusé » (« Votre profil ne donne pas accès à cet écran. ») ; Rd : redirection vers le tableau de bord (route inconnue) ; Ai : aiguillage (message bref puis application interne) ; Cx : écran de connexion (aucune session de l'application dans l'onglet ; la connexion y est ensuite refusée par la couche, CNX_COUCHE_REFUSEE) ; ✗ : écart avec l'attendu.

Contrôles faits sur chaque ouverture : verdict de la garde, absence d'erreur JavaScript, et, pour chaque refus, contenu de la zone principale réduit au seul message de refus (57 caractères, aucune donnée affichée : 0 fuite sur tous les refus). Remarques sur les écrans affichés : `/des/processus/instances/INS-0001` et `/des/demandes/INS-0001` sont ouverts par la garde (PIL_ACTIVITY_OWN, D-98) puis MS-PILOTAGE simulé répond « Instance introuvable » (aucune instance de ce numéro, ou instance non lisible) ; `/des/processus/PRC-0001` est affiché pour les profils qui lisent l'unité du processus et donne « Processus introuvable » pour les autres (RSSI, CONF, CHDR, CFDR, SNA, PROD, SYS, COND, ASYS), contrôle de lecture du service sans fuite ; `/des/liaisons/LIA-2025-071/mise-en-service` affiche « Aucun dossier de mise en service » (état de la liaison).

| Route | ING | CDES | SADM | RSSI | CONF | DG | CHDR | CFDR | SNA | PROD | RES | SYS | COND | COLL | RESP | ASYS | CORR |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `/des/tableau-de-bord` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/mon-compte` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/institutions` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/institutions/INST-0042` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/liaisons` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/liaisons/LIA-2026-030` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/liaisons/LIA-2026-017/operations/OPI-017-3/cloture` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/liaisons/LIA-2025-071/mise-en-service` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/parcours` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/parcours/LIA-2026-030` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/certification/lancer` | R | A | R | R | R | R | A | A | R | R | R | R | R | R | R | R | Cx |
| `/des/certification/mes-actions` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | R | R | Cx |
| `/des/messagerie` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/notifications` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/activite` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/agenda` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/taches` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/rapports` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/equipe` | R | A | R | A | A | A | R | A | R | R | R | R | R | R | A | R | Cx |
| `/des/equipe/demandes` | R | A | R | A | A | A | R | A | R | R | R | R | R | R | A | R | Cx |
| `/des/processus` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/processus/instances/INS-0001` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/processus/PRC-0001` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/demandes` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/demandes/lancer` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/demandes/INS-0001` | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | A | Cx |
| `/des/referentiels/operations` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | A | R | Cx |
| `/des/referentiels/types-projet` | A | A | A | A | A | A | A | A | A | A | A | A | A | R | A | R | Cx |
| `/des/administration/comptes` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/comptes/nouveau` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/comptes/u-chef-des` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/journal` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/profils` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/demandes` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/matrice-pouvoirs` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/organisation` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/organigramme` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/administration/politique-securite` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/plateforme/supervision` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | A | Cx |
| `/des/plateforme/infrastructure` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | A | Cx |
| `/des/plateforme/configuration` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | A | Cx |
| `/des/plateforme/journalisation` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | A | Cx |
| `/des/plateforme/courriels` | R | R | A | A | R | R | R | R | R | R | R | R | R | R | R | R | Cx |
| `/des/route-inconnue` | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Rd | Cx |
| `/console/tableau-de-bord` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/mon-compte` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/comptes` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/comptes/nouveau` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/comptes/u-chef-des` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/journal` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/profils` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/demandes` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/matrice-pouvoirs` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/organisation` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/organigramme` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/politique-securite` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/messagerie` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/notifications` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/plateforme/courriels` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | R | Cx |
| `/console/plateforme/configuration` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/plateforme/journalisation` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/plateforme/supervision` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/plateforme/infrastructure` | Ai | Ai | A | A | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | A | Cx |
| `/console/route-inconnue` | Ai | Ai | Rd | Rd | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Ai | Rd | Cx |
| `/portail/tableau-de-bord` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/mon-compte` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons/LIA-2026-022` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons/LIA-2026-046/coordonnees` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons/LIA-2025-088/livrables/LIA-2025-088%3A15` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons/LIA-2026-060/proces-verbal` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons/LIA-2026-027/attestations` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/liaisons/LIA-2025-088/fiche-suivi` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/notifications` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | A |
| `/portail/route-inconnue` | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Cx | Rd |

Résultat : 1275 ouvertures, 0 écart.

## 4. Anomalies

### ACC-01 (majeure, services simulés) : une session existante n'est pas aiguillée ; elle prend un profil par défaut

- Étapes : ouvrir un onglet dont la session de démonstration (gm.sessionDemo, ou fragment `#gm-session-demo=` repris par `reprendreSessionDemo`) porte `{"identifiant":"correspondant-a","role":"ROLE_CORRESPONDANT"}`, puis ouvrir http://localhost:6102/console/tableau-de-bord ; recommencer sur http://localhost:6100/des/tableau-de-bord ; ouvrir http://localhost:6101/portail/tableau-de-bord avec la session de n.mvondo (ROLE_COLLABORATEUR).
- Attendu (D-79, garde `Aiguillage`) : le correspondant est conduit vers le portail ; le collaborateur vers l'application interne.
- Observé : la console ouvre la session comme **super administrateur** (Serge Mouelle Kombi, toute l'administration) ; l'application interne comme **chef de la DDRM** (Bertrand Nguema Obiang) ; le portail ouvre l'espace du correspondant A pour le collaborateur. Captures [420](captures/acces/420-collaborateur-session-existante-6101-clair.png), [421](captures/acces/421-correspondant-session-existante-6100-clair.png), [422](captures/acces/422-correspondant-session-existante-6102-clair.png). Le cas de la console ouverte par un collaborateur, lui, est bien aiguillé ([423](captures/acces/423-collaborateur-session-existante-6102-clair.png)).
- Cause probable : le rôle de la session de démonstration inconnu de l'application retombe sur un profil par défaut au lieu de produire une session d'institution que `Aiguillage` renverrait : apps/console/src/App.tsx (`profilDemo`, repli `'ROLE_SUPER_ADMIN'`), apps/departement/src/profils.ts (`roleDemo`, repli `'ROLE_CHEF_DDRM'`), apps/portail/src/App.tsx (`sessionDemo` ignore le rôle et rend toujours `sessionCorrespondant`). Branche simulée absente des constructions de production, mais elle fausse la démonstration de l'aiguillage D-79 et donne, dans une construction de recette, les droits du super administrateur à toute session au rôle inconnu.

### ACC-02 (majeure, services simulés) : catalogue des profils et permissions de la console simulée non régénéré

- Étapes : console, super administrateur, « Profils et permissions », fiche « Chef du DES » ; puis application interne, « Profils et permissions » > Fonctionnalités, modifier `liaisons` (Visible désactivé), motif, clé de sécurité.
- Attendu : composition du profil identique au domaine (generer-realm-gimac.py) ; modification acceptée.
- Observé : la fiche du chef du DES montre Messagerie 0 / 2 et un aperçu de 14 entrées (16 en réalité : Messagerie et Lancer une certification manquent) [321](captures/acces/321-super-admin-console-profil-chef-des-clair.png) ; la modification de `liaisons` est refusée « Permission inconnue » [316](captures/acces/316-des-catalogue-liaisons-resultat-clair.png). Par le même calcul, `mesActionsCertification`, `dossierCertification` (CER_SECURITY_VERIFY) et `portail` (CER_PORTAL_SIGN) ne peuvent pas être modifiées.
- Écarts de `PROFILS_REALM` avec le générateur : tous les profils internes sans COL_MESSAGE_READ et COL_MESSAGE_SEND ; ING avec HOM_LIAISON_OPEN en trop ; CDES et CFDR sans HOM_LIAISON_OPEN, HOM_PROJECT_DESIGNATE, HOM_STEP_MANAGE ; CHDR sans HOM_STEP_MANAGE ; RSSI sans CER_SECURITY_VERIFY ni HOM_EVIDENCE_SUBMIT ; SNA sans CER_DOSSIER_VERIFY ; PROD sans CER_CHANGE_RECORD_LINK ni CER_GOLIVE_REPORT_SUBMIT ; COND sans CER_PV_ESTABLISH, HOM_OPERATION_VALIDATE, HOM_STEP_MANAGE ; CORR sans CER_PORTAL_SIGN ni NOT_INBOX_READ. `PERMISSIONS_REALM` ne contient pas CER_PORTAL_SIGN, CER_SECURITY_VERIFY, HOM_PROJECT_DESIGNATE (ni INTERNAL_COLLABORATION_READ, INTERNAL_DOCUMENT_DEPOSIT).
- Cause : packages/administration/src/services/simule/catalogue.ts (« Généré à partir du script du domaine ; ne pas modifier à la main ») n'a pas été régénéré depuis les décisions D-56 à D-97 ; packages/administration/src/services/simule/index.ts (`PERMISSIONS_DOMAINE`) en dépend.

### ACC-03 (mineure) : refus du service présenté comme une panne de la clé de sécurité

- Étapes : comme ACC-02, modification de `liaisons`.
- Observé : la fenêtre de signature affiche à la fois « La clé de sécurité n'a pas répondu. Vérifiez que la clé est branchée puis réessayez. » et « Permission inconnue. » [316](captures/acces/316-des-catalogue-liaisons-resultat-clair.png) ; la clé a répondu, c'est le service qui refuse (400 IAM_PERMISSION_UNKNOWN).
- Cause probable : packages/design-system/src/saisie/SignatureField/SignatureField.tsx (`signature.erreur`) passe à l'état d'échec de la clé pour toute erreur levée par l'exécution de l'acte.

### ACC-04 (mineure) : code technique ROLE_SUPER_ADMIN affiché sur le tableau de bord interne

- Étapes : se connecter comme super.admin sur http://localhost:6100.
- Observé : en-tête « Serge Mouelle Kombi · ROLE_SUPER_ADMIN » en fr-FR et en en-US ([011](captures/acces/011-super-admin-des-tableau-de-bord-clair.png), [416](captures/acces/416-super-admin-en-US-des-tableau-de-bord.png)) ; les 16 autres profils affichent leur libellé.
- Cause : clé `profil.ROLE_SUPER_ADMIN` absente de packages/lot1/src/i18n/ecrans/par.ts (TableauDeBordPage.tsx, `t(`profil.${r}`, { defaultValue: r })`) ; la console, qui lit le libellé servi, affiche bien « Super administrateur ».

### ACC-05 (mineure, services simulés) : identité et permissions du RSSI et du super administrateur différentes selon l'application

- Étapes : se connecter comme h.kamga sur l'application interne puis sur la console.
- Observé : l'application interne ouvre « Hervé Kamga Tchoupo » [016](captures/acces/016-rssi-des-tableau-de-bord-clair.png), la console « Carine Ebongue Ndjock » [018](captures/acces/018-rssi-console-tableau-de-bord-clair.png) pour le même identifiant, alors que les comptes de démonstration sont réputés communs aux trois applications (profilsDemo.ts). Les permissions de la console suivent le jeu de l'administration : RSSI sans CER_SECURITY_VERIFY ni HOM_EVIDENCE_SUBMIT, super administrateur sans REF_METIER_ADMIN, HOM_REFERENTIEL_MIGRATE ni HOM_REFERENTIEL_READ_ALL (sans effet sur la barre latérale de la console, dont aucune entrée ne dépend de ces permissions).
- Cause : apps/console/src/navigation.ts (`PROFILS_DEMO = { ...PROFILS_INTERNES_DEMO, ...PROFILS_ADMINISTRATION }`) et packages/administration/src/profilsDemo.ts, non alignés sur generer-realm-gimac.py.

### ACC-06 (mineure) : matrice des profils de l'interface différente du domaine pour deux profils

- Constat (comparaison des permissions de packages/lot1/src/permissions.ts, `PROFILS`, avec le générateur exécuté) : ROLE_INGENIEUR_HOMOLOGATION sans HOM_REPLAY_ADMIN ; ROLE_CORRESPONDANT sans NOT_INBOX_READ (notifications du portail, D-93 §11.4). Les 15 autres profils sont identiques.
- Effet observé : aucun aujourd'hui (aucune fonctionnalité du catalogue ne cite HOM_REPLAY_ADMIN ; la cloche du portail simulé ne teste pas NOT_INBOX_READ). À aligner pour que les services simulés reproduisent les jetons réels.

### ACC-07 (mineure, accessibilité) : tiroir de navigation à 390 px

- Étapes : à 390 px, toucher le bouton de menu de la barre supérieure, puis appuyer sur Échap.
- Observé : le tiroir s'ouvre sans débordement [402](captures/acces/402-collaborateur-mobile-navigation-ouverte-clair.png), mais le bouton qui l'ouvre s'appelle « Replier la navigation », sans `aria-expanded` ; le focus reste sur le bouton et Échap ne referme pas le tiroir (le gestionnaire Échap n'agit que si le focus est dans le tiroir). Même constat pour SADM et CORR ([407](captures/acces/407-super-admin-mobile-navigation-ouverte-clair.png), [412](captures/acces/412-correspondant-mobile-navigation-ouverte-clair.png)).
- Cause probable : packages/design-system/src/enveloppe/TopBar/TopBar.tsx (bouton `topbar.replier`) et packages/socle/src/enveloppe/Enveloppe.tsx (tiroir `gm-shell-tiroir`, `onKeyDown` sur le voile, focus non déplacé dans le tiroir).

### ACC-08 (mineure, mise en page) : icône au-dessus du libellé dans le catalogue des fonctionnalités

- Observé : dans « Profils et permissions » > Fonctionnalités, l'icône passe au-dessus du libellé pour plusieurs lignes (Infrastructure, Configuration, Journalisation, Courriels de la plateforme) au lieu d'être à sa gauche [304](captures/acces/304-console-catalogue-masquee-clair.png).
- Cause probable : rendu de la colonne `libelle` de packages/administration/src/composants/fonctionnalites.tsx (ligne sans `flex-wrap: nowrap` quand le libellé et la clé sont longs).

## 5. Observations sans anomalie

- Toutes les entrées de barre latérale (applications interne et console, portail) et tous les widgets des 17 profils sont conformes à l'attendu calculé ; les trois profils qui portent une permission de console (SADM, RSSI, ASYS) atterrissent sur la console quand ils s'y connectent, les 13 autres profils internes sont aiguillés vers l'application interne avec un message bref.
- Les 1 275 ouvertures par adresse directe sont conformes : chaque route non permise donne la page « Accès refusé » (sans donnée) ou l'aiguillage, chaque route permise s'affiche sans erreur JavaScript.
- Liens profonds D-98 : avec « Processus de l'unité » masquée (entrée absente, route refusée), l'intervenant du pôle réseau ouvre l'instance et son étape (« Terminer l'étape » présent), depuis l'adresse et depuis sa tâche ; l'initiatrice ouvre sa demande. Un compte d'une autre unité (chargé de la DDRM) reçoit « Instance introuvable », sans aucune donnée ; la collaboratrice, membre du pôle réseau destinataire, lit l'instance de son unité (règle de lecture par unité de MS-PILOTAGE). Le message « L'instance n'a pas pu être lue » est présenté comme une erreur avec « Réessayer » ([209](captures/acces/209-charge-ddrm-processus-hors-intervenants-clair.png)) : choix d'un 404 qui ne révèle pas l'existence, à garder en tête pour la formulation.
- Catalogue : masquer « Courriels de la plateforme » (console) et « Institutions membres » (application interne) retire l'entrée sans rechargement, marque la ligne « Masquée » et fait refuser l'écran ; le rétablissement la remet ([304](captures/acces/304-console-catalogue-masquee-clair.png), [305](captures/acces/305-console-catalogue-route-refusee-clair.png), [307](captures/acces/307-console-catalogue-retablie-clair.png), [311](captures/acces/311-des-catalogue-masquee-clair.png), [312](captures/acces/312-des-catalogue-route-refusee-clair.png), [314](captures/acces/314-des-catalogue-retablie-clair.png)).
- 390 px : aucun débordement horizontal (largeur défilable 390 px) sur les 9 écrans relevés ; tableaux rendus en fiches empilées ([409](captures/acces/409-super-admin-mobile-comptes-clair.png)).
- Langue : en en-US, aucune clé de traduction affichée et aucun libellé d'interface resté en français sur les quatre écrans relevés (les données de démonstration, comme le titre d'un rendez-vous, restent en français) ; en fr-FR, aucun texte d'interface en anglais relevé sur les captures.
- Notifications du portail : servies en services simulés seulement ; en services réels, la page et la cloche sont retirées tant que la passerelle ne sert pas /bff/portail/notification (apps/portail/src/App.tsx, limite documentée).

## 6. Correction

Corrections du 08/10/2026, menées avec celles du lot « administration » (registre : [corrections/acces.json](corrections/acces.json), captures « après » : [captures/acces/corrections/](captures/acces/corrections/), refaites sur une construction isolée en services simulés, ports 6600 à 6602, fr-FR).

| Anomalie | Correctif | Test | Après |
|---|---|---|---|
| ACC-01 | Une session de démonstration garde son rôle dans chaque application (`sessionDemoDuRole`, `packages/lot1/src/profilsDemo.ts`) : sur la console (`apps/console/src/App.tsx`) et l'application interne (`apps/departement/src/profils.ts`), un profil externe devient un compte d'institution ; sur le portail (`apps/portail/src/App.tsx`), un profil interne reste un compte interne. La garde `Aiguillage` (D-79) les conduit alors vers leur application ; plus aucun profil par défaut pour une session ouverte. | `packages/lot1/src/domaine.test.ts` (session de démonstration d'un rôle). | [ACC-01](captures/acces/corrections/ACC-01-apres.png) |
| ACC-02 | Même cause et même correctif que ADM-01 : catalogue des permissions et des profils du serveur simulé produit par `infra/keycloak/generer-realm-gimac.py` (`frontend/packages/lot1/src/domaine-gimac.json`), plus aucune recopie. Toute fonctionnalité du catalogue (dont `liaisons`, `mesActionsCertification`, `dossierCertification`, `portail`) se modifie sans « Permission inconnue ». | `packages/administration/src/services/simule/catalogue.test.ts` (concordance avec `infra/keycloak/gimac-realm.json`, catalogue des fonctionnalités modifiable). | [ACC-02](captures/acces/corrections/ACC-02-apres.png) |
| ACC-03 | Même correctif que ADM-03 : état « Acte refusé par le service » de `SignatureField.tsx` et `SignDialog.tsx`, message et code du service, sans « Réessayer » ni mise en cause de la clé. | `packages/administration/src/pages/RecetteAdministration.test.tsx` (ADM-03). | [ACC-03](captures/acces/corrections/ACC-03-apres.png) |
| ACC-04 | Clé `profil.ROLE_SUPER_ADMIN` ajoutée en français, anglais et espagnol (`packages/lot1/src/i18n/ecrans/par.ts`). | `domaine.test.ts` : chaque profil du domaine a son libellé d'interface dans les trois langues. | [ACC-04](captures/acces/corrections/ACC-04-apres.png) |
| ACC-05 | La console reprend l'identité des comptes communs aux trois applications (RSSI : h.kamga, Hervé Kamga Tchoupo) et la composition du domaine pour chaque profil privilégié (`packages/administration/src/profilsDemo.ts`) ; l'annuaire simulé de la console suit (`services/simule/donnees.ts`). Specs mises à jour : `e2e/specs/connexion.spec.ts`, `e2e/specs/accessibilite.spec.ts`, `apps/console/src/App.test.tsx`. | `catalogue.test.ts` (profils de démonstration = domaine Keycloak). | [ACC-05](captures/acces/corrections/ACC-05-apres.png) |
| ACC-06 | Matrice `PROFILS` de `packages/lot1/src/permissions.ts` lue dans le domaine généré : l'ingénieur d'homologation porte HOM_REPLAY_ADMIN, le correspondant NOT_INBOX_READ ; présentations (`presentations.ts`) et profils privilégiés (`pouvoirs.ts`) aussi. | `domaine.test.ts`, `permissions.test.ts` mis à jour (correspondant avec NOT_INBOX_READ). | Aucun effet visible : pas de capture. |
| ACC-07 | Bouton de menu nommé « Ouvrir la navigation » ou « Fermer la navigation », avec `aria-expanded` et `aria-controls` (`TopBar.tsx`, `Button.tsx`) ; à l'ouverture, le focus entre dans le tiroir ; Échap le referme d'où que vienne la touche et rend le focus au bouton (`socle/src/enveloppe/Enveloppe.tsx`). | `socle/src/enveloppe/Enveloppe.test.tsx` (accessibilité ACC-07). | [ACC-07](captures/acces/corrections/ACC-07-apres.png) |
| ACC-08 | Colonne Libellé du catalogue des fonctionnalités en ligne sans retour : l'icône reste à gauche, seul le texte se replie (`composants/fonctionnalites.tsx`). | Aucun test unitaire (mise en page) ; vérifié sur la capture. | [ACC-08](captures/acces/corrections/ACC-08-apres.png) |

**Validation** : compilation TypeScript sans erreur de socle, design-system, compte, administration, console et portail (lot1 et application interne : seule erreur dans homologation-recette.test.ts, fichier d'un autre lot en cours) ; tests unitaires : administration 143/143 (dont catalogue.test.ts et RecetteAdministration.test.tsx), compte 45/45, console 8/8, socle (élision, tiroir) et lot1 (domaine.test.ts, permissions, fonctionnalités, administration) verts sur les fichiers touchés ; les échecs restants de lot1, du socle (connexion, sous charge), du design-system (finition des dates) et de l'application interne (instance de processus inconnue) relèvent de fichiers modifiés par les autres lots ; `python3 infra/keycloak/generer-realm-gimac.py` : gimac-realm.json et gimac-realm-production.json inchangés, domaine-gimac.json stable ; `infra/ci/valider-infra.sh` : contrôle du domaine Keycloak réussi (il compare désormais aussi domaine-gimac.json), deux échecs étrangers à ce lot (tests OPA, copies kubernetes-gimac) ; E2E sur ports isolés (6600, -fixadm) : accessibilite, administration, configuration-d91-d94, connexion, console-*, infrastructure*, profils-organisation, roles-redirections : 146 réussis, 10 échecs dont 4 corrigés ensuite dans les specs (identité du RSSI, 14 comptes, fenêtre « Acte refusé » à fermer : console-refus, console-rssi, console-super-admin repassés au vert) et 6 dans configuration-d91-d94 (parcours, portail et tâches à prendre, modifiés par les lots certification et quotidien). Sorties E2E supprimées, captures versionnées de e2e/captures remises à l'identique.
