package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Routes de DES-12 appelées par l'interface (docs/lot1/correspondance.md) : arbre des versions de tous les
 * types, fiche d'une version avec la version en vigueur, contrôle à blanc restitué pour l'écran, chronologie,
 * liaisons rattachées à une version, simulation et migration des liaisons (HOM-03).
 */
class ReferentialDes12IT extends AbstractHomologationIT {

    private static final UUID INGENIEUR = UUID.fromString("00000000-0000-0000-0000-00000000e012");
    private static final UUID CHEF_DES = UUID.fromString("00000000-0000-0000-0000-00000000c012");

    @Autowired
    ParticipantStubAdapter registre;

    @Autowired
    ManageReferentialUseCase referentiels;

    private UUID adherent(String nom) {
        UUID id = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(id, nom, "ADHERENT", "ETABLISSEMENT_PAIEMENT", List.of()));
        return id;
    }

    private String ouvrir(UUID institution) throws Exception {
        String corps = mvc
            .perform(
                post("/api/v1/liaisons")
                    .with(utilisateur(CHEF_DES, "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CadrageLiaisonIT.ouverture(institution, TypeProjet.CARTE_INTERBANCAIRE, CollaboratorStubAdapter.CONDUCTEUR_1))
            )
            .andExpect(status().isCreated())
            // Sans service souscrit connu du registre : catalogue du type de projet (règle du simulateur).
            .andExpect(jsonPath("$.perimetre").value(contains("COMMERCE_ELECTRONIQUE", "PAIEMENT", "RETRAIT", "TRANSFERT", "TRANSVERSE")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(corps, "$.id");
    }

    @Test
    @DisplayName("DES-12 : arbre de tous les types, fiche, contrôle restitué et chronologie d'une version publiée")
    void arbreFicheControleChronologie() throws Exception {
        UUID v1 = fixtures.referentielPublie(TypeProjet.CARTE_INTERBANCAIRE);
        UUID brouillon = fixtures.brouillonComplet(TypeProjet.DELEGATAIRE);

        mvc
            .perform(get("/api/v1/referentiels").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(hasItem(v1.toString())))
            .andExpect(jsonPath("$[*].id").value(hasItem(brouillon.toString())));
        mvc
            .perform(get("/api/v1/referentiels").param("type", "DELEGATAIRE").with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mvc
            .perform(get("/api/v1/referentiels/{id}", v1).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("PUBLIEE"))
            .andExpect(jsonPath("$.versionActiveId").value(v1.toString()))
            .andExpect(jsonPath("$.actionsPermises").value(contains("DERIVER", "MIGRER")));
        mvc
            .perform(get("/api/v1/referentiels/{id}", brouillon).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(jsonPath("$.versionActiveId").doesNotExist());
        mvc
            .perform(get("/api/v1/referentiels/{id}", UUID.randomUUID()).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isNotFound());

        mvc
            .perform(post("/api/v1/referentiels/{id}/controle", brouillon).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acyclique").value(true))
            .andExpect(jsonPath("$.ordreTopologique.length()").value(3))
            .andExpect(jsonPath("$.cycle").isEmpty())
            .andExpect(jsonPath("$.incoherences").isEmpty())
            .andExpect(jsonPath("$.manquants").isEmpty())
            .andExpect(jsonPath("$.conforme").value(true));

        UUID incomplet = fixtures.brouillonIncomplet(TypeProjet.PAIEMENT_MOBILE, "PM-X-01");
        mvc
            .perform(post("/api/v1/referentiels/{id}/controle", incomplet).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")))
            .andExpect(jsonPath("$.manquants[0].code").value("PM-X-01"))
            .andExpect(jsonPath("$.manquants[0].attributs").value(hasItem("partie")))
            .andExpect(jsonPath("$.conforme").value(false));

        mvc
            .perform(get("/api/v1/referentiels/{id}/historique", v1).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].event").value("Version publiée sous le numéro 1"))
            .andExpect(jsonPath("$[1].event").value("Version créée"));
    }

    @Test
    @DisplayName("HOM-03 : liaisons de l'ancienne version comptées, simulées puis migrées vers la nouvelle version en vigueur")
    void simulationEtMigration() throws Exception {
        UUID v1 = fixtures.referentielPublie(TypeProjet.CARTE_INTERBANCAIRE);
        String liaison = ouvrir(adherent("Établissement à migrer"));

        mvc
            .perform(get("/api/v1/liaisons").param("versionId", v1.toString()).param("actives", "true").param("taille", "1").with(utilisateur(CHEF_DES, "HOM_LIAISON_LIST")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1));

        UUID v2 = referentiels.creerBrouillon(TypeProjet.CARTE_INTERBANCAIRE, "Version 2", v1).getId();
        mvc.perform(post("/api/v1/referentiels/{id}/publication", v2).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_PUBLISH"))).andExpect(status().isOk());

        mvc
            .perform(post("/api/v1/referentiels/{id}/migrations/simulation", v2).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_EDIT")))
            .andExpect(status().isForbidden());
        mvc
            .perform(post("/api/v1/referentiels/{id}/migrations/simulation", v2).with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].liaisonId").value(liaison))
            .andExpect(jsonPath("$[0].conservees").value(3))
            .andExpect(jsonPath("$[0].ajoutees").value(0))
            .andExpect(jsonPath("$[0].exclue").value(false));
        mvc
            .perform(
                post("/api/v1/referentiels/{id}/migrations", v2)
                    .with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"liaisonsRetenues\":[]}")
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("HOM_MIGRATION_EMPTY"));
        mvc
            .perform(
                post("/api/v1/referentiels/{id}/migrations", v2)
                    .with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"liaisonsRetenues\":[\"" + UUID.randomUUID() + "\"]}")
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_MIGRATION_LIAISON_INELIGIBLE"));
        mvc
            .perform(
                post("/api/v1/referentiels/{id}/migrations", v2)
                    .with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"liaisonsRetenues\":[\"" + liaison + "\"]}")
            )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.etat").value("APPROUVEE"))
            .andExpect(jsonPath("$.versionSourceId").value(v1.toString()))
            .andExpect(jsonPath("$.liaisonsRetenues[0]").value(liaison));

        assertThat(jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison)).isEqualTo(v2);
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where event_type = 'homologation.referential-migrated.v1'", Integer.class)).isEqualTo(1);
        mvc
            .perform(get("/api/v1/referentiels/{id}/historique", v2).with(utilisateur(INGENIEUR, "HOM_REFERENTIEL_READ")))
            .andExpect(jsonPath("$[*].event").value(hasItem("Liaisons migrées vers cette version")));
        // L'ancienne version, retirée, ne reçoit plus de migration.
        mvc
            .perform(post("/api/v1/referentiels/{id}/migrations/simulation", v1).with(utilisateur(CHEF_DES, "HOM_REFERENTIEL_MIGRATE")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOM_MIGRATION_TARGET_NOT_PUBLISHED"));
    }
}
