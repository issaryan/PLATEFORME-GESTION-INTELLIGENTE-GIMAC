package cm.gimac.des.reporting.service.dto;

import cm.gimac.des.reporting.domain.enumeration.FactDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.reporting.domain.ProjectionRule} entity.
 */
@Schema(
    description = "Règle de projection d'un sujet en fait (table R de l'algorithme 6.56) :\ndomaine cible, mesures à extraire et champs alimentant les dimensions.\nUn sujet sans règle est acquitté sans traitement."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProjectionRuleDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 150)
    @Schema(description = "Sujet du bus, par exemple incident.incident-resolved.v1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @NotNull
    private FactDomain domain;

    @Schema(
        description = "Extraction des mesures : JSON (compteur, durée entre deux instants, montant agrégé).",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String measureMapping;

    @Schema(
        description = "Correspondance des champs de la charge utile vers les dimensions : JSON.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Lob
    private String dimensionMapping;

    @Size(max = 1000)
    @Schema(description = "Codes des indicateurs auxquels le sujet contribue, séparés par des virgules.")
    private String indicatorCodes;

    @NotNull
    private Boolean active;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public FactDomain getDomain() {
        return domain;
    }

    public void setDomain(FactDomain domain) {
        this.domain = domain;
    }

    public String getMeasureMapping() {
        return measureMapping;
    }

    public void setMeasureMapping(String measureMapping) {
        this.measureMapping = measureMapping;
    }

    public String getDimensionMapping() {
        return dimensionMapping;
    }

    public void setDimensionMapping(String dimensionMapping) {
        this.dimensionMapping = dimensionMapping;
    }

    public String getIndicatorCodes() {
        return indicatorCodes;
    }

    public void setIndicatorCodes(String indicatorCodes) {
        this.indicatorCodes = indicatorCodes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectionRuleDTO)) {
            return false;
        }

        ProjectionRuleDTO projectionRuleDTO = (ProjectionRuleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, projectionRuleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ProjectionRuleDTO{" +
            "id='" + getId() + "'" +
            ", subject='" + getSubject() + "'" +
            ", domain='" + getDomain() + "'" +
            ", measureMapping='" + getMeasureMapping() + "'" +
            ", dimensionMapping='" + getDimensionMapping() + "'" +
            ", indicatorCodes='" + getIndicatorCodes() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
