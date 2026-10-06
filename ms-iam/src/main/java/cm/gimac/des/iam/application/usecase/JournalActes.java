package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.event.AvatarModifiePayload;
import cm.gimac.des.iam.domain.event.CompteAdministrePayload;
import cm.gimac.des.iam.domain.event.ConfigurationModifieePayload;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministration;
import cm.gimac.des.iam.infrastructure.persistence.JournalAdministrationRepository;
import cm.gimac.des.iam.infrastructure.security.CorrelationContext;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trace de chaque acte d'administration (D-62) : entrée du journal consultable depuis la console et
 * événement vers le registre d'audit, dans la même transaction.
 */
@Component
public class JournalActes {

    private final JournalAdministrationRepository journal;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public JournalActes(JournalAdministrationRepository journal, OutboxPublisher outbox, Clock clock) {
        this.journal = journal;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void compte(Acteur acteur, String action, String compteId, String libelle, String motif, List<String> details) {
        journal.save(new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), action, compteId, libelle, motif, String.join(", ", details), CorrelationContext.current()));
        outbox.publish("account", compteId, CompteAdministrePayload.TYPE, new CompteAdministrePayload(action, compteId, libelle, acteur.id(), motif, details));
    }

    /**
     * Photo de profil remplacée ou retirée (docs/lot1/mon-compte.md §1) : entrée du journal
     * d'administration et événement iam.avatar-changed.v1, sans l'image.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void avatar(Acteur acteur, String compteId, String identifiant, String action, String version) {
        journal.save(
            new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), "AVATAR_" + action, compteId, identifiant, null, version == null ? "" : "version " + version, CorrelationContext.current())
        );
        outbox.publish("avatar", compteId, AvatarModifiePayload.TYPE, new AvatarModifiePayload(compteId, identifiant, action, version, acteur.id()));
    }

    /**
     * Matrice des pouvoirs modifiée pour un profil (D-81 §3) : entrée MATRICE_POUVOIRS_MODIFIEE (avant,
     * après) et événement de configuration vers l'audit.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void matrice(Acteur acteur, String profil, String motif, String avant, String apres) {
        String details = tronquer("avant: " + avant + ", après: " + apres, 2000);
        journal.save(new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), "MATRICE_POUVOIRS_MODIFIEE", null, profil, motif, details, CorrelationContext.current()));
        outbox.publish("configuration", "MATRICE_POUVOIRS", ConfigurationModifieePayload.TYPE, new ConfigurationModifieePayload("MATRICE_POUVOIRS", acteur.id(), motif, List.of(profil)));
    }

    /**
     * Demande à l'administration soumise, acceptée, refusée ou annulée (D-81 §4) : entrée du journal
     * (cible : la demande) ; les événements iam.request-* partent par ailleurs.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void demande(Acteur acteur, String action, String demandeId, String libelle, String motif, List<String> details) {
        journal.save(
            new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), action, demandeId, libelle, motif == null ? null : tronquer(motif, 1000), tronquer(String.join(", ", details), 2000), CorrelationContext.current())
        );
    }

    /**
     * Libellé ou description d'un profil modifiés (D-86, docs/lot1/profils-organisation.md §2) : entrée
     * CONFIGURATION_PROFIL_MODIFIE (cible : le code du profil ; détails : avant, après) et événement de
     * configuration vers l'audit.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void profil(Acteur acteur, String code, String motif, String avant, String apres) {
        String details = tronquer("avant: " + avant + ", après: " + apres, 2000);
        journal.save(new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), "CONFIGURATION_PROFIL_MODIFIE", null, code, motif == null ? null : tronquer(motif, 1000), details, CorrelationContext.current()));
        outbox.publish("configuration", "PROFIL_MODIFIE", ConfigurationModifieePayload.TYPE, new ConfigurationModifieePayload("PROFIL_MODIFIE", acteur.id(), motif, List.of(code, "libelle", "description")));
    }

    private static String tronquer(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void configuration(Acteur acteur, String objet, String motif, List<String> champs) {
        journal.save(new JournalAdministration(clock.instant(), acteur.id(), acteur.nom(), "CONFIGURATION_" + objet, null, objet, motif, String.join(", ", champs), CorrelationContext.current()));
        outbox.publish("configuration", objet, ConfigurationModifieePayload.TYPE, new ConfigurationModifieePayload(objet, acteur.id(), motif, champs));
    }
}
