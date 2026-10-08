package cm.gimac.des.homologation.domain.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Intervenant d'une opération désigné dans un événement du parcours (D-93 §1) : qualité (R, V, C, I, E), fonction du
 * catalogue et son libellé ; {@code compteId} : compte interne désigné ; {@code aAffecter} : la fonction n'est tenue par
 * personne, {@code profils} donne alors les profils qui la confèrent (notification par profil) ; {@code institutionId} :
 * institution qui tient une fonction externe (participant ou institution liée). Jamais de donnée de contact.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DestinataireOperation(
    String qualite,
    String fonction,
    String libelleFonction,
    String compteId,
    boolean aAffecter,
    List<String> profils,
    String institutionId
) {}
