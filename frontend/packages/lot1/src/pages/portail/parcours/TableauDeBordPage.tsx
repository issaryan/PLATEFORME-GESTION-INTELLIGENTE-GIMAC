import { useTranslation } from 'react-i18next'
import { InlineAlert, Link, Skeleton, StatCard, Tag, Button } from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import { useMesLiaisonsPortail, useMesTachesPortail } from '../../../hooks/parcours'
import { EnTete, Section } from './commun'
import { TableLiaisons, TableTaches } from './listes'

// Tableau de bord du participant (page d'accueil du portail, D-58) : compteurs calculés par le service
// pour l'institution du jeton, tâches qui lui incombent (qualité Externe, CR §3.4) menant chacune à son
// geste, et ses liaisons avec l'étape courante et les deux couvertures.

// Trois colonnes au poste (ce qui est à faire, puis l'état des délais), une seule sur mobile : StatCard a 200 px de largeur minimale.
const grille = { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 300px), 1fr))', gap: 'var(--gm-space-4)' }

/** Ordre de lecture des compteurs : ce que le participant doit faire, puis l'état de ses délais. */
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
        sousTitre={
          <span className="gm-row">
            <span>{session ? t('pp.tableau.bienvenue', { nom: session.nom }) : null}</span>
            <Tag>{t('pp.profil')}</Tag>
            {institution ? <Tag>{institution}</Tag> : null}
          </span>
        }
      />
      <Section titre={t('pp.tableau.aFaire')}>
        {taches.isPending ? (
          <div style={grille} aria-busy="true">{ORDRE.map((c) => <Skeleton key={c} height={104} />)}</div>
        ) : taches.isError ? (
          <InlineAlert level="crit" title={t('commun.erreur.titre')} correlationId={taches.error?.correlationId || undefined}
            action={<Button size="sm" icon="refresh" onClick={() => void taches.refetch()}>{t('commun.reessayer')}</Button>}>
            {taches.error?.message}
          </InlineAlert>
        ) : (
          <div style={grille}>
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
      <Section titre={t('pp.tableau.mesTaches')}>
        <p className="gm-type-body" style={{ margin: 0, color: 'var(--gm-color-text-secondary)' }}>{t('pp.tableau.mesTachesAide')}</p>
        <TableTaches taches={tableau?.taches ?? []} requete={taches} />
      </Section>
      <Section titre={t('pp.tableau.mesLiaisons')} action={<Link href="/portail/liaisons">{t('pp.voirTout')}</Link>}>
        <TableLiaisons liaisons={liaisons.data ?? []} requete={liaisons} />
      </Section>
    </div>
  )
}
