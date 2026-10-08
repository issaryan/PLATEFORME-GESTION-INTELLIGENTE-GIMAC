package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.PilotageTachesStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.taches.RelaisTachesPilotage;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
        // D-93 §1 : la DDRM n'est plus tenue d'office par le premier titulaire de l'annuaire ; le conducteur reçoit
        // « Affecter les intervenants » et « Affecter <fonction> » pour la fonction attendue par le rang 3.
        assertThat(pilotage.journal()).contains("CREER " + ref + "/AFFECTATION|" + conducteur, "CREER " + ref + "/AFFECTER/DDRM|" + conducteur);
        assertThat(pilotage.journal()).noneMatch(x -> x.startsWith("CREER " + ref + "/3/R|"));
        assertThat(pilotage.taches().get(ref + "/AFFECTER/DDRM|" + conducteur).tache().titre()).startsWith("Affecter Développement du Réseau et du Marché");
        mvc
            .perform(put("/api/v1/liaisons/" + liaison + "/fonctions/DDRM").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"personneId\":\"" + DDRM + "\",\"motif\":\"m\"}"))
            .andExpect(status().isNoContent());
        relais.relayer();
        assertThat(pilotage.taches().get(ref + "/AFFECTER/DDRM|" + conducteur).etat()).isEqualTo("TERMINEE");
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
    @DisplayName("D-93 : renvoi par le validateur (tâche de R rouverte), départ d'un compte (rôles à affecter, tâche au conducteur), événements")
    void renvoiEtDepart() throws Exception {
        UUID ep = adherent("Établissement Renvoi", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        String ref = reference(liaison);
        String conducteur = CollaboratorStubAdapter.CONDUCTEUR_1.toString();
        mvc
            .perform(put("/api/v1/liaisons/" + liaison + "/fonctions/DDRM").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"personneId\":\"" + DDRM + "\",\"motif\":\"m\"}"))
            .andExpect(status().isNoContent());
        String o3 = opId(liaison, "3");
        // RACI imposé : un tiers sans qualité ne dépose pas ; la DDRM désignée dépose.
        String preuve = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"p.pdf\"}";
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/parcours/operations/" + o3 + "/preuves").with(utilisateur(VALIDATEUR, avec(COMMUN, "ROLE_CHEF_DES", "HOM_EVIDENCE_SUBMIT"))).contentType(MediaType.APPLICATION_JSON).content(preuve))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_DEPOT_NON_AUTORISE"));
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/parcours/operations/" + o3 + "/preuves").with(utilisateur(DDRM, avec(COMMUN, "ROLE_CHARGE_DDRM", "HOM_EVIDENCE_SUBMIT"))).contentType(MediaType.APPLICATION_JSON).content(preuve))
            .andExpect(status().isNoContent());
        // Seul le V désigné (le conducteur) valide ou renvoie.
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/parcours/operations/" + o3 + "/renvoi").with(validateur()).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"x\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("HOM_VALIDATION_NON_VALIDATEUR"));
        assertThat(JsonPath.<List<String>>read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations?etape=CADRAGE", conducteur()), "$[0].actionsPermises")).contains("VALIDER", "RENVOYER");
        mvc
            .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/parcours/operations/" + o3 + "/renvoi").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Pièce illisible\"}"))
            .andExpect(status().isNoContent());
        assertThat(etat(liaison, "3")).isEqualTo("A_FAIRE");
        assertThat(JsonPath.<String>read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations?etape=CADRAGE", conducteur()), "$[0].renvois[0].motif")).isEqualTo("Pièce illisible");
        relais.relayer();
        assertThat(pilotage.journal()).contains("ROUVRIR " + ref + "/3/R|" + DDRM, "CLOTURER " + ref + "/3/V|" + conducteur);
        List<String> types = jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by created_at", String.class, liaison);
        assertThat(types).contains("homologation.operation-activated.v1", "homologation.operation-validation-requested.v1");
        String renvoi = jdbc.queryForObject(
            "select cast(payload as text) from outbox_event where aggregate_id = ? and event_type = 'homologation.operation-activated.v1' and cast(payload as text) like '%RENVOI%'",
            String.class,
            liaison
        );
        assertThat(JsonPath.<String>read(renvoi, "$.motifRenvoi")).isEqualTo("Pièce illisible");
        assertThat(JsonPath.<String>read(renvoi, "$.typeProjet")).isEqualTo("CARTE_INTERBANCAIRE");

        // Départ de la chargée DDRM (iam.account-administered.v1, DESACTIVATION) : rôles à affecter, tâche au conducteur.
        comptes.recevoir(
            "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"iam.account-administered.v1\",\"data\":{\"action\":\"DESACTIVATION\",\"compteId\":\"" + DDRM + "\"}}"
        );
        assertThat(JsonPath.<String>read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations?etape=CADRAGE", conducteur()), "$[0].raci.R.id")).isEqualTo("A_AFFECTER:DDRM");
        relais.relayer();
        assertThat(pilotage.taches().get(ref + "/3/R|" + DDRM).etat()).isEqualTo("TERMINEE");
        assertThat(pilotage.taches().get(ref + "/AFFECTER/DDRM|" + conducteur).etat()).isNotEqualTo("TERMINEE");
    }

    @Autowired
    cm.gimac.des.homologation.infrastructure.messaging.consumer.ComptesIamConsumer comptes;

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
