package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.ServiceCourriel;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.Courriel;
import java.time.Duration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Courriels envoyés par MS-IAM (D-65) : invitation, réinitialisation du mot de passe oublié, rappel
 * des actions exigées. Gabarits en français, anglais et espagnol ({@code i18n/courriels*.properties}),
 * choisis selon la langue du compte ; le lien mène à l'application de la couche du compte.
 */
@Component
public class CourrielsPlateforme {

    public enum Modele {
        INVITATION,
        REINITIALISATION,
        ACTIONS,
    }

    private static final Set<String> LANGUES = Set.of("fr", "en", "es");
    private static final String BLEU = "#0b3d91";

    private final ServiceCourriel service;

    public CourrielsPlateforme(ServiceCourriel service) {
        this.service = service;
    }

    /** Compose et envoie le courriel ; IamException 503 IAM_SMTP_FAILED si le serveur le refuse. */
    public void envoyer(Compte compte, Modele modele, String lien, Duration validite) {
        service.envoyer(composer(compte, modele, lien, validite));
    }

    static Courriel composer(Compte compte, Modele modele, String lien, Duration validite) {
        ResourceBundle m = gabarits(compte.langue());
        String prefixe = modele.name().toLowerCase(Locale.ROOT) + ".";
        String duree = validite == null ? "" : duree(m, validite);
        String salutation = remplacer(m.getString("salutation"), compte.nomAffiche(), duree);
        String corps = remplacer(m.getString(prefixe + "corps"), compte.identifiant(), duree);
        String action = m.getString(prefixe + "action");
        String objet = m.getString(prefixe + "objet");
        String texte = String.join(
            "\n\n",
            salutation,
            corps,
            action + " : " + lien,
            m.getString("ignorer"),
            "-- \n" + m.getString("pied")
        );
        String html =
            "<!doctype html><html lang=\"" + langue(compte.langue()) + "\"><body style=\"margin:0;padding:24px;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#1a1f2b\">" +
            "<table role=\"presentation\" width=\"100%\" style=\"max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #d5dbe5\">" +
            "<tr><td style=\"background:" + BLEU + ";color:#ffffff;padding:16px 24px;font-size:18px;font-weight:bold\">" + e(m.getString("entete")) + "</td></tr>" +
            "<tr><td style=\"padding:24px\"><h1 style=\"font-size:20px;margin:0 0 16px\">" + e(m.getString(prefixe + "titre")) + "</h1>" +
            "<p>" + e(salutation) + "</p><p>" + e(corps) + "</p>" +
            "<p style=\"margin:24px 0\"><a href=\"" + e(lien) + "\" style=\"background:" + BLEU + ";color:#ffffff;padding:12px 20px;text-decoration:none;display:inline-block\">" + e(action) + "</a></p>" +
            "<p style=\"font-size:13px;color:#4a5568\">" + e(m.getString("lien.copie")) + "<br><span style=\"word-break:break-all\">" + e(lien) + "</span></p>" +
            "<p style=\"font-size:13px;color:#4a5568\">" + e(m.getString("ignorer")) + "</p></td></tr>" +
            "<tr><td style=\"border-top:1px solid #d5dbe5;padding:12px 24px;font-size:12px;color:#4a5568\">" + e(m.getString("pied")) + "</td></tr>" +
            "</table></body></html>";
        return new Courriel(compte.email(), objet, texte, html);
    }

    static ResourceBundle gabarits(String langue) {
        return ResourceBundle.getBundle("i18n/courriels", Locale.forLanguageTag(langue(langue)), ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    static String langue(String langue) {
        return langue != null && LANGUES.contains(langue) ? langue : "fr";
    }

    static String duree(ResourceBundle m, Duration d) {
        return d.toMinutes() >= 60 && d.toMinutes() % 60 == 0 ? remplacer(m.getString("duree.heures"), String.valueOf(d.toHours()), "") : remplacer(m.getString("duree.minutes"), String.valueOf(d.toMinutes()), "");
    }

    private static String remplacer(String modele, String a, String b) {
        return modele.replace("{0}", a == null ? "" : a).replace("{1}", b == null ? "" : b);
    }

    private static String e(String s) {
        return HtmlUtils.htmlEscape(s);
    }
}
