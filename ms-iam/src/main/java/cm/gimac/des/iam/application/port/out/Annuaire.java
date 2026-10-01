package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fournisseur d'identité (Keycloak), autorité des comptes, des facteurs et des sessions. MS-IAM
 * l'administre entièrement pour la console (D-62) : personne n'a à ouvrir la console de Keycloak.
 * Adaptateur réel (API d'administration) et adaptateur de substitution en mémoire (PI §0.8).
 */
public interface Annuaire {
    List<Compte> lister();

    Optional<Compte> lire(String id);

    Optional<Compte> parIdentifiant(String identifiant);

    /** Compte portant ce courriel (casse ignorée), s'il est unique. */
    Optional<Compte> parEmail(String email);

    boolean emailPris(String email, String saufId);

    /**
     * Crée un compte actif, sans mot de passe, avec les actions de premier accès exigées par la
     * plateforme (attribut gimac_actions_requises, D-65) ; renvoie son identifiant.
     */
    String creer(NouveauCompte compte, List<ActionRequise> actions);

    void modifierFiche(String id, FicheCompte fiche);

    void marquerProfilComplet(String id, boolean complet);

    void definirProfils(String id, Set<String> profils);

    void definirActif(String id, boolean actif);

    void supprimer(String id);

    /**
     * Ajoute des actions exigées par la plateforme (attribut gimac_actions_requises), sans retirer les
     * autres ; jamais d'action requise de Keycloak, qui bloquerait l'octroi direct (D-65).
     */
    void exigerActions(String id, Set<ActionRequise> actions);

    /** Retire des actions exigées par la plateforme, une fois faites. */
    void retirerActions(String id, Set<ActionRequise> actions);

    /**
     * Mot de passe provisoire tiré au hasard, non temporaire pour le fournisseur d'identité (l'octroi
     * direct doit l'accepter) ; renvoyé une seule fois. Le changement est exigé par l'action MOT_DE_PASSE.
     */
    String definirMotDePasseProvisoire(String id);

    /**
     * Définit le mot de passe (définitif) ; IamException 400 IAM_PASSWORD_POLICY (détail : violations)
     * ou IAM_PASSWORD_REUSED si le fournisseur d'identité le refuse.
     */
    void definirMotDePasse(String id, String motDePasse);

    /**
     * Vérifie le mot de passe d'un compte par l'octroi direct du client gimac-connexion (mot de passe
     * seul) : la protection contre la force brute s'applique. La session ouverte est aussitôt fermée.
     */
    boolean verifierMotDePasse(String identifiant, String motDePasse);

    /** Supprime les codes TOTP configurés (identifiants OTP du fournisseur d'identité). */
    void reinitialiserSecondFacteur(String id);

    /**
     * Enregistre le code TOTP configuré sur la plateforme (extension gimac-totp) après vérification du
     * code par le fournisseur d'identité ; remplace un code existant. IamException 400
     * IAM_TOTP_CODE_INVALID si le code est refusé.
     */
    void enregistrerTotp(String id, String secretBase32, String code, String libelle);

    /** Le courriel du compte est prouvé (lien reçu et suivi). */
    void marquerEmailVerifie(String id);

    List<SessionCompte> sessions(String id);

    void fermerSessions(String id);

    void fermerSession(String id, String sessionId);

    PolitiqueSecurite politique();

    void appliquerPolitique(PolitiqueSecurite politique);

    ConfigurationMessagerie messagerie();

    void appliquerMessagerie(ConfigurationMessagerie messagerie);

    void testerMessagerie(String destinataire);

    /** Profils (rôles composites ROLE_*) avec leurs permissions et leur nombre de titulaires. */
    List<Profil> profils();

    /** Permissions connues du realm (rôles en majuscules non préfixés par ROLE_). */
    List<String> permissions();

    void definirPermissionsProfil(String profil, Set<String> permissions);
}
