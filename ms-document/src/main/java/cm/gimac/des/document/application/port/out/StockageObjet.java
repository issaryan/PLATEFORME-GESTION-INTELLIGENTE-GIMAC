package cm.gimac.des.document.application.port.out;

import java.util.Optional;

/** Port sortant : dépôt WORM des pièces (Ceph RGW en production, mémoire en développement). */
public interface StockageObjet {
    /** Référence de l'objet déposé : sa clé et la version attribuée par le dépôt. */
    record ObjetDepose(String cle, String version) {}

    ObjetDepose deposer(String cle, byte[] contenu, String typeMime, String sha256);

    Optional<byte[]> lire(String cle);
}
