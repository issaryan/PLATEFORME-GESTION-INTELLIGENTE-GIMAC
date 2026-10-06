package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.ApercuBureautiqueUseCase;
import cm.gimac.des.document.application.port.in.ApercuBureautiqueUseCase.Demande;
import cm.gimac.des.document.domain.DocumentException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

/**
 * Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md §2), exposé sous
 * /bff/{des,console}/document/apercus : le corps de la requête porte les octets du fichier (son type, ou
 * application/octet-stream), l'en-tête {@code X-Nom-Fichier} son nom encodé en URI (pour l'extension). Réponse :
 * le PDF converti, affiché en ligne par l'aperçu PDF. Comptes internes seulement (aucune institution dans le
 * jeton), permission PIL_ACTIVITY_OWN ; jeton à deux facteurs comme toute route métier.
 */
@RestController
@RequestMapping("/api/v1/apercus")
public class ApercuResource {

    public static final String ENTETE_NOM = "X-Nom-Fichier";

    private final ApercuBureautiqueUseCase useCase;

    public ApercuResource(ApercuBureautiqueUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")
    public ResponseEntity<byte[]> apercu(
        @RequestHeader(name = ENTETE_NOM, required = false) String nomEncode,
        HttpServletRequest requete,
        Authentication auth
    ) {
        var compte = DocumentDepotResource.deposant(auth);
        if (compte.institutionId() != null) {
            throw DocumentException.interdit("DOC_APERCU_RESERVE_INTERNE", "L'aperçu est réservé aux comptes internes");
        }
        long max = useCase.tailleMax();
        long annoncee = requete.getContentLengthLong();
        if (annoncee > max) {
            throw DocumentException.tropVolumineux("DOC_APERCU_TROP_VOLUMINEUX", annoncee, max);
        }
        byte[] pdf = useCase.apercu(new Demande(nom(nomEncode), lire(requete, max)), compte.id());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("apercu.pdf").build().toString())
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
            .body(pdf);
    }

    /** Nom décodé (encodeURIComponent côté navigateur) ; un encodage invalide vaut absence de nom. */
    static String nom(String nomEncode) {
        if (nomEncode == null || nomEncode.isBlank()) {
            return null;
        }
        try {
            return UriUtils.decode(nomEncode, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Corps lu au plus jusqu'à max + 1 octets : le dépassement est constaté par le cas d'utilisation. */
    static byte[] lire(HttpServletRequest requete, long max) {
        try (InputStream corps = requete.getInputStream()) {
            return corps.readNBytes((int) Math.min(Integer.MAX_VALUE - 8L, max + 1));
        } catch (IOException e) {
            throw DocumentException.invalide("DOC_FILE_UNREADABLE", "Le fichier transmis est illisible");
        }
    }
}
