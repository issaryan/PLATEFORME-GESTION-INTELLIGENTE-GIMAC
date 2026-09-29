package cm.gimac.des.certification.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the {@link cm.gimac.des.certification.domain.GoLiveReport} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GoLiveReportDTO implements Serializable {

    private UUID id;

    @NotNull
    @Size(max = 120)
    private String documentRef;

    @NotNull
    @Size(max = 64)
    private String empreinte;

    @NotNull
    private Instant heureBascule;

    @NotNull
    private Boolean dansFenetre;

    private UUID deposantId;

    private Instant dateDepot;

    private Boolean regularise;

    @NotNull
    private GoLiveDossierDTO dossier;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public void setEmpreinte(String empreinte) {
        this.empreinte = empreinte;
    }

    public Instant getHeureBascule() {
        return heureBascule;
    }

    public void setHeureBascule(Instant heureBascule) {
        this.heureBascule = heureBascule;
    }

    public Boolean getDansFenetre() {
        return dansFenetre;
    }

    public void setDansFenetre(Boolean dansFenetre) {
        this.dansFenetre = dansFenetre;
    }

    public UUID getDeposantId() {
        return deposantId;
    }

    public void setDeposantId(UUID deposantId) {
        this.deposantId = deposantId;
    }

    public Instant getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(Instant dateDepot) {
        this.dateDepot = dateDepot;
    }

    public Boolean getRegularise() {
        return regularise;
    }

    public void setRegularise(Boolean regularise) {
        this.regularise = regularise;
    }

    public GoLiveDossierDTO getDossier() {
        return dossier;
    }

    public void setDossier(GoLiveDossierDTO dossier) {
        this.dossier = dossier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoLiveReportDTO)) {
            return false;
        }

        GoLiveReportDTO goLiveReportDTO = (GoLiveReportDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, goLiveReportDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GoLiveReportDTO{" +
            "id='" + getId() + "'" +
            ", documentRef='" + getDocumentRef() + "'" +
            ", empreinte='" + getEmpreinte() + "'" +
            ", heureBascule='" + getHeureBascule() + "'" +
            ", dansFenetre='" + getDansFenetre() + "'" +
            ", deposantId='" + getDeposantId() + "'" +
            ", dateDepot='" + getDateDepot() + "'" +
            ", regularise='" + getRegularise() + "'" +
            ", dossier=" + getDossier() +
            "}";
    }
}
