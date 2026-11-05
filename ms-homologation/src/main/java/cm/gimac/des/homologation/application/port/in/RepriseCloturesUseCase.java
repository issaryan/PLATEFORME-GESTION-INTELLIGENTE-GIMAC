package cm.gimac.des.homologation.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clôture sans suite de liaisons reprises (D-137) : une liaison créée par la reprise de l'existant sans projet réel
 * (liaison par défaut « REPRISE-<code>-… » du 13/10/2026) passe au statut CLOSE_SANS_SUITE, qui est connu du parcours
 * et le fige. Aucune suppression : la liaison, son parcours et ses preuves sont conservés. Idempotent par référence
 * d'origine ; motif du lot obligatoire ; en simulation, rien n'est écrit.
 */
public interface RepriseCloturesUseCase {
    int LIGNES_MAX = 500;
    int MOTIF_MIN = 3;
    int MOTIF_MAX = 500;

    /** Liaison à clore, désignée par sa référence d'origine ; motif propre facultatif, ajouté à celui du lot. */
    record ClotureReprise(String referenceOrigine, String motif) {}

    enum Resultat {
        CLOSE,
        INCHANGEE,
        REFUSEE,
    }

    record Ligne(int rang, String referenceOrigine, Resultat resultat, UUID liaisonId, String statutPrecedent, String code, String motif, Map<String, Object> details) {}

    record Rapport(boolean simulation, int total, Map<Resultat, Long> totaux, List<Ligne> lignes) {}

    Rapport clore(List<ClotureReprise> lignes, boolean simulation, String motif, RepriseLiaisonsUseCase.Acteur acteur);
}
