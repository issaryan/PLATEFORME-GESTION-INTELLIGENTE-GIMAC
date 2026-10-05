import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Banner, Button, DataTable, Drawer, InlineAlert, Select, StatCard, StatusBadge, Switch, TextField, type Column } from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import { useOperationsParcours } from '../../../../hooks/parcours'
import { ETAPES, type DossierLiaison, type OperationParcours } from '../../../../modele/parcours'
import { fonctionsDe } from '../../../../permissions'
import { Donnees } from '../../liaisons/communs'
import { Alertes, Couverture, Essais, Essentiel, GesteAilleurs, InfosOperation, PreuveDeposee, Raci } from '../operations/Detail'
import { BlocValidation, FormulaireAttribution, FormulaireLivrable, FormulairePreuve, usePermis } from '../operations/Execution'
import { useActionsOperation } from '../operations/actions'
import { useExecuteurActions } from '../../../../ressources/actions'
import { compter, externe, filtrer, grouper, ongletDuGeste, plateformes, TON_ETAT, VUES, type Filtres, type Vue } from '../operations/regles'
import { tonEtape, tonSla, useFormatDate } from '../communs'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'

// Onglet « Opérations » du dossier de liaison (étapes 3 à 10, exécution des étapes 6 à 9) : opérations
// regroupées par étape et par segment, RACI+E, plateforme, grain de preuve, verrous, échéances ; tiroir
// d'exécution ouvert par le lien profond ?operation=<id> (tâches du tableau de bord). Filtres dans
// l'adresse, remplacés sans empiler l'historique : le retour mène au tableau de bord en un geste.
export function OngletOperations({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useOperationsParcours(dossier.id)
  return (
    <Donnees requete={q} estVide={(l) => l.length === 0} vide={t('parcoursOps.vide')}>
      {(ops) => <Vue dossier={dossier} ops={ops} />}
    </Donnees>
  )
}

function Vue({ dossier, ops }: { dossier: DossierLiaison; ops: OperationParcours[] }) {
  const { t } = useTranslation()
  const session = useSession()
  const fonctions = useMemo(() => fonctionsDe(session?.roles ?? []), [session])
  const [params, setParams] = useSearchParams()
  const [dernier, setDernier] = useState<string | null>(null)
  const [deplies, setDeplies] = useState<string[]>([])
  const maj = (k: string, v: string | null) =>
    setParams((p) => {
      const n = new URLSearchParams(p)
      if (v) n.set(k, v)
      else n.delete(k)
      return n
    }, { replace: true })
  const vueParam = params.get('vue') ?? ''
  const filtres: Filtres = {
    vue: (VUES as string[]).includes(vueParam) ? (vueParam as Vue) : '',
    etape: params.get('etape') ?? '',
    miennes: params.get('miennes') === '1',
    recherche: params.get('q') ?? '',
  }
  const libelle = (o: OperationParcours) => libelleOperation(t, o)
  const visibles = filtrer(ops, filtres, fonctions, libelle)
  const groupes = grouper(visibles)
  const n = compter(ops)
  const opId = params.get('operation')
  const ouverte = opId ? ops.find((o) => o.id === opId) ?? null : null
  const ouvrir = (id: string) => maj('operation', id)
  const ouvrirRang = (rang: string) => {
    const o = ops.find((x) => x.rang === rang)
    if (o) ouvrir(o.id)
  }
  const fermer = () => {
    setDernier(opId)
    maj('operation', null)
  }
  const o28 = ops.find((o) => o.oubliSansSymptome && o.etat !== 'FAITE')
  const etapesPresentes = ETAPES.filter((e) => ops.some((o) => o.etape === e))
  const filtre = Boolean(filtres.vue || filtres.etape || filtres.miennes || filtres.recherche)

  return (
    <div className="gm-col">
      {o28 ? (
        <Banner level={o28.etat === 'VERROUILLEE' ? 'warn' : 'crit'} title={t('parcoursOps.alerte28.titre')}>
          <span className="gm-col" style={{ gap: 'var(--gm-space-2)', alignItems: 'flex-start' }}>
            <span>{t('parcoursOps.alerte28.texte', { rang: o28.rang, libelle: libelle(o28), etat: t(`parcours.etatOp.${o28.etat}`) })}</span>
            <Button size="sm" variant="secondary" onClick={() => ouvrir(o28.id)}>{t('parcoursOps.alerte28.ouvrir', { rang: o28.rang })}</Button>
          </span>
        </Banner>
      ) : null}
      {opId && !ouverte ? (
        <InlineAlert level="warn" title={t('parcoursOps.introuvable.titre')} action={<Button size="sm" onClick={() => maj('operation', null)}>{t('parcoursOps.introuvable.fermer')}</Button>}>
          {t('parcoursOps.introuvable.texte', { id: opId })}
        </InlineAlert>
      ) : null}

      <div className="gm-stat-grid">
        <StatCard label={t('parcoursOps.compteur.aFaire')} value={n.aFaire} />
        <StatCard label={t('parcoursOps.compteur.aValider')} value={n.aValider} status={n.aValider ? 'warn' : undefined} statusLabel={n.aValider ? t('parcours.etatOp.EN_VALIDATION') : undefined} />
        <StatCard label={t('parcoursOps.compteur.retard')} value={n.retard} status={n.retard ? 'crit' : 'ok'} statusLabel={t(n.retard ? 'parcours.sla.DEPASSE' : 'parcours.sla.DANS_LES_DELAIS')} />
        <StatCard label={t('parcoursOps.compteur.faites')} value={n.faites} unit={` / ${n.total}`} />
      </div>

      <div className="gm-row" style={{ alignItems: 'flex-end', gap: 'var(--gm-space-3)' }} role="search" aria-label={t('parcoursOps.filtres')}>
        <div style={{ flex: '0 1 240px' }}>
          <TextField label={t('parcoursOps.recherche')} value={filtres.recherche} onChange={(v) => maj('q', v || null)} autoComplete="off" />
        </div>
        <div style={{ flex: '0 1 200px' }}>
          <Select label={t('parcoursOps.filtre.vue')} value={filtres.vue} onChange={(v) => maj('vue', v)} options={[{ value: '', label: t('parcoursOps.vue.toutes') }, ...VUES.map((v) => ({ value: v, label: t(`parcoursOps.vue.${v}`) }))]} />
        </div>
        <div style={{ flex: '0 1 220px' }}>
          <Select label={t('parcoursOps.filtre.etape')} value={filtres.etape} onChange={(v) => maj('etape', v)} options={[{ value: '', label: t('parcoursOps.etape.toutes') }, ...etapesPresentes.map((e) => ({ value: e, label: t(`parcours.etape.${e}`) }))]} />
        </div>
        {fonctions.length ? <Switch label={t('parcoursOps.filtre.miennes')} checked={filtres.miennes} onChange={(v) => maj('miennes', v ? '1' : null)} /> : null}
        {filtre ? (
          <Button variant="ghost" size="sm" icon="refresh" onClick={() => setParams((p) => { const k = new URLSearchParams(p); ['vue', 'etape', 'miennes', 'q'].forEach((x) => k.delete(x)); return k }, { replace: true })}>
            {t('parcoursOps.filtre.reinitialiser')}
          </Button>
        ) : null}
      </div>
      <p className="gm-section-desc" aria-live="polite">{t('parcoursOps.resultat', { n: visibles.length, total: ops.length })}</p>

      {groupes.length === 0 ? <InlineAlert level="info">{t('parcoursOps.aucunResultat')}</InlineAlert> : null}
      {groupes.map((g) => {
        const etape = dossier.etapes.find((e) => e.code === g.etape)
        // Une étape close est repliée par défaut (sauf filtre actif ou opération ouverte) : l'attention va aux étapes en cours.
        const repliee = etape?.etat === 'CLOSE' && !filtre && !deplies.includes(g.etape) && !g.segments.some((x) => x.operations.some((o) => o.id === opId || o.id === dernier))
        const nb = g.segments.reduce((k, x) => k + x.operations.length, 0)
        return (
          <section key={g.etape} className="gm-col" style={{ gap: 'var(--gm-space-3)' }} aria-labelledby={`ops-${g.etape}`}>
            <div className="gm-row" style={{ justifyContent: 'space-between' }}>
              <h3 id={`ops-${g.etape}`} className="gm-type-h3" style={{ margin: 0 }}>
                {etape?.rang ? t('parcoursOps.etapeN', { n: etape.rang, etape: t(`parcours.etape.${g.etape}`) }) : t(`parcours.etape.${g.etape}`)}
              </h3>
              {etape ? (
                <span className="gm-row">
                  <span className="gm-field-help">{t('parcoursOps.avancement', { faites: etape.actionsFaites, total: etape.actionsTotal })}</span>
                  <StatusBadge status={tonEtape[etape.etat]} label={t(`parcours.etatEtape.${etape.etat}`)} />
                  {etape.etat === 'CLOSE' && !filtre ? (
                    <Button variant="ghost" size="sm" icon={repliee ? 'chevron-right' : 'chevron-down'} aria-expanded={!repliee} aria-controls={`ops-${g.etape}-corps`}
                      onClick={() => setDeplies((l) => (repliee ? [...l, g.etape] : l.filter((x) => x !== g.etape)))}>
                      {t(repliee ? 'parcoursOps.deplier' : 'parcoursOps.replier', { count: nb })}
                    </Button>
                  ) : null}
                </span>
              ) : null}
            </div>
            <div id={`ops-${g.etape}-corps`} className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
            {repliee ? null : g.segments.map((s) => (
              <div key={s.segment} className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
                {g.segments.length > 1 || s.segment !== 'AMONT' ? (
                  <h4 className="gm-type-small-strong" style={{ margin: 0 }}>
                    {[t(`parcours.segment.${s.segment}`), ...plateformes(s.operations).map((p) => t(`parcours.plateforme.${p}`))].join(' · ')}
                  </h4>
                ) : null}
                <TableOperations operations={s.operations} selection={opId ?? dernier} onOuvrir={ouvrir} />
              </div>
            ))}
            </div>
          </section>
        )
      })}

      {ouverte ? <Tiroir key={ouverte.id} op={ouverte} dossier={dossier} onFermer={fermer} onOuvrirRang={ouvrirRang} /> : null}
    </div>
  )
}

function TableOperations({ operations, selection, onOuvrir }: { operations: OperationParcours[]; selection: string | null; onOuvrir: (id: string) => void }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const colonnes: Column<OperationParcours>[] = [
    {
      key: 'rang',
      header: t('parcoursOps.col.rang'),
      width: 64,
      render: (o) => (
        <Button variant="ghost" size="sm" aria-label={t('parcoursOps.ouvrir', { rang: o.rang, libelle: libelleOperation(t, o) })} onClick={(e) => { e.stopPropagation(); onOuvrir(o.id) }}>
          <span className="gm-mono">{o.rang}</span>
        </Button>
      ),
    },
    {
      key: 'libelle',
      header: t('parcoursOps.col.operation'),
      render: (o) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-type-body-strong">{libelleOperation(t, o)}</span>
          {o.oubliSansSymptome ? <StatusBadge status="crit" icon="triangle-alert" label={t('parcoursOps.marque.oubli')} /> : null}
          {o.cle ? <StatusBadge status="neutral" icon="key-round" label={t('parcoursOps.marque.cle')} /> : null}
          {o.secret ? <StatusBadge status="neutral" icon="lock" label={t('parcoursOps.marque.secret')} /> : null}
          {o.valeurReference ? <StatusBadge status="info" icon="scroll-text" label={t('parcoursOps.marque.valeur')} /> : null}
          {o.pivot ? <StatusBadge status="info" icon="flag" label={t('parcoursOps.marque.pivot')} /> : null}
          {o.grain === 'PHASE' ? <StatusBadge status="neutral" icon="file-text" label={t('parcours.grain.PHASE')} /> : null}
        </span>
      ),
    },
    { key: 'plateforme', header: t('parcoursOps.col.plateforme'), width: 170, render: (o) => (o.plateforme ? t(`parcours.plateforme.${o.plateforme}`) : '—') },
    { key: 'responsable', header: t('parcours.qualite.R'), width: 190, render: (o) => (o.raci.R ? t(`parcours.fonction.${o.raci.R.fonction}`) : t('parcoursOps.nonDesigne')) },
    {
      key: 'etat',
      header: t('parcoursOps.col.etat'),
      width: 200,
      render: (o) => (
        <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
          <StatusBadge status={TON_ETAT[o.etat]} label={t(`parcours.etatOp.${o.etat}`)} />
          {o.predecesseursNonFaits.length ? <span className="gm-field-help">{t('parcoursOps.attend', { rangs: o.predecesseursNonFaits.join(', ') })}</span> : null}
        </span>
      ),
    },
    {
      key: 'echeance',
      header: t('parcoursOps.col.echeance'),
      width: 130,
      render: (o) =>
        o.faiteLe ? (
          <span style={{ whiteSpace: 'nowrap' }}>{date(o.faiteLe)}</span>
        ) : (
          <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
            <span style={{ whiteSpace: 'nowrap' }}>{o.echeance ? date(o.echeance) : '—'}</span>
            {o.sla ? <StatusBadge status={tonSla[o.sla]} label={t(`parcours.sla.${o.sla}`)} /> : null}
          </span>
        ),
    },
  ]
  return <DataTable columns={colonnes} rows={operations} rowKey={(o) => o.id} selected={selection ? [selection] : []} onRowOpen={(o) => onOuvrir(o.id)} density="compact" />
}

