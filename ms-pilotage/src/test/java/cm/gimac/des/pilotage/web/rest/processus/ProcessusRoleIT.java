package cm.gimac.des.pilotage.web.rest.processus;

import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.CHEF_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.INGENIEUR_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.PROFIL_RESEAU;
import static cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.TECHNICIEN_RESEAU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter.Compte;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-138 : une étape adressée à un rôle (intervenant PROFIL) est vue et faisable par tous les porteurs du rôle dans
 * l'unité, comme un message et ses écouteurs ; aucun porteur n'a à la prendre ; le premier acte de fin l'emporte, le
 * second reçoit 409 et ne la voit plus ; un porteur qui reçoit le rôle après l'activation la voit aussi.
 */
class ProcessusRoleIT extends AbstractProcessusIT {

    private static final String NOUVEAU = "00000000-0000-4000-8000-0000000d0138";

    private String publieEtapeDuRole() throws Exception {
        Map<String, Object> etape = new HashMap<>();
        etape.put("code", "UNIQUE");
        etape.put("libelle", "Étape du rôle");
        etape.put("action", "Traiter la demande");
        etape.put("intervenant", Map.of("mode", "PROFIL", "profil", PROFIL_RESEAU));
        etape.put("delaiJours", 2);
        Map<String, Object> d = new HashMap<>(json.convertValue(definition("ROLE_RESEAU"), Map.class));
        d.put("etapes", List.of(etape));
        String id = creer(chef(CHEF_RESEAU), d);
        publier(chef(CHEF_RESEAU), id);
        return id;
    }

    private List<String> mesTaches(String qui) throws Exception {
        JsonNode l = lire(mvc.perform(get("/api/v1/taches").param("vue", "moi").with(agent(qui))).andExpect(status().isOk()).andReturn());
        List<String> ids = new ArrayList<>();
        l.get("contenu").forEach(t -> ids.add(t.get("id").asText()));
        return ids;
    }

