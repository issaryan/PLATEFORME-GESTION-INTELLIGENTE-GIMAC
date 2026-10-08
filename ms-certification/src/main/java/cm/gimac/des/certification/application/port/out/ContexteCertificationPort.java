package cm.gimac.des.certification.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contexte de certification d'une liaison, servi par MS-HOMOLOGATION
 * ({@code GET /api/v1/interne/liaisons/{id}/contexte-certification}, jeton de service,
 * INTERNAL_HOMOLOGATION_READ ; docs/lot1/parcours-backend.md §2). Il sert à établir le procès-verbal
 * (100 % exigés, constat fait), à déterminer les attestations exigées et à cloisonner le portail. Depuis D-91, il
 * porte le modèle du type de projet ({@code modeleTypeProjet}) qui fixe les attestations exigées.
 */
public interface ContexteCertificationPort {
    /** Contexte de la liaison, vide si elle est inconnue ; 503 CER_HOMOLOGATION_UNAVAILABLE si le service ne répond pas. */
    Optional<ContexteCertification> contexte(UUID liaisonId);

    record Couverture(Integer taux, int numerateur, int denominateur) {}

    record ContexteCertification(
        UUID liaisonId,
        String reference,
        String institutionId,
        String institutionNom,
        String typeProjet,
        boolean fintech,
        String statut,
        Couverture couvertureCertification,
        boolean couvertureConstatee,
        List<String> servicesCouverts,
        String conducteurId,
        String conducteurNom,
        String modeleTypeProjet,
        List<String> attestationsExigees
    ) {
        public ContexteCertification {
            servicesCouverts = servicesCouverts == null ? List.of() : List.copyOf(servicesCouverts);
            couvertureCertification = couvertureCertification == null ? new Couverture(null, 0, 0) : couvertureCertification;
            attestationsExigees = attestationsExigees == null ? null : List.copyOf(attestationsExigees);
        }

        /** Contexte sans modèle (MS-HOMOLOGATION antérieure à D-91) : le modèle est déduit du code du type. */
        public ContexteCertification(
            UUID liaisonId,
            String reference,
            String institutionId,
            String institutionNom,
            String typeProjet,
            boolean fintech,
            String statut,
            Couverture couvertureCertification,
            boolean couvertureConstatee,
            List<String> servicesCouverts,
            String conducteurId,
            String conducteurNom
        ) {
            this(
                liaisonId,
                reference,
                institutionId,
                institutionNom,
                typeProjet,
                fintech,
                statut,
                couvertureCertification,
                couvertureConstatee,
                servicesCouverts,
                conducteurId,
                conducteurNom,
                null,
                null
            );
        }

        /** Modèle du type de projet (D-91) ; absent : celui du type repris de même code, sinon générique. */
        public String modele() {
            return cm.gimac.des.certification.domain.service.ApprobationPolicy.modele(modeleTypeProjet, typeProjet);
        }
    }
}
