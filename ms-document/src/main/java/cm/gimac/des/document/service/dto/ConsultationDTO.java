package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.ConsultationAction;
import cm.gimac.des.document.domain.enumeration.ConsultationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.Consultation} entity.
 */
@Schema(
    description = "Journal de consultation en insertion seule (RG-06, algorithme 6.19) :\ninscrit avant toute restitution, y compris les refus."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsultationDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @Schema(description = "Institution du sujet, pour un représentant d'un participant.")
    private UUID institutionId;

    @NotNull
    private ConsultationAction action;

    @NotNull
    private ConsultationChannel channel;

    @NotNull
    private Instant consultedAt;

    @NotNull
    private UUID correlationId;

    @NotNull
    @Schema(
        description = "Faux pour une consultation refusée, journalisée et publiée elle aussi.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean granted;

    @Size(max = 255)
    private String refusalReason;

    @NotNull
    private DocumentDTO document;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public ConsultationAction getAction() {
        return action;
    }

    public void setAction(ConsultationAction action) {
        this.action = action;
    }

    public ConsultationChannel getChannel() {
        return channel;
    }

    public void setChannel(ConsultationChannel channel) {
        this.channel = channel;
    }

    public Instant getConsultedAt() {
        return consultedAt;
    }

    public void setConsultedAt(Instant consultedAt) {
        this.consultedAt = consultedAt;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public DocumentDTO getDocument() {
        return document;
    }

    public void setDocument(DocumentDTO document) {
        this.document = document;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsultationDTO)) {
            return false;
        }

        ConsultationDTO consultationDTO = (ConsultationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, consultationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsultationDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", action='" + getAction() + "'" +
            ", channel='" + getChannel() + "'" +
            ", consultedAt='" + getConsultedAt() + "'" +
            ", correlationId='" + getCorrelationId() + "'" +
            ", granted='" + getGranted() + "'" +
            ", refusalReason='" + getRefusalReason() + "'" +
            ", document=" + getDocument() +
            "}";
    }
}
