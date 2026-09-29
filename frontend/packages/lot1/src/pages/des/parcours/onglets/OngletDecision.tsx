import { useTranslation } from 'react-i18next'
import { StateMachineBar } from '@gimac/design-system'
import type { ApprobationSecurite, DossierLiaison, ProcesVerbalCertification } from '../../../../modele/parcours'
import { useApprobation, useProcesVerbal } from '../../../../hooks/parcours'
import { Donnees } from '../../liaisons/communs'
import { CarteApprobation } from '../decision/Approbation'
import { CarteProcesVerbal } from '../decision/ProcesVerbal'
import { CarteVisas } from '../decision/Visas'

// Onglet « Décision » du dossier de la liaison (D-56, modèle hybride) : verrou 1 (procès-verbal de
// certification), visas optionnels s'ils sont activés, verrou 2 (approbation de la sécurité). La barre
// d'états résume où en est la décision ; chaque carte ne propose que les gestes permis à l'utilisateur.

type Jalon = { key: string; fait: boolean }

function jalons(pv: ProcesVerbalCertification, a: ApprobationSecurite): Jalon[] {
  const visas = a.visas.filter((v) => v.actif)
  return [
    { key: 'couverture', fait: pv.couverture.taux === 100 },
    { key: 'pvEtabli', fait: pv.etat !== 'NON_ETABLI' },
    { key: 'pvSigne', fait: pv.etat === 'SIGNE' },
    { key: 'dossierChangement', fait: a.dossierChangement !== null },
    { key: 'attestations', fait: a.etat === 'VERIFIE_ARCHIVE' || a.etat === 'APPROUVEE' },
    ...(visas.length ? [{ key: 'visas', fait: visas.every((v) => v.etat === 'SIGNE_FAVORABLE') }] : []),
    { key: 'approbation', fait: a.etat === 'APPROUVEE' },
  ]
}

function Decision({ dossier, pv, approbation }: { dossier: DossierLiaison; pv: ProcesVerbalCertification; approbation: ApprobationSecurite }) {
  const { t } = useTranslation()
  const j = jalons(pv, approbation)
  const courant = j.find((x) => !x.fait)?.key ?? j[j.length - 1].key
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <StateMachineBar
        label={t('decision.jalons.titre')}
        states={j.map((x) => ({ key: x.key, label: t(`decision.jalon.${x.key}`) }))}
        current={courant}
        traversed={j.filter((x) => x.fait).map((x) => x.key)}
      />
      <CarteProcesVerbal dossier={dossier} pv={pv} />
      <CarteVisas dossier={dossier} approbation={approbation} pv={pv} />
      <CarteApprobation dossier={dossier} approbation={approbation} pv={pv} />
    </div>
  )
}

export function OngletDecision({ dossier }: { dossier: DossierLiaison }) {
  const pv = useProcesVerbal(dossier.id)
  const approbation = useApprobation(dossier.id)
  return (
    <Donnees requete={pv}>
      {(p) => (
        <Donnees requete={approbation}>
          {(a) => <Decision dossier={dossier} pv={p} approbation={a} />}
        </Donnees>
      )}
    </Donnees>
  )
}
