package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Resultat;
import cm.gimac.des.document.application.port.in.DepotFragmenteUseCase.Etat;
import cm.gimac.des.document.application.port.in.LireDocumentUseCase.Metadonnees;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Représentations JSON des routes /api/v1 de MS-DOCUMENT. */
public final class DocumentVues {

    private DocumentVues() {}

    /** Réponse du dépôt : identifiant, empreinte SHA-256 (hexadécimal minuscule), nom, taille en octets, type MIME. */
    public record DocumentDeposeVue(UUID id, String sha256, String nomFichier, long taille, String typeMime) {
        static DocumentDeposeVue de(Resultat r) {
            return new DocumentDeposeVue(r.id(), r.sha256(), r.nomFichier(), r.taille(), r.typeMime());
        }
    }

    /** Réponse de l'interface interne : empreinte seule. */
    public record EmpreinteVue(String sha256) {}

    /** Ouverture d'un dépôt fragmenté (docs/lot1/depots-fragmentes.md §3). */
    public record OuvertureDepotRequete(String nomFichier, String typeMime, Long taille, String finalite, String reference) {}

    /**
     * Dépôt fragmenté (« Depot » du contrat) ; les champs propres à un état (documentId, sha256,
     * motif, expireLe) sont omis dans les autres.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DepotVue(
        UUID id,
        String etat,
        String nomFichier,
        String typeMime,
        long taille,
        long taillePartie,
        int nombreParties,
        List<Integer> partiesRecues,
        UUID documentId,
        String sha256,
        boolean dedoublonne,
        String motif,
        Instant expireLe
    ) {
        static DepotVue de(Etat e) {
            return new DepotVue(
                e.id(),
                e.etat().name(),
                e.nomFichier(),
                e.typeMime(),
                e.taille(),
                e.taillePartie(),
                e.nombreParties(),
                e.partiesRecues(),
                e.documentId(),
                e.sha256(),
                e.dedoublonne(),
                e.motif(),
                e.expireLe()
            );
        }
    }

    /** Métadonnées d'un document pour l'interface interne (docs/lot1/depots-fragmentes.md §4). */
    public record MetadonneesVue(
        UUID id,
        String nomFichier,
        long taille,
        String typeMime,
        String sha256,
        String finalite,
        String reference,
        String deposant,
        String institutionId,
        Instant deposeLe
    ) {
        static MetadonneesVue de(Metadonnees m) {
            return new MetadonneesVue(
                m.id(),
                m.nomFichier(),
                m.taille(),
                m.typeMime(),
                m.sha256(),
                m.finalite(),
                m.reference(),
                m.deposant(),
                m.institutionId(),
                m.deposeLe()
            );
        }
    }
}
