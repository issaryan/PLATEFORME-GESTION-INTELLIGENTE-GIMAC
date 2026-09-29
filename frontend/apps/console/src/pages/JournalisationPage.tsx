import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, DataTable, Dialog, EmptyState, SearchInput, Select, StatusBadge, TextArea, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useMessageErreur } from '@gimac/compte'
import { cles, useConfigurationPlateforme, useEffet, useJournalisation } from '../hooks'
import type { NiveauLogger } from '../modele/plateforme'
import { Annonce } from '../composants/Annonce'
import { type Retour } from '../composants/actes'
import { etatListe } from '../composants/formats'

// Journalisation des services (D-61) : niveaux des loggers d'un service (PLATFORM_CONFIG_READ), filtrés
// par préfixe ; changement à chaud avec motif (PLATFORM_CONFIG_EDIT, super administrateur seulement),
// journalisé par la passerelle. Le RSSI consulte sans pouvoir modifier.

function Modifier({ logger, niveaux, onFermer, onValider }: { logger: NiveauLogger; niveaux: string[]; onFermer: () => void; onValider: (niveau: string, motif: string) => void }) {
  const { t } = useTranslation()
  const [niveau, setNiveau] = useState(logger.niveauConfigure ?? logger.niveauEffectif ?? 'INFO')
  const [motif, setMotif] = useState('')
  const ok = motif.trim().length >= 5 && motif.trim().length <= 500
  return (
    <Dialog open id="gm-dlg-niveau" title={t('console.journalisation.modifier')} description={logger.nom} onClose={onFermer}
      footer={[
        <Button key="a" variant="secondary" onClick={onFermer}>{t('commun.annuler')}</Button>,
        <Button key="b" variant="primary" disabled={!ok} disabledReason={t('console.journalisation.motif.aide')} onClick={() => onValider(niveau, motif.trim())}>{t('console.journalisation.appliquer')}</Button>,
      ]}>
      <div className="gm-col">
        <Select name="niveau" label={t('console.journalisation.niveau')} value={niveau} onChange={setNiveau}
          options={[...niveaux.map((n) => ({ value: n, label: n })), { value: 'NULL', label: t('console.journalisation.heriter') }]} />
        <TextArea name="motifNiveau" label={t('console.motif')} required value={motif} onChange={setMotif} help={t('console.journalisation.motif.aide')} />
      </div>
    </Dialog>
  )
}

export default function JournalisationPage() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const message = useMessageErreur()
  const [params, setParams] = useSearchParams()
  const service = params.get('service') ?? ''
  const [filtre, setFiltre] = useState('')
  const configuration = useConfigurationPlateforme()
  const niveaux = useJournalisation(service, filtre.trim() || undefined)
  const modifier = useEffet((s, v: { logger: string; niveau: string; motif: string }) => s.plateforme.modifierJournalisation(service, v), [cles.plateforme])
  const [ouvert, setOuvert] = useState<NiveauLogger | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const edition = aPermission(permissions, 'PLATFORM_CONFIG_EDIT')
  const lignes = niveaux.data?.loggers ?? []

  const colonnes: Column<NiveauLogger>[] = [
    { key: 'nom', header: t('console.journalisation.logger'), render: (l) => <span className="gm-mono">{l.nom}</span> },
    { key: 'niveauConfigure', header: t('console.journalisation.configure'), render: (l) => l.niveauConfigure ?? t('console.journalisation.herite') },
    { key: 'niveauEffectif', header: t('console.journalisation.effectif'), render: (l) => <StatusBadge status={l.niveauEffectif === 'DEBUG' || l.niveauEffectif === 'TRACE' ? 'warn' : 'neutral'} label={l.niveauEffectif ?? '—'} /> },
    ...(edition
      ? [{ key: 'actions', header: '', type: 'actions' as const, render: (l: NiveauLogger) => <Button size="sm" onClick={() => setOuvert(l)}>{t('console.journalisation.modifier')}</Button> }]
      : []),
  ]

  return (
    <div className="gm-col">
      <h1 className="gm-heading-display">{t('console.journalisation.titre')}</h1>
      <p style={{ margin: 0 }}>{t(edition ? 'console.journalisation.texte' : 'console.journalisation.lecture')}</p>
      <div className="gm-row" style={{ alignItems: 'flex-end' }}>
        <div style={{ width: 280 }}>
          <Select name="service" label={t('console.journalisation.service')} value={service}
            onChange={(v) => {
              setRetour(null)
              setParams(v ? { service: v } : {})
            }}
            options={[{ value: '', label: t('console.journalisation.choisir') }, ...(configuration.data?.services ?? []).map((s) => ({ value: s.nom, label: s.nom }))]} />
        </div>
        <SearchInput label={t('console.journalisation.filtre')} value={filtre} onChange={setFiltre} />
      </div>
      <Annonce retour={retour} />
      {service ? (
        <DataTable columns={colonnes} rows={lignes} rowKey={(l) => l.nom} density="compact" onRetry={() => void niveaux.refetch()}
          emptyText={t('console.journalisation.aucun')} {...etatListe(niveaux, lignes)} />
      ) : (
        <EmptyState icon="file-text" title={t('console.journalisation.choisir')} />
      )}
      {ouvert ? (
        <Modifier logger={ouvert} niveaux={niveaux.data?.niveauxDisponibles ?? []} onFermer={() => setOuvert(null)} onValider={(niveau, motif) => {
          const logger = ouvert.nom
          setOuvert(null)
          modifier.mutateAsync({ logger, niveau, motif }).then(
            (c) => setRetour({ niveau: 'info', message: { texte: t('console.journalisation.applique', { logger: c.logger, niveau: c.niveau }) } }),
            (e: unknown) => setRetour({ niveau: 'crit', message: message(e) }),
          )
        }} />
      ) : null}
    </div>
  )
}
