package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Couche;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import cm.gimac.des.iam.domain.compte.FinaliteJeton;
import cm.gimac.des.iam.infrastructure.persistence.JetonLien;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Connexion sur la plateforme, routes internes appelées par la passerelle (D-65, connexion.md §3.1) :
 * état de connexion d'un compte après l'octroi direct, mot de passe oublié (courriel de la plateforme,
 * sans révéler l'existence d'un compte), nouveau mot de passe par le lien du courriel. Aucun mot de
 * passe, jeton ni code n'est journalisé.
 */
@Service
@Transactional
public class ConnexionService {

    private static final Logger LOG = LoggerFactory.getLogger(ConnexionService.class);

    private final Annuaire annuaire;
    private final LiensCourriel liens;
    private final JournalActes journal;

    public ConnexionService(Annuaire annuaire, LiensCourriel liens, JournalActes journal) {
        this.annuaire = annuaire;
        this.liens = liens;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public EtatConnexion etat(String id) {
        return EtatConnexion.de(annuaire.lire(id).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", id)));
    }

    /**
     * Mot de passe oublié : si l'identifiant (ou le courriel) désigne un compte actif, un lien de
     * réinitialisation (30 min) lui est envoyé ; sinon rien. La réponse est la même dans tous les cas.
     */
    public void motDePasseOublie(String identifiant, String couche) {
        Optional<Compte> compte = rechercher(identifiant).filter(Compte::actif).filter(c -> c.email() != null && !c.email().isBlank());
        if (compte.isEmpty()) {
            LOG.info("Mot de passe oublié : aucun compte actif joignable pour l'identifiant saisi ; aucun courriel");
            return;
        }
        Compte c = compte.orElseThrow();
        Couche cible = Couche.pour(c, Couche.lire(couche));
        try {
            liens.envoyer(c, FinaliteJeton.REINITIALISATION, cible);
        } catch (IamException e) {
            // Même réponse que pour un compte inconnu : l'échec est tracé, jamais révélé.
            LOG.warn("Mot de passe oublié : courriel de réinitialisation non envoyé pour le compte {} ({})", c.id(), e.code());
            journal.compte(titulaire(c), "MOT_DE_PASSE_OUBLIE_ECHEC", c.id(), c.identifiant(), null, List.of(e.code()));
            return;
        }
        journal.compte(titulaire(c), "MOT_DE_PASSE_OUBLIE", c.id(), c.identifiant(), null, List.of(cible.name()));
    }

    /**
     * Nouveau mot de passe par le lien d'un courriel (invitation ou réinitialisation) : jeton inconnu,
     * expiré ou déjà utilisé → 400 CNX_JETON_INVALIDE ; mot de passe refusé → 400 IAM_PASSWORD_POLICY
     * (le jeton reste utilisable). Le lien prouve la boîte aux lettres : le courriel devient vérifié.
     */
    public void nouveauMotDePasse(String jeton, String motDePasse) {
        JetonLien j = liens.utilisable(jeton).orElseThrow(ConnexionService::jetonInvalide);
        Compte c = annuaire.lire(j.getCompteId()).filter(Compte::actif).orElseThrow(ConnexionService::jetonInvalide);
        MotsDePasse.verifier(annuaire, c, motDePasse);
        annuaire.definirMotDePasse(c.id(), motDePasse);
        liens.utiliser(j);
        annuaire.retirerActions(c.id(), Set.of(ActionRequise.MOT_DE_PASSE, ActionRequise.VERIFIER_COURRIEL));
        if (!c.emailVerifie()) {
            annuaire.marquerEmailVerifie(c.id());
        }
        if (FinaliteJeton.REINITIALISATION.name().equals(j.getFinalite())) {
            annuaire.fermerSessions(c.id());
        }
        journal.compte(titulaire(c), "NOUVEAU_MOT_DE_PASSE", c.id(), c.identifiant(), null, List.of(j.getFinalite()));
    }

    private Optional<Compte> rechercher(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            return Optional.empty();
        }
        String saisi = identifiant.trim();
        Optional<Compte> c = annuaire.parIdentifiant(saisi.toLowerCase(Locale.ROOT));
        return c.isPresent() || !saisi.contains("@") ? c : annuaire.parEmail(saisi);
    }

    /** Le titulaire agit lui-même (lien reçu par courriel) : il est l'acteur de la trace. */
    static Acteur titulaire(Compte c) {
        return new Acteur(c.id(), c.nomAffiche(), Set.of());
    }

    private static IamException jetonInvalide() {
        return IamException.invalide("CNX_JETON_INVALIDE", "Lien inconnu, expiré ou déjà utilisé", Map.of());
    }
}
