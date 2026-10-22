package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.usecase.sandbox.DesactivationInactifsService;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Suspension d'institution et protection du dernier administrateur (D-119, US-EXI-06) de bout en bout sur PostgreSQL :
 * refus d'introspection et de connexion dès la pose de l'indicateur, sessions révoquées, état des comptes restitué au
 * rétablissement, reprise de l'étape pivot sans double effet (même concurrente), compte protégé épargné par la
 * désactivation pour inactivité.
 */
class SandboxPerimetresIT extends AbstractIamIT {

    @Autowired
    Annuaire annuaire;

    @Autowired
    JwtDecoder decodeur;

    @Autowired
    DesactivationInactifsService desactivation;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate transactions;

    String code;
    String admin;
    String testeur;
    String suspenduAvant;

    static RequestPostProcessor sbxEspace() {
        return jwt().jwt(j -> j.subject("sa-sbx-espace").claim("preferred_username", "service-account-sbx-espace").claim("azp", "sbx-espace")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_SBX_SAGA"));
    }

    static RequestPostProcessor passerelleIntrospection() {
        return jwt().jwt(j -> j.subject("sa-gateway").claim("azp", "gateway-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_SBX_INTROSPECT"));
    }

    static RequestPostProcessor passerelle() {
        return jwt().jwt(j -> j.subject("service-account-gateway-service").claim("azp", "gateway-service")).authorities(new SimpleGrantedAuthority("INTERNAL_IAM_READ"));
    }

    String compte(String role) {
        String s = role.substring(9).toLowerCase() + "." + code;
        return annuaire.creer(new NouveauCompte(s, "Awa", "Ndiaye", s + "@institution.test", null, null, code, Set.of(role)), List.of());
    }

    void jeton(String valeur, String sujet) {
        Instant t = Instant.now();
        when(decodeur.decode(valeur)).thenReturn(Jwt.withTokenValue(valeur).header("alg", "RS256").subject(sujet).claim("roles", List.of("ROLE_SBX_TESTEUR", "SBX_CER_EXECUTION")).issuedAt(t).expiresAt(t.plusSeconds(300)).build());
    }

    boolean introspection(String jeton) throws Exception {
        String r = mvc
            .perform(post("/api/v1/interne/sandbox/introspection").with(passerelleIntrospection()).contentType(MediaType.APPLICATION_JSON).content("{\"jeton\":\"" + jeton + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(r, "$.active");
    }

    boolean actifALaConnexion(String id) throws Exception {
        String r = mvc.perform(get("/api/v1/interne/comptes/{id}/etat-connexion", id).with(passerelle())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.actif");
    }

    org.springframework.test.web.servlet.ResultActions suspendre(boolean suspendu, String cle) throws Exception {
        var req = put("/api/v1/interne/sandbox/perimetres/{code}/suspension", code).with(sbxEspace()).contentType(MediaType.APPLICATION_JSON).content("{\"suspendu\":" + suspendu + ",\"motif\":\"Statut du registre\"}");
        return mvc.perform(cle == null ? req : req.header("Idempotency-Key", cle));
    }

    @BeforeEach
    void init() {
        code = "9" + ThreadLocalRandom.current().nextInt(10000, 99999);
        admin = compte("ROLE_SBX_ADMIN_INSTITUTION");
        testeur = compte("ROLE_SBX_TESTEUR");
        suspenduAvant = compte("ROLE_SBX_LECTEUR");
        annuaire.definirActif(suspenduAvant, false);
        ((AnnuaireEnMemoire) annuaire).ouvrirSession(testeur, "10.0.0.1");
        jeton("jeton-" + code, testeur);
    }

    @Test
    @DisplayName("US-EXI-06 scénario 1 (intégration) : introspection refusée et connexion refusée dès la suspension, sessions révoquées, état des comptes inchangé")
    void refusImmediatApresSuspension() throws Exception {
        assertThat(introspection("jeton-" + code)).isTrue();
        assertThat(actifALaConnexion(testeur)).isTrue();
        suspendre(true, "saga-" + code + "/3").andExpect(status().isOk()).andExpect(jsonPath("$.suspendu").value(true)).andExpect(jsonPath("$.sessionsRevoquees").value(3));
        assertThat(introspection("jeton-" + code)).isFalse();
        assertThat(actifALaConnexion(testeur)).isFalse();
        assertThat(annuaire.sessions(testeur)).isEmpty();
        assertThat(annuaire.lire(testeur).orElseThrow().actif()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where action = 'PERIMETRE_SUSPENDU' and cible_id = ?", Integer.class, code)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and event_type = 'iam.account-administered.v1' and payload::text like '%PERIMETRE_SUSPENDU%'", Integer.class, testeur)).isEqualTo(1);
        mvc.perform(get("/api/v1/sandbox/perimetres?taille=100").with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[?(@.code == '" + code + "')]").isNotEmpty());
        mvc.perform(get("/api/v1/sandbox/perimetres/{code}", code).with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.suspendu").value(true));
    }

    @Test
    @DisplayName("US-EXI-06 scénario 2 : le rétablissement restitue l'état antérieur, le compte suspendu avant le reste")
    void retablissement() throws Exception {
        suspendre(true, "saga-" + code + "/3").andExpect(status().isOk());
        suspendre(false, "saga-r" + code + "/1").andExpect(status().isOk()).andExpect(jsonPath("$.suspendu").value(false));
        assertThat(introspection("jeton-" + code)).isTrue();
        assertThat(actifALaConnexion(testeur)).isTrue();
        assertThat(actifALaConnexion(admin)).isTrue();
        assertThat(actifALaConnexion(suspenduAvant)).isFalse();
    }

    @Test
    @DisplayName("US-EXI-06 scénario 4 : reprises de l'étape pivot avec la même clé, même concurrentes : indicateur posé une seule fois")
    void repriseIdempotente() throws Exception {
        String cle = "saga-" + code + "/3";
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Callable<Integer>> reprises = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                reprises.add(() -> suspendre(true, cle).andReturn().getResponse().getStatus());
            }
            for (Future<Integer> f : pool.invokeAll(reprises)) {
                assertThat(f.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdownNow();
        }
        suspendre(true, cle).andExpect(status().isOk()).andExpect(jsonPath("$.rejouee").value(true));
        assertThat(jdbc.queryForObject("select count(*) from etape_idempotente where cle = ?", Integer.class, cle)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from perimetre_suspendu where code = ?", Integer.class, code)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where action = 'PERIMETRE_SUSPENDU' and cible_id = ?", Integer.class, code)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and payload::text like '%PERIMETRE_SUSPENDU%'", Integer.class, admin)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-EXI-06 scénario 3 (recette) : le compte protégé est épargné par la désactivation pour inactivité ; sa suspension par l'institution est refusée avec motif")
    void compteProtege() throws Exception {
        mvc
            .perform(put("/api/v1/interne/sandbox/comptes/{id}/protection-dernier-administrateur", admin).with(sbxEspace()).header("Idempotency-Key", "saga-p" + code + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"protege\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.protege").value(true));
        Timestamp ancienne = Timestamp.from(Instant.now().minusSeconds(200L * 86400));
        // auto-commit désactivé dans le pool (JHipster) : écriture dans une transaction validée.
        for (String id : List.of(admin, testeur)) {
            transactions.executeWithoutResult(t -> jdbc.update("insert into activite_compte (compte_id, derniere_connexion) values (?, ?) on conflict (compte_id) do update set derniere_connexion = excluded.derniere_connexion", id, ancienne));
        }
        DesactivationInactifsService.Bilan b = desactivation.executer();
        assertThat(b.execute()).as(b.toString()).isTrue();
        assertThat(b.ecartes()).as(b.toString()).isGreaterThanOrEqualTo(1);
        assertThat(b.desactives()).as(b.toString()).isGreaterThanOrEqualTo(1);
        assertThat(annuaire.lire(admin).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(testeur).orElseThrow().actif()).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from journal_administration where action = 'DESACTIVATION_INACTIVITE_ECARTEE' and cible_id = ?", Integer.class, admin)).isEqualTo(1);
        mvc
            .perform(
                put("/api/v1/administration-deleguee/comptes/{id}/etat", admin)
                    .with(AdministrationDelegueeIT.administrateur("autre-admin-" + code, code))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"actif\":false,\"motif\":\"Départ\"}")
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IAM_SBX_COMPTE_PROTEGE"))
            .andExpect(jsonPath("$.details.motif").exists());
        mvc
            .perform(put("/api/v1/interne/sandbox/comptes/{id}/protection-dernier-administrateur", admin).with(sbxEspace()).contentType(MediaType.APPLICATION_JSON).content("{\"protege\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.protege").value(false));
        mvc
            .perform(put("/api/v1/interne/sandbox/comptes/{id}/protection-dernier-administrateur", admin).with(sbxEspace()).contentType(MediaType.APPLICATION_JSON).content("{\"protege\":true}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/sandbox/protections?taille=100").with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ"))).andExpect(status().isOk()).andExpect(jsonPath("$.contenu[?(@.compteId == '" + admin + "')]").isNotEmpty());
    }

    @Autowired
    cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus perimetres;

    @Autowired
    cm.gimac.des.iam.application.port.out.sandbox.ProtectionsAdministrateur protections;

    @Test
    @DisplayName("Adaptateurs JDBC : pose et levée répétées sans effet, état rendu (indicateur et protection)")
    void adaptateursIdempotents() {
        Instant t = Instant.parse("2026-10-22T08:00:00Z");
        transactions.executeWithoutResult(x -> {
            assertThat(perimetres.poser(code, "sa", null, t)).isTrue();
            assertThat(perimetres.poser(code, "sa", "autre", t)).isFalse();
            assertThat(perimetres.lire(code)).hasValueSatisfying(p -> assertThat(p.motif()).isNull());
            assertThat(perimetres.lever(code)).isTrue();
            assertThat(perimetres.lever(code)).isFalse();
            assertThat(protections.definir(admin, code, true, "sa", t)).isTrue();
            assertThat(protections.definir(admin, code, true, "sa", t)).isFalse();
            assertThat(protections.protege(admin)).isTrue();
            assertThat(protections.definir(admin, code, false, "sa", t)).isTrue();
            assertThat(protections.definir(admin, code, false, "sa", t)).isFalse();
            assertThat(protections.protege(admin)).isFalse();
        });
    }
}
