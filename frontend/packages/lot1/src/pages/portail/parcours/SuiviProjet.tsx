import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CoverageBar, DataTable, InlineAlert, KeyValueList, Link, ProgressRing, Skeleton, StateMachineBar, StatusBadge, Tag, type Column, type CoverageBarProps, Note } from '@gimac/design-system'
import { EtatsRequete, usePermissions } from '@gimac/socle'
import type { DossierLiaison, EtapeParcours, ServicePerimetre, TacheActeur } from '../../../modele/parcours'
import { useApprobationPortail, useDossierPortail, useFicheSuiviPortail, useMesTachesPortail, useProcesVerbalPortail } from '../../../hooks/parcours'
import { EnTete, Echeance, PERM, Section, gesteDe, lienLivrable, tonEtape, tonStatut, useDate, useFonction, useLibelleTache, usePermissionPortail, useTypeProjet } from './commun'
import { TableTaches } from './listes'

// Suivi de mon projet (portail, par liaison) : ce qui est attendu du participant, les blocages qui le
// concernent, la progression des dix étapes lisible pour un externe (le verrou 2 n'est pas une étape,
// D-56), les deux couvertures, ses interlocuteurs désignés par leur seule fonction, et ses pièces.

const colonnes = { display: 'flex', flexWrap: 'wrap' as const, gap: 'var(--gm-space-6)', alignItems: 'flex-start' }

const niveau = (s: ServicePerimetre): CoverageBarProps['level'] =>
  !s.inclus ? 0 : s.etat === 'OUVERT' ? 4 : s.etat === 'CERTIFIE' || s.etat === 'NON_OUVERT' ? 3 : s.etat === 'EN_TEST' ? 2 : 1

