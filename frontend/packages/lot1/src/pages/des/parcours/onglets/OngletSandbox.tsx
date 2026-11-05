import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Banner, Card, DataTable, DateTime, Hash, KeyValueList, Note, StatusBadge, type Column, type Status } from '@gimac/design-system'
import { useExecutionsSandbox, useResultatsSandbox } from '../../../../hooks/parcours'
import type { DossierLiaison } from '../../../../modele/parcours'
import type { CampagneSandbox, ExecutionSandbox, PieceSandbox, ResultatsSandbox, ScenarioNonRapproche, TauxSandbox } from '../../../../modele/sandbox'
import { Donnees, etatListe } from '../../liaisons/communs'
import { GraphesCampagne } from './GraphesSandbox'
import { Valeur } from '../communs'

// Onglet « Résultats de la sandbox » du dossier de liaison (DES-09, US-EXI-17, D-118 point 1) : campagnes reprises par
// MS-HOMOLOGATION depuis SBX-LECTURE (US-EXI-14), verdicts natif et normalisé avec la version de la table, taux par
// service avec numérateur, dénominateur et version de formule, pièces et leur empreinte copiable, scénarios non
// rapprochés. Lecture seule : aucune action vers la sandbox, mention permanente « repris de la sandbox, jamais recalculé » ;
// une relève en échec laisse les résultats déjà repris affichés avec la date de la dernière relève réussie. Chaque
// campagne porte sa vue graphique (jauges, verdicts, réussite par service), les tableaux restant en appoint.

const TAILLE_DEFAUT = 25
const tonCampagne: Record<string, Status> = { TERMINEE: 'ok', EN_COURS: 'info', SUSPENDUE: 'warn', ANNULEE: 'neutral', SIGNALEE: 'crit' }
const tonVerdict: Record<string, Status> = { FAVORABLE: 'ok', DEFAVORABLE: 'crit', NON_EXECUTE: 'neutral' }
const tonPiece: Record<string, Status> = { DEPOSEE: 'ok', DEJA_CONSERVEE: 'ok', EMPREINTE_DIVERGENTE: 'crit' }

