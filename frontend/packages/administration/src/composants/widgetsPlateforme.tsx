import { useTranslation } from 'react-i18next'
import { InlineAlert, Link, LinkList, PageSection, Skeleton, StatGrid, type Status } from '@gimac/design-system'
import { useAlertes, useCharges, useServicesPlateforme } from '../hooks'
import type { Charge } from '../modele/infrastructure'
import { useChemins } from '../services/useServicesConsole'
import { STATUT_PLATEFORME } from './supervision'

// Widgets de plateforme des tableaux de bord (D-79) : résumé de la supervision (état global, services,
// alertes) pour PLATFORM_SUPERVISION_READ, et charges de travail en erreur pour PLATFORM_OPS_READ (D-78).
// Chacun mène à son écran, dans l'application courante (console ou application interne).

/** Charge à surveiller : en erreur, partielle ou de santé dégradée. */
export const enErreur = (c: Charge) => c.etat === 'EN_ERREUR' || c.etat === 'PARTIELLE' || c.sante === 'DEGRADEE'
export const STATUT_CHARGE: Record<Charge['etat'], Status> = { EN_COURS: 'ok', ARRETEE: 'neutral', REDEMARRAGE: 'info', EN_ERREUR: 'crit', PARTIELLE: 'warn' }

export function WidgetSupervision() {
  const { t } = useTranslation()
  const { plateforme } = useChemins()
  const services = useServicesPlateforme()
  const alertes = useAlertes()
  const i = services.data
  const n = alertes.data ? (alertes.data.joignable ? alertes.data.nombre : -1) : undefined
  return (
    <PageSection title={t('console.supervision.titre')} description={t('infra.widget.supervision.texte')}
      actions={<Link href={`${plateforme}/supervision`}>{t('infra.widget.supervision.ouvrir')}</Link>}>
      {services.isPending ? <Skeleton height={96} /> : services.isError || !i ? (
        <InlineAlert level="crit" title={t('commun.erreur.titre')} correlationId={services.error?.correlationId || undefined} />
      ) : (
        <StatGrid label={t('console.supervision.titre')} items={[
          { key: 'etat', label: t('console.plateforme.etatGlobal'), value: t(`console.plateforme.etat.${i.global.etat}`), status: STATUT_PLATEFORME[i.global.etat],
            statusLabel: t(`console.plateforme.etat.${i.global.etat}`) },
          { key: 'services', label: t('console.plateforme.servicesUp'), value: `${i.global.up} / ${i.global.total}`, status: i.global.down > 0 ? 'warn' : 'ok',
            statusLabel: t('console.plateforme.servicesDown', { n: i.global.down }) },
          { key: 'alertes', label: t('console.alertes.actives'), value: n === undefined ? '…' : n < 0 ? t('console.alertes.injoignable') : n,
            status: n && n > 0 ? 'warn' : 'ok', statusLabel: t(n && n > 0 ? 'console.aTraiter' : 'console.aJour') },
        ]} />
      )}
    </PageSection>
  )
}

export function WidgetChargesEnErreur() {
  const { t } = useTranslation()
  const { plateforme } = useChemins()
  const requete = useCharges()
  const e = requete.data
  const enAlerte = (e?.charges ?? []).filter(enErreur)
  return (
    <PageSection title={t('infra.widget.titre')} description={t('infra.widget.texte')}
      actions={<Link href={`${plateforme}/infrastructure`}>{t('infra.widget.ouvrir')}</Link>}>
      {requete.isPending ? <Skeleton height={64} /> : requete.isError || !e ? (
        <InlineAlert level="crit" title={t('commun.erreur.titre')} correlationId={requete.error?.correlationId || undefined} />
      ) : e.orchestrateur === 'AUCUN' ? (
        <InlineAlert level="info" title={t('infra.nonConfiguree.titre')}>{t('infra.nonConfiguree.texte')}</InlineAlert>
      ) : (
        <>
          <StatGrid label={t('infra.widget.titre')} items={[
            { key: 'charges', label: t('infra.widget.charges'), value: e.charges.length },
            { key: 'enErreur', label: t('infra.widget.enErreur'), value: enAlerte.length, status: enAlerte.length > 0 ? 'crit' : 'ok',
              statusLabel: t(enAlerte.length > 0 ? 'console.aTraiter' : 'console.aJour') },
          ]} />
          <LinkList label={t('infra.widget.titre')} pagination={false} empty={t('infra.widget.aucune')}
            items={enAlerte.map((c) => ({ key: c.nom, href: `${plateforme}/infrastructure?charge=${encodeURIComponent(c.nom)}`, label: c.nom,
              status: STATUT_CHARGE[c.etat], statusLabel: t(`infra.etat.${c.etat}`), meta: t('infra.widget.repliques', { pretes: c.repliques.pretes, voulues: c.repliques.voulues }) }))} />
        </>
      )}
    </PageSection>
  )
}
