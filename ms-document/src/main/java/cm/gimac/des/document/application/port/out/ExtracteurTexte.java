package cm.gimac.des.document.application.port.out;

import java.util.Optional;

/** Port sortant : extraction du texte d'une pièce déposée, pour le contrôle de masquage. */
public interface ExtracteurTexte {
    /** Texte de la pièce, ou vide si son type n'en porte pas. */
    Optional<String> extraire(byte[] contenu, String typeMime);
}
