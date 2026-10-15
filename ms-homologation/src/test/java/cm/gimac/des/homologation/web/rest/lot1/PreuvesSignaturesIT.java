package cm.gimac.des.homologation.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.config.liquibase.RepriseReferentielPreuvesCrV17;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.service.PreuvesSignaturesCrV17;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.DocumentStubAdapter;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Reprise D-109 (RepriseReferentielPreuvesCrV17) et exigence des preuves de bout en bout : version publiée par type,
 * liaisons rattachées sans changement d'état d'exécution, opération en validation sans nouvelle exigence, opération
 * reprise affichée « reprise, pièce non numérisée » ; puis une liaison carte conduite jusqu'au suivi en déposant chaque
 * preuve attendue (une par service au grain du service), en validant et en recueillant chaque signature dans l'ordre ;
 * pièce téléchargée avec son empreinte, chronologie réelle.
 */
class PreuvesSignaturesIT extends ParcoursITSupport {

    @Autowired
    DocumentStubAdapter documents;

    Map<TypeProjet, UUID> reprendre(TypeProjet type) {
        return jdbc.execute(
            (ConnectionCallback<Map<TypeProjet, UUID>>) c -> {
                Map<TypeProjet, UUID> r = RepriseReferentielPreuvesCrV17.executer(c, Instant.parse("2026-10-15T00:00:00Z"), Set.of(type));
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return r;
            }
        );
    }

    void modifier(String... sql) {
        jdbc.execute(
            (ConnectionCallback<Void>) c -> {
                try (var st = c.createStatement()) {
                    for (String q : sql) {
                        st.executeUpdate(q);
                    }
                }
                if (!c.getAutoCommit()) {
                    c.commit();
                }
                return null;
            }
        );
    }

    static Map<String, Object> livrable(Map<String, Object> op, String code) {
        List<Map<String, Object>> l = JsonPath.read(op, "$.livrablesAttendus[?(@.code == '" + code + "')]");
        return l.get(0);
    }

