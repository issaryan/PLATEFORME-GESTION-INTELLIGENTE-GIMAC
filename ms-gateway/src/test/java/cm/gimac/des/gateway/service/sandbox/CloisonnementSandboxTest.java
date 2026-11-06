package cm.gimac.des.gateway.service.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.EmetteurJetonReduit;
import cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox.Portee;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox.Demande;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox.Resultat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Décision temps réel de la passerelle sur les routes de la sandbox (US-EXI-04, 06, 07 et 24). */
class CloisonnementSandboxTest {

    static final RouteSandbox SIMULATION = new RouteSandbox("portail", "simulation");

    final List<JournalDecisionsSandbox.Entree> journal = new ArrayList<>();
    final List<JetonReduit> emis = new ArrayList<>();
    final AtomicReference<Portee> reponse = new AtomicReference<>();
    final AtomicReference<Throwable> panne = new AtomicReference<>();
    final List<String> appels = new ArrayList<>();
    EmetteurJetonReduit emetteur;
    boolean quotaAdmis;

    final IntrospectionSandbox introspection = (jeton, institution, composant) -> {
        appels.add(jeton + "|" + institution + "|" + composant);
        return panne.get() == null ? Mono.justOrEmpty(reponse.get()) : Mono.error(panne.get());
    };

    @BeforeEach
    void preparer() {
        reponse.set(new Portee(true, "u-1", "99001", List.of("SBX_SIM_SIMULATION", "NOT_INBOX_READ", "SBX_SIM_CONSULTATION"), "COMPTE"));
        panne.set(null);
        emetteur = j -> {
            emis.add(j);
            return Optional.of("jeton-reduit-" + j.institution());
        };
        quotaAdmis = true;
    }

    CloisonnementSandbox service(EmetteurJetonReduit e, Map<String, LimiteurTentatives> quotas) {
        return new CloisonnementSandbox(introspection, journal::add, quotas, e);
    }

    CloisonnementSandbox service() {
        return service(emetteur, Map.of("portail", cle -> Mono.just(quotaAdmis)));
    }

    static Demande demande(String institution, String demandee) {
        return new Demande(
            "u-1",
            institution,
            demandee,
            SIMULATION,
            "GET",
            "/bff/portail/sbx-simulation/essais",
            List.of("SBX_SIM_ADMINISTRATION"),
            "2",
            "10.0.0.1",
            "jeton-original-secret"
        );
    }

