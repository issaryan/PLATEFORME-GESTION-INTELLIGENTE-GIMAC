package cm.gimac.des.gateway.domain.supervision;

import java.util.List;
import java.util.Map;

/** Environnement effectif d'un service (/management/env) : profils actifs et valeur retenue de chaque propriété. */
public record EnvironnementService(List<String> profilsActifs, Map<String, String> proprietes) {
    public EnvironnementService {
        profilsActifs = List.copyOf(profilsActifs);
        proprietes = Map.copyOf(proprietes);
    }
}
