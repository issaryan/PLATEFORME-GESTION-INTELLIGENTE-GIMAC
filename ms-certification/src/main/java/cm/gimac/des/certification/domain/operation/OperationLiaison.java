package cm.gimac.des.certification.domain.operation;

import cm.gimac.des.certification.domain.decision.CircuitSignatureConverter;
import cm.gimac.des.certification.domain.decision.EtapeSignature;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Projection locale d'une opération du parcours d'une liaison (D-93 §4), tenue par les événements de MS-HOMOLOGATION
 * (homologation.operation-activated.v1, -validation-requested.v1, -signed.v1, homologation.liaison-milestone-reached.v1).
 * Elle dit si l'opération qui porte un geste de certification est ouverte et qui y est désigné. Un événement plus
 * ancien que le dernier appliqué est ignoré (ordre de la clé = liaison, mais rejeu possible).
 */
@Entity
@Table(name = "operation_liaison")
public class OperationLiaison {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "liaison_id", nullable = false, updatable = false)
    private UUID liaisonId;

    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "libelle", length = 300)
    private String libelle;

    @Column(name = "geste", length = 40)
    private String geste;

    @Column(name = "vue", length = 30)
    private String vue;

    @Column(name = "jalon", length = 30)
    private String jalon;

    /** Événement de MS-CERTIFICATION qui clôt l'opération, tel que MS-HOMOLOGATION le publie (absent : aucun). */
    @Column(name = "declencheur", length = 120)
    private String declencheur;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 20, nullable = false)
    private EtatOperation etat;

    @Convert(converter = JsonColonne.Intervenants.class)
    @Column(name = "intervenants", columnDefinition = "text")
    private List<Intervenant> intervenants = new ArrayList<>();

    @Convert(converter = JsonColonne.Codes.class)
    @Column(name = "institutions_concernees", columnDefinition = "text")
    private List<String> institutionsConcernees = new ArrayList<>();

    /** Signatures configurées de l'opération (D-91), quand l'événement les porte. */
    @Convert(converter = CircuitSignatureConverter.class)
    @Column(name = "signatures", columnDefinition = "text")
    private List<EtapeSignature> signatures = new ArrayList<>();

    @Column(name = "evenement_le", nullable = false)
    private Instant evenementLe;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected OperationLiaison() {}

    public OperationLiaison(UUID id, UUID liaisonId) {
        this.id = id;
        this.liaisonId = liaisonId;
        this.etat = EtatOperation.OUVERTE;
        this.evenementLe = Instant.EPOCH;
    }

    /** Description de l'opération portée par l'événement (champs absents : valeurs gardées). */
    public record Description(String code, String libelle, String geste, String vue, String jalon, String declencheur) {}

    /** Vrai si l'événement de cet instant est plus récent que le dernier appliqué (il est alors retenu). */
    public boolean retenir(Instant le) {
        Instant instant = le == null ? Instant.EPOCH : le;
        if (instant.isBefore(evenementLe)) {
            return false;
        }
        this.evenementLe = instant;
        return true;
    }

    public void decrire(Description d) {
        code = d.code() != null ? d.code() : code;
        libelle = d.libelle() != null ? d.libelle() : libelle;
        geste = d.geste() != null ? d.geste() : geste;
        vue = d.vue() != null ? d.vue() : vue;
        jalon = d.jalon() != null ? d.jalon() : jalon;
        declencheur = d.declencheur() != null ? d.declencheur() : declencheur;
    }

    /** Déclencheur publié par MS-HOMOLOGATION avec la description de l'opération : il fait foi, même absent. */
    public void definirDeclencheur(String declencheur) {
        this.declencheur = declencheur == null || declencheur.isBlank() ? null : declencheur;
    }

    public void designer(List<Intervenant> intervenants, List<String> institutionsConcernees, List<EtapeSignature> signatures) {
        this.intervenants = new ArrayList<>(intervenants);
        this.institutionsConcernees = new ArrayList<>(institutionsConcernees);
        if (signatures != null) {
            this.signatures = new ArrayList<>(signatures);
        }
    }

    public void setEtat(EtatOperation etat) {
        this.etat = etat;
    }

    public boolean estOuverte() {
        return etat == EtatOperation.OUVERTE;
    }

    /** Personne désignée en responsable (R) ou validateur (V) de l'opération. */
    public boolean designe(String compte) {
        return intervenants.stream().anyMatch(i -> ("R".equals(i.qualite()) || "V".equals(i.qualite())) && i.tenuPar(compte));
    }

    /** R ou V à affecter : tout porteur de la permission du geste peut agir (comme MS-HOMOLOGATION, D-93 §8.3). */
    public boolean aAffecter() {
        return intervenants.stream().anyMatch(i -> ("R".equals(i.qualite()) || "V".equals(i.qualite())) && i.aAffecter());
    }

    /** Institution qui tient une fonction de l'opération (R ou E externe). */
    public boolean concerne(String institution) {
        return (
            institution != null &&
            (institutionsConcernees.stream().anyMatch(institution::equalsIgnoreCase) || intervenants.stream().anyMatch(i -> i.tenuParInstitution(institution)))
        );
    }

    /** Intervenants internes de l'opération qui tiennent cette fonction. */
    public List<Intervenant> tenantsDe(String fonction) {
        return intervenants.stream().filter(i -> fonction != null && fonction.equals(i.fonction())).toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getLiaisonId() {
        return liaisonId;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getGeste() {
        return geste;
    }

    public String getVue() {
        return vue;
    }

    public String getJalon() {
        return jalon;
    }

    public String getDeclencheur() {
        return declencheur;
    }

    public EtatOperation getEtat() {
        return etat;
    }

    public List<Intervenant> getIntervenants() {
        return List.copyOf(intervenants);
    }

    public List<String> getInstitutionsConcernees() {
        return List.copyOf(institutionsConcernees);
    }

    public List<EtapeSignature> getSignatures() {
        return List.copyOf(signatures);
    }

    public Instant getEvenementLe() {
        return evenementLe;
    }
}
