package cm.gimac.des.collaboration.domain;

import cm.gimac.des.collaboration.domain.enumeration.VerificationState;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Terminal enrôlé et son état de vérification (signature croisée par la clé
 * maîtresse). Seules les parties publiques sont connues ; le lot de pré-clés
 * à usage unique est détenu par le serveur de clés de Matrix Synapse.
 */
@Entity
@Table(name = "verified_terminal")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VerifiedTerminal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Propriétaire (MS-IAM), ou compte technique du terminal d'archivage.
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Identifiant du terminal (device Matrix), par exemple TRM-7A21.
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "terminal_id", length = 100, nullable = false, unique = true)
    private String terminalId;

    /**
     * Clé d'identité publique Ed25519.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "identity_key", length = 64, nullable = false)
    private String identityKey;

    /**
     * Clé d'accord publique X25519.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "agreement_key", length = 64, nullable = false)
    private String agreementKey;

    /**
     * Pré-clé signée publique.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "signed_pre_key", length = 64, nullable = false)
    private String signedPreKey;

    /**
     * Signature Ed25519 de la pré-clé signée par la clé d'identité (64 octets).
     */
    @NotNull
    @Size(min = 128, max = 128)
    @Pattern(regexp = "^[0-9a-f]{128}$")
    @Column(name = "signed_pre_key_signature", length = 128, nullable = false)
    private String signedPreKeySignature;

    /**
     * Nombre de pré-clés à usage unique encore disponibles.
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "one_time_pre_keys", nullable = false)
    private Integer oneTimePreKeys;

    /**
     * Signature croisée par la clé maîtresse de l'utilisateur (64 octets).
     */
    @Size(min = 128, max = 128)
    @Pattern(regexp = "^[0-9a-f]{128}$")
    @Column(name = "cross_signature", length = 128)
    private String crossSignature;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private VerificationState state;

    /**
     * Terminal d'archivage déclaré du service (D-28), clés privées dans le coffre.
     */
    @NotNull
    @Column(name = "archive_terminal", nullable = false)
    private Boolean archiveTerminal;

    @NotNull
    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public VerifiedTerminal id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public VerifiedTerminal userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTerminalId() {
        return this.terminalId;
    }

    public VerifiedTerminal terminalId(String terminalId) {
        this.setTerminalId(terminalId);
        return this;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getIdentityKey() {
        return this.identityKey;
    }

    public VerifiedTerminal identityKey(String identityKey) {
        this.setIdentityKey(identityKey);
        return this;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public String getAgreementKey() {
        return this.agreementKey;
    }

    public VerifiedTerminal agreementKey(String agreementKey) {
        this.setAgreementKey(agreementKey);
        return this;
    }

    public void setAgreementKey(String agreementKey) {
        this.agreementKey = agreementKey;
    }

    public String getSignedPreKey() {
        return this.signedPreKey;
    }

    public VerifiedTerminal signedPreKey(String signedPreKey) {
        this.setSignedPreKey(signedPreKey);
        return this;
    }

    public void setSignedPreKey(String signedPreKey) {
        this.signedPreKey = signedPreKey;
    }

    public String getSignedPreKeySignature() {
        return this.signedPreKeySignature;
    }

    public VerifiedTerminal signedPreKeySignature(String signedPreKeySignature) {
        this.setSignedPreKeySignature(signedPreKeySignature);
        return this;
    }

    public void setSignedPreKeySignature(String signedPreKeySignature) {
        this.signedPreKeySignature = signedPreKeySignature;
    }

    public Integer getOneTimePreKeys() {
        return this.oneTimePreKeys;
    }

    public VerifiedTerminal oneTimePreKeys(Integer oneTimePreKeys) {
        this.setOneTimePreKeys(oneTimePreKeys);
        return this;
    }

    public void setOneTimePreKeys(Integer oneTimePreKeys) {
        this.oneTimePreKeys = oneTimePreKeys;
    }

    public String getCrossSignature() {
        return this.crossSignature;
    }

    public VerifiedTerminal crossSignature(String crossSignature) {
        this.setCrossSignature(crossSignature);
        return this;
    }

    public void setCrossSignature(String crossSignature) {
        this.crossSignature = crossSignature;
    }

    public VerificationState getState() {
        return this.state;
    }

    public VerifiedTerminal state(VerificationState state) {
        this.setState(state);
        return this;
    }

    public void setState(VerificationState state) {
        this.state = state;
    }

    public Boolean getArchiveTerminal() {
        return this.archiveTerminal;
    }

    public VerifiedTerminal archiveTerminal(Boolean archiveTerminal) {
        this.setArchiveTerminal(archiveTerminal);
        return this;
    }

    public void setArchiveTerminal(Boolean archiveTerminal) {
        this.archiveTerminal = archiveTerminal;
    }

    public Instant getRegisteredAt() {
        return this.registeredAt;
    }

    public VerifiedTerminal registeredAt(Instant registeredAt) {
        this.setRegisteredAt(registeredAt);
        return this;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getVerifiedAt() {
        return this.verifiedAt;
    }

    public VerifiedTerminal verifiedAt(Instant verifiedAt) {
        this.setVerifiedAt(verifiedAt);
        return this;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getRevokedAt() {
        return this.revokedAt;
    }

    public VerifiedTerminal revokedAt(Instant revokedAt) {
        this.setRevokedAt(revokedAt);
        return this;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerifiedTerminal)) {
            return false;
        }
        return getId() != null && getId().equals(((VerifiedTerminal) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VerifiedTerminal{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", terminalId='" + getTerminalId() + "'" +
            ", identityKey='" + getIdentityKey() + "'" +
            ", agreementKey='" + getAgreementKey() + "'" +
            ", signedPreKey='" + getSignedPreKey() + "'" +
            ", signedPreKeySignature='" + getSignedPreKeySignature() + "'" +
            ", oneTimePreKeys=" + getOneTimePreKeys() +
            ", crossSignature='" + getCrossSignature() + "'" +
            ", state='" + getState() + "'" +
            ", archiveTerminal='" + getArchiveTerminal() + "'" +
            ", registeredAt='" + getRegisteredAt() + "'" +
            ", verifiedAt='" + getVerifiedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            "}";
    }
}
