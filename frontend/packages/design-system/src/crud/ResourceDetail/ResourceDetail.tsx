import { useEffect, useState } from 'react'
import type { TimelineEvent } from '../../lib/types'
import { IconButton } from '../../actions/IconButton/IconButton'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { KeyValueList } from '../../affichage/KeyValueList/KeyValueList'
import { Timeline } from '../../affichage/Timeline/Timeline'
import { EmptyState } from '../../affichage/EmptyState/EmptyState'
import { CorrelationIdChip } from '../../affichage/CorrelationIdChip/CorrelationIdChip'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { StateMachineBar } from '../../donnees/StateMachineBar/StateMachineBar'
import type { ResourceAction, ResourceDescriptor } from '../descripteur'
import { actionButton, allowed, champ, renderField, statusOf } from '../outils'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ResourceDetailProps<T> {
  descriptor: ResourceDescriptor<T>
  row: T
  /** « page » : fiche à onglets (G3) ; « drawer » : corps du tiroir de ResourceList. */
  mode?: 'page' | 'drawer'
  tab?: string
  permissions?: string[]
  timeline?: TimelineEvent[]
  /** Identifiants de corrélation des dernières entrées du registre. */
  onAction?: (a: ResourceAction<T>, row: T | null) => void
  /** Onglet contrôlé : appelé à chaque changement ; si `tab` change, l'onglet suit. */
  onTabChange?: (key: string) => void
  /** États parcourus, transmis à la barre d'états. */
  traversed?: string[]
  /** false : pas d'onglet Audit. */
  audit?: string[] | false
}

const aucuneAction = () => {}

// Fiche à onglets : en-tête, statut, champs inDetail, onglets du descripteur, chronologie, audit (§8.3).
export function ResourceDetail<T extends object>(p: ResourceDetailProps<T>) {
  const t = useLibelle()
  const desc = p.descriptor
  const row = (p.row || {}) as T
  const perms = p.permissions
  const mode = p.mode || 'page'
  const fields = desc.fields.filter((f) => f.inDetail && (!f.permission || !perms || perms.indexOf(f.permission) >= 0))
  const tabs = [{ key: 'detail', label: t('drawer.detail') }]
    .concat((desc.tabs || []).map((o) => ({ key: o.key, label: o.label })))
    .concat(desc.timeline ? [{ key: 'chronologie', label: t('drawer.chronologie') }] : [])
    .concat(p.audit === false ? [] : [{ key: 'audit', label: t('drawer.audit') }])
  const [tab, setTabState] = useState(p.tab || 'detail')
  useEffect(() => {
    if (p.tab) setTabState(p.tab)
  }, [p.tab])
  const setTab = (k: string) => {
    setTabState(k)
    if (p.onTabChange) p.onTabChange(k)
  }
  const st = statusOf(desc, row)
  const acts = desc.actions.filter((a) => a.scope === 'row' && allowed(a, row, perms))
  const kv = <KeyValueList items={fields.map((f) => ({ key: f.label, value: renderField(f, champ(row, f.name), row, desc, t), mono: f.kind === 'mono' }))} />
  const dico = desc.statusDictionary
  const machine =
    dico && desc.statusField ? (
      <StateMachineBar
        label={t('crud.statut')}
        states={Object.keys(dico).map((k) => ({ key: k, label: dico[k].label }))}
        current={String(champ(row, desc.statusField))}
        allowed={champ(row, 'transitionsPermises') as string[] | undefined}
        traversed={p.traversed}
      />
    ) : null
  if (mode === 'drawer') return <div className="gm-col">{machine}{kv}</div>
  const run = { run: p.onAction || aucuneAction }
  let contenu
  if (tab === 'detail') contenu = kv
  else if (tab === 'chronologie') contenu = <Timeline events={p.timeline || []} />
  else if (tab === 'audit')
    contenu = <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>{(p.audit || []).map((c: string) => <CorrelationIdChip key={c} id={c} />)}</div>
  else {
    const onglet = (desc.tabs || []).filter((x) => x.key === tab)[0]
    contenu = onglet && onglet.render ? onglet.render(row) : <EmptyState title={t('commun.aucunElement')} text={t('crud.ongletVide')} />
  }
  return (
    <div className="gm-resource">
      <header className="gm-objhead">
        <div className="gm-objhead-main">
          <div className="gm-row">
            <span className="gm-field-help">{desc.label.singular}</span>
            <span className="gm-mono">{String(champ(row, desc.idField))}</span>
          </div>
          <h1 className="gm-heading-display">{String(champ(row, desc.titleField))}</h1>
          <div className="gm-row">{desc.statusField ? <StatusBadge status={st.status} label={st.label} /> : null}</div>
        </div>
        <div className="gm-row">
          {acts.filter((a) => a.variant !== 'primary').slice(0, 2).map((a) => actionButton(a, row, run, 'md'))}
          {acts.filter((a) => a.variant === 'primary').slice(0, 1).map((a) => actionButton(a, row, run, 'md'))}
          {acts.length > 3 ? <IconButton variant="ghost" icon="menu" label={t('crud.plusActions')} /> : null}
        </div>
      </header>
      {machine ? <div style={{ margin: 'var(--gm-space-4) 0' }}>{machine}</div> : null}
      <Tabs items={tabs} value={tab} onChange={setTab} />
      <div style={{ paddingTop: 'var(--gm-space-4)' }}>{contenu}</div>
    </div>
  )
}
