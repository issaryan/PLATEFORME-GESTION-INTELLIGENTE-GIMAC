package cm.gimac.des.monitoring.application.port.out;

/**
 * Dépôt d'un rapport de quart scellé dans MS-DOCUMENT (WORM en mode conformité, rétention 10 ans : D-11, RG-18) :
 * rend la référence du document. MS-DOCUMENT injoignable : 503 MON_DEPOT_INDISPONIBLE, le rapport reste visé.
 */
public interface DepotDocumentPort {
    String deposer(byte[] contenu, String nom, String typeMime, String empreinte);
}
