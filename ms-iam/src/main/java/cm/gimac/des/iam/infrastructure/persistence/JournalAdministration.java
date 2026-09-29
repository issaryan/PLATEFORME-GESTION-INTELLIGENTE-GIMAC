package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Journal des actes d'administration de la plateforme (D-62) : qui a fait quoi, sur quel compte,
 * pourquoi. Écriture seule ; chaque entrée part aussi vers le registre d'audit par l'outbox.
 */
@Entity
@Table(name = "journal_administration")
public class JournalAdministration {

    @Id
    private UUID id;

    @Column(name = "horodatage", nullable = false)
    private Instant horodatage;

    @Column(name = "acteur_id", nullable = false, length = 100)
    private String acteurId;

    @Column(name = "acteur_nom", length = 255)
    private String acteurNom;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "cible_id", length = 100)
    private String cibleId;

    @Column(name = "cible_libelle", length = 255)
    private String cibleLibelle;

    @Column(name = "motif", length = 1000)
    private String motif;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    protected JournalAdministration() {}

    public JournalAdministration(Instant horodatage, String acteurId, String acteurNom, String action, String cibleId, String cibleLibelle, String motif, String details, String correlationId) {
        this.id = UUID.randomUUID();
        this.horodatage = horodatage;
        this.acteurId = acteurId;
        this.acteurNom = acteurNom;
        this.action = action;
        this.cibleId = cibleId;
        this.cibleLibelle = cibleLibelle;
        this.motif = motif;
        this.details = details;
        this.correlationId = correlationId;
    }

    public UUID getId() { return id; }
    public Instant getHorodatage() { return horodatage; }
    public String getActeurId() { return acteurId; }
    public String getActeurNom() { return acteurNom; }
    public String getAction() { return action; }
    public String getCibleId() { return cibleId; }
    public String getCibleLibelle() { return cibleLibelle; }
    public String getMotif() { return motif; }
    public String getDetails() { return details; }
    public String getCorrelationId() { return correlationId; }
}
