package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.web.rest.processus.ProcessusChangementIT.lancement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.infrastructure.config.DemandeChangementConfiguration;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
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

    @Autowired
    ProcessusRepository processus;

    /**
     * Modèle système tel que publié du 28/10 au 04/11/2026 (gabarit daté, agent SSI à l'étape SSI), écrit par le domaine :
     * l'API génère désormais le code d'un processus et ne permet plus d'en choisir un.
     */
    private String modeleDate() {
        Instant le = Instant.parse("2026-10-28T08:00:00Z");
        Intervenant agent = new Intervenant(ModeIntervenant.PROFIL, "ROLE_AGENT_SSI", null, DemandeChangementConfiguration.UNITE, null);
        List<Etape> etapes = DemandeChangementConfiguration.etapes().stream().map(e -> !"SSI".equals(e.code()) ? e : new Etape(
            e.code(), e.libelle(), e.action(), agent, e.validateur(), e.predecesseurs(), e.delaiJours(), e.livrables(),
            e.signatures().stream().map(x -> new SignatureRequise(x.code(), x.libelle(), agent, x.porte(), x.ordre())).toList(),
            e.champs(), e.position(), e.delaiMaxJours())).toList();
        Processus p = Processus.brouillon(DemandeChangementConfiguration.CODE, DemandeChangementConfiguration.UNITE, "systeme", le);
        p.decrire("Demande de changement", "Fiche RFC", "Exploitation", 10, etapes, "systeme", le);
        p.definirLancement(new Initiateurs(List.of(RegleInitiateur.TOUT_LE_GIMAC), List.of(), List.of(), List.of(), List.of(), true), DemandeChangementConfiguration.formulaire(), "systeme", le);
        p.definirReference("CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}");
        p.publier("systeme", "Modèle initial de la fiche RFC", false, le);
        return processus.saveAndFlush(p).getId().toString();
    }

    /** Modèles provisionnés par le système : codes d'étape fixes, aucune traduction. */
    private static String url(String instance, String etape, String acte) {
        return INSTANCES + "/" + instance + "/etapes/" + etape + "/" + acte;
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
        String v1 = modeleDate();
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
