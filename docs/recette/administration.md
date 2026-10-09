# Recette du lot « administration » (ADM)

Plateforme GIMAC « Moabi », construction en services simulés : application interne http://localhost:6100, portail http://localhost:6101, console http://localhost:6102. Navigateur Chromium piloté pas à pas (Playwright, contexte `fr-FR`, un contexte par profil), chaque geste suivi d’une capture relue à l’écran. Recette du 08/10/2026 : **155 contrôles, 146 conformes, 9 en anomalie**, 181 captures.

## 1. Périmètre

**Profils** : super administrateur (ROLE_SUPER_ADMIN, compte `super.admin`), RSSI (ROLE_RSSI, compte `rssi`), administrateur système (ROLE_ADMIN_SYSTEME, compte `admin.systeme`).

**Applications** : console d’administration (6102) et partie administration et plateforme de l’application interne (6100, `/des/administration/*`, `/des/plateforme/*`) ; portail (6101) pour l’aiguillage des comptes internes.

**Écrans et flux couverts** :

- connexion réelle (identifiant, mot de passe, code à six chiffres) des trois profils à la console ; refus et aiguillage d’un compte interne sur le portail ;
- tableaux de bord du super administrateur, du RSSI et de l’exploitation (indicateurs cliquables, liens, état temps réel) ;
- comptes : recherche, filtres, création (saisie vide, mot de passe provisoire, invitation par courriel), fiche, rattachement à une unité et à un n+1, modification de la fiche avec abandon, attribution et retrait de profils (dont un profil privilégié) avec motif et clé de sécurité, actions exigées, réinitialisation du second facteur, mot de passe provisoire, fermeture des sessions, désactivation, activation, journal du compte, suppression irréversible signée, propre compte protégé ;
- refus du dernier super administrateur (désactivation, retrait du profil, suppression) ;
- profils et permissions : liste, composition en lecture, libellé et description, réglage « profil externe » (refus quand le profil a des titulaires), recomposition par fonctionnalité (D-90) avec aperçu de la barre latérale, comparaison, nouveau profil (contrôles, alerte « profil privilégié »), suppression ;
- catalogue des fonctionnalités (D-94) : saisie invalide, renommage, masquage et effet immédiat sur la barre latérale et sur l’accès direct, rétablissement ;
- matrice des pouvoirs : portées Aucune, Directs, Équipe, Unités et « Toutes les unités » réservée aux processus, profils demandables (jamais privilégiés), enregistrement signé ;
- organisation et organigramme : unité modifiée (responsable), unité créée (contrôles), déplacement par glisser-déposer, changement de n+1, panneau des pouvoirs, sous-unité créée, renommée et désactivée, chef désigné au clavier (vue Liste) ;
- demandes à l’administration : acceptation signée (compte créé), refus motivé, filtre ;
- politique de sécurité (mots de passe, OTP, sessions, force brute) : refus local, refus du service champ par champ, application signée ;
- courriels de la plateforme : port invalide, enregistrement, courriel de test ;
- journal d’administration, configuration, journalisation des services (niveau changé ; lecture seule pour RSSI et admin système), centre de contrôle ;
- infrastructure Kubernetes : courbes, pods, événements, journaux, redémarrage, mise à l’échelle (bornes), arrêt et démarrage, retour à la version précédente, nœuds et tâches ; lecture seule du RSSI ;
- messagerie et notifications de la console, Mon compte (fiche, clé FIDO2, mot de passe faible puis conforme, TOTP, sessions) ;
- droits : barre latérale et accès direct de chaque route de la console et de l’administration de l’application interne pour les trois profils, écrans d’autres lots refusés ;
- thème sombre (console et application interne), largeur 390 px, interface en anglais (en-US).

**Limite** : la construction en services simulés sert une infrastructure Kubernetes ; l’exploitation Docker (gestes Docker, refus « non pris en charge ») n’est servie que par la passerelle et n’a pas pu être recettée ici (elle est couverte par e2e/specs/infrastructure.spec.ts sur la passerelle simulée).

## 2. Tableau des contrôles

