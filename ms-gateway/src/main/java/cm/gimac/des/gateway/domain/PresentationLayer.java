package cm.gimac.des.gateway.domain;

import cm.gimac.des.gateway.domain.enumeration.NetworkZone;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Couche de présentation connue de la passerelle (CC §6.1.1.3).
 * Trois enregistrements attendus : département, portail, console.
 */
@Table("presentation_layer")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PresentationLayer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    /**
     * Code unique de la couche : DEPARTEMENT, PORTAIL ou CONSOLE.
     */
    @NotNull
    @Size(max = 50)
    @Column("name")
    private String name;

    @NotNull
    @Column("zone")
    private NetworkZone zone;

    /**
     * Exigence d'authentification, par exemple \"OIDC+FIDO2+mTLS\".
     */
    @NotNull
    @Size(max = 255)
    @Column("authentication_requirement")
    private String authenticationRequirement;

    /**
     * Préfixe des routes BFF de la couche, par exemple /bff/des.
     */
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^\\/[a-z0-9\\/-]*$")
    @Column("bff_prefix")
    private String bffPrefix;

    /**
     * Quota par défaut en requêtes par minute : 500, 100 ou 1000 (algorithme 6.1).
     */
    @NotNull
    @Min(value = 1)
    @Column("default_rate_limit_per_minute")
    private Integer defaultRateLimitPerMinute;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public PresentationLayer id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public PresentationLayer name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NetworkZone getZone() {
        return this.zone;
    }

    public PresentationLayer zone(NetworkZone zone) {
        this.setZone(zone);
        return this;
    }

    public void setZone(NetworkZone zone) {
        this.zone = zone;
    }

    public String getAuthenticationRequirement() {
        return this.authenticationRequirement;
    }

    public PresentationLayer authenticationRequirement(String authenticationRequirement) {
        this.setAuthenticationRequirement(authenticationRequirement);
        return this;
    }

    public void setAuthenticationRequirement(String authenticationRequirement) {
        this.authenticationRequirement = authenticationRequirement;
    }

    public String getBffPrefix() {
        return this.bffPrefix;
    }

    public PresentationLayer bffPrefix(String bffPrefix) {
        this.setBffPrefix(bffPrefix);
        return this;
    }

    public void setBffPrefix(String bffPrefix) {
        this.bffPrefix = bffPrefix;
    }

    public Integer getDefaultRateLimitPerMinute() {
        return this.defaultRateLimitPerMinute;
    }

    public PresentationLayer defaultRateLimitPerMinute(Integer defaultRateLimitPerMinute) {
        this.setDefaultRateLimitPerMinute(defaultRateLimitPerMinute);
        return this;
    }

    public void setDefaultRateLimitPerMinute(Integer defaultRateLimitPerMinute) {
        this.defaultRateLimitPerMinute = defaultRateLimitPerMinute;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PresentationLayer)) {
            return false;
        }
        return getId() != null && getId().equals(((PresentationLayer) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PresentationLayer{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", zone='" + getZone() + "'" +
            ", authenticationRequirement='" + getAuthenticationRequirement() + "'" +
            ", bffPrefix='" + getBffPrefix() + "'" +
            ", defaultRateLimitPerMinute=" + getDefaultRateLimitPerMinute() +
            "}";
    }
}
