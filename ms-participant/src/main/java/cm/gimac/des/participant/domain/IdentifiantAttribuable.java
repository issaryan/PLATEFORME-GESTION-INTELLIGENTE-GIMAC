package cm.gimac.des.participant.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.IdGeneratorType;

/**
 * Identifiant UUID généré à la création, sauf s'il est déjà attribué (reprise de l'existant, D-107) : une institution
 * reprise garde l'UUID que portent déjà les comptes de ses correspondants (attribut Keycloak institution_id).
 */
@IdGeneratorType(IdentifiantAttribuableGenerateur.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface IdentifiantAttribuable {}
