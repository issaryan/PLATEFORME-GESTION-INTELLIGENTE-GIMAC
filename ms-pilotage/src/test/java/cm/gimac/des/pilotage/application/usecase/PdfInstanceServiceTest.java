package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.InstanceLue;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort.Document;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort.Fait;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.SignatureInstance;
import cm.gimac.des.pilotage.infrastructure.config.DemandeChangementConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;

/** PDF d'une instance au gabarit commun (D-139) : cartouche, données, étapes, signatures, langues. Personnes par leur rôle. */
class PdfInstanceServiceTest {
    static final Instant EDITION = Instant.parse("2026-11-06T09:30:00Z");
    private final InstancesProcessusUseCase instances = mock(InstancesProcessusUseCase.class);
    private final RenduPdfPort rendu = mock(RenduPdfPort.class);
    private final PdfInstanceService service = new PdfInstanceService(instances, rendu, Clock.fixed(EDITION, ZoneOffset.UTC));
    private final Appelant appelant = new Appelant("u-dem", "Demandeur", Set.of());

    /** Demande de changement lancée, étape SSI signée, seconde étape SSI en attente de signature, étapes sans définition. */
    InstanceProcessus demande(String raison) {
        Processus p = Processus.brouillon(DemandeChangementConfiguration.CODE, DemandeChangementConfiguration.UNITE, "test", Instant.parse("2026-10-28T08:00:00Z"));
        p.decrire("Demande de changement", "", "Exploitation", 5, DemandeChangementConfiguration.etapes(), "test", Instant.parse("2026-10-28T08:00:00Z"));
        p.definirLancement(null, DemandeChangementConfiguration.formulaire(), "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Ouverture d'un accès en lecture à la plateforme AMA", "u-dem", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        i.definirReference("CHG-2026-10-28-000001");
        i.renseignerFormulaire(Map.of("RAISON", raison, "ELEMENT_CONFIGURATION", "AMA"));
        EtapeInstance ssi = new EtapeInstance(i.getId(), "SSI", 4);
        SignatureInstance attendue = SignatureInstance.attendue(DemandeChangementConfiguration.etapes().get(4).signatures().get(0), null);
        ssi.activer(java.util.UUID.randomUUID(), null, List.of(attendue), java.time.LocalDate.parse("2026-11-02"), Instant.parse("2026-10-28T09:30:00Z"));
        ssi.preremplir(Map.of("DATE_DEPLOIEMENT", "2026-11-02", "DATE_REVUE", "2026-11-03", "EVALUATION", "Conforme à la politique d'accès", "DECISION", "ACCEPTEE", "DESCRIPTION", "Avis favorable, accès limité à la lecture."));
        ssi.soumettre(DemandeChangementConfiguration.etapes().get(4), Instant.parse("2026-10-28T09:45:00Z"));
        ssi.signer("DECISION_SSI", "u-ssi", "3f6c".repeat(16), List.of(), Instant.parse("2026-10-28T10:00:00Z"));
        EtapeInstance enAttente = new EtapeInstance(i.getId(), "SSI", 5);
        enAttente.activer(java.util.UUID.randomUUID(), null, List.of(SignatureInstance.attendue(DemandeChangementConfiguration.etapes().get(4).signatures().get(0), null)), java.time.LocalDate.parse("2026-11-02"), Instant.parse("2026-10-28T09:30:00Z"));
        EtapeInstance implementation = new EtapeInstance(i.getId(), "IMPLEMENTATION", 3);
        EtapeInstance vue = new EtapeInstance(i.getId(), "INCONNUE", 0);
        InstanceLue lue = new InstanceLue(i, p, List.of(ssi, enAttente, implementation, vue), Map.of(), List.of(), Map.of("u-dem", "Analyste de la cellule rapprochement", "u-ssi", "Responsable de la sécurité des systèmes d'information"), Map.of(), null, null);
        when(instances.instance(appelant, i.getId())).thenReturn(lue);
        when(rendu.rendre(any())).thenReturn("%PDF-test".getBytes());
        return i;
    }

    Document rendu(InstanceProcessus i, String langue) {
        PdfInstanceService.PdfInstance pdf = service.telecharger(appelant, i.getId(), langue);
        assertThat(pdf.contenu()).isEqualTo("%PDF-test".getBytes());
        ArgumentCaptor<Document> d = ArgumentCaptor.forClass(Document.class);
        verify(rendu, atLeastOnce()).rendre(d.capture());
        return d.getValue();
    }

    @Test
    void pdfDeLaDemandeEtDeSaSignature() {
        InstanceProcessus i = demande("<script>alert('x')</script> & accès");
        Document d = rendu(i, null);
        assertThat(d.titre()).isEqualTo("Demande de changement");
        assertThat(d.sousTitre()).isEqualTo("Ouverture d'un accès en lecture à la plateforme AMA");
        assertThat(d.reference()).isEqualTo("CHG-2026-10-28-000001");
        assertThat(d.classification()).isEqualTo("INTERNE");
        assertThat(d.langue()).isEqualTo("fr");
        assertThat(d.edition()).isEqualTo(EDITION);
        assertThat(d.faits()).containsExactly(
            new Fait("Référence", "CHG-2026-10-28-000001"), new Fait("État", "En cours"), new Fait("Demandeur", "Analyste de la cellule rapprochement"), new Fait("Date de la demande", "28/10/2026 10:00")
        );
        assertThat(d.corps())
            .startsWith("<h2>Données du demandeur</h2><table class=\"fiche\"><tbody>")
            .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; accès")
            .doesNotContain("<script>alert")
            .contains("<td class=\"valeur\">AMA</td>", "<td class=\"valeur\"><span class=\"muet\">—</span></td>")
            .contains("<td class=\"valeur\">02/11/2026</td>", "<td class=\"valeur\">Acceptée</td>")
            .doesNotContain("ACCEPTEE")
            .contains("Responsable de la sécurité des systèmes d&#39;information", "28/10/2026 11:00", "<div class=\"empreinte\">Empreinte : " + "3f6c".repeat(16) + "</div>")
            .contains("<span class=\"badge ok\">Signée</span>", "<span class=\"badge attente\">Attendue</span>")
            .contains("<h2>INCONNUE <span class=\"badge attente\">En attente</span></h2>");
    }

    @Test
    void langues() {
        InstanceProcessus i = demande("Consultation");
        Document en = rendu(i, "EN");
        assertThat(en.langue()).isEqualTo("en");
        assertThat(en.faits()).extracting(Fait::libelle).containsExactly("Reference", "Status", "Requester", "Request date");
        assertThat(en.faits().get(3).valeur()).isEqualTo("2026-10-28 10:00");
        assertThat(en.corps()).contains("Requester data", "Signed", "Awaited", "2026-11-02", "<td class=\"valeur\">Accepted</td>");
        Document es = rendu(i, "es");
        assertThat(es.corps()).contains("Datos del solicitante", "Firmada", "Pendiente", "<td class=\"valeur\">Aceptada</td>");
        assertThat(rendu(i, "de").langue()).isEqualTo("fr");
    }

    @Test
    void dates() {
        Map<String, String> fr = TextesPdfInstance.de("fr");
        assertThat(PdfInstanceService.date(fr, null)).isEqualTo("—");
        assertThat(PdfInstanceService.date(fr, "2026-10-28T09:00:00Z")).isEqualTo("28/10/2026 10:00");
        assertThat(PdfInstanceService.date(fr, "Conforme")).isEqualTo("Conforme");
        assertThat(TextesPdfInstance.langue(null)).isEqualTo("fr");
    }

    @Test
    void pdfSansReferenceNiDefinition() {
        Processus p = Processus.brouillon("AUTRE", "SSI", "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Autre", "u", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        InstanceLue lue = new InstanceLue(i, null, List.of(), Map.of(), List.of(), Map.of(), Map.of(), null, null);
        when(instances.instance(appelant, i.getId())).thenReturn(lue);
        when(rendu.rendre(any())).thenReturn("%PDF-test".getBytes());
        PdfInstanceService.PdfInstance pdf = service.telecharger(appelant, i.getId(), "fr");
        assertThat(pdf.nomFichier()).isEqualTo(i.getId() + ".pdf");
        ArgumentCaptor<Document> d = ArgumentCaptor.forClass(Document.class);
        verify(rendu).rendre(d.capture());
        assertThat(d.getValue().corps()).contains("<p class=\"muet\">Aucune donnée saisie.</p>");
        assertThat(d.getValue().faits().get(2).valeur()).isEqualTo("u");
    }

    @Test
    void pdfAvecDefinitionMaisSansReference() {
        Processus p = Processus.brouillon("AUTRE", "SSI", "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Autre", "u", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        InstanceLue lue = new InstanceLue(i, p, List.of(), Map.of(), List.of(), Map.of(), Map.of(), null, null);
        when(instances.instance(appelant, i.getId())).thenReturn(lue);
        when(rendu.rendre(any())).thenReturn("%PDF-test".getBytes());
        PdfInstanceService.PdfInstance pdf = service.telecharger(appelant, i.getId(), null);
        assertThat(pdf.nomFichier()).isEqualTo(i.getId() + ".pdf");
    }

    /**
     * Écrit la requête exacte de la route interne pour la demande de changement (fr, en, es) dans
     * GIMAC_PDF_ESSAI_DOSSIER, imprimée ensuite par ConvertisseurHtmlGotenbergReelTest de MS-DOCUMENT.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "GIMAC_PDF_ESSAI_DOSSIER", matches = ".+")
    void ecrireEssai() throws Exception {
        Path dossier = Path.of(System.getenv("GIMAC_PDF_ESSAI_DOSSIER"));
        Files.createDirectories(dossier);
        InstanceProcessus i = demande("Consultation des journaux de compensation par l'équipe de rapprochement.");
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        for (String l : List.of("fr", "en", "es")) {
            Object requete = cm.gimac.des.pilotage.infrastructure.client.RenduPdfEssai.requete(rendu(i, l));
            Files.writeString(dossier.resolve("instance-" + l + ".json"), json.writeValueAsString(requete));
            // Exemple des services simulés du frontend (lot1/src/assets), marqué « Démonstration ».
            var demo = json.valueToTree(requete);
            ((com.fasterxml.jackson.databind.node.ObjectNode) demo.get("gabarit")).put("filigrane", Map.of("fr", "Démonstration", "en", "Demonstration", "es", "Demostración").get(l));
            Files.writeString(dossier.resolve("instance-demonstration-" + l + ".json"), json.writeValueAsString(demo));
        }
    }
}
