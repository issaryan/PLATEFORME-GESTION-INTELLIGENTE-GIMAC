package cm.gimac.des.pilotage.domain.processus;

/** Livrable couvert par une signature : code, document et empreinte SHA-256 au moment de la signature. */
public record DocumentSigne(String livrable, String documentId, String sha256) {}
