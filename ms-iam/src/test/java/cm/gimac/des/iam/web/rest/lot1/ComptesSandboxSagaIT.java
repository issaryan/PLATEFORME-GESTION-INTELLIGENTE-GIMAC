package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.application.port.out.Annuaire;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Contrat interne OUVERTURE/MATRICE de SBX-ESPACE sur l'annuaire et PostgreSQL. */
class ComptesSandboxSagaIT extends AbstractIamIT {

    @Autowired Annuaire annuaire;

    static RequestPostProcessor sbxEspace() {
        return jwt().jwt(j -> j.subject("sa-sbx-espace").claim("preferred_username", "service-account-sbx-espace")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_SBX_SAGA"));
    }

    @Test
    @DisplayName("OUVERTURE rejouée sans doublon, puis MATRICE limitée aux profils SBX")
    void ouvertureEtMatrice() throws Exception {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        String identifiant = "saga." + suffixe;
        String cle = "saga-" + suffixe + "/1";
        String corps = "{\"identifiant\":\"" + identifiant + "\",\"prenom\":\"Awa\",\"nom\":\"Saga\",\"email\":\"" + identifiant + "@test.cm\",\"institutionId\":\"99001\",\"profils\":[\"ROLE_SBX_TESTEUR\"]}";
        String response = mvc.perform(post("/api/v1/interne/sandbox/comptes").with(sbxEspace()).header("Idempotency-Key", cle).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.identifiant").value(identifiant)).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");
        mvc.perform(post("/api/v1/interne/sandbox/comptes").with(sbxEspace()).header("Idempotency-Key", cle).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.rejouee").value(true));
        annuaire.definirProfils(id, java.util.Set.of("ROLE_SBX_TESTEUR", "ROLE_CORRESPONDANT"));
        mvc.perform(put("/api/v1/interne/sandbox/comptes/{id}/profils-sandbox", id).with(sbxEspace()).header("Idempotency-Key", "saga-" + suffixe + "/2").contentType(MediaType.APPLICATION_JSON).content("{\"profils\":[\"ROLE_SBX_LECTEUR\"],\"motif\":\"Matrice de recette\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.profils[?(@ == 'ROLE_CORRESPONDANT')]").isNotEmpty());
        assertThat(annuaire.lire(id).orElseThrow().profils()).containsExactlyInAnyOrder("ROLE_CORRESPONDANT", "ROLE_SBX_LECTEUR");
        assertThat(jdbc.queryForObject("select count(*) from etape_idempotente where cle in (?, ?)", Integer.class, cle, "saga-" + suffixe + "/2")).isEqualTo(2);
    }

    @Test
    @DisplayName("US-INS-07 : matrice globale rejouée, permissions hors SBX et validation GIMAC conservées")
    void matriceGlobale() throws Exception {
        String cle = "saga-matrice-" + UUID.randomUUID() + "/1";
        String corps = "{\"version\":7,\"profils\":{\"ROLE_SBX_LECTEUR\":[\"SBX_ISO_SIMULATION\"],\"ROLE_SNA\":[\"SBX_CER_VALIDATION\",\"SBX_CER_CONSULTATION\"]}}";
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(sbxEspace()).header("Idempotency-Key", cle).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(7)).andExpect(jsonPath("$.rejouee").value(false));
        assertThat(annuaire.compositions().get("ROLE_SBX_LECTEUR")).contains("SBX_ISO_SIMULATION", "NOT_INBOX_READ").noneMatch(p -> p.startsWith("SBX_LEC_"));
        assertThat(annuaire.compositions().get("ROLE_SNA")).contains("SBX_CER_VALIDATION", "SBX_CER_CONSULTATION");
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(sbxEspace()).header("Idempotency-Key", cle).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(7)).andExpect(jsonPath("$.rejouee").value(true));
        assertThat(jdbc.queryForObject("select count(*) from etape_idempotente where cle = ?", Integer.class, cle)).isEqualTo(1);
    }

    @Test
    @DisplayName("D-133 : le chargé de l'implémentation des projets de la SNA traverse les deux listes ; un profil inconnu reste refusé")
    void matriceAvecLeChargeDeLImplementationSna() throws Exception {
        String inconnu = "{\"version\":9,\"profils\":{\"ROLE_CHARGE_IMPLEMENTATION_SNA\":[\"SBX_CER_CONSULTATION\"],\"ROLE_INCONNU\":[\"SBX_CER_CONSULTATION\"]}}";
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(sbxEspace()).header("Idempotency-Key", "saga-matrice-" + UUID.randomUUID() + "/1").contentType(MediaType.APPLICATION_JSON).content(inconnu))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("IAM_SBX_PROFIL_INVALIDE"));
        String corps = "{\"version\":9,\"profils\":{\"ROLE_CHARGE_IMPLEMENTATION_SNA\":[\"SBX_CER_CONSULTATION\"]}}";
        mvc.perform(put("/api/v1/interne/sandbox/matrice").with(sbxEspace()).header("Idempotency-Key", "saga-matrice-" + UUID.randomUUID() + "/1").contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(9)).andExpect(jsonPath("$.rejouee").value(false));
        assertThat(annuaire.compositions().get("ROLE_CHARGE_IMPLEMENTATION_SNA")).contains("SBX_CER_CONSULTATION");
    }
}
