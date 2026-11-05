package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.SUPER_ADMIN;
import static cm.gimac.des.pilotage.web.rest.processus.ProcessusChangementIT.lancement;
import static cm.gimac.des.pilotage.web.rest.processus.ProcessusChangementIT.url;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import cm.gimac.des.pilotage.infrastructure.config.DemandeChangementConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;

/**
 * Migration du modèle DEMANDE_CHANGEMENT au démarrage (D-131), sur PostgreSQL réel : la version publiée au gabarit daté
 * et à l'agent SSI, en recette depuis le 28/10, est remplacée par une nouvelle version ; une instance ouverte sur
 * l'ancienne version la garde (référence, étapes, signataire) jusqu'à sa clôture et reste exportable en PDF ; seules
 * les nouvelles instances prennent la nouvelle version.
 */
class ProcessusChangementMigrationIT extends AbstractProcessusIT {
    private static final String AGENT_SSI = "00000000-0000-4000-8000-0000000ss002";
    private static final String RSSI = "00000000-0000-4000-8000-0000000ss001";

    @Autowired
    DemandeChangementConfiguration provisionnement;

    @BeforeEach
    void securiteEtBaseVide() {
        organisation.enregistrer(new Compte(RSSI, "Sara SSI", "RSSI", "SSI", null, true, true));
        organisation.attribuerProfils(RSSI, "ROLE_RSSI");
        organisation.enregistrer(new Compte(AGENT_SSI, "Alain SSI", "Agent SSI", "SSI", null, true, true));
        organisation.attribuerProfils(AGENT_SSI, "ROLE_AGENT_SSI");
        organisation.attribuerProfils(AGENT, "ROLE_PRODUCTION_MONETIQUE");
        // Le modèle provisionné au démarrage du contexte est retiré : la base imite la recette du 28/10.
        jdbc.execute((ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute("truncate table processus_historique, processus_etape_instance, processus_instance, processus cascade");
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
    }

    /** Modèle système tel que publié du 28/10 au 04/11/2026 : gabarit daté, agent SSI à l'étape SSI. */
    private ObjectNode modeleDate() throws Exception {
        ObjectNode d = json.createObjectNode();
        d.put("code", DemandeChangementConfiguration.CODE);
        d.put("unite", DemandeChangementConfiguration.UNITE);
        d.put("libelle", "Demande de changement");
        d.put("description", "Fiche RFC");
        d.put("categorie", "Exploitation");
        d.put("delaiCibleJours", 10);
        d.put("referenceModele", "CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}");
        d.set("etapes", json.readTree(json.writeValueAsString(DemandeChangementConfiguration.etapes()).replace("\"ROLE_RSSI\"", "\"ROLE_AGENT_SSI\"")));
        d.set("formulaire", json.valueToTree(DemandeChangementConfiguration.formulaire()));
        d.putObject("initiateurs").putArray("regles").add("TOUT_LE_GIMAC");
        ((ObjectNode) d.at("/initiateurs")).put("initiateurIntervenant", true);
        return d;
    }

    private static Map<String, Object> champsSsi() {
        Map<String, Object> champs = new HashMap<>();
        champs.put("DATE_DEPLOIEMENT", "2026-11-02");
        champs.put("DATE_REVUE", "2026-11-03");
        champs.put("EVALUATION", "Risque maîtrisé");
        champs.put("DECISION", "ACCEPTEE");
        champs.put("DESCRIPTION", "Favorable");
        return champs;
    }

    private void jusquASsi(String id) throws Exception {
        for (String etape : List.of("IDENTIFICATION", "RAISON", "IMPACT")) {
            mvc.perform(post(url(id, etape, "fin")).with(agent(AGENT))).andExpect(status().isOk());
        }
        String document = deposer(id, AGENT, "dossier.pdf", "application/pdf");
        mvc.perform(json(post(url(id, "IMPLEMENTATION", "livrables")).with(agent(AGENT)), Map.of("livrable", "DOSSIER_CHANGEMENT", "documentId", document))).andExpect(status().isOk());
        mvc.perform(post(url(id, "IMPLEMENTATION", "fin")).with(agent(AGENT))).andExpect(status().isOk());
    }

    private void signer(String id, String signataire) throws Exception {
        mvc.perform(json(put(url(id, "SSI", "champs")).with(agent(signataire)), Map.of("valeurs", champsSsi()))).andExpect(status().isOk());
        mvc.perform(post(url(id, "SSI", "fin")).with(agent(signataire))).andExpect(status().isOk());
        mvc.perform(post(url(id, "SSI", "signature")).with(agent(signataire)).header("X-Reauth-Assertion", "assertion").contentType(MediaType.APPLICATION_JSON).content("{\"signature\":\"DECISION_SSI\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("CLOSE"));
    }

    @Test
    void instanceOuverteGardeSaVersionNouvellesInstancesSurLaSuivante() throws Exception {
        String v1 = creer(administration(SUPER_ADMIN), modeleDate());
        publier(administration(SUPER_ADMIN), v1);
        JsonNode ouverte = lire(mvc.perform(json(post(BASE + "/" + v1 + "/instances").with(agent(AGENT)), lancement())).andExpect(status().isCreated()).andReturn());
        String ancienne = ouverte.get("id").asText();
        String reference = ouverte.get("reference").asText();
        assertThat(reference).matches("CHG-\\d{4}-\\d{2}-\\d{2}-\\d{6}");
        jusquASsi(ancienne);

        provisionnement.run(null);

        // Nouvelle version publiée, l'ancienne retirée, jamais réécrite.
        assertThat(jdbc.queryForList("select version, etat, reference_modele from processus where code = ? order by version", DemandeChangementConfiguration.CODE))
            .extracting(l -> List.of(l.get("version"), l.get("etat"), l.get("reference_modele")))
            .containsExactly(List.of(1, "RETIREE", "CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}"), List.of(2, "PUBLIEE", DemandeChangementConfiguration.REFERENCE));
        String v2 = jdbc.queryForObject("select cast(id as varchar) from processus where code = ? and version = 2", String.class, DemandeChangementConfiguration.CODE);
        assertThat(jdbc.queryForObject("select cast(processus_id as varchar) from processus_instance where cast(id as varchar) = ?", String.class, ancienne)).isEqualTo(v1);

        // L'instance ouverte poursuit sur la version 1 : référence inchangée, l'agent SSI d'origine signe, le RSSI non.
        mvc.perform(get(INSTANCES + "/" + ancienne).with(agent(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.reference").value(reference));
        mvc.perform(json(put(url(ancienne, "SSI", "champs")).with(agent(RSSI)), Map.of("valeurs", champsSsi()))).andExpect(status().isForbidden());
        signer(ancienne, AGENT_SSI);
        mvc.perform(get(INSTANCES + "/" + ancienne + "/pdf").with(agent(AGENT)))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(reference + ".pdf")));

        // Nouvelle instance : version 2, format de la fiche, RSSI signataire ; l'ancienne version ne se lance plus.
        mvc.perform(json(post(BASE + "/" + v1 + "/instances").with(agent(AGENT)), lancement())).andExpect(status().is4xxClientError());
        JsonNode nouvelle = lire(mvc.perform(json(post(BASE + "/" + v2 + "/instances").with(agent(AGENT)), lancement())).andExpect(status().isCreated()).andReturn());
        assertThat(nouvelle.get("reference").asText()).matches("CHG-\\d{4}-\\d{1,6}");
        jusquASsi(nouvelle.get("id").asText());
        mvc.perform(json(put(url(nouvelle.get("id").asText(), "SSI", "champs")).with(agent(AGENT_SSI)), Map.of("valeurs", champsSsi()))).andExpect(status().isForbidden());
        signer(nouvelle.get("id").asText(), RSSI);

        // Démarrage suivant : rien à remplacer, aucune troisième version.
        provisionnement.run(null);
        assertThat(jdbc.queryForObject("select count(*) from processus where code = ?", Integer.class, DemandeChangementConfiguration.CODE)).isEqualTo(2);
    }
}
