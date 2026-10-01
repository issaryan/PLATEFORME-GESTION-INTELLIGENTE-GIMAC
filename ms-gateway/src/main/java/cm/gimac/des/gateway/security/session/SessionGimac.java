package cm.gimac.des.gateway.security.session;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.WebSession;

/**
 * Attributs d'une session ouverte par POST /api/connexion et règles qui s'y appliquent : échéance
 * (inactivité, durée absolue), appels de fond qui ne comptent pas comme activité, chemins admis
 * dans une session restreinte (premier accès, contrat D-65 §2).
 */
public final class SessionGimac {

    public static final String DEBUT = "gimac.session.debut";
    public static final String DERNIERE_ACTIVITE = "gimac.session.derniere-activite";
    public static final String RESTREINTE = "gimac.session.restreinte";
    /** Enregistrement OAuth2 du client dont la session détient les jetons (connexion ou oidc). */
    public static final String ENREGISTREMENT = "gimac.session.enregistrement";
    /** Nom du principal sous lequel le client autorisé est enregistré. */
    public static final String PRINCIPAL = "gimac.session.principal";

    /** En-tête des appels de fond (sondages, canal poussé) : « 1 », pas une activité de l'utilisateur. */
    public static final String EN_TETE_FOND = "X-Gimac-Fond";

    private static final Pattern CANAL_POUSSE = Pattern.compile("^/bff/[^/]+/live$");
    private static final Pattern PREMIER_ACCES = Pattern.compile(
        "^/bff/(des|portail|console)/iam/moi(/premiere-configuration|/mot-de-passe|/second-facteur(/.*)?)?$"
    );

    private SessionGimac() {}

    /** Session ouverte par la connexion de la plateforme (et donc soumise aux échéances). */
    public static boolean geree(WebSession session) {
        return session.getAttribute(DEBUT) instanceof Instant;
    }

    public static boolean restreinte(WebSession session) {
        return Boolean.TRUE.equals(session.getAttribute(RESTREINTE));
    }

    /** Fin d'inactivité prévue, bornée par la durée absolue ; vide hors session gérée. */
    public static Optional<Instant> expireA(WebSession session, ParametresSession parametres) {
        if (!geree(session)) {
            return Optional.empty();
        }
        Instant debut = session.getAttribute(DEBUT);
        Object derniere = session.getAttribute(DERNIERE_ACTIVITE);
        Instant activite = derniere instanceof Instant i ? i : debut;
        Instant inactivite = activite.plus(parametres.inactiviteMax());
        Instant absolue = debut.plus(parametres.dureeMax());
        return Optional.of(inactivite.isBefore(absolue) ? inactivite : absolue);
    }

    public static boolean expiree(WebSession session, Instant maintenant, ParametresSession parametres) {
        return expireA(session, parametres).map(fin -> !maintenant.isBefore(fin)).orElse(false);
    }

    /** Activité de l'utilisateur (explicite) : repousse la fin d'inactivité d'une session gérée. */
    public static void prolonger(WebSession session, Instant maintenant) {
        if (geree(session)) {
            session.getAttributes().put(DERNIERE_ACTIVITE, maintenant);
        }
    }

    /** Appel de fond : en-tête X-Gimac-Fond: 1 ou canal poussé /bff/{couche}/live. */
    public static boolean appelDeFond(String chemin, String enTeteFond) {
        return "1".equals(enTeteFond) || CANAL_POUSSE.matcher(chemin).matches();
    }

    /**
     * Chemins admis dans une session restreinte : l'assistant de premier accès sur MS-IAM
     * (GET|POST /bff/{couche}/iam/moi, …/premiere-configuration, …/mot-de-passe,
     * …/second-facteur/**), la session, sa prolongation, la déconnexion et la connexion.
     */
    public static boolean admisEnSessionRestreinte(HttpMethod methode, String chemin) {
        if ("/api/connexion".equals(chemin) || chemin.startsWith("/api/connexion/")) {
            return true;
        }
        if (HttpMethod.GET.equals(methode) && "/api/session".equals(chemin)) {
            return true;
        }
        if (HttpMethod.POST.equals(methode) && ("/api/session/prolongation".equals(chemin) || "/api/logout".equals(chemin))) {
            return true;
        }
        return (HttpMethod.GET.equals(methode) || HttpMethod.POST.equals(methode)) && PREMIER_ACCES.matcher(chemin).matches();
    }
}
