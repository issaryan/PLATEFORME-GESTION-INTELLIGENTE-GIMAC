package cm.gimac.des.homologation.web.rest.lot1;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.homologation.AbstractHomologationIT;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.infrastructure.client.CollaboratorStubAdapter;
import cm.gimac.des.homologation.infrastructure.client.ParticipantStubAdapter;
import cm.gimac.des.homologation.infrastructure.messaging.consumer.EvenementsCertificationConsumer;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Outils des tests d'intégration du parcours (étapes 4 à 10) : ouverture d'une liaison, jetons des
 * acteurs (profil et permissions du realm, second facteur), gestes par l'API et événements de
 * MS-CERTIFICATION remis au consommateur idempotent.
 */
abstract class ParcoursITSupport extends AbstractHomologationIT {

    static final UUID CHEF_DES = UUID.fromString("00000000-0000-4000-8000-0000000d0001");
    static final UUID SNA = UUID.fromString("00000000-0000-4000-8000-0000000d0002");
    static final UUID DDRM = UUID.fromString("00000000-0000-4000-8000-0000000d0003");
    static final UUID VALIDATEUR = UUID.fromString("00000000-0000-4000-8000-0000000d0004");
    static final String SHA = "a".repeat(64);

    @Autowired
    ParticipantStubAdapter registre;

    @Autowired
    CollaboratorStubAdapter annuaire;

    @Autowired
    EvenementsCertificationConsumer consommateur;

    /** Permissions des profils (frontend/packages/lot1/src/permissions.ts). */
    static final String[] COMMUN = { "HOM_LIAISON_LIST", "HOM_LIAISON_READ", "HOM_DASHBOARD_READ", "HOM_REFERENTIEL_READ" };

    static String[] avec(String[] base, String... plus) {
        String[] r = Arrays.copyOf(base, base.length + plus.length);
        System.arraycopy(plus, 0, r, base.length, plus.length);
        return r;
    }

    static RequestPostProcessor chefDes() {
        return utilisateur(CHEF_DES, avec(COMMUN, "ROLE_CHEF_DES", "HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE"));
    }

    static RequestPostProcessor conducteur() {
        return utilisateur(CollaboratorStubAdapter.CONDUCTEUR_1, avec(COMMUN, "ROLE_CONDUCTEUR_PROJET", "HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE"));
    }

    static RequestPostProcessor sna() {
        return utilisateur(SNA, avec(COMMUN, "ROLE_SNA", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "CER_DOSSIER_VERIFY"));
    }

    static RequestPostProcessor ddrm() {
        return utilisateur(DDRM, avec(COMMUN, "ROLE_CHARGE_DDRM", "HOM_STEP_MANAGE"));
    }

    /** Validateur distinct de tout responsable : il détient HOM_OPERATION_VALIDATE. */
    static RequestPostProcessor validateur() {
        return utilisateur(VALIDATEUR, avec(COMMUN, "ROLE_CHEF_DES", "HOM_OPERATION_VALIDATE"));
    }

