package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.AGENT;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.SUPER_ADMIN;
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
import org.springframework.http.MediaType;

/** Fiche RFC du moteur : demandeur, responsable, SSI, référence datée et PDF avant/après signature. */
class ProcessusChangementIT extends AbstractProcessusIT {
    private static final String SSI = "00000000-0000-4000-8000-0000000ss001";
    private static final String AGENT_SSI = "00000000-0000-4000-8000-0000000ss002";

    @BeforeEach
    void securite() {
        // Verrou 2 (D-130) : le RSSI signe ; l'agent SSI de la même unité est refusé.
        organisation.enregistrer(new Compte(SSI, "Sara SSI", "RSSI", "SSI", null, true, true));
        organisation.attribuerProfils(SSI, "ROLE_RSSI");
        organisation.enregistrer(new Compte(AGENT_SSI, "Alain SSI", "Agent SSI", "SSI", null, true, true));
        organisation.attribuerProfils(AGENT_SSI, "ROLE_AGENT_SSI");
        organisation.attribuerProfils(AGENT, "ROLE_PRODUCTION_MONETIQUE");
    }

    private ObjectNode definition() {
        ObjectNode d = json.createObjectNode();
        d.put("code", "RFC_TEST");
        d.put("unite", DemandeChangementConfiguration.UNITE);
        d.put("libelle", "Demande de changement");
        d.put("description", "Fiche RFC");
        d.put("categorie", "Exploitation");
        d.put("delaiCibleJours", 10);
        d.put("referenceModele", DemandeChangementConfiguration.REFERENCE);
        d.set("etapes", json.valueToTree(DemandeChangementConfiguration.etapes()));
        d.set("formulaire", json.valueToTree(DemandeChangementConfiguration.formulaire()));
        d.putObject("initiateurs").putArray("regles").add("TOUT_LE_GIMAC");
        ((ObjectNode) d.at("/initiateurs")).put("initiateurIntervenant", true);
        return d;
    }

    static Map<String, Object> lancement() {
        Map<String, Object> f = new HashMap<>();
        f.put("TYPE_DEMANDEUR", "SUPPORT_MONETIQUE");
        f.put("ELEMENT_CONFIGURATION", "AMA");
        f.put("IDENTIFIANT_EC", "EC-AMA-1");
        f.put("PRIORITE", "URGENT");
        f.put("DECLENCHEUR", "CLIENT");
        f.put("DEMANDEUR_CONTACT", "david@gimac.test");
        f.put("RAISON", "Accès en lecture");
        f.put("ELEMENT_IMPACTE", "AMA, BD");
        f.put("DESCRIPTION_IMPACT", "Nouvel accès");
        f.put("RESPONSABLE_CONTACT", "alice@gimac.test");
        f.put("PLAN_TEST", "Vérifier les droits");
        f.put("PLAN_RETOUR", "Restaurer");
        Map<String, Object> l = new HashMap<>();
        l.put("intitule", "Accès lecture AMA");
        l.put("formulaire", f);
        l.put("designations", Map.of("IMPLEMENTATION", AGENT));
        return l;
    }

    String url(String id, String etape, String acte) {
        return urlEtape(id, etape, acte);
    }

