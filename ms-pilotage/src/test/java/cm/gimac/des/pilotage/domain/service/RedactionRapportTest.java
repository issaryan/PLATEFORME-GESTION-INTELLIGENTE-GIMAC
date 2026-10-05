package cm.gimac.des.pilotage.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.domain.activite.Periode;
import cm.gimac.des.pilotage.domain.activite.StatutActivite;
import cm.gimac.des.pilotage.domain.activite.TypeRapport;
import cm.gimac.des.pilotage.domain.service.RedactionRapport.Ligne;
import cm.gimac.des.pilotage.domain.service.RedactionRapport.TacheTerminee;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedactionRapportTest {

    private static final LocalDate MARDI = LocalDate.of(2026, 10, 6);
    private static final LocalDate MERCREDI = LocalDate.of(2026, 10, 7);

    @Test
    @DisplayName("Quotidien : titre, Réalisé avec durées, En cours, Reporté, Tâches terminées, Prévu demain")
    void quotidien() {
        List<Ligne> activites = List.of(
            new Ligne(MARDI, 2, "Revue du dossier L-12", StatutActivite.FAITE, 90, null),
            new Ligne(MARDI, 1, "Réunion d'équipe", StatutActivite.FAITE, 45, null),
            new Ligne(MARDI, 3, "Rédaction de la note", StatutActivite.EN_COURS, null, null),
            new Ligne(MARDI, 4, "Appel au participant", StatutActivite.A_FAIRE, null, null),
            new Ligne(MARDI, 5, "Visite du site", StatutActivite.REPORTEE, null, MERCREDI),
            new Ligne(MARDI, 6, "Tâche annulée", StatutActivite.ANNULEE, null, null)
        );
        String md = RedactionRapport.rediger(
            Periode.de(TypeRapport.QUOTIDIEN, "2026-10-06"),
            activites,
            List.of(new TacheTerminee(MARDI, "Valider le procès-verbal")),
            List.of(new Ligne(MERCREDI, 1, "Visite du site", StatutActivite.A_FAIRE, null, null)),
            MERCREDI
        );
        assertThat(md).startsWith("# Rapport quotidien du mardi 6 octobre 2026\n");
        assertThat(md).contains("## Réalisé\n\n- Réunion d'équipe (45 min)\n- Revue du dossier L-12 (1 h 30)\n");
        assertThat(md).contains("## En cours\n\n- Rédaction de la note\n- Appel au participant (non commencée)\n");
        assertThat(md).contains("## Reporté\n\n- Visite du site (reporté au mercredi 7 octobre 2026)\n");
        assertThat(md).contains("## Tâches terminées\n\n- Valider le procès-verbal\n");
        assertThat(md).contains("## Prévu demain (mercredi 7 octobre 2026)\n\n- Visite du site\n");
        assertThat(md).doesNotContain("Tâche annulée");
    }

    @Test
    @DisplayName("Rubriques vides : « Néant »")
    void vide() {
        String md = RedactionRapport.rediger(Periode.de(TypeRapport.QUOTIDIEN, "2026-10-09"), List.of(), List.of(), List.of(), LocalDate.of(2026, 10, 12));
        assertThat(md).contains("## Réalisé\n\n- Néant\n").contains("## Prévu demain (lundi 12 octobre 2026)\n\n- Néant\n");
    }

    @Test
    @DisplayName("Hebdomadaire : rubriques regroupées par jour (jours sans activité omis), total des heures, prévu au jour ouvré suivant")
    void hebdomadaire() {
        List<Ligne> activites = List.of(
            new Ligne(LocalDate.of(2026, 10, 5), 1, "Atelier", StatutActivite.FAITE, 120, null),
            new Ligne(MARDI, 1, "Revue", StatutActivite.FAITE, 90, null),
            new Ligne(MARDI, 2, "Note", StatutActivite.EN_COURS, 30, null)
        );
        String md = RedactionRapport.rediger(
            Periode.de(TypeRapport.HEBDOMADAIRE, "2026-W41"),
            activites,
            List.of(new TacheTerminee(LocalDate.of(2026, 10, 8), "Clore la campagne")),
            List.of(),
            LocalDate.of(2026, 10, 12)
        );
        assertThat(md).startsWith("# Rapport hebdomadaire, semaine 41 (du lundi 5 octobre 2026 au dimanche 11 octobre 2026)\n");
        assertThat(md).contains("**Total des heures : 3 h 30**");
        assertThat(md).contains("## Lundi 5 octobre 2026\n\n### Réalisé\n\n- Atelier (2 h)\n");
        assertThat(md).contains("## Mardi 6 octobre 2026\n\n### Réalisé\n\n- Revue (1 h 30)\n");
        assertThat(md).contains("## Jeudi 8 octobre 2026\n\n### Réalisé\n\n- Néant\n");
        assertThat(md).contains("### Tâches terminées\n\n- Clore la campagne\n");
        assertThat(md).doesNotContain("Mercredi 7 octobre");
        assertThat(md).contains("## Prévu (lundi 12 octobre 2026)\n\n- Néant\n");
    }

    @Test
    @DisplayName("Durées : minutes, heures, heures et minutes")
    void durees() {
        assertThat(RedactionRapport.duree(45)).isEqualTo("45 min");
        assertThat(RedactionRapport.duree(120)).isEqualTo("2 h");
        assertThat(RedactionRapport.duree(95)).isEqualTo("1 h 35");
        assertThat(RedactionRapport.duree(65)).isEqualTo("1 h 05");
    }
}
