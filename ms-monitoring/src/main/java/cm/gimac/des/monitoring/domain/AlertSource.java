package cm.gimac.des.monitoring.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Source émettrice enregistrée (pile d'observabilité, système de production),
 * avec son certificat et son schéma en liste blanche (D-23 ; écart CC complété).
 */
@Entity
@Table(name = "alert_source")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 50)
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 255)
    @Column(name = "label", length = 255, nullable = false)
    private String label;

    /**
     * Empreinte SHA-256 du certificat client mTLS.
     */
    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Column(name = "certificate_thumbprint", length = 64, nullable = false)
    private String certificateThumbprint;

    /**
     * Schéma JSON du message, champs admis en liste blanche.
     */
    @Lob
    @Column(name = "allowed_schema", nullable = false)
    private String allowedSchema;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public AlertSource id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public AlertSource code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public AlertSource label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCertificateThumbprint() {
        return this.certificateThumbprint;
    }

    public AlertSource certificateThumbprint(String certificateThumbprint) {
        this.setCertificateThumbprint(certificateThumbprint);
        return this;
    }

    public void setCertificateThumbprint(String certificateThumbprint) {
        this.certificateThumbprint = certificateThumbprint;
    }

    public String getAllowedSchema() {
        return this.allowedSchema;
    }

    public AlertSource allowedSchema(String allowedSchema) {
        this.setAllowedSchema(allowedSchema);
        return this;
    }

    public void setAllowedSchema(String allowedSchema) {
        this.allowedSchema = allowedSchema;
    }

    public Boolean getActive() {
        return this.active;
    }

    public AlertSource active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AlertSource)) {
            return false;
        }
        return getId() != null && getId().equals(((AlertSource) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertSource{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", certificateThumbprint='" + getCertificateThumbprint() + "'" +
            ", allowedSchema='" + getAllowedSchema() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
