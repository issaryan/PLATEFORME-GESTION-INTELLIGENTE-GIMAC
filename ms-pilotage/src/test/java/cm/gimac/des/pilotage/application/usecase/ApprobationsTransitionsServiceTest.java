package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.pilotage.application.port.in.ApprobationsTransitionsUseCase.Proposition;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.CibleInterne;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.Creation;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheInterne;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase.TacheLue;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.activite.TypeOrigine;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tâche de l'approbateur d'une transition (D-98) : offre par permission, titre, lien, clôture et remise à prendre. */
class ApprobationsTransitionsServiceTest {

    private final TachesUseCase taches = mock(TachesUseCase.class);
    private final ApprobationsTransitionsService service = new ApprobationsTransitionsService(taches);

    private static TacheLue lue() {
        return new TacheLue(Tache.pourOffre("charge", null, List.of(), List.of("PAR_TRANSITION_APPROVE"), Instant.EPOCH), List.of(), Map.of(), false);
    }

    @Test
    @DisplayName("Proposition d'adhésion : offre DECISION du service participant, à prendre par PAR_TRANSITION_APPROVE, sans profil, lien vers les transitions")
    void proposee() {
        when(taches.creerPourService(eq("participant-service"), any())).thenReturn(new Creation(lue(), true));
        boolean creee = service.proposee(
            new Proposition("i-1", "t-1", "CANDIDAT", "ADHERENT", "Dossier complet", "charge", 1, "Banque B", "/des/institutions/i-1?onglet=transitions")
        );
        assertThat(creee).isTrue();
        ArgumentCaptor<TacheInterne> t = ArgumentCaptor.forClass(TacheInterne.class);
        verify(taches).creerPourService(eq("participant-service"), t.capture());
        TacheInterne d = t.getValue();
        assertThat(d.titre()).isEqualTo("Prononcer l'adhésion : Banque B");
        assertThat(d.description()).contains("CANDIDAT vers ADHERENT", "Motif : Dossier complet.", "onglet Transitions").doesNotContain("Deux approbations");
        assertThat(d.priorite()).isEqualTo("HAUTE");
        assertThat(d.responsableId()).isNull();
        assertThat(d.createurId()).isEqualTo("charge");
        assertThat(d.profilsRequis()).isNull();
        assertThat(d.uniteCible()).isNull();
        assertThat(d.permissionsRequises()).containsExactly("PAR_TRANSITION_APPROVE");
        assertThat(d.origine()).isEqualTo(new TachesUseCase.Origine("DECISION", "i-1/TRANSITION/t-1", "participant", "/des/institutions/i-1?onglet=transitions"));
    }

    @Test
    @DisplayName("Autre transition, deux approbations, sans nom ni motif ; lien absent ou étranger : onglet des transitions")
    void autreTransition() {
        when(taches.creerPourService(any(), any())).thenReturn(new Creation(lue(), false));
        assertThat(service.proposee(new Proposition("i-2", "t-2", "SUSPENDU", "ACTIF", " ", "charge", 2, null, "https://ailleurs.example/x"))).isFalse();
        ArgumentCaptor<TacheInterne> t = ArgumentCaptor.forClass(TacheInterne.class);
        verify(taches).creerPourService(any(), t.capture());
        assertThat(t.getValue().titre()).isEqualTo("Statuer sur la transition SUSPENDU vers ACTIF : i-2");
        assertThat(t.getValue().description()).contains("Deux approbations").doesNotContain("Motif");
        assertThat(t.getValue().origine().lien()).isEqualTo("/des/institutions/i-2?onglet=transitions");
        assertThat(ApprobationsTransitionsService.lien(null, "i-3")).isEqualTo("/des/institutions/i-3?onglet=transitions");
        assertThat(ApprobationsTransitionsService.lien("//ailleurs", "i-3")).isEqualTo("/des/institutions/i-3?onglet=transitions");
        assertThat(ApprobationsTransitionsService.lien("/des/a b", "i-3")).isEqualTo("/des/institutions/i-3?onglet=transitions");
        assertThat(ApprobationsTransitionsService.lien("/des/" + "x".repeat(600), "i-3")).isEqualTo("/des/institutions/i-3?onglet=transitions");
        String long_ = ApprobationsTransitionsService.titre(new Proposition("i", "t", "A", "B", null, null, 1, "N".repeat(300), null));
        assertThat(long_).hasSize(200).endsWith("…");
        assertThat(ApprobationsTransitionsService.description(new Proposition("i", "t", "A", "B", "M".repeat(3000), null, 1, null, null))).hasSize(2000);
    }

    @Test
    @DisplayName("Approbation définitive et refus : offre terminée ; première approbation : offre remise à prendre")
    void clotureEtRemise() {
        when(taches.terminerPourService(eq("participant-service"), any())).thenReturn(List.of(lue()));
        when(taches.rouvrirPourService(eq("participant-service"), any())).thenReturn(List.of());
        assertThat(service.approuvee("i-1", "t-1", true)).isEqualTo(1);
        assertThat(service.rejetee("i-1", "t-2")).isEqualTo(1);
        assertThat(service.approuvee("i-1", "t-3", false)).isZero();
        ArgumentCaptor<CibleInterne> c = ArgumentCaptor.forClass(CibleInterne.class);
        verify(taches, org.mockito.Mockito.times(2)).terminerPourService(eq("participant-service"), c.capture());
        assertThat(c.getAllValues()).extracting(x -> x.origine().reference()).containsExactly("i-1/TRANSITION/t-1", "i-1/TRANSITION/t-2");
        assertThat(c.getAllValues()).allMatch(CibleInterne::aPrendre).allMatch(x -> x.responsableId() == null && TypeOrigine.DECISION.name().equals(x.origine().type()));
        ArgumentCaptor<CibleInterne> r = ArgumentCaptor.forClass(CibleInterne.class);
        verify(taches).rouvrirPourService(eq("participant-service"), r.capture());
        assertThat(r.getValue().aPrendre()).isTrue();
        assertThat(r.getValue().origine().reference()).isEqualTo("i-1/TRANSITION/t-3");
    }

    @Test
    @DisplayName("Notification d'une offre : lien de l'origine pour une décision, aucun sinon")
    void lienNotification() {
        Tache decision = Tache.pourOffre("charge", null, List.of(), List.of("PAR_TRANSITION_APPROVE"), Instant.EPOCH);
        decision.provenirDe(TypeOrigine.DECISION, "i-1/TRANSITION/t-1", "participant", "/des/institutions/i-1?onglet=transitions");
        assertThat(TacheService.lienNotification(decision)).isEqualTo("/des/institutions/i-1?onglet=transitions");
        Tache operation = Tache.pourOffre("homologation", null, List.of(), List.of("CER_DOSSIER_VERIFY"), Instant.EPOCH);
        operation.provenirDe(TypeOrigine.OPERATION, "LIA-1/AFFECTER/SNA", "homologation", "/des/parcours/LIA-1");
        assertThat(TacheService.lienNotification(operation)).isNull();
    }
}
