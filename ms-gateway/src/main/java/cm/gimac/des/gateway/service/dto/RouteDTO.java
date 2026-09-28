package cm.gimac.des.gateway.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link cm.gimac.des.gateway.domain.Route} entity.
 */
@Schema(
    description = "Route exposée par une couche vers un seul service cible (CC §6.1.1.3, GW-03).\nLes permissions requises servent à réduire la portée du jeton interne (RFC 8693)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RouteDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Motif de chemin, par exemple /api/participants/**.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String path;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Service cible, par exemple ms-participant.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetService;

    @NotNull
    @Size(max = 1024)
    @Schema(description = "Permissions requises, séparées par des virgules.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requiredPermissions;

    @Min(value = 1)
    @Schema(description = "Quota propre à la route ; il prime sur celui de la couche.")
    private Integer rateLimitPerMinute;

    @NotNull
    @Schema(
        description = "Active l'inspection WAF et le masquage des numéros de carte sur le corps.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean inspectBody;

    @NotNull
    private Boolean active;

    @NotNull
    private PresentationLayerDTO layer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getRequiredPermissions() {
        return requiredPermissions;
    }

    public void setRequiredPermissions(String requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
    }

    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public Boolean getInspectBody() {
        return inspectBody;
    }

    public void setInspectBody(Boolean inspectBody) {
        this.inspectBody = inspectBody;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public PresentationLayerDTO getLayer() {
        return layer;
    }

    public void setLayer(PresentationLayerDTO layer) {
        this.layer = layer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RouteDTO)) {
            return false;
        }

        RouteDTO routeDTO = (RouteDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, routeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RouteDTO{" +
            "id=" + getId() +
            ", path='" + getPath() + "'" +
            ", targetService='" + getTargetService() + "'" +
            ", requiredPermissions='" + getRequiredPermissions() + "'" +
            ", rateLimitPerMinute=" + getRateLimitPerMinute() +
            ", inspectBody='" + getInspectBody() + "'" +
            ", active='" + getActive() + "'" +
            ", layer=" + getLayer() +
            "}";
    }
}
