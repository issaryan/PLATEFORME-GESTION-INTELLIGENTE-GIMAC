package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.iam.AbstractIamIT;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.sandbox.DelegationsSandbox;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.sandbox.Delegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mandats de partenaire sur les délégations temporaires de MS-IAM (D-119, US-EXI-04) de bout en bout sur PostgreSQL :
 * jeton délégué projeté sur l'institution mandante, refus d'introspection à l'échéance sans le planificateur, révocation
 * immédiate, durée excessive refusée sans état partiel, mandats compris dans la suspension du périmètre mandant.
 */
class SandboxDelegationsIT extends AbstractIamIT {

    @Autowired
    Annuaire annuaire;

    @Autowired
    JwtDecoder decodeur;

    @Autowired
    DelegationsSandbox delegations;

    @Autowired
    TransactionTemplate transactions;

    String mandant;
    String partenaire;
    String jeton;

    @BeforeEach
    void init() {
        mandant = "9" + ThreadLocalRandom.current().nextInt(10000, 99999);
        partenaire = annuaire.creer(
            new NouveauCompte("integrateur." + mandant, "Paul", "Ebode", "paul." + mandant + "@integrateur.test", null, null, "8" + mandant.substring(1), Set.of("ROLE_SBX_PARTENAIRE")),
            List.of()
        );
        ((AnnuaireEnMemoire) annuaire).ouvrirSession(partenaire, "10.0.0.9");
        jeton = "jeton-partenaire-" + mandant;
        Instant t = Instant.now();
        when(decodeur.decode(jeton)).thenReturn(Jwt.withTokenValue(jeton).header("alg", "RS256").subject(partenaire).claim("roles", List.of("ROLE_SBX_PARTENAIRE")).issuedAt(t).expiresAt(t.plusSeconds(300)).build());
    }