    @Test
    @DisplayName("Reprise D-109 : version publiée, liaison rattachée, états inchangés, validation en cours préservée, reprise sans pièce, idempotente")
    void reprise() throws Exception {
        UUID ep = adherent("Établissement Preuves", "ETABLISSEMENT_PAIEMENT", "RETRAIT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        UUID ancienne = jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison);
        modifier(
            "update parcours_operation set etat = 'EN_VALIDATION', preuve = '{\"documentRef\": \"DOC-3\", \"nom\": \"perimetre.pdf\", \"empreinte\": \"" + SHA + "\", \"deposeLe\": \"2026-10-10\", \"deposePar\": \"Chargé DDRM\"}'::jsonb where liaison_id = '" + liaison + "' and rang = '3'",
            "update parcours_operation set etat = 'FAITE', faite_le = '2026-10-11' where liaison_id = '" + liaison + "' and rang = '4'",
            "update parcours_operation set etat = 'FAITE', faite_le = '2026-10-11', preuve = '{\"reprise\": true, \"commentaire\": \"reprise, pièce non numérisée\", \"deposeLe\": \"2026-09-01\"}'::jsonb where liaison_id = '" + liaison + "' and rang = '5'"
        );
        var etats = jdbc.queryForList("select rang, etat, echeance, faite_le, preuve::text from parcours_operation where liaison_id = ?::uuid order by rang", liaison);
        int numeroAvant = jdbc.queryForObject("select max(numero) from referential_version where type_projet = 'CARTE_INTERBANCAIRE'", Integer.class);

        UUID v = reprendre(TypeProjet.CARTE_INTERBANCAIRE).get(TypeProjet.CARTE_INTERBANCAIRE);

        assertThat(jdbc.queryForObject("select etat from referential_version where id = ?", String.class, ancienne)).isEqualTo("RETIREE");
        assertThat(jdbc.queryForMap("select etat, numero, libelle from referential_version where id = ?", v))
            .containsEntry("etat", "PUBLIEE")
            .containsEntry("numero", numeroAvant + 1)
            .containsEntry("libelle", PreuvesSignaturesCrV17.libelleVersion());
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ?", Integer.class, v)).isEqualTo(43);
        assertThat(jdbc.queryForObject("select count(*) from reference_operation where version_id = ? and jsonb_array_length(livrables) = 0", Integer.class, v)).isZero();
        assertThat(jdbc.queryForObject("select livrables::text from reference_operation where version_id = ? and code = '13'", String.class, v)).contains("ping");
        assertThat(jdbc.queryForObject("select preuve_attendue ->> 'description' from reference_operation where version_id = ? and code = 'C10'", String.class, v)).contains("XML");
        assertThat(jdbc.queryForObject("select delai_max_jours from reference_operation where version_id = ? and code = '7'", Integer.class, v)).isEqualTo(10);
        assertThat(jdbc.queryForObject("select signatures::text from reference_operation where version_id = ? and code = '19'", String.class, v)).contains("\"GIMAC\"").contains("CHEF_DES");
        assertThat(jdbc.queryForObject("select count(*) from referential_etape where version_id = ?", Integer.class, v)).isEqualTo(11);
        assertThat(jdbc.queryForObject("select version_id from liaison where id = ?::uuid", UUID.class, liaison)).isEqualTo(v);
        assertThat(jdbc.queryForObject("select count(*) from parcours_operation where liaison_id = ?::uuid and version_id <> ?", Integer.class, liaison, v)).isZero();
        // Aucun état d'exécution ne change (état, échéance, date, preuve).
        assertThat(jdbc.queryForList("select rang, etat, echeance, faite_le, preuve::text from parcours_operation where liaison_id = ?::uuid order by rang", liaison)).isEqualTo(etats);
        // Ouverte : modèle complet, V passé à la fonction à affecter ; en validation : sa preuve vaut dépôt repris.
        Map<String, Object> o3 = jdbc.queryForMap("select depots::text as d, raci -> 'V' ->> 'id' as v, delai_max_jours as m from parcours_operation where liaison_id = ?::uuid and rang = '3'", liaison);
        assertThat((String) o3.get("d")).contains("\"livrable\": \"L1\"").contains("DOC-3").contains("\"repris\": true");
        assertThat(o3).containsEntry("v", "A_AFFECTER:CHEF_DDRM").containsEntry("m", 10);
        // Faite : livrables pour l'affichage, aucune signature exigée.
        assertThat(jdbc.queryForMap("select livrables::text as l, signatures::text as s from parcours_operation where liaison_id = ?::uuid and rang = '5'", liaison))
            .containsEntry("s", null)
            .satisfies(m -> assertThat((String) m.get("l")).contains("Grille de frais"));
        // Vues : en validation (déposée), faite avant l'exigence (non exigée), reprise (sans pièce), verrouillée (attendue).
        assertThat(livrable(operation(liaison, "3"), "L1")).containsEntry("etat", "DEPOSEE");
        assertThat(livrable(operation(liaison, "4"), "L1")).containsEntry("etat", "NON_EXIGEE");
        assertThat(livrable(operation(liaison, "5"), "L1")).containsEntry("etat", "REPRISE_SANS_PIECE");
        assertThat(operation(liaison, "5")).containsEntry("reprise", true);
        assertThat(livrable(operation(liaison, "13"), "L1")).containsEntry("etat", "ATTENDUE").containsEntry("grain", "OPERATION");
        assertThat((String) livrable(operation(liaison, "13"), "L1").get("consigne")).contains("Valeurs sensibles masquées");
        // La validation en cours n'est pas bloquée par la nouvelle exigence.
        mvc
            .perform(post("/api/v1/parcours/operations/" + opId(liaison, "3") + "/validation").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(porteur(Fonction.CHEF_DDRM, null)))
            .andExpect(status().isNoContent());
        assertThat(etat(liaison, "3")).isEqualTo("EN_SIGNATURE");

        assertThat(reprendre(TypeProjet.CARTE_INTERBANCAIRE)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from referential_version where libelle = ?", Integer.class, PreuvesSignaturesCrV17.libelleVersion())).isEqualTo(1);
    }

    @Test
    @DisplayName("Parcours mobile et délégataire complets sur la version D-109 : chaque preuve exigée, signatures dans l'ordre")
    void autresTypes() throws Exception {
        for (TypeProjet t : List.of(TypeProjet.PAIEMENT_MOBILE, TypeProjet.DELEGATAIRE)) {
            UUID ep = adherent("Participant " + t.name(), "ETABLISSEMENT_PAIEMENT", "RETRAIT", "TRANSFERT");
            String liaison = ouvrir(ep, t);
            reprendre(t);
            List<String> signatures = new ArrayList<>();
            List<String> parService = new ArrayList<>();
            conduire(liaison, ep, signatures, parService);
            assertThat(operations(liaison)).hasSize(t == TypeProjet.DELEGATAIRE ? 67 : 49).allSatisfy(o -> assertThat(o.get("etat")).isEqualTo("FAITE"));
            assertThat(signatures).contains("3/VISA_PERIMETRE", "7/VISA_DDRM", "15/SIGNATURE_SNA", "31/VISA_CHEF_DES");
            assertThat(parService).contains("30/RETRAIT", "30/TRANSFERT");
            if (t == TypeProjet.DELEGATAIRE) {
                assertThat(signatures).contains("5/VISA_TARIFS");
                assertThat(parService).contains("16/RETRAIT");
            } else {
                assertThat(parService).doesNotContain("16/RETRAIT");
            }
        }
    }

    @Test
    @DisplayName("Parcours carte complet sur la version D-109 : chaque preuve exigée au bon moment, signatures dans l'ordre, pièce et chronologie")
    void parcoursComplet() throws Exception {
        UUID ep = adherent("Banque Preuves", "ETABLISSEMENT_PAIEMENT", "RETRAIT", "TRANSFERT");
        String liaison = ouvrir(ep, TypeProjet.CARTE_INTERBANCAIRE);
        reprendre(TypeProjet.CARTE_INTERBANCAIRE);

        // Rang 3 : la validation est refusée tant que la fiche de périmètre manque ; une pièce libre est refusée.
        String o3 = opId(liaison, "3");
        mvc
            .perform(post("/api/v1/parcours/operations/" + o3 + "/livrables/L9/sans-objet").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Sans objet\"}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("HOM_LIVRABLE_INCONNU"));
        List<String> signatures = new ArrayList<>();
        List<String> parService = new ArrayList<>();
        conduire(liaison, ep, signatures, parService);

        assertThat(operations(liaison)).allSatisfy(o -> assertThat(o.get("etat")).isEqualTo("FAITE"));
        assertThat(signatures).contains("3/VISA_PERIMETRE", "7/VISA_DDRM", "7/VISA_DES", "15/SIGNATURE_SNA", "15/VISA_CHEF_DES", "31/SIGNATURE_PRODUCTION", "31/VISA_CHEF_DES");
        assertThat(signatures.indexOf("7/VISA_DDRM")).isLessThan(signatures.indexOf("7/VISA_DES"));
        assertThat(parService).containsExactlyInAnyOrder("16/RETRAIT", "16/TRANSFERT", "30/RETRAIT", "30/TRANSFERT");
        // Livrables validés ; pièce automatique du procès-verbal ; signature du GIMAC reprise de MS-CERTIFICATION.
        assertThat(livrable(operation(liaison, "13"), "L1")).containsEntry("etat", "VALIDEE");
        assertThat(livrable(operation(liaison, "18"), "L1")).containsEntry("etat", "VALIDEE");
        assertThat((List<String>) JsonPath.read(operation(liaison, "19"), "$.signatures[*].etat")).containsExactly("SIGNEE");
        assertThat((List<Object>) JsonPath.read(operation(liaison, "16"), "$.livrablesAttendus[0].services[*].etat")).containsExactly("VALIDEE", "VALIDEE");

        // Pièce : déposée au registre, rattachée au rang 4, téléchargée avec son empreinte ; une autre est refusée.
        byte[] contenu = "capture d'écran".getBytes(StandardCharsets.UTF_8);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        UUID doc = UUID.fromString(jdbc.queryForObject("select depots -> 0 ->> 'documentRef' from parcours_operation where liaison_id = ?::uuid and rang = '13'", String.class, liaison));
        documents.retenir(doc, contenu, "ping.png", "image/png");
        modifier("update parcours_operation set depots = jsonb_set(depots, '{0,empreinte}', '\"" + sha + "\"') where liaison_id = '" + liaison + "' and rang = '13'");
        mvc
            .perform(get("/api/v1/parcours/operations/" + opId(liaison, "13") + "/pieces/" + doc).with(conducteur()))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Empreinte-Sha256", sha));
        mvc.perform(get("/api/v1/parcours/operations/" + opId(liaison, "13") + "/pieces/" + UUID.randomUUID()).with(conducteur())).andExpect(status().isNotFound());

        String chrono = lire("/api/v1/liaisons/" + liaison + "/chronologie", conducteur());
        assertThat((List<String>) JsonPath.read(chrono, "$[*].type")).contains("DEPOT", "SIGNATURE", "FAITE", "PIECE_AUTOMATIQUE");
    }

    /** Porteur d'une fonction : la personne désignée, sinon un compte qui détient un profil de la fonction. */
    static RequestPostProcessor porteur(Fonction f, String id) {
        List<String> perms = new ArrayList<>(List.of(COMMUN));
        perms.addAll(f.profils());
        perms.addAll(List.of("HOM_OPERATION_VALIDATE", "HOM_EVIDENCE_SUBMIT"));
        UUID compte = id == null || id.startsWith("A_AFFECTER:") || id.startsWith("EXT-") ? UUID.randomUUID() : UUID.fromString(id);
        if (CollaboratorStubAdapter.CONDUCTEUR_1.equals(compte)) {
            return conducteur();
        }
        return utilisateur(compte, perms.toArray(String[]::new));
    }

    /** Conduit la liaison jusqu'au bout : chaque livrable obligatoire déposé, validation, signatures dans l'ordre. */
    @SuppressWarnings("unchecked")
    void conduire(String liaison, UUID institution, List<String> signatures, List<String> parService) throws Exception {
        for (int garde = 0; garde < 300; garde++) {
            Map<String, Object> o = operations(liaison).stream().filter(x -> !"FAITE".equals(x.get("etat"))).findFirst().orElse(null);
            if (o == null) {
                return;
            }
            String rang = (String) o.get("rang");
            String id = (String) o.get("id");
            String etatOp = (String) o.get("etat");
            if ("EN_SIGNATURE".equals(etatOp)) {
                List<Map<String, Object>> suivantes = JsonPath.read(o, "$.signatures[?(@.suivante == true)]");
                assertThat(suivantes).as("signature suivante de " + rang).isNotEmpty();
                for (Map<String, Object> s : suivantes) {
                    String attendu = s.get("attendu") == null ? null : (String) ((Map<String, Object>) s.get("attendu")).get("id");
                    mvc
                        .perform(post("/api/v1/liaisons/" + liaison + "/operations/" + rang + "/signatures/" + s.get("code")).header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(porteur(Fonction.valueOf((String) s.get("signataire")), attendu)))
                        .andExpect(status().isNoContent());
                    signatures.add(rang + "/" + s.get("code"));
                }
                continue;
            }
            if ("EN_VALIDATION".equals(etatOp)) {
                Map<String, Object> v = (Map<String, Object>) ((Map<String, Object>) o.get("raci")).get("V");
                mvc
                    .perform(post("/api/v1/parcours/operations/" + id + "/validation").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(porteur(Fonction.valueOf((String) v.get("fonction")), (String) v.get("id"))))
                    .andExpect(status().isNoContent());
                continue;
            }
            switch (rang) {
                case "6" -> mvc
                    .perform(put("/api/v1/liaisons/" + liaison + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[{\"fonction\":\"SNA\",\"personneId\":\"" + SNA + "\"}]}"))
                    .andExpect(status().isNoContent());
                case "12" -> mvc
                    .perform(put("/api/v1/portail/liaisons/" + liaison + "/coordonnees-techniques").with(correspondant(institution)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"essai.exemple\",\"port\":\"8443\"}"))
                    .andExpect(status().isNoContent());
                case "17" -> mvc.perform(post("/api/v1/liaisons/" + liaison + "/couverture/constat").with(sna())).andExpect(status().isNoContent());
                case "18" -> evenement("certification.pv-established.v1", liaison, "\"etabliPar\":\"Conducteur\",\"empreinte\":\"" + SHA + "\"");
                case "19" -> evenement("certification.pv-signed.v1", liaison, "\"signatures\":[{\"fonction\":\"CHEF_DES\",\"signataire\":\"Chef DES\",\"signature\":\"GIMAC\",\"signeLe\":\"2026-10-15T09:00:00Z\"},{\"fonction\":\"PARTICIPANT\",\"signataire\":\"Représentant\",\"signature\":\"PARTICIPANT\"}],\"empreinte\":\"" + SHA + "\"");
                case "20" -> evenement("certification.change-record-constituted.v1", liaison, "\"documentRef\":\"CHG-DOS\",\"nom\":\"dossier.pdf\",\"empreinte\":\"" + SHA + "\"");
                case "21", "22" -> evenement("certification.attestations-archived.v1", liaison, "\"attestations\":[{\"type\":\"PCI_DSS\",\"documentRef\":\"ATT-1\"}]");
                case "23", "24", "25" -> evenement("certification.security-approved.v1", liaison, "\"approuveeLe\":\"2026-10-06\"");
                case "26" -> evenement("certification.change-record-referenced.v1", liaison, "\"reference\":\"CHG-2026-0412\"");
                case "32" -> {
                    mvc.perform(post("/api/v1/liaisons/" + liaison + "/fiche-suivi/remise").with(conducteur())).andExpect(status().isNoContent());
                    List<String> codes = JsonPath.read(lire("/api/v1/portail/liaisons/" + liaison + "/fiche-suivi", correspondant(institution)), "$.lignes[*].serviceCode");
                    String lignes = String.join(",", codes.stream().map(c -> "{\"serviceCode\":\"" + c + "\",\"ouvert\":true,\"commentaire\":null}").toList());
                    mvc
                        .perform(put("/api/v1/portail/liaisons/" + liaison + "/fiche-suivi").with(correspondant(institution)).contentType(MediaType.APPLICATION_JSON).content("{\"lignes\":[" + lignes + "]}"))
                        .andExpect(status().isNoContent());
                }
                case "33" -> mvc.perform(post("/api/v1/liaisons/" + liaison + "/fiche-suivi/constat").with(sna())).andExpect(status().isNoContent());
                default -> deposerTout(o, liaison, parService);
            }
        }
        Map<String, Object> o = operations(liaison).stream().filter(x -> !"FAITE".equals(x.get("etat"))).findFirst().orElseThrow();
        throw new AssertionError("Parcours bloqué au rang " + o.get("rang") + " (" + o.get("etat") + ") : " + o.get("livrablesAttendus") + " " + o.get("signatures") + " " + o.get("raci"));
    }

    /** Dépose chaque livrable obligatoire encore attendu (un par service au grain du service). */
    @SuppressWarnings("unchecked")
    void deposerTout(Map<String, Object> o, String liaison, List<String> parService) throws Exception {
        String rang = (String) o.get("rang");
        String id = (String) o.get("id");
        boolean livrable = "DEPOSER_LIVRABLE".equals(o.get("geste"));
        String complement = "C1".equals(rang)
            ? "\"ceremonie\":{\"tenueLe\":\"2026-10-01\",\"porteurs\":[\"RSSI\",\"Dépositaire 2\",\"Dépositaire 3\"],\"codesControle\":[\"A1B2C3\",\"0F0F0F\",\"ABCD\"]}"
            : Boolean.TRUE.equals(o.get("cle")) ? "\"codeControle\":\"A1B2C3\"" : Boolean.TRUE.equals(o.get("valeurReference")) ? "\"valeurReference\":\"REF-" + rang + "\"" : "";
        List<Map<String, Object>> attendus = (List<Map<String, Object>>) o.get("livrablesAttendus");
        assertThat(attendus).as("livrables attendus de " + rang).isNotEmpty();
        for (Map<String, Object> l : attendus) {
            // Opération sans livrable obligatoire (rang 35) : sa pièce facultative la fait avancer.
            boolean seule = attendus.stream().noneMatch(x -> Boolean.TRUE.equals(x.get("obligatoire")));
            if ((!seule && !Boolean.TRUE.equals(l.get("obligatoire"))) || !List.of("ATTENDUE", "REFUSEE").contains(l.get("etat"))) {
                continue;
            }
            List<String> services = "SERVICE".equals(l.get("grain"))
                ? ((List<Map<String, Object>>) l.get("services")).stream().filter(s -> "ATTENDUE".equals(s.get("etat"))).map(s -> (String) s.get("serviceCode")).toList()
                : List.of("");
            for (String s : services) {
                String service = s.isEmpty() ? null : s;
                UUID doc = UUID.randomUUID();
                List<String> types = (List<String>) l.get("typesPieces");
                documents.typer(doc, types == null || types.isEmpty() ? "application/pdf" : types.get(0));
                String corps = "{\"documentId\":\"" + doc + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"" + rang + ".pdf\",\"livrable\":\"" + l.get("code") + "\"" +
                    (service == null ? "" : ",\"service\":\"" + service + "\"") + (complement.isEmpty() || livrable ? "" : "," + complement) + "}";
                if (service != null) {
                    parService.add(rang + "/" + service);
                }
                mvc
                    .perform(post("/api/v1/parcours/operations/" + id + (livrable ? "/livrable" : "/preuves")).with(conducteur()).contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isNoContent());
            }
            if (!livrable) {
                return;
            }
        }
    }
}