    @Test
    @DisplayName("Compte propre : portée IAM autoritaire, jeton réduit et décision auditée")
    void comptePropre() {
        Resultat r = service().evaluer(demande("99001", null)).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.decision().motif()).isEqualTo(Motif.INSTITUTION_PROPRE);
        assertThat(r.jeton()).contains("jeton-reduit-99001");
        assertThat(appels).containsExactly("jeton-original-secret|null|SBX-SIMULATION");
        assertThat(emis).singleElement().satisfies(j -> {
            assertThat(j.institution()).isEqualTo("99001");
            assertThat(j.institutionPropre()).isEqualTo("99001");
            assertThat(j.permissions()).containsExactly("SBX_SIM_CONSULTATION", "SBX_SIM_SIMULATION");
            assertThat(j.audience()).isEqualTo("sbx-simulation");
            assertThat(j.sujet()).isEqualTo("u-1");
        });
        assertThat(journal).singleElement().satisfies(e -> {
            assertThat(e.statut()).isEqualTo(200);
            assertThat(e.decision()).isEqualTo(r.decision());
        });
        assertThat(demande("99001", null).toString()).doesNotContain("jeton-original-secret").contains("jetonOriginal=****");
    }

    @Test
    @DisplayName("Mandat actif confirmé par IAM : institution et permissions du mandat relayées")
    void mandatActif() {
        // D-134 : l'institution propre est le code rendu par MS-IAM, jamais l'institution_id du jeton original (UUID).
        reponse.set(new Portee(true, "u-1", "99002", List.of("SBX_SIM_CONSULTATION"), "MANDAT", "99010"));
        Resultat r = service().evaluer(demande("6f1c2a3e-7777-4a6b-9c1d-000000000010", "99002")).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.decision().motif()).isEqualTo(Motif.MANDAT_ACTIF);
        assertThat(r.jeton()).contains("jeton-reduit-99002");
        assertThat(emis.get(0).institutionPropre()).isEqualTo("99010");
        assertThat(emis.get(0).permissions()).containsExactly("SBX_SIM_CONSULTATION");
        assertThat(appels).containsExactly("jeton-original-secret|99002|SBX-SIMULATION");
    }

    @Test
    @DisplayName("Mandat échu, suspension ou compte inactif : même 404, aucun ancien cache consulté")
    void refusTempsReelIndiscernables() {
        reponse.set(Portee.refusee());
        Resultat echu = service().evaluer(demande("99010", "99002")).block();
        Resultat suspension = service().evaluer(demande("99001", null)).block();
        assertThat(echu.statut()).isEqualTo(404);
        assertThat(suspension.statut()).isEqualTo(404);
        assertThat(echu.decision().motif()).isEqualTo(Motif.INTROSPECTION_REFUSEE);
        assertThat(suspension.decision().motif()).isEqualTo(Motif.INTROSPECTION_REFUSEE);
        assertThat(emis).isEmpty();
        assertThat(journal).hasSize(2).allSatisfy(e -> assertThat(e.statut()).isEqualTo(404));
        assertThat(new Portee(false, null, null, null, null).permissions()).isEmpty();
    }

    @Test
    @DisplayName("Prestataire autorisé sur SBX-SIMULATION seulement : portée sans institution ; hors composant IAM refuse")
    void prestataire() {
        reponse.set(new Portee(true, "prestataire", null, List.of(), "PRESTATAIRE"));
        Resultat admis = service().evaluer(demande("", null)).block();
        assertThat(admis.statut()).isEqualTo(200);
        assertThat(admis.decision().motif()).isEqualTo(Motif.PRESTATAIRE_AUTORISE);
        assertThat(emis.get(0).institution()).isEmpty();
        reponse.set(Portee.refusee());
        assertThat(service().evaluer(demande("", null)).block().statut()).isEqualTo(404);
        assertThat(appels).allSatisfy(appel -> assertThat(appel).endsWith("|SBX-SIMULATION"));
    }

    @Test
    @DisplayName("IAM indisponible ou réponse vide : refus 404 par défaut et décision auditée")
    void iamIndisponible() {
        panne.set(new IllegalStateException("IAM injoignable"));
        Resultat r = service().evaluer(demande("99001", null)).block();
        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.decision().motif()).isEqualTo(Motif.IAM_INDISPONIBLE);
        assertThat(journal.get(0).decision().motif()).isEqualTo(Motif.IAM_INDISPONIBLE);
        panne.set(null);
        reponse.set(null);
        assertThat(service().evaluer(demande("99001", null)).block().decision().motif()).isEqualTo(Motif.IAM_INDISPONIBLE);
    }

    @Test
    @DisplayName("Sans émetteur de jeton réduit : refus pour tout compte (D-134, le jeton original n'est jamais relayé)")
    void jetonReduitIndisponible() {
        reponse.set(new Portee(true, "u-1", "99002", List.of(), "MANDAT"));
        assertThat(service(null, Map.of()).evaluer(demande("99010", "99002")).block().decision().motif()).isEqualTo(Motif.JETON_REDUIT_INDISPONIBLE);
        reponse.set(new Portee(true, "prestataire", null, List.of(), "PRESTATAIRE"));
        assertThat(service(null, Map.of()).evaluer(demande("", null)).block().statut()).isEqualTo(404);
        reponse.set(new Portee(true, "u-1", "99001", List.of(), "COMPTE"));
        Resultat propre = service(null, Map.of()).evaluer(demande("99001", null)).block();
        assertThat(propre.statut()).isEqualTo(404);
        assertThat(propre.decision().motif()).isEqualTo(Motif.JETON_REDUIT_INDISPONIBLE);
        assertThat(propre.jeton()).isEmpty();
    }

    static final List<String> DOUBLE_ROLE = List.of("SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "SBX_RAC_ADMINISTRATION", "SBX_RAC_CONSULTATION", "SBX_SIM_SIMULATION", "SBX_CER_CONSULTATION");

    static Demande demande(String contexte) {
        return new Demande("u-1", "99001", null, SIMULATION, "GET", "/bff/portail/sbx-simulation/essais", DOUBLE_ROLE, "2", "10.0.0.1", "jeton-original-secret", contexte);
    }

    @Test
    @DisplayName("D-133 : double rôle, contexte INTEGRATION par défaut, sans le droit d'administrer l'institution")
    void doubleRoleIntegrationParDefaut() {
        reponse.set(new Portee(true, "u-1", "99001", DOUBLE_ROLE, "COMPTE"));
        Resultat r = service().evaluer(demande((String) null)).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(emis).singleElement().satisfies(j -> {
            assertThat(j.contexte()).isEqualTo("INTEGRATION");
            assertThat(j.permissions()).doesNotContain("SBX_HAB_ADMINISTRATION").contains("SBX_RAC_ADMINISTRATION", "SBX_SIM_SIMULATION", "SBX_HAB_CONSULTATION");
        });
        assertThat(demande("INTEGRATION").toString()).contains("contexte=INTEGRATION");
    }

    @Test
    @DisplayName("D-133 : contexte ADMINISTRATION, administration de l'institution et consultations seulement")
    void doubleRoleAdministration() {
        reponse.set(new Portee(true, "u-1", "99001", DOUBLE_ROLE, "COMPTE"));
        assertThat(service().evaluer(demande("administration")).block().statut()).isEqualTo(200);
        assertThat(emis.get(0).contexte()).isEqualTo("ADMINISTRATION");
        assertThat(emis.get(0).permissions()).containsExactly("SBX_CER_CONSULTATION", "SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "SBX_RAC_CONSULTATION");
    }

    @Test
    @DisplayName("D-133 : contexte inconnu refusé ; double rôle sans jeton réduit refusé ; compte simple non réduit")
    void doubleRoleRefus() {
        reponse.set(new Portee(true, "u-1", "99001", DOUBLE_ROLE, "COMPTE"));
        Resultat inconnu = service().evaluer(demande("SUPERUTILISATEUR")).block();
        assertThat(inconnu.statut()).isEqualTo(404);
        assertThat(inconnu.decision().motif()).isEqualTo(Motif.CONTEXTE_INVALIDE);
        Resultat sansEmetteur = service(null, Map.of()).evaluer(demande("INTEGRATION")).block();
        assertThat(sansEmetteur.statut()).isEqualTo(404);
        assertThat(sansEmetteur.decision().motif()).isEqualTo(Motif.JETON_REDUIT_INDISPONIBLE);
        reponse.set(new Portee(true, "u-1", "99001", List.of("SBX_HAB_ADMINISTRATION", "SBX_HAB_CONSULTATION", "SBX_RAC_CONSULTATION"), "COMPTE"));
        assertThat(service().evaluer(demande("ADMINISTRATION")).block().statut()).isEqualTo(200);
        assertThat(emis.get(emis.size() - 1).contexte()).isNull();
        assertThat(emis.get(emis.size() - 1).permissions()).contains("SBX_HAB_ADMINISTRATION");
    }

    @Test
    @DisplayName("Mesure d'attente (déploiement sans émetteur) : jeton original relayé pour un compte du GIMAC ou d'une institution, jamais pour un mandat ou un prestataire")
    void jetonReduitNonObligatoire() {
        CloisonnementSandbox attente = new CloisonnementSandbox(introspection, journal::add, Map.of(), null, false);
        reponse.set(new Portee(true, "agent", null, List.of("SBX_CER_CONSULTATION"), "COMPTE"));
        Resultat console = attente.evaluer(demande("", null)).block();
        assertThat(console.statut()).isEqualTo(200);
        assertThat(console.decision().motif()).isEqualTo(Motif.COMPTE_GIMAC);
        assertThat(console.jeton()).isEmpty();
        reponse.set(new Portee(true, "u-1", "99001", List.of(), "COMPTE", "99001"));
        assertThat(attente.evaluer(demande("99001", null)).block().statut()).isEqualTo(200);
        reponse.set(new Portee(true, "u-1", "99002", List.of(), "MANDAT", "99010"));
        assertThat(attente.evaluer(demande("99010", "99002")).block().decision().motif()).isEqualTo(Motif.JETON_REDUIT_INDISPONIBLE);
        reponse.set(new Portee(true, "prestataire", null, List.of(), "PRESTATAIRE"));
        assertThat(attente.evaluer(demande("", null)).block().statut()).isEqualTo(404);
    }

    @Test
    @DisplayName("Quota dépassé après introspection admise : 429 et décision journalisée")
    void quota() {
        List<String> cles = new ArrayList<>();
        quotaAdmis = false;
        CloisonnementSandbox s = service(emetteur, Map.of("portail", cle -> {
            cles.add(cle);
            return Mono.just(quotaAdmis);
        }));
        Resultat r = s.evaluer(demande("99001", null)).block();
        assertThat(r.statut()).isEqualTo(429);
        assertThat(r.decision().motif()).isEqualTo(Motif.QUOTA_DEPASSE);
        assertThat(cles).containsExactly("sbx:portail:99001:simulation");
        assertThat(journal.get(0).statut()).isEqualTo(429);
    }

    @Test
    @DisplayName("Compte du GIMAC : quota par sujet IAM, jeton réduit sans institution")
    void compteGimac() {
        reponse.set(new Portee(true, "u-gimac", null, List.of("SBX_TRA_CONSULTATION"), "COMPTE"));
        List<String> cles = new ArrayList<>();
        CloisonnementSandbox s = service(emetteur, Map.of("console", cle -> {
            cles.add(cle);
            return Mono.just(true);
        }));
        Demande d = new Demande("u-gimac", "", null, new RouteSandbox("console", "trace"), "GET", "/bff/console/sbx-trace/x", List.of(), "2", "", "j");
        Resultat r = s.evaluer(d).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.decision().motif()).isEqualTo(Motif.COMPTE_GIMAC);
        assertThat(cles).containsExactly("sbx:console:gimac:u-gimac:trace");
        assertThat(emis.get(0).institution()).isEmpty();
        assertThat(appels).containsExactly("j|null|SBX-TRACE");
    }
}
