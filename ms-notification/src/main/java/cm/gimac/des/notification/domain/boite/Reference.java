package cm.gimac.des.notification.domain.boite;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Objet désigné par une notification : service d'origine, type et identifiant (par exemple
 * collaboration, CONVERSATION, c-1). Sert de clé de consolidation et de marquage en lecture.
 */
@Embeddable
public class Reference {

    public static final String COLLABORATION = "collaboration";
    public static final String PILOTAGE = "pilotage";
    public static final String IAM = "iam";
    public static final String PARTICIPANT = "participant";
    public static final String TRANSITION = "TRANSITION";
    public static final String HOMOLOGATION = "homologation";
    public static final String MONITORING = "monitoring";
    public static final String SANDBOX = "sandbox";
    public static final String CONVERSATION = "CONVERSATION";
    public static final String MESSAGE = "MESSAGE";
    public static final String TACHE = "TACHE";
    public static final String RAPPORT = "RAPPORT";
    public static final String DEMANDE = "DEMANDE";
    public static final String PROCESSUS = "PROCESSUS";
    public static final String OPERATION = "OPERATION";
    public static final String LIAISON = "LIAISON";

    @Column(name = "reference_service", nullable = false, length = 40)
    private String service;

    @Column(name = "reference_type", nullable = false, length = 40)
    private String type;

    @Column(name = "reference_id", nullable = false, length = 80)
    private String id;

    protected Reference() {}

    public Reference(String service, String type, String id) {
        this.service = service;
        this.type = type;
        this.id = id;
    }

    public static Reference conversation(String id) {
        return new Reference(COLLABORATION, CONVERSATION, id);
    }

    /** Message précis d'une conversation (mention ou réponse, D-82). */
    public static Reference message(String id) {
        return new Reference(COLLABORATION, MESSAGE, id);
    }

    /** Vrai si la référence désigne une conversation de la messagerie. */
    public boolean estConversation() {
        return COLLABORATION.equals(service) && CONVERSATION.equals(type);
    }

    public static Reference tache(String id) {
        return new Reference(PILOTAGE, TACHE, id);
    }

    public static Reference rapport(String id) {
        return new Reference(PILOTAGE, RAPPORT, id);
    }

    /** Demande de notification de la sandbox (US-EXI-09) : modèle et identifiant de la demande. */
    public static Reference sandbox(String modele, String id) {
        return new Reference(SANDBOX, modele, id);
    }

    /** Objet du monitoring et des astreintes (D-116) : rapport, rapport quotidien d'une journée. */
    public static Reference monitoring(String type, String id) {
        return new Reference(MONITORING, type, id);
    }

    /** Demande à l'administration (MS-IAM, docs/lot1/pouvoirs-hierarchiques.md §4). */
    /** Proposition de transition de statut d'une institution (MS-PARTICIPANT, DDR-08). */
    public static Reference transition(String id) {
        return new Reference(PARTICIPANT, TRANSITION, id);
    }

    public static Reference demande(String id) {
        return new Reference(IAM, DEMANDE, id);
    }

    /** Instance d'un processus d'unité (MS-PILOTAGE, D-87). */
    public static Reference processus(String instanceId) {
        return new Reference(PILOTAGE, PROCESSUS, instanceId);
    }

    /** Opération du parcours d'une liaison (MS-HOMOLOGATION, D-93). */
    public static Reference operationHomologation(String operationId) {
        return new Reference(HOMOLOGATION, OPERATION, operationId);
    }

    /** Liaison d'homologation (jalon atteint, MS-HOMOLOGATION, D-93). */
    public static Reference liaisonHomologation(String liaisonId) {
        return new Reference(HOMOLOGATION, LIAISON, liaisonId);
    }

    public String getService() {
        return service;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reference r && Objects.equals(service, r.service) && Objects.equals(type, r.type) && Objects.equals(id, r.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, type, id);
    }

    @Override
    public String toString() {
        return service + "/" + type + "/" + id;
    }
}
