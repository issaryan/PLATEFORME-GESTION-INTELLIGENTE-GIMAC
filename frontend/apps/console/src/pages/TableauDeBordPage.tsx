import { useTranslation } from 'react-i18next'
import { Card, DataTable, EmptyState, InlineAlert, Link, LiveStatGroup, Skeleton, StatCard, StatusBadge, Tag, type Column, type Status } from '@gimac/design-system'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import { dateHeure, useLibelleProfil } from '@gimac/compte'
import { useAlertes, useFluxPlateforme, useIndicateursComptes, useJournal, useServeurs, useServicesPlateforme } from '../hooks'
import type { Alerte, EtatConteneur, EtatHote, EtatPlateforme, EtatService } from '../modele/plateforme'
import type { EntreeJournal } from '../modele/comptes'
import { Donnees, Section } from '../composants/communs'
import { etatDirect, grille, nombre, octets, pourcent } from '../composants/formats'

// Tableau de bord de la console (D-61, D-62), composé selon les permissions de la session :
// - super administrateur (PLATFORM_SUPERVISION_READ et IAM_ROLE_PRIVILEGED_GRANT) : état de la plateforme
//   en temps réel (flux SSE /api/plateforme/flux), services, serveurs, alertes, indicateurs des comptes ;
// - RSSI (IAM_ACCOUNT_READ sans IAM_ROLE_PRIVILEGED_GRANT) : posture de sécurité des comptes, derniers
//   actes d'administration, politique de sécurité, et la supervision si la session l'ouvre.

const STATUT_PLATEFORME: Record<EtatPlateforme, Status> = { OPERATIONNELLE: 'ok', DEGRADEE: 'warn', HORS_SERVICE: 'crit' }
const statutService = (e: EtatService['etat']): Status => (e === 'UP' ? 'ok' : e === 'INCONNU' ? 'neutral' : 'crit')
const statutSeverite = (s: string | null): Status => (s === 'critical' ? 'crit' : s === 'warning' ? 'warn' : 'info')
const SEVERITES = ['critical', 'warning', 'info'] as const

