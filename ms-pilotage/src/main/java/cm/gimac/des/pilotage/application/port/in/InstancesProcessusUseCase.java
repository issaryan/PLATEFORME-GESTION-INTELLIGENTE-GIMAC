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

    /**
     * Candidats à l'exécution d'une étape en cours (réaffectation) : porteurs du profil exigé (profil de la tâche à
     * prendre, de l'intervenant PROFIL, à défaut son profil de remplacement) dans l'unité puis ailleurs. Réservé à qui
     * peut réaffecter l'étape (403 PIL_PROCESSUS_INTERDIT).
     */
    ProcessusUseCase.Candidats candidats(Appelant appelant, UUID id, String etape);

    InstanceLue suspendre(Appelant appelant, UUID id, String motif);

    InstanceLue reprendre(Appelant appelant, UUID id, String motif);

    InstanceLue annuler(Appelant appelant, UUID id, String motif);

    /**
     * Départ ou désactivation d'un compte (iam.account-administered.v1 DESACTIVATION ou SUPPRESSION) : ses
     * étapes en cours et ses signatures ou validations attendues passent au profil de remplacement (tâche à
     * prendre), le chef de l'unité est notifié. @return nombre d'étapes réaffectées.
     */
    int traiterDepart(String compteId, String acteurId);

    /**
     * Profils retirés ou unité changée pour un compte (iam.account-administered.v1 PROFILS ou
     * COMPTE_RATTACHEMENT_MODIFIE, D-93) : chaque rôle que le compte tient sur une étape en cours (exécutant, validateur,
     * signataire attendu) est revu selon le mode de désignation ; s'il ne le tient plus, l'étape est réaffectée à la
     * personne résolue ou repasse à prendre. {@code cause} : PROFIL_RETIRE ou CHANGEMENT_UNITE. @return étapes revues.
     */
    int traiterChangementCompte(String compteId, String cause, String acteurId);

    /**
     * Nouveau chef d'unité (iam.configuration-changed.v1, objet UNITE_ORGANISATIONNELLE, D-93) : les rôles CHEF_UNITE
     * de cette unité sur les étapes en cours passent au nouveau chef (ou au profil de remplacement s'il n'y en a
     * plus). @return étapes revues.
     */
    int traiterChangementChef(String unite, String acteurId);
}