| N° | Écran ou flux | Profil | Geste | Résultat attendu | Résultat observé | Capture | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | Barre latérale console | Super administrateur | Ouvrir le tableau de bord et lister les entrées | 18 entrées du catalogue (consoleDepartement, consoleTableauDeBord, consoleMessagerie, consoleNotifications, consoleMonActivite, consoleComptes, consoleDemandes, consoleProfils, consoleMatrice, consoleOrganisation, consoleOrganigramme, consoleJournal, consolePolitique, consoleSupervision, consoleInfrastructure, consoleConfiguration, consoleJournalisation, consoleCourriels) | 20 liens : Retour au département ; Tableau de bord ; Messagerie3 ; Notifications ; Mon activité ; Comptes ; Demandes à traiter ; Profils et permissions ; Matrice des pouvoirs ; Organisation ; Organigramme ; Journal d’administration ; Politique de sécurité ; Centre de contrôle ; Infrastructure ; Configuration ; Journalisation ; Courriels de la plateforme ; Mon compte ; Se déconnecter | [001](captures/administration/001-superadmin-console-barre-laterale-clair.png) | Conforme |
| 2 | Accès directs console | Super administrateur | Ouvrir par l’adresse les 16 routes d’administration et de plateforme | routes permises ouvertes, autres refusées sans fuite | 16 ouvertes, 0 refusées (aucune) |  | Conforme |
| 3 | Barre latérale des | Super administrateur | Ouvrir le tableau de bord et lister les entrées | 28 entrées du catalogue (tableauDeBord, messagerie, notifications, maJournee, agenda, taches, rapports, processus, lancerDemande, mesDemandes, institutions, mesActionsCertification, liaisons, referentiels, adminComptes, adminDemandes, adminProfils, adminMatrice, adminOrganisation, adminOrganigramme, adminJournal, adminPolitique, supervision, infrastructure, configuration, journalisation, courriels, adminConsole) | 30 liens : Tableau de bord ; Messagerie3 ; Notifications ; Ma journée ; Agenda ; Tâches ; Rapports ; Processus de l’unité ; Lancer une demande ; Mes demandes ; Institutions membres ; Mes actions de certification ; Liaisons et parcours ; Référentiels d'opérations ; Comptes utilisateurs ; Demandes à traiter ; Profils et permissions ; Matrice des pouvoirs ; Organisation ; Organigramme ; Journal d’administration ; Politique de sécurité ; Supervision ; Infrastructure ; Configuration ; Journalisation ; Courriels de la plateforme ; Console de la plateforme ; Mon compte ; Se déconnecter | [002](captures/administration/002-superadmin-des-barre-laterale-clair.png) | Conforme |
| 4 | Accès directs des | Super administrateur | Ouvrir par l’adresse les 13 routes d’administration et de plateforme | routes permises ouvertes, autres refusées sans fuite | 13 ouvertes, 0 refusées (aucune) |  | Conforme |
| 5 | Barre latérale console | RSSI | Ouvrir le tableau de bord et lister les entrées | 18 entrées du catalogue (consoleDepartement, consoleTableauDeBord, consoleMessagerie, consoleNotifications, consoleMonActivite, consoleComptes, consoleDemandes, consoleProfils, consoleMatrice, consoleOrganisation, consoleOrganigramme, consoleJournal, consolePolitique, consoleSupervision, consoleInfrastructure, consoleConfiguration, consoleJournalisation, consoleCourriels) | 20 liens : Retour au département ; Tableau de bord ; Messagerie3 ; Notifications ; Mon activité ; Comptes ; Demandes à traiter ; Profils et permissions ; Matrice des pouvoirs ; Organisation ; Organigramme ; Journal d’administration ; Politique de sécurité ; Centre de contrôle ; Infrastructure ; Configuration ; Journalisation ; Courriels de la plateforme ; Mon compte ; Se déconnecter | [003](captures/administration/003-rssi-console-barre-laterale-clair.png) | Conforme |
| 6 | Accès directs console | RSSI | Ouvrir par l’adresse les 16 routes d’administration et de plateforme | routes permises ouvertes, autres refusées sans fuite | 16 ouvertes, 0 refusées (aucune) |  | Conforme |
| 7 | Barre latérale des | RSSI | Ouvrir le tableau de bord et lister les entrées | 29 entrées du catalogue (tableauDeBord, messagerie, notifications, maJournee, agenda, taches, rapports, equipe, processus, lancerDemande, mesDemandes, institutions, mesActionsCertification, liaisons, referentiels, adminComptes, adminDemandes, adminProfils, adminMatrice, adminOrganisation, adminOrganigramme, adminJournal, adminPolitique, supervision, infrastructure, configuration, journalisation, courriels, adminConsole) | 31 liens : Tableau de bord ; Messagerie3 ; Notifications ; Ma journée ; Agenda ; Tâches ; Rapports ; Mon équipe ; Processus de l’unité ; Lancer une demande ; Mes demandes ; Institutions membres ; Mes actions de certification ; Liaisons et parcours ; Référentiels d'opérations ; Comptes utilisateurs ; Demandes à traiter ; Profils et permissions ; Matrice des pouvoirs ; Organisation ; Organigramme ; Journal d’administration ; Politique de sécurité ; Supervision ; Infrastructure ; Configuration ; Journalisation ; Courriels de la plateforme ; Console de la plateforme ; Mon compte ; Se déconnecter | [004](captures/administration/004-rssi-des-barre-laterale-clair.png) | Conforme |
| 8 | Accès directs des | RSSI | Ouvrir par l’adresse les 13 routes d’administration et de plateforme | routes permises ouvertes, autres refusées sans fuite | 13 ouvertes, 0 refusées (aucune) |  | Conforme |
| 9 | Barre latérale console | Admin système | Ouvrir le tableau de bord et lister les entrées | 9 entrées du catalogue (consoleDepartement, consoleTableauDeBord, consoleMessagerie, consoleNotifications, consoleMonActivite, consoleSupervision, consoleInfrastructure, consoleConfiguration, consoleJournalisation) | 11 liens : Retour au département ; Tableau de bord ; Messagerie3 ; Notifications ; Mon activité ; Centre de contrôle ; Infrastructure ; Configuration ; Journalisation ; Mon compte ; Se déconnecter | [005](captures/administration/005-adminsys-console-barre-laterale-clair.png) | Conforme |
| 10 | Accès directs console | Admin système | Ouvrir par l’adresse les 16 routes d’administration et de plateforme | routes permises ouvertes, autres refusées sans fuite | 7 ouvertes, 9 refusées (consoleComptes, consoleDemandes, consoleProfils, consoleMatrice, consoleOrganisation, consoleOrganigramme, consoleJournal, consolePolitique, consoleCourriels) |  | Conforme |
| 11 | Barre latérale des | Admin système | Ouvrir le tableau de bord et lister les entrées | 15 entrées du catalogue (tableauDeBord, messagerie, notifications, maJournee, agenda, taches, rapports, processus, lancerDemande, mesDemandes, supervision, infrastructure, configuration, journalisation, adminConsole) | 17 liens : Tableau de bord ; Messagerie3 ; Notifications ; Ma journée ; Agenda ; Tâches ; Rapports ; Processus de l’unité ; Lancer une demande ; Mes demandes ; Supervision ; Infrastructure ; Configuration ; Journalisation ; Console de la plateforme ; Mon compte ; Se déconnecter | [015](captures/administration/015-adminsys-des-barre-laterale-clair.png) | Conforme |
| 12 | Accès directs des | Admin système | Ouvrir par l’adresse les 13 routes d’administration et de plateforme | routes permises ouvertes, autres refusées sans fuite | 4 ouvertes, 9 refusées (adminComptes, adminDemandes, adminProfils, adminMatrice, adminOrganisation, adminOrganigramme, adminJournal, adminPolitique, courriels) |  | Conforme |
| 13 | Tableau de bord super administrateur | Super administrateur | Ouvrir la console | Titre, gestion des utilisateurs, indicateurs, état temps réel | Titre « Tableau de bord du super administrateur », région Gestion des utilisateurs et État en temps réel présentes | [025](captures/administration/025-superadmin-console-tableau-de-bord-plein-clair.png) | Conforme |
| 14 | Tableau de bord super administrateur | Super administrateur | Cliquer l’indicateur « Sans second facteur » | Liste des comptes filtrée SANS_2FA | URL /console/comptes?etat=SANS_2FA, filtre État = SANS_2FA, 2 compte(s) | [026](captures/administration/026-superadmin-comptes-filtre-sans2fa-clair.png) | Conforme |
| 15 | Comptes | Super administrateur | Rechercher « ndong », filtrer par profil RSSI | Filtrage effectif ; aucun résultat signalé | c.ndong trouvé : true ; filtre RSSI : 1 compte(s) | [027](captures/administration/027-superadmin-comptes-filtre-profil-clair.png) | Conforme |
| 16 | Comptes > Créer un compte | Super administrateur | Envoyer le formulaire vide | Champs obligatoires signalés, aucun envoi | Messages : Champ obligatoire. / Champ obligatoire. / Champ obligatoire. / Champ obligatoire. / Choisissez au moins un profil. | [028](captures/administration/028-superadmin-creation-compte-vide-clair.png) | Conforme |
| 17 | Comptes > Créer un compte | Super administrateur | Saisir identité, profil Normes et Agréments, « Les deux », créer | Compte créé, mot de passe provisoire affiché une fois, copie | Confirmation : true ; secret affiché : oui (16 caractères) | [030](captures/administration/030-superadmin-creation-compte-secret-clair.png) | Conforme |
| 18 | Fiche du compte créé | Super administrateur | Ouvrir la fiche du compte | Fiche r.essomba avec onglets Fiche, Sécurité, Sessions, Journal | Titre « Roger Essomba » | [031](captures/administration/031-superadmin-fiche-compte-cree-clair.png) | Conforme |
| 19 | Fiche du compte > Rattachement | Super administrateur | Rattacher à l’unité SNA et au n+1 Rachel Biyong Mengue | Rattachement enregistré et affiché | Modale : unité (unités actives seulement) et responsable cherché par nom ; après enregistrement : « Le rattachement du compte a été enregistré », Unité Service Normes et Agréments (DES), Responsable Rachel Biyong Mengue (r.biyong) | [033](captures/administration/033-superadmin-rattachement-enregistre-clair.png) | Conforme |
| 20 | Fiche du compte > Profils | Super administrateur | Ajouter Conformité, retirer Normes et Agréments, signer (motif et clé) | Profils modifiés après signature | Message : true ; en-tête : RECompter.essombaRoger Essombaactifpremier accès en attentesans second facteurConformité | [035](captures/administration/035-superadmin-profils-modifies-clair.png) | Conforme |
| 21 | Fiche du compte > Profils | Super administrateur | Octroyer le profil privilégié Administrateur système (signature) | Octroi permis au super administrateur | Case proposée : true ; avertissement : «  » ; en-tête : RECompter.essombaRoger Essombaactifpremier accès en attentesans second facteurAdministrateur systèmeConformité | [037](captures/administration/037-superadmin-profil-privilegie-octroye-clair.png) | Conforme |
| 22 | Fiche du compte > Profils | Super administrateur | Retirer le profil Administrateur système (signature) | Retrait enregistré | En-tête : RECompter.essombaRoger Essombaactifpremier accès en attentesans second facteurConformité |  | Conforme |
| 23 | Fiche du compte > Sécurité | Super administrateur | Exiger des actions (vérifier le courriel) avec motif | Actions exigées | Message « Les actions ont été exigées. » true |  | Conforme |
| 24 | Fiche du compte > Sécurité | Super administrateur | Réinitialiser le second facteur, motif vide puis motif | Motif exigé, puis réinitialisation | Sans motif, le bouton de confirmation reste désactivé (motif obligatoire) ; le flux complet avec motif est vérifié ci-dessous | [039](captures/administration/039-superadmin-securite-2fa-motif-obligatoire-clair.png) | Conforme |
| 25 | Fiche du compte > Sécurité | Super administrateur | Mot de passe provisoire avec motif | Secret affiché une seule fois | Secret de 16 caractères affiché | [040](captures/administration/040-superadmin-securite-mdp-provisoire-clair.png) | Conforme |
| 26 | Fiche du compte > Sessions | Super administrateur | Fermer les sessions avec motif | Sessions fermées | Message : true | [041](captures/administration/041-superadmin-sessions-fermees-clair.png) | Conforme |
| 27 | Fiche du compte | Super administrateur | Modifier la fiche (fonction), annuler avec abandon puis enregistrer | Abandon confirmé ; fiche enregistrée | Abandon demandé : true ; enregistrement : true |  | Conforme |
| 28 | Fiche du compte | Super administrateur | Désactiver puis activer le compte (motifs) | Compte désactivé puis activé | Désactivé : true ; activé : true | [043](captures/administration/043-superadmin-compte-desactive-clair.png) | Conforme |
| 29 | Fiche du compte > Journal | Super administrateur | Ouvrir l’onglet Journal | Actes du compte listés (création, profils, sécurité, statut) | 10 lignes de journal | [044](captures/administration/044-superadmin-fiche-journal-clair.png) | Conforme |
| 30 | Fiche du compte | Super administrateur | Supprimer le compte : confirmation irréversible puis signature | Compte supprimé, retour à la liste | Bouton « Continuer vers la signature » désactivé avant la case ; après signature, retour à la liste « Comptes » : 13 comptes (14 après création), r.essomba absent | [046](captures/administration/046-superadmin-compte-supprime-clair.png) | Conforme |
| 31 | Comptes > Créer un compte | Super administrateur | Inviter : créer un compte avec « Envoyer l’invitation par courriel » | Compte créé, invitation envoyée, aucun secret affiché | Retour : Compte a.owona crééa.owona@gimac-afr.orgIdentifianta.owonaAdresse électroniquea.owona@gimac-afr.orgInvitation par courrielenvoyéeFermerCréer un autre compteOuvrir la fiche du compte ; secret affiché : false | [047](captures/administration/047-superadmin-invitation-envoyee-clair.png) | Conforme |
| 32 | Fiche de son propre compte | Super administrateur | Ouvrir super.admin | Mention « propre compte », pas de suppression ni désactivation de soi | Mention : true ; bouton Supprimer : 0 ; bouton Désactiver : 0 | [048](captures/administration/048-superadmin-propre-compte-clair.png) | Conforme |
| 33 | Fiche du compte > Sécurité | Super administrateur | Réinitialiser le second facteur de f.tchoumi avec motif | Second facteur réinitialisé | Message « Le second facteur a été réinitialisé. » : true | [049](captures/administration/049-superadmin-securite-2fa-reinitialise-clair.png) | Conforme |
| 34 | Profils et permissions | Super administrateur | Ouvrir la liste des profils | Profils livrés avec libellé, description, dernière modification ; correspondant marqué externe ; pas de suppression des profils livrés | Correspondant « Profil externe » : true ; bouton Supprimer sur ROLE_SNA : 0 | [050](captures/administration/050-superadmin-profils-liste-clair.png) | Conforme |
| 35 | Profils et permissions > composition | Super administrateur | Voir la composition du profil Super administrateur (onglet Droits) | Droits du domaine : IAM, PLATFORM, messagerie COL_MESSAGE_* | 35 droits cochés ; COL_MESSAGE_READ présent : false | [051](captures/administration/051-superadmin-profil-superadmin-droits-clair.png) | Anomalie |
| 36 | Profils et permissions > libellé | Super administrateur | Modifier libellé et description du profil Pôle réseau (motif, clé) | Profil enregistré, auteur de la modification affiché | Message : true ; auteur affiché : true | [053](captures/administration/053-superadmin-profil-libelle-enregistre-clair.png) | Conforme |
| 37 | Profils et permissions > externe | Super administrateur | Rendre externe le profil Normes et Agréments (titulaires présents) | Refus explicite : caractère externe immuable tant qu’il a des titulaires | Avertissement : true ; refus MS-IAM affiché : true | [055](captures/administration/055-superadmin-profil-externe-refus-clair.png) | Conforme |
| 38 | Profils et permissions > composition D-90 | Super administrateur | Recomposer Normes et Agréments : cocher « Mon équipe », un droit HOM_REPLAY_ADMIN ; aperçu de la barre latérale | Aperçu marque l’entrée ajoutée ; composition enregistrée après signature | Aperçu « (ajoutée) » : true ; onglets : Fonctionnalités, Droits, Pouvoirs, Organisation ; enregistrement : true | [057](captures/administration/057-superadmin-profil-composition-enregistree-clair.png) | Conforme |
| 39 | Profils et permissions > comparer | Super administrateur | Comparer SNA et Chef du DES, différences seulement | Droits qui diffèrent listés | Synthèse des différences affichée : true | [058](captures/administration/058-superadmin-profils-comparaison-clair.png) | Conforme |
| 40 | Profils et permissions > nouveau profil | Super administrateur | Envoyer un nouveau profil vide | Code invalide et description obligatoire signalés | Code invalide : true ; description obligatoire : true | [059](captures/administration/059-superadmin-nouveau-profil-vide-clair.png) | Conforme |
| 41 | Profils et permissions > nouveau profil | Super administrateur | Créer ROLE_ASTREINTE_NOC (droit HOM_LIAISON_READ) ; cocher Journal d’administration pour voir l’alerte « profil privilégié » puis décocher ; signer | Alerte privilégié affichée ; profil créé | Alerte profil privilégié : true ; création : true | [061](captures/administration/061-superadmin-nouveau-profil-cree-clair.png) | Conforme |
| 42 | Profils et permissions > suppression | Super administrateur | Supprimer ROLE_ASTREINTE_NOC (signature) | Profil supprimé | Message : true | [062](captures/administration/062-superadmin-profil-supprime-clair.png) | Conforme |
| 43 | Profils et permissions > Fonctionnalités | Super administrateur | Ouvrir l’onglet Fonctionnalités, application console | Catalogue de la console listé | Ligne consoleOrganigramme : true ; URL ?vue=fonctionnalites | [063](captures/administration/063-superadmin-catalogue-fonctionnalites-clair.png) | Conforme |
| 44 | Catalogue > modifier | Super administrateur | Vider libellé et ordre, icône invalide, enregistrer | Erreurs de saisie avant tout appel | Libellé : true ; ordre : true ; icône : true | [064](captures/administration/064-superadmin-catalogue-saisie-invalide-clair.png) | Conforme |
| 45 | Catalogue > modifier | Super administrateur | Renommer consoleOrganigramme « Organigramme du GIMAC » (motif, clé) | Barre latérale renommée sans reconstruction | Entrée « Organigramme du GIMAC » dans la barre latérale : true | [066](captures/administration/066-superadmin-catalogue-barre-renommee-clair.png) | Conforme |
| 46 | Catalogue > masquer | Super administrateur | Masquer consoleJournal (interrupteur Visible), signer | Entrée retirée de la barre ; écran refusé par l’adresse | Avertissement : true ; entrée absente : true ; écran refusé : true (captures 067-superadmin-catalogue-journal-masque-clair.png, 068-superadmin-catalogue-journal-refuse-clair.png) | [068](captures/administration/068-superadmin-catalogue-journal-refuse-clair.png) | Conforme |
| 47 | Catalogue > rétablir | Super administrateur | Rendre de nouveau visible consoleJournal | Entrée revenue dans la barre latérale | Entrée revenue : true |  | Conforme |
| 48 | Matrice des pouvoirs | Super administrateur | Ouvrir la matrice | Lignes par profil, colonnes des pouvoirs dont « Processus : définir / lancer » | Colonnes processus : true | [069](captures/administration/069-superadmin-matrice-clair.png) | Conforme |
| 49 | Matrice des pouvoirs > ligne | Super administrateur | Ouvrir Responsable de service, lire les portées proposées | Portées AUCUNE à UNITE_ET_SOUS_UNITES pour les gestes ; « Toutes les unités » seulement pour les processus | Gestes : Aucune / Directs / Équipe / Unités ; processus : Aucune / Directs / Équipe / Unités / Toutes les unités ; « Toutes » hors processus : false ; profils privilégiés ou externes demandables : 0 ; enregistrement : true | [071](captures/administration/071-superadmin-matrice-enregistree-clair.png) | Conforme |
| 50 | Demandes à traiter | Super administrateur | Ouvrir la liste | Demandes en attente des chefs (compte, profils) | Demande de compte d’Ingrid Fouda Ngono listée : true | [072](captures/administration/072-superadmin-demandes-liste-clair.png) | Conforme |
| 51 | Demandes à traiter > accepter | Super administrateur | Ouvrir la demande de création de compte, accepter avec la clé | Demande acceptée, compte créé | Identifiant i.fouda présenté : true ; acceptation : true | [074](captures/administration/074-superadmin-demande-acceptee-clair.png) | Conforme |
| 52 | Demandes à traiter > refuser | Super administrateur | Ouvrir la demande de profils de Nadège Mvondo Assiga, refuser avec motif | Demande refusée | Refus : true |  | Conforme |
| 53 | Demandes à traiter > filtre | Super administrateur | Filtrer État : toutes | Demandes traitées visibles avec leur état | 4 demandes listées | [076](captures/administration/076-superadmin-demandes-toutes-clair.png) | Conforme |
| 54 | Demandes à traiter > effet | Super administrateur | Ouvrir Comptes après l’acceptation | Le compte i.fouda est créé | i.fouda dans la liste : true | [077](captures/administration/077-superadmin-comptes-apres-acceptation-clair.png) | Conforme |
| 55 | Organisation | Super administrateur | Ouvrir l’arbre et sélectionner Service Production informatique | Fiche de l’unité avec chef, gestes Modifier et Désactiver | Gestes visibles : true | [078](captures/administration/078-superadmin-organisation-arbre-clair.png) | Conforme |
| 56 | Organisation > modifier | Super administrateur | Désigner Patrick Mbida Essomba responsable de l’unité | Unité modifiée, responsable affiché | Message : true ; responsable affiché : true | [080](captures/administration/080-superadmin-organisation-modifiee-clair.png) | Conforme |
| 57 | Organisation > nouvelle unité | Super administrateur | Créer une unité sans code puis avec code CELLULE_AUDIT | Code exigé, puis unité créée | Erreurs à vide : Code attendu : majuscules, chiffres ou souligné, 2 à 32 caractères, commençant par une lettre. / Indiquez le libellé de l’unité. / Choisissez l’unité parente. ; création : true | [082](captures/administration/082-superadmin-organisation-unite-creee-clair.png) | Conforme |
| 58 | Organigramme | Super administrateur | Ouvrir l’arbre, sélectionner SNA | Unités avec chef et effectif ; membres du SNA | Arbre chargé : DES « Chef : Christian Ndong Obiang · 5 personnes », membres du SNA avec leur n+1 et gestes Renommer, Déplacer, Sous-unité, Désactiver. Pendant le chargement, la carte affiche brièvement l’identifiant technique « Chef : u-chef-des · 0 personne » (voir ADM-anomalie d’affichage transitoire) | [083](captures/administration/083-superadmin-organigramme-arbre-clair.png) | Conforme |
| 59 | Organigramme > glisser-déposer | Super administrateur | Glisser le pôle système sur le SNA, confirmer avec motif | Unité déplacée | Confirmation : true ; déplacement : true |  | Conforme |
| 60 | Organigramme > n+1 | Super administrateur | Déposer Rachel Biyong Mengue sur Patrick Mbida Essomba (nouveau n+1) | Nouveau n+1 enregistré | Message : true | [085](captures/administration/085-superadmin-organigramme-n1-clair.png) | Conforme |
| 61 | Organigramme > pouvoirs | Super administrateur | Ouvrir le panneau Pouvoirs de Rachel Biyong Mengue, suivre le lien vers la matrice | Portée effective, lien vers la ligne de la matrice | Lien vers /console/matrice-pouvoirs?profil=ROLE_SNA | [086](captures/administration/086-superadmin-organigramme-panneau-pouvoirs-clair.png) | Conforme |
| 62 | Organigramme > sous-unité | Super administrateur | Créer une sous-unité du SNA, la renommer, la désactiver (motif) | Créée, renommée, désactivée | Créée : true ; renommée : true ; désactivée : true | [087](captures/administration/087-superadmin-organigramme-sous-unite-desactivee-clair.png) | Conforme |
| 63 | Organigramme > vue Liste | Super administrateur | Désigner au clavier le chef du Pôle réseau (motif) | Chef d’unité désigné | Message : true | [089](captures/administration/089-superadmin-organigramme-chef-designe-clair.png) | Conforme |
| 64 | Politique de sécurité | Super administrateur | Ouvrir l’écran | Mots de passe, OTP, sessions et force brute présentés ; bouton Modifier | Bouton Modifier la politique : true | [090](captures/administration/090-superadmin-politique-clair.png) | Conforme |
| 65 | Politique de sécurité > modifier | Super administrateur | Saisir « cinq » dans l’historique | Refus local « Nombre entier attendu », aucune signature | Message : true ; modales ouvertes : 1 | [091](captures/administration/091-superadmin-politique-saisie-invalide-clair.png) | Conforme |
| 66 | Politique de sécurité > modifier | Super administrateur | Longueur 8 et période OTP 90 s, signer | Refus champ par champ du service (12 à 128 ; 30 à 60) | Bandeau : true ; longueur : true ; période : true | [092](captures/administration/092-superadmin-politique-refus-service-clair.png) | Conforme |
| 67 | Politique de sécurité > appliquer | Super administrateur | Longueur 16, OTP 30 s HmacSHA512, inactivité 20 min, signer | Politique appliquée et affichée | Message : true ; longueur 16 affichée : true | [093](captures/administration/093-superadmin-politique-appliquee-clair.png) | Conforme |
| 68 | Courriels de la plateforme | Super administrateur | Ouvrir l’écran | Configuration SMTP, mot de passe masqué | « mot de passe défini » affiché sans valeur : true | [094](captures/administration/094-superadmin-courriels-clair.png) | Conforme |
| 69 | Courriels > modifier | Super administrateur | Port 0 | Erreur « Port de 1 à 65535 » | Champ mot de passe de type password ; erreur port : true | [095](captures/administration/095-superadmin-courriels-port-invalide-clair.png) | Conforme |
| 70 | Courriels > modifier | Super administrateur | Port 465, SSL/TLS implicite, nouveau secret, motif | Messagerie enregistrée | Message : true | [096](captures/administration/096-superadmin-courriels-enregistres-clair.png) | Conforme |
| 71 | Courriels > test | Super administrateur | Envoyer un courriel de test à rssi@gimac-afr.org | Envoi confirmé | Message : true | [097](captures/administration/097-superadmin-courriels-test-clair.png) | Conforme |
| 72 | Journal d’administration | Super administrateur | Ouvrir, filtrer par compte concerné (k.ateba, désactivé) | Actes filtrés | 5 actes ; filtré : 1, Désactivation visible : true | [098](captures/administration/098-superadmin-journal-clair.png) | Conforme |
| 73 | Configuration de la plateforme | Super administrateur | Ouvrir l’écran | Paramètres des services, secrets masqués | bff-console-iam : true ; secret masqué : true | [100](captures/administration/100-superadmin-configuration-clair.png) | Conforme |
| 74 | Journalisation des services | Super administrateur | Filtrer « org. », passer org.hibernate.SQL en DEBUG (motif) | Niveau appliqué | Message : true | [102](captures/administration/102-superadmin-journalisation-applique-clair.png) | Conforme |
| 75 | Centre de contrôle (supervision) | Super administrateur | Ouvrir l’écran | État temps réel, services, serveurs, alertes | Région temps réel : true | [103](captures/administration/103-superadmin-supervision-clair.png) | Conforme |
| 76 | Infrastructure | Super administrateur | Ouvrir l’écran (super administrateur, PLATFORM_OPS_EXECUTE) | Charges Kubernetes avec gestes ; gateway non arrêtable | Charges de travail (Kubernetes), nœuds, tâches et historique ; gestes Courbes, Journaux, Redémarrer, Arrêter, Mettre à l’échelle, Rétablir ; aucun geste Arrêter sur gateway (charge protégée) | [104](captures/administration/104-superadmin-infrastructure-clair.png) | Conforme |
| 77 | Infrastructure > geste | Super administrateur | Redémarrer iam (motif, clé) | Opération acceptée puis réussie dans l’historique | Acceptée : true ; réussie : true | [105](captures/administration/105-superadmin-infrastructure-redemarrage-clair.png) | Conforme |
| 78 | Tableau de bord RSSI | RSSI | Ouvrir la console | Titre RSSI, gestion des utilisateurs, posture de sécurité, derniers actes, état temps réel | Titre « Tableau de bord de la sécurité des SI » ; posture : true ; derniers actes : true ; temps réel : true | [106](captures/administration/106-rssi-console-tableau-de-bord-plein-clair.png) | Conforme |
| 79 | Tableau de bord RSSI | RSSI | Cliquer « Consulter la politique de sécurité » puis « Voir tout le journal » | Écrans ouverts | Politique de sécurité ; Journal d’administration |  | Conforme |
| 80 | Comptes > Créer un compte | RSSI | Ouvrir la création | Aucun profil privilégié proposé (Super administrateur, Sécurité des SI, Administrateur système) | Profils privilégiés proposés : aucun ; profils standard proposés : true | [107](captures/administration/107-rssi-creation-compte-sans-privilegie-clair.png) | Conforme |
| 81 | Comptes > Créer un compte | RSSI | Créer c.mbarga (Pôle système, invitation) | Compte créé | Création : true | [108](captures/administration/108-rssi-creation-compte-ok-clair.png) | Conforme |
| 82 | Fiche d’un compte privilégié (admin.systeme) | RSSI | Modifier les profils de Thierry Abessolo Nkoulou | Profil privilégié détenu non modifiable par le RSSI ; aucun privilégié à cocher | Le compte u-admin-systeme (admin.systeme, Thierry Abessolo Nkoulou), identité de démonstration de l’administrateur système, n’existe pas dans l’annuaire des comptes de la console : « Le chargement a échoué. Ressource introuvable ». La liste des comptes (13) ne comporte aucun administrateur système ; le contrôle sur un compte privilégié est fait sur super.admin (case Super administrateur absente pour le RSSI). | [109](captures/administration/109-rssi-echec-priv-fiche.png) | Anomalie |
| 83 | Dernier super administrateur | RSSI | Désactiver super.admin (motif) | Refus : la plateforme garde au moins un super administrateur actif | Refus affiché : true | [110](captures/administration/110-rssi-dernier-superadmin-desactivation-refusee-clair.png) | Conforme |
| 84 | Dernier super administrateur | RSSI | Modifier les profils de super.admin : ajouter Conformité | Case Super administrateur absente ; profil privilégié conservé | Case absente : true ; modification : true ; Super administrateur conservé : true | [111](captures/administration/111-rssi-dernier-superadmin-profils-clair.png) | Conforme |
| 85 | Dernier super administrateur | RSSI | Supprimer super.admin (confirmation, signature) | Refus du service dans la signature | Refus affiché : true | [112](captures/administration/112-rssi-dernier-superadmin-suppression-refusee-clair.png) | Conforme |
| 86 | Profils et permissions | RSSI | Voir la composition de ROLE_SUPER_ADMIN | Consultation seule, droits désactivés | Consultation seule : true ; case désactivée : true | [113](captures/administration/113-rssi-profil-superadmin-consultation-clair.png) | Conforme |
| 87 | Profils et permissions > nouveau profil | RSSI | Composer un profil avec « Journal d’administration » (permission IAM, privilégiée) | Le RSSI ne peut pas créer de profil privilégié (case désactivée ou refus) | Case « Journal d’administration » désactivée : false ; après signature : refus « privilégiéMotif *Profil d’auditConsigné dans la chronologie et le registre d'audit. » | [115](captures/administration/115-rssi-nouveau-profil-privilegie-resultat-clair.png) | Conforme |
| 88 | Journalisation des services | RSSI | Ouvrir la journalisation du service participant | Lecture seule : changement réservé au super administrateur | Mention lecture seule : true ; boutons Modifier le niveau : 0 | [116](captures/administration/116-rssi-journalisation-lecture-seule-clair.png) | Conforme |
| 89 | Configuration de la plateforme | RSSI | Ouvrir l’écran | Lecture seule (pas de PLATFORM_CONFIG_EDIT) | Boutons de modification : aucun | [117](captures/administration/117-rssi-configuration-clair.png) | Conforme |
| 90 | Infrastructure | RSSI | Ouvrir l’écran | Lecture seule : journaux et courbes, aucun geste | Bandeau lecture seule : true ; Journaux de iam : 1 ; gestes : 0 | [118](captures/administration/118-rssi-infrastructure-lecture-seule-clair.png) | Conforme |
| 91 | Infrastructure > journaux | RSSI | Ouvrir les journaux de iam, 5 lignes depuis 30 min | Journaux affichés et actualisés | Panneau « Journaux de iam » ouvert sous le tableau (Lignes, Depuis, Actualiser) ; après actualisation, 5 lignes sur 30 minutes | [119](captures/administration/119-rssi-infrastructure-journaux-clair.png) | Conforme |
| 92 | Politique de sécurité | RSSI | Modifier la politique (RSSI porte PLATFORM_SECURITY_POLICY_EDIT) | Modification permise, signée | Application : true | [120](captures/administration/120-rssi-politique-appliquee-clair.png) | Conforme |
| 93 | Courriels de la plateforme | RSSI | Modifier l’expéditeur (RSSI porte PLATFORM_MESSAGING_EDIT) | Enregistrement permis | Enregistrement : true | [121](captures/administration/121-rssi-courriels-clair.png) | Conforme |
| 94 | Matrice des pouvoirs | RSSI | Ouvrir la matrice (IAM_PROFILE_EDIT) | Matrice modifiable | Matrice chargée : 16 boutons « Pouvoirs du profil … » (édition par ligne permise avec IAM_PROFILE_EDIT) | [122](captures/administration/122-rssi-matrice-clair.png) | Conforme |
| 95 | Demandes à traiter | RSSI | Accepter la demande de compte d’Ingrid Fouda Ngono | Acceptation permise au RSSI | Acceptation : true | [123](captures/administration/123-rssi-demande-acceptee-clair.png) | Conforme |
| 96 | Organigramme | RSSI | Ouvrir l’organigramme | Arbre et gestes (RSSI administre les comptes) | Arbre : true | [124](captures/administration/124-rssi-organigramme-clair.png) | Conforme |
| 97 | Tableau de bord de l’exploitation | Admin système | Ouvrir la console | Tableau de l’exploitation : charges en erreur, supervision ; aucune gestion des utilisateurs | Titre « Tableau de bord de l’exploitation » ; charges en erreur : true ; gestion des utilisateurs : 0 | [125](captures/administration/125-adminsys-console-tableau-de-bord-plein-clair.png) | Conforme |
| 98 | Tableau de bord de l’exploitation | Admin système | Cliquer la charge en erreur « pilotage » | Infrastructure ouverte sur les journaux de pilotage | URL /console/plateforme/infrastructure?charge=pilotage ; journaux : true | [126](captures/administration/126-adminsys-infrastructure-journaux-pilotage-clair.png) | Conforme |
| 99 | Infrastructure | Admin système | Lire les charges, gestes proposés | Gestes Redémarrer, Arrêter, Échelle, Rétablir ; aucun arrêt de la passerelle | Redémarrer iam : 1 ; Arrêter gateway : 0 | [127](captures/administration/127-adminsys-infrastructure-clair.png) | Conforme |
| 100 | Infrastructure > fiche | Admin système | Ouvrir les courbes de homologation : onglets Pods et Événements | Fiche avec courbes, pods et événements | Pods : true ; événements : true | [130](captures/administration/130-adminsys-infrastructure-evenements-clair.png) | Conforme |
| 101 | Infrastructure > redémarrer | Admin système | Redémarrer iam sans motif puis avec motif et clé | Motif exigé ; opération acceptée puis réussie | Clé désactivée sans motif : true ; acceptée : true ; réussie : true | [132](captures/administration/132-adminsys-infrastructure-historique-clair.png) | Conforme |
| 102 | Infrastructure > échelle | Admin système | Mettre certification à 9 répliques puis 3 | Hors bornes refusé (0 à 5) ; 3 / 3 ensuite | Refus hors bornes : true ; 3 / 3 : true | [133](captures/administration/133-adminsys-infrastructure-echelle-refus-clair.png) | Conforme |
| 103 | Infrastructure > arrêter, démarrer | Admin système | Arrêter puis démarrer certification | Charge arrêtée puis redémarrée | Démarrer proposé après arrêt : true ; Arrêter de nouveau proposé : true | [134](captures/administration/134-adminsys-infrastructure-arretee-clair.png) | Conforme |
| 104 | Infrastructure > rétablir | Admin système | Rétablir la version précédente de homologation | Opération consignée | Historique : true |  | Conforme |
| 105 | Infrastructure > nœuds et tâches | Admin système | Lire les nœuds du cluster et les jobs | Sections présentes | Nœuds : true ; tâches : true | [135](captures/administration/135-adminsys-infrastructure-noeuds-clair.png) | Conforme |
| 106 | Journalisation des services | Admin système | Ouvrir la journalisation | Lecture seule pour l’administrateur système | Boutons Modifier le niveau : 0 | [136](captures/administration/136-adminsys-journalisation-clair.png) | Conforme |
| 107 | Configuration de la plateforme | Admin système | Ouvrir la configuration | Lecture seule | Boutons de modification : aucun | [137](captures/administration/137-adminsys-configuration-clair.png) | Conforme |
| 108 | Centre de contrôle | Admin système | Ouvrir la supervision | État temps réel | Temps réel : true | [138](captures/administration/138-adminsys-supervision-clair.png) | Conforme |
| 109 | Recherche globale (Ctrl+K) | Admin système | Chercher « ndong » | Aucun compte proposé (pas d’IAM_ACCOUNT_READ) | Résultat : ÉchapAucun résultat pour « ndong »↑↓ parcourirEntrée ouvrirCtrl+K | [139](captures/administration/139-adminsys-recherche-globale-clair.png) | Conforme |
| 110 | Application interne > tableau de bord | Admin système | Ouvrir le tableau de bord de l’application interne | Région Supervision de la plateforme ; pas d’administration des comptes | Supervision : true ; gestion des utilisateurs : 0 | [140](captures/administration/140-adminsys-des-tableau-de-bord-clair.png) | Conforme |
| 111 | Application interne > Infrastructure | Admin système | Ouvrir l’infrastructure de l’application interne | Même écran, gestes présents | URL /des/plateforme/infrastructure ; Redémarrer iam : 1 | [141](captures/administration/141-adminsys-des-infrastructure-clair.png) | Conforme |
| 112 | Application interne > autres lots | Admin système | Ouvrir par l’adresse /des/institutions, /des/parcours, /des/referentiels/operations, /des/equipe, /des/certification/lancer | Refus propre pour chacun | /des/institutions : refusé ; /des/parcours : refusé ; /des/referentiels/operations : refusé ; /des/equipe : refusé ; /des/certification/lancer : refusé | [142](captures/administration/142-adminsys-des-refus-autres-lots-clair.png) | Conforme |
| 113 | Mon compte (console) | Super administrateur | Ouvrir Mon compte, modifier ma fiche | Fiche enregistrée | Enregistrement : true |  | Conforme |
| 114 | Messagerie (console) | Super administrateur | Ouvrir la messagerie, une conversation, envoyer un message | Message affiché dans le fil | 3 conversations ; message affiché : true | [146](captures/administration/146-superadmin-messagerie-console-clair.png) | Conforme |
| 115 | Notifications (console) | Super administrateur | Ouvrir la cloche, puis « Voir tout » | Centre de notifications puis page Notifications | Cloche « Notifications, 2 non lues » ; panneau : true ; page « Notifications » | [148](captures/administration/148-superadmin-notifications-page-clair.png) | Conforme |
| 116 | Application interne > Mon équipe | Super administrateur | Ouvrir /des/equipe (pas de PIL_TEAM_READ) | Refus propre | Refus : true | [149](captures/administration/149-superadmin-des-equipe-refusee-clair.png) | Conforme |
| 117 | Application interne > Administration | Super administrateur | Créer un compte depuis /des/administration/comptes/nouveau | Modale de création dans l’application interne | Modale : true ; profil privilégié proposé au super administrateur : true | [150](captures/administration/150-superadmin-des-creation-compte-clair.png) | Conforme |
| 118 | Application interne > lien console | Super administrateur | Cliquer « Console de la plateforme » | Console ouverte sur le port 6102 | href http://localhost:6102/console/tableau-de-bord ; page « Tableau de bord du super administrateur » (http://localhost:6102/console/tableau-de-bord) | [151](captures/administration/151-superadmin-lien-console-clair.png) | Conforme |
| 119 | Console > Retour au département | Super administrateur | Cliquer « Retour au département » | Application interne ouverte (port 6100) | href http://localhost:6100/des/tableau-de-bord ; URL http://localhost:6100/des/tableau-de-bord |  | Conforme |
| 120 | Application interne > tableau de bord (sombre) | RSSI | Ouvrir le tableau de bord | Gestion des utilisateurs et derniers actes ; thème sombre | Gestion : true ; data-theme=dark | [153](captures/administration/153-rssi-des-tableau-de-bord-sombre.png) | Conforme |
| 121 | Application interne > Comptes utilisateurs (sombre) | RSSI | Ouvrir « Comptes utilisateurs » | Écran lisible en sombre | Titre « Comptes » | [154](captures/administration/154-rssi-des-comptes-sombre.png) | Conforme |
| 122 | Application interne > Profils et permissions (sombre) | RSSI | Ouvrir « Profils et permissions » | Écran lisible en sombre | Titre « Profils et permissions » | [155](captures/administration/155-rssi-des-profils-sombre.png) | Conforme |
| 123 | Application interne > Matrice des pouvoirs (sombre) | RSSI | Ouvrir « Matrice des pouvoirs » | Écran lisible en sombre | Titre « Matrice des pouvoirs » | [156](captures/administration/156-rssi-des-matrice-sombre.png) | Conforme |
| 124 | Application interne > Organigramme (sombre) | RSSI | Ouvrir « Organigramme » | Écran lisible en sombre | Titre « Organigramme » | [157](captures/administration/157-rssi-des-organigramme-sombre.png) | Conforme |
| 125 | Application interne > Politique de sécurité (sombre) | RSSI | Ouvrir « Politique de sécurité » | Écran lisible en sombre | Titre « Politique de sécurité » | [158](captures/administration/158-rssi-des-politique-sombre.png) | Conforme |
| 126 | Application interne > Infrastructure (sombre) | RSSI | Ouvrir « Infrastructure » | Écran lisible en sombre | Titre « Infrastructure » | [159](captures/administration/159-rssi-des-infrastructure-sombre.png) | Conforme |
| 127 | Application interne > fiche compte (sombre) | RSSI | Ouvrir la fiche de r.biyong, onglet Sécurité | Actes de sécurité présents | Actes de sécurité : true | [160](captures/administration/160-rssi-des-fiche-securite-sombre.png) | Conforme |
| 128 | Console /console/tableau-de-bord (sombre) | Super administrateur | Ouvrir en thème sombre | Lisible, contrastes corrects | Titre « Tableau de bord du super administrateur » ; fond rgb(24, 26, 30) | [161](captures/administration/161-superadmin-console-tableau-de-bord-sombre.png) | Conforme |
| 129 | Console /console/comptes (sombre) | Super administrateur | Ouvrir en thème sombre | Lisible, contrastes corrects | Titre « Comptes » ; fond rgb(24, 26, 30) | [162](captures/administration/162-superadmin-console-comptes-sombre.png) | Conforme |
| 130 | Console /console/profils (sombre) | Super administrateur | Ouvrir en thème sombre | Lisible, contrastes corrects | Titre « Profils et permissions » ; fond rgb(24, 26, 30) | [163](captures/administration/163-superadmin-console-profils-sombre.png) | Conforme |
| 131 | Console /console/organigramme (sombre) | Super administrateur | Ouvrir en thème sombre | Lisible, contrastes corrects | Titre « Organigramme » ; fond rgb(24, 26, 30) | [164](captures/administration/164-superadmin-console-organigramme-sombre.png) | Conforme |
| 132 | Console /console/plateforme/infrastructure (sombre) | Super administrateur | Ouvrir en thème sombre | Lisible, contrastes corrects | Titre « Infrastructure » ; fond rgb(24, 26, 30) | [165](captures/administration/165-superadmin-console-infrastructure-sombre.png) | Conforme |
| 133 | Console /console/politique-securite (sombre) | Super administrateur | Ouvrir en thème sombre | Lisible, contrastes corrects | Titre « Politique de sécurité » ; fond rgb(24, 26, 30) | [166](captures/administration/166-superadmin-console-politique-sombre.png) | Conforme |
| 134 | Fiche compte (sombre) | Super administrateur | Ouvrir Modifier les profils sur r.biyong | Modale lisible en sombre | Modale ouverte | [167](captures/administration/167-superadmin-modale-profils-sombre.png) | Conforme |
| 135 | Console /console/tableau-de-bord (390 px) | Super administrateur | Ouvrir à 390 px de large | Pas de défilement horizontal de la page | Débord horizontal : 0 px | [168](captures/administration/168-superadmin-console-tableau-de-bord-mobile.png) | Conforme |
| 136 | Console /console/comptes (390 px) | Super administrateur | Ouvrir à 390 px de large | Pas de défilement horizontal de la page | Débord horizontal : 0 px | [169](captures/administration/169-superadmin-console-comptes-mobile.png) | Conforme |
| 137 | Console /console/comptes/u-sna (390 px) | Super administrateur | Ouvrir à 390 px de large | Pas de défilement horizontal de la page | Débord horizontal : 0 px | [170](captures/administration/170-superadmin-console-fiche-mobile.png) | Conforme |
| 138 | Console /console/matrice-pouvoirs (390 px) | Super administrateur | Ouvrir à 390 px de large | Pas de défilement horizontal de la page | Débord horizontal : 0 px | [171](captures/administration/171-superadmin-console-matrice-mobile.png) | Conforme |
| 139 | Console (390 px) > menu | Super administrateur | Ouvrir le menu de navigation | Barre latérale accessible par un bouton | Menu ouvert, entrée Comptes visible : true | [172](captures/administration/172-superadmin-console-menu-mobile.png) | Conforme |
| 140 | Console /console/tableau-de-bord (en-US) | Super administrateur | Ouvrir avec un navigateur en anglais | Textes en anglais, aucune clé brute ni reste en français | Super administrator dashboard ; clés brutes : aucune ; interface entièrement en anglais, aucune clé brute ; seuls les libellés des profils restent en français (Chef du DES, Sécurité des SI…) : ce sont des données servies par MS-IAM (libellé modifiable, D-86), pas des textes de l’interface | [173](captures/administration/173-superadmin-console-tableau-de-bord-en.png) | Conforme |
| 141 | Console /console/comptes/u-sna (en-US) | Super administrateur | Ouvrir avec un navigateur en anglais | Textes en anglais, aucune clé brute ni reste en français | Rachel Biyong Mengue ; clés brutes : aucune ; lignes restées en français : aucune | [174](captures/administration/174-superadmin-console-fiche-en.png) | Conforme |
| 142 | Console /console/matrice-pouvoirs (en-US) | Super administrateur | Ouvrir avec un navigateur en anglais | Textes en anglais, aucune clé brute ni reste en français | Powers matrix ; clés brutes : aucune ; interface entièrement en anglais, aucune clé brute ; seuls les libellés des profils restent en français (Chef du DES, Sécurité des SI…) : ce sont des données servies par MS-IAM (libellé modifiable, D-86), pas des textes de l’interface | [175](captures/administration/175-superadmin-console-matrice-en.png) | Conforme |
| 143 | Console /console/plateforme/infrastructure (en-US) | Super administrateur | Ouvrir avec un navigateur en anglais | Textes en anglais, aucune clé brute ni reste en français | Infrastructure ; clés brutes : aucune ; lignes restées en français : aucune | [176](captures/administration/176-superadmin-console-infrastructure-en.png) | Conforme |
| 144 | Mon compte (console) | Super administrateur | Enrôler une clé, mot de passe trop faible puis conforme, TOTP, fermer les autres sessions | Faible refusé ; chaque geste confirmé | Clé : true ; « court » : Ce mot de passe ne respecte pas la politique de sécurité.Règles non respectées : au moins 12 caractères ; au moins une majuscule ; au moins un chiffre ; au moins un caractère spécial.sim-mv001a0l-1 ; changé : true ; QR : true ; TOTP : true ; sessions : true | [179](captures/administration/179-superadmin-mon-compte-securite-clair.png) | Conforme |
| 145 | Connexion à la console | Super administrateur | Se connecter avec super.admin, mot de passe et code à six chiffres | Tableau de bord du super administrateur | URL /console/tableau-de-bord ; titre « Tableau de bord du super administrateur » | [180](captures/administration/180-superadmin-connexion-console-clair.png) | Conforme |
| 146 | Connexion au portail des participants | Super administrateur | Se connecter au portail avec super.admin | Compte interne refusé sur le portail (CNX_COUCHE_REFUSEE) et conduit vers l’application interne | URL http://localhost:6100/connexion?motif=aiguillage&depuis=portail&identifiant=super.admin ; message : aucun | [181](captures/administration/181-superadmin-connexion-portail-refusee-clair.png) | Conforme |
| 147 | Connexion à la console | RSSI | Se connecter avec rssi, mot de passe et code à six chiffres | Tableau de bord de la sécurité des SI | URL /console/tableau-de-bord ; titre « Tableau de bord de la sécurité des SI » | [182](captures/administration/182-rssi-connexion-console-clair.png) | Conforme |
| 148 | Connexion à la console | Admin système | Se connecter avec admin.systeme, mot de passe et code à six chiffres | Tableau de bord de l’exploitation | URL /console/tableau-de-bord ; titre « Tableau de bord de l’exploitation » | [183](captures/administration/183-adminsys-connexion-console-clair.png) | Conforme |
| 149 | Signature d’un acte refusé par le service | Super administrateur | Signer « rendre externe » un profil qui a des titulaires | Refus métier présenté seul ; la clé de sécurité n’est pas mise en cause | Le refus de MS-IAM s’affiche, mais le bloc de signature passe en erreur « La clé de sécurité n’a pas répondu. Vérifiez que la clé est branchée puis réessayez » avec « Réessayer », alors que la clé a répondu | [055](captures/administration/055-superadmin-profil-externe-refus-clair.png) | Anomalie |
| 150 | Signature d’un acte refusé par le service | RSSI | Signer la création d’un profil privilégié (Journal d’administration) | Refus métier présenté seul, idéalement avant la signature | La case est cochable, le refus « réservée au super administrateur » n’arrive qu’après la clé, accompagné de « La clé de sécurité n’a pas répondu » et « Réessayer » | [115](captures/administration/115-rssi-nouveau-profil-privilegie-resultat-clair.png) | Anomalie |
| 151 | Barre latérale de l’application interne | Admin système | Comparer l’entrée « Supervision » et le titre de l’écran | Même libellé dans la barre et le titre (et dans la console) | Application interne : entrée « Supervision » ; console : entrée « Centre de contrôle » ; titre de l’écran dans les deux : « Centre de contrôle » | [015](captures/administration/015-adminsys-des-barre-laterale-clair.png) | Anomalie |
| 152 | Profils et permissions > nouveau profil | Super administrateur | Lire l’aperçu de la barre latérale d’un nouveau profil | Aperçu fidèle : Messagerie, Notifications et Mon activité ajoutées d’office (texte de la modale) | La modale annonce « Messagerie, Mon activité et notifications lui sont ajoutées » mais l’aperçu ne montre que Tableau de bord, Mes actions de certification et Journal d’administration (3 entrées) | [060](captures/administration/060-superadmin-nouveau-profil-privilegie-alerte-clair.png) | Anomalie |
| 153 | Profils et permissions > arbre des fonctionnalités | Super administrateur | Lire les noms accessibles des boutons de dépliage | Français correct | Noms « Droits de Agenda », « Droits de Institutions membres », « Droits de Administration de tous les domaines », « Droits de Organisation » (élision manquante) | [056](captures/administration/056-superadmin-profil-composition-arbre-apercu-clair.png) | Anomalie |
| 154 | Organigramme | Super administrateur | Ouvrir l’organigramme et lire la carte du DES pendant le chargement | Nom du chef ou indicateur de chargement | Pendant le chargement des comptes, la carte affiche l’identifiant technique « Chef : u-chef-des · 0 personne », remplacé ensuite par « Christian Ndong Obiang · 5 personnes » | [083](captures/administration/083-superadmin-organigramme-arbre-clair.png) | Anomalie |
| 155 | Profils de démonstration de la console | RSSI | Comparer les permissions de la session de démonstration de la console au domaine Keycloak | Mêmes permissions que generer-realm-gimac.py | Console (packages/administration/src/profilsDemo.ts) : RSSI sans CER_SECURITY_VERIFY ni HOM_EVIDENCE_SUBMIT ; super administrateur sans HOM_REFERENTIEL_READ_ALL, HOM_REFERENTIEL_MIGRATE ni REF_METIER_ADMIN (l’application interne, @gimac/lot1 PROFILS, est alignée) |  | Anomalie |

