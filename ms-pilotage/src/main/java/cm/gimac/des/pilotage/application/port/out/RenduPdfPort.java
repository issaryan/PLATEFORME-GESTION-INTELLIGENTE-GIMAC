package cm.gimac.des.pilotage.application.port.out;

/** Rendu PDF mutualisé de MS-DOCUMENT. */
public interface RenduPdfPort {
    byte[] rendre(String html, String piedDePage);
}
