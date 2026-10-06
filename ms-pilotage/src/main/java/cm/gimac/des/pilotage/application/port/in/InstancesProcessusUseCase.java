package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exécution des processus d'unité (§2) : lancement, étapes (dépôts, champs, fin, signatures FIDO2, validation),
 * réaffectation, suspension, reprise, annulation ; départ d'une personne (événement de MS-IAM).
 */
public interface InstancesProcessusUseCase {
    /** Corps de POST /processus/{id}/instances. */
    record Lancement(String intitule, Map<String, Object> champs, Map<String, String> designations) {}

    /** Ce que l'appelant peut faire sur une étape. */
    record MoiEtape(boolean executer, boolean prendre, List<String> signer, boolean valider, boolean reaffecter) {}

    record DroitsInstance(boolean suspendre, boolean reprendre, boolean annuler, boolean reaffecter) {}

    /** Instance lue : définition utilisée, étapes, tâches liées, historique, noms, droits de l'appelant. */
    record InstanceLue(
        InstanceProcessus instance,
        Processus processus,
        List<EtapeInstance> etapes,
        Map<UUID, Tache> taches,
        List<HistoriqueProcessus> historique,
        Map<String, String> noms,
        Map<String, MoiEtape> moi,
        DroitsInstance droits,
        LocalDate aujourdhui
    ) {}

    /** {@code vue} : miennes (je suis ou ai été exécutant, validateur ou signataire), unite (mes unités), lancees. */
    Page<InstanceLue> instances(Appelant appelant, String vue, String etat, Integer page, Integer taille);

    InstanceLue lancer(Appelant appelant, UUID processusId, Lancement lancement);

    InstanceLue instance(Appelant appelant, UUID id);

    InstanceLue deposer(Appelant appelant, UUID id, String etape, String livrable, String documentId);

    InstanceLue renseigner(Appelant appelant, UUID id, String etape, Map<String, Object> valeurs);

    InstanceLue signer(Appelant appelant, UUID id, String etape, String signature, String assertion);

    InstanceLue terminer(Appelant appelant, UUID id, String etape);

    InstanceLue valider(Appelant appelant, UUID id, String etape, String decision, String motif);

    InstanceLue reaffecter(Appelant appelant, UUID id, String etape, String personneId, String motif);

    InstanceLue suspendre(Appelant appelant, UUID id, String motif);

    InstanceLue reprendre(Appelant appelant, UUID id, String motif);

    InstanceLue annuler(Appelant appelant, UUID id, String motif);

    /**
     * Départ ou désactivation d'un compte (iam.account-administered.v1 DESACTIVATION ou SUPPRESSION) : ses
     * étapes en cours et ses signatures ou validations attendues passent au profil de remplacement (tâche à
     * prendre), le chef de l'unité est notifié. @return nombre d'étapes réaffectées.
     */
    int traiterDepart(String compteId, String acteurId);
}
