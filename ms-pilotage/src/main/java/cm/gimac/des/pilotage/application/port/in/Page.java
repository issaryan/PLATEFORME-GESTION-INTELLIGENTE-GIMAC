package cm.gimac.des.pilotage.application.port.in;

import java.util.List;

/** Page d'une liste : contenu, nombre total, numéro de page (à partir de 0) et taille. */
public record Page<T>(List<T> contenu, long total, int page, int taille) {
    public Page {
        contenu = List.copyOf(contenu);
    }
}
