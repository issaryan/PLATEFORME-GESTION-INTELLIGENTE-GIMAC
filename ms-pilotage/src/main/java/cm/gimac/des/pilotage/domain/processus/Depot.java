package cm.gimac.des.pilotage.domain.processus;

/**
 * Dépôt rattaché à un livrable (§2) : document de MS-DOCUMENT, son empreinte SHA-256 (hexadécimal), le
 * déposant et l'horodatage ISO 8601.
 */
public record Depot(String livrable, String documentId, String nomFichier, String typeMime, long taille, String sha256, String deposePar, String deposeLe) {}
