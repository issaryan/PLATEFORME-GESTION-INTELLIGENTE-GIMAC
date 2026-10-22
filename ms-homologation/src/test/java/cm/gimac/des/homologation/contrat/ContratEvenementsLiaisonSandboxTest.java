package cm.gimac.des.homologation.contrat;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.event.LiaisonOpenedPayload;
import cm.gimac.des.homologation.domain.event.LiaisonStatusChangedPayload;
import cm.gimac.des.homologation.infrastructure.messaging.TopicResolver;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat des événements de liaison lus par SBX-CAMPAGNE et SBX-LECTURE (US-EXI-16, D-122) : schémas publiés,
 * sujet, et règle de lecture des consommateurs rejouée par une projection de référence (les services de la sandbox
 * sont encore absents, sandbox-existant-adapte.md §1). Revue : aucun client de la sandbox en écriture.
 */
class ContratEvenementsLiaisonSandboxTest {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final UUID LIAISON = UUID.fromString("0192f0aa-0000-7000-8000-000000000001");
    static final UUID INSTITUTION = UUID.fromString("0192f0aa-0000-7000-8000-000000099001");

    static LiaisonOpenedPayload ouverture() {
        return new LiaisonOpenedPayload(LIAISON, INSTITUTION, "PAIEMENT_MOBILE", UUID.randomUUID(), 1, UUID.randomUUID(), null, List.of("TRANSVERSE", "PAIEMENT_MOBILE"), UUID.randomUUID(), "PAIEMENT_MOBILE").avecEtat("99001", 1);
    }

    static LiaisonStatusChangedPayload changement(String statut, String avant, long version) {
        return new LiaisonStatusChangedPayload(LIAISON, INSTITUTION, "99001", "PAIEMENT_MOBILE", statut, avant, statut.equals("OUVERTE") ? "OUVERTE" : statut, version, Instant.parse("2026-10-22T08:00:00Z"));
    }

    static JsonNode json(Object o) {
        return JSON.valueToTree(o);
    }

    /**
     * Projection de référence d'un consommateur de la sandbox (SBX-CAMPAGNE, CC-SBX-GIMAC-02 §6.1) : liaisons
     * proposables tenues localement, version d'état comparée, aucune référence à MS-HOMOLOGATION.
     */
    static final class ProjectionDeReference {

        record Connue(String statut, long version) {}

        final Map<String, Connue> liaisons = new HashMap<>();

        /** Vrai si l'événement change la projection ; faux s'il est acquitté sans effet. */
        boolean recevoir(String type, JsonNode d) {
            String id = d.get("liaisonId").asString();
            long version = d.get("versionEtat").asLong();
            String statut = type.equals(LiaisonOpenedPayload.TYPE) ? "OUVERTE" : d.get("statut").asString();
            Connue c = liaisons.get(id);
            if (c != null && version <= c.version()) {
                return false;
            }
            liaisons.put(id, new Connue(statut, version));
            return true;
        }

        boolean campagneOuvrable(String liaisonId) {
            Connue c = liaisons.get(liaisonId);
            return c != null && c.statut().equals("OUVERTE");
        }
    }

