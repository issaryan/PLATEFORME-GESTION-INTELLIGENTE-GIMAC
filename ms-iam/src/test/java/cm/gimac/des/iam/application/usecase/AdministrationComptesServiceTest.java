package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Administration des comptes (D-62) sur l'annuaire de substitution : règles, journal, invitations. */
@ExtendWith(MockitoExtension.class)
class AdministrationComptesServiceTest {

    static final Set<String> CATALOGUE = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI", "ROLE_CHEF_DDRM", "ROLE_CHARGE_DDRM");
    static final Set<String> PRIVILEGIES = Set.of("ROLE_SUPER_ADMIN", "ROLE_RSSI");
    static final Acteur SUPER = new Acteur("demo-super-admin", "Super", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
    static final Acteur RSSI = new Acteur("demo-rssi", "RSSI", Set.of());

    @Mock
    JournalActes journal;

    @Mock
    JournalAdministrationRepository entrees;

    AnnuaireEnMemoire annuaire;
    AdministrationComptesService service;

    static IamProprietes proprietes() {
        return new IamProprietes(
            new IamProprietes.Keycloak("http://kc", "gimac", "iam-service", "s"),
            new IamProprietes.PremiereConnexion(List.of(ActionRequise.UPDATE_PASSWORD, ActionRequise.CONFIGURE_TOTP, ActionRequise.UPDATE_PROFILE), Duration.ofHours(72)),
            new IamProprietes.Profils(CATALOGUE, PRIVILEGIES),
            new IamProprietes.Webauthn("localhost", "GIMAC", List.of("http://localhost"), Duration.ofMinutes(2))
        );
    }

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(CATALOGUE), PRIVILEGIES);
        service = new AdministrationComptesService(annuaire, journal, entrees, proprietes(), Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZoneOffset.UTC));
    }

    NouveauCompte nouveau(String id, String... profils) {
        return new NouveauCompte(id, "Prénom", "Nom", id + "@gimac.test", "+237600000000", "Chargé", null, Set.of(profils));
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Création par le RSSI : actions de premier accès, invitation, journal ; mot de passe provisoire rendu une fois")
    void creation() {
        var c = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false);
        assertThat(c.compte().actionsRequises()).containsExactly(ActionRequise.UPDATE_PASSWORD, ActionRequise.CONFIGURE_TOTP, ActionRequise.UPDATE_PROFILE);
        assertThat(c.compte().premierAccesEnAttente()).isTrue();
        assertThat(c.invitationEnvoyee()).isTrue();
        assertThat(c.motDePasseProvisoire()).isNull();
        assertThat(annuaire.invitations()).containsExactly(c.compte().id());
        verify(journal).compte(eq(RSSI), eq("CREATION"), eq(c.compte().id()), eq("agent.ddrm"), isNull(), anyList());

        var p = service.creer(RSSI, nouveau("agent.deux", "ROLE_CHARGE_DDRM"), false, true);
        assertThat(p.motDePasseProvisoire()).startsWith("Provisoire-");
    }

    @Test
    @DisplayName("Création refusée : identifiant invalide, courriel absent, doublon, aucun mode d'accès, profil privilégié par le RSSI")
    void creationRefusee() {
        refus(() -> service.creer(RSSI, nouveau("Majuscules", "ROLE_CHARGE_DDRM"), true, false), "IAM_USERNAME_INVALID");
        refus(() -> service.creer(RSSI, new NouveauCompte("sans.courriel", "a", "b", " ", null, null, null, Set.of("ROLE_CHARGE_DDRM")), true, false), "IAM_EMAIL_REQUIRED");
        refus(() -> service.creer(RSSI, nouveau("rssi", "ROLE_CHARGE_DDRM"), true, false), "IAM_ACCOUNT_CONFLICT");
        refus(() -> service.creer(RSSI, new NouveauCompte("autre", "a", "b", "rssi@gimac.test", null, null, null, Set.of("ROLE_CHARGE_DDRM")), true, false), "IAM_ACCOUNT_CONFLICT");
        refus(() -> service.creer(RSSI, nouveau("sans.acces", "ROLE_CHARGE_DDRM"), false, false), "IAM_ONBOARDING_REQUIRED");
        refus(() -> service.creer(RSSI, nouveau("futur.admin", "ROLE_SUPER_ADMIN"), true, false), "IAM_PRIVILEGED_PROFILE");
        assertThat(service.creer(SUPER, nouveau("futur.admin", "ROLE_SUPER_ADMIN"), true, false).compte().profils()).containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("Liste filtrée et paginée ; indicateurs du tableau de bord")
    void listeEtIndicateurs() {
        String id = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        assertThat(service.lister(new AdministrationComptesService.Filtre("agent", null, null), 0, 25).total()).isEqualTo(1);
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, "ROLE_RSSI", null), 0, 25).contenu()).extracting(Compte::identifiant).containsExactly("rssi");
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "PREMIER_ACCES"), 0, 25).total()).isEqualTo(1);
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "SANS_2FA"), 0, 25).total()).isEqualTo(1);
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "ACTIF"), 1, 2).contenu()).hasSize(1);
        service.definirActif(RSSI, id, false, "départ");
        assertThat(service.lister(new AdministrationComptesService.Filtre(null, null, "INACTIF"), 0, 25).total()).isEqualTo(1);
        refus(() -> service.lister(new AdministrationComptesService.Filtre(null, null, "BIDON"), 0, 25), "IAM_FILTER_INVALID");
        when(entrees.countByHorodatageAfter(any())).thenReturn(4L);
        var i = service.indicateurs();
        assertThat(i.total()).isEqualTo(3);
        assertThat(i.actifs()).isEqualTo(2);
        assertThat(i.inactifs()).isEqualTo(1);
        assertThat(i.privilegies()).isEqualTo(2);
        assertThat(i.actesDepuis24Heures()).isEqualTo(4);
        assertThat(i.parProfil()).containsEntry("ROLE_RSSI", 1L);
    }

    @Test
    @DisplayName("Fiche, profils, activation, suppression, avec journal et motif")
    void cycleDeVie() {
        String id = service.creer(SUPER, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        assertThat(service.modifier(RSSI, id, new FicheCompte("Jean", null, "jean@gimac.test", null, "Chef", null, null)).email()).isEqualTo("jean@gimac.test");
        refus(() -> service.modifier(RSSI, id, new FicheCompte(null, null, "rssi@gimac.test", null, null, null, null)), "IAM_ACCOUNT_CONFLICT");
        refus(() -> service.modifier(RSSI, id, new FicheCompte(null, null, null, "xx", null, null, null)), "IAM_PHONE_INVALID");
        assertThat(service.definirProfils(RSSI, id, Set.of("ROLE_CHEF_DDRM"), "promotion").profils()).containsExactly("ROLE_CHEF_DDRM");
        refus(() -> service.definirProfils(RSSI, id, Set.of("ROLE_CHEF_DDRM"), " "), "IAM_REASON_REQUIRED");
        refus(() -> service.definirProfils(RSSI, "demo-rssi", Set.of(), "moi"), "IAM_SELF_ADMINISTRATION");
        assertThat(service.definirActif(RSSI, id, false, "départ").actif()).isFalse();
        assertThat(service.definirActif(RSSI, id, true, "retour").actif()).isTrue();
        service.supprimer(SUPER, id, "doublon");
        refus(() -> service.lire(id), "IAM_ACCOUNT_NOT_FOUND");
        verify(journal).compte(eq(SUPER), eq("SUPPRESSION"), eq(id), eq("agent.ddrm"), eq("doublon"), anyList());
    }

    @Test
    @DisplayName("Le dernier super administrateur actif ne peut être désactivé, supprimé ni perdre son profil")
    void dernierSuperAdmin() {
        refus(() -> service.definirActif(RSSI, "demo-super-admin", false, "x"), "IAM_LAST_SUPER_ADMIN");
        refus(() -> service.supprimer(RSSI, "demo-super-admin", "x"), "IAM_LAST_SUPER_ADMIN");
        Acteur autreSuper = new Acteur("autre", "Autre", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
        refus(() -> service.definirProfils(autreSuper, "demo-super-admin", Set.of("ROLE_RSSI"), "x"), "IAM_LAST_SUPER_ADMIN");
        service.creer(SUPER, nouveau("second.admin", "ROLE_SUPER_ADMIN"), true, false);
        assertThat(service.definirActif(RSSI, "demo-super-admin", false, "rotation").actif()).isFalse();
    }

    @Test
    @DisplayName("Actions de sécurité : actions requises (courriel), second facteur, mot de passe provisoire, sessions")
    void securite() {
        String id = service.creer(RSSI, nouveau("agent.ddrm", "ROLE_CHARGE_DDRM"), true, false).compte().id();
        annuaire.ouvrirSession(id, "10.0.0.1");
        assertThat(service.sessions(id)).hasSize(1);
        assertThat(service.exigerActions(RSSI, id, Set.of(ActionRequise.VERIFY_EMAIL), true, "courriel changé").actionsRequises()).contains(ActionRequise.VERIFY_EMAIL);
        assertThat(annuaire.invitations()).hasSize(2);
        refus(() -> service.exigerActions(RSSI, id, Set.of(), false, "x"), "IAM_ACTIONS_REQUIRED");
        assertThat(service.exigerActions(RSSI, id, Set.of(ActionRequise.UPDATE_PASSWORD), false, "rotation").actionsRequises()).contains(ActionRequise.UPDATE_PASSWORD);
        assertThat(service.reinitialiserSecondFacteur(RSSI, id, "perte du téléphone").secondFacteur()).isFalse();
        assertThat(service.sessions(id)).isEmpty();
        assertThat(service.motDePasseProvisoire(RSSI, id, "oubli")).isNotBlank();
        annuaire.ouvrirSession(id, "10.0.0.2");
        service.fermerSessions(RSSI, id, "incident");
        assertThat(service.sessions(id)).isEmpty();
        verify(journal, never()).compte(eq(RSSI), eq("SUPPRESSION"), any(), any(), any(), anyList());
    }
}