## 3. Matrice des accès du lot

Attendu calculé à partir du catalogue `ms-iam/src/main/resources/config/fonctionnalites-initiales.json` (prédicats `tous` et `unDe`) et des permissions des profils de `infra/keycloak/generer-realm-gimac.py`. « visible » : entrée de la barre latérale présente et écran ouvert par l’adresse directe ; « refusée » : entrée absente et écran « Accès refusé, votre profil ne donne pas accès à cet écran », sans donnée affichée. Les entrées fixes « Mon compte » et « Se déconnecter » sont présentes pour tous.

### Console (6102)

| Route | Super administrateur | RSSI | Admin système |
|---|---|---|---|
| `/console/tableau-de-bord` | visible : conforme | visible : conforme | visible : conforme |
| `/console/messagerie` | visible : conforme | visible : conforme | visible : conforme |
| `/console/notifications` | visible : conforme | visible : conforme | visible : conforme |
| `/console/comptes` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/demandes` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/profils` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/matrice-pouvoirs` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/organisation` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/organigramme` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/journal` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/politique-securite` | visible : conforme | visible : conforme | refusée : conforme |
| `/console/plateforme/supervision` | visible : conforme | visible : conforme | visible : conforme |
| `/console/plateforme/infrastructure` | visible : conforme | visible : conforme | visible : conforme |
| `/console/plateforme/configuration` | visible : conforme | visible : conforme | visible : conforme |
| `/console/plateforme/journalisation` | visible : conforme | visible : conforme | visible : conforme |
| `/console/plateforme/courriels` | visible : conforme | visible : conforme | refusée : conforme |

### Application interne, administration et plateforme (6100)

| Route | Super administrateur | RSSI | Admin système |
|---|---|---|---|
| `/des/administration/comptes` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/demandes` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/profils` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/matrice-pouvoirs` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/organisation` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/organigramme` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/journal` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/administration/politique-securite` | visible : conforme | visible : conforme | refusée : conforme |
| `/des/plateforme/supervision` | visible : conforme | visible : conforme | visible : conforme |
| `/des/plateforme/infrastructure` | visible : conforme | visible : conforme | visible : conforme |
| `/des/plateforme/configuration` | visible : conforme | visible : conforme | visible : conforme |
| `/des/plateforme/journalisation` | visible : conforme | visible : conforme | visible : conforme |
| `/des/plateforme/courriels` | visible : conforme | visible : conforme | refusée : conforme |

