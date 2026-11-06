package cm.gimac.des.document.infrastructure.apercu;

import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
import cm.gimac.des.document.application.port.out.ConvertisseurHtml;
import cm.gimac.des.document.application.usecase.ApercuProprietes;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur du port {@link ConvertisseurHtml} vers Gotenberg 8 (Chromium sans interface) du conteneur interne
 * {@code conversion-documents} (D-102, docs/lot1/organigramme.md §4) : {@code POST /forms/chromium/convert/html},
 * multipart, parties {@code index.html} et {@code footer.html}, papier et marges en pouces, fonds imprimés. Le moteur
 * est lancé avec JavaScript désactivé et une liste d'adresses admises réduite à ses fichiers temporaires.
 *
 * <p>Réponses : 200, le PDF ; 400 ou 422 : document refusé ; 429, 5xx, délai ou connexion impossible : moteur
 * indisponible (même délais que l'aperçu bureautique, réglages {@code gimac.apercu.*}).
 */
@Component
public class ConvertisseurHtmlGotenberg implements ConvertisseurHtml {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertisseurHtmlGotenberg.class);
    static final String ROUTE = "/forms/chromium/convert/html";
    private static final double MM_PAR_POUCE = 25.4;

    private final RestClient client;

    public ConvertisseurHtmlGotenberg(ApercuProprietes proprietes) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(proprietes.delaiConnexion()).version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(http);
        fabrique.setReadTimeout(proprietes.delai());
        this.client = RestClient.builder().baseUrl(proprietes.url()).requestFactory(fabrique).build();
    }

    @Override
    public byte[] imprimer(String html, String piedDePage, Papier papier) {
        return imprimer(html, piedDePage, papier, Map.of());
    }

    /** Métadonnées du PDF (D-139) : champ {@code metadata} de Gotenberg, objet JSON des clés XMP courantes. */
    @Override
    public byte[] imprimer(String html, String piedDePage, Papier papier, Map<String, String> metadonnees) {
        MultiValueMap<String, Object> f = new LinkedMultiValueMap<>();
        f.add("files", fichier("index.html", html));
        if (piedDePage != null && !piedDePage.isBlank()) {
            f.add("files", fichier("footer.html", piedDePage));
        }
        f.add("paperWidth", pouces(papier.largeurMm()));
        f.add("paperHeight", pouces(papier.hauteurMm()));
        f.add("marginTop", pouces(papier.margeHautMm()));
        f.add("marginBottom", pouces(papier.margeBasMm()));
        f.add("marginLeft", pouces(papier.margeCoteMm()));
        f.add("marginRight", pouces(papier.margeCoteMm()));
        f.add("printBackground", "true");
        f.add("preferCssPageSize", "false");
        f.add("emulatedMediaType", "print");
        if (metadonnees != null && !metadonnees.isEmpty()) {
            f.add("metadata", json(metadonnees));
        }
        try {
            return client
                .post()
                .uri(ROUTE)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_PDF)
                .body(f)
                .exchange((requete, reponse) -> {
                    HttpStatusCode statut = reponse.getStatusCode();
                    if (statut.is2xxSuccessful()) {
                        return reponse.getBody().readAllBytes();
                    }
                    int code = statut.value();
                    LOG.warn("Moteur de mise en page : réponse {}", code);
                    if (code == 400 || code == 422) {
                        throw new ConversionRefusee("Document refusé par le moteur (" + code + ")");
                    }
                    throw new MoteurIndisponible("Moteur de mise en page indisponible (" + code + ")", null);
                });
        } catch (ConversionRefusee | MoteurIndisponible e) {
            throw e;
        } catch (ResourceAccessException e) {
            LOG.warn("Moteur de mise en page injoignable ou délai dépassé : {}", e.getMostSpecificCause().getClass().getSimpleName());
            throw new MoteurIndisponible("Moteur de mise en page injoignable", e);
        } catch (RestClientException e) {
            throw new MoteurIndisponible("Réponse inattendue du moteur de mise en page", e);
        }
    }

    private static HttpEntity<ByteArrayResource> fichier(String nom, String contenu) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
        h.setContentDisposition(ContentDisposition.formData().name("files").filename(nom).build());
        return new HttpEntity<>(new ByteArrayResource(contenu.getBytes(StandardCharsets.UTF_8)), h);
    }

    /** Objet JSON plat de chaînes, sans dépendance au sérialiseur de l'application. */
    static String json(Map<String, String> m) {
        StringBuilder b = new StringBuilder("{");
        m.forEach((k, v) -> {
            if (b.length() > 1) {
                b.append(',');
            }
            b.append(chaine(k)).append(':').append(chaine(v));
        });
        return b.append('}').toString();
    }

    private static String chaine(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }

    static String pouces(double mm) {
        return String.format(Locale.ROOT, "%.4f", mm / MM_PAR_POUCE);
    }
}
