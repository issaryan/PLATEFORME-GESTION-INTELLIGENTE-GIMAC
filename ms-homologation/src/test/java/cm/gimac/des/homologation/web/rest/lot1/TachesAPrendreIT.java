package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.PilotageTachesStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.consumer.ComptesIamConsumer;
import cm.gimac.des.homologation.infrastructure.messaging.consumer.TachesPilotageConsumer;
import cm.gimac.des.homologation.infrastructure.messaging.taches.RelaisTachesPilotage;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Tâches à prendre (D-93, D-94, docs/lot1/effet-reel-referentiels.md §8.8) : une fonction interne à affecter dont le
 * catalogue donne une permission est offerte dans MS-PILOTAGE aux porteurs de la permission (au lieu de la tâche
 * « Affecter » au conducteur) ; la prise (pilotage.task-taken.v1) affecte la fonction à la personne, sa tâche de R suit ;
 * le départ de la personne remet la tâche à prendre ; sans porteur, la tâche revient au conducteur.
 */
class TachesAPrendreIT extends ParcoursITSupport {

    @Autowired
    PilotageTachesStubAdapter pilotage;

    @Autowired
    RelaisTachesPilotage relais;

    @Autowired
    TachesPilotageConsumer prises;

    @Autowired
    ComptesIamConsumer comptes;

    @BeforeEach
    void init() {
        pilotage.vider();
        pilotage.indisponible(false);
        // Données : la fonction DDRM est caractérisée par une permission que compose le profil de la chargée DDRM.
        permissionsDdrm("[\"HOM_TEST_FONCTION_DDRM\"]");
        annuaire.composer("ROLE_CHARGE_DDRM", Set.of("HOM_TEST_FONCTION_DDRM"));
    }

    @AfterEach
    void retablir() {
        // Valeur de départ du catalogue (D-97).
        permissionsDdrm("[\"HOM_FUNCTION_DDRM\"]");
        annuaire.reinitialiserCompositions();
    }

