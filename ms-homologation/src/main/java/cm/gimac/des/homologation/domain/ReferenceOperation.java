package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * A ReferenceOperation.
 */
@Entity
@Table(name = "reference_operation")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferenceOperation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 40)
    @Column(name = "code", length = 40, nullable = false)
    private String code;

    @NotNull
    @Size(max = 200)
    @Column(name = "libelle", length = 200, nullable = false)
    private String libelle;

    @NotNull
    @Size(max = 60)
    @Column(name = "phase", length = 60, nullable = false)
    private String phase;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "segment", nullable = false)
    private Segment segment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "partie", nullable = false)
    private Partie partie;

    @Size(max = 40)
    @Column(name = "service_code", length = 40)
    private String serviceCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nature_couverture", nullable = false)
    private NatureCouverture natureCouverture;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preuve_attendue", columnDefinition = "jsonb")
    private String preuveAttendue;

    @Size(max = 80)
    @Column(name = "id_scenario", length = 80)
    private String idScenario;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predecesseurs", columnDefinition = "jsonb")
    private String predecesseurs;

    @Size(max = 60)
    @Column(name = "fonction_responsable", length = 60)
    private String fonctionResponsable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grille", columnDefinition = "jsonb")
    private String grille;

    @ManyToOne(optional = false)
    @NotNull
    private ReferentialVersion version;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReferenceOperation id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public ReferenceOperation code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return this.libelle;
    }

    public ReferenceOperation libelle(String libelle) {
        this.setLibelle(libelle);
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getPhase() {
        return this.phase;
    }

    public ReferenceOperation phase(String phase) {
        this.setPhase(phase);
        return this;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Segment getSegment() {
        return this.segment;
    }

    public ReferenceOperation segment(Segment segment) {
        this.setSegment(segment);
        return this;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Partie getPartie() {
        return this.partie;
    }

    public ReferenceOperation partie(Partie partie) {
        this.setPartie(partie);
        return this;
    }

    public void setPartie(Partie partie) {
        this.partie = partie;
    }

    public String getServiceCode() {
        return this.serviceCode;
    }

    public ReferenceOperation serviceCode(String serviceCode) {
        this.setServiceCode(serviceCode);
        return this;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public NatureCouverture getNatureCouverture() {
        return this.natureCouverture;
    }

    public ReferenceOperation natureCouverture(NatureCouverture natureCouverture) {
        this.setNatureCouverture(natureCouverture);
        return this;
    }

    public void setNatureCouverture(NatureCouverture natureCouverture) {
        this.natureCouverture = natureCouverture;
    }

    public String getPreuveAttendue() {
        return this.preuveAttendue;
    }

    public ReferenceOperation preuveAttendue(String preuveAttendue) {
        this.setPreuveAttendue(preuveAttendue);
        return this;
    }

    public void setPreuveAttendue(String preuveAttendue) {
        this.preuveAttendue = preuveAttendue;
    }

    public String getIdScenario() {
        return this.idScenario;
    }

    public ReferenceOperation idScenario(String idScenario) {
        this.setIdScenario(idScenario);
        return this;
    }

    public void setIdScenario(String idScenario) {
        this.idScenario = idScenario;
    }

    public String getPredecesseurs() {
        return this.predecesseurs;
    }

    public ReferenceOperation predecesseurs(String predecesseurs) {
        this.setPredecesseurs(predecesseurs);
        return this;
    }

    public void setPredecesseurs(String predecesseurs) {
        this.predecesseurs = predecesseurs;
    }

    public String getFonctionResponsable() {
        return this.fonctionResponsable;
    }

    public ReferenceOperation fonctionResponsable(String fonctionResponsable) {
        this.setFonctionResponsable(fonctionResponsable);
        return this;
    }

    public void setFonctionResponsable(String fonctionResponsable) {
        this.fonctionResponsable = fonctionResponsable;
    }

    public String getGrille() {
        return this.grille;
    }

    public ReferenceOperation grille(String grille) {
        this.setGrille(grille);
        return this;
    }

    public void setGrille(String grille) {
        this.grille = grille;
    }

    public ReferentialVersion getVersion() {
        return this.version;
    }

    public void setVersion(ReferentialVersion referentialVersion) {
        this.version = referentialVersion;
    }

    public ReferenceOperation version(ReferentialVersion referentialVersion) {
        this.setVersion(referentialVersion);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferenceOperation)) {
            return false;
        }
        return getId() != null && getId().equals(((ReferenceOperation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferenceOperation{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +
            ", phase='" + getPhase() + "'" +
            ", segment='" + getSegment() + "'" +
            ", partie='" + getPartie() + "'" +
            ", serviceCode='" + getServiceCode() + "'" +
            ", natureCouverture='" + getNatureCouverture() + "'" +
            ", preuveAttendue='" + getPreuveAttendue() + "'" +
            ", idScenario='" + getIdScenario() + "'" +
            ", predecesseurs='" + getPredecesseurs() + "'" +
            ", fonctionResponsable='" + getFonctionResponsable() + "'" +
            ", grille='" + getGrille() + "'" +
            "}";
    }
}
