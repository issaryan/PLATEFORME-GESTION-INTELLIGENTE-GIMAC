package cm.gimac.des.participant.application.usecase;

import cm.gimac.des.participant.application.AdhesionProperties;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.application.port.in.RepriseInstitutionsUseCase;
import cm.gimac.des.participant.application.usecase.RepriseInstitutionsValidation.Fiche;
import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import cm.gimac.des.participant.domain.enumeration.SubscriptionState;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.domain.event.InstitutionRegisteredPayload;
import cm.gimac.des.participant.domain.event.InstitutionUpdatedPayload;
import cm.gimac.des.participant.domain.event.StatusChangedPayload;
import cm.gimac.des.participant.domain.service.InstitutionLifecycle;
import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reprise des institutions membres (D-107, docs/exploitation/reprise-des-donnees.md §3). Une transaction par ligne :
 * une ligne refusée n'empêche pas les autres ; en simulation, chaque ligne est jouée jusqu'à l'écriture en base
 * (contraintes comprises) puis annulée. Chaque écriture est tracée comme une saisie : journal de l'institution,
 * transition de statut automatique motivée « reprise de l'existant », événements participant.* par l'outbox.
 * Les prérequis du maker-checker (pièces validées, attestation, mise en service) ne sont pas exigés : la décision
 * d'origine a été prise hors de la plateforme, sa référence est conservée dans le motif de la transition.
 */
