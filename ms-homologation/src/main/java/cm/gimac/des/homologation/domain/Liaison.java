package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
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
 * A Liaison.
 */
@Entity
@Table(name = "liaison")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Liaison implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_projet", nullable = false)
    private TypeProjet typeProjet;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Size(max = 200)
    @Column(name = "institution_nom", length = 200)
    private String institutionNom;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false)
    private EtatLiaison etat;

    @NotNull
    @Column(name = "chef_de_projet_id", nullable = false)
    private UUID chefDeProjetId;

    @Column(name = "banque_partenaire_id")
    private UUID banquePartenaireId;

    @NotNull
    @Column(name = "date_ouverture", nullable = false)
    private Instant dateOuverture;

    @Size(max = 500)
    @Column(name = "motif_etat", length = 500)
    private String motifEtat;

    @NotNull
    @Column(name = "cycle_aptitude", nullable = false)
    private Integer cycleAptitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ordre_instanciation", columnDefinition = "jsonb")
    private String ordreInstanciation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "etat_avant_gel", columnDefinition = "jsonb")
    private String etatAvantGel;

    @ManyToOne(optional = false)
    @NotNull
    private ReferentialVersion version;

    @JsonIgnoreProperties(value = { "liaison" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "liaison")
    private ContractualScope contractualScope;

    /** Liaison d'origine d'un service supplémentaire (rang 35, D-57) ; hors unicité des liaisons actives. */
    @Column(name = "liaison_origine_id")
    private UUID liaisonOrigineId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getLiaisonOrigineId() {
        return this.liaisonOrigineId;
    }

    public Liaison liaisonOrigineId(UUID id) {
        this.liaisonOrigineId = id;
        return this;
    }

    public UUID getId() {
        return this.id;
    }

    public Liaison id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TypeProjet getTypeProjet() {
        return this.typeProjet;
    }

    public Liaison typeProjet(TypeProjet typeProjet) {
        this.setTypeProjet(typeProjet);
        return this;
    }

    public void setTypeProjet(TypeProjet typeProjet) {
        this.typeProjet = typeProjet;
    }

    public UUID getInstitutionId() {
        return this.institutionId;
    }

    public Liaison institutionId(UUID institutionId) {
        this.setInstitutionId(institutionId);
        return this;
    }

    public void setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionNom() {
        return this.institutionNom;
    }

    public Liaison institutionNom(String institutionNom) {
        this.setInstitutionNom(institutionNom);
        return this;
    }

    public void setInstitutionNom(String institutionNom) {
        this.institutionNom = institutionNom;
    }

    public EtatLiaison getEtat() {
        return this.etat;
    }

    public Liaison etat(EtatLiaison etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatLiaison etat) {
        this.etat = etat;
    }

    public UUID getChefDeProjetId() {
        return this.chefDeProjetId;
    }

    public Liaison chefDeProjetId(UUID chefDeProjetId) {
        this.setChefDeProjetId(chefDeProjetId);
        return this;
    }

    public void setChefDeProjetId(UUID chefDeProjetId) {
        this.chefDeProjetId = chefDeProjetId;
    }

    public UUID getBanquePartenaireId() {
        return this.banquePartenaireId;
    }

    public Liaison banquePartenaireId(UUID banquePartenaireId) {
        this.setBanquePartenaireId(banquePartenaireId);
        return this;
    }

    public void setBanquePartenaireId(UUID banquePartenaireId) {
        this.banquePartenaireId = banquePartenaireId;
    }

    public Instant getDateOuverture() {
        return this.dateOuverture;
    }

    public Liaison dateOuverture(Instant dateOuverture) {
        this.setDateOuverture(dateOuverture);
        return this;
    }

    public void setDateOuverture(Instant dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public String getMotifEtat() {
        return this.motifEtat;
    }

    public Liaison motifEtat(String motifEtat) {
        this.setMotifEtat(motifEtat);
        return this;
    }

    public void setMotifEtat(String motifEtat) {
        this.motifEtat = motifEtat;
    }

    public Integer getCycleAptitude() {
        return this.cycleAptitude;
    }

    public Liaison cycleAptitude(Integer cycleAptitude) {
        this.setCycleAptitude(cycleAptitude);
        return this;
    }

    public void setCycleAptitude(Integer cycleAptitude) {
        this.cycleAptitude = cycleAptitude;
    }

    public String getOrdreInstanciation() {
        return this.ordreInstanciation;
    }

    public Liaison ordreInstanciation(String ordreInstanciation) {
        this.setOrdreInstanciation(ordreInstanciation);
        return this;
    }

    public void setOrdreInstanciation(String ordreInstanciation) {
        this.ordreInstanciation = ordreInstanciation;
    }

    public String getEtatAvantGel() {
        return this.etatAvantGel;
    }

    public Liaison etatAvantGel(String etatAvantGel) {
        this.setEtatAvantGel(etatAvantGel);
        return this;
    }

    public void setEtatAvantGel(String etatAvantGel) {
        this.etatAvantGel = etatAvantGel;
    }

    public ReferentialVersion getVersion() {
        return this.version;
    }

    public void setVersion(ReferentialVersion referentialVersion) {
        this.version = referentialVersion;
    }

    public Liaison version(ReferentialVersion referentialVersion) {
        this.setVersion(referentialVersion);
        return this;
    }

    public ContractualScope getContractualScope() {
        return this.contractualScope;
    }

    public void setContractualScope(ContractualScope contractualScope) {
        if (this.contractualScope != null) {
            this.contractualScope.setLiaison(null);
        }
        if (contractualScope != null) {
            contractualScope.setLiaison(this);
        }
        this.contractualScope = contractualScope;
    }

    public Liaison contractualScope(ContractualScope contractualScope) {
        this.setContractualScope(contractualScope);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Liaison)) {
            return false;
        }
        return getId() != null && getId().equals(((Liaison) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Liaison{" +
            "id=" + getId() +
            ", typeProjet='" + getTypeProjet() + "'" +
            ", institutionId='" + getInstitutionId() + "'" +
            ", institutionNom='" + getInstitutionNom() + "'" +
            ", etat='" + getEtat() + "'" +
            ", chefDeProjetId='" + getChefDeProjetId() + "'" +
            ", banquePartenaireId='" + getBanquePartenaireId() + "'" +
            ", dateOuverture='" + getDateOuverture() + "'" +
            ", motifEtat='" + getMotifEtat() + "'" +
            ", cycleAptitude=" + getCycleAptitude() +
            ", ordreInstanciation='" + getOrdreInstanciation() + "'" +
            ", etatAvantGel='" + getEtatAvantGel() + "'" +
            "}";
    }
}
