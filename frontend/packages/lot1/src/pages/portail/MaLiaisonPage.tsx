import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import {
  Button, DagView, DataTable, EmptyState, Hash, InlineAlert, KeyValueList, LiaisonProgress, ResourceDetail, ResourceList, StatusBadge, type Column,
} from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { CampaignResult } from '../../modele/homologation'
import type { PortailCertification, PortailLiaison } from '../../modele/portail'
import { useAFaire, useCertificationsPortail, useMaLiaison, useMesLiaisons, useTelechargerAttestationPortail } from '../../hooks/portail'
import { couverturesNonCalculables, versDag, versLiaisonProgress } from '../../adaptateurs/portail'
import { verifierEmpreinte } from '../../adaptateurs/certification'
import { etatDossier, verdictCampagne } from '../../dictionnaires'
import { avecTypeProjetLisible, portailLiaisonDescriptor, portailOperationDescriptor } from '../../ressources/portail'
import { useDescripteurDS } from '../../ressources/descripteur'
import { enregistrerFichier } from '../../ressources/fichier'

// POR-03 Ma liaison (portail des participants, HOM-24, CER-13) : liste des liaisons de l'institution du
// jeton, puis fiche en lecture seule. Aucun champ interne ; les données viennent des hooks du portail.

function useEtroit(seuil = 640) {
  const [etroit, setEtroit] = useState(() => window.innerWidth < seuil)
  useEffect(() => {
    const f = () => setEtroit(window.innerWidth < seuil)
    window.addEventListener('resize', f)
    return () => window.removeEventListener('resize', f)
  }, [seuil])
  return etroit
}

function useLibellesEtats() {
  const { t } = useTranslation()
  return { vide: t('commun.vide'), erreur: t('commun.erreur.titre'), reessayer: t('commun.reessayer') }
}

function OngletAFaire({ liaisonId }: { liaisonId: string }) {
  const q = useAFaire(liaisonId)
  const descripteur = useDescripteurDS(portailOperationDescriptor)
  return (
    <ResourceList
      descriptor={descripteur}
      rows={q.data ?? []}
      total={q.data?.length ?? 0}
      state={q.isPending ? 'loading' : q.isError ? 'error' : q.data?.length === 0 ? 'empty' : 'ok'}
      error={q.error ?? undefined}
      embedded
      liveUpdates={false}
    />
  )
}

function OngletParcours({ liaison }: { liaison: PortailLiaison }) {
  const { t } = useTranslation()
  const etroit = useEtroit()
  if (liaison.graphe.noeuds.length === 0) return <EmptyState icon="route" title={t('portail.parcours.vide')} />
  // Sous le point de rupture étroit, le graphe s'ouvre sur sa vue liste équivalente (DS-GIMAC-01 §10.2).
  return <DagView key={etroit ? 'l' : 'g'} {...versDag(liaison.graphe, (p) => t(`partie.${p}`))} view={etroit ? 'list' : 'graph'} />
}

function OngletResultats({ liaison }: { liaison: PortailLiaison }) {
  const { t } = useTranslation()
  const colonnes: Column<CampaignResult>[] = [
    { key: 'idScenario', header: t('campagne.scenario'), type: 'mono' },
    { key: 'operationCode', header: t('campagne.operation'), render: (c) => <span className="gm-mono">{c.operationCode ?? t('campagne.sansOperation')}</span> },
    { key: 'verdict', header: t('campagne.verdict'), render: (c) => <StatusBadge status={verdictCampagne[c.verdict].status} label={t(verdictCampagne[c.verdict].label)} /> },
    { key: 'dateExecution', header: t('campagne.dateExecution'), type: 'datetime' },
    { key: 'versionScenario', header: t('campagne.versionScenario'), type: 'mono' },
    { key: 'referenceTrace', header: t('campagne.referenceTrace'), type: 'mono' },
  ]
  return (
    <div className="gm-col">
      <InlineAlert level="info">{t('resultats.reprisNonRecalcules')}</InlineAlert>
      <DataTable columns={colonnes} rows={liaison.resultats} rowKey={(c) => c.id} state={liaison.resultats.length ? 'ok' : 'empty'} emptyText={t('campagne.aucun')} />
    </div>
  )
}

function OngletCouvertures({ liaison }: { liaison: PortailLiaison }) {
  const { t } = useTranslation()
  return (
    <div className="gm-col">
      <LiaisonProgress {...versLiaisonProgress(liaison)} />
      {couverturesNonCalculables(liaison).map((m, i) => (
        <InlineAlert key={i} level="info">{t('couverture.nonCalculable', { motif: m })}</InlineAlert>
      ))}
    </div>
  )
}

