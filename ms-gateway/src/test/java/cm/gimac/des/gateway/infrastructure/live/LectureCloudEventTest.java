package cm.gimac.des.gateway.infrastructure.live;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.live.EvenementLive;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lecture des enveloppes CloudEvents publiées par les services (PI §0.4). */
class LectureCloudEventTest {

    static final String HOMOLOGATION = """
        {"specversion":"1.0","id":"e-1","source":"/ms-homologation","type":"homologation.liaison-opened.v1",
         "subject":"l-1","time":"2026-09-30T10:00:00Z","correlationid":"k-1","institutionid":"INST-0042",
         "data":{"reference":"LIA-2026-027","secret":"ne sort jamais"}}""";

    static final String MESSAGE = """
        {"specversion":"1.0","id":"m-1","source":"/ms-collaboration","type":"collaboration.message-sent.v1",
         "subject":"c-1","institutionid":null,
         "data":{"conversationId":"c-1","messageId":"m-1","auteurId":"u0","destinataires":["u1","u2",""," ",7],"envoyeLe":"2026-09-30T10:00:00Z"}}""";

    final LectureCloudEvent lecture = new LectureCloudEvent();

    @Test
    @DisplayName("Enveloppe d'homologation : identifiant, type, sujet, institution ; sujet fonctionnel = premier segment")
    void homologation() {
        EvenementLive e = lecture.lire("homologation.liaison", HOMOLOGATION).orElseThrow();
        assertThat(e).isEqualTo(
            new EvenementLive("e-1", "homologation.liaison-opened.v1", "l-1", "homologation", "INST-0042", java.util.Set.of())
        );
        assertThat(e.vue().toString()).doesNotContain("LIA-2026-027", "secret");
    }

    @Test
    @DisplayName("Message de la messagerie : destinataires de data.destinataires (textes non vides)")
    void message() {
        EvenementLive e = lecture.lire("collaboration.message", MESSAGE).orElseThrow();
        assertThat(e.topic()).isEqualTo(EvenementLive.COLLABORATION);
        assertThat(e.institutionId()).isNull();
        assertThat(e.destinataires()).containsExactlyInAnyOrder("u1", "u2");
    }

    @Test
    @DisplayName("Saisie (D-72) : auteur lu dans data.auteurId, destinataires ; aucun acteur hors collaboration")
    void saisie() {
        String ce = "{\"id\":\"s-1\",\"type\":\"collaboration.message-typing.v1\",\"subject\":\"c1\",\"data\":{\"auteurId\":\"u2\",\"destinataires\":[\"u1\"]}}";
        EvenementLive e = lecture.lire("collaboration.message", ce).orElseThrow();
        assertThat(e.acteur()).isEqualTo("u2");
        assertThat(e.destinataires()).containsExactly("u1");
        assertThat(e.ephemere()).isTrue();
        String h = "{\"id\":\"h-1\",\"type\":\"homologation.liaison-opened.v1\",\"data\":{\"auteurId\":\"u2\"}}";
        assertThat(lecture.lire("homologation.liaison", h).orElseThrow().acteur()).isNull();
    }

    @Test
    @DisplayName("Message dont data est une chaîne JSON : destinataires lus")
    void dataChaine() {
        String ce = "{\"id\":\"m-2\",\"type\":\"collaboration.message-sent.v1\",\"data\":\"{\\\"destinataires\\\":[\\\"u9\\\"]}\"}";
        assertThat(lecture.lire("collaboration.message", ce).orElseThrow().destinataires()).containsExactly("u9");
    }

    @Test
    @DisplayName("Message sans destinataires exploitables : aucun destinataire")
    void sansDestinataires() {
        assertThat(lecture.lire("collaboration.message", "{\"id\":\"m\",\"type\":\"t\"}").orElseThrow().destinataires()).isEmpty();
        assertThat(lecture.lire("collaboration.message", "{\"id\":\"m\",\"type\":\"t\",\"data\":{\"destinataires\":\"u1\"}}").orElseThrow().destinataires()).isEmpty();
        assertThat(lecture.lire("collaboration.message", "{\"id\":\"m\",\"type\":\"t\",\"data\":\"pas du json\"}").orElseThrow().destinataires()).isEmpty();
        assertThat(lecture.lire("collaboration.message", "{\"id\":\"m\",\"type\":\"t\",\"data\":[1]}").orElseThrow().destinataires()).isEmpty();
    }

    @Test
    @DisplayName("Hors collaboration, data.destinataires n'est pas lu")
    void destinatairesIgnores() {
        String ce = "{\"id\":\"e\",\"type\":\"t\",\"data\":{\"destinataires\":[\"u1\"]}}";
        assertThat(lecture.lire("certification.quitus", ce).orElseThrow().destinataires()).isEmpty();
    }

    @Test
    @DisplayName("Valeurs inexploitables ignorées : vide, non JSON, non objet, sans identifiant ou type")
    void ignorees() {
        assertThat(lecture.lire("homologation.liaison", null)).isEmpty();
        assertThat(lecture.lire(null, HOMOLOGATION)).isEmpty();
        assertThat(lecture.lire("homologation.liaison", " ")).isEmpty();
        assertThat(lecture.lire("homologation.liaison", "pas du json")).isEmpty();
        assertThat(lecture.lire("homologation.liaison", "[1,2]")).isEmpty();
        assertThat(lecture.lire("homologation.liaison", "{\"type\":\"t\"}")).isEmpty();
        assertThat(lecture.lire("homologation.liaison", "{\"id\":\"\",\"type\":\"t\"}")).isEmpty();
        assertThat(lecture.lire("homologation.liaison", "{\"id\":\"e\",\"type\":null}")).isEmpty();
    }

    @Test
    @DisplayName("Sujet Kafka sans point : sujet fonctionnel inchangé ; sujet CloudEvents facultatif")
    void topic() {
        assertThat(LectureCloudEvent.topic("homologation")).isEqualTo("homologation");
        assertThat(LectureCloudEvent.topic("certification.quitus")).isEqualTo("certification");
        Optional<EvenementLive> e = lecture.lire("homologation", "{\"id\":\"e\",\"type\":\"t\"}");
        assertThat(e).isPresent();
        assertThat(e.orElseThrow().subject()).isNull();
    }
}
