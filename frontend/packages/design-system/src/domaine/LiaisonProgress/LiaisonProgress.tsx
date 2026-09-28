import { ProgressRing } from '../../affichage/ProgressRing/ProgressRing'
import { CoverageBar, type CoverageBarProps } from '../../affichage/CoverageBar/CoverageBar'

export interface LiaisonProgressProps {
  certification: number
  commissioning: number
  phase: string
  referential: string
  services?: CoverageBarProps[]
}

// Avancement d'une liaison (§7.7) : deux anneaux, phase courante, couverture par service.
export function LiaisonProgress(p: LiaisonProgressProps) {
  return (
    <div className="gm-liaison">
      <div className="gm-row" style={{ gap: 'var(--gm-space-5)', alignItems: 'flex-start' }}>
        <ProgressRing label="Certification" value={p.certification} status={p.certification >= 100 ? 'ok' : undefined} statusLabel="certifiée" />
        <ProgressRing label="Mise en service" value={p.commissioning} />
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-field-help">Phase courante</span>
          <strong>{p.phase}</strong>
          <span className="gm-field-help">{'Référentiel ' + p.referential}</span>
        </div>
      </div>
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)', marginTop: 'var(--gm-space-4)' }}>
        {(p.services || []).map((s) => (
          <CoverageBar key={s.service} service={s.service} level={s.level} />
        ))}
      </div>
    </div>
  )
}
