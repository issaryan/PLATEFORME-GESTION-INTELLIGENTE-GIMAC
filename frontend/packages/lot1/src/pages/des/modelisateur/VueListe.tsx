import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, DataTable, Dialog, Icon, Select, Tag, type Column } from '@gimac/design-system'
import type { GrapheReferentiel, NoeudGraphe } from '../../../modele/homologation'
import type { SourceEtapes } from '../../../adaptateurs/modelisateur'

// Vue liste du modélisateur, équivalente au canevas pour le clavier et les lecteurs d'écran : une ligne par
// opération (rôles, chronologie, anomalies), et les mêmes gestes (modifier, relier, délier) par des boutons.

type Ligne = NoeudGraphe & Record<string, unknown>

/** Texte comparable : minuscules, sans accent. */
const normaliser = (x: string) => x.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

export type VueListeProps = {
  graphe: GrapheReferentiel
  /** Étapes de la version (D-89) : libellés de la colonne Étape. */
  etapes?: SourceEtapes
  lecture: boolean
  surlignes: Set<string>
  onOuvrir: (code: string) => void
  onRelier: (de: string, vers: string) => void
  onDelier: (de: string, vers: string) => void
}

/** Arcs d'une opération au clavier : prédécesseurs retirables, ajout d'un prédécesseur. */
function DialogueArcs({ noeud, graphe, onRelier, onDelier, onFermer }: { noeud: NoeudGraphe; graphe: GrapheReferentiel; onRelier: VueListeProps['onRelier']; onDelier: VueListeProps['onDelier']; onFermer: () => void }) {
  const { t } = useTranslation()
  const [choix, setChoix] = useState('')
  const possibles = graphe.noeuds.filter((n) => !n.fantome && n.code !== noeud.code && !noeud.predecesseurs.includes(n.code))
  return (
    <Dialog open id="gm-dialogue-arcs" size="md" title={t('modelisateur.arcs.titre', { code: noeud.code })} subtitle={noeud.libelle} onClose={onFermer}
      footer={<Button variant="ghost" onClick={onFermer}>{t('commun.fermer')}</Button>}>
      <div className="gm-col">
        <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('modelisateur.champ.predecesseurs')}</h3>
        {noeud.predecesseurs.length ? (
          <ul className="gm-col" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 'var(--gm-space-1)' }}>
            {noeud.predecesseurs.map((p) => (
              <li key={p} className="gm-row" style={{ justifyContent: 'space-between' }}>
                <span><span className="gm-mono">{p}</span> {graphe.noeuds.find((n) => n.code === p)?.libelle ?? ''}</span>
                <Button size="sm" variant="ghost" icon="x" onClick={() => onDelier(p, noeud.code)}>{t('modelisateur.arcs.retirer', { de: p, vers: noeud.code })}</Button>
              </li>
            ))}
          </ul>
        ) : <p className="gm-field-help" style={{ margin: 0 }}>{t('modelisateur.arcs.aucun')}</p>}
        <div className="gm-row" style={{ alignItems: 'flex-end' }}>
          <Select name="nouveauPredecesseur" label={t('modelisateur.arcs.ajouter')} value={choix} onChange={setChoix}
            options={[{ value: '', label: t('modelisateur.aucun') }, ...possibles.map((n) => ({ value: n.code, label: `${n.code} · ${n.libelle}` }))]} />
          <Button icon="plus" disabled={!choix} disabledReason={t('modelisateur.arcs.choisir')} onClick={() => { onRelier(choix, noeud.code); setChoix('') }}>{t('modelisateur.arcs.relier')}</Button>
        </div>
      </div>
    </Dialog>
  )
}

