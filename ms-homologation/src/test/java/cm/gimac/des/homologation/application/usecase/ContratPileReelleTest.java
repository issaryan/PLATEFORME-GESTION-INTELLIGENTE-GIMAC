package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.NoeudReferentiel;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrats révélés par le parcours réel par la passerelle (01/10/2026) : finalité de la réauthentification
 * demandée par l'interface, périmètre d'une liaison sans service souscrit, jeu d'initialisation des référentiels.
 */
class ContratPileReelleTest {

    @Test
    @DisplayName("Validation d'une opération : finalité OPERATION_VALIDATE, celle que demande l'interface (SignPurpose du socle)")
    void finaliteDeLaValidation() {
        assertThat(ParcoursCommandes.FINALITE_VALIDATION).isEqualTo("OPERATION_VALIDATE");
    }

    @Test
    @DisplayName("Périmètre : services souscrits s'il y en a, sinon catalogue du type ; TRANSVERSE toujours en tête")
    void perimetreParDefaut() {
        InstitutionView sans = new InstitutionView(null, "EP", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of());
        assertThat(OpenLiaisonService.perimetre(TypeProjet.PAIEMENT_MOBILE, sans)).containsExactly("TRANSVERSE", "PAIEMENT_MARCHAND", "REMITTANCE", "FACTURE", "CASH_IN");
        InstitutionView avec = new InstitutionView(null, "EP", "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of(new InstitutionView.ServiceSouscrit("RETRAIT", "2026-09-01"), new InstitutionView.ServiceSouscrit("TRANSVERSE", null)));
        assertThat(OpenLiaisonService.perimetre(TypeProjet.CARTE_INTERBANCAIRE, avec)).containsExactly("TRANSVERSE", "RETRAIT");
    }

    private static final Pattern OPERATION = Pattern.compile(
        "INSERT INTO reference_operation \\(.*?\\) VALUES \\('[^']+'::uuid, '([^']+)', '((?:[^']|'')*)', '([A-Z_]+)', '([A-Z_]+)', '([A-Z]+)', 'TRANSVERSE', 'AUCUNE', '(\\{[^']*\\})'::jsonb, NULL, '(\\[[^']*\\])'::jsonb, '([A-Z_]+)', NULL, '([0-9a-f-]+)'::uuid\\);"
    );

    @Test
    @DisplayName("Jeu d'initialisation (annexe A, D-59) : une version par type, complète et acyclique, publiable telle quelle")
    void jeuDInitialisationPubliable() throws IOException {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/config/liquibase/data/referentiel_cr_initialisation.sql")) {
            assertThat(in).isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, List<NoeudReferentiel>> parVersion = new HashMap<>();
        int lignes = 0;
        for (String l : sql.split("\n")) {
            if (!l.startsWith("INSERT INTO reference_operation")) continue;
            lignes++;
            Matcher m = OPERATION.matcher(l);
            assertThat(m.matches()).as(l).isTrue();
            assertThat(m.group(2)).isNotBlank();
            List<String> preds = new ArrayList<>();
            Matcher p = Pattern.compile("\"([A-Z0-9]+)\"").matcher(m.group(7));
            while (p.find()) preds.add(p.group(1));
            parVersion.computeIfAbsent(m.group(9), k -> new ArrayList<>()).add(new NoeudReferentiel(m.group(1), m.group(2), Segment.valueOf(m.group(4)), preds));
        }
        assertThat(sql).contains("'CARTE_INTERBANCAIRE'", "'PAIEMENT_MOBILE'", "'DELEGATAIRE'").doesNotContain("PUBLIEE");
        assertThat(parVersion).hasSize(3);
        assertThat(lignes).isEqualTo(45 + 51 + 69);
        ReferentialGraphValidator validateur = new ReferentialGraphValidator();
        parVersion.values().forEach(n -> assertThat(validateur.controler(n).anomalies()).isEmpty());
    }
}
