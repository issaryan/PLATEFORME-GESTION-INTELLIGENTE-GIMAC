package cm.gimac.des.iam.application.port.out.sandbox;

import cm.gimac.des.iam.domain.sandbox.PerimetreSuspendu;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Indicateurs de suspension des périmètres d'institution (D-119, US-EXI-06), table perimetre_suspendu. */
public interface PerimetresSuspendus {
    Optional<PerimetreSuspendu> lire(String code);

    default boolean suspendu(String code) {
        return code != null && lire(code).isPresent();
    }

    /** Pose l'indicateur s'il est absent ; vrai si l'état a changé. */
    boolean poser(String code, String acteurId, String motif, Instant le);

    /** Lève l'indicateur s'il est posé ; vrai si l'état a changé. */
    boolean lever(String code);

    /** Périmètres suspendus, les plus récents d'abord, page à partir de 0. */
    List<PerimetreSuspendu> lister(int page, int taille);

    long compter();
}
