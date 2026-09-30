package cm.gimac.des.collaboration.domain.messagerie;

import java.util.UUID;

/** Étiquette de projet : liaison citée par un message (référence LIA-AAAA-NNN). */
public record Etiquette(UUID liaisonId, String reference) {}
