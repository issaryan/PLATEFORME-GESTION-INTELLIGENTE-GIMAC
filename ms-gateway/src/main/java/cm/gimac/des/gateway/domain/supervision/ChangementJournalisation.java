package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;

/** Trace d'un changement de niveau de journalisation appliqué à chaud. */
public record ChangementJournalisation(String service, String logger, String niveau, String motif, String auteur, Instant horodatage) {}
