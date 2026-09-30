package cm.gimac.des.collaboration.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.Etiquette;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Règles de la messagerie de travail (D-64, docs/lot1/messagerie.md §1). */
class ReglesMessagerieTest {

    final ReglesMessagerie regles = new ReglesMessagerie();
    static final UUID LIAISON = UUID.randomUUID();

    static void refuse(ThrowingCallable appel, String code) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(CollaborationException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(400);
        });
    }

    static FichierJoint fichier(String nom, String type, long taille) {
        return new FichierJoint(nom, type, taille, () -> new ByteArrayInputStream(new byte[0]));
    }

    @Nested
    class Contenu {

        @Test
        @DisplayName("Espaces retirés aux bords ; 1 à 4 000 caractères ; vide admis avec une pièce jointe")
        void contenu() {
            assertThat(regles.contenu("  Bonjour  ", 0)).isEqualTo("Bonjour");
            assertThat(regles.contenu("x".repeat(4000), 0)).hasSize(4000);
            assertThat(regles.contenu("   ", 1)).isEmpty();
            assertThat(regles.contenu(null, 2)).isEmpty();
            refuse(() -> regles.contenu("  ", 0), "COL_MESSAGE_EMPTY");
            refuse(() -> regles.contenu(null, 0), "COL_MESSAGE_EMPTY");
            refuse(() -> regles.contenu("x".repeat(4001), 0), "COL_MESSAGE_TOO_LONG");
        }

        @Test
        @DisplayName("Titre facultatif borné, extrait de 120 caractères")
        void titreEtExtrait() {
            assertThat(regles.titre(null)).isNull();
            assertThat(regles.titre("  ")).isNull();
            assertThat(regles.titre("  Recette EP A ")).isEqualTo("Recette EP A");
            assertThat(regles.titre("t".repeat(250))).hasSize(200);
            assertThat(regles.extrait(null)).isEmpty();
            assertThat(regles.extrait("court")).isEqualTo("court");
            assertThat(regles.extrait("e".repeat(300))).hasSize(120);
        }
    }

    @Nested
    class Destinataires {

        @Test
        @DisplayName("Distincts, sans blanc ; 1 à 50 ; jamais l'expéditeur")
        void destinataires() {
            assertThat(regles.destinataires(Arrays.asList(" b ", "c", "b", null, " "), "a")).containsExactly("b", "c");
            refuse(() -> regles.destinataires(null, "a"), "COL_RECIPIENT_REQUIRED");
            refuse(() -> regles.destinataires(List.of(" "), "a"), "COL_RECIPIENT_REQUIRED");
            refuse(() -> regles.destinataires(List.of("b", "a"), "a"), "COL_RECIPIENT_SELF");
            List<String> cinquanteEtUn = IntStream.range(0, 51).mapToObj(i -> "d" + i).toList();
            refuse(() -> regles.destinataires(cinquanteEtUn, "a"), "COL_TOO_MANY_RECIPIENTS");
            assertThat(regles.destinataires(cinquanteEtUn.subList(0, 50), "a")).hasSize(50);
        }

        @Test
        @DisplayName("Conversation directe : un destinataire, sans titre ni liaison ; clé triée")
        void directe() {
            assertThat(regles.estDirecte(List.of("b"), null, null)).isTrue();
            assertThat(regles.estDirecte(List.of("b", "c"), null, null)).isFalse();
            assertThat(regles.estDirecte(List.of("b"), "Titre", null)).isFalse();
            assertThat(regles.estDirecte(List.of("b"), null, LIAISON)).isFalse();
            assertThat(regles.cleDirecte("b", "a")).isEqualTo("a|b").isEqualTo(regles.cleDirecte("a", "b"));
        }

        @Test
        @DisplayName("Joignable : compte interne (sans institution) et actif")
        void joignable() {
            assertThat(new Collaborateur("1", "A", null, null, null, true).joignable()).isTrue();
            assertThat(new Collaborateur("1", "A", null, Set.of(), " ", true).joignable()).isTrue();
            assertThat(new Collaborateur("1", "A", null, Set.of(), "inst", true).joignable()).isFalse();
            assertThat(new Collaborateur("1", "A", null, Set.of(), null, false).joignable()).isFalse();
        }
    }

    @Nested
    class Etiquettes {

        @Test
        @DisplayName("Au plus 10, référence LIA-AAAA-NNN, doublons retirés")
        void etiquettes() {
            assertThat(regles.etiquettes(null)).isEmpty();
            assertThat(regles.etiquettes(List.of())).isEmpty();
            assertThat(regles.etiquettes(List.of(new Etiquette(LIAISON, " LIA-2026-027 "), new Etiquette(LIAISON, "LIA-2026-027")))).containsExactly(
                new Etiquette(LIAISON, "LIA-2026-027")
            );
            List<Etiquette> onze = new ArrayList<>(Collections.nCopies(11, new Etiquette(LIAISON, "LIA-2026-001")));
            refuse(() -> regles.etiquettes(onze), "COL_TOO_MANY_TAGS");
            refuse(() -> regles.etiquettes(List.of(new Etiquette(LIAISON, "LIA-26-027"))), "COL_TAG_INVALID");
            refuse(() -> regles.etiquettes(List.of(new Etiquette(null, "LIA-2026-027"))), "COL_TAG_INVALID");
            refuse(() -> regles.etiquettes(List.of(new Etiquette(LIAISON, null))), "COL_TAG_INVALID");
            refuse(() -> regles.etiquettes(Arrays.asList(new Etiquette[] { null })), "COL_TAG_INVALID");
            assertThat(regles.etiquettes(List.of(new Etiquette(LIAISON, "LIA-2026-123456")))).hasSize(1);
        }
    }

    @Nested
    class Pieces {

        @ParameterizedTest(name = "{0} ({1})")
        @CsvSource(
            {
                "rapport.pdf,application/pdf,application/pdf",
                "capture.PNG,image/png,image/png",
                "photo.jpg,image/jpeg,image/jpeg",
                "photo.jpeg,application/octet-stream,image/jpeg",
                "notes.txt,text/plain;charset=UTF-8,text/plain",
                "export.csv,application/vnd.ms-excel,text/csv",
                "pv.docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "suivi.xlsx,,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "ancien.doc,application/msword,application/msword",
                "ancien.xls,application/vnd.ms-excel,application/vnd.ms-excel",
                "deck.ppt,application/vnd.ms-powerpoint,application/vnd.ms-powerpoint",
                "deck.pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation,application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "lot.zip,application/x-zip-compressed,application/zip",
            }
        )
        @DisplayName("Formats admis : type rendu canonique")
        void admis(String nom, String type, String attendu) {
            assertThat(regles.pieces(List.of(fichier(nom, type, 10))).get(0).typeMime()).isEqualTo(attendu);
        }

        @Test
        @DisplayName("Au plus 5 pièces de 20 Mo ; formats et contenus vides refusés ; nom réduit à son dernier segment")
        void refus() {
            assertThat(regles.pieces(null)).isEmpty();
            assertThat(regles.pieces(List.of())).isEmpty();
            assertThat(regles.pieces(List.of(fichier("C:\\temp\\dossier/pv.pdf", "application/pdf", 1))).get(0).nom()).isEqualTo("pv.pdf");
            refuse(() -> regles.pieces(List.of(fichier(null, "application/pdf", 1))), "COL_ATTACHMENT_TYPE");
            refuse(() -> regles.pieces(List.of(fichier(" ", "application/pdf", 1))), "COL_ATTACHMENT_TYPE");
            refuse(() -> regles.pieces(Collections.nCopies(6, fichier("a.pdf", "application/pdf", 1))), "COL_TOO_MANY_ATTACHMENTS");
            assertThat(regles.pieces(Collections.nCopies(5, fichier("a.pdf", "application/pdf", 1)))).hasSize(5);
            refuse(() -> regles.pieces(List.of(fichier("gros.pdf", "application/pdf", ReglesMessagerie.PIECE_TAILLE_MAX + 1))), "COL_ATTACHMENT_TOO_LARGE");
            assertThat(regles.pieces(List.of(fichier("juste.pdf", "application/pdf", ReglesMessagerie.PIECE_TAILLE_MAX)))).hasSize(1);
            refuse(() -> regles.pieces(List.of(fichier("script.exe", "application/octet-stream", 1))), "COL_ATTACHMENT_TYPE");
            refuse(() -> regles.pieces(List.of(fichier("sans-extension", "application/pdf", 1))), "COL_ATTACHMENT_TYPE");
            refuse(() -> regles.pieces(List.of(fichier("faux.pdf", "text/html", 1))), "COL_ATTACHMENT_TYPE");
            refuse(() -> regles.pieces(List.of(fichier("vide.pdf", "application/pdf", 0))), "COL_ATTACHMENT_TYPE");
        }
    }
}
