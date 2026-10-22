package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.application.port.out.PiecesSandbox;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox.ReglePan;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Catalogue et pièces de la sandbox en mémoire, valeurs de départ du changeset (tests sans base). */
class PiecesSandboxMemoire implements PiecesSandbox {

    record Evenement(String type, String cle, String donnees, Instant le) {}

    final Map<String, TypePieceSandbox> types = new LinkedHashMap<>();
    final List<PieceSandbox> pieces = new ArrayList<>();
    final List<Evenement> evenements = new ArrayList<>();
    boolean modificationConcurrente;

    PiecesSandboxMemoire() {
        definir("SBX_BILAN_CERTIFICATION", Period.ofYears(10), true, Set.of("sbx-campagne"), ReglePan.QUARANTAINE);
        definir("SBX_RAPPORT_ECARTS_ISO20022", Period.ofYears(10), true, Set.of("sbx-iso20022"), ReglePan.QUARANTAINE);
        definir("SBX_TRACE_EXPORTEE", Period.ofYears(10), true, Set.of("sbx-trace"), ReglePan.QUARANTAINE);
        definir("SBX_FICHIER_REMIS_PARTICIPANT", Period.ofYears(5), false, Set.of("sbx-trace", "sbx-campagne"), ReglePan.REFUS);
    }

    void definir(String code, Period retention, boolean probante, Set<String> services, ReglePan regle) {
        types.put(code, new TypePieceSandbox(code, Map.of("fr", code, "en", code, "es", code), retention, probante, services, regle, 1024, true, 1));
    }

    @Override
    public List<TypePieceSandbox> types() {
        return List.copyOf(types.values());
    }

    @Override
    public Optional<TypePieceSandbox> type(String code) {
        return Optional.ofNullable(types.get(code));
    }

    @Override
    public boolean modifierRetention(TypePieceSandbox modifie, int versionLue, String auteur, String motif, Instant le) {
        if (modificationConcurrente || types.get(modifie.code()).version() != versionLue) {
            return false;
        }
        types.put(modifie.code(), modifie);
        return true;
    }

    @Override
    public Optional<PieceSandbox> scellee(String codeParticipant, String type, String sha256) {
        return pieces
            .stream()
            .filter(p -> p.etat() == PieceSandbox.Etat.SCELLEE && p.codeParticipant().equals(codeParticipant) && p.type().equals(type) && p.sha256().equals(sha256))
            .findFirst();
    }

    @Override
    public void inscrire(PieceSandbox piece) {
        pieces.add(piece);
    }

    @Override
    public List<PieceSandbox> quarantaines(int page, int taille) {
        List<PieceSandbox> q = pieces
            .stream()
            .filter(p -> p.etat() == PieceSandbox.Etat.QUARANTAINE)
            .sorted(Comparator.comparing(PieceSandbox::deposeeLe).reversed())
            .toList();
        return q.subList(Math.min(page * taille, q.size()), Math.min((page + 1) * taille, q.size()));
    }

    @Override
    public long nombreQuarantaines() {
        return pieces.stream().filter(p -> p.etat() == PieceSandbox.Etat.QUARANTAINE).count();
    }

    @Override
    public void publier(String type, String cle, String donneesJson, Instant le) {
        evenements.add(new Evenement(type, cle, donneesJson, le));
    }
}
