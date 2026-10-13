package cm.gimac.des.participant.web.rest.lot1;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.participant.AbstractParticipantIT;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prise de contact (DES-07, POST /api/v1/institutions, servi sous /bff/des/participant/institutions) :
 * corps tels que le formulaire de la DDRM les produit pour des saisies réalistes. Avant correction,
 * la date saisie au format du champ (JJ/MM/AAAA) partait telle quelle et rendait un 400 « Failed to
 * read request » sans champ, un BIC ou un code BEAC copié avec des espaces était refusé, et la date du
 * jour saisie à Douala entre minuit et une heure était jugée future par le serveur en UTC.
 */
class PriseDeContactIT extends AbstractParticipantIT {

    static final ObjectMapper JSON = JsonMapper.builder().build();

    /** Corps du formulaire : tous les champs présents, facultatifs à null sauf surcharge. */
    static String corps(Map<String, Object> surcharges) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("nom", "Banque des Lagunes");
        c.put("sigle", null);
        c.put("pays", "GA");
        c.put("categorie", "BANQUE");
        c.put("codeIdentification", null);
        c.put("codeBanqueBeac", null);
        c.put("datePriseDeContact", null);
        c.put("originePriseDeContact", null);
        c.put("projetEnvisage", null);
        c.putAll(surcharges);
        return JSON.writeValueAsString(c);
    }

    ResultActions enregistrer(Map<String, Object> surcharges) throws Exception {
        try {
            return mvc.perform(
                post("/api/v1/institutions")
                    .with(utilisateur(UUID.randomUUID(), "PAR_INSTITUTION_CREATE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corps(surcharges))
            );
        } finally {
            // Chaque cas réutilise les mêmes codes : la contrainte d'unicité ne doit pas s'en mêler.
            effacer(jdbc);
        }
    }

    void refuse(Map<String, Object> surcharges, String champ) throws Exception {
        enregistrer(surcharges)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.validation"))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value(champ));
    }

    static Map<String, Object> m(String k, Object v) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put(k, v);
        return r;
    }

    @Test
    @DisplayName("BIC à 8 et 11 caractères, saisi en minuscules ou avec des espaces : accepté et normalisé")
    void codeIdentification() throws Exception {
        enregistrer(m("codeIdentification", "BDLGGALI")).andExpect(status().isCreated()).andExpect(jsonPath("$.codeIdentification").value("BDLGGALI"));
        enregistrer(m("codeIdentification", "BDLGGALIXXX")).andExpect(status().isCreated()).andExpect(jsonPath("$.codeIdentification").value("BDLGGALIXXX"));
        enregistrer(m("codeIdentification", "bdlggali")).andExpect(status().isCreated()).andExpect(jsonPath("$.codeIdentification").value("BDLGGALI"));
        enregistrer(m("codeIdentification", " BDLG GA LI XXX ")).andExpect(status().isCreated()).andExpect(jsonPath("$.codeIdentification").value("BDLGGALIXXX"));
        refuse(m("codeIdentification", "BDLGGALIX"), "codeIdentification");
        refuse(m("codeIdentification", "BD1GGALI"), "codeIdentification");
    }

    @Test
    @DisplayName("Code banque BEAC : espaces retirés ; tiret et longueur refusés sur le champ")
    void codeBanqueBeac() throws Exception {
        enregistrer(m("codeBanqueBeac", "10005 001")).andExpect(status().isCreated()).andExpect(jsonPath("$.codeBanqueBeac").value("10005001"));
        enregistrer(m("codeBanqueBeac", "cm001")).andExpect(status().isCreated()).andExpect(jsonPath("$.codeBanqueBeac").value("CM001"));
        refuse(m("codeBanqueBeac", "10005-001"), "codeBanqueBeac");
        refuse(m("codeBanqueBeac", "12345678901"), "codeBanqueBeac");
    }

    @Test
    @DisplayName("Champs facultatifs vides : ramenés à l'absence de valeur, date par défaut au jour de Douala")
    void champsVides() throws Exception {
        Map<String, Object> vides = new LinkedHashMap<>();
        vides.put("sigle", "");
        vides.put("codeIdentification", "   ");
        vides.put("codeBanqueBeac", "");
        vides.put("originePriseDeContact", " ");
        vides.put("projetEnvisage", "");
        enregistrer(vides)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sigle").value(nullValue()))
            .andExpect(jsonPath("$.codeIdentification").value(nullValue()))
            .andExpect(jsonPath("$.codeBanqueBeac").value(nullValue()))
            .andExpect(jsonPath("$.datePriseDeContact").value(LocalDate.now(JourPasseOuPresent.FUSEAU).toString()));
    }

    @Test
    @DisplayName("Date de prise de contact : jour de Douala accepté, lendemain refusé, saisie JJ/MM/AAAA refusée sur le champ")
    void datePriseDeContact() throws Exception {
        LocalDate douala = LocalDate.now(JourPasseOuPresent.FUSEAU);
        enregistrer(m("datePriseDeContact", douala.toString())).andExpect(status().isCreated()).andExpect(jsonPath("$.datePriseDeContact").value(douala.toString()));
        enregistrer(m("datePriseDeContact", "2026-01-12")).andExpect(status().isCreated());
        refuse(m("datePriseDeContact", douala.plusDays(1).toString()), "datePriseDeContact");
        // Format du champ de saisie, envoyé tel quel avant correction : 400 « Failed to read request » sans champ.
        refuse(m("datePriseDeContact", "06/10/2026"), "datePriseDeContact");
    }

    @Test
    @DisplayName("Sigle, origine et projet : longueurs du service")
    void longueurs() throws Exception {
        Map<String, Object> complet = new LinkedHashMap<>();
        complet.put("sigle", "BDL");
        complet.put("originePriseDeContact", "o".repeat(255));
        complet.put("projetEnvisage", "p".repeat(1000));
        enregistrer(complet).andExpect(status().isCreated()).andExpect(jsonPath("$.sigle").value("BDL"));
        refuse(m("sigle", "S".repeat(51)), "sigle");
        refuse(m("originePriseDeContact", "o".repeat(256)), "originePriseDeContact");
        refuse(m("projetEnvisage", "p".repeat(1001)), "projetEnvisage");
    }

    @Test
    @DisplayName("D-104 : filiales d'un même groupe, chacune institution à part entière ; filtre par groupe et liste des groupes")
    void groupe() throws Exception {
        try {
            // Nom, pays, groupe saisi, groupe retenu (espaces réduits).
            String[][] filiales = {
                { "Afriland First Bank Cameroun", "CM", " Groupe  Afriland ", "Groupe Afriland" },
                { "Afriland First Bank Guinée équatoriale", "GQ", "Groupe Afriland", "Groupe Afriland" },
                { "BGFIBank Gabon", "GA", "Groupe BGFI", "Groupe BGFI" },
                { "Banque isolée", "TD", "  ", null },
            };
            for (String[] f : filiales) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("nom", f[0]);
                s.put("pays", f[1]);
                s.put("groupe", f[2]);
                mvc
                    .perform(post("/api/v1/institutions").with(utilisateur(UUID.randomUUID(), "PAR_INSTITUTION_CREATE")).contentType(MediaType.APPLICATION_JSON).content(corps(s)))
                    .andExpect(status().isCreated())
                    .andExpect(f[3] == null ? jsonPath("$.groupe").value(nullValue()) : jsonPath("$.groupe").value(f[3]));
            }
            var lecteur = utilisateur(UUID.randomUUID(), "PAR_INSTITUTION_READ");
            mvc
                .perform(get("/api/v1/institutions").param("groupe", "groupe afriland").with(lecteur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.contenu[*].groupe", everyItem(org.hamcrest.Matchers.is("Groupe Afriland"))));
            mvc.perform(get("/api/v1/institutions").param("q", "bgfi").with(lecteur)).andExpect(jsonPath("$.total").value(1));
            mvc
                .perform(get("/api/v1/institutions/groupes").with(lecteur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("Groupe Afriland", "Groupe BGFI")));
        } finally {
            effacer(jdbc);
        }
        refuse(m("groupe", "g".repeat(101)), "groupe");
    }

    @Test
    @DisplayName("Nom, pays et catégorie : obligatoires, valeurs du référentiel")
    void obligatoires() throws Exception {
        refuse(m("nom", " "), "nom");
        refuse(m("pays", "FR"), "pays");
        refuse(m("categorie", "INCONNUE"), "categorie");
    }
}
