package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.exploitation.Empreintes;
import cm.gimac.des.monitoring.infrastructure.persistance.MainCouranteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main courante chaînée d'une vacation (MON-14, CC 6.1.10.1) : chaque entrée porte l'empreinte de la précédente,
 * {@code empreinte = SHA-256(précédente ∥ vacation ∥ rang ∥ instant ∥ auteur ∥ nature ∥ texte ∥ référence)} ; une entrée
 * n'est jamais modifiée, une correction est une nouvelle entrée qui cite la précédente. La vacation entre dans
 * l'empreinte : deux chaînes qui reçoivent la même entrée automatique au même instant restent distinctes.
 * L'instant est arrondi à la microseconde, précision de la base, pour que l'empreinte se recalcule à la relecture.
 */
@Component
public class MainCourante {

    /** Auteur des entrées automatiques (acquittements, escalades, anomalies). */
    public static final UUID SYSTEME = new UUID(0L, 0L);

    private final MainCouranteRepository entrees;

    public MainCourante(MainCouranteRepository entrees) {
        this.entrees = entrees;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ShiftLogEntry ajouter(Shift vacation, UUID auteur, ShiftLogNature nature, String texte, String reference, ShiftLogEntry rectifie, Instant le) {
        ShiftLogEntry derniere = entrees.findFirstByShiftIdOrderByChainIndexDesc(vacation.getId()).orElse(null);
        long rang = derniere == null ? 1 : derniere.getChainIndex() + 1;
        String precedente = derniere == null ? Empreintes.ORIGINE : derniere.getHash();
        ShiftLogEntry e = new ShiftLogEntry()
            .chainIndex(rang)
            .occurredAt(le.truncatedTo(ChronoUnit.MICROS))
            .authorId(auteur)
            .nature(nature)
            .text(texte)
            .reference(reference)
            .previousHash(precedente)
            .shift(vacation)
            .corrects(rectifie);
        e.setHash(empreinte(e));
        return entrees.save(e);
    }

    /** Empreinte d'une entrée, recalculée à partir de ses champs. */
    public static String empreinte(ShiftLogEntry e) {
        return Empreintes.sha256(
            e.getPreviousHash(),
            String.valueOf(e.getShift().getId()),
            String.valueOf(e.getChainIndex()),
            e.getOccurredAt().toString(),
            String.valueOf(e.getAuthorId()),
            e.getNature().name(),
            e.getText(),
            e.getReference()
        );
    }

    /** Vérification de la chaîne : intacte, nombre d'entrées, tête. */
    public record Verification(boolean intacte, int entrees, String tete) {}

    public static Verification verifier(List<ShiftLogEntry> chaine) {
        String precedente = Empreintes.ORIGINE;
        long rang = 1;
        for (ShiftLogEntry e : chaine) {
            if (e.getChainIndex() != rang || !precedente.equals(e.getPreviousHash()) || !empreinte(e).equals(e.getHash())) {
                return new Verification(false, chaine.size(), precedente);
            }
            precedente = e.getHash();
            rang++;
        }
        return new Verification(true, chaine.size(), precedente);
    }
}
