package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.WipOverride} entity.
 */
@Schema(
    description = "Dérogation motivée à la limite d'en-cours (algorithme 6.54, étape 4) :\napprobateur distinct du demandeur, expiration au plus 72 h après l'octroi,\nune seule dérogation active par colonne."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class WipOverrideDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(description = "Demandeur (MS-IAM).", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID requesterId;

    @NotNull
    @Schema(
        description = "Approbateur, responsable ou chef du pôle, distinct du demandeur (MS-IAM).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID approverId;

    @NotNull
    @Size(max = 500)
    private String reason;

    @NotNull
    private Instant grantedAt;

    @NotNull
    private Instant expiresAt;

    @Schema(description = "Avis d'expiration émis par la veille quotidienne (étape 6).")
    private Instant expiryNotifiedAt;

    @NotNull
    private BoardColumnDTO boardColumn;

    @NotNull
    private TaskDTO task;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getExpiryNotifiedAt() {
        return expiryNotifiedAt;
    }

    public void setExpiryNotifiedAt(Instant expiryNotifiedAt) {
        this.expiryNotifiedAt = expiryNotifiedAt;
    }

    public BoardColumnDTO getBoardColumn() {
        return boardColumn;
    }

    public void setBoardColumn(BoardColumnDTO boardColumn) {
        this.boardColumn = boardColumn;
    }

    public TaskDTO getTask() {
        return task;
    }

    public void setTask(TaskDTO task) {
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WipOverrideDTO)) {
            return false;
        }

        WipOverrideDTO wipOverrideDTO = (WipOverrideDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, wipOverrideDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "WipOverrideDTO{" +
            "id='" + getId() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", reason='" + getReason() + "'" +
            ", grantedAt='" + getGrantedAt() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", expiryNotifiedAt='" + getExpiryNotifiedAt() + "'" +
            ", boardColumn=" + getBoardColumn() +
            ", task=" + getTask() +
            "}";
    }
}
