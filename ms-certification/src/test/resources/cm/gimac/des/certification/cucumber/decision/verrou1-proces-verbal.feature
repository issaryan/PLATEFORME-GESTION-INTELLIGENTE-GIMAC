# language: fr
# REC-GIMAC-01 D-56, verrou 1 : procès-verbal de certification (CR-CERT rangs 17 à 19), établi par le
# conducteur de projet à 100 % de couverture constatée, signé par le GIMAC et par le participant, chacun
# après réauthentification forte (D-35). Règles et codes de refus : serveur de substitution de l'interface.
Fonctionnalité: Verrou 1 : procès-verbal de certification

  Scénario: Nominal : procès-verbal établi à 100 % de couverture constatée
    Étant donné une liaison "CARTE_INTERBANCAIRE" dont la couverture de certification constatée vaut 100 %
    Quand le conducteur de projet consulte le procès-verbal
    Alors les actions permises sont "ETABLIR"
    Quand le conducteur de projet établit le procès-verbal
    Alors le procès-verbal est à l'état "ETABLI" avec une empreinte
    Et l'événement "certification.pv-established.v1" est écrit dans l'outbox

  Scénario: Erreur : couverture de certification incomplète
    Étant donné une liaison "PAIEMENT_MOBILE" dont la couverture de certification constatée vaut 75 %
    Quand le conducteur de projet consulte le procès-verbal
    Alors les actions permises sont "ETABLIR_BLOQUE"
    Quand le conducteur de projet établit le procès-verbal
    Alors la réponse a le statut 409 et le code "CER_COVERAGE_INCOMPLETE"

  Scénario: Nominal : double signature du GIMAC et du participant avec réauthentification forte
    Étant donné une liaison "CARTE_INTERBANCAIRE" dont le procès-verbal est établi
    Quand le chef du DES signe le procès-verbal sans réauthentification
    Alors la réponse a le statut 401 et le code "IAM_REAUTH_REQUIRED"
    Quand le chef du DES signe le procès-verbal avec son assertion FIDO2
    Alors le procès-verbal est à l'état "SIGNE_GIMAC"
    Quand le chef du DES signe le procès-verbal avec son assertion FIDO2
    Alors la réponse a le statut 409 et le code "CER_PV_NOT_SIGNABLE"
    Quand le participant signe le procès-verbal au portail avec son assertion FIDO2
    Alors le procès-verbal est à l'état "SIGNE"
    Et l'événement "certification.pv-signed.v1" est écrit dans l'outbox
    Et chaque signature est scellée par un cachet horodaté et immuable

  Scénario: Sécurité : portail cloisonné à l'institution du jeton
    Étant donné une liaison "CARTE_INTERBANCAIRE" dont le procès-verbal est établi
    Quand le correspondant d'une autre institution consulte le procès-verbal au portail
    Alors la réponse a le statut 404 et le code "HOM_LIAISON_NOT_FOUND"
