package cm.gimac.des.pilotage.domain.activite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class RapportTest {

    private static final Instant LE = Instant.parse("2026-10-06T17:00:00Z");
    private static final Indicateurs IND = new Indicateurs(2, 3, 120, 1);

    private static Rapport rapport(ModeRapport mode) {
        return new Rapport(UUID.randomUUID(), "auteur", Periode.de(TypeRapport.QUOTIDIEN, "2026-10-06"), mode, LE);
    }

    private static void erreur(Executable e, int statut, String code) {
        assertThatThrownBy(e::execute).isInstanceOfSatisfying(PilotageException.class, x -> {
            assertThat(x.status()).isEqualTo(statut);
            assertThat(x.code()).isEqualTo(code);
        });
    }

    @Test
    @DisplayName("Soumission : contenu ou document exigé (422 PIL_RAPPORT_INCOMPLET), destinataire exigé (422 PIL_SANS_RESPONSABLE)")
    void soumission() {
        Rapport vide = rapport(ModeRapport.REDIGE);
        erreur(() -> vide.soumettre("n1", IND, LE), 422, "PIL_RAPPORT_INCOMPLET");
        Rapport document = rapport(ModeRapport.DOCUMENT);
        document.rediger(ModeRapport.DOCUMENT, "Texte d'accompagnement", LE);
        erreur(() -> document.soumettre("n1", IND, LE), 422, "PIL_RAPPORT_INCOMPLET");
        document.joindre(UUID.randomUUID(), "rapport.pdf", "application/pdf", 10, LE);

        erreur(() -> document.soumettre(null, IND, LE), 422, "PIL_SANS_RESPONSABLE");
        assertThat(document.getEtat()).isEqualTo(EtatRapport.BROUILLON);
        document.soumettre("n1", IND, LE);
        assertThat(document.getEtat()).isEqualTo(EtatRapport.SOUMIS);
        assertThat(document.getDestinataireId()).isEqualTo("n1");
        assertThat(document.indicateursFiges()).isEqualTo(IND);
    }

    @Test
    @DisplayName("Un rapport soumis n'est plus modifiable (409) ; seul un rapport soumis est visé ; VISE est final")
    void cycle() {
        Rapport r = rapport(ModeRapport.REDIGE);
        r.rediger(ModeRapport.REDIGE, "Réalisé : revue du dossier", LE);
        erreur(() -> r.viser("n1", null, LE), 409, "PIL_TRANSITION_INTERDITE");
        r.soumettre("n1", IND, LE);
        erreur(() -> r.rediger(ModeRapport.REDIGE, "autre", LE), 409, "PIL_RAPPORT_NON_MODIFIABLE");
        erreur(() -> r.soumettre("n1", IND, LE), 409, "PIL_RAPPORT_NON_MODIFIABLE");
        r.viser("n1", "Bien reçu", LE);
        assertThat(r.getEtat()).isEqualTo(EtatRapport.VISE);
        assertThat(r.getVisaCommentaire()).isEqualTo("Bien reçu");
        erreur(() -> r.viser("n2", null, LE), 409, "PIL_TRANSITION_INTERDITE");
        erreur(() -> r.joindre(UUID.randomUUID(), "x", "text/plain", 1, LE), 409, "PIL_RAPPORT_NON_MODIFIABLE");
    }
}
