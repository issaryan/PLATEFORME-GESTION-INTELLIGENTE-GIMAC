package cm.gimac.des.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Paramètre secret gardé par MS-IAM, chiffré (AES-256-GCM) : mot de passe du serveur de messagerie
 * saisi dans la console (D-62), que le fournisseur d'identité ne restitue jamais et qu'il faut à
 * MS-IAM pour envoyer ses courriels (D-65).
 */
@Entity
@Table(name = "parametre_chiffre")
public class ParametreChiffre {

    public static final String SMTP_MOT_DE_PASSE = "smtp.mot-de-passe";

    @Id
    @Column(name = "cle", length = 100)
    private String cle;

    @Column(name = "valeur_chiffree", nullable = false, length = 2048)
    private String valeurChiffree;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    protected ParametreChiffre() {}

    public ParametreChiffre(String cle, String valeurChiffree, Instant modifieLe) {
        this.cle = cle;
        this.valeurChiffree = valeurChiffree;
        this.modifieLe = modifieLe;
    }

    public String getCle() { return cle; }
    public String getValeurChiffree() { return valeurChiffree; }
    public Instant getModifieLe() { return modifieLe; }
}
