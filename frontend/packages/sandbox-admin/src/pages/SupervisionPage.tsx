import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, DataTable, InlineAlert, PageHeader, PageSection, StatusBadge, type Column, type Status } from '@gimac/design-system'
import { useAlertesSandbox, useDisponibilites, useElementsSupervises, useEtatSante } from '../hooks'
import type { Alerte, Disponibilite, ElementSupervise, NiveauSante, PageSupervision } from '../modele'
import { etatListe, useFormat } from '../composants/communs'

export const TAILLE_SUPERVISION = 10
const STATUT_SANTE: Record<NiveauSante, Status> = { NOMINAL: 'ok', DEGRADE: 'warn', INDISPONIBLE: 'crit', EN_MAINTENANCE: 'info' }

type RequetePage<T> = { data?: PageSupervision<T>; isPending: boolean; isError: boolean; refetch: () => unknown }

function Pagination<T>({ titre, page, changerPage, requete, children }: { titre: string; page: number; changerPage: (page: number) => void; requete: RequetePage<T>; children: ReactNode }) {
  const { t } = useTranslation()
  const total = requete.data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / TAILLE_SUPERVISION))
  return <PageSection title={titre} description={t('sbx.supervision.page', { page: page + 1, pages, total })} actions={<span className="gm-row">
    <Button variant="ghost" icon="arrow-left" disabled={page === 0} onClick={() => changerPage(page - 1)}>{t('sbx.supervision.precedente')}</Button>
    <Button variant="ghost" icon="arrow-right" disabled={(page + 1) * TAILLE_SUPERVISION >= total} onClick={() => changerPage(page + 1)}>{t('sbx.supervision.suivante')}</Button>
  </span>}>{children}</PageSection>
}

export default function SupervisionPage() {
  const { t } = useTranslation()
  const f = useFormat()
  const sante = useEtatSante()
  const [pageElements, setPageElements] = useState(0)
  const [pageAlertes, setPageAlertes] = useState(0)
  const [pageDisponibilites, setPageDisponibilites] = useState(0)
  const elements = useElementsSupervises(pageElements, TAILLE_SUPERVISION)
  const alertes = useAlertesSandbox(pageAlertes, TAILLE_SUPERVISION)
  const disponibilites = useDisponibilites(pageDisponibilites, TAILLE_SUPERVISION)
  const lignesElements = elements.data?.elements ?? []
  const lignesAlertes = alertes.data?.elements ?? []
  const lignesDisponibilites = disponibilites.data?.elements ?? []
  const colonnesElements: Column<ElementSupervise>[] = [
    { key: 'nature', header: t('sbx.supervision.nature'), type: 'mono' },
    { key: 'module', header: t('sbx.supervision.module'), type: 'mono' },
    { key: 'codeParticipant', header: t('sbx.supervision.participant'), render: (x) => x.codeParticipant ?? t('sbx.supervision.tous') },
    { key: 'criticite', header: t('sbx.supervision.criticite'), render: (x) => t(`sbx.supervision.criticite.${x.criticite}`) },
    { key: 'externe', header: t('sbx.supervision.externe'), render: (x) => t(x.externe ? 'sbx.supervision.oui' : 'sbx.supervision.non') },
    { key: 'enMaintenance', header: t('sbx.supervision.maintenance'), render: (x) => t(x.enMaintenance ? 'sbx.supervision.oui' : 'sbx.supervision.non') },
  ]
  const colonnesAlertes: Column<Alerte>[] = [
    { key: 'criticite', header: t('sbx.supervision.criticite'), render: (x) => t(`sbx.supervision.criticite.${x.criticite}`) },
    { key: 'message', header: t('sbx.supervision.message') },
    { key: 'element', header: t('sbx.supervision.nature'), render: (x) => x.element.nature },
    { key: 'etat', header: t('sbx.supervision.etatAlerte'), render: (x) => t(`sbx.supervision.etatAlerte.${x.etat}`) },
    { key: 'leveeLe', header: t('sbx.supervision.leveeLe'), render: (x) => f.instant(x.leveeLe) },
  ]
  const colonnesDisponibilites: Column<Disponibilite>[] = [
    { key: 'module', header: t('sbx.supervision.module'), type: 'mono' },
    { key: 'periode', header: t('sbx.supervision.periode'), type: 'mono' },
    { key: 'valeur', header: t('sbx.supervision.disponibilite'), render: (x) => `${(x.valeur / 100).toFixed(2)} %` },
    { key: 'publieLe', header: t('sbx.supervision.publieLe'), render: (x) => f.instant(x.publieLe) },
  ]
  return <div className="gm-col">
    <PageHeader title={t('sbx.supervision.titre')} subtitle={t('sbx.supervision.sousTitre')} />
    <PageSection title={t('sbx.supervision.sante')}>
      {sante.isPending ? <p>{t('sbx.supervision.chargement')}</p> : null}
      {sante.isError ? <InlineAlert level="crit" title={t('sbx.supervision.erreur')}><Button variant="ghost" onClick={() => void sante.refetch()}>{t('sbx.supervision.reessayer')}</Button></InlineAlert> : null}
      {sante.data ? <div className="gm-col">
        <p><StatusBadge status={STATUT_SANTE[sante.data.niveauGlobal]} label={t(`sbx.supervision.niveau.${sante.data.niveauGlobal}`)} /> {f.instant(sante.data.calculeLe)}</p>
        <p>{t('sbx.supervision.causeExterne')}: {t(sante.data.causeExterne ? 'sbx.supervision.oui' : 'sbx.supervision.non')}</p>
        <pre aria-label={t('sbx.supervision.detailModules')}>{sante.data.etatsModules}</pre>
      </div> : null}
    </PageSection>
    <Pagination titre={t('sbx.supervision.elements')} page={pageElements} changerPage={setPageElements} requete={elements}>
      <DataTable columns={colonnesElements} rows={lignesElements} rowKey={(x) => x.id} pagination={false} emptyText={t('sbx.supervision.aucunElement')} emptyHelp={t('sbx.supervision.aucunElementAide')} {...etatListe(elements, lignesElements)} onRetry={() => void elements.refetch()} />
    </Pagination>
    <Pagination titre={t('sbx.supervision.alertes')} page={pageAlertes} changerPage={setPageAlertes} requete={alertes}>
      <DataTable columns={colonnesAlertes} rows={lignesAlertes} rowKey={(x) => x.id} pagination={false} emptyText={t('sbx.supervision.aucuneAlerte')} emptyHelp={t('sbx.supervision.aucuneAlerteAide')} {...etatListe(alertes, lignesAlertes)} onRetry={() => void alertes.refetch()} />
    </Pagination>
    <Pagination titre={t('sbx.supervision.disponibilites')} page={pageDisponibilites} changerPage={setPageDisponibilites} requete={disponibilites}>
      <DataTable columns={colonnesDisponibilites} rows={lignesDisponibilites} rowKey={(x) => x.id} pagination={false} emptyText={t('sbx.supervision.aucuneDisponibilite')} emptyHelp={t('sbx.supervision.aucuneDisponibiliteAide')} {...etatListe(disponibilites, lignesDisponibilites)} onRetry={() => void disponibilites.refetch()} />
    </Pagination>
  </div>
}
