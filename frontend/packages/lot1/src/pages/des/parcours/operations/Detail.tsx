import { useTranslation } from 'react-i18next'
import { Button, DataTable, Hash, InlineAlert, KeyValueList, Link, StatusBadge, type Column, Note } from '@gimac/design-system'
import type { DossierLiaison, OperationParcours, Personne, ServicePerimetre } from '../../../../modele/parcours'
import { tonService, tonSla, useFormatDate } from '../communs'
import { externe, lienDuGeste, ongletDuGeste } from './regles'

// Lecture d'une opération dans le tiroir : ce qu'elle attend, où elle s'exécute, son verrou, sa RACI+E,
// la preuve déposée (empreinte, valeur de référence relisible, code de contrôle, cérémonie), et pour les
// rangs 16 et 17 l'état des essais et la couverture calculés par le service.

const qui = (p: Personne | null, t: (k: string) => string) => (p ? `${p.nom} · ${t(`parcours.fonction.${p.fonction}`)}` : t('parcoursOps.nonDesigne'))

/** Alertes permanentes attachées à l'opération (oubli sans symptôme, clé, secret, pièce pivot). */
export function Alertes({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  return (
    <>
      {op.oubliSansSymptome && op.etat !== 'FAITE' ? <InlineAlert level="crit" title={t('parcoursOps.alerte28.titre')}>{t('parcours.alerte.oubliSansSymptome')}</InlineAlert> : null}
      {op.oubliSansSymptome && op.etat === 'FAITE' ? <InlineAlert level="info" title={t('parcoursOps.alerte28.faite')}>{t('parcours.alerte.oubliSansSymptome')}</InlineAlert> : null}
      {op.pivot ? <InlineAlert level="info">{t('parcours.alerte.pivot')}</InlineAlert> : null}
    </>
  )
}

/** L'essentiel en tête de l'exécution : qui, pour quand (et le délai), où. */
export function Essentiel({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  return (
    <KeyValueList
      items={[
        { key: t('parcours.qualite.R'), value: qui(op.raci.R, t) },
        ...(op.etat !== 'FAITE'
          ? [{ key: t('parcoursOps.echeance'), value: <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}><span>{op.echeance ? date(op.echeance) : t('parcoursOps.sansEcheance')}</span>{op.sla ? <StatusBadge status={tonSla[op.sla]} label={t(`parcours.sla.${op.sla}`)} /> : null}</span> }]
          : []),
        { key: t('parcoursOps.plateformeExecution'), value: op.plateforme ? t(`parcours.plateforme.${op.plateforme}`) : t('parcoursOps.plateforme.aucune') },
      ]}
    />
  )
}

export function InfosOperation({ op, onOuvrir }: { op: OperationParcours; onOuvrir: (rang: string) => void }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  return (
    <div className="gm-col">
      <Alertes op={op} />
      <KeyValueList
        items={[
          { key: t('parcoursOps.col.etape'), value: `${t(`parcours.etape.${op.etape}`)} · ${t(`parcours.segment.${op.segment}`)}` },
          { key: t('parcoursOps.preuveAttendue'), value: t(`parcours.preuveAttendue.${op.rang}`) },
          { key: t('parcoursOps.plateformeExecution'), value: op.plateforme ? t(`parcours.plateforme.${op.plateforme}`) : t('parcoursOps.plateforme.aucune') },
          { key: t('parcoursOps.grain'), value: t(`parcours.grain.${op.grain}`) },
          { key: t('parcours.qualite.R'), value: qui(op.raci.R, t) },
          { key: t('parcoursOps.echeance'), value: op.echeance ? date(op.echeance) : t('parcoursOps.sansEcheance') },
          ...(op.sla ? [{ key: t('parcoursOps.sla'), value: <StatusBadge status={tonSla[op.sla]} label={t(`parcours.sla.${op.sla}`)} /> }] : []),
          ...(op.faiteLe ? [{ key: t('parcoursOps.faiteLe'), value: date(op.faiteLe) }] : []),
        ]}
      />
      {op.predecesseursNonFaits.length ? (
        <InlineAlert level="info" title={t('parcoursOps.verrou.titre')}>
          <span className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <span>{t('parcoursOps.verrou.texte')}</span>
            <span className="gm-row">
              {op.predecesseursNonFaits.map((r) => (
                <Button key={r} size="sm" variant="secondary" onClick={() => onOuvrir(r)}>{`${r} · ${t(`parcours.op.${r}`)}`}</Button>
              ))}
            </span>
          </span>
        </InlineAlert>
      ) : null}
    </div>
  )
}

export function Raci({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const liste = (l: Personne[]) => (l.length ? l.map((p) => qui(p, t)).join(' ; ') : t('parcoursOps.aucun'))
  return (
    <div className="gm-col">
      <KeyValueList
        items={[
          { key: t('parcours.qualite.R'), value: qui(op.raci.R, t) },
          { key: t('parcours.qualite.V'), value: qui(op.raci.V, t) },
          { key: t('parcours.qualite.C'), value: liste(op.raci.C) },
          { key: t('parcours.qualite.I'), value: liste(op.raci.I) },
          { key: t('parcours.qualite.E'), value: liste(op.raci.E) },
        ]}
      />
      <Note>{t('parcoursOps.raci.regle')}</Note>
    </div>
  )
}

export function PreuveDeposee({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const p = op.preuve
  if (!p) return <InlineAlert level="info">{t('parcoursOps.preuve.aucune')}</InlineAlert>
  const c = p.ceremonie
  return (
    <div className="gm-col">
      <KeyValueList
        items={[
          { key: t('parcoursOps.preuve.fichier'), value: p.nom },
          { key: t('parcoursOps.preuve.empreinte'), value: <Hash value={p.empreinte} /> },
          { key: t('parcoursOps.preuve.deposeeLe'), value: date(p.deposeLe) },
          { key: t('parcoursOps.preuve.deposeePar'), value: p.deposePar },
          { key: t('parcoursOps.preuve.reference'), value: p.documentRef, mono: true },
          ...(p.valeurReference ? [{ key: t(p.valeurReference.libelle), value: p.valeurReference.valeur, mono: true }] : []),
          ...(p.codeControle ? [{ key: t('parcoursOps.preuve.codeControle'), value: p.codeControle, mono: true }] : []),
        ]}
      />
      {p.valeurReference ? <Note>{t('parcoursOps.valeur.reference')}</Note> : null}
      {c ? (
        <>
          <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('parcoursOps.ceremonie.titre')}</h3>
          <KeyValueList
            items={[
              { key: t('parcoursOps.ceremonie.tenueLe'), value: date(c.tenueLe) },
              ...c.porteurs.map((nom, i) => ({ key: t('parcoursOps.ceremonie.composante', { n: i + 1 }), value: <span>{nom} · <span className="gm-mono">{c.codesControle[i] ?? ''}</span></span> })),
            ]}
          />
          <Note>{t('parcoursOps.ceremonie.aucuneComposante')}</Note>
        </>
      ) : null}
    </div>
  )
}

/** Rang 16 : essais service par service ; sur le simulateur, le taux est repris tel quel, jamais recalculé. */
export function Essais({ op, dossier }: { op: OperationParcours; dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const simulateur = op.plateforme === 'SIMULATEUR'
  const lignes = dossier.perimetre.filter((s) => s.inclus)
  const colonnes: Column<ServicePerimetre>[] = [
    { key: 'libelle', header: t('parcoursOps.essais.service') },
    { key: 'etat', header: t('parcoursOps.col.etat'), render: (s) => <StatusBadge status={tonService[s.etat]} label={t(`parcours.servicePerimetre.${s.etat}`)} /> },
    { key: 'entreeEssai', header: t('parcoursOps.essais.entree'), render: (s) => (s.entreeEssai ? date(s.entreeEssai) : '—') },
    { key: 'sortieEssai', header: t('parcoursOps.essais.sortie'), render: (s) => (s.sortieEssai ? date(s.sortieEssai) : '—') },
    ...(simulateur
      ? [{ key: 'avancementSimulateur', header: t('parcoursOps.essais.avancement'), type: 'number' as const, render: (s: ServicePerimetre) => (s.avancementSimulateur === null ? '—' : `${s.avancementSimulateur} %`) }]
      : []),
  ]
  return (
    <div className="gm-col">
      {simulateur ? <InlineAlert level="info" title={t('parcours.plateforme.SIMULATEUR')}>{t('parcours.alerte.simulateur')}</InlineAlert> : null}
      <DataTable columns={colonnes} rows={lignes} rowKey={(s) => s.serviceCode} density="compact" state={lignes.length ? 'ok' : 'empty'} emptyText={t('parcoursOps.essais.vide')} />
    </div>
  )
}

/** Rang 17 : couverture calculée par le service ; non calculable, son motif est affiché (jamais 0 %). */
export function Couverture({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const c = dossier.couvertureCertification
  return (
    <div className="gm-col">
      {c.calculable && c.taux !== null ? (
        <KeyValueList
          items={[
            { key: t('parcoursOps.couverture.taux'), value: <span className="gm-type-stat">{`${c.taux} %`}</span> },
            { key: t('parcoursOps.couverture.services'), value: t('parcoursOps.couverture.ratio', { n: c.numerateur, d: c.denominateur }) },
          ]}
        />
      ) : (
        <InlineAlert level="warn" title={t('parcoursOps.couverture.nonCalculable')}>{c.motifNonCalculable ?? ''}</InlineAlert>
      )}
      {c.taux !== null && c.taux < 100 ? <InlineAlert level="warn">{t('parcoursOps.couverture.incomplete')}</InlineAlert> : null}
      <Note>{t('parcoursOps.couverture.regle')}</Note>
    </div>
  )
}

/** Opération dont le geste appartient à un autre onglet du dossier, ou au participant (portail). */
export function GesteAilleurs({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const onglet = ongletDuGeste(op.rang)
  if (onglet)
    return (
      <InlineAlert level="info" title={t(`parcoursOps.ailleurs.${onglet}.titre`)}>
        <span className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
          <span>{t(`parcoursOps.ailleurs.${onglet}.texte`)}</span>
          <Link href={lienDuGeste(onglet, op)}>{t(`parcoursOps.ailleurs.${onglet}.lien`)}</Link>
        </span>
      </InlineAlert>
    )
  if (externe(op)) return <InlineAlert level="info" title={t('parcoursOps.externe.titre')}>{t(`parcoursOps.externe.${op.raci.R?.fonction}`)}</InlineAlert>
  return null
}
