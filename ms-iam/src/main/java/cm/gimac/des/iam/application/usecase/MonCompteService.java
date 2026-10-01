package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.Totp;
import cm.gimac.des.iam.infrastructure.persistence.CleSecuriteRepository;
import cm.gimac.des.iam.infrastructure.persistence.EnrolementTotp;
import cm.gimac.des.iam.infrastructure.persistence.EnrolementTotpRepository;
import cm.gimac.des.iam.infrastructure.securite.ChiffrementSecrets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Espace « Mon compte » de chaque utilisateur (D-62, D-65) : premier accès (mot de passe définitif,
 * code TOTP, fiche, charte), coordonnées, changement de mot de passe et de code TOTP, clés de sécurité,
 * sessions ouvertes. Tout se fait sur la plateforme ; aucune page du fournisseur d'identité n'est
 * montrée. Ni mot de passe, ni secret, ni code n'est journalisé.
 */
@Service
@Transactional
public class MonCompteService {

    /** Finalité de la réauthentification forte exigée pour remplacer un code TOTP (IAM-07). */
    public static final String FINALITE_SENSIBLE = "ACCOUNT_SENSITIVE";
    private static final Set<String> LANGUES = Set.of("fr", "en", "es");
    /** Tolérance d'un pas de 30 s de part et d'autre (décalage d'horloge du téléphone). */
    private static final int TOLERANCE_TOTP = 1;

    /** Compte, avancement du premier accès et état de la sécurité. */
    public record MonCompte(Compte compte, long clesSecurite, int sessions, List<String> etapesRestantes) {}

    /** Enrôlement TOTP proposé : secret (base 32) et URI du code QR, à ne jamais journaliser. */
    public record Enrolement(UUID enrolementId, String secret, String uri) {
        @Override
        public String toString() {
            return "Enrolement[enrolementId=" + enrolementId + ", secret=****]";
        }
    }

    private final Annuaire annuaire;
    private final JournalActes journal;
    private final CleSecuriteRepository cles;
    private final EnrolementTotpRepository enrolements;
    private final ChiffrementSecrets chiffrement;
    private final ReauthentificationService reauth;
    private final IamProprietes proprietes;
    private final Clock clock;
    private final SecureRandom aleatoire = new SecureRandom();

