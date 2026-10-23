package cm.gimac.des.iam.application.port.out.sandbox;

import java.util.Map;
import java.util.Optional;

/**
 * Lecture d'un jeton présenté à l'introspection (D-119) : signature, émetteur, audience et échéance vérifiés par le
 * décodeur du fournisseur d'identité du GIMAC ; vide si le jeton est invalide ou échu.
 */
public interface LecteurJeton {
    Optional<Map<String, Object>> lire(String jeton);
}