Autres entrées vérifiées : console « Retour au département », « Messagerie », « Notifications », « Mon activité » visibles pour les trois profils ; application interne : « Mon équipe » visible pour le RSSI seulement (`/des/equipe` refusé au super administrateur et à l’admin système), « Console de la plateforme » visible pour les trois ; écrans des autres lots (`/des/institutions`, `/des/parcours`, `/des/referentiels/operations`, `/des/certification/lancer`) refusés à l’admin système. Gestes réservés : le RSSI ne voit aucun profil privilégié à attribuer (création, modification des profils), la composition du super administrateur est en consultation seule, la journalisation est en lecture seule, l’infrastructure n’offre aucun geste ; l’admin système n’a ni comptes, ni demandes, ni profils, ni matrice, ni organisation, ni journal, ni politique, ni courriels (refusés), aucun geste de journalisation ou de configuration, et la recherche globale ne lui rend aucun compte.

## 4. Anomalies

### ADM-01 : Catalogue simulé des profils non aligné sur le domaine Keycloak (mineure)

- **Reproduire** : Console, super administrateur : Profils et permissions, « Modifier la composition » de ROLE_SUPER_ADMIN, onglet Droits.
- **Observé** : La composition affichée (33 permissions sur 74) ne comporte pas COL_MESSAGE_READ ni COL_MESSAGE_SEND, et le domaine COL n’existe pas dans la liste des permissions. Écarts relevés par comparaison avec generer-realm-gimac.py : tous les profils internes sans COL_MESSAGE_* ; RSSI sans CER_SECURITY_VERIFY ni HOM_EVIDENCE_SUBMIT ; chef du DES et chef de la DDRM sans HOM_LIAISON_OPEN, HOM_PROJECT_DESIGNATE, HOM_STEP_MANAGE ; ingénieur d’homologation avec HOM_LIAISON_OPEN en trop ; conducteur de projet sans CER_PV_ESTABLISH, HOM_OPERATION_VALIDATE, HOM_STEP_MANAGE ; correspondant sans CER_PORTAL_SIGN ni NOT_INBOX_READ. L’aperçu de la barre latérale d’un profil (ex. Normes et Agréments) omet donc la Messagerie.
- **Capture** : [051-superadmin-profil-superadmin-droits-clair.png](captures/administration/051-superadmin-profil-superadmin-droits-clair.png)
- **Cause probable** : frontend/packages/administration/src/services/simule/catalogue.ts (PERMISSIONS_REALM, PROFILS_REALM), annoncé « généré à partir du script du domaine », non régénéré.