    public MonCompteService(
        Annuaire annuaire,
        JournalActes journal,
        CleSecuriteRepository cles,
        EnrolementTotpRepository enrolements,
        ChiffrementSecrets chiffrement,
        ReauthentificationService reauth,
        IamProprietes proprietes,
        Clock clock
    ) {
        this.annuaire = annuaire;
        this.journal = journal;
        this.cles = cles;
        this.enrolements = enrolements;
        this.chiffrement = chiffrement;
        this.reauth = reauth;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MonCompte moi(Acteur acteur) {
        Compte c = compte(acteur);
        return new MonCompte(c, cles.countBySujet(acteur.id()), annuaire.sessions(acteur.id()).size(), c.etapesRestantes().stream().map(Enum::name).toList());
    }

    /** Première configuration : coordonnées vérifiées, charte d'usage acceptée ; la fiche devient complète. */
    public MonCompte premiereConfiguration(Acteur acteur, String telephone, String langue, String fonction, boolean charteAcceptee) {
        if (!charteAcceptee) {
            throw IamException.invalide("IAM_CHARTER_REQUIRED", "La charte d'usage doit être acceptée", Map.of());
        }
        if (telephone == null || telephone.isBlank()) {
            throw IamException.invalide("IAM_PHONE_REQUIRED", "Le téléphone professionnel est obligatoire", Map.of());
        }
        ReglesAdministration.verifierTelephone(telephone);
        verifierLangue(langue);
        Compte c = compte(acteur);
        annuaire.modifierFiche(acteur.id(), new FicheCompte(null, null, null, telephone, fonction, langue, null));
        annuaire.marquerProfilComplet(acteur.id(), true);
        annuaire.retirerActions(acteur.id(), Set.of(ActionRequise.FICHE, ActionRequise.CHARTE));
        journal.compte(acteur, "PREMIERE_CONFIGURATION", acteur.id(), c.identifiant(), null, List.of());
        return moi(acteur);
    }

    /** Coordonnées modifiables par le titulaire ; le courriel, l'identifiant et les profils restent à l'administration. */
    public MonCompte modifier(Acteur acteur, String prenom, String nom, String telephone, String fonction, String langue) {
        ReglesAdministration.verifierTelephone(telephone);
        if (langue != null) verifierLangue(langue);
        Compte c = compte(acteur);
        annuaire.modifierFiche(acteur.id(), new FicheCompte(prenom, nom, null, telephone, fonction, langue, null));
        journal.compte(acteur, "FICHE_PERSONNELLE", acteur.id(), c.identifiant(), null, List.of());
        return moi(acteur);
    }

    /**
     * Changement de mot de passe (premier accès ou « Mon compte ») : le mot de passe actuel est vérifié
     * par le fournisseur d'identité (octroi direct sur gimac-connexion, force brute comprise), le
     * nouveau contrôlé par la politique puis enregistré ; l'action MOT_DE_PASSE est retirée.
     */
    public void changerMotDePasse(Acteur acteur, String actuel, String nouveau) {
        Compte c = compte(acteur);
        if (actuel == null || actuel.isEmpty() || !annuaire.verifierMotDePasse(c.identifiant(), actuel)) {
            throw IamException.invalide("IAM_PASSWORD_INVALID", "Mot de passe actuel incorrect", Map.of());
        }
        if (actuel.equals(nouveau)) {
            throw IamException.invalide("IAM_PASSWORD_REUSED", "Le nouveau mot de passe doit différer des précédents", Map.of("violations", List.of("HISTORIQUE")));
        }
        MotsDePasse.verifier(annuaire, c, nouveau);
        annuaire.definirMotDePasse(acteur.id(), nouveau);
        annuaire.retirerActions(acteur.id(), Set.of(ActionRequise.MOT_DE_PASSE));
        journal.compte(acteur, "LIBRE_SERVICE_MOT_DE_PASSE", acteur.id(), c.identifiant(), null, List.of());
    }

    /**
     * Début de la configuration du code TOTP : secret aléatoire de 20 octets (base 32), gardé chiffré
     * 10 min, et URI du code QR selon la politique OTP en vigueur. Un enrôlement précédent est remplacé.
     */
    public Enrolement enrolementTotp(Acteur acteur) {
        Compte c = compte(acteur);
        PolitiqueSecurite p = annuaire.politique();
        String secret = Totp.nouveauSecret(aleatoire);
        Instant maintenant = clock.instant();
        enrolements.supprimerPour(acteur.id());
        EnrolementTotp e = enrolements.save(new EnrolementTotp(acteur.id(), chiffrement.chiffrer(secret), maintenant, maintenant.plus(proprietes.liens().validiteEnrolementTotp())));
        return new Enrolement(e.getId(), secret, Totp.uri(c.identifiant(), secret, p.otpChiffres(), p.otpPeriodeSecondes(), p.otpAlgorithme()));
    }

    /**
     * Fin de la configuration : le code saisi est vérifié (RFC 6238, tolérance d'un pas), puis le secret
     * est enregistré dans le fournisseur d'identité (extension gimac-totp), qui vérifie à nouveau le
     * code ; un code existant est remplacé et l'action SECOND_FACTEUR retirée. Remplacer un code déjà
     * configuré exige la réauthentification forte FIDO2 si une clé est enrôlée (ACCOUNT_SENSITIVE).
     */
    public void confirmerTotp(Acteur acteur, UUID enrolementId, String code, String libelle, String assertion) {
        Compte c = compte(acteur);
        Instant maintenant = clock.instant();
        EnrolementTotp e = enrolementId == null
            ? null
            : enrolements.findById(enrolementId).filter(x -> x.utilisable(acteur.id(), maintenant)).orElse(null);
        if (e == null) {
            throw enrolementExpire();
        }
        if (c.secondFacteur() && cles.countBySujet(acteur.id()) > 0) {
            if (assertion == null || assertion.isBlank()) {
                throw IamException.reauthRequise();
            }
            if (!reauth.verifier(assertion, acteur.id(), FINALITE_SENSIBLE)) {
                throw IamException.interdit("IAM_REAUTH_INVALID", "Réauthentification forte invalide");
            }
        }
        PolitiqueSecurite p = annuaire.politique();
        String secret;
        try {
            secret = chiffrement.dechiffrer(e.getSecretChiffre());
        } catch (IllegalStateException illisible) {
            // Clé de chiffrement changée depuis le début de la configuration (redémarrage sans clé fixe).
            throw enrolementExpire();
        }
        if (!Totp.verifier(secret, code == null ? null : code.trim(), maintenant, p.otpChiffres(), p.otpPeriodeSecondes(), p.otpAlgorithme(), TOLERANCE_TOTP)) {
            throw codeInvalide();
        }
        annuaire.enregistrerTotp(acteur.id(), secret, code.trim(), libelle);
        enrolements.delete(e);
        annuaire.retirerActions(acteur.id(), Set.of(ActionRequise.SECOND_FACTEUR));
        journal.compte(acteur, c.secondFacteur() ? "LIBRE_SERVICE_SECOND_FACTEUR_REMPLACE" : "LIBRE_SERVICE_SECOND_FACTEUR", acteur.id(), c.identifiant(), null, List.of());
    }

    /**
     * Demande en libre-service (route antérieure à D-65) : l'action est inscrite pour la prochaine
     * connexion, où la plateforme la fera faire. Seuls le mot de passe et le second facteur s'y prêtent.
     */
    public MonCompte demander(Acteur acteur, ActionRequise action) {
        if (action != ActionRequise.MOT_DE_PASSE && action != ActionRequise.SECOND_FACTEUR) {
            throw IamException.invalide("IAM_ACTION_NOT_ALLOWED", "Action non disponible en libre-service", Map.of("action", String.valueOf(action)));
        }
        Compte c = compte(acteur);
        annuaire.exigerActions(acteur.id(), Set.of(action));
        journal.compte(acteur, "LIBRE_SERVICE_" + action.name(), acteur.id(), c.identifiant(), null, List.of());
        return moi(acteur);
    }

    @Transactional(readOnly = true)
    public List<SessionCompte> sessions(Acteur acteur) {
        return annuaire.sessions(acteur.id());
    }

    public void fermerSession(Acteur acteur, String sessionId) {
        Compte c = compte(acteur);
        annuaire.fermerSession(acteur.id(), sessionId);
        journal.compte(acteur, "LIBRE_SERVICE_FERMETURE_SESSION", acteur.id(), c.identifiant(), null, List.of(sessionId));
    }

    public void fermerToutesSessions(Acteur acteur) {
        Compte c = compte(acteur);
        annuaire.fermerSessions(acteur.id());
        journal.compte(acteur, "LIBRE_SERVICE_FERMETURE_SESSIONS", acteur.id(), c.identifiant(), null, List.of());
    }

    private Compte compte(Acteur acteur) {
        return annuaire.lire(acteur.id()).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", acteur.id()));
    }

    static IamException enrolementExpire() {
        return IamException.invalide("IAM_TOTP_ENROLMENT_EXPIRED", "Configuration expirée : recommencez", Map.of());
    }

    static IamException codeInvalide() {
        return IamException.invalide("IAM_TOTP_CODE_INVALID", "Code de vérification incorrect", Map.of());
    }

    private static void verifierLangue(String langue) {
        if (langue == null || !LANGUES.contains(langue)) {
            throw IamException.invalide("IAM_LANGUAGE_INVALID", "Langue : fr, en ou es", Map.of());
        }
    }
}
