package cm.gimac.des.collaboration.web.rest.messagerie;

import java.util.stream.Collectors;
import java.util.Set;
import java.util.Map;
import cm.gimac.des.collaboration.domain.messagerie.ConversationMembre;
import cm.gimac.des.collaboration.application.port.in.ConsulterMessagerieUseCase;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase;
import cm.gimac.des.collaboration.application.port.in.GererGroupeUseCase;
import cm.gimac.des.collaboration.application.port.in.SignalerSaisieUseCase;
import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Expediteur;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.AjoutMembresRequest;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.ConversationVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.CreationRequest;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.EnvoiRequest;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.MessageVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.NonLusVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.PageMessagesVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.PageVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.PersonneVue;
import cm.gimac.des.collaboration.web.rest.messagerie.MessagerieVues.RenommageRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final GererGroupeUseCase groupes;
    private final SignalerSaisieUseCase saisie;

    public MessagerieResource(EnvoyerMessageUseCase envoi, ConsulterMessagerieUseCase lecture, GererGroupeUseCase groupes, SignalerSaisieUseCase saisie) {
        this.envoi = envoi;
        this.lecture = lecture;
        this.groupes = groupes;
        this.saisie = saisie;
    }

    /** Conversation avec les photos de profil courantes de ses membres. */
    private ConversationVue vue(ConsulterMessagerieUseCase.ConversationLue l) {
        return ConversationVue.de(l, lecture.versionsAvatar(l.membres().stream().map(ConversationMembre::getMembreId).collect(Collectors.toSet())));
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
        Map<String, String> avatars = lecture.versionsAvatar(p.contenu().stream().flatMap(c -> c.membres().stream()).map(ConversationMembre::getMembreId).collect(Collectors.toSet()));
        return new PageVue<>(p.contenu().stream().map(c -> ConversationVue.de(c, avatars)).toList(), p.total(), p.page(), p.taille());
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
                message.etiquettes(),
                Boolean.TRUE.equals(message.groupe()),
                vide(message.documents()),
                vide(message.transferts())
            ),
            pieces(fichiers)
        );
        ConversationVue vue = vue(lecture.conversation(moi.id(), r.conversationId()));
        return ResponseEntity.status(r.creee() ? HttpStatus.CREATED : HttpStatus.OK)
            .location(URI.create("/api/v1/messagerie/conversations/" + r.conversationId()))
            .body(vue);
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize(LIRE)
    public ConversationVue conversation(@PathVariable UUID id, Authentication auth) {
        return vue(lecture.conversation(appelant(auth).id(), id));
    }

    /** Nouveau nom d'un groupe (D-68), réservé à son administrateur. */
    @PatchMapping(value = "/conversations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(ENVOYER)
    public ConversationVue renommer(@PathVariable UUID id, @RequestBody RenommageRequest demande, Authentication auth) {
        Expediteur moi = appelant(auth);
        groupes.renommer(moi, id, demande.titre());
        return vue(lecture.conversation(moi.id(), id));
    }

    /** Membres ajoutés à un groupe (D-68), réservé à son administrateur. */
    @PostMapping(value = "/conversations/{id}/membres", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(ENVOYER)
    public ConversationVue ajouterMembres(@PathVariable UUID id, @RequestBody AjoutMembresRequest demande, Authentication auth) {
        Expediteur moi = appelant(auth);
        groupes.ajouterMembres(moi, id, demande.membres());
        return vue(lecture.conversation(moi.id(), id));
    }

    /** Retrait d'un membre par l'administrateur, ou départ de l'appelant (son propre identifiant). */
    @DeleteMapping("/conversations/{id}/membres/{membreId}")
    @PreAuthorize(ENVOYER)
    public ResponseEntity<Void> retirerMembre(@PathVariable UUID id, @PathVariable String membreId, Authentication auth) {
        groupes.retirerMembre(appelant(auth), id, membreId);
        return ResponseEntity.noContent().build();
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
        Map<String, String> avatars = lecture.versionsAvatar(p.contenu().stream().map(m -> m.message().getAuteurId()).collect(Collectors.toSet()));
        return new PageMessagesVue(p.contenu().stream().map(m -> MessageVue.de(m, moi, avatars)).toList(), p.plusAnciens());
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
        var r = envoi.envoyer(
            moi,
            id,
            new EnvoyerMessageUseCase.NouveauMessage(
                message.contenu(),
                message.etiquettes(),
                vide(message.documents()),
                vide(message.transferts())
            ),
            pieces(fichiers)
        );
        var lu = lecture.message(moi.id(), r.messageId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageVue.de(lu, moi.id(), lecture.versionsAvatar(Set.of(lu.message().getAuteurId()))));
    }

    @PostMapping("/conversations/{id}/lu")
    @PreAuthorize(LIRE)
    public ResponseEntity<Void> marquerLu(@PathVariable UUID id, Authentication auth) {
        envoi.marquerLu(appelant(auth).id(), id);
        return ResponseEntity.noContent().build();
    }

    /** « En train d'écrire » (D-72) : signal éphémère aux autres membres ; 204, 404 hors de la conversation. */
    @PostMapping("/conversations/{id}/saisie")
    @PreAuthorize(ENVOYER)
    public ResponseEntity<Void> saisie(@PathVariable UUID id, Authentication auth) {
        saisie.signalerSaisie(appelant(auth).id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/non-lus")
    @PreAuthorize(LIRE)
    public NonLusVue nonLus(Authentication auth) {
        return new NonLusVue(lecture.nonLus(appelant(auth).id()));
    }

    /**
     * Contenu d'une pièce, relayé en flux quelle que soit sa taille (D-74) : le flux de MS-DOCUMENT est
     * recopié par tampons vers la réponse, jamais chargé en entier ; Content-Length est la taille connue.
     * InputStreamResource exactement : pas de traitement de Range, qui exigerait un flux relisible.
     */
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

    private static <T> List<T> vide(List<T> liste) {
        return liste == null ? List.of() : liste;
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
