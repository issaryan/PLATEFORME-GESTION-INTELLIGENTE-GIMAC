package cm.gimac.des.homologation.domain.parcours;

import java.util.List;

/** Cérémonie de clés (C1, CR §5.4.1) : date de tenue, porteurs des trois composantes et leurs codes de contrôle. */
public record Ceremonie(String tenueLe, List<String> porteurs, List<String> codesControle) {
    public Ceremonie {
        porteurs = porteurs == null ? List.of() : List.copyOf(porteurs);
        codesControle = codesControle == null ? List.of() : List.copyOf(codesControle);
    }
}