export function VueListe(p: VueListeProps) {
  const { t } = useTranslation()
  const [arcs, setArcs] = useState<string | null>(null)
  // Liste paginée (DataTable) et filtrable par code ou libellé : une opération se retrouve sans parcourir les pages.
  const [filtre, setFiltre] = useState('')
  const fonction = (f: string | null) => (f ? t(`parcours.fonction.${f}`, { defaultValue: f }) : '')
  const colonnes: Column<Ligne>[] = [
    { key: 'code', header: t('modelisateur.champ.code'), type: 'mono', width: 80 },
    { key: 'libelle', header: t('modelisateur.champ.libelle'), render: (r) => (r.fantome ? <span className="gm-row"><span className="gm-field-help">{r.libelle}</span><Tag>{t('modelisateur.fantome')}</Tag></span> : r.libelle) },
    { key: 'etape', header: t('modelisateur.champ.etape'), render: (r) => (r.etape ? (p.etapes?.version ? p.etapes.etapes.find((e) => e.code === r.etape)?.libelle ?? r.etape : t(`parcours.etape.${r.etape}`, { defaultValue: r.etape })) : '') },
    { key: 'segment', header: t('modelisateur.champ.segment'), render: (r) => (r.segment ? t(`parcours.segment.${r.segment}`) : '') },
    { key: 'responsable', header: 'R', render: (r) => fonction(r.responsable) },
    { key: 'validateur', header: 'V', render: (r) => fonction(r.validateur) },
    { key: 'predecesseurs', header: t('modelisateur.champ.predecesseurs'), render: (r) => <span className="gm-row" style={{ flexWrap: 'wrap', gap: 2 }}>{(r.predecesseurs ?? []).map((x) => <Tag key={x}>{x}</Tag>)}</span> },
    { key: 'delaiJours', header: t('modelisateur.champ.delai'), render: (r) => t('modelisateur.carte.delai', { n: r.delaiJours ?? 0 }) },
    { key: 'debutAuPlusTot', header: t('modelisateur.champ.fin'), render: (r) => (r.finAuPlusTot === null ? '' : t('modelisateur.jourN', { n: r.finAuPlusTot })) },
    {
      key: 'anomalies', header: t('modelisateur.colonne.anomalies'),
      render: (r) => (r.anomalies?.length ? <span className="gm-mod-carte-anomalies">{r.anomalies.map((a) => t(`modelisateur.anomalie.${a}`)).join(' ; ')}</span> : r.critique ? t('modelisateur.carte.critique') : ''),
    },
    {
      key: 'actions', header: t('modelisateur.colonne.actions'), type: 'actions',
      // Fantôme (D-89) : ni consultation ni arcs.
      render: (r) => (r.fantome ? null : (
        <>
          <Button size="sm" variant="ghost" icon={p.lecture ? 'eye' : 'pen-line'} aria-label={t(p.lecture ? 'modelisateur.action.consulterOp' : 'modelisateur.action.modifierOp', { code: r.code })} onClick={() => p.onOuvrir(r.code)}>
            {p.lecture ? t('modelisateur.action.consulter') : t('modelisateur.action.modifier')}
          </Button>
          {!p.lecture ? (
            <Button size="sm" variant="ghost" icon="route" aria-label={t('modelisateur.arcs.titre', { code: r.code })} onClick={() => setArcs(r.code)}>{t('modelisateur.action.arcs')}</Button>
          ) : null}
        </>
      )),
    },
  ]
  const noeud = arcs ? p.graphe.noeuds.find((n) => n.code === arcs) : undefined
  const f = normaliser(filtre.trim())
  const lignes = (f ? p.graphe.noeuds.filter((n) => normaliser(`${n.code} ${n.libelle}`).includes(f)) : p.graphe.noeuds) as Ligne[]
  return (
    <div data-testid="modelisateur-liste">
      <div className="gm-input-wrap" style={{ maxWidth: 420, marginBottom: 'var(--gm-space-2)' }}>
        <input type="search" className="gm-input" aria-label={t('modelisateur.liste.filtre')} placeholder={t('modelisateur.liste.filtre')} value={filtre} onChange={(e) => setFiltre(e.target.value)} />
        <span className="gm-input-suffix"><Icon name="search" /></span>
      </div>
      {/* Nouveau filtre : la table repart de sa première page (ou de celle de la ligne mise en évidence). */}
      <DataTable<Ligne> key={filtre.trim()} columns={colonnes} rows={lignes} rowKey={(r) => r.code} state="ok" density="compact" selected={[...p.surlignes]}
        emptyText={t('referentiel.operations.aucune')} />
      {noeud ? <DialogueArcs noeud={noeud} graphe={p.graphe} onRelier={p.onRelier} onDelier={p.onDelier} onFermer={() => setArcs(null)} /> : null}
    </div>
  )
}