function SectionSupervision() {
  const { t, i18n } = useTranslation()
  const flux = useFluxPlateforme()
  const services = useServicesPlateforme()
  const serveurs = useServeurs()
  const alertes = useAlertes()
  const instantane = flux.impulsion?.instantane ?? services.data
  const nombreAlertes = flux.impulsion ? flux.impulsion.nombreAlertes : alertes.data?.nombre

  const colServices: Column<EtatService>[] = [
    { key: 'nom', header: t('console.plateforme.service'), render: (s) => <span className="gm-mono">{s.nom}</span> },
    { key: 'etat', header: t('console.plateforme.etat'), render: (s) => <StatusBadge status={statutService(s.etat)} label={t(`console.plateforme.sante.${s.etat}`)} /> },
    { key: 'latenceMs', header: t('console.plateforme.latence'), type: 'number', render: (s) => (s.latenceMs === null ? '—' : `${s.latenceMs} ms`) },
    { key: 'version', header: t('console.plateforme.version'), render: (s) => <span className="gm-mono">{s.info?.version ?? '—'}</span> },
    { key: 'cpu', header: t('console.plateforme.cpu'), type: 'number', render: (s) => pourcent(s.metriques?.cpuProcessus, 100) },
    { key: 'tas', header: t('console.plateforme.tas'), type: 'number', render: (s) => pourcent(s.metriques?.memoireTasPourcent) },
    {
      key: 'disque', header: t('console.plateforme.disque'), type: 'number',
      render: (s) => (s.metriques?.disqueTotalOctets && s.metriques.disqueLibreOctets !== null ? pourcent(1 - s.metriques.disqueLibreOctets / s.metriques.disqueTotalOctets, 100) : '—'),
    },
  ]
  const colHotes: Column<EtatHote>[] = [
    { key: 'instance', header: t('console.plateforme.hote'), render: (h) => <span className="gm-mono">{h.instance}</span> },
    { key: 'cpuPourcent', header: t('console.plateforme.cpu'), type: 'number', render: (h) => pourcent(h.cpuPourcent) },
    { key: 'memoirePourcent', header: t('console.plateforme.memoire'), type: 'number', render: (h) => pourcent(h.memoirePourcent) },
    { key: 'disquePourcent', header: t('console.plateforme.disque'), type: 'number', render: (h) => pourcent(h.disquePourcent) },
    { key: 'chargeMoyenne1m', header: t('console.plateforme.charge'), type: 'number', render: (h) => nombre(h.chargeMoyenne1m, 2) },
  ]
  const colConteneurs: Column<EtatConteneur>[] = [
    { key: 'nom', header: t('console.plateforme.conteneur'), render: (c) => <span className="gm-mono">{c.nom}</span> },
    { key: 'cpuCoeurs', header: t('console.plateforme.cpuCoeurs'), type: 'number', render: (c) => nombre(c.cpuCoeurs, 2) },
    { key: 'memoireOctets', header: t('console.plateforme.memoire'), type: 'number', render: (c) => octets(c.memoireOctets) },
  ]
  const colAlertes: Column<Alerte>[] = [
    { key: 'severite', header: t('console.alertes.severite'), render: (a) => <StatusBadge status={statutSeverite(a.severite)} label={t(`console.alertes.severite.${a.severite ?? 'info'}`, { defaultValue: a.severite ?? '' })} /> },
    { key: 'nom', header: t('console.alertes.nom'), render: (a) => <span className="gm-mono">{a.nom}</span> },
    { key: 'resume', header: t('console.alertes.resume'), render: (a) => a.resume ?? '—' },
    { key: 'cible', header: t('console.alertes.cible'), render: (a) => <span className="gm-mono">{a.cible ?? '—'}</span> },
    { key: 'depuis', header: t('console.alertes.depuis'), render: (a) => dateHeure(a.depuis, i18n.language) },
  ]

  return (
    <Section titre={t('console.supervision.titre')} id="gm-supervision">
      {flux.etat === 'perdu' ? <InlineAlert level="warn" title={t('console.flux.perdu.texte')} /> : null}
      {instantane ? (
        <LiveStatGroup
          title={t('console.plateforme.etatTitre')}
          level={3}
          state={etatDirect(flux.etat)}
          updatedAt={dateHeure(instantane.horodatage, i18n.language)}
          items={[
            { key: 'etat', label: t('console.plateforme.etatGlobal'), value: t(`console.plateforme.etat.${instantane.global.etat}`),
              status: STATUT_PLATEFORME[instantane.global.etat], statusLabel: t(`console.plateforme.etat.${instantane.global.etat}`) },
            { key: 'services', label: t('console.plateforme.servicesUp'), value: `${instantane.global.up} / ${instantane.global.total}`,
              status: instantane.global.down > 0 ? 'warn' : 'ok', statusLabel: t('console.plateforme.servicesDown', { n: instantane.global.down }) },
            { key: 'alertes', label: t('console.alertes.actives'), value: nombreAlertes === undefined ? '—' : nombreAlertes < 0 ? t('console.alertes.injoignable') : nombreAlertes,
              status: nombreAlertes && nombreAlertes > 0 ? 'warn' : 'ok', statusLabel: t(nombreAlertes && nombreAlertes > 0 ? 'console.aTraiter' : 'console.aJour') },
            ...(instantane.keycloak
              ? [{ key: 'keycloak', label: t('console.plateforme.keycloak'), value: instantane.keycloak.latenceMs === null ? '—' : `${instantane.keycloak.latenceMs} ms`,
                  status: statutService(instantane.keycloak.etat), statusLabel: t(`console.plateforme.sante.${instantane.keycloak.etat}`) }]
              : []),
          ]}
        />
      ) : services.isError ? (
        <InlineAlert level="crit" title={t('commun.erreur.titre')} correlationId={services.error?.correlationId || undefined}>{services.error?.message}</InlineAlert>
      ) : (
        <Skeleton height={96} />
      )}
      {instantane ? (
        <Card title={t('console.plateforme.services')}>
          <DataTable columns={colServices} rows={instantane.services} rowKey={(s) => s.nom} density="compact" />
        </Card>
      ) : null}
      <Card title={t('console.plateforme.serveurs')}>
        <Donnees requete={serveurs}>
          {(s) => (
            <div className="gm-col">
              {!s.prometheusJoignable ? <InlineAlert level="warn" title={t('console.plateforme.prometheusInjoignable')}>{s.erreur}</InlineAlert> : null}
              <span className="gm-field-help">{t('console.plateforme.cibles', { up: s.ciblesUp, down: s.ciblesDown })}</span>
              <DataTable columns={colHotes} rows={s.hotes} rowKey={(h) => h.instance} density="compact" emptyText={t('console.plateforme.aucunHote')} state={s.hotes.length ? 'ok' : 'empty'} />
              <DataTable columns={colConteneurs} rows={s.conteneurs} rowKey={(c) => c.nom} density="compact" emptyText={t('console.plateforme.aucunConteneur')} state={s.conteneurs.length ? 'ok' : 'empty'} />
            </div>
          )}
        </Donnees>
      </Card>
      <Card title={t('console.alertes.titre')}>
        <Donnees requete={alertes}>
          {(a) =>
            !a.joignable ? (
              <InlineAlert level="warn" title={t('console.alertes.injoignable')}>{a.erreur}</InlineAlert>
            ) : (
              <div className="gm-col">
                <div style={grille}>
                  {SEVERITES.map((s) => {
                    const n = a.alertes.filter((x) => (x.severite ?? 'info') === s).length
                    return <StatCard key={s} label={t(`console.alertes.severite.${s}`)} value={n} status={n > 0 ? statutSeverite(s) : 'ok'} statusLabel={t(n > 0 ? 'console.aTraiter' : 'console.aJour')} />
                  })}
                </div>
                <DataTable columns={colAlertes} rows={a.alertes} rowKey={(x) => `${x.nom}-${x.cible}`} density="compact" emptyText={t('console.alertes.aucune')} state={a.alertes.length ? 'ok' : 'empty'} />
              </div>
            )
          }
        </Donnees>
      </Card>
    </Section>
  )
}

