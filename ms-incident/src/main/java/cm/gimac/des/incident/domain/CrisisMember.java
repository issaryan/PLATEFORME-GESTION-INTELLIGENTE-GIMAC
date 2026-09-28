package cm.gimac.des.incident.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Membre de la cellule, de droit ou suppléant (algorithme 6.42, étapes 1 et 4).
 */
@Entity
@Table(name = "crisis_member")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CrisisMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Rôle de droit, par exemple « chef du DES ».
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "role", length = 100, nullable = false)
    private String role;

    /**
     * Poids dans le quorum : 2 pour la Direction Générale et le chef du DES, 1 sinon.
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "weight", nullable = false)
    private Integer weight;

    /**
     * Titulaire remplacé, pour un suppléant.
     */
    @Column(name = "substitute_for")
    private UUID substituteFor;

    @NotNull
    @Column(name = "convened_at", nullable = false)
    private Instant convenedAt;

    /**
     * Présence effective ; l'acquittement de la convocation ne vaut pas présence.
     */
    @Column(name = "joined_at")
    private Instant joinedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "incident" }, allowSetters = true)
    private CrisisCell cell;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public CrisisMember id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public CrisisMember userId(UUID userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return this.role;
    }

    public CrisisMember role(String role) {
        this.setRole(role);
        return this;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getWeight() {
        return this.weight;
    }

    public CrisisMember weight(Integer weight) {
        this.setWeight(weight);
        return this;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public UUID getSubstituteFor() {
        return this.substituteFor;
    }

    public CrisisMember substituteFor(UUID substituteFor) {
        this.setSubstituteFor(substituteFor);
        return this;
    }

    public void setSubstituteFor(UUID substituteFor) {
        this.substituteFor = substituteFor;
    }

    public Instant getConvenedAt() {
        return this.convenedAt;
    }

    public CrisisMember convenedAt(Instant convenedAt) {
        this.setConvenedAt(convenedAt);
        return this;
    }

    public void setConvenedAt(Instant convenedAt) {
        this.convenedAt = convenedAt;
    }

    public Instant getJoinedAt() {
        return this.joinedAt;
    }

    public CrisisMember joinedAt(Instant joinedAt) {
        this.setJoinedAt(joinedAt);
        return this;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public CrisisCell getCell() {
        return this.cell;
    }

    public void setCell(CrisisCell crisisCell) {
        this.cell = crisisCell;
    }

    public CrisisMember cell(CrisisCell crisisCell) {
        this.setCell(crisisCell);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CrisisMember)) {
            return false;
        }
        return getId() != null && getId().equals(((CrisisMember) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CrisisMember{" +
            "id=" + getId() +
            ", userId='" + getUserId() + "'" +
            ", role='" + getRole() + "'" +
            ", weight=" + getWeight() +
            ", substituteFor='" + getSubstituteFor() + "'" +
            ", convenedAt='" + getConvenedAt() + "'" +
            ", joinedAt='" + getJoinedAt() + "'" +
            "}";
    }
}