/** Instant ISO affiché dans la langue courante, à l'heure de Douala. */
function useFormatInstant() {
  const { i18n } = useTranslation()
  const format = new Intl.DateTimeFormat(i18n.language, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Douala' })
  return (iso: string) => {
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : format.format(d)
  }
}

function Instant({ iso }: { iso: string | null | undefined }) {
  const instant = useFormatInstant()
  return iso ? <DateTime local={instant(iso)} iso={iso} utc={iso.replace('T', ' ').replace(/(\.\d+)?Z$/, '')} /> : <Valeur>{null}</Valeur>
}

/** Copie d'une empreinte dans le presse-papiers (sans effet si le navigateur la refuse). */
function copier(texte: string) {
  void navigator.clipboard?.writeText(texte).catch(() => undefined)
}

export function OngletSandbox({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useResultatsSandbox(dossier.id)
  return (
    <div className="gm-col">
      <Note title={t('sandboxResultats.mention.titre')}>{t('sandboxResultats.mention.texte')}</Note>
      <Donnees requete={q}>{(r) => <Resultats resultats={r} liaisonId={dossier.id} />}</Donnees>
    </div>
  )
}

function Resultats({ resultats: r, liaisonId }: { resultats: ResultatsSandbox; liaisonId: string }) {
  const { t } = useTranslation()
  const instant = useFormatInstant()
  const [choisie, setChoisie] = useState<string | undefined>(r.campagnes[0]?.campagneId)
  const campagne = r.campagnes.find((c) => c.campagneId === choisie) ?? r.campagnes[0]
  const colonnes: Column<CampagneSandbox>[] = [
    { key: 'code', header: t('sandboxResultats.campagne.code'), render: (c) => <span className="gm-mono">{c.code ?? c.campagneId}</span> },
    { key: 'etat', header: t('sandboxResultats.campagne.etat'), render: (c) => <StatusBadge status={tonCampagne[c.etat] ?? 'neutral'} label={t(`sandboxResultats.etat.${c.etat}`, { defaultValue: c.etat })} /> },
    { key: 'interfaceTest', header: t('sandboxResultats.campagne.interface'), render: (c) => <Valeur>{c.interfaceTest}</Valeur> },
    { key: 'executions', header: t('sandboxResultats.campagne.executions'), type: 'number' },
    { key: 'nonRapprochees', header: t('sandboxResultats.campagne.nonRapprochees'), type: 'number' },
    { key: 'releveLe', header: t('sandboxResultats.campagne.releveLe'), render: (c) => <Instant iso={c.releveLe} /> },
  ]
  return (
    <div className="gm-col">
      {r.enEchec ? (
        <Banner level="warn" title={t('sandboxResultats.echec.titre')}>
          {t('sandboxResultats.echec.texte', {
            echec: r.dernierEchec ? instant(r.dernierEchec) : '',
            motif: r.motifEchec ?? t('sandboxResultats.echec.sansMotif'),
            reussie: r.derniereReleveReussie ? instant(r.derniereReleveReussie) : t('sandboxResultats.releve.jamais'),
          })}
        </Banner>
      ) : null}
      <Card title={t('sandboxResultats.releve.titre')}>
        <KeyValueList
          items={[
            { key: t('sandboxResultats.releve.derniere'), value: r.derniereReleveReussie ? <Instant iso={r.derniereReleveReussie} /> : <span className="gm-field-help">{t('sandboxResultats.releve.jamais')}</span> },
            { key: t('sandboxResultats.releve.campagnes'), value: r.campagnes.length },
            { key: t('sandboxResultats.releve.nonRapproches'), value: r.scenariosNonRapproches.length },
          ]}
        />
      </Card>
      <Card title={t('sandboxResultats.campagnes.titre')}>
        <DataTable
          columns={colonnes}
          rows={r.campagnes}
          rowKey={(c) => c.campagneId}
          selected={campagne ? [campagne.campagneId] : []}
          onRowOpen={(c) => setChoisie(c.campagneId)}
          state={r.campagnes.length ? 'ok' : 'empty'}
          emptyText={t('sandboxResultats.campagnes.vide')}
          emptyHelp={t('sandboxResultats.campagnes.videAide')}
        />
      </Card>
      {campagne ? <Campagne campagne={campagne} liaisonId={liaisonId} /> : null}
      <ScenariosNonRapproches scenarios={r.scenariosNonRapproches} />
    </div>
  )
}

function Campagne({ campagne: c, liaisonId }: { campagne: CampagneSandbox; liaisonId: string }) {
  const { t } = useTranslation()
  const taux: Column<TauxSandbox>[] = [
    { key: 'service', header: t('sandboxResultats.taux.service'), render: (x) => x.service ?? <span className="gm-field-help">{t('sandboxResultats.taux.campagne')}</span> },
    { key: 'nature', header: t('sandboxResultats.taux.nature'), render: (x) => t(`sandboxResultats.nature.${x.nature}`, { defaultValue: x.nature }) },
    { key: 'numerateur', header: t('sandboxResultats.taux.numerateur'), type: 'number' },
    { key: 'denominateur', header: t('sandboxResultats.taux.denominateur'), type: 'number' },
    { key: 'valeur', header: t('sandboxResultats.taux.valeur'), render: (x) => <span className="gm-num">{t('sandboxResultats.taux.pourcent', { valeur: x.valeur })}</span> },
    { key: 'versionFormule', header: t('sandboxResultats.taux.versionFormule'), type: 'mono' },
    { key: 'horodatage', header: t('sandboxResultats.taux.horodatage'), render: (x) => <Instant iso={x.horodatage} /> },
  ]
  const pieces: Column<PieceSandbox>[] = [
    { key: 'nature', header: t('sandboxResultats.piece.nature'), render: (p) => t(`sandboxResultats.natureP.${p.nature}`, { defaultValue: p.nature }) },
    { key: 'version', header: t('sandboxResultats.piece.version'), type: 'number' },
    { key: 'nom', header: t('sandboxResultats.piece.nom'), render: (p) => <Valeur>{p.nom}</Valeur> },
    { key: 'taille', header: t('sandboxResultats.piece.taille'), render: (p) => <span className="gm-num">{t('sandboxResultats.piece.octets', { count: p.taille })}</span> },
    { key: 'empreinte', header: t('sandboxResultats.piece.empreinte'), render: (p) => <Hash value={p.empreinte} onCopy={() => copier(p.empreinte)} /> },
    { key: 'statut', header: t('sandboxResultats.piece.statut'), render: (p) => <StatusBadge status={tonPiece[p.statut] ?? 'neutral'} label={t(`sandboxResultats.statutPiece.${p.statut}`, { defaultValue: p.statut })} /> },
  ]
  return (
    <>
      <Card title={t('sandboxResultats.campagne.titre', { code: c.code ?? c.campagneId })}>
        {c.signalement ? <Banner level="warn" title={t('sandboxResultats.signalement.titre')}>{t(`sandboxResultats.signalement.${c.signalement}`, { defaultValue: c.signalement })}</Banner> : null}
        <KeyValueList
          items={[
            { key: t('sandboxResultats.campagne.identifiant'), value: c.campagneId, mono: true },
            { key: t('sandboxResultats.campagne.services'), value: c.services.length ? c.services.join(', ') : <Valeur>{null}</Valeur> },
            { key: t('sandboxResultats.campagne.versionBilan'), value: <Valeur>{c.versionBilan}</Valeur> },
            { key: t('sandboxResultats.campagne.versionTable'), value: <Valeur>{c.versionTable}</Valeur> },
          ]}
        />
      </Card>
      <GraphesCampagne campagne={c} />
      <Card title={t('sandboxResultats.taux.titre')}>
        <DataTable columns={taux} rows={c.taux} rowKey={(x) => `${x.service ?? '-'}:${x.nature}`} state={c.taux.length ? 'ok' : 'empty'} emptyText={t('sandboxResultats.taux.vide')} />
      </Card>
      <Executions liaisonId={liaisonId} campagneId={c.campagneId} />
      <Card title={t('sandboxResultats.pieces.titre')}>
        <DataTable columns={pieces} rows={c.pieces} rowKey={(p) => `${p.nature}:${p.version}`} state={c.pieces.length ? 'ok' : 'empty'} emptyText={t('sandboxResultats.pieces.vide')} />
      </Card>
    </>
  )
}

function Executions({ liaisonId, campagneId }: { liaisonId: string; campagneId: string }) {
  const { t } = useTranslation()
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(TAILLE_DEFAUT)
  const q = useExecutionsSandbox(liaisonId, campagneId, page - 1, taille)
  const colonnes: Column<ExecutionSandbox>[] = [
    { key: 'identifiantExecution', header: t('sandboxResultats.execution.identifiant'), type: 'mono' },
    { key: 'identifiantScenario', header: t('sandboxResultats.execution.scenario'), type: 'mono' },
    { key: 'dateExecution', header: t('sandboxResultats.execution.date'), render: (e) => <Instant iso={e.dateExecution} /> },
    { key: 'verdictNatif', header: t('sandboxResultats.execution.verdictNatif'), type: 'mono' },
    {
      key: 'verdictNormalise',
      header: t('sandboxResultats.execution.verdictNormalise'),
      // Contrat de lecture : favorable, defavorable, non_execute (minuscules) ; libellé et ton par la forme majuscule.
      render: (e) => (e.verdictNormalise ? <StatusBadge status={tonVerdict[e.verdictNormalise.toUpperCase()] ?? 'neutral'} label={t(`sandboxResultats.verdict.${e.verdictNormalise.toUpperCase()}`, { defaultValue: e.verdictNormalise })} /> : <Valeur>{null}</Valeur>),
    },
    { key: 'versionTable', header: t('sandboxResultats.execution.versionTable'), render: (e) => <Valeur>{e.versionTable}</Valeur> },
    {
      key: 'operationCode',
      header: t('sandboxResultats.execution.operation'),
      render: (e) => (e.operationCode ? <span className="gm-mono">{e.operationCode}</span> : <StatusBadge status="warn" label={t('sandboxResultats.execution.nonRapprochee')} />),
    },
  ]
  const lignes = q.data?.contenu
  const etat = etatListe(q, lignes)
  return (
    <Card title={t('sandboxResultats.executions.titre')}>
      <DataTable
        columns={colonnes}
        rows={lignes ?? []}
        rowKey={(e) => e.identifiantExecution}
        total={q.data?.total ?? 0}
        page={page}
        pageSize={taille}
        onPage={setPage}
        onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }}
        state={etat.state}
        error={etat.error}
        onRetry={() => void q.refetch()}
        emptyText={t('sandboxResultats.executions.vide')}
      />
    </Card>
  )
}

function ScenariosNonRapproches({ scenarios }: { scenarios: ScenarioNonRapproche[] }) {
  const { t } = useTranslation()
  const colonnes: Column<ScenarioNonRapproche>[] = [
    { key: 'idScenario', header: t('sandboxResultats.scenario.identifiant'), type: 'mono' },
    { key: 'campagneId', header: t('sandboxResultats.scenario.campagne'), render: (s) => <span className="gm-mono">{s.campagneId ?? ''}</span> },
    { key: 'qualification', header: t('sandboxResultats.scenario.qualification'), render: (s) => <StatusBadge status="warn" label={t(`sandboxResultats.qualification.${s.qualification}`, { defaultValue: s.qualification })} /> },
    { key: 'dateDetection', header: t('sandboxResultats.scenario.detection'), render: (s) => <Instant iso={s.dateDetection} /> },
  ]
  return (
    <Card title={t('sandboxResultats.scenarios.titre')}>
      <DataTable columns={colonnes} rows={scenarios} rowKey={(s) => s.id} state={scenarios.length ? 'ok' : 'empty'} emptyText={t('sandboxResultats.scenarios.vide')} />
    </Card>
  )
}
