package cm.gimac.des.iam.domain;

import cm.gimac.des.iam.domain.enumeration.SessionState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Session liée au certificat client (RFC 8705), révocable (IAM-03, GW-05).
 */
@Entity
@Table(name = "user_session")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Empreinte SHA-256 du certificat client, encodée en base64url.
     */
    @NotNull
    @Size(max = 64)
    @Column(name = "certificate_thumbprint", length = 64, nullable = false)
    private String certificateThumbprint;

    /**
     * Méthodes d'authentification (amr), séparées par des virgules.
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "auth_methods", length = 255, nullable = false)
    private String authMethods;

    @NotNull
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SessionState state;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Size(max = 255)
    @Column(name = "revocation_reason", length = 255)
    private String revocationReason;

    @ManyToOne(optional = false)
    @NotNull
    private Collaborator collaborator;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public UserSession id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCertificateThumbprint() {
        return this.certificateThumbprint;
    }

    public UserSession certificateThumbprint(String certificateThumbprint) {
        this.setCertificateThumbprint(certificateThumbprint);
        return this;
    }

    public void setCertificateThumbprint(String certificateThumbprint) {
        this.certificateThumbprint = certificateThumbprint;
    }

    public String getAuthMethods() {
        return this.authMethods;
    }

    public UserSession authMethods(String authMethods) {
        this.setAuthMethods(authMethods);
        return this;
    }

    public void setAuthMethods(String authMethods) {
        this.authMethods = authMethods;
    }

    public Instant getIssuedAt() {
        return this.issuedAt;
    }

    public UserSession issuedAt(Instant issuedAt) {
        this.setIssuedAt(issuedAt);
        return this;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public UserSession expiresAt(Instant expiresAt) {
        this.setExpiresAt(expiresAt);
        return this;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public SessionState getState() {
        return this.state;
    }

    public UserSession state(SessionState state) {
        this.setState(state);
        return this;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public Instant getRevokedAt() {
        return this.revokedAt;
    }

    public UserSession revokedAt(Instant revokedAt) {
        this.setRevokedAt(revokedAt);
        return this;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevocationReason() {
        return this.revocationReason;
    }

    public UserSession revocationReason(String revocationReason) {
        this.setRevocationReason(revocationReason);
        return this;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    public Collaborator getCollaborator() {
        return this.collaborator;
    }

    public void setCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
    }

    public UserSession collaborator(Collaborator collaborator) {
        this.setCollaborator(collaborator);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserSession)) {
            return false;
        }
        return getId() != null && getId().equals(((UserSession) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserSession{" +
            "id=" + getId() +
            ", certificateThumbprint='" + getCertificateThumbprint() + "'" +
            ", authMethods='" + getAuthMethods() + "'" +
            ", issuedAt='" + getIssuedAt() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", state='" + getState() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", revocationReason='" + getRevocationReason() + "'" +
            "}";
    }
}