function Blocages({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const fonction = useFonction()
  const ouverts = d.blocages.filter((b) => !b.leveLe)
  if (ouverts.length === 0) return null
  return (
    <Section titre={t('pp.blocages.titre')}>
      {ouverts.map((b) => (
        <InlineAlert key={b.id} level="warn" title={t('pp.blocages.depuis', { date: ecrire(b.depuis), etape: t(`parcours.etape.${b.etape}`) })}>
          <KeyValueList
            items={[
              { key: t('pp.blocages.motif'), value: b.motif },
              { key: t('pp.blocages.prochaineAction'), value: b.prochaineAction },
              { key: t('pp.blocages.porteur'), value: fonction(b.responsable.fonction) },
              { key: t('pp.blocages.levee'), value: ecrire(b.echeanceLevee) },
            ]}
          />
        </InlineAlert>
      ))}
    </Section>
  )
}

/** Les dix étapes d'un coup d'œil (le verrou 2 n'est pas numéroté, D-56). */
function BarreEtapes({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  return (
    <StateMachineBar
      label={t('pp.progression.titre')}
      states={d.etapes.filter((e) => e.rang !== null).map((e) => ({ key: e.code, label: `${e.rang}. ${t(`parcours.etape.${e.code}`)}` }))}
      current={d.etapeCourante === 'APPROBATION_SECURITE' ? undefined : d.etapeCourante}
      traversed={d.etapes.filter((e) => e.etat === 'CLOSE').map((e) => e.code)}
    />
  )
}

function Progression({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const fonction = useFonction()
  const qui = (porteurs: EtapeParcours['porteurs']) => [...porteurs].sort((a, b) => Number(b === 'PARTICIPANT') - Number(a === 'PARTICIPANT')).map((f) => (f === 'PARTICIPANT' ? t('pp.vous') : fonction(f))).join(', ')
  const libelle = (e: EtapeParcours) => (e.rang === null ? t('pp.verrou2', { etape: t(`parcours.etape.${e.code}`) }) : `${e.rang}. ${t(`parcours.etape.${e.code}`)}`)
  const cols: Column<EtapeParcours>[] = [
    { key: 'code', header: t('pp.etape'), render: (e) => <span className={e.rang === null ? 'gm-type-small-strong' : 'gm-type-body-strong'}>{libelle(e)}</span> },
    {
      key: 'etat', header: t('pp.etat'),
      render: (e) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
          <StatusBadge status={tonEtape[e.etat]} label={t(`parcours.etatEtape.${e.etat}`)} />
        </span>
      ),
    },
    {
      key: 'porteurs', header: t('pp.porteurs'),
      render: (e) => (e.porteurs.length ? qui(e.porteurs) : '—'),
    },
    { key: 'actions', header: t('pp.avancement'), render: (e) => (e.actionsTotal ? <span className="gm-num">{t('pp.actionsFaites', { n: e.actionsFaites, total: e.actionsTotal })}</span> : '—') },
    { key: 'echeance', header: t('pp.echeanceOuCloture'), render: (e) => (e.closeLe ? <span className="gm-num">{t('pp.closeLe', { date: ecrire(e.closeLe) })}</span> : e.etat === 'SANS_OBJET' ? '—' : <Echeance date={e.echeance} sla={e.sla} />) },
  ]
  return (
    <Section titre={t('pp.progression.titre')}>
      {d.etapeCourante === 'APPROBATION_SECURITE' ? <InlineAlert level="info">{t('pp.progression.verrou2')}</InlineAlert> : null}
      <DataTable columns={cols} rows={d.etapes} rowKey={(e) => e.code} density="compact" />
    </Section>
  )
}

function Couvertures({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const ring = (libelle: string, v: DossierLiaison['couvertureCertification']) => (
    <ProgressRing label={libelle} value={v.calculable ? v.taux : null} status={v.taux === 100 ? 'ok' : undefined} statusLabel={t('pp.complete')} />
  )
  const simulateur = d.perimetre.filter((s) => s.avancementSimulateur !== null)
  return (
    <Card title={t('pp.couvertures.titre')}>
      <div className="gm-col">
        <div className="gm-row" style={{ gap: 'var(--gm-space-5)', alignItems: 'flex-start' }}>
          {ring(t('pp.couvertureCertification'), d.couvertureCertification)}
          {ring(t('pp.couvertureMiseEnService'), d.couvertureMiseEnService)}
        </div>
        <Note>{t('pp.couvertures.aide')}</Note>
        <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
          {d.perimetre.map((s) => <CoverageBar key={s.serviceCode} service={s.libelle} level={niveau(s)} />)}
        </div>
        {simulateur.length ? (
          <InlineAlert level="info" title={t('parcours.alerte.simulateur')}>
            {simulateur.map((s) => `${s.libelle} : ${s.avancementSimulateur} %`).join(' · ')}
          </InlineAlert>
        ) : null}
      </div>
    </Card>
  )
}

function Interlocuteurs({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const fonction = useFonction()
  const ecrire = useDate()
  const courante = d.etapes.find((e) => e.code === d.etapeCourante)
  return (
    <Card title={t('pp.interlocuteurs.titre')}>
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('pp.interlocuteurs.pilotage'), value: d.conducteur ? fonction('CONDUCTEUR') : t('pp.nonDesigne') },
            { key: t('pp.interlocuteurs.fonctionnel'), value: fonction(d.responsableFonctionnel?.fonction) },
            { key: t('pp.interlocuteurs.technique'), value: fonction(d.responsableTechnique?.fonction) },
            ...(courante && courante.porteurs.length ? [{ key: t('pp.interlocuteurs.etapeCourante'), value: courante.porteurs.map((f) => (f === 'PARTICIPANT' ? t('pp.vous') : fonction(f))).join(', ') }] : []),
            ...(d.banquePartenaireNom ? [{ key: t('parcours.fonction.BANQUE_PARTENAIRE'), value: d.banquePartenaireNom }] : []),
            ...(d.personnalisateurNom ? [{ key: t('parcours.fonction.PERSONNALISATEUR'), value: d.personnalisateurNom }] : []),
          ]}
        />
        {d.interlocuteursParticipant.length ? (
          <>
            <h3 className="gm-type-small-strong" style={{ margin: 0 }}>{t('pp.interlocuteurs.vosCorrespondants')}</h3>
            <KeyValueList items={d.interlocuteursParticipant.map((i) => ({ key: i.fonction, value: i.nom }))} />
          </>
        ) : null}
        <h3 className="gm-type-small-strong" style={{ margin: 0 }}>{t('pp.calendrier')}</h3>
        <KeyValueList
          items={[
            { key: t('pp.demarrage'), value: ecrire(d.dateDemarrage) },
            { key: t('pp.cibleProduction'), value: d.dateCibleProduction ? ecrire(d.dateCibleProduction) : t('pp.nonFixee') },
            ...(d.versionReferentiel !== null ? [{ key: t('pp.referentiel'), value: t('pp.version', { n: d.versionReferentiel }) }] : []),
          ]}
        />
      </div>
    </Card>
  )
}

/** Pièces du participant sur la liaison : chacune avec son état et l'écran où la consulter ou l'établir ; livrables attendus menant à leur écran de dépôt. */
/** Étapes postérieures à l'intégration, où les coordonnées techniques d'essai ont été communiquées si la liaison en attendait. */
const APRES_INTEGRATION: string[] = ['TESTS', 'CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION', 'SUIVI']

function Pieces({ d, livrables }: { d: DossierLiaison; livrables: TacheActeur[] }) {
  const { t } = useTranslation()
  const certification = usePermissionPortail(PERM.lireCertification)
  const libelle = useLibelleTache()
  const base = `/portail/liaisons/${d.id}`
  const c = d.coordonneesTechniques
  const piece = (titre: string, etat: ReactNode, chemin: string) => (
    <li className="gm-row" style={{ justifyContent: 'space-between', gap: 'var(--gm-space-3)', padding: 'var(--gm-space-2) 0', borderBottom: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
      <span className="gm-col" style={{ gap: 'var(--gm-space-1)', minWidth: 0 }}>
        <span className="gm-type-body-strong">{titre}</span>
        <span>{etat}</span>
      </span>
      <Link href={`${base}/${chemin}`}>{t('pp.ouvrir')}</Link>
    </li>
  )
  return (
    <Card title={t('pp.pieces.titre')}>
      <ul className="gm-col" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 0 }}>
        {/* CER-09 : passé l'intégration sans coordonnées, la liaison n'en attendait pas (modèle sans essai de connexion). */}
        {piece(t('pp.coordonnees.titre'), c ? <span className="gm-mono" style={{ overflowWrap: 'anywhere' }}>{`${c.adresse}:${c.port}`}</span> : <StatusBadge status="neutral" label={t(APRES_INTEGRATION.includes(d.etapeCourante) ? 'pp.sansObjet' : 'pp.nonCommuniquees')} />, 'coordonnees')}
        {certification ? piece(t('pp.pv.titre'), <EtatPv id={d.id} />, 'proces-verbal') : null}
        {certification ? piece(t('pp.attestations.titre'), <EtatApprobation id={d.id} />, 'attestations') : null}
        {piece(t('pp.fiche.titre'), <EtatFiche id={d.id} />, 'fiche-suivi')}
        {livrables.map((x) => (
          <li key={x.operationId} className="gm-row" style={{ justifyContent: 'space-between', gap: 'var(--gm-space-3)', padding: 'var(--gm-space-2) 0', borderBottom: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
            <span className="gm-col" style={{ gap: 'var(--gm-space-1)', minWidth: 0 }}>
              <span className="gm-type-body-strong">{libelle(x)}</span>
              <span><StatusBadge status="warn" label={t('pp.livrable.aDeposer')} /></span>
            </span>
            <Link href={lienLivrable(x)}>{t('pp.livrable.deposer')}</Link>
          </li>
        ))}
      </ul>
    </Card>
  )
}

function EtatPv({ id }: { id: string }) {
  const { t } = useTranslation()
  const q = useProcesVerbalPortail(id)
  if (q.isPending) return <Skeleton width="60%" />
  if (!q.data) return <StatusBadge status="neutral" label="—" />
  const e = q.data.etat
  return <StatusBadge status={e === 'SIGNE' ? 'ok' : e === 'NON_ETABLI' ? 'neutral' : 'warn'} label={t(`parcours.pv.etat.${e}`)} />
}
function EtatApprobation({ id }: { id: string }) {
  const { t } = useTranslation()
  const q = useApprobationPortail(id)
  if (q.isPending) return <Skeleton width="60%" />
  if (!q.data) return <StatusBadge status="neutral" label="—" />
  const fournies = q.data.attestations.filter((a) => a.preuve).length
  // CER-09 : aucune attestation exigée (modèle générique) : rien n'est attendu, ni maintenant ni plus tard.
  if (q.data.attestations.length === 0) return <StatusBadge status="neutral" label={t('pp.attestations.aucuneExigee')} />
  if (fournies === 0 && !q.data.actionsPermises.includes('DEPOSER_ATTESTATION')) return <StatusBadge status="neutral" label={t('pp.attestations.pasAttendues')} />
  return <StatusBadge status={fournies === q.data.attestations.length ? 'ok' : 'neutral'} label={t('pp.attestations.fournies', { n: fournies, total: q.data.attestations.length })} />
}
function EtatFiche({ id }: { id: string }) {
  const { t } = useTranslation()
  const q = useFicheSuiviPortail(id)
  if (q.isPending) return <Skeleton width="60%" />
  if (!q.data) return <StatusBadge status="neutral" label="—" />
  const e = q.data.etat
  return <StatusBadge status={e === 'REMISE' ? 'warn' : e === 'A_REMETTRE' ? 'neutral' : 'ok'} label={t(`pp.fiche.etat.${e}`)} />
}

function Suivi({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const typeProjet = useTypeProjet()
  const permissions = usePermissions()
  const taches = useMesTachesPortail()
  const miennes = (taches.data?.taches ?? []).filter((x) => x.liaisonId === d.id)
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <EnTete
        retour={{ href: '/portail/tableau-de-bord', libelle: t('pp.retourTableau') }}
        titre={t('pp.suivi.titre', { reference: d.reference })}
        sousTitre={
          <span className="gm-row">
            <span>{typeProjet(d)}</span>
            <span aria-hidden="true">·</span>
            <span>{d.institutionNom}</span>
            {d.liaisonOrigineId ? <Tag>{t('pp.serviceSupplementaire', { origine: d.liaisonOrigineId })}</Tag> : null}
          </span>
        }
        etat={
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
            <StatusBadge status={tonStatut[d.statut]} label={t(`parcours.statut.${d.statut}`)} />
            {d.blocages.some((b) => !b.leveLe) ? <StatusBadge status="crit" label={t('pp.bloquee')} /> : null}
          </span>
        }
      />
      <BarreEtapes d={d} />
      <div style={colonnes}>
        <div className="gm-col" style={{ flex: '2 1 560px', minWidth: 0, gap: 'var(--gm-space-6)' }}>
          <Section titre={t('pp.suivi.attendu')}>
            <TableTaches taches={miennes} requete={taches} liaison={false} />
          </Section>
          <Blocages d={d} />
        </div>
        <div className="gm-col" style={{ flex: '1 1 320px', minWidth: 0 }}>
          <Couvertures d={d} />
          <Pieces d={d} livrables={miennes.filter((x) => gesteDe(x, permissions) === 'DEPOSER_LIVRABLE')} />
        </div>
      </div>
      <div style={colonnes}>
        <div className="gm-col" style={{ flex: '2 1 560px', minWidth: 0 }}>
          <Progression d={d} />
        </div>
        <div className="gm-col" style={{ flex: '1 1 320px', minWidth: 0 }}>
          <Interlocuteurs d={d} />
        </div>
      </div>
    </div>
  )
}

export function SuiviProjet({ id }: { id: string }) {
  const { t } = useTranslation()
  const q = useDossierPortail(id)
  return (
    <EtatsRequete etat={q} emptyTitle={t('pp.introuvable')} errorTitle={t('pp.introuvable')} retryLabel={t('commun.reessayer')}>
      {(d) => <Suivi d={d} />}
    </EtatsRequete>
  )
}