### ADM-02 : Compte de démonstration de l’administrateur système absent de l’annuaire des comptes (mineure)

- **Reproduire** : Console, RSSI ou super administrateur : ouvrir `/console/comptes/u-admin-systeme`, ou chercher « abessolo » dans Comptes.
- **Observé** : « Le chargement a échoué. Ressource introuvable ». La liste compte 13 comptes sans aucun administrateur système (tableau de bord : « Comptes privilégiés 2 ») alors que le compte `admin.systeme` (Thierry Abessolo Nkoulou) se connecte à la console. Le cycle de vie d’un compte administrateur système et le refus au RSSI de modifier ce profil privilégié ne peuvent pas être recettés.
- **Capture** : [109-rssi-echec-priv-fiche.png](captures/administration/109-rssi-echec-priv-fiche.png)
- **Cause probable** : frontend/packages/administration/src/services/simule/donnees.ts (comptes et rattachements simulés sans u-admin-systeme).

### ADM-03 : Refus métier présenté comme une panne de la clé de sécurité (mineure)

- **Reproduire** : Super administrateur : rendre externe le profil Normes et Agréments puis signer ; ou RSSI : nouveau profil avec « Journal d’administration » puis signer.
- **Observé** : Le message du service s’affiche, mais le bloc de signature passe à l’état d’erreur « La clé de sécurité n’a pas répondu. Vérifiez que la clé est branchée puis réessayez » avec « Réessayer », alors que la clé a répondu : l’utilisateur est orienté vers une mauvaise cause et invité à resigner un acte qui sera refusé de nouveau.
- **Capture** : [055-superadmin-profil-externe-refus-clair.png](captures/administration/055-superadmin-profil-externe-refus-clair.png)
- **Cause probable** : frontend/packages/design-system/src/conteneurs/SignDialog/SignDialog.tsx (tout rejet de la signature mis à l’état `error` de SignatureField.tsx, sans distinguer le refus du service).

