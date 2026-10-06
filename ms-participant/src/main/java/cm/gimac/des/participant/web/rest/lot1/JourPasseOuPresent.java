package cm.gimac.des.participant.web.rest.lot1;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Jour passé ou présent au fuseau de la plateforme (Africa/Douala, UTC+1), et non à celui de la JVM :
 * {@code @PastOrPresent} compare à la date du serveur (UTC) et refuse la date du jour saisie à Douala
 * entre minuit et une heure.
 */
@Documented
@Constraint(validatedBy = JourPasseOuPresent.Validateur.class)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JourPasseOuPresent {
    /** Fuseau des agents du GIMAC (sans heure d'été). */
    ZoneId FUSEAU = ZoneId.of("Africa/Douala");

    String message() default "must be a date in the past or in the present (Africa/Douala)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Comparaison au jour courant à Douala ; l'horloge est substituable dans les tests. */
    class Validateur implements ConstraintValidator<JourPasseOuPresent, LocalDate> {

        private final Clock horloge;

        public Validateur() {
            this(Clock.systemUTC());
        }

        Validateur(Clock horloge) {
            this.horloge = horloge;
        }

        @Override
        public boolean isValid(LocalDate jour, ConstraintValidatorContext ctx) {
            return jour == null || !jour.isAfter(LocalDate.now(horloge.withZone(FUSEAU)));
        }
    }
}
