package cm.gimac.des.document.service.dto;

import cm.gimac.des.document.domain.enumeration.PurgeState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.document.domain.PurgeRequest} entity.
 */
@Schema(
    description = "Demande de purge à l'échéance, à quatre yeux (DOC-06) : demandeur et\napprobateur distincts ; impossible sous verrou actif ou conservation légale."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PurgeRequestDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID requesterId;

    @NotNull
    @Size(max = 1000)
    private String reason;

    @NotNull
    private Instant requestedAt;

    private UUID approverId;

    private Instant approvedAt;

    @NotNull
    private PurgeState state;

    @Size(max = 500)
    private String rejectionReason;

    private Instant executedAt;

    @NotNull
    private DocumentDTO document;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public PurgeState getState() {
        return state;
    }

    public void setState(PurgeState state) {
        this.state = state;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
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
        if (!(o instanceof PurgeRequestDTO)) {
            return false;
        }

        PurgeRequestDTO purgeRequestDTO = (PurgeRequestDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, purgeRequestDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PurgeRequestDTO{" +
            "id='" + getId() + "'" +
            ", requesterId='" + getRequesterId() + "'" +
            ", reason='" + getReason() + "'" +
            ", requestedAt='" + getRequestedAt() + "'" +
            ", approverId='" + getApproverId() + "'" +
            ", approvedAt='" + getApprovedAt() + "'" +
            ", state='" + getState() + "'" +
            ", rejectionReason='" + getRejectionReason() + "'" +
            ", executedAt='" + getExecutedAt() + "'" +
            ", document=" + getDocument() +
            "}";
    }
}
