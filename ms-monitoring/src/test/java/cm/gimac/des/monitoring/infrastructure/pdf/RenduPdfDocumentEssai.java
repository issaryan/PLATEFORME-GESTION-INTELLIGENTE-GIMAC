package cm.gimac.des.monitoring.infrastructure.pdf;

import cm.gimac.des.monitoring.application.port.out.RenduPdfPort.Document;

/** Accès des tests d'autres paquets au corps exact de la requête interne (impression réelle, D-139). */
public final class RenduPdfDocumentEssai {

    private RenduPdfDocumentEssai() {}

    public static Object requete(Document d) {
        return RenduPdfDocument.requete(d);
    }
}
