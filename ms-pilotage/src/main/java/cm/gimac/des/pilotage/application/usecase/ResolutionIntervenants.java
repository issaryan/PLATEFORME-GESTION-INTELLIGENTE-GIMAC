package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.MembreProfil;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.UniteMembres;
import cm.gimac.des.pilotage.domain.processus.Affectation;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Résolution d'un intervenant à l'activation d'une étape (§2), avec l'organisation de MS-IAM : PROFIL → à prendre
 * par les membres actifs de l'unité portant le profil ; PERSONNE, AU_LANCEMENT (désignation), CHEF_UNITE
 * (responsable de l'unité), LANCEUR, N_PLUS_1_LANCEUR → cette personne si elle est un compte interne actif ;
 * sinon (personne partie, désactivée, aucun chef, aucun n+1) → à prendre par les porteurs du profil de
 * remplacement (à défaut, tout membre de l'unité). L'unité est celle de l'intervenant, à défaut l'unité
 * propriétaire, sous-unités comprises.
 */
@Component
class ResolutionIntervenants {

    /**
     * Affectation résolue ; {@code parti} : personne prévue mais partie ou inactive (null sinon) ; {@code note} :
     * explication pour l'historique (null si la résolution est nominale).
     */
    record Resolution(Affectation affectation, String parti, String note) {
        boolean nominative() {
            return affectation.personneId() != null;
        }
    }

    private final Equipes equipes;

    ResolutionIntervenants(Equipes equipes) {
        this.equipes = equipes;
    }

    Resolution resoudre(Intervenant i, String uniteProcessus, String lanceurId, String nPlus1, Map<String, String> designations, String cle) {
        String unite = i.uniteOu(uniteProcessus);
        return switch (i.mode()) {
            case PROFIL -> new Resolution(Affectation.profil(i.profil(), unite), null, null);
            case PERSONNE -> personne(i, unite, i.personneId(), "Personne nommée");
            case AU_LANCEMENT -> personne(i, unite, designations.get(cle), "Personne désignée au lancement");
            case CHEF_UNITE -> personne(i, unite, chef(unite), "Chef de l'unité " + unite);
            case LANCEUR -> personne(i, unite, lanceurId, "Lanceur");
            case N_PLUS_1_LANCEUR -> personne(i, unite, nPlus1, "N+1 du lanceur");
        };
    }

    private Resolution personne(Intervenant i, String unite, String id, String qui) {
        if (id != null && equipes.interneActif(id)) {
            return new Resolution(Affectation.personne(id, unite), null, null);
        }
        Affectation remplacement = Affectation.profil(i.remplacement(), unite);
        String par = i.remplacement() == null ? "tout membre de l'unité " + unite : "le profil " + i.remplacement() + " (unité " + unite + ")";
        if (id == null) {
            return new Resolution(remplacement, null, qui + " absent : étape à prendre par " + par);
        }
        return new Resolution(remplacement, id, qui + " partie ou inactive : remplacement par " + par);
    }

    /** Responsable (chef) de l'unité, ou null. */
    String chef(String unite) {
        return equipes.membresUnite(unite, null).map(UniteMembres::responsableId).orElse(null);
    }

    /** Membres actifs de l'unité portant le profil (null : tous), sauf {@code sauf}. */
    List<String> porteurs(String unite, String profil, String sauf) {
        return equipes
            .membresUnite(unite, profil)
            .map(UniteMembres::actifs)
            .orElse(List.of())
            .stream()
            .map(MembreProfil::id)
            .filter(id -> !id.equals(sauf))
            .toList();
    }

    /** Destinataires d'une affectation : la personne, ou les porteurs du profil, à défaut le chef de l'unité. */
    List<String> destinataires(Affectation a, String sauf) {
        if (a == null) {
            return List.of();
        }
        if (a.personneId() != null) {
            return Objects.equals(a.personneId(), sauf) ? List.of() : List.of(a.personneId());
        }
        List<String> p = porteurs(a.unite(), a.profil(), sauf);
        if (!p.isEmpty()) {
            return p;
        }
        String chef = chef(a.unite());
        return chef == null || chef.equals(sauf) ? List.of() : List.of(chef);
    }

    /** Le compte répond à l'affectation : personne nommée, ou membre actif de l'unité portant le profil. */
    boolean affecte(String compteId, Affectation a) {
        if (a == null || compteId == null) {
            return false;
        }
        if (a.personneId() != null) {
            return a.personneId().equals(compteId);
        }
        return equipes.porteProfilDans(compteId, a.unite(), a.profil());
    }

    /**
     * Clé d'une désignation au lancement : CODE_ETAPE (exécutant), CODE_ETAPE.validateur,
     * CODE_ETAPE.signature.CODE (signataire).
     */
    static String cle(String etape, String element) {
        return ControleProcessus.INTERVENANT.equals(element) ? etape : etape + "." + element;
    }
}
