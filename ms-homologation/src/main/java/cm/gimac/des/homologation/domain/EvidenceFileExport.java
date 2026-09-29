package cm.gimac.des.homologation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A EvidenceFileExport.
 */
@Entity
@Table(name = "evidence_file_export")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvidenceFileExport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifeste", columnDefinition = "jsonb")
    private String manifeste;

    @Size(max = 64)
    @Column(name = "empreinte_manifeste", length = 64)
    private String empreinteManifeste;

    @Size(max = 120)
    @Column(name = "document_ref", length = 120)
    private String documentRef;

    @Column(name = "date_export")
    private Instant dateExport;

    @Column(name = "demandeur_id")
    private UUID demandeurId;

    @Size(max = 64)
    @Column(name = "socle_empreinte", length = 64)
    private String socleEmpreinte;

    @Size(max = 30)
    @Column(name = "etat", length = 30)
    private String etat;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "version", "contractualScope" }, allowSetters = true)
    private Liaison liaison;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public EvidenceFileExport id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getManifeste() {
        return this.manifeste;
    }

    public EvidenceFileExport manifeste(String manifeste) {
        this.setManifeste(manifeste);
        return this;
    }

    public void setManifeste(String manifeste) {
        this.manifeste = manifeste;
    }

    public String getEmpreinteManifeste() {
        return this.empreinteManifeste;
    }

    public EvidenceFileExport empreinteManifeste(String empreinteManifeste) {
        this.setEmpreinteManifeste(empreinteManifeste);
        return this;
    }

    public void setEmpreinteManifeste(String empreinteManifeste) {
        this.empreinteManifeste = empreinteManifeste;
    }

    public String getDocumentRef() {
        return this.documentRef;
    }

    public EvidenceFileExport documentRef(String documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public Instant getDateExport() {
        return this.dateExport;
    }

    public EvidenceFileExport dateExport(Instant dateExport) {
        this.setDateExport(dateExport);
        return this;
    }

    public void setDateExport(Instant dateExport) {
        this.dateExport = dateExport;
    }

    public UUID getDemandeurId() {
        return this.demandeurId;
    }

    public EvidenceFileExport demandeurId(UUID demandeurId) {
        this.setDemandeurId(demandeurId);
        return this;
    }

    public void setDemandeurId(UUID demandeurId) {
        this.demandeurId = demandeurId;
    }

    public String getSocleEmpreinte() {
        return this.socleEmpreinte;
    }

    public EvidenceFileExport socleEmpreinte(String socleEmpreinte) {
        this.setSocleEmpreinte(socleEmpreinte);
        return this;
    }

    public void setSocleEmpreinte(String socleEmpreinte) {
        this.socleEmpreinte = socleEmpreinte;
    }

    public String getEtat() {
        return this.etat;
    }

    public EvidenceFileExport etat(String etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public Liaison getLiaison() {
        return this.liaison;
    }

    public void setLiaison(Liaison liaison) {
        this.liaison = liaison;
    }

    public EvidenceFileExport liaison(Liaison liaison) {
        this.setLiaison(liaison);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvidenceFileExport)) {
            return false;
        }
        return getId() != null && getId().equals(((EvidenceFileExport) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvidenceFileExport{" +
            "id=" + getId() +
            ", manifeste='" + getManifeste() + "'" +
            ", empreinteManifeste='" + getEmpreinteManifeste() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", dateExport='" + getDateExport() + "'" +
            ", demandeurId='" + getDemandeurId() + "'" +
            ", socleEmpreinte='" + getSocleEmpreinte() + "'" +
            ", etat='" + getEtat() + "'" +
            "}";
    }
}
