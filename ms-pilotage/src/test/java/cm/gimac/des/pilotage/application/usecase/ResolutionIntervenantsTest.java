package cm.gimac.des.pilotage.application.usecase;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_DES;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.domain.processus.Affectation;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Unite;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Résolution des intervenants (D-87 §2) et remplacement d'une personne partie, sur l'organisation simulée. */
class ResolutionIntervenantsTest {

    private static final String RESEAU = "ROLE_POLE_RESEAU";
    private OrganisationStubAdapter organisation;
    private ResolutionIntervenants resolution;

    @BeforeEach
    void preparer() {
        organisation = new OrganisationStubAdapter(true);
        resolution = new ResolutionIntervenants(new Equipes(organisation));
    }

    private static Intervenant i(ModeIntervenant mode, String profil, String personne, String remplacement) {
        return new Intervenant(mode, profil, personne, null, remplacement);
    }

    private ResolutionIntervenants.Resolution resoudre(Intervenant i, String lanceur, String n1, Map<String, String> d) {
        return resolution.resoudre(i, "POLE_RESEAU", lanceur, n1, d, "E");
    }

    @Test
    @DisplayName("Chaque mode : profil (à prendre), personne, désignation, chef d'unité, lanceur, n+1 du lanceur")
    void modes() {
        assertThat(resoudre(i(ModeIntervenant.PROFIL, RESEAU, null, null), null, null, Map.of()).affectation()).isEqualTo(Affectation.profil(RESEAU, "POLE_RESEAU"));
        assertThat(resoudre(i(ModeIntervenant.PERSONNE, null, INGENIEUR_RESEAU, RESEAU), null, null, Map.of()).affectation()).isEqualTo(Affectation.personne(INGENIEUR_RESEAU, "POLE_RESEAU"));
        var designe = resoudre(i(ModeIntervenant.AU_LANCEMENT, null, null, RESEAU), null, null, Map.of("E", TECHNICIEN_RESEAU));
        assertThat(designe.nominative()).isTrue();
        assertThat(designe.affectation().personneId()).isEqualTo(TECHNICIEN_RESEAU);
        assertThat(resoudre(i(ModeIntervenant.CHEF_UNITE, null, null, null), null, null, Map.of()).affectation().personneId()).isEqualTo(CHEF_RESEAU);
        assertThat(resoudre(i(ModeIntervenant.LANCEUR, null, null, RESEAU), INGENIEUR_RESEAU, CHEF_RESEAU, Map.of()).affectation().personneId()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(resoudre(i(ModeIntervenant.N_PLUS_1_LANCEUR, null, null, RESEAU), INGENIEUR_RESEAU, CHEF_RESEAU, Map.of()).affectation().personneId()).isEqualTo(CHEF_RESEAU);
        Intervenant ailleurs = new Intervenant(ModeIntervenant.CHEF_UNITE, null, null, "DES", null);
        assertThat(resolution.resoudre(ailleurs, "POLE_RESEAU", null, null, Map.of(), "E").affectation()).isEqualTo(Affectation.personne(CHEF_DES, "DES"));
    }

    @Test
    @DisplayName("Remplacement : personne partie, absente, unité sans chef → à prendre par le profil de remplacement (à défaut : l'unité)")
    void remplacement() {
        organisation.enregistrer(new Compte(INGENIEUR_RESEAU, "Ingrid Mballa", "Ingénieure réseau", "POLE_RESEAU", CHEF_RESEAU, false, true));
        var partie = resoudre(i(ModeIntervenant.PERSONNE, null, INGENIEUR_RESEAU, RESEAU), null, null, Map.of());
        assertThat(partie.affectation()).isEqualTo(Affectation.profil(RESEAU, "POLE_RESEAU"));
        assertThat(partie.parti()).isEqualTo(INGENIEUR_RESEAU);
        assertThat(partie.note()).contains("partie");
        var sansN1 = resoudre(i(ModeIntervenant.N_PLUS_1_LANCEUR, null, null, RESEAU), TECHNICIEN_RESEAU, null, Map.of());
        assertThat(sansN1.parti()).isNull();
        assertThat(sansN1.note()).contains("absent");
        organisation.enregistrerUnite(new Unite("POLE_RESEAU", "PROD_INFO", null));
        var sansChef = resoudre(i(ModeIntervenant.CHEF_UNITE, null, null, null), null, null, Map.of());
        assertThat(sansChef.affectation()).isEqualTo(new Affectation(null, null, "POLE_RESEAU"));
        assertThat(sansChef.note()).contains("tout membre");
    }

    @Test
    @DisplayName("Destinataires et appartenance : personne, porteurs actifs du profil, à défaut le chef ; contrôle d'une affectation")
    void destinatairesEtAffectation() {
        assertThat(resolution.destinataires(Affectation.profil(RESEAU, "POLE_RESEAU"), null)).containsExactlyInAnyOrder(INGENIEUR_RESEAU, TECHNICIEN_RESEAU);
        assertThat(resolution.destinataires(Affectation.profil(RESEAU, "POLE_RESEAU"), INGENIEUR_RESEAU)).containsExactly(TECHNICIEN_RESEAU);
        assertThat(resolution.destinataires(Affectation.profil("ROLE_POLE_SYSTEME", "POLE_RESEAU"), null)).containsExactly(CHEF_RESEAU);
        assertThat(resolution.destinataires(Affectation.personne(CHEF_RESEAU, "POLE_RESEAU"), CHEF_RESEAU)).isEmpty();
        assertThat(resolution.destinataires(null, null)).isEmpty();
        assertThat(resolution.affecte(TECHNICIEN_RESEAU, Affectation.profil(RESEAU, "POLE_RESEAU"))).isTrue();
        assertThat(resolution.affecte(CHEF_RESEAU, Affectation.profil(RESEAU, "POLE_RESEAU"))).isFalse();
        assertThat(resolution.affecte(CHEF_RESEAU, new Affectation(null, null, "POLE_RESEAU"))).isTrue();
        assertThat(resolution.affecte(CHEF_RESEAU, Affectation.personne(CHEF_RESEAU, "POLE_RESEAU"))).isTrue();
        assertThat(resolution.affecte(null, Affectation.personne(CHEF_RESEAU, "POLE_RESEAU"))).isFalse();
        assertThat(ResolutionIntervenants.cle("E", "intervenant")).isEqualTo("E");
        assertThat(ResolutionIntervenants.cle("E", "signature.S")).isEqualTo("E.signature.S");
    }
}
