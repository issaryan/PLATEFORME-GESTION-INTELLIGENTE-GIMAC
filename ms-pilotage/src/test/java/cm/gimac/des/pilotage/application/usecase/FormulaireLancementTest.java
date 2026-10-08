package cm.gimac.des.pilotage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.application.port.out.DocumentPort.Metadonnees;
import cm.gimac.des.pilotage.application.usecase.FormulaireLancement.ErreurChamp;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Formulaire;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Formulaire de lancement (D-98 §2) : valeurs normalisées par type, erreurs rendues ensemble champ par champ. */
class FormulaireLancementTest {

    private static final UUID PROCESSUS = UUID.randomUUID();
    private static final String MOI = "initiateur";
    private static final String SHA = "a".repeat(64);

    private static ChampFormulaire champ(String code, TypeChampFormulaire type, boolean obligatoire, List<String> choix, List<String> types) {
        return new ChampFormulaire(code, code.toLowerCase(), type, choix, obligatoire, null, types);
    }

    private static final Formulaire FORMULAIRE = new Formulaire(
        List.of(
            champ("OBJET", TypeChampFormulaire.TEXTE, true, null, null),
            champ("DETAIL", TypeChampFormulaire.TEXTE_LONG, false, null, null),
            champ("QUANTITE", TypeChampFormulaire.NOMBRE, false, null, null),
            champ("JOUR", TypeChampFormulaire.DATE, false, null, null),
            champ("PERIODE", TypeChampFormulaire.PERIODE, true, null, null),
            champ("TYPE", TypeChampFormulaire.LISTE, false, List.of("Annuel", "Exceptionnel"), null),
            champ("ATTESTE", TypeChampFormulaire.CASE, true, null, null),
            champ("PIECE", TypeChampFormulaire.PIECE_JOINTE, false, null, List.of("application/pdf", "image/*"))
        )
    );

    private static Function<UUID, Optional<Metadonnees>> documents(Map<UUID, Metadonnees> m) {
        return id -> Optional.ofNullable(m.get(id));
    }

    private static Metadonnees doc(UUID id, String type, String deposant, String reference) {
        return new Metadonnees(id, "piece.pdf", 1200, type, SHA, "PROCESSUS_UNITE", reference, deposant);
    }

    @Test
    @DisplayName("Valeurs valides : texte, nombre, date, période, liste, case, pièce jointe déposée par l'initiateur")
    void valides() {
        UUID d = UUID.randomUUID();
        Map<String, Object> saisies = new HashMap<>();
        saisies.put("OBJET", "  Poste du deuxième étage ");
        saisies.put("QUANTITE", "12,50");
        saisies.put("JOUR", "2026-10-15");
        saisies.put("PERIODE", Map.of("debut", "2026-10-20", "fin", "2026-10-24"));
        saisies.put("TYPE", "Annuel");
        saisies.put("ATTESTE", true);
        saisies.put("PIECE", Map.of("documentId", d.toString()));
        saisies.put("DETAIL", "");
        Map<String, Object> v = FormulaireLancement.valeurs(FORMULAIRE, saisies, PROCESSUS, MOI, documents(Map.of(d, doc(d, "application/pdf", MOI, PROCESSUS.toString()))));
        assertThat(v).containsEntry("OBJET", "Poste du deuxième étage").containsEntry("QUANTITE", "12.5").containsEntry("JOUR", "2026-10-15");
        assertThat(v).containsEntry("PERIODE", Map.of("debut", "2026-10-20", "fin", "2026-10-24")).containsEntry("TYPE", "Annuel").containsEntry("ATTESTE", true);
        assertThat(v).doesNotContainKey("DETAIL");
        @SuppressWarnings("unchecked")
        Map<String, Object> piece = (Map<String, Object>) v.get("PIECE");
        assertThat(piece).containsEntry("documentId", d.toString()).containsEntry("sha256", SHA).containsEntry("typeMime", "application/pdf").containsEntry("nomFichier", "piece.pdf");
        assertThat(v.keySet()).containsExactly("OBJET", "QUANTITE", "JOUR", "PERIODE", "TYPE", "ATTESTE", "PIECE");
    }

