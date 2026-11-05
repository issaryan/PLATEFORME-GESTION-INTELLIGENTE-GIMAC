package cm.gimac.des.homologation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Modification du périmètre d'un projet de certification (D-133), jamais modifiée ni supprimée : version atteinte,
 * auteur, permission qui a autorisé l'acte, services inclus avant et après (codes séparés par des virgules), motif et
 * horodatage. La version 1 est le périmètre dérivé à l'ouverture, sans ligne.
 */
@Entity
@Table(name = "perimetre_modification")
public class PerimetreModification {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Column(name = "version_perimetre", nullable = false)
    private int versionPerimetre;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "auteur_nom", length = 200)
    private String auteurNom;

    @Column(name = "permission_utilisee", nullable = false, length = 60)
    private String permissionUtilisee;

    @Column(name = "avant", nullable = false, length = 4000)
    private String avant;

    @Column(name = "apres", nullable = false, length = 4000)
    private String apres;

    @Column(name = "motif", nullable = false, length = 500)
    private String motif;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    protected PerimetreModification() {}

    public PerimetreModification(UUID liaisonId, int versionPerimetre, UUID auteurId, String auteurNom, String permissionUtilisee, String avant, String apres, String motif, Instant modifieLe) {
        this.id = UUID.randomUUID();
        this.liaisonId = liaisonId;
        this.versionPerimetre = versionPerimetre;
        this.auteurId = auteurId;
        this.auteurNom = auteurNom;
        this.permissionUtilisee = permissionUtilisee;
        this.avant = avant;
        this.apres = apres;
        this.motif = motif;
        this.modifieLe = modifieLe;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public int getVersionPerimetre() {
        return versionPerimetre;
    }

    public UUID getAuteurId() {
        return auteurId;
    }

    public String getAuteurNom() {
        return auteurNom;
    }

    public String getPermissionUtilisee() {
        return permissionUtilisee;
    }

    public String getAvant() {
        return avant;
    }

    public String getApres() {
        return apres;
    }

    public String getMotif() {
        return motif;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }
}
