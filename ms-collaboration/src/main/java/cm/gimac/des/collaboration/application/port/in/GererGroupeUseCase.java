package cm.gimac.des.collaboration.application.port.in;

import cm.gimac.des.collaboration.application.port.in.EnvoyerMessageUseCase.Expediteur;
import java.util.List;
import java.util.UUID;

/**
 * Gestion d'un groupe de discussion (D-68) : l'administrateur renomme le groupe, ajoute ou retire des
 * membres ; tout membre peut le quitter. Chaque geste s'inscrit dans le fil comme un événement.
 */
public interface GererGroupeUseCase {
    void renommer(Expediteur appelant, UUID conversationId, String titre);

    void ajouterMembres(Expediteur appelant, UUID conversationId, List<String> membres);

    /** Retrait par l'administrateur, ou départ quand l'appelant se retire lui-même. */
    void retirerMembre(Expediteur appelant, UUID conversationId, String membreId);
}
