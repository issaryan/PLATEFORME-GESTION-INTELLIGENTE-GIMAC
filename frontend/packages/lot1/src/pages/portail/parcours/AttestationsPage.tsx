import { useState } from 'react'
import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, Hash, InlineAlert, KeyValueList, StatusBadge } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { ApprobationSecurite, Attestation, DossierLiaison, TypeAttestation } from '../../../modele/parcours'
import { useApprobationPortail, useDeposerAttestation } from '../../../hooks/parcours'
import { ChampFichier } from '../../commun/ChampFichier'
import { CadreAction } from './cadre'
import { PERM, useDate, useFonction, useGeste, usePermissionPortail } from './commun'

// Fourniture des attestations de sécurité (rangs 21 et 22, verrou 2, D-56) : ISO 27001, PCI DSS ou GSMA
// selon l'activité, conformité COBAC pour une fintech. PDF seulement ; le motif d'un rejet par la sécurité
// des systèmes d'information est affiché et l'attestation peut être redéposée.

const etatDe = (a: Attestation) => (a.motifRejet && !a.preuve ? 'REJETEE' : a.verifieeLe ? 'VERIFIEE' : a.preuve ? 'EN_VERIFICATION' : 'A_FOURNIR')
const TON = { REJETEE: 'crit', VERIFIEE: 'ok', EN_VERIFICATION: 'info', A_FOURNIR: 'neutral' } as const

type DepotAttestation = { liaisonId: string; type: TypeAttestation; fichier: File }

function Depot({ liaisonId, a }: { liaisonId: string; a: Attestation }) {
  const { t } = useTranslation()
  const deposer = useDeposerAttestation()
  const geste = useGeste()
  const [fichier, setFichier] = useState<File | null>(null)
  const [controle, setControle] = useState(false)
  const libelle = t(`parcours.attestation.${a.type}`)
  const erreur = !controle ? undefined : !fichier ? t('pp.attestations.fichierRequis') : !/\.pdf$/i.test(fichier.name) ? t('pp.attestations.formatPdf') : undefined
  const envoyer = () => {
    setControle(true)
    if (!fichier || !/\.pdf$/i.test(fichier.name)) return
    geste<DepotAttestation>(
      {
        key: `deposerAttestation-${a.type}`, label: 'pp.attestations.deposer', icon: 'upload', variant: 'primary', permission: PERM.deposer, confirm: 'confirm',
        consequences: (x) => (
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <KeyValueList items={[{ key: t('pp.attestations.type'), value: libelle }, { key: t('pp.livrable.fichier'), value: x.fichier.name, mono: true }]} />
            <span>{t('pp.attestations.consequence')}</span>
          </div>
        ),
        effet: (x) => deposer.mutateAsync(x),
      },
      { liaisonId, type: a.type, fichier },
      libelle,
    )
  }
  return (
    <form className="gm-col" noValidate onSubmit={(e) => { e.preventDefault(); envoyer() }}>
      <ChampFichier name={`attestation-${a.type}`} label={t('pp.attestations.fichier', { type: libelle })} help={t('pp.attestations.fichierAide')} accept="PDF" maxSize={t('pp.tailleMax')} required
        fichier={fichier} onFichier={(f) => { setFichier(f); setControle(Boolean(f)) }} error={erreur} />
      <div className="gm-row">
        <Button type="submit" variant="primary" icon="upload">{t(a.motifRejet ? 'pp.attestations.redeposer' : 'pp.attestations.deposer')}</Button>
      </div>
    </form>
  )
}

function CarteAttestation({ app, a }: { app: ApprobationSecurite; a: Attestation }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const fonction = useFonction()
  const peut = usePermissionPortail(PERM.deposer)
  const etat = etatDe(a)
  const deposable = !a.preuve && app.actionsPermises.includes('DEPOSER_ATTESTATION')
  return (
    <Card title={t(`parcours.attestation.${a.type}`)} actions={<StatusBadge status={TON[etat]} label={t(`pp.attestations.etat.${etat}`)} />}>
      <div className="gm-col">
        {a.motifRejet && !a.preuve ? <InlineAlert level="crit" title={t('pp.attestations.rejetee', { par: fonction('RSSI') })}>{a.motifRejet}</InlineAlert> : null}
        {a.preuve ? (
          <KeyValueList items={[
            { key: t('pp.livrable.fichier'), value: a.preuve.nom, mono: true },
            { key: t('pp.attestations.deposeeLe'), value: ecrire(a.preuve.deposeLe) },
            { key: t('pp.pv.empreinte'), value: <Hash value={a.preuve.empreinte} /> },
            ...(a.verifieeLe ? [{ key: t('pp.attestations.verifiee'), value: t('pp.attestations.verifieeLe', { date: ecrire(a.verifieeLe), par: fonction('RSSI') }) }] : []),
          ]} />
        ) : null}
        {deposable ? (peut ? <Depot liaisonId={app.liaisonId} a={a} /> : <InlineAlert level="info">{t('pp.droitManquant')}</InlineAlert>) : null}
      </div>
    </Card>
  )
}

function Attestations({ app }: { app: ApprobationSecurite }) {
  const { t } = useTranslation()
  const requises = app.attestations.filter((a) => a.requise)
  const manquantes = requises.filter((a) => !a.preuve)
  return (
    <div className="gm-col">
      <InlineAlert level="info" title={t('pp.attestations.exigees', { liste: requises.map((a) => t(`parcours.attestation.${a.type}`)).join(', ') })}>
        {t(app.fintech ? 'pp.attestations.fintech' : 'pp.attestations.activite')}
      </InlineAlert>
      {manquantes.length && !app.actionsPermises.includes('DEPOSER_ATTESTATION') ? <InlineAlert level="info">{t('pp.attestations.pasEncore')}</InlineAlert> : null}
      {requises.map((a) => <CarteAttestation key={a.type} app={app} a={a} />)}
    </div>
  )
}

function Chargement({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useApprobationPortail(d.id)
  return (
    <EtatsRequete etat={q} emptyTitle={t('pp.introuvable')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(app) => <Attestations app={app} />}
    </EtatsRequete>
  )
}

export default function AttestationsPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const lire = usePermissionPortail(PERM.lireCertification)
  return (
    <CadreAction id={id} titre={t('pp.attestations.titre')}>
      {(d) => (lire ? <Chargement d={d} /> : <InlineAlert level="info">{t('pp.droitLecture')}</InlineAlert>)}
    </CadreAction>
  )
}
