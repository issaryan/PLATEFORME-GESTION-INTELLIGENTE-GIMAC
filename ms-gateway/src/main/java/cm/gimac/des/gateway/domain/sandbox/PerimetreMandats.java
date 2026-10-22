package cm.gimac.des.gateway.domain.sandbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Projection, dans la passerelle, du périmètre de visibilité d'un compte calculé par SBX-ESPACE (algorithme 6.4) :
 * institution propre et institutions mandantes avec la fin de leur mandat. Tenue à jour par les instantanés
 * sbx-espace.scope-changed.v1, remplacée seulement par une version supérieure.
 */
public record PerimetreMandats(String compte, String institutionPropre, Map<String, Instant> mandantes, long version) {

    public PerimetreMandats {
        Objects.requireNonNull(compte, "compte");
        institutionPropre = institutionPropre == null ? "" : institutionPropre;
        mandantes = Map.copyOf(mandantes);
    }

    /** Fin du mandat de cette institution mandante, nulle si le compte n'en a reçu aucun. */
    public Instant finMandat(String codeParticipant) {
        return mandantes.get(codeParticipant);
    }
}
