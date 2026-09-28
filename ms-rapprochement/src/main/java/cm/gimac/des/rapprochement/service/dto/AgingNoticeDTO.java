package cm.gimac.des.rapprochement.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.rapprochement.domain.AgingNotice} entity.
 */
@Schema(description = "Application d'une règle à un suspens, mémorisée pour n'avoir lieu qu'une fois.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AgingNoticeDTO implements Serializable {

    private UUID id;

    @NotNull
    private Instant appliedAt;

    @NotNull
    @Min(value = 0)
    private Integer ageInDays;

    @NotNull
    private SuspensDTO suspens;

    @NotNull
    private AgingRuleDTO rule;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Integer getAgeInDays() {
        return ageInDays;
    }

    public void setAgeInDays(Integer ageInDays) {
        this.ageInDays = ageInDays;
    }

    public SuspensDTO getSuspens() {
        return suspens;
    }

    public void setSuspens(SuspensDTO suspens) {
        this.suspens = suspens;
    }

    public AgingRuleDTO getRule() {
        return rule;
    }

    public void setRule(AgingRuleDTO rule) {
        this.rule = rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgingNoticeDTO)) {
            return false;
        }

        AgingNoticeDTO agingNoticeDTO = (AgingNoticeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, agingNoticeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AgingNoticeDTO{" +
            "id='" + getId() + "'" +
            ", appliedAt='" + getAppliedAt() + "'" +
            ", ageInDays=" + getAgeInDays() +
            ", suspens=" + getSuspens() +
            ", rule=" + getRule() +
            "}";
    }
}
