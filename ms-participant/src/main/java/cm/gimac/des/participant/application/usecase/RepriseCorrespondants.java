package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.application.port.in.RepriseCorrespondantsUseCase;
import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.enumeration.CorrespondentFunction;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import cm.gimac.des.participant.domain.enumeration.Reachability;
import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reprise des correspondants (D-132). Une transaction par ligne, comme la reprise des institutions : une ligne refusée
 * n'empêche pas les autres ; en simulation, chaque ligne est jouée jusqu'à l'écriture en base puis annulée.
 * Idempotence par le couple institution et compte : absent, le correspondant est déclaré actif, proposé et validé par
 * l'acteur de la reprise, joignabilité NON_VERIFIE ; présent, ses nom, fonction, courriel, téléphone et canaux sont mis
 * à jour et il est réactivé ; rapporté inchangé si rien ne diffère. Chaque écriture est journalisée sur l'institution.
 * Le compte n'est pas vérifié auprès de MS-IAM : la Sandbox GIMAC contrôle à l'ouverture de l'espace qu'il est rattaché
 * à l'institution.
 */
@Service
public class RepriseCorrespondants implements RepriseCorrespondantsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RepriseCorrespondants.class);
    static final String MOTIF_DEFAUT = "Reprise de l'existant";
    static final String CANAUX_DEFAUT = "messagerie";
    private static final Set<InstitutionStatus> CLOSES = Set.of(InstitutionStatus.CLOS_SANS_SUITE, InstitutionStatus.RETIRE);

    private final InstitutionRepository institutions;
    private final CorrespondentRepository correspondants;
    private final InteractionRepository interactions;
    private final EntityManager em;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public RepriseCorrespondants(
        InstitutionRepository institutions,
        CorrespondentRepository correspondants,
        InteractionRepository interactions,
        EntityManager em,
        PlatformTransactionManager transactions,
        Clock clock
    ) {
        this.institutions = institutions;
        this.correspondants = correspondants;
        this.interactions = interactions;
        this.em = em;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /** Ligne contrôlée : valeurs normalisées. */
    record Fiche(String codeMembre, UUID compteId, String nom, CorrespondentFunction fonction, String email, String telephone, String canaux, boolean astreinte) {}

    @Override
    public Rapport importer(List<CorrespondantRepris> lignes, boolean simulation, String motif, Acteur acteur) {
        if (lignes == null || lignes.isEmpty()) {
            throw ParticipantException.unprocessable("PAR_REPRISE_VIDE", "Aucun correspondant à reprendre", Map.of());
        }
        if (lignes.size() > LIGNES_MAX) {
            throw ParticipantException.unprocessable("PAR_REPRISE_TROP_DE_LIGNES", "Lot trop volumineux", Map.of("max", LIGNES_MAX, "recues", lignes.size()));
        }
        String m = RepriseInstitutionsValidation.texte(motif);
        String motifLot = m == null ? MOTIF_DEFAUT : m.substring(0, Math.min(m.length(), 200));
        Set<String> vus = new HashSet<>();
        List<Ligne> rapport = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            CorrespondantRepris l = lignes.get(i);
            String code = l == null ? null : RepriseInstitutionsValidation.texte(l.codeMembre());
            if (l == null) {
                rapport.add(refus(i, null, null, "PAR_REPRISE_LIGNE_INVALIDE", "Ligne vide", Map.of()));
            } else if (code != null && l.compteId() != null && !vus.add(code.toUpperCase(Locale.ROOT) + "/" + l.compteId())) {
                rapport.add(refus(i, code, l.compteId(), "PAR_REPRISE_DOUBLON_FICHIER", "Correspondant déjà présent plus haut dans le fichier", Map.of()));
            } else {
                rapport.add(executer(i, l, simulation, motifLot, acteur));
            }
        }
        Map<Resultat, Long> totaux = new EnumMap<>(Resultat.class);
        for (Resultat res : Resultat.values()) {
            totaux.put(res, rapport.stream().filter(x -> x.resultat() == res).count());
        }
        LOG.info("Reprise des correspondants ({}) par {} : {}", simulation ? "simulation" : "réelle", acteur.id(), totaux);
        return new Rapport(simulation, lignes.size(), totaux, rapport);
    }

    private Ligne executer(int rang, CorrespondantRepris l, boolean simulation, String motif, Acteur acteur) {
        try {
            return transaction.execute(statut -> {
                Ligne r = traiter(rang, valider(l), motif, acteur);
                em.flush();
                if (simulation) {
                    statut.setRollbackOnly();
                }
                return r;
            });
        } catch (ParticipantException e) {
            return refus(rang, l.codeMembre(), l.compteId(), e.code(), e.getMessage(), e.details());
        } catch (DataAccessException | PersistenceException | jakarta.validation.ConstraintViolationException e) {
            LOG.warn("Reprise du correspondant de {} refusée par la base : {}", l.codeMembre(), e.getClass().getSimpleName());
            return refus(rang, l.codeMembre(), l.compteId(), "PAR_REPRISE_ECRITURE_REFUSEE", "Écriture refusée par la base de données", Map.of("cause", e.getClass().getSimpleName()));
        }
    }

    static Fiche valider(CorrespondantRepris l) {
        List<String> manquants = new ArrayList<>();
        String code = RepriseInstitutionsValidation.texte(l.codeMembre());
        String nom = RepriseInstitutionsValidation.texte(l.nom());
        if (code == null) {
            manquants.add("codeMembre");
        }
        if (l.compteId() == null) {
            manquants.add("compteId");
        }
        if (nom == null) {
            manquants.add("nom");
        }
        if (!manquants.isEmpty()) {
            throw ParticipantException.unprocessable("PAR_REPRISE_CHAMP_OBLIGATOIRE", "Champ obligatoire absent", Map.of("champs", manquants));
        }
        CorrespondentFunction fonction = CorrespondentFunction.MONETIQUE;
        String f = RepriseInstitutionsValidation.texte(l.fonction());
        if (f != null) {
            try {
                fonction = CorrespondentFunction.valueOf(f.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ParticipantException.unprocessable("PAR_REPRISE_FONCTION_INCONNUE", "Fonction de correspondant inconnue", Map.of("fonction", f));
            }
        }
        String email = RepriseInstitutionsValidation.texte(l.email());
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ParticipantException.unprocessable("PAR_REPRISE_COURRIEL_INVALIDE", "Courriel invalide", Map.of("email", email));
        }
        String telephone = RepriseInstitutionsValidation.texte(l.telephone());
        String canaux = RepriseInstitutionsValidation.texte(l.canaux());
        return new Fiche(
            code.toUpperCase(Locale.ROOT),
            l.compteId(),
            tronquer(nom, 255),
            fonction,
            email == null ? null : tronquer(email.toLowerCase(Locale.ROOT), 255),
            telephone == null ? null : tronquer(telephone, 30),
            canaux == null ? CANAUX_DEFAUT : tronquer(canaux, 255),
            Boolean.TRUE.equals(l.astreinte247())
        );
    }

    private Ligne traiter(int rang, Fiche f, String motif, Acteur acteur) {
        Institution i = institutions
            .findByParticipantCode(f.codeMembre())
            .orElseThrow(() -> ParticipantException.unprocessable("PAR_REPRISE_INSTITUTION_INCONNUE", "Aucune institution ne porte ce code membre", Map.of("codeMembre", f.codeMembre())));
        if (CLOSES.contains(i.getStatus())) {
            throw ParticipantException.conflict("PAR_REPRISE_INSTITUTION_CLOSE", "L'institution est close : aucun correspondant n'y est déclaré", Map.of("statut", i.getStatus().name()));
        }
        UUID institution = i.getId();
        Specification<Correspondent> meme = (r, q, cb) -> cb.and(cb.equal(r.get("institution").get("id"), institution), cb.equal(r.get("userId"), f.compteId()));
        Correspondent c = correspondants.findAll(meme).stream().findFirst().orElse(null);
        if (c == null) {
            c = new Correspondent()
                .fullName(f.nom())
                .duty(f.fonction())
                .channels(f.canaux())
                .email(f.email())
                .phone(f.telephone())
                .reachability(Reachability.NON_VERIFIE)
                .onCall247(f.astreinte())
                .userId(f.compteId())
                .active(true)
                .proposedBy(acteur.id())
                .validatedBy(acteur.id())
                .validatedAt(clock.instant())
                .institution(i);
            em.persist(c);
            journaliser(i, "Correspondant repris de l'existant : " + f.nom() + " (" + f.fonction() + ") : " + motif, acteur, c.getId());
            return new Ligne(rang, f.codeMembre(), f.compteId(), Resultat.CREE, c.getId(), List.of("creation"), null, null, Map.of());
        }
        List<String> changements = new ArrayList<>();
        changer(c.getFullName(), f.nom(), c::setFullName, "nom", changements);
        changer(c.getDuty(), f.fonction(), c::setDuty, "fonction", changements);
        changer(c.getChannels(), f.canaux(), c::setChannels, "canaux", changements);
        changer(c.getOnCall247(), f.astreinte(), c::setOnCall247, "astreinte247", changements);
        changer(c.getActive(), true, c::setActive, "actif", changements);
        if (f.email() != null) {
            changer(c.getEmail(), f.email(), c::setEmail, "email", changements);
        }
        if (f.telephone() != null) {
            changer(c.getPhone(), f.telephone(), c::setPhone, "telephone", changements);
        }
        if (changements.isEmpty()) {
            return new Ligne(rang, f.codeMembre(), f.compteId(), Resultat.INCHANGE, c.getId(), List.of(), null, null, Map.of());
        }
        journaliser(i, "Correspondant mis à jour par la reprise de l'existant (" + String.join(", ", changements) + ") : " + motif, acteur, c.getId());
        return new Ligne(rang, f.codeMembre(), f.compteId(), Resultat.MIS_A_JOUR, c.getId(), changements, null, null, Map.of());
    }

    private static <T> void changer(T avant, T apres, Consumer<T> poser, String champ, List<String> changements) {
        if (!Objects.equals(avant, apres)) {
            poser.accept(apres);
            changements.add(champ);
        }
    }

    private void journaliser(Institution i, String resume, Acteur acteur, UUID correspondant) {
        interactions.save(
            new Interaction()
                .nature(InteractionNature.AUTRE)
                .summary(tronquer(resume, 500))
                .occurredAt(clock.instant())
                .authorId(acteur.id())
                .reference(correspondant.toString())
                .institution(i)
        );
    }

    private static String tronquer(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static Ligne refus(int rang, String code, UUID compte, String erreur, String motif, Map<String, Object> details) {
        return new Ligne(rang, code, compte, Resultat.REFUSE, null, List.of(), erreur, motif, details == null ? Map.of() : details);
    }
}
