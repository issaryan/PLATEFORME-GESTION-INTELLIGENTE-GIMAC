import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { DataTable, Sparkline, StatusBadge, type Column, type Status } from '@gimac/design-system'
import { useServicesSupervises } from '../hooks'
import type { MesureService, NiveauSante } from '../modele'
import { etatListe, useFormat } from './communs'
import { PaginationSupervision } from './PaginationSupervision'

// Mesures par service lues dans SBX-TRACE (cycle de l'algorithme 6.49) : état confirmé, disponibilité sur 24 heures,
// latences, débit, erreurs 5xx, files Kafka et rebuts d'outbox. Une valeur absente n'a pas été mesurée (tiret).
export const TAILLE_SERVICES = 20
const STATUT: Record<NiveauSante, Status> = { NOMINAL: 'ok', DEGRADE: 'warn', INDISPONIBLE: 'crit', EN_MAINTENANCE: 'info' }

export function ServicesSupervision() {
  const { t, i18n } = useTranslation()
  const f = useFormat()
  const [page, setPage] = useState(0)
  const services = useServicesSupervises(page, TAILLE_SERVICES)
  const lignes = services.data?.elements ?? []
  const nombre = (v: number | null, unite = '') => (v === null ? '—' : v.toLocaleString(i18n.language) + unite)
  const colonnes: Column<MesureService>[] = [
    { key: 'service', header: t('sbx.supervision.services.service'), type: 'mono' },
    { key: 'module', header: t('sbx.supervision.module'), type: 'mono' },
    { key: 'niveau', header: t('sbx.supervision.services.etat'), render: (x) => x.niveau
      ? <StatusBadge status={x.joignable ? STATUT[x.niveau] : 'crit'} label={x.joignable ? t(`sbx.supervision.niveau.${x.niveau}`) : t('sbx.supervision.services.injoignable')} />
      : <StatusBadge status="neutral" label={t('sbx.supervision.services.nonMesure')} /> },
    { key: 'disponibilite24h', header: t('sbx.supervision.services.disponibilite'), render: (x) => (x.disponibilite24h === null ? '—' : `${(x.disponibilite24h / 100).toFixed(2)} %`) },
    { key: 'latenceMoyenneMs', header: t('sbx.supervision.services.latence'), render: (x) => `${nombre(x.latenceMoyenneMs, ' ms')} / ${nombre(x.latenceMaxMs, ' ms')}` },
    { key: 'serie', header: t('sbx.supervision.services.tendance'), render: (x) => <Sparkline points={x.serie.map((p) => p.latenceMoyenneMs)} label={t('sbx.supervision.services.tendanceDe', { service: x.service })} unit="ms" /> },
    { key: 'debitParMinute', header: t('sbx.supervision.services.debit'), render: (x) => nombre(x.debitParMinute) },
    { key: 'erreurs', header: t('sbx.supervision.services.erreurs'), render: (x) => nombre(x.erreurs) },
    { key: 'retardKafka', header: t('sbx.supervision.services.files'), render: (x) => nombre(x.retardKafka) },
    { key: 'rebuts', header: t('sbx.supervision.services.rebuts'), render: (x) => nombre(x.rebuts) },
    { key: 'mesureLe', header: t('sbx.supervision.services.mesureLe'), render: (x) => f.instant(x.mesureLe) },
  ]
  return <PaginationSupervision titre={t('sbx.supervision.services.titre')} page={page} taille={TAILLE_SERVICES} changerPage={setPage} requete={services}>
    <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.service} pagination={false} emptyText={t('sbx.supervision.services.aucun')} emptyHelp={t('sbx.supervision.services.aucunAide')} {...etatListe(services, lignes)} onRetry={() => void services.refetch()} />
  </PaginationSupervision>
}
