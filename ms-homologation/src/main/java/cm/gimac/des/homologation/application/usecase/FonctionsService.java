package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.DefinitionFonction;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.event.FunctionChangedPayload;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.DefinitionFonctionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Catalogue des fonctions des rôles R, V, C, I, E et des déposants (D-91, docs/lot1/types-projet.md §2) : création
 * d'une fonction reliée à un ou plusieurs profils IAM, modification avec motif (libellé, profils, externe, active).
 * Une fonction ne se supprime pas. L'écriture est visible aussitôt du domaine ({@link CatalogueFonctions}) et relue
 * après la transaction (validée ou annulée). Chaque acte écrit son événement dans l'outbox (audit).
 */
@Service
@Transactional
public class FonctionsService {

    static final String AGREGAT = "fonction";

    public record Creation(String code, String libelle, List<String> profils, Boolean externe, List<String> permissions) {
        public Creation(String code, String libelle, List<String> profils, Boolean externe) {
            this(code, libelle, profils, externe, null);
        }
    }

    public record Modification(String libelle, List<String> profils, Boolean externe, Boolean actif, String motif, List<String> permissions) {
        public Modification(String libelle, List<String> profils, Boolean externe, Boolean actif, String motif) {
            this(libelle, profils, externe, actif, motif, null);
        }
    }

    private final DefinitionFonctionRepository fonctions;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public FonctionsService(DefinitionFonctionRepository fonctions, OutboxPublisher outbox, Clock clock) {
        this.fonctions = fonctions;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DefinitionFonction> lister(Boolean actif) {
        return fonctions
            .findAll()
            .stream()
            .filter(f -> actif == null || f.isActif() == actif)
            .sorted(Comparator.comparing((DefinitionFonction f) -> Fonction.valueOf(f.getCode())))
            .toList();
    }

    @Transactional(readOnly = true)
    public DefinitionFonction lire(String code) {
        return fonctions.findById(code).orElseThrow(() -> introuvable(code));
    }

    public DefinitionFonction creer(Creation c, UUID acteur) {
        Instant maintenant = clock.instant();
        DefinitionFonction f = DefinitionFonction.creer(c.code(), c.libelle(), c.profils(), Boolean.TRUE.equals(c.externe()), c.permissions(), acteur, maintenant);
        if (fonctions.existsById(f.getCode())) {
            throw HomologationException.conflict("HOM_FONCTION_CODE_PRIS", "Ce code de fonction est déjà pris.", Map.of("code", f.getCode()));
        }
        f = fonctions.saveAndFlush(f);
        publier(FunctionChangedPayload.CREEE, f, List.of(), null, acteur, maintenant);
        retenir(f);
        return f;
    }

    public DefinitionFonction modifier(String code, Modification m, UUID acteur) {
        String motif = m.motif() == null ? "" : m.motif().trim();
        if (motif.isEmpty() || motif.length() > 500) {
            throw HomologationException.unprocessable("HOM_MOTIF_REQUIS", "Le motif de la modification compte 1 à 500 caractères.");
        }
        DefinitionFonction f = fonctions.findByIdForUpdate(code).orElseThrow(() -> introuvable(code));
        Instant maintenant = clock.instant();
        List<String> changements = f.modifier(m.libelle(), m.profils(), m.externe(), m.actif(), m.permissions(), acteur, maintenant);
        if (!changements.isEmpty()) {
            fonctions.flush();
            publier(FunctionChangedPayload.MODIFIEE, f, changements, motif, acteur, maintenant);
            retenir(f);
        }
        return f;
    }

    private void publier(String type, DefinitionFonction f, List<String> changements, String motif, UUID acteur, Instant le) {
        outbox.publish(
            AGREGAT,
            f.getCode(),
            type,
            new FunctionChangedPayload(f.getCode(), f.getLibelle(), f.getProfils(), f.isExterne(), f.isActif(), changements, motif, acteur, le, f.getPermissions())
        );
    }

    /** Visible aussitôt du domaine ; catalogue relu après la transaction (validée ou annulée). */
    private static void retenir(DefinitionFonction f) {
        CatalogueFonctions.retenir(f.definition());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        CatalogueFonctions.invalider();
                    }
                }
            );
        }
    }

    private static HomologationException introuvable(String code) {
        return HomologationException.notFound("HOM_FONCTION_INTROUVABLE", "Fonction inconnue du catalogue.", code);
    }
}
