import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button, Card, DataTable, DateTimeField, EmptyState, Hash, InlineAlert, KeyValueList, ResourceList,
  StatusBadge, TextField, type Column,
} from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { Attestation, Condition, Dossier, Horodatage } from '../../../modele/certification'
import {
  useAttestation, useConditions, useDeposerProcesVerbal, useFicheDeChangement, useHorodatages, useLierFiche, useProcesVerbal,
  useRegistre, useTelechargerAttestation,
} from '../../../hooks/certification'
import { verifierEmpreinte } from '../../../adaptateurs/certification'
import { actionApprouverStatut, registreDescriptor } from '../../../ressources/dossier'
import { useExecuteurActions } from '../../../ressources/actions'
import { useDescripteurDS } from '../../../ressources/descripteur'
import { enregistrerFichier } from '../../../ressources/fichier'
import { ChampFichier } from '../../commun/ChampFichier'

// Onglets de DES-11 : compositions de composants du catalogue, alimentées par les hooks de données.

function useLibellesEtats() {
  const { t } = useTranslation()
  return { vide: t('commun.vide'), erreur: t('commun.erreur.titre'), reessayer: t('commun.reessayer') }
}

export function OngletCompletude({ dossier }: { dossier: Dossier }) {
  const { t } = useTranslation()
  const e = useLibellesEtats()
  const q = useConditions(dossier.id)
  const colonnes: Column<Condition>[] = [
    { key: 'code', header: t('condition.libelle'), render: (c) => t(`condition.${c.code}`) },
    { key: 'satisfaite', header: t('condition.etat'), render: (c) => <StatusBadge status={c.satisfaite ? 'ok' : 'warn'} label={t(c.satisfaite ? 'condition.satisfaite' : 'condition.nonSatisfaite')} /> },
    { key: 'source', header: t('condition.source') },
    { key: 'detail', header: t('condition.detail'), render: (c) => c.detail ?? '' },
    { key: 'dateVerification', header: t('condition.dateVerification'), type: 'datetime', render: (c) => c.dateVerification ?? t('commun.nonDisponible') },
  ]
  return (
    <EtatsRequete etat={q} isEmpty={(l) => l.length === 0} emptyTitle={e.vide} errorTitle={e.erreur} retryLabel={e.reessayer}>
      {(cs) => <DataTable columns={colonnes} rows={cs} rowKey={(c) => c.code} state="ok" />}
    </EtatsRequete>
  )
}

export function OngletFiche({ dossier }: { dossier: Dossier }) {
  const { t } = useTranslation()
  const e = useLibellesEtats()
  const q = useFicheDeChangement(dossier.id)
  const lier = useLierFiche()
  const [reference, setReference] = useState('')
  const formulaire = dossier.actionsPermises.includes('LIER_FICHE') ? (
    <Card title={t('fiche.action.lier')}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)', maxWidth: 480 }}>
        <TextField name="referenceFiche" label={t('fiche.reference')} help={t('fiche.reference.aide')} mono required value={reference} onChange={setReference} />
        {lier.error ? <InlineAlert level="crit" correlationId={lier.error.correlationId || undefined}>{t(`erreur.${lier.error.code}`, { defaultValue: lier.error.message })}</InlineAlert> : null}
        <Button variant="primary" icon="check" loading={lier.isPending} disabled={!reference.trim()} disabledReason={t('fiche.reference.requise')} onClick={() => lier.mutate({ dossierId: dossier.id, reference: reference.trim() })}>
          {t('fiche.action.lier')}
        </Button>
      </div>
    </Card>
  ) : null
  return (
    <EtatsRequete etat={q} emptyTitle={e.vide} errorTitle={e.erreur} retryLabel={e.reessayer}>
      {(f) =>
        f === null ? (
          <div className="gm-col">
            <EmptyState icon="file-text" title={t('fiche.aucune')} text={t('fiche.aucune.texte')} />
            {formulaire}
          </div>
        ) : (
          <div className="gm-col">
            {f.aRelire ? <InlineAlert level="warn">{t('fiche.aRelire')}</InlineAlert> : null}
            <KeyValueList
              items={[
                { key: t('fiche.reference'), value: f.reference, mono: true },
                { key: t('fiche.etat'), value: t(`fiche.etat.${f.etatApprobation}`, { defaultValue: f.etatApprobation }) },
                { key: t('fiche.fenetre'), value: t('fiche.fenetre.valeur', { debut: f.fenetreDebut, fin: f.fenetreFin }) },
                { key: t('fiche.dateLecture'), value: f.dateLecture },
              ]}
            />
            <p className="gm-field-help" style={{ margin: 0 }}>{t('fiche.lectureSeule')}</p>
          </div>
        )
      }
    </EtatsRequete>
  )
}

