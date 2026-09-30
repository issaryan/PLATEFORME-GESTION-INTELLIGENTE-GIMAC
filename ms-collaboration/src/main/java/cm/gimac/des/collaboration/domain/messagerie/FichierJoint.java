package cm.gimac.des.collaboration.domain.messagerie;

import java.io.IOException;
import java.io.InputStream;

/** Pièce jointe reçue, avant son dépôt dans MS-DOCUMENT : le contenu est lu à la demande. */
public record FichierJoint(String nom, String typeMime, long taille, Source source) {
    /** Accès au contenu, ouvert une fois par dépôt. */
    @FunctionalInterface
    public interface Source {
        InputStream ouvrir() throws IOException;
    }

    public FichierJoint avecNomEtType(String nouveauNom, String nouveauType) {
        return new FichierJoint(nouveauNom, nouveauType, taille, source);
    }
}