@Service
public class RepriseInstitutions implements RepriseInstitutionsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RepriseInstitutions.class);
    static final String MOTIF_DEFAUT = "Reprise de l'existant";
    static final String AGREGAT = "institution";
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Douala");

    private final InstitutionRepository institutions;
    private final AdhesionPieceRepository pieces;
    private final StatusTransitionRepository transitions;
    private final InteractionRepository interactions;
    private final SubscribedServiceRepository services;
    private final OutboxPublisher outbox;
    private final AdhesionProperties adhesion;
    private final EntityManager em;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public RepriseInstitutions(
        InstitutionRepository institutions,
        AdhesionPieceRepository pieces,
        StatusTransitionRepository transitions,
        InteractionRepository interactions,
        SubscribedServiceRepository services,
        OutboxPublisher outbox,
        AdhesionProperties adhesion,
        EntityManager em,
        PlatformTransactionManager transactions,
        Clock clock
    ) {
        this.institutions = institutions;
        this.pieces = pieces;
        this.transitions = transitions;
        this.interactions = interactions;
        this.services = services;
        this.outbox = outbox;
        this.adhesion = adhesion;
        this.em = em;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /** Contexte d'un lot : codes et UUID déjà vus, institutions créées par une ligne précédente d'une simulation. */
    private record Lot(boolean simulation, String motif, Acteur acteur, Set<String> codes, Set<UUID> ids, Set<String> simulees) {}

    @Override
    public Rapport importer(List<InstitutionReprise> lignes, boolean simulation, String motif, Acteur acteur) {
        if (lignes == null || lignes.isEmpty()) {
            throw ParticipantException.unprocessable("PAR_REPRISE_VIDE", "Aucune institution à reprendre", Map.of());
        }
        if (lignes.size() > LIGNES_MAX) {
            throw ParticipantException.unprocessable("PAR_REPRISE_TROP_DE_LIGNES", "Lot trop volumineux", Map.of("max", LIGNES_MAX, "recues", lignes.size()));
        }
        String m = RepriseInstitutionsValidation.texte(motif);
        Lot lot = new Lot(simulation, m == null ? MOTIF_DEFAUT : (m.length() > 200 ? m.substring(0, 200) : m), acteur, new HashSet<>(), new HashSet<>(), new HashSet<>());
        List<Ligne> rapport = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            InstitutionReprise l = lignes.get(i);
            Ligne r = doublon(i, l, lot);
            if (r == null) {
                r = executer(i, l, lot);
            }
            if (simulation && r.resultat() == Resultat.CREEE) {
                lot.simulees().add(r.codeMembre());
            }
            rapport.add(r);
        }
        Map<Resultat, Long> totaux = new EnumMap<>(Resultat.class);
        for (Resultat res : Resultat.values()) {
            totaux.put(res, rapport.stream().filter(x -> x.resultat() == res).count());
        }
        LOG.info("Reprise des institutions ({}) par {} : {}", simulation ? "simulation" : "réelle", acteur.id(), totaux);
        return new Rapport(simulation, lignes.size(), totaux, rapport);
    }

    /** Doublon dans le fichier (code membre ou UUID déjà vu) : les lignes annulées d'une simulation ne se voient pas. */
    private static Ligne doublon(int rang, InstitutionReprise l, Lot lot) {
        String code = l == null ? null : RepriseInstitutionsValidation.texte(l.codeMembre());
        if (l == null) {
            return refus(rang, null, "PAR_REPRISE_LIGNE_INVALIDE", "Ligne vide", Map.of());
        }
        if (code != null && !lot.codes().add(code.toUpperCase(java.util.Locale.ROOT))) {
            return refus(rang, code, "PAR_REPRISE_DOUBLON_FICHIER", "Code membre déjà présent plus haut dans le fichier", Map.of("codeMembre", code));
        }
        if (l.id() != null && !lot.ids().add(l.id())) {
            return refus(rang, code, "PAR_REPRISE_DOUBLON_FICHIER", "Identifiant déjà présent plus haut dans le fichier", Map.of("id", l.id().toString()));
        }
        return null;
    }

    private Ligne executer(int rang, InstitutionReprise l, Lot lot) {
        try {
            return transaction.execute(statut -> {
                Ligne r = traiter(rang, l, lot);
                em.flush();
                if (lot.simulation()) {
                    statut.setRollbackOnly();
                }
                return r;
            });
        } catch (ParticipantException e) {
            return refus(rang, l.codeMembre(), e.code(), e.getMessage(), e.details());
        } catch (DataAccessException | PersistenceException | jakarta.validation.ConstraintViolationException e) {
            LOG.warn("Reprise de l'institution {} refusée par la base : {}", l.codeMembre(), e.getClass().getSimpleName());
            return refus(rang, l.codeMembre(), "PAR_REPRISE_ECRITURE_REFUSEE", "Écriture refusée par la base de données", Map.of("cause", e.getClass().getSimpleName()));
        }
    }

    private Ligne traiter(int rang, InstitutionReprise l, Lot lot) {
        Fiche f = RepriseInstitutionsValidation.valider(l, aujourdhui());
        List<String> avertissements = new ArrayList<>(f.avertissements());
        Institution existante = institutions.findByParticipantCode(f.codeMembre()).orElse(null);
        if (f.id() != null) {
            if (existante != null && !existante.getId().equals(f.id())) {
                throw ParticipantException.conflict(
                    "PAR_REPRISE_ID_DIVERGENT",
                    "Ce code membre est déjà porté par une institution d'un autre identifiant",
                    Map.of("codeMembre", f.codeMembre(), "id", f.id().toString(), "idEnregistre", existante.getId().toString())
                );
            }
            if (existante == null && institutions.existsById(f.id())) {
                throw ParticipantException.conflict("PAR_REPRISE_ID_DEJA_PRIS", "Cet identifiant est déjà porté par une autre institution", Map.of("id", f.id().toString()));
            }
        }
        UUID soi = existante == null ? new UUID(0, 0) : existante.getId();
        unicite(f.codeIdentification() != null && institutions.existsByIdentificationCodeAndIdNot(f.codeIdentification(), soi), "codeIdentification", f.codeIdentification());
        unicite(f.codeBanqueBeac() != null && institutions.existsByBeacBankCodeAndIdNot(f.codeBanqueBeac(), soi), "codeBanqueBeac", f.codeBanqueBeac());
        Institution reglement = banque(f.banqueReglement(), "banqueReglement", f.codeMembre(), lot, avertissements);
        Institution marraine = banque(f.banqueMarraine(), "banqueMarraine", f.codeMembre(), lot, avertissements);
        return existante == null
            ? creer(rang, f, reglement, marraine, lot, avertissements)
            : mettreAJour(rang, f, institutions.findByIdForUpdate(existante.getId()).orElseThrow(), reglement, marraine, lot, avertissements);
    }

    private Ligne creer(int rang, Fiche f, Institution reglement, Institution marraine, Lot lot, List<String> avertissements) {
        Instant maintenant = clock.instant();
        Institution i = new Institution()
            .status(f.statut())
            .complianceState(conformite(f.statut()))
            .openLinkCount(0)
            .openSuspenseCount(0)
            .createdAt(maintenant)
            .ddrmOfficerId(lot.acteur().id())
            .ddrmOfficerName(lot.acteur().nom());
        i.setId(f.id());
        i.setParticipantCode(f.codeMembre());
        i.setContactOrigin(lot.motif());
        appliquerFiche(i, f, new ArrayList<>());
        appliquerConditions(i, f, reglement, marraine, new ArrayList<>());
        i.setContactedOn(premiere(f.datePriseDeContact(), f.dateAdhesion(), f.dateDecision(), aujourdhui()));
        appliquerDates(i, f, lot);
        em.persist(i);
        for (AdhesionProperties.PieceDefinition def : adhesion.pieces()) {
            pieces.save(new AdhesionPiece().code(def.code()).label(def.libelle()).mandatory(def.obligatoire()).state(AdhesionPieceState.ATTENDUE).institution(i));
        }
        souscrire(i, f, new ArrayList<>());
        journaliser(i, InteractionNature.AUTRE, "Institution reprise de l'existant : " + lot.motif(), lot.acteur(), f.referenceDecision());
        outbox.publish(
            AGREGAT,
            i.getId(),
            InstitutionRegisteredPayload.TYPE,
            new InstitutionRegisteredPayload(i.getId(), i.getName(), i.getAcronym(), i.getCountry(), i.getCategory().name(), i.getStatus().name(), i.getContactedOn(), lot.acteur().id())
        );
        if (f.statut() != InstitutionStatus.CANDIDAT) {
            transitionReprise(i, InstitutionStatus.CANDIDAT, f, lot);
        }
        return new Ligne(rang, f.codeMembre(), Resultat.CREEE, i.getId(), i.getStatus().name(), List.of("creation"), avertissements, null, null, Map.of());
    }

    private Ligne mettreAJour(int rang, Fiche f, Institution i, Institution reglement, Institution marraine, Lot lot, List<String> avertissements) {
        List<String> changements = new ArrayList<>();
        appliquerFiche(i, f, changements);
        appliquerConditions(i, f, reglement, marraine, changements);
        changer(i.getContactedOn(), f.datePriseDeContact(), i::setContactedOn, "datePriseDeContact", changements);
        InstitutionStatus avant = i.getStatus();
        if (avant != f.statut()) {
            if (!InstitutionLifecycle.atteignable(avant, f.statut())) {
                throw ParticipantException.conflict(
                    "PAR_REPRISE_STATUT_INATTEIGNABLE",
                    "Le statut cible n'est pas atteignable depuis le statut enregistré : la reprise ne remonte pas l'automate",
                    Map.of("statut", avant.name(), "cible", f.statut().name())
                );
            }
            i.setStatus(f.statut());
            i.setComplianceState(conformite(f.statut()));
            changements.add("statut");
        }
        if (avant != f.statut() || f.dateAdhesion() != null) {
            LocalDate adhesionAvant = i.getJoinedOn();
            appliquerDates(i, f, lot);
            if (!Objects.equals(adhesionAvant, i.getJoinedOn())) {
                changements.add("dateAdhesion");
            }
        }
        souscrire(i, f, changements);
        if (changements.isEmpty()) {
            return new Ligne(rang, f.codeMembre(), Resultat.INCHANGEE, i.getId(), i.getStatus().name(), List.of(), avertissements, null, null, Map.of());
        }
        journaliser(i, InteractionNature.AUTRE, "Fiche mise à jour par la reprise de l'existant (" + String.join(", ", changements) + ") : " + lot.motif(), lot.acteur(), f.referenceDecision());
        outbox.publish(AGREGAT, i.getId(), InstitutionUpdatedPayload.TYPE, new InstitutionUpdatedPayload(i.getId(), List.copyOf(changements), lot.acteur().id()));
        if (avant != f.statut()) {
            transitionReprise(i, avant, f, lot);
        }
        return new Ligne(rang, f.codeMembre(), Resultat.MISE_A_JOUR, i.getId(), i.getStatus().name(), List.copyOf(changements), avertissements, null, null, Map.of());
    }

    /** Transition de reprise : automatique, motivée, décidée par le compte de reprise, tracée et publiée. */
    private void transitionReprise(Institution i, InstitutionStatus depuis, Fiche f, Lot lot) {
        Instant maintenant = clock.instant();
        String motif = lot.motif()
            + (f.referenceDecision() == null ? "" : " ; décision " + f.referenceDecision())
            + (f.dateDecision() == null ? "" : " du " + f.dateDecision());
        StatusTransition t = transitions.save(
            new StatusTransition()
                .fromStatus(depuis)
                .toStatus(i.getStatus())
                .reason(motif)
                .requesterId(lot.acteur().id())
                .requestedAt(maintenant)
                .automaticTransition(true)
                .requiredApprovals(1)
                .approverId(lot.acteur().id())
                .state(TransitionState.APPROUVEE)
                .decidedAt(maintenant)
                .institution(i)
        );
        journaliser(i, InteractionNature.CHANGEMENT_STATUT, depuis + " vers " + i.getStatus() + " : " + motif, lot.acteur(), t.getId().toString());
        outbox.publish(
            AGREGAT,
            i.getId(),
            StatusChangedPayload.TYPE,
            new StatusChangedPayload(i.getId(), i.getParticipantCode(), depuis.name(), i.getStatus().name(), motif, t.getId(), f.dateDecision() == null ? maintenant : debut(f.dateDecision()))
        );
    }

    /** Dates propres au statut : prononcé de l'adhésion, clôture sans suite. Une date déjà connue n'est pas effacée. */
    private void appliquerDates(Institution i, Fiche f, Lot lot) {
        InstitutionStatus s = i.getStatus();
        if (s == InstitutionStatus.CLOS_SANS_SUITE) {
            if (i.getClosedAt() == null) {
                i.setClosedAt(f.dateDecision() == null ? clock.instant() : debut(f.dateDecision()));
                i.setClosureReason(lot.motif());
            }
            return;
        }
        if (!InstitutionLifecycle.atteignable(InstitutionStatus.ADHERENT, s)) {
            return;
        }
        LocalDate adhesion = premiere(f.dateAdhesion(), s == InstitutionStatus.ADHERENT ? f.dateDecision() : null, null, null);
        if (adhesion != null && !adhesion.equals(i.getJoinedOn())) {
            i.setJoinedOn(adhesion);
            i.setAdmittedAt(debut(adhesion));
        }
        if (i.getAdmittedById() == null) {
            i.setAdmittedById(lot.acteur().id());
        }
    }

    private static void appliquerFiche(Institution i, Fiche f, List<String> changements) {
        changer(i.getName(), f.nom(), i::setName, "nom", changements);
        changer(i.getAcronym(), f.sigle(), i::setAcronym, "sigle", changements);
        changer(i.getCountry(), f.pays(), i::setCountry, "pays", changements);
        changer(i.getCategory(), f.categorie(), i::setCategory, "categorie", changements);
        changer(i.getIdentificationCode(), f.codeIdentification(), i::setIdentificationCode, "codeIdentification", changements);
        changer(i.getBeacBankCode(), f.codeBanqueBeac(), i::setBeacBankCode, "codeBanqueBeac", changements);
        changer(i.getGroupName(), f.groupe(), i::setGroupName, "groupe", changements);
    }

    private static void appliquerConditions(Institution i, Fiche f, Institution reglement, Institution marraine, List<String> changements) {
        changer(i.getMembershipMode(), f.mode(), i::setMembershipMode, "modeAdhesion", changements);
        changer(i.getSettlementAccountRef(), f.compteReglement(), i::setSettlementAccountRef, "compteReglement", changements);
        changer(i.getRingFencedAccountRef(), f.compteCantonnement(), i::setRingFencedAccountRef, "compteCantonnement", changements);
        changer(identifiant(i.getSettlementBank()), identifiant(reglement), x -> i.setSettlementBank(reglement), "banqueReglement", changements);
        changer(identifiant(i.getSponsorBank()), identifiant(marraine), x -> i.setSponsorBank(marraine), "banqueMarraine", changements);
    }

    /** Services souscrits absents ajoutés (jamais résiliés par la reprise). */
    private void souscrire(Institution i, Fiche f, List<String> changements) {
        Set<String> deja = new HashSet<>();
        if (i.getId() != null && em.contains(i)) {
            services.findByInstitution_IdAndStateOrderByServiceCode(i.getId(), SubscriptionState.ACTIF).forEach(s -> deja.add(s.getServiceCode()));
        }
        boolean ajout = false;
        for (ServiceRepris s : f.services()) {
            if (deja.add(s.code())) {
                LocalDate depuis = premiere(s.depuis(), i.getJoinedOn(), aujourdhui(), null);
                services.save(new SubscribedService().serviceCode(s.code()).subscribedOn(depuis).state(SubscriptionState.ACTIF).institution(i));
                ajout = true;
            }
        }
        if (ajout) {
            changements.add("servicesSouscrits");
        }
    }

    /** Banque désignée par code membre ; créée par une ligne précédente d'une simulation : signalée, non rattachée. */
    private Institution banque(String code, String champ, String soi, Lot lot, List<String> avertissements) {
        if (code == null) {
            return null;
        }
        if (code.equals(soi)) {
            throw ParticipantException.conflict("PAR_SPONSOR_INVALID", "Une institution ne peut être sa propre banque de règlement ou marraine", Map.of("champ", champ));
        }
        Institution b = institutions.findByParticipantCode(code).orElse(null);
        if (b == null && lot.simulation() && lot.simulees().contains(code)) {
            avertissements.add("Banque " + code + " (" + champ + ") créée par une ligne précédente du fichier : rattachée à l'import réel");
            return null;
        }
        if (b == null) {
            throw ParticipantException.conflict("PAR_REPRISE_BANQUE_INCONNUE", "Banque inconnue du registre (à reprendre avant cette ligne)", Map.of("champ", champ, "codeMembre", code));
        }
        return b;
    }

    private static void unicite(boolean doublon, String champ, String valeur) {
        if (doublon) {
            throw ParticipantException.conflict("PAR_CODE_ALREADY_USED", "Code déjà attribué à une autre institution", Map.of("champ", champ, "valeur", valeur));
        }
    }

    /** Une valeur fournie remplace la valeur enregistrée ; une valeur absente ne l'efface pas. */
    private static <T> void changer(T actuel, T nouveau, Consumer<T> appliquer, String champ, List<String> changements) {
        if (nouveau != null && !nouveau.equals(actuel)) {
            appliquer.accept(nouveau);
            changements.add(champ);
        }
    }

    private static UUID identifiant(Institution i) {
        return i == null ? null : i.getId();
    }

    static ComplianceState conformite(InstitutionStatus s) {
        return switch (s) {
            case CANDIDAT, A_REGULARISER, CLOS_SANS_SUITE -> ComplianceState.A_REGULARISER;
            default -> ComplianceState.CONFORME;
        };
    }

    @SafeVarargs
    private static <T> T premiere(T... valeurs) {
        for (T v : valeurs) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private void journaliser(Institution i, InteractionNature nature, String resume, Acteur acteur, String reference) {
        interactions.save(
            new Interaction()
                .nature(nature)
                .summary(resume.length() > 500 ? resume.substring(0, 500) : resume)
                .occurredAt(clock.instant())
                .authorId(acteur.id())
                .reference(reference)
                .institution(i)
        );
    }

    private static Ligne refus(int rang, String code, String erreur, String motif, Map<String, Object> details) {
        return new Ligne(rang, code, Resultat.REFUSEE, null, null, List.of(), List.of(), erreur, motif, details == null ? Map.of() : details);
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(clock.withZone(FUSEAU));
    }

    private static Instant debut(LocalDate d) {
        return d.atStartOfDay(FUSEAU).toInstant();
    }
}
