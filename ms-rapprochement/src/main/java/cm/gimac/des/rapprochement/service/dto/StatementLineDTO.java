package cm.gimac.des.rapprochement.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.StatementLine} entity.
 */
@Schema(
    description = "Ligne normalisée d'un état (objet-valeur du CC, persisté pour 400 jours).\nLa clé pivot (PivotKey) est aplatie ; aucune donnée de compte."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StatementLineDTO implements Serializable {

    private UUID id;

    @NotNull
    @Min(value = 1)
    private Integer lineNumber;

    @NotNull
    @Size(max = 64)
    private String transactionRef;

    @Size(max = 64)
    private String authorizationRef;

    @NotNull
    @Schema(
        description = "Horodatage de l'opération (timestamp du CC, renommé : mot réservé SQL).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant operationAt;

    @NotNull
    private BigDecimal amount;

    @NotNull
    @Size(max = 20)
    private String participantCode;

    @Size(max = 30)
    @Schema(description = "Statut tel que déclaré par la source, non interprété (RG-43).")
    private String lineStatus;

    @NotNull
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency;

    @NotNull
    private ImportedStatementDTO statement;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getAuthorizationRef() {
        return authorizationRef;
    }

    public void setAuthorizationRef(String authorizationRef) {
        this.authorizationRef = authorizationRef;
    }

    public Instant getOperationAt() {
        return operationAt;
    }

    public void setOperationAt(Instant operationAt) {
        this.operationAt = operationAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getParticipantCode() {
        return participantCode;
    }

    public void setParticipantCode(String participantCode) {
        this.participantCode = participantCode;
    }

    public String getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(String lineStatus) {
        this.lineStatus = lineStatus;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ImportedStatementDTO getStatement() {
        return statement;
    }

    public void setStatement(ImportedStatementDTO statement) {
        this.statement = statement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatementLineDTO)) {
            return false;
        }

        StatementLineDTO statementLineDTO = (StatementLineDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, statementLineDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StatementLineDTO{" +
            "id='" + getId() + "'" +
            ", lineNumber=" + getLineNumber() +
            ", transactionRef='" + getTransactionRef() + "'" +
            ", authorizationRef='" + getAuthorizationRef() + "'" +
            ", operationAt='" + getOperationAt() + "'" +
            ", amount=" + getAmount() +
            ", participantCode='" + getParticipantCode() + "'" +
            ", lineStatus='" + getLineStatus() + "'" +
            ", currency='" + getCurrency() + "'" +
            ", statement=" + getStatement() +
            "}";
    }
}
