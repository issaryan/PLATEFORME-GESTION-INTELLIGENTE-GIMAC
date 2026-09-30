# language: fr
# REC-GIMAC-01 D-56, verrou 2 : approbation du changement par la sécurité des SI (CR-CERT rangs 20 à 26).
# Dossier de changement, fiche de changement lue et jamais refaite, attestations exigées selon l'activité
# (PCI DSS carte et délégataire ; ISO 27001 et GSMA mobile ; COBAC fintech), vérification avec rejet
# motivé puis archivage ; visas optionnels de la conformité et de la Direction Générale.
Fonctionnalité: Verrou 2 : approbation de la sécurité

  Scénario: Nominal : approbation complète avec les attestations d'une fintech de paiement mobile
    Étant donné une liaison "PAIEMENT_MOBILE" d'une fintech dont le procès-verbal est signé des deux parties
    Alors les attestations exigées sont "ISO_27001,GSMA,COBAC"
    Quand la production monétique constitue le dossier de changement
    Et le participant dépose les attestations exigées au portail
    Et la sécurité des SI vérifie conformes toutes les attestations
    Et la sécurité des SI archive les attestations
    Et la production monétique référence la fiche de changement "CHG-2026-0412"
    Alors l'approbation de la sécurité est à l'état "APPROUVEE"
    Et l'événement "certification.attestations-archived.v1" est écrit dans l'outbox
    Et l'événement "certification.security-approved.v1" est écrit dans l'outbox

  Scénario: Erreur : rejet motivé d'une attestation
    Étant donné une liaison "CARTE_INTERBANCAIRE" dont le procès-verbal est signé des deux parties
    Et le participant a déposé l'attestation "PCI_DSS"
    Quand la sécurité des SI rejette l'attestation "PCI_DSS" avec le motif ""
    Alors la réponse a le statut 422 et le code "CER_REJECTION_REASON_REQUIRED"
    Quand la sécurité des SI rejette l'attestation "PCI_DSS" avec le motif "Attestation expirée"
    Alors l'attestation "PCI_DSS" est de nouveau attendue avec le motif "Attestation expirée"
    Quand la sécurité des SI archive les attestations
    Alors la réponse a le statut 409 et le code "CER_ATTESTATIONS_INCOMPLETE"

  Scénario: Nominal : visas de la conformité et de la Direction Générale insérés avant l'approbation
    Étant donné une liaison "DELEGATAIRE" dont le procès-verbal est signé des deux parties
    Et la production monétique a constitué le dossier de changement
    Et le participant dépose les attestations exigées au portail
    Et la sécurité des SI vérifie conformes toutes les attestations
    Et la sécurité des SI archive les attestations
    Quand la production monétique référence la fiche de changement "CHG-2026-0413"
    Alors l'approbation de la sécurité est à l'état "VERIFIE_ARCHIVE"
    Quand la conformité signe son visa favorable avec son assertion FIDO2
    Et la Direction Générale signe son visa favorable avec son assertion FIDO2
    Et la production monétique relit la fiche de changement
    Alors l'approbation de la sécurité est à l'état "APPROUVEE"

  Scénario: Erreur : visa non requis pour ce type de projet
    Étant donné une liaison "CARTE_INTERBANCAIRE" dont le procès-verbal est signé des deux parties
    Quand la Direction Générale signe son visa favorable avec son assertion FIDO2
    Alors la réponse a le statut 409 et le code "CER_VISA_NOT_REQUIRED"
