package cm.gimac.des.gateway.domain.live;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Connexion au canal poussé : couche, compte (claim « sub », identifiant IAM), institution
 * (claim « institution_id ») et sujets retenus. Porte les règles de remise d'un événement.
 *
 * @param sujets sujets demandés et permis à la couche
 */
public record Abonne(Couche couche, String compte, String institutionId, Set<String> sujets) {
    public Abonne {
        sujets = Set.copyOf(sujets);
    }

    /**
     * Abonné d'après le paramètre « sujets » (liste séparée par des virgules) : les sujets
     * inconnus ou interdits à la couche sont ignorés ; sans paramètre, tous ceux de la couche.
     */
    public static Abonne de(Couche couche, String compte, String institutionId, String sujetsDemandes) {
        Set<String> retenus = new LinkedHashSet<>();
        if (sujetsDemandes == null || sujetsDemandes.isBlank()) {
            retenus.addAll(couche.sujets());
        } else {
            Arrays.stream(sujetsDemandes.split(","))
                .map(String::trim)
                .filter(couche.sujets()::contains)
                .forEach(retenus::add);
        }
        return new Abonne(couche, compte, institutionId, retenus);
    }

    /**
     * Règles de remise : sujet retenu ; sur le portail, événement de l'institution de
     * l'utilisateur seulement (RG-59) ; en collaboration, utilisateur parmi les destinataires ;
     * présence (D-72) : tout abonné d'une couche interne, sauf pour sa propre présence.
     */
    public boolean recoit(EvenementLive e) {
        if (!sujets.contains(e.topic())) {
            return false;
        }
        if (couche == Couche.PORTAIL && (vide(institutionId) || !institutionId.equals(e.institutionId()))) {
            return false;
        }
        if (EvenementLive.PRESENCE.equals(e.topic())) {
            return !vide(compte) && !compte.equals(e.acteur());
        }
        if (EvenementLive.COLLABORATION.equals(e.topic())) {
            return !vide(compte) && e.destinataires().contains(compte);
        }
        return true;
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
