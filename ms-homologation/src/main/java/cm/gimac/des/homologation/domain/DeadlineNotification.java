package cm.gimac.des.homologation.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A DeadlineNotification.
 */
@Entity
@Table(name = "deadline_notification")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DeadlineNotification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @NotNull
    @Size(max = 30)
    @Column(name = "seuil", length = 30, nullable = false)
    private String seuil;

    @NotNull
    @Column(name = "date_envoi", nullable = false)
    private Instant dateEnvoi;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public DeadlineNotification id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOperationId() {
        return this.operationId;
    }

    public DeadlineNotification operationId(UUID operationId) {
        this.setOperationId(operationId);
        return this;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public String getSeuil() {
        return this.seuil;
    }

    public DeadlineNotification seuil(String seuil) {
        this.setSeuil(seuil);
        return this;
    }

    public void setSeuil(String seuil) {
        this.seuil = seuil;
    }

    public Instant getDateEnvoi() {
        return this.dateEnvoi;
    }

    public DeadlineNotification dateEnvoi(Instant dateEnvoi) {
        this.setDateEnvoi(dateEnvoi);
        return this;
    }

    public void setDateEnvoi(Instant dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeadlineNotification)) {
            return false;
        }
        return getId() != null && getId().equals(((DeadlineNotification) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DeadlineNotification{" +
            "id=" + getId() +
            ", operationId='" + getOperationId() + "'" +
            ", seuil='" + getSeuil() + "'" +
            ", dateEnvoi='" + getDateEnvoi() + "'" +
            "}";
    }
}
