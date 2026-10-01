package cm.gimac.des.gateway.domain.live;

import static cm.gimac.des.gateway.domain.live.EvenementLive.CERTIFICATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.COLLABORATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.HOMOLOGATION;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Couche de présentation qui ouvre le canal (/bff/{couche}/live) et sujets qu'elle peut recevoir.
 * La collaboration est réservée aux acteurs internes (D-64) : DES et console, jamais le portail.
 */
public enum Couche {
    DES(Set.of(HOMOLOGATION, CERTIFICATION, COLLABORATION)),
    PORTAIL(Set.of(HOMOLOGATION, CERTIFICATION)),
    CONSOLE(Set.of(HOMOLOGATION, CERTIFICATION, COLLABORATION));

    private final Set<String> sujets;

    Couche(Set<String> sujets) {
        this.sujets = sujets;
    }

    public Set<String> sujets() {
        return sujets;
    }

    /** Couche nommée dans le chemin (des, portail, console), vide sinon. */
    public static Optional<Couche> depuis(String nom) {
        if (nom == null) {
            return Optional.empty();
        }
        String n = nom.toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(c -> c.name().equals(n)).findFirst();
    }
}
