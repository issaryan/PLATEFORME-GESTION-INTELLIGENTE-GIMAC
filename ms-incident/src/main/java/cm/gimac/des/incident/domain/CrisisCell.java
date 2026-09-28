package cm.gimac.des.incident.domain;

import cm.gimac.des.incident.domain.enumeration.CrisisState;
import cm.gimac.des.incident.domain.enumeration.Severity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Cellule de crise (D-14, algorithme 6.42). Elle ne passe CLOSE que sur
 * rapport post-incident signé, rédigé au plus tard 5 jours ouvrés après la
 * résolution (RG-42), et ARCHIVEE lorsque l'archive scellée par
 * MS-COLLABORATION est référencée. Critères d'activation non débrayables (RG-40).
 */
@Entity
@Table(name = "crisis_cell")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisCell implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Référence lisible, par exemple CRI-2026-009.
     */
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^CRI-[0-9]{4}-[0-9]{3,}$")
    @Column(name = "reference", length = 20, nullable = false, unique = true)
    private String reference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private Severity level;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private CrisisState state;

    /**
     * Vrai pour une ouverture automatique à la qualification S1.
     */
    @NotNull
    @Column(name = "automatic_activation", nullable = false)
    private Boolean automaticActivation;

    /**
     * Motif de la décision du chef du DES pour un incident S2 ou un événement grave.
     */
    @Size(max = 1000)
    @Column(name = "activation_reason", length = 1000)
    private String activationReason;

    @Column(name = "convened_by")
    private UUID convenedBy;

    @NotNull
    @Column(name = "convened_at", nullable = false)
    private Instant convenedAt;

    /**
     * Échéance de quorum (convocation + 15 min), déclenchant la suppléance.
     */
    @NotNull
    @Column(name = "quorum_deadline", nullable = false)
    private Instant quorumDeadline;

    /**
     * Seuil du quorum pondéré : 4 par défaut (D-14).
     */
    @NotNull
    @Min(value = 1)
    @Column(name = "quorum_threshold", nullable = false)
    private Integer quorumThreshold;

    @Column(name = "constituted_at")
    private Instant constitutedAt;

    /**
     * Salon de crise ouvert par MS-COLLABORATION, par exemple SAL-CRISE-2026-009.
     */
    @Size(max = 50)
    @Column(name = "room_ref", length = 50)
    private String roomRef;

    /**
     * Rapport post-incident : échéance à 5 jours ouvrés de la résolution (RG-42).
     */
    @Column(name = "post_incident_report_due_at")
    private Instant postIncidentReportDueAt;

    @Size(max = 100)
    @Column(name = "post_incident_report_ref", length = 100)
    private String postIncidentReportRef;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "post_incident_report_digest", length = 64)
    private String postIncidentReportDigest;

    @Column(name = "post_incident_report_signed_by")
    private UUID postIncidentReportSignedBy;

    @Column(name = "post_incident_report_signed_at")
    private Instant postIncidentReportSignedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Archive des échanges scellée par MS-COLLABORATION.
     */
    @Size(max = 100)
    @Column(name = "archive_ref", length = 100)
    private String archiveRef;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Une seule cellule active par incident.
     */
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "ticket", "matrix" }, allowSetters = true)
    private Incident incident;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CrisisCell id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return this.reference;
    }

    public CrisisCell reference(String reference) {
        this.setReference(reference);
        return this;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Severity getLevel() {
        return this.level;
    }

    public CrisisCell level(Severity level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(Severity level) {
        this.level = level;
    }

    public CrisisState getState() {
        return this.state;
    }

    public CrisisCell state(CrisisState state) {
        this.setState(state);
        return this;
    }

    public void setState(CrisisState state) {
        this.state = state;
    }

    public Boolean getAutomaticActivation() {
        return this.automaticActivation;
    }

    public CrisisCell automaticActivation(Boolean automaticActivation) {
        this.setAutomaticActivation(automaticActivation);
        return this;
    }

    public void setAutomaticActivation(Boolean automaticActivation) {
        this.automaticActivation = automaticActivation;
    }

    public String getActivationReason() {
        return this.activationReason;
    }

    public CrisisCell activationReason(String activationReason) {
        this.setActivationReason(activationReason);
        return this;
    }

    public void setActivationReason(String activationReason) {
        this.activationReason = activationReason;
    }

    public UUID getConvenedBy() {
        return this.convenedBy;
    }

    public CrisisCell convenedBy(UUID convenedBy) {
        this.setConvenedBy(convenedBy);
        return this;
    }

    public void setConvenedBy(UUID convenedBy) {
        this.convenedBy = convenedBy;
    }

    public Instant getConvenedAt() {
        return this.convenedAt;
    }

    public CrisisCell convenedAt(Instant convenedAt) {
        this.setConvenedAt(convenedAt);
        return this;
    }

    public void setConvenedAt(Instant convenedAt) {
        this.convenedAt = convenedAt;
    }

    public Instant getQuorumDeadline() {
        return this.quorumDeadline;
    }

    public CrisisCell quorumDeadline(Instant quorumDeadline) {
        this.setQuorumDeadline(quorumDeadline);
        return this;
    }

    public void setQuorumDeadline(Instant quorumDeadline) {
        this.quorumDeadline = quorumDeadline;
    }

    public Integer getQuorumThreshold() {
        return this.quorumThreshold;
    }

    public CrisisCell quorumThreshold(Integer quorumThreshold) {
        this.setQuorumThreshold(quorumThreshold);
        return this;
    }

    public void setQuorumThreshold(Integer quorumThreshold) {
        this.quorumThreshold = quorumThreshold;
    }

    public Instant getConstitutedAt() {
        return this.constitutedAt;
    }

    public CrisisCell constitutedAt(Instant constitutedAt) {
        this.setConstitutedAt(constitutedAt);
        return this;
    }

    public void setConstitutedAt(Instant constitutedAt) {
        this.constitutedAt = constitutedAt;
    }

    public String getRoomRef() {
        return this.roomRef;
    }

    public CrisisCell roomRef(String roomRef) {
        this.setRoomRef(roomRef);
        return this;
    }

    public void setRoomRef(String roomRef) {
        this.roomRef = roomRef;
    }

    public Instant getPostIncidentReportDueAt() {
        return this.postIncidentReportDueAt;
    }

    public CrisisCell postIncidentReportDueAt(Instant postIncidentReportDueAt) {
        this.setPostIncidentReportDueAt(postIncidentReportDueAt);
        return this;
    }

    public void setPostIncidentReportDueAt(Instant postIncidentReportDueAt) {
        this.postIncidentReportDueAt = postIncidentReportDueAt;
    }

    public String getPostIncidentReportRef() {
        return this.postIncidentReportRef;
    }

    public CrisisCell postIncidentReportRef(String postIncidentReportRef) {
        this.setPostIncidentReportRef(postIncidentReportRef);
        return this;
    }

    public void setPostIncidentReportRef(String postIncidentReportRef) {
        this.postIncidentReportRef = postIncidentReportRef;
    }

    public String getPostIncidentReportDigest() {
        return this.postIncidentReportDigest;
    }

    public CrisisCell postIncidentReportDigest(String postIncidentReportDigest) {
        this.setPostIncidentReportDigest(postIncidentReportDigest);
        return this;
    }

    public void setPostIncidentReportDigest(String postIncidentReportDigest) {
        this.postIncidentReportDigest = postIncidentReportDigest;
    }

    public UUID getPostIncidentReportSignedBy() {
        return this.postIncidentReportSignedBy;
    }

    public CrisisCell postIncidentReportSignedBy(UUID postIncidentReportSignedBy) {
        this.setPostIncidentReportSignedBy(postIncidentReportSignedBy);
        return this;
    }

    public void setPostIncidentReportSignedBy(UUID postIncidentReportSignedBy) {
        this.postIncidentReportSignedBy = postIncidentReportSignedBy;
    }

    public Instant getPostIncidentReportSignedAt() {
        return this.postIncidentReportSignedAt;
    }

    public CrisisCell postIncidentReportSignedAt(Instant postIncidentReportSignedAt) {
        this.setPostIncidentReportSignedAt(postIncidentReportSignedAt);
        return this;
    }

    public void setPostIncidentReportSignedAt(Instant postIncidentReportSignedAt) {
        this.postIncidentReportSignedAt = postIncidentReportSignedAt;
    }

    public UUID getClosedBy() {
        return this.closedBy;
    }

    public CrisisCell closedBy(UUID closedBy) {
        this.setClosedBy(closedBy);
        return this;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return this.closedAt;
    }

    public CrisisCell closedAt(Instant closedAt) {
        this.setClosedAt(closedAt);
        return this;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getArchiveRef() {
        return this.archiveRef;
    }

    public CrisisCell archiveRef(String archiveRef) {
        this.setArchiveRef(archiveRef);
        return this;
    }

    public void setArchiveRef(String archiveRef) {
        this.archiveRef = archiveRef;
    }

    public Instant getArchivedAt() {
        return this.archivedAt;
    }

    public CrisisCell archivedAt(Instant archivedAt) {
        this.setArchivedAt(archivedAt);
        return this;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Incident getIncident() {
        return this.incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public CrisisCell incident(Incident incident) {
        this.setIncident(incident);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisCell)) {
            return false;
        }
        return getId() != null && getId().equals(((CrisisCell) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisCell{" +
            "id=" + getId() +
            ", reference='" + getReference() + "'" +
            ", level='" + getLevel() + "'" +
            ", state='" + getState() + "'" +
            ", automaticActivation='" + getAutomaticActivation() + "'" +
            ", activationReason='" + getActivationReason() + "'" +
            ", convenedBy='" + getConvenedBy() + "'" +
            ", convenedAt='" + getConvenedAt() + "'" +
            ", quorumDeadline='" + getQuorumDeadline() + "'" +
            ", quorumThreshold=" + getQuorumThreshold() +
            ", constitutedAt='" + getConstitutedAt() + "'" +
            ", roomRef='" + getRoomRef() + "'" +
            ", postIncidentReportDueAt='" + getPostIncidentReportDueAt() + "'" +
            ", postIncidentReportRef='" + getPostIncidentReportRef() + "'" +
            ", postIncidentReportDigest='" + getPostIncidentReportDigest() + "'" +
            ", postIncidentReportSignedBy='" + getPostIncidentReportSignedBy() + "'" +
            ", postIncidentReportSignedAt='" + getPostIncidentReportSignedAt() + "'" +
            ", closedBy='" + getClosedBy() + "'" +
            ", closedAt='" + getClosedAt() + "'" +
            ", archiveRef='" + getArchiveRef() + "'" +
            ", archivedAt='" + getArchivedAt() + "'" +
            "}";
    }
}
