package cm.gimac.des.homologation.infrastructure.client.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Couche anticorruption du contrat L04 : toute réponse hors contrat est écartée en entier. */
class TraducteurLectureSandboxTest {

    static byte[] b(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    static final String ENREGISTREMENT =
        "{\"identifiantCampagne\":\"C\",\"identifiantLiaison\":\"L\",\"codeParticipant\":\"99001\",\"identifiantCas\":\"K\",\"identifiantScenario\":\"S\"," +
        "\"versionScenario\":\"3\",\"identifiantExecution\":\"E\",\"dateExecution\":\"2026-10-13T16:52:07Z\",\"verdictNatif\":\"%s\",\"modeExecution\":\"hybride\"," +
        "\"referenceTrace\":\"SANS_TRACE\",\"position\":\"p\",\"nature\":\"%s\",\"origine\":\"REPRISE\"}";

    @Test
    @DisplayName("Page : vocabulaire des verdicts et natures, indicateur close exigé, éléments facultatifs absents")
    void page() {
        var p = TraducteurLectureSandbox.page(b("{\"enregistrements\":[" + ENREGISTREMENT.formatted("SKIPPED", "VALIDATION") + "],\"positionSuivante\":null,\"close\":false,\"etiquette\":\"e\"}"));
        assertThat(p.enregistrements().get(0).verdictNormalise()).isNull();
        assertThat(p.enregistrements().get(0).versionTable()).isNull();
        assertThat(p.positionSuivante()).isNull();
        assertThatThrownBy(() -> TraducteurLectureSandbox.page(b("{\"enregistrements\":[" + ENREGISTREMENT.formatted("OK", "VALIDATION") + "],\"close\":true,\"etiquette\":\"e\"}"))).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.page(b("{\"enregistrements\":[" + ENREGISTREMENT.formatted("PASSED", "AUTRE") + "],\"close\":true,\"etiquette\":\"e\"}"))).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.page(b("{\"enregistrements\":[],\"etiquette\":\"e\"}"))).isInstanceOf(HorsContrat.class).hasMessageContaining("close");
        assertThatThrownBy(() -> TraducteurLectureSandbox.page(b("{\"close\":true,\"etiquette\":\"e\"}"))).isInstanceOf(HorsContrat.class).hasMessageContaining("enregistrements");
        assertThatThrownBy(() -> TraducteurLectureSandbox.page(b("{\"enregistrements\":[" + ENREGISTREMENT.formatted("PASSED", "VALIDATION").replace("2026-10-13T16:52:07Z", "hier") + "],\"close\":true,\"etiquette\":\"e\"}")))
            .isInstanceOf(HorsContrat.class)
            .hasMessageContaining("ISO 8601");
    }

    @Test
    @DisplayName("Campagne : éléments exigés, nombres entiers, version du bilan facultative")
    void campagne() {
        String base = "{\"identifiantCampagne\":\"C\",\"identifiantLiaison\":\"L\",\"codeParticipant\":\"99001\",\"etat\":\"EN_COURS\",\"services\":[\"RETRAIT\"],\"taux\":[%s]}";
        var c = TraducteurLectureSandbox.campagne(b(base.formatted("")));
        assertThat(c.versionBilan()).isNull();
        assertThat(c.services()).containsExactly("RETRAIT");
        assertThatThrownBy(() -> TraducteurLectureSandbox.campagne(b(base.formatted("{\"nature\":\"REUSSITE\",\"numerateur\":\"1\",\"denominateur\":1,\"valeur\":\"1\",\"versionFormule\":\"1\",\"horodatage\":\"2026-10-13T16:52:07Z\"}"))))
            .isInstanceOf(HorsContrat.class)
            .hasMessageContaining("numerateur");
        assertThatThrownBy(() -> TraducteurLectureSandbox.campagne(b(base.formatted("").replace("\"etat\":\"EN_COURS\",", "\"etat\":\" \",")))).isInstanceOf(HorsContrat.class);
    }

    @Test
    @DisplayName("Réponses illisibles ou qui ne sont pas des objets : écartées")
    void illisibles() {
        assertThatThrownBy(() -> TraducteurLectureSandbox.pieces(b("{"))).isInstanceOf(HorsContrat.class).hasMessageContaining("illisible");
        assertThatThrownBy(() -> TraducteurLectureSandbox.pieces(b("[]"))).isInstanceOf(HorsContrat.class).hasMessageContaining("objet");
        assertThat(TraducteurLectureSandbox.pieces(b("{\"pieces\":[{\"nature\":\"BILAN\",\"version\":1,\"empreinte\":\"AB\",\"taille\":2}]}")).get(0).empreinte()).isEqualTo("ab");
    }

    @Test
    @DisplayName("Repr-Digest : sha-256 parmi plusieurs algorithmes, absent ou illisible refusé")
    void reprDigest() {
        String hex = "00".repeat(31) + "ff";
        assertThat(TraducteurLectureSandbox.empreinteRepr("sha-512=:AA==:, " + TraducteurLectureSandbox.reprDigest(hex))).isEqualTo(hex);
        assertThatThrownBy(() -> TraducteurLectureSandbox.empreinteRepr(null)).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.empreinteRepr("sha-512=:AA==:")).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.empreinteRepr("sha-256=:%%%:")).isInstanceOf(HorsContrat.class).hasMessageContaining("illisible");
        assertThatThrownBy(() -> TraducteurLectureSandbox.empreinteRepr("sha-256=:")).isInstanceOf(HorsContrat.class);
    }
}
