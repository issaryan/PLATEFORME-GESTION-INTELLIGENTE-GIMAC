package cm.gimac.des.monitoring.service.dto;

import cm.gimac.des.monitoring.domain.enumeration.SignatureRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.ReportSignature} entity.
 */
@Schema(description = "Signature WebAuthn d'un rapport de quart, liée aux précédentes (D-12).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReportSignatureDTO implements Serializable {

    private UUID id;

    @NotNull
    private SignatureRole role;

    @NotNull
    private UUID signerId;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Défi signé : H1, H2 ou H3.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String challenge;

    @Schema(
        description = "Assertion WebAuthn (authenticatorData, clientDataJSON, signature), en JSON.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String assertion;

    @NotNull
    private Instant signedAt;

    @Size(max = 2000)
    @Schema(description = "Réserves de l'entrant, citées sans altérer J.")
    private String reservations;

    @NotNull
    private ShiftReportDTO report;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SignatureRole getRole() {
        return role;
    }

    public void setRole(SignatureRole role) {
        this.role = role;
    }

    public UUID getSignerId() {
        return signerId;
    }

    public void setSignerId(UUID signerId) {
        this.signerId = signerId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public String getReservations() {
        return reservations;
    }

    public void setReservations(String reservations) {
        this.reservations = reservations;
    }

    public ShiftReportDTO getReport() {
        return report;
    }

    public void setReport(ShiftReportDTO report) {
        this.report = report;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportSignatureDTO)) {
            return false;
        }

        ReportSignatureDTO reportSignatureDTO = (ReportSignatureDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, reportSignatureDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReportSignatureDTO{" +
            "id='" + getId() + "'" +
            ", role='" + getRole() + "'" +
            ", signerId='" + getSignerId() + "'" +
            ", challenge='" + getChallenge() + "'" +
            ", assertion='" + getAssertion() + "'" +
            ", signedAt='" + getSignedAt() + "'" +
            ", reservations='" + getReservations() + "'" +
            ", report=" + getReport() +
            "}";
    }
}
