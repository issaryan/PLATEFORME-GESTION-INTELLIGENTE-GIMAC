package cm.gimac.des.certification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.Couverture;
import cm.gimac.des.certification.config.AsyncSyncConfiguration;
import cm.gimac.des.certification.config.DatabaseTestcontainer;
import cm.gimac.des.certification.config.JacksonHibernateConfiguration;
import cm.gimac.des.certification.config.KafkaTestContainer;
import cm.gimac.des.certification.config.TestSecurityConfiguration;
import cm.gimac.des.certification.infrastructure.client.ContexteCertificationStubAdapter;
import cm.gimac.des.certification.infrastructure.client.FicheChangementStubAdapter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base des tests d'intégration des verrous de la décision : PostgreSQL et Kafka par Testcontainers,
 * adaptateurs de substitution (contexte de MS-HOMOLOGATION, MS-IAM, MS-DOCUMENT, cachet), jeton simulé à
 * deux facteurs (acr 2, D-61) portant les permissions voulues. Exécutés sur le poste de développement (D-60).
 */
@SpringBootTest(
    classes = {
        CertificationApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        KafkaTestContainer.class,
    }
)
@AutoConfigureMockMvc
public abstract class AbstractCertificationIT {

    /** Institution du participant des scénarios et une autre, pour le cloisonnement du portail. */
    public static final String INSTITUTION = "00000000-0000-4000-8000-00000000a042";
    public static final String AUTRE_INSTITUTION = "00000000-0000-4000-8000-00000000b017";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelaisOutboxDeTest relais;

    @Autowired
    protected ContexteCertificationStubAdapter contextes;

    @Autowired
    protected FicheChangementStubAdapter fiches;

    /**
     * Les scénarios passent par MockMvc et valident leurs transactions : leurs données sont effacées après
     * chaque test pour ne pas fausser les tests générés, qui comptent les lignes.
     */
    @org.junit.jupiter.api.AfterEach
    void effacerDonneesDuScenario() {
        effacer(jdbc);
    }

    /** La réserve de connexions n'est pas en validation automatique (JHipster) : validation explicite. */
    public static void effacer(JdbcTemplate jdbc) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                // TRUNCATE ne déclenche pas le refus ligne à ligne des signatures immuables (DELETE, UPDATE).
                st.execute(
                    "truncate table visa_securite, attestation_securite, approbation_securite, signature_verrou, proces_verbal_certification, " +
                    "outbox_event, processed_event cascade"
                );
                st.execute("delete from timestamp_token where objet_type = 'SIGNATURE'");
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
    }

    /** Contexte de MS-HOMOLOGATION (substitut) pour une nouvelle liaison. */
    public ContexteCertification liaison(String typeProjet, boolean fintech, int taux, boolean constatee, String institution) {
        UUID id = UUID.randomUUID();
        int denominateur = 4;
        int numerateur = taux * denominateur / 100;
        ContexteCertification c = new ContexteCertification(
            id,
            "LIA-2026-" + id.toString().substring(0, 3).toUpperCase(),
            institution,
            "Établissement de paiement A",
            typeProjet,
            fintech,
            "EN_TEST",
            new Couverture(taux, numerateur, denominateur),
            constatee,
            List.of("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE").subList(0, numerateur),
            UUID.randomUUID().toString(),
            "Conducteur de projet"
        );
        contextes.enregistrer(c);
        return c;
    }

    /** Jeton d'agent du DES : sujet, nom, acr 2 et permissions. */
    public static RequestPostProcessor agent(String sujet, String nom, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(sujet).claim("acr", "2").claim("name", nom))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    /** Jeton du correspondant du participant : institution, acr 2 et permissions du portail. */
    public static RequestPostProcessor correspondant(String institution, String... permissions) {
        return jwt()
            .jwt(j -> j.subject("corr-" + institution.substring(institution.length() - 4)).claim("acr", "2").claim("name", "Représentant du participant").claim("institution_id", institution))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }
}
