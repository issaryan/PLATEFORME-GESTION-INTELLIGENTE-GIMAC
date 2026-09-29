package cm.gimac.des.certification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A GoLiveReport.
 */
@Entity
@Table(name = "go_live_report")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GoLiveReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Size(max = 120)
    @Column(name = "document_ref", length = 120, nullable = false)
    private String documentRef;

    @NotNull
    @Size(max = 64)
    @Column(name = "empreinte", length = 64, nullable = false)
    private String empreinte;

    @NotNull
    @Column(name = "heure_bascule", nullable = false)
    private Instant heureBascule;

    @NotNull
    @Column(name = "dans_fenetre", nullable = false)
    private Boolean dansFenetre;

    @Column(name = "deposant_id")
    private UUID deposantId;

    @Column(name = "date_depot")
    private Instant dateDepot;

    @Column(name = "regularise")
    private Boolean regularise;

    @JsonIgnoreProperties(value = { "circuit", "goLiveReport", "attestation" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private GoLiveDossier dossier;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public GoLiveReport id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentRef() {
        return this.documentRef;
    }

    public GoLiveReport documentRef(String documentRef) {
        this.setDocumentRef(documentRef);
        return this;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getEmpreinte() {
        return this.empreinte;
    }

    public GoLiveReport empreinte(String empreinte) {
        this.setEmpreinte(empreinte);
        return this;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public Instant getHeureBascule() {
        return this.heureBascule;
    }

    public GoLiveReport heureBascule(Instant heureBascule) {
        this.setHeureBascule(heureBascule);
        return this;
    }

    public void setHeureBascule(Instant heureBascule) {
        this.heureBascule = heureBascule;
    }

    public Boolean getDansFenetre() {
        return this.dansFenetre;
    }

    public GoLiveReport dansFenetre(Boolean dansFenetre) {
        this.setDansFenetre(dansFenetre);
        return this;
    }

    public void setDansFenetre(Boolean dansFenetre) {
        this.dansFenetre = dansFenetre;
    }

    public UUID getDeposantId() {
        return this.deposantId;
    }

    public GoLiveReport deposantId(UUID deposantId) {
        this.setDeposantId(deposantId);
        return this;
    }

    public void setDeposantId(UUID deposantId) {
        this.deposantId = deposantId;
    }

    public Instant getDateDepot() {
        return this.dateDepot;
    }

    public GoLiveReport dateDepot(Instant dateDepot) {
        this.setDateDepot(dateDepot);
        return this;
    }

    public void setDateDepot(Instant dateDepot) {
        this.dateDepot = dateDepot;
    }

    public Boolean getRegularise() {
        return this.regularise;
    }

    public GoLiveReport regularise(Boolean regularise) {
        this.setRegularise(regularise);
        return this;
    }

    public void setRegularise(Boolean regularise) {
        this.regularise = regularise;
    }

    public GoLiveDossier getDossier() {
        return this.dossier;
    }

    public void setDossier(GoLiveDossier goLiveDossier) {
        this.dossier = goLiveDossier;
    }

    public GoLiveReport dossier(GoLiveDossier goLiveDossier) {
        this.setDossier(goLiveDossier);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoLiveReport)) {
            return false;
        }
        return getId() != null && getId().equals(((GoLiveReport) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GoLiveReport{" +
            "id=" + getId() +
            ", documentRef='" + getDocumentRef() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", heureBascule='" + getHeureBascule() + "'" +
            ", dansFenetre='" + getDansFenetre() + "'" +
            ", deposantId='" + getDeposantId() + "'" +
            ", dateDepot='" + getDateDepot() + "'" +
            ", regularise='" + getRegularise() + "'" +
            "}";
    }
}
