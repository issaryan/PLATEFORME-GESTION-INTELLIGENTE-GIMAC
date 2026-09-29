package cm.gimac.des.document.web.rest.v1;

import cm.gimac.des.document.application.port.in.DeposerDocumentUseCase.Resultat;
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
}