export function OngletProcesVerbal({ dossier }: { dossier: Dossier }) {
  const { t } = useTranslation()
  const e = useLibellesEtats()
  const q = useProcesVerbal(dossier.id)
  const deposer = useDeposerProcesVerbal()
  const [fichier, setFichier] = useState<File | null>(null)
  const [bascule, setBascule] = useState({ date: '', time: '' })
  const permis = dossier.actionsPermises.includes('DEPOSER_PROCES_VERBAL')
  return (
    <EtatsRequete etat={q} emptyTitle={e.vide} errorTitle={e.erreur} retryLabel={e.reessayer}>
      {(pv) =>
        pv ? (
          <KeyValueList
            items={[
              { key: t('pv.empreinte'), value: <Hash value={pv.empreinte} /> },
              { key: t('pv.heureBascule'), value: pv.heureBascule },
              { key: t('pv.fenetre'), value: <StatusBadge status={pv.horsFenetre ? 'warn' : 'ok'} label={t(pv.horsFenetre ? 'pv.horsFenetre' : 'pv.dansFenetre')} /> },
            ]}
          />
        ) : (
          <div className="gm-col">
            <EmptyState icon="file-text" title={t('pv.aucun')} text={t(permis ? 'pv.aucun.depot' : 'pv.aucun.attente')} />
            {permis ? (
              <Card title={t('pv.action.deposer')}>
                <div className="gm-col" style={{ gap: 'var(--gm-space-3)', maxWidth: 480 }}>
                  <ChampFichier name="proces-verbal" label={t('pv.fichier')} accept="PDF" maxSize="20 Mo" required fichier={fichier} onFichier={setFichier} />
                  <DateTimeField name="heureBascule" label={t('pv.heureBascule')} date={bascule.date} time={bascule.time} onChange={setBascule} required />
                  {deposer.error ? <InlineAlert level="crit" correlationId={deposer.error.correlationId || undefined}>{t(`erreur.${deposer.error.code}`, { defaultValue: deposer.error.message })}</InlineAlert> : null}
                  <Button
                    variant="primary"
                    icon="upload"
                    loading={deposer.isPending}
                    disabled={!fichier || !bascule.date || !bascule.time}
                    disabledReason={t('pv.incomplet')}
                    onClick={() => fichier && deposer.mutate({ dossierId: dossier.id, fichier, heureBascule: `${bascule.date} ${bascule.time}` })}
                  >
                    {t('pv.action.deposer')}
                  </Button>
                </div>
              </Card>
            ) : null}
          </div>
        )
      }
    </EtatsRequete>
  )
}

export function OngletHorodatages({ dossier }: { dossier: Dossier }) {
  const { t } = useTranslation()
  const e = useLibellesEtats()
  const q = useHorodatages(dossier.id)
  const colonnes: Column<Horodatage>[] = [
    { key: 'objetType', header: t('horodatage.objet'), render: (h) => t(`horodatage.objet.${h.objetType}`) },
    { key: 'empreinte', header: t('horodatage.empreinte'), render: (h) => <Hash value={h.empreinte} /> },
    { key: 'dateJeton', header: t('horodatage.date'), type: 'datetime' },
    { key: 'verifie', header: t('horodatage.verification'), render: (h) => <StatusBadge status={h.verifie ? 'ok' : 'crit'} label={t(h.verifie ? 'horodatage.verifie' : 'horodatage.nonVerifie')} /> },
    { key: 'autoriteChaine', header: t('horodatage.autorite') },
  ]
  return (
    <EtatsRequete etat={q} isEmpty={(l) => l.length === 0} emptyTitle={t('horodatage.aucun')} errorTitle={e.erreur} retryLabel={e.reessayer}>
      {(hs) => <DataTable columns={colonnes} rows={hs} rowKey={(h) => h.id} state="ok" />}
    </EtatsRequete>
  )
}