    @Test
    @DisplayName("Deux porteurs voient l'étape, le premier la termine sans prise, le second reçoit 409 ; un nouveau porteur la voit")
    void tousLesPorteursVoientEtEffectuent() throws Exception {
        String definition = publieEtapeDuRole();
        String instance = lire(mvc.perform(json(post(BASE + "/" + definition + "/instances").with(chef(CHEF_RESEAU)), Map.of("intitule", "Demande au rôle"))).andExpect(status().isCreated()).andReturn())
            .get("id").asText();
        Map<String, Object> t = jdbc.queryForMap("select id::text as id, etat, responsable_id, pour_role from tache where origine_reference like ?", instance + "%");
        assertThat(t).containsEntry("etat", "A_PRENDRE").containsEntry("pour_role", true);
        assertThat(t.get("responsable_id")).isNull();
        String tache = (String) t.get("id");
        // Notification à l'activation : à tous les porteurs actuels du rôle dans l'unité.
        assertThat(textes(charge("pilotage.process-step-activated.v1", instance).get("destinataires"))).contains(TECHNICIEN_RESEAU, INGENIEUR_RESEAU);

        // Visibilité résolue à la lecture : les deux porteurs, puis un porteur qui reçoit le rôle après l'activation.
        assertThat(mesTaches(TECHNICIEN_RESEAU)).contains(tache);
        assertThat(mesTaches(INGENIEUR_RESEAU)).contains(tache);
        organisation.enregistrer(new Compte(NOUVEAU, "Nouvelle porteuse", "Technicienne réseau", "POLE_RESEAU", CHEF_RESEAU, true, true));
        assertThat(mesTaches(NOUVEAU)).doesNotContain(tache);
        organisation.attribuerProfils(NOUVEAU, PROFIL_RESEAU);
        assertThat(mesTaches(NOUVEAU)).contains(tache);
        mvc.perform(get(INSTANCES + "/" + instance).with(agent(NOUVEAU))).andExpect(status().isOk())
            .andExpect(jsonPath("$.etapes[0].moi.executer").value(true));

        // Exécution sans prise : le premier acte de fin l'emporte, le second porteur reçoit 409 et ne la voit plus.
        mvc.perform(post(urlEtape(instance, "UNIQUE", "fin")).with(agent(INGENIEUR_RESEAU))).andExpect(status().isOk());
        // Second acte : 409 explicite (étape terminée, ou instance close quand c'était sa dernière étape).
        mvc.perform(post(urlEtape(instance, "UNIQUE", "fin")).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.oneOf("PIL_PROCESSUS_ETAPE_ETAT", "PIL_PROCESSUS_INSTANCE_ETAT")));
        assertThat(mesTaches(TECHNICIEN_RESEAU)).doesNotContain(tache);
        assertThat(mesTaches(NOUVEAU)).doesNotContain(tache);
        // Audit : l'auteur réel de l'acte.
        assertThat(jdbc.queryForObject("select responsable_id from tache where id::text = ?", String.class, tache)).isEqualTo(INGENIEUR_RESEAU);
        assertThat(jdbc.queryForObject("select etat from tache where id::text = ?", String.class, tache)).isEqualTo("TERMINEE");
    }

    @Test
    @DisplayName("Un seul porteur dans l'unité : l'étape reste adressée au rôle, jamais figée sur la personne")
    void seulPorteurResteAuRole() throws Exception {
        organisation.attribuerProfils(TECHNICIEN_RESEAU);
        String definition = publieEtapeDuRole();
        String instance = lire(mvc.perform(json(post(BASE + "/" + definition + "/instances").with(chef(CHEF_RESEAU)), Map.of("intitule", "Un seul porteur"))).andExpect(status().isCreated()).andReturn())
            .get("id").asText();
        Map<String, Object> t = jdbc.queryForMap("select etat, responsable_id, pour_role from tache where origine_reference like ?", instance + "%");
        assertThat(t).containsEntry("etat", "A_PRENDRE").containsEntry("pour_role", true);
        assertThat(t.get("responsable_id")).isNull();
        // Un seul porteur : la notification part quand même à tous les porteurs actuels (ici, le seul).
        assertThat(textes(charge("pilotage.process-step-activated.v1", instance).get("destinataires"))).containsExactly(INGENIEUR_RESEAU);
        // Le rôle donné ensuite à un autre membre lui rend l'étape visible et faisable.
        organisation.attribuerProfils(TECHNICIEN_RESEAU, PROFIL_RESEAU);
        mvc.perform(get(INSTANCES + "/" + instance).with(agent(TECHNICIEN_RESEAU))).andExpect(status().isOk())
            .andExpect(jsonPath("$.etapes[0].moi.executer").value(true));
    }

    @Test
    @DisplayName("Deux porteurs terminent au même instant : l'un l'emporte (200), l'autre reçoit 409 ; la tâche est close une fois")
    void finsSimultanees() throws Exception {
        String definition = publieEtapeDuRole();
        String instance = lire(mvc.perform(json(post(BASE + "/" + definition + "/instances").with(chef(CHEF_RESEAU)), Map.of("intitule", "Concurrence"))).andExpect(status().isCreated()).andReturn())
            .get("id").asText();
        String url = urlEtape(instance, "UNIQUE", "fin");
        java.util.concurrent.CyclicBarrier depart = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<org.springframework.mock.web.MockHttpServletResponse>> fins = new ArrayList<>();
            for (String qui : List.of(TECHNICIEN_RESEAU, INGENIEUR_RESEAU)) {
                fins.add(pool.submit(() -> {
                    depart.await();
                    return mvc.perform(post(url).with(agent(qui))).andReturn().getResponse();
                }));
            }
            List<Integer> statuts = new ArrayList<>();
            List<String> corps = new ArrayList<>();
            for (var f : fins) {
                statuts.add(f.get().getStatus());
                corps.add(f.get().getContentAsString());
            }
            assertThat(statuts).containsExactlyInAnyOrder(200, 409);
            assertThat(String.join(" ", corps)).containsAnyOf("PIL_PROCESSUS_ETAPE_ETAT", "PIL_PROCESSUS_INSTANCE_ETAT", "PIL_PROCESSUS_CONCURRENCE");
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select count(*) from tache where origine_reference like ? and etat = 'TERMINEE'", Integer.class, instance + "%")).isEqualTo(1);
    }

    @org.springframework.beans.factory.annotation.Autowired
    cm.gimac.des.pilotage.repository.TacheRepository taches;

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    @DisplayName("Deux transactions sur la même version de la tâche du rôle : la première l'emporte, la seconde échoue (verrou optimiste)")
    void memeVersionDeuxTransactions() throws Exception {
        String definition = publieEtapeDuRole();
        String instance = lire(mvc.perform(json(post(BASE + "/" + definition + "/instances").with(chef(CHEF_RESEAU)), Map.of("intitule", "Version"))).andExpect(status().isCreated()).andReturn())
            .get("id").asText();
        java.util.UUID id = java.util.UUID.fromString(jdbc.queryForObject("select id::text from tache where origine_reference like ?", String.class, instance + "%"));
        org.springframework.transaction.support.TransactionTemplate tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        // Deux transactions lisent la même version (deux porteurs qui ouvrent la tâche au même moment).
        var vueA = tx.execute(s -> taches.findById(id).orElseThrow());
        var vueB = tx.execute(s -> taches.findById(id).orElseThrow());
        // La première agit et valide ; la seconde, sur la version périmée, est refusée par la base.
        tx.executeWithoutResult(s -> {
            vueA.transiter(cm.gimac.des.pilotage.domain.activite.EtatTache.OUVERTE, null, TECHNICIEN_RESEAU, true, java.time.Instant.now());
            taches.saveAndFlush(vueA);
        });
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            vueB.transiter(cm.gimac.des.pilotage.domain.activite.EtatTache.OUVERTE, null, INGENIEUR_RESEAU, true, java.time.Instant.now());
            taches.saveAndFlush(vueB);
        })).isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        assertThat(jdbc.queryForObject("select responsable_id from tache where id = ?", String.class, id)).isEqualTo(TECHNICIEN_RESEAU);
    }
}
