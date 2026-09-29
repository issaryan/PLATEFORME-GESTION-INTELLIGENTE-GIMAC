import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { InlineAlert } from '../../conteneurs/InlineAlert/InlineAlert'
import { Button } from '../../actions/Button/Button'
import { useLibelle } from '../../i18n/LangueProvider'

export interface ShiftHandoverPanelProps {
  shift: string
  summary: string
  outgoing?: string
  incoming?: string
  visa?: string
  /** Horodatage de chaque signature apposée. */
  signatures?: { out?: string; in?: string; visa?: string }
}

// Rapport de relève de quart (§7.7, DES-19) : responsabilité transférée à la contresignature.
export function ShiftHandoverPanel(p: ShiftHandoverPanelProps) {
  const t = useLibelle()
  const sig = p.signatures || {}
  const rows: ReadonlyArray<readonly ['out' | 'in' | 'visa', string, string]> = [
    ['out', t('releve.sortante'), p.outgoing || t('releve.chefSortant')],
    ['in', t('releve.entrante'), p.incoming || t('releve.chefEntrant')],
    ['visa', t('releve.visa'), p.visa || t('quitus.signataire.tech')],
  ]
  return (
    <div className="gm-card gm-handover">
      <div className="gm-card-header">
        <h2 className="gm-card-title">{t('releve.titre')}</h2>
        <span className="gm-field-help gm-num">{p.shift}</span>
      </div>
      <div className="gm-card-body gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        <div className="gm-field-help">{p.summary}</div>
        <ol className="gm-quitus">
          {rows.map((r, i) => {
            const s = sig[r[0]]
            return (
              <li key={r[0]} className={cx('gm-quitus-step', s ? 'gm-quitus-signed' : 'gm-quitus-todo')}>
                <span className="gm-step-num">{s ? <Icon name="check" size={14} /> : i + 1}</span>
                <div style={{ flex: 1 }}>
                  <div className="gm-banner-title">{r[1]}</div>
                  <div className="gm-field-help">
                    {r[2]}
                    {s ? ' · ' + s : ''}
                  </div>
                </div>
                {s ? <StatusBadge status="ok" label={t('releve.signee')} icon="pen-line" /> : <StatusBadge status="neutral" label={t('releve.attente')} />}
              </li>
            )
          })}
        </ol>
        <InlineAlert level={sig.in ? 'info' : 'warn'} title={sig.in ? t('releve.transferee') : t('releve.nonTransferee')}>
          {sig.in ? t('releve.scelle') : t('releve.condition')}
        </InlineAlert>
      </div>
      <div className="gm-card-footer">
        <Button variant="secondary" icon="file-text">{t('releve.generer')}</Button>
        <Button variant="primary" icon="pen-line" disabled={!!sig.in} disabledReason={t('releve.deja')}>
          {sig.out ? t('releve.contresigner') : t('releve.signer')}
        </Button>
      </div>
    </div>
  )
}
