package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.Indicateurs;
import cm.gimac.des.pilotage.domain.activite.Rapport;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/** Rapports quotidiens et hebdomadaires (docs/lot1/mon-activite.md §2.4). */
public interface RapportsUseCase {
    /** Document joint : ticket d'un dépôt fragmenté de finalité RAPPORT_ACTIVITE, référence = rapport. */
    record DocumentSaisi(String ticket) {}

    record RapportSaisi(String type, String periode, String mode, String contenu, DocumentSaisi document) {}

    record RapportLu(Rapport rapport, Indicateurs indicateurs, Map<String, String> noms) {}

    record DocumentLu(String nom, String typeMime, long taille, InputStream flux) {}

    /** {@code vue} : moi (mes rapports) ou recus (soumis à moi, ou de mon équipe avec PIL_TEAM_READ). */
    Page<RapportLu> rapports(Appelant appelant, String vue, String type, String etat, Integer page, Integer taille);

    RapportLu rapport(Appelant appelant, UUID id);

    /** Contenu markdown proposé pour la période ; rien n'est enregistré. */
    String preremplir(Appelant appelant, String type, String periode);

    RapportLu creer(Appelant appelant, RapportSaisi saisie);

    RapportLu modifier(Appelant appelant, UUID id, RapportSaisi saisie);

    RapportLu soumettre(Appelant appelant, UUID id);

    RapportLu viser(Appelant appelant, UUID id, String commentaire);

    DocumentLu document(Appelant appelant, UUID id);
}
