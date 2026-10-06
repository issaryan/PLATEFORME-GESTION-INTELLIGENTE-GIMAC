import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, ConfirmDialog, DataTable, DiffView, EmptyState, InlineAlert, KeyValueList, Tag, type Column } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import {
  useCatalogueReferentiels, useControleReferentiel, useExporterReferentiel, useLiaisonsImpactees, useMigrerLiaisons, useOperationsReferentiel, useSimulationMigration,
} from '../../../hooks/referentiels'
import { parSegment, suiteCycle, versLignesDiff } from '../../../adaptateurs/referentiel'
import { enregistrerFichier } from '../../../ressources/fichier'
import type { ImpactMigration, ReferenceOperation, ReferentielVersion } from '../../../modele/homologation'

// Compositions de DES-12 (onglets de la version), faites uniquement de composants du catalogue.
// Aucune ne calcule de règle : contrôle d'acyclicité et impact de migration viennent du service.

function useEtats() {
  const { t } = useTranslation()
  return { emptyTitle: t('commun.vide'), errorTitle: t('commun.erreur.titre'), retryLabel: t('commun.reessayer') }
}

type LigneOp = ReferenceOperation & Record<string, unknown>

/** Bandeau de la lecture filtrée (D-89 §3) : le lecteur ne voit que les opérations qui le concernent. */
export function BandeauConcerne() {
  const { t } = useTranslation()
  return <InlineAlert level="info" title={t('referentiel.concerne.titre')}>{t('referentiel.concerne.texte')}</InlineAlert>
}

/** Export CSV des opérations de la version (GET …/export, D-89), filtré comme la liste. */
export function BoutonExport({ versionId }: { versionId: string }) {
  const { t } = useTranslation()
  const exporter = useExporterReferentiel()
  return (
    <Button size="sm" icon="download" loading={exporter.isPending}
      onClick={() => exporter.mutate(versionId, { onSuccess: (f) => enregistrerFichier(f.blob, f.nom) })}>{t('referentiel.action.exporter')}</Button>
  )
}

/** Onglet « Opérations » : tableau par segment (tronc commun amont, branche, tronc commun aval). */
export function OngletOperations({ versionId }: { versionId: string }) {
  const { t } = useTranslation()
  const etats = useEtats()
  const q = useOperationsReferentiel(versionId)
  // Libellés des étapes de la version (D-89) : celles du catalogue ?version=, la clé i18n à défaut.
  const catalogue = useCatalogueReferentiels(versionId)
  const libelleEtape = (e: string) => catalogue.data?.etapesVersion?.find((x) => x.code === e)?.libelle ?? t(`parcours.etape.${e}`, { defaultValue: e })
  const colonnes: Column<LigneOp>[] = [
    { key: 'code', header: t('operation.code'), type: 'mono', width: 110 },
    { key: 'libelle', header: t('operation.libelle'), render: (r) => (r.fantome ? <span className="gm-row"><span className="gm-field-help">{r.libelle}</span><Tag>{t('referentiel.fantome')}</Tag></span> : r.libelle) },
    { key: 'etape', header: t('modelisateur.champ.etape'), render: (r) => (r.etape ? libelleEtape(r.etape) : r.phase ?? '') },
    { key: 'responsable', header: 'R', render: (r) => (r.responsable ? t(`parcours.fonction.${r.responsable}`) : r.fonctionResponsable ?? '') },
    { key: 'validateur', header: 'V', render: (r) => (r.validateur ? t(`parcours.fonction.${r.validateur}`) : '') },
    { key: 'predecesseurs', header: t('referentiel.op.predecesseurs'), render: (r) => <span className="gm-row">{r.predecesseurs.map((p) => <Tag key={p}>{p}</Tag>)}</span> },
    { key: 'delaiJours', header: t('modelisateur.champ.delai'), render: (r) => t('modelisateur.carte.delai', { n: r.delaiJours ?? 0 }) },
    { key: 'geste', header: t('modelisateur.champ.geste'), render: (r) => (r.geste ? t(`modelisateur.geste.${r.geste}`) : '') },
    { key: 'idScenario', header: t('referentiel.op.scenario'), render: (r) => (r.idScenario ? <span className="gm-mono">{r.idScenario}</span> : <span className="gm-field-help">{t('referentiel.op.sansScenario')}</span>) },
  ]
  return (
    <EtatsRequete etat={q} {...etats} emptyTitle={t('referentiel.operations.aucune')} isEmpty={(o) => o.operations.length === 0}>
      {({ operations: ops, filtre }) => (
        <div className="gm-col">
          {filtre === 'CONCERNE' ? <BandeauConcerne /> : null}
          <div className="gm-row"><BoutonExport versionId={versionId} /></div>
          {parSegment(ops.filter((o) => !o.fantome)).map((g) => (
            <section key={g.segment} className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
              <h2 className="gm-type-h3" style={{ margin: 0 }}>{t(`parcours.segment.${g.segment}`)}</h2>
              <DataTable<LigneOp> columns={colonnes} rows={g.operations as LigneOp[]} rowKey={(r) => r.code} state="ok" density="compact" />
            </section>
          ))}
          {ops.some((o) => o.fantome) ? (
            <section className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
              <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('referentiel.fantomes.titre')}</h2>
              <p className="gm-field-help" style={{ margin: 0 }}>{t('referentiel.fantomes.aide')}</p>
              <DataTable<LigneOp> columns={colonnes.slice(0, 2)} rows={ops.filter((o) => o.fantome) as LigneOp[]} rowKey={(r) => r.code} state="ok" density="compact" />
            </section>
          ) : null}
        </div>
      )}
    </EtatsRequete>
  )
}

