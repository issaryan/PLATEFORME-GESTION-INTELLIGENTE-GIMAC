package cm.gimac.des.document.application.usecase.gabarit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

/**
 * Polices de la charte (Ubuntu, Inter) et logos de Moabi et du GIMAC du gabarit commun (D-139), mêmes fichiers que
 * l'organigramme de MS-IAM, lus une fois dans les ressources de MS-DOCUMENT et rendus en data URI : le document
 * imprimé ne charge aucune ressource externe.
 */
final class RessourcesGabarit {

    private static volatile String polices;
    private static volatile String moabi;
    private static volatile String gimac;

    private RessourcesGabarit() {}

    static String logoMoabi() {
        if (moabi == null) {
            moabi = "data:image/svg+xml;base64," + base64("gabarit/moabi-logo.svg");
        }
        return moabi;
    }

    static String logoGimac() {
        if (gimac == null) {
            gimac = "data:image/png;base64," + base64("gabarit/gimac-logo-couleur.png");
        }
        return gimac;
    }

    static String polices() {
        if (polices == null) {
            String latin =
                "U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD";
            String latinExt = "U+0100-02BA,U+02BD-02C5,U+02C7-02CC,U+02CE-02D7,U+02DD-02FF,U+1D00-1DBF,U+1E00-1E9F,U+1EF2-1EFF,U+2020,U+20A0-20AB,U+20AD-20C0,U+2113,U+2C60-2C7F,U+A720-A7FF";
            StringBuilder b = new StringBuilder();
            for (Object[] p : new Object[][] { { "Ubuntu", "ubuntu", 500 }, { "Ubuntu", "ubuntu", 700 }, { "Inter", "inter", 400 }, { "Inter", "inter", 500 }, { "Inter", "inter", 600 } }) {
                for (String sous : List.of("latin", "latin-ext")) {
                    b.append("@font-face{font-family:'").append(p[0]).append("';font-style:normal;font-weight:").append(p[2]);
                    b.append(";src:url(data:font/woff2;base64,").append(base64("gabarit/polices/" + p[1] + "-" + sous + "-" + p[2] + "-normal.woff2")).append(") format('woff2')");
                    b.append(";unicode-range:").append(sous.equals("latin") ? latin : latinExt).append("}");
                }
            }
            polices = b.toString();
        }
        return polices;
    }

    static String base64(String chemin) {
        try (InputStream in = RessourcesGabarit.class.getClassLoader().getResourceAsStream(chemin)) {
            if (in == null) {
                throw new IllegalStateException("Ressource absente : " + chemin);
            }
            return Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
