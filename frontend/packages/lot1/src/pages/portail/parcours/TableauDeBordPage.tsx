import { useTranslation } from 'react-i18next'
import { InlineAlert, Link, Skeleton, StatCard, Button } from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import { useMesLiaisonsPortail, useMesTachesPortail } from '../../../hooks/parcours'
import { EnTete, Section } from './commun'
import { TableLiaisons, TableTaches } from './listes'

// Tableau de bord du participant (page d'accueil du portail, D-58) : compteurs calculés par le service
// pour l'institution du jeton, tâches qui lui incombent (qualité Externe, CR §3.4) menant chacune à son
// geste, et ses liaisons avec l'étape courante et les deux couvertures.

// Trois colonnes au poste (ce qui est à faire, puis l'état des délais), une seule sur mobile : StatCard a 200 px de largeur minimale.

/** Ordre de lecture des compteurs : ce que le participant doit faire, puis l'état de ses délais. */
/** Pictogramme de chaque compteur du participant. */
const ORDRE = ['tachesARealiser', 'pvASigner', 'attestationsAFournir', 'fichesSuivi', 'tachesEnRetard', 'echeancesProches']

export default function TableauDeBordPage() {
  const { t } = useTranslation()
  const session = useSession()
  const taches = useMesTachesPortail()
  const liaisons = useMesLiaisonsPortail()
  const tableau = taches.data
  const rang = (cle: string) => (ORDRE.includes(cle) ? ORDRE.indexOf(cle) : ORDRE.length)
  const compteurs = [...(tableau?.compteurs ?? [])].sort((a, b) => rang(a.cle) - rang(b.cle))
  const institution = liaisons.data?.[0]?.institutionNom
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <EnTete
        titre={t('pp.tableau.titre')}
        sousTitre={t('pp.tableau.sousTitre')}
        utilisateur={session ? { name: session.nom, roles: [t('pp.profil'), ...(institution ? [institution] : [])] } : undefined}
      />
      <Section titre={t('pp.tableau.aFaire')}>
        {taches.isPending ? (
          <div className="gm-stat-grid" aria-busy="true">{ORDRE.map((c) => <Skeleton key={c} height={104} />)}</div>
        ) : taches.isError ? (
          <InlineAlert level="crit" title={t('commun.erreur.titre')} correlationId={taches.error?.correlationId || undefined}
            action={<Button size="sm" icon="refresh" onClick={() => void taches.refetch()}>{t('commun.reessayer')}</Button>}>
            {taches.error?.message}
          </InlineAlert>
        ) : (
          <div className="gm-stat-grid" role="group" aria-label={t('pp.tableau.aFaire')}>
            {compteurs.map((c) => (
              <StatCard
                key={c.cle}
                label={t(`parcours.compteur.${c.cle}`)}
                value={c.valeur}
                status={c.ton}
                statusLabel={t(c.valeur === 0 ? 'pp.compteur.aJour' : c.ton === 'crit' ? 'pp.compteur.enRetard' : 'pp.compteur.aTraiter')}
              />
            ))}
          </div>
        )}
      </Section>
      <Section titre={t('pp.tableau.mesTaches')} description={t('pp.tableau.mesTachesAide')}>
        <TableTaches taches={tableau?.taches ?? []} requete={taches} />
      </Section>
      <Section titre={t('pp.tableau.mesLiaisons')} action={<Link href="/portail/liaisons">{t('pp.voirTout')}</Link>}>
        <TableLiaisons liaisons={liaisons.data ?? []} requete={liaisons} />
      </Section>
    </div>
  )
}
