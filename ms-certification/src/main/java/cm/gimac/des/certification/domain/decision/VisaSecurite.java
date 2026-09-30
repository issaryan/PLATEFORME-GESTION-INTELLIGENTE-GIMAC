package cm.gimac.des.certification.domain.decision;

import cm.gimac.des.certification.domain.enumeration.EtatVisa;
import cm.gimac.des.certification.domain.enumeration.NatureVisa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Visa optionnel (conformité, Direction Générale : D-56), activé par type de projet et désactivé par
 * défaut ; s'il est actif, il doit être favorable avant l'approbation de la sécurité. Sa signature est une
 * ligne immuable de signature_verrou.
 */
@Entity
@Table(name = "visa_securite")
public class VisaSecurite {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approbation_id", nullable = false)
    private ApprobationSecurite approbation;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature", length = 30, nullable = false)
    private NatureVisa nature;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 30, nullable = false)
    private EtatVisa etat = EtatVisa.ATTENDU;

    @Column(name = "motif", length = 1000)
    private String motif;

    @Column(name = "signature_id")
    private UUID signatureId;

    protected VisaSecurite() {}

    VisaSecurite(ApprobationSecurite approbation, NatureVisa nature, boolean actif) {
        this.id = UUID.randomUUID();
        this.approbation = approbation;
        this.nature = nature;
        this.actif = actif;
    }

    public void signer(boolean favorable, String motif, UUID signatureId) {
        this.etat = favorable ? EtatVisa.SIGNE_FAVORABLE : EtatVisa.SIGNE_DEFAVORABLE;
        this.motif = motif;
        this.signatureId = signatureId;
    }

    public UUID getId() {
        return id;
    }

    public NatureVisa getNature() {
        return nature;
    }

    public boolean isActif() {
        return actif;
    }

    public EtatVisa getEtat() {
        return etat;
    }

    public String getMotif() {
        return motif;
    }

    public UUID getSignatureId() {
        return signatureId;
    }
}
