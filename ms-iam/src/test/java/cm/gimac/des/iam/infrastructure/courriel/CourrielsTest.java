package cm.gimac.des.iam.infrastructure.courriel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.ConfigurationMessagerie;
import cm.gimac.des.iam.domain.compte.Courriel;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffre;
import cm.gimac.des.iam.infrastructure.persistence.ParametreChiffreRepository;
import cm.gimac.des.iam.infrastructure.securite.ChiffrementSecrets;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Adaptateurs de messagerie (D-65) : SMTP avec les paramètres de la console, substitut en mémoire. */
class CourrielsTest {

    static final Courriel COURRIEL = new Courriel("awa@gimac.test", "Objet", "texte avec jeton", "<p>html</p>");

    Annuaire annuaire = mock(Annuaire.class);
    ParametreChiffreRepository parametres = mock(ParametreChiffreRepository.class);
    ChiffrementSecrets chiffrement = new ChiffrementSecrets(null);
    JavaMailSenderImpl envoi = mock(JavaMailSenderImpl.class);

    CourrielSmtp smtp() {
        return new CourrielSmtp(annuaire, parametres, chiffrement) {
            @Override
            JavaMailSenderImpl expediteur(ConfigurationMessagerie m) {
                super.expediteur(m);
                return envoi;
            }
        };
    }

    @Test
    @DisplayName("SMTP : paramètres de la console, mot de passe déchiffré, message multipart (texte et HTML) avec nom d'expéditeur")
    void envoi() throws Exception {
        ConfigurationMessagerie m = new ConfigurationMessagerie("smtp.gimac.cm", 587, "des@gimac.cm", "Plateforme DES", true, false, true, "des", null, true);
        when(annuaire.messagerie()).thenReturn(m);
        when(parametres.findById(ParametreChiffre.SMTP_MOT_DE_PASSE)).thenReturn(Optional.of(new ParametreChiffre(ParametreChiffre.SMTP_MOT_DE_PASSE, chiffrement.chiffrer("s3cret"), Instant.EPOCH)));
        when(envoi.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        smtp().envoyer(COURRIEL);
        ArgumentCaptor<MimeMessage> envoye = ArgumentCaptor.forClass(MimeMessage.class);
        verify(envoi).send(envoye.capture());
        assertThat(envoye.getValue().getSubject()).isEqualTo("Objet");
        assertThat(envoye.getValue().getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("awa@gimac.test");
        assertThat(envoye.getValue().getFrom()[0].toString()).contains("Plateforme DES").contains("des@gimac.cm");

        JavaMailSenderImpl reel = new CourrielSmtp(annuaire, parametres, chiffrement).expediteur(m);
        assertThat(reel.getHost()).isEqualTo("smtp.gimac.cm");
        assertThat(reel.getPort()).isEqualTo(587);
        assertThat(reel.getUsername()).isEqualTo("des");
        assertThat(reel.getPassword()).isEqualTo("s3cret");
        assertThat(reel.getJavaMailProperties()).containsEntry("mail.smtp.starttls.enable", "true").containsEntry("mail.smtp.auth", "true").containsEntry("mail.transport.protocol", "smtp");
        JavaMailSenderImpl ssl = new CourrielSmtp(annuaire, parametres, chiffrement).expediteur(new ConfigurationMessagerie("h", 465, "a@b.cm", null, false, true, false, null, null, false));
        assertThat(ssl.getJavaMailProperties()).containsEntry("mail.transport.protocol", "smtps");
        assertThat(ssl.getUsername()).isNull();
    }

    @Test
    @DisplayName("SMTP : messagerie non configurée ou envoi refusé → 503 IAM_SMTP_FAILED")
    void echecs() {
        when(annuaire.messagerie()).thenReturn(new ConfigurationMessagerie(" ", 25, "a@b.cm", null, false, false, false, null, null, false));
        assertThatThrownBy(() -> smtp().envoyer(COURRIEL)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_SMTP_FAILED"));
        when(annuaire.messagerie()).thenReturn(new ConfigurationMessagerie("h", 25, null, null, false, false, false, null, null, false));
        assertThatThrownBy(() -> smtp().envoyer(COURRIEL)).isInstanceOf(IamException.class);
        when(annuaire.messagerie()).thenReturn(new ConfigurationMessagerie("h", 25, "a@b.cm", "", false, false, false, null, null, false));
        when(envoi.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("refus")).when(envoi).send(any(MimeMessage.class));
        assertThatThrownBy(() -> smtp().envoyer(COURRIEL)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.status()).isEqualTo(503));
    }

    @Test
    @DisplayName("Substitut en mémoire : 100 courriels au plus, dernier par destinataire")
    void enMemoire() {
        CourrielsEnMemoire m = new CourrielsEnMemoire();
        for (int i = 0; i < 105; i++) {
            m.envoyer(new Courriel(i % 2 == 0 ? "a@gimac.test" : "b@gimac.test", "Objet " + i, "t", "h"));
        }
        assertThat(m.envoyes()).hasSize(100);
        assertThat(m.dernierPour("A@gimac.test")).map(Courriel::objet).contains("Objet 104");
        assertThat(m.dernierPour("b@gimac.test")).map(Courriel::objet).contains("Objet 103");
        assertThat(m.dernierPour("c@gimac.test")).isEmpty();
    }
}
