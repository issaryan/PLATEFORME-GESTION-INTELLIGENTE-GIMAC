package cm.gimac.des.gateway.domain.sandbox;

import java.util.List;

/**
 * Contenu du jeton interne réduit relayé vers un service de la sandbox (RFC 8693, CC-SBX §5.1.4) : sujet, institution
 * effective (propre ou mandante), institution propre, permissions de la sandbox seulement, niveau d'assurance et
 * audience restreinte au service visé.
 */
public record JetonReduit(String sujet, String institution, String institutionPropre, List<String> permissions, String acr, String audience, String contexte) {
    public JetonReduit {
        permissions = List.copyOf(permissions);
    }

    /** Jeton sans contexte d'action (compte sans double rôle, D-133). */
    public JetonReduit(String sujet, String institution, String institutionPropre, List<String> permissions, String acr, String audience) {
        this(sujet, institution, institutionPropre, permissions, acr, audience, null);
    }
}
