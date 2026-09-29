package cm.gimac.des.certification.domain;

import cm.gimac.des.certification.domain.enumeration.EtatQuitus;
import cm.gimac.des.certification.domain.enumeration.NatureQuitus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A Quitus.
 */
@Entity
@Table(name = "quitus")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Quitus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false)
    private NatureQuitus nature;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatQuitus etat;

    @Column(name = "date_attendu")
    private Instant dateAttendu;

    @Size(max = 500)
    @Column(name = "motif_caducite", length = 500)
    private String motifCaducite;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "goLiveDossier" }, allowSetters = true)
    private CertificationCircuit circuit;

    @JsonIgnoreProperties(value = { "quitus" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "quitus")
    private SignedDecision signedDecision;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Quitus id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NatureQuitus getNature() {
        return this.nature;
    }

    public Quitus nature(NatureQuitus nature) {
        this.setNature(nature);
        return this;
    }

    public void setNature(NatureQuitus nature) {
        this.nature = nature;
    }

    public EtatQuitus getEtat() {
        return this.etat;
    }

    public Quitus etat(EtatQuitus etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatQuitus etat) {
        this.etat = etat;
    }

    public Instant getDateAttendu() {
        return this.dateAttendu;
    }

    public Quitus dateAttendu(Instant dateAttendu) {
        this.setDateAttendu(dateAttendu);
        return this;
    }

    public void setDateAttendu(Instant dateAttendu) {
        this.dateAttendu = dateAttendu;
    }

    public String getMotifCaducite() {
        return this.motifCaducite;
    }

    public Quitus motifCaducite(String motifCaducite) {
        this.setMotifCaducite(motifCaducite);
        return this;
    }

    public void setMotifCaducite(String motifCaducite) {
        this.motifCaducite = motifCaducite;
    }

    public CertificationCircuit getCircuit() {
        return this.circuit;
    }

    public void setCircuit(CertificationCircuit certificationCircuit) {
        this.circuit = certificationCircuit;
    }

    public Quitus circuit(CertificationCircuit certificationCircuit) {
        this.setCircuit(certificationCircuit);
        return this;
    }

    public SignedDecision getSignedDecision() {
        return this.signedDecision;
    }

    public void setSignedDecision(SignedDecision signedDecision) {
        if (this.signedDecision != null) {
            this.signedDecision.setQuitus(null);
        }
        if (signedDecision != null) {
            signedDecision.setQuitus(this);
        }
        this.signedDecision = signedDecision;
    }

    public Quitus signedDecision(SignedDecision signedDecision) {
        this.setSignedDecision(signedDecision);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quitus)) {
            return false;
        }
        return getId() != null && getId().equals(((Quitus) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Quitus{" +
            "id=" + getId() +
            ", nature='" + getNature() + "'" +
            ", etat='" + getEtat() + "'" +
            ", dateAttendu='" + getDateAttendu() + "'" +
            ", motifCaducite='" + getMotifCaducite() + "'" +
            "}";
    }
}
