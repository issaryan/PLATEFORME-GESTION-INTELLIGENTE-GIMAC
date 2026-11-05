package cm.gimac.des.homologation.domain.parcours;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service du périmètre d'une liaison, suivi de bout en bout : essais (entrée, sortie), certification,
 * ouverture déclarée par le participant sur la fiche de suivi (rang 32).
 */
@Entity
@Table(name = "parcours_service")
public class ServiceParcours {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "inclus", nullable = false)
    private boolean inclus;

    @Column(name = "entree_essai")
    private LocalDate entreeEssai;

    @Column(name = "sortie_essai")
    private LocalDate sortieEssai;

    @Column(name = "certifie_le")
    private LocalDate certifieLe;

    @Column(name = "ouvert")
    private Boolean ouvert;

    @Column(name = "commentaire")
    private String commentaire;

    protected ServiceParcours() {}

    public ServiceParcours(UUID liaisonId, String serviceCode, String libelle, boolean inclus) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
        this.serviceCode = serviceCode;
        this.libelle = libelle;
        this.inclus = inclus;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean isInclus() {
        return inclus;
    }

    /** Inclusion au périmètre, changée par le seul cas de modification du périmètre (D-133). */
    public void setInclus(boolean inclus) {
        this.inclus = inclus;
    }

    public LocalDate getEntreeEssai() {
        return entreeEssai;
    }

    public void setEntreeEssai(LocalDate entreeEssai) {
        this.entreeEssai = entreeEssai;
    }

    public LocalDate getSortieEssai() {
        return sortieEssai;
    }

    public void setSortieEssai(LocalDate sortieEssai) {
        this.sortieEssai = sortieEssai;
    }

    public LocalDate getCertifieLe() {
        return certifieLe;
    }

    public void setCertifieLe(LocalDate certifieLe) {
        this.certifieLe = certifieLe;
    }

    public Boolean getOuvert() {
        return ouvert;
    }

    public void setOuvert(Boolean ouvert) {
        this.ouvert = ouvert;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public boolean certifie() {
        return certifieLe != null;
    }
}
