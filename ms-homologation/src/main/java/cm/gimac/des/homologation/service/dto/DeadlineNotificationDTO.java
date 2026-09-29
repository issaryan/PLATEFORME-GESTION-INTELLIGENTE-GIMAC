package cm.gimac.des.homologation.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.DeadlineNotification} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeadlineNotificationDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID operationId;

    @NotNull
    @Size(max = 30)
    private String seuil;

    @NotNull
    private Instant dateEnvoi;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public String getSeuil() {
        return seuil;
    }

    public void setSeuil(String seuil) {
        this.seuil = seuil;
    }

    public Instant getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(Instant dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeadlineNotificationDTO)) {
            return false;
        }

        DeadlineNotificationDTO deadlineNotificationDTO = (DeadlineNotificationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, deadlineNotificationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeadlineNotificationDTO{" +
            "id='" + getId() + "'" +
            ", operationId='" + getOperationId() + "'" +
            ", seuil='" + getSeuil() + "'" +
            ", dateEnvoi='" + getDateEnvoi() + "'" +
            "}";
    }
}
