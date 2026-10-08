package cm.gimac.des.homologation.config;

import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.TopologicalSorter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Services de domaine purs déclarés comme beans : ils ne portent aucune annotation Spring (PI HOM-02 A2). */
@Configuration
public class DomainServicesConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TopologicalSorter topologicalSorter() {
        return new TopologicalSorter();
    }

    /**
     * Contrôle du référentiel ; gestes de certification attendus par jalon lus en configuration
     * ({@code gimac.homologation.referentiel.gestes-certification.toujours.<JALON>} et {@code .avec-attestations.<JALON>} :
     * déclencheurs séparés par des virgules), docs/lot1/effet-reel-referentiels.md §10.5.
     */
    @Bean
    public ReferentialGraphValidator referentialGraphValidator(TopologicalSorter sorter, org.springframework.core.env.Environment env) {
        return new ReferentialGraphValidator(
            sorter,
            new ReferentialGraphValidator.ExigencesCertification(exigences(env, "toujours"), exigences(env, "avec-attestations"))
        );
    }

    static java.util.Map<cm.gimac.des.homologation.domain.parcours.Jalon, java.util.List<String>> exigences(org.springframework.core.env.Environment env, String famille) {
        java.util.Map<cm.gimac.des.homologation.domain.parcours.Jalon, java.util.List<String>> m = new java.util.EnumMap<>(cm.gimac.des.homologation.domain.parcours.Jalon.class);
        org.springframework.boot.context.properties.bind.Binder.get(env)
            .bind("gimac.homologation.referentiel.gestes-certification." + famille, org.springframework.boot.context.properties.bind.Bindable.mapOf(String.class, String.class))
            .orElse(java.util.Map.of())
            .forEach((jalon, liste) ->
                m.put(
                    cm.gimac.des.homologation.domain.parcours.Jalon.valueOf(jalon.trim()),
                    java.util.Arrays.stream(liste.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList()
                )
            );
        return m;
    }

    @Bean
    public cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy liaisonOpeningPolicy() {
        return new cm.gimac.des.homologation.domain.service.LiaisonOpeningPolicy();
    }

    @Bean
    public cm.gimac.des.homologation.domain.service.ParcoursProjection parcoursProjection() {
        return new cm.gimac.des.homologation.domain.service.ParcoursProjection();
    }

    @Bean
    public cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur tableauDeBordCalculateur(
        cm.gimac.des.homologation.domain.service.ParcoursProjection projection,
        org.springframework.core.io.ResourceLoader chargeur,
        @org.springframework.beans.factory.annotation.Value("${gimac.homologation.tableau.compteurs:classpath:config/tableau-acteur-compteurs.json}") String emplacement
    ) throws java.io.IOException {
        // Compteurs du tableau d'acteur décrits en données (D-94 §3) : fichier livré, ou fichier externe de l'exploitation.
        try (var in = chargeur.getResource(emplacement).getInputStream()) {
            String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return new cm.gimac.des.homologation.domain.service.TableauDeBordCalculateur(projection, cm.gimac.des.homologation.domain.parcours.DefinitionCompteur.lire(json));
        }
    }

    @Bean
    public ReferentialCompletenessChecker referentialCompletenessChecker() {
        return new ReferentialCompletenessChecker();
    }
}
