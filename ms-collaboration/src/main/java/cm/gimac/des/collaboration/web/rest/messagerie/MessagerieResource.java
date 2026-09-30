package cm.gimac.des.collaboration.web.rest.messagerie;

import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Expediteur;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.ConversationVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.CreationRequest;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.EnvoiRequest;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.MessageVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.NonLusVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.PageMessagesVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.PageVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.PersonneVue;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
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
 * Messagerie instantanée de travail (D-64), exposée par la passerelle sous
 * /bff/des/collaboration/messagerie : annuaire, conversations, messages, lecture, non-lus et pièces
 * jointes. Jeton à deux facteurs (SecurityConfiguration) puis permission COL_MESSAGE_READ ou
 * COL_MESSAGE_SEND ; l'appelant est le sujet du jeton.
 */
@RestController
@RequestMapping("/api/v1/messagerie")
public class MessagerieResource {

    private static final String LIRE = "hasAuthority('COL_MESSAGE_READ')";
    private static final String ENVOYER = "hasAuthority('COL_MESSAGE_SEND')";

    private final EnvoyerMessageUseCase envoi;
    private final ConsulterMessagerieUseCase lecture;

    public MessagerieResource(EnvoyerMessageUseCase envoi, ConsulterMessagerieUseCase lecture) {
        this.envoi = envoi;
        this.lecture = lecture;
    }

    @GetMapping("/annuaire")
    @PreAuthorize(LIRE)
    public PageVue<PersonneVue> annuaire(
        @RequestParam(name = "recherche", required = false) String recherche,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille,
        Authentication auth
    ) {
        var p = lecture.annuaire(appelant(auth).id(), recherche, page, taille);
        return new PageVue<>(p.contenu().stream().map(PersonneVue::de).toList(), p.total(), p.page(), p.taille());
    }

    @GetMapping("/conversations")
    @PreAuthorize(LIRE)
    public PageVue<ConversationVue> conversations(
        @RequestParam(name = "recherche", required = false) String recherche,
        @RequestParam(name = "liaisonId", required = false) UUID liaisonId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "taille", defaultValue = "20") int taille,
        Authentication auth
    ) {
        var p = lecture.conversations(appelant(auth).id(), recherche, liaisonId, page, taille);
        return new PageVue<>(p.contenu().stream().map(ConversationVue::de).toList(), p.total(), p.page(), p.taille());
    }

    @PostMapping(value = "/conversations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ENVOYER)
    public ResponseEntity<ConversationVue> creer(
        @RequestPart("message") CreationRequest message,
        @RequestPart(name = "fichiers", required = false) List<MultipartFile> fichiers,
        Authentication auth
    ) {
        Expediteur moi = appelant(auth);
        var r = envoi.creerConversation(
            moi,
            new EnvoyerMessageUseCase.NouvelleConversation(
                message.destinataires(),
                message.titre(),
                message.liaisonId(),
                message.liaisonReference(),
                message.contenu(),
                message.etiquettes()
            ),
            pieces(fichiers)
        );
        ConversationVue vue = ConversationVue.de(lecture.conversation(moi.id(), r.conversationId()));
        return ResponseEntity.status(r.creee() ? HttpStatus.CREATED : HttpStatus.OK)
            .location(URI.create("/api/v1/messagerie/conversations/" + r.conversationId()))
            .body(vue);
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize(LIRE)
    public ConversationVue conversation(@PathVariable UUID id, Authentication auth) {
        return ConversationVue.de(lecture.conversation(appelant(auth).id(), id));
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize(LIRE)
    public PageMessagesVue messages(
        @PathVariable UUID id,
        @RequestParam(name = "avant", required = false) UUID avant,
        @RequestParam(name = "taille", defaultValue = "50") int taille,
        Authentication auth
    ) {
        String moi = appelant(auth).id();
        var p = lecture.messages(moi, id, avant, taille);
        return new PageMessagesVue(p.contenu().stream().map(m -> MessageVue.de(m, moi)).toList(), p.plusAnciens());
    }

    @PostMapping(value = "/conversations/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ENVOYER)
    public ResponseEntity<MessageVue> envoyer(
        @PathVariable UUID id,
        @RequestPart("message") EnvoiRequest message,
        @RequestPart(name = "fichiers", required = false) List<MultipartFile> fichiers,
        Authentication auth
    ) {
        Expediteur moi = appelant(auth);
        var r = envoi.envoyer(moi, id, new EnvoyerMessageUseCase.NouveauMessage(message.contenu(), message.etiquettes()), pieces(fichiers));
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageVue.de(lecture.message(moi.id(), r.messageId()), moi.id()));
    }

    @PostMapping("/conversations/{id}/lu")
    @PreAuthorize(LIRE)
    public ResponseEntity<Void> marquerLu(@PathVariable UUID id, Authentication auth) {
        envoi.marquerLu(appelant(auth).id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/non-lus")
    @PreAuthorize(LIRE)
    public NonLusVue nonLus(Authentication auth) {
        return new NonLusVue(lecture.nonLus(appelant(auth).id()));
    }

    @GetMapping("/pieces/{pieceId}")
    @PreAuthorize(LIRE)
    public ResponseEntity<Resource> piece(@PathVariable UUID pieceId, Authentication auth) {
        var p = lecture.piece(appelant(auth).id(), pieceId);
        HttpHeaders entetes = new HttpHeaders();
        String empreinte = reprDigest(p.piece().getEmpreinte());
        if (empreinte != null) {
            // Empreinte du dépôt (RFC 9530), vérifiable par le client après téléchargement.
            entetes.set("Repr-Digest", empreinte);
        }
        return ResponseEntity.ok()
            .headers(entetes)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(p.piece().getNom(), StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.parseMediaType(p.piece().getTypeMime()))
            .contentLength(p.piece().getTaille())
            .body(new InputStreamResource(p.contenu().flux()));
    }

    /** En-tête Repr-Digest (RFC 9530) d'une empreinte SHA-256 hexadécimale ; null si elle est mal formée. */
    static String reprDigest(String sha256Hex) {
        if (sha256Hex == null || !sha256Hex.matches("^[0-9a-fA-F]{64}$")) {
            return null;
        }
        return "sha-256=:" + Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256Hex)) + ":";
    }

    private static List<FichierJoint> pieces(List<MultipartFile> fichiers) {
        if (fichiers == null) {
            return List.of();
        }
        return fichiers.stream().map(f -> new FichierJoint(f.getOriginalFilename(), f.getContentType(), f.getSize(), f::getInputStream)).toList();
    }

    /** Appelant : sujet du jeton utilisateur (identifiant IAM) et nom affiché qu'il porte. */
    static Expediteur appelant(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwt && jwt.getToken().getSubject() != null && !jwt.getToken().getSubject().isBlank()) {
            String nom = jwt.getToken().getClaimAsString("name");
            if (nom == null || nom.isBlank()) {
                nom = jwt.getToken().getClaimAsString("preferred_username");
            }
            return new Expediteur(jwt.getToken().getSubject(), nom);
        }
        throw CollaborationException.interdit("COL_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
    }
}