    @Test
    void cycleCompletEtPdf() throws Exception {
        String processus = creer(administration(SUPER_ADMIN), definition());
        publierLevee(processus);
        JsonNode i = lire(mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(AGENT)), lancement())).andExpect(status().isCreated()).andReturn());
        String id = i.get("id").asText();
        String reference = i.get("reference").asText();
        assertThat(reference).matches("CHG-\\d{4}-\\d{1,6}");
        for (String etape : List.of("IDENTIFICATION", "RAISON", "IMPACT")) {
            mvc.perform(post(url(id, etape, "fin")).with(agent(AGENT))).andExpect(status().isOk());
        }
        String document = deposer(id, AGENT, "dossier.pdf", "application/pdf");
        mvc.perform(json(post(url(id, "IMPLEMENTATION", "livrables")).with(agent(AGENT)), Map.of("livrable", "DOSSIER_CHANGEMENT", "documentId", document))).andExpect(status().isOk());
        mvc.perform(post(url(id, "IMPLEMENTATION", "fin")).with(agent(AGENT))).andExpect(status().isOk());
        mvc.perform(get(INSTANCES + "/" + id + "/pdf").with(agent(AGENT)))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(reference + ".pdf")));
        Map<String, Object> champs = new HashMap<>();
        champs.put("DATE_DEPLOIEMENT", "2026-11-02");
        champs.put("DATE_REVUE", "2026-11-03");
        champs.put("EVALUATION", "Risque maîtrisé");
        champs.put("DECISION", "ACCEPTEE");
        champs.put("DESCRIPTION", "Favorable");
        mvc.perform(json(put(url(id, "SSI", "champs")).with(agent(AGENT_SSI)), Map.of("valeurs", champs))).andExpect(status().isForbidden());
        mvc.perform(json(put(url(id, "SSI", "champs")).with(agent(SSI)), Map.of("valeurs", champs))).andExpect(status().isOk());
        mvc.perform(post(url(id, "SSI", "fin")).with(agent(SSI))).andExpect(status().isOk());
        mvc.perform(post(url(id, "SSI", "signature")).with(agent(AGENT_SSI)).header("X-Reauth-Assertion", "assertion").contentType(MediaType.APPLICATION_JSON).content("{\"signature\":\"DECISION_SSI\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post(url(id, "SSI", "signature")).with(agent(SSI)).header("X-Reauth-Assertion", "assertion").contentType(MediaType.APPLICATION_JSON).content("{\"signature\":\"DECISION_SSI\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("CLOSE"));
        byte[] pdf = mvc.perform(get(INSTANCES + "/" + id + "/pdf").with(agent(SSI))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).startsWith("%PDF".getBytes());
        mvc.perform(get(INSTANCES + "/" + id + "/pdf")).andExpect(status().isUnauthorized());
        mvc.perform(get(INSTANCES + "/" + id).with(agent(SUPER_ADMIN))).andExpect(status().isForbidden());
        mvc.perform(get(INSTANCES + "/" + id).with(agent(AGENT))).andExpect(status().isOk()).andExpect(jsonPath("$.reference").value(reference));
    }

    /** D-142 : la mise en œuvre ouverte à tout le GIMAC est un avertissement levé avec motif à la publication. */
    private void publierLevee(String processus) throws Exception {
        mvc.perform(json(post(BASE + "/" + processus + "/publication").with(administration(SUPER_ADMIN)), Map.of("motif", "Mise en œuvre confiée à toute personne interne (D-142)", "leverAvertissements", true)))
            .andExpect(status().isOk());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("D-142 : une personne d'une autre unité, sans le profil Production monétique, est désignée et la demande se lance ; un compte inactif reste refusé")
    void miseEnOeuvreOuverteATousLeGimac() throws Exception {
        String processus = creer(administration(SUPER_ADMIN), definition());
        publierLevee(processus);
        Map<String, Object> l = new HashMap<>(lancement());
        // L'ingénieure du pôle réseau ne porte pas ROLE_PRODUCTION_MONETIQUE et n'est pas de l'unité SSI.
        l.put("designations", Map.of("IMPLEMENTATION", cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU));
        JsonNode i = lire(mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(AGENT)), l)).andExpect(status().isCreated()).andReturn());
        assertThat(i.at("/designations/" + etapeDe(processus, "IMPLEMENTATION") + "/id").asText()).isEqualTo(cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU);
        // Contrôle de sécurité réel conservé : un compte inactif n'est jamais désignable.
        String inactif = "00000000-0000-4000-8000-0000000ss142";
        organisation.enregistrer(new Compte(inactif, "Compte inactif", "Ancien agent", "POLE_RESEAU", null, false, true));
        l.put("designations", Map.of("IMPLEMENTATION", inactif));
        mvc.perform(json(post(BASE + "/" + processus + "/instances").with(agent(AGENT)), l)).andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PIL_PROCESSUS_DESIGNATION_INVALIDE"));
    }
}
