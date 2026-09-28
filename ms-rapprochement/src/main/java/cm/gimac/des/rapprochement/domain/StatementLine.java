package cm.gimac.des.rapprochement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ligne normalisée d'un état (objet-valeur du CC, persisté pour 400 jours).
 * La clé pivot (PivotKey) est aplatie ; aucune donnée de compte.
 */
@Entity
@Table(name = "statement_line")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StatementLine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Min(value = 1)
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @NotNull
    @Size(max = 64)
    @Column(name = "transaction_ref", length = 64, nullable = false)
    private String transactionRef;

    @Size(max = 64)
    @Column(name = "authorization_ref", length = 64)
    private String authorizationRef;

    /**
     * Horodatage de l'opération (timestamp du CC, renommé : mot réservé SQL).
     */
    @NotNull
    @Column(name = "operation_at", nullable = false)
    private Instant operationAt;

    @NotNull
    @Column(name = "amount", precision = 21, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Size(max = 20)
    @Column(name = "participant_code", length = 20, nullable = false)
    private String participantCode;

    /**
     * Statut tel que déclaré par la source, non interprété (RG-43).
     */
    @Size(max = 30)
    @Column(name = "line_status", length = 30)
    private String lineStatus;

    @NotNull
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @ManyToOne(optional = false)
    @NotNull
    private ImportedStatement statement;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public StatementLine id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getLineNumber() {
        return this.lineNumber;
    }

    public StatementLine lineNumber(Integer lineNumber) {
        this.setLineNumber(lineNumber);
        return this;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getTransactionRef() {
        return this.transactionRef;
    }

    public StatementLine transactionRef(String transactionRef) {
        this.setTransactionRef(transactionRef);
        return this;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getAuthorizationRef() {
        return this.authorizationRef;
    }

    public StatementLine authorizationRef(String authorizationRef) {
        this.setAuthorizationRef(authorizationRef);
        return this;
    }

    public void setAuthorizationRef(String authorizationRef) {
        this.authorizationRef = authorizationRef;
    }

    public Instant getOperationAt() {
        return this.operationAt;
    }

    public StatementLine operationAt(Instant operationAt) {
        this.setOperationAt(operationAt);
        return this;
    }

    public void setOperationAt(Instant operationAt) {
        this.operationAt = operationAt;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public StatementLine amount(BigDecimal amount) {
        this.setAmount(amount);
        return this;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getParticipantCode() {
        return this.participantCode;
    }

    public StatementLine participantCode(String participantCode) {
        this.setParticipantCode(participantCode);
        return this;
    }

    public void setParticipantCode(String participantCode) {
        this.participantCode = participantCode;
    }

    public String getLineStatus() {
        return this.lineStatus;
    }

    public StatementLine lineStatus(String lineStatus) {
        this.setLineStatus(lineStatus);
        return this;
    }

    public void setLineStatus(String lineStatus) {
        this.lineStatus = lineStatus;
    }

    public String getCurrency() {
        return this.currency;
    }

    public StatementLine currency(String currency) {
        this.setCurrency(currency);
        return this;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ImportedStatement getStatement() {
        return this.statement;
    }

    public void setStatement(ImportedStatement importedStatement) {
        this.statement = importedStatement;
    }

    public StatementLine statement(ImportedStatement importedStatement) {
        this.setStatement(importedStatement);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatementLine)) {
            return false;
        }
        return getId() != null && getId().equals(((StatementLine) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StatementLine{" +
            "id=" + getId() +
            ", lineNumber=" + getLineNumber() +
            ", transactionRef='" + getTransactionRef() + "'" +
            ", authorizationRef='" + getAuthorizationRef() + "'" +
            ", operationAt='" + getOperationAt() + "'" +
            ", amount=" + getAmount() +
            ", participantCode='" + getParticipantCode() + "'" +
            ", lineStatus='" + getLineStatus() + "'" +
            ", currency='" + getCurrency() + "'" +
            "}";
    }
}
