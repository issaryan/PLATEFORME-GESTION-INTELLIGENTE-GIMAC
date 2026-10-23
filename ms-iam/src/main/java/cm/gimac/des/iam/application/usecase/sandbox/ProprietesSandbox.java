package cm.gimac.des.iam.application.usecase.sandbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres propres à la Sandbox GIMAC dans MS-IAM (bloc gimac.sandbox, D-119) : désactivation des comptes inactifs
 * de la sandbox et durées maximales des délégations.
 *
 * @param inactiviteJours jours sans connexion au-delà desquels un compte de la sandbox est désactivé (90 par défaut)
 * @param mandatDureeMax durée maximale d'un mandat de partenaire (90 jours par défaut, US-EXI-04)
 * @param prestataireDureeMax durée maximale d'un accès de prestataire (8 heures par défaut, US-EXI-24)
 */
@ConfigurationProperties(prefix = "gimac.sandbox")
public record ProprietesSandbox(Integer inactiviteJours, Duration mandatDureeMax, Duration prestataireDureeMax) {
    public ProprietesSandbox {
        inactiviteJours = inactiviteJours == null || inactiviteJours < 1 ? 90 : inactiviteJours;
        mandatDureeMax = mandatDureeMax == null ? Duration.ofDays(90) : mandatDureeMax;
        prestataireDureeMax = prestataireDureeMax == null ? Duration.ofHours(8) : prestataireDureeMax;
    }
}
