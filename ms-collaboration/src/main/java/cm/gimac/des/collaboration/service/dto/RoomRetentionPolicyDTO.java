package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.RetentionAnchor;
import cm.gimac.des.collaboration.domain.enumeration.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.RoomRetentionPolicy} entity.
 */
@Schema(
    description = "Politique de rétention paramétrée par type de salon (D-11). Valeurs par\ndéfaut du CC (tableau 6.66) : QUART 180 jours ; THEME 1 an ; INCIDENT 1 an\naprès clôture du ticket ; CRISE 90 jours après scellement, archive 10 ans."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RoomRetentionPolicyDTO implements Serializable {

    private UUID id;

    @NotNull
    private RoomType roomType;

    @NotNull
    @Schema(
        description = "Durée de conservation des messages chiffrés sur le serveur de messagerie.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Duration messageRetention;

    @NotNull
    private RetentionAnchor anchor;

    @NotNull
    @Schema(
        description = "Archivage par le terminal d'archivage (D-28 : salons de crise et salons liés à un ticket).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean archived;

    @Min(value = 1)
    @Max(value = 99)
    @Schema(description = "Rétention de l'archive en années ; 10 ans au moins pour une archive de crise (D-11, RG-53).")
    private Integer archiveRetentionYears;

    @NotNull
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Duration getMessageRetention() {
        return messageRetention;
    }

    public void setMessageRetention(Duration messageRetention) {
        this.messageRetention = messageRetention;
    }

    public RetentionAnchor getAnchor() {
        return anchor;
    }

    public void setAnchor(RetentionAnchor anchor) {
        this.anchor = anchor;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Integer getArchiveRetentionYears() {
        return archiveRetentionYears;
    }

    public void setArchiveRetentionYears(Integer archiveRetentionYears) {
        this.archiveRetentionYears = archiveRetentionYears;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomRetentionPolicyDTO)) {
            return false;
        }

        RoomRetentionPolicyDTO roomRetentionPolicyDTO = (RoomRetentionPolicyDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, roomRetentionPolicyDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RoomRetentionPolicyDTO{" +
            "id='" + getId() + "'" +
            ", roomType='" + getRoomType() + "'" +
            ", messageRetention='" + getMessageRetention() + "'" +
            ", anchor='" + getAnchor() + "'" +
            ", archived='" + getArchived() + "'" +
            ", archiveRetentionYears=" + getArchiveRetentionYears() +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