function SectionComptes({ posture }: { posture: boolean }) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const indicateurs = useIndicateursComptes()
  return (
    <Section titre={t(posture ? 'console.posture.titre' : 'console.comptes.indicateurs')} id="gm-comptes" action={<Link href="/console/comptes">{t('console.voirComptes')}</Link>}>
      <Donnees requete={indicateurs}>
        {(i) => (
          <div className="gm-col">
            <div style={grille}>
              {posture ? null : <StatCard label={t('console.indicateur.total')} value={i.total} />}
              {posture ? null : <StatCard label={t('console.indicateur.actifs')} value={i.actifs} />}
              <StatCard label={t('console.indicateur.sansSecondFacteur')} value={i.sansSecondFacteur} status={i.sansSecondFacteur > 0 ? 'crit' : 'ok'}
                statusLabel={t(i.sansSecondFacteur > 0 ? 'console.aTraiter' : 'console.aJour')} />
              <StatCard label={t('console.indicateur.premierAcces')} value={i.premierAccesEnAttente} status={i.premierAccesEnAttente > 0 ? 'warn' : 'ok'}
                statusLabel={t(i.premierAccesEnAttente > 0 ? 'console.enAttente' : 'console.aJour')} />
              <StatCard label={t('console.indicateur.privilegies')} value={i.privilegies} />
              {posture ? <StatCard label={t('console.indicateur.crees30')} value={i.creesDepuis30Jours} /> : <StatCard label={t('console.indicateur.inactifs')} value={i.inactifs} />}
              <StatCard label={t('console.indicateur.actes24')} value={i.actesDepuis24Heures} />
            </div>
            {posture ? (
              <Card title={t('console.indicateur.parProfil')}>
                <span className="gm-row">
                  {Object.entries(i.parProfil).map(([p, n]) => <Tag key={p}>{`${libelleProfil(p)} : ${n}`}</Tag>)}
                </span>
              </Card>
            ) : null}
          </div>
        )}
      </Donnees>
    </Section>
  )
}

