package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Couche;
import cm.gimac.des.iam.domain.compte.FinaliteJeton;
import cm.gimac.des.iam.domain.service.JetonsLien;
import cm.gimac.des.iam.infrastructure.persistence.JetonLien;
import cm.gimac.des.iam.infrastructure.persistence.JetonLienRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Liens des courriels de la plateforme (D-65) : jeton aléatoire de 32 octets (base64url), conservé
 * haché (SHA-256) avec sa finalité, son compte, son échéance (invitation 24 h, réinitialisation 30 min)
 * et sa date d'usage ; un seul lien ouvert par compte, usage unique. Le lien mène à
 * {@code {adresse de la couche}/connexion/nouveau-mot-de-passe?jeton=…}.
 *
 * <p>S'exécute dans la transaction de l'appelant (sans intercepteur propre : un échec d'envoi rattrapé
 * par l'appelant ne condamne pas sa transaction).
 */
@Component
public class LiensCourriel {

    static final String CHEMIN_NOUVEAU_MOT_DE_PASSE = "/connexion/nouveau-mot-de-passe?jeton=";
    static final String CHEMIN_CONNEXION = "/connexion";

    private final JetonLienRepository jetons;
    private final CourrielsPlateforme courriels;
    private final IamProprietes proprietes;
    private final Clock clock;
    private final SecureRandom aleatoire = new SecureRandom();

    public LiensCourriel(JetonLienRepository jetons, CourrielsPlateforme courriels, IamProprietes proprietes, Clock clock) {
        this.jetons = jetons;
        this.courriels = courriels;
        this.proprietes = proprietes;
        this.clock = clock;
    }

    /**
     * Émet un lien à jeton pour le compte (les liens encore ouverts sont retirés) et l'envoie par
     * courriel ; IamException 503 IAM_SMTP_FAILED si l'envoi échoue.
     */
    public void envoyer(Compte compte, FinaliteJeton finalite, Couche couche) {
        Instant maintenant = clock.instant();
        Duration validite = finalite.validite(proprietes.liens().validiteInvitation(), proprietes.liens().validiteReinitialisation());
        jetons.ouverts(compte.id()).forEach(j -> j.utiliser(maintenant));
        String jeton = JetonsLien.nouveau(aleatoire);
        jetons.save(new JetonLien(JetonsLien.empreinte(jeton), finalite.name(), compte.id(), couche.name(), maintenant, maintenant.plus(validite)));
        String lien = proprietes.liens().adresse(couche) + CHEMIN_NOUVEAU_MOT_DE_PASSE + jeton;
        courriels.envoyer(compte, finalite == FinaliteJeton.INVITATION ? CourrielsPlateforme.Modele.INVITATION : CourrielsPlateforme.Modele.REINITIALISATION, lien, validite);
    }

    /** Rappel des actions exigées, avec un lien vers l'écran de connexion de la couche (sans jeton). */
    public void rappeler(Compte compte, Couche couche) {
        courriels.envoyer(compte, CourrielsPlateforme.Modele.ACTIONS, proprietes.liens().adresse(couche) + CHEMIN_CONNEXION, null);
    }

    /** Jeton utilisable (connu, ni expiré ni déjà utilisé), verrouillé jusqu'à la fin de la transaction. */
    public Optional<JetonLien> utilisable(String jeton) {
        if (!JetonsLien.bienForme(jeton)) {
            return Optional.empty();
        }
        Instant maintenant = clock.instant();
        return jetons.findByEmpreinteForUpdate(JetonsLien.empreinte(jeton)).filter(j -> j.utilisable(maintenant));
    }

    /** Le jeton a servi : lui et tout autre lien ouvert du compte sont retirés. */
    public void utiliser(JetonLien jeton) {
        Instant maintenant = clock.instant();
        jeton.utiliser(maintenant);
        jetons.ouverts(jeton.getCompteId()).forEach(j -> j.utiliser(maintenant));
    }
}
