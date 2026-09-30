package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Qualités sur une opération (CR §3.6.1) : R et V uniques, C, I et E multiples. */
public record Raci(
    @JsonProperty("R") Personne r,
    @JsonProperty("C") List<Personne> c,
    @JsonProperty("V") Personne v,
    @JsonProperty("I") List<Personne> i,
    @JsonProperty("E") List<Personne> e
) {
    public Raci {
        c = c == null ? List.of() : List.copyOf(c);
        i = i == null ? List.of() : List.copyOf(i);
        e = e == null ? List.of() : List.copyOf(e);
    }

    public Raci avecR(Personne p) {
        return new Raci(p, c, v, i, e);
    }

    public Raci avecV(Personne p) {
        return new Raci(r, c, p, i, e);
    }

    /** Ajoute une personne à une qualité multiple (C, I, E), sans doublon. */
    public Raci ajouter(Qualite q, Personne p) {
        UnaryOperator<List<Personne>> plus = l -> {
            List<Personne> n = new ArrayList<>(l.stream().filter(x -> !x.id().equals(p.id())).toList());
            n.add(p);
            return n;
        };
        return switch (q) {
            case C -> new Raci(r, plus.apply(c), v, i, e);
            case I -> new Raci(r, c, v, plus.apply(i), e);
            case E -> new Raci(r, c, v, i, plus.apply(e));
            case R -> avecR(p);
            case V -> avecV(p);
        };
    }

    /** Remplace, dans toutes les qualités internes, la personne qui tient une fonction (affectation des ressources, rang 6). */
    public Raci remplacer(Fonction f, Personne p) {
        UnaryOperator<Personne> un = x -> x != null && x.fonction() == f ? p : x;
        UnaryOperator<List<Personne>> tous = l -> l.stream().map(un).distinct().toList();
        return new Raci(un.apply(r), tous.apply(c), un.apply(v), tous.apply(i), e);
    }
}
