package cm.gimac.des.gateway.domain.supervision;

import java.util.List;
import java.util.Map;

/** Configuration d'un service : profils actifs et propriétés retenues (triées), secrets masqués. */
public record ConfigurationService(String nom, String url, boolean lue, String erreur, List<String> profilsActifs, Map<String, String> proprietes) {
    public ConfigurationService {
        profilsActifs = List.copyOf(profilsActifs);
    }
}
