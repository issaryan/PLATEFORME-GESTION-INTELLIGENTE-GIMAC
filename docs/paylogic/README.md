# Façade ISO 20022 du GIMAC : vérification des correctifs annoncés par Paylogic

| | |
|---|---|
| Objet | Vérifier, par des tests réels, les correctifs de la checklist E1 à E11 et de la spécification « Instant Payment API ISO 20022 » v2.3 mise à jour, transmises par Paylogic |
| Environnement | Façade ISO 20022 de test du GIMAC (port 8445), membre authentifié 10025 |
| Date des essais | 05/11/2026, de 10 h 40 à 10 h 50 (heure de Douala) |
| Méthode | Les 22 cas de recette du 08/10/2026 rejoués à l'identique, plus des cas dédiés à chaque correctif ; réponses brutes conservées (pacs.002, pacs.028, acmt.024, corps HTTP) |
| Périmètre | Façade de production en test du GIMAC exploitée par Paylogic. Ce rapport ne porte pas sur la Sandbox GIMAC |

## 1. Synthèse

| Statut | Écarts |
|---|---|
| Conformes | E2, E3, E4 (sur ACSC et PDNG), E9 |
| Non conformes | **E1** (statut après doublon), **E7** (Request to Pay) |
| Partiel | **E6** (codes de rejet : quatre cas renvoient encore XX99) |
| À éprouver | E5 (troncature des noms longs) |
| Documentaires, sans test | E8, E10 |
| Ouvert | E11 (aucun participant ISO 20022 réel raccordé) |

La checklist de Paylogic marque E1 à E7 et E9 « Corrigé et testé en test — OK ». Les essais du 05/11 ne le confirment pas pour E1, E6 et E7.

## 2. Résultats par écart

| Écart | Ce qu'annonce Paylogic | Résultat réel du 05/11 | Verdict |
|---|---|---|---|
| E1 Statut après doublon | pacs.028 renvoie le statut de toute transaction, y compris après un doublon | Cas 5 (A2A) accepté ACSC, puis le même message rejoué est bien rejeté AM05. Mais **pacs.028 sur l'original rend RJCT X050 « No data found »**. Même réponse pour la transaction du cas 1 (BLCK). Les transactions PDNG (cas 3, 4, 7) sont, elles, bien relues ACSC | **Non conforme** |
| E2 acmt.024 interprotocole | Nom et identité du bénéficiaire restitués, y compris pour un portefeuille | acmt.023 sur un portefeuille du membre 12001 : acmt.024 Vrfctn=true avec le nom complet du titulaire | Conforme |
| E3 Message mal formé | Rejet maîtrisé « Invalid input message » | pacs.008 tronqué : HTTP 400, corps JSON `InvalidInputFile` « Invalid input message », sans erreur interne | Conforme |
| E4 ClrSysRef | Référence du switch renseignée dans le pacs.002 | ClrSysRef présent sur tous les ACSC et PDNG (cas 2 à 9, 14, 19 à 22) ; absent sur RJCT et BLCK | Conforme ; à préciser pour RJCT et BLCK |
| E5 Noms complets | Dbtr/Nm et Cdtr/Nm conservés intégralement | Nom complet restitué dans acmt.024 ; la troncature des noms longs n'a pas été provoquée faute de cas adapté | À éprouver |
| E6 Codes de rejet précis | Référentiel de codes complété (§12.3), plus de XX99 générique | Corrects : AM05 (doublon), RC10 (membre destinataire inconnu), AG01 (type de transaction refusé). **Encore XX99** : nombre de transactions incohérent (cas 13, AM18 attendu) ; montant négatif (cas 15) ; SchmeNm absent (cas 18, avec « Mandatory field 'SchmeNm' ») ; délai dépassé vers le destinataire (cas 1 et 10, BLCK « Timeout during send to payee », AB06 attendu selon §12.3) | **Partiel** |
| E7 Request to Pay | Flux pain.013 entrant, pain.014 en réponse | Le traitement pain.013 est bien déployé, mais refuse chaque message essayé : « Payment information is missing » ou « missing from member ». Il échoue ensuite par une erreur interne (NullPointerException dans `DocumentPain013.getInstdAgtId`) en construisant le pain.014 de rejet, d'où **HTTP 400 au lieu d'un pain.014 RJCT**. Neuf variantes essayées, toutes conformes à la §9.4 : schéma pain.013.001.10, request_to_pay, types WALLET et EWLT, InstdAmt, PmtInfId égal au MsgId, PmtMtd TRF, ChrgBr SLEV, débiteur au niveau PmtInf et créancier dans CdtTrfTx, type de paiement au niveau PmtInf puis au niveau de la transaction | **Non conforme** |
| E8 Profil pacs.008 | Différences entre l'ISO de base et le profil documentées (§4.4) | Documentation reçue ; aucun test spécifique | Documentaire |
| E9 IBAN A2W et W2A | IBAN accepté et mappé en compte bancaire | Cas 6 (W2A avec IBAN) : ACSC | Conforme |
| E10 Données client | Matrice sendercustomerdata / receivercustomerdata ajoutée | Documentation reçue ; aucun test spécifique | Documentaire |
| E11 Parcours bénéficiaire | Non traité par Paylogic | Non éprouvable : aucun participant ISO 20022 réel n'est raccordé | Ouvert |

