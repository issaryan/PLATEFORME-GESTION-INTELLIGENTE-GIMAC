package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Public modifiable sur une version publiée (D-103, docs/lot1/lancement-processus.md §11) : domaine (brouillon et version
 * publiée réglables, version retirée figée, séparation des fonctions conservée), motif (3 à 500 caractères, obligatoire
 * sur une version publiée), résumé de l'historique.
 */
class PublicProcessusTest {

    private static final Instant LE = Instant.parse("2026-10-12T08:00:00Z");
    private static final Instant APRES = Instant.parse("2026-10-12T09:00:00Z");

    private static Initiateurs personnes(String... ids) {
        return new Initiateurs(List.of(RegleInitiateur.PERSONNES), List.of(), List.of(), List.of(), List.of(ids), false);
    }

    @Test
    @DisplayName("Domaine : public réglable sur un brouillon et une version publiée, séparation des fonctions conservée, version retirée figée")
    void domaine() {
        Processus p = Processus.brouillon("P", "U", "auteur", LE);
        p.definirLancement(new Initiateurs(List.of(RegleInitiateur.MEMBRES_UNITE), null, null, null, null, true), null, "auteur", LE);
        p.reglerPublic(personnes("a"), "chef", LE);
        assertThat(p.getInitiateurs().regles()).containsExactly(RegleInitiateur.PERSONNES);
        assertThat(p.getInitiateurs().initiateurIntervenant()).isTrue();

        p.publier("chef", null, false, LE);
        p.reglerPublic(personnes("a", "b"), "admin", APRES);
        assertThat(p.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(p.getInitiateurs().personnes()).containsExactly("a", "b");
        assertThat(p.getInitiateurs().initiateurIntervenant()).isTrue();
        assertThat(p.getModifiePar()).isEqualTo("admin");
        assertThat(p.getModifieLe()).isEqualTo(APRES);
        // Le reste d'une version publiée demeure figé.
        assertThatThrownBy(() -> p.definirLancement(null, null, "admin", APRES)).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_ETAT"));

        p.reglerPublic(null, "admin", APRES);
        assertThat(p.getInitiateurs().regles()).containsExactly(RegleInitiateur.MEMBRES_UNITE);
        assertThat(p.getInitiateurs().initiateurIntervenant()).isTrue();

        p.retirer("admin", "Fin", APRES);
        assertThatThrownBy(() -> p.reglerPublic(personnes("c"), "admin", APRES)).isInstanceOfSatisfying(PilotageException.class, x -> {
            assertThat(x.code()).isEqualTo("PIL_PROCESSUS_NON_MODIFIABLE");
            assertThat(x.status()).isEqualTo(409);
        });
        assertThat(p.getInitiateurs().regles()).containsExactly(RegleInitiateur.MEMBRES_UNITE);
    }

    @Test
    @DisplayName("Motif : obligatoire sur une version publiée, facultatif sur un brouillon, 3 à 500 caractères")
    void motif() {
        assertThat(SaisieProcessus.motifPublic(null, false)).isNull();
        assertThat(SaisieProcessus.motifPublic("  ", false)).isNull();
        assertThat(SaisieProcessus.motifPublic("  Ouverture  ", true)).isEqualTo("Ouverture");
        assertThat(SaisieProcessus.motifPublic("abc", true)).isEqualTo("abc");
        assertThat(SaisieProcessus.motifPublic("x".repeat(500), true)).hasSize(500);
        for (String refuse : new String[] { null, "", "  ", "ab", "x".repeat(501) }) {
            assertThatThrownBy(() -> SaisieProcessus.motifPublic(refuse, true)).isInstanceOfSatisfying(PilotageException.class, x -> {
                assertThat(x.code()).isEqualTo("PIL_REQUETE_INVALIDE");
                assertThat(x.details()).containsEntry("champ", "motif");
            });
        }
        assertThatThrownBy(() -> SaisieProcessus.motifPublic("ab", false)).isInstanceOf(PilotageException.class);
    }

    @Test
    @DisplayName("Historique : règles et paramètres résumés, personnes nommées (identifiant à défaut)")
    void resume() {
        Initiateurs i = new Initiateurs(
            List.of(RegleInitiateur.MEMBRES_UNITE, RegleInitiateur.UNITES, RegleInitiateur.PROFILS, RegleInitiateur.PERMISSIONS, RegleInitiateur.PERSONNES, RegleInitiateur.TOUT_LE_GIMAC),
            List.of("POLE_RESEAU"),
            List.of("ROLE_CONFORMITE"),
            List.of("PIL_ACTIVITY_OWN"),
            List.of("a", "inconnu"),
            false
        );
        assertThat(ProcessusService.resumePublic(i, Map.of("a", "Clarisse Bilong"))).isEqualTo(
            "MEMBRES_UNITE, UNITES (POLE_RESEAU), PROFILS (ROLE_CONFORMITE), PERMISSIONS (PIL_ACTIVITY_OWN), PERSONNES (Clarisse Bilong, inconnu), TOUT_LE_GIMAC"
        );
        assertThat(ProcessusService.resumePublic(Initiateurs.parDefaut(), Map.of())).isEqualTo("MEMBRES_UNITE");
    }
}
