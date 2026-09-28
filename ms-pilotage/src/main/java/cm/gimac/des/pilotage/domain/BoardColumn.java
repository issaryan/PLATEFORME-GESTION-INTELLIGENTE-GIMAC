package cm.gimac.des.pilotage.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * Colonne d'un tableau (objet-valeur BoardColumn du CC, persisté en entité
 * pour porter le verrou de ligne de l'algorithme 6.54).
 */
@Entity
@Table(name = "board_column")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BoardColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Code de la colonne, par exemple A_FAIRE, EN_COURS, EN_REVUE, TERMINE.
     */
    @NotNull
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z_]+$")
    @Column(name = "code", length = 30, nullable = false)
    private String code;

    @NotNull
    @Size(max = 100)
    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @NotNull
    @Min(value = 0)
    @Column(name = "position", nullable = false)
    private Integer position;

    /**
     * Limite d'en-cours Lc ; 0 = colonne non bornée (RG-49).
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "wip_limit", nullable = false)
    private Integer wipLimit;

    /**
     * Colonne terminale : entrée soumise aux critères d'acceptation.
     */
    @NotNull
    @Column(name = "terminal", nullable = false)
    private Boolean terminal;

    @ManyToOne(optional = false)
    @NotNull
    private Board board;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public BoardColumn id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public BoardColumn code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return this.label;
    }

    public BoardColumn label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getPosition() {
        return this.position;
    }

    public BoardColumn position(Integer position) {
        this.setPosition(position);
        return this;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getWipLimit() {
        return this.wipLimit;
    }

    public BoardColumn wipLimit(Integer wipLimit) {
        this.setWipLimit(wipLimit);
        return this;
    }

    public void setWipLimit(Integer wipLimit) {
        this.wipLimit = wipLimit;
    }

    public Boolean getTerminal() {
        return this.terminal;
    }

    public BoardColumn terminal(Boolean terminal) {
        this.setTerminal(terminal);
        return this;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public Board getBoard() {
        return this.board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public BoardColumn board(Board board) {
        this.setBoard(board);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoardColumn)) {
            return false;
        }
        return getId() != null && getId().equals(((BoardColumn) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BoardColumn{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", label='" + getLabel() + "'" +
            ", position=" + getPosition() +
            ", wipLimit=" + getWipLimit() +
            ", terminal='" + getTerminal() + "'" +
            "}";
    }
}
