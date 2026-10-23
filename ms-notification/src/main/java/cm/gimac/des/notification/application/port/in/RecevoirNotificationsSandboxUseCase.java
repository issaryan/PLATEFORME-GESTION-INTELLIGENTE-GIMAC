package cm.gimac.des.notification.application.port.in;

import java.time.Instant;
import java.util.List;

/**
 * Demandes de notification de la Sandbox GIMAC (US-EXI-09, docs/lot1/sandbox-existant-adapte.md §11), événements
 * {@code sbx-<service>.notification-requested.v1} des onze services : une notification SANDBOX par destinataire résolu,
 * selon ses préférences, jamais regroupée ; chaque notification publie notification.inapp-created.v1 par l'outbox. Une
 * demande d'un modèle sans règle part au rebut avec son motif et lève une alerte d'exploitation.
 */
public interface RecevoirNotificationsSandboxUseCase {
    /**
     * Demande reçue : identifiant de l'événement, service émetteur ({@code sbx-espace}…), modèle, destinataires (comptes
     * ou {@code ROLE:<rôle>}), code participant et code de l'espace, instant de la demande.
     */
    record Demande(String evenementId, String service, String modele, List<String> destinataires, String codeParticipant, String codeEspace, Instant le) {}

    /** Destinataire résolu à blanc : compte, couche du lien (portail ou DES), titre et lien qui seraient remis. */
    record Resolution(String compteId, boolean portail, String titre, String lien) {}

    /** Modèle du catalogue : code, titre et sous-titre (gabarits), portée. */
    record ModeleVue(String code, String titre, String sousTitre, String portee) {}

    /** Rend le nombre de notifications créées ; modèle sans règle : rebut et alerte, 0. */
    int demander(Demande demande);

    /** Résolution à blanc d'une demande (NOT-05) : rien n'est créé ni publié ; modèle sans règle : 400. */
    List<Resolution> simuler(String modele, List<String> destinataires, String codeParticipant);

    /** Catalogue des modèles de la sandbox. */
    List<ModeleVue> modeles();
}
