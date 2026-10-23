package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.service.Totp;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.courriel.CourrielsEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.AvatarCompteRepository;
import cm.gimac.des.iam.infrastructure.persistence.CleSecuriteRepository;
import cm.gimac.des.iam.infrastructure.persistence.EnrolementTotp;
import cm.gimac.des.iam.infrastructure.persistence.EnrolementTotpRepository;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffre;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffreRepository;
import cm.gimac.des.iam.infrastructure.securite.ChiffrementSecrets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** « Mon compte » (premier accès, mot de passe, code TOTP, libre-service) et configuration de l'authentification (D-62, D-65). */
@ExtendWith(MockitoExtension.class)
class MonCompteEtConfigurationTest {

    static final Acteur SUPER = new Acteur("demo-super-admin", "Super", Set.of("IAM_ROLE_PRIVILEGED_GRANT"));
    static final Acteur RSSI = new Acteur("demo-rssi", "RSSI", Set.of());

    @Mock
    JournalActes journal;

    @Mock
    CleSecuriteRepository cles;

    @Mock
    EnrolementTotpRepository enrolements;

    @Mock
    ParametreChiffreRepository parametres;

    @Mock
    ReauthentificationService reauth;

    @Mock
    AvatarCompteRepository avatars;

    final CourrielsEnMemoire courriels = new CourrielsEnMemoire();

