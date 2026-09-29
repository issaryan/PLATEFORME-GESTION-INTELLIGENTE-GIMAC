package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Commande;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Fichier;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.infrastructure.security.CorrelationContext;
import cm.gimac.des.document.web.rest.v1.DocumentVues.DocumentDeposeVue;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dépôt direct d'un fichier dans MS-DOCUMENT (tranche DDRM : pièces du dossier d'adhésion ;
 * preuves d'homologation). Exposé sous /bff/des/document/documents. Multipart : {@code fichier},
 * {@code finalite} (ADHESION...), {@code reference} (texte libre, par exemple institutionId/code).
 * Les paramètres sont lus comme facultatifs pour que leur absence donne un code DOC_* explicite.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentDepotResource {

    private final DeposerDocumentUseCase useCase;

    public DocumentDepotResource(DeposerDocumentUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('PAR_ADHESION_INSTRUCT', 'HOM_EVIDENCE_SUBMIT', 'HOM_PORTAL_SUBMIT', 'DOC_DEPOSIT')")
    public ResponseEntity<DocumentDeposeVue> deposer(
        @RequestPart(name = "fichier", required = false) MultipartFile fichier,
        @RequestParam(name = "finalite", required = false) String finalite,
        @RequestParam(name = "reference", required = false) String reference,
        Authentication auth
    ) {
        Commande commande = new Commande(fichier == null ? null : lire(fichier), finalite, reference);
        DeposerDocumentUseCase.Resultat r = useCase.deposer(commande, deposant(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentDeposeVue.de(r));
    }

    static Fichier lire(MultipartFile fichier) {
        try {
            return new Fichier(fichier.getOriginalFilename(), fichier.getContentType(), fichier.getBytes());
        } catch (IOException e) {
            throw DocumentException.invalide("DOC_FILE_UNREADABLE", "Le fichier transmis est illisible");
        }
    }

    /** Déposant : sujet du jeton utilisateur et, pour un représentant d'un participant, son institution. */
    static Deposant deposant(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank()) {
            throw DocumentException.interdit("DOC_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
        }
        Jwt t = jwt.getToken();
        String institution = t.getClaimAsString(CorrelationContext.INSTITUTION_CLAIM);
        return new Deposant(t.getSubject(), institution == null || institution.isBlank() ? null : institution);
    }
}
