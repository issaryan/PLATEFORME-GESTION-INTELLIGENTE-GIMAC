package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Deposant;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Demandeur;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Ouverture;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.web.rest.v1.DocumentVues.DepotVue;
import cm.gimac.des.document.web.rest.v1.DocumentVues.OuvertureDepotRequete;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dépôts fragmentés (claim check, D-74, docs/lot1/depots-fragmentes.md §3), exposés sous
 * /bff/{des|console|portail}/document/depots (corps de 17 Mio au plus, borné par la passerelle).
 * Toutes les routes exigent l'une des permissions du dépôt direct, COL_MESSAGE_SEND ou PIL_ACTIVITY_OWN
 * (rapport d'activité, D-76) ; la
 * permission propre à la finalité est contrôlée à l'ouverture. Seul le compte qui a ouvert un
 * dépôt y a accès (sujet du jeton), 404 DOC_UPLOAD_NOT_FOUND pour tout autre.
 */
@RestController
@RequestMapping("/api/v1/depots")
public class DepotFragmenteResource {

    private final DepotFragmenteUseCase useCase;

    public DepotFragmenteResource(DepotFragmenteUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('PAR_ADHESION_INSTRUCT', 'HOM_EVIDENCE_SUBMIT', 'HOM_STEP_MANAGE', 'CER_CHANGE_RECORD_LINK', 'HOM_PORTAL_SUBMIT', 'DOC_DEPOSIT', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN')")
    public ResponseEntity<DepotVue> ouvrir(@RequestBody OuvertureDepotRequete requete, Authentication auth) {
        Deposant deposant = DocumentDepotResource.deposant(auth);
        Set<String> permissions = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
        Ouverture ouverture = new Ouverture(requete.nomFichier(), requete.typeMime(), requete.taille(), requete.finalite(), requete.reference());
        DepotVue vue = DepotVue.de(useCase.ouvrir(ouverture, new Demandeur(deposant.id(), deposant.institutionId(), permissions)));
        return ResponseEntity.status(HttpStatus.CREATED).body(vue);
    }

    /** Partie {@code numero} (à partir de 1) en octets bruts ; renvoyer une partie la remplace. */
    @PutMapping(path = "/{id}/parties/{numero}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('PAR_ADHESION_INSTRUCT', 'HOM_EVIDENCE_SUBMIT', 'HOM_STEP_MANAGE', 'CER_CHANGE_RECORD_LINK', 'HOM_PORTAL_SUBMIT', 'DOC_DEPOSIT', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN')")
    public ResponseEntity<Void> partie(@PathVariable UUID id, @PathVariable int numero, HttpServletRequest requete, Authentication auth) {
        String deposant = DocumentDepotResource.deposant(auth).id();
        // Corps lu en flux par le cas d'utilisation, borné à la taille attendue de la partie.
        try (InputStream corps = requete.getInputStream()) {
            useCase.recevoirPartie(id, numero, corps, requete.getContentLengthLong(), deposant);
        } catch (IOException e) {
            throw DocumentException.invalide("DOC_FILE_UNREADABLE", "La partie transmise est illisible");
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PAR_ADHESION_INSTRUCT', 'HOM_EVIDENCE_SUBMIT', 'HOM_STEP_MANAGE', 'CER_CHANGE_RECORD_LINK', 'HOM_PORTAL_SUBMIT', 'DOC_DEPOSIT', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN')")
    public DepotVue etat(@PathVariable UUID id, Authentication auth) {
        return DepotVue.de(useCase.etat(id, DocumentDepotResource.deposant(auth).id()));
    }

    @PostMapping("/{id}/fin")
    @PreAuthorize("hasAnyAuthority('PAR_ADHESION_INSTRUCT', 'HOM_EVIDENCE_SUBMIT', 'HOM_STEP_MANAGE', 'CER_CHANGE_RECORD_LINK', 'HOM_PORTAL_SUBMIT', 'DOC_DEPOSIT', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN')")
    public ResponseEntity<DepotVue> terminer(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DepotVue.de(useCase.terminer(id, DocumentDepotResource.deposant(auth).id())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PAR_ADHESION_INSTRUCT', 'HOM_EVIDENCE_SUBMIT', 'HOM_STEP_MANAGE', 'CER_CHANGE_RECORD_LINK', 'HOM_PORTAL_SUBMIT', 'DOC_DEPOSIT', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN')")
    public ResponseEntity<Void> abandonner(@PathVariable UUID id, Authentication auth) {
        useCase.abandonner(id, DocumentDepotResource.deposant(auth).id());
        return ResponseEntity.noContent().build();
    }
}
