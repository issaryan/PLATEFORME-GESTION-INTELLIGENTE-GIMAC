package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.LireDocumentUseCase;
import cm.gimac.des.document.application.port.in.LireDocumentUseCase.Contenu;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.infrastructure.security.ServiceTokenAuthorizationManager;
import cm.gimac.des.document.web.rest.v1.DocumentVues.DocumentDeposeVue;
import cm.gimac.des.document.web.rest.v1.DocumentVues.EmpreinteVue;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface interne de MS-DOCUMENT, réservée aux jetons de service (client_credentials), non
 * exposée par la passerelle ; chaque route exige sa permission interne :
 * <ul>
 *   <li>{@code GET /{id}/empreinte} (INTERNAL_DOCUMENT_READ) : MS-PARTICIPANT revérifie l'empreinte
 *   déclarée d'une pièce avant de la rattacher au dossier ;</li>
 *   <li>{@code POST /} (INTERNAL_DOCUMENT_DEPOSIT) : MS-COLLABORATION dépose une pièce jointe de la
 *   messagerie pour le compte de l'utilisateur {@code deposantId} (même chaîne que le dépôt direct) ;</li>
 *   <li>{@code GET /{id}/contenu} (INTERNAL_DOCUMENT_READ) : contenu du document, vérifié contre son
 *   empreinte, restitué en pièce jointe avec l'en-tête {@code Repr-Digest} (RFC 9530).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/interne/documents")
public class DocumentInterneResource {

    static final String REPR_DIGEST = "Repr-Digest";
    /** attr-char (RFC 8187) hors lettres et chiffres ASCII. */
    private static final String ATTR_CHAR_SPECIAUX = "!#$&+-.^_`|~";

    private final DeposerDocumentUseCase depot;
    private final LireDocumentUseCase lecture;

    public DocumentInterneResource(DeposerDocumentUseCase depot, LireDocumentUseCase lecture) {
        this.depot = depot;
        this.lecture = lecture;
    }

    @GetMapping("/{id}/empreinte")
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_READ')")
    public EmpreinteVue empreinte(@PathVariable UUID id) {
        return new EmpreinteVue(depot.empreinte(id));
    }

    /**
     * Dépôt pour le compte d'un utilisateur. Multipart : {@code fichier}, {@code finalite}
     * (MESSAGERIE...), {@code reference} et {@code deposantId} ; lus comme facultatifs pour que leur
     * absence donne un code DOC_* explicite. Le déposant inscrit est l'utilisateur et le client du
     * service appelant.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_DEPOSIT')")
    public ResponseEntity<DocumentDeposeVue> deposer(
        @RequestPart(name = "fichier", required = false) MultipartFile fichier,
        @RequestParam(name = "finalite", required = false) String finalite,
        @RequestParam(name = "reference", required = false) String reference,
        @RequestParam(name = "deposantId", required = false) String deposantId,
        Authentication auth
    ) {
        Deposant deposant = Deposant.pourLeCompteDe(deposantId, service(auth));
        Commande commande = new Commande(fichier == null ? null : DocumentDepotResource.lire(fichier), finalite, reference);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentDeposeVue.de(depot.deposer(commande, deposant)));
    }

    @GetMapping("/{id}/contenu")
    @PreAuthorize("hasAuthority('INTERNAL_DOCUMENT_READ')")
    public ResponseEntity<byte[]> contenu(@PathVariable UUID id, Authentication auth) {
        Contenu c = lecture.lire(id, service(auth));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(c.typeMime()))
            .contentLength(c.taille())
            .header(HttpHeaders.CONTENT_DISPOSITION, pieceJointe(c.nomFichier()))
            .header(REPR_DIGEST, reprDigest(c.sha256()))
            .header("X-Content-Type-Options", "nosniff")
            .body(c.octets());
    }

    /**
     * En-tête Content-Disposition d'une pièce jointe : {@code attachment; filename*=UTF-8''<nom>},
     * nom encodé en UTF-8 selon RFC 8187 (caractères hors attr-char en %XX), sans forme de repli.
     */
    static String pieceJointe(String nom) {
        StringBuilder sb = new StringBuilder("attachment; filename*=UTF-8''");
        for (byte b : nom.getBytes(StandardCharsets.UTF_8)) {
            char ch = (char) (b & 0xFF);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ATTR_CHAR_SPECIAUX.indexOf(ch) >= 0) {
                sb.append(ch);
            } else {
                sb.append('%').append(HexFormat.of().withUpperCase().toHexDigits(b));
            }
        }
        return sb.toString();
    }

    /** En-tête Repr-Digest (RFC 9530) : {@code sha-256=:<empreinte en base64>:}. */
    static String reprDigest(String sha256Hex) {
        return "sha-256=:" + Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256Hex)) + ":";
    }

    /** Client du service appelant, lu dans le jeton de service ; un jeton utilisateur est refusé. */
    static String service(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt) {
            return ServiceTokenAuthorizationManager.clientDeService(jwt.getToken()).orElseThrow(DocumentInterneResource::jetonDeServiceExige);
        }
        throw jetonDeServiceExige();
    }

    private static DocumentException jetonDeServiceExige() {
        return DocumentException.interdit("DOC_SERVICE_TOKEN_REQUIRED", "Jeton de service exigé");
    }
}