    /** Instant réel : l'annuaire de substitution vérifie le code TOTP à l'heure du système. */
    static final Instant MAINTENANT = Instant.now();
    final ChiffrementSecrets chiffrement = new ChiffrementSecrets(null);
    final Map<UUID, EnrolementTotp> stock = new HashMap<>();
    AnnuaireEnMemoire annuaire;
    MonCompteService moi;
    ConfigurationAuthentificationService configuration;
    String nouveau;
    Acteur titulaire;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(
            Map.of("ROLE_SUPER_ADMIN", Set.of("IAM_ACCOUNT_READ"), "ROLE_RSSI", Set.of("IAM_ACCOUNT_READ"), "ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ")));
        Clock clock = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
        moi = new MonCompteService(
            annuaire,
            journal,
            cles,
            enrolements,
            chiffrement,
            reauth,
            AdministrationComptesServiceTest.proprietes(),
            avatars,
            new CourrielsPlateforme(courriels),
            clock
        );
        configuration = new ConfigurationAuthentificationService(annuaire, journal, AdministrationComptesServiceTest.proprietes(), parametres, chiffrement, clock);
        nouveau = annuaire.creer(
            new cm.gimac.des.iam.domain.compte.NouveauCompte("agent", "A", "B", "agent@gimac.test", null, null, null, Set.of("ROLE_CHARGE_DDRM")),
            java.util.List.of(ActionRequise.FICHE)
        );
        lenient().when(enrolements.save(any(EnrolementTotp.class))).thenAnswer(i -> {
            EnrolementTotp e = i.getArgument(0);
            stock.put(e.getId(), e);
            return e;
        });
        lenient().when(enrolements.findById(any(UUID.class))).thenAnswer(i -> Optional.ofNullable(stock.get((UUID) i.getArgument(0))));
        titulaire = new Acteur(nouveau, "Agent", Set.of());
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Premier accès : étapes restantes, puis fiche complète après la première configuration")
    void premierAcces() {
        when(cles.countBySujet(nouveau)).thenReturn(0L);
        assertThat(moi.moi(titulaire).etapesRestantes()).containsExactly("SECOND_FACTEUR", "FICHE", "CHARTE");
        refus(() -> moi.premiereConfiguration(titulaire, "+237600000000", "fr", null, false), "IAM_CHARTER_REQUIRED");
        refus(() -> moi.premiereConfiguration(titulaire, " ", "fr", null, true), "IAM_PHONE_REQUIRED");
        refus(() -> moi.premiereConfiguration(titulaire, "+237600000000", "de", null, true), "IAM_LANGUAGE_INVALID");
        var m = moi.premiereConfiguration(titulaire, "+237600000000", "en", "Chargé DDRM", true);
        assertThat(m.compte().profilComplet()).isTrue();
        assertThat(m.compte().langue()).isEqualTo("en");
        assertThat(m.etapesRestantes()).containsExactly("SECOND_FACTEUR");
        verify(journal).compte(eq(titulaire), eq("PREMIERE_CONFIGURATION"), eq(nouveau), eq("agent"), isNull(), anyList());
    }

    @Test
    @DisplayName("Libre-service : coordonnées, mot de passe, second facteur, sessions")
    void libreService() {
        assertThat(moi.modifier(titulaire, "Alice", null, "+237 699 99 99 99", null, "es").compte().prenom()).isEqualTo("Alice");
        refus(() -> moi.modifier(titulaire, null, null, null, null, "xx"), "IAM_LANGUAGE_INVALID");
        assertThat(moi.demander(titulaire, ActionRequise.MOT_DE_PASSE).compte().actionsRequises()).contains(ActionRequise.MOT_DE_PASSE);
        assertThat(moi.demander(titulaire, ActionRequise.SECOND_FACTEUR).compte().actionsRequises()).contains(ActionRequise.SECOND_FACTEUR);
        refus(() -> moi.demander(titulaire, ActionRequise.VERIFIER_COURRIEL), "IAM_ACTION_NOT_ALLOWED");
        String s = annuaire.ouvrirSession(nouveau, "10.1.1.1");
        annuaire.ouvrirSession(nouveau, "10.1.1.2");
        assertThat(moi.sessions(titulaire)).hasSize(2);
        moi.fermerSession(titulaire, s);
        refus(() -> moi.fermerSession(titulaire, s), "IAM_SESSION_NOT_FOUND");
        moi.fermerToutesSessions(titulaire);
        assertThat(moi.sessions(titulaire)).isEmpty();
        refus(() -> moi.moi(new Acteur("inconnu", "x", Set.of())), "IAM_ACCOUNT_NOT_FOUND");
    }

    @Test
    @DisplayName("Politique de sécurité : appliquée avec motif et différences journalisées ; refusée sous les planchers")
    void politique() {
        PolitiqueSecurite p = new PolitiqueSecurite(14, 1, 1, 1, 1, 6, 60, true, 6, 30, "HmacSHA256", 20, 8, true, 5, 30);
        assertThat(configuration.appliquerPolitique(RSSI, p, "revue annuelle")).isEqualTo(p);
        verify(journal).configuration(eq(RSSI), eq("POLITIQUE_SECURITE"), eq("revue annuelle"), anyList());
        assertThat(ConfigurationAuthentificationService.differences(p, p)).isEmpty();
        assertThat(configuration.politique().longueurMinimale()).isEqualTo(14);
        refus(() -> configuration.appliquerPolitique(RSSI, p, ""), "IAM_REASON_REQUIRED");
        PolitiqueSecurite faible = new PolitiqueSecurite(6, 0, 0, 0, 0, 0, 0, false, 6, 30, "HmacSHA1", 30, 10, true, 5, 15);
        refus(() -> configuration.appliquerPolitique(RSSI, faible, "x"), "IAM_POLICY_INVALID");
    }

    @Test
    @DisplayName("Messagerie : lue sans secret, appliquée, testée ; saisies invalides refusées")
    void messagerie() {
        ConfigurationMessagerie m = new ConfigurationMessagerie("smtp.gimac.cm", 587, "des@gimac.cm", "DES", true, false, true, "des", "secret", false);
        ConfigurationMessagerie lue = configuration.appliquerMessagerie(SUPER, m, "mise en service");
        assertThat(lue.motDePasse()).isNull();
        assertThat(lue.motDePasseDefini()).isTrue();
        // D-65 : copie chiffrée du mot de passe SMTP, pour les courriels envoyés par MS-IAM.
        ArgumentCaptor<ParametreChiffre> garde = ArgumentCaptor.forClass(ParametreChiffre.class);
        verify(parametres).save(garde.capture());
        assertThat(garde.getValue().getCle()).isEqualTo(ParametreChiffre.SMTP_MOT_DE_PASSE);
        assertThat(garde.getValue().getValeurChiffree()).doesNotContain("secret");
        assertThat(chiffrement.dechiffrer(garde.getValue().getValeurChiffree())).isEqualTo("secret");
        configuration.appliquerMessagerie(SUPER, new ConfigurationMessagerie("smtp.gimac.cm", 25, "des@gimac.cm", null, false, false, false, null, null, false), "sans authentification");
        verify(parametres).deleteById(ParametreChiffre.SMTP_MOT_DE_PASSE);
        configuration.appliquerMessagerie(SUPER, new ConfigurationMessagerie("smtp.gimac.cm", 587, "des@gimac.cm", null, true, false, true, "des", "", true), "mot de passe conservé");
        configuration.testerMessagerie("rssi@gimac.cm");
        refus(() -> configuration.testerMessagerie("pas-une-adresse"), "IAM_SMTP_INVALID");
        refus(() -> configuration.appliquerMessagerie(SUPER, new ConfigurationMessagerie("", 0, "x", null, false, false, false, null, null, false), "x"), "IAM_SMTP_INVALID");
        refus(() -> configuration.appliquerMessagerie(SUPER, new ConfigurationMessagerie("h", 25, "a@b.cm", null, false, false, true, " ", null, false), "x"), "IAM_SMTP_INVALID");
        annuaire.appliquerMessagerie(new ConfigurationMessagerie(" ", 25, "a@b.cm", null, false, false, false, null, null, false));
        refus(() -> configuration.testerMessagerie("rssi@gimac.cm"), "IAM_SMTP_FAILED");
    }

    @Test
    @DisplayName("Profils : composition des permissions, privilégiés réservés au super administrateur, permissions inconnues refusées")
    void profils() {
        assertThat(configuration.profils()).extracting("code").contains("ROLE_CHARGE_DDRM", "ROLE_RSSI");
        assertThat(configuration.permissions()).contains("IAM_ACCOUNT_READ", "PAR_INSTITUTION_READ");
        assertThat(configuration.definirPermissions(RSSI, "ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ", "HOM_DASHBOARD_READ"), "revue").permissions()).hasSize(2);
        // D-97 : les permissions des fonctions opérationnelles sont connues et non privilégiées, composables sans octroi privilégié.
        Set<String> fonctions = Set.of("HOM_FUNCTION_DDRM", "HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_NETWORK", "HOM_FUNCTION_SYSTEM", "HOM_FUNCTION_OPERATIONS");
        assertThat(configuration.permissions()).containsAll(fonctions);
        var compose = configuration.definirPermissions(RSSI, "ROLE_CHARGE_DDRM", fonctions, "fonctions opérationnelles");
        assertThat(compose.permissions()).containsExactlyInAnyOrderElementsOf(fonctions);
        assertThat(compose.privilegie()).isFalse();
        // D-94 : un profil livré qui reçoit une permission IAM_* ou PLATFORM_* devient privilégié : octroi privilégié exigé.
        refus(() -> configuration.definirPermissions(RSSI, "ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ", "IAM_ACCOUNT_READ"), "revue"), "IAM_PRIVILEGED_PROFILE");
        assertThat(configuration.definirPermissions(SUPER, "ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ", "IAM_ACCOUNT_READ"), "revue").privilegie()).isTrue();
        refus(() -> configuration.definirPermissions(RSSI, "ROLE_RSSI", Set.of(), "x"), "IAM_PRIVILEGED_PROFILE");
        refus(() -> configuration.definirPermissions(SUPER, "ROLE_INCONNU", Set.of(), "x"), "IAM_PROFILE_NOT_FOUND");
        refus(() -> configuration.definirPermissions(SUPER, "ROLE_RSSI", Set.of("PERM_INEXISTANTE"), "x"), "IAM_PERMISSION_UNKNOWN");
        assertThat(configuration.definirPermissions(SUPER, "ROLE_RSSI", Set.of("IAM_ACCOUNT_READ"), "x").privilegie()).isTrue();
    }

    @Test
    @DisplayName("D-65 : changement du mot de passe, actuel vérifié, politique et réutilisation contrôlées, action MOT_DE_PASSE retirée")
    void motDePasse() {
        annuaire.exigerActions(nouveau, Set.of(ActionRequise.MOT_DE_PASSE));
        String provisoire = annuaire.definirMotDePasseProvisoire(nouveau);
        refus(() -> moi.changerMotDePasse(titulaire, "faux", "Nouveau-Mot-2026!"), "IAM_PASSWORD_INVALID");
        refus(() -> moi.changerMotDePasse(titulaire, null, "Nouveau-Mot-2026!"), "IAM_PASSWORD_INVALID");
        refus(() -> moi.changerMotDePasse(titulaire, provisoire, provisoire), "IAM_PASSWORD_REUSED");
        assertThatThrownBy(() -> moi.changerMotDePasse(titulaire, provisoire, "court")).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_PASSWORD_POLICY");
            assertThat(e.details().get("violations")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).contains("LONGUEUR");
        });
        moi.changerMotDePasse(titulaire, provisoire, "Nouveau-Mot-2026!");
        assertThat(annuaire.verifierMotDePasse("agent", "Nouveau-Mot-2026!")).isTrue();
        assertThat(annuaire.lire(nouveau).orElseThrow().actionsRequises()).doesNotContain(ActionRequise.MOT_DE_PASSE);
        verify(journal).compte(eq(titulaire), eq("LIBRE_SERVICE_MOT_DE_PASSE"), eq(nouveau), eq("agent"), isNull(), anyList());
        refus(() -> moi.changerMotDePasse(titulaire, "Nouveau-Mot-2026!", provisoire), "IAM_PASSWORD_REUSED");
        // Courriel d'information au titulaire après tout changement (mon-compte.md §2).
        var info = courriels.dernierPour("agent@gimac.test").orElseThrow();
        assertThat(info.objet()).isEqualTo("Votre mot de passe a été modifié");
        assertThat(info.texte()).contains("/des/mon-compte").doesNotContain("Nouveau-Mot-2026!").contains("RSSI");
    }

    @Test
    @DisplayName("mon-compte.md §2 : fermerAutresSessions ferme toutes les sessions du fournisseur d'identité sauf la courante")
    void motDePasseFermerAutresSessions() {
        String provisoire = annuaire.definirMotDePasseProvisoire(nouveau);
        String courante = annuaire.ouvrirSession(nouveau, "10.0.0.1");
        annuaire.ouvrirSession(nouveau, "10.0.0.2");
        annuaire.ouvrirSession(nouveau, "10.0.0.3");
        assertThat(moi.changerMotDePasse(titulaire, provisoire, "Nouveau-Mot-2026!", true, courante)).isEqualTo(2);
        assertThat(annuaire.sessions(nouveau)).extracting(cm.gimac.des.iam.domain.compte.SessionCompte::id).containsExactly(courante);
        verify(journal).compte(eq(titulaire), eq("LIBRE_SERVICE_MOT_DE_PASSE"), eq(nouveau), eq("agent"), isNull(), eq(List.of("autres sessions fermées : 2")));
        // Sans session courante connue (jeton sans sid) : aucune session n'est fermée ici.
        annuaire.ouvrirSession(nouveau, "10.0.0.4");
        assertThat(moi.changerMotDePasse(titulaire, "Nouveau-Mot-2026!", "Autre-Mot-2026!x", true, null)).isZero();
        assertThat(annuaire.sessions(nouveau)).hasSize(2);
        // Option absente : rien n'est fermé.
        assertThat(moi.changerMotDePasse(titulaire, "Autre-Mot-2026!x", "Troisieme-Mot-2026!", false, courante)).isZero();
    }

    @Test
    @DisplayName("mon-compte.md §2 : une panne de la messagerie n'annule pas le changement de mot de passe")
    void motDePasseCourrielEnPanne() {
        moi = new MonCompteService(
            annuaire,
            journal,
            cles,
            enrolements,
            chiffrement,
            reauth,
            AdministrationComptesServiceTest.proprietes(),
            avatars,
            new CourrielsPlateforme(c -> {
                throw IamException.indisponible("IAM_SMTP_FAILED", "panne");
            }),
            Clock.systemUTC()
        );
        String provisoire = annuaire.definirMotDePasseProvisoire(nouveau);
        moi.changerMotDePasse(titulaire, provisoire, "Nouveau-Mot-2026!");
        assertThat(annuaire.verifierMotDePasse("agent", "Nouveau-Mot-2026!")).isTrue();
    }

    @Test
    @DisplayName("mon-compte.md §1 : GET /moi porte la version de la photo de profil, nulle sans photo")
    void avatarVersion() {
        when(avatars.version(nouveau)).thenReturn(Optional.empty(), Optional.of("0123456789abcdef"));
        assertThat(moi.moi(titulaire).avatarVersion()).isNull();
        assertThat(moi.moi(titulaire).avatarVersion()).isEqualTo("0123456789abcdef");
    }

    @Test
    @DisplayName("D-65 : enrôlement TOTP (secret base 32 chiffré, URI otpauth), confirmation par un code juste, action SECOND_FACTEUR retirée")
    void totp() {
        annuaire.exigerActions(nouveau, Set.of(ActionRequise.SECOND_FACTEUR));
        var e = moi.enrolementTotp(titulaire);
        assertThat(e.secret()).matches("[A-Z2-7]{32}");
        assertThat(e.uri()).isEqualTo("otpauth://totp/GIMAC%20DES:agent?secret=" + e.secret() + "&issuer=GIMAC%20DES&digits=6&period=30&algorithm=SHA1");
        assertThat(e.toString()).doesNotContain(e.secret());
        verify(enrolements).supprimerPour(nouveau);
        EnrolementTotp garde = stock.get(e.enrolementId());
        assertThat(garde.getSecretChiffre()).doesNotContain(e.secret());
        assertThat(garde.getExpireLe()).isEqualTo(MAINTENANT.plusSeconds(600));

        // Codes calculés à l'instant réel : l'annuaire en mémoire vérifie à Instant.now(), et MAINTENANT est fixé au
        // chargement de la classe (plus de 30 s plus tôt sur un poste chargé, d'où un code précédent déjà hors tolérance).
        Instant reel = Instant.now();
        String juste = Totp.code(e.secret(), reel, 6, 30, "HmacSHA1");
        String faux = juste.equals("000000") ? "111111" : "000000";
        refus(() -> moi.confirmerTotp(titulaire, e.enrolementId(), faux, null, null), "IAM_TOTP_CODE_INVALID");
        refus(() -> moi.confirmerTotp(titulaire, UUID.randomUUID(), juste, null, null), "IAM_TOTP_ENROLMENT_EXPIRED");
        refus(() -> moi.confirmerTotp(titulaire, null, juste, null, null), "IAM_TOTP_ENROLMENT_EXPIRED");
        refus(() -> moi.confirmerTotp(new Acteur("demo-rssi", "RSSI", Set.of()), e.enrolementId(), juste, null, null), "IAM_TOTP_ENROLMENT_EXPIRED");
        // Tolérance d'un pas (30 s) : le code précédent est encore admis.
        String precedent = Totp.code(e.secret(), reel.minusSeconds(30), 6, 30, "HmacSHA1");
        moi.confirmerTotp(titulaire, e.enrolementId(), " " + precedent + " ", "Téléphone", null);
        assertThat(annuaire.lire(nouveau).orElseThrow().secondFacteur()).isTrue();
        assertThat(annuaire.lire(nouveau).orElseThrow().actionsRequises()).doesNotContain(ActionRequise.SECOND_FACTEUR);
        assertThat(annuaire.libelleTotp(nouveau)).contains("Téléphone");
        verify(enrolements).delete(garde);
        verify(journal).compte(eq(titulaire), eq("LIBRE_SERVICE_SECOND_FACTEUR"), eq(nouveau), eq("agent"), isNull(), anyList());
    }

    @Test
    @DisplayName("D-65 : remplacer un code TOTP exige la réauthentification FIDO2 si une clé est enrôlée ; enrôlement illisible ou expiré refusé")
    void remplacementTotp() {
        annuaire.enregistrerTotp(nouveau, "JBSWY3DPEHPK3PXP", Totp.code("JBSWY3DPEHPK3PXP", Instant.now(), 6, 30, "HmacSHA1"), null);
        when(cles.countBySujet(nouveau)).thenReturn(1L);
        var e = moi.enrolementTotp(titulaire);
        // Codes calculés à l'instant réel : l'annuaire en mémoire vérifie à Instant.now(), et MAINTENANT est fixé au
        // chargement de la classe (plus de 30 s plus tôt sur un poste chargé, d'où un code précédent déjà hors tolérance).
        Instant reel = Instant.now();
        String juste = Totp.code(e.secret(), reel, 6, 30, "HmacSHA1");
        assertThatThrownBy(() -> moi.confirmerTotp(titulaire, e.enrolementId(), juste, null, " ")).isInstanceOfSatisfying(IamException.class, x -> assertThat(x.status()).isEqualTo(428));
        when(reauth.verifier("rejouee", nouveau, MonCompteService.FINALITE_SENSIBLE)).thenReturn(false);
        refus(() -> moi.confirmerTotp(titulaire, e.enrolementId(), juste, null, "rejouee"), "IAM_REAUTH_INVALID");
        when(reauth.verifier("valide", nouveau, MonCompteService.FINALITE_SENSIBLE)).thenReturn(true);
        moi.confirmerTotp(titulaire, e.enrolementId(), juste, null, "valide");
        verify(journal).compte(eq(titulaire), eq("LIBRE_SERVICE_SECOND_FACTEUR_REMPLACE"), eq(nouveau), eq("agent"), isNull(), anyList());

        // Secret chiffré avec une autre clé (redémarrage sans clé fixe) : l'enrôlement est à refaire.
        EnrolementTotp etranger = new EnrolementTotp(nouveau, new ChiffrementSecrets(null).chiffrer("JBSWY3DPEHPK3PXP"), MAINTENANT, MAINTENANT.plusSeconds(600));
        stock.put(etranger.getId(), etranger);
        refus(() -> moi.confirmerTotp(titulaire, etranger.getId(), juste, null, "valide"), "IAM_TOTP_ENROLMENT_EXPIRED");
        EnrolementTotp expire = new EnrolementTotp(nouveau, chiffrement.chiffrer("JBSWY3DPEHPK3PXP"), MAINTENANT.minusSeconds(700), MAINTENANT.minusSeconds(100));
        stock.put(expire.getId(), expire);
        refus(() -> moi.confirmerTotp(titulaire, expire.getId(), juste, null, "valide"), "IAM_TOTP_ENROLMENT_EXPIRED");
        verify(reauth, never()).verifier(eq("valide"), anyString(), eq("ADMIN_SENSITIVE"));
    }
}
