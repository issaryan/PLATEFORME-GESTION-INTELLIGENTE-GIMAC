package cm.gimac.des.participant.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pièces du dossier d'adhésion (CR-CERT rang 2, D-57), paramétrées dans
 * gimac.participant.adhesion.pieces. Chaque candidat reçoit la liste en vigueur à son
 * enregistrement ; une modification ultérieure ne touche pas les dossiers déjà ouverts.
 */
@ConfigurationProperties(prefix = "gimac.participant.adhesion")
public record AdhesionProperties(List<PieceDefinition> pieces) {
    public record PieceDefinition(String code, String libelle, boolean obligatoire) {}

    public AdhesionProperties {
        pieces = pieces == null ? List.of() : List.copyOf(pieces);
    }
}
