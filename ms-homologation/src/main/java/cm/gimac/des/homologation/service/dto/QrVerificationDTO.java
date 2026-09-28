package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.QrForm;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.QrVerification} entity.
 */
@Schema(
    description = "Vérification du code QR marchand, rattachée à une opération de la branche\npaiement mobile (HOM-08). Le service n'en génère ni n'en décode aucun."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QrVerificationDTO implements Serializable {

    private UUID id;

    @NotNull
    private QrForm form;

    @NotNull
    @Schema(description = "Résultat constaté par l'ingénieur d'homologation.", requiredMode = Schema.RequiredMode.REQUIRED)
    private ScenarioOutcome outcome;

    @NotNull
    private LocalDate verifiedOn;

    @NotNull
    private UUID verifierId;

    @NotNull
    private UUID evidenceDocumentId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String evidenceDigest;

    private Instant evidenceSealedAt;

    @NotNull
    private LiaisonDTO liaison;

    @NotNull
    private OperationDTO operation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public QrForm getForm() {
        return form;
    }

    public void setForm(QrForm form) {
        this.form = form;
    }

    public ScenarioOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ScenarioOutcome outcome) {
        this.outcome = outcome;
    }

    public LocalDate getVerifiedOn() {
        return verifiedOn;
    }

    public void setVerifiedOn(LocalDate verifiedOn) {
        this.verifiedOn = verifiedOn;
    }

    public UUID getVerifierId() {
        return verifierId;
    }

    public void setVerifierId(UUID verifierId) {
        this.verifierId = verifierId;
    }

    public UUID getEvidenceDocumentId() {
        return evidenceDocumentId;
    }

    public void setEvidenceDocumentId(UUID evidenceDocumentId) {
        this.evidenceDocumentId = evidenceDocumentId;
    }

    public String getEvidenceDigest() {
        return evidenceDigest;
    }

    public void setEvidenceDigest(String evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public Instant getEvidenceSealedAt() {
        return evidenceSealedAt;
    }

    public void setEvidenceSealedAt(Instant evidenceSealedAt) {
        this.evidenceSealedAt = evidenceSealedAt;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    public OperationDTO getOperation() {
        return operation;
    }

    public void setOperation(OperationDTO operation) {
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QrVerificationDTO)) {
            return false;
        }

        QrVerificationDTO qrVerificationDTO = (QrVerificationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, qrVerificationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QrVerificationDTO{" +
            "id='" + getId() + "'" +
            ", form='" + getForm() + "'" +
            ", outcome='" + getOutcome() + "'" +
            ", verifiedOn='" + getVerifiedOn() + "'" +
            ", verifierId='" + getVerifierId() + "'" +
            ", evidenceDocumentId='" + getEvidenceDocumentId() + "'" +
            ", evidenceDigest='" + getEvidenceDigest() + "'" +
            ", evidenceSealedAt='" + getEvidenceSealedAt() + "'" +
            ", liaison=" + getLiaison() +
            ", operation=" + getOperation() +
            "}";
    }
}
