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

/** Port sortant : fournisseur d'identité piloté par MS-IAM (Keycloak, D-62). */
public interface Annuaire {
    List<Compte> lister();

    Optional<Compte> lire(String id);

    Optional<Compte> parIdentifiant(String identifiant);

    boolean emailPris(String email, String saufId);

    String creer(NouveauCompte c, List<ActionRequise> actions);

    void modifierFiche(String id, FicheCompte f);

    void marquerProfilComplet(String id, boolean complet);

    void definirProfils(String id, Set<String> profils);

    void definirActif(String id, boolean actif);

    void supprimer(String id);

    void exigerActions(String id, Set<ActionRequise> actions);

    void envoyerInvitation(String id, List<ActionRequise> actions, Duration validite);

    String definirMotDePasseProvisoire(String id);

    void reinitialiserSecondFacteur(String id);

    List<SessionCompte> sessions(String id);

    void fermerSessions(String id);

    void fermerSession(String id, String sessionId);

    PolitiqueSecurite politique();

    void appliquerPolitique(PolitiqueSecurite p);

    ConfigurationMessagerie messagerie();

    void appliquerMessagerie(ConfigurationMessagerie c);

    void testerMessagerie(String destinataire);

    List<Profil> profils();

    List<String> permissions();

    void definirPermissionsProfil(String profil, Set<String> permissions);
}
