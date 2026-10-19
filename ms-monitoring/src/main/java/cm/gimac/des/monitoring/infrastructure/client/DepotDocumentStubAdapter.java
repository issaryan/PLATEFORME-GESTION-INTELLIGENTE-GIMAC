package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.DepotDocumentPort;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Substitut du dépôt (développement, tests ; jamais en production) : référence « DOC-LOCAL-… », dépôts retenus. */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "stub", matchIfMissing = true)
public class DepotDocumentStubAdapter implements DepotDocumentPort {

    public record Depot(String nom, String typeMime, String empreinte, int taille) {}

    private final List<Depot> depots = new CopyOnWriteArrayList<>();

    @Override
    public String deposer(byte[] contenu, String nom, String typeMime, String empreinte) {
        depots.add(new Depot(nom, typeMime, empreinte, contenu.length));
        return "DOC-LOCAL-" + empreinte.substring(0, 12);
    }

    public List<Depot> depots() {
        return List.copyOf(depots);
    }
}
