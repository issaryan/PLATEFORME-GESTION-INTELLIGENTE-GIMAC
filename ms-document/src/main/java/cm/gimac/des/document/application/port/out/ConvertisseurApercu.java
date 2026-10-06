package cm.gimac.des.document.application.port.out;

/**
 * Port sortant : conversion d'un document bureautique en PDF pour l'aperçu (D-83). Adaptateur réel : le
 * moteur Gotenberg (LibreOffice sans interface) du conteneur interne {@code conversion-documents}, dont
 * MS-DOCUMENT est le seul client.
 */
public interface ConvertisseurApercu {
    /**
     * PDF du document. {@code extension} (en minuscules, admise par SignatureBureautique) indique au
     * moteur le format de la source ; le nom d'origine ne lui est jamais transmis.
     *
     * @throws ConversionRefusee le moteur refuse le fichier (corrompu, protégé par mot de passe)
     * @throws MoteurIndisponible moteur injoignable, saturé ou délai dépassé
     */
    byte[] convertirEnPdf(String extension, byte[] contenu);

    /** Le moteur a refusé le fichier : il ne pourra pas être converti tel quel. */
    class ConversionRefusee extends RuntimeException {

        public ConversionRefusee(String message) {
            super(message);
        }
    }

    /** Le moteur n'a pas pu répondre : une nouvelle tentative plus tard peut réussir. */
    class MoteurIndisponible extends RuntimeException {

        public MoteurIndisponible(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
