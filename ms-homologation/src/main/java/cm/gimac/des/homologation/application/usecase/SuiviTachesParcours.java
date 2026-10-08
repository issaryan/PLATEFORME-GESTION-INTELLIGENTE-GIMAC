package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tâches de MS-PILOTAGE nées du parcours (docs/lot1/modelisateur-processus.md §5, D-93 §1). Chaque parcours chargé pour
 * une écriture est photographié ; avant la validation de la transaction, l'état de ses opérations est comparé à la
 * photographie et les demandes de tâches (et les événements du parcours, {@link EvenementsParcours}) sont écrits dans la
 * même transaction :
 * <ul>
 * <li>opération devenue éligible (A_FAIRE, EN_COURS ou BLOQUEE) : tâche au membre interne désigné R ; R tenu par une
 * fonction externe sans institution liée : tâche au conducteur (l'opération reste à sa main) ;</li>
 * <li>validation demandée (EN_VALIDATION) : tâche au membre V désigné ; la tâche de R est terminée ;</li>
 * <li>fonction interne à affecter en R, en V (validation demandée) ou en C (tâche aux consultés) d'une opération
 * engagée : tâche à prendre par les porteurs de ses permissions (D-94), sinon « Affecter &lt;fonction&gt; » au
 * conducteur, terminée dès l'affectation (D-93 §1) ;</li>
 * <li>{@code consultesRecoiventTache} : tâche à chaque consulté interne désigné tant que l'opération est engagée ;</li>
 * <li>opération faite, sans objet ou retirée : tâche terminée ; reprise d'une opération dépassée ou renvoyée : tâche
 * rouverte ;</li>
 * <li>signatures attendues (EN_SIGNATURE, D-91) : tâche au membre désigné pour chaque signature du plus petit ordre
 * restant, terminée quand elle est donnée ;</li>
 * <li>changement de la personne qui tient R, V ou C : tâche de l'ancien terminée, tâche du nouveau créée ;</li>
 * <li>liaison au cadrage, conducteur désigné : tâche « Affecter les intervenants » au conducteur, terminée à
 * l'affectation (geste AFFECTER_RESSOURCES).</li>
 * </ul>
 * Une demande refusée par MS-PILOTAGE (compte inactif ou inconnu) remonte au conducteur (RelaisTachesPilotage). Un
 * incident d'écriture est consigné et n'interrompt jamais le geste métier.
 */
@Component
public class SuiviTachesParcours {

    private static final Logger LOG = LoggerFactory.getLogger(SuiviTachesParcours.class);
    static final Set<EtatOperationParcours> ACTIF_R = EnumSet.of(EtatOperationParcours.A_FAIRE, EtatOperationParcours.EN_COURS, EtatOperationParcours.BLOQUEE);
    static final Set<EtatOperationParcours> DEPASSE_R = EnumSet.of(
        EtatOperationParcours.EN_VALIDATION,
        EtatOperationParcours.EN_SIGNATURE,
        EtatOperationParcours.FAITE,
        EtatOperationParcours.SANS_OBJET
    );
    static final Set<EtatOperationParcours> DEPASSE_V = EnumSet.of(EtatOperationParcours.EN_SIGNATURE, EtatOperationParcours.FAITE, EtatOperationParcours.SANS_OBJET);
    /** États où les consultés sont sollicités : opération engagée ou en validation. */
    static final Set<EtatOperationParcours> ACTIF_C = EnumSet.of(
        EtatOperationParcours.A_FAIRE,
        EtatOperationParcours.EN_COURS,
        EtatOperationParcours.BLOQUEE,
        EtatOperationParcours.EN_VALIDATION
    );
    static final String ORIGINE = "OPERATION";

    /**
     * {@code r} : compte qui reçoit la tâche de R (désigné interne, ou conducteur pour une fonction externe non liée) ;
     * {@code s} : signataires attendus maintenant (code de la signature, compte interne désigné), D-91 ; {@code c} :
     * consultés internes désignés qui reçoivent une tâche.
     */
    record EtatOperation(String code, EtatOperationParcours etat, String r, String v, Map<String, String> s, Set<String> c) {
        EtatOperation(String code, EtatOperationParcours etat, String r, String v) {
            this(code, etat, r, v, Map.of(), Set.of());
        }

        EtatOperation(String code, EtatOperationParcours etat, String r, String v, Map<String, String> s) {
            this(code, etat, r, v, s, Set.of());
        }
    }

    /**
     * {@code conducteur} : conducteur qui doit affecter les intervenants ; {@code aAffecter} : fonctions internes à
     * affecter, par code, avec leur libellé ; {@code jalons} : jalons atteints ; {@code conducteurLiaison} : conducteur
     * désigné de la liaison.
     */
    record Photo(Map<UUID, EtatOperation> operations, String conducteur, Map<String, String> aAffecter, Set<Jalon> jalons, String conducteurLiaison) {
        Photo(Map<UUID, EtatOperation> operations, String conducteur) {
            this(operations, conducteur, Map.of(), Set.of(), null);
        }
    }

    private final DemandesTaches demandes;
    private EvenementsParcours evenements;
    private PorteursPermission porteurs;

    /** Porteurs d'une permission (MS-IAM) : candidats d'une fonction à affecter (D-94). */
    @Autowired(required = false)
    public void setPorteurs(PorteursPermission porteurs) {
        this.porteurs = porteurs;
    }

    public SuiviTachesParcours(DemandesTaches demandes) {
        this.demandes = demandes;
    }

    /** Événements du parcours (D-93 §1), écrits dans la même transaction que les demandes de tâches. */
    @Autowired(required = false)
    public void setEvenements(EvenementsParcours evenements) {
        this.evenements = evenements;
    }

    /** Parcours chargé pour une écriture : photographié s'il ne l'est pas déjà dans la transaction. */
    public void observer(Parcours p) {
        enregistrer(p, photo(p));
    }

    /** Parcours nouvellement instancié : rien n'existait avant lui. */
    public void observerNouveau(Parcours p) {
        enregistrer(p, new Photo(Map.of(), null, Map.of(), Set.of(), null));
    }

    private void enregistrer(Parcours p, Photo photo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        Synchronisation s = TransactionSynchronizationManager.getSynchronizations()
            .stream()
            .filter(Synchronisation.class::isInstance)
            .map(Synchronisation.class::cast)
            .findFirst()
            .orElseGet(() -> {
                Synchronisation n = new Synchronisation();
                TransactionSynchronizationManager.registerSynchronization(n);
                return n;
            });
        s.suivis.putIfAbsent(p.id(), new Suivi(p, photo));
    }

    record Suivi(Parcours parcours, Photo avant) {}

    private final class Synchronisation implements TransactionSynchronization {

        private final Map<UUID, Suivi> suivis = new LinkedHashMap<>();

        @Override
        public void beforeCommit(boolean readOnly) {
            if (readOnly) {
                return;
            }
            for (Suivi s : suivis.values()) {
                try {
                    emettre(s.parcours(), s.avant());
                } catch (RuntimeException e) {
                    LOG.warn("Tâches du parcours {} non demandées : {}", s.parcours().id(), e.toString());
                }
                if (evenements != null) {
                    try {
                        Map<UUID, EtatOperationParcours> etats = new HashMap<>();
                        s.avant().operations().forEach((id, e) -> etats.put(id, e.etat()));
                        evenements.emettre(s.parcours(), etats, s.avant().jalons());
                    } catch (RuntimeException e) {
                        LOG.warn("Événements du parcours {} non écrits : {}", s.parcours().id(), e.toString());
                    }
                }
            }
        }
    }

    static Photo photo(Parcours p) {
        Map<UUID, EtatOperation> m = new HashMap<>();
        p.operations().forEach(o -> m.put(o.getId(), new EtatOperation(o.getRang(), o.getEtat(), responsableR(p, o), interne(o.getRaci().v()), signataires(p, o), consultes(o))));
        return new Photo(m, conducteurActif(p), aAffecter(p), EvenementsParcours.jalons(p), conducteurId(p));
    }

    /** Identifiant d'un membre interne désigné (compte du GIMAC), sinon null : fonction à affecter ou tiers. */
    static String interne(Personne x) {
        if (x == null || !x.affectee() || x.institutionnelle() || x.fonction() == null || x.fonction().externe()) {
            return null;
        }
        try {
            return UUID.fromString(x.id()).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Compte qui reçoit la tâche de R : le membre interne désigné ; une fonction externe que ne tient aucune institution
     * liée reste à la main du conducteur (D-93 §1) ; le participant et une institution liée agissent sur le portail.
     */
    static String responsableR(Parcours p, OperationParcours o) {
        Personne r = o.getRaci().r();
        String id = interne(r);
        if (id != null) {
            return id;
        }
        if (r != null && !r.affectee() && r.fonction().externe()) {
            return conducteurId(p);
        }
        return null;
    }

    static String conducteurId(Parcours p) {
        Personne c = p.conducteur();
        return c == null ? null : c.id();
    }

    /** Consultés internes désignés qui reçoivent une tâche (réglage de l'opération, D-93 §1). */
    static Set<String> consultes(OperationParcours o) {
        if (!o.isConsultesRecoiventTache()) {
            return Set.of();
        }
        Set<String> s = new LinkedHashSet<>();
        o.getRaci().c().forEach(x -> {
            String id = interne(x);
            if (id != null) {
                s.add(id);
            }
        });
        return s;
    }

    /**
     * Fonctions internes à affecter sur les opérations engagées : R d'une opération éligible, V d'une opération en
     * validation, C d'une opération dont les consultés reçoivent une tâche (D-93 §1).
     */
    static Map<String, String> aAffecter(Parcours p) {
        Map<String, String> m = new TreeMap<>();
        for (OperationParcours o : p.operations()) {
            var raci = o.getRaci();
            if (ACTIF_R.contains(o.getEtat())) {
                ajouterAAffecter(m, raci.r());
            }
            if (o.getEtat() == EtatOperationParcours.EN_VALIDATION) {
                ajouterAAffecter(m, raci.v());
            }
            if (o.isConsultesRecoiventTache() && ACTIF_C.contains(o.getEtat())) {
                raci.c().forEach(x -> ajouterAAffecter(m, x));
            }
        }
        return m;
    }

    private static void ajouterAAffecter(Map<String, String> m, Personne x) {
        if (x != null && !x.affectee() && x.fonction() != null && !x.fonction().externe()) {
            m.put(x.fonction().name(), x.fonction().libelle());
        }
    }

    /**
     * Signataires attendus maintenant d'une opération EN_SIGNATURE (D-91) : pour chaque signature du plus petit ordre
     * restant, le membre interne désigné pour sa fonction, s'il y en a un.
     */
    static Map<String, String> signataires(Parcours p, OperationParcours o) {
        if (o.getEtat() != EtatOperationParcours.EN_SIGNATURE) {
            return Map.of();
        }
        Map<String, String> m = new LinkedHashMap<>();
        for (SignatureOperation s : o.signaturesSuivantes()) {
            String id = p.titulaire(o, s.signataire()).map(SuiviTachesParcours::interne).orElse(null);
            if (id != null) {
                m.put(s.code(), id);
            }
        }
        return m;
    }

    /** Conducteur qui doit affecter les intervenants : désigné, et l'affectation n'est pas faite. */
    static String conducteurActif(Parcours p) {
        boolean affectationEnCours = !p.operations(o -> o.porte(Geste.AFFECTER_RESSOURCES) && !o.faite() && o.getEtat() != EtatOperationParcours.SANS_OBJET).isEmpty();
        UUID c = p.liaison().getChefDeProjetId();
        return affectationEnCours && !p.etat().isConducteurADesigner() && c != null ? c.toString() : null;
    }

    void emettre(Parcours p, Photo avant) {
        String ref = p.etat().getReference();
        String conducteur = conducteurId(p);
        Map<UUID, OperationParcours> presentes = new HashMap<>();
        for (OperationParcours o : p.operations()) {
            presentes.put(o.getId(), o);
            EtatOperation av = avant.operations().getOrDefault(o.getId(), new EtatOperation(o.getRang(), EtatOperationParcours.VERROUILLEE, null, null));
            String rAvant = ACTIF_R.contains(av.etat()) ? av.r() : null;
            String rApres = ACTIF_R.contains(o.getEtat()) ? responsableR(p, o) : null;
            boolean renvoi = av.etat() == EtatOperationParcours.EN_VALIDATION && ACTIF_R.contains(o.getEtat());
            comparer(p, o, "R", rAvant, rApres, DEPASSE_R.contains(av.etat()) || renvoi, conducteur);
            String vAvant = av.etat() == EtatOperationParcours.EN_VALIDATION ? av.v() : null;
            String vApres = o.getEtat() == EtatOperationParcours.EN_VALIDATION ? interne(o.getRaci().v()) : null;
            // Nouvelle validation après un renvoi : la tâche du validateur, terminée au renvoi, est rouverte.
            boolean revalidation = ACTIF_R.contains(av.etat()) && !o.getRenvois().isEmpty();
            comparer(p, o, "V", vAvant, vApres, DEPASSE_V.contains(av.etat()) || revalidation, conducteur);
            Map<String, String> sApres = signataires(p, o);
            Set<String> codes = new LinkedHashSet<>(av.s().keySet());
            codes.addAll(sApres.keySet());
            for (String c : codes) {
                comparer(p, o, "S/" + c, av.s().get(c), sApres.get(c), false, conducteur);
            }
            Set<String> cAvant = ACTIF_C.contains(av.etat()) ? av.c() : Set.of();
            Set<String> cApres = ACTIF_C.contains(o.getEtat()) ? consultes(o) : Set.of();
            consulter(p, o, cAvant, cApres, conducteur);
        }
        avant
            .operations()
            .forEach((id, av) -> {
                if (!presentes.containsKey(id)) {
                    // Opération retirée par une migration : ses tâches actives sont terminées.
                    if (ACTIF_R.contains(av.etat()) && av.r() != null) {
                        demandes.cloturer(new PilotageTachesPort.Designation(origine(ref + "/" + av.code() + "/R", null), av.r()));
                    }
                    if (av.etat() == EtatOperationParcours.EN_VALIDATION && av.v() != null) {
                        demandes.cloturer(new PilotageTachesPort.Designation(origine(ref + "/" + av.code() + "/V", null), av.v()));
                    }
                    av.s().forEach((c, compte) -> demandes.cloturer(new PilotageTachesPort.Designation(origine(ref + "/" + av.code() + "/S/" + c, null), compte)));
                    if (ACTIF_C.contains(av.etat())) {
                        av.c().forEach(compte -> demandes.cloturer(new PilotageTachesPort.Designation(origine(ref + "/" + av.code() + "/C", null), compte)));
                    }
                }
            });
        affectation(p, avant);
        affecterFonctions(p, avant);
    }

    /** Tâche « Affecter les intervenants » au conducteur tant que l'opération d'affectation est engagée. */
    private void affectation(Parcours p, Photo avant) {
        String ref = p.etat().getReference();
        String cAvant = avant.conducteur();
        String cApres = conducteurActif(p);
        if (!Objects.equals(cAvant, cApres)) {
            PilotageTachesPort.Origine o = origine(ref + "/AFFECTATION", "/des/parcours/" + p.id());
            if (cAvant != null) {
                demandes.cloturer(new PilotageTachesPort.Designation(o, cAvant));
            }
            if (cApres != null) {
                String echeance = p.parGeste(Geste.AFFECTER_RESSOURCES).map(OperationParcours::getEcheance).map(Object::toString).orElse(null);
                demandes.creer(
                    new PilotageTachesPort.Tache(
                        "Affecter les intervenants de la liaison " + ref,
                        "Liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : associer à chaque fonction du référentiel les membres du GIMAC qui la tiennent.",
                        echeance,
                        "NORMALE",
                        cApres,
                        null,
                        o
                    ),
                    null
                );
            }
        }
    }

    /**
     * Fonction interne à affecter sur une opération engagée (D-93 §1, D-94, docs/lot1/effet-reel-referentiels.md §8.8) :
     * tâche à prendre dans MS-PILOTAGE par les porteurs de l'une des permissions de la fonction (catalogue D-91), référence
     * {@code <réf.>/AFFECTER/<FONCTION>} ; sa prise (pilotage.task-taken.v1) affecte la fonction à la personne. Sans
     * permission au catalogue, sans porteur dans MS-IAM ou MS-IAM indisponible : tâche « Affecter &lt;fonction&gt; » au
     * conducteur, comme avant. Les deux sont terminées dès l'affectation ; la tâche à prendre est remise à prendre si la
     * fonction redevient à affecter (départ d'une personne).
     */
    private void affecterFonctions(Parcours p, Photo avant) {
        String ref = p.etat().getReference();
        String cAvant = avant.conducteurLiaison();
        String cApres = conducteurId(p);
        Map<String, String> fAvant = avant.aAffecter();
        Map<String, String> fApres = aAffecter(p);
        Set<String> toutes = new LinkedHashSet<>(fAvant.keySet());
        toutes.addAll(fApres.keySet());
        for (String f : toutes) {
            boolean etait = fAvant.containsKey(f);
            boolean est = fApres.containsKey(f);
            if (etait == est && (!est || Objects.equals(cAvant, cApres))) {
                continue;
            }
            PilotageTachesPort.Origine o = origine(ref + "/AFFECTER/" + f, "/des/parcours/" + p.id() + "?onglet=etapes&fonction=" + f);
            if (etait && cAvant != null) {
                demandes.cloturer(new PilotageTachesPort.Designation(o, cAvant));
            }
            if (!est) {
                // Fonction affectée : la tâche à prendre (prise ou non) est terminée.
                demandes.cloturer(PilotageTachesPort.Designation.offre(o));
                continue;
            }
            String libelle = fApres.get(f);
            List<String> permissions = Fonction.valueOf(f).permissions();
            if (offrable(p, f, permissions)) {
                demandes.creer(
                    new PilotageTachesPort.Tache(
                        abreger("Prendre la fonction " + libelle, 200 - ref.length() - 3) + " (" + ref + ")",
                        "Liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : la fonction « " + libelle + " » (" + f + ") n'est tenue par " +
                        "personne alors qu'une opération l'attend. En prenant cette tâche, vous êtes désigné pour cette fonction sur la liaison ; " +
                        "ses tâches vous sont alors confiées.",
                        null,
                        "HAUTE",
                        null,
                        null,
                        o,
                        permissions
                    ),
                    cApres
                );
                demandes.rouvrir(PilotageTachesPort.Designation.offre(o));
            } else if (cApres != null) {
                demandes.creer(
                    new PilotageTachesPort.Tache(
                        abreger("Affecter " + libelle, 200 - ref.length() - 3) + " (" + ref + ")",
                        "Liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : la fonction « " + libelle + " » (" + f + ") n'est tenue par personne " +
                        "alors qu'une opération l'attend. Désigner la personne qui la tient ; ses tâches lui seront alors confiées.",
                        null,
                        "HAUTE",
                        cApres,
                        null,
                        o
                    ),
                    null
                );
                demandes.rouvrir(new PilotageTachesPort.Designation(o, cApres));
            }
        }
    }

    /**
     * La fonction à affecter s'offre en tâche à prendre : elle a des permissions au catalogue et MS-IAM leur connaît au
     * moins un porteur interne actif. MS-IAM indisponible : non (tâche au conducteur), le geste n'échoue jamais.
     */
    private boolean offrable(Parcours p, String f, List<String> permissions) {
        if (porteurs == null || permissions.isEmpty()) {
            return false;
        }
        try {
            return !porteurs.porteurs(permissions).isEmpty();
        } catch (RuntimeException e) {
            LOG.warn("Porteurs de la fonction {} illisibles (liaison {}) : tâche au conducteur ; {}", f, p.id(), e.toString());
            return false;
        }
    }

    private void consulter(Parcours p, OperationParcours o, Set<String> avant, Set<String> apres, String conducteur) {
        if (avant.equals(apres)) {
            return;
        }
        PilotageTachesPort.Origine origine = origine(p.etat().getReference() + "/" + o.getRang() + "/C", ParcoursProjection.lienOperation(p, o));
        for (String id : avant) {
            if (!apres.contains(id)) {
                demandes.cloturer(new PilotageTachesPort.Designation(origine, id));
            }
        }
        for (String id : apres) {
            if (!avant.contains(id)) {
                demandes.creer(tache(p, o, "C", id, origine), conducteur);
                demandes.rouvrir(new PilotageTachesPort.Designation(origine, id));
            }
        }
    }

    private void comparer(Parcours p, OperationParcours o, String qualite, String avant, String apres, boolean depassee, String conducteur) {
        if (Objects.equals(avant, apres)) {
            return;
        }
        PilotageTachesPort.Origine origine = origine(p.etat().getReference() + "/" + o.getRang() + "/" + qualite, ParcoursProjection.lienOperation(p, o));
        if (avant != null) {
            demandes.cloturer(new PilotageTachesPort.Designation(origine, avant));
        }
        if (apres != null) {
            demandes.creer(tache(p, o, qualite, apres, origine), apres.equals(conducteur) ? null : conducteur);
            if (depassee) {
                // Opération reprise après avoir été dépassée ou renvoyée : la tâche déjà terminée est rouverte.
                demandes.rouvrir(new PilotageTachesPort.Designation(origine, apres));
            }
        }
    }

    private static PilotageTachesPort.Origine origine(String reference, String lien) {
        return new PilotageTachesPort.Origine(ORIGINE, reference, DemandesTaches.SERVICE, lien);
    }

    static PilotageTachesPort.Tache tache(Parcours p, OperationParcours o, String qualite, String responsable, PilotageTachesPort.Origine origine) {
        String libelle = o.getLibelle() == null || o.getLibelle().isBlank() ? "Opération " + o.getRang() : o.getLibelle();
        String ref = p.etat().getReference();
        boolean signature = qualite.startsWith("S/");
        String prefixe = switch (qualite) {
            case "V" -> "Valider : ";
            case "C" -> "Avis attendu : ";
            default -> signature ? "Signer : " : "";
        };
        String titre = abreger(prefixe + libelle, 200 - ref.length() - 3) + " (" + ref + ")";
        Personne r = o.getRaci().r();
        String description;
        if (signature) {
            description = "Signature « " + libelleSignature(o, qualite.substring(2)) + " » attendue sur l'opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : signer avec votre clé de sécurité.";
        } else if ("V".equals(qualite)) {
            description = "Validation demandée sur l'opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : vérifier la preuve déposée, puis valider ou renvoyer avec un motif.";
        } else if ("C".equals(qualite)) {
            description = "Opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : vous êtes consulté (C) ; votre avis est attendu avant la validation.";
        } else if (r != null && !r.affectee() && r.fonction().externe()) {
            description = "Opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + "), étape " + o.getEtape().libelle() +
            " : la fonction externe « " + r.fonction().libelle() + " » n'est tenue par aucune institution liée ; l'opération reste à votre main (conducteur).";
        } else {
            description = "Opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + "), étape " + o.getEtape().libelle() + " : vous en êtes responsable (R).";
        }
        return new PilotageTachesPort.Tache(titre, description, o.getEcheance() == null ? null : o.getEcheance().toString(), "NORMALE", responsable, null, origine);
    }

    private static String libelleSignature(OperationParcours o, String code) {
        return o.getSignatures().stream().filter(x -> x.code().equals(code)).map(SignatureOperation::libelle).findFirst().orElse(code);
    }

    static String abreger(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(1, max - 1)) + "…";
    }
}
