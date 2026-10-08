package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.domain.DefinitionTypeProjet;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.event.ProjectTypeCreatedPayload;
import cm.gimac.des.homologation.domain.event.ProjectTypeUpdatedPayload;
import cm.gimac.des.homologation.domain.parcours.ModeleTypeProjet;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.DefinitionTypeProjetRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Types de projet (D-91, docs/lot1/types-projet.md §1) : création avec le brouillon v1 de leur référentiel (vierge :
 * les onze étapes canoniques sans opération ; copie : étapes, opérations, rôles, livrables et signatures de la version
 * active d'un autre type), modification du libellé, de la description, du modèle (avant la première publication) et
 * de l'état actif, avec motif. Aucun type ne se supprime. Chaque acte écrit son événement dans l'outbox (audit).
 */
@Service
@Transactional
public class TypesProjetService {

    static final String AGREGAT = "type-projet";

    /** Départ du référentiel d'un nouveau type. */
    public enum Mode {
        VIERGE,
        COPIE,
    }

    public record Creation(String code, String libelle, String description, ModeleTypeProjet modele, Mode mode, String typeSource) {}

    public record Modification(String libelle, String description, ModeleTypeProjet modele, Boolean actif, String motif) {}

    public record Cree(DefinitionTypeProjet type, ReferentialVersion brouillon) {}

    private final DefinitionTypeProjetRepository types;
    private final ReferentialVersionRepository versions;
    private final ManageReferentialUseCase referentiels;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public TypesProjetService(
        DefinitionTypeProjetRepository types,
        ReferentialVersionRepository versions,
        ManageReferentialUseCase referentiels,
        OutboxPublisher outbox,
        Clock clock
    ) {
        this.types = types;
        this.versions = versions;
        this.referentiels = referentiels;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DefinitionTypeProjet> lister(Boolean actif) {
        List<DefinitionTypeProjet> l = actif == null ? types.findAll() : types.findByActif(actif);
        return l.stream().sorted(Comparator.comparing(DefinitionTypeProjet::type)).toList();
    }

    @Transactional(readOnly = true)
    public DefinitionTypeProjet lire(String code) {
        return types.findById(code).orElseThrow(() -> introuvable(code));
    }

    /** Le code et le modèle sont figés : une version du référentiel du type a été publiée. */
    @Transactional(readOnly = true)
    public boolean fige(DefinitionTypeProjet t) {
        return versions.dejaPubliee(t.type());
    }

    public Cree creer(Creation c, UUID acteur) {
        if (c.mode() == null) {
            throw depart("Le départ du référentiel est VIERGE ou COPIE.");
        }
        Instant maintenant = clock.instant();
        DefinitionTypeProjet t = DefinitionTypeProjet.creer(c.code(), c.libelle(), c.description(), c.modele(), acteur, maintenant);
        if (types.existsById(t.getCode())) {
            throw HomologationException.conflict("HOM_TYPE_PROJET_CODE_PRIS", "Ce code de type de projet est déjà pris.", Map.of("code", t.getCode()));
        }
        ReferentialVersion source = null;
        if (c.mode() == Mode.COPIE) {
            if (c.typeSource() == null || c.typeSource().isBlank()) {
                throw depart("Une copie désigne le type de projet source (typeSource).");
            }
            DefinitionTypeProjet st = types.findById(c.typeSource()).orElseThrow(() -> introuvable(c.typeSource()));
            source = versions
                .findFirstByTypeProjetAndEtat(st.type(), EtatVersionReferentiel.PUBLIEE)
                .orElseThrow(() ->
                    HomologationException.conflict(
                        "HOM_TYPE_PROJET_SOURCE_NON_PUBLIE",
                        "Le type source n'a aucune version publiée de son référentiel à copier.",
                        Map.of("typeSource", st.getCode())
                    )
                );
        }
        t = types.saveAndFlush(t);
        String libelle = "Référentiel " + t.getLibelle() + " (version 1)";
        ReferentialVersion brouillon = source == null
            ? referentiels.creerBrouillon(t.type(), libelle, null, acteur, null)
            : referentiels.copierVers(source.getId(), t.type(), libelle, acteur);
        outbox.publish(
            AGREGAT,
            t.getCode(),
            ProjectTypeCreatedPayload.TYPE,
            new ProjectTypeCreatedPayload(
                t.getCode(),
                t.getLibelle(),
                t.getDescription(),
                t.getModele().name(),
                t.isActif(),
                c.mode().name(),
                source == null ? null : source.getTypeProjet().name(),
                source == null ? null : source.getId(),
                brouillon.getId(),
                acteur,
                maintenant
            )
        );
        return new Cree(t, brouillon);
    }

    public DefinitionTypeProjet modifier(String code, Modification m, UUID acteur) {
        String motif = m.motif() == null ? "" : m.motif().trim();
        if (motif.isEmpty() || motif.length() > 500) {
            throw HomologationException.unprocessable("HOM_MOTIF_REQUIS", "Le motif de la modification compte 1 à 500 caractères.");
        }
        DefinitionTypeProjet t = types.findByIdForUpdate(code).orElseThrow(() -> introuvable(code));
        Instant maintenant = clock.instant();
        List<String> changements = new ArrayList<>();
        if (m.modele() != null && m.modele() != t.getModele()) {
            if (fige(t)) {
                throw HomologationException.conflict(
                    "HOM_TYPE_PROJET_FIGE",
                    "Le code et le modèle d'un type sont figés après la première publication de son référentiel.",
                    Map.of("code", code, "modele", t.getModele().name())
                );
            }
            t.changerModele(m.modele(), acteur, maintenant);
            changements.add("modele");
        }
        changements.addAll(t.modifier(m.libelle(), m.description(), m.actif(), acteur, maintenant));
        if (!changements.isEmpty()) {
            outbox.publish(
                AGREGAT,
                t.getCode(),
                ProjectTypeUpdatedPayload.TYPE,
                new ProjectTypeUpdatedPayload(
                    t.getCode(),
                    t.getLibelle(),
                    t.getDescription(),
                    t.getModele().name(),
                    t.isActif(),
                    List.copyOf(changements),
                    motif,
                    acteur,
                    maintenant
                )
            );
        }
        return t;
    }

    private static HomologationException introuvable(String code) {
        return HomologationException.notFound("HOM_TYPE_PROJET_INTROUVABLE", "Type de projet inconnu.", code);
    }

    private static HomologationException depart(String message) {
        return HomologationException.unprocessable("HOM_TYPE_PROJET_DEPART_INVALIDE", message);
    }
}
