package cm.gimac.des.monitoring.infrastructure.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Rapport de monitoring ou d'astreinte rattaché au programme (D-116, table monitoring_report). */
@Entity
@Table(name = "monitoring_report")
public class RapportMonitoring {

    /** Nature du rapport : quotidien de 8 h, d'une vacation, d'une astreinte, de fin, ou autre. */
    public enum Nature {
        QUOTIDIEN,
        VACATION,
        ASTREINTE,
        FIN,
        AUTRE,
    }

    /** Pièce déposée dans MS-DOCUMENT. */
    public record Piece(String id, String nom, long taille, String type) {}

    @Id
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 20)
    private Nature nature;

    @Column(name = "title", nullable = false, length = 160)
    private String titre;

    @Column(name = "summary", length = 2000)
    private String resume;

    @Column(name = "report_date")
    private LocalDate date;

    @Column(name = "shift_id")
    private UUID vacationId;

    @Column(name = "assignment_id")
    private UUID affectationId;

    @Column(name = "author_id", nullable = false)
    private UUID auteurId;

    @Column(name = "deposited_at", nullable = false)
    private Instant deposeLe;

    @Column(name = "due_at")
    private Instant echeance;

    @Column(name = "late", nullable = false)
    private boolean enRetard;

    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId;

    @Column(name = "document_name", nullable = false)
    private String documentNom;

    @Column(name = "document_size", nullable = false)
    private long documentTaille;

    @Column(name = "document_type", nullable = false, length = 120)
    private String documentType;

    protected RapportMonitoring() {}

    public RapportMonitoring(
        Nature nature,
        String titre,
        String resume,
        LocalDate date,
        UUID vacationId,
        UUID affectationId,
        UUID auteurId,
        Instant deposeLe,
        Instant echeance,
        Piece piece
    ) {
        this.id = UUID.randomUUID();
        this.nature = nature;
        this.titre = titre;
        this.resume = resume;
        this.date = date;
        this.vacationId = vacationId;
        this.affectationId = affectationId;
        this.auteurId = auteurId;
        this.deposeLe = deposeLe;
        this.echeance = echeance;
        this.enRetard = echeance != null && deposeLe.isAfter(echeance);
        this.documentId = piece.id();
        this.documentNom = piece.nom();
        this.documentTaille = piece.taille();
        this.documentType = piece.type();
    }

    public UUID getId() {
        return id;
    }

    public Nature getNature() {
        return nature;
    }

    public String getTitre() {
        return titre;
    }

    public String getResume() {
        return resume;
    }

    public LocalDate getDate() {
        return date;
    }

    public UUID getVacationId() {
        return vacationId;
    }

    public UUID getAffectationId() {
        return affectationId;
    }

    public UUID getAuteurId() {
        return auteurId;
    }

    public Instant getDeposeLe() {
        return deposeLe;
    }

    public Instant getEcheance() {
        return echeance;
    }

    public boolean isEnRetard() {
        return enRetard;
    }

    public Piece getPiece() {
        return new Piece(documentId, documentNom, documentTaille, documentType);
    }
}
