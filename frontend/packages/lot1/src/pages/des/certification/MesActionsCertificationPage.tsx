import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { DataTable, Link, PageHeader, StatCard, StatusBadge, Tabs, Tag, type Column } from '@gimac/design-system'
import { usePermissions } from '@gimac/socle'
import { useCatalogueFonctionnalites } from '../../../catalogue'
import { accesFonctionnalite } from '../../../fonctionnalites'
import type { LiaisonSuivie, MesOperations, MonOperation, Personne } from '../../../modele/parcours'
import { useMesOperations } from '../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../hooks/referentiels'
import { Donnees } from '../liaisons/communs'
import { BadgeStatut, Echeance, useFormatDate } from '../parcours/communs'
import { TON_ETAT } from '../parcours/operations/regles'

// « Mes actions de certification » (D-98 §5, docs/lot1/lancement-processus.md §8) : chaque opération des parcours de
// certification que la personne doit faire (responsable, validateur, consulté, signataire) ou peut prendre, servie par
// MS-HOMOLOGATION (GET /parcours/mes-operations), avec le lien direct vers l'écran de l'opération où ses actions réelles
// se réalisent ; puis les opérations qu'elle suit et l'état des liaisons qu'elle conduit ou sur lesquelles elle
// intervient (statut, avancement, jalons). Le canal poussé rafraîchit la liste à chaque geste d'un autre acteur.

const ONGLETS = ['a-faire', 'a-suivre', 'liaisons'] as const
type Onglet = (typeof ONGLETS)[number]

/** Rôle de la personne sur l'opération : signature attendue, tâche à prendre, sinon sa qualité (R, V, C). */
function Role({ o }: { o: MonOperation }) {
  const { t } = useTranslation()
  return (
    <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
      {o.signature ? <StatusBadge status="info" icon="pen-line" label={t('certif.role.SIGNATAIRE')} /> : o.qualite ? <Tag>{`${o.qualite} · ${t(`certif.role.${o.qualite}`)}`}</Tag> : null}
      {o.aPrendre ? <StatusBadge status="warn" label={t('certif.role.A_PRENDRE')} /> : null}
    </span>
  )
}

/** Actions que le service permet maintenant à la personne, et le geste de certification de l'opération s'il y en a un. */
function Actions({ o }: { o: MonOperation }) {
  const { t } = useTranslation()
  const gestes = o.actionsPermises.filter((a) => a !== 'ATTRIBUER' && a !== 'CONSULTER').map((a) => t(`certif.action.${a}`, { defaultValue: a }))
  // Geste de certification (verrous de la décision) : réalisé dans l'onglet Décision du dossier.
  if (o.declencheur && !gestes.length) gestes.push(t(`certif.geste.${o.declencheur.replace(/^certification\./, '').replace(/\.v\d+$/, '')}`, { defaultValue: t('certif.geste.autre') }))
  return <span>{gestes.join(' · ')}</span>
}

function Intervenant({ p }: { p: Personne | null | undefined }) {
  const { t } = useTranslation()
  const f = useFonctionsCatalogue()
  if (!p) return <span className="gm-field-help">{t('certif.actions.nonDesigne')}</span>
  return (
    <span className="gm-col" style={{ gap: 0 }}>
      <span>{p.aAffecter ? t('certif.actions.aAffecter', { fonction: f.libelle(p.fonction) }) : p.nom}</span>
      {p.aAffecter ? null : <span className="gm-field-help">{f.libelle(p.fonction)}</span>}
    </span>
  )
}

function useColonnes(suivi: boolean): Column<MonOperation>[] {
  const { t } = useTranslation()
  const operation: Column<MonOperation> = {
    key: 'operation', header: t('certif.actions.col.operation'),
    render: (o) => (
      <span className="gm-col" style={{ gap: 0 }}>
        <Link href={o.lien}>{o.libelle || o.code}</Link>
        <span className="gm-field-help">
          <span className="gm-mono">{o.code}</span> · {t(`parcours.etape.${o.etape}`, { defaultValue: o.etapeCode || o.etape })}
          {o.jalon ? ` · ${t(`certif.jalon.${o.jalon}`)}` : ''}
        </span>
      </span>
    ),
  }
  const liaison: Column<MonOperation> = {
    key: 'liaison', header: t('certif.actions.col.liaison'),
    render: (o) => (
      <span className="gm-col" style={{ gap: 0 }}>
        <Link href={`/des/parcours/${o.liaisonId}`} mono>{o.liaisonReference}</Link>
        <span className="gm-field-help">{o.institutionNom}</span>
      </span>
    ),
  }
  const etat: Column<MonOperation> = { key: 'etat', header: t('certif.actions.col.etat'), width: 170, render: (o) => <StatusBadge status={TON_ETAT[o.etat]} label={t(`parcours.etatOp.${o.etat}`)} /> }
  const echeance: Column<MonOperation> = { key: 'echeance', header: t('certif.actions.col.echeance'), width: 140, render: (o) => <Echeance iso={o.echeance ?? null} sla={o.sla ?? null} /> }
  if (suivi) {
    return [
      operation, liaison, etat,
      { key: 'responsable', header: t('certif.actions.col.responsable'), render: (o) => <Intervenant p={o.responsable} /> },
      { key: 'validateur', header: t('certif.actions.col.validateur'), render: (o) => <Intervenant p={o.validateur} /> },
      echeance,
    ]
  }
  return [
    operation, liaison,
    { key: 'role', header: t('certif.actions.col.role'), width: 170, render: (o) => <Role o={o} /> },
    etat,
    { key: 'actions', header: t('certif.actions.col.actions'), render: (o) => <Actions o={o} /> },
    echeance,
  ]
}

