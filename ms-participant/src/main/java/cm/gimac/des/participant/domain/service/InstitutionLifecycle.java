package cm.gimac.des.participant.domain.service;

import static cm.gimac.des.participant.domain.enumeration.InstitutionStatus.*;

import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Automate du cycle de vie d'une institution (CC algorithme 6.25, étendu par D-57).
 *
 * <p>Relation de transition δ : celle de l'équation 6.27, dans laquelle (CANDIDAT, EN_HOMOLOGATION)
 * est remplacé par le passage par ADHERENT (étapes 1 et 2 du parcours, CR-CERT §4.2.1), plus la
 * clôture sans suite d'un candidat ou d'un adhérent. RETIRE et CLOS_SANS_SUITE sont terminaux.
 *
 * <p>Service de domaine pur : aucune dépendance Spring ni base de données.
 */
public final class InstitutionLifecycle {

    private static final Map<InstitutionStatus, Set<InstitutionStatus>> DELTA = new EnumMap<>(InstitutionStatus.class);

    static {
        DELTA.put(CANDIDAT, EnumSet.of(ADHERENT, CLOS_SANS_SUITE));
        DELTA.put(ADHERENT, EnumSet.of(EN_HOMOLOGATION, CLOS_SANS_SUITE));
        DELTA.put(EN_HOMOLOGATION, EnumSet.of(ACTIF));
        DELTA.put(ACTIF, EnumSet.of(A_REGULARISER, SUSPENDU, RETIRE));
        DELTA.put(A_REGULARISER, EnumSet.of(ACTIF, SUSPENDU));
        DELTA.put(SUSPENDU, EnumSet.of(ACTIF, RETIRE));
        DELTA.put(RETIRE, EnumSet.noneOf(InstitutionStatus.class));
        DELTA.put(CLOS_SANS_SUITE, EnumSet.noneOf(InstitutionStatus.class));
    }

    /**
     * États d'arrivée atteints sans proposition humaine : EN_HOMOLOGATION à l'ouverture de la
     * première liaison (D-57) ; A_REGULARISER à l'échéance J-0 d'une pièce (D-13).
     */
    private static final Set<InstitutionStatus> AUTOMATIQUES = EnumSet.of(EN_HOMOLOGATION);

    private InstitutionLifecycle() {}

    public static Set<InstitutionStatus> transitionsPermises(InstitutionStatus depuis) {
        return Collections.unmodifiableSet(DELTA.getOrDefault(depuis, EnumSet.noneOf(InstitutionStatus.class)));
    }

    public static boolean estTerminal(InstitutionStatus etat) {
        return transitionsPermises(etat).isEmpty();
    }

    /** Étape 1 de 6.25 : rejette toute proposition hors de δ, en restituant les transitions permises. */
    public static void verifierTransition(InstitutionStatus depuis, InstitutionStatus vers) {
        if (!transitionsPermises(depuis).contains(vers)) {
            throw ParticipantException.conflict(
                "PAR_TRANSITION_NOT_ALLOWED",
                "Transition non autorisée : " + depuis + " vers " + vers,
                Map.of("de", depuis.name(), "vers", vers.name(), "permises", noms(transitionsPermises(depuis)))
            );
        }
    }

    /** Vérifie qu'un acteur peut proposer cette transition (elle n'est pas réservée au système). */
    public static void verifierProposable(InstitutionStatus depuis, InstitutionStatus vers) {
        verifierTransition(depuis, vers);
        if (AUTOMATIQUES.contains(vers)) {
            throw ParticipantException.conflict(
                "PAR_TRANSITION_AUTOMATIC",
                "Cette transition est déclenchée par le système et ne peut être proposée",
                Map.of("vers", vers.name())
            );
        }
    }

    /**
     * Reprise de l'existant (D-107) : l'état {@code vers} est atteignable depuis {@code depuis} par une suite de
     * transitions de δ (ou lui est égal). La reprise saute les étapes intermédiaires, jamais le sens de l'automate.
     */
    public static boolean atteignable(InstitutionStatus depuis, InstitutionStatus vers) {
        Set<InstitutionStatus> vus = EnumSet.of(depuis);
        java.util.Deque<InstitutionStatus> file = new java.util.ArrayDeque<>(List.of(depuis));
        while (!file.isEmpty()) {
            for (InstitutionStatus suivant : transitionsPermises(file.poll())) {
                if (vus.add(suivant)) {
                    file.add(suivant);
                }
            }
        }
        return vus.contains(vers);
    }

    /** Levée de suspension : deux approbations distinctes (RG-14) ; une seule sinon. */
    public static int approbationsRequises(InstitutionStatus depuis, InstitutionStatus vers) {
        return depuis == SUSPENDU && vers == ACTIF ? 2 : 1;
    }

    private static List<String> noms(Set<InstitutionStatus> etats) {
        return etats.stream().map(Enum::name).sorted().toList();
    }
}
