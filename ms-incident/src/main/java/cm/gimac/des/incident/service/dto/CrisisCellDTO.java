package cm.gimac.des.incident.service.dto;

import cm.gimac.des.incident.domain.enumeration.CrisisState;
import cm.gimac.des.incident.domain.enumeration.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.CrisisCell} entity.
 */
@Schema(
    description = "Cellule de crise (D-14, algorithme 6.42). Elle ne passe CLOSE que sur\nrapport post-incident signé, rédigé au plus tard 5 jours ouvrés après la\nrésolution (RG-42), et ARCHIVEE lorsque l'archive scellée par\nMS-COLLABORATION est référencée. Critères d'activation non débrayables (RG-40)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisCellDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^CRI-[0-9]{4}-[0-9]{3,}$")
    @Schema(description = "Référence lisible, par exemple CRI-2026-009.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reference;

    @NotNull
    private Severity level;

    @NotNull
    private CrisisState state;

    @NotNull
    @Schema(description = "Vrai pour une ouverture automatique à la qualification S1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean automaticActivation;

    @Size(max = 1000)
    @Schema(description = "Motif de la décision du chef du DES pour un incident S2 ou un événement grave.")
    private String activationReason;

    private UUID convenedBy;

    @NotNull
    private Instant convenedAt;

    @NotNull
    @Schema(
        description = "Échéance de quorum (convocation + 15 min), déclenchant la suppléance.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant quorumDeadline;

    @NotNull
    @Min(value = 1)
    @Schema(description = "Seuil du quorum pondéré : 4 par défaut (D-14).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quorumThreshold;

    private Instant constitutedAt;

    @Size(max = 50)
    @Schema(description = "Salon de crise ouvert par MS-COLLABORATION, par exemple SAL-CRISE-2026-009.")
    private String roomRef;

    @Schema(description = "Rapport post-incident : échéance à 5 jours ouvrés de la résolution (RG-42).")
    private Instant postIncidentReportDueAt;

    @Size(max = 100)
    private String postIncidentReportRef;

    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String postIncidentReportDigest;

    private UUID postIncidentReportSignedBy;

    private Instant postIncidentReportSignedAt;

    private UUID closedBy;

    private Instant closedAt;

    @Size(max = 100)
    @Schema(description = "Archive des échanges scellée par MS-COLLABORATION.")
    private String archiveRef;

    private Instant archivedAt;

    @NotNull
    @Schema(description = "Une seule cellule active par incident.")
    private IncidentDTO incident;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Severity getLevel() {
        return level;
    }

    public void setLevel(Severity level) {
        this.level = level;
    }

    public CrisisState getState() {
        return state;
    }

    public void setState(CrisisState state) {
        this.state = state;
    }

    public Boolean getAutomaticActivation() {
        return automaticActivation;
    }

    public void setAutomaticActivation(Boolean automaticActivation) {
        this.automaticActivation = automaticActivation;
    }

    public String getActivationReason() {
        return activationReason;
    }

    public void setActivationReason(String activationReason) {
        this.activationReason = activationReason;
    }

    public UUID getConvenedBy() {
        return convenedBy;
    }

    public void setConvenedBy(UUID convenedBy) {
        this.convenedBy = convenedBy;
    }

    public Instant getConvenedAt() {
        return convenedAt;
    }

    public void setConvenedAt(Instant convenedAt) {
        this.convenedAt = convenedAt;
    }

    public Instant getQuorumDeadline() {
        return quorumDeadline;
    }

    public void setQuorumDeadline(Instant quorumDeadline) {
        this.quorumDeadline = quorumDeadline;
    }

    public Integer getQuorumThreshold() {
        return quorumThreshold;
    }

    public void setQuorumThreshold(Integer quorumThreshold) {
        this.quorumThreshold = quorumThreshold;
    }

    public Instant getConstitutedAt() {
        return constitutedAt;
    }

    public void setConstitutedAt(Instant constitutedAt) {
        this.constitutedAt = constitutedAt;
    }

    public String getRoomRef() {
        return roomRef;
    }

    public void setRoomRef(String roomRef) {
        this.roomRef = roomRef;
    }

    public Instant getPostIncidentReportDueAt() {
        return postIncidentReportDueAt;
    }

    public void setPostIncidentReportDueAt(Instant postIncidentReportDueAt) {
        this.postIncidentReportDueAt = postIncidentReportDueAt;
    }

    public String getPostIncidentReportRef() {
        return postIncidentReportRef;
    }

    public void setPostIncidentReportRef(String postIncidentReportRef) {
        this.postIncidentReportRef = postIncidentReportRef;
    }

    public String getPostIncidentReportDigest() {
        return postIncidentReportDigest;
    }

    public void setPostIncidentReportDigest(String postIncidentReportDigest) {
        this.postIncidentReportDigest = postIncidentReportDigest;
    }

    public UUID getPostIncidentReportSignedBy() {
        return postIncidentReportSignedBy;
    }

    public void setPostIncidentReportSignedBy(UUID postIncidentReportSignedBy) {
        this.postIncidentReportSignedBy = postIncidentReportSignedBy;
    }

    public Instant getPostIncidentReportSignedAt() {
        return postIncidentReportSignedAt;
    }

    public void setPostIncidentReportSignedAt(Instant postIncidentReportSignedAt) {
        this.postIncidentReportSignedAt = postIncidentReportSignedAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getArchiveRef() {
        return archiveRef;
    }

    public void setArchiveRef(String archiveRef) {
        this.archiveRef = archiveRef;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public IncidentDTO getIncident() {
        return incident;
    }

    public void setIncident(IncidentDTO incident) {
        this.incident = incident;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisCellDTO)) {
            return false;
        }

        CrisisCellDTO crisisCellDTO = (CrisisCellDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, crisisCellDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisCellDTO{" +
            "id='" + getId() + "'" +
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
            ", incident=" + getIncident() +
            "}";
    }
}
