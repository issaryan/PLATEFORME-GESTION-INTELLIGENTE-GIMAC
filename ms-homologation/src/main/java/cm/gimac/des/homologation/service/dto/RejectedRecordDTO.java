package cm.gimac.des.homologation.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.RejectedRecord} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RejectedRecordDTO implements Serializable {

    private UUID id;

    @Size(max = 60)
    private String source;

    @Lob
    private String chargeUtile;

    @Size(max = 64)
    private String empreinte;

    @Size(max = 500)
    private String motif;

    private Instant dateRejet;

    private Boolean relance;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }

    public void setChargeUtile(String chargeUtile) {
        this.chargeUtile = chargeUtile;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getDateRejet() {
        return dateRejet;
    }

    public void setDateRejet(Instant dateRejet) {
        this.dateRejet = dateRejet;
    }

    public Boolean getRelance() {
        return relance;
    }

    public void setRelance(Boolean relance) {
        this.relance = relance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RejectedRecordDTO)) {
            return false;
        }

        RejectedRecordDTO rejectedRecordDTO = (RejectedRecordDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, rejectedRecordDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RejectedRecordDTO{" +
            "id='" + getId() + "'" +
            ", source='" + getSource() + "'" +
            ", chargeUtile='" + getChargeUtile() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", motif='" + getMotif() + "'" +
            ", dateRejet='" + getDateRejet() + "'" +
            ", relance='" + getRelance() + "'" +
            "}";
    }
}
