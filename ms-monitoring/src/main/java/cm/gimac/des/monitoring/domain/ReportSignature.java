package cm.gimac.des.monitoring.domain;

import cm.gimac.des.monitoring.domain.enumeration.SignatureRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Signature WebAuthn d'un rapport de quart, liée aux précédentes (D-12).
 */
@Entity
@Table(name = "report_signature")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReportSignature implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private SignatureRole role;

    @NotNull
    @Column(name = "signer_id", nullable = false)
    private UUID signerId;

    /**
     * Défi signé : H1, H2 ou H3.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "challenge", length = 64, nullable = false)
    private String challenge;

    /**
     * Assertion WebAuthn (authenticatorData, clientDataJSON, signature), en JSON.
     */
    @Lob
    @Column(name = "assertion", nullable = false)
    private String assertion;

    @NotNull
    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    /**
     * Réserves de l'entrant, citées sans altérer J.
     */
    @Size(max = 2000)
    @Column(name = "reservations", length = 2000)
    private String reservations;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "shift", "supersedes", "shiftReport" }, allowSetters = true)
    private ShiftReport report;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReportSignature id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SignatureRole getRole() {
        return this.role;
    }

    public ReportSignature role(SignatureRole role) {
        this.setRole(role);
        return this;
    }

    public void setRole(SignatureRole role) {
        this.role = role;
    }

    public UUID getSignerId() {
        return this.signerId;
    }

    public ReportSignature signerId(UUID signerId) {
        this.setSignerId(signerId);
        return this;
    }

    public void setSignerId(UUID signerId) {
        this.signerId = signerId;
    }

    public String getChallenge() {
        return this.challenge;
    }

    public ReportSignature challenge(String challenge) {
        this.setChallenge(challenge);
        return this;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getAssertion() {
        return this.assertion;
    }

    public ReportSignature assertion(String assertion) {
        this.setAssertion(assertion);
        return this;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public Instant getSignedAt() {
        return this.signedAt;
    }

    public ReportSignature signedAt(Instant signedAt) {
        this.setSignedAt(signedAt);
        return this;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public String getReservations() {
        return this.reservations;
    }

    public ReportSignature reservations(String reservations) {
        this.setReservations(reservations);
        return this;
    }

    public void setReservations(String reservations) {
        this.reservations = reservations;
    }

    public ShiftReport getReport() {
        return this.report;
    }

    public void setReport(ShiftReport shiftReport) {
        this.report = shiftReport;
    }

    public ReportSignature report(ShiftReport shiftReport) {
        this.setReport(shiftReport);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportSignature)) {
            return false;
        }
        return getId() != null && getId().equals(((ReportSignature) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReportSignature{" +
            "id=" + getId() +
            ", role='" + getRole() + "'" +
            ", signerId='" + getSignerId() + "'" +
            ", challenge='" + getChallenge() + "'" +
            ", assertion='" + getAssertion() + "'" +
            ", signedAt='" + getSignedAt() + "'" +
            ", reservations='" + getReservations() + "'" +
            "}";
    }
}
