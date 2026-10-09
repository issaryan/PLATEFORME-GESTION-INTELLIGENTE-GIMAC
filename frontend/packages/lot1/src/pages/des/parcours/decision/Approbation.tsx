import { useTranslation } from 'react-i18next'
import { Card, InlineAlert, StatusBadge, Note } from '@gimac/design-system'
import type { ApprobationSecurite, DossierLiaison, ProcesVerbalCertification } from '../../../../modele/parcours'
import { BlocAttestations } from './Attestations'
import { BlocDossierChangement, BlocFicheChangement } from './FicheChangement'
import { TON_APPROBATION } from './commun'
import { useFormatDate } from '../communs'

// Verrou 2 : approbation du changement par la sécurité des SI (D-56, rangs 20 à 26). Elle fait passer la
// liaison à « Approuvée par la sécurité » ; la mise en production ne commence qu'après.

export function CarteApprobation({ dossier, approbation, pv }: { dossier: DossierLiaison; approbation: ApprobationSecurite; pv: ProcesVerbalCertification }) {
  const { t } = useTranslation()
  const formatDate = useFormatDate()
  const ouvert = pv.etat === 'SIGNE'
  return (
    <Card
      title={t('decision.approbation.titre')}
      actions={<StatusBadge status={TON_APPROBATION[approbation.etat] ?? 'neutral'} label={t(`parcours.approbation.etat.${approbation.etat}`)} />}
    >
      <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
        <Note>{t('decision.approbation.explication')}</Note>
        {!ouvert ? <InlineAlert level="info" title={t('decision.approbation.ferme.titre')}>{t('decision.approbation.ferme.texte')}</InlineAlert> : null}
        {approbation.etat === 'APPROUVEE' ? (
          <InlineAlert level="info" title={t('decision.approbation.approuvee.titre')}>
            {t('decision.approbation.approuvee.texte', { date: formatDate(approbation.approuveeLe) })}
          </InlineAlert>
        ) : null}
        {ouvert || approbation.etat !== 'EN_ATTENTE_DOSSIER' ? (
          <>
            <BlocDossierChangement dossier={dossier} approbation={approbation} />
            <BlocAttestations dossier={dossier} approbation={approbation} />
            <BlocFicheChangement dossier={dossier} approbation={approbation} />
          </>
        ) : null}
      </div>
    </Card>
  )
}
