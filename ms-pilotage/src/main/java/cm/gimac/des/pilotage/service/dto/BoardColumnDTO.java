package cm.gimac.des.pilotage.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.pilotage.domain.BoardColumn} entity.
 */
@Schema(
    description = "Colonne d'un tableau (objet-valeur BoardColumn du CC, persisté en entité\npour porter le verrou de ligne de l'algorithme 6.54)."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BoardColumnDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Schema(
        description = "Code de la colonne, par exemple A_FAIRE, EN_COURS, EN_REVUE, TERMINE.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    @NotNull
    @Size(max = 100)
    private String label;

    @NotNull
    @Min(value = 0)
    private Integer position;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Limite d'en-cours Lc ; 0 = colonne non bornée (RG-49).", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer wipLimit;

    @NotNull
    @Schema(description = "Colonne terminale : entrée soumise aux critères d'acceptation.", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean terminal;

    @NotNull
    private BoardDTO board;

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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getWipLimit() {
        return wipLimit;
    }

    public void setWipLimit(Integer wipLimit) {
        this.wipLimit = wipLimit;
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public BoardDTO getBoard() {
        return board;
    }

    public void setBoard(BoardDTO board) {
        this.board = board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoardColumnDTO)) {
            return false;
        }

        BoardColumnDTO boardColumnDTO = (BoardColumnDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, boardColumnDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BoardColumnDTO{" +
            "id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", position=" + getPosition() +
            ", wipLimit=" + getWipLimit() +
            ", terminal='" + getTerminal() + "'" +
            ", board=" + getBoard() +
            "}";
    }
}
