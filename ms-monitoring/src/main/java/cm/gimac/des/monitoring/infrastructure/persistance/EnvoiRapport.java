package cm.gimac.des.monitoring.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Envoi d'un rapport à un destinataire choisi par son auteur (D-116, table monitoring_report_send). */
@Entity
@Table(name = "monitoring_report_send")
public class EnvoiRapport {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID rapportId;

    @Column(name = "recipient_id", nullable = false)
    private UUID destinataireId;

    @Column(name = "sent_at", nullable = false)
    private Instant le;

    @Column(name = "message", length = 500)
    private String message;

    protected EnvoiRapport() {}

    public EnvoiRapport(UUID rapportId, UUID destinataireId, Instant le, String message) {
        this.id = UUID.randomUUID();
        this.rapportId = rapportId;
        this.destinataireId = destinataireId;
        this.le = le;
        this.message = message;
    }

    public UUID getRapportId() {
        return rapportId;
    }

    public UUID getDestinataireId() {
        return destinataireId;
    }

    public Instant getLe() {
        return le;
    }

    public String getMessage() {
        return message;
    }
}
