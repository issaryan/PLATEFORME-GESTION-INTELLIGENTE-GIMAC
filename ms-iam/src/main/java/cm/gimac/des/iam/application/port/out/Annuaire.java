package cm.gimac.des.iam.application.port.out;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import cm.gimac.des.iam.domain.compte.PresentationProfil;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import java.util.List;
import java.util.Map;
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

    /**
     * Rattachement organisationnel (D-76) : attributs unite et responsable_id du compte ; une valeur
     * nulle retire l'attribut.
     */
    void definirRattachement(String id, String unite, String responsableId);

    /**
     * Comptes dont le responsable (n+1) est ce compte (attribut responsable_id), 500 au plus. Comptes
     * allégés : profils et second facteur n'y sont pas renseignés.
     */
    List<Compte> parResponsable(String responsableId);

    /**
     * Comptes rattachés à cette unité (attribut unite), 500 au plus (D-81, membres des unités dirigées).
     * Comptes allégés, comme {@link #parResponsable(String)}.
     */
    List<Compte> parUnite(String unite);

    /**
     * Comptes rattachés à cette institution (attribut institution_id, correspondance exacte), 500 au plus
     * (D-93 §11, destinataires des notifications d'une institution). Comptes allégés, comme
     * {@link #parResponsable(String)}.
     */
    default List<Compte> parInstitution(String institutionId) {
        return lister().stream().filter(c -> institutionId.equals(c.institutionId())).toList();
    }

    /**
     * Comptes actifs, hors comptes de service (D-92, périmètre de la portée TOUTES). Comptes allégés, comme
     * {@link #parResponsable(String)} : ni profils ni second facteur.
     */
    default List<Compte> actifs() {
        return lister().stream().filter(Compte::actif).toList();
    }

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

    /** État de verrouillage par la protection contre la force brute (US-EXI-23) ; compte inconnu : 404. */
    cm.gimac.des.iam.domain.compte.Verrouillage verrouillage(String id);

    /** Lève le verrouillage temporaire et remet à zéro le compteur d'échecs (US-EXI-23) ; compte inconnu : 404. */
    void debloquer(String id);

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

    /**
     * Permissions de chaque profil (rôle composite ROLE_*), par code (D-94 : qualités d'un compte déduites de
     * la composition de ses profils). Lecture sans titulaires.
     */
    default Map<String, Set<String>> compositions() {
        Map<String, Set<String>> m = new java.util.TreeMap<>();
        profils().forEach(p -> m.put(p.code(), p.permissions()));
        return m;
    }

    /**
     * Codes des profils externes (D-94) : attribut gimac_externe du rôle, ou à défaut une permission du portail
     * (*_PORTAL_*).
     */
    default Set<String> profilsExternes() {
        Set<String> s = new java.util.TreeSet<>();
        profils().stream().filter(Profil::externe).forEach(p -> s.add(p.code()));
        return s;
    }

    /** Attribut gimac_externe des profils qui le portent (vrai ou faux), par code ; absent : non posé. */
    Map<String, Boolean> attributsExternes();

    /**
     * Pose l'attribut gimac_externe d'un profil (D-94) ; les autres attributs sont conservés. IamException 404
     * IAM_PROFILE_NOT_FOUND si le rôle n'existe pas.
     */
    void definirExterne(String code, boolean externe);

    /** Permissions connues du realm (rôles en majuscules non préfixés par ROLE_). */
    List<String> permissions();

    void definirPermissionsProfil(String profil, Set<String> permissions);

    /**
     * Crée un profil : rôle composite du realm (code ROLE_*), avec sa description et ses permissions
     * (D-81, docs/lot1/pouvoirs-hierarchiques.md §5). IamException 409 IAM_PROFIL_EXISTANT si un rôle
     * porte déjà ce nom.
     */
    default void creerProfil(String code, String description, Set<String> permissions) {
        creerProfil(code, null, description, permissions);
    }

    /**
     * Crée un profil avec son libellé (D-86, docs/lot1/profils-organisation.md §2 : attribut gimac_libelle
     * du rôle ; nul, non renseigné).
     */
    void creerProfil(String code, String libelle, String description, Set<String> permissions);

    /** Supprime le rôle composite d'un profil créé. */
    void supprimerProfil(String code);

    /**
     * Libellé (attribut gimac_libelle) et description du rôle composite d'un profil (D-86 §2) ; un libellé
     * nul retire l'attribut. IamException 404 IAM_PROFILE_NOT_FOUND si le rôle n'existe pas.
     */
    void definirPresentationProfil(String code, String libelle, String description);

    /**
     * Libellé (nul s'il n'est pas renseigné) et description de chaque profil (rôle composite ROLE_*), par
     * code, en une lecture légère (sans composition ni titulaires).
     */
    Map<String, PresentationProfil> presentationsProfils();

    /**
     * Profils (rôles composites ROLE_*, hors ROLE_USER) de chaque compte qui en porte, par identifiant de
     * compte (D-86, membres d'une unité pour le moteur de processus).
     */
    Map<String, Set<String>> profilsParCompte();
}
