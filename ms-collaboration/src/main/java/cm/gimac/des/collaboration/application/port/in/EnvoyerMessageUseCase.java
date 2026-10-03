package cm.gimac.des.collaboration.application.port.in;

import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.util.List;
import java.util.UUID;

/** Écritures de la messagerie de travail (D-64) : nouvelle conversation, nouveau message, lecture. */
public interface EnvoyerMessageUseCase {
    /** Appelant : identifiant IAM (sujet du jeton) et nom porté par le jeton, à défaut de l'annuaire. */
    record Expediteur(String id, String nomJeton) {}

    /**
     * « groupe » (D-68) : groupe de discussion nommé, même avec un seul autre membre ; le premier message
     * y est alors facultatif. Sans lui, deux personnes sans titre ni liaison forment une conversation directe.
     * « documents » et « transferts » (D-74) : voir {@link NouveauMessage}.
     */
    record NouvelleConversation(
        List<String> destinataires,
        String titre,
        UUID liaisonId,
        String liaisonReference,
        String contenu,
        List<Etiquette> etiquettes,
        boolean groupe,
        List<UUID> documents,
        List<UUID> transferts
    ) {
        public NouvelleConversation(
            List<String> destinataires,
            String titre,
            UUID liaisonId,
            String liaisonReference,
            String contenu,
            List<Etiquette> etiquettes,
            boolean groupe
        ) {
            this(destinataires, titre, liaisonId, liaisonReference, contenu, etiquettes, groupe, List.of(), List.of());
        }

        public NouvelleConversation(List<String> destinataires, String titre, UUID liaisonId, String liaisonReference, String contenu, List<Etiquette> etiquettes) {
            this(destinataires, titre, liaisonId, liaisonReference, contenu, etiquettes, false);
        }
    }

    /**
     * Nouveau message. « documents » (D-74) : tickets de documents déposés par l'expéditeur par envoi
     * fragmenté, de finalité MESSAGERIE ; « transferts » : pièces jointes existantes repartagées (même
     * document, sans copie).
     */
    record NouveauMessage(String contenu, List<Etiquette> etiquettes, List<UUID> documents, List<UUID> transferts) {
        public NouveauMessage(String contenu, List<Etiquette> etiquettes) {
            this(contenu, etiquettes, List.of(), List.of());
        }
    }

    /** Conversation créée, ou conversation directe existante réutilisée ; message ajouté. */
    record Envoi(UUID conversationId, UUID messageId, boolean creee) {}

    Envoi creerConversation(Expediteur expediteur, NouvelleConversation demande, List<FichierJoint> fichiers);

    Envoi envoyer(Expediteur expediteur, UUID conversationId, NouveauMessage demande, List<FichierJoint> fichiers);

    /** Marque lus tous les messages de la conversation, jusqu'au dernier. */
    void marquerLu(String membreId, UUID conversationId);
}
