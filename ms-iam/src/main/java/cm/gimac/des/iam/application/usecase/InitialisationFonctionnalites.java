package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.domain.fonctionnalite.Fonctionnalite;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Chargement, au démarrage, des valeurs de départ du catalogue des fonctionnalités (D-94) : fichier
 * {@code config/fonctionnalites-initiales.json} des ressources (gimac.iam.fonctionnalites.initiales), généré depuis
 * le registre de l'interface. Forme : objet {@code { "fonctionnalites": [ … ] }} (ou tableau seul), chaque entrée au
 * format servi par GET /api/v1/configuration/fonctionnalites. Idempotent ; un échec n'empêche pas le démarrage.
 */
@Component
public class InitialisationFonctionnalites {

    private static final Logger LOG = LoggerFactory.getLogger(InitialisationFonctionnalites.class);

    private final FonctionnalitesService service;
    private final ObjectMapper json;
    private final ResourceLoader ressources;
    private final String emplacement;

    public InitialisationFonctionnalites(
        FonctionnalitesService service,
        ObjectMapper json,
        ResourceLoader ressources,
        @Value("${gimac.iam.fonctionnalites.initiales:classpath:config/fonctionnalites-initiales.json}") String emplacement
    ) {
        this.service = service;
        this.json = json;
        this.ressources = ressources;
        this.emplacement = emplacement;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void auDemarrage() {
        try {
            FonctionnalitesService.Chargement c = charger();
            LOG.info(
                "Fonctionnalités : {} ajoutée(s), {} mise(s) à jour, {} retirée(s), {} modification(s) de la plateforme conservée(s), {} ignorée(s)",
                c.ajoutees(),
                c.remplacees(),
                c.retirees(),
                c.conservees(),
                c.ignorees()
            );
        } catch (IOException | RuntimeException e) {
            LOG.warn("Fonctionnalités : valeurs de départ non chargées ({})", e.getMessage());
        }
    }

    /** Lit le fichier des valeurs de départ et les charge. */
    public FonctionnalitesService.Chargement charger() throws IOException {
        return service.charger(lire());
    }

    /** Valeurs de départ du fichier ; fichier absent : aucune. */
    public List<Fonctionnalite> lire() throws IOException {
        Resource r = ressources.getResource(emplacement);
        if (!r.exists()) {
            LOG.warn("Fonctionnalités : fichier des valeurs de départ absent ({})", emplacement);
            return List.of();
        }
        try (InputStream in = r.getInputStream()) {
            JsonNode racine = json.readTree(in);
            JsonNode liste = racine.isArray() ? racine : racine.path("fonctionnalites");
            List<Fonctionnalite> l = new ArrayList<>();
            for (JsonNode n : liste) {
                l.add(json.treeToValue(n, Fonctionnalite.class));
            }
            return l;
        }
    }
}
