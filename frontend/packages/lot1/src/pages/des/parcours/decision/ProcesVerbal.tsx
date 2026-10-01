import { useTranslation } from 'react-i18next'
import { Button, Card, CoverageBar, Hash, InlineAlert, KeyValueList, ProgressRing, StatusBadge, Note } from '@gimac/design-system'
import type { DossierLiaison, ProcesVerbalCertification, ServicePerimetre } from '../../../../modele/parcours'
import { useEtablirProcesVerbal, useSignerProcesVerbalGimac } from '../../../../hooks/parcours'
import { EtatSignature, SousTitre, TON_PV, useDetient, useLancer, usePeut, Vide, type Action, PERMISSION_GESTE } from './commun'

// Verrou 1 : procès-verbal de certification (D-56, rangs 17 à 19). La couverture du périmètre doit valoir
// 100 % ; le conducteur de projet établit le procès-verbal ; le chef du DES le signe pour le GIMAC (ancien
// « quitus technique ») après réauthentification FIDO2 ; le participant le signe sur son portail.

/** Niveau de la barre de couverture d'un service (0 hors périmètre … 3 certifié, 4 en service). */
const NIVEAU: Record<ServicePerimetre['etat'], 1 | 2 | 3 | 4> = { NON_DEMARRE: 1, EN_TEST: 2, CERTIFIE: 3, OUVERT: 4, NON_OUVERT: 3 }

export function CarteProcesVerbal({ dossier, pv }: { dossier: DossierLiaison; pv: ProcesVerbalCertification }) {
  const { t } = useTranslation()
  const peut = usePeut()
  const detient = useDetient()
  const lancer = useLancer()
  const etablir = useEtablirProcesVerbal()
  const signer = useSignerProcesVerbalGimac()
  const titre = `${dossier.reference} · ${dossier.institutionNom}`
  const inclus = dossier.perimetre.filter((s) => s.inclus)
  const taux = pv.couverture.taux
  const complete = taux === 100

  const actionEtablir: Action<ProcesVerbalCertification> = {
    key: 'etablirPv', permission: PERMISSION_GESTE.etablirPv[0], label: 'decision.pv.action.etablir', icon: 'scroll-text', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: () => <p style={{ margin: 0 }}>{t('decision.pv.etablir.consequence')}</p>,
    run: () => etablir.mutateAsync(pv.liaisonId),
  }
  const actionSigner: Action<ProcesVerbalCertification> = {
    key: 'signerPvGimac', permission: PERMISSION_GESTE.signerPvGimac[0], label: 'decision.pv.action.signerGimac', icon: 'pen-line', variant: 'primary', scope: 'row', confirm: 'sign',
    sign: { purpose: 'QUITUS_SIGN', empreinte: (p) => p.empreinte ?? '' },
    consequences: (p) => <p style={{ margin: 0 }}>{t(p.signatureParticipant ? 'decision.pv.signer.consequenceFinale' : 'decision.pv.signer.consequence')}</p>,
    run: ({ assertion }) => signer.mutateAsync({ liaisonId: pv.liaisonId, assertion: assertion ?? '' }),
  }

  const peutEtablir = peut(pv.actionsPermises, 'ETABLIR', 'etablirPv')
  const bloque = pv.actionsPermises.includes('ETABLIR_BLOQUE')
  const peutSigner = peut(pv.actionsPermises, 'SIGNER_GIMAC', 'signerPvGimac')

  return (
    <Card
      title={t('decision.pv.titre')}
      actions={<StatusBadge status={TON_PV[pv.etat] ?? 'neutral'} label={t(`parcours.pv.etat.${pv.etat}`)} />}
    >
      <div className="gm-col">
        <Note>{t('decision.pv.explication')}</Note>

        <SousTitre>{t('decision.pv.couverture')}</SousTitre>
        <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)' }}>
          <div style={{ flex: '0 0 200px', display: 'flex', justifyContent: 'center' }}>
            <ProgressRing
              label={t('decision.pv.couvertureLibelle')}
              value={taux}
              status={complete ? 'ok' : 'warn'}
              statusLabel={t(complete ? 'decision.pv.couvertureComplete' : 'decision.pv.couvertureIncomplete', { n: pv.couverture.numerateur, d: pv.couverture.denominateur })}
            />
          </div>
          <div className="gm-col" style={{ flex: '1 1 280px', gap: 'var(--gm-space-2)' }} aria-label={t('decision.pv.servicesCouverts')}>
            {inclus.map((s) => (
              <CoverageBar key={s.serviceCode} service={s.libelle} level={pv.servicesCouverts.includes(s.serviceCode) ? Math.max(3, NIVEAU[s.etat]) as 3 | 4 : NIVEAU[s.etat]} />
            ))}
          </div>
        </div>
        {bloque ? (
          <InlineAlert level="warn" title={t('decision.pv.bloque.titre')}>
            {t('decision.pv.bloque.texte', { taux: taux ?? 0, n: pv.couverture.numerateur, d: pv.couverture.denominateur })}
          </InlineAlert>
        ) : null}
        {pv.etat === 'NON_ETABLI' && !bloque && !pv.actionsPermises.includes('ETABLIR') ? (
          <InlineAlert level="info">{t('decision.pv.attenteConstat')}</InlineAlert>
        ) : null}

        <SousTitre>{t('decision.pv.piece')}</SousTitre>
        <KeyValueList
          items={[
            { key: t('decision.pv.etabliPar'), value: pv.etabliPar ?? <Vide /> },
            { key: t('decision.pv.etabliLe'), value: pv.etabliLe ?? <Vide /> },
            { key: t('decision.pv.empreinte'), value: pv.empreinte ? <Hash value={pv.empreinte} /> : <Vide /> },
            { key: t('decision.pv.signatureGimac'), value: <EtatSignature signature={pv.signatureGimac} attente={t('decision.pv.attenteGimac')} /> },
            { key: t('decision.pv.signatureParticipant'), value: <EtatSignature signature={pv.signatureParticipant} attente={t('decision.pv.attenteParticipant')} /> },
          ]}
        />
        {pv.etat === 'SIGNE' ? <InlineAlert level="info" title={t('decision.pv.certifiee.titre')}>{t('decision.pv.certifiee.texte')}</InlineAlert> : null}

        {peutEtablir || (bloque && detient('etablirPv')) || peutSigner ? (
          <div className="gm-row" data-testid="actions-pv">
            {peutEtablir ? (
              <Button variant="primary" icon="scroll-text" onClick={() => lancer(actionEtablir, pv, titre)}>{t('decision.pv.action.etablir')}</Button>
            ) : bloque && detient('etablirPv') ? (
              <Button variant="primary" icon="scroll-text" disabled disabledReason={t('decision.pv.bloque.raison', { taux: taux ?? 0 })}>{t('decision.pv.action.etablir')}</Button>
            ) : null}
            {peutSigner ? (
              <Button variant="primary" icon="pen-line" onClick={() => lancer(actionSigner, pv, titre)}>{t('decision.pv.action.signerGimac')}</Button>
            ) : null}
          </div>
        ) : null}
      </div>
    </Card>
  )
}
