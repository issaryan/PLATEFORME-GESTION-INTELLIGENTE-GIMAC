package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.FicheCompte;
import cm.gimac.des.iam.domain.compte.SessionCompte;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.infrastructure.persistence.CleSecuriteRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Espace « Mon compte » de chaque utilisateur (D-62) : première configuration après la création
 * du compte, coordonnées, changement de mot de passe et de second facteur, clés de sécurité,
 * sessions ouvertes. Tout se fait depuis la plateforme ; le fournisseur d'identité n'affiche que
 * ses écrans d'authentification, que la plateforme ouvre elle-même.
 */
@Service
@Transactional
public class MonCompteService {

    private static final Set<String> LANGUES = Set.of("fr", "en", "es");

    /** Compte, avancement du premier accès et état de la sécurité. */
    public record MonCompte(Compte compte, long clesSecurite, int sessions, List<String> etapesRestantes) {}

    private final Annuaire annuaire;
    private final JournalActes journal;
    private final CleSecuriteRepository cles;

    public MonCompteService(Annuaire annuaire, JournalActes journal, CleSecuriteRepository cles) {
        this.annuaire = annuaire;
        this.journal = journal;
        this.cles = cles;
    }

    @Transactional(readOnly = true)
    public MonCompte moi(Acteur acteur) {
        Compte c = compte(acteur);
        List<String> restantes = new java.util.ArrayList<>(c.actionsRequises().stream().map(Enum::name).toList());
        if (!c.profilComplet()) restantes.add("FICHE");
        if (!c.secondFacteur() && !restantes.contains(ActionRequise.CONFIGURE_TOTP.name())) restantes.add(ActionRequise.CONFIGURE_TOTP.name());
        return new MonCompte(c, cles.countBySujet(acteur.id()), annuaire.sessions(acteur.id()).size(), restantes);
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
     * Changement de mot de passe ou de second facteur : l'action est inscrite pour la prochaine
     * authentification, que l'interface déclenche aussitôt (reconnexion).
     */
    public MonCompte demander(Acteur acteur, ActionRequise action) {
        if (action != ActionRequise.UPDATE_PASSWORD && action != ActionRequise.CONFIGURE_TOTP) {
            throw IamException.invalide("IAM_ACTION_NOT_ALLOWED", "Action non disponible en libre-service", Map.of("action", String.valueOf(action)));
        }
        Compte c = compte(acteur);
        if (action == ActionRequise.CONFIGURE_TOTP) {
            annuaire.reinitialiserSecondFacteur(acteur.id());
        } else {
            annuaire.exigerActions(acteur.id(), Set.of(action));
        }
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

    private static void verifierLangue(String langue) {
        if (langue == null || !LANGUES.contains(langue)) {
            throw IamException.invalide("IAM_LANGUAGE_INVALID", "Langue : fr, en ou es", Map.of());
        }
    }
}
