package cm.gimac.des.incident.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * Démarrage du service quelle que soit la sélection des adaptateurs (profil prod compris ; panne de MS-IAM du 06/11/2026,
 * D-134) : un composant Spring
 * qui déclare plusieurs constructeurs, publics ou non, en désigner un seul par @Autowired, sinon Spring cherche un
 * constructeur sans argument et le contexte ne démarre pas. Les adaptateurs « real », actifs seulement en prod, ne
 * sont jamais instanciés par les tests de contexte : ce contrôle porte sur tous les composants, actifs ou non.
 */
class ConstructeursInjectionTest {

    @Test
    @DisplayName("Tout composant à plusieurs constructeurs (publics ou non) en désigne un par @Autowired, ou garde un constructeur sans argument")
    void unSeulConstructeurDInjection() throws Exception {
        // Sans évaluer les @Conditional : les adaptateurs « real » (gimac.adapters.*=real) ne sont actifs qu'en prod.
        AnnotationTypeFilter composant = new AnnotationTypeFilter(Component.class);
        ClassPathScanningCandidateComponentProvider scan = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(MetadataReader lecteur) throws IOException {
                return composant.match(lecteur, getMetadataReaderFactory());
            }
        };
        List<String> fautifs = new ArrayList<>();
        int verifies = 0;
        for (BeanDefinition d : scan.findCandidateComponents("cm.gimac.des.incident")) {
            Class<?> c = Class.forName(d.getBeanClassName());
            verifies++;
            Constructor<?>[] constructeurs = Arrays.stream(c.getDeclaredConstructors()).filter(k -> !k.isSynthetic()).toArray(Constructor[]::new);
            if (constructeurs.length < 2) {
                continue;
            }
            long injection = Arrays.stream(constructeurs).filter(k -> k.isAnnotationPresent(Autowired.class)).count();
            boolean sansArgument = Arrays.stream(constructeurs).anyMatch(k -> k.getParameterCount() == 0);
            // Règle de Spring : le constructeur @Autowired, ou à défaut le constructeur sans argument ; sinon, échec.
            if (injection > 1 || (injection == 0 && !sansArgument)) {
                fautifs.add(c.getName());
            }
        }
        assertThat(fautifs).isEmpty();
        assertThat(verifies).as("composants parcourus").isPositive();
    }
}
