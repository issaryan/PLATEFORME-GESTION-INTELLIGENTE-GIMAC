package cm.gimac.des.iam.domain.compte;

import java.time.Duration;

/**
 * Finalité d'un jeton de lien envoyé par courriel (D-65) : invitation (24 h), réinitialisation du mot
 * de passe oublié (30 min). Le jeton est à usage unique et conservé haché.
 */
public enum FinaliteJeton {
    INVITATION,
    REINITIALISATION;

    public Duration validite(Duration invitation, Duration reinitialisation) {
        return this == INVITATION ? invitation : reinitialisation;
    }
}
