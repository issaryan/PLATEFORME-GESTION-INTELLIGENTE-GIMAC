package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.IamException;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Organigramme de bout en bout (D-102, docs/lot1/organigramme.md), sur PostgreSQL réel et l'annuaire en mémoire :
 * affectation de plusieurs personnes à un service (n+1 résolu, journal motivé, événement par personne), refus d'ensemble
 * sans aucune modification, retrait, motif des gestes d'unité, et organigramme en PDF composé par MS-IAM (moteur de
 * MS-DOCUMENT simulé au port : document reçu contrôlé, moteur indisponible : 503).
 */
class OrganigrammeIT extends AbstractIamIT {

    @MockitoBean
    RenduPdf rendu;

    String creer(String identifiant, String prenom, String institution) throws Exception {
        String corps = """
            {"identifiant":"%s","prenom":"%s","nom":"Essomba","email":"%s@gimac.test","telephone":"+237600000019","fonction":"Fonction de %s",%s"profils":["%s"],"envoyerInvitation":true}"""
            .formatted(identifiant, prenom, identifiant, prenom, institution == null ? "" : "\"institutionId\":\"" + institution + "\",", institution == null ? "ROLE_CHARGE_DDRM" : "ROLE_CORRESPONDANT");
        String reponse = mvc
            .perform(post("/api/v1/comptes").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(reponse, "$.compte.id");
    }

    static String suffixe() {
        return java.util.UUID.randomUUID().toString().substring(0, 6);
    }

    @Test
    @DisplayName("Affecter deux personnes à un service : n+1 = chef du service, journal motivé et événement par personne ; refus d'ensemble ; retrait")
    void affecterEtRetirer() throws Exception {
        String s = suffixe();
        String chef = creer("chef." + s, "Gaston", null);
        String a = creer("agent.a." + s, "Idriss", null);
        String b = creer("agent.b." + s, "Nadège", null);
        String externe = creer("corr." + s, "Brice", "INST-0042");
        String code = "CELLULE_" + s.toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        mvc
            .perform(
                post("/api/v1/organisation/unites")
                    .with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"%s\",\"libelle\":\"Cellule supervision\",\"type\":\"CELLULE\",\"parent\":\"POLE_RESEAU\",\"responsableId\":\"%s\",\"motif\":\"Création de la cellule\"}".formatted(code, chef))
            )
            .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select motif from journal_administration where action = 'CONFIGURATION_UNITE_ORGANISATIONNELLE' and details like ?", String.class, "%code=" + code + "%")).isEqualTo("Création de la cellule");

        String u = "/api/v1/organisation/unites/" + code + "/affectations";
        // Refus d'ensemble : un compte d'institution parmi les personnes, rien n'est modifié.
        mvc
            .perform(post(u).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content("{\"comptes\":[\"%s\",\"%s\"],\"motif\":\"Renfort\"}".formatted(a, externe)))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("ORG_AFFECTATION_REFUSEE"))
            .andExpect(jsonPath("$.details.refus[0].id").value(externe))
            .andExpect(jsonPath("$.details.refus[0].code").value("ORG_COMPTE_EXTERNE"));
        mvc.perform(get("/api/v1/comptes/{id}", a).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))).andExpect(jsonPath("$.unite").value(Matchers.nullValue()));
        // Motif obligatoire.
        mvc
            .perform(post(u).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content("{\"comptes\":[\"%s\"]}".formatted(a)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IAM_REASON_REQUIRED"));
        // Agent du département sans droit d'administration ni pouvoir de rattachement : 403.
        mvc
            .perform(post(u).with(utilisateur(a, "PIL_ACTIVITY_OWN")).contentType(MediaType.APPLICATION_JSON).content("{\"comptes\":[\"%s\"],\"motif\":\"Renfort\"}".formatted(b)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORG_HORS_PERIMETRE"));

        mvc
            .perform(post(u).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)).contentType(MediaType.APPLICATION_JSON).content("{\"comptes\":[\"%s\",\"%s\"],\"motif\":\"Renfort de la cellule\"}".formatted(a, b)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(a))
            .andExpect(jsonPath("$[0].avant.unite").value(Matchers.nullValue()))
            .andExpect(jsonPath("$[0].apres.unite").value(code))
            .andExpect(jsonPath("$[0].apres.responsableId").value(chef))
            .andExpect(jsonPath("$[1].apres.responsableId").value(chef));
        mvc
            .perform(get("/api/v1/comptes/{id}", b).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)))
            .andExpect(jsonPath("$.unite").value(code))
            .andExpect(jsonPath("$.responsableId").value(chef));
        for (String id : List.of(a, b)) {
            var ligne = jdbc.queryForMap("select motif, details from journal_administration where action = 'COMPTE_RATTACHEMENT_MODIFIE' and cible_id = ? order by horodatage desc limit 1", id);
            assertThat(ligne.get("motif")).isEqualTo("Renfort de la cellule");
            assertThat((String) ligne.get("details")).contains("après: unite=" + code + " responsable=" + chef).contains("affectation=" + code);
            assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = 'iam.account-administered.v1' and payload::text like '%COMPTE_RATTACHEMENT_MODIFIE%'", Integer.class, id)).isEqualTo(1);
        }

        // Retrait : unité et n+1 venu de l'unité retirés ; seconde fois : 409.
        mvc
            .perform(delete(u + "/{id}", a).param("motif", "Départ de la cellule").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apres.unite").value(Matchers.nullValue()))
            .andExpect(jsonPath("$.apres.responsableId").value(Matchers.nullValue()));
        mvc.perform(delete(u + "/{id}", a).param("motif", "Encore").with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORG_AFFECTATION_ABSENTE"));
        mvc.perform(delete(u + "/{id}", b).with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IAM_REASON_REQUIRED"));

        // Motif du rattachement d'administration (geste existant), désormais journalisé.
        mvc
            .perform(
                put("/api/v1/comptes/{id}/rattachement", b)
                    .with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"unite\":\"%s\",\"responsableId\":null,\"motif\":\"Autonomie\"}".formatted(code))
            )
            .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select motif from journal_administration where action = 'COMPTE_RATTACHEMENT_MODIFIE' and cible_id = ? order by horodatage desc limit 1", String.class, b)).isEqualTo("Autonomie");
    }

    @Test
    @DisplayName("Organigramme en PDF : document composé depuis les données (unité créée, chef, membres), pièce jointe ; moteur indisponible : 503")
    void organigrammePdf() throws Exception {
        AtomicReference<String> recu = new AtomicReference<>();
        AtomicReference<RenduPdf.Mise> mise = new AtomicReference<>();
        when(rendu.rendre(anyString(), anyString(), any())).thenAnswer(i -> {
            recu.set(i.getArgument(0));
            mise.set(i.getArgument(2));
            return "%PDF-1.7 essai".getBytes(StandardCharsets.US_ASCII);
        });
        String s = suffixe();
        String chef = creer("chef.pdf." + s, "Solange", null);
        mvc
            .perform(
                post("/api/v1/organisation/unites")
                    .with(utilisateur(SUPER_ADMIN, PERMISSIONS_SUPER_ADMIN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"QUALITE_%s\",\"libelle\":\"Cellule <qualité> & audit\",\"type\":\"CELLULE\",\"parent\":\"SNA\",\"responsableId\":\"%s\"}".formatted(s.toUpperCase(java.util.Locale.ROOT).replace('-', '_'), chef))
            )
            .andExpect(status().isCreated());
        byte[] pdf = mvc
            .perform(get("/api/v1/organisation/organigramme.pdf").param("membres", "true").with(utilisateur("agent", "PIL_ACTIVITY_OWN")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", Matchers.matchesPattern("attachment; filename=\"organigramme-moabi-\\d{4}-\\d{2}-\\d{2}\\.pdf\"")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertThat(new String(pdf, StandardCharsets.US_ASCII)).startsWith("%PDF-");
        assertThat(mise.get()).isEqualTo(new RenduPdf.Mise(RenduPdf.Format.A3, true));
        assertThat(recu.get()).contains("<h1>Organigramme</h1>").contains("Cellule &lt;qualité&gt; &amp; audit").contains("Solange Essomba").contains("Membres par unité").doesNotContain("<qualité>");

        mvc.perform(get("/api/v1/organisation/organigramme.pdf").param("racine", "INCONNUE").with(utilisateur("agent", "IAM_ACCOUNT_READ"))).andExpect(status().isNotFound());
        when(rendu.rendre(anyString(), anyString(), any())).thenThrow(IamException.indisponible("ORG_PDF_INDISPONIBLE", "Moteur indisponible"));
        mvc
            .perform(get("/api/v1/organisation/organigramme.pdf").param("racine", "DES").param("format", "A4").with(utilisateur("agent", "IAM_ACCOUNT_READ")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("ORG_PDF_INDISPONIBLE"));
    }
}