## 3. Détail des 22 cas rejoués

| Cas | Parcours | Réponse pacs.002 | Code | ClrSysRef |
|---|---|---|---|---|
| 1 | W2W | BLCK | XX99 « Timeout during send to payee » | non |
| 2 | W2A | ACSC | | oui |
| 3 | A2W | PDNG, puis ACSC au pacs.028 | | oui |
| 4 | Achat marchand | PDNG, puis ACSC au pacs.028 | | oui |
| 5 | A2A | ACSC | | oui |
| 6 | W2A avec IBAN | ACSC | | oui |
| 7 | W2W | PDNG, puis ACSC au pacs.028 | | oui |
| 8 | Message expiré | PDNG | | oui |
| 9 | Asynchrone | PDNG | | oui |
| 10 | Suspect puis rejet | BLCK | XX99 « Timeout during send to payee » | non |
| 11 | Doublon | RJCT | AM05 | non |
| 12 | Membre destinataire inconnu | RJCT | RC10 « Invalid member » | non |
| 13 | NbOfTxs incohérent | RJCT | XX99 (AM18 attendu) | non |
| 14 | Côté bénéficiaire | ACSC | | oui |
| 15 | Montant négatif | RJCT | XX99 | non |
| 16 | Intention | RJCT | AG01 | non |
| 17 | Facture | RJCT | AG01 | non |
| 18 | SchmeNm absent | RJCT | XX99 « Mandatory field 'SchmeNm' » | non |
| 19 à 22 | Côté bénéficiaire (W2A, A2W, A2A, marchand) | ACSC | | oui |

Contrôles complémentaires : doublon du cas 5 RJCT AM05 ; pacs.008 tronqué HTTP 400 InvalidInputFile ; acmt.023 sur un portefeuille de 12001 Vrfctn=true ; pacs.028 sur les cas 1 et 5 RJCT X050, sur les cas 3, 4 et 7 ACSC.

## 4. Demandes à adresser à Paylogic

1. **E1** : corriger pacs.028 pour qu'il retrouve une transaction déjà traitée (ACSC ou BLCK), en particulier après un doublon, et indiquer la date de déploiement sur l'environnement de test.
2. **E6** : appliquer la §12.3 aux quatre cas encore en XX99. Préciser le code attendu pour chacun : AM18 pour un nombre de transactions incohérent ; le code d'un montant négatif ; celui d'un SchmeNm absent ; AB06 ou XX99 pour un délai dépassé vers le destinataire.
3. **E7** : fournir un exemple de pain.013 accepté sur l'environnement de test, avec le pain.014 attendu. Corriger l'erreur interne qui empêche de renvoyer un pain.014 de rejet.
4. **E4** : confirmer si ClrSysRef doit aussi être renseigné sur les réponses RJCT et BLCK.
5. **E5** : fournir le cas de test qui démontre la conservation des noms longs, ou nous indiquer la longueur à éprouver.
6. **E11** : convenir du calendrier de rejeu des parcours bénéficiaire avec les premiers participants ISO 20022 raccordés.

## 5. Pièces disponibles sur demande

Messages envoyés et réponses brutes de chaque cas, horodatés : 22 cas rejoués, doublon, message tronqué, acmt.023, neuf variantes pain.013, cinq pacs.028.

## Journal

| Date | Mise à jour |
|---|---|
| 06/11/2026 | Création du rapport à partir des essais réels du 05/11/2026 sur la façade de test |
