package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.EssaisRepris;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.LiaisonReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.OperationReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.RessourceReprise;
import cm.gimac.des.homologation.application.port.in.RepriseLiaisonsUseCase.ServiceRepris;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.Avancement;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.EtatRepris;
import cm.gimac.des.homologation.domain.parcours.ValeurReference;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contrôle d'une liaison reprise (D-107) : normalisation, avancement par défaut, refus par champ. */
class RepriseLiaisonsValidationTest {

    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 13);
    static final LocalDate FUTUR = AUJOURDHUI.plusDays(1);

    static LiaisonReprise ligne(String avancement, List<RessourceReprise> ressources, List<ServiceRepris> services, List<OperationReprise> ops) {
        return new LiaisonReprise("202604-BQ-01", "bq-1", "paiement_mobile", " Projet ", " Suivi Excel ", null, null, LocalDate.of(2025, 1, 1), null, ressources, "bq-p", services, avancement, ops, null);
    }

    static OperationReprise op(String rang, String etat) {
        return new OperationReprise(rang, etat, null, null, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> champs(LiaisonReprise l) {
        try {
            RepriseLiaisonsValidation.valider(l, AUJOURDHUI);
        } catch (HomologationException e) {
            assertThat(e.code()).isEqualTo("HOM_REPRISE_LIGNE_INVALIDE");
            return (Map<String, String>) e.details().get("champs");
        }
        throw new AssertionError("refus attendu");
    }

    @Test
    @DisplayName("Ligne valide : normalisée ; avancement par défaut selon la présence d'opérations")
    void normalisation() {
        var v = RepriseLiaisonsValidation.valider(
            ligne(null, List.of(new RessourceReprise("sna", UUID.randomUUID())), List.of(new ServiceRepris("inc_account_inquiry", null, true, " 12 cas ")), List.of(
                new OperationReprise("M5", "faite", null, AUJOURDHUI, " ok ", new ValeurReference("BIN", "123456"), "DOC-1", "a".repeat(64), null)
            )),
            AUJOURDHUI
        );
        assertThat(v.codeMembre()).isEqualTo("BQ-1");
        assertThat(v.typeProjet().name()).isEqualTo("PAIEMENT_MOBILE");
        assertThat(v.intitule()).isEqualTo("Projet");
        assertThat(v.origineEtat()).isEqualTo("Suivi Excel");
        assertThat(v.banquePartenaire()).isEqualTo("BQ-P");
        assertThat(v.avancement()).isEqualTo(Avancement.DETAILLE);
        assertThat(v.ressources()).containsKey(Fonction.SNA);
        assertThat(v.services().get(0).code()).isEqualTo("INC_ACCOUNT_INQUIRY");
        assertThat(v.services().get(0).commentaire()).isEqualTo("12 cas");
        assertThat(v.operations().get(0).etat()).isEqualTo(EtatRepris.FAITE);
        assertThat(v.operations().get(0).commentaire()).isEqualTo("ok");
        assertThat(RepriseLiaisonsValidation.valider(ligne(null, null, null, null), AUJOURDHUI).avancement()).isEqualTo(Avancement.A_QUALIFIER);
        assertThat(RepriseLiaisonsValidation.valider(ligne("en_production", null, null, null), AUJOURDHUI).avancement()).isEqualTo(Avancement.EN_PRODUCTION);
    }

    @Test
    @DisplayName("Champs obligatoires et formats : un message par champ")
    void formats() {
        assertThat(champs(new LiaisonReprise(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)))
            .containsOnlyKeys("referenceOrigine", "codeMembre", "typeProjet");
        assertThat(champs(new LiaisonReprise("réf ?", "a b", "x", "i".repeat(201), "o".repeat(101), FUTUR, null, FUTUR, null, null, "a b", null, "PLUS_TARD", null, null)))
            .containsKeys("referenceOrigine", "codeMembre", "typeProjet", "intitule", "origineEtat", "dateOuverture", "dateMiseEnProduction", "banquePartenaire", "avancement");
        assertThat(champs(new LiaisonReprise("R", "BQ", "PAIEMENT_MOBILE", null, null, null, null, null, null, null, null, null, "EN_PRODUCTION", null, null)))
            .containsKey("dateMiseEnProduction");
        assertThat(champs(ligne("A_QUALIFIER", null, null, List.of(op("M5", "FAITE"))))).containsKey("avancement");
    }

    @Test
    @DisplayName("Codes de service du TDR-SBX-GIMAC-02 D.1 (D-143) : préfixes ISO. et CRT. admis, codes mal formés refusés")
    void codesDuTdr() {
        var v = RepriseLiaisonsValidation.valider(ligne(null, null, List.of(new ServiceRepris("ISO.W2W", null, null, null), new ServiceRepris("crt.net", null, null, null)), null), AUJOURDHUI);
        assertThat(v.services()).extracting(s -> s.code()).containsExactly("ISO.W2W", "CRT.NET");
        for (String mal : List.of(".W2W", "1SO.W2W", "ISO W2W", "ISO-W2W", "A", "X".repeat(41))) {
            assertThat(champs(ligne(null, null, List.of(new ServiceRepris(mal, null, null, null)), null)).get("services")).as(mal).contains("invalide");
        }
    }

    @Test
    @DisplayName("Ressources, services et opérations invalides")
    void listes() {
        UUID c = UUID.randomUUID();
        assertThat(champs(ligne(null, Arrays.asList(new RessourceReprise("INCONNUE", c)), null, null)).get("ressources")).contains("INCONNUE");
        assertThat(champs(ligne(null, Arrays.asList((RessourceReprise) null), null, null)).get("ressources")).contains("vide");
        assertThat(champs(ligne(null, List.of(new RessourceReprise("CONDUCTEUR", c)), null, null)).get("ressources")).contains("non affectable");
        assertThat(champs(ligne(null, List.of(new RessourceReprise("SNA", c), new RessourceReprise("SNA", c)), null, null)).get("ressources")).contains("double");
        assertThat(champs(ligne(null, null, Arrays.asList((ServiceRepris) null), null)).get("services")).contains("vide");
        assertThat(champs(ligne(null, null, List.of(new ServiceRepris("A", null, null, null), new ServiceRepris("AB", null, null, null), new ServiceRepris("ab", null, null, null)), null)).get("services")).contains("double");
        assertThat(champs(ligne(null, null, List.of(new ServiceRepris("AB", FUTUR, null, null)), null)).get("services")).contains("future");
        Map<String, String> ops = champs(ligne(null, null, null, Arrays.asList(
            op(null, "FAITE"),
            op("M1", "FINIE"),
            op("M2", "FAITE"),
            op("M2", "FAITE"),
            new OperationReprise("M3", "FAITE", null, FUTUR, null, null, null, null, null),
            new OperationReprise("M4", "FAITE", null, null, null, null, "DOC", "XYZ", null),
            new OperationReprise("M5", "FAITE", null, null, null, null, "DOC", null, null),
            new OperationReprise("M6", "FAITE", null, null, "c".repeat(501), null, null, null, null),
            new OperationReprise("M7", "FAITE", null, null, null, new ValeurReference("BIN", " "), null, null, null),
            op("M8", null)
        )));
        assertThat(ops).containsKeys("operations", "operations.M1", "operations.M2", "operations.M3", "operations.M4", "operations.M5", "operations.M6", "operations.M7", "operations.M8");
    }

    static LiaisonReprise avecEssais(EssaisRepris e) {
        return new LiaisonReprise("R-1", "BQ-1", "PAIEMENT_MOBILE", null, null, null, null, null, null, null, null, null, null, null, e);
    }

    @Test
    @DisplayName("Essais repris (D-110) : bornes des cas attribués et réussis, mesure non future")
    void essais() {
        assertThat(RepriseLiaisonsValidation.valider(avecEssais(new EssaisRepris(37, 109, AUJOURDHUI)), AUJOURDHUI).essais().reussis()).isEqualTo(37);
        assertThat(RepriseLiaisonsValidation.valider(avecEssais(null), AUJOURDHUI).essais()).isNull();
        assertThat(champs(avecEssais(new EssaisRepris(1, 0, null)))).containsKey("essais.attribues");
        assertThat(champs(avecEssais(new EssaisRepris(1, null, null)))).containsKey("essais.attribues");
        assertThat(champs(avecEssais(new EssaisRepris(1, 100_001, null)))).containsKey("essais.attribues");
        assertThat(champs(avecEssais(new EssaisRepris(10, 9, null)))).containsKey("essais.reussis");
        assertThat(champs(avecEssais(new EssaisRepris(null, 9, null)))).containsKey("essais.reussis");
        assertThat(champs(avecEssais(new EssaisRepris(-1, 9, null)))).containsKey("essais.reussis");
        assertThat(champs(avecEssais(new EssaisRepris(1, 9, FUTUR)))).containsKey("essais.mesureLe");
    }
}