function Tiroir({ op, dossier, onFermer, onOuvrirRang }: { op: OperationParcours; dossier: DossierLiaison; onFermer: () => void; onOuvrirRang: (rang: string) => void }) {
  const { t } = useTranslation()
  const onglets = [
    { key: 'execution', label: t('parcoursOps.tiroir.execution') },
    { key: 'detail', label: t('parcoursOps.tiroir.detail') },
    { key: 'raci', label: t('parcoursOps.tiroir.raci') },
    { key: 'preuve', label: t('parcoursOps.tiroir.preuve') },
  ]
  return (
    <Drawer
      type={t('parcoursOps.tiroir.type')}
      reference={op.rang}
      status={TON_ETAT[op.etat]}
      statusLabel={t(`parcours.etatOp.${op.etat}`)}
      title={libelleOperation(t, op)}
      tabs={onglets}
      tab="execution"
      size="lg"
      onClose={onFermer}
      render={(onglet) => (
        <div className="gm-col">
          {onglet === 'execution' ? <Execution op={op} dossier={dossier} onOuvrirRang={onOuvrirRang} /> : null}
          {onglet === 'detail' ? (
            <>
              <InfosOperation op={op} onOuvrir={onOuvrirRang} />
              {op.rang === '16' ? <Essais op={op} dossier={dossier} /> : null}
              {op.rang === '17' ? <Couverture dossier={dossier} /> : null}
            </>
          ) : null}
          {onglet === 'raci' ? <Raci op={op} /> : null}
          {onglet === 'preuve' ? <PreuveDeposee op={op} /> : null}
        </div>
      )}
    />
  )
}

