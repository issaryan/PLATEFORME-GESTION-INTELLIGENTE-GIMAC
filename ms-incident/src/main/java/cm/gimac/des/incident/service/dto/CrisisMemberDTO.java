package cm.gimac.des.incident.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.incident.domain.CrisisMember} entity.
 */
@Schema(description = "Membre de la cellule, de droit ou suppléant (algorithme 6.42, étapes 1 et 4).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisMemberDTO implements Serializable {

    private UUID id;

    @NotNull
    private UUID userId;

    @NotNull
    @Size(max = 100)
    @Schema(description = "Rôle de droit, par exemple « chef du DES ».", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;

    @NotNull
    @Min(value = 0)
    @Schema(
        description = "Poids dans le quorum : 2 pour la Direction Générale et le chef du DES, 1 sinon.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer weight;

    @Schema(description = "Titulaire remplacé, pour un suppléant.")
    private UUID substituteFor;

    @NotNull
    private Instant convenedAt;

    @Schema(description = "Présence effective ; l'acquittement de la convocation ne vaut pas présence.")
    private Instant joinedAt;

    @NotNull
    private CrisisCellDTO cell;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public UUID getSubstituteFor() {
        return substituteFor;
    }

    public void setSubstituteFor(UUID substituteFor) {
        this.substituteFor = substituteFor;
    }

    public Instant getConvenedAt() {
        return convenedAt;
    }

    public void setConvenedAt(Instant convenedAt) {
        this.convenedAt = convenedAt;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public CrisisCellDTO getCell() {
        return cell;
    }

    public void setCell(CrisisCellDTO cell) {
        this.cell = cell;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisMemberDTO)) {
            return false;
        }

        CrisisMemberDTO crisisMemberDTO = (CrisisMemberDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, crisisMemberDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisMemberDTO{" +
            "id='" + getId() + "'" +
            ", userId='" + getUserId() + "'" +
            ", role='" + getRole() + "'" +
            ", weight=" + getWeight() +
            ", substituteFor='" + getSubstituteFor() + "'" +
            ", convenedAt='" + getConvenedAt() + "'" +
            ", joinedAt='" + getJoinedAt() + "'" +
            ", cell=" + getCell() +
            "}";
    }
}
