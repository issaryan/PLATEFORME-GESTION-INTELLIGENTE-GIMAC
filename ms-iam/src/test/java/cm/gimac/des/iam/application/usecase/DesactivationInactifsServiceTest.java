package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.sandbox.VerrouTaches;
import cm.gimac.des.iam.application.usecase.sandbox.DesactivationInactifsService;
import cm.gimac.des.iam.application.usecase.sandbox.JournalSandbox;
import cm.gimac.des.iam.application.usecase.sandbox.Memoires;
import cm.gimac.des.iam.application.usecase.sandbox.ProprietesSandbox;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Désactivation des comptes inactifs de la sandbox (D-119, US-EXI-06 scénario 3, US-EXI-23), sans Docker : seuls les
 * comptes d'institution à rôle de la sandbox sont visés ; le compte protégé est écarté ; une seule réplique s'exécute.
 */
@ExtendWith(MockitoExtension.class)
class DesactivationInactifsServiceTest {

    static final String INST = "99001";

    @Mock
    JournalActes actes;

    @Mock
    JournalAdministrationRepository entrees;

    @Mock
    JournalSandbox journal;

    AnnuaireEnMemoire annuaire;
    Memoires.Activite activite;
    Memoires.Protections protections;
    Memoires.Horloge horloge;
    AtomicBoolean libre;
    DesactivationInactifsService service;
    Instant creation;

    static IamProprietes proprietes() {
        return new IamProprietes(
            new IamProprietes.Keycloak("http://kc", "gimac", "iam-service", "s"),
            new IamProprietes.PremiereConnexion(List.of(ActionRequise.MOT_DE_PASSE)),
            new IamProprietes.Profils(Set.of("ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR", "ROLE_CORRESPONDANT", "ROLE_SNA"), Set.of("ROLE_SBX_ADMIN_INSTITUTION", "ROLE_SBX_TESTEUR", "ROLE_CORRESPONDANT")),
            null,
            null,
            null,
            null
        );
    }

    String compte(String identifiant, String institution, String profil) {
        return annuaire.creer(new NouveauCompte(identifiant, "P", identifiant, identifiant + "@t.test", null, null, institution, Set.of(profil)), List.of());
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(proprietes().profils().attribuables()));
        for (String p : proprietes().profils().externesInitiaux()) {
            annuaire.definirExterne(p, true);
        }
        creation = Instant.now();
        horloge = new Memoires.Horloge(creation.plus(Duration.ofDays(100)));
        activite = new Memoires.Activite();
        protections = new Memoires.Protections();
        libre = new AtomicBoolean(true);
        VerrouTaches verrou = tache -> {
            assertThat(tache).isEqualTo(DesactivationInactifsService.TACHE);
            return libre.get();
        };
        CatalogueProfils catalogue = new CatalogueProfils(proprietes(), new DemandesEtProfilsServiceTest.ProfilsCreesEnMemoire(), annuaire);
        LiensCourriel liens = new LiensCourriel(JetonsEnMemoire.creer(new ArrayList<>()), new CourrielsPlateforme(new CourrielsEnMemoire()), proprietes(), horloge);
        AdministrationComptesService comptes = new AdministrationComptesService(annuaire, actes, entrees, proprietes(), liens, horloge, catalogue);
        service = new DesactivationInactifsService(annuaire, catalogue, activite, protections, comptes, journal, verrou, new ProprietesSandbox(90, null, null), horloge);
    }

    @Test
    @DisplayName("US-EXI-06 scénario 3 : le dernier administrateur protégé est écarté de la désactivation pour inactivité, les autres inactifs désactivés")
    void compteProtegeEcarte() {
        String admin = compte("admin.99001", INST, "ROLE_SBX_ADMIN_INSTITUTION");
        String testeur = compte("testeur.99001", INST, "ROLE_SBX_TESTEUR");
        String recent = compte("recent.99001", INST, "ROLE_SBX_TESTEUR");
        String correspondant = compte("corr.99001", INST, "ROLE_CORRESPONDANT");
        String interne = compte("agent.sna", null, "ROLE_SNA");
        activite.connexion(recent, horloge.instant().minus(Duration.ofDays(3)));
        protections.definir(admin, INST, true, "sa", horloge.instant());

        DesactivationInactifsService.Bilan b = service.executer();

        assertThat(b.execute()).isTrue();
        assertThat(b.desactives()).isEqualTo(1);
        assertThat(b.ecartes()).isEqualTo(1);
        assertThat(annuaire.lire(admin).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(testeur).orElseThrow().actif()).isFalse();
        assertThat(annuaire.lire(recent).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(correspondant).orElseThrow().actif()).isTrue();
        assertThat(annuaire.lire(interne).orElseThrow().actif()).isTrue();
        verify(journal).compte(eq(DesactivationInactifsService.SYSTEME), eq("DESACTIVATION_INACTIVITE_ECARTEE"), eq(admin), any(), any(), eq(List.of(INST)));
        verify(actes).compte(eq(DesactivationInactifsService.SYSTEME), eq("DESACTIVATION"), eq(testeur), any(), eq("Aucune connexion depuis 90 jours"), anyList());
    }

    @Test
    @DisplayName("Verrou tenu par une autre réplique : aucun passage")
    void verrouTenu() {
        String testeur = compte("testeur.99001", INST, "ROLE_SBX_TESTEUR");
        libre.set(false);
        DesactivationInactifsService.Bilan b = service.executer();
        assertThat(b.execute()).isFalse();
        assertThat(annuaire.lire(testeur).orElseThrow().actif()).isTrue();
        verify(journal, never()).compte(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Paramètres de la sandbox : valeurs par défaut (90 jours, mandat 90 jours, prestataire 8 heures)")
    void parametres() {
        ProprietesSandbox p = new ProprietesSandbox(null, null, null);
        assertThat(p.inactiviteJours()).isEqualTo(90);
        assertThat(p.mandatDureeMax()).isEqualTo(Duration.ofDays(90));
        assertThat(p.prestataireDureeMax()).isEqualTo(Duration.ofHours(8));
        assertThat(new ProprietesSandbox(0, Duration.ofDays(30), Duration.ofHours(4)).inactiviteJours()).isEqualTo(90);
        assertThat(new ProprietesSandbox(30, null, null).inactiviteJours()).isEqualTo(30);
    }
}
