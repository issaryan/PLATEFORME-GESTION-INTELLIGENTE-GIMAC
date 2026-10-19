package cm.gimac.des.monitoring.application.port.in;

import java.util.List;
import java.util.function.Function;

/** Page d'une liste : contenu, nombre total, numéro de page (à partir de 0) et taille. */
public record Page<T>(List<T> contenu, long total, int page, int taille) {
    public Page {
        contenu = List.copyOf(contenu);
    }

    public <R> Page<R> map(Function<T, R> f) {
        return new Page<>(contenu.stream().map(f).toList(), total, page, taille);
    }
}
