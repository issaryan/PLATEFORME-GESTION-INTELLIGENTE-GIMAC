import { ProgressRing } from '../../affichage/ProgressRing/ProgressRing'
import { CoverageBar, type CoverageBarProps } from '../../affichage/CoverageBar/CoverageBar'
import { useLibelle } from '../../i18n/LangueProvider'

export interface LiaisonProgressProps {
  certification: number
  commissioning: number
  phase: string
  referential: string
  services?: CoverageBarProps[]
}

// Avancement d'une liaison (§7.7) : deux anneaux, phase courante, couverture par service.
export function LiaisonProgress(p: LiaisonProgressProps) {
  const t = useLibelle()
  return (
    <div className="gm-liaison">
      <div className="gm-row" style={{ gap: 'var(--gm-space-5)', alignItems: 'flex-start' }}>
        <ProgressRing label={t('liaison.certification')} value={p.certification} status={p.certification >= 100 ? 'ok' : undefined} statusLabel={t('liaison.certifiee')} />
        <ProgressRing label={t('liaison.miseEnService')} value={p.commissioning} />
        <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-field-help">{t('liaison.phase')}</span>
          <strong>{p.phase}</strong>
          <span className="gm-field-help">{t('liaison.referentiel', { version: p.referential })}</span>
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