/** Onglet « Comparaison » : version active (gauche) contre version courante (droite). */
export function OngletComparaison({ version }: { version: ReferentielVersion }) {
  const { t } = useTranslation()
  const etats = useEtats()
  const actif = version.versionActiveId && version.versionActiveId !== version.id ? version.versionActiveId : null
  const avant = useOperationsReferentiel(actif)
  const apres = useOperationsReferentiel(actif ? version.id : null)
  if (!actif) return <EmptyState icon="info" title={t('referentiel.comparaison.sans.titre')} text={t('referentiel.comparaison.sans.texte')} />
  return (
    <EtatsRequete etat={avant} {...etats}>
      {(a) => (
        <EtatsRequete etat={apres} {...etats}>
          {(b) => <DiffView leftTitle={t('referentiel.active', { id: actif })} rightTitle={t('referentiel.courante', { id: version.id })} lines={versLignesDiff(a.operations.filter((o) => !o.fantome), b.operations.filter((o) => !o.fantome))} />}
        </EtatsRequete>
      )}
    </EtatsRequete>
  )
}

type LigneIncoherence = { code: string; predecesseur: string; motif: string } & Record<string, unknown>

/** Onglet « Contrôle » : restitution telle que calculée par le service (HOM-02). */
export function OngletControle({ versionId }: { versionId: string }) {
  const { t } = useTranslation()
  const etats = useEtats()
  const q = useControleReferentiel(versionId)
  return (
    <EtatsRequete etat={q} {...etats}>
      {(r) => (
        <div className="gm-col" data-testid="controle-restitution">
          {r.acyclique ? (
            <>
              <InlineAlert level="info" title={t('referentiel.controle.acyclique')}>{t('referentiel.controle.ordre')}</InlineAlert>
              <ol className="gm-row" aria-label={t('referentiel.controle.ordre')} style={{ listStyle: 'none', margin: 0, padding: 0 }}>
                {r.ordreTopologique.map((c, i) => (
                  <li key={c}><Tag>{`${i + 1}. ${c}`}</Tag></li>
                ))}
              </ol>
            </>
          ) : (
            <InlineAlert level="crit" title={t('referentiel.controle.cycleTitre')}>
              <span data-testid="cycle">{t('referentiel.controle.cycle', { suite: suiteCycle(r.cycle) })}</span>
            </InlineAlert>
          )}
          {r.incoherences.length ? (
            <section className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
              <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('referentiel.controle.incoherence')}</h2>
              <DataTable<LigneIncoherence>
                columns={[
                  { key: 'code', header: t('operation.code'), type: 'mono' },
                  { key: 'predecesseur', header: t('referentiel.controle.predecesseur'), type: 'mono' },
                  { key: 'motif', header: t('referentiel.controle.motif') },
                ]}
                rows={r.incoherences as LigneIncoherence[]}
                rowKey={(x) => `${x.code}-${x.predecesseur}`}
                state="ok"
                density="compact"
              />
            </section>
          ) : null}
          {r.manquants.length ? (
            <section className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
              <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('referentiel.controle.manquants')}</h2>
              <KeyValueList items={r.manquants.map((m) => ({ key: m.code, value: m.attributs.join(', '), mono: true }))} />
            </section>
          ) : null}
          {r.acyclique && !r.incoherences.length && !r.manquants.length ? <InlineAlert level="info">{t('referentiel.controle.conforme')}</InlineAlert> : null}
        </div>
      )}
    </EtatsRequete>
  )
}

