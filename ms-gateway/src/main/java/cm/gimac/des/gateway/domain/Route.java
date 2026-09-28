package cm.gimac.des.gateway.domain;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Route exposée par une couche vers un seul service cible (CC §6.1.1.3, GW-03).
 * Les permissions requises servent à réduire la portée du jeton interne (RFC 8693).
 */
@Table("route")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Route implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    /**
     * Motif de chemin, par exemple /api/participants/**.
     */
    @NotNull
    @Size(max = 255)
    @Column("path")
    private String path;

    /**
     * Service cible, par exemple ms-participant.
     */
    @NotNull
    @Size(max = 100)
    @Column("target_service")
    private String targetService;

    /**
     * Permissions requises, séparées par des virgules.
     */
    @NotNull
    @Size(max = 1024)
    @Column("required_permissions")
    private String requiredPermissions;

    /**
     * Quota propre à la route ; il prime sur celui de la couche.
     */
    @Min(value = 1)
    @Column("rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    /**
     * Active l'inspection WAF et le masquage des numéros de carte sur le corps.
     */
    @NotNull
    @Column("inspect_body")
    private Boolean inspectBody;

    @NotNull
    @Column("active")
    private Boolean active;

    @org.springframework.data.annotation.Transient
    private PresentationLayer layer;

    @Column("layer_id")
    private Long layerId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Route id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return this.path;
    }

    public Route path(String path) {
        this.setPath(path);
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTargetService() {
        return this.targetService;
    }

    public Route targetService(String targetService) {
        this.setTargetService(targetService);
        return this;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getRequiredPermissions() {
        return this.requiredPermissions;
    }

    public Route requiredPermissions(String requiredPermissions) {
        this.setRequiredPermissions(requiredPermissions);
        return this;
    }

    public void setRequiredPermissions(String requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
    }

    public Integer getRateLimitPerMinute() {
        return this.rateLimitPerMinute;
    }

    public Route rateLimitPerMinute(Integer rateLimitPerMinute) {
        this.setRateLimitPerMinute(rateLimitPerMinute);
        return this;
    }

    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public Boolean getInspectBody() {
        return this.inspectBody;
    }

    public Route inspectBody(Boolean inspectBody) {
        this.setInspectBody(inspectBody);
        return this;
    }

    public void setInspectBody(Boolean inspectBody) {
        this.inspectBody = inspectBody;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Route active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public PresentationLayer getLayer() {
        return this.layer;
    }

    public void setLayer(PresentationLayer presentationLayer) {
        this.layer = presentationLayer;
        this.layerId = presentationLayer != null ? presentationLayer.getId() : null;
    }

    public Route layer(PresentationLayer presentationLayer) {
        this.setLayer(presentationLayer);
        return this;
    }

    public Long getLayerId() {
        return this.layerId;
    }

    public void setLayerId(Long presentationLayer) {
        this.layerId = presentationLayer;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Route)) {
            return false;
        }
        return getId() != null && getId().equals(((Route) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Route{" +
            "id=" + getId() +
            ", path='" + getPath() + "'" +
            ", targetService='" + getTargetService() + "'" +
            ", requiredPermissions='" + getRequiredPermissions() + "'" +
            ", rateLimitPerMinute=" + getRateLimitPerMinute() +
            ", inspectBody='" + getInspectBody() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
