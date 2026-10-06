package cm.gimac.des.pilotage.domain.activite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TacheTest {

    private static final String CREATEUR = "createur";
    private static final String RESPONSABLE = "responsable";
    private static final Instant LE = Instant.parse("2026-10-06T08:00:00Z");

    private static Tache tache(EtatTache etat) {
        Tache t = new Tache(CREATEUR, RESPONSABLE, LE);
        t.decrire("Préparer le dossier", null, LocalDate.of(2026, 10, 10), PrioriteTache.NORMALE, LE);
        // Chemin permis jusqu'à l'état voulu.
        switch (etat) {
            case OUVERTE -> {}
            case EN_COURS -> t.transiter(EtatTache.EN_COURS, null, RESPONSABLE, LE);
            case BLOQUEE -> t.transiter(EtatTache.BLOQUEE, "attente", RESPONSABLE, LE);
            case TERMINEE -> {
                t.transiter(EtatTache.EN_COURS, null, RESPONSABLE, LE);
                t.transiter(EtatTache.TERMINEE, null, RESPONSABLE, LE);
            }
            case ANNULEE -> t.transiter(EtatTache.ANNULEE, "abandon", RESPONSABLE, LE);
            case A_PRENDRE -> throw new IllegalArgumentException("tâche d'unité : aPrendre()");
        }
        return t;
    }

    @ParameterizedTest(name = "{0} vers {1} : {2}")
    @CsvSource(
        {
            "OUVERTE, EN_COURS, true",
            "OUVERTE, BLOQUEE, true",
            "OUVERTE, ANNULEE, true",
            "OUVERTE, TERMINEE, false",
            "EN_COURS, BLOQUEE, true",
            "EN_COURS, TERMINEE, true",
            "EN_COURS, ANNULEE, true",
            "EN_COURS, OUVERTE, false",
            "BLOQUEE, EN_COURS, true",
            "BLOQUEE, ANNULEE, true",
            "BLOQUEE, TERMINEE, false",
            "TERMINEE, EN_COURS, true",
            "TERMINEE, ANNULEE, false",
            "ANNULEE, EN_COURS, false",
            "ANNULEE, OUVERTE, false",
        }
    )
    @DisplayName("Transitions du CC (p.238, p.245) ; hors transition permise : 409 PIL_TRANSITION_INTERDITE")
    void transitions(EtatTache de, EtatTache vers, boolean permise) {
        Tache t = tache(de);
        if (permise) {
            assertThat(t.transiter(vers, "motif", RESPONSABLE, LE)).isEqualTo(de);
            assertThat(t.getEtat()).isEqualTo(vers);
        } else {
            assertThatThrownBy(() -> t.transiter(vers, "motif", RESPONSABLE, LE)).isInstanceOfSatisfying(PilotageException.class, e -> {
                assertThat(e.status()).isEqualTo(409);
                assertThat(e.code()).isEqualTo("PIL_TRANSITION_INTERDITE");
            });
            assertThat(t.getEtat()).isEqualTo(de);
        }
    }

    @Test
    @DisplayName("BLOQUEE, ANNULEE et la réouverture exigent un motif : 422 PIL_MOTIF_OBLIGATOIRE")
    void motifs() {
        motifExige(tache(EtatTache.OUVERTE), EtatTache.BLOQUEE);
        motifExige(tache(EtatTache.EN_COURS), EtatTache.ANNULEE);
        motifExige(tache(EtatTache.TERMINEE), EtatTache.EN_COURS);
        Tache t = tache(EtatTache.OUVERTE);
        t.transiter(EtatTache.EN_COURS, " ", RESPONSABLE, LE);
        assertThat(t.getEtat()).isEqualTo(EtatTache.EN_COURS);
    }

    private static void motifExige(Tache t, EtatTache vers) {
        assertThatThrownBy(() -> t.transiter(vers, "  ", RESPONSABLE, LE)).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.status()).isEqualTo(422);
            assertThat(e.code()).isEqualTo("PIL_MOTIF_OBLIGATOIRE");
        });
    }

    @Test
    @DisplayName("Le responsable fait les transitions ; le créateur peut seulement annuler ; tout autre : 403")
    void acteurs() {
        Tache t = tache(EtatTache.OUVERTE);
        assertThatThrownBy(() -> t.transiter(EtatTache.EN_COURS, null, CREATEUR, LE)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_ACTION_RESERVEE")
        );
        assertThatThrownBy(() -> t.transiter(EtatTache.ANNULEE, "x", "autre", LE)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.status()).isEqualTo(403)
        );
        t.transiter(EtatTache.ANNULEE, "plus nécessaire", CREATEUR, LE);
        assertThat(t.getEtat()).isEqualTo(EtatTache.ANNULEE);
    }

    @Test
    @DisplayName("Terminée : date de fin posée ; réouverte : effacée ; retard selon l'échéance et l'état")
    void termineeEtRetard() {
        Tache t = tache(EtatTache.EN_COURS);
        assertThat(t.enRetard(LocalDate.of(2026, 10, 11))).isTrue();
        assertThat(t.enRetard(LocalDate.of(2026, 10, 10))).isFalse();
        Instant fin = LE.plusSeconds(60);
        t.transiter(EtatTache.TERMINEE, null, RESPONSABLE, fin);
        assertThat(t.getTermineeLe()).isEqualTo(fin);
        assertThat(t.enRetard(LocalDate.of(2026, 10, 11))).isFalse();
        t.transiter(EtatTache.EN_COURS, "reprise demandée", RESPONSABLE, fin);
        assertThat(t.getTermineeLe()).isNull();
    }

    @ParameterizedTest(name = "terminée par le service depuis {0} : {1}")
    @CsvSource({ "OUVERTE, true", "EN_COURS, true", "BLOQUEE, true", "TERMINEE, false", "ANNULEE, false" })
    @DisplayName("D-77 : le service qui a créé la tâche la termine depuis tout état ouvert ; sinon rien ne change")
    void terminerParService(EtatTache depuis, boolean change) {
        Tache t = tache(depuis);
        Instant plusTard = LE.plusSeconds(60);
        EtatTache avant = t.terminerParService(plusTard);
        if (change) {
            assertThat(avant).isEqualTo(depuis);
            assertThat(t.getEtat()).isEqualTo(EtatTache.TERMINEE);
            assertThat(t.getTermineeLe()).isEqualTo(plusTard);
        } else {
            assertThat(avant).isNull();
            assertThat(t.getEtat()).isEqualTo(depuis);
        }
    }

    @ParameterizedTest(name = "rouverte par le service depuis {0} : {1}")
    @CsvSource({ "OUVERTE, false", "EN_COURS, false", "BLOQUEE, false", "TERMINEE, true", "ANNULEE, false" })
    @DisplayName("D-77 : le service rouvre une tâche terminée (EN_COURS) ; une tâche ouverte ou annulée reste inchangée")
    void rouvrirParService(EtatTache depuis, boolean change) {
        Tache t = tache(depuis);
        EtatTache avant = t.rouvrirParService(LE.plusSeconds(60));
        if (change) {
            assertThat(avant).isEqualTo(EtatTache.TERMINEE);
            assertThat(t.getEtat()).isEqualTo(EtatTache.EN_COURS);
            assertThat(t.getTermineeLe()).isNull();
        } else {
            assertThat(avant).isNull();
            assertThat(t.getEtat()).isEqualTo(depuis);
        }
    }

    // ------------------------------------------------------------------ tâches confiées (D-81 §6)

    private static Tache aPrendre() {
        Tache t = Tache.pourUnite(CREATEUR, "POLE_RESEAU", LE);
        t.decrire("Renouveler les certificats", null, LocalDate.of(2026, 10, 10), PrioriteTache.HAUTE, LE);
        return t;
    }

    @Test
    @DisplayName("Tâche d'unité : à prendre, sans responsable ; prise ou attribution vers OUVERTE, une seule fois (409)")
    void aPrendreVersOuverte() {
        Tache t = aPrendre();
        assertThat(t.getEtat()).isEqualTo(EtatTache.A_PRENDRE);
        assertThat(t.getResponsableId()).isNull();
        assertThat(t.getUniteCible()).isEqualTo("POLE_RESEAU");
        assertThat(t.enRetard(LocalDate.of(2026, 10, 11))).isTrue();
        assertThat(EtatTache.A_PRENDRE.peutPasserA(EtatTache.OUVERTE)).isTrue();
        assertThat(EtatTache.A_PRENDRE.peutPasserA(EtatTache.ANNULEE)).isTrue();
        assertThat(EtatTache.A_PRENDRE.peutPasserA(EtatTache.EN_COURS)).isFalse();
        assertThat(EtatTache.OUVERTE.peutPasserA(EtatTache.A_PRENDRE)).isFalse();

        Instant prise = LE.plusSeconds(60);
        t.confierA(RESPONSABLE, prise);
        assertThat(t.getEtat()).isEqualTo(EtatTache.OUVERTE);
        assertThat(t.getResponsableId()).isEqualTo(RESPONSABLE);
        assertThat(t.getModifieLe()).isEqualTo(prise);
        assertThatThrownBy(() -> t.confierA("autre", prise)).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.code()).isEqualTo("PIL_TRANSITION_INTERDITE");
        });
        assertThat(t.getResponsableId()).isEqualTo(RESPONSABLE);
        t.transiter(EtatTache.EN_COURS, null, RESPONSABLE, prise);
        assertThat(t.getEtat()).isEqualTo(EtatTache.EN_COURS);
    }

    @Test
    @DisplayName("Tâche à prendre : seul le créateur l'annule (motif exigé) ; ni la transition OUVERTE ni une autre ; annulée, elle ne se prend plus")
    void aPrendreTransitions() {
        Tache t = aPrendre();
        assertThatThrownBy(() -> t.transiter(EtatTache.OUVERTE, null, "membre", LE)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_ACTION_RESERVEE")
        );
        assertThatThrownBy(() -> t.transiter(EtatTache.OUVERTE, null, CREATEUR, LE)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_ACTION_RESERVEE")
        );
        assertThatThrownBy(() -> t.transiter(EtatTache.ANNULEE, "x", "membre", LE)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.status()).isEqualTo(403)
        );
        assertThatThrownBy(() -> t.transiter(EtatTache.ANNULEE, " ", CREATEUR, LE)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_MOTIF_OBLIGATOIRE")
        );
        assertThatThrownBy(() -> t.confierA(" ", LE)).isInstanceOf(PilotageException.class);
        t.transiter(EtatTache.ANNULEE, "plus nécessaire", CREATEUR, LE);
        assertThat(t.getEtat()).isEqualTo(EtatTache.ANNULEE);
        assertThat(t.getResponsableId()).isNull();
        assertThatThrownBy(() -> t.confierA(RESPONSABLE, LE)).isInstanceOfSatisfying(PilotageException.class, e -> assertThat(e.status()).isEqualTo(409));
    }

    @Test
    @DisplayName("Relance : une par 24 h (429 PIL_RELANCE_RECENTE avec la prochaine date) ; tâche à prendre ou close : 409")
    void relance() {
        Tache t = tache(EtatTache.EN_COURS);
        t.relancer(LE);
        assertThat(t.getRelanceeLe()).isEqualTo(LE);
        assertThatThrownBy(() -> t.relancer(LE.plus(Duration.ofHours(23)))).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.status()).isEqualTo(429);
            assertThat(e.code()).isEqualTo("PIL_RELANCE_RECENTE");
            assertThat(e.details()).containsEntry("prochaineRelance", LE.plus(Tache.DELAI_RELANCE).toString());
        });
        assertThat(t.getRelanceeLe()).isEqualTo(LE);
        t.relancer(LE.plus(Duration.ofHours(24)));
        assertThat(t.getRelanceeLe()).isEqualTo(LE.plus(Duration.ofHours(24)));

        for (Tache close : List.of(aPrendre(), tache(EtatTache.TERMINEE), tache(EtatTache.ANNULEE))) {
            assertThatThrownBy(() -> close.relancer(LE)).isInstanceOfSatisfying(PilotageException.class, e -> {
                assertThat(e.status()).isEqualTo(409);
                assertThat(e.code()).isEqualTo("PIL_RELANCE_IMPOSSIBLE");
            });
        }
    }

    @Test
    @DisplayName("Envoi à plusieurs personnes : chaque tâche porte le groupe")
    void groupe() {
        Tache t = tache(EtatTache.OUVERTE);
        assertThat(t.getGroupeId()).isNull();
        UUID g = UUID.randomUUID();
        t.rattacherAuGroupe(g);
        assertThat(t.getGroupeId()).isEqualTo(g);
    }
}
