package cm.gimac.des.homologation.config;

import cm.gimac.des.homologation.domain.parcours.Jalon;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

/** Accès des tests à la lecture des gestes de certification attendus (méthode du paquet de configuration). */
public final class DomainServicesConfigurationAcces {

    private DomainServicesConfigurationAcces() {}

    public static Map<Jalon, List<String>> exigences(Environment env, String famille) {
        return DomainServicesConfiguration.exigences(env, famille);
    }
}
