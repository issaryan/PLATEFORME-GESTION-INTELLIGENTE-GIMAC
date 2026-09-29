import { Icon } from '../../icones/Icon/Icon'
import { Tag } from '../../affichage/Tag/Tag'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface LogbookEntry {
  at: string
  actor: string
  kind?: string
  text: string
  /** Empreinte de chaînage : chaque entrée scelle la précédente. */
  hash: string
}

export interface LogbookEntryListProps {
  entries?: LogbookEntry[]
  draft?: string
  onDraft?: (v: string) => void
  onSubmit?: () => void
}

// Main courante chaînée d'une vacation ou d'une cellule de crise (§7.7, gabarit G8).
export function LogbookEntryList(p: LogbookEntryListProps) {
  const t = useLibelle()
  return (
    <div className="gm-logbook">
      <ol className="gm-logbook-list" aria-label={t('main.label')}>
        {(p.entries || []).map((e, i) => (
          <li key={i} className="gm-logbook-entry">
            <div className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
              <span className="gm-mono gm-field-help">{e.at}</span>
              <strong>{e.actor}</strong>
              {e.kind ? <Tag>{e.kind}</Tag> : null}
              <span className="gm-field-help" style={{ marginLeft: 'auto' }}>
                <Icon name="lock" size={14} /> {e.hash}
              </span>
            </div>
            <div>{e.text}</div>
          </li>
        ))}
      </ol>
      <div className="gm-logbook-input">
        <input type="text" className="gm-input" aria-label={t('main.nouvelle')} value={p.draft || ''} onChange={(e) => p.onDraft && p.onDraft(e.target.value)} />
        <Button variant="primary" icon="send" onClick={p.onSubmit}>{t('main.consigner')}</Button>
      </div>
    </div>
  )
}
