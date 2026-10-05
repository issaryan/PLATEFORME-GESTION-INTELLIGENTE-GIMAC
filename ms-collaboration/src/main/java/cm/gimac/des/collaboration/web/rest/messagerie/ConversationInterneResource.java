package cm.gimac.des.collaboration.web.rest.messagerie;

import cm.gimac.des.collaboration.application.port.in.ConsulterConversationInterneUseCase;
import cm.gimac.des.collaboration.application.port.in.ConsulterConversationInterneUseCase.ConversationInterne;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interface interne (PI §0.7, D-80) : {@code GET /api/v1/interne/conversations/{id}} →
 * {@code { id, type, nom, membres }}, pour le jeton de service portant INTERNAL_COLLABORATION_READ
 * (client notification-service). Aucun message ni pièce n'est exposé ; conversation inconnue : 404.
 */
@RestController
@RequestMapping("/api/v1/interne/conversations")
public class ConversationInterneResource {

    private final ConsulterConversationInterneUseCase conversations;

    public ConversationInterneResource(ConsulterConversationInterneUseCase conversations) {
        this.conversations = conversations;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INTERNAL_COLLABORATION_READ')")
    public ConversationInterne conversation(@PathVariable UUID id) {
        return conversations.conversationInterne(id);
    }
}
