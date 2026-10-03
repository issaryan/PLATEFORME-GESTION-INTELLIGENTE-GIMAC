package cm.gimac.des.document.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.domain.DepotDocument;
import cm.gimac.des.document.domain.DepotFragmente;
import cm.gimac.des.document.domain.enumeration.EtatDepotFragmente;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import cm.gimac.des.document.repository.DepotFragmenteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base de données simulée pour les cas d'utilisation des dépôts fragmentés : dépôts et documents
 * en mémoire derrière les interfaces des dépôts Spring Data (Mockito). Une entité lue est l'entité
 * conservée, comme dans un contexte de persistance (les modifications faites dans une transaction
 * sont gardées) ; {@code saveAndFlush} contrôle et incrémente la version (verrouillage optimiste).
 * Gestionnaire de transactions simulé et horloge réglable.
 */
final class BaseEnMemoire {

    /** Horloge réglable, en UTC. */
    static final class Horloge extends Clock {

        private Instant maintenant;

        Horloge(Instant depart) {
            this.maintenant = depart;
        }

        void avancer(Duration d) {
            maintenant = maintenant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant;
        }
    }

    final Map<UUID, DepotFragmente> depots = new ConcurrentHashMap<>();
    final Map<UUID, Long> versions = new ConcurrentHashMap<>();
    final List<DepotDocument> documents = new ArrayList<>();
    final DepotFragmenteRepository depotRepository = mock(DepotFragmenteRepository.class);
    final DepotDocumentRepository documentRepository = mock(DepotDocumentRepository.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);

    BaseEnMemoire() {
        when(depotRepository.save(any(DepotFragmente.class))).thenAnswer(i -> enregistrer(i.getArgument(0)));
        when(depotRepository.saveAndFlush(any(DepotFragmente.class))).thenAnswer(i -> enregistrer(i.getArgument(0)));
        when(depotRepository.findById(any(UUID.class))).thenAnswer(i -> Optional.ofNullable(depots.get((UUID) i.getArgument(0))));
        when(depotRepository.verrouiller(any(UUID.class))).thenAnswer(i -> Optional.ofNullable(depots.get((UUID) i.getArgument(0))));
        when(depotRepository.findByIdAndDeposant(any(UUID.class), anyString())).thenAnswer(i -> du(i.getArgument(0), i.getArgument(1)));
        when(depotRepository.verrouiller(any(UUID.class), anyString())).thenAnswer(i -> du(i.getArgument(0), i.getArgument(1)));
        when(depotRepository.aVerifier(any(), any(), any())).thenAnswer(i -> {
            EtatDepotFragmente etat = i.getArgument(0);
            Instant maintenant = i.getArgument(1);
            Pageable page = i.getArgument(2);
            return depots
                .values()
                .stream()
                .filter(d -> d.getEtat() == etat && (d.getRepriseLe() == null || !d.getRepriseLe().isAfter(maintenant)))
                .sorted(Comparator.comparing(DepotFragmente::getActifLe))
                .limit(page.getPageSize())
                .map(DepotFragmente::getId)
                .toList();
        });
        when(depotRepository.inactifs(any(), any(), any())).thenAnswer(i -> {
            EtatDepotFragmente etat = i.getArgument(0);
            Instant seuil = i.getArgument(1);
            return depots.values().stream().filter(d -> d.getEtat() == etat && d.getActifLe().isBefore(seuil)).map(DepotFragmente::getId).toList();
        });
        when(documentRepository.saveAndFlush(any(DepotDocument.class))).thenAnswer(i -> {
            DepotDocument d = i.getArgument(0);
            synchronized (documents) {
                documents.add(d);
            }
            return d;
        });
        when(documentRepository.findFirstBySha256AndTailleOrderByDeposeLeAsc(anyString(), anyLong())).thenAnswer(i -> {
            synchronized (documents) {
                return documents
                    .stream()
                    .filter(d -> d.getSha256().equals(i.getArgument(0)) && d.getTaille() == (long) i.getArgument(1))
                    .min(Comparator.comparing(DepotDocument::getDeposeLe))
                    .map(d -> d);
            }
        });
    }

    private Optional<DepotFragmente> du(UUID id, String deposant) {
        return Optional.ofNullable(depots.get(id)).filter(d -> d.getDeposant().equals(deposant));
    }

    private DepotFragmente enregistrer(DepotFragmente d) {
        Long inscrite = versions.get(d.getId());
        if (!Objects.equals(inscrite, d.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(DepotFragmente.class, d.getId());
        }
        long suivante = inscrite == null ? 0 : inscrite + 1;
        d.version(suivante);
        versions.put(d.getId(), suivante);
        depots.put(d.getId(), d);
        return d;
    }

    /** Modification par une autre instance : la version inscrite change. */
    void modifierAilleurs(UUID id) {
        DepotFragmente d = depots.get(id);
        long suivante = versions.get(id) + 1;
        versions.put(id, suivante);
        d.version(suivante);
    }

    DepotFragmente depot(UUID id) {
        return depots.get(id);
    }
}
