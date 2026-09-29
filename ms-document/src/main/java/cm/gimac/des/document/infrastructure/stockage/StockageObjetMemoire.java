package cm.gimac.des.document.infrastructure.stockage;

import cm.gimac.des.document.application.port.out.StockageObjet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Substitut du dépôt objet (profils dev et test, PI §0.8) : objets en mémoire, perdus à l'arrêt.
 * Il imite un compartiment versionné : un second dépôt sous la même clé ajoute une version et
 * n'écrase jamais la précédente. Jamais sélectionné en production.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.stockage", havingValue = "stub")
public class StockageObjetMemoire implements StockageObjet {

    private final Map<String, List<byte[]>> objets = new ConcurrentHashMap<>();

    @Override
    public ObjetDepose deposer(String cle, byte[] contenu, String typeMime, String sha256) {
        List<byte[]> versions = objets.computeIfAbsent(cle, c -> new ArrayList<>());
        synchronized (versions) {
            versions.add(contenu.clone());
            return new ObjetDepose(cle, "v" + versions.size());
        }
    }

    @Override
    public Optional<byte[]> lire(String cle) {
        List<byte[]> versions = objets.get(cle);
        if (versions == null) {
            return Optional.empty();
        }
        synchronized (versions) {
            return Optional.of(versions.get(versions.size() - 1).clone());
        }
    }

    /** Nombre de versions conservées sous une clé (tests). */
    public int versions(String cle) {
        List<byte[]> versions = objets.get(cle);
        return versions == null ? 0 : versions.size();
    }
}
