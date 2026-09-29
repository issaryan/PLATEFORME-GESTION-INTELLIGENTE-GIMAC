package cm.gimac.des.certification.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.TimestampToken} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TimestampTokenDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    private String objetType;

    @NotNull
    private UUID objetId;

    @NotNull
    @Size(max = 64)
    private String empreinte;

    @NotNull
    @Size(max = 64)
    private String nonce;

    @Lob
    private String jeton;

    @NotNull
    private Instant dateJeton;

    @NotNull
    private Boolean verifie;

    @Size(max = 500)
    private String autoriteChaine;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getObjetType() {
        return objetType;
    }

    public void setObjetType(String objetType) {
        this.objetType = objetType;
    }

    public UUID getObjetId() {
        return objetId;
    }

    public void setObjetId(UUID objetId) {
        this.objetId = objetId;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getJeton() {
        return jeton;
    }

    public void setJeton(String jeton) {
        this.jeton = jeton;
    }

    public Instant getDateJeton() {
        return dateJeton;
    }

    public void setDateJeton(Instant dateJeton) {
        this.dateJeton = dateJeton;
    }

    public Boolean getVerifie() {
        return verifie;
    }

    public void setVerifie(Boolean verifie) {
        this.verifie = verifie;
    }

    public String getAutoriteChaine() {
        return autoriteChaine;
    }

    public void setAutoriteChaine(String autoriteChaine) {
        this.autoriteChaine = autoriteChaine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimestampTokenDTO)) {
            return false;
        }

        TimestampTokenDTO timestampTokenDTO = (TimestampTokenDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, timestampTokenDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TimestampTokenDTO{" +
            "id='" + getId() + "'" +
            ", objetType='" + getObjetType() + "'" +
            ", objetId='" + getObjetId() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", nonce='" + getNonce() + "'" +
            ", jeton='" + getJeton() + "'" +
            ", dateJeton='" + getDateJeton() + "'" +
            ", verifie='" + getVerifie() + "'" +
            ", autoriteChaine='" + getAutoriteChaine() + "'" +
            "}";
    }
}
