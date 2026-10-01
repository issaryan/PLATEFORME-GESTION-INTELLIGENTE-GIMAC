package cm.gimac.des.gateway.domain.connexion;

import java.util.List;

/** Issue d'une connexion réussie (contrat D-65 §2) : code requis, premier accès ou session ouverte. */
public sealed interface ResultatConnexion {
    /** Mot de passe vérifié, compte configuré : l'écran demande le code TOTP ; aucune session. */
    record CodeRequis() implements ResultatConnexion {}

    /** Session restreinte ouverte (acr 1) : assistant de premier accès. */
    record PremierAcces(List<String> etapes) implements ResultatConnexion {
        public PremierAcces {
            etapes = List.copyOf(etapes);
        }
    }

    /** Session complète ouverte (acr 2). */
    record Connecte() implements ResultatConnexion {}
}