### ADM-04 : RSSI : composition d’un profil privilégié refusée seulement après la signature (mineure)

- **Reproduire** : Console, RSSI : Profils et permissions, « Nouveau profil », cocher « Journal d’administration », continuer, signer.
- **Observé** : La case est cochable et l’alerte « Profil privilégié » s’affiche, mais rien n’indique que le geste est réservé au super administrateur ; le refus « L’attribution ou la modification d’un profil privilégié est réservée au super administrateur » n’arrive qu’après le motif et la clé (et avec l’erreur de ADM-03). Le refus est bien fait par le service (règle respectée).
- **Capture** : [115-rssi-nouveau-profil-privilegie-resultat-clair.png](captures/administration/115-rssi-nouveau-profil-privilegie-resultat-clair.png)
- **Cause probable** : frontend/packages/administration/src/composants/profils.tsx et fonctionnalites.tsx (cases des fonctionnalités privilégiées non désactivées sans IAM_ROLE_PRIVILEGED_GRANT), à confirmer.

### ADM-05 : Libellé incohérent « Supervision » et « Centre de contrôle » (mineure)

- **Reproduire** : Application interne, n’importe quel profil de plateforme : barre latérale, groupe Plateforme.
- **Observé** : L’entrée s’appelle « Supervision » dans l’application interne, « Centre de contrôle » dans la console, et l’écran ouvert est titré « Centre de contrôle » dans les deux.
- **Capture** : [015-adminsys-des-barre-laterale-clair.png](captures/administration/015-adminsys-des-barre-laterale-clair.png)
- **Cause probable** : frontend/packages/lot1/src/i18n/ecrans/administration.ts (`nav.plateforme.supervision`).

### ADM-06 : Aperçu de la barre latérale d’un nouveau profil incomplet (mineure)

- **Reproduire** : Console, super administrateur : « Nouveau profil », cocher un droit de Liaisons et parcours et « Journal d’administration ».
- **Observé** : La modale annonce « Messagerie, Mon activité et notifications lui sont ajoutées », mais l’aperçu « 3 entrées visibles » n’affiche ni Messagerie, ni Notifications, ni Mon activité.
- **Capture** : [060-superadmin-nouveau-profil-privilegie-alerte-clair.png](captures/administration/060-superadmin-nouveau-profil-privilegie-alerte-clair.png)
- **Cause probable** : frontend/packages/administration/src/composants/fonctionnalites.tsx (aperçu calculé sans les permissions ajoutées d’office), à confirmer.

### ADM-07 : Élision manquante dans les noms accessibles de l’arbre des fonctionnalités (mineure)

- **Reproduire** : Console : composition d’un profil, boutons de dépliage des droits.
- **Observé** : Noms lus par les lecteurs d’écran : « Droits de Agenda », « Droits de Institutions membres », « Droits de Organisation », « Droits de Administration de tous les domaines ».
- **Capture** : [056-superadmin-profil-composition-arbre-apercu-clair.png](captures/administration/056-superadmin-profil-composition-arbre-apercu-clair.png)
- **Cause probable** : frontend/packages/administration/src/i18n/fr.ts (`console.profils.fct.droitsDe` : « Droits de {{fonctionnalite}} »).

### ADM-08 : Identifiant technique affiché pendant le chargement de l’organigramme (mineure)

- **Reproduire** : Console : Organigramme, lire la carte du DES aussitôt l’écran affiché.
- **Observé** : Tant que les comptes ne sont pas chargés, la carte indique « Chef : u-chef-des · 0 personne » (identifiant technique et effectif faux) avant « Chef : Christian Ndong Obiang · 5 personnes ».
- **Capture** : [083-superadmin-organigramme-arbre-clair.png](captures/administration/083-superadmin-organigramme-arbre-clair.png)
- **Cause probable** : frontend/packages/administration/src/pages/OrganigrammePage.tsx et composants/organigramme.ts (repli sur l’identifiant quand le compte n’est pas encore connu), à confirmer.

### ADM-09 : Permissions des profils de démonstration de la console différentes du domaine (mineure)

- **Reproduire** : Comparer `packages/administration/src/profilsDemo.ts` à generer-realm-gimac.py.
- **Observé** : RSSI de la console sans CER_SECURITY_VERIFY ni HOM_EVIDENCE_SUBMIT ; super administrateur sans HOM_REFERENTIEL_READ_ALL, HOM_REFERENTIEL_MIGRATE ni REF_METIER_ADMIN. Ces profils priment dans la console sur ceux de @gimac/lot1 (alignés). Sans effet visible sur les écrans de la console aujourd’hui, mais toute fonctionnalité future fondée sur ces permissions divergerait entre la console et l’application interne.
- **Cause probable** : frontend/packages/administration/src/profilsDemo.ts.

Aucune anomalie bloquante ni majeure : tous les flux d’administration aboutissent et toutes les règles de droits vérifiées sont respectées (dernier super administrateur protégé, octroi privilégié réservé au super administrateur, admin système sans administration des comptes, RSSI en lecture sur l’infrastructure et la journalisation).

## 5. Remarques

- Les libellés des profils (« Chef du DES », « Sécurité des SI »…) restent en français dans l’interface anglaise : ce sont des données servies par MS-IAM (libellé modifiable, D-86), non des textes de l’interface ; aucune clé de traduction brute relevée en en-US.
- L’état des services simulés vit dans la page : un rechargement complet le réinitialise (par exemple le journal d’administration ne garde pas un acte fait avant un rechargement). Comportement de démonstration, non retenu comme anomalie.
- Aucune erreur JavaScript ni réponse réseau en erreur relevée dans la console du navigateur pendant les parcours.
- Scripts de la recette (hors dépôt) : scratchpad `recette/administration/01-acces.mjs` à `10-connexion.mjs`.

## 6. Index des captures

