# Recette du lot CER : décision de certification et portail du participant

Recette menée le 8 octobre 2026 sur les applications construites en services simulés : application interne http://localhost:6100, portail http://localhost:6101, console http://localhost:6102 (ports 6000 à 6002 refusés par Chromium, ERR_UNSAFE_PORT). Navigateur Chromium sans tête, contexte en `fr-FR` (quelques écrans en `en-US`), 1440 px et 390 px, thèmes clair et sombre. Chaque geste a été fait par clic dans le navigateur (Playwright piloté pas à pas, une seule fenêtre ouverte à la fois), chaque capture a été relue. État de démonstration continu (`gm.demoContinue`) transmis d'un acteur à l'autre et de l'application interne au portail.

Captures : `docs/recette/captures/certification/` (108 fichiers, numérotés dans l'ordre du parcours).

## 1. Périmètre

**Profils** : chef du DES (ROLE_CHEF_DES), conducteur de projet (ROLE_CONDUCTEUR_PROJET), RSSI (ROLE_RSSI), conformité (ROLE_CONFORMITE), directeur général (ROLE_DG), correspondant d'institution (ROLE_CORRESPONDANT, portail). Acteurs d'appoint, pour faire avancer les liaisons : chargé DDRM, Normes et Agréments (SNA), production monétique, pôle système.

**Écrans** : tableau de bord (« Mon travail »), Lancer une certification, Mes actions de certification (onglets À faire, À suivre, Liaisons suivies), dossier de liaison (onglets Périmètre, Étapes, Opérations, Décision, Mise en service, Blocages, Gouvernance), tiroir d'opération, onglet Décision (verrou 1 procès-verbal, circuit de signature, visas, verrou 2 approbation de la sécurité, dossier et fiche de changement, attestations), fiche d'institution. Portail : Mon espace de certification, Mes liaisons, suivi d'une liaison, procès-verbal, attestations, coordonnées d'essai, fiche de suivi des services, dépôt d'un livrable, cloche et liste des notifications, préférences (langue, thème), Mon compte, connexion.

**Flux de bout en bout réalisés**

| Flux | Liaison | Gestes |
|---|---|---|
| A. Type créé dans la plateforme (D-98) | LIA-2026-066, Établissement de paiement A, Agrégateur de paiement | lancement par le chef du DES, refus du doublon, A1 (dépôt DDRM, validation signée du conducteur), A2 (essais SNA, validation), A3 (constat de couverture), établissement du PV par le conducteur, signature GIMAC par le chef du DES (carte visée par `?operation=`), signature du participant au portail, dossier de changement, fiche de changement (référence invalide puis valide), relecture RSSI, approbation, bascule A8, jalon EN_PRODUCTION, institution INST-0042 Actif |
| B. Modèle Délégataire, rang 19 | LIA-2026-027, Établissement de paiement A | signature du PV par le participant au portail d'abord (rang 19), puis signature GIMAC (PV « Signé des deux parties », liaison Certifiée), dépôt d'attestation au portail (refus .docx, dépôt PDF), dossier de changement, rejet motivé par la RSSI, redépôt, déclaration de conformité, archivage, fiche de changement, relecture, approbation de la sécurité |
| C. Visas conformité et DG | LIA-2026-019, Banque G | vérification et archivage des attestations, fiche CHG-2026-519, relecture refusant l'approbation tant que les visas manquent, visa défavorable sans motif impossible, visa favorable de la conformité, visa favorable du DG, relecture : approbation prononcée |
| D. Mise en production, rapport | LIA-2026-005, Banque E | rangs 28 et 29 (pôle système), 30 et 31 (production monétique, rapport de mise en production), validations signées du chef du DES, liaison En production |
| E. Circuit de signature d'opération, ordre imposé | LIA-2026-036, rang 11 | VISA_NORMES (ordre 1, SNA) puis VISA_CONDUITE (ordre 2, conducteur) ; le conducteur n'a pas de bouton tant que l'ordre 1 n'est pas signé |
| F. Portail complet | liaisons de l'Établissement de paiement A | tableau de bord, liaisons, suivi, PV, attestations, coordonnées (invalides puis valides), fiche de suivi (incomplète puis transmise), notifications, préférences, Mon compte, cloisonnement, connexions croisées |

## 2. Tableau des contrôles

| N° | Écran ou flux | Profil | Geste | Résultat attendu | Résultat observé | Capture | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | Barre latérale | chef DES | ouverture du tableau de bord | entrées du catalogue permises par le profil | 18 entrées conformes au catalogue (dont Lancer une certification, Mes actions, Liaisons, Mon équipe) | 001 | Conforme |
| 2 | Barre latérale | conducteur | idem | sans Mon équipe ni Lancer une certification, avec Mes projets | conforme | 007 | Conforme |
| 3 | Barre latérale | RSSI | idem | entrées métier, administration et plateforme | 31 entrées, conformes | 041 | Conforme |
| 4 | Barre latérale | conformité, DG | idem | entrées métier sans administration | conformes | 052, 062 | Conforme |
| 5 | Barre latérale | correspondant | ouverture du portail | Tableau de bord, Mes liaisons, Mon compte, Se déconnecter | conforme | 020 | Conforme |
| 6 | Lancer une certification | chef DES | institution, type, conducteur, Ouvrir la liaison | liaison ouverte au cadrage | LIA-2026-066 ouverte, message de confirmation | 002, 003 | Conforme |
| 7 | Lancer une certification | chef DES | même institution et même type une seconde fois | refus du doublon | « Ouverture refusée : une liaison est déjà ouverte… » avec identifiant de corrélation | 006 | Conforme |
| 8 | Dossier, identification | chef DES | lecture | libellé du type de projet | code brut « AGREGATEUR » | 003 | Anomalie CER-05 |
| 9 | Décision avant PV | chef DES | lecture clair et sombre | circuit affiché tel que configuré, verrou 2 en attente | circuit GIMAC ordre 1, participant ordre 1, verrou 2 en attente | 004, 005 | Conforme |
| 10 | Mes actions | conducteur | lecture | opération à valider et à faire | A1 « V · Validateur », compteurs cohérents | 007 | Conforme |
| 11 | Tiroir A1 | conducteur | Valider l'opération | clé de sécurité, empreinte, opération close | dialogue de clé, validation signée, opération close | 008, 009 | Conforme |
| 12 | Décision avant constat | conducteur | lecture | pas de bouton Établir | aucun bouton, message d'attente du constat (rang 17) | 010 | Conforme |
| 13 | Décision, carte visée | conducteur | lien « Établir le procès-verbal » depuis Mes actions | onglet Décision, carte du PV signalée et amenée en vue | `?onglet=decision&operation=…:A4`, bandeau « Opération visée », carte encadrée | 011 | Conforme |
| 14 | Établir le PV | conducteur | bouton, confirmation | PV figé avec empreinte, état « Établi, à signer » | conforme, toast « effectué » | 012, 013 | Conforme |
| 15 | Dates de la pièce | conducteur | lecture | dates formatées | « Établi le 2026-09-29 » brut | 013 | Anomalie CER-06 |
| 16 | Statut de la liaison | conducteur | lecture | statut cohérent avec l'étape | « Cadrée » à l'étape Certification | 013 | Anomalie CER-11 |
| 17 | Gestes réservés | conducteur, RSSI, conformité, DG | ouverture de la Décision d'un PV établi | pas de « Signer pour le GIMAC » ni « Établir » | aucun bouton pour ces quatre profils | (journal) | Conforme |
| 18 | Mon travail | chef DES | lecture | compteur « Procès-verbaux à signer » | 2 à traiter | 014 | Conforme |
| 19 | Décision, carte visée | chef DES | lien « Signer le procès-verbal » depuis Mes actions | carte `#decision-pv` ciblée, approbation non ciblée, dans la vue | conforme en clair et en sombre | 015, 016 | Conforme |
| 20 | Signature GIMAC | chef DES | Signer, Annuler, puis Signer et clé | finalité SIGNATURE_OPERATION, empreinte, état inchangé après Annuler | dialogue de clé avec empreinte ; finalité SIGNATURE_OPERATION vérifiée dans le code (ProcesVerbal.tsx l.55) ; Annuler sans effet ; « Signé par le GIMAC » | 017, 018, 019 | Conforme |
| 21 | Refus CER_SIGNATAIRE_NON_DESIGNE, CER_SIGNATURE_ORDRE, CER_OPERATION_NON_OUVERTE | tous | tentative de signature hors tour | refus explicite avec le message traduit | inatteignable : l'interface masque le bouton et les services simulés ne rendent jamais ces codes ; messages présents dans i18n (d91-d94.ts l.82 à 86) | 070 | Anomalie CER-01 |
| 22 | Ordre du circuit du PV | tous | lecture du circuit | ordre configuré respecté | circuit simulé figé (deux étapes d'ordre 1), ordre non vérifiable | 004 | Anomalie CER-01 |
| 23 | Portail, tableau de bord | correspondant | lecture | ce que le GIMAC attend, liaisons de son institution seulement | 4 tâches dont « Signer le PV » LIA-2026-066, 5 liaisons de l'institution | 020 | Conforme |
| 24 | Portail, PV | correspondant | bouton Signer depuis le tableau de bord | page du PV, circuit, signature GIMAC apposée | conforme, aucune personne interne nommée (fonctions seulement) | 021 | Conforme |
| 25 | Portail, signature | correspondant | Signer, Annuler, Signer et clé | PV « Signé des deux parties », liaison Certifiée | conforme en clair et en sombre | 022, 023, 024 | Conforme |
| 26 | Portail, rang 19 avant le GIMAC | correspondant | signature de LIA-2026-027 en premier | acceptée (même ordre), « attend la signature du GIMAC » | conforme | 034, 035 | Conforme |
| 27 | Signature GIMAC après le participant | chef DES | Signer | conséquence « votre signature certifie la liaison », liaison Certifiée | conforme, étape Approbation de la sécurité | 036 | Conforme |
| 28 | PV signé | chef DES | lecture | deux signatures, verrou 2 ouvert | conforme | 025 | Conforme |
| 29 | Empreintes | tous | comparaison de pièces différentes | empreintes distinctes | empreintes identiques entre liaisons et entre pièces | 013, 036, 042, 049 | Anomalie CER-07 |
| 30 | Dossier de changement | production monétique | Constituer, pièce jointe, confirmation | dossier constitué, carte d'approbation visée | conforme | 026 | Conforme |
| 31 | Fiche de changement invalide | production monétique | « fiche 42 » | refus CER_CHANGE_RECORD_INVALID clair | message en texte gris dans la confirmation, sans style d'erreur | 026 | Anomalie CER-10 |
| 32 | Fiche de changement valide | production monétique | CHG-2026-420 | fiche référencée « En examen » | conforme | 028 | Conforme |
| 33 | Mes actions | RSSI | lecture | « Faire approuver le changement » | présent | 027 | Conforme |
| 34 | Relecture de la fiche | RSSI | Relire la fiche | approbation prononcée, jalon Approuvée par la sécurité | « Approuvée », bandeau « mise en production peut commencer » (clair et sombre) | 028, 029, 030 | Conforme |
| 35 | Attestations du modèle générique | RSSI | lecture | texte cohérent | bandeau « PCI DSS exigée » au-dessus de « Aucune attestation exigée » | 029 | Anomalie CER-09 |
| 36 | Bascule A8 et jalon | production, conducteur | dépôt, validation signée | EN_PRODUCTION, 8 opérations sur 8, quatre jalons atteints | conforme | 031, 032 | Conforme |
| 37 | Institution | conducteur | fiche INST-0042 | statut Actif | « Actif », historique au jalon EN_PRODUCTION | 033 | Conforme |
| 38 | Portail, attestations | correspondant | ouverture | attestation PCI DSS à fournir | conforme | 037 | Conforme |
| 39 | Portail, format | correspondant | dépôt d'un .docx | refus « doit être un fichier PDF » | refus en ligne, sans envoi | 038 | Conforme |
| 40 | Portail, dépôt | correspondant | PDF, confirmation | attestation fournie | conforme | 039, 040 | Conforme |
| 41 | Mon travail | RSSI | lecture | Attestations à vérifier, Approbations en attente | 2 et 2 | 041 | Conforme |
| 42 | Rejet sans motif | RSSI | Rejeter sans motif | bouton désactivé | bouton Rejeter désactivé | 043 | Conforme |
| 43 | Rejet motivé | RSSI | motif saisi | « Rejetée, à fournir de nouveau » avec motif | conforme | 044 | Conforme |
| 44 | Portail, rejet | correspondant | ouverture | motif visible, redépôt possible | « Attestation rejetée par la Sécurité des SI », motif, « Déposer une nouvelle attestation » | 045 | Conforme |
| 45 | Vérifier | RSSI | Déclarer conforme | attestation Conforme | conforme | 046, 047 | Conforme |
| 46 | Archiver | RSSI | Archiver les attestations | « Vérifiée et archivée », état Vérifié et archivé | conforme | 048, 049 | Conforme |
| 47 | Approbation LIA-2026-027 | RSSI | relecture de CHG-2026-427 | Approuvée par la sécurité | conforme | 050 | Conforme |
| 48 | Portail, attestations vérifiées | correspondant | lecture | vérifiée, vérifiée par fonction | « vérifiée… par la Sécurité des systèmes d'information » | 051 | Conforme |
| 49 | Visas, découverte | conformité, DG | tableau de bord, Mes actions | visa attendu trouvable et relié à la liaison | compteur « Visas attendus 1 » sans lien ; Mes actions vide ; « Ma tâche (0) » | 052, 053, 062 | Anomalie CER-02 |
| 50 | Visas, carte | conformité | Décision de LIA-2026-019 | deux visas attendus, boutons de son seul visa | boutons sur le visa de conformité seulement | 054, 058 | Conforme |
| 51 | Relecture avant visas | RSSI | Relire la fiche | pas d'approbation tant qu'un visa actif manque | « Les visas activés doivent être favorables… », fiche En examen | 057 | Conforme |
| 52 | Visa défavorable sans motif | conformité | clé de sécurité sans motif | refus | bouton de clé désactivé, motif obligatoire | 059 | Conforme |
| 53 | Visa favorable | conformité | motif, clé | visa signé, immuable | « Favorable, Signée par Responsable conformité » | 060, 061 | Conforme |
| 54 | Visa DG | DG | motif, clé | visa signé ; aucun bouton sur le visa de conformité | conforme, clair et sombre | 063, 064 | Conforme |
| 55 | Approbation après visas | RSSI | Relire la fiche | statut Approuvée après le visa du DG | « Approuvée », liaison Approuvée par la sécurité | 065 | Conforme |
| 56 | Rapport de mise en production | production, chef DES | rangs 28 à 31, dépôts et validations | rapport déposé puis validé, liaison En production | conforme (le validateur désigné est le conducteur 2 ; le chef du DES valide au titre de HOM_STEP_MANAGE) | 066 à 069 | Conforme |
| 57 | Circuit d'opération, ordre imposé | conducteur, SNA | ouverture du rang 11 de LIA-2026-036 | ordre 2 non signable avant l'ordre 1 | conducteur : pas de bouton, « À venir » ; après la signature SNA : bouton Signer ; opération close après les deux | 070 à 073 | Conforme |
| 58 | Signataire attendu | conducteur | lecture du circuit | conducteur de la liaison | « Conducteur de projet 1 » au lieu du conducteur 2 de la liaison | 070 | Anomalie CER-16 |
| 59 | Dates des signatures d'opération | conducteur | lecture | même horloge que le reste | signatures « 8 oct. 2026 », clôture « 29 sept. 2026 » | 073 | Anomalie CER-08 |
| 60 | Portail, liaisons | correspondant | Mes liaisons, suivi 066 et 027 | aucune donnée interne, interlocuteurs par fonction | aucune personne interne, aucune autre institution | 074 à 077 | Conforme |
| 61 | Portail, pièces du suivi | correspondant | lecture du suivi de 066 | pièces cohérentes avec le modèle | « Attestations : pas encore attendues », « Coordonnées : non communiquées » pour un modèle qui n'en exige pas | 075 | Anomalie CER-09 |
| 62 | Cloisonnement | correspondant | `/portail/liaisons/LIA-2026-019`, `-022`, `-005`, PV et attestations de 019, livrable de 022 | introuvable, sans fuite | « Ce parcours est introuvable. » partout, aucune donnée | 078, 079 | Conforme |
| 63 | Message d'introuvable | correspondant | lecture | message unique, sans action inutile | titre et corps identiques, bouton Réessayer | 078 | Anomalie CER-18 |
| 64 | Cloche | correspondant | ouverture | notifications de son institution, sans donnée interne | 2 notifications, aucune donnée interne | 080 | Conforme |
| 65 | Liste des notifications | correspondant | Voir tout, filtres | liste, filtres Non lues et Toutes | conforme ; sous-titre interne (« messages reçus, rapports soumis ») | 081 | Anomalie CER-13 |
| 66 | Lien de notification | correspondant | « Action attendue : Paramétrage d'essai » | écran du geste attendu | « Ce livrable ne vous est pas demandé » ; aucune notification du PV à signer ni des attestations | 082 | Anomalie CER-12 |
| 67 | Mon compte | correspondant | lecture, menus | fiche du correspondant | incohérences (nom, courriel du GIMAC, couche interne dans les appareils), préférences internes | 083 | Anomalies CER-13, CER-14 |
| 68 | Préférences | correspondant | bouton globe | langue et thème | « Préférences : Langue, Thème » | 085 | Conforme |
| 69 | Menu utilisateur | correspondant | ouverture | Mon compte, Se déconnecter | conforme | 084 | Conforme |
| 70 | Routes internes au portail | correspondant | `/des/tableau-de-bord`, `/des/parcours/…`, `/console/…`, `/portail/administration` sur 6101 | refus ou redirection | redirection vers Mon espace de certification | (journal) | Conforme |
| 71 | Connexion à l'application interne | correspondant | identifiant correspondant-a sur 6100 | refus, aiguillage vers le portail | « Ce compte est un compte d'institution : il s'ouvre dans le portail… » | 086 | Conforme |
| 72 | Connexion à la console | correspondant | idem sur 6102 | idem | conforme | 087 | Conforme |
| 73 | Routes internes avec la session du portail | correspondant | `/des/parcours/…`, `/des/certification/mes-actions`, `/console/tableau-de-bord` | connexion exigée | redirection vers `/connexion?retour=…` | 088 | Conforme |
| 74 | Connexion d'un interne au portail | chef DES | identifiant interne sur 6101 | aiguillage vers l'application interne | « Ce compte est un compte interne… » | 089 | Conforme |
| 75 | Accès direct refusés | conducteur, conformité, DG, chef DES | routes hors profil (voir §3) | « Accès refusé » sans donnée | « Accès refusé : votre profil ne donne pas accès à cet écran » | 090, 091, 092 | Conforme |
| 76 | Coordonnées d'essai | correspondant | valeurs invalides, puis valides | messages en ligne, puis enregistrement | conforme | 103, 104 | Conforme |
| 77 | Fiche de suivi des services | correspondant | envoi incomplet, puis complet | refus en ligne, puis fiche transmise | conforme | 105 à 108 | Conforme |
| 78 | Mobile 390 px, portail | correspondant | tableau de bord, PV, attestations | sans défilement horizontal | aucun défilement de page | 093, 094, 095 | Conforme |
| 79 | Mobile 390 px, Décision | chef DES | carte du PV | lisible | barres de couverture disparues, valeurs coupées | 096, 097 | Anomalie CER-04 |
| 80 | Mobile 390 px, Mes actions | chef DES | lecture | sans défilement de page | tableau défilant dans son conteneur | 098 | Conforme |
| 81 | Thème sombre, couverture | chef DES, correspondant | Décision, suivi | segments colorés selon le niveau | tous les segments gris en sombre | 016, 077 | Anomalie CER-03 |
| 82 | en-US | chef DES, correspondant | Décision, Mes actions, PV, tableau de bord | interface entièrement traduite | traduite, sauf étapes du circuit « Signature du GIMAC / du participant » et noms de signataires | 099 à 102 | Anomalie CER-15 |
| 83 | Textes | divers | lecture | accords corrects | « 1 services sur 1 », « Représentant de Établissement… », « Rémittence entrante est-il ouvert », « Ma tâche (0) » | 013, 024, 105, 052 | Anomalie CER-19 |
| 84 | Relecture après approbation | RSSI | lecture | geste clos | « Relire la fiche de changement » toujours proposé après « Approuvée » | 029, 065 | Anomalie CER-20 |
| 85 | Fiche d'institution | conducteur | lecture de INST-0042 | données cohérentes | prise de contact et enregistrement datés du jour, alors que LIA-2025-088 est en production depuis juillet ; BIC et code BEAC vides sans « non renseigné » | 033 | Anomalie CER-17 |
| 86 | Console du navigateur | tous | surveillance de tout le parcours | aucune erreur JavaScript ni réponse en erreur | aucune erreur relevée (journal du script) | | Conforme |

**Bilan** : 86 contrôles, 64 conformes, 22 contrôles en anomalie, soit 20 anomalies distinctes (0 bloquante, 2 majeures, 18 mineures).

## 3. Matrice des accès du lot

Entrées attendues : catalogue `ms-iam/src/main/resources/config/fonctionnalites-initiales.json` (champs `tous` et `unDe`) croisé avec les permissions de `infra/keycloak/generer-realm-gimac.py` (calcul refait par script). V : visible ou ouvert ; R : refusé proprement (« Accès refusé » ou redirection) ; « - » : non applicable.

| Entrée ou route | Chef DES | Conducteur | RSSI | Conformité | DG | Correspondant | Conforme |
|---|---|---|---|---|---|---|---|
| Lancer une certification `/des/certification/lancer` | V | R | R | R | R | R (portail) | oui |
| Mes actions de certification `/des/certification/mes-actions` | V | V | V | V | V | R (portail) | oui |
| Liaisons et parcours `/des/parcours` | V | V | V | V | V | R | oui |
| Mes projets `/des/parcours?mesProjets=1` (entrée) | absente | V | absente | absente | absente | - | oui |
| Mon équipe `/des/equipe` | V | R | V | V | V | - | oui |
| Référentiels d'opérations | V | V | V | V | V | - | oui |
| Dossier de liaison, onglet Décision | V | V | V | V | V | R (portail) | oui |
| Comptes `/des/administration/comptes` | R | R | V | R | R | - | oui |
| Supervision `/des/plateforme/supervision` | R | R | V | R | R | - | oui |
| Configuration `/des/plateforme/configuration` | R | R | V | R | R | - | oui |
| Console de la plateforme (lien) | absente | absente | V | absente | absente | refus à la connexion | oui |
| Portail : Tableau de bord, Mes liaisons | - | - | - | - | - | V | oui |
| Portail : liaison d'une autre institution | - | - | - | - | - | introuvable | oui |
| Portail : `/des/…`, `/console/…` sur 6101 | - | - | - | - | - | redirigé | oui |
| Connexion du correspondant sur 6100 et 6102 | - | - | - | - | - | refusée, aiguillée | oui |
| Connexion d'un interne sur 6101 | refusée, aiguillée | - | - | - | - | - | oui |

Gestes réservés vérifiés dans les écrans permis : Établir le PV (conducteur seul), Signer pour le GIMAC (chef du DES seul), Constituer le dossier et Référencer la fiche (production monétique seule), Déclarer conforme, Rejeter, Archiver, Relire (RSSI seule), visa de conformité (conformité seule), visa de la DG (DG seul), signature du participant (portail seul). Aucun bouton d'un autre profil n'est apparu.

## 4. Anomalies

### CER-01 (majeure) : refus CER_SIGNATAIRE_NON_DESIGNE, CER_SIGNATURE_ORDRE et CER_OPERATION_NON_OUVERTE et ordre du circuit du PV invérifiables en services simulés
- Étapes : dérouler la signature du PV (chef du DES, participant) et des opérations à signatures ; chercher à signer hors de son tour ou sans être le signataire désigné.
- Observé : l'interface masque le bouton hors tour (ce qui est correct), mais aucun chemin ne produit ces refus : les services simulés ne rendent jamais ces trois codes (ils rendent HOM_SIGNATURE_ORDRE, HOM_SIGNATURE_NON_SIGNATAIRE, HOM_SIGNATURE_ETAT, CER_PV_NOT_SIGNABLE), et le circuit du PV y est figé à deux étapes d'ordre 1 : l'ordre imposé du circuit du PV et l'affichage d'un circuit configuré autrement ne peuvent pas être recettés. Les messages français existent (d91-d94.ts l.82 à 86).
- Captures : 004, 070.
- Cause probable : `frontend/packages/lot1/src/services/simule/parcours.ts`, `circuitDe` (l.53) figé et `signerOperation` (l.719 à 731) avec des codes HOM_* ; le serveur rend les codes CER_* (`ms-certification/.../ProcesVerbalPolicy.java` l.66 à 80, `OperationsLiaison.java` l.186 et l.213).

### CER-02 (majeure) : visa attendu introuvable pour la conformité et la DG
- Étapes : se connecter en conformité (ou DG) alors que LIA-2026-019 attend son visa ; ouvrir le tableau de bord puis Mes actions de certification.
- Observé : compteur « Visas attendus 1 » non cliquable ; « Ma tâche (0) » ; Mes actions affiche 0 partout (« Aucune action ne vous attend »). L'acteur doit deviner la liaison pour viser.
- Captures : 052, 053, 062.
- Cause probable : `services/simule/parcours.ts`, compteurs `visasAttendus` sans lien (l.821 et 822) et `tachesDe` / `mesOperations` qui ignorent les visas.

### CER-03 (mineure) : barres de couverture toutes grises en thème sombre
- Étapes : thème sombre, onglet Décision ou Périmètre, ou suivi du portail.
- Observé : les quatre segments restent gris quel que soit le niveau (« certifié » indiscernable de « hors périmètre », hors légende texte).
- Captures : 016, 077.
- Cause : `frontend/packages/design-system/src/styles/13-affichages-derives.css` l.12 : `[data-theme="dark"] .gm-cov-seg` est plus spécifique que `.gm-cov-1` à `.gm-cov-4` (l.13 à 16).

### CER-04 (mineure) : carte du PV dégradée à 390 px
- Étapes : 390 px de large, onglet Décision de LIA-2026-019.
- Observé : la barre de couverture disparaît (colonne écrasée) ; les valeurs « par Chef du département DES », « par Représentant de Banque G » sont coupées au bord de la carte.
- Capture : 097.
- Cause probable : `13-affichages-derives.css` l.8 (`grid-template-columns: 200px 1fr 120px` sans repli mobile) et liste clé-valeur sans retour à la ligne dans la carte.

### CER-05 (mineure) : type de projet créé dans la plateforme affiché par son code
- Étapes : ouvrir LIA-2026-066 (Agrégateur de paiement) dans le dossier, au portail (tableau de bord, liaisons, suivi, PV).
- Observé : « AGREGATEUR » au lieu de « Agrégateur de paiement » (libellé pourtant affiché à l'écran de lancement).
- Captures : 003, 020, 021.
- Cause probable : `t('parcours.typeProjet.<code>', { defaultValue: code })` sans recours au libellé du référentiel : `pages/des/parcours/DossierLiaisonPage.tsx` l.67, `pages/portail/parcours/SuiviProjet.tsx` l.226, `cadre.tsx` l.26, `listes.tsx` l.89.

### CER-06 (mineure) : dates ISO brutes dans l'onglet Décision
- Observé : « Établi le 2026-09-29 », « Signée par …, le 2026-09-29 », « Constitué par …, le 2026-09-29 », « Lue le 2026-09-29 », « Déposée le 2026-08-29 », y compris en en-US ; le portail formate (« 29 septembre 2026 »).
- Captures : 013, 019, 029, 061, 099.
- Cause probable : `pages/des/parcours/decision/ProcesVerbal.tsx` l.101 (`pv.etabliLe` affiché tel quel), `EtatSignature` de `decision/commun.tsx`, cartes Approbation et Attestations.

### CER-07 (mineure) : empreintes identiques pour des pièces différentes
- Observé : PV de LIA-2026-066 et de LIA-2026-027 : `f50c43ef…0798f309` ; dossiers de changement de 066 et 027 : `e10c2473…3970616d` ; attestation rejetée et attestation redéposée : `e40c292c…935d2117`.
- Captures : 013, 036, 042, 049.
- Cause : `hex()` de `services/simule/parcours.ts` l.31 (et `donnees.ts` l.45) ne mélange que les 8 premiers caractères de la graine.

### CER-08 (mineure) : deux horloges dans la démonstration
- Observé : date simulée au 29 sept. 2026 (établissements, validations, « depuis le 20 sept. 2026 (9 jours) ») mais signatures d'opération datées du 8 oct. 2026 et « Ma journée : jeudi 8 octobre 2026 ».
- Captures : 001, 073.
- Cause probable : `jour()` et `AUJOURDHUI` fixes contre `new Date()` dans `signerOperation` (`services/simule/parcours.ts` l.730).

### CER-09 (mineure) : attestations et pièces incohérentes pour le modèle générique
- Observé : bandeau « Activité carte : attestation PCI DSS exigée » au-dessus d'un tableau « Aucune attestation exigée » ; au portail, « Attestations de sécurité : pas encore attendues » et « Coordonnées techniques d'essai : Non communiquées » pour une liaison en production qui n'en exige aucune.
- Captures : 029, 075.

### CER-10 (mineure) : refus de la référence de fiche de changement peu visible
- Étapes : production monétique, Référencer la fiche, saisir « fiche 42 », confirmer.
- Observé : le message CER_CHANGE_RECORD_INVALID s'affiche en texte gris ordinaire en tête de la confirmation, sans alerte d'erreur ; la saisie, mise en capitales, n'est pas contrôlée avant la confirmation.
- Capture : 026.

### CER-11 (mineure) : statut « Cadrée » pendant toute la certification du modèle générique
- Observé : LIA-2026-066 reste « Cadrée » aux étapes Tests et Certification, jusqu'à la signature du PV (interne et portail).
- Captures : 013, 015, 021.

### CER-12 (mineure) : notifications du portail figées et lien sans geste
- Observé : la cloche ne contient que deux notifications de démonstration ; « Action attendue : Paramétrage d'essai » mène à « Ce livrable ne vous est pas demandé » ; aucune notification n'est émise pour le PV à signer, l'attestation rejetée ou la fiche de suivi.
- Captures : 080, 081, 082.
- Cause probable : `frontend/packages/lot1/src/services/notifications.ts` l.115 (`notificationsPortailDemo` statique).

### CER-13 (mineure) : textes et préférences internes au portail
- Observé : Mon compte du correspondant propose les notifications « Nouveaux messages de la messagerie », « Rapports qui me sont soumis », « Mentions », « Demandes à l'administration », « Processus d'unité » ; le texte de la photo parle de la messagerie ; la page Notifications est sous-titrée « Messages reçus, mentions et réponses, tâches confiées et rapports soumis ». Le correspondant n'a pas ces fonctions.
- Captures : 081, 083.

### CER-14 (mineure) : identité du correspondant incohérente
- Observé : en-tête « Correspondant A » (initiales CA) mais fiche « Brice Nzeng Mba » (BN) ; courriel `a.ngombarga@gimac-afr.org` (domaine du GIMAC) ; fonction « Ingénieure d'exploitation » ; un appareil connecté sur la couche « Application du département », inaccessible à ce profil.
- Capture : 083.
- Cause probable : `frontend/apps/portail/src/App.tsx` (`monCompteSimule`) et `connexionDemo.ts` (nom « Correspondant A »), données du compte simulé.

### CER-15 (mineure) : circuit de signature non traduit en en-US
- Observé : étapes « Signature du GIMAC » et « Signature du participant », signataires « Chef du département DES », « Représentant de … » restent en français.
- Captures : 099, 101.
- Cause probable : libellés portés par les données simulées (`circuitDe`, `signe()` de `services/simule/parcours.ts`).

### CER-16 (mineure) : signataire attendu erroné sur le circuit d'opération
- Observé : sur LIA-2026-036, VISA_CONDUITE attend « Conducteur de projet 1 » alors que la liaison est conduite par « Conducteur de projet 2 ».
- Captures : 070, 073.
- Cause probable : `services/simule/parcours.ts` l.517, `personne(x.signataire)` prend toujours la première personne de la fonction.

### CER-17 (mineure) : fiche de l'institution INST-0042 incohérente
- Observé : statut « En homologation » avant la bascule alors que LIA-2025-088 est en production depuis juillet ; prise de contact et enregistrement datés du jour (format jj/mm/aaaa différent du reste) ; BIC et code BEAC vides sans « non renseigné ». Hors lot strict, signalé pour la DDRM.
- Capture : 033.

### CER-18 (mineure) : message d'introuvable doublé au portail
- Observé : « Ce parcours est introuvable. » en titre et en corps, avec un bouton « Réessayer » inutile pour un refus définitif.
- Captures : 078, 079.

### CER-19 (mineure) : accords et formulations
- Observé : « 1 services sur 1 », « Représentant de Établissement de paiement A », « Rémittence entrante est-il ouvert en production ? », « Ma tâche (0) ».
- Captures : 013, 024, 105, 052.

### CER-20 (mineure) : relecture de la fiche toujours proposée après l'approbation
- Observé : le bouton « Relire la fiche de changement » reste affiché quand l'approbation est « Approuvée ».
- Captures : 029, 065.
- Cause probable : `services/simule/parcours.ts` l.924 (`RELIRE_FICHE` ajouté dès qu'une fiche existe).

## 5. Correction

Corrections faites le 8 octobre 2026. Chaque correctif est vérifié sur une reconstruction simulée de l'application interne et du portail (port isolé 6400 et 6401) : capture « après » dans `docs/recette/captures/certification/corrections/<ID>-apres.png`, registre dans `docs/recette/corrections/certification.json`. Le simulateur suit désormais les règles de MS-CERTIFICATION et de MS-HOMOLOGATION ; quand le défaut existait aussi côté serveur, le serveur est corrigé et testé.

| Anomalie | Correctif | Fichiers principaux | Test |
|---|---|---|---|
| CER-01 (majeure) | Le simulateur construit le circuit du PV comme `CircuitsSignature.procesVerbal` : signatures configurées de l'opération ouverte qui porte la signature (déclencheur `certification.pv-signed.v1`), complétées de la signature du participant au portail, circuit par défaut sinon, figé à la première signature. Signature dans l'ordre (`ProcesVerbalPolicy.signables`), refus 409 `CER_SIGNATURE_ORDRE` (avec `details.attendues`), 403 `CER_SIGNATAIRE_NON_DESIGNE`, 409 `CER_OPERATION_NON_OUVERTE` et `CER_OPERATION_ABSENTE`, aussi pour les visas ; l'action « Signer » n'est proposée qu'à qui peut signer maintenant. Démonstration d'un circuit à ordre imposé sur LIA-2026-025 (participant ordre 1, GIMAC ordre 2). Le serveur rendait déjà ces codes. | `frontend/packages/lot1/src/services/simule/parcours.ts` | `certification-recette.test.ts` (CER-01, quatre cas) |
| CER-02 (majeure) | Compteur « Visas attendus » relié à l'onglet Décision de la liaison (`lienUnique DOSSIER:decision`) ; tâche « Signer : Visa de la conformité / de la Direction Générale » et entrée de « Mes actions de certification » (signataire) sur la première opération ouverte de l'approbation de la sécurité, pour la fonction du visa (MS-HOMOLOGATION `TableauDeBordCalculateur.partVisa`, simulateur `tachesVisa`). Le visa n'est proposé que si cette opération est ouverte (MS-CERTIFICATION `ApprobationSecuriteService.actionsDes`), sinon sa signature serait refusée. Aucune tâche n'est créée dans MS-PILOTAGE : le tableau d'acteur et « Mes actions » lisent MS-HOMOLOGATION. | `ms-homologation/.../TableauDeBordCalculateur.java`, `tableau-acteur-compteurs.json`, `ms-certification/.../ApprobationSecuriteService.java`, `parcours.ts` | `TableauDeBordCalculateurTest.visasAttendus`, `GestesParOperationTest.visaSurOperationOuverte`, `certification-recette.test.ts` (CER-02), `OngletDecision.test.tsx` (CER-02) |
| CER-03 | Couleur de niveau des segments plus spécifique que le fond neutre du thème sombre. | `design-system/src/styles/13-affichages-derives.css` | capture |
| CER-04 | En dessous de 600 px, barre de couverture et liste clé-valeur sur une colonne, valeurs renvoyées à la ligne. | `13-affichages-derives.css`, `06-affichage.css` | capture |
| CER-05 | Application interne : libellé du type lu au catalogue du modélisateur quand la liste des types n'est pas lisible. Portail : MS-HOMOLOGATION sert le libellé du type avec la liaison (`typeProjetLibelle` de `Vues.Dossier`, `Vues.LiaisonResume` et `LiaisonVue`, lu de la définition du type), repris par le simulateur ; tableau de bord, liaisons, suivi, écrans d'action et procès-verbal du portail nomment le type par son libellé (« Agrégateur de paiement »), le code seulement à défaut. | `ms-homologation` (`Liaison.java`, `Vues.java`, `ParcoursProjection.java`, `LiaisonsCadrageResource.java`), `lot1` (`modele/parcours.ts`, `services/http/parcours.ts`, `services/simule/parcours.ts`, `pages/portail/parcours/commun.tsx`, `listes.tsx`, `cadre.tsx`, `SuiviProjet.tsx`, `DossierLiaisonPage.tsx`, `hooks/referentiels.ts`) | `ParcoursProjectionTest.libelleTypeProjet`, `certification-recette.test.ts` (CER-05), [capture](captures/certification/corrections/CER-05-apres.png) |
| CER-06 | Toutes les dates de l'onglet Décision et de Mise en service au format local (`useFormatDate`). Les vues du serveur restent en ISO, le format est celui de l'interface. | `decision/ProcesVerbal.tsx`, `commun.tsx`, `FicheChangement.tsx`, `Attestations.tsx`, `Approbation.tsx`, `FicheSuivi.tsx`, `Exploitation.tsx` | capture |
| CER-07 | Empreinte de démonstration calculée sur toute la graine (une seule fonction pour les deux simulateurs). | `services/simule/donnees.ts`, `parcours.ts` | `certification-recette.test.ts` (CER-07) |
| CER-08 | Signatures, visas et renvois horodatés sur l'horloge de la démonstration ; le jour de référence est désormais le jour courant de Douala (correctif DDR-15 du lot DDRM), comme « Ma journée ». | `parcours.ts` | `certification-recette.test.ts` (CER-08 et CER-16) |
| CER-09 | Bandeau d'exigence d'attestation lu sur les attestations servies (aucun pour le modèle générique) ; au portail, « aucune exigée pour cette liaison » et coordonnées « Sans objet » une fois l'intégration passée sans coordonnées. Serveur déjà correct (`ApprobationPolicy.attestationsExigees`). | `decision/Attestations.tsx`, `portail/parcours/SuiviProjet.tsx`, `i18n/ecrans/parcours-portail.ts` | capture |
| CER-10 | Référence contrôlée à la saisie avec le motif du serveur (bouton désactivé, message sous le champ) ; tout refus du service s'affiche en alerte d'erreur dans la confirmation (`ExecuteurActions`). | `decision/FicheChangement.tsx`, `ressources/actions.tsx` | `OngletDecision.test.tsx`, `parcours-securite.spec.ts` (CER-10) |
| CER-11 | Avant tout jalon, statut déduit de l'étape courante (`Parcours.statutCalcule` de MS-HOMOLOGATION) : En intégration, En test (essais et certification). | `parcours.ts` | `certification-recette.test.ts` (CER-11) |
| CER-12 | Notifications du portail simulé tirées des tâches de l'institution (procès-verbal à signer, attestations, coordonnées, livrables, fiche de suivi), lien du geste au portail, relues régulièrement. MS-NOTIFICATION les émettait déjà (`ReglesHomologation.lienPortailOperation`). | `services/notifications.ts`, `apps/portail/src/App.tsx` | `notifications.test.ts` (CER-12) |
| CER-13 | Au portail, seul le type « Homologation » est servi et réglable (MS-NOTIFICATION : `TypeNotification.PORTAIL`, 400 sur un type interne ; simulateur et Mon compte) ; sous-titre de la page Notifications et texte de la photo propres au portail. | `ms-notification/.../TypeNotification.java`, `PreferencesNotificationsResource.java`, `Vues.java`, `socle/src/notifications/*`, `compte/src/pages/monCompte/Notifications.tsx`, `Photo.tsx` | `BoiteNotificationsIT.preferencesPortail`, `notifications.test.ts` (CER-13) |
| CER-14 | Même nom (Brice Nzeng Mba) dans l'en-tête et la fiche, adresse et fonction de l'institution, aucune session sur une couche interne pour un compte d'institution. | `apps/portail/src/connexionDemo.ts`, `App.tsx`, `compte/src/services/simule.ts`, `e2e/specs/portail-parcours.spec.ts` | capture |
| CER-15 | Étapes du circuit par défaut traduites par leur code (GIMAC, PARTICIPANT) en fr, en et es ; le libellé configuré reste affiché pour les autres. Les noms des signataires sont des données (personnes), non traduits. | `i18n/ecrans/d91-d94.ts`, `decision/ProcesVerbal.tsx`, `portail/parcours/ProcesVerbalPage.tsx` | capture |
| CER-16 | Signataire CONDUCTEUR attendu et enregistré : le conducteur désigné de la liaison (comme `Parcours.titulaire`). | `parcours.ts` | `certification-recette.test.ts` (CER-08 et CER-16) |
| CER-17 | Prise de contact et enregistrement des institutions du jeu datés avant l'adhésion, historique chronologique, valeur absente « non renseigné » quel que soit son type d'affichage. Non modifié : le statut « En homologation » (l'institution a des liaisons en homologation et seule une institution adhérente ou en homologation peut ouvrir une liaison, D-57 ; décision du lot DDRM) et le format jj/mm/aaaa des fiches génériques (convention du système de conception). | `services/simule/participant.ts`, `design-system/src/crud/outils.tsx` | `finition.test.tsx` (CER-17) |
| CER-18 | Un refus définitif (404, 403) ne propose plus « Réessayer » ; un message identique au titre n'est plus répété. | `socle/src/requete/QueryBoundary.tsx` | capture |
| CER-19 | Accord du nombre de services (« 1 service sur 1 »), « Mes tâches (0) », question « Le service « … » est-il ouvert en production ? », signataire du participant nommé « Correspondant … ». | `i18n/ecrans/parcours-decision.ts`, `parcours-portail.ts`, `parcours-cadrage.ts`, `parcours.ts` | capture |
| CER-20 | Aucun geste de l'approbation proposé une fois close (approuvée ou refusée), dans le simulateur comme dans `ApprobationPolicy.actionsDes` (serveur déjà correct). | `parcours.ts` | `certification-recette.test.ts` (CER-20), `decision.test.ts` |

Validation : `./mvnw -o verify` réussi pour MS-HOMOLOGATION, MS-CERTIFICATION et MS-NOTIFICATION (un dépassement de délai d'`EvenementsHomologationKafkaIT` sous charge, repassé seul avec `BoiteNotificationsIT`) ; compilation TypeScript de design-system, socle, compte, lot1 et du portail ; tests unitaires de design-system (545), compte (44), socle (172, deux fichiers repassés seuls après des dépassements de délai sous charge) et des fichiers de lot1 touchés ; `node infra/ci/contrat/routes.mjs` sans écart nouveau ; specs E2E `portail-parcours`, `parcours-securite`, `mon-compte` et `certification-bout-en-bout` sur le port 6400 : 16 réussies, un échec étranger à ce lot (colonne « Unité » des tâches à prendre modifiée par DDR-16). Les autres échecs de lot1 relevés pendant la correction proviennent des lots menés en parallèle (processus, institutions, référentiels).
