import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, InlineAlert, KeyValueList, Skeleton, StatusBadge } from '@gimac/design-system'
import { useApprobation, useProcesVerbal } from '../../../../hooks/parcours'
import { TON_APPROBATION, TON_PV, TON_VISA } from './commun'

// Résumé de la décision de mise en production (D-56) dans le dossier de mise en service (DES-11) : les quatre
// quitus du plan sont remplacés par le procès-verbal de certification et l'approbation de la sécurité, tenus
// dans l'onglet « Décision » du dossier de la liaison, vers lequel ce résumé conduit.

export function ResumeDecision({ liaisonId }: { liaisonId: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const pv = useProcesVerbal(liaisonId)
  const approbation = useApprobation(liaisonId)
  const chargement = pv.isPending || approbation.isPending
  const p = pv.data
  const a = approbation.data
  const visas = a?.visas.filter((v) => v.actif) ?? []
  return (
    <div className="gm-col" data-testid="resume-decision">
      <InlineAlert level="info" title={t('decision.resume.titre')}>{t('decision.resume.texte')}</InlineAlert>
      {chargement ? (
        <Skeleton height={96} />
      ) : p && a ? (
        <KeyValueList
          items={[
            { key: t('decision.pv.titre'), value: <StatusBadge status={TON_PV[p.etat] ?? 'neutral'} label={t(`parcours.pv.etat.${p.etat}`)} /> },
            ...visas.map((v) => ({ key: t(`parcours.visa.${v.nature}`), value: <StatusBadge status={TON_VISA[v.etat] ?? 'neutral'} label={t(`parcours.visaEtat.${v.etat}`)} /> })),
            { key: t('decision.approbation.titre'), value: <StatusBadge status={TON_APPROBATION[a.etat] ?? 'neutral'} label={t(`parcours.approbation.etat.${a.etat}`)} /> },
          ]}
        />
      ) : (
        <p className="gm-field-help" style={{ margin: 0 }}>{t('decision.resume.indisponible')}</p>
      )}
      <div className="gm-row">
        <Button variant="primary" icon="arrow-right" onClick={() => void navigate(`/des/parcours/${liaisonId}?onglet=decision`)}>
          {t('decision.resume.ouvrir')}
        </Button>
      </div>
    </div>
  )
}
