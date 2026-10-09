# Contre-recette sur la construction intégrée

Contre-recette des anomalies bloquantes et majeures de la recette exhaustive (docs/recette/*.md, registres
docs/recette/corrections/*.json), rejouées à la main sur la construction qui intègre les corrections des six lots.

## Construction et méthode

- Code : branche integration-d81-d90, dernier commit 4a15e77 (déployé), corrections de la recette par-dessus (non commitées),
  restes compris (CER-05 au portail, QUO-09 dans le texte des messages).
- Applications de recette reconstruites dans frontend/apps/<app> :
  `GIMAC_E2E=1 VITE_SERVICES=simule VITE_URL_DES=http://localhost:6100 VITE_URL_PORTAIL=http://localhost:6101 VITE_URL_CONSOLE=http://localhost:6102 VITE_PORT_DES=6100 VITE_PORT_PORTAIL=6101 VITE_PORT_CONSOLE=6102 npx vite build --outDir dist-recette --emptyOutDir`,
  servies par `vite preview --host 127.0.0.1` sur 6100 (application interne), 6101 (portail) et 6102 (console).
- Gestes rejoués dans Chromium, un contexte neuf par scénario (stockage vidé), locale fr-FR, fuseau Africa/Douala, 1440 × 900,
  thème clair ; changement d'acteur dans le même onglet en démonstration continue (D-98), connexion par le formulaire pour le
  compte de démonstration du service RH (QUO-01). Chaque anomalie est rejouée selon les étapes de son compte rendu, le résultat
  observé est relevé dans la page (textes, boutons offerts, adresse atteinte), puis capturé et relu.
- Captures : docs/recette/captures/contre-recette/<ID>.png.
- Deux passages : un premier sur la construction intégrée, puis, après le dernier correctif de l'interface (« Citer une liaison »
  réservé aux lecteurs des liaisons, ci-dessous), applications reconstruites, aperçus relancés et les 25 anomalies rejouées de
  nouveau ; les captures sont celles du second passage.

## Bilan

25 anomalies rejouées (2 bloquantes, 23 majeures ; le lot administration n'en compte aucune) : 25 conformes sur la construction
intégrée, aucune régression.

## Anomalies rejouées

| Anomalie | Gravité | Geste rejoué (profil) | Observé sur la construction intégrée | Verdict | Capture |
| --- | --- | --- | --- | --- | --- |
| ACC-01 Session existante non aiguillée | majeure | Onglet portant la session de démonstration du correspondant ouvert sur la console (6102) et sur l'application interne (6100) ; session de la collaboratrice ouverte sur le portail (6101) | Correspondant conduit au portail (« Mon espace de certification », Brice Nzeng Mba, Établissement de paiement A) depuis 6100 et 6102 ; collaboratrice conduite à l'application interne depuis 6101 ; plus aucun profil par défaut | Conforme | [ACC-01](captures/contre-recette/ACC-01.png) |
| ACC-02 Catalogue de la console simulée non régénéré | majeure | Super administrateur : console, composition du profil Chef du DES ; application interne, fonctionnalité `liaisons` masquée, motif, clé de sécurité | Composition du domaine (Messagerie 2 / 2, Lancer une certification présent) ; modification signée (« Signature confirmée »), `liaisons` « Masquée », aucun « Permission inconnue » | Conforme | [ACC-02](captures/contre-recette/ACC-02.png) |
| CER-01 Circuit du PV et refus CER_* invérifiables | majeure | Conducteur : établit le PV de LIA-2026-025 ; chef du DES : onglet Décision | Circuit configuré affiché dans l'ordre (1 participant au portail, 2 GIMAC) ; « Signer pour le GIMAC » non offert au chef du DES avant la signature du participant | Conforme | [CER-01](captures/contre-recette/CER-01.png) |
| CER-02 Visa attendu introuvable | majeure | Conformité : tableau de bord ; RSSI : attestations de LIA-2026-019 déclarées conformes et archivées ; conformité : Mes actions de certification | Compteur « Visas attendus » relié à l'onglet Décision de LIA-2026-019 ; tâche « Signer : Visa de la conformité » dans Mon travail ; ligne « À signer » dans Mes actions de certification | Conforme | [CER-02](captures/contre-recette/CER-02.png) |
| DDR-01 Maker-checker non appliqué | bloquante | Chef de la DDRM : propose l'adhésion de la Banque de Libreville, onglet Propositions de statut | Proposition « Proposée par Vous », en attente, sans Approuver ni Rejeter offerts au proposant (le refus PAR_SEPARATION_OF_DUTIES du service simulé est couvert par participant.test.ts) | Conforme | [DDR-01](captures/contre-recette/DDR-01.png) |
| DDR-02 Identifiants réattribués | bloquante | Chargée : crée Banque Atlantique du Littoral ; changement d'acteur et rechargement ; chef : crée Trésor public centrafricain | Identifiants distincts (INS-0074 puis INS-0079) ; la fiche INS-0074 reste celle de la Banque Atlantique du Littoral | Conforme | [DDR-02](captures/contre-recette/DDR-02.png) |
| DDR-03 Conditions d'adhésion sans contrôle | majeure | Chargée : conditions de Fintech Kribi Pay enregistrées vides, puis code « bal@01 ! » en adhésion indirecte sans marraine ni cantonnement | Refus sous les champs et résumé : code et mode obligatoires, motif du code, compte de cantonnement et banque marraine exigés (RG-12) ; rien n'est enregistré | Conforme | [DDR-03](captures/contre-recette/DDR-03.png) |
| DDR-04 Modification de fiche perdue en partie | majeure | Chargée : BIC, code BEAC, origine, date de prise de contact et sigle modifiés, page rechargée | Tous les champs enregistrés (FKPYCMCX, 10077, Courrier du directeur général, 15/09/2026, FKP) | Conforme | [DDR-04](captures/contre-recette/DDR-04.png) |
| DDR-05 Dépôt par un acteur non habilité | majeure | Chef de la DDRM : tiroir de l'opération 4 de LIA-2026-044 (R : chargée de la DDRM) | Aucun dépôt offert : « Aucune action ne vous revient sur cette opération » ; le dépôt de la chargée est tracé à son nom (HOM-03) | Conforme | [DDR-05](captures/contre-recette/DDR-05.png) |
| DDR-06 Affectation sans désignation | majeure | Chef de la DDRM : « Affecter les ressources » de LIA-2026-044, toutes les fonctions « Non affecté » | Refus avant envoi : « Désignez une personne pour chacune des 9 fonctions signalées », message sous chaque fonction, règle annoncée | Conforme | [DDR-06](captures/contre-recette/DDR-06.png) |
| DDR-07 Filtre Statut sans effet | majeure | Chargée : Institutions membres, Statut « En homologation » | Liste filtrée (2 institutions en homologation), valeur choisie gardée dans la liste déroulante | Conforme | [DDR-07](captures/contre-recette/DDR-07.png) |
| DDR-08 Acteurs non notifiés | majeure | Chargée : propose l'adhésion de la Banque de Libreville ; chef : rejette avec motif ; chargée : Notifications | « Proposition de statut refusée : Banque de Libreville », motif du refus, non lue, en tête de la liste de la chargée | Conforme | [DDR-08](captures/contre-recette/DDR-08.png) |
| DDR-09 Tâches confiées perdues entre acteurs | majeure | Chef : nouvelle tâche confiée à Fatimé Abakar Hissein ; changement d'acteur et rechargement ; chargée : Tâches | La tâche « Relancer la Banque de Libreville sur les statuts », confiée par Bertrand Nguema Obiang, figure dans Mes tâches de la chargée | Conforme | [DDR-09](captures/contre-recette/DDR-09.png) |
| DDR-10 Action d'un autre profil annoncée | majeure | Chef de la DDRM : Mes actions de certification | Seule son opération (affectation des ressources de LIA-2026-044) ; plus de « Signature du procès-verbal » | Conforme | [DDR-10](captures/contre-recette/DDR-10.png) |
| HOM-01 Publication d'un graphe invalide | majeure | Ingénieur : brouillon dérivé de l'Agrégateur v1, opération A4 supprimée, Publier la version | La confirmation annonce le refus ; publication refusée (« la publication est refusée »), la version 1 reste publiée | Conforme | [HOM-01](captures/contre-recette/HOM-01.png) |
| HOM-02 Refus GESTE_CERTIFICATION_ABSENT absent | majeure | Même brouillon, Contrôler | Anomalie « Geste de certification absent » en libellé français (« Procès-verbal de certification établi ») | Conforme | [HOM-02](captures/contre-recette/HOM-02.png) |
| HOM-03 RACI non imposé dans le tiroir | majeure | Pôle système puis Normes et Agréments : opération 4 de LIA-2026-044 ; la chargée dépose entre-temps | Pôle système : aucun dépôt ; chargée : dépôt accepté à son nom ; Normes et Agréments : ni Valider ni Renvoyer | Conforme | [HOM-03](captures/contre-recette/HOM-03.png) |
| HOM-04 Tâche à prendre par fonction absente | majeure | Après la réunion de lancement de LIA-2026-041, pôle réseau : Tâches, À prendre, « Prendre la tâche » | Tâche « Prendre la fonction Pôle réseau (LIA-2026-041) » offerte, prise (« Tâche prise ») ; Gouvernance : Pôle réseau tenu par Idriss Mahamat Saleh | Conforme | [HOM-04](captures/contre-recette/HOM-04.png) |
| HOM-05 Opération à livrable close dès le dépôt | majeure | Chargée : compte rendu de l'opération 7 de LIA-2026-041 déposé ; conducteur : visa | Opération « En signature » après le dépôt, visa du conducteur attendu ; close seulement après la signature du conducteur | Conforme | [HOM-05](captures/contre-recette/HOM-05.png) |
| HOM-06 Livrables portés non proposés | majeure | Ingénieur : Ajouter une opération, livrables L1 et L2 saisis, Signatures, Ajouter une signature | « Livrables portés » propose aussitôt L1 et L2 de la modale, puis ceux des opérations antérieures | Conforme | [HOM-06](captures/contre-recette/HOM-06.png) |
| QUO-01 Aucun compte du service RH | majeure | Collaboratrice : demande de congé ; connexion par le formulaire en t.bekolo, code à six chiffres ; étape ANALYSE | Session de Thierry Bekolo Ngoa, exécutant de « Vérification des droits à congé », « Terminer l'étape » offert | Conforme | [QUO-01](captures/contre-recette/QUO-01.png) |
| QUO-02 Réaffectation à la demandeuse | majeure | Direction générale : « Réaffecter l'étape » ANALYSE de la demande de congé | La demandeuse n'est ni proposée ni choisie d'office ; plus de « Seul porteur … choisi automatiquement » | Conforme | [QUO-02](captures/contre-recette/QUO-02.png) |
| QUO-03 Tâche à prendre sur « Instance introuvable » | majeure | Chef du DES : étape CONFIGURATION de « Accès VPN d'un participant » confiée au profil Responsable de service (aucun porteur au pôle réseau, 2 ailleurs), publiée ; collaboratrice : lance et termine la demande ; responsable de service : Tâches, À prendre, Ouvrir | L'instance s'ouvre (« VPN de recette pour la Banque B »), « Prendre l'étape » offert depuis l'instance | Conforme | [QUO-03](captures/contre-recette/QUO-03.png) |
| QUO-04 Pièce du formulaire non consultable | majeure | Collaboratrice : demande de congé avec note.pdf, « Ouvrir note.pdf » | Aperçu du PDF dans une fenêtre, avec « Télécharger » | Conforme | [QUO-04](captures/contre-recette/QUO-04.png) |
| QUO-05 Aucune notification d'étape | majeure | Collaboratrice : demande de changement d'adresse IP ; pôle réseau : cloche | « Étape à réaliser : Analyse technique », sous-titre de la demande et échéance, non lue | Conforme | [QUO-05](captures/contre-recette/QUO-05.png) |

## Écart relevé et corrigé pendant la contre-recette

- QUO-09 (mineure, prolongement) : la collaboratrice, qui ne lit pas les liaisons, se voyait encore offrir « Citer une liaison »
  dans la saisie d'un message, dont la recherche lui serait refusée. Le bouton n'est plus offert qu'à un lecteur des liaisons
  (`MessageriePage.tsx`) ; test unitaire (MessageriePage.test.tsx, QUO-09) et scénario E2E (messagerie.spec.ts, collaboratrice
  sans accès aux liaisons) ajoutés ; rejoué sur la construction finale.

Aucune régression des anomalies corrigées n'a été trouvée.

## Remarques hors anomalies rejouées

- Tiroir d'une opération en validation sans validateur désigné (LIA-2026-044, conducteur à désigner) : « Preuve déposée, en
  attente de la validation de Non désigné. » La formulation est antérieure aux corrections (OngletOperations.tsx, inchangé) ;
  à reprendre en mineure.
- Mes actions de certification du chef de la DDRM : la colonne « Actions possibles » de l'affectation des ressources est vide,
  le geste se fait depuis le dossier (comportement antérieur, identique à la capture après de DDR-10).
