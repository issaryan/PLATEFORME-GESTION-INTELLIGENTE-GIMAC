package cm.gimac.des.homologation.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A RejectedRecord.
 */
@Entity
@Table(name = "rejected_record")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RejectedRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Size(max = 60)
    @Column(name = "source", length = 60)
    private String source;

    @Lob
    @Column(name = "charge_utile")
    private String chargeUtile;

    @Size(max = 64)
    @Column(name = "empreinte", length = 64)
    private String empreinte;

    @Size(max = 500)
    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "date_rejet")
    private Instant dateRejet;

    @Column(name = "relance")
    private Boolean relance;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public RejectedRecord id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return this.source;
    }

    public RejectedRecord source(String source) {
        this.setSource(source);
        return this;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getChargeUtile() {
        return this.chargeUtile;
    }

    public RejectedRecord chargeUtile(String chargeUtile) {
        this.setChargeUtile(chargeUtile);
        return this;
    }

    public void setChargeUtile(String chargeUtile) {
        this.chargeUtile = chargeUtile;
    }

    public String getEmpreinte() {
        return this.empreinte;
    }

    public RejectedRecord empreinte(String empreinte) {
        this.setEmpreinte(empreinte);
        return this;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public String getMotif() {
        return this.motif;
    }

    public RejectedRecord motif(String motif) {
        this.setMotif(motif);
        return this;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getDateRejet() {
        return this.dateRejet;
    }

    public RejectedRecord dateRejet(Instant dateRejet) {
        this.setDateRejet(dateRejet);
        return this;
    }

    public void setDateRejet(Instant dateRejet) {
        this.dateRejet = dateRejet;
    }

    public Boolean getRelance() {
        return this.relance;
    }

    public RejectedRecord relance(Boolean relance) {
        this.setRelance(relance);
        return this;
    }

    public void setRelance(Boolean relance) {
        this.relance = relance;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RejectedRecord)) {
            return false;
        }
        return getId() != null && getId().equals(((RejectedRecord) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RejectedRecord{" +
            "id=" + getId() +
            ", source='" + getSource() + "'" +
            ", chargeUtile='" + getChargeUtile() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", motif='" + getMotif() + "'" +
            ", dateRejet='" + getDateRejet() + "'" +
            ", relance='" + getRelance() + "'" +
            "}";
    }
}
