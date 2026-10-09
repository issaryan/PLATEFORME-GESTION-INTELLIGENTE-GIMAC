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
    /**
     * Corps de POST /processus/{id}/instances : objet ({@code intitule}), champs des étapes initiales, désignations,
     * valeurs du formulaire de lancement (D-98 §2, par code de champ).
     */
    record Lancement(String intitule, Map<String, Object> champs, Map<String, String> designations, Map<String, Object> formulaire) {
        public Lancement(String intitule, Map<String, Object> champs, Map<String, String> designations) {
            this(intitule, champs, designations, null);
        }
    }

    /** Ce que l'appelant peut faire sur une étape. */
    record MoiEtape(boolean executer, boolean prendre, List<String> signer, boolean valider, boolean reaffecter) {
        /**
         * L'étape attend une décision de l'appelant (D-98) : l'exécuter, la prendre, la signer ou la valider ; la
         * réaffectation, geste de pilotage, n'en fait pas partie.
         */
        public boolean attendMoi() {
            return executer || prendre || !signer.isEmpty() || valider;
        }
    }

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

    /**
     * {@code vue} : miennes (je suis ou ai été exécutant, validateur ou signataire), unite (mes unités), lancees ; chaque
     * instance porte les actions permises à l'appelant sur ses étapes ({@code moi}, D-98).
     */
    Page<InstanceLue> instances(Appelant appelant, String vue, String etat, Integer page, Integer taille);

    InstanceLue lancer(Appelant appelant, UUID processusId, Lancement lancement);

    /**
     * « Mes demandes » (D-98 §3) : instances lancées par l'appelant, les plus récentes d'abord, avec l'état de chaque
     * étape, la personne qui tient chaque étape en cours (tâches) et la dernière entrée de l'historique ; filtre d'état
     * et recherche sur l'objet, le libellé ou le code du processus.
     */
    Page<InstanceLue> demandes(Appelant appelant, String etat, String q, Integer page, Integer taille);

    InstanceLue instance(Appelant appelant, UUID id);

    /** Document d'une instance (pièce du formulaire de lancement ou livrable déposé), relayé en flux depuis MS-DOCUMENT. */
    record DocumentInstance(String nom, String typeMime, long taille, java.io.InputStream flux) {}

    /**
     * QUO-04 : pièce jointe du formulaire de lancement ou livrable déposé d'une étape, lisible de qui lit l'instance
     * (403 PIL_PROCESSUS_INTERDIT sinon) ; document étranger à l'instance : 404 PIL_PROCESSUS_DOCUMENT_INCONNU.
     */
    DocumentInstance document(Appelant appelant, UUID id, String documentId);

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