type LigneImpact = ImpactMigration & Record<string, unknown>

/** Onglet « Migration » : simulation par liaison, sélection des liaisons retenues, confirmation (HOM-03). */
export function OngletMigration({ version }: { version: ReferentielVersion }) {
  const { t } = useTranslation()
  const etats = useEtats()
  const q = useSimulationMigration(version.id)
  const migrer = useMigrerLiaisons(version.id)
  const [retenues, setRetenues] = useState<string[]>([])
  const [confirmer, setConfirmer] = useState(false)
  const peutMigrer = version.actionsPermises.includes('MIGRER')
  const basculer = (id: string, v: boolean) => setRetenues((r) => (v ? [...r, id] : r.filter((x) => x !== id)))
  return (
    <EtatsRequete etat={q} {...etats} emptyTitle={t('referentiel.migration.aucune')} isEmpty={(l) => l.length === 0}>
      {(impacts) => (
        <div className="gm-col">
          <DataTable<LigneImpact>
            columns={[
              {
                key: 'liaisonId', header: t('referentiel.migration.liaison'), width: 180,
                render: (r) =>
                  r.exclue || !peutMigrer ? (
                    <span className="gm-mono">{r.liaisonId}</span>
                  ) : (
                    <Checkbox label={r.liaisonId} checked={retenues.includes(r.liaisonId)} onChange={(v: boolean) => basculer(r.liaisonId, v)} />
                  ),
              },
              { key: 'institutionNom', header: t('referentiel.migration.institution') },
              { key: 'conservees', header: t('referentiel.migration.conservees'), type: 'number' },
              { key: 'ajoutees', header: t('referentiel.migration.ajoutees'), type: 'number' },
              { key: 'retirees', header: t('referentiel.migration.retirees'), type: 'number' },
              { key: 'modifiees', header: t('referentiel.migration.modifiees'), type: 'number' },
              { key: 'motifExclusion', header: t('referentiel.migration.exclusion'), render: (r) => (r.exclue ? <span className="gm-field-help">{t('referentiel.migration.exclue', { motif: r.motifExclusion ?? '' })}</span> : null) },
            ]}
            rows={impacts as LigneImpact[]}
            rowKey={(r) => r.liaisonId}
            state="ok"
          />
          {peutMigrer ? (
            <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
              <Button variant="primary" icon="route" disabled={!retenues.length} disabledReason={t('referentiel.migration.aucuneRetenue')} onClick={() => setConfirmer(true)}>
                {t('referentiel.action.migrer', { n: retenues.length })}
              </Button>
            </div>
          ) : null}
          {migrer.isSuccess ? <InlineAlert level="info">{t('referentiel.migration.effectuee')}</InlineAlert> : null}
          {confirmer ? (
            <ConfirmDialog
              open
              title={t('referentiel.action.migrer', { n: retenues.length })}
              confirmLabel={t('referentiel.action.migrer', { n: retenues.length })}
              object={retenues.map((id) => ({ key: t('referentiel.migration.liaison'), value: id, mono: true }))}
              consequences={[t('referentiel.migration.consequence')]}
              description={migrer.error ? `${migrer.error.message} ${migrer.error.correlationId}` : undefined}
              onConfirm={() => migrer.mutate(retenues, { onSuccess: () => { setConfirmer(false); setRetenues([]) } })}
              onClose={() => setConfirmer(false)}
            />
          ) : null}
        </div>
      )}
    </EtatsRequete>
  )
}

/** Corps du ConfirmDialog de publication : liaisons qui restent sur la version active. */
export function ImpactPublication({ versionId }: { versionId: string }) {
  const { t } = useTranslation()
  const q = useLiaisonsImpactees(versionId)
  if (!q.data) return null
  return <InlineAlert level="info">{t('referentiel.publier.impact', { n: q.data.total })}</InlineAlert>
}
