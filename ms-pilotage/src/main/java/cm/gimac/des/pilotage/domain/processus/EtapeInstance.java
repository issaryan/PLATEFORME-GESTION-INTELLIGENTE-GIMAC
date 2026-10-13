package cm.gimac.des.pilotage.domain.processus;

import cm.gimac.des.pilotage.domain.activite.EntitePilotage;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Étape d'une instance (§2) : état (EN_ATTENTE, ACTIVE, A_SIGNER, A_VALIDER, TERMINEE, ANNULEE), tâche liée
 * (l'exécutant est le responsable de la tâche, ou personne tant qu'elle est à prendre), validateur et
 * signataires résolus à l'activation, dépôts des livrables, valeurs des champs, signatures. La tâche se
 * termine avec l'étape : aucune double saisie.
 */
@Entity
@Table(name = "processus_etape_instance")
public class EtapeInstance extends EntitePilotage {

    @Column(name = "instance_id", nullable = false, updatable = false)
    private UUID instanceId;

    @Column(name = "code", nullable = false, length = 40, updatable = false)
    private String code;

    @Column(name = "rang", nullable = false, updatable = false)
    private int rang;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 16)
    private EtatEtape etat = EtatEtape.EN_ATTENTE;

    @Column(name = "tache_id")
    private UUID tacheId;

    @Column(name = "validateur_id", length = 80)
    private String validateurId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validateur", columnDefinition = "jsonb")
    private Affectation validateur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "signatures", columnDefinition = "jsonb", nullable = false)
    private List<SignatureInstance> signatures = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "depots", columnDefinition = "jsonb", nullable = false)
    private List<Depot> depots = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valeurs", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> valeurs = Map.of();

    @Column(name = "activee_le")
    private Instant activeeLe;

    /** Échéance maximale (D-106) : au-delà, l'étape est en retard (dépassement). */
    @Column(name = "echeance")
    private LocalDate echeance;

    /** Échéance cible (D-106) : l'atteindre annonce l'échéance proche. */
    @Column(name = "echeance_cible")
    private LocalDate echeanceCible;

    @Column(name = "soumise_le")
    private Instant soumiseLe;

    @Column(name = "terminee_le")
    private Instant termineeLe;

    @Column(name = "renvois", nullable = false)
    private int renvois;

    @Version
    @Column(name = "verrou", nullable = false)
    private long verrou;

    protected EtapeInstance() {}

    public EtapeInstance(UUID instanceId, String code, int rang) {
        this.instanceId = instanceId;
        this.code = code;
        this.rang = rang;
    }

    /** Activation (prédécesseurs terminés) : tâche créée, validateur et signataires résolus, échéance. */
    public void activer(UUID tache, Affectation validateurResolu, List<SignatureInstance> attendues, LocalDate echeanceEtape, Instant le) {
        activer(tache, validateurResolu, attendues, echeanceEtape, echeanceEtape, le);
    }

    /** Activation avec échéances cible et maximale distinctes (D-106). */
    public void activer(UUID tache, Affectation validateurResolu, List<SignatureInstance> attendues, LocalDate echeanceCibleEtape, LocalDate echeanceEtape, Instant le) {
        exigerEtat(EtatEtape.EN_ATTENTE);
        this.echeanceCible = echeanceCibleEtape;
        this.etat = EtatEtape.ACTIVE;
        this.tacheId = tache;
        affecterValidateur(validateurResolu);
        this.signatures = attendues == null ? List.of() : List.copyOf(attendues);
        this.echeance = echeanceEtape;
        this.activeeLe = le;
    }

    /** Valeurs initiales des champs (champs de lancement d'une étape initiale). */
    public void preremplir(Map<String, String> v) {
        Map<String, String> m = new LinkedHashMap<>(valeurs);
        m.putAll(v);
        this.valeurs = Map.copyOf(m);
    }

    /** Dépôt d'un livrable (remplace le dépôt précédent du même livrable) : étape active. */
    public void deposer(Depot d) {
        exigerEtat(EtatEtape.ACTIVE);
        List<Depot> l = new ArrayList<>(depots.stream().filter(x -> !x.livrable().equals(d.livrable())).toList());
        l.add(d);
        this.depots = List.copyOf(l);
    }

    /** Valeurs des champs (null retire la valeur) : étape active. */
    public void renseigner(Map<String, String> v) {
        exigerEtat(EtatEtape.ACTIVE);
        Map<String, String> m = new LinkedHashMap<>(valeurs);
        v.forEach((k, x) -> {
            if (x == null) {
                m.remove(k);
            } else {
                m.put(k, x);
            }
        });
        this.valeurs = Map.copyOf(m);
    }

    /**
     * Fin déclarée par l'exécutant : livrables obligatoires déposés (422 PIL_PROCESSUS_LIVRABLES_MANQUANTS),
     * champs obligatoires remplis (422 PIL_PROCESSUS_CHAMPS_MANQUANTS) ; l'étape attend ensuite ses signatures,
     * sa validation, ou se termine.
     */
    public void soumettre(Etape def, Instant le) {
        exigerEtat(EtatEtape.ACTIVE);
        List<String> manquants = def.livrables().stream().filter(Livrable::obligatoire).map(Livrable::code).filter(c -> depot(c) == null).toList();
        if (!manquants.isEmpty()) {
            throw PilotageException.regle("PIL_PROCESSUS_LIVRABLES_MANQUANTS", "Livrables obligatoires non déposés", Map.of("livrables", manquants));
        }
        List<String> vides = def.champs().stream().filter(Champ::obligatoire).map(Champ::code).filter(c -> valeurs.get(c) == null || valeurs.get(c).isBlank()).toList();
        if (!vides.isEmpty()) {
            throw PilotageException.regle("PIL_PROCESSUS_CHAMPS_MANQUANTS", "Champs obligatoires non remplis", Map.of("champs", vides));
        }
        this.soumiseLe = le;
        avancerApresSignatures(le);
    }

    /** Signature suivante attendue : la plus petite par ordre parmi celles non signées. */
    public void exigerSignable(String codeSignature) {
        exigerEtat(EtatEtape.A_SIGNER);
        SignatureInstance s = signature(codeSignature);
        if (s == null) {
            throw PilotageException.introuvable("PIL_PROCESSUS_SIGNATURE_INCONNUE", codeSignature);
        }
        if (s.etat() == EtatSignature.SIGNEE) {
            throw PilotageException.conflit("PIL_PROCESSUS_DEJA_SIGNEE", "Signature déjà apposée", Map.of("signature", codeSignature));
        }
        boolean avant = signatures.stream().anyMatch(x -> x.etat() == EtatSignature.ATTENDUE && x.ordre() < s.ordre());
        if (avant) {
            throw PilotageException.conflit("PIL_PROCESSUS_SIGNATURE_ORDRE", "Une signature d'ordre inférieur est encore attendue", Map.of("signature", codeSignature));
        }
    }

    public void signer(String codeSignature, String par, String empreinte, List<DocumentSigne> documents, Instant le) {
        exigerSignable(codeSignature);
        remplacerSignature(codeSignature, s -> s.signee(par, le.toString(), empreinte, documents));
        if (signatures.stream().allMatch(x -> x.etat() == EtatSignature.SIGNEE)) {
            avancerApresSignatures(le);
        }
    }

    public void valider(Instant le) {
        exigerEtat(EtatEtape.A_VALIDER);
        terminer(le);
    }

    /** Renvoi par le validateur : l'étape redevient active, les signatures sont à refaire. */
    public void renvoyer() {
        exigerEtat(EtatEtape.A_VALIDER);
        this.etat = EtatEtape.ACTIVE;
        this.signatures = signatures.stream().map(SignatureInstance::remiseEnAttente).toList();
        this.soumiseLe = null;
        this.renvois++;
    }

    public void annuler(Instant le) {
        if (!etat.estFinal()) {
            this.etat = EtatEtape.ANNULEE;
            this.termineeLe = le;
        }
    }

    /** Validateur réaffecté (départ, réaffectation). */
    public void affecterValidateur(Affectation a) {
        this.validateur = a;
        this.validateurId = a == null ? null : a.personneId();
    }

    /** Signatures attendues de {@code personneId} confiées à {@code nouvelle} (départ). @return codes concernés. */
    public List<String> reaffecterSignatures(String personneId, UnaryOperator<SignatureInstance> remplacement) {
        List<String> codes = new ArrayList<>();
        this.signatures = signatures
            .stream()
            .map(s -> {
                if (s.etat() == EtatSignature.ATTENDUE && s.attendu() != null && s.attendu().nomme(personneId)) {
                    codes.add(s.code());
                    return remplacement.apply(s);
                }
                return s;
            })
            .toList();
        return codes;
    }

    /** Signataire attendu d'une signature encore attendue remplacé (prise de sa tâche, changement d'organisation). */
    public void reaffecterSignature(String codeSignature, Affectation a) {
        remplacerSignature(codeSignature, s -> s.etat() == EtatSignature.ATTENDUE ? s.reaffectee(a) : s);
    }

    private void avancerApresSignatures(Instant le) {
        if (signatures.stream().anyMatch(x -> x.etat() == EtatSignature.ATTENDUE)) {
            this.etat = EtatEtape.A_SIGNER;
        } else if (validateur != null) {
            this.etat = EtatEtape.A_VALIDER;
        } else {
            terminer(le);
        }
    }

    private void terminer(Instant le) {
        this.etat = EtatEtape.TERMINEE;
        this.termineeLe = le;
    }

    private void remplacerSignature(String c, UnaryOperator<SignatureInstance> f) {
        this.signatures = signatures.stream().map(s -> s.code().equals(c) ? f.apply(s) : s).toList();
    }

    public void exigerEtat(EtatEtape... attendus) {
        if (Arrays.stream(attendus).noneMatch(a -> a == etat)) {
            throw PilotageException.conflit(
                "PIL_PROCESSUS_ETAPE_ETAT",
                "Acte impossible dans l'état de l'étape",
                Map.of("etape", code, "etat", etat.name(), "attendu", Arrays.stream(attendus).map(Enum::name).toList())
            );
        }
    }

    public Depot depot(String livrable) {
        return depots.stream().filter(d -> d.livrable().equals(livrable)).findFirst().orElse(null);
    }

    public SignatureInstance signature(String c) {
        return signatures.stream().filter(s -> s.code().equals(c)).findFirst().orElse(null);
    }

    /** Signatures attendues que l'on peut apposer maintenant (ordre minimal parmi les non signées). */
    public List<SignatureInstance> signaturesOuvertes() {
        if (etat != EtatEtape.A_SIGNER) {
            return List.of();
        }
        int min = signatures.stream().filter(s -> s.etat() == EtatSignature.ATTENDUE).mapToInt(SignatureInstance::ordre).min().orElse(Integer.MAX_VALUE);
        return signatures.stream().filter(s -> s.etat() == EtatSignature.ATTENDUE && s.ordre() == min).sorted(Comparator.comparing(SignatureInstance::code)).toList();
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public String getCode() {
        return code;
    }

    public int getRang() {
        return rang;
    }

    public EtatEtape getEtat() {
        return etat;
    }

    public UUID getTacheId() {
        return tacheId;
    }

    public String getValidateurId() {
        return validateurId;
    }

    public Affectation getValidateur() {
        return validateur;
    }

    public List<SignatureInstance> getSignatures() {
        return signatures;
    }

    public List<Depot> getDepots() {
        return depots;
    }

    public Map<String, String> getValeurs() {
        return valeurs;
    }

    public Instant getActiveeLe() {
        return activeeLe;
    }

    public LocalDate getEcheance() {
        return echeance;
    }

    /** Échéance cible (D-106) ; une étape activée avant D-106 : son échéance. */
    public LocalDate getEcheanceCible() {
        return echeanceCible != null ? echeanceCible : echeance;
    }

    /** En retard (D-106) : en cours et échéance maximale dépassée. */
    public boolean enRetard(LocalDate aujourdhui) {
        return echeance != null && etat.enCours() && echeance.isBefore(aujourdhui);
    }

    /** Échéance proche (D-106) : en cours, échéance cible atteinte, échéance maximale pas encore dépassée. */
    public boolean aLApproche(LocalDate aujourdhui) {
        LocalDate cible = getEcheanceCible();
        return cible != null && etat.enCours() && !cible.isAfter(aujourdhui) && !enRetard(aujourdhui);
    }

    public Instant getSoumiseLe() {
        return soumiseLe;
    }

    public Instant getTermineeLe() {
        return termineeLe;
    }

    public int getRenvois() {
        return renvois;
    }
}
