package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.RenduPdfUseCase;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Demande;
import cm.gimac.des.document.application.port.in.RenduPdfUseCase.Format;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.infrastructure.security.ServiceTokenAuthorizationManager;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rendu PDF interne (D-102, docs/lot1/organigramme.md §4), réservé aux jetons de service portant
 * INTERNAL_DOCUMENT_RENDER (MS-IAM pour l'organigramme), jamais relayé par la passerelle : corps JSON
 * {@code { html, piedDePage, format: A4|A3, orientation: PAYSAGE|PORTRAIT }}, réponse : le PDF. Rien n'est conservé.
 */
@RestController
@RequestMapping("/api/v1/interne/rendus")
public class RenduInterneResource {

    /** Corps de la requête ; la taille est bornée par le cas d'utilisation (4 Mio). */
    public record RenduRequest(String html, String piedDePage, String format, String orientation) {}

    private final RenduPdfUseCase useCase;

    public RenduInterneResource(RenduPdfUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(path = "/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_RENDER')")
    public ResponseEntity<byte[]> pdf(@RequestBody RenduRequest r, Authentication auth) {
        Format format;
        try {
            format = Format.valueOf(r.format() == null ? "" : r.format().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Format de papier A4 ou A3");
        }
        String o = r.orientation() == null ? "PAYSAGE" : r.orientation().trim().toUpperCase(java.util.Locale.ROOT);
        if (!o.equals("PAYSAGE") && !o.equals("PORTRAIT")) {
            throw DocumentException.invalide("DOC_RENDU_INVALIDE", "Orientation PAYSAGE ou PORTRAIT");
        }
        String client = auth instanceof JwtAuthenticationToken jwt ? ServiceTokenAuthorizationManager.clientDeService(jwt.getToken()).orElse("?") : "?";
        byte[] pdf = useCase.rendre(new Demande(r.html(), r.piedDePage(), format, o.equals("PAYSAGE")), client);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore()).body(pdf);
    }
}
