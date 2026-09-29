package cm.gimac.des.homologation.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.homologation.domain.EvaluationBase} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvaluationBaseDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 64)
    private String empreinte;

    @NotNull
    private Integer cycle;

    @NotNull
    private Instant dateCalcul;

    @Lob
    private String composition;

    @NotNull
    private LiaisonDTO liaison;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public Integer getCycle() {
        return cycle;
    }

    public void setCycle(Integer cycle) {
        this.cycle = cycle;
    }

    public Instant getDateCalcul() {
        return dateCalcul;
    }

    public void setDateCalcul(Instant dateCalcul) {
        this.dateCalcul = dateCalcul;
    }

    public String getComposition() {
        return composition;
    }

    public void setComposition(String composition) {
        this.composition = composition;
    }

    public LiaisonDTO getLiaison() {
        return liaison;
    }

    public void setLiaison(LiaisonDTO liaison) {
        this.liaison = liaison;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationBaseDTO)) {
            return false;
        }

        EvaluationBaseDTO evaluationBaseDTO = (EvaluationBaseDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, evaluationBaseDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvaluationBaseDTO{" +
            "id='" + getId() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", cycle=" + getCycle() +
            ", dateCalcul='" + getDateCalcul() + "'" +
            ", composition='" + getComposition() + "'" +
            ", liaison=" + getLiaison() +
            "}";
    }
}
