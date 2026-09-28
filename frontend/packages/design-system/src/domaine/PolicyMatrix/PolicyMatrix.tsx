import { cx } from '../../lib/cx'
import { Icon } from '../../icones/Icon/Icon'
import { Button } from '../../actions/Button/Button'

export interface PolicyPermission {
  key: string
  label: string
  grants?: Record<string, boolean>
  /** Modifications en attente d'approbation à quatre yeux. */
  pending?: Record<string, boolean>
}

export interface PolicyMatrixProps {
  roles?: string[]
  permissions?: PolicyPermission[]
}

// Matrice rôles × permissions par domaine (§7.7, console CON-01).
export function PolicyMatrix(p: PolicyMatrixProps) {
  return (
    <div className="gm-table-wrap">
      <table className="gm-table gm-table-compact gm-policy" role="grid">
        <thead>
          <tr>
            <th scope="col">Permission</th>
            {(p.roles || []).map((r) => (
              <th key={r} scope="col" style={{ textAlign: 'center' }}>{r}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {(p.permissions || []).map((perm) => (
            <tr key={perm.key}>
              <th scope="row" style={{ background: 'transparent', color: 'inherit', fontWeight: 400 }}>
                <span className="gm-mono">{perm.key}</span>
                <div className="gm-field-help">{perm.label}</div>
              </th>
              {(p.roles || []).map((r) => {
                const v = perm.grants && perm.grants[r]
                const pend = perm.pending && perm.pending[r]
                return (
                  <td key={r} style={{ textAlign: 'center' }}>
                    <span className={cx('gm-policy-cell', v && 'gm-policy-on', pend && 'gm-policy-pending')} role="img" aria-label={(v ? 'accordée' : 'refusée') + (pend ? ", modification en attente d'approbation" : '')}>
                      <Icon name={pend ? 'hourglass' : v ? 'square-check' : 'square'} size={18} />
                    </span>
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="gm-table-toolbar" style={{ borderTop: 'var(--gm-border-1) solid var(--gm-color-border)', borderBottom: 0 }}>
        <Icon name="hourglass" />
        {" Modification en attente d'approbation à quatre yeux"}
        <span className="gm-grow" />
        <Button size="sm" variant="primary" icon="pen-line">Approuver les modifications</Button>
      </div>
    </div>
  )
}
