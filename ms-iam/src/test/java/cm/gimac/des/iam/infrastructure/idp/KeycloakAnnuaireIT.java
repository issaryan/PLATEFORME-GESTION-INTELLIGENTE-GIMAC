package cm.gimac.des.iam.infrastructure.idp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.service.Totp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Adaptateur Keycloak de l'annuaire (D-62, D-65) face au vrai fournisseur d'identité, realm GIMAC
 * généré (infra/keycloak/gimac-realm.json) et extension gimac-totp : le compte de service iam-service
 * y porte les rôles d'administration du realm, le profil utilisateur déclare les attributs de la
 * plateforme (dont gimac_actions_requises), le client gimac-connexion vérifie le mot de passe par
 * l'octroi direct. Keycloak est lancé par Testcontainers, ou déjà démarré si GIMAC_KEYCLOAK_IT_URL
 * est fourni (GIMAC_KEYCLOAK_IT_SECRET : secret de iam-service, GIMAC_KEYCLOAK_IT_SECRET_CONNEXION :
 * secret de gimac-connexion).
 */
class KeycloakAnnuaireIT {

    static final String SECRET = "secret-iam-it";
    static final String SECRET_CONNEXION = "secret-connexion-it";
    static final String IMAGE = "quay.io/keycloak/keycloak:26.7.4-0";
    static final Path EXTENSION = Path.of("../infra/keycloak/extensions/gimac-totp");
    static final Path JAR = EXTENSION.resolve("target/gimac-totp.jar");

    static GenericContainer<?> conteneur;
    static Keycloak client;
    static KeycloakAnnuaire annuaire;
    static boolean extensionChargee;

