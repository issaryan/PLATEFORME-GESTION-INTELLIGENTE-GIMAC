package cm.gimac.des.certification.domain.decision;

import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Signature d'un verrou (D-35) : assertion WebAuthn personnelle vérifiée par MS-IAM, conservée, puis cachet
 * de la plateforme horodaté sur l'empreinte de la décision canonique. Immuable : aucune colonne n'est
 * modifiable (entité en lecture seule après insertion, déclencheur en base, unicité liaison-objet-étape).
 */
@Entity
@Immutable
@Table(name = "signature_verrou")
public class SignatureVerrou {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false)
    private UUID liaisonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "objet", length = 20, nullable = false)
    private ObjetSignature objet;

    /** Code de la signature dans le circuit (D-94) ; reprise : GIMAC, PARTICIPANT, CONFORMITE, DIRECTION_GENERALE. */
    @Column(name = "etape", length = 40, nullable = false)
    private String etape;

    @Column(name = "signataire_id", length = 80, nullable = false)
    private String signataireId;

    @Column(name = "signataire_nom", length = 200, nullable = false)
    private String signataireNom;

    @Column(name = "fonction", length = 40, nullable = false)
    private String fonction;

    @Column(name = "favorable", nullable = false)
    private boolean favorable;

    @Column(name = "motif", length = 1000)
    private String motif;

    @Column(name = "signe_le", nullable = false)
    private Instant signeLe;

    @Column(name = "empreinte_objet", length = 64)
    private String empreinteObjet;

    @Column(name = "empreinte_decision", length = 64, nullable = false)
    private String empreinteDecision;

    @Column(name = "assertion", nullable = false, columnDefinition = "text")
    private String assertion;

    @Column(name = "cachet", nullable = false, columnDefinition = "text")
    private String cachet;

    @Column(name = "horodatage", nullable = false)
    private Instant horodatage;

    @Column(name = "jeton_horodatage_id")
    private UUID jetonHorodatageId;

    protected SignatureVerrou() {}

    /** Décision signée : objet, étape du circuit, signataire, sens et motif, instant et empreinte de l'objet. */
    public record Decision(
        UUID liaisonId,
        ObjetSignature objet,
        EtapeSignature etape,
        String signataireId,
        String signataireNom,
        boolean favorable,
        String motif,
        Instant signeLe,
        String empreinteObjet
    ) {}

    /** Scellement de la décision : empreinte canonique, assertion conservée, cachet et horodatage. */
    public record Scellement(String empreinteDecision, String assertion, String cachet, Instant horodatage, UUID jetonHorodatageId) {}

    public SignatureVerrou(UUID id, Decision d, Scellement s) {
        this.id = id;
        this.liaisonId = d.liaisonId();
        this.objet = d.objet();
        this.etape = d.etape().code();
        this.signataireId = d.signataireId();
        this.signataireNom = d.signataireNom();
        this.fonction = d.etape().fonction();
        this.favorable = d.favorable();
        this.motif = d.motif();
        this.signeLe = d.signeLe();
        this.empreinteObjet = d.empreinteObjet();
        this.empreinteDecision = s.empreinteDecision();
        this.assertion = s.assertion();
        this.cachet = s.cachet();
        this.horodatage = s.horodatage();
        this.jetonHorodatageId = s.jetonHorodatageId();
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public ObjetSignature getObjet() {
        return objet;
    }

    public String getEtape() {
        return etape;
    }

    public String getSignataireId() {
        return signataireId;
    }

    public String getSignataireNom() {
        return signataireNom;
    }

    public String getFonction() {
        return fonction;
    }

    public boolean isFavorable() {
        return favorable;
    }

    public String getMotif() {
        return motif;
    }

    public Instant getSigneLe() {
        return signeLe;
    }

    public String getEmpreinteObjet() {
        return empreinteObjet;
    }

    public String getEmpreinteDecision() {
        return empreinteDecision;
    }

    public String getAssertion() {
        return assertion;
    }

    public String getCachet() {
        return cachet;
    }

    public Instant getHorodatage() {
        return horodatage;
    }

    public UUID getJetonHorodatageId() {
        return jetonHorodatageId;
    }
}
