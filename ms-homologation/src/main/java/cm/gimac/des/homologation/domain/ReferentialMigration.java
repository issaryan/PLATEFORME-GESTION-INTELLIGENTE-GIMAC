package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.EtatMigration;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A ReferentialMigration.
 */
@Entity
@Table(name = "referential_migration")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferentialMigration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "version_source_id")
    private UUID versionSourceId;

    @Column(name = "version_cible_id")
    private UUID versionCibleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat")
    private EtatMigration etat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rapport", columnDefinition = "jsonb")
    private String rapport;

    @Column(name = "date_demande")
    private Instant dateDemande;

    @Column(name = "decideur_id")
    private UUID decideurId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "liaisons_retenues", columnDefinition = "jsonb")
    private String liaisonsRetenues;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReferentialMigration id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVersionSourceId() {
        return this.versionSourceId;
    }

    public ReferentialMigration versionSourceId(UUID versionSourceId) {
        this.setVersionSourceId(versionSourceId);
        return this;
    }

    public void setVersionSourceId(UUID versionSourceId) {
        this.versionSourceId = versionSourceId;
    }

    public UUID getVersionCibleId() {
        return this.versionCibleId;
    }

    public ReferentialMigration versionCibleId(UUID versionCibleId) {
        this.setVersionCibleId(versionCibleId);
        return this;
    }

    public void setVersionCibleId(UUID versionCibleId) {
        this.versionCibleId = versionCibleId;
    }

    public EtatMigration getEtat() {
        return this.etat;
    }

    public ReferentialMigration etat(EtatMigration etat) {
        this.setEtat(etat);
        return this;
    }

    public void setEtat(EtatMigration etat) {
        this.etat = etat;
    }

    public String getRapport() {
        return this.rapport;
    }

    public ReferentialMigration rapport(String rapport) {
        this.setRapport(rapport);
        return this;
    }

    public void setRapport(String rapport) {
        this.rapport = rapport;
    }

    public Instant getDateDemande() {
        return this.dateDemande;
    }

    public ReferentialMigration dateDemande(Instant dateDemande) {
        this.setDateDemande(dateDemande);
        return this;
    }

    public void setDateDemande(Instant dateDemande) {
        this.dateDemande = dateDemande;
    }

    public UUID getDecideurId() {
        return this.decideurId;
    }

    public ReferentialMigration decideurId(UUID decideurId) {
        this.setDecideurId(decideurId);
        return this;
    }

    public void setDecideurId(UUID decideurId) {
        this.decideurId = decideurId;
    }

    public String getLiaisonsRetenues() {
        return this.liaisonsRetenues;
    }

    public ReferentialMigration liaisonsRetenues(String liaisonsRetenues) {
        this.setLiaisonsRetenues(liaisonsRetenues);
        return this;
    }

    public void setLiaisonsRetenues(String liaisonsRetenues) {
        this.liaisonsRetenues = liaisonsRetenues;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferentialMigration)) {
            return false;
        }
        return getId() != null && getId().equals(((ReferentialMigration) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferentialMigration{" +
            "id=" + getId() +
            ", versionSourceId='" + getVersionSourceId() + "'" +
            ", versionCibleId='" + getVersionCibleId() + "'" +
            ", etat='" + getEtat() + "'" +
            ", rapport='" + getRapport() + "'" +
            ", dateDemande='" + getDateDemande() + "'" +
            ", decideurId='" + getDecideurId() + "'" +
            ", liaisonsRetenues='" + getLiaisonsRetenues() + "'" +
            "}";
    }
}