    /** La réserve de connexions n'est pas en validation automatique : écriture validée explicitement. */
    private void permissionsDdrm(String json) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var st = c.prepareStatement("update referentiel_fonction set permissions = cast(? as jsonb) where code = 'DDRM'")) {
                st.setString(1, json);
                st.executeUpdate();
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
        CatalogueFonctions.invalider();
    }

    private String reference(String liaison) throws Exception {
        return JsonPath.read(lire("/api/v1/liaisons/" + liaison + "/dossier", conducteur()), "$.reference");
    }

    private static String prise(String id, PilotageTachesStubAdapter.TacheTenue offre, UUID responsable, String mode) {
        var o = offre.tache().origine();
        return (
            "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"pilotage.task-taken.v1\",\"source\":\"pilotage\",\"data\":{\"tacheId\":\"" + UUID.randomUUID() +
            "\",\"origine\":{\"type\":\"" + o.type() + "\",\"reference\":\"" + o.reference() + "\",\"service\":\"" + o.service() + "\",\"lien\":\"" + o.lien() +
            "\"},\"responsableId\":\"" + responsable + "\",\"acteurId\":\"" + responsable + "\",\"mode\":\"" + mode +
            "\",\"profilsRequis\":[],\"permissionsRequises\":[\"HOM_TEST_FONCTION_DDRM\"],\"profilRequis\":null,\"uniteCible\":null,\"titre\":\"t\",\"le\":\"2026-10-08T08:00:00Z\"}}"
        );
    }

    @Test
    @DisplayName("Fonction à affecter : tâche à prendre par permission, prise (affectation, tâche de R), idempotence, départ (remise à prendre)")
    void priseEtDepart() throws Exception {
        UUID ep = adherent("Établissement À prendre", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        String ref = reference(liaison);
        String conducteur = CollaboratorStubAdapter.CONDUCTEUR_1.toString();
        relais.relayer();
        String cle = ref + "/AFFECTER/DDRM|null";
        assertThat(pilotage.taches()).containsKey(cle).doesNotContainKey(ref + "/AFFECTER/DDRM|" + conducteur);
        PilotageTachesStubAdapter.TacheTenue offre = pilotage.taches().get(cle);
        assertThat(offre.etat()).isEqualTo("A_PRENDRE");
        assertThat(offre.tache().responsableId()).isNull();
        assertThat(offre.tache().permissionsRequises()).containsExactly("HOM_TEST_FONCTION_DDRM");
        assertThat(offre.tache().titre()).startsWith("Prendre la fonction Développement du Réseau et du Marché");
        assertThat(offre.tache().origine().lien()).startsWith("/des/parcours/" + liaison);

        // Prise par la chargée DDRM : la fonction lui est affectée, sa tâche de R arrive, l'offre est terminée.
        String evenement = UUID.randomUUID().toString();
        prises.recevoir(prise(evenement, offre, DDRM, "PRISE"));
        assertThat(JsonPath.<String>read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations?etape=CADRAGE", conducteur()), "$[0].raci.R.id")).isEqualTo(DDRM.toString());
        relais.relayer();
        assertThat(pilotage.taches().get(cle).etat()).isEqualTo("TERMINEE");
        assertThat(pilotage.taches()).containsKey(ref + "/3/R|" + DDRM);
        // Même événement rejoué : sans effet.
        prises.recevoir(prise(evenement, offre, DDRM, "PRISE"));
        assertThat(jdbc.queryForObject("select count(*) from processed_event where event_id = ?", Integer.class, evenement)).isEqualTo(1);
        // Prise devenue sans objet (fonction déjà affectée) : ignorée.
        prises.recevoir(prise(UUID.randomUUID().toString(), offre, SNA, "ATTRIBUTION"));
        assertThat(JsonPath.<String>read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations?etape=CADRAGE", conducteur()), "$[0].raci.R.id")).isEqualTo(DDRM.toString());

        // Départ de la chargée DDRM : la fonction redevient à affecter, la tâche est remise à prendre.
        comptes.recevoir(
            "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"iam.account-administered.v1\",\"data\":{\"action\":\"DESACTIVATION\",\"compteId\":\"" + DDRM + "\"}}"
        );
        relais.relayer();
        assertThat(pilotage.taches().get(cle).etat()).isEqualTo("A_PRENDRE");
        assertThat(pilotage.journal()).contains("ROUVRIR " + cle);
        assertThat(pilotage.taches()).doesNotContainKey(ref + "/AFFECTER/DDRM|" + conducteur);
    }

    @Test
    @DisplayName("Prise par une personne qui ne tient ni le profil ni la permission : ignorée ; route d'affectation admise par la permission")
    void eligibilite() throws Exception {
        UUID ep = adherent("Établissement Éligibilité", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        String ref = reference(liaison);
        relais.relayer();
        PilotageTachesStubAdapter.TacheTenue offre = pilotage.taches().get(ref + "/AFFECTER/DDRM|null");
        prises.recevoir(prise(UUID.randomUUID().toString(), offre, SNA, "PRISE"));
        assertThat(JsonPath.<String>read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations?etape=CADRAGE", conducteur()), "$[0].raci.R.id")).isEqualTo("A_AFFECTER:DDRM");
        // Le SNA reçoit la permission par composition (données de MS-IAM) : l'affectation par la route est admise.
        annuaire.composer("ROLE_SNA", Set.of("HOM_TEST_FONCTION_DDRM"));
        mvc
            .perform(put("/api/v1/liaisons/" + liaison + "/fonctions/DDRM").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"personneId\":\"" + SNA + "\",\"motif\":\"m\"}"))
            .andExpect(status().isNoContent());
        relais.relayer();
        assertThat(pilotage.taches().get(ref + "/AFFECTER/DDRM|null").etat()).isEqualTo("TERMINEE");
        assertThat(pilotage.taches()).containsKey(ref + "/3/R|" + SNA);
    }

    @Test
    @DisplayName("Valeurs livrées (D-97) : chaque fonction opérationnelle porte sa permission en base ; DDRM offerte aux porteurs de HOM_FUNCTION_DDRM")
    void valeursLivrees() throws Exception {
        permissionsDdrm("[\"HOM_FUNCTION_DDRM\"]");
        annuaire.reinitialiserCompositions();
        // Reprise Liquibase : la base porte les valeurs de départ du catalogue, pour chaque fonction reprise.
        for (CatalogueFonctions.Definition d : CatalogueFonctions.REPRISES) {
            String lu = jdbc.queryForObject("select permissions::text from referentiel_fonction where code = ?", String.class, d.code());
            assertThat(JsonPath.<List<String>>read(lu, "$")).as(d.code()).containsExactlyElementsOf(d.permissions());
        }
        assertThat(CatalogueFonctions.REPRISES)
            .filteredOn(d -> List.of("DDRM", "PRODUCTION_MONETIQUE", "POLE_RESEAU", "POLE_SYSTEME", "EXPLOITATION").contains(d.code()))
            .extracting(CatalogueFonctions.Definition::permissions)
            .containsExactly(
                List.of("HOM_FUNCTION_DDRM"),
                List.of("HOM_FUNCTION_PRODUCTION"),
                List.of("HOM_FUNCTION_NETWORK"),
                List.of("HOM_FUNCTION_SYSTEM"),
                List.of("HOM_FUNCTION_OPERATIONS")
            );
        UUID ep = adherent("Établissement Valeurs livrées", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        String ref = reference(liaison);
        relais.relayer();
        String conducteur = CollaboratorStubAdapter.CONDUCTEUR_1.toString();
        assertThat(pilotage.taches()).containsKey(ref + "/AFFECTER/DDRM|null").doesNotContainKey(ref + "/AFFECTER/DDRM|" + conducteur);
        assertThat(pilotage.taches().get(ref + "/AFFECTER/DDRM|null").tache().permissionsRequises()).containsExactly("HOM_FUNCTION_DDRM");
    }

    @Test
    @DisplayName("Sans porteur de la permission dans MS-IAM : tâche « Affecter » au conducteur, comme avant")
    void sansPorteur() throws Exception {
        annuaire.reinitialiserCompositions();
        UUID ep = adherent("Établissement Sans porteur", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        String ref = reference(liaison);
        relais.relayer();
        String conducteur = CollaboratorStubAdapter.CONDUCTEUR_1.toString();
        assertThat(pilotage.taches()).containsKey(ref + "/AFFECTER/DDRM|" + conducteur).doesNotContainKey(ref + "/AFFECTER/DDRM|null");
        mvc
            .perform(put("/api/v1/liaisons/" + liaison + "/fonctions/DDRM").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"personneId\":\"" + SNA + "\",\"motif\":\"m\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_PERSON_NOT_ELIGIBLE"));
    }
}
