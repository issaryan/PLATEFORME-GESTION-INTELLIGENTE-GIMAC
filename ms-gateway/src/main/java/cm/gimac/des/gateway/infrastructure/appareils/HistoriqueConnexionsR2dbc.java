package cm.gimac.des.gateway.infrastructure.appareils;

import cm.gimac.des.gateway.domain.appareils.Appareil;
import cm.gimac.des.gateway.domain.appareils.ResultatTentative;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import cm.gimac.des.gateway.domain.appareils.TypeAppareil;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import io.r2dbc.spi.Readable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Historique des connexions dans la base PostgreSQL de la passerelle (table historique_connexion,
 * Liquibase), lu et écrit par R2DBC comme le reste de la passerelle. Le compte est retrouvé par son
 * sujet ou par l'empreinte d'un de ses identifiants (tentatives dont le mot de passe a été refusé).
 */
public class HistoriqueConnexionsR2dbc implements HistoriqueConnexions {

    private static final String CONDITION = "(sujet = :sujet or empreinte_identifiant in (:empreintes))";

    private final DatabaseClient base;

    public HistoriqueConnexionsR2dbc(DatabaseClient base) {
        this.base = base;
    }

    @Override
    public Mono<Void> enregistrer(TentativeConnexion t) {
        DatabaseClient.GenericExecuteSpec s = base
            .sql(
                "insert into historique_connexion (id, le, sujet, empreinte_identifiant, resultat, navigateur, systeme, type_appareil, adresse_ip, couche) " +
                "values (:id, :le, :sujet, :empreinte, :resultat, :navigateur, :systeme, :type, :ip, :couche)"
            )
            .bind("id", UUID.randomUUID())
            .bind("le", OffsetDateTime.ofInstant(t.le(), ZoneOffset.UTC))
            .bind("resultat", t.resultat().name())
            .bind("navigateur", t.appareil().navigateur())
            .bind("systeme", t.appareil().systeme())
            .bind("type", t.appareil().type().name());
        s = lier(s, "sujet", t.sujet());
        s = lier(s, "empreinte", t.empreinteIdentifiant());
        s = lier(s, "ip", t.adresseIp());
        s = lier(s, "couche", t.couche());
        return s.then();
    }

    @Override
    public Mono<Page> lire(String sujet, Collection<String> empreintes, int page, int taille) {
        List<String> e = empreintes == null || empreintes.isEmpty() ? List.of("-") : List.copyOf(empreintes);
        String s = sujet == null ? "-" : sujet;
        Mono<Long> total = base
            .sql("select count(*) as n from historique_connexion where " + CONDITION)
            .bind("sujet", s)
            .bind("empreintes", e)
            .map(r -> ((Number) r.get("n")).longValue())
            .one();
        Mono<List<TentativeConnexion>> contenu = base
            .sql("select * from historique_connexion where " + CONDITION + " order by le desc, id desc limit :limite offset :decalage")
            .bind("sujet", s)
            .bind("empreintes", e)
            .bind("limite", taille)
            .bind("decalage", (long) page * taille)
            .map(HistoriqueConnexionsR2dbc::tentative)
            .all()
            .collectList();
        return Mono.zip(contenu, total).map(z -> new Page(z.getT1(), z.getT2()));
    }

    @Override
    public Flux<Appareil> appareilsConnus(String sujet) {
        return base
            .sql("select distinct navigateur, systeme, type_appareil from historique_connexion where sujet = :sujet and resultat = 'REUSSIE'")
            .bind("sujet", sujet)
            .map(r -> new Appareil(r.get("navigateur", String.class), r.get("systeme", String.class), type(r.get("type_appareil", String.class))))
            .all();
    }

    @Override
    public Mono<Long> purger(Instant avant) {
        return base.sql("delete from historique_connexion where le < :avant").bind("avant", OffsetDateTime.ofInstant(avant, ZoneOffset.UTC)).fetch().rowsUpdated();
    }

    static TentativeConnexion tentative(Readable r) {
        OffsetDateTime le = r.get("le", OffsetDateTime.class);
        return new TentativeConnexion(
            le == null ? null : le.toInstant(),
            r.get("sujet", String.class),
            r.get("empreinte_identifiant", String.class),
            ResultatTentative.valueOf(r.get("resultat", String.class)),
            new Appareil(r.get("navigateur", String.class), r.get("systeme", String.class), type(r.get("type_appareil", String.class))),
            r.get("adresse_ip", String.class),
            r.get("couche", String.class)
        );
    }

    static TypeAppareil type(String valeur) {
        try {
            return valeur == null ? TypeAppareil.INCONNU : TypeAppareil.valueOf(valeur);
        } catch (IllegalArgumentException e) {
            return TypeAppareil.INCONNU;
        }
    }

    private static DatabaseClient.GenericExecuteSpec lier(DatabaseClient.GenericExecuteSpec s, String nom, String valeur) {
        return valeur == null ? s.bindNull(nom, String.class) : s.bind(nom, valeur);
    }
}
