package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.application.usecase.AvatarService;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.service.TraitementAvatar.ImageAvatar;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Photo de profil (docs/lot1/mon-compte.md §1). Le titulaire remplace ou retire la sienne
 * (/api/v1/moi/avatar, sous /bff/{des,portail,console}/iam/moi/avatar) ; tout compte interne lit
 * celle d'un collègue (/api/v1/avatars/{compteId}, sous /bff/{des,console}/iam/avatars), un
 * correspondant (jeton portant institution_id) seulement la sienne. Jeton à deux facteurs exigé
 * (acr 2 ou 3, chaîne de sécurité).
 */
@RestController
public class AvatarResource {

    /** Réponse de PUT /moi/avatar : version (empreinte courte) de la photo conservée. */
    public record VersionVue(String version) {}

    /** Image immuable pour une version donnée : le client la garde un jour sans la redemander. */
    static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(1)).cachePrivate().immutable();

    private final AvatarService service;

    public AvatarResource(AvatarService service) {
        this.service = service;
    }

    @PutMapping(path = "/api/v1/moi/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VersionVue remplacer(@RequestPart(name = "image", required = false) MultipartFile image, Authentication auth) throws IOException {
        if (image == null || image.isEmpty()) {
            throw IamException.invalide("IAM_AVATAR_UNREADABLE", "Image absente ou vide (partie « image »)", Map.of());
        }
        return new VersionVue(service.remplacer(ActeurCourant.de(auth), image.getBytes()));
    }

    @DeleteMapping("/api/v1/moi/avatar")
    public ResponseEntity<Void> retirer(Authentication auth) {
        service.retirer(ActeurCourant.de(auth));
        return ResponseEntity.noContent().build();
    }

    /** Photo du titulaire (seule lisible depuis le portail). */
    @GetMapping("/api/v1/moi/avatar")
    public ResponseEntity<byte[]> mienne(Authentication auth) {
        return image(ActeurCourant.de(auth).id());
    }

    /** Photo d'un compte : tout compte interne ; un correspondant, la sienne seulement. */
    @GetMapping("/api/v1/avatars/{compteId}")
    public ResponseEntity<byte[]> lire(@PathVariable String compteId, Authentication auth) {
        String sujet = ActeurCourant.de(auth).id();
        if (!sujet.equals(compteId) && correspondant(auth)) {
            throw IamException.interdit("IAM_AVATAR_FORBIDDEN", "Photo d'un autre compte non accessible depuis le portail");
        }
        return image(compteId);
    }

    private ResponseEntity<byte[]> image(String compteId) {
        ImageAvatar a = service.lire(compteId).orElseThrow(() -> IamException.introuvable("IAM_AVATAR_NOT_FOUND", compteId));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(a.type()))
            .cacheControl(CACHE)
            .eTag('"' + a.version() + '"')
            .body(a.octets());
    }

    /** Jeton d'un correspondant de participant : institution_id renseignée. */
    static boolean correspondant(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            String institution = jwt.getToken().getClaimAsString("institution_id");
            return institution != null && !institution.isBlank();
        }
        return false;
    }
}
