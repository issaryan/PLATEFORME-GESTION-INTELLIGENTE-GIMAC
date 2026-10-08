import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, DataTable, Link, ProgressRing, StatusBadge, Tag, type Column } from '@gimac/design-system'
import { usePermissions, type ApiError } from '@gimac/socle'
import type { CoverageValue } from '../../../modele/commun'
import type { LiaisonResume, TacheActeur } from '../../../modele/parcours'
import { Echeance, GESTES, gesteDe, lienGeste, tonStatut, useDate, useLibelleTache } from './commun'

// Listes du portail du participant : les tâches qui lui incombent, chacune menant à son geste, et ses
// liaisons avec l'étape courante et les deux couvertures (CR §6.5 : lues directement et séparément).

type Etat = { isPending: boolean; isError: boolean; error: ApiError | null; refetch: () => unknown }
const etatTable = (q: Etat, n: number) => (q.isPending ? 'loading' : q.isError ? 'error' : n === 0 ? 'empty' : 'ok')

export function Couverture({ libelle, valeur }: { libelle: string; valeur: CoverageValue }) {
  const { t } = useTranslation()
  return (
    <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }} title={valeur.calculable ? `${valeur.numerateur} / ${valeur.denominateur}` : valeur.motifNonCalculable}>
      <ProgressRing compact label={libelle} value={valeur.calculable ? valeur.taux : null} />
      <span className="gm-num gm-type-small">{valeur.calculable ? `${valeur.numerateur} / ${valeur.denominateur}` : t('pp.nonCalculable')}</span>
    </span>
  )
}

/** Tâches du participant : libellé, liaison, étape, qualité, échéance, et le geste qui la réalise. */
export function TableTaches({ taches, requete, liaison = true }: { taches: TacheActeur[]; requete: Etat; liaison?: boolean }) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const libelle = useLibelleTache()
  const colonnes: Column<TacheActeur>[] = [
    {
      key: 'rang', header: t('pp.tache'),
      render: (x) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)', textAlign: 'left' }}>
          <span className="gm-type-body-strong">{libelle(x)}</span>
          {/* Preuve attendue d'une opération reprise de l'annexe A ; rien pour une opération d'un type créé (D-98). */}
          {i18n.exists(`parcours.preuveAttendue.${x.rang}`) ? (
            <span className="gm-type-small" style={{ color: 'var(--gm-color-text-secondary)' }}>
              {t('pp.preuve', { preuve: t(`parcours.preuveAttendue.${x.rang}`) })}
            </span>
          ) : null}
        </span>
      ),
    },
    ...(liaison ? [{ key: 'liaisonReference', header: t('pp.liaison'), render: (x: TacheActeur) => <span style={{ whiteSpace: 'nowrap' }}><Link href={`/portail/liaisons/${x.liaisonId}`} mono>{x.liaisonReference}</Link></span> }] : []),
    { key: 'etape', header: t('pp.etape'), render: (x) => t(`parcours.etape.${x.etape}`) },
    { key: 'qualite', header: t('pp.qualite'), render: (x) => <Tag>{t(`parcours.qualite.${x.qualite}`)}</Tag> },
    { key: 'echeance', header: t('pp.echeance'), render: (x) => <Echeance date={x.echeance} sla={x.sla} /> },
    {
      key: 'action', header: t('pp.action'),
      render: (x) => {
        const g = gesteDe(x, permissions)
        return g ? (
          <Button size="sm" variant="secondary" icon={GESTES[g].icone} onClick={() => void navigate(lienGeste(x, g))}>{t(GESTES[g].cle)}</Button>
        ) : (
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
            <StatusBadge status="neutral" label={t('pp.enAttenteGimac')} />
            <Link href={x.lien}>{t('pp.voir')}</Link>
          </span>
        )
      },
    },
  ]
  return (
    <DataTable
      columns={colonnes}
      rows={taches}
      rowKey={(x) => x.operationId}
      state={etatTable(requete, taches.length)}
      error={requete.error ?? undefined}
      onRetry={() => void requete.refetch()}
      emptyText={t('pp.taches.vide')}
      emptyHelp={t('pp.taches.videAide')}
    />
  )
}

/** Liaisons de l'institution du jeton : étape courante, statut, deux couvertures, prochaine échéance. */
export function TableLiaisons({ liaisons, requete }: { liaisons: LiaisonResume[]; requete: Etat }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const colonnes: Column<LiaisonResume>[] = [
    {
      key: 'reference', header: t('pp.liaison'),
      render: (l) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)', textAlign: 'left' }}>
          <span style={{ whiteSpace: 'nowrap' }}><Link href={`/portail/liaisons/${l.id}`} mono>{l.reference}</Link></span>
          <span className="gm-type-small">{t(`parcours.typeProjet.${l.typeProjet}`, { defaultValue: String(l.typeProjet) })}</span>
        </span>
      ),
    },
    { key: 'etapeCourante', header: t('pp.etapeCourante'), render: (l) => t(`parcours.etape.${l.etapeCourante}`) },
    {
      key: 'statut', header: t('pp.statut'),
      render: (l) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
          <StatusBadge status={tonStatut[l.statut]} label={t(`parcours.statut.${l.statut}`)} />
          {l.bloquee ? <StatusBadge status="crit" label={t('pp.bloquee')} /> : null}
        </span>
      ),
    },
    { key: 'certification', header: t('pp.couvertureCertification'), render: (l) => <Couverture libelle={t('pp.couvertureCertification')} valeur={l.couvertureCertification} /> },
    { key: 'miseEnService', header: t('pp.couvertureMiseEnService'), render: (l) => <Couverture libelle={t('pp.couvertureMiseEnService')} valeur={l.couvertureMiseEnService} /> },
    { key: 'prochaineEcheance', header: t('pp.prochaineEcheance'), render: (l) => <span className="gm-num">{l.prochaineEcheance ? ecrire(l.prochaineEcheance) : t('pp.sansEcheance')}</span> },
  ]
  return (
    <DataTable
      columns={colonnes}
      rows={liaisons}
      rowKey={(l) => l.id}
      state={etatTable(requete, liaisons.length)}
      error={requete.error ?? undefined}
      onRetry={() => void requete.refetch()}
      emptyText={t('pp.liaisons.vide')}
      emptyHelp={t('pp.liaisons.videAide')}
    />
  )
}