function AttestationPortail({ certification }: { certification: PortailCertification & { attestation: NonNullable<PortailCertification['attestation']> } }) {
  const { t } = useTranslation()
  const telecharger = useTelechargerAttestationPortail()
  const [verifiee, setVerifiee] = useState<boolean | null | undefined>(undefined)
  const a = certification.attestation
  const agir = () =>
    telecharger.mutateAsync(certification.id).then((f) => {
      enregistrerFichier(f.blob, `${a.numero}.pdf`)
      setVerifiee(verifierEmpreinte(f.empreinte, a.empreinteDocument))
    })
  return (
    <div className="gm-col">
      <KeyValueList
        items={[
          { key: t('attestation.numero'), value: a.numero, mono: true },
          { key: t('registre.etat'), value: <StatusBadge status={a.etat === 'EN_VIGUEUR' ? 'ok' : a.etat === 'REVOQUEE' ? 'crit' : 'warn'} label={t(`attestation.etat.${a.etat}`)} /> },
          { key: t('attestation.empreinte'), value: <Hash value={a.empreinteDocument} onVerify={() => void agir()} verified={verifiee} /> },
        ]}
      />
      {verifiee === true ? <InlineAlert level="info">{t('attestation.empreinteVerifiee')}</InlineAlert> : null}
      {verifiee === null ? <InlineAlert level="warn">{t('attestation.empreinteIndisponible')}</InlineAlert> : null}
      {verifiee === false ? <InlineAlert level="crit" title={t('attestation.empreinteDivergente')}>{t('attestation.empreinteDivergente.texte')}</InlineAlert> : null}
      {telecharger.error ? <InlineAlert level="crit" correlationId={telecharger.error.correlationId || undefined}>{telecharger.error.message}</InlineAlert> : null}
      <div>
        <Button variant="primary" size="lg" icon="download" loading={telecharger.isPending} onClick={() => void agir()}>{t('attestation.action.telecharger')}</Button>
      </div>
    </div>
  )
}

function OngletCertification({ liaisonId }: { liaisonId: string }) {
  const { t } = useTranslation()
  const e = useLibellesEtats()
  const q = useCertificationsPortail()
  return (
    <EtatsRequete etat={q} isEmpty={(l) => !l.some((c) => c.liaisonId === liaisonId)} emptyTitle={t('portail.certification.aucune')} errorTitle={e.erreur} retryLabel={e.reessayer}>
      {(l) => {
        const c = l.find((x) => x.liaisonId === liaisonId)
        if (!c) return null
        return (
          <div className="gm-col">
            <KeyValueList
              items={[
                { key: t('dossier.etat'), value: <StatusBadge status={etatDossier[c.etatDossier]?.status ?? 'neutral'} label={t(`dossier.etat.${c.etatDossier}`)} /> },
                ...(c.motifCommunicable ? [{ key: t('portail.motifRefus'), value: c.motifCommunicable }] : []),
              ]}
            />
            {c.attestation ? <AttestationPortail certification={{ ...c, attestation: c.attestation }} /> : <EmptyState icon="shield-check" title={t('attestation.aucune')} text={t('portail.attestation.aVenir')} />}
          </div>
        )
      }}
    </EtatsRequete>
  )
}

function FicheLiaison({ liaison, onglet }: { liaison: PortailLiaison; onglet?: string }) {
  const base = useDescripteurDS(portailLiaisonDescriptor)
  const descripteur = useMemo(() => {
    const rendus: Record<string, (l: PortailLiaison) => ReactNode> = {
      'a-faire': (l) => <OngletAFaire liaisonId={l.id} />,
      parcours: (l) => <OngletParcours liaison={l} />,
      resultats: (l) => <OngletResultats liaison={l} />,
      couvertures: (l) => <OngletCouvertures liaison={l} />,
      certification: (l) => <OngletCertification liaisonId={l.id} />,
    }
    // Sur la fiche, l'action « ouvrir » n'a pas d'objet : elle ne sert qu'à la liste.
    return { ...base, actions: [], tabs: base.tabs?.map((o) => ({ ...o, render: rendus[o.key] })) }
  }, [base])
  const { t } = useTranslation()
  return <ResourceDetail descriptor={descripteur} row={avecTypeProjetLisible(liaison, t)} mode="page" tab={onglet ?? 'a-faire'} />
}

function Fiche({ id }: { id: string }) {
  const { t } = useTranslation()
  const [recherche] = useSearchParams()
  const q = useMaLiaison(id)
  return (
    <EtatsRequete etat={q} emptyTitle={t('portail.introuvable')} errorTitle={t('portail.introuvable')} retryLabel={t('commun.reessayer')}>
      {(l) => <FicheLiaison liaison={l} onglet={recherche.get('onglet') ?? undefined} />}
    </EtatsRequete>
  )
}

function Liste() {
  const { t } = useTranslation()
  const q = useMesLiaisons()
  const descripteur = useDescripteurDS(portailLiaisonDescriptor)
  return (
    <ResourceList
      descriptor={descripteur}
      rows={(q.data ?? []).map((l) => avecTypeProjetLisible(l, t))}
      total={q.data?.length ?? 0}
      state={q.isPending ? 'loading' : q.isError ? 'error' : q.data?.length === 0 ? 'empty' : 'ok'}
      error={q.error ?? undefined}
      liveUpdates={false}
    />
  )
}

export default function MaLiaisonPage() {
  const { id } = useParams()
  return id ? <Fiche id={id} /> : <Liste />
}
