package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.event.AvatarModifiePayload;
import cm.gimac.des.iam.domain.service.TraitementAvatar;
import cm.gimac.des.iam.domain.service.TraitementAvatar.ImageAvatar;
import cm.gimac.des.iam.infrastructure.persistence.AvatarCompte;
import cm.gimac.des.iam.infrastructure.persistence.AvatarCompteRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Photo de profil des comptes (docs/lot1/mon-compte.md §1) : le titulaire la remplace ou la retire ;
 * tout compte interne la lit (annuaire, messagerie, console). L'image est contrôlée et réencodée par
 * {@link TraitementAvatar} avant d'être conservée ; chaque changement entre au journal
 * d'administration et part vers l'audit (iam.avatar-changed.v1), sans l'image.
 */
@Service
@Transactional
public class AvatarService {

    private final AvatarCompteRepository depot;
    private final Annuaire annuaire;
    private final JournalActes journal;
    private final Clock clock;

    public AvatarService(AvatarCompteRepository depot, Annuaire annuaire, JournalActes journal, Clock clock) {
        this.depot = depot;
        this.annuaire = annuaire;
        this.journal = journal;
        this.clock = clock;
    }

    /** Remplace la photo du titulaire ; rend la nouvelle version. */
    public String remplacer(Acteur acteur, byte[] octets) {
        Compte c = compte(acteur);
        ImageAvatar image = TraitementAvatar.traiter(octets);
        AvatarCompte avatar = depot
            .findById(acteur.id())
            .map(a -> {
                a.remplacer(image.octets(), image.type(), image.version(), clock.instant());
                return a;
            })
            .orElseGet(() -> new AvatarCompte(acteur.id(), image.octets(), image.type(), image.version(), clock.instant()));
        depot.save(avatar);
        journal.avatar(acteur, acteur.id(), c.identifiant(), AvatarModifiePayload.REMPLACEE, image.version());
        return image.version();
    }

    /** Retire la photo du titulaire (retour aux initiales) ; sans photo, rien n'est tracé. */
    public void retirer(Acteur acteur) {
        Compte c = compte(acteur);
        depot
            .findById(acteur.id())
            .ifPresent(a -> {
                depot.delete(a);
                journal.avatar(acteur, acteur.id(), c.identifiant(), AvatarModifiePayload.RETIREE, null);
            });
    }

    /** Photo d'un compte, telle que conservée. */
    @Transactional(readOnly = true)
    public Optional<ImageAvatar> lire(String compteId) {
        return compteId == null ? Optional.empty() : depot.findById(compteId).map(a -> new ImageAvatar(a.getImage(), a.getType(), a.getVersion()));
    }

    /** Version de la photo d'un compte ; vide sans photo. */
    @Transactional(readOnly = true)
    public Optional<String> version(String compteId) {
        return compteId == null ? Optional.empty() : depot.version(compteId);
    }

    /** Versions des photos d'un ensemble de comptes (annuaire, listes) ; les comptes sans photo sont absents. */
    @Transactional(readOnly = true)
    public Map<String, String> versions(Collection<String> compteIds) {
        Map<String, String> v = new HashMap<>();
        if (compteIds == null || compteIds.isEmpty()) {
            return v;
        }
        depot.versions(compteIds).forEach(a -> v.put(a.getCompteId(), a.getVersion()));
        return v;
    }

    private Compte compte(Acteur acteur) {
        return annuaire.lire(acteur.id()).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", acteur.id()));
    }
}
