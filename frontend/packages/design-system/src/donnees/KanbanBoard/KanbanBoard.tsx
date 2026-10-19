import { useState } from 'react'
import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'
import { TicketCard, type TicketCardProps } from '../../domaine/TicketCard/TicketCard'
import { useLibelle } from '../../i18n/LangueProvider'

export interface KanbanColumn {
  key: string
  label: string
  /** Limite d'en-cours affichée (« 4/5 »). */
  limit?: number
  allowAdd?: boolean
  cards: TicketCardProps[]
  /** Ajout d'une carte dans la colonne ; absent : bouton inerte (catalogue) ou masqué si `masquerAjout`. */
  onAdd?: () => void
}

export interface KanbanBoardProps {
  label?: string
  columns: KanbanColumn[]
  /**
   * Déplacement d'une carte vers une colonne (D-113) : glisser-déposer à la souris, Alt + flèche gauche ou droite au
   * clavier. Absent : tableau en lecture.
   */
  onMove?: (cardId: string, toColumn: string) => void
  /** Déplacement permis (transitions renvoyées par le service) ; absent : toute autre colonne. */
  canMove?: (cardId: string, toColumn: string) => boolean
  /** Ouverture de la fiche d'une carte (clic ou Entrée). */
  onOpen?: (cardId: string) => void
  /** Sans `onAdd`, le bouton d'ajout est retiré au lieu d'être inerte. */
  masquerAjout?: boolean
  /** Cartes par page dans chaque colonne (D-113) ; au-delà, la colonne est paginée. */
  pageSize?: number
}

// Tableau Kanban avec limites d'en-cours (§7.6, DES-23) ; cartes glissables et déplaçables au clavier (D-113).
export function KanbanBoard(p: KanbanBoardProps) {
  const t = useLibelle()
  const [survol, setSurvol] = useState<string | null>(null)
  const [annonce, setAnnonce] = useState('')
  const [pages, setPages] = useState<Record<string, number>>({})
  const taille = p.pageSize ?? 8
  const cols = p.columns || []
  const permis = (id: string, vers: string, de: string) => vers !== de && (!p.canMove || p.canMove(id, vers))
  const deplacer = (id: string, de: string, vers: string) => {
    if (!p.onMove || !permis(id, vers, de)) return
    p.onMove(id, vers)
    setAnnonce(t('kanban.deplacee', { carte: id, colonne: cols.find((c) => c.key === vers)?.label ?? vers }))
  }
  /** Colonne permise la plus proche dans un sens (Alt + flèche). */
  const voisine = (id: string, de: string, sens: 1 | -1) => {
    const i = cols.findIndex((c) => c.key === de)
    for (let j = i + sens; j >= 0 && j < cols.length; j += sens) if (permis(id, cols[j].key, de)) return cols[j].key
    return null
  }
  return (
    <div className="gm-kanban" role="group" aria-label={p.label || t('kanban.label')}>
      {cols.map((c) => {
        const n = c.cards.length
        const over = c.limit && n > c.limit
        const full = c.limit && n >= c.limit
        const nbPages = Math.max(1, Math.ceil(n / taille))
        const page = Math.min(pages[c.key] ?? 1, nbPages)
        const visibles = c.cards.slice((page - 1) * taille, page * taille)
        const aller = (x: number) => setPages((o) => ({ ...o, [c.key]: x }))
        return (
          <section key={c.key} className={cx('gm-kanban-col', over && 'gm-kanban-over', survol === c.key && 'gm-kanban-cible')} aria-label={c.label}
            onDragOver={p.onMove ? (e) => {
              const id = e.dataTransfer.types.includes('text/plain') ? survolCarte.current : null
              if (id && permis(id.carte, c.key, id.colonne)) { e.preventDefault(); setSurvol(c.key) }
            } : undefined}
            onDragLeave={() => setSurvol((x) => (x === c.key ? null : x))}
            onDrop={p.onMove ? (e) => {
              e.preventDefault()
              setSurvol(null)
              const id = survolCarte.current
              if (id) deplacer(id.carte, id.colonne, c.key)
              survolCarte.current = null
            } : undefined}>
            <header className="gm-kanban-head">
              <strong>{c.label}</strong>
              {c.limit ? (
                <span className={cx('gm-kanban-wip gm-num', over && 'gm-late')}>
                  {n + '/' + c.limit}
                  {over ? <Icon name="octagon-alert" size={14} /> : null}
                </span>
              ) : (
                <span className="gm-field-help gm-num">{n}</span>
              )}
            </header>
            {over ? <div className="gm-kanban-notice">{t('kanban.depassee')}</div> : null}
            <div className="gm-kanban-cards">
              {visibles.map((card) =>
                p.onMove || p.onOpen ? (
                  <div key={card.id} className="gm-kanban-carte" draggable={!!p.onMove}
                    aria-keyshortcuts={p.onMove ? 'Alt+ArrowLeft Alt+ArrowRight' : undefined}
                    title={p.onMove ? t('kanban.aideClavier') : undefined}
                    onDragStart={(e) => { survolCarte.current = { carte: card.id, colonne: c.key }; e.dataTransfer.setData('text/plain', card.id); e.dataTransfer.effectAllowed = 'move' }}
                    onDragEnd={() => setSurvol(null)}
                    onClick={p.onOpen ? () => p.onOpen?.(card.id) : undefined}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && p.onOpen) { e.preventDefault(); p.onOpen(card.id) }
                      if (p.onMove && e.altKey && (e.key === 'ArrowRight' || e.key === 'ArrowLeft')) {
                        e.preventDefault()
                        const vers = voisine(card.id, c.key, e.key === 'ArrowRight' ? 1 : -1)
                        if (vers) deplacer(card.id, c.key, vers)
                      }
                    }}>
                    <TicketCard {...card} />
                  </div>
                ) : (
                  <TicketCard key={card.id} {...card} />
                ),
              )}
              {n === 0 ? <div className="gm-kanban-empty">{t('kanban.aucune')}</div> : null}
            </div>
            {nbPages > 1 ? (
              <nav className="gm-kanban-pages" aria-label={t('kanban.pagination', { colonne: c.label })}>
                <Button size="sm" variant="ghost" icon="arrow-left" iconOnly aria-label={t('kanban.pagePrecedente')} disabled={page <= 1} disabledReason={t('kanban.premierePage')} onClick={() => aller(page - 1)} />
                <span className="gm-field-help gm-num">{t('kanban.page', { page, pages: nbPages })}</span>
                <Button size="sm" variant="ghost" icon="arrow-right" iconOnly aria-label={t('kanban.pageSuivante')} disabled={page >= nbPages} disabledReason={t('kanban.dernierePage')} onClick={() => aller(page + 1)} />
              </nav>
            ) : null}
            {c.onAdd || !p.masquerAjout ? (
              <footer className="gm-kanban-foot">
                <Button size="sm" variant="ghost" icon="plus" disabled={!!(full && !c.allowAdd)} disabledReason={t('kanban.atteinte')} onClick={c.onAdd}>{t('kanban.ajouter')}</Button>
              </footer>
            ) : null}
          </section>
        )
      })}
      <span className="gm-sr-only" role="status" aria-live="polite">{annonce}</span>
    </div>
  )
}

/** Carte en cours de glissement : les événements de survol ne donnent pas accès aux données transférées. */
const survolCarte: { current: { carte: string; colonne: string } | null } = { current: null }
