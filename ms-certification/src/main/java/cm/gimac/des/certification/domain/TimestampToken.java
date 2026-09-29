package cm.gimac.des.certification.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A TimestampToken.
 */
@Entity
@Table(name = "timestamp_token")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TimestampToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 20)
    @Column(name = "objet_type", length = 20, nullable = false)
    private String objetType;

    @NotNull
    @Column(name = "objet_id", nullable = false)
    private UUID objetId;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte", length = 64, nullable = false)
    private String empreinte;

    @NotNull
    @Size(max = 64)
    @Column(name = "nonce", length = 64, nullable = false)
    private String nonce;

    @Lob
    @Column(name = "jeton", nullable = false)
    private String jeton;

    @NotNull
    @Column(name = "date_jeton", nullable = false)
    private Instant dateJeton;

    @NotNull
    @Column(name = "verifie", nullable = false)
    private Boolean verifie;

    @Size(max = 500)
    @Column(name = "autorite_chaine", length = 500)
    private String autoriteChaine;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public TimestampToken id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getObjetType() {
        return this.objetType;
    }

    public TimestampToken objetType(String objetType) {
        this.setObjetType(objetType);
        return this;
    }

    public void setObjetType(String objetType) {
        this.objetType = objetType;
    }

    public UUID getObjetId() {
        return this.objetId;
    }

    public TimestampToken objetId(UUID objetId) {
        this.setObjetId(objetId);
        return this;
    }

    public void setObjetId(UUID objetId) {
        this.objetId = objetId;
    }

    public String getEmpreinte() {
        return this.empreinte;
    }

    public TimestampToken empreinte(String empreinte) {
        this.setEmpreinte(empreinte);
        return this;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public String getNonce() {
        return this.nonce;
    }

    public TimestampToken nonce(String nonce) {
        this.setNonce(nonce);
        return this;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getJeton() {
        return this.jeton;
    }

    public TimestampToken jeton(String jeton) {
        this.setJeton(jeton);
        return this;
    }

    public void setJeton(String jeton) {
        this.jeton = jeton;
    }

    public Instant getDateJeton() {
        return this.dateJeton;
    }

    public TimestampToken dateJeton(Instant dateJeton) {
        this.setDateJeton(dateJeton);
        return this;
    }

    public void setDateJeton(Instant dateJeton) {
        this.dateJeton = dateJeton;
    }

    public Boolean getVerifie() {
        return this.verifie;
    }

    public TimestampToken verifie(Boolean verifie) {
        this.setVerifie(verifie);
        return this;
    }

    public void setVerifie(Boolean verifie) {
        this.verifie = verifie;
    }

    public String getAutoriteChaine() {
        return this.autoriteChaine;
    }

    public TimestampToken autoriteChaine(String autoriteChaine) {
        this.setAutoriteChaine(autoriteChaine);
        return this;
    }

    public void setAutoriteChaine(String autoriteChaine) {
        this.autoriteChaine = autoriteChaine;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimestampToken)) {
            return false;
        }
        return getId() != null && getId().equals(((TimestampToken) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TimestampToken{" +
            "id=" + getId() +
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
