import { useTranslation } from 'react-i18next'
import { Button, DataTable, Hash, InlineAlert, StatusBadge, type Column, type Status, Note } from '@gimac/design-system'
import type { ApprobationSecurite, Attestation, DossierLiaison } from '../../../../modele/parcours'
import { useArchiverAttestations, useVerifierAttestation } from '../../../../hooks/parcours'
import { SousTitre, useLancer, usePeut, Vide, type Action, PERMISSION_GESTE } from './commun'
import { useFormatDate } from '../communs'

// Verrou 2, rangs 21 à 23 : attestations exigées selon l'activité (ISO 27001, PCI DSS ou GSMA ; conformité
// COBAC si fintech), fournies par le participant sur son portail, vérifiées une à une par la sécurité des SI
// (conforme, ou rejet motivé qui les redemande), puis archivées ensemble.

type EtatAffiche = 'ARCHIVEE' | 'CONFORME' | 'A_VERIFIER' | 'REJETEE' | 'ATTENDUE'
const TON: Record<EtatAffiche, Status> = { ARCHIVEE: 'ok', CONFORME: 'ok', A_VERIFIER: 'warn', REJETEE: 'crit', ATTENDUE: 'neutral' }

const etatDe = (a: Attestation): EtatAffiche =>
  a.archiveeLe ? 'ARCHIVEE' : a.verifieeLe ? 'CONFORME' : a.preuve ? 'A_VERIFIER' : a.motifRejet ? 'REJETEE' : 'ATTENDUE'

export function BlocAttestations({ dossier, approbation }: { dossier: DossierLiaison; approbation: ApprobationSecurite }) {
  const { t } = useTranslation()
  const formatDate = useFormatDate()
  const peut = usePeut()
  const lancer = useLancer()
  const verifier = useVerifierAttestation()
  const archiver = useArchiverAttestations()
  const liaisonId = approbation.liaisonId
  const libelle = (a: Attestation) => t(`parcours.attestation.${a.type}`)
  const verifiable = peut(approbation.actionsPermises, 'VERIFIER_ATTESTATION', 'verifierAttestation')

  const conforme: Action<Attestation> = {
    key: 'attestationConforme', permission: PERMISSION_GESTE.verifierAttestation[0], label: 'decision.attestation.action.conforme', icon: 'check', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: (a) => <p style={{ margin: 0 }}>{t('decision.attestation.conforme.consequence', { type: libelle(a) })}</p>,
    run: ({ row }) => verifier.mutateAsync({ liaisonId, type: row.type, conforme: true, motif: '' }),
  }
  const rejeter: Action<Attestation> = {
    key: 'attestationRejet', permission: PERMISSION_GESTE.verifierAttestation[0], label: 'decision.attestation.action.rejeter', icon: 'x', variant: 'danger', scope: 'row', confirm: 'confirm', reason: true,
    consequences: (a) => <p style={{ margin: 0 }}>{t('decision.attestation.rejet.consequence', { type: libelle(a) })}</p>,
    run: ({ row, motif }) => verifier.mutateAsync({ liaisonId, type: row.type, conforme: false, motif: motif ?? '' }),
  }
  const archiverAction: Action<ApprobationSecurite> = {
    key: 'archiverAttestations', permission: PERMISSION_GESTE.archiverAttestations[0], label: 'decision.attestation.action.archiver', icon: 'lock', variant: 'primary', scope: 'row', confirm: 'confirm',
    consequences: () => <p style={{ margin: 0 }}>{t('decision.attestation.archiver.consequence')}</p>,
    run: () => archiver.mutateAsync(liaisonId),
  }

  const colonnes: Column<Attestation>[] = [
    {
      key: 'type', header: t('decision.attestation.type'),
      render: (a) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-type-body-strong">{libelle(a)}</span>
          <span className="gm-field-help">{t(a.type === 'COBAC' ? 'decision.attestation.rang22' : 'decision.attestation.rang21')}</span>
        </span>
      ),
    },
    {
      key: 'etat', header: t('decision.attestation.etat'),
      render: (a) => {
        const e = etatDe(a)
        return (
          <span className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
            <StatusBadge status={TON[e]} label={t(`decision.attestation.etat.${e}`)} />
            {e === 'REJETEE' && a.motifRejet ? <span className="gm-field-help">{t('decision.attestation.motif', { motif: a.motifRejet })}</span> : null}
          </span>
        )
      },
    },
    {
      key: 'piece', header: t('decision.attestation.piece'),
      render: (a) => (a.preuve ? (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-mono">{a.preuve.nom}</span>
          <Hash value={a.preuve.empreinte} />
          <span className="gm-field-help">{t('decision.attestation.deposee', { date: formatDate(a.preuve.deposeLe) })}</span>
        </span>
      ) : <Vide texte={t('decision.attestation.aucunePiece')} />),
    },
    {
      key: 'verification', header: t('decision.attestation.verification'),
      render: (a) => (a.verifieeLe ? t('decision.attestation.verifieePar', { nom: a.verifieePar ?? '', date: formatDate(a.verifieeLe) }) : <Vide />),
    },
    {
      key: 'actions', header: t('decision.attestation.actions'), type: 'actions',
      render: (a) => (verifiable && etatDe(a) === 'A_VERIFIER' ? (
        <span className="gm-row">
          <Button size="sm" variant="primary" icon="check" onClick={() => lancer(conforme, a, `${libelle(a)} · ${dossier.reference}`)}>{t('decision.attestation.action.conforme')}</Button>
          <Button size="sm" variant="danger" icon="x" onClick={() => lancer(rejeter, a, `${libelle(a)} · ${dossier.reference}`)}>{t('decision.attestation.action.rejeter')}</Button>
        </span>
      ) : null),
    },
  ]

  const aVerifier = approbation.attestations.filter((a) => etatDe(a) === 'A_VERIFIER').length
  return (
    <div className="gm-col">
      <SousTitre>{t('decision.attestation.titre')}</SousTitre>
      {/* CER-09 : l'exigence se lit sur les attestations servies (modèle générique : aucune), jamais sur le type de projet. */}
      {approbation.attestations.length ? <Note>{t(approbation.attestations.some((a) => a.type === 'GSMA' || a.type === 'ISO_27001') ? 'decision.attestation.exigenceMobile' : 'decision.attestation.exigenceCarte')}</Note> : null}
      {approbation.fintech ? <InlineAlert level="info">{t('decision.attestation.fintech')}</InlineAlert> : null}
      {verifiable && aVerifier > 0 ? <InlineAlert level="warn">{t('decision.attestation.aVerifier', { count: aVerifier })}</InlineAlert> : null}
      <DataTable columns={colonnes} rows={approbation.attestations} rowKey={(a) => a.type} state={approbation.attestations.length ? 'ok' : 'empty'} emptyText={t('decision.attestation.aucune')} />
      {peut(approbation.actionsPermises, 'ARCHIVER', 'archiverAttestations') ? (
        <div className="gm-row">
          <Button variant="primary" icon="lock" onClick={() => lancer(archiverAction, approbation, dossier.reference)}>{t('decision.attestation.action.archiver')}</Button>
        </div>
      ) : null}
    </div>
  )
}
