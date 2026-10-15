import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Banner, Button, DataTable, Drawer, InlineAlert, Select, StatCard, StatusBadge, Switch, TextField, type Column } from '@gimac/design-system'
import { useChronologieParcours, useMesFonctions, useOperationsParcours } from '../../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../../hooks/referentiels'
import { ETAPES, type DossierLiaison, type OperationParcours } from '../../../../modele/parcours'
import { Donnees } from '../../liaisons/communs'
import { Alertes, Couverture, Essais, Essentiel, GesteAilleurs, InfosOperation, PreuveDeposee, Raci } from '../operations/Detail'
import { BlocValidation, FormulaireAttribution, FormulaireLivrable, FormulairePreuve, LivrablesAttendus, Renvois, Signatures, usePermis } from '../operations/Execution'
import { useActionsOperation } from '../operations/actions'
import { Chronologie, LivrablesEtPreuves, SignaturesOperation } from '../operations/Preuves'
import { useExecuteurActions } from '../../../../ressources/actions'
import { compter, etapeDe, externe, filtrer, grouper, ongletDuGeste, plateformes, TON_ETAT, VUES, type Filtres, type Vue } from '../operations/regles'
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
  // Fonctions de l'utilisateur servies par MS-HOMOLOGATION (D-94 §6.5).
  const mes = useMesFonctions()
  const fonctions = useMemo(() => (mes.data ?? []).map((f) => f.code), [mes.data])
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
  // Étapes réelles de la version (D-93) : ordre et libellés configurés ; les onze étapes canoniques à défaut.
  const etapesVersion = useMemo(() => [...(dossier.etapesVersion ?? [])].sort((a, b) => a.ordre - b.ordre), [dossier.etapesVersion])
  const ordre = etapesVersion.length ? etapesVersion.map((e) => e.code) : ETAPES
  const libelleEtape = (code: string) => etapesVersion.find((e) => e.code === code)?.libelle ?? t(`parcours.etape.${code}`, { defaultValue: code })
  const groupes = grouper(visibles, ordre)
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
  const etapesPresentes = [...ordre, ...ops.map(etapeDe).filter((e) => !ordre.includes(e))].filter((e, i, l) => l.indexOf(e) === i && ops.some((o) => etapeDe(o) === e))
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
          <Select label={t('parcoursOps.filtre.etape')} value={filtres.etape} onChange={(v) => maj('etape', v)} options={[{ value: '', label: t('parcoursOps.etape.toutes') }, ...etapesPresentes.map((e) => ({ value: e, label: libelleEtape(e) }))]} />
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
        const etape = etapesVersion.find((e) => e.code === g.etape) ?? dossier.etapes.find((e) => e.code === g.etape)
        const rangEtape = dossier.etapes.find((e) => e.code === g.etape)?.rang
        // Une étape close est repliée par défaut (sauf filtre actif ou opération ouverte) : l'attention va aux étapes en cours.
        const repliee = etape?.etat === 'CLOSE' && !filtre && !deplies.includes(g.etape) && !g.segments.some((x) => x.operations.some((o) => o.id === opId || o.id === dernier))
        const nb = g.segments.reduce((k, x) => k + x.operations.length, 0)
        return (
          <section key={g.etape} className="gm-col" style={{ gap: 'var(--gm-space-3)' }} aria-labelledby={`ops-${g.etape}`}>
            <div className="gm-row" style={{ justifyContent: 'space-between' }}>
              <h3 id={`ops-${g.etape}`} className="gm-type-h3" style={{ margin: 0 }}>
                {etapesVersion.length ? libelleEtape(g.etape) : rangEtape ? t('parcoursOps.etapeN', { n: rangEtape, etape: libelleEtape(g.etape) }) : libelleEtape(g.etape)}
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

      {ouverte ? <Tiroir key={ouverte.id} op={ouverte} dossier={dossier} onFermer={fermer} onOuvrirRang={ouvrirRang} libelleDe={(r) => { const x = ops.find((o) => o.rang === r); return x ? libelle(x) : r }} /> : null}
    </div>
  )
}

function TableOperations({ operations, selection, onOuvrir }: { operations: OperationParcours[]; selection: string | null; onOuvrir: (id: string) => void }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const fonctions = useFonctionsCatalogue()
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
    {
      key: 'responsable', header: t('parcours.qualite.R'), width: 190,
      render: (o) => (o.raci.R ? <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}><span>{fonctions.libelle(o.raci.R.fonction)}</span>{o.raci.R.aAffecter ? <StatusBadge status="warn" label={t('parcoursOps.aAffecterCourt')} /> : null}</span> : t('parcoursOps.nonDesigne')),
    },
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

