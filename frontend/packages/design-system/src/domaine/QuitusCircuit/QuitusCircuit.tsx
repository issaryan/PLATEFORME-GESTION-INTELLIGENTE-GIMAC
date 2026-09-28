import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'

export interface QuitusState {
  state: 'todo' | 'pending' | 'signed' | 'refused'
  signer?: string
  at?: string
  reason?: string
}

export interface QuitusCircuitProps {
  quitus?: { tech?: QuitusState; sec?: QuitusState; dg?: QuitusState }
}

type CleQuitus = 'tech' | 'sec' | 'dg'

const QUITUS: ReadonlyArray<readonly [CleQuitus, string, string]> = [
  ['tech', 'Quitus technique', 'Chef du DES'],
  ['sec', 'Quitus sécurité et conformité', 'RSSI et conformité'],
  ['dg', 'Quitus de la direction générale', 'Direction générale'],
]

const MAP: Record<QuitusState['state'], [Status, string]> = {
  signed: ['ok', 'signé'],
  pending: ['info', 'en attente de signature'],
  refused: ['crit', 'refusé'],
  todo: ['neutral', 'à venir'],
}

// Circuit des quitus dans l'ordre imposé (§7.7, dossier de mise en service).
export function QuitusCircuit(p: QuitusCircuitProps) {
  const q = p.quitus || {}
  return (
    <ol className="gm-quitus">
      {QUITUS.map((s, i) => {
        const st: QuitusState = q[s[0]] || { state: 'todo' }
        const m = MAP[st.state] || MAP.todo
        return (
          <li key={s[0]} className={cx('gm-quitus-step', 'gm-quitus-' + st.state)}>
            <span className="gm-step-num">{st.state === 'signed' ? <Icon name="check" size={14} /> : i + 1}</span>
            <div style={{ flex: 1 }}>
              <div className="gm-banner-title">{s[1]}</div>
              <div className="gm-field-help">
                {'Signataire : ' + (st.signer || s[2])}
                {st.at ? ' · ' + st.at : ''}
                {st.reason ? ' · motif : ' + st.reason : ''}
              </div>
            </div>
            <StatusBadge status={m[0]} label={m[1]} icon={st.state === 'signed' ? 'pen-line' : undefined} />
          </li>
        )
      })}
    </ol>
  )
}
