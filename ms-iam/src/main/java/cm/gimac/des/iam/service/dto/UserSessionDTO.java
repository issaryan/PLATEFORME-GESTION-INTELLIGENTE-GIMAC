package cm.gimac.des.iam.service.dto;

import cm.gimac.des.iam.domain.enumeration.SessionState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.iam.domain.UserSession} entity.
 */
@Schema(description = "Session liée au certificat client (RFC 8705), révocable (IAM-03, GW-05).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserSessionDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 64)
    @Schema(description = "Empreinte SHA-256 du certificat client, encodée en base64url.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String certificateThumbprint;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Méthodes d'authentification (amr), séparées par des virgules.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authMethods;

    @NotNull
    private Instant issuedAt;

    @NotNull
    private Instant expiresAt;

    @NotNull
    private SessionState state;

    private Instant revokedAt;

    @Size(max = 255)
    private String revocationReason;

    @NotNull
    private CollaboratorDTO collaborator;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCertificateThumbprint() {
        return certificateThumbprint;
    }

    public void setCertificateThumbprint(String certificateThumbprint) {
        this.certificateThumbprint = certificateThumbprint;
    }

    public String getAuthMethods() {
        return authMethods;
    }

    public void setAuthMethods(String authMethods) {
        this.authMethods = authMethods;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public CollaboratorDTO getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(CollaboratorDTO collaborator) {
        this.collaborator = collaborator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserSessionDTO)) {
            return false;
        }

        UserSessionDTO userSessionDTO = (UserSessionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, userSessionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserSessionDTO{" +
            "id='" + getId() + "'" +
            ", certificateThumbprint='" + getCertificateThumbprint() + "'" +
            ", authMethods='" + getAuthMethods() + "'" +
            ", issuedAt='" + getIssuedAt() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", state='" + getState() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", revocationReason='" + getRevocationReason() + "'" +
            ", collaborator=" + getCollaborator() +
            "}";
    }
}