    @Test
    @DisplayName("Schémas : les charges sérialisées respectent les schémas publiés des deux événements")
    void schemas() {
        assertThat(SchemaContrat.charger("sandbox/homologation.liaison-opened.v1.schema.json").ecarts(json(ouverture()))).isEmpty();
        SchemaContrat changement = SchemaContrat.charger("sandbox/homologation.liaison-status-changed.v1.schema.json");
        assertThat(changement.ecarts(json(changement("OUVERTE", null, 1)))).isEmpty();
        assertThat(changement.ecarts(json(changement("GELEE", "OUVERTE", 2)))).isEmpty();
        assertThat(changement.ecarts(json(changement("CLOSE", "GELEE", 3)))).isEmpty();
        // Le contrôle refuse bien un document hors contrat.
        JsonNode faux = JSON.readTree("{\"liaisonId\":\"x\",\"statut\":\"SUSPENDUE\",\"versionEtat\":0,\"survenuLe\":\"hier\"}");
        assertThat(changement.ecarts(faux)).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Sujet : les deux événements partent sur homologation.liaison (point P14)")
    void sujet() {
        TopicResolver t = new TopicResolver();
        assertThat(t.topicFor(LiaisonOpenedPayload.TYPE)).isEqualTo("homologation.liaison");
        assertThat(t.topicFor(LiaisonStatusChangedPayload.TYPE)).isEqualTo("homologation.liaison");
    }

    @Test
    @DisplayName("CA1 nominal : après homologation.liaison-opened.v1, la liaison est proposable à l'ouverture d'une campagne")
    void ca1LiaisonProposable() {
        ProjectionDeReference p = new ProjectionDeReference();
        assertThat(p.campagneOuvrable(LIAISON.toString())).isFalse();
        assertThat(p.recevoir(LiaisonOpenedPayload.TYPE, json(ouverture()))).isTrue();
        assertThat(p.recevoir(LiaisonStatusChangedPayload.TYPE, json(changement("OUVERTE", null, 1)))).isFalse();
        assertThat(p.campagneOuvrable(LIAISON.toString())).isTrue();
    }

    @Test
    @DisplayName("CA2 alternatif : gel publié avec une version supérieure, toute nouvelle campagne est refusée")
    void ca2GelRefuseLesCampagnes() {
        ProjectionDeReference p = new ProjectionDeReference();
        p.recevoir(LiaisonOpenedPayload.TYPE, json(ouverture()));
        assertThat(p.recevoir(LiaisonStatusChangedPayload.TYPE, json(changement("GELEE", "OUVERTE", 2)))).isTrue();
        assertThat(p.campagneOuvrable(LIAISON.toString())).isFalse();
    }

    @Test
    @DisplayName("CA3 erreur : événement périmé (version inférieure à la connue), acquitté sans effet")
    void ca3EvenementPerime() {
        ProjectionDeReference p = new ProjectionDeReference();
        p.recevoir(LiaisonOpenedPayload.TYPE, json(ouverture()));
        p.recevoir(LiaisonStatusChangedPayload.TYPE, json(changement("GELEE", "OUVERTE", 2)));
        p.recevoir(LiaisonStatusChangedPayload.TYPE, json(changement("CLOSE", "GELEE", 3)));
        // Réception tardive d'une version 2 rejouée : sans effet, la liaison reste close.
        assertThat(p.recevoir(LiaisonStatusChangedPayload.TYPE, json(changement("OUVERTE", "GELEE", 2)))).isFalse();
        assertThat(p.liaisons.get(LIAISON.toString())).isEqualTo(new ProjectionDeReference.Connue("CLOSE", 3));
        // La projection de référence ne tient aucune dépendance vers MS-HOMOLOGATION : aucun appel en retour.
        assertThat(Arrays.stream(ProjectionDeReference.class.getDeclaredFields()).map(f -> f.getType().getName())).noneMatch(n -> n.startsWith("cm.gimac.des.homologation"));
    }

    @Test
    @DisplayName("Revue : aucune route d'écriture de MS-HOMOLOGATION n'admet un compte ou une permission de la sandbox")
    void aucuneEcritureDeLaSandbox() throws Exception {
        var classes = new PathMatchingResourcePatternResolver().getResources("classpath*:cm/gimac/des/homologation/web/rest/**/*.class");
        int routesEcriture = 0;
        for (var r : classes) {
            String nom = r.getURL().getPath().replaceAll(".*/cm/gimac/des/homologation/", "cm.gimac.des.homologation.").replace('/', '.').replace(".class", "");
            if (nom.contains("$")) {
                continue;
            }
            Class<?> c = Class.forName(nom);
            if (!c.isAnnotationPresent(RestController.class)) {
                continue;
            }
            for (Method m : c.getDeclaredMethods()) {
                if (!ecriture(m)) {
                    continue;
                }
                routesEcriture++;
                PreAuthorize p = m.getAnnotation(PreAuthorize.class);
                String regle = p == null ? "" : p.value();
                assertThat(regle).as(c.getSimpleName() + "." + m.getName()).doesNotContain("SBX").doesNotContain("sbx");
            }
        }
        assertThat(routesEcriture).isGreaterThan(10);
    }

    static boolean ecriture(Method m) {
        if (m.isAnnotationPresent(PostMapping.class) || m.isAnnotationPresent(PutMapping.class) || m.isAnnotationPresent(DeleteMapping.class) || m.isAnnotationPresent(PatchMapping.class)) {
            return true;
        }
        RequestMapping rm = m.getAnnotation(RequestMapping.class);
        return rm != null && Arrays.stream(rm.method()).anyMatch(x -> x != RequestMethod.GET && x != RequestMethod.HEAD);
    }
}