    static RequestPostProcessor correspondant(UUID institution) {
        return jwt()
            .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("acr", "2").claim("institution_id", institution.toString()).claim("name", "Awa Correspondante"))
            .authorities(
                Arrays.stream(new String[] { "ROLE_CORRESPONDANT", "HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT" }).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new)
            );
    }

    static RequestPostProcessor service() {
        return jwt().jwt(j -> j.subject("service-account-certification-service")).authorities(new SimpleGrantedAuthority("INTERNAL_HOMOLOGATION_READ"));
    }

    /** Adhérent enregistré au registre de substitution, avec ses services souscrits. */
    UUID adherent(String nom, String categorie, String... services) {
        UUID id = UUID.randomUUID();
        registre.enregistrer(new InstitutionView(id, nom, "ADHERENT", categorie, Arrays.stream(services).map(s -> new InstitutionView.ServiceSouscrit(s, "2026-09-01")).toList()));
        return id;
    }

    /** Liaison ouverte au cadrage par le chef du DES, conducteur CONDUCTEUR_1 ; ingénieur SNA inscrit à l'annuaire. */
    String ouvrir(UUID institution, TypeProjet type) throws Exception {
        annuaire.enregistrer(new CollaboratorView(SNA, "Ingénieur Normes et Agréments", Set.of("ROLE_SNA"), null, true));
        annuaire.enregistrer(new CollaboratorView(DDRM, "Chargé DDRM", Set.of("ROLE_CHARGE_DDRM"), null, true));
        fixtures.referentielPublie(type);
        String json = "{\"institutionId\":\"%s\",\"typeProjet\":\"%s\",\"chefDeProjetId\":\"%s\"}".formatted(institution, type, CollaboratorStubAdapter.CONDUCTEUR_1);
        String corps = mvc.perform(post("/api/v1/liaisons").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.id");
    }

    String lire(String uri, RequestPostProcessor qui) throws Exception {
        return mvc.perform(get(uri).with(qui)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    List<Map<String, Object>> operations(String liaison) throws Exception {
        return JsonPath.read(lire("/api/v1/liaisons/" + liaison + "/parcours/operations", conducteur()), "$");
    }

    Map<String, Object> operation(String liaison, String rang) throws Exception {
        return operations(liaison).stream().filter(o -> rang.equals(o.get("rang"))).findFirst().orElseThrow();
    }

    String opId(String liaison, String rang) throws Exception {
        return (String) operation(liaison, rang).get("id");
    }

    String etat(String liaison, String rang) throws Exception {
        return (String) operation(liaison, rang).get("etat");
    }

    static String piece(String nom) {
        return "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"" + nom + "\",\"commentaire\":\"relu\"}";
    }

    void livrable(String opId, RequestPostProcessor qui) throws Exception {
        mvc.perform(post("/api/v1/parcours/operations/" + opId + "/livrable").with(qui).contentType(MediaType.APPLICATION_JSON).content(piece("livrable.pdf"))).andExpect(status().isNoContent());
    }

    void preuve(String opId, String complement) throws Exception {
        String corps = "{\"documentId\":\"" + UUID.randomUUID() + "\",\"empreinte\":\"" + SHA + "\",\"nom\":\"preuve.pdf\"" + (complement.isEmpty() ? "" : "," + complement) + "}";
        mvc.perform(post("/api/v1/parcours/operations/" + opId + "/preuves").with(conducteur()).contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isNoContent());
    }

    void valider(String opId) throws Exception {
        mvc.perform(post("/api/v1/parcours/operations/" + opId + "/validation").header("X-Reauth-Assertion", "YXNzZXJ0aW9u").with(validateur())).andExpect(status().isNoContent());
    }

    /** Événement de MS-CERTIFICATION remis au consommateur, comme le ferait Kafka. */
    void evenement(String type, String liaison, String data) {
        consommateur.recevoir("{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"" + type + "\",\"data\":{\"liaisonId\":\"" + liaison + "\"" + (data.isEmpty() ? "" : "," + data) + "}}");
    }

    /**
     * Fait progresser la liaison par l'API jusqu'à l'opération {@code avant} exclue : preuve et validation
     * pour une clé ou une valeur de référence, constat pour le rang 17, événements de MS-CERTIFICATION pour
     * les verrous (rangs 18 à 26), fiche de suivi pour les rangs 32 et 33, livrable sinon.
     */
    void avancer(String liaison, String avant, UUID institution) throws Exception {
        for (int garde = 0; garde < 120; garde++) {
            Map<String, Object> o = operations(liaison).stream().filter(x -> !"FAITE".equals(x.get("etat"))).findFirst().orElse(null);
            if (o == null || avant.equals(o.get("rang"))) {
                return;
            }
            String rang = (String) o.get("rang");
            String id = (String) o.get("id");
            switch (rang) {
                case "6" -> mvc
                    .perform(put("/api/v1/liaisons/" + liaison + "/ressources").with(chefDes()).contentType(MediaType.APPLICATION_JSON).content("{\"affectations\":[{\"fonction\":\"SNA\",\"personneId\":\"" + SNA + "\"}]}"))
                    .andExpect(status().isNoContent());
                case "12" -> mvc
                    .perform(put("/api/v1/portail/liaisons/" + liaison + "/coordonnees-techniques").with(correspondant(institution)).contentType(MediaType.APPLICATION_JSON).content("{\"adresse\":\"essai.exemple\",\"port\":\"8443\"}"))
                    .andExpect(status().isNoContent());
                case "17" -> mvc.perform(post("/api/v1/liaisons/" + liaison + "/couverture/constat").with(sna())).andExpect(status().isNoContent());
                case "18" -> evenement("certification.pv-established.v1", liaison, "\"etabliPar\":\"Conducteur\",\"empreinte\":\"" + SHA + "\"");
                case "19" -> evenement("certification.pv-signed.v1", liaison, "\"signatures\":[{\"fonction\":\"CHEF_DES\",\"signataire\":\"Chef DES\"},{\"fonction\":\"PARTICIPANT\",\"signataire\":\"Représentant\"}],\"empreinte\":\"" + SHA + "\"");
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
                default -> {
                    if ("C1".equals(rang)) {
                        preuve(id, "\"ceremonie\":{\"tenueLe\":\"2026-10-01\",\"porteurs\":[\"RSSI\",\"Dépositaire 2\",\"Dépositaire 3\"],\"codesControle\":[\"A1B2C3\",\"0F0F0F\",\"ABCD\"]}");
                        valider(id);
                    } else if (Boolean.TRUE.equals(o.get("cle"))) {
                        preuve(id, "\"codeControle\":\"A1B2C3\"");
                        valider(id);
                    } else if (Boolean.TRUE.equals(o.get("valeurReference"))) {
                        preuve(id, "\"valeurReference\":\"REF-" + rang + "\"");
                        valider(id);
                    } else {
                        livrable(id, conducteur());
                    }
                }
            }
        }
        throw new AssertionError("Parcours bloqué avant " + avant);
    }
}
