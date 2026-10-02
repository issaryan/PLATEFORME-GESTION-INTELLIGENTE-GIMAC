import { useTranslation } from 'react-i18next'
import { Card, DataTable, InlineAlert, KeyValueList, Link, PageHeader, StatusBadge, Tag, type Column } from '@gimac/design-system'
import { dateHeure } from '@gimac/compte'
import { useConfigurationPlateforme } from '../hooks'
import type { RouteVue } from '../modele/plateforme'
import { Donnees, Section } from '../composants/communs'

// Configuration effective de la plateforme (D-61, PLATFORM_CONFIG_READ) : routes de la passerelle et,
// pour chaque service, profils actifs et propriétés d'exploitation. Les secrets sont masqués par la
// passerelle (******) : l'interface ne reçoit jamais de valeur sensible.
export default function ConfigurationPlateformePage() {
  const { t, i18n } = useTranslation()
  const configuration = useConfigurationPlateforme()
  const colonnes: Column<RouteVue>[] = [
    { key: 'id', header: t('console.config.route'), render: (r) => <span className="gm-mono">{r.id}</span> },
    { key: 'uri', header: t('console.config.destination'), render: (r) => <span className="gm-mono">{r.uri}</span> },
    { key: 'predicats', header: t('console.config.predicats'), render: (r) => <span className="gm-mono">{r.predicats}</span> },
    { key: 'filtres', header: t('console.config.filtres'), render: (r) => <span className="gm-row">{r.filtres.map((f) => <Tag key={f}>{f}</Tag>)}</span> },
    { key: 'ordre', header: t('console.config.ordre'), type: 'number' },
  ]
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <PageHeader title={t('console.config.titre')} subtitle={t('console.config.sousTitre')} />
      <Donnees requete={configuration}>
        {(c) => (
          <>
            <span className="gm-type-small">{t('console.config.lueLe', { heure: dateHeure(c.horodatage, i18n.language) })}</span>
            <Section titre={t('console.config.routes')} id="gm-routes">
              <DataTable columns={colonnes} rows={c.routes} rowKey={(r) => r.id} density="compact" />
            </Section>
            <Section titre={t('console.config.services')} id="gm-services">
              {c.services.map((s) => (
                <Card key={s.nom} title={<span className="gm-mono">{s.nom}</span>}
                  actions={<StatusBadge status={s.lue ? 'ok' : 'warn'} label={t(s.lue ? 'console.config.lue' : 'console.config.nonLue')} />}
                  footer={<Link href={`/console/plateforme/journalisation?service=${encodeURIComponent(s.nom)}`}>{t('console.config.versJournalisation')}</Link>}>
                  <div className="gm-col">
                    {!s.lue ? <InlineAlert level="warn" title={t('console.config.injoignable')}>{s.erreur}</InlineAlert> : null}
                    <KeyValueList
                      items={[
                        { key: t('console.config.adresse'), value: s.url, mono: true },
                        { key: t('console.config.profilsActifs'), value: s.profilsActifs.length ? <span className="gm-row">{s.profilsActifs.map((p) => <Tag key={p}>{p}</Tag>)}</span> : '—' },
                        ...Object.entries(s.proprietes).map(([k, v]) => ({ key: k, value: v, mono: true })),
                      ]}
                    />
                  </div>
                </Card>
              ))}
            </Section>
          </>
        )}
      </Donnees>
    </div>
  )
}
