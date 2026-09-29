package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import java.time.Duration;
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

    boolean emailPris(String email, String saufId);

    /** Crée un compte actif, sans mot de passe, avec les actions de premier accès ; renvoie son identifiant. */
    String creer(NouveauCompte compte, List<ActionRequise> actions);

    void modifierFiche(String id, FicheCompte fiche);

    void marquerProfilComplet(String id, boolean complet);

    void definirProfils(String id, Set<String> profils);

    void definirActif(String id, boolean actif);

    void supprimer(String id);

    /** Ajoute des actions requises à la prochaine connexion, sans retirer les autres. */
    void exigerActions(String id, Set<ActionRequise> actions);

    /** Courriel d'invitation portant un lien d'exécution des actions, valable la durée donnée. */
    void envoyerInvitation(String id, List<ActionRequise> actions, Duration validite);

    /** Mot de passe provisoire à usage unique (changement imposé) ; renvoyé une seule fois. */
    String definirMotDePasseProvisoire(String id);

    /** Supprime les seconds facteurs configurés et impose d'en configurer un nouveau. */
    void reinitialiserSecondFacteur(String id);

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
