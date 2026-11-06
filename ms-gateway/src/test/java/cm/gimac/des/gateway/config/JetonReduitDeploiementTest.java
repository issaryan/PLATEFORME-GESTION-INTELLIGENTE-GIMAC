package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Panne du 06/11/2026 sur le serveur 214 (D-134) : le jeton réduit était obligatoire alors que l'émetteur du jeton
 * interne n'est actif qu'avec gimac.securite.jeton-fantome.actif, posé dans aucune configuration de déploiement ; toute la
 * console de la sandbox répondait 404 (JETON_REDUIT_INDISPONIBLE). Chaque configuration de la passerelle et de ses
 * déploiements Compose doit donc rendre le jeton réduit obligatoire seulement si l'émetteur est actif.
 */
class JetonReduitDeploiementTest {

    static final String OBLIGATOIRE = "GIMAC_SANDBOX_JETON_REDUIT_OBLIGATOIRE";
    static final String EMETTEUR = "GIMAC_SECURITE_JETON_INTERNE_ACTIF";
    static final String CLE = "GIMAC_CLE_SIGNATURE_CHEMIN";

    @SuppressWarnings("unchecked")
    static Object valeur(Map<?, ?> racine, String... chemin) {
        Object n = racine;
        for (String c : chemin) {
            if (!(n instanceof Map<?, ?> m)) {
                return null;
            }
            n = m.get(c);
        }
        return n;
    }

    static boolean vrai(Object v) {
        return v != null && "true".equalsIgnoreCase(String.valueOf(v).replaceAll("^\\$\\{[A-Z_]+:(.*)}$", "$1"));
    }

    @Test
    @DisplayName("Profils de la passerelle : jeton réduit obligatoire seulement si l'émetteur du jeton interne est actif")
    void profilsDeLaPasserelle() throws Exception {
        List<Path> fichiers;
        try (Stream<Path> s = Files.list(Path.of("src/main/resources/config"))) {
            fichiers = s.filter(f -> f.getFileName().toString().matches("application.*\\.yml")).toList();
        }
        assertThat(fichiers).isNotEmpty();
        for (Path f : fichiers) {
            try (InputStream in = Files.newInputStream(f)) {
                for (Object doc : new Yaml().loadAll(in)) {
                    if (doc instanceof Map<?, ?> racine && vrai(valeur(racine, "gimac", "sandbox", "jeton-reduit", "obligatoire"))) {
                        assertThat(vrai(valeur(racine, "gimac", "securite", "jeton-fantome", "actif")) || vrai(valeur(racine, "gimac", "securite", "jeton-interne", "actif")))
                            .as(f.toString()).isTrue();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Déploiements Compose de la plateforme : même règle sur les variables de la passerelle")
    void deploiementsCompose() throws Exception {
        List<Path> compose = new ArrayList<>();
        try (Stream<Path> s = Files.list(Path.of("..", "infra", "docker-compose"))) {
            s.filter(f -> f.getFileName().toString().matches(".*\\.ya?ml")).forEach(compose::add);
        }
        assertThat(compose).isNotEmpty();
        for (Path f : compose) {
            String texte = Files.readString(f);
            if (texte.matches("(?s).*" + OBLIGATOIRE + "\\s*[:=]\\s*\"?true.*")) {
                assertThat(texte).as(f.toString()).containsPattern(EMETTEUR + "\\s*[:=]\\s*\"?true");
            }
            // Émetteur actif : clé persistante montée depuis un fichier hors dépôt, jamais une paire éphémère.
            if (texte.matches("(?s).*" + EMETTEUR + "\\s*[:=]\\s*\"?true.*")) {
                assertThat(texte).as(f.toString()).containsPattern(CLE + "\\s*[:=]").containsPattern("GIMAC_CLE_SIGNATURE_EPHEMERE\\s*[:=]\\s*\"?false");
            }
        }
    }

    @Test
    @DisplayName("Profil prod (application.yml puis application-prod.yml) : valeur résolue comme au démarrage, jeton réduit non obligatoire sans émetteur")
    void profilProdResolu() throws Exception {
        org.springframework.core.env.StandardEnvironment env = new org.springframework.core.env.StandardEnvironment();
        org.springframework.boot.env.YamlPropertySourceLoader yaml = new org.springframework.boot.env.YamlPropertySourceLoader();
        for (String f : new String[] { "application.yml", "application-prod.yml" }) {
            for (var source : yaml.load(f, new org.springframework.core.io.FileSystemResource("src/main/resources/config/" + f))) {
                env.getPropertySources().addFirst(source);
            }
        }
        env.setActiveProfiles("prod");
        boolean emetteur = Boolean.parseBoolean(env.resolvePlaceholders("${gimac.securite.jeton-fantome.actif:false}"))
            || Boolean.parseBoolean(env.resolvePlaceholders("${gimac.securite.jeton-interne.actif:false}"));
        String explicite = env.resolvePlaceholders("${gimac.sandbox.jeton-reduit.obligatoire:}");
        // Sans valeur explicite, la passerelle rend le jeton réduit obligatoire dès que l'émetteur est actif.
        boolean obligatoire = explicite.isBlank() ? emetteur : Boolean.parseBoolean(explicite);
        assertThat(!obligatoire || emetteur).isTrue();
    }

    @Test
    @DisplayName("Démarrage refusé si le jeton réduit est explicitement obligatoire sans émetteur ; sinon, obligatoire selon l'émetteur")
    void demarrageRefuse() {
        SandboxConfiguration c = new SandboxConfiguration();
        assertThatThrownBy(() ->
            c.cloisonnementSandbox(new SandboxProperties(), null, null, vide(), vide(), vide(), "true")
        ).isInstanceOf(IllegalStateException.class).hasMessageContaining("jeton-interne");
        assertThat(c.cloisonnementSandbox(new SandboxProperties(), null, null, vide(), vide(), vide(), "false")).isNotNull();
        assertThat(c.cloisonnementSandbox(new SandboxProperties(), null, null, vide(), vide(), vide(), "")).isNotNull();
    }

    static <T> org.springframework.beans.factory.ObjectProvider<T> vide() {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return null;
            }

            @Override
            public T getIfAvailable() {
                return null;
            }

            @Override
            public T getIfUnique() {
                return null;
            }

            @Override
            public T getObject() {
                return null;
            }
        };
    }
}
