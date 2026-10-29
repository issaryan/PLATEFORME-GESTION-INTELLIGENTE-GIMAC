package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase;
import cm.gimac.des.pilotage.application.port.in.InstancesProcessusUseCase.InstanceLue;
import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.domain.processus.EtapeInstance;
import cm.gimac.des.pilotage.domain.processus.InstanceProcessus;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.SignatureInstance;
import cm.gimac.des.pilotage.infrastructure.config.DemandeChangementConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PdfInstanceServiceTest {
    private final InstancesProcessusUseCase instances = mock(InstancesProcessusUseCase.class);
    private final RenduPdfPort rendu = mock(RenduPdfPort.class);
    private final PdfInstanceService service = new PdfInstanceService(instances, rendu);
    private final Appelant appelant = new Appelant("u-dem", "David Demandeur", Set.of());

    @Test
    void pdfDeLaDemandeEtDeSaSignature() {
        Processus p = Processus.brouillon(DemandeChangementConfiguration.CODE, DemandeChangementConfiguration.UNITE, "test", Instant.parse("2026-10-28T08:00:00Z"));
        p.decrire("Demande de changement", "", "Exploitation", 5, DemandeChangementConfiguration.etapes(), "test", Instant.parse("2026-10-28T08:00:00Z"));
        p.definirLancement(null, DemandeChangementConfiguration.formulaire(), "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Accès lecture AMA", "u-dem", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        i.definirReference("CHG-2026-10-28-000001");
        i.renseignerFormulaire(Map.of("RAISON", "<script>alert('x')</script> & accès", "ELEMENT_CONFIGURATION", "AMA"));
        EtapeInstance ssi = new EtapeInstance(i.getId(), "SSI", 4);
        SignatureInstance attendue = SignatureInstance.attendue(DemandeChangementConfiguration.etapes().get(4).signatures().get(0), null);
        ssi.activer(java.util.UUID.randomUUID(), null, List.of(attendue), java.time.LocalDate.parse("2026-11-02"), Instant.parse("2026-10-28T09:30:00Z"));
        ssi.preremplir(Map.of("DATE_DEPLOIEMENT", "2026-11-02", "DATE_REVUE", "2026-11-03", "EVALUATION", "Conforme", "DECISION", "ACCEPTEE", "DESCRIPTION", "Favorable"));
        ssi.soumettre(DemandeChangementConfiguration.etapes().get(4), Instant.parse("2026-10-28T09:45:00Z"));
        SignatureInstance signature = attendue.signee("u-ssi", "2026-10-28T10:00:00Z", "a".repeat(64), List.of());
        ssi.signer("DECISION_SSI", "u-ssi", signature.empreinte(), List.of(), Instant.parse("2026-10-28T10:00:00Z"));
        // Une seconde étape SSI reste non signée : aucun nom ne doit être affiché dans ce cas.
        EtapeInstance enAttente = new EtapeInstance(i.getId(), "SSI", 5);
        enAttente.activer(java.util.UUID.randomUUID(), null, List.of(SignatureInstance.attendue(DemandeChangementConfiguration.etapes().get(4).signatures().get(0), null)), java.time.LocalDate.parse("2026-11-02"), Instant.parse("2026-10-28T09:30:00Z"));
        EtapeInstance implementation = new EtapeInstance(i.getId(), "IMPLEMENTATION", 3);
        EtapeInstance vue = new EtapeInstance(i.getId(), "INCONNUE", 0);
        InstanceLue lue = new InstanceLue(i, p, List.of(ssi, enAttente, implementation, vue), Map.of(), List.of(), Map.of("u-dem", "David Demandeur", "u-ssi", "Sara SSI"), Map.of(), null, null);
        when(instances.instance(appelant, i.getId())).thenReturn(lue);
        when(rendu.rendre(anyString(), anyString())).thenReturn("%PDF-test".getBytes());
        PdfInstanceService.PdfInstance pdf = service.telecharger(appelant, i.getId());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(rendu).rendre(html.capture(), eq("Moabi · CHG-2026-10-28-000001 · Document interne"));
        assertThat(pdf.nomFichier()).isEqualTo("CHG-2026-10-28-000001.pdf");
        assertThat(pdf.contenu()).isEqualTo("%PDF-test".getBytes());
        assertThat(html.getValue()).contains("David Demandeur", "AMA", "Sara SSI", "ACCEPTEE", "a".repeat(64), "INCONNUE")
            .contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; accès")
            .doesNotContain("<script>alert");
        assertThat(signature.etat().name()).isEqualTo("SIGNEE");
    }

    @Test
    void pdfSansReferenceNiDefinition() {
        Processus p = Processus.brouillon("AUTRE", "SSI", "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Autre", "u", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        InstanceLue lue = new InstanceLue(i, null, List.of(), Map.of(), List.of(), Map.of(), Map.of(), null, null);
        when(instances.instance(appelant, i.getId())).thenReturn(lue);
        when(rendu.rendre(anyString(), anyString())).thenReturn("%PDF-test".getBytes());
        PdfInstanceService.PdfInstance pdf = service.telecharger(appelant, i.getId());
        assertThat(pdf.nomFichier()).isEqualTo(i.getId() + ".pdf");
    }

    @Test
    void pdfAvecDefinitionMaisSansReference() {
        Processus p = Processus.brouillon("AUTRE", "SSI", "test", Instant.parse("2026-10-28T08:00:00Z"));
        InstanceProcessus i = new InstanceProcessus(p, "Autre", "u", null, Map.of(), Instant.parse("2026-10-28T09:00:00Z"));
        InstanceLue lue = new InstanceLue(i, p, List.of(), Map.of(), List.of(), Map.of(), Map.of(), null, null);
        when(instances.instance(appelant, i.getId())).thenReturn(lue);
        when(rendu.rendre(anyString(), anyString())).thenReturn("%PDF-test".getBytes());
        PdfInstanceService.PdfInstance pdf = service.telecharger(appelant, i.getId());
        assertThat(pdf.nomFichier()).isEqualTo(i.getId() + ".pdf");
    }
}
