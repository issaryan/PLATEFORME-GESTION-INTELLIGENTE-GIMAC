package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.document.application.usecase.ApercuBureautiqueService;
import cm.gimac.des.document.application.usecase.ApercuProprietes;
import cm.gimac.des.document.config.SecurityConfiguration;
import cm.gimac.des.document.config.TestSecurityConfiguration;
import cm.gimac.des.document.infrastructure.apercu.ConvertisseurApercuGotenberg;
import cm.gimac.des.document.web.rest.errors.DocumentExceptionHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.jhipster.config.JHipsterProperties;

/**
 * Route d'aperçu bureautique (D-83, apercu-bureautique.md §2) avec la vraie chaîne de filtres, le vrai cas
 * d'utilisation et le vrai adaptateur Gotenberg, relié à un faux moteur HTTP local : sécurité (jeton à deux
 * facteurs, PIL_ACTIVITY_OWN, compte interne), réponse PDF en ligne, et codes d'erreur du contrat en
 * application/problem+json. Exécutée sans Docker.
 */
@WebMvcTest(controllers = ApercuResource.class)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, DocumentExceptionHandler.class, ApercuResourceTest.Moteur.class })
@EnableConfigurationProperties(JHipsterProperties.class)
class ApercuResourceTest {

    static final String ROUTE = "/api/v1/apercus";
    static final byte[] PDF = "%PDF-1.7 apercu".getBytes(StandardCharsets.US_ASCII);

    /** Faux moteur : 200 et un PDF, ou le statut porté par le contenu (« statut=NNN »). */
    static final HttpServer MOTEUR;
    static final List<String> REQUETES = new CopyOnWriteArrayList<>();

