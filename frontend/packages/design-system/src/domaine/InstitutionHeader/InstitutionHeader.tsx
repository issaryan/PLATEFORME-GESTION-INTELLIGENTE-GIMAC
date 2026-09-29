import type { ReactNode } from 'react'
import type { Status } from '../../lib/types'
import { Tag } from '../../affichage/Tag/Tag'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { useLibelle } from '../../i18n/LangueProvider'
import type { CleLibelle } from '../../i18n/libelles'

export type CountryCode = 'CM' | 'CF' | 'CG' | 'GA' | 'GQ' | 'TD'

const FLAGS: Record<string, CleLibelle> = { CM: 'pays.CM', CF: 'pays.CF', CG: 'pays.CG', GA: 'pays.GA', GQ: 'pays.GQ', TD: 'pays.TD' }

export interface InstitutionHeaderProps {
  code: string
  category: string
  country: CountryCode | (string & {})
  name: string
  acronym?: string
  status: Status
  statusLabel: string
  compliance: Status
  complianceLabel: string
  mode?: string
  actions?: ReactNode
}

// En-tête de la fiche d'une institution membre (§7.7, gabarit G3).
export function InstitutionHeader(p: InstitutionHeaderProps) {
  const t = useLibelle()
  return (
    <header className="gm-objhead">
      <div className="gm-objhead-main">
        <div className="gm-row">
          <span className="gm-mono gm-field-help">{p.code}</span>
          <Tag>{p.category}</Tag>
          <span className="gm-field-help">{FLAGS[p.country] ? t(FLAGS[p.country]) : p.country}</span>
        </div>
        <h1 className="gm-heading-display">
          {p.name}
          {p.acronym ? <span className="gm-objhead-acr">{' (' + p.acronym + ')'}</span> : null}
        </h1>
        <div className="gm-row">
          <StatusBadge status={p.status} label={p.statusLabel} />
          <span className="gm-field-help">{t('institution.conformite')}</span>
          <StatusBadge status={p.compliance} label={p.complianceLabel} />
          {p.mode ? <span className="gm-field-help">{t('institution.adhesion', { mode: p.mode })}</span> : null}
        </div>
      </div>
      <div className="gm-row">{p.actions}</div>
    </header>
  )
}
