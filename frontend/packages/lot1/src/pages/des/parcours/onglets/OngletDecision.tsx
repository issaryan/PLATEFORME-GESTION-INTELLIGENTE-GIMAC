import { useEffect, useRef, type ReactNode } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { StateMachineBar, StatusBadge } from '@gimac/design-system'
import type { ApprobationSecurite, DossierLiaison, ProcesVerbalCertification } from '../../../../modele/parcours'
import { useApprobation, useOperationsParcours, useProcesVerbal } from '../../../../hooks/parcours'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'
import { Donnees } from '../../liaisons/communs'
import { CarteApprobation } from '../decision/Approbation'
import { CarteProcesVerbal } from '../decision/ProcesVerbal'
import { CarteVisas } from '../decision/Visas'
import { carteDecision, type CarteDecision } from '../operations/regles'
import '../parcours-d93.css'

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

/**
 * Carte mise en évidence (lien profond ?operation=, D-98) : comme l'opération ouverte dans l'onglet des opérations, la
 * carte du geste visé défile dans la vue et se signale (liseré, fond passager, libellé de l'opération), une fois.
 */
function Cible({ id, libelle, children }: { id: string; libelle: string | null; children: ReactNode }) {
  const { t } = useTranslation()
  const ref = useRef<HTMLDivElement>(null)
  const vise = libelle !== null
  useEffect(() => {
    if (vise) ref.current?.scrollIntoView?.({ block: 'start', behavior: 'smooth' })
  }, [vise])
  return (
    <div id={id} ref={ref} className={vise ? 'gm-decision-carte gm-decision-cible' : 'gm-decision-carte'} aria-current={vise ? 'step' : undefined}>
      {vise ? <StatusBadge status="info" icon="arrow-down" label={t('certif.decision.cible', { libelle })} /> : null}
      {children}
    </div>
  )
}

function Decision({ dossier, pv, approbation, cible, libelle }: {
  dossier: DossierLiaison; pv: ProcesVerbalCertification; approbation: ApprobationSecurite; cible: CarteDecision | null; libelle: string | null
}) {
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
      <Cible id="decision-pv" libelle={cible === 'pv' ? libelle : null}><CarteProcesVerbal dossier={dossier} pv={pv} /></Cible>
      <CarteVisas dossier={dossier} approbation={approbation} pv={pv} />
      <Cible id="decision-approbation" libelle={cible === 'approbation' ? libelle : null}><CarteApprobation dossier={dossier} approbation={approbation} pv={pv} /></Cible>
    </div>
  )
}

export function OngletDecision({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const pv = useProcesVerbal(dossier.id)
  const approbation = useApprobation(dossier.id)
  // Lien de tâche ou de notification : ?operation=<id> désigne l'opération dont le geste se fait sur l'une des cartes.
  const [params] = useSearchParams()
  const opId = params.get('operation')
  const ops = useOperationsParcours(dossier.id, undefined, Boolean(opId))
  const operation = opId ? ops.data?.find((o) => o.id === opId) : undefined
  const cible = carteDecision(operation)
  const libelle = operation ? libelleOperation(t, operation) : null
  return (
    <Donnees requete={pv}>
      {(p) => (
        <Donnees requete={approbation}>
          {(a) => <Decision dossier={dossier} pv={p} approbation={a} cible={cible} libelle={libelle} />}
        </Donnees>
      )}
    </Donnees>
  )
}
