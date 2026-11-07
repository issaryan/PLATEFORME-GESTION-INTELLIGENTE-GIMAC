package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.InstitutionReprise;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase.ServiceRepris;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contrôle d'une ligne de reprise des institutions (D-107) : normalisation, refus par champ, avertissements. */
class RepriseInstitutionsValidationTest {

    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 13);

    static InstitutionReprise ligne(String code, String statut, String mode, String compteReglement, String marraine, String cantonnement, LocalDate adhesion) {
        return new InstitutionReprise(code, null, "Banque fictive", null, "cm", "banque", null, null, null, statut, null, null, null, adhesion, mode, compteReglement, cantonnement, null, marraine, null);
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> champs(InstitutionReprise l) {
        try {
            RepriseInstitutionsValidation.valider(l, AUJOURDHUI);
        } catch (ParticipantException e) {
            assertThat(e.code()).isEqualTo("PAR_REPRISE_LIGNE_INVALIDE");
            return (Map<String, String>) e.details().get("champs");
        }
        throw new AssertionError("refus attendu");
    }

    @Test
    @DisplayName("Ligne complète : valeurs normalisées (majuscules, espaces du groupe), aucun avertissement")
    void normalisation() {
        var f = RepriseInstitutionsValidation.valider(
            new InstitutionReprise(
                " bq-01 ", null, " Banque fictive ", "BF", "cm", "banque", "bdlggali", "b01", "  Groupe   A ", "actif",
                LocalDate.of(2022, 1, 1), "PV-1", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1), "direct", "CR-1", null, null, null,
                List.of(new ServiceRepris("inc_account_inquiry", LocalDate.of(2021, 2, 1)))
            ),
            AUJOURDHUI
        );
        assertThat(f.codeMembre()).isEqualTo("BQ-01");
        assertThat(f.nom()).isEqualTo("Banque fictive");
        assertThat(f.pays()).isEqualTo("CM");
        assertThat(f.categorie()).isEqualTo(InstitutionCategory.BANQUE);
        assertThat(f.statut()).isEqualTo(InstitutionStatus.ACTIF);
        assertThat(f.mode()).isEqualTo(MembershipMode.DIRECT);
        assertThat(f.codeIdentification()).isEqualTo("BDLGGALI");
        assertThat(f.groupe()).isEqualTo("Groupe A");
        assertThat(f.services()).containsExactly(new ServiceRepris("INC_ACCOUNT_INQUIRY", LocalDate.of(2021, 2, 1)));
        assertThat(f.avertissements()).isEmpty();
    }

    @Test
    @DisplayName("Champs obligatoires absents et valeurs inconnues : un message par champ")
    void obligatoires() {
        Map<String, String> c = champs(new InstitutionReprise(null, null, " ", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThat(c).containsOnlyKeys("codeMembre", "nom", "pays", "categorie", "statut");
        c = champs(new InstitutionReprise("a b", null, "x".repeat(256), "s".repeat(51), "CMR", "ASSURANCE", "BAD", "B-1", "g".repeat(101), "ACTIVE",
            AUJOURDHUI.plusDays(1), "r".repeat(101), AUJOURDHUI.plusDays(1), AUJOURDHUI.plusDays(1), "MIXTE", "c".repeat(51), "c".repeat(51), "a b", "a b", null));
        assertThat(c).containsKeys("codeMembre", "nom", "sigle", "pays", "categorie", "statut", "modeAdhesion", "codeIdentification", "codeBanqueBeac",
            "groupe", "referenceDecision", "compteReglement", "compteCantonnement", "banqueReglement", "banqueMarraine", "dateDecision",
            "datePriseDeContact", "dateAdhesion");
        assertThat(c.get("categorie")).contains("ASSURANCE");
    }

    @Test
    @DisplayName("Services souscrits : code invalide, doublon, date future, ligne vide")
    void services() {
        assertThat(champs(avecServices(new ServiceRepris("1BAD", null))).get("servicesSouscrits")).contains("invalide");
        assertThat(champs(avecServices((ServiceRepris) null)).get("servicesSouscrits")).contains("vide");
        assertThat(champs(avecServices(new ServiceRepris("A_B", null), new ServiceRepris("a_b", null))).get("servicesSouscrits")).contains("double");
        assertThat(champs(avecServices(new ServiceRepris("A_B", AUJOURDHUI.plusDays(1)))).get("servicesSouscrits")).contains("future");
    }

    @Test
    @DisplayName("Codes de service du TDR-SBX-GIMAC-02 annexe D.1 : préfixes ISO. et CRT. admis, codes mal formés refusés")
    void codesDuTdr() {
        var f = RepriseInstitutionsValidation.valider(avecServices(new ServiceRepris("ISO.W2W", null), new ServiceRepris("crt.net", null), new ServiceRepris("W2W", null)), AUJOURDHUI);
        assertThat(f.services()).extracting(ServiceRepris::code).containsExactly("ISO.W2W", "CRT.NET", "W2W");
        for (String mal : List.of(".W2W", "1SO.W2W", "ISO W2W", "ISO-W2W", "A", "X".repeat(51))) {
            assertThat(champs(avecServices(new ServiceRepris(mal, null))).get("servicesSouscrits")).as(mal).contains("invalide");
        }
    }

    static InstitutionReprise avecServices(ServiceRepris... s) {
        return new InstitutionReprise("BQ-1", null, "Banque", null, "CM", "BANQUE", null, null, null, "CANDIDAT", null, null, null, null, null, null, null, null, null, Arrays.asList(s));
    }

    @Test
    @DisplayName("Avertissements : pays hors CEMAC, conditions d'adhésion incomplètes, date d'adhésion inconnue")
    void avertissements() {
        var horsCemac = RepriseInstitutionsValidation.valider(
            new InstitutionReprise("REM-1", null, "Partenaire", null, "fr", "PARTENAIRE_REMITTANCE", null, null, "MFSA", "CANDIDAT", null, null, null, null, null, null, null, null, null, null),
            AUJOURDHUI
        );
        assertThat(horsCemac.avertissements()).singleElement().asString().contains("hors CEMAC");
        assertThat(RepriseInstitutionsValidation.valider(ligne("B-1", "ACTIF", null, null, null, null, null), AUJOURDHUI).avertissements())
            .anyMatch(a -> a.contains("Mode d'adhésion inconnu"))
            .anyMatch(a -> a.contains("Date d'adhésion inconnue"));
        assertThat(RepriseInstitutionsValidation.valider(ligne("B-1", "ACTIF", "DIRECT", null, null, null, AUJOURDHUI), AUJOURDHUI).avertissements())
            .singleElement().asString().contains("RG-11");
        assertThat(RepriseInstitutionsValidation.valider(ligne("B-1", "SUSPENDU", "INDIRECT", null, "BQ-2", null, AUJOURDHUI), AUJOURDHUI).avertissements())
            .singleElement().asString().contains("RG-12");
        assertThat(RepriseInstitutionsValidation.valider(ligne("B-1", "ACTIF", "INDIRECT", null, "BQ-2", "CC-1", AUJOURDHUI), AUJOURDHUI).avertissements()).isEmpty();
        // Adhérent : la date de la décision tient lieu de date d'adhésion ; candidat et clos sans suite : rien à signaler.
        var adherent = new InstitutionReprise("B-1", null, "B", null, "CM", "BANQUE", null, null, null, "ADHERENT", AUJOURDHUI, null, null, null, "DIRECT", "CR", null, null, null, null);
        assertThat(RepriseInstitutionsValidation.valider(adherent, AUJOURDHUI).avertissements()).isEmpty();
        assertThat(RepriseInstitutionsValidation.valider(ligne("B-1", "CLOS_SANS_SUITE", null, null, null, null, null), AUJOURDHUI).avertissements()).isEmpty();
        assertThat(RepriseInstitutionsValidation.valider(ligne("B-1", "CANDIDAT", null, null, null, null, null), AUJOURDHUI).avertissements()).isEmpty();
    }
}