- [001-superadmin-console-barre-laterale-clair.png](captures/administration/001-superadmin-console-barre-laterale-clair.png)
- [002-superadmin-des-barre-laterale-clair.png](captures/administration/002-superadmin-des-barre-laterale-clair.png)
- [003-rssi-console-barre-laterale-clair.png](captures/administration/003-rssi-console-barre-laterale-clair.png)
- [004-rssi-des-barre-laterale-clair.png](captures/administration/004-rssi-des-barre-laterale-clair.png)
- [005-adminsys-console-barre-laterale-clair.png](captures/administration/005-adminsys-console-barre-laterale-clair.png)
- [006-adminsys-console-acces-consoleComptes.png](captures/administration/006-adminsys-console-acces-consoleComptes.png)
- [007-adminsys-console-acces-consoleDemandes.png](captures/administration/007-adminsys-console-acces-consoleDemandes.png)
- [008-adminsys-console-acces-consoleProfils.png](captures/administration/008-adminsys-console-acces-consoleProfils.png)
- [009-adminsys-console-acces-consoleMatrice.png](captures/administration/009-adminsys-console-acces-consoleMatrice.png)
- [010-adminsys-console-acces-consoleOrganisation.png](captures/administration/010-adminsys-console-acces-consoleOrganisation.png)
- [011-adminsys-console-acces-consoleOrganigramme.png](captures/administration/011-adminsys-console-acces-consoleOrganigramme.png)
- [012-adminsys-console-acces-consoleJournal.png](captures/administration/012-adminsys-console-acces-consoleJournal.png)
- [013-adminsys-console-acces-consolePolitique.png](captures/administration/013-adminsys-console-acces-consolePolitique.png)
- [014-adminsys-console-acces-consoleCourriels.png](captures/administration/014-adminsys-console-acces-consoleCourriels.png)
- [015-adminsys-des-barre-laterale-clair.png](captures/administration/015-adminsys-des-barre-laterale-clair.png)
- [016-adminsys-des-acces-adminComptes.png](captures/administration/016-adminsys-des-acces-adminComptes.png)
- [017-adminsys-des-acces-adminDemandes.png](captures/administration/017-adminsys-des-acces-adminDemandes.png)
- [018-adminsys-des-acces-adminProfils.png](captures/administration/018-adminsys-des-acces-adminProfils.png)
- [019-adminsys-des-acces-adminMatrice.png](captures/administration/019-adminsys-des-acces-adminMatrice.png)
- [020-adminsys-des-acces-adminOrganisation.png](captures/administration/020-adminsys-des-acces-adminOrganisation.png)
- [021-adminsys-des-acces-adminOrganigramme.png](captures/administration/021-adminsys-des-acces-adminOrganigramme.png)
- [022-adminsys-des-acces-adminJournal.png](captures/administration/022-adminsys-des-acces-adminJournal.png)
- [023-adminsys-des-acces-adminPolitique.png](captures/administration/023-adminsys-des-acces-adminPolitique.png)
- [024-adminsys-des-acces-courriels.png](captures/administration/024-adminsys-des-acces-courriels.png)
- [025-superadmin-console-tableau-de-bord-plein-clair.png](captures/administration/025-superadmin-console-tableau-de-bord-plein-clair.png)
- [026-superadmin-comptes-filtre-sans2fa-clair.png](captures/administration/026-superadmin-comptes-filtre-sans2fa-clair.png)
- [027-superadmin-comptes-filtre-profil-clair.png](captures/administration/027-superadmin-comptes-filtre-profil-clair.png)
- [028-superadmin-creation-compte-vide-clair.png](captures/administration/028-superadmin-creation-compte-vide-clair.png)
- [029-superadmin-creation-compte-saisie-clair.png](captures/administration/029-superadmin-creation-compte-saisie-clair.png)
- [030-superadmin-creation-compte-secret-clair.png](captures/administration/030-superadmin-creation-compte-secret-clair.png)
- [031-superadmin-fiche-compte-cree-clair.png](captures/administration/031-superadmin-fiche-compte-cree-clair.png)
- [032-superadmin-rattachement-modale-clair.png](captures/administration/032-superadmin-rattachement-modale-clair.png)
- [033-superadmin-rattachement-enregistre-clair.png](captures/administration/033-superadmin-rattachement-enregistre-clair.png)
- [034-superadmin-profils-signature-clair.png](captures/administration/034-superadmin-profils-signature-clair.png)
- [035-superadmin-profils-modifies-clair.png](captures/administration/035-superadmin-profils-modifies-clair.png)
- [036-superadmin-profil-privilegie-octroi-clair.png](captures/administration/036-superadmin-profil-privilegie-octroi-clair.png)
- [037-superadmin-profil-privilegie-octroye-clair.png](captures/administration/037-superadmin-profil-privilegie-octroye-clair.png)
- [038-superadmin-securite-exiger-actions-clair.png](captures/administration/038-superadmin-securite-exiger-actions-clair.png)
- [039-superadmin-securite-2fa-motif-obligatoire-clair.png](captures/administration/039-superadmin-securite-2fa-motif-obligatoire-clair.png)
- [040-superadmin-securite-mdp-provisoire-clair.png](captures/administration/040-superadmin-securite-mdp-provisoire-clair.png)
- [041-superadmin-sessions-fermees-clair.png](captures/administration/041-superadmin-sessions-fermees-clair.png)
- [042-superadmin-fiche-abandon-clair.png](captures/administration/042-superadmin-fiche-abandon-clair.png)
- [043-superadmin-compte-desactive-clair.png](captures/administration/043-superadmin-compte-desactive-clair.png)
- [044-superadmin-fiche-journal-clair.png](captures/administration/044-superadmin-fiche-journal-clair.png)
- [045-superadmin-suppression-confirmation-clair.png](captures/administration/045-superadmin-suppression-confirmation-clair.png)
- [046-superadmin-compte-supprime-clair.png](captures/administration/046-superadmin-compte-supprime-clair.png)
- [047-superadmin-invitation-envoyee-clair.png](captures/administration/047-superadmin-invitation-envoyee-clair.png)
- [048-superadmin-propre-compte-clair.png](captures/administration/048-superadmin-propre-compte-clair.png)
- [049-superadmin-securite-2fa-reinitialise-clair.png](captures/administration/049-superadmin-securite-2fa-reinitialise-clair.png)
- [050-superadmin-profils-liste-clair.png](captures/administration/050-superadmin-profils-liste-clair.png)
- [051-superadmin-profil-superadmin-droits-clair.png](captures/administration/051-superadmin-profil-superadmin-droits-clair.png)
- [052-superadmin-profil-modifier-libelle-clair.png](captures/administration/052-superadmin-profil-modifier-libelle-clair.png)
- [053-superadmin-profil-libelle-enregistre-clair.png](captures/administration/053-superadmin-profil-libelle-enregistre-clair.png)
- [054-superadmin-profil-externe-modale-clair.png](captures/administration/054-superadmin-profil-externe-modale-clair.png)
- [055-superadmin-profil-externe-refus-clair.png](captures/administration/055-superadmin-profil-externe-refus-clair.png)
- [056-superadmin-profil-composition-arbre-apercu-clair.png](captures/administration/056-superadmin-profil-composition-arbre-apercu-clair.png)
- [057-superadmin-profil-composition-enregistree-clair.png](captures/administration/057-superadmin-profil-composition-enregistree-clair.png)
- [058-superadmin-profils-comparaison-clair.png](captures/administration/058-superadmin-profils-comparaison-clair.png)
- [059-superadmin-nouveau-profil-vide-clair.png](captures/administration/059-superadmin-nouveau-profil-vide-clair.png)
- [060-superadmin-nouveau-profil-privilegie-alerte-clair.png](captures/administration/060-superadmin-nouveau-profil-privilegie-alerte-clair.png)
- [061-superadmin-nouveau-profil-cree-clair.png](captures/administration/061-superadmin-nouveau-profil-cree-clair.png)
- [062-superadmin-profil-supprime-clair.png](captures/administration/062-superadmin-profil-supprime-clair.png)
- [063-superadmin-catalogue-fonctionnalites-clair.png](captures/administration/063-superadmin-catalogue-fonctionnalites-clair.png)
- [064-superadmin-catalogue-saisie-invalide-clair.png](captures/administration/064-superadmin-catalogue-saisie-invalide-clair.png)
- [065-superadmin-catalogue-modale-renommer-clair.png](captures/administration/065-superadmin-catalogue-modale-renommer-clair.png)
- [066-superadmin-catalogue-barre-renommee-clair.png](captures/administration/066-superadmin-catalogue-barre-renommee-clair.png)
- [067-superadmin-catalogue-journal-masque-clair.png](captures/administration/067-superadmin-catalogue-journal-masque-clair.png)
- [068-superadmin-catalogue-journal-refuse-clair.png](captures/administration/068-superadmin-catalogue-journal-refuse-clair.png)
- [069-superadmin-matrice-clair.png](captures/administration/069-superadmin-matrice-clair.png)
- [070-superadmin-matrice-ligne-portees-clair.png](captures/administration/070-superadmin-matrice-ligne-portees-clair.png)
- [071-superadmin-matrice-enregistree-clair.png](captures/administration/071-superadmin-matrice-enregistree-clair.png)
- [072-superadmin-demandes-liste-clair.png](captures/administration/072-superadmin-demandes-liste-clair.png)
- [073-superadmin-demande-compte-fiche-clair.png](captures/administration/073-superadmin-demande-compte-fiche-clair.png)
- [074-superadmin-demande-acceptee-clair.png](captures/administration/074-superadmin-demande-acceptee-clair.png)
- [075-superadmin-demande-refus-motif-clair.png](captures/administration/075-superadmin-demande-refus-motif-clair.png)
- [076-superadmin-demandes-toutes-clair.png](captures/administration/076-superadmin-demandes-toutes-clair.png)
- [077-superadmin-comptes-apres-acceptation-clair.png](captures/administration/077-superadmin-comptes-apres-acceptation-clair.png)
- [078-superadmin-organisation-arbre-clair.png](captures/administration/078-superadmin-organisation-arbre-clair.png)
- [079-superadmin-organisation-modale-clair.png](captures/administration/079-superadmin-organisation-modale-clair.png)
- [080-superadmin-organisation-modifiee-clair.png](captures/administration/080-superadmin-organisation-modifiee-clair.png)
- [081-superadmin-organisation-nouvelle-vide-clair.png](captures/administration/081-superadmin-organisation-nouvelle-vide-clair.png)
- [082-superadmin-organisation-unite-creee-clair.png](captures/administration/082-superadmin-organisation-unite-creee-clair.png)
- [083-superadmin-organigramme-arbre-clair.png](captures/administration/083-superadmin-organigramme-arbre-clair.png)
- [084-superadmin-organigramme-deplacer-confirmation-clair.png](captures/administration/084-superadmin-organigramme-deplacer-confirmation-clair.png)
- [085-superadmin-organigramme-n1-clair.png](captures/administration/085-superadmin-organigramme-n1-clair.png)
- [086-superadmin-organigramme-panneau-pouvoirs-clair.png](captures/administration/086-superadmin-organigramme-panneau-pouvoirs-clair.png)
- [087-superadmin-organigramme-sous-unite-desactivee-clair.png](captures/administration/087-superadmin-organigramme-sous-unite-desactivee-clair.png)
- [088-superadmin-organigramme-vue-liste-clair.png](captures/administration/088-superadmin-organigramme-vue-liste-clair.png)
- [089-superadmin-organigramme-chef-designe-clair.png](captures/administration/089-superadmin-organigramme-chef-designe-clair.png)
- [090-superadmin-politique-clair.png](captures/administration/090-superadmin-politique-clair.png)
- [091-superadmin-politique-saisie-invalide-clair.png](captures/administration/091-superadmin-politique-saisie-invalide-clair.png)
- [092-superadmin-politique-refus-service-clair.png](captures/administration/092-superadmin-politique-refus-service-clair.png)
- [093-superadmin-politique-appliquee-clair.png](captures/administration/093-superadmin-politique-appliquee-clair.png)
- [094-superadmin-courriels-clair.png](captures/administration/094-superadmin-courriels-clair.png)
- [095-superadmin-courriels-port-invalide-clair.png](captures/administration/095-superadmin-courriels-port-invalide-clair.png)
- [096-superadmin-courriels-enregistres-clair.png](captures/administration/096-superadmin-courriels-enregistres-clair.png)
- [097-superadmin-courriels-test-clair.png](captures/administration/097-superadmin-courriels-test-clair.png)
- [098-superadmin-journal-clair.png](captures/administration/098-superadmin-journal-clair.png)
- [099-superadmin-journal-filtre-clair.png](captures/administration/099-superadmin-journal-filtre-clair.png)
- [100-superadmin-configuration-clair.png](captures/administration/100-superadmin-configuration-clair.png)
- [101-superadmin-journalisation-modale-clair.png](captures/administration/101-superadmin-journalisation-modale-clair.png)
- [102-superadmin-journalisation-applique-clair.png](captures/administration/102-superadmin-journalisation-applique-clair.png)
- [103-superadmin-supervision-clair.png](captures/administration/103-superadmin-supervision-clair.png)
- [104-superadmin-infrastructure-clair.png](captures/administration/104-superadmin-infrastructure-clair.png)
- [105-superadmin-infrastructure-redemarrage-clair.png](captures/administration/105-superadmin-infrastructure-redemarrage-clair.png)
- [106-rssi-console-tableau-de-bord-plein-clair.png](captures/administration/106-rssi-console-tableau-de-bord-plein-clair.png)
- [107-rssi-creation-compte-sans-privilegie-clair.png](captures/administration/107-rssi-creation-compte-sans-privilegie-clair.png)
- [108-rssi-creation-compte-ok-clair.png](captures/administration/108-rssi-creation-compte-ok-clair.png)
- [109-rssi-echec-priv-fiche.png](captures/administration/109-rssi-echec-priv-fiche.png)
- [110-rssi-dernier-superadmin-desactivation-refusee-clair.png](captures/administration/110-rssi-dernier-superadmin-desactivation-refusee-clair.png)
- [111-rssi-dernier-superadmin-profils-clair.png](captures/administration/111-rssi-dernier-superadmin-profils-clair.png)
- [112-rssi-dernier-superadmin-suppression-refusee-clair.png](captures/administration/112-rssi-dernier-superadmin-suppression-refusee-clair.png)
- [113-rssi-profil-superadmin-consultation-clair.png](captures/administration/113-rssi-profil-superadmin-consultation-clair.png)
- [114-rssi-nouveau-profil-privilegie-clair.png](captures/administration/114-rssi-nouveau-profil-privilegie-clair.png)
- [115-rssi-nouveau-profil-privilegie-resultat-clair.png](captures/administration/115-rssi-nouveau-profil-privilegie-resultat-clair.png)
- [116-rssi-journalisation-lecture-seule-clair.png](captures/administration/116-rssi-journalisation-lecture-seule-clair.png)
- [117-rssi-configuration-clair.png](captures/administration/117-rssi-configuration-clair.png)
- [118-rssi-infrastructure-lecture-seule-clair.png](captures/administration/118-rssi-infrastructure-lecture-seule-clair.png)
- [119-rssi-infrastructure-journaux-clair.png](captures/administration/119-rssi-infrastructure-journaux-clair.png)
- [120-rssi-politique-appliquee-clair.png](captures/administration/120-rssi-politique-appliquee-clair.png)
- [121-rssi-courriels-clair.png](captures/administration/121-rssi-courriels-clair.png)
- [122-rssi-matrice-clair.png](captures/administration/122-rssi-matrice-clair.png)
- [123-rssi-demande-acceptee-clair.png](captures/administration/123-rssi-demande-acceptee-clair.png)
- [124-rssi-organigramme-clair.png](captures/administration/124-rssi-organigramme-clair.png)
- [125-adminsys-console-tableau-de-bord-plein-clair.png](captures/administration/125-adminsys-console-tableau-de-bord-plein-clair.png)
- [126-adminsys-infrastructure-journaux-pilotage-clair.png](captures/administration/126-adminsys-infrastructure-journaux-pilotage-clair.png)
- [127-adminsys-infrastructure-clair.png](captures/administration/127-adminsys-infrastructure-clair.png)
- [128-adminsys-infrastructure-courbes-clair.png](captures/administration/128-adminsys-infrastructure-courbes-clair.png)
- [129-adminsys-infrastructure-pods-clair.png](captures/administration/129-adminsys-infrastructure-pods-clair.png)
- [130-adminsys-infrastructure-evenements-clair.png](captures/administration/130-adminsys-infrastructure-evenements-clair.png)
- [131-adminsys-infrastructure-redemarrer-signature-clair.png](captures/administration/131-adminsys-infrastructure-redemarrer-signature-clair.png)
- [132-adminsys-infrastructure-historique-clair.png](captures/administration/132-adminsys-infrastructure-historique-clair.png)
- [133-adminsys-infrastructure-echelle-refus-clair.png](captures/administration/133-adminsys-infrastructure-echelle-refus-clair.png)
- [134-adminsys-infrastructure-arretee-clair.png](captures/administration/134-adminsys-infrastructure-arretee-clair.png)
- [135-adminsys-infrastructure-noeuds-clair.png](captures/administration/135-adminsys-infrastructure-noeuds-clair.png)
- [136-adminsys-journalisation-clair.png](captures/administration/136-adminsys-journalisation-clair.png)
- [137-adminsys-configuration-clair.png](captures/administration/137-adminsys-configuration-clair.png)
- [138-adminsys-supervision-clair.png](captures/administration/138-adminsys-supervision-clair.png)
- [139-adminsys-recherche-globale-clair.png](captures/administration/139-adminsys-recherche-globale-clair.png)
- [140-adminsys-des-tableau-de-bord-clair.png](captures/administration/140-adminsys-des-tableau-de-bord-clair.png)
- [141-adminsys-des-infrastructure-clair.png](captures/administration/141-adminsys-des-infrastructure-clair.png)
- [142-adminsys-des-refus-autres-lots-clair.png](captures/administration/142-adminsys-des-refus-autres-lots-clair.png)
- [143-superadmin-mon-compte-clair.png](captures/administration/143-superadmin-mon-compte-clair.png)
- [144-superadmin-mon-compte-mdp-faible-clair.png](captures/administration/144-superadmin-mon-compte-mdp-faible-clair.png)
- [146-superadmin-messagerie-console-clair.png](captures/administration/146-superadmin-messagerie-console-clair.png)
- [147-superadmin-notifications-cloche-clair.png](captures/administration/147-superadmin-notifications-cloche-clair.png)
- [148-superadmin-notifications-page-clair.png](captures/administration/148-superadmin-notifications-page-clair.png)
- [149-superadmin-des-equipe-refusee-clair.png](captures/administration/149-superadmin-des-equipe-refusee-clair.png)
- [150-superadmin-des-creation-compte-clair.png](captures/administration/150-superadmin-des-creation-compte-clair.png)
- [151-superadmin-lien-console-clair.png](captures/administration/151-superadmin-lien-console-clair.png)
- [153-rssi-des-tableau-de-bord-sombre.png](captures/administration/153-rssi-des-tableau-de-bord-sombre.png)
- [154-rssi-des-comptes-sombre.png](captures/administration/154-rssi-des-comptes-sombre.png)
- [155-rssi-des-profils-sombre.png](captures/administration/155-rssi-des-profils-sombre.png)
- [156-rssi-des-matrice-sombre.png](captures/administration/156-rssi-des-matrice-sombre.png)
- [157-rssi-des-organigramme-sombre.png](captures/administration/157-rssi-des-organigramme-sombre.png)
- [158-rssi-des-politique-sombre.png](captures/administration/158-rssi-des-politique-sombre.png)
- [159-rssi-des-infrastructure-sombre.png](captures/administration/159-rssi-des-infrastructure-sombre.png)
- [160-rssi-des-fiche-securite-sombre.png](captures/administration/160-rssi-des-fiche-securite-sombre.png)
- [161-superadmin-console-tableau-de-bord-sombre.png](captures/administration/161-superadmin-console-tableau-de-bord-sombre.png)
- [162-superadmin-console-comptes-sombre.png](captures/administration/162-superadmin-console-comptes-sombre.png)
- [163-superadmin-console-profils-sombre.png](captures/administration/163-superadmin-console-profils-sombre.png)
- [164-superadmin-console-organigramme-sombre.png](captures/administration/164-superadmin-console-organigramme-sombre.png)
- [165-superadmin-console-infrastructure-sombre.png](captures/administration/165-superadmin-console-infrastructure-sombre.png)
- [166-superadmin-console-politique-sombre.png](captures/administration/166-superadmin-console-politique-sombre.png)
- [167-superadmin-modale-profils-sombre.png](captures/administration/167-superadmin-modale-profils-sombre.png)
- [168-superadmin-console-tableau-de-bord-mobile.png](captures/administration/168-superadmin-console-tableau-de-bord-mobile.png)
- [169-superadmin-console-comptes-mobile.png](captures/administration/169-superadmin-console-comptes-mobile.png)
- [170-superadmin-console-fiche-mobile.png](captures/administration/170-superadmin-console-fiche-mobile.png)
- [171-superadmin-console-matrice-mobile.png](captures/administration/171-superadmin-console-matrice-mobile.png)
- [172-superadmin-console-menu-mobile.png](captures/administration/172-superadmin-console-menu-mobile.png)
- [173-superadmin-console-tableau-de-bord-en.png](captures/administration/173-superadmin-console-tableau-de-bord-en.png)
- [174-superadmin-console-fiche-en.png](captures/administration/174-superadmin-console-fiche-en.png)
- [175-superadmin-console-matrice-en.png](captures/administration/175-superadmin-console-matrice-en.png)
- [176-superadmin-console-infrastructure-en.png](captures/administration/176-superadmin-console-infrastructure-en.png)
- [177-superadmin-mon-compte-mdp-faible-refuse-clair.png](captures/administration/177-superadmin-mon-compte-mdp-faible-refuse-clair.png)
- [178-superadmin-mon-compte-totp-clair.png](captures/administration/178-superadmin-mon-compte-totp-clair.png)
- [179-superadmin-mon-compte-securite-clair.png](captures/administration/179-superadmin-mon-compte-securite-clair.png)
- [180-superadmin-connexion-console-clair.png](captures/administration/180-superadmin-connexion-console-clair.png)
- [181-superadmin-connexion-portail-refusee-clair.png](captures/administration/181-superadmin-connexion-portail-refusee-clair.png)
- [182-rssi-connexion-console-clair.png](captures/administration/182-rssi-connexion-console-clair.png)
- [183-adminsys-connexion-console-clair.png](captures/administration/183-adminsys-connexion-console-clair.png)

