package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.ParticipantDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adaptateur de substitution (développement, tests) : institutions enregistrées par les jeux de données. */
@Component
@ConditionalOnProperty(name = "gimac.adapters.participant-reader", havingValue = "stub", matchIfMissing = true)
public class ParticipantStubAdapter implements ParticipantDirectoryPort {

    /** Jeu de démonstration : un adhérent et une banque adhérente, stables d'un démarrage à l'autre. */
    public static final UUID EP_ADHERENT = UUID.fromString("00000000-0000-4000-8000-00000000a001");
    public static final UUID BANQUE_ADHERENTE = UUID.fromString("00000000-0000-4000-8000-00000000b001");

    private final Map<UUID, InstitutionView> institutions = new ConcurrentHashMap<>();
    /** Codes membres des institutions enregistrées (reprise de l'existant, D-107). */
    private final Map<String, UUID> codes = new ConcurrentHashMap<>();

    public ParticipantStubAdapter() {
        enregistrer(new InstitutionView(EP_ADHERENT, "Établissement de paiement A", "ADHERENT", "ETABLISSEMENT_PAIEMENT",
            List.of(new InstitutionView.ServiceSouscrit("PAIEMENT_MOBILE", "2026-09-01"))));
        enregistrer(new InstitutionView(BANQUE_ADHERENTE, "Banque B", "ADHERENT", "BANQUE",
            List.of(new InstitutionView.ServiceSouscrit("CARTE_INTERBANCAIRE", "2026-09-01"))));
    }

    public void enregistrer(InstitutionView institution) {
        institutions.put(institution.id(), institution);
    }

    /** Enregistre une institution et son code membre (jeux d'essai de la reprise, D-107). */
    public void enregistrer(InstitutionView institution, String codeMembre) {
        // Le code membre est aussi le code participant servi par l'interface interne (US-EXI-16).
        enregistrer(
            institution.codeParticipant() != null
                ? institution
                : new InstitutionView(institution.id(), institution.nom(), institution.statut(), institution.categorie(), institution.servicesSouscrits(), codeMembre)
        );
        codes.put(codeMembre, institution.id());
    }

    @Override
    public Optional<InstitutionView> parCodeMembre(String codeMembre) {
        return Optional.ofNullable(codes.get(codeMembre)).map(institutions::get);
    }

    @Override
    public InstitutionView getInstitution(UUID institutionId) {
        return Optional.ofNullable(institutions.get(institutionId)).orElseThrow(() ->
            HomologationException.conflict("HOM_LIAISON_INSTITUTION_UNKNOWN", "Institution inconnue du registre", Map.of("institutionId", institutionId.toString()))
        );
    }
}
