package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.Indicateurs;
import cm.gimac.des.pilotage.domain.activite.MotifDestinataire;
import cm.gimac.des.pilotage.domain.activite.Rapport;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Rapports quotidiens et hebdomadaires (docs/lot1/mon-activite.md §2.4). */
public interface RapportsUseCase {
    /** Document joint : ticket d'un dépôt fragmenté de finalité RAPPORT_ACTIVITE, référence = rapport. */
    record DocumentSaisi(String ticket) {}

    record RapportSaisi(String type, String periode, String mode, String contenu, DocumentSaisi document) {}

    record RapportLu(Rapport rapport, Indicateurs indicateurs, Map<String, String> noms) {}

    record DocumentLu(String nom, String typeMime, long taille, InputStream flux) {}

    /**
     * Destinataire possible d'un rapport : {@code lien} N_PLUS_UN, CHEF_UNITE, CHEF_UNITE_PARENTE (chef de l'unité
     * {@code unite}) ou CHAINE (responsable plus haut dans la chaîne hiérarchique).
     */
    record ChoixDestinataire(String id, String lien, String unite) {}

    /**
     * Destinataire qui recevra le rapport s'il est soumis maintenant ({@code destinataireId} et {@code motif} nuls :
     * rapport déposé, {@code depot}), et choix possibles dans la chaîne hiérarchique de l'auteur.
     */
    record Destinataires(String destinataireId, MotifDestinataire motif, boolean depot, List<ChoixDestinataire> choix, Map<String, String> noms) {}

    /** {@code vue} : moi (mes rapports) ou recus (soumis à moi, ou de mon équipe avec PIL_TEAM_READ). */
    Page<RapportLu> rapports(Appelant appelant, String vue, String type, String etat, Integer page, Integer taille);

    RapportLu rapport(Appelant appelant, UUID id);

    /** Contenu markdown proposé pour la période ; rien n'est enregistré. */
    String preremplir(Appelant appelant, String type, String periode);

    RapportLu creer(Appelant appelant, RapportSaisi saisie);

    RapportLu modifier(Appelant appelant, UUID id, RapportSaisi saisie);

    /**
     * Soumission : destinataire résolu (n+1 actif, chef de l'unité, chef d'une unité parente) ou choisi par l'auteur dans
     * sa chaîne hiérarchique ({@code destinataireId}, sinon 422 PIL_DESTINATAIRE_INVALIDE) ; aucun : rapport déposé.
     */
    RapportLu soumettre(Appelant appelant, UUID id, String destinataireId);

    /** Destinataire et choix possibles avant la soumission ; auteur seul. */
    Destinataires destinataires(Appelant appelant, UUID id);

    RapportLu viser(Appelant appelant, UUID id, String commentaire);

    DocumentLu document(Appelant appelant, UUID id);
}
