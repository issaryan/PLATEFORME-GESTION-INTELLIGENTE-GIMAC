package cm.gimac.des.monitoring.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.monitoring.application.port.in.Vues.AffectationVue;
import cm.gimac.des.monitoring.application.port.in.Vues.CreneauVue;
import cm.gimac.des.monitoring.application.port.in.Vues.GrilleVue;
import cm.gimac.des.monitoring.application.port.in.Vues.NonCouverteVue;
import cm.gimac.des.monitoring.application.port.in.Vues.Personne;
import cm.gimac.des.monitoring.application.port.out.RenduPdfPort.Document;
import cm.gimac.des.monitoring.application.port.out.RenduPdfPort.Fait;
import cm.gimac.des.monitoring.infrastructure.pdf.RenduPdfDocumentEssai;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Programme en PDF au gabarit commun (D-117 §11.5, D-139) : cartouche de la grille, blocs quotidiens à deux colonnes,
 * chef de quart signalé, support de niveau 2, créneaux non couverts, filigrane du projet non publié, textes fr, en, es.
 * Personnes désignées par leur rôle.
 */
class ExportProgrammeTest {

    static final Instant EDITION = Instant.parse("2026-11-06T09:30:00Z");

    static GrilleVue grille(boolean publiee) {
        LocalDate lundi = LocalDate.parse("2026-11-09");
        List<LocalDate> jours = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            jours.add(lundi.plusDays(i));
        }
        List<Integer> tous = List.of(1, 2, 3, 4, 5, 6, 7);
        List<CreneauVue> creneaux = List.of(
            creneau("MATIN", "Matin", "Morning", "Mañana", tous, "07:00", "15:00", "JOUR", 1, true),
            creneau("SOIR", "Soir", "Evening", "Tarde", tous, "15:00", "23:00", "JOUR", 2, true),
            creneau("NUIT", "Nuit", "Night", "Noche", tous, "23:00", "07:00", "JOUR", 3, true),
            creneau("VEILLE", "Veille", "Watch", "Vigilia", List.of(1, 3, 5), "12:00", "13:00", "JOUR", 4, true),
            creneau("ANCIEN", "Ancien", "Old", "Antiguo", tous, "00:00", "01:00", "JOUR", 5, false),
            creneau("N2", "Support niveau 2", "Level 2 support", "Soporte nivel 2", tous, "00:00", "23:59", "SEMAINE", 6, true),
            creneau("N3", "Support niveau 3", "Level 3 support", "Soporte nivel 3", tous, "00:00", "23:59", "SEMAINE", 7, true)
        );
        Personne[] ops = new Personne[6];
        for (int i = 0; i < ops.length; i++) {
            ops[i] = new Personne("op-" + i, "Opérateur de supervision " + (i + 1));
        }
        List<AffectationVue> a = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate j = jours.get(i);
            a.add(affectation(j, "MATIN", ops[i % 6], true));
            a.add(affectation(j, "MATIN", ops[(i + 1) % 6], false));
            a.add(affectation(j, "SOIR", ops[(i + 2) % 6], false));
            if (i != 5) {
                a.add(affectation(j, "SOIR", ops[(i + 3) % 6], false));
                a.add(affectation(j, "NUIT", ops[(i + 4) % 6], false));
            }
        }
        a.add(affectation(lundi, "N2", new Personne("n2", "Ingénieur d'exploitation <niveau 2>"), false));
        List<NonCouverteVue> manques = List.of(new NonCouverteVue(jours.get(5), "NUIT", null, null, null, 1, List.of()), new NonCouverteVue(jours.get(5), "INCONNU", null, null, null, 2, List.of()));
        Personne chef = new Personne("chef", "Chef du service supervision");
        return new GrilleVue(
            UUID.randomUUID(), "2026-W46", 2, publiee ? "PUBLIEE" : "BROUILLON", "OPTIMAL", null, null, null,
            Instant.parse("2026-11-05T09:00:00Z"), chef, publiee ? Instant.parse("2026-11-05T10:30:00Z") : null, publiee ? chef : null,
            jours, creneaux, a, manques, List.of(), List.of()
        );
    }

    static CreneauVue creneau(String code, String fr, String en, String es, List<Integer> jours, String debut, String fin, String portee, int ordre, boolean actif) {
        return new CreneauVue(code, Map.of("fr", fr, "en", en, "es", es), jours, debut, fin, portee, 1, 1, 2, List.of(), List.of(), false, true, null, ordre, actif);
    }

    static AffectationVue affectation(LocalDate j, String creneau, Personne p, boolean chef) {
        return new AffectationVue(UUID.randomUUID(), j, creneau, null, null, false, 1, p, chef);
    }

    static ExportProgramme export() {
        Quarts quarts = mock(Quarts.class);
        when(quarts.reglages()).thenReturn(Reglages.defaut());
        when(quarts.maintenant()).thenReturn(EDITION);
        return new ExportProgramme(null, null, null, quarts);
    }

    @Test
    @DisplayName("Programme publié en français : en-tête, cartouche, blocs, chef de quart, support, manques, sans filigrane")
    void publie() {
        Document d = export().document(grille(true), "fr", "programme-2026-W46-r2");
        assertThat(d.titre()).isEqualTo("Liste des agents en permanence du 9 au 15 novembre 2026");
        assertThat(d.sousTitre()).isEqualTo("Programme 2026-W46, révision 2");
        assertThat(d.reference()).isEqualTo("programme-2026-W46-r2");
        assertThat(d.classification()).isEqualTo("INTERNE");
        assertThat(d.langue()).isEqualTo("fr");
        assertThat(d.edition()).isEqualTo(EDITION);
        assertThat(d.filigrane()).isNull();
        assertThat(d.paysage()).isTrue();
        assertThat(d.faits()).containsExactly(
            new Fait("Semaine", "2026-W46"), new Fait("Révision", "2"), new Fait("État", "Publié"), new Fait("Publication", "Publié par Chef du service supervision le 05/11/2026 11:30")
        );
        assertThat(d.corps())
            .startsWith("<h2>Créneaux quotidiens</h2><div class=\"colonnes\"><div class=\"bloc\"><h3>Matin · de 07:00 à 15:00 · lundi à dimanche</h3><table class=\"compact\">")
            .contains("<h3>Veille · de 12:00 à 13:00 · lun., mer., ven.</h3>")
            .doesNotContain("Ancien")
            .contains("<td><strong>Lundi 09/11</strong></td><td>Opérateur de supervision 1 <span class=\"badge ok\">chef de quart</span><br>Opérateur de supervision 2</td>")
            .contains("<td><strong>Samedi 14/11</strong></td><td><span class=\"muet\">Personne</span></td>")
            .contains("<h2>Support de niveau 2 de la semaine</h2><table class=\"fiche\"><tbody><tr><th>Support niveau 2</th><td>Ingénieur d&#39;exploitation &lt;niveau 2&gt;</td></tr>")
            .contains("<tr><th>Support niveau 3</th><td><span class=\"muet\">Personne</span></td></tr>")
            .contains("<div class=\"encart alerte bloc\"><h3>Créneaux non couverts</h3><ul><li>Samedi 14/11 · Nuit · manque 1</li><li>Samedi 14/11 · INCONNU · manque 2</li></ul></div>");
    }

    @Test
    @DisplayName("Projet non publié en anglais et en espagnol : filigrane, état et mention de projet traduits")
    void projet() {
        Document en = export().document(grille(false), "en", "r");
        assertThat(en.filigrane()).isEqualTo("Draft");
        assertThat(en.faits()).extracting(Fait::valeur).contains("Unpublished draft", "Unpublished draft, generated by Chef du service supervision on 05/11/2026 10:00");
        assertThat(en.faits().get(3).libelle()).isEqualTo("Generation");
        assertThat(en.corps()).contains("<h2>Daily slots</h2>", "shift lead", "Uncovered slots", "1 missing");
        Document es = export().document(grille(false), "es", "r");
        assertThat(es.filigrane()).isEqualTo("Borrador");
        assertThat(es.corps()).contains("<h2>Franjas diarias</h2>", "jefe de turno", "Franjas no cubiertas", "faltan 1");
    }

    @Test
    @DisplayName("Grille sans créneau quotidien, support ni manque : corps vide")
    void vide() {
        GrilleVue g = grille(true);
        GrilleVue nue = new GrilleVue(g.id(), g.semaine(), 1, g.etat(), g.statutSolveur(), null, null, null, g.genereeLe(), null, g.publieeLe(), null, g.jours(), List.of(), List.of(), List.of(), List.of(), List.of());
        assertThat(export().corps(nue, "fr")).isEmpty();
        assertThat(export().document(nue, "fr", "r").faits().get(3).valeur()).isEqualTo("Publié par  le 05/11/2026 11:30");
    }

    /**
     * Écrit les requêtes exactes de la route interne (programme publié en fr, en, es ; projet en fr) dans
     * GIMAC_PDF_ESSAI_DOSSIER, imprimées ensuite par ConvertisseurHtmlGotenbergReelTest de MS-DOCUMENT.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GIMAC_PDF_ESSAI_DOSSIER", matches = ".+")
    void ecrireEssai() throws Exception {
        Path d = Path.of(System.getenv("GIMAC_PDF_ESSAI_DOSSIER"));
        Files.createDirectories(d);
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String l : List.of("fr", "en", "es")) {
            Files.writeString(d.resolve("programme-" + l + ".json"), json.writeValueAsString(RenduPdfDocumentEssai.requete(export().document(grille(true), l, "programme-2026-W46-r2"))));
        }
        Files.writeString(d.resolve("programme-projet-fr.json"), json.writeValueAsString(RenduPdfDocumentEssai.requete(export().document(grille(false), "fr", "programme-2026-W46-r2"))));
        // Exemples des services simulés du frontend (exploitation/src/assets), marqués « Démonstration ».
        Map<String, String> demonstration = Map.of("fr", "Démonstration", "en", "Demonstration", "es", "Demostración");
        for (String l : List.of("fr", "en", "es")) {
            Document x = export().document(grille(true), l, "programme-2026-W46-r2");
            Document demo = new Document(x.corps(), x.titre(), x.sousTitre(), x.reference(), x.classification(), x.langue(), x.edition(), demonstration.get(l), x.faits(), x.paysage());
            Files.writeString(d.resolve("programme-demonstration-" + l + ".json"), json.writeValueAsString(RenduPdfDocumentEssai.requete(demo)));
        }
    }
}
