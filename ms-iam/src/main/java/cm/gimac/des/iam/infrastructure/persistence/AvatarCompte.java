package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Photo de profil d'un compte (docs/lot1/mon-compte.md §1) : image réencodée 256 × 256, sans
 * métadonnées, son type, sa version (empreinte courte) et sa date de mise à jour. Jamais l'image
 * d'origine.
 */
@Entity
@Table(name = "avatar_compte")
public class AvatarCompte {

    @Id
    @Column(name = "compte_id", nullable = false, length = 100)
    private String compteId;

    @Column(name = "image", nullable = false)
    private byte[] image;

    @Column(name = "type_contenu", nullable = false, length = 30)
    private String type;

    @Column(name = "version", nullable = false, length = 32)
    private String version;

    @Column(name = "mis_a_jour_le", nullable = false)
    private Instant misAJourLe;

    protected AvatarCompte() {}

    public AvatarCompte(String compteId, byte[] image, String type, String version, Instant misAJourLe) {
        this.compteId = compteId;
        remplacer(image, type, version, misAJourLe);
    }

    public final void remplacer(byte[] image, String type, String version, Instant misAJourLe) {
        this.image = image == null ? null : image.clone();
        this.type = type;
        this.version = version;
        this.misAJourLe = misAJourLe;
    }

    public String getCompteId() { return compteId; }
    public byte[] getImage() { return image == null ? null : image.clone(); }
    public String getType() { return type; }
    public String getVersion() { return version; }
    public Instant getMisAJourLe() { return misAJourLe; }
}
