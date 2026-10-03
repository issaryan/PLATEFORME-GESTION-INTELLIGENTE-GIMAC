package cm.gimac.des.document.application.port.in;

import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Port entrant des dépôts fragmentés (claim check, D-74, docs/lot1/depots-fragmentes.md §3) : un
 * fichier de plusieurs gigaoctets est déposé partie par partie, puis vérifié par le serveur
 * (empreinte SHA-256, taille, numéros de carte jusqu'à 20 Mo, déduplication) ; le client reçoit
 * l'identifiant du document créé (le ticket). Seul le compte qui a ouvert un dépôt y a accès : pour
 * tout autre, le dépôt est inconnu (404 DOC_UPLOAD_NOT_FOUND).
 */
public interface DepotFragmenteUseCase {
    /** Annonce du fichier : nom, type MIME déclaré, taille exacte en octets, finalité et référence. */
    record Ouverture(String nomFichier, String typeMime, Long taille, String finalite, String reference) {}

    /** Compte qui ouvre le dépôt : sujet du jeton, institution éventuelle et permissions. */
    record Demandeur(String id, String institutionId, Set<String> permissions) {}

    /**
     * État restitué au client. {@code documentId} et {@code sha256} ne sont renseignés qu'à l'état
     * DISPONIBLE, {@code motif} qu'à l'état REJETE, {@code expireLe} (fin du délai d'inactivité)
     * qu'à l'état OUVERT.
     */
    record Etat(
        UUID id,
        EtatDepotFragmente etat,
        String nomFichier,
        String typeMime,
        long taille,
        long taillePartie,
        int nombreParties,
        List<Integer> partiesRecues,
        UUID documentId,
        String sha256,
        boolean dedoublonne,
        String motif,
        Instant expireLe
    ) {}

    /**
     * Ouvre un dépôt ; 400 (finalité, référence, taille), 403 DOC_PURPOSE_FORBIDDEN (permission
     * de la finalité absente), 413 DOC_FILE_TOO_LARGE.
     */
    Etat ouvrir(Ouverture ouverture, Demandeur demandeur);

    /**
     * Reçoit (ou remplace) la partie {@code numero} lue dans {@code corps} ; {@code longueurAnnoncee}
     * est la longueur annoncée par le client (Content-Length), négative si elle est inconnue.
     * 400 DOC_PART_SIZE si la taille n'est pas exactement celle attendue, 409 DOC_UPLOAD_CLOSED si
     * le dépôt n'est plus ouvert.
     */
    void recevoirPartie(UUID id, int numero, InputStream corps, long longueurAnnoncee, String deposant);

    Etat etat(UUID id, String deposant);

    /** Termine l'envoi : assemblage puis vérification en tâche de fond ; 409 DOC_UPLOAD_INCOMPLETE s'il manque des parties. */
    Etat terminer(UUID id, String deposant);

    /** Abandonne le dépôt (sans effet s'il l'est déjà) ; 409 DOC_UPLOAD_CLOSED s'il est terminé. */
    void abandonner(UUID id, String deposant);
}
