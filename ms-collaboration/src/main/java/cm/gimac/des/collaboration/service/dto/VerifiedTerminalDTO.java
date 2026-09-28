package cm.gimac.des.collaboration.service.dto;

import cm.gimac.des.collaboration.domain.enumeration.VerificationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.collaboration.domain.VerifiedTerminal} entity.
 */
@Schema(
    description = "Terminal enrôlé et son état de vérification (signature croisée par la clé\nmaîtresse). Seules les parties publiques sont connues ; le lot de pré-clés\nà usage unique est détenu par le serveur de clés de Matrix Synapse."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VerifiedTerminalDTO implements Serializable {

    private UUID id;

    @NotNull
    @Schema(
        description = "Propriétaire (MS-IAM), ou compte technique du terminal d'archivage.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID userId;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Identifiant du terminal (device Matrix), par exemple TRM-7A21.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Clé d'identité publique Ed25519.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String identityKey;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Clé d'accord publique X25519.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String agreementKey;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Pré-clé signée publique.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signedPreKey;

    @NotNull
    @Size(min = 128, max = 128)
    @Pattern(regexp = "^[0-9a-f]{128}$")
    @Schema(
        description = "Signature Ed25519 de la pré-clé signée par la clé d'identité (64 octets).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String signedPreKeySignature;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Nombre de pré-clés à usage unique encore disponibles.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer oneTimePreKeys;

    @Size(min = 128, max = 128)
    @Pattern(regexp = "^[0-9a-f]{128}$")
    @Schema(description = "Signature croisée par la clé maîtresse de l'utilisateur (64 octets).")
    private String crossSignature;

    @NotNull
    private VerificationState state;

    @NotNull
    @Schema(
        description = "Terminal d'archivage déclaré du service (D-28), clés privées dans le coffre.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean archiveTerminal;

    @NotNull
    private Instant registeredAt;

    private Instant verifiedAt;

    private Instant revokedAt;

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

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public String getAgreementKey() {
        return agreementKey;
    }

    public void setAgreementKey(String agreementKey) {
        this.agreementKey = agreementKey;
    }

    public String getSignedPreKey() {
        return signedPreKey;
    }

    public void setSignedPreKey(String signedPreKey) {
        this.signedPreKey = signedPreKey;
    }

    public String getSignedPreKeySignature() {
        return signedPreKeySignature;
    }

    public void setSignedPreKeySignature(String signedPreKeySignature) {
        this.signedPreKeySignature = signedPreKeySignature;
    }

    public Integer getOneTimePreKeys() {
        return oneTimePreKeys;
    }

    public void setOneTimePreKeys(Integer oneTimePreKeys) {
        this.oneTimePreKeys = oneTimePreKeys;
    }

    public String getCrossSignature() {
        return crossSignature;
    }

    public void setCrossSignature(String crossSignature) {
        this.crossSignature = crossSignature;
    }

    public VerificationState getState() {
        return state;
    }

    public void setState(VerificationState state) {
        this.state = state;
    }

    public Boolean getArchiveTerminal() {
        return archiveTerminal;
    }

    public void setArchiveTerminal(Boolean archiveTerminal) {
        this.archiveTerminal = archiveTerminal;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerifiedTerminalDTO)) {
            return false;
        }

        VerifiedTerminalDTO verifiedTerminalDTO = (VerifiedTerminalDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, verifiedTerminalDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VerifiedTerminalDTO{" +
            "id='" + getId() + "'" +
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
