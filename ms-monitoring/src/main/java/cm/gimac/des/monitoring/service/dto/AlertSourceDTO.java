package cm.gimac.des.monitoring.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.monitoring.domain.AlertSource} entity.
 */
@Schema(
    description = "Source émettrice enregistrée (pile d'observabilité, système de production),\navec son certificat et son schéma en liste blanche (D-23 ; écart CC complété)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AlertSourceDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 50)
    private String code;

    @NotNull
    @Size(max = 255)
    private String label;

    @NotNull
    @Size(min = 64, max = 64)
    @Pattern(regexp = "^[0-9a-f]{64}$")
    @Schema(description = "Empreinte SHA-256 du certificat client mTLS.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String certificateThumbprint;

    @Schema(description = "Schéma JSON du message, champs admis en liste blanche.", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String allowedSchema;

    @NotNull
    private Boolean active;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCertificateThumbprint() {
        return certificateThumbprint;
    }

    public void setCertificateThumbprint(String certificateThumbprint) {
        this.certificateThumbprint = certificateThumbprint;
    }

    public String getAllowedSchema() {
        return allowedSchema;
    }

    public void setAllowedSchema(String allowedSchema) {
        this.allowedSchema = allowedSchema;
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
        if (!(o instanceof AlertSourceDTO)) {
            return false;
        }

        AlertSourceDTO alertSourceDTO = (AlertSourceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, alertSourceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AlertSourceDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", certificateThumbprint='" + getCertificateThumbprint() + "'" +
            ", allowedSchema='" + getAllowedSchema() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
