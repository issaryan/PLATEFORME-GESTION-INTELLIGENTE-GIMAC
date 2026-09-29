package cm.gimac.des.homologation.service.dto;

import cm.gimac.des.homologation.domain.enumeration.EtatDisjoncteur;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.ReplayCursor} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ReplayCursorDTO implements Serializable {

    private UUID id;

    @Size(max = 80)
    private String campagneId;

    private UUID liaisonId;

    @Size(max = 120)
    private String position;

    private Instant dateDernierSucces;

    private Integer echecsConsecutifs;

    private EtatDisjoncteur etatDisjoncteur;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public void setLiaisonId(UUID liaisonId) {
        this.liaisonId = liaisonId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Instant getDateDernierSucces() {
        return dateDernierSucces;
    }

    public void setDateDernierSucces(Instant dateDernierSucces) {
        this.dateDernierSucces = dateDernierSucces;
    }

    public Integer getEchecsConsecutifs() {
        return echecsConsecutifs;
    }

    public void setEchecsConsecutifs(Integer echecsConsecutifs) {
        this.echecsConsecutifs = echecsConsecutifs;
    }

    public EtatDisjoncteur getEtatDisjoncteur() {
        return etatDisjoncteur;
    }

    public void setEtatDisjoncteur(EtatDisjoncteur etatDisjoncteur) {
        this.etatDisjoncteur = etatDisjoncteur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReplayCursorDTO)) {
            return false;
        }

        ReplayCursorDTO replayCursorDTO = (ReplayCursorDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, replayCursorDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ReplayCursorDTO{" +
            "id='" + getId() + "'" +
            ", campagneId='" + getCampagneId() + "'" +
            ", liaisonId='" + getLiaisonId() + "'" +
            ", position='" + getPosition() + "'" +
            ", dateDernierSucces='" + getDateDernierSucces() + "'" +
            ", echecsConsecutifs=" + getEchecsConsecutifs() +
            ", etatDisjoncteur='" + getEtatDisjoncteur() + "'" +
            "}";
    }
}
