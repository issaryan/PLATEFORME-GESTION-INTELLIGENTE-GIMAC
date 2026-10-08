package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Porteurs d'une permission dans l'annuaire de MS-IAM (D-94 §3) : pilote d'un référentiel (HOM_REFERENTIEL_EDIT ou
 * REF_METIER_ADMIN), conducteur de projet (HOM_PROJECT_CONDUCT), candidats d'une fonction à affecter (permissions de la
 * fonction au catalogue D-91). Aucune règle ne cite un nom de profil : la lecture passe par
 * {@code GET /api/v1/interne/collaborateurs?permission=} ; MS-IAM indisponible : 503 HOM_IAM_UNAVAILABLE (aucun repli
 * de configuration). Seuls les comptes internes actifs (sans institution) sont retenus.
 */
@Component
public class PorteursPermission {

    private final CollaboratorDirectoryPort annuaire;

    public PorteursPermission(CollaboratorDirectoryPort annuaire) {
        this.annuaire = annuaire;
    }

    /** Comptes internes actifs portant l'une des permissions, triés par nom. */
    public List<CollaboratorView> porteurs(String... permissions) {
        return porteurs(List.of(permissions));
    }

    /** Comptes internes actifs portant l'une des permissions, triés par nom. */
    public List<CollaboratorView> porteurs(List<String> permissions) {
        Map<UUID, CollaboratorView> m = new LinkedHashMap<>();
        for (String p : permissions) {
            annuaire.parPermission(p).stream().filter(PorteursPermission::interneActif).forEach(c -> m.putIfAbsent(c.id(), c));
        }
        return m.values().stream().sorted(Comparator.comparing(CollaboratorView::nomAffiche, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    /** Le compte est interne, actif et porte l'une des permissions. */
    public boolean porte(UUID compte, String... permissions) {
        return porte(compte, List.of(permissions));
    }

    /** Le compte est interne, actif et porte l'une des permissions. */
    public boolean porte(UUID compte, List<String> permissions) {
        if (compte == null) {
            return false;
        }
        for (String p : permissions) {
            if (annuaire.parPermission(p).stream().anyMatch(c -> compte.equals(c.id()) && interneActif(c))) {
                return true;
            }
        }
        return false;
    }

    static boolean interneActif(CollaboratorView c) {
        return c.actif() && (c.institutionId() == null || c.institutionId().isBlank());
    }
}
