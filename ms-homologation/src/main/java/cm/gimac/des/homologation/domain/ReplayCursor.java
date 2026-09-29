package cm.gimac.des.homologation.domain;

import cm.gimac.des.homologation.domain.enumeration.EtatDisjoncteur;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A ReplayCursor.
 */
@Entity
@Table(name = "replay_cursor")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReplayCursor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Size(max = 80)
    @Column(name = "campagne_id", length = 80)
    private String campagneId;

    @Column(name = "liaison_id")
    private UUID liaisonId;

    @Size(max = 120)
    @Column(name = "position", length = 120)
    private String position;

    @Column(name = "date_dernier_succes")
    private Instant dateDernierSucces;

    @Column(name = "echecs_consecutifs")
    private Integer echecsConsecutifs;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_disjoncteur")
    private EtatDisjoncteur etatDisjoncteur;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public ReplayCursor id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampagneId() {
        return this.campagneId;
    }

    public ReplayCursor campagneId(String campagneId) {
        this.setCampagneId(campagneId);
        return this;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public UUID getLiaisonId() {
        return this.liaisonId;
    }

    public ReplayCursor liaisonId(UUID liaisonId) {
        this.setLiaisonId(liaisonId);
        return this;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public String getPosition() {
        return this.position;
    }

    public ReplayCursor position(String position) {
        this.setPosition(position);
        return this;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Instant getDateDernierSucces() {
        return this.dateDernierSucces;
    }

    public ReplayCursor dateDernierSucces(Instant dateDernierSucces) {
        this.setDateDernierSucces(dateDernierSucces);
        return this;
    }

    public void setDateDernierSucces(Instant dateDernierSucces) {
        this.dateDernierSucces = dateDernierSucces;
    }

    public Integer getEchecsConsecutifs() {
        return this.echecsConsecutifs;
    }

    public ReplayCursor echecsConsecutifs(Integer echecsConsecutifs) {
        this.setEchecsConsecutifs(echecsConsecutifs);
        return this;
    }

    public void setEchecsConsecutifs(Integer echecsConsecutifs) {
        this.echecsConsecutifs = echecsConsecutifs;
    }

    public EtatDisjoncteur getEtatDisjoncteur() {
        return this.etatDisjoncteur;
    }

    public ReplayCursor etatDisjoncteur(EtatDisjoncteur etatDisjoncteur) {
        this.setEtatDisjoncteur(etatDisjoncteur);
        return this;
    }

    public void setEtatDisjoncteur(EtatDisjoncteur etatDisjoncteur) {
        this.etatDisjoncteur = etatDisjoncteur;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReplayCursor)) {
            return false;
        }
        return getId() != null && getId().equals(((ReplayCursor) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReplayCursor{" +
            "id=" + getId() +
            ", campagneId='" + getCampagneId() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", position='" + getPosition() + "'" +
            ", dateDernierSucces='" + getDateDernierSucces() + "'" +
            ", echecsConsecutifs=" + getEchecsConsecutifs() +
            ", etatDisjoncteur='" + getEtatDisjoncteur() + "'" +
            "}";
    }
}
