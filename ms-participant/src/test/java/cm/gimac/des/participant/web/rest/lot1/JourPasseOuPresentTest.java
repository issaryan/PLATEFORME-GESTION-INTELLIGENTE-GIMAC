package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Date de prise de contact saisie à Douala (UTC+1) juste après minuit, serveur en UTC. */
class JourPasseOuPresentTest {

    /** 7 octobre 2026 à 00 h 30 à Douala : encore le 6 octobre en UTC. */
    static final Instant MINUIT_TRENTE_DOUALA = Instant.parse("2026-10-06T23:30:00Z");

    record AncienneContrainte(@PastOrPresent LocalDate jour) {}

    @Test
    @DisplayName("Cause : @PastOrPresent compare au jour du serveur (UTC) et refuse le jour de Douala")
    void ancienneContrainteRefuseLeJourDeDouala() {
        try (
            ValidatorFactory f = Validation.byDefaultProvider()
                .configure()
                .clockProvider(() -> Clock.fixed(MINUIT_TRENTE_DOUALA, ZoneOffset.UTC))
                .buildValidatorFactory()
        ) {
            assertThat(f.getValidator().validate(new AncienneContrainte(LocalDate.parse("2026-10-07")))).hasSize(1);
        }
    }

    @Test
    @DisplayName("Correction : le jour de Douala est accepté, le lendemain refusé, l'absence de date admise")
    void jourDeDoualaAccepte() {
        var v = new JourPasseOuPresent.Validateur(Clock.fixed(MINUIT_TRENTE_DOUALA, ZoneOffset.UTC));
        assertThat(v.isValid(LocalDate.parse("2026-10-07"), null)).isTrue();
        assertThat(v.isValid(LocalDate.parse("2026-10-06"), null)).isTrue();
        assertThat(v.isValid(LocalDate.parse("2026-10-08"), null)).isFalse();
        assertThat(v.isValid(null, null)).isTrue();
        assertThat(new JourPasseOuPresent.Validateur().isValid(LocalDate.now(JourPasseOuPresent.FUSEAU), null)).isTrue();
    }

    @Test
    @DisplayName("Requête de candidature : codes normalisés avant validation, champs vides ramenés à null")
    void normalisation() {
        var r = new InstitutionRequests.CandidatureRequest("  Banque  ", "", "CM", null, " bdlg ga li ", "cm 001", null, "  ", "");
        assertThat(r.codeIdentification()).isEqualTo("BDLGGALI");
        assertThat(r.codeBanqueBeac()).isEqualTo("CM001");
        assertThat(r.sigle()).isNull();
        assertThat(r.originePriseDeContact()).isNull();
        assertThat(r.projetEnvisage()).isNull();
        try (ValidatorFactory f = Validation.buildDefaultValidatorFactory()) {
            var futur = new InstitutionRequests.CandidatureRequest("B", null, "CM", null, null, null, LocalDate.now(JourPasseOuPresent.FUSEAU).plusDays(1), null, null);
            assertThat(f.getValidator().validate(futur)).extracting(c -> c.getPropertyPath().toString()).contains("datePriseDeContact");
        }
    }
}
