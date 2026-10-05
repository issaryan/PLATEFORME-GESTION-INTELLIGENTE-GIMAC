package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tâches de MS-PILOTAGE nées du parcours (docs/lot1/modelisateur-processus.md §5). Chaque parcours chargé pour
 * une écriture est photographié ; avant la validation de la transaction, l'état de ses opérations est comparé à
 * la photographie et les demandes de tâches sont écrites dans la même transaction :
 * <ul>
 * <li>opération devenue éligible (A_FAIRE, EN_COURS ou BLOQUEE) : tâche au membre qui tient la qualité R, s'il est
 * désigné et interne ;</li>
 * <li>validation demandée (EN_VALIDATION) : tâche au membre V ; la tâche de R est terminée ;</li>
 * <li>opération faite, sans objet ou retirée : tâche terminée ; reprise d'une opération dépassée : tâche rouverte ;</li>
 * <li>changement de la personne qui tient R ou V : tâche de l'ancien terminée, tâche du nouveau créée ;</li>
 * <li>liaison au cadrage, conducteur désigné : tâche « Affecter les intervenants » au conducteur, terminée à
 * l'affectation (geste AFFECTER_RESSOURCES).</li>
 * </ul>
 * Un incident d'écriture est consigné et n'interrompt jamais le geste métier.
 */
@Component
public class SuiviTachesParcours {

    private static final Logger LOG = LoggerFactory.getLogger(SuiviTachesParcours.class);
    static final Set<EtatOperationParcours> ACTIF_R = EnumSet.of(EtatOperationParcours.A_FAIRE, EtatOperationParcours.EN_COURS, EtatOperationParcours.BLOQUEE);
    static final Set<EtatOperationParcours> DEPASSE_R = EnumSet.of(EtatOperationParcours.EN_VALIDATION, EtatOperationParcours.FAITE, EtatOperationParcours.SANS_OBJET);
    static final Set<EtatOperationParcours> DEPASSE_V = EnumSet.of(EtatOperationParcours.FAITE, EtatOperationParcours.SANS_OBJET);
    static final String ORIGINE = "OPERATION";

    record EtatOperation(String code, EtatOperationParcours etat, String r, String v) {}

    record Photo(Map<UUID, EtatOperation> operations, String conducteur) {}

    private final DemandesTaches demandes;

    public SuiviTachesParcours(DemandesTaches demandes) {
        this.demandes = demandes;
    }

    /** Parcours chargé pour une écriture : photographié s'il ne l'est pas déjà dans la transaction. */
    public void observer(Parcours p) {
        enregistrer(p, photo(p));
    }

    /** Parcours nouvellement instancié : rien n'existait avant lui. */
    public void observerNouveau(Parcours p) {
        enregistrer(p, new Photo(Map.of(), null));
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
            }
        }
    }

    static Photo photo(Parcours p) {
        Map<UUID, EtatOperation> m = new HashMap<>();
        p.operations().forEach(o -> m.put(o.getId(), new EtatOperation(o.getRang(), o.getEtat(), interne(o.getRaci().r()), interne(o.getRaci().v()))));
        return new Photo(m, conducteurActif(p));
    }

    /** Identifiant d'un membre interne désigné (compte du GIMAC), sinon null : fonction à affecter ou tiers. */
    static String interne(Personne x) {
        if (x == null || !x.affectee() || x.fonction() == null || x.fonction().externe()) {
            return null;
        }
        try {
            return UUID.fromString(x.id()).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Conducteur qui doit affecter les intervenants : désigné, et l'affectation n'est pas faite. */
    static String conducteurActif(Parcours p) {
        boolean affectationEnCours = !p.operations(o -> o.porte(Geste.AFFECTER_RESSOURCES) && !o.faite() && o.getEtat() != EtatOperationParcours.SANS_OBJET).isEmpty();
        UUID c = p.liaison().getChefDeProjetId();
        return affectationEnCours && !p.etat().isConducteurADesigner() && c != null ? c.toString() : null;
    }

    void emettre(Parcours p, Photo avant) {
        String ref = p.etat().getReference();
        Map<UUID, OperationParcours> presentes = new HashMap<>();
        for (OperationParcours o : p.operations()) {
            presentes.put(o.getId(), o);
            EtatOperation av = avant.operations().getOrDefault(o.getId(), new EtatOperation(o.getRang(), EtatOperationParcours.VERROUILLEE, null, null));
            String rAvant = ACTIF_R.contains(av.etat()) ? av.r() : null;
            String rApres = ACTIF_R.contains(o.getEtat()) ? interne(o.getRaci().r()) : null;
            comparer(p, o, "R", rAvant, rApres, DEPASSE_R.contains(av.etat()));
            String vAvant = av.etat() == EtatOperationParcours.EN_VALIDATION ? av.v() : null;
            String vApres = o.getEtat() == EtatOperationParcours.EN_VALIDATION ? interne(o.getRaci().v()) : null;
            comparer(p, o, "V", vAvant, vApres, DEPASSE_V.contains(av.etat()));
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
                }
            });
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
                    )
                );
            }
        }
    }

    private void comparer(Parcours p, OperationParcours o, String qualite, String avant, String apres, boolean depassee) {
        if (Objects.equals(avant, apres)) {
            return;
        }
        PilotageTachesPort.Origine origine = origine(p.etat().getReference() + "/" + o.getRang() + "/" + qualite, ParcoursProjection.lienOperation(p, o));
        if (avant != null) {
            demandes.cloturer(new PilotageTachesPort.Designation(origine, avant));
        }
        if (apres != null) {
            demandes.creer(tache(p, o, qualite, apres, origine));
            if (depassee) {
                // Opération reprise après avoir été dépassée : la tâche déjà terminée est rouverte.
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
        String titre = ("V".equals(qualite) ? "Valider : " : "") + libelle;
        titre = abreger(titre, 200 - ref.length() - 3) + " (" + ref + ")";
        String description = "V".equals(qualite)
            ? "Validation demandée sur l'opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + ") : vérifier la preuve déposée et valider."
            : "Opération " + o.getRang() + " de la liaison " + ref + " (" + p.liaison().getInstitutionNom() + "), étape " + o.getEtape().libelle() + " : vous en êtes responsable (R).";
        return new PilotageTachesPort.Tache(titre, description, o.getEcheance() == null ? null : o.getEcheance().toString(), "NORMALE", responsable, null, origine);
    }

    private static String abreger(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(1, max - 1)) + "…";
    }
}
