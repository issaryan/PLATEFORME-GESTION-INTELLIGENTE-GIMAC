package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.RenduPdfPort.Document;

/** Accès des tests d'autres paquets au corps exact de la requête interne (impression réelle, D-139). */
public final class RenduPdfEssai {

    private RenduPdfEssai() {}

    public static Object requete(Document d) {
        return RenduPdfRestAdapter.requete(d);
    }
}
