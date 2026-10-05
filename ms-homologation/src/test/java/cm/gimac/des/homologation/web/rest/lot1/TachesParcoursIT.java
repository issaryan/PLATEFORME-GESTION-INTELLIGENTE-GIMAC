package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.PilotageTachesStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.taches.RelaisTachesPilotage;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Tâches de MS-PILOTAGE nées du parcours (D-77, §5) : conducteur à l'ouverture, R à l'éligibilité, V à la
 * validation demandée, clôtures ; MS-PILOTAGE indisponible : le geste aboutit, la demande est réessayée.
 */
class TachesParcoursIT extends ParcoursITSupport {

    @Autowired
    PilotageTachesStubAdapter pilotage;

    @Autowired
    RelaisTachesPilotage relais;

    @BeforeEach
    void init() {
        pilotage.vider();
        pilotage.indisponible(false);
    }

    String reference(String liaison) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(lire("/api/v1/liaisons/" + liaison + "/dossier", conducteur()), "$.reference");
    }

    @Test
    @DisplayName("Ouverture : conducteur (affecter les intervenants) et R du rang 3 ; preuve : R terminée, V créée ; validation : V terminée")
    void cycle() throws Exception {
        UUID ep = adherent("Établissement Tâches", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        String ref = reference(liaison);
        relais.relayer();
        String conducteur = CollaboratorStubAdapter.CONDUCTEUR_1.toString();
        assertThat(pilotage.journal()).containsExactlyInAnyOrder("CREER " + ref + "/AFFECTATION|" + conducteur, "CREER " + ref + "/3/R|" + DDRM);
        var tache3 = pilotage.taches().get(ref + "/3/R|" + DDRM).tache();
        assertThat(tache3.titre()).isEqualTo("Arrêt du périmètre des services (" + ref + ")");
        assertThat(tache3.origine().type()).isEqualTo("OPERATION");
        assertThat(tache3.origine().service()).isEqualTo("homologation");
        assertThat(tache3.origine().lien()).startsWith("/des/parcours/" + liaison + "?onglet=operations&operation=");
        assertThat(tache3.echeance()).isNotNull();

        // Preuve du rang 3 : validation demandée (V, le conducteur) ; la tâche de R est terminée.
        preuve(opId(liaison, "3"), "");
        relais.relayer();
        assertThat(pilotage.journal()).contains("CLOTURER " + ref + "/3/R|" + DDRM, "CREER " + ref + "/3/V|" + conducteur);
        assertThat(pilotage.taches().get(ref + "/3/V|" + conducteur).tache().titre()).isEqualTo("Valider : Arrêt du périmètre des services (" + ref + ")");

        // MS-PILOTAGE indisponible : la validation aboutit, la demande attend et part au retour du service.
        pilotage.indisponible(true);
        valider(opId(liaison, "3"));
        relais.relayer();
        assertThat(etat(liaison, "3")).isEqualTo("FAITE");
        assertThat(jdbc.queryForObject("select count(*) from demande_tache_pilotage where statut = 'EN_ATTENTE' and tentatives = 1", Integer.class)).isPositive();
        pilotage.indisponible(false);
        jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
                try (var st = c.createStatement()) {
                    st.executeUpdate("update demande_tache_pilotage set prochain_essai = now() - interval '1 minute' where statut = 'EN_ATTENTE'");
                }
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return null;
            }
        );
        relais.relayer();
        assertThat(pilotage.taches().get(ref + "/3/V|" + conducteur).etat()).isEqualTo("TERMINEE");
        // Rangs 4 et 5 devenus éligibles : tâches à la chargée DDRM.
        assertThat(pilotage.taches()).containsKeys(ref + "/4/R|" + DDRM, ref + "/5/R|" + DDRM);

        // Affectation des ressources : la tâche du conducteur est terminée.
        mvc
            .perform(put("/api/v1/liaisons/" + liaison + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[{\"fonction\":\"SNA\",\"personneId\":\"" + SNA + "\"}]}"))
            .andExpect(status().isNoContent());
        relais.relayer();
        assertThat(pilotage.taches().get(ref + "/AFFECTATION|" + conducteur).etat()).isEqualTo("TERMINEE");
        assertThat(jdbc.queryForObject("select count(*) from demande_tache_pilotage where statut <> 'ENVOYEE'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Conducteur redésigné : sa tâche passe au nouveau conducteur")
    void redesignation() throws Exception {
        UUID ep = adherent("Établissement Redésignation", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.PAIEMENT_MOBILE);
        relais.relayer();
        mvc
            .perform(
                put("/api/v1/liaisons/" + liaison + "/chef-de-projet")
                    .with(chefDes())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"chefDeProjetId\":\"" + CollaboratorStubAdapter.CONDUCTEUR_2 + "\",\"motif\":\"Charge\"}")
            )
            .andExpect(status().is2xxSuccessful());
        relais.relayer();
        String ref = reference(liaison);
        assertThat(pilotage.taches().get(ref + "/AFFECTATION|" + CollaboratorStubAdapter.CONDUCTEUR_1).etat()).isEqualTo("TERMINEE");
        assertThat(pilotage.taches().get(ref + "/AFFECTATION|" + CollaboratorStubAdapter.CONDUCTEUR_2).etat()).isEqualTo("OUVERTE");
    }
}
