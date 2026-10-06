package cm.gimac.des.participant.web.rest.lot1;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Candidature;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.ConditionsAdhesion;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Depot;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/** Corps des requêtes de la DDRM, validés à l'entrée (mêmes contraintes que le JDL). */
final class InstitutionRequests {

    private InstitutionRequests() {}

    record CandidatureRequest(
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 50) String sigle,
        @NotNull @Pattern(regexp = "^(CM|CF|CG|GA|GQ|TD)$") String pays,
        @NotNull InstitutionCategory categorie,
        @Pattern(regexp = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$") String codeIdentification,
        @Size(max = 10) @Pattern(regexp = "^[A-Z0-9]+$") String codeBanqueBeac,
        @JourPasseOuPresent LocalDate datePriseDeContact,
        @Size(max = 255) String originePriseDeContact,
        @Size(max = 1000) String projetEnvisage
    ) {
        /**
         * Saisie normalisée avant validation : codes BIC et BEAC en majuscules et sans espaces (copiés
         * d'un courrier, « bdlg ga li »), champs facultatifs vides ramenés à l'absence de valeur.
         */
        CandidatureRequest {
            sigle = vide(sigle);
            codeIdentification = code(codeIdentification);
            codeBanqueBeac = code(codeBanqueBeac);
            originePriseDeContact = vide(originePriseDeContact);
            projetEnvisage = vide(projetEnvisage);
        }

        Candidature commande() {
            return new Candidature(
                nom.trim(),
                sigle,
                pays,
                categorie,
                vide(codeIdentification),
                vide(codeBanqueBeac),
                datePriseDeContact,
                originePriseDeContact,
                projetEnvisage
            );
        }
    }

    record ConditionsRequest(
        @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9-]+$") String codeParticipant,
        @NotNull MembershipMode mode,
        UUID banqueReglementId,
        UUID banqueMarraineId,
        @Size(max = 50) String compteReglement,
        @Size(max = 50) String compteCantonnement
    ) {
        ConditionsAdhesion commande() {
            return new ConditionsAdhesion(codeParticipant, mode, banqueReglementId, banqueMarraineId, vide(compteReglement), vide(compteCantonnement));
        }
    }

    record DepotRequest(
        @NotNull UUID documentId,
        @NotNull @Pattern(regexp = "^[0-9a-f]{64}$") String documentSha256,
        @Size(max = 255) String nomFichier
    ) {
        Depot commande() {
            return new Depot(documentId, documentSha256, nomFichier);
        }
    }

    record MotifRequest(@NotBlank @Size(max = 500) String motif) {}

    record TransitionRequest(@NotNull InstitutionStatus vers, @NotBlank @Size(max = 1000) String motif, UUID pieceDecisionId) {}

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String code(String s) {
        String sansEspaces = s == null ? null : s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return sansEspaces == null || sansEspaces.isEmpty() ? null : sansEspaces;
    }
}
