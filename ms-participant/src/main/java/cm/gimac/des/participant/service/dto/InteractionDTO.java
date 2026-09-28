package cm.gimac.des.participant.service.dto;

import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.participant.domain.Interaction} entity.
 */
@Schema(description = "Journal des interactions, en ajout seul (PAR-10).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InteractionDTO implements Serializable {

    private UUID id;

    @NotNull
    private InteractionNature nature;

    @NotNull
    @Size(max = 500)
    private String summary;

    @NotNull
    private Instant occurredAt;

    @NotNull
    private UUID authorId;

    @Size(max = 100)
    @Schema(description = "Référence de l'objet : ticket, liaison, réunion ou pièce, par exemple PIE-2026-118.")
    private String reference;

    @Schema(description = "Identifiant de remise d'un signalement (algorithme 6.29).")
    private UUID notificationId;

    @Schema(description = "Identifiant CloudEvents, pour une interaction reçue par événement (idempotence).")
    private UUID sourceEventId;

    @NotNull
    private InstitutionDTO institution;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InteractionNature getNature() {
        return nature;
    }

    public void setNature(InteractionNature nature) {
        this.nature = nature;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public InstitutionDTO getInstitution() {
        return institution;
    }

    public void setInstitution(InstitutionDTO institution) {
        this.institution = institution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InteractionDTO)) {
            return false;
        }

        InteractionDTO interactionDTO = (InteractionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, interactionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InteractionDTO{" +
            "id='" + getId() + "'" +
            ", nature='" + getNature() + "'" +
            ", summary='" + getSummary() + "'" +
            ", occurredAt='" + getOccurredAt() + "'" +
            ", authorId='" + getAuthorId() + "'" +
            ", reference='" + getReference() + "'" +
            ", notificationId='" + getNotificationId() + "'" +
            ", sourceEventId='" + getSourceEventId() + "'" +
            ", institution=" + getInstitution() +
            "}";
    }
}