    @Test
    @DisplayName("Toutes les erreurs ensemble : 422 PIL_PROCESSUS_FORMULAIRE_INVALIDE et fieldErrors formulaire.CODE")
    void erreurs() {
        UUID autre = UUID.randomUUID();
        UUID image = UUID.randomUUID();
        Map<String, Object> saisies = new HashMap<>();
        saisies.put("DETAIL", "x".repeat(4001));
        saisies.put("QUANTITE", "douze");
        saisies.put("JOUR", "15/10/2026");
        saisies.put("PERIODE", Map.of("debut", "2026-10-24", "fin", "2026-10-20"));
        saisies.put("TYPE", "Maladie");
        saisies.put("ATTESTE", false);
        saisies.put("PIECE", autre.toString());
        saisies.put("INCONNU", "?");
        Function<UUID, Optional<Metadonnees>> docs = documents(Map.of(autre, doc(autre, "application/pdf", "quelqu-un-d-autre", null), image, doc(image, "text/plain", MOI, null)));
        assertThatThrownBy(() -> FormulaireLancement.valeurs(FORMULAIRE, saisies, PROCESSUS, MOI, docs)).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.status()).isEqualTo(422);
            assertThat(e.code()).isEqualTo("PIL_PROCESSUS_FORMULAIRE_INVALIDE");
            @SuppressWarnings("unchecked")
            List<ErreurChamp> l = (List<ErreurChamp>) e.details().get("fieldErrors");
            Map<String, String> parChamp = new HashMap<>();
            l.forEach(x -> parChamp.put(x.field(), x.code()));
            assertThat(parChamp)
                .containsEntry("formulaire.INCONNU", "INCONNU")
                .containsEntry("formulaire.OBJET", "OBLIGATOIRE")
                .containsEntry("formulaire.DETAIL", "TROP_LONG")
                .containsEntry("formulaire.QUANTITE", "FORMAT")
                .containsEntry("formulaire.JOUR", "FORMAT")
                .containsEntry("formulaire.PERIODE", "PERIODE")
                .containsEntry("formulaire.TYPE", "HORS_LISTE")
                .containsEntry("formulaire.ATTESTE", "OBLIGATOIRE")
                .containsEntry("formulaire.PIECE", "PIECE_INVALIDE");
            assertThat(l).allSatisfy(x -> assertThat(x.objectName()).isEqualTo("lancement"));
        });
        // Type de pièce hors de ceux admis ; pièce d'une autre référence que la définition lancée.
        Map<String, Object> s2 = new HashMap<>(Map.of("OBJET", "a", "PERIODE", Map.of("debut", "2026-10-20", "fin", "2026-10-20"), "ATTESTE", "true", "PIECE", image.toString()));
        assertThatThrownBy(() -> FormulaireLancement.valeurs(FORMULAIRE, s2, PROCESSUS, MOI, docs)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.details().get("fieldErrors").toString()).contains("TYPE_PIECE")
        );
        UUID ailleurs = UUID.randomUUID();
        s2.put("PIECE", Map.of("documentId", ailleurs.toString()));
        Function<UUID, Optional<Metadonnees>> docs2 = documents(Map.of(ailleurs, doc(ailleurs, "image/png", MOI, UUID.randomUUID().toString())));
        assertThatThrownBy(() -> FormulaireLancement.valeurs(FORMULAIRE, s2, PROCESSUS, MOI, docs2)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.details().get("fieldErrors").toString()).contains("PIECE_INVALIDE")
        );
    }

    @Test
    @DisplayName("Formulaire vide : aucune valeur, aucun champ admis")
    void vide() {
        assertThat(FormulaireLancement.valeurs(Formulaire.vide(), null, PROCESSUS, MOI, id -> Optional.empty())).isEmpty();
        assertThatThrownBy(() -> FormulaireLancement.valeurs(Formulaire.vide(), Map.of("X", "y"), PROCESSUS, MOI, id -> Optional.empty())).isInstanceOf(PilotageException.class);
    }
}
