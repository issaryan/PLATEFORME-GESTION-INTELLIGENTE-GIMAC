import { useTranslation } from 'react-i18next'
import { Button, Card, KeyValueList, StatusBadge, Note } from '@gimac/design-system'
import type { ApprobationSecurite, DossierLiaison, ProcesVerbalCertification, Visa } from '../../../../modele/parcours'
import { useSignerVisa } from '../../../../hooks/parcours'
import { EtatSignature, TON_VISA, useLancer, usePeut, type Action, type Geste, PERMISSION_GESTE } from './commun'

// Visas optionnels de la conformité et de la direction générale (D-56) : paramétrés par type de projet,
// désactivés par défaut ; la zone n'existe que s'ils sont actifs. Ils s'insèrent avant l'approbation de la
// sécurité ; favorable ou défavorable, chaque visa est motivé et signé après réauthentification FIDO2.

const ACTION: Record<Visa['nature'], { action: string; geste: Geste }> = {
  CONFORMITE: { action: 'SIGNER_VISA_CONFORMITE', geste: 'visaConformite' },
  DIRECTION_GENERALE: { action: 'SIGNER_VISA_DG', geste: 'visaDg' },
}

export function CarteVisas({ dossier, approbation, pv }: { dossier: DossierLiaison; approbation: ApprobationSecurite; pv: ProcesVerbalCertification }) {
  const { t } = useTranslation()
  const peut = usePeut()
  const lancer = useLancer()
  const signer = useSignerVisa()
  const actifs = approbation.visas.filter((v) => v.actif)
  if (actifs.length === 0) return null
  const empreinte = approbation.dossierChangement?.empreinte ?? pv.empreinte ?? ''

  const viser = (favorable: boolean, geste: Geste): Action<Visa> => ({
    key: favorable ? 'visaFavorable' : 'visaDefavorable',
    permission: PERMISSION_GESTE[geste][0],
    label: favorable ? 'decision.visa.action.favorable' : 'decision.visa.action.defavorable',
    icon: favorable ? 'check' : 'x', variant: favorable ? 'primary' : 'danger', scope: 'row', confirm: 'sign', reason: true,
    sign: { purpose: 'SIGNATURE_OPERATION', empreinte: () => empreinte },
    consequences: (v) => <p style={{ margin: 0 }}>{t(favorable ? 'decision.visa.favorable.consequence' : 'decision.visa.defavorable.consequence', { visa: t(`parcours.visa.${v.nature}`) })}</p>,
    run: ({ row, motif, assertion }) => signer.mutateAsync({ liaisonId: approbation.liaisonId, nature: row.nature, favorable, motif: motif ?? '', assertion: assertion ?? '' }),
  })

  return (
    <Card title={t('decision.visa.titre')}>
      <div className="gm-col">
        <Note>{t('decision.visa.explication')}</Note>
        {actifs.map((v) => {
          const { action, geste } = ACTION[v.nature]
          const signable = peut(approbation.actionsPermises, action, geste)
          const titre = `${t(`parcours.visa.${v.nature}`)} · ${dossier.reference}`
          return (
            <section key={v.nature} className="gm-panel gm-col" aria-label={t(`parcours.visa.${v.nature}`)} style={{ gap: 'var(--gm-space-3)' }}>
              <div className="gm-row" style={{ justifyContent: 'space-between' }}>
                <h3 className="gm-type-h3" style={{ margin: 0 }}>{t(`parcours.visa.${v.nature}`)}</h3>
                <StatusBadge status={TON_VISA[v.etat] ?? 'neutral'} label={t(`parcours.visaEtat.${v.etat}`)} />
              </div>
              <KeyValueList
                items={[
                  { key: t('decision.visa.signature'), value: <EtatSignature signature={v.signature} attente={t('decision.visa.attente')} /> },
                  ...(v.motif ? [{ key: t('decision.visa.motif'), value: v.motif }] : []),
                ]}
              />
              {signable ? (
                <div className="gm-row">
                  <Button variant="primary" icon="check" onClick={() => lancer(viser(true, geste), v, titre)}>{t('decision.visa.action.favorable')}</Button>
                  <Button variant="danger" icon="x" onClick={() => lancer(viser(false, geste), v, titre)}>{t('decision.visa.action.defavorable')}</Button>
                </div>
              ) : null}
            </section>
          )
        })}
      </div>
    </Card>
  )
}
