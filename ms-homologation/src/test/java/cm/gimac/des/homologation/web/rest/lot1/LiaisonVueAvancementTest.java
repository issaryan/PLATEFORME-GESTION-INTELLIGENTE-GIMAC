package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.application.usecase.LiaisonQueries;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Couverture;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.ParcoursSamples;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Vue de liste d'une liaison (D-110) : avancement global servi au DES et au portail ; conducteur à désigner sans nom. */
class LiaisonVueAvancementTest {

    private static Liaison liaison() {
        Liaison l = ParcoursSamples.liaison(TypeProjet.PAIEMENT_MOBILE);
        l.setDateOuverture(Instant.parse("2026-04-01T00:00:00Z"));
        return l;
    }

    private static Vues.LiaisonResume resume(Couverture avancement) {
        return resume(avancement, "C");
    }

    private static Vues.LiaisonResume resume(Couverture avancement, String conducteur) {
        Couverture c = Couverture.de(0, 1);
        return new Vues.LiaisonResume("id", "LIA-2026-101", "Banque", TypeProjet.PAIEMENT_MOBILE, null, StatutParcours.EN_TEST, EtapeCode.TESTS, conducteur, c, c, false, 0, null, null, avancement);
    }

    @Test
    void conducteurADesigner_aucunNom() {
        var f = new LiaisonQueries.Fiche(liaison(), "service-account-reprise-service", null, List.of(), List.of(), List.of(), resume(null, null));
        assertThat(LiaisonsCadrageResource.LiaisonVue.de(f).chefDeProjetNom()).isNull();
        assertThat(LiaisonsCadrageResource.LiaisonVue.dePortail(f).chefDeProjetNom()).isNull();
        var g = new LiaisonQueries.Fiche(liaison(), "Guilene", null, List.of(), List.of(), List.of(), resume(null, "Guilene"));
        assertThat(LiaisonsCadrageResource.LiaisonVue.de(g).chefDeProjetNom()).isEqualTo("Guilene");
        assertThat(LiaisonsCadrageResource.LiaisonVue.de(new LiaisonQueries.Fiche(liaison(), "X", null, List.of(), List.of(), List.of())).chefDeProjetNom()).isEqualTo("X");
    }

    @Test
    void avancementServiAuDesEtAuPortail() {
        var f = new LiaisonQueries.Fiche(liaison(), "C", null, List.of(), List.of(), List.of(), resume(new Couverture(47, 47, 100, true, null)));
        var v = LiaisonsCadrageResource.LiaisonVue.de(f);
        assertThat(v.avancement().taux()).isEqualTo(47.0);
        assertThat(v.avancement().calculable()).isTrue();
        assertThat(LiaisonsCadrageResource.LiaisonVue.dePortail(f).avancement()).isEqualTo(v.avancement());
    }

    @Test
    void sansParcoursOuSansAvancement_nonCalculable() {
        assertThat(LiaisonsCadrageResource.LiaisonVue.de(new LiaisonQueries.Fiche(liaison(), "C", null, List.of(), List.of(), List.of())).avancement().calculable()).isFalse();
        var f = new LiaisonQueries.Fiche(liaison(), "C", null, List.of(), List.of(), List.of(), resume(null));
        assertThat(LiaisonsCadrageResource.LiaisonVue.de(f).avancement().calculable()).isFalse();
    }
}
