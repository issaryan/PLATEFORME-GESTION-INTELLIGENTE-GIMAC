package cm.gimac.des.gateway.domain.sandbox.port;

import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import java.util.Optional;

/** Émission du jeton interne réduit (signé par la passerelle, RS256, lié au certificat mTLS quand il existe). */
public interface EmetteurJetonReduit {
    /** Jeton sérialisé, vide si l'émission est impossible (émetteur absent ou en échec). */
    Optional<String> emettre(JetonReduit jeton);
}