function SectionActes() {
  const { t, i18n } = useTranslation()
  const journal = useJournal({ page: 1, taille: 5 })
  const colonnes: Column<EntreeJournal>[] = [
    { key: 'horodatage', header: t('console.journal.horodatage'), render: (j) => dateHeure(j.horodatage, i18n.language) },
    { key: 'acteurNom', header: t('console.journal.acteur'), render: (j) => j.acteurNom ?? j.acteurId },
    { key: 'action', header: t('console.journal.action'), render: (j) => t(`console.journal.action.${j.action}`, { defaultValue: j.action }) },
    { key: 'cibleLibelle', header: t('console.journal.cible'), render: (j) => <span className="gm-mono">{j.cibleLibelle ?? '—'}</span> },
    { key: 'motif', header: t('console.journal.motif'), render: (j) => j.motif ?? '—' },
  ]
  return (
    <Section titre={t('console.actes.titre')} id="gm-actes" action={<Link href="/console/journal">{t('console.actes.voirTout')}</Link>}>
      <Donnees requete={journal} estVide={(p) => p.contenu.length === 0} vide={t('console.journal.vide')}>
        {(p) => <DataTable columns={colonnes} rows={p.contenu} rowKey={(j) => j.id} density="compact" />}
      </Donnees>
    </Section>
  )
}

export default function TableauDeBordPage() {
  const { t } = useTranslation()
  const session = useSession()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil()
  const privilegie = aPermission(permissions, 'IAM_ROLE_PRIVILEGED_GRANT')
  const supervision = aPermission(permissions, 'PLATFORM_SUPERVISION_READ')
  const lecteurComptes = aPermission(permissions, 'IAM_ACCOUNT_READ')
  const superAdmin = supervision && privilegie
  const rssi = lecteurComptes && !privilegie
  const profils = (session?.roles ?? []).filter((r) => r !== 'ROLE_USER')
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <header className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        <h1 className="gm-heading-display">{t(superAdmin ? 'console.tableau.superAdmin' : rssi ? 'console.tableau.rssi' : 'console.tableau.titre')}</h1>
        {session ? (
          <div className="gm-row">
            <span>{t('console.tableau.bienvenue', { nom: session.nom })}</span>
            {profils.map((r) => <Tag key={r}>{libelleProfil(r)}</Tag>)}
          </div>
        ) : null}
      </header>
      {superAdmin ? (
        <>
          <SectionSupervision />
          {lecteurComptes ? <SectionComptes posture={false} /> : null}
        </>
      ) : null}
      {rssi ? (
        <>
          <SectionComptes posture />
          {aPermission(permissions, 'IAM_JOURNAL_READ') ? <SectionActes /> : null}
          {aPermission(permissions, 'PLATFORM_SECURITY_POLICY_READ') ? (
            <Card title={t('console.nav.politique')}>
              <div className="gm-col">
                <span>{t('console.tableau.politique.texte')}</span>
                <Link href="/console/politique-securite">{t('console.tableau.politique.lien')}</Link>
              </div>
            </Card>
          ) : null}
          {supervision ? <SectionSupervision /> : null}
        </>
      ) : null}
      {!superAdmin && !rssi ? <EmptyState icon="gauge" title={t('console.tableau.vide.titre')} text={t('console.tableau.vide.texte')} /> : null}
    </div>
  )
}
