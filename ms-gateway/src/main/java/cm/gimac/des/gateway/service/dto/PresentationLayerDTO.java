package cm.gimac.des.gateway.service.dto;

import cm.gimac.des.gateway.domain.enumeration.NetworkZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link cm.gimac.des.gateway.domain.PresentationLayer} entity.
 */
@Schema(
    description = "Couche de présentation connue de la passerelle (CC §6.1.1.3).\nTrois enregistrements attendus : département, portail, console."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PresentationLayerDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 50)
    @Schema(description = "Code unique de la couche : DEPARTEMENT, PORTAIL ou CONSOLE.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    private NetworkZone zone;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Exigence d'authentification, par exemple \"OIDC+FIDO2+mTLS\".", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authenticationRequirement;

    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^\\/[a-z0-9\\/-]*$")
    @Schema(description = "Préfixe des routes BFF de la couche, par exemple /bff/des.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bffPrefix;

    @NotNull
    @Min(value = 1)
    @Schema(
        description = "Quota par défaut en requêtes par minute : 500, 100 ou 1000 (algorithme 6.1).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer defaultRateLimitPerMinute;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NetworkZone getZone() {
        return zone;
    }

    public void setZone(NetworkZone zone) {
        this.zone = zone;
    }

    public String getAuthenticationRequirement() {
        return authenticationRequirement;
    }

    public void setAuthenticationRequirement(String authenticationRequirement) {
        this.authenticationRequirement = authenticationRequirement;
    }

    public String getBffPrefix() {
        return bffPrefix;
    }

    public void setBffPrefix(String bffPrefix) {
        this.bffPrefix = bffPrefix;
    }

    public Integer getDefaultRateLimitPerMinute() {
        return defaultRateLimitPerMinute;
    }

    public void setDefaultRateLimitPerMinute(Integer defaultRateLimitPerMinute) {
        this.defaultRateLimitPerMinute = defaultRateLimitPerMinute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PresentationLayerDTO)) {
            return false;
        }

        PresentationLayerDTO presentationLayerDTO = (PresentationLayerDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, presentationLayerDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PresentationLayerDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", zone='" + getZone() + "'" +
            ", authenticationRequirement='" + getAuthenticationRequirement() + "'" +
            ", bffPrefix='" + getBffPrefix() + "'" +
            ", defaultRateLimitPerMinute=" + getDefaultRateLimitPerMinute() +
            "}";
    }
}
