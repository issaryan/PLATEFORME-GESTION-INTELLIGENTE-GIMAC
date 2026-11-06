import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { DataTable, Sparkline, StatusBadge, type Column, type Status } from '@gimac/design-system'
import { useServicesSupervises } from '../hooks'
import type { MesureService, NiveauSante } from '../modele'
import { etatListe } from './communs'
import { Discret, NomCode, useLibellesSupervision } from './libellesSupervision'
import { PaginationSupervision } from './PaginationSupervision'

// Mesures par service lues dans SBX-TRACE (cycle de l'algorithme 6.49) : état confirmé, disponibilité sur 24 heures,
// latences, débit, erreurs 5xx, files Kafka et rebuts d'outbox. Une valeur absente n'a pas été mesurée (tiret) ; un
// service mesuré sans requête métier est « sans activité ». La date du dernier cycle est donnée une fois, en tête.
export const TAILLE_SERVICES = 20
const STATUT: Record<NiveauSante, Status> = { NOMINAL: 'ok', DEGRADE: 'warn', INDISPONIBLE: 'crit', EN_MAINTENANCE: 'info' }

/** Date du dernier cycle : « 05/11/2026 » et « 19 h 46 UTC » en français, forme courte de la langue ailleurs. */
export function dateMesures(iso: string, langue: string): { date: string; heure: string } {
  const d = new Date(iso)
  const date = d.toLocaleDateString(langue, { timeZone: 'UTC' })
  const h = d.getUTCHours()
  const m = String(d.getUTCMinutes()).padStart(2, '0')
  const heure = langue.startsWith('fr') ? `${h} h ${m}` : d.toLocaleTimeString(langue, { hour: '2-digit', minute: '2-digit', timeZone: 'UTC' })
  return { date, heure: `${heure} UTC` }
}

export function ServicesSupervision() {
  const { t, i18n } = useTranslation()
  const l = useLibellesSupervision()
  const [page, setPage] = useState(0)
  const services = useServicesSupervises(page, TAILLE_SERVICES)
  const lignes = services.data?.elements ?? []
  const nombre = (v: number | null, decimales = 0) => (v === null ? '—' : l.nombre(v, decimales))
  const inactif = (x: MesureService) => x.joignable && x.latenceMoyenneMs === null && x.debitParMinute === 0
  const derniere = lignes.map((x) => x.mesureLe).filter((x): x is string => x !== null).sort().at(-1)
  const colonnes: Column<MesureService>[] = [
    { key: 'service', header: t('sbx.supervision.services.service'), render: (x) => <NomCode code={x.service} /> },
    { key: 'module', header: t('sbx.supervision.module'), render: (x) => l.nom(x.module) },
    { key: 'niveau', header: t('sbx.supervision.services.etat'), render: (x) => x.niveau
      ? <StatusBadge status={x.joignable ? STATUT[x.niveau] : 'crit'} label={x.joignable ? t(`sbx.supervision.niveau.${x.niveau}`) : t('sbx.supervision.services.injoignable')} />
      : <StatusBadge status="neutral" label={t('sbx.supervision.services.nonMesure')} /> },
    { key: 'disponibilite24h', header: t('sbx.supervision.services.disponibilite'), render: (x) => (x.disponibilite24h === null ? '—' : l.pourcent(x.disponibilite24h)) },
    { key: 'latenceMoyenneMs', header: t('sbx.supervision.services.latence'), render: (x) => inactif(x)
      ? <Discret>{t('sbx.supervision.services.sansActivite')}</Discret>
      : `${nombre(x.latenceMoyenneMs)} / ${nombre(x.latenceMaxMs)} ms` },
    { key: 'serie', header: t('sbx.supervision.services.tendance'), render: (x) => {
      const points = x.serie.map((p) => p.latenceMoyenneMs)
      // Mini-courbe seulement à partir de deux mesures : un point isolé ne dit rien de la tendance.
      return points.filter((p) => p !== null).length >= 2
        ? <Sparkline points={points} label={t('sbx.supervision.services.tendanceDe', { service: l.nom(x.service) })} unit="ms" aire hauteur={28} />
        : null
    } },
    { key: 'debitParMinute', header: t('sbx.supervision.services.debit'), render: (x) => nombre(x.debitParMinute, x.debitParMinute === 0 ? 0 : 2) },
    { key: 'erreurs', header: t('sbx.supervision.services.erreurs'), render: (x) => nombre(x.erreurs) },
    { key: 'retardKafka', header: t('sbx.supervision.services.files'), render: (x) => nombre(x.retardKafka) },
    { key: 'rebuts', header: t('sbx.supervision.services.rebuts'), render: (x) => nombre(x.rebuts) },
  ]
  return <PaginationSupervision titre={t('sbx.supervision.services.titre')} page={page} taille={TAILLE_SERVICES} changerPage={setPage} requete={services}>
    {derniere ? <p className="gm-type-small" style={{ margin: 0 }}>{t('sbx.supervision.services.mesuresDu', dateMesures(derniere, i18n.language))}</p> : null}
    <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.service} pagination={false} emptyText={t('sbx.supervision.services.aucun')} emptyHelp={t('sbx.supervision.services.aucunAide')} {...etatListe(services, lignes)} onRetry={() => void services.refetch()} />
  </PaginationSupervision>
}