/** Fiche d'une attestation : numéro, empreinte vérifiable au téléchargement, demande de statut en attente. */
export function FicheAttestation({ attestation: a, telecharger }: { attestation: Attestation; telecharger: (a: Attestation) => Promise<boolean | null> }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const [verifiee, setVerifiee] = useState<boolean | null | undefined>(undefined)
  const [erreur, setErreur] = useState<string | null>(null)
  const agir = () => {
    setErreur(null)
    telecharger(a).then(setVerifiee).catch((e: { message?: string }) => setErreur(e.message ?? ''))
  }
  return (
    <div className="gm-col">
      <KeyValueList
        items={[
          { key: t('attestation.numero'), value: a.numero, mono: true },
          { key: t('dossier.institution'), value: a.institutionNom },
          { key: t('registre.typeProjet'), value: t(`typeProjet.${a.typeProjet}`) },
          { key: t('registre.services'), value: a.servicesCertifies.join(', ') },
          { key: t('registre.dateEmission'), value: a.dateEmission },
          { key: t('registre.etat'), value: <StatusBadge status={a.etat === 'EN_VIGUEUR' ? 'ok' : a.etat === 'REVOQUEE' ? 'crit' : 'warn'} label={t(`attestation.etat.${a.etat}`)} /> },
          { key: t('attestation.empreinte'), value: <Hash value={a.empreinteDocument} onVerify={a.actionsPermises.includes('TELECHARGER') ? agir : undefined} verified={verifiee} /> },
        ]}
      />
      {verifiee === null ? <InlineAlert level="warn">{t('attestation.empreinteIndisponible')}</InlineAlert> : null}
      {verifiee === false ? <InlineAlert level="crit" title={t('attestation.empreinteDivergente')}>{t('attestation.empreinteDivergente.texte')}</InlineAlert> : null}
      {erreur ? <InlineAlert level="crit">{erreur}</InlineAlert> : null}
      {a.demandeEnAttente?.demandeeParMoi ? <InlineAlert level="info">{t('statut.quatreYeux')}</InlineAlert> : null}
      <div className="gm-row">
        {a.actionsPermises.includes('TELECHARGER') ? (
          <Button variant="secondary" icon="download" onClick={agir}>{t('attestation.action.telecharger')}</Button>
        ) : null}
        {actionApprouverStatut.isAllowed?.(a) ? (
          <Button variant="primary" icon="pen-line" onClick={() => executeur.lancer(actionApprouverStatut, a, a.numero, 'certification')}>
            {t('statut.action.approuver')}
          </Button>
        ) : null}
      </div>
    </div>
  )
}

export function OngletAttestation({ dossier }: { dossier: Dossier }) {
  const { t } = useTranslation()
  if (!dossier.numeroAttestation) return <EmptyState icon="shield-check" title={t('attestation.aucune')} text={t('attestation.aucune.texte')} />
  return <AttestationDuDossier numero={dossier.numeroAttestation} />
}

function AttestationDuDossier({ numero }: { numero: string }) {
  const e = useLibellesEtats()
  const q = useAttestation(numero)
  const telecharger = useTelechargerAttestation()
  return (
    <EtatsRequete etat={q} emptyTitle={e.vide} errorTitle={e.erreur} retryLabel={e.reessayer}>
      {(a) => (
        <FicheAttestation
          attestation={a}
          telecharger={async (x) => {
            const f = await telecharger.mutateAsync(x.numero)
            enregistrerFichier(f.blob, `${x.numero}.pdf`)
            return verifierEmpreinte(f.empreinte, x.empreinteDocument)
          }}
        />
      )}
    </EtatsRequete>
  )
}

export function OngletRegistre() {
  const { t } = useTranslation()
  const q = useRegistre({ page: 1, taille: 25 })
  const descripteur = useDescripteurDS(registreDescriptor)
  const executeur = useExecuteurActions()
  const exporter = registreDescriptor.actions[0]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
        <Button variant="secondary" icon="download" onClick={() => executeur.lancer(exporter, null as never, t('registre.pluriel'), 'certification')}>
          {t(exporter.label)}
        </Button>
      </div>
      <ResourceList
      descriptor={descripteur}
      rows={q.data?.contenu ?? []}
      total={q.data?.total ?? 0}
      state={q.isPending ? 'loading' : q.isError ? 'error' : q.data && q.data.total === 0 ? 'empty' : 'ok'}
      error={q.error ?? undefined}
      embedded
    />
    </div>
  )
}