## 7. Correction

Corrections du 08/10/2026 (registre : [corrections/administration.json](corrections/administration.json), captures « après » : [captures/administration/corrections/](captures/administration/corrections/), refaites sur une construction isolée en services simulés, ports 6600 à 6602, mêmes profils, thème clair, fr-FR).

| Anomalie | Correctif | Test | Après |
|---|---|---|---|
| ADM-01 | Plus de copie manuelle : `infra/keycloak/generer-realm-gimac.py` écrit `frontend/packages/lot1/src/domaine-gimac.json` (permissions, profils composites ROLE_*, libellé, mission, caractère externe, lus dans le domaine généré comme MS-IAM les lit dans Keycloak) ; `packages/lot1/src/domaine.ts` l'expose ; le serveur simulé de la console (`services/simule/catalogue.ts`, `index.ts`) en dépend. Ses règles ne citent plus aucun nom de profil : privilégié = composition IAM_* ou PLATFORM_* (créer, recomposer, attribuer, y compris rendre privilégié un profil livré), externe = attribut du profil, dernier compte portant IAM_ROLE_PRIVILEGED_GRANT protégé (désactivation, suppression, retrait du profil ou de la permission). La matrice des pouvoirs écarte les profils externes par leur attribut. | `administration/src/services/simule/catalogue.test.ts` : relit `infra/keycloak/gimac-realm.json` et échoue dès que le catalogue simulé, les profils servis ou les profils de démonstration divergent ; le générateur vérifie aussi la concordance. | [ADM-01](captures/administration/corrections/ADM-01-apres.png) : COL_MESSAGE_READ et COL_MESSAGE_SEND cochés pour le super administrateur |
| ADM-02 | Compte `admin.systeme` (Thierry Abessolo Nkoulou, u-admin-systeme, profil Administrateur système) dans l'annuaire simulé de la console, rattaché au Service Production informatique. | `catalogue.test.ts` (fiche lue, recherche « abessolo », retrait refusé au RSSI : 403 IAM_PRIVILEGED_PROFILE) ; décomptes mis à jour dans `simule.test.ts`, `ComptesPage.test.tsx`, `ProfilsOrganisation.test.tsx`. | [ADM-02](captures/administration/corrections/ADM-02-apres.png) |
| ADM-03 | Nouvel état `refused` du bloc de signature (`SignatureField.tsx`, `SignDialog.tsx`) : quand la clé a répondu et que le service refuse l'acte, « Acte refusé par le service », message du service, détail s'il diffère et « Code du refus : … », bouton « Fermer », sans « Réessayer ». `composants/actes.tsx` distingue la réponse de la clé du refus du service ; la panne de la clé garde son message. | `RecetteAdministration.test.tsx` (ADM-03, refus IAM_PROFIL_EXTERNE_TITULAIRES et panne de clé). | [ADM-03](captures/administration/corrections/ADM-03-apres.png) |
| ADM-04 | Sans IAM_ROLE_PRIVILEGED_GRANT, l'arbre des fonctionnalités (nouveau profil, fiche d'un profil) montre les droits privilégiés désactivés avec la raison « Réservé à qui peut attribuer les profils privilégiés » et un avertissement en tête ; la vue Droits ne les propose pas. Un droit déjà présent reste décochable. Le service refait le contrôle. | `RecetteAdministration.test.tsx` (ADM-04, RSSI et super administrateur). | [ADM-04](captures/administration/corrections/ADM-04-apres.png) |
| ADM-05 | Nom retenu, celui du catalogue des fonctionnalités : « Supervision » (FR), « Supervision » (EN), « Supervisión » (ES) pour l'entrée de l'application interne, l'entrée de la console et le titre de l'écran. | `SupervisionPage.test.tsx`, `InfrastructurePage.test.tsx` (titre « Supervision »). | [ADM-05](captures/administration/corrections/ADM-05-apres.png) |
| ADM-06 | L'aperçu de la barre latérale d'un nouveau profil compose les permissions que MS-IAM ajoute d'office (`PERMISSIONS_COMMUNES` : messagerie, Mon activité, notifications). | `RecetteAdministration.test.tsx` (ADM-06). | [ADM-06](captures/administration/corrections/ADM-06-apres.png) |
| ADM-07 | Règle générale d'élision et de contraction de « de » (`socle/src/i18n/elision.ts`, formateur i18next `{{nom, de}}` enregistré par `initI18n`) : élision devant une voyelle, « du » et « des » devant « le » et « les », h jamais élidé faute de distinguer h muet et h aspiré. Appliquée aux libellés français à nom variable de la console : droits d'une fonctionnalité, comparaison de deux profils, membres, pouvoirs, n+1, unité et chef d'une unité. | `socle/src/i18n/elision.test.ts`, `RecetteAdministration.test.tsx` (ADM-07). | [ADM-07](captures/administration/corrections/ADM-07-apres.png) : noms accessibles reportés dans l'encart |
| ADM-08 | Tant que les comptes ne sont pas lus, la carte d'une unité indique « Chef : chargement… » sans effectif ; plus jamais l'identifiant technique. | `RecetteAdministration.test.tsx` (ADM-08, lecture des comptes retenue). | [ADM-08](captures/administration/corrections/ADM-08-apres.png) : carte une fois les comptes lus (l'état « chargement… », fugace, est vérifié par le test) |
| ADM-09 | Profils de démonstration de la console (`administration/src/profilsDemo.ts`) et de l'application interne (`lot1/src/permissions.ts`, `PROFILS`) composés à partir du domaine généré ; la console reprend l'identité des comptes communs aux trois applications pour chaque profil privilégié, choisi par sa composition. | `catalogue.test.ts` (profils de démonstration = domaine Keycloak), `lot1/src/domaine.test.ts`. | Aucun effet visible (permissions sans écran dans la console) : pas de capture. |

**Gestes Docker** : l'infrastructure simulée savait déjà servir un orchestrateur Docker ; la démonstration le choisit désormais par la préférence `gm.orchestrateurDemo` du stockage local (`DOCKER`, `KUBERNETES` par défaut, ou `AUCUN` pour une exploitation non configurée) ou par `VITE_ORCHESTRATEUR_DEMO` à la construction (`orchestrateurDemo()`, `administration/src/services/simule/index.ts`). Les gestes Docker (redémarrer, arrêter, démarrer, sans échelle ni retour de version) se recettent donc en services simulés. Test : `catalogue.test.ts` (orchestrateur de la démonstration).

**Validation** : compilation TypeScript sans erreur de socle, design-system, compte, administration, console et portail (lot1 et application interne : seule erreur dans homologation-recette.test.ts, fichier d'un autre lot en cours) ; tests unitaires : administration 143/143 (dont catalogue.test.ts et RecetteAdministration.test.tsx), compte 45/45, console 8/8, socle (élision, tiroir) et lot1 (domaine.test.ts, permissions, fonctionnalités, administration) verts sur les fichiers touchés ; les échecs restants de lot1, du socle (connexion, sous charge), du design-system (finition des dates) et de l'application interne (instance de processus inconnue) relèvent de fichiers modifiés par les autres lots ; `python3 infra/keycloak/generer-realm-gimac.py` : gimac-realm.json et gimac-realm-production.json inchangés, domaine-gimac.json stable ; `infra/ci/valider-infra.sh` : contrôle du domaine Keycloak réussi (il compare désormais aussi domaine-gimac.json), deux échecs étrangers à ce lot (tests OPA, copies kubernetes-gimac) ; E2E sur ports isolés (6600, -fixadm) : accessibilite, administration, configuration-d91-d94, connexion, console-*, infrastructure*, profils-organisation, roles-redirections : 146 réussis, 10 échecs dont 4 corrigés ensuite dans les specs (identité du RSSI, 14 comptes, fenêtre « Acte refusé » à fermer : console-refus, console-rssi, console-super-admin repassés au vert) et 6 dans configuration-d91-d94 (parcours, portail et tâches à prendre, modifiés par les lots certification et quotidien). Sorties E2E supprimées, captures versionnées de e2e/captures remises à l'identique.
