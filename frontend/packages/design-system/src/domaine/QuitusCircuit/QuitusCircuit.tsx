import { cx } from '../../lib/cx'
import type { Status } from '../../lib/types'
import { Icon } from '../../icones/Icon/Icon'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export interface QuitusState {
  state: 'todo' | 'pending' | 'signed' | 'refused'
  signer?: string
  at?: string
  reason?: string
}

export interface QuitusCircuitProps {
  quitus?: { tech?: QuitusState; sec?: QuitusState; conf?: QuitusState; dg?: QuitusState }
  /**
   * « trois » (défaut, rendu publié) : technique, sécurité et conformité réunis, direction générale.
   * « quatre » (PI-GIMAC-01) : technique, puis sécurité et conformité en parallèle au même rang,
   * puis direction générale.
   */
  natures?: 'trois' | 'quatre'
}

type CleQuitus = 'tech' | 'sec' | 'conf' | 'dg'
type Etape = readonly [CleQuitus, CleLibelle, CleLibelle, number]

const TROIS: ReadonlyArray<Etape> = [
  ['tech', 'quitus.technique', 'quitus.signataire.tech', 1],
  ['sec', 'quitus.securiteConformite', 'quitus.signataire.secConf', 2],
  ['dg', 'quitus.direction', 'quitus.signataire.dg', 3],
]
const QUATRE: ReadonlyArray<Etape> = [
  ['tech', 'quitus.technique', 'quitus.signataire.tech', 1],
  ['sec', 'quitus.securite', 'quitus.signataire.sec', 2],
  ['conf', 'quitus.conformite', 'quitus.signataire.conf', 2],
  ['dg', 'quitus.direction', 'quitus.signataire.dg', 3],
]
const MAP: Record<QuitusState['state'], [Status, CleLibelle]> = {
  signed: ['ok', 'quitus.signe'],
  pending: ['info', 'quitus.attente'],
  refused: ['crit', 'quitus.refuse'],
  todo: ['neutral', 'quitus.aVenir'],
}

// Circuit des quitus dans l'ordre imposé (§7.7, dossier de mise en service).
export function QuitusCircuit(p: QuitusCircuitProps) {
  const t = useLibelle()
  const q = p.quitus || {}
  const etapes = p.natures === 'quatre' ? QUATRE : TROIS
  return (
    <ol className="gm-quitus">
      {etapes.map((s) => {
        const st: QuitusState = q[s[0]] || { state: 'todo' }
        const m = MAP[st.state] || MAP.todo
        const parallele = p.natures === 'quatre' && s[3] === 2
        return (
          <li key={s[0]} className={cx('gm-quitus-step', 'gm-quitus-' + st.state)}>
            <span className="gm-step-num">{st.state === 'signed' ? <Icon name="check" size={14} /> : s[3]}</span>
            <div style={{ flex: 1 }}>
              <div className="gm-banner-title">{t(s[1])}</div>
              <div className="gm-field-help">
                {t('quitus.signataire', { qui: st.signer || t(s[2]) })}
                {st.at ? ' · ' + st.at : ''}
                {st.reason ? ' · ' + t('quitus.motif', { motif: st.reason }) : ''}
                {parallele ? ' · ' + t('quitus.parallele') : ''}
              </div>
            </div>
            <StatusBadge status={m[0]} label={t(m[1])} icon={st.state === 'signed' ? 'pen-line' : undefined} />
          </li>
        )
      })}
    </ol>
  )
}
