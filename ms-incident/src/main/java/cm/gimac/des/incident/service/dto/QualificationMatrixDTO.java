package cm.gimac.des.incident.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.QualificationMatrix} entity.
 */
@Schema(
    description = "Matrice de qualification impact × urgence, versionnée (objet-valeur du CC,\npersisté car paramétrable). Une version déjà appliquée n'est jamais modifiée."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class QualificationMatrixDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 20)
    @Schema(description = "Version, par exemple MQ-2026.1.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String matrixVersion;

    @Schema(description = "Tableau JSON 5×5 des sévérités, indexé [impact][urgence].", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String cells;

    @Schema(description = "Liste JSON des règles du marqueur réglementaire (seuil Dreg...).")
    @Lob
    private String regulatoryRules;

    @NotNull
    private Instant effectiveFrom;

    @NotNull
    private Boolean active;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMatrixVersion() {
        return matrixVersion;
    }

    public void setMatrixVersion(String matrixVersion) {
        this.matrixVersion = matrixVersion;
    }

    public String getCells() {
        return cells;
    }

    public void setCells(String cells) {
        this.cells = cells;
    }

    public String getRegulatoryRules() {
        return regulatoryRules;
    }

    public void setRegulatoryRules(String regulatoryRules) {
        this.regulatoryRules = regulatoryRules;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
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
        if (!(o instanceof QualificationMatrixDTO)) {
            return false;
        }

        QualificationMatrixDTO qualificationMatrixDTO = (QualificationMatrixDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, qualificationMatrixDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "QualificationMatrixDTO{" +
            "id='" + getId() + "'" +
            ", matrixVersion='" + getMatrixVersion() + "'" +
            ", cells='" + getCells() + "'" +
            ", regulatoryRules='" + getRegulatoryRules() + "'" +
            ", effectiveFrom='" + getEffectiveFrom() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
