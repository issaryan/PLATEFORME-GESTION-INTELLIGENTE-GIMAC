package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.application.port.out.sandbox.ActiviteComptes;
import cm.gimac.des.iam.application.port.out.sandbox.PerimetresSuspendus;
import cm.gimac.des.iam.application.port.out.sandbox.ProtectionsAdministrateur;
import cm.gimac.des.iam.domain.sandbox.PerimetreSuspendu;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Adaptateurs en mémoire des ports de la sandbox (D-119), pour les tests unitaires sans Docker. */
public final class Memoires {

    private Memoires() {}

    /** Horloge réglable. */
    public static final class Horloge extends Clock {

        public final AtomicReference<Instant> maintenant;

        public Horloge(Instant debut) {
            maintenant = new AtomicReference<>(debut);
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
            return maintenant.get();
        }

        public void avancer(java.time.Duration d) {
            maintenant.set(maintenant.get().plus(d));
        }
    }

    public static final class Perimetres implements PerimetresSuspendus {

        public final Map<String, PerimetreSuspendu> lignes = new LinkedHashMap<>();

        @Override
        public Optional<PerimetreSuspendu> lire(String code) {
            return Optional.ofNullable(lignes.get(code));
        }

        @Override
        public boolean poser(String code, String acteurId, String motif, Instant le) {
            return lignes.putIfAbsent(code, new PerimetreSuspendu(code, le, acteurId, motif)) == null;
        }

        @Override
        public boolean lever(String code) {
            return lignes.remove(code) != null;
        }

        @Override
        public List<PerimetreSuspendu> lister(int page, int taille) {
            return lignes.values().stream().sorted(Comparator.comparing(PerimetreSuspendu::depuis).reversed()).skip((long) page * taille).limit(taille).toList();
        }

        @Override
        public long compter() {
            return lignes.size();
        }
    }

    public static final class Protections implements ProtectionsAdministrateur {

        public final Map<String, Protection> lignes = new LinkedHashMap<>();

        @Override
        public boolean protege(String compteId) {
            return lignes.containsKey(compteId);
        }

        @Override
        public boolean definir(String compteId, String institutionId, boolean protege, String acteurId, Instant le) {
            if (!protege) {
                return lignes.remove(compteId) != null;
            }
            return lignes.putIfAbsent(compteId, new Protection(compteId, institutionId, le, acteurId)) == null;
        }

        @Override
        public List<Protection> lister(int page, int taille) {
            return lignes.values().stream().skip((long) page * taille).limit(taille).toList();
        }

        @Override
        public long compter() {
            return lignes.size();
        }
    }

    public static final class Activite implements ActiviteComptes {

        public final Map<String, Instant> lignes = new HashMap<>();

        @Override
        public void connexion(String compteId, Instant le) {
            lignes.merge(compteId, le, (a, b) -> a.isAfter(b) ? a : b);
        }

        @Override
        public Map<String, Instant> dernieres() {
            return Map.copyOf(lignes);
        }
    }

    /** Étapes de saga en mémoire, réservation comprise (méthode par défaut du port). */
    public static final class Etapes implements EtapesIdempotentes {

        public final Map<String, String> lignes = new LinkedHashMap<>();

        @Override
        public Optional<String> resultat(String cle, String operation) {
            return Optional.ofNullable(lignes.get(cle + "/" + operation));
        }

        @Override
        public void enregistrer(String cle, String operation, String resultat, String acteur) {
            lignes.put(cle + "/" + operation, resultat);
        }
    }
}
