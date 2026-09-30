package cm.gimac.des.certification.application.usecase;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.domain.CertificationException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Accès à une liaison : contexte de certification lu dans MS-HOMOLOGATION (404 HOM_LIAISON_NOT_FOUND si
 * elle est inconnue) et cloisonnement du portail à l'institution du jeton (404 pour une autre institution,
 * sans révéler l'existence de la liaison : docs/lot1/parcours-backend.md §4).
 */
@Component
public class AccesLiaison {

    private final ContexteCertificationPort contextes;

    public AccesLiaison(ContexteCertificationPort contextes) {
        this.contextes = contextes;
    }

    public ContexteCertification contexte(UUID liaisonId) {
        return contextes.contexte(liaisonId).orElseThrow(AccesLiaison::introuvable);
    }

    /** Portail : l'institution de la liaison doit être celle du jeton. */
    public static void verifierInstitution(String institutionLiaison, Acteur acteur) {
        if (institutionLiaison == null || acteur.institutionId() == null || !institutionLiaison.equalsIgnoreCase(acteur.institutionId())) {
            throw introuvable();
        }
    }

    static CertificationException introuvable() {
        return CertificationException.introuvable("HOM_LIAISON_NOT_FOUND", "Ce parcours est introuvable.");
    }
}