    ResultActions accorder(Instant debut, Instant fin, String cle) throws Exception {
        String corps = "{\"compteId\":\"" + partenaire + "\",\"institution\":\"" + mandant + "\",\"permissions\":[\"SBX_CER_EXECUTION\",\"SBX_RAC_CONSULTATION\"],\"debut\":\"" + debut + "\",\"fin\":\"" + fin + "\"}";
        return mvc.perform(post("/api/v1/interne/sandbox/delegations").with(SandboxPerimetresIT.sbxEspace()).header("Idempotency-Key", cle).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    String introspection(String institution) throws Exception {
        return mvc
            .perform(
                post("/api/v1/interne/sandbox/introspection")
                    .with(SandboxPerimetresIT.passerelleIntrospection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"jeton\":\"" + jeton + "\",\"institution\":\"" + institution + "\"}")
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    @Test
    @DisplayName("US-EXI-04 scénario 1 : jeton délégué portant institution_id du mandant et les seuls droits du mandat ; délégation datée, rejeu sans second mandat")
    void octroi() throws Exception {
        Instant debut = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(60);
        Instant fin = debut.plus(Duration.ofDays(30));
        String r = accorder(debut, fin, "saga-m" + mandant + "/4").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(r, "$.delegation.id");
        accorder(debut, fin, "saga-m" + mandant + "/4").andExpect(status().isOk()).andExpect(jsonPath("$.delegation.id").value(id));
        assertThat(jdbc.queryForObject("select count(*) from delegation_sandbox where institution_id = ?", Integer.class, mandant)).isEqualTo(1);
        String i = introspection(mandant);
        assertThat((Boolean) JsonPath.read(i, "$.active")).isTrue();
        assertThat((String) JsonPath.read(i, "$.institution_id")).isEqualTo(mandant);
        assertThat((List<String>) JsonPath.read(i, "$.permissions")).containsExactly("SBX_CER_EXECUTION", "SBX_RAC_CONSULTATION");
        assertThat((String) JsonPath.read(i, "$.delegation_id")).isEqualTo(id);
        mvc.perform(get("/api/v1/interne/sandbox/delegations/{id}", id).with(SandboxPerimetresIT.sbxEspace())).andExpect(status().isOk())
            .andExpect(jsonPath("$.delegation.debut").value(debut.toString())).andExpect(jsonPath("$.delegation.fin").value(fin.toString()));
        mvc.perform(get("/api/v1/sandbox/delegations?type=MANDAT&taille=100").with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[?(@.delegation.id == '" + id + "')].etat").value("ACTIVE"));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and payload::text like '%MANDAT_ACCORDE%'", Integer.class, partenaire)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-EXI-04 scénario 2 (intégration) : introspection refusée dès la date de fin, sans action du planificateur")
    void echeanceSansPlanificateur() throws Exception {
        Instant t = Instant.now();
        Delegation echue = new Delegation(UUID.randomUUID().toString(), TypeDelegation.MANDAT, partenaire, mandant, Set.of("SBX_CER_EXECUTION"), Set.of(),
            t.minus(Duration.ofDays(2)), t.minusSeconds(1), EtatDelegation.ACCORDEE, "sa", null, null, t.minus(Duration.ofDays(2)), null);
        transactions.executeWithoutResult(x -> delegations.enregistrer(echue));
        assertThat((Boolean) JsonPath.read(introspection(mandant), "$.active")).isFalse();
        assertThat(jdbc.queryForObject("select etat from delegation_sandbox where id = ?::uuid", String.class, echue.id())).isEqualTo("ACCORDEE");
    }

    @Test
    @DisplayName("US-EXI-04 scénario 3 (recette) : révocation, délégation close, sessions du partenaire révoquées et accès refusé immédiatement")
    void revocation() throws Exception {
        Instant debut = Instant.now().minusSeconds(60);
        String id = JsonPath.read(accorder(debut, debut.plus(Duration.ofDays(1)), "saga-m" + mandant + "/4").andReturn().getResponse().getContentAsString(), "$.delegation.id");
        assertThat((Boolean) JsonPath.read(introspection(mandant), "$.active")).isTrue();
        mvc.perform(delete("/api/v1/interne/sandbox/delegations/{id}", id).param("motif", "Fin du contrat").with(SandboxPerimetresIT.sbxEspace()).header("Idempotency-Key", "saga-r" + mandant + "/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("REVOQUEE"));
        assertThat((Boolean) JsonPath.read(introspection(mandant), "$.active")).isFalse();
        assertThat(annuaire.sessions(partenaire)).isEmpty();
        assertThat(jdbc.queryForObject("select etat from delegation_sandbox where id = ?::uuid", String.class, id)).isEqualTo("REVOQUEE");
        mvc.perform(put("/api/v1/interne/sandbox/delegations/{id}/fin", id).with(SandboxPerimetresIT.sbxEspace()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fin\":\"" + debut.plus(Duration.ofDays(2)) + "\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IAM_SBX_DELEGATION_CLOSE"));
        mvc.perform(get("/api/v1/interne/sandbox/delegations/{id}", "inconnue").with(SandboxPerimetresIT.sbxEspace())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-EXI-04 scénario 4 : durée excessive refusée par un problème HTTP normalisé, sans état partiel")
    void dureeExcessive() throws Exception {
        Instant debut = Instant.now();
        accorder(debut, debut.plus(Duration.ofDays(91)), "saga-x" + mandant + "/4")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("IAM_SBX_DUREE_EXCESSIVE"))
            .andExpect(jsonPath("$.type").value("https://des.gimac.local/problemes/IAM_SBX_DUREE_EXCESSIVE"));
        assertThat(jdbc.queryForObject("select count(*) from delegation_sandbox where institution_id = ?", Integer.class, mandant)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from etape_idempotente where cle = ?", Integer.class, "saga-x" + mandant + "/4")).isZero();
    }

    @Test
    @DisplayName("Prolongation et suspension du périmètre mandant : délégations de mandat comprises (US-EXI-06 scénario 1)")
    void prolongationEtSuspension() throws Exception {
        Instant debut = Instant.now().minusSeconds(60);
        String id = JsonPath.read(accorder(debut, debut.plus(Duration.ofDays(1)), null).andReturn().getResponse().getContentAsString(), "$.delegation.id");
        mvc.perform(put("/api/v1/interne/sandbox/delegations/{id}/fin", id).with(SandboxPerimetresIT.sbxEspace()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fin\":\"" + debut.plus(Duration.ofDays(10)) + "\"}")).andExpect(status().isOk());
        ((AnnuaireEnMemoire) annuaire).ouvrirSession(partenaire, "10.0.0.10");
        mvc.perform(put("/api/v1/interne/sandbox/perimetres/{code}/suspension", mandant).with(SandboxPerimetresIT.sbxEspace()).contentType(MediaType.APPLICATION_JSON).content("{\"suspendu\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionsRevoquees").value(1));
        assertThat(annuaire.sessions(partenaire)).isEmpty();
        assertThat((Boolean) JsonPath.read(introspection(mandant), "$.active")).isFalse();
    }

    @Test
    @DisplayName("Adaptateur JDBC : délégation de prestataire avec composants, liste filtrée ou non, identifiant invalide")
    void adaptateur() {
        Instant t = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Delegation p = new Delegation(UUID.randomUUID().toString(), TypeDelegation.PRESTATAIRE, partenaire, null, Set.of(), Set.of("SBX-TRACE", "MIFOS"),
            t, t.plusSeconds(3600), EtatDelegation.DEMANDEE, "u1", null, "maintenance", t, null);
        transactions.executeWithoutResult(x -> {
            delegations.enregistrer(p);
            assertThat(delegations.lire(p.id())).hasValueSatisfying(d -> {
                assertThat(d.composants()).containsExactlyInAnyOrder("SBX-TRACE", "MIFOS");
                assertThat(d.permissions()).isEmpty();
                assertThat(d.institutionId()).isNull();
            });
            delegations.mettreAJour(p.avecEtat(EtatDelegation.REFUSEE, "rssi", "non", t));
            assertThat(delegations.lire(p.id()).orElseThrow().closeLe()).isEqualTo(t);
            assertThat(delegations.lister(TypeDelegation.PRESTATAIRE, 0, 100)).extracting(Delegation::id).contains(p.id());
            assertThat(delegations.lister(null, 0, 1000)).extracting(Delegation::id).contains(p.id());
            assertThat(delegations.compter(null)).isGreaterThanOrEqualTo(delegations.compter(TypeDelegation.PRESTATAIRE));
            assertThat(delegations.lire("pas-un-uuid")).isEmpty();
            assertThat(delegations.enCours(partenaire, TypeDelegation.PRESTATAIRE, t)).isEmpty();
        });
    }

    String introspectionPrestataire(String jetonPrestataire, String institution, String composant) throws Exception {
        String corps = "{\"jeton\":\"" + jetonPrestataire + "\"" + (institution == null ? "" : ",\"institution\":\"" + institution + "\"") + ",\"composant\":\"" + composant + "\"}";
        return mvc
            .perform(post("/api/v1/interne/sandbox/introspection").with(SandboxPerimetresIT.passerelleIntrospection()).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    @Test
    @DisplayName("US-EXI-24 (intégration) : demande sur SBX-TRACE approuvée par le RSSI, octroi journalisé, introspection bornée au composant, données d'institution refusées, fin à l'échéance")
    void accesPrestataire() throws Exception {
        String prestataire = annuaire.creer(new NouveauCompte("presta." + mandant, "Jean", "Mbarga", "jean." + mandant + "@presta.test", null, null, null, Set.of()), List.of());
        String jetonPrestataire = "jeton-prestataire-" + mandant;
        Instant t = Instant.now();
        when(decodeur.decode(jetonPrestataire)).thenReturn(Jwt.withTokenValue(jetonPrestataire).header("alg", "RS256").subject(prestataire).claim("roles", List.of("SBX_TRA_CONSULTATION")).issuedAt(t).expiresAt(t.plusSeconds(300)).build());
        Instant debut = t.truncatedTo(ChronoUnit.SECONDS).minusSeconds(60);
        String corps = "{\"compteId\":\"" + prestataire + "\",\"composants\":[\"SBX-TRACE\"],\"debut\":\"" + debut + "\",\"fin\":\"" + debut.plus(Duration.ofHours(4)) + "\",\"motif\":\"Correctif\"}";
        String r = mvc.perform(post("/api/v1/sandbox/delegations/prestataire").with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content(corps))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.etat").value("DEMANDEE")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(r, "$.delegation.id");
        // Demande en attente : aucun accès ; l'auteur de la demande ne l'approuve pas.
        assertThat((Boolean) JsonPath.read(introspectionPrestataire(jetonPrestataire, null, "SBX-TRACE"), "$.active")).isFalse();
        mvc.perform(post("/api/v1/sandbox/delegations/{id}/approbation", id).with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_SECURITY"))).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("IAM_SBX_APPROBATION_PROPRE"));
        mvc.perform(post("/api/v1/sandbox/delegations/{id}/approbation", id).with(utilisateur("demo-rssi", "IAM_ACCOUNT_SECURITY"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.etat").value("ACTIVE")).andExpect(jsonPath("$.delegation.approbateurId").value("demo-rssi"));
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and payload::text like '%ACCES_PRESTATAIRE_ACCORDE%'", Integer.class, prestataire)).isEqualTo(1);
        String i = introspectionPrestataire(jetonPrestataire, null, "SBX-TRACE");
        assertThat((Boolean) JsonPath.read(i, "$.active")).isTrue();
        assertThat((String) JsonPath.read(i, "$.type")).isEqualTo("PRESTATAIRE");
        assertThat((List<String>) JsonPath.read(i, "$.composants")).containsExactly("SBX-TRACE");
        assertThat((List<String>) JsonPath.read(i, "$.permissions")).isEmpty();
        assertThat((Boolean) JsonPath.read(introspectionPrestataire(jetonPrestataire, null, "SBX-CAMPAGNE"), "$.active")).isFalse();
        // Critère 3 : données d'une institution demandées, refus journalisé.
        assertThat((Boolean) JsonPath.read(introspectionPrestataire(jetonPrestataire, "99001", "SBX-TRACE"), "$.active")).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and payload::text like '%TENTATIVE_ACCES_PRESTATAIRE%'", Integer.class, prestataire)).isEqualTo(1);
        // Critère 2 : à l'échéance, l'introspection est refusée.
        transactions.executeWithoutResult(x -> jdbc.update("update delegation_sandbox set fin = ? where id = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)), UUID.fromString(id)));
        assertThat((Boolean) JsonPath.read(introspectionPrestataire(jetonPrestataire, null, "SBX-TRACE"), "$.active")).isFalse();
        mvc.perform(get("/api/v1/sandbox/delegations?type=PRESTATAIRE&taille=100").with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_READ"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.contenu[?(@.delegation.id == '" + id + "')].etat").value("ECHUE"));
        mvc.perform(post("/api/v1/sandbox/delegations/{id}/revocation", id).with(utilisateur("demo-rssi", "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Clos\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("REVOQUEE"));
        // Refus d'une autre demande.
        String r2 = mvc.perform(post("/api/v1/sandbox/delegations/prestataire").with(utilisateur(SUPER_ADMIN, "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content(corps.replace(debut.toString(), Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/sandbox/delegations/{id}/refus", (String) JsonPath.read(r2, "$.delegation.id")).with(utilisateur("demo-rssi", "IAM_ACCOUNT_SECURITY")).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Non\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.etat").value("REFUSEE"));
    }
}
