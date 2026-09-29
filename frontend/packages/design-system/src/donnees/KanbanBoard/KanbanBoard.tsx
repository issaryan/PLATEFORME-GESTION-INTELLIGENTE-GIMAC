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
}

export interface KanbanBoardProps {
  label?: string
  columns: KanbanColumn[]
}

// Tableau Kanban avec limites d'en-cours (§7.6, DES-23).
export function KanbanBoard(p: KanbanBoardProps) {
  const t = useLibelle()
  return (
    <div className="gm-kanban" role="group" aria-label={p.label || t('kanban.label')}>
      {(p.columns || []).map((c) => {
        const n = c.cards.length
        const over = c.limit && n > c.limit
        const full = c.limit && n >= c.limit
        return (
          <section key={c.key} className={cx('gm-kanban-col', over && 'gm-kanban-over')} aria-label={c.label}>
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
              {c.cards.map((card) => (
                <TicketCard key={card.id} {...card} />
              ))}
              {n === 0 ? <div className="gm-kanban-empty">{t('kanban.aucune')}</div> : null}
            </div>
            <footer className="gm-kanban-foot">
              <Button size="sm" variant="ghost" icon="plus" disabled={!!(full && !c.allowAdd)} disabledReason={t('kanban.atteinte')}>{t('kanban.ajouter')}</Button>
            </footer>
          </section>
        )
      })}
    </div>
  )
}