    static {
        try {
            MOTEUR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MOTEUR.createContext("/forms/libreoffice/convert", e -> {
            String corps = new String(e.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            REQUETES.add(corps);
            int i = corps.indexOf("statut=");
            int statut = i < 0 ? 200 : Integer.parseInt(corps.substring(i + 7, i + 10));
            byte[] r = statut == 200 ? PDF : "refus".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(statut, r.length);
            e.getResponseBody().write(r);
            e.close();
        });
        MOTEUR.start();
    }

    @TestConfiguration
    static class Moteur {

        @Bean
        ApercuProprietes apercuProprietes() {
            return new ApercuProprietes("http://127.0.0.1:" + MOTEUR.getAddress().getPort(), null, null, null, null, null, null);
        }

        @Bean
        ConvertisseurApercuGotenberg convertisseur(ApercuProprietes p) {
            return new ConvertisseurApercuGotenberg(p);
        }

        @Bean
        ApercuBureautiqueService apercuService(ConvertisseurApercuGotenberg c, ApercuProprietes p) {
            return new ApercuBureautiqueService(c, p);
        }
    }

    @AfterAll
    static void arreter() {
        MOTEUR.stop(0);
    }

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void vider() {
        REQUETES.clear();
    }

    static RequestPostProcessor interne(String sujet, String acr, String... permissions) {
        return jwt()
            .jwt(j -> {
                j.subject(sujet);
                if (acr != null) {
                    j.claim("acr", acr);
                }
            })
            .authorities(Stream.of(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    static RequestPostProcessor agent() {
        return interne("agent-" + UUID.randomUUID(), "2", "PIL_ACTIVITY_OWN");
    }

    /** docx de contenu unique (signature ZIP), pour ne pas être servi par le cache d'un autre test. */
    static byte[] docx(String texte) {
        return ("PK\u0003\u0004" + texte + " " + UUID.randomUUID()).getBytes(StandardCharsets.ISO_8859_1);
    }

    static MockHttpServletRequestBuilder apercu(String nom, byte[] contenu) {
        MockHttpServletRequestBuilder b = post(ROUTE).contentType(MediaType.APPLICATION_OCTET_STREAM).content(contenu).accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_PROBLEM_JSON);
        return nom == null ? b : b.header(ApercuResource.ENTETE_NOM, URLEncoder.encode(nom, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    @Test
    @DisplayName("200 : PDF en ligne, cache privé d'une heure ; le moteur ne reçoit que document.<extension>")
    void apercuPdf() throws Exception {
        mvc
            .perform(apercu("Rapport d'activité (version 2).DOCX", docx("x")).with(agent()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"apercu.pdf\""))
            .andExpect(header().string("Cache-Control", "max-age=3600, private"))
            .andExpect(content().bytes(PDF));
        assertThat(REQUETES).hasSize(1);
        assertThat(REQUETES.get(0)).contains("filename=\"document.docx\"").doesNotContain("Rapport");
    }

    @Test
    @DisplayName("Type du fichier transmis tel quel (pptx) : accepté")
    void typeDuFichier() throws Exception {
        mvc
            .perform(
                post(ROUTE)
                    .contentType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                    .header(ApercuResource.ENTETE_NOM, "diapos.pptx")
                    .content(docx("p"))
                    .with(agent())
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("401 sans jeton")
    void sansJeton() throws Exception {
        mvc.perform(apercu("a.docx", docx("x"))).andExpect(status().isUnauthorized());
        assertThat(REQUETES).isEmpty();
    }

    @Test
    @DisplayName("403 sans second facteur, sans PIL_ACTIVITY_OWN, ou pour un compte d'institution (DOC_APERCU_RESERVE_INTERNE)")
    void refus() throws Exception {
        mvc.perform(apercu("a.docx", docx("x")).with(interne("u1", null, "PIL_ACTIVITY_OWN"))).andExpect(status().isForbidden());
        mvc.perform(apercu("a.docx", docx("x")).with(interne("u1", "2", "COL_MESSAGE_READ"))).andExpect(status().isForbidden());
        mvc
            .perform(
                apercu("a.docx", docx("x")).with(
                    jwt().jwt(j -> j.subject("u2").claim("acr", "2").claim("institution_id", "inst-1")).authorities(new SimpleGrantedAuthority("PIL_ACTIVITY_OWN"))
                )
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DOC_APERCU_RESERVE_INTERNE"));
        assertThat(REQUETES).isEmpty();
    }

    @Test
    @DisplayName("GET : aucune route de lecture")
    void lecture() throws Exception {
        mvc.perform(get(ROUTE).with(agent())).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("415 DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE en problem+json (même si le client n'accepte que le PDF) : extension, nom absent, signature")
    void format() throws Exception {
        for (MockHttpServletRequestBuilder r : List.of(
            apercu("a.pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
            apercu(null, docx("x")),
            apercu("a.doc", docx("x")),
            post(ROUTE).contentType(MediaType.APPLICATION_OCTET_STREAM).header(ApercuResource.ENTETE_NOM, "a.docx").content("MZ exécutable".getBytes(StandardCharsets.UTF_8)).accept(MediaType.APPLICATION_PDF)
        )) {
            mvc
                .perform(r.with(agent()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE"));
        }
        assertThat(REQUETES).isEmpty();
    }

    @Test
    @DisplayName("413 DOC_APERCU_TROP_VOLUMINEUX au-delà de 20 Mo")
    void tropVolumineux() throws Exception {
        byte[] gros = Arrays.copyOf(docx("g"), 20 * 1024 * 1024 + 1);
        mvc
            .perform(apercu("a.docx", gros).with(agent()))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("DOC_APERCU_TROP_VOLUMINEUX"))
            .andExpect(jsonPath("$.details.tailleMax").value(20 * 1024 * 1024));
        assertThat(REQUETES).isEmpty();
    }

    @Test
    @DisplayName("422 DOC_APERCU_ILLISIBLE (refus du moteur), 503 DOC_APERCU_INDISPONIBLE (moteur saturé ou délai)")
    void erreursMoteur() throws Exception {
        mvc
            .perform(apercu("a.docx", docx("statut=400")).with(agent()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("DOC_APERCU_ILLISIBLE"));
        mvc
            .perform(apercu("a.docx", docx("statut=503")).with(agent()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("DOC_APERCU_INDISPONIBLE"));
    }

    @Test
    @DisplayName("429 DOC_APERCU_LIMITE à la 31e conversion de la minute pour un même compte")
    void limite() throws Exception {
        RequestPostProcessor compte = interne("agent-limite", "2", "PIL_ACTIVITY_OWN");
        for (int i = 0; i < 30; i++) {
            mvc.perform(apercu("a.docx", docx("n" + i)).with(compte)).andExpect(status().isOk());
        }
        mvc
            .perform(apercu("a.docx", docx("n30")).with(compte))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("DOC_APERCU_LIMITE"));
        mvc.perform(apercu("a.docx", docx("n30")).with(agent())).andExpect(status().isOk());
    }
}
