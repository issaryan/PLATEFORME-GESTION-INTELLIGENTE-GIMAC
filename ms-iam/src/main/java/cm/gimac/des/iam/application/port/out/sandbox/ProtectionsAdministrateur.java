package cm.gimac.des.iam.application.port.out.sandbox;

import java.time.Instant;
import java.util.List;

/**
 * Attribut de protection du dernier administrateur actif d'une institution (D-119, US-EXI-06), table
 * protection_administrateur : posé et levé par SBX-ESPACE, lu par l'administration déléguée et la désactivation des
 * comptes inactifs.
 */
public interface ProtectionsAdministrateur {
    /** Compte protégé, avec son institution et la date de pose. */
    record Protection(String compteId, String institutionId, Instant depuis, String acteurId) {}

    boolean protege(String compteId);

    /** Pose ou lève la protection ; vrai si l'état a changé. */
    boolean definir(String compteId, String institutionId, boolean protege, String acteurId, Instant le);

    /** Comptes protégés, page à partir de 0, triés par institution puis compte. */
    List<Protection> lister(int page, int taille);

    long compter();
}
