import { useState } from 'react'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { useLibelle } from '../../i18n/LangueProvider'

export interface DiffLine {
  ln?: number
  rn?: number
  type?: 'add' | 'del' | 'mod'
  text?: string
  left?: string
  right?: string
}

export interface DiffViewProps {
  leftTitle: string
  rightTitle: string
  lines: DiffLine[]
  mode?: 'split' | 'unified'
}

function classe(l: DiffLine): string {
  return l.type === 'add' ? 'gm-diff-add' : l.type === 'del' ? 'gm-diff-del' : l.type === 'mod' ? 'gm-diff-mod' : ''
}

// Comparaison de deux versions, côte à côte ou unifiée, avec légende textuelle (§7.6).
export function DiffView(p: DiffViewProps) {
  const t = useLibelle()
  const lines = p.lines || []
  const [mode, setMode] = useState<string>(p.mode || 'split')
  return (
    <div className="gm-diff">
      <div className="gm-dag-toolbar">
        <span>
          <strong>{p.leftTitle}</strong>
          <span className="gm-field-help"> {t('diff.contre')} </span>
          <strong>{p.rightTitle}</strong>
        </span>
        <span style={{ flex: 1 }} />
        <span className="gm-row" style={{ gap: 'var(--gm-space-3)', fontSize: 12 }}>
          <span className="gm-diff-key gm-diff-add">{t('diff.ajoute')}</span>
          <span className="gm-diff-key gm-diff-del">{t('diff.retire')}</span>
          <span className="gm-diff-key gm-diff-mod">{t('diff.modifie')}</span>
        </span>
        <Tabs items={[{ key: 'split', label: t('diff.coteACote') }, { key: 'unified', label: t('diff.unifie') }]} value={mode} onChange={setMode} />
      </div>
      <table className="gm-diff-table" role="table">
        {mode === 'split' ? (
          <tbody>
            {lines.map((l, i) => (
              <tr key={i} className={classe(l)}>
                <td className="gm-diff-n gm-num">{l.type === 'add' ? '' : l.ln}</td>
                <td className="gm-diff-code">{l.type === 'add' ? '' : l.left !== undefined ? l.left : l.text}</td>
                <td className="gm-diff-n gm-num">{l.type === 'del' ? '' : l.rn}</td>
                <td className="gm-diff-code">{l.type === 'del' ? '' : l.right !== undefined ? l.right : l.text}</td>
              </tr>
            ))}
          </tbody>
        ) : (
          <tbody>
            {lines.map((l, i) => {
              const sign = l.type === 'add' ? '+' : l.type === 'del' ? '−' : l.type === 'mod' ? '~' : ' '
              return (
                <tr key={i} className={classe(l)}>
                  <td className="gm-diff-n gm-num">{l.ln || ''}</td>
                  <td className="gm-diff-n gm-num">{l.rn || ''}</td>
                  <td className="gm-diff-code">{sign + ' ' + (l.type === 'mod' ? l.right : l.left !== undefined ? l.left : l.right !== undefined ? l.right : l.text)}</td>
                </tr>
              )
            })}
          </tbody>
        )}
      </table>
    </div>
  )
}