/** Jalons d'une liaison suivie, atteints ou attendus. */
function Jalons({ l }: { l: LiaisonSuivie }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  return (
    <div role="list" aria-label={t('certif.liaison.jalons')} className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
      {l.jalons.map((j) => (
        <span role="listitem" key={`${j.jalon}-${j.operationId ?? ''}`}>
          <StatusBadge status={j.atteint ? 'ok' : 'neutral'} icon={j.atteint ? 'check' : 'hourglass'}
            label={`${t(`certif.jalon.${j.jalon}`)} : ${j.atteint ? t('certif.jalon.atteint', { date: date(j.atteintLe ?? '') }) : t('certif.jalon.attendu')}`} />
        </span>
      ))}
    </div>
  )
}

/** Liaisons suivies en tableau paginé (D-112) : statut, étape en cours, avancement et jalons atteints ou attendus. */
function useColonnesLiaisons(): Column<LiaisonSuivie>[] {
  const { t } = useTranslation()
  return [
    {
      key: 'liaison', header: t('certif.actions.col.liaison'),
      render: (l) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <Link href={l.lien} mono>{l.reference}</Link>
          <span className="gm-field-help">{l.institutionNom}</span>
          {l.conducteur ? <Tag>{t('certif.liaison.conducteur')}</Tag> : null}
        </span>
      ),
    },
    { key: 'statut', header: t('certif.actions.col.statut'), width: 170, render: (l) => <BadgeStatut statut={l.statut} /> },
    { key: 'etape', header: t('certif.actions.col.etape'), render: (l) => t(`parcours.etape.${l.etapeCourante}`, { defaultValue: l.etapeCourante }) },
    { key: 'avancement', header: t('certif.actions.col.avancement'), width: 200, render: (l) => t('certif.liaison.avancement', { faites: l.operationsFaites, total: l.operationsTotal }) },
    { key: 'jalons', header: t('certif.liaison.jalons'), render: (l) => <Jalons l={l} /> },
  ]
}

function Vue({ m }: { m: MesOperations }) {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const onglet: Onglet = (ONGLETS as readonly string[]).includes(params.get('onglet') ?? '') ? (params.get('onglet') as Onglet) : 'a-faire'
  const colonnesAgir = useColonnes(false)
  const colonnesSuivre = useColonnes(true)
  const colonnesLiaisons = useColonnesLiaisons()
  const n = {
    aFaire: m.aAgir.filter((o) => o.qualite === 'R' && !o.aPrendre).length,
    aPrendre: m.aAgir.filter((o) => o.aPrendre).length,
    aValider: m.aAgir.filter((o) => o.qualite === 'V' && !o.aPrendre).length,
    aSigner: m.aAgir.filter((o) => o.signature).length,
  }
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <div className="gm-stat-grid" role="group" aria-label={t('certif.actions.compteurs')}>
        {(['aFaire', 'aPrendre', 'aValider', 'aSigner'] as const).map((k) => (
          <StatCard key={k} label={t(`certif.actions.compteur.${k}`)} value={n[k]} status={n[k] ? 'warn' : 'ok'} statusLabel={t(n[k] ? 'parcoursActeur.ton.warn' : 'parcoursActeur.ton.ok')} />
        ))}
        <StatCard label={t('certif.actions.compteur.suivies')} value={m.liaisons.length} status="info" statusLabel={t('parcoursActeur.ton.info')} />
      </div>
      <Tabs
        items={[
          { key: 'a-faire', label: t('certif.actions.onglet.aAgir', { count: m.aAgir.length }) },
          { key: 'a-suivre', label: t('certif.actions.onglet.aSuivre', { count: m.aSuivre.length }) },
          { key: 'liaisons', label: t('certif.actions.onglet.liaisons', { count: m.liaisons.length }) },
        ]}
        value={onglet}
        onChange={(k) => setParams(k === 'a-faire' ? {} : { onglet: k }, { replace: true })}
      />
      {onglet === 'a-faire' ? (
        <DataTable columns={colonnesAgir} rows={m.aAgir} rowKey={(o) => `${o.operationId}:${o.qualite ?? ''}:${o.signature ?? ''}`} pageSize={25} density="compact"
          state={m.aAgir.length ? 'ok' : 'empty'} emptyText={t('certif.actions.aucune')} emptyHelp={t('certif.actions.aucune.aide')} />
      ) : null}
      {onglet === 'a-suivre' ? (
        <DataTable columns={colonnesSuivre} rows={m.aSuivre} rowKey={(o) => o.operationId} pageSize={25} density="compact" state={m.aSuivre.length ? 'ok' : 'empty'} emptyText={t('certif.actions.aucuneSuivie')} />
      ) : null}
      {onglet === 'liaisons' ? (
        <DataTable columns={colonnesLiaisons} rows={m.liaisons} rowKey={(l) => l.liaisonId} pageSize={25} density="compact"
          state={m.liaisons.length ? 'ok' : 'empty'} emptyText={t('certif.actions.aucuneLiaison')} />
      ) : null}
    </div>
  )
}

export default function MesActionsCertificationPage() {
  const { t } = useTranslation()
  const q = useMesOperations()
  const permissions = usePermissions()
  // Le lancement n'est proposé qu'à qui l'ouvre selon le catalogue des fonctionnalités (D-94), jamais selon un profil.
  const catalogue = useCatalogueFonctionnalites()
  const lancer = accesFonctionnalite(catalogue, 'lancerCertification', permissions) === 'ouvert'
  return (
    <div className="gm-col">
      <PageHeader
        title={t('certif.actions.titre')}
        subtitle={t('certif.actions.description')}
        actions={lancer ? <Link href="/des/certification/lancer">{t('certif.actions.lancer')}</Link> : null}
      />
      <Donnees requete={q}>{(m) => <Vue m={m} />}</Donnees>
    </div>
  )
}