function Tiroir({ op, dossier, onFermer, onOuvrirRang, libelleDe }: { op: OperationParcours; dossier: DossierLiaison; onFermer: () => void; onOuvrirRang: (rang: string) => void; libelleDe: (rang: string) => string }) {
  const { t } = useTranslation()
  // D-109 : livrables et preuves (état, pièce vérifiable), signatures (fonction, ordre, date), chronologie réelle.
  const onglets = [
    { key: 'execution', label: t('parcoursOps.tiroir.execution') },
    { key: 'livrables', label: t('preuves.onglet.livrables') },
    { key: 'signatures', label: t('preuves.onglet.signatures') },
    { key: 'chronologie', label: t('preuves.onglet.chronologie') },
    { key: 'detail', label: t('parcoursOps.tiroir.detail') },
    { key: 'raci', label: t('parcoursOps.tiroir.raci') },
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
          {onglet === 'execution' ? <Execution op={op} dossier={dossier} onOuvrirRang={onOuvrirRang} libelleDe={libelleDe} /> : null}
          {onglet === 'detail' ? (
            <>
              <InfosOperation op={op} onOuvrir={onOuvrirRang} libelleDe={libelleDe} />
              {finEssais(op) ? <Essais op={op} dossier={dossier} /> : null}
              {op.geste === 'CONSTATER_COUVERTURE' ? <Couverture dossier={dossier} /> : null}
            </>
          ) : null}
          {onglet === 'raci' ? <Raci op={op} /> : null}
          {onglet === 'livrables' ? (
            <>
              <LivrablesEtPreuves op={op} />
              {op.preuve && !op.reprise ? <PreuveDeposee op={op} /> : null}
            </>
          ) : null}
          {onglet === 'signatures' ? <SignaturesOperation op={op} /> : null}
          {onglet === 'chronologie' ? <ChronologieOperation op={op} /> : null}
        </div>
      )}
    />
  )
}

/** Chronologie réelle d'une opération (D-109) : événements de la liaison qui la concernent. */
function ChronologieOperation({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const q = useChronologieParcours(op.liaisonId)
  return (
    <Donnees requete={q} vide={t('preuves.chrono.vide')}>
      {(l) => <Chronologie evenements={l.filter((e) => e.operationId === op.id)} avecOperation={false} />}
    </Donnees>
  )
}

/** Fin des essais (vue ESSAIS, particularité FIN_ESSAIS, D-93) : état des essais service par service. */
const finEssais = (op: OperationParcours) => op.vue === 'ESSAIS' && op.particularite === 'FIN_ESSAIS'

function Execution({ op, dossier, onOuvrirRang, libelleDe }: { op: OperationParcours; dossier: DossierLiaison; onOuvrirRang: (rang: string) => void; libelleDe: (rang: string) => string }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const permis = usePermis(op)
  const executeur = useExecuteurActions()
  const { constat } = useActionsOperation()
  const fonctions = useFonctionsCatalogue()
  const ailleurs = ongletDuGeste(op) !== null || externe(op, fonctions.externes)
  const geste = !ailleurs && (permis.preuve || permis.livrable || permis.constater)
  const blocages = dossier.blocages.filter((b) => !b.leveLe)
  return (
    <div className="gm-col">
      <Alertes op={op} />
      <Essentiel op={op} />
      <Renvois op={op} />
      {ailleurs ? <GesteAilleurs op={op} /> : null}
      {op.vue === 'COORDONNEES' && op.preuve?.valeurReference ? <PreuveDeposee op={op} /> : null}
      {op.etat === 'VERROUILLEE' ? <InfosOperation op={{ ...op, oubliSansSymptome: false, pivot: false }} onOuvrir={onOuvrirRang} libelleDe={libelleDe} /> : null}
      {op.etat === 'BLOQUEE' ? (
        <InlineAlert level="crit" title={t('parcoursOps.bloquee.titre')}>
          {blocages.length ? blocages.map((b) => t('parcoursOps.bloquee.texte', { motif: b.motif, action: b.prochaineAction, echeance: date(b.echeanceLevee) })).join(' ') : t('parcoursOps.bloquee.sansDetail')}
        </InlineAlert>
      ) : null}
      {!ailleurs && finEssais(op) ? <Essais op={op} dossier={dossier} /> : null}
      <LivrablesAttendus op={op} />
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
          {permis.valider ? <BlocValidation op={op} renvoyer={permis.renvoyer} /> : <InlineAlert level="info">{t('parcoursOps.enValidation', { qui: op.raci.V ? (op.raci.V.aAffecter ? t('parcoursOps.aAffecter', { fonction: fonctions.libelle(op.raci.V.fonction) }) : `${op.raci.V.nom} · ${fonctions.libelle(op.raci.V.fonction)}`) : t('parcoursOps.nonDesigne') })}</InlineAlert>}
        </>
      ) : null}
      {op.etat === 'EN_SIGNATURE' ? <PreuveDeposee op={op} /> : null}
      <Signatures op={op} />
      {op.etat === 'FAITE' ? <InlineAlert level="info" title={t('parcours.etatOp.FAITE')}>{t('parcoursOps.faite', { date: date(op.faiteLe) })}</InlineAlert> : null}
      {!ailleurs && !geste && !permis.valider && ['A_FAIRE', 'EN_COURS'].includes(op.etat) ? (
        <InlineAlert level="info">{t('parcoursOps.aucuneAction', { qui: op.raci.R ? fonctions.libelle(op.raci.R.fonction) : t('parcoursOps.nonDesigne') })}</InlineAlert>
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
