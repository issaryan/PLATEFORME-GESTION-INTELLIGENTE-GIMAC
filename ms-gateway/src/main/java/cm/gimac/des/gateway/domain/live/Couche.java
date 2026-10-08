package cm.gimac.des.gateway.domain.live;

import static cm.gimac.des.gateway.domain.live.EvenementLive.CERTIFICATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.COLLABORATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.HOMOLOGATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.NOTIFICATION;
import static cm.gimac.des.gateway.domain.live.EvenementLive.PRESENCE;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Couche de présentation qui ouvre le canal (/bff/{couche}/live) et sujets qu'elle peut recevoir.
 * La collaboration et la présence sont réservées aux acteurs internes (D-64, D-72) : DES et console, jamais
 * le portail. Les notifications de la plateforme (D-80) vont aussi au portail (D-93 §11.4), au seul destinataire.
 */
public enum Couche {
    DES(Set.of(HOMOLOGATION, CERTIFICATION, COLLABORATION, PRESENCE, NOTIFICATION)),
    PORTAIL(Set.of(HOMOLOGATION, CERTIFICATION, NOTIFICATION)),
    CONSOLE(Set.of(HOMOLOGATION, CERTIFICATION, COLLABORATION, PRESENCE, NOTIFICATION));

    /** Couche des acteurs internes, seuls à figurer dans la présence (D-72). */
    public boolean interne() {
        return this != PORTAIL;
    }

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
