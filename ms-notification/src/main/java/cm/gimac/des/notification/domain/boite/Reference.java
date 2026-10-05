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
    public static final String CONVERSATION = "CONVERSATION";
    public static final String TACHE = "TACHE";
    public static final String RAPPORT = "RAPPORT";

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

    public static Reference tache(String id) {
        return new Reference(PILOTAGE, TACHE, id);
    }

    public static Reference rapport(String id) {
        return new Reference(PILOTAGE, RAPPORT, id);
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
