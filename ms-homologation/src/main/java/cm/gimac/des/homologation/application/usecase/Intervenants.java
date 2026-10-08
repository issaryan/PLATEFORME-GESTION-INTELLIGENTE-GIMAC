package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Personne;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Intervenants du parcours lus dans l'annuaire de MS-IAM : personne désignée par son identifiant (qualités,
 * blocages, ressources) et intervenants d'une fonction ({@code GET /intervenants?fonction=}).
 */
@Component
public class Intervenants {

    private final CollaboratorDirectoryPort annuaire;
    private PorteursPermission porteurs;

    public Intervenants(CollaboratorDirectoryPort annuaire) {
        this.annuaire = annuaire;
    }

    /** Porteurs d'une permission (D-94) : une personne qui porte l'une des permissions de la fonction peut la tenir. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPorteurs(PorteursPermission porteurs) {
        this.porteurs = porteurs;
    }

    /** Collaborateurs actifs qui tiennent une fonction, par le premier profil qui la confère. */
    public List<Personne> deFonction(Fonction f) {
        if (f.profils().isEmpty()) {
            return List.of();
        }
        return f
            .profils()
            .stream()
            .flatMap(p -> annuaire.parProfil(p).stream())
            .filter(CollaboratorView::actif)
            .map(c -> new Personne(c.id().toString(), c.nomAffiche(), f))
            .distinct()
            .toList();
    }

    /** Personne désignée par son identifiant ; sa fonction est celle de son premier profil connu. */
    public Personne personne(String personneId) {
        CollaboratorView c = collaborateur(personneId);
        Fonction f = c.profils().stream().map(Fonction::duProfil).flatMap(Optional::stream).findFirst().orElseThrow(Intervenants::nonEligible);
        return new Personne(c.id().toString(), c.nomAffiche(), f);
    }

    /**
     * Personne affectée à une fonction (rang 6) : elle doit détenir un profil qui la confère ou porter l'une des
     * permissions qui la caractérisent (D-94, lues dans MS-IAM).
     */
    public Personne pourFonction(String personneId, Fonction f) {
        CollaboratorView c = collaborateur(personneId);
        boolean parProfil = f.profils().stream().anyMatch(c.profils()::contains);
        if (!parProfil && (porteurs == null || f.permissions().isEmpty() || !porteurs.porte(c.id(), f.permissions()))) {
            throw nonEligible();
        }
        return new Personne(c.id().toString(), c.nomAffiche(), f);
    }

    /** Nom affiché d'un collaborateur ; null si l'annuaire est injoignable (la lecture reste possible). */
    public String nom(UUID id) {
        try {
            return annuaire.findCollaborator(id).map(CollaboratorView::nomAffiche).orElse(null);
        } catch (HomologationException e) {
            return null;
        }
    }

    private CollaboratorView collaborateur(String personneId) {
        UUID id;
        try {
            id = UUID.fromString(personneId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw inconnue();
        }
        return annuaire.findCollaborator(id).filter(CollaboratorView::actif).orElseThrow(Intervenants::inconnue);
    }

    private static HomologationException inconnue() {
        return HomologationException.unprocessable("HOM_PERSON_UNKNOWN", "Personne inconnue.");
    }

    private static HomologationException nonEligible() {
        return HomologationException.unprocessable("HOM_PERSON_NOT_ELIGIBLE", "La personne désignée ne détient pas le profil de cette fonction.");
    }
}