    /**
     * Jar de l'extension gimac-totp : construit s'il manque (Maven Wrapper de MS-IAM, sans les tests de
     * l'extension, qui ont leur propre construction). Vide s'il ne peut l'être.
     */
    static boolean construireExtension() {
        if (Files.exists(JAR)) {
            return true;
        }
        if (!Files.exists(EXTENSION.resolve("pom.xml"))) {
            return false;
        }
        try {
            Process p = new ProcessBuilder(Path.of("mvnw").toAbsolutePath().toString(), "-q", "-B", "-ntp", "-f", EXTENSION.resolve("pom.xml").toString(), "package", "-DskipTests", "-Djacoco.skip=true")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
            return p.waitFor(5, TimeUnit.MINUTES) && p.exitValue() == 0 && Files.exists(JAR);
        } catch (java.io.IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @BeforeAll
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    @SuppressWarnings("resource")
    static void demarrer() {
        String url = System.getenv("GIMAC_KEYCLOAK_IT_URL");
        String secret = System.getenv().getOrDefault("GIMAC_KEYCLOAK_IT_SECRET", SECRET);
        String secretConnexion = System.getenv().getOrDefault("GIMAC_KEYCLOAK_IT_SECRET_CONNEXION", SECRET_CONNEXION);
        if (url == null || url.isBlank()) {
            conteneur = new GenericContainer<>(IMAGE)
                .withExposedPorts(8080, 9000)
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                .withEnv("KC_HEALTH_ENABLED", "true")
                .withEnv("GIMAC_SECRET_IAM_SERVICE", SECRET)
                .withEnv("GIMAC_SECRET_CONNEXION", SECRET_CONNEXION)
                .withEnv("GIMAC_SECRET_WEB_APP", "x")
                .withEnv("GIMAC_SECRET_GATEWAY_SERVICE", "x")
                .withEnv("GIMAC_SECRET_TESTER_CLI", "x")
                .withEnv("GIMAC_SECRET_HOMOLOGATION_SERVICE", "x")
                .withEnv("GIMAC_SECRET_CERTIFICATION_SERVICE", "x")
                .withEnv("GIMAC_SECRET_PARTICIPANT_SERVICE", "x")
                .withEnv("GIMAC_SECRET_GATEWAY_SUPERVISION", "x")
                .withEnv("GIMAC_SECRET_COLLABORATION_SERVICE", "x")
                .withEnv("GIMAC_SECRET_INTERNAL", "x")
                .withEnv("GIMAC_MOT_DE_PASSE_TEST", "Test-Gimac-2026!")
                .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("../infra/keycloak/gimac-realm.json")),
                    "/opt/keycloak/data/import/gimac-realm.json"
                )
                .withCommand("start-dev", "--import-realm")
                // Sonde de disponibilité de Keycloak (port de gestion), puis le domaine importé ; délai
                // large : le démarrage est lent sous la charge de constructions parallèles.
                .waitingFor(Wait.forHttp("/health/ready").forPort(9000).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(10)));
            extensionChargee = construireExtension();
            if (extensionChargee) {
                conteneur.withCopyFileToContainer(MountableFile.forHostPath(JAR), "/opt/keycloak/providers/gimac-totp.jar");
            }
            conteneur.start();
            url = "http://" + conteneur.getHost() + ":" + conteneur.getMappedPort(8080);
        } else {
            extensionChargee = true;
        }
        client = KeycloakBuilder.builder()
            .serverUrl(url)
            .realm("gimac")
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId("iam-service")
            .clientSecret(secret)
            .build();
        annuaire = new KeycloakAnnuaire(client, url, "gimac", "gimac-connexion", secretConnexion);
        // Le domaine est importé avant que la sonde ne réponde ; on attend qu'il serve des jetons.
        long fin = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (true) {
            try {
                client.tokenManager().getAccessTokenString();
                break;
            } catch (RuntimeException e) {
                if (System.nanoTime() > fin) throw e;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException i) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    @AfterAll
    static void arreter() {
        if (client != null) client.close();
        if (conteneur != null) conteneur.stop();
    }

    static String unique(String prefixe) {
        return prefixe + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static NouveauCompte nouveau(String identifiant, String... profils) {
        return new NouveauCompte(identifiant, "Awa", "Mballa", identifiant + "@gimac.test", "+237600000001", "Chargée DDRM", "INST-0042", Set.of(profils));
    }

    @Test
    @DisplayName("Création : attributs de la plateforme conservés par le profil utilisateur, profil et actions de premier accès")
    void creationEtRelecture() {
        String identifiant = unique("agent");
        String id = annuaire.creer(nouveau(identifiant, "ROLE_CHARGE_DDRM"), List.of(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR));
        // D-65 : aucune action requise de Keycloak, les actions sont celles de la plateforme (attribut).
        assertThat(client.realm("gimac").users().get(id).toRepresentation().getRequiredActions()).isNullOrEmpty();
        assertThat(client.realm("gimac").users().get(id).toRepresentation().getAttributes().get("gimac_actions_requises")).containsExactly("MOT_DE_PASSE", "SECOND_FACTEUR");
        Compte c = annuaire.lire(id).orElseThrow();
        assertThat(c.identifiant()).isEqualTo(identifiant);
        assertThat(c.telephone()).isEqualTo("+237600000001");
        assertThat(c.fonction()).isEqualTo("Chargée DDRM");
        assertThat(c.institutionId()).isEqualTo("INST-0042");
        assertThat(c.langue()).isEqualTo("fr");
        assertThat(c.profilComplet()).isFalse();
        assertThat(c.premierAccesEnAttente()).isTrue();
        assertThat(c.profils()).containsExactly("ROLE_CHARGE_DDRM");
        assertThat(c.actionsRequises()).containsExactlyInAnyOrder(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR);
        assertThat(c.secondFacteur()).isFalse();
        assertThat(c.creeLe()).isNotNull();

        assertThat(annuaire.parIdentifiant(identifiant)).map(Compte::id).contains(id);
        assertThat(annuaire.emailPris(identifiant + "@gimac.test", null)).isTrue();
        assertThat(annuaire.emailPris(identifiant + "@gimac.test", id)).isFalse();
        assertThat(annuaire.lister()).extracting(Compte::identifiant).contains(identifiant).noneMatch(u -> u.startsWith("service-account-"));
        assertThatThrownBy(() -> annuaire.creer(nouveau(identifiant, "ROLE_CHARGE_DDRM"), List.of())).isInstanceOfSatisfying(IamException.class, e ->
            assertThat(e.code()).isEqualTo("IAM_ACCOUNT_CONFLICT")
        );
    }

    @Test
    @DisplayName("Fiche, fin du premier accès, profils, activation, actions requises, second facteur, mot de passe provisoire")
    void cycleDeVie() {
        String id = annuaire.creer(nouveau(unique("cycle"), "ROLE_CHARGE_DDRM"), List.of());
        annuaire.modifierFiche(id, new FicheCompte("Aïcha", null, null, "+237699000000", "Cheffe DDRM", "en", "INST-0043"));
        annuaire.marquerProfilComplet(id, true);
        annuaire.definirProfils(id, Set.of("ROLE_CHEF_DDRM"));
        Compte c = annuaire.lire(id).orElseThrow();
        assertThat(c.prenom()).isEqualTo("Aïcha");
        assertThat(c.telephone()).isEqualTo("+237699000000");
        assertThat(c.langue()).isEqualTo("en");
        assertThat(c.institutionId()).isEqualTo("INST-0043");
        assertThat(c.profilComplet()).isTrue();
        assertThat(c.profils()).containsExactly("ROLE_CHEF_DDRM");

        annuaire.definirActif(id, false);
        assertThat(annuaire.lire(id).orElseThrow().actif()).isFalse();
        annuaire.definirActif(id, true);
        assertThat(annuaire.lire(id).orElseThrow().actif()).isTrue();

        annuaire.exigerActions(id, Set.of(ActionRequise.MOT_DE_PASSE));
        annuaire.exigerActions(id, Set.of(ActionRequise.SECOND_FACTEUR, ActionRequise.CHARTE));
        annuaire.reinitialiserSecondFacteur(id);
        assertThat(annuaire.lire(id).orElseThrow().actionsRequises()).containsExactly(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR, ActionRequise.CHARTE);
        annuaire.retirerActions(id, Set.of(ActionRequise.CHARTE, ActionRequise.FICHE));
        assertThat(annuaire.lire(id).orElseThrow().actionsRequises()).containsExactly(ActionRequise.MOT_DE_PASSE, ActionRequise.SECOND_FACTEUR);
        String provisoire = annuaire.definirMotDePasseProvisoire(id);
        assertThat(provisoire).hasSize(20);
        // Non temporaire : l'octroi direct l'accepte (D-65).
        assertThat(client.realm("gimac").users().get(id).credentials()).anyMatch(k -> "password".equals(k.getType()));
        assertThat(annuaire.sessions(id)).isEmpty();
        annuaire.fermerSessions(id);
        assertThatThrownBy(() -> annuaire.fermerSession(id, UUID.randomUUID().toString())).isInstanceOfSatisfying(IamException.class, e ->
            assertThat(e.status()).isEqualTo(404)
        );

        annuaire.supprimer(id);
        assertThat(annuaire.lire(id)).isEmpty();
        assertThatThrownBy(() -> annuaire.supprimer(id)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("D-76 : rattachement (attributs unite et responsable_id) conservé, recherche des comptes d'un responsable par attribut ; D-92 : comptes actifs")
    void rattachement() {
        String chef = annuaire.creer(nouveau(unique("chef"), "ROLE_CHARGE_DDRM"), List.of());
        String agent = annuaire.creer(nouveau(unique("agent"), "ROLE_CHARGE_DDRM"), List.of());
        String autre = annuaire.creer(nouveau(unique("autre"), "ROLE_CHARGE_DDRM"), List.of());
        annuaire.definirRattachement(agent, "POLE_RESEAU", chef);
        annuaire.definirRattachement(autre, "DES", chef + "-x");
        Compte c = annuaire.lire(agent).orElseThrow();
        assertThat(c.unite()).isEqualTo("POLE_RESEAU");
        assertThat(c.responsableId()).isEqualTo(chef);
        // La fiche modifiée ensuite garde le rattachement.
        annuaire.modifierFiche(agent, new FicheCompte(null, null, null, null, "Technicien réseau", null, null));
        assertThat(annuaire.lire(agent).orElseThrow().unite()).isEqualTo("POLE_RESEAU");
        assertThat(annuaire.parResponsable(chef)).extracting(Compte::id).containsExactly(agent);
        assertThat(annuaire.parResponsable(chef)).extracting(Compte::fonction).containsExactly("Technicien réseau");
        assertThat(annuaire.parResponsable(agent)).isEmpty();
        // D-81 : membres d'une unité, recherche par attribut (correspondance exacte).
        String cellule = "CELLULE_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        annuaire.definirRattachement(autre, cellule, null);
        assertThat(annuaire.parUnite(cellule)).extracting(Compte::id).containsExactly(autre);
        assertThat(annuaire.parUnite(cellule + "_BIS")).isEmpty();
        annuaire.definirRattachement(agent, null, null);
        assertThat(annuaire.lire(agent).orElseThrow().unite()).isNull();
        assertThat(annuaire.lire(agent).orElseThrow().responsableId()).isNull();
        assertThat(annuaire.parResponsable(chef)).isEmpty();
        // D-92 : comptes actifs (périmètre TOUTES), allégés, sans compte de service ni compte désactivé.
        annuaire.definirActif(autre, false);
        List<Compte> actifs = annuaire.actifs();
        assertThat(actifs).extracting(Compte::id).contains(chef, agent).doesNotContain(autre);
        assertThat(actifs).allSatisfy(x -> {
            assertThat(x.actif()).isTrue();
            assertThat(x.identifiant()).doesNotStartWith("service-account-");
        });
        assertThat(actifs).filteredOn(x -> x.id().equals(chef)).singleElement().satisfies(x -> assertThat(x.profils()).isEmpty());
    }

    @Test
    @DisplayName("D-94 §3 : porteurs d'une permission lus dans la composition Keycloak, profil créé et recomposition compris ; externe et désactivé exclus")
    void porteursPermission() {
        String code = "ROLE_IT_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        annuaire.creerProfil(code, "Profil d'essai", Set.of("COL_MESSAGE_READ"));
        String interne = annuaire.creer(new NouveauCompte(unique("porteur"), "Awa", "Mballa", unique("porteur") + "@gimac.test", null, null, null, Set.of(code)), List.of());
        String externe = annuaire.creer(nouveau(unique("externe"), code), List.of());
        String parti = annuaire.creer(new NouveauCompte(unique("parti"), "Paul", "Ndi", unique("parti") + "@gimac.test", null, null, null, Set.of(code)), List.of());
        annuaire.definirActif(parti, false);
        try {
            assertThat(cm.gimac.des.iam.application.usecase.PorteursPermission.lire(annuaire, "HOM_DASHBOARD_READ")).extracting(Compte::id).doesNotContain(interne, externe, parti);
            annuaire.definirPermissionsProfil(code, Set.of("COL_MESSAGE_READ", "HOM_DASHBOARD_READ"));
            List<Compte> porteurs = cm.gimac.des.iam.application.usecase.PorteursPermission.lire(annuaire, "HOM_DASHBOARD_READ");
            assertThat(porteurs).extracting(Compte::id).contains(interne).doesNotContain(externe, parti);
            assertThat(porteurs).filteredOn(x -> x.id().equals(interne)).singleElement().satisfies(x -> {
                assertThat(x.profils()).contains(code);
                assertThat(x.nomAffiche()).isEqualTo("Awa Mballa");
            });
            assertThat(porteurs).allSatisfy(x -> {
                assertThat(x.actif()).isTrue();
                assertThat(x.interne()).isTrue();
            });
        } finally {
            annuaire.supprimer(interne);
            annuaire.supprimer(externe);
            annuaire.supprimer(parti);
            annuaire.supprimerProfil(code);
        }
    }

    @Test
    @DisplayName("D-93 §11 : comptes d'une institution par l'attribut institution_id (correspondance exacte), comptes actifs seulement")
    void parInstitution() {
        String institution = "INST-" + UUID.randomUUID().toString().substring(0, 8);
        String actif = annuaire.creer(new NouveauCompte(unique("corr"), "Awa", "Mballa", unique("corr") + "@gimac.test", null, null, institution, Set.of("ROLE_CORRESPONDANT")), List.of());
        String parti = annuaire.creer(new NouveauCompte(unique("corrparti"), "Paul", "Ndi", unique("corrparti") + "@gimac.test", null, null, institution, Set.of("ROLE_CORRESPONDANT")), List.of());
        String voisin = annuaire.creer(new NouveauCompte(unique("voisin"), "Eric", "Ndi", unique("voisin") + "@gimac.test", null, null, institution + "-B", Set.of("ROLE_CORRESPONDANT")), List.of());
        annuaire.definirActif(parti, false);
        try {
            List<Compte> l = annuaire.parInstitution(institution);
            assertThat(l).extracting(Compte::id).containsExactly(actif);
            assertThat(l.get(0).institutionId()).isEqualTo(institution);
            assertThat(l.get(0).actif()).isTrue();
            assertThat(annuaire.parInstitution(institution + "-C")).isEmpty();
        } finally {
            annuaire.supprimer(actif);
            annuaire.supprimer(parti);
            annuaire.supprimer(voisin);
        }
    }

    @Test
    @DisplayName("D-81 : profil créé (rôle composite, description, permissions, repère gimac_profil_cree), attribuable, refus du doublon, suppression")
    void profilsCrees() {
        String code = "ROLE_IT_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        Set<String> permissions = Set.of("HOM_DASHBOARD_READ", "COL_MESSAGE_READ", "COL_MESSAGE_SEND", "PIL_ACTIVITY_OWN", "NOT_INBOX_READ");
        annuaire.creerProfil(code, "Profil d'essai", permissions);
        Profil p = annuaire.profils().stream().filter(x -> x.code().equals(code)).findFirst().orElseThrow();
        assertThat(p.description()).isEqualTo("Profil d'essai");
        assertThat(p.permissions()).containsExactlyInAnyOrderElementsOf(permissions);
        assertThat(p.titulaires()).isZero();
        assertThat(client.realm("gimac").roles().get(code).toRepresentation().getAttributes()).containsEntry("gimac_profil_cree", List.of("true"));
        assertThat(client.realm("gimac").roles().get(code).getRoleComposites()).extracting(org.keycloak.representations.idm.RoleRepresentation::getName).contains("ROLE_USER");
        assertThatThrownBy(() -> annuaire.creerProfil(code, "Doublon", Set.of())).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_PROFIL_EXISTANT"));

        String titulaire = annuaire.creer(nouveau(unique("titulaire"), code), List.of());
        assertThat(annuaire.lire(titulaire).orElseThrow().profils()).contains(code);
        assertThat(annuaire.profils().stream().filter(x -> x.code().equals(code)).findFirst().orElseThrow().titulaires()).isEqualTo(1);
        annuaire.supprimer(titulaire);
        annuaire.supprimerProfil(code);
        assertThat(annuaire.profils()).extracting(Profil::code).doesNotContain(code);
    }

    @Test
    @DisplayName("D-97 : permission de chaque fonction opérationnelle importée et composée dans les profils que le catalogue des fonctions associe")
    void permissionsDesFonctions() {
        assertThat(annuaire.permissions()).contains("HOM_FUNCTION_DDRM", "HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_NETWORK", "HOM_FUNCTION_SYSTEM", "HOM_FUNCTION_OPERATIONS");
        java.util.Map<String, Set<String>> attendues = java.util.Map.of(
            "ROLE_CHARGE_DDRM", Set.of("HOM_FUNCTION_DDRM"),
            "ROLE_PRODUCTION_MONETIQUE", Set.of("HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_OPERATIONS"),
            "ROLE_POLE_RESEAU", Set.of("HOM_FUNCTION_NETWORK"),
            "ROLE_POLE_SYSTEME", Set.of("HOM_FUNCTION_SYSTEM")
        );
        for (Profil p : annuaire.profils()) {
            Set<String> fonctions = new java.util.TreeSet<>(p.permissions());
            fonctions.removeIf(x -> !x.startsWith("HOM_FUNCTION_"));
            assertThat(fonctions).as(p.code()).isEqualTo(attendues.getOrDefault(p.code(), Set.of()));
        }
        assertThat(client.realm("gimac").roles().get("HOM_FUNCTION_DDRM").toRepresentation().getDescription()).isEqualTo("Tenir la fonction DDRM dans un projet d'homologation");
    }

    @Test
    @DisplayName("D-86 : libellé (attribut gimac_libelle) et mission des profils livrés importés ; modification du libellé et de la description, attributs conservés ; profils par compte")
    void presentationDesProfils() {
        var presentations = annuaire.presentationsProfils();
        assertThat(presentations.get("ROLE_POLE_RESEAU").libelle()).isEqualTo("Pôle réseau");
        assertThat(presentations.get("ROLE_DG").libelle()).isEqualTo("Direction générale");
        assertThat(presentations.values()).allSatisfy(p -> assertThat(p.description()).isNotEqualTo("Profil du lot 1"));
        assertThat(annuaire.profils()).filteredOn(p -> p.code().equals("ROLE_CHEF_DES")).singleElement().satisfies(p -> {
            assertThat(p.libelle()).isEqualTo("Chef du DES");
            assertThat(p.description()).startsWith("Coordonnateur technique : dirige le Département Exploitation et Supervision");
            assertThat(p.permissions()).contains("PIL_ACTIVITY_OWN");
        });

        // Profil créé avec son libellé, puis renommé : le repère gimac_profil_cree reste.
        String code = "ROLE_IT_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        annuaire.creerProfil(code, "Cellule d'essai", "Profil d'essai", Set.of("COL_MESSAGE_READ"));
        assertThat(annuaire.presentationsProfils().get(code).libelle()).isEqualTo("Cellule d'essai");
        annuaire.definirPresentationProfil(code, "Cellule renommée", "Nouvelle mission");
        var role = client.realm("gimac").roles().get(code).toRepresentation();
        assertThat(role.getDescription()).isEqualTo("Nouvelle mission");
        assertThat(role.getAttributes()).containsEntry("gimac_libelle", List.of("Cellule renommée")).containsEntry("gimac_profil_cree", List.of("true"));
        assertThat(annuaire.profils()).filteredOn(p -> p.code().equals(code)).singleElement().satisfies(p -> {
            assertThat(p.libelle()).isEqualTo("Cellule renommée");
            assertThat(p.description()).isEqualTo("Nouvelle mission");
            assertThat(p.permissions()).contains("COL_MESSAGE_READ");
        });
        // Libellé retiré : le code en tient lieu ; description vidée.
        annuaire.definirPresentationProfil(code, null, "");
        assertThat(annuaire.presentationsProfils().get(code).libelle()).isNull();
        assertThat(annuaire.profils()).filteredOn(p -> p.code().equals(code)).singleElement().extracting(Profil::libelle).isEqualTo(code);
        assertThatThrownBy(() -> annuaire.definirPresentationProfil("ROLE_INEXISTANT_IT", "X", "")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_PROFILE_NOT_FOUND"));

        // Profils par compte (membres d'une unité).
        String titulaire = annuaire.creer(nouveau(unique("presentation"), code), List.of());
        assertThat(annuaire.profilsParCompte().get(titulaire)).containsExactly(code);
        annuaire.supprimer(titulaire);
        annuaire.supprimerProfil(code);
        assertThat(annuaire.presentationsProfils()).doesNotContainKey(code);

        // Profil livré : modification puis retour à la valeur importée.
        var avant = annuaire.presentationsProfils().get("ROLE_SNA");
        annuaire.definirPresentationProfil("ROLE_SNA", "Normes, agréments et qualité", "Mission modifiée");
        assertThat(annuaire.presentationsProfils().get("ROLE_SNA").libelle()).isEqualTo("Normes, agréments et qualité");
        assertThat(client.realm("gimac").roles().get("ROLE_SNA").getRoleComposites()).isNotEmpty();
        annuaire.definirPresentationProfil("ROLE_SNA", avant.libelle(), avant.description());
        assertThat(annuaire.presentationsProfils().get("ROLE_SNA")).isEqualTo(avant);
    }

    @Test
    @DisplayName("D-94 : attribut gimac_externe d'un profil lu par l'annuaire, posé sans perdre les autres attributs, repli *_PORTAL_*")
    void profilExterne() {
        // Correspondant livré : externe (attribut du domaine, ou à défaut ses permissions du portail).
        assertThat(annuaire.profilsExternes()).contains("ROLE_CORRESPONDANT").doesNotContain("ROLE_SUPER_ADMIN", "ROLE_CHEF_DES");
        assertThat(annuaire.profils()).filteredOn(p -> p.code().equals("ROLE_CORRESPONDANT")).singleElement().extracting(Profil::externe).isEqualTo(true);

        String code = "ROLE_IT_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        annuaire.creerProfil(code, "Auditeur externe", "Profil d'essai", Set.of("HOM_PORTAL_READ"));
        // Sans attribut : règle de repli (permission du portail).
        assertThat(annuaire.attributsExternes()).doesNotContainKey(code);
        assertThat(annuaire.profilsExternes()).contains(code);
        // Attribut posé à faux : le profil n'est plus externe malgré sa permission du portail.
        annuaire.definirExterne(code, false);
        assertThat(annuaire.attributsExternes()).containsEntry(code, false);
        assertThat(annuaire.profilsExternes()).doesNotContain(code);
        assertThat(annuaire.profils()).filteredOn(p -> p.code().equals(code)).singleElement().extracting(Profil::externe).isEqualTo(false);
        var role = client.realm("gimac").roles().get(code).toRepresentation();
        assertThat(role.getAttributes())
            .containsEntry("gimac_externe", List.of("false"))
            .containsEntry("gimac_libelle", List.of("Auditeur externe"))
            .containsEntry("gimac_profil_cree", List.of("true"));
        // Profil sans permission du portail rendu externe par l'attribut seul.
        annuaire.definirPermissionsProfil(code, Set.of("COL_MESSAGE_READ"));
        annuaire.definirExterne(code, true);
        assertThat(annuaire.profilsExternes()).contains(code);
        assertThat(annuaire.compositions().get(code)).containsExactly("COL_MESSAGE_READ");
        // Le libellé modifié ensuite conserve l'attribut externe.
        annuaire.definirPresentationProfil(code, "Auditeur", "Mission");
        assertThat(annuaire.attributsExternes()).containsEntry(code, true);
        assertThatThrownBy(() -> annuaire.definirExterne("ROLE_INEXISTANT_IT", true)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_PROFILE_NOT_FOUND"));
        annuaire.supprimerProfil(code);
        assertThat(annuaire.attributsExternes()).doesNotContainKey(code);
    }

    @Test
    @DisplayName("Politique de sécurité : écrite dans le realm puis relue à l'identique")
    void politique() {
        PolitiqueSecurite p = new PolitiqueSecurite(14, 1, 1, 1, 1, 5, 90, true, 6, 30, "HmacSHA256", 30, 10, true, 5, 15);
        annuaire.appliquerPolitique(p);
        assertThat(annuaire.politique()).isEqualTo(p);
    }

    @Test
    @DisplayName("Messagerie : relue sans secret ; le mot de passe est conservé s'il n'est pas ressaisi")
    void messagerie() {
        annuaire.appliquerMessagerie(new ConfigurationMessagerie("smtp.gimac.test", 587, "des@gimac.test", "DES", true, false, true, "des", "s3cret", false));
        ConfigurationMessagerie lue = annuaire.messagerie();
        assertThat(lue.hote()).isEqualTo("smtp.gimac.test");
        assertThat(lue.motDePasse()).isNull();
        assertThat(lue.motDePasseDefini()).isTrue();
        annuaire.appliquerMessagerie(new ConfigurationMessagerie("smtp2.gimac.test", 465, "des@gimac.test", "DES", false, true, true, "des", null, true));
        assertThat(annuaire.messagerie().motDePasseDefini()).isTrue();
        assertThat(annuaire.messagerie().port()).isEqualTo(465);
        // Aucun serveur SMTP joignable : l'envoi d'essai échoue proprement.
        assertThatThrownBy(() -> annuaire.testerMessagerie("rssi@gimac.test")).isInstanceOf(IamException.class);
    }

    @Test
    @DisplayName("Profils par fonction : permissions composées, profils privilégiés signalés, composition modifiable")
    void profils() {
        List<Profil> profils = annuaire.profils();
        Profil superAdmin = profils.stream().filter(p -> p.code().equals("ROLE_SUPER_ADMIN")).findFirst().orElseThrow();
        assertThat(superAdmin.privilegie()).isTrue();
        // D-94 : privilégié par les permissions (IAM_*, PLATFORM_*), quel que soit le nom.
        assertThat(profils).filteredOn(p -> p.code().equals("ROLE_ADMIN_SYSTEME")).singleElement().extracting(Profil::privilegie).isEqualTo(true);
        assertThat(profils).filteredOn(p -> p.code().equals("ROLE_CHEF_DES")).singleElement().extracting(Profil::privilegie).isEqualTo(false);
        assertThat(annuaire.compositions().get("ROLE_SUPER_ADMIN")).isEqualTo(superAdmin.permissions());
        assertThat(superAdmin.permissions()).contains("IAM_ROLE_PRIVILEGED_GRANT", "PLATFORM_CONFIG_EDIT", "IAM_ACCOUNT_DELETE");
        Profil rssi = profils.stream().filter(p -> p.code().equals("ROLE_RSSI")).findFirst().orElseThrow();
        assertThat(rssi.permissions()).contains("IAM_ACCOUNT_CREATE", "PLATFORM_SECURITY_POLICY_EDIT").doesNotContain("IAM_ROLE_PRIVILEGED_GRANT");
        assertThat(annuaire.permissions()).contains("PAR_INSTITUTION_READ", "IAM_ACCOUNT_READ").noneMatch(p -> p.startsWith("ROLE_"));
        // CR v1.7 §3.3 : agent de la sécurité des SI, sous le RSSI, sans administration ni quitus (profil non privilégié).
        Profil agentSsi = profils.stream().filter(p -> p.code().equals("ROLE_AGENT_SSI")).findFirst().orElseThrow();
        assertThat(agentSsi.privilegie()).isFalse();
        assertThat(agentSsi.permissions()).contains("CER_SECURITY_VERIFY", "HOM_EVIDENCE_SUBMIT").noneMatch(p -> p.startsWith("IAM_") || p.startsWith("PLATFORM_") || p.startsWith("CER_QUITUS_"));
        // CR v1.7 §4.2.1 : la DDRM ouvre le projet ; le chef du DES n'ouvre plus de liaison.
        assertThat(profils).filteredOn(p -> p.code().equals("ROLE_CHEF_DES")).singleElement().satisfies(p -> assertThat(p.permissions()).contains("HOM_PROJECT_DESIGNATE").doesNotContain("HOM_LIAISON_OPEN"));
        assertThat(profils).filteredOn(p -> p.code().equals("ROLE_CHEF_DDRM")).singleElement().satisfies(p -> assertThat(p.permissions()).contains("HOM_LIAISON_OPEN"));

        Profil sna = profils.stream().filter(p -> p.code().equals("ROLE_SNA")).findFirst().orElseThrow();
        Set<String> avant = new TreeSet<>(sna.permissions());
        Set<String> apres = new TreeSet<>(avant);
        apres.add("HOM_EVIDENCE_FILE_EXPORT");
        annuaire.definirPermissionsProfil("ROLE_SNA", apres);
        assertThat(annuaire.profils().stream().filter(p -> p.code().equals("ROLE_SNA")).findFirst().orElseThrow().permissions()).contains("HOM_EVIDENCE_FILE_EXPORT");
        annuaire.definirPermissionsProfil("ROLE_SNA", avant);
        assertThat(annuaire.profils().stream().filter(p -> p.code().equals("ROLE_SNA")).findFirst().orElseThrow().permissions()).isEqualTo(avant);
    }

    @Test
    @DisplayName("Compte inconnu : absent ; fournisseur injoignable : erreur 503 explicite")
    void erreurs() {
        assertThat(annuaire.lire(UUID.randomUUID().toString())).isEmpty();
        try (
            Keycloak injoignable = KeycloakBuilder.builder()
                .serverUrl("http://127.0.0.1:1")
                .realm("gimac")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId("iam-service")
                .clientSecret("x")
                .build()
        ) {
            KeycloakAnnuaire horsLigne = new KeycloakAnnuaire(injoignable, "http://127.0.0.1:1/", "gimac", "gimac-connexion", "x");
            assertThatThrownBy(horsLigne::lister).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_IDP_UNAVAILABLE"));
            assertThatThrownBy(() -> horsLigne.verifierMotDePasse("agent", "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_IDP_UNAVAILABLE"));
            assertThatThrownBy(() -> horsLigne.enregistrerTotp("id", "JBSWY3DPEHPK3PXP", "123456", null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_IDP_UNAVAILABLE"));
            assertThatThrownBy(() -> horsLigne.definirMotDePasse("id", "Nouveau-Mot-2026!")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_IDP_UNAVAILABLE"));
        }
    }

    @Test
    @DisplayName("D-65 : mot de passe vérifié par l'octroi direct gimac-connexion, défini selon la politique, refus ramenés aux violations")
    void motsDePasse() {
        String identifiant = unique("mdp");
        String id = annuaire.creer(nouveau(identifiant, "ROLE_SNA"), List.of(ActionRequise.MOT_DE_PASSE));
        assertThat(annuaire.parEmail(identifiant.toUpperCase() + "@GIMAC.TEST")).map(Compte::id).contains(id);
        assertThat(annuaire.parEmail("personne-" + identifiant + "@gimac.test")).isEmpty();
        String provisoire = annuaire.definirMotDePasseProvisoire(id);
        assertThat(annuaire.verifierMotDePasse(identifiant, provisoire)).isTrue();
        assertThat(annuaire.verifierMotDePasse(identifiant, "faux-" + provisoire)).isFalse();
        assertThat(annuaire.verifierMotDePasse("inconnu-" + identifiant, provisoire)).isFalse();

        assertThatThrownBy(() -> annuaire.definirMotDePasse(id, "court")).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_PASSWORD_POLICY");
            assertThat(e.details().get("violations")).isEqualTo(List.of("LONGUEUR"));
        });
        annuaire.definirMotDePasse(id, "Nouveau-Mot-2026!");
        assertThat(annuaire.verifierMotDePasse(identifiant, "Nouveau-Mot-2026!")).isTrue();
        assertThat(annuaire.verifierMotDePasse(identifiant, provisoire)).isFalse();
        assertThat(client.realm("gimac").users().get(id).toRepresentation().getRequiredActions()).isNullOrEmpty();

        annuaire.appliquerPolitique(new PolitiqueSecurite(12, 1, 1, 1, 1, 3, 0, true, 6, 30, "HmacSHA1", 30, 10, true, 5, 15));
        assertThatThrownBy(() -> annuaire.definirMotDePasse(id, "Nouveau-Mot-2026!")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_PASSWORD_REUSED"));
        assertThatThrownBy(() -> annuaire.definirMotDePasse("inconnu", "Nouveau-Mot-2027!")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.status()).isEqualTo(404));

        assertThat(annuaire.lire(id).orElseThrow().emailVerifie()).isFalse();
        annuaire.marquerEmailVerifie(id);
        assertThat(annuaire.lire(id).orElseThrow().emailVerifie()).isTrue();
    }

    @Test
    @DisplayName("D-65 : code TOTP enregistré par l'extension gimac-totp (code vérifié), puis supprimé par la réinitialisation")
    void totp() {
        Assumptions.assumeTrue(extensionChargee, "extension gimac-totp non construite (infra/keycloak/extensions/gimac-totp)");
        String id = annuaire.creer(nouveau(unique("totp"), "ROLE_SNA"), List.of(ActionRequise.SECOND_FACTEUR));
        String secret = Totp.nouveauSecret(new SecureRandom());
        // Politique OTP courante du domaine (un autre test peut l'avoir changée).
        PolitiqueSecurite p = annuaire.politique();
        String juste = Totp.code(secret, Instant.now(), p.otpChiffres(), p.otpPeriodeSecondes(), p.otpAlgorithme());
        String faux = juste.startsWith("0") ? "1".repeat(juste.length()) : "0".repeat(juste.length());
        assertThatThrownBy(() -> annuaire.enregistrerTotp(id, secret, faux, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_TOTP_CODE_INVALID"));
        assertThat(annuaire.lire(id).orElseThrow().secondFacteur()).isFalse();
        annuaire.enregistrerTotp(id, secret, juste, "Téléphone \"pro\"");
        assertThat(annuaire.lire(id).orElseThrow().secondFacteur()).isTrue();
        assertThat(client.realm("gimac").users().get(id).credentials()).anyMatch(c -> "otp".equals(c.getType()) && "Téléphone \"pro\"".equals(c.getUserLabel()));
        assertThatThrownBy(() -> annuaire.enregistrerTotp(UUID.randomUUID().toString(), secret, juste, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.status()).isEqualTo(404));
        annuaire.reinitialiserSecondFacteur(id);
        assertThat(annuaire.lire(id).orElseThrow().secondFacteur()).isFalse();
    }
}
