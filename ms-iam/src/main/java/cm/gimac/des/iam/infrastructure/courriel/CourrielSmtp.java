package cm.gimac.des.iam.infrastructure.courriel;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.application.port.out.ServiceCourriel;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.Courriel;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffre;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffreRepository;
import cm.gimac.des.iam.infrastructure.securite.ChiffrementSecrets;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Envoi des courriels de la plateforme par SMTP (D-65), avec les paramètres de messagerie de la
 * console (D-62) : hôte, port, expéditeur, TLS et utilisateur lus à chaque envoi, mot de passe dans la
 * copie chiffrée de MS-IAM. Échec : 503 IAM_SMTP_FAILED ; le corps (qui porte le lien) n'est jamais tracé.
 */
public class CourrielSmtp implements ServiceCourriel {

    private static final Logger LOG = LoggerFactory.getLogger(CourrielSmtp.class);
    private static final int DELAI_MS = 10_000;

    private final Annuaire annuaire;
    private final ParametreChiffreRepository parametres;
    private final ChiffrementSecrets chiffrement;

    public CourrielSmtp(Annuaire annuaire, ParametreChiffreRepository parametres, ChiffrementSecrets chiffrement) {
        this.annuaire = annuaire;
        this.parametres = parametres;
        this.chiffrement = chiffrement;
    }

    @Override
    public void envoyer(Courriel courriel) {
        ConfigurationMessagerie m = annuaire.messagerie();
        if (m.hote() == null || m.hote().isBlank() || m.expediteur() == null || m.expediteur().isBlank()) {
            throw IamException.indisponible("IAM_SMTP_FAILED", "Messagerie de la plateforme non configurée");
        }
        JavaMailSenderImpl envoi = expediteur(m);
        try {
            MimeMessage message = envoi.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            if (m.nomExpediteur() == null || m.nomExpediteur().isBlank()) {
                h.setFrom(m.expediteur());
            } else {
                h.setFrom(m.expediteur(), m.nomExpediteur());
            }
            h.setTo(courriel.destinataire());
            h.setSubject(courriel.objet());
            h.setText(courriel.texte(), courriel.html());
            envoi.send(message);
            LOG.info("Courriel « {} » envoyé à {}", courriel.objet(), courriel.destinataire());
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            LOG.warn("Courriel « {} » non envoyé : {}", courriel.objet(), e.getClass().getSimpleName());
            throw IamException.indisponible("IAM_SMTP_FAILED", "Le serveur de messagerie a refusé l'envoi");
        }
    }

    JavaMailSenderImpl expediteur(ConfigurationMessagerie m) {
        JavaMailSenderImpl envoi = new JavaMailSenderImpl();
        envoi.setHost(m.hote());
        envoi.setPort(m.port());
        envoi.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties p = envoi.getJavaMailProperties();
        p.put("mail.transport.protocol", m.ssl() ? "smtps" : "smtp");
        p.put("mail.smtp.starttls.enable", String.valueOf(m.starttls()));
        p.put("mail.smtp.starttls.required", String.valueOf(m.starttls()));
        p.put("mail.smtp.ssl.enable", String.valueOf(m.ssl()));
        p.put("mail.smtp.auth", String.valueOf(m.authentification()));
        p.put("mail.smtp.connectiontimeout", String.valueOf(DELAI_MS));
        p.put("mail.smtp.timeout", String.valueOf(DELAI_MS));
        p.put("mail.smtp.writetimeout", String.valueOf(DELAI_MS));
        if (m.authentification()) {
            envoi.setUsername(m.utilisateur());
            parametres.findById(ParametreChiffre.SMTP_MOT_DE_PASSE).ifPresent(s -> envoi.setPassword(chiffrement.dechiffrer(s.getValeurChiffree())));
        }
        return envoi;
    }
}
