package cm.gimac.des.document.infrastructure.apercu;

import cm.gimac.des.document.application.port.out.ConvertisseurApercu;
import cm.gimac.des.document.application.usecase.ApercuProprietes;
import java.net.http.HttpClient;
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
 * Adaptateur du port {@link ConvertisseurApercu} vers Gotenberg 8 (LibreOffice sans interface), conteneur
 * interne {@code conversion-documents} (D-83, docs/lot1/apercu-bureautique.md §1, §4) :
 * {@code POST /forms/libreoffice/convert}, multipart, partie {@code files} nommée {@code document.<extension>}
 * (le nom d'origine du fichier ne quitte pas MS-DOCUMENT).
 *
 * <p>Réponses du moteur : 200, le PDF ; 400 (fichier protégé par mot de passe, formulaire refusé) ou 500
 * (LibreOffice n'a pas pu ouvrir le fichier, constaté sur un fichier corrompu) : conversion refusée ; 429
 * (file d'attente pleine), 502 à 504 (délai de 60 s dépassé), délai de réponse ou connexion impossible :
 * moteur indisponible.
 */
@Component
public class ConvertisseurApercuGotenberg implements ConvertisseurApercu {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertisseurApercuGotenberg.class);
    static final String ROUTE = "/forms/libreoffice/convert";

    private final RestClient client;

    public ConvertisseurApercuGotenberg(ApercuProprietes proprietes) {
        HttpClient http = HttpClient.newBuilder()
            .connectTimeout(proprietes.delaiConnexion())
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(http);
        fabrique.setReadTimeout(proprietes.delai());
        this.client = RestClient.builder().baseUrl(proprietes.url()).requestFactory(fabrique).build();
    }

    @Override
    public byte[] convertirEnPdf(String extension, byte[] contenu) {
        HttpHeaders entetePartie = new HttpHeaders();
        entetePartie.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        entetePartie.setContentDisposition(ContentDisposition.formData().name("files").filename("document." + extension).build());
        MultiValueMap<String, Object> formulaire = new LinkedMultiValueMap<>();
        formulaire.add("files", new HttpEntity<>(new ByteArrayResource(contenu), entetePartie));
        try {
            return client
                .post()
                .uri(ROUTE)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_PDF)
                .body(formulaire)
                .exchange((requete, reponse) -> {
                    HttpStatusCode statut = reponse.getStatusCode();
                    if (statut.is2xxSuccessful()) {
                        return reponse.getBody().readAllBytes();
                    }
                    int code = statut.value();
                    LOG.warn("Moteur de conversion : réponse {} pour un fichier {}", code, extension);
                    if (code == 400 || code == 422 || code == 500) {
                        throw new ConversionRefusee("Conversion refusée par le moteur (" + code + ")");
                    }
                    throw new MoteurIndisponible("Moteur de conversion indisponible (" + code + ")", null);
                });
        } catch (ConversionRefusee | MoteurIndisponible e) {
            throw e;
        } catch (ResourceAccessException e) {
            LOG.warn("Moteur de conversion injoignable ou délai dépassé : {}", e.getMostSpecificCause().getClass().getSimpleName());
            throw new MoteurIndisponible("Moteur de conversion injoignable", e);
        } catch (RestClientException e) {
            throw new MoteurIndisponible("Réponse inattendue du moteur de conversion", e);
        }
    }
}