function Execution({ op, dossier, onOuvrirRang }: { op: OperationParcours; dossier: DossierLiaison; onOuvrirRang: (rang: string) => void }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const permis = usePermis(op)
  const executeur = useExecuteurActions()
  const { constat } = useActionsOperation()
  const ailleurs = ongletDuGeste(op.rang) !== null || externe(op)
  const geste = !ailleurs && (permis.preuve || permis.livrable || permis.constater)
  const blocages = dossier.blocages.filter((b) => !b.leveLe)
  return (
    <div className="gm-col">
      <Alertes op={op} />
      <Essentiel op={op} />
      {ailleurs ? <GesteAilleurs op={op} /> : null}
      {op.rang === '12' && op.preuve?.valeurReference ? <PreuveDeposee op={op} /> : null}
      {op.etat === 'VERROUILLEE' ? <InfosOperation op={{ ...op, oubliSansSymptome: false, pivot: false }} onOuvrir={onOuvrirRang} /> : null}
      {op.etat === 'BLOQUEE' ? (
        <InlineAlert level="crit" title={t('parcoursOps.bloquee.titre')}>
          {blocages.length ? blocages.map((b) => t('parcoursOps.bloquee.texte', { motif: b.motif, action: b.prochaineAction, echeance: date(b.echeanceLevee) })).join(' ') : t('parcoursOps.bloquee.sansDetail')}
        </InlineAlert>
      ) : null}
      {!ailleurs && op.rang === '16' ? <Essais op={op} dossier={dossier} /> : null}
      {!ailleurs && permis.constater ? (
        <>
          <Couverture dossier={dossier} />
          <div className="gm-row">
            <Button variant="primary" icon="square-check" onClick={() => executeur.lancer(constat, op, `${op.rang} · ${libelleOperation(t, op)}`, 'parcours')}>{t(constat.label)}</Button>
          </div>
        </>
      ) : null}
      {!ailleurs && permis.preuve ? <FormulairePreuve op={op} /> : null}
      {!ailleurs && permis.livrable ? <FormulaireLivrable op={op} /> : null}
      {op.etat === 'EN_VALIDATION' ? (
        <>
          <PreuveDeposee op={op} />
          {permis.valider ? <BlocValidation op={op} /> : <InlineAlert level="info">{t('parcoursOps.enValidation', { qui: op.raci.V ? `${op.raci.V.nom} · ${t(`parcours.fonction.${op.raci.V.fonction}`)}` : t('parcoursOps.nonDesigne') })}</InlineAlert>}
        </>
      ) : null}
      {op.etat === 'FAITE' ? <InlineAlert level="info" title={t('parcours.etatOp.FAITE')}>{t('parcoursOps.faite', { date: date(op.faiteLe) })}</InlineAlert> : null}
      {!ailleurs && !geste && !permis.valider && ['A_FAIRE', 'EN_COURS'].includes(op.etat) ? (
        <InlineAlert level="info">{t('parcoursOps.aucuneAction', { qui: op.raci.R ? t(`parcours.fonction.${op.raci.R.fonction}`) : t('parcoursOps.nonDesigne') })}</InlineAlert>
      ) : null}
      {permis.attribuer ? (
        <section className="gm-col" aria-labelledby={`attr-${op.id}`} style={{ borderTop: 'var(--gm-border-1) solid var(--gm-color-border)', paddingTop: 'var(--gm-space-4)' }}>
          <h3 id={`attr-${op.id}`} className="gm-type-h3" style={{ margin: 0 }}>{t('parcoursOps.attribution.titre')}</h3>
          <FormulaireAttribution op={op} />
        </section>
      ) : null}
    </div>
  )
}
