package cm.gimac.des.audit.application.usecase;

import cm.gimac.des.audit.application.port.in.ScellerSignatureUseCase;
import cm.gimac.des.audit.domain.AuditEvent;
import cm.gimac.des.audit.domain.DeduplicationEntry;
import cm.gimac.des.audit.domain.JsonCanonicalizer;
import cm.gimac.des.audit.domain.MerkleNode;
import cm.gimac.des.audit.domain.registre.ArbreMerkle;
import cm.gimac.des.audit.repository.AuditEventRepository;
import cm.gimac.des.audit.repository.DeduplicationEntryRepository;
import cm.gimac.des.audit.repository.MerkleNodeRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscription au registre d'audit (CC §6.1.3.3) des signatures de processus d'unité et d'opérations d'homologation
 * (D-93 §11) : déduplication par identifiant CloudEvents avant tout calcul, verrou du registre (index strictement
 * croissant et sans trou, attribué dans la transaction), forme canonique RFC 8785 de l'enveloppe, feuille
 * {@code SHA256(0x00 || canon)}, nœuds des sous-arbres complets achevés {@code SHA256(0x01 || g || d)} : la feuille
 * est chaînée à toutes les précédentes par l'arbre, et la racine de période la scellera. Aucune mise à jour, aucune
 * suppression.
 */
@Service
@Transactional
public class RegistreAuditService implements ScellerSignatureUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegistreAuditService.class);

    /** Acteur inscrit quand la charge ne nomme pas le signataire (anomalie conservée, jamais écartée). */
    static final String ACTEUR_INCONNU = "INCONNU";

    private final AuditEventRepository evenements;
    private final DeduplicationEntryRepository deduplication;
    private final MerkleNodeRepository noeuds;
    private final JsonCanonicalizer canonique;
    private final Clock horloge;

    public RegistreAuditService(
        AuditEventRepository evenements,
        DeduplicationEntryRepository deduplication,
        MerkleNodeRepository noeuds,
        JsonCanonicalizer canonique,
        Clock horloge
    ) {
        this.evenements = evenements;
        this.deduplication = deduplication;
        this.noeuds = noeuds;
        this.canonique = canonique;
        this.horloge = horloge;
    }

    @Override
    public Optional<Long> sceller(SignatureRecue s) {
        UUID eventId = identifiant(s.eventId());
        if (deduplication.existsByEventId(eventId)) {
            return Optional.empty();
        }
        // Un seul inscripteur à la fois : l'index suit l'ordre de réception, sans trou.
        evenements.verrouillerRegistre();
        if (deduplication.existsByEventId(eventId)) {
            return Optional.empty();
        }
        String canon = canonique.canonicalize(s.enveloppe());
        String feuille = ArbreMerkle.feuille(canon);
        Long dernier = evenements.dernierIndex();
        long index = dernier == null ? 0L : dernier + 1;
        Instant maintenant = horloge.instant().truncatedTo(ChronoUnit.MICROS);

        AuditEvent e = new AuditEvent();
        e.setLogIndex(index);
        e.setEventId(eventId);
        e.setOccurredAt(s.survenueLe() == null ? maintenant : s.survenueLe().truncatedTo(ChronoUnit.MICROS));
        e.setRecordedAt(maintenant);
        e.setActorId(borne(s.signataireId() == null || s.signataireId().isBlank() ? ACTEUR_INCONNU : s.signataireId().strip(), 100));
        e.setActionType(s.nature().action());
        e.setDomain(s.nature().domaine());
        e.setSubject(s.nature().type());
        e.setCorrelationId(uuidOuNull(s.correlationId()));
        e.setPayload(canon);
        e.setLeafHash(feuille);
        evenements.save(e);

        DeduplicationEntry d = new DeduplicationEntry();
        d.setEventId(eventId);
        d.setAuditIndex(index);
        d.setRecordedAt(maintenant);
        deduplication.save(d);

        for (ArbreMerkle.Noeud n : ArbreMerkle.noeudsAInscrire(index, feuille, this::empreinte)) {
            MerkleNode m = new MerkleNode();
            m.setLevel(n.niveau());
            m.setPosition(n.position());
            m.setHash(n.empreinte());
            noeuds.save(m);
        }
        LOG.info("Signature {} ({}) scellée au registre, index {}", s.eventId(), s.nature().action(), index);
        return Optional.of(index);
    }

    /** Racine de l'arbre des {@code taille} premières feuilles, recalculée depuis les nœuds inscrits. */
    @Transactional(readOnly = true)
    public String racine(long taille) {
        return ArbreMerkle.racine(taille, this::empreinte);
    }

    private String empreinte(int niveau, long position) {
        return noeuds.findFirstByLevelAndPosition(niveau, position).map(MerkleNode::getHash).orElse(null);
    }

    /** Identifiant CloudEvents : UUID tel quel, sinon UUID de nom (type 3) de sa forme UTF-8, stable. */
    static UUID identifiant(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static UUID uuidOuNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s.strip());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String borne(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
