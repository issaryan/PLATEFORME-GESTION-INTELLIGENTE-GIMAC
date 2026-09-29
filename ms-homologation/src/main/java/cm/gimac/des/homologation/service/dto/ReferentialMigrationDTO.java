package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.EtatMigration;
import jakarta.persistence.Lob;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ReferentialMigration} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReferentialMigrationDTO implements Serializable {

    private UUID id;

    private UUID versionSourceId;

    private UUID versionCibleId;

    private EtatMigration etat;

    @Lob
    private String rapport;

    private Instant dateDemande;

    private UUID decideurId;

    @Lob
    private String liaisonsRetenues;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVersionSourceId() {
        return versionSourceId;
    }

    public void setVersionSourceId(UUID versionSourceId) {
        this.versionSourceId = versionSourceId;
    }

    public UUID getVersionCibleId() {
        return versionCibleId;
    }

    public void setVersionCibleId(UUID versionCibleId) {
        this.versionCibleId = versionCibleId;
    }

    public EtatMigration getEtat() {
        return etat;
    }

    public void setEtat(EtatMigration etat) {
        this.etat = etat;
    }

    public String getRapport() {
        return rapport;
    }

    public void setRapport(String rapport) {
        this.rapport = rapport;
    }

    public Instant getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Instant dateDemande) {
        this.dateDemande = dateDemande;
    }

    public UUID getDecideurId() {
        return decideurId;
    }

    public void setDecideurId(UUID decideurId) {
        this.decideurId = decideurId;
    }

    public String getLiaisonsRetenues() {
        return liaisonsRetenues;
    }

    public void setLiaisonsRetenues(String liaisonsRetenues) {
        this.liaisonsRetenues = liaisonsRetenues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReferentialMigrationDTO)) {
            return false;
        }

        ReferentialMigrationDTO referentialMigrationDTO = (ReferentialMigrationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, referentialMigrationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReferentialMigrationDTO{" +
            "id='" + getId() + "'" +
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
