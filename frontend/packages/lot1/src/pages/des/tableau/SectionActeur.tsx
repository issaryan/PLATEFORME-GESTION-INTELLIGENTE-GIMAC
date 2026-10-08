import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Card, DataTable, Link, StatCard, StatusBadge, Tag, type Column, type Status, Note } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { Blocage, Fonction, LiaisonResume, TableauActeur, TacheActeur } from '../../../modele/parcours'
import { useTableauActeur } from '../../../hooks/parcours'
import { PERM } from '../../../permissions'
import { Donnees } from '../liaisons/communs'
import { colonnesLiaisons, Echeance, Jour, useFormatDate } from '../parcours/communs'

// Section « Mon travail » du tableau de bord (D-58, CR §3.6) : pour toute fonction du parcours, les
// compteurs que le service calcule pour elle, ses tâches (qualité, étape, opération, échéance, SLA), ses
// liaisons et les blocages en cours. Le conducteur retrouve ses projets et les procès-verbaux à établir ;
// les chefs de département, les liaisons sans conducteur et les liaisons en amont, avec leurs gestes.

const AMONT = ['CADRAGE', 'LANCEMENT', 'ATELIERS']
const LIBELLE_TON: Record<Status, string> = { ok: 'parcoursActeur.ton.ok', warn: 'parcoursActeur.ton.warn', crit: 'parcoursActeur.ton.crit', info: 'parcoursActeur.ton.info', neutral: 'parcoursActeur.ton.info' }

function Titre({ children, actions }: { children: ReactNode; actions?: ReactNode }) {
  return (
    <div className="gm-resource-head">
      <h2 className="gm-type-h3" style={{ margin: 0 }}>{children}</h2>
      {actions ? <div className="gm-row">{actions}</div> : null}
    </div>
  )
}

/** Liste servie triée par le service ; la table la pagine à l'affichage (pagination toujours visible). */
function Paginee<T extends object>({ lignes, colonnes, cle, vide, aide }: { lignes: T[]; colonnes: Column<T>[]; cle: (r: T) => string; vide: string; aide?: string }) {
  return (
    <DataTable columns={colonnes} rows={lignes} rowKey={cle} pageSize={10} density="compact" state={lignes.length ? 'ok' : 'empty'} emptyText={vide} emptyHelp={aide} />
  )
}

/** Libellé d'un compteur : servi par MS-HOMOLOGATION (compteurs décrits en données, D-94 §6.5), sinon traduit par sa clé. */
const libelleCompteur = (t: (k: string, o?: Record<string, unknown>) => string, c: TableauActeur['compteurs'][number]) => c.libelle || t(`parcours.compteur.${c.cle}`, { defaultValue: c.cle })

/** Pictogramme de chaque compteur de « Mon travail » (le compteur sans pictogramme connu n'en porte pas). */
function Compteurs({ tableau }: { tableau: TableauActeur }) {
  const { t } = useTranslation()
  return (
    <div className="gm-stat-grid" role="group" aria-label={t('parcoursActeur.compteurs')}>
      {tableau.compteurs.map((c) => (
        <StatCard
          key={c.cle}
          label={c.lien ? <Link href={c.lien}>{libelleCompteur(t, c)}</Link> : libelleCompteur(t, c)}
          value={c.valeur}
          status={c.ton}
          statusLabel={t(LIBELLE_TON[c.ton])}
        />
      ))}
    </div>
  )
}

function Taches({ taches }: { taches: TacheActeur[] }) {
  const { t } = useTranslation()
  const colonnes: Column<TacheActeur>[] = [
    {
      key: 'operation', header: t('parcoursActeur.operation'),
      render: (x) => (
        <span className="gm-col" style={{ gap: 0 }}>
          {/* Libellé servi (D-93), le code de l'opération à défaut ; badges « à prendre » et « signature ». */}
          <Link href={x.lien}>{x.libelle || x.rang}</Link>
          <span className="gm-field-help">
            <span className="gm-mono">{x.rang}</span> · {t(`parcours.etape.${x.etape}`, { defaultValue: x.etape })}
          </span>
          {x.aPrendre || x.signature ? (
            <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
              {x.aPrendre ? <StatusBadge status="warn" label={t('parcoursActeur.aPrendre')} /> : null}
              {x.signature ? <StatusBadge status="info" icon="pen-line" label={t('parcoursActeur.signature')} /> : null}
            </span>
          ) : null}
        </span>
      ),
    },
    {
      key: 'liaison', header: t('parcoursActeur.liaison'),
      render: (x) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <span className="gm-mono">{x.liaisonReference}</span>
          <span className="gm-field-help">{x.institutionNom}</span>
        </span>
      ),
    },
    { key: 'qualite', header: t('parcoursActeur.qualite'), render: (x) => <Tag>{`${x.qualite} · ${t(`parcours.qualite.${x.qualite}`)}`}</Tag> },
    { key: 'echeance', header: t('parcoursEtapes.echeance'), render: (x) => <Echeance iso={x.echeance} sla={x.sla} /> },
    {
      key: 'alerte', header: t('parcoursActeur.vigilance'),
      render: (x) => (x.oubliSansSymptome ? <span title={t('parcours.alerte.oubliSansSymptome')}><StatusBadge status="warn" label={t('parcoursActeur.oubliSansSymptome')} /></span> : null),
    },
  ]
  return (
    <Card title={t('parcoursActeur.taches', { count: taches.length })}>
      {taches.some((x) => x.oubliSansSymptome) ? (
        <Note>{t('parcours.alerte.oubliSansSymptome')}</Note>
      ) : null}
      <Paginee lignes={taches} colonnes={colonnes} cle={(x) => `${x.operationId}:${x.qualite}`} vide={t('parcoursActeur.aucuneTache')} aide={t('parcoursActeur.aucuneTache.aide')} />
    </Card>
  )
}

function Blocages({ blocages }: { blocages: Blocage[] }) {
  const { t } = useTranslation()
  const colonnes: Column<Blocage>[] = [
    { key: 'liaison', header: t('parcoursActeur.liaison'), render: (b) => <span style={{ whiteSpace: 'nowrap' }}><Link href={`/des/parcours/${b.liaisonId}?onglet=blocages`} mono>{b.liaisonId}</Link></span> },
    { key: 'motif', header: t('parcoursBlocages.motif'), render: (b) => b.motif },
    { key: 'depuis', header: t('parcoursBlocages.depuis'), render: (b) => <Jour iso={b.depuis} /> },
    { key: 'prochaineAction', header: t('parcoursBlocages.prochaineAction'), render: (b) => b.prochaineAction },
    { key: 'responsable', header: t('parcoursBlocages.responsable'), render: (b) => b.responsable.nom },
    { key: 'echeanceLevee', header: t('parcoursBlocages.echeanceLevee'), render: (b) => <Jour iso={b.echeanceLevee} /> },
  ]
  return (
    <Card title={t('parcoursActeur.blocages', { count: blocages.length })}>
      <Paginee lignes={blocages} colonnes={colonnes} cle={(b) => b.id} vide={t('parcoursBlocages.aucun')} />
    </Card>
  )
}

function MonTravail({ tableau }: { tableau: TableauActeur }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  // Qualités lues dans les permissions du jeton (D-94), jamais dans un nom de fonction ou de profil.
  const conducteur = aPermission(permissions, PERM.projetConduire)
  const chef = aPermission(permissions, PERM.projetDesigner)
  const ddrm = aPermission(permissions, PERM.adhesionInstruire)
  const sansConducteur = tableau.liaisons.filter((l) => !l.conducteurNom)
  const amont = tableau.liaisons.filter((l) => AMONT.includes(l.etapeCourante))
  const designer = (l: LiaisonResume) => (
    <Button size="sm" variant="secondary" icon="user" onClick={() => void navigate(`/des/parcours/${l.id}?action=designer`)}>{t('parcoursConducteur.designer')}</Button>
  )
  // Procès-verbaux à établir : tâches du conducteur dont le geste se réalise dans l'onglet de la décision (lien servi).
  const pv = conducteur && aPermission(permissions, PERM.pvEtablir) ? tableau.taches.filter((x) => x.qualite === 'R' && x.lien.includes('onglet=decision')) : []
  const formatDate = useFormatDate()
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <Compteurs tableau={tableau} />
      {conducteur && pv.length ? (
        <Card title={t('parcoursActeur.pvAEtablir', { count: pv.length })}>
          <ul className="gm-col" style={{ gap: 'var(--gm-space-2)', listStyle: 'none', margin: 0, padding: 0 }}>
            {pv.map((x) => (
              <li key={x.operationId} className="gm-row" style={{ justifyContent: 'space-between' }}>
                <span>
                  <span className="gm-mono">{x.liaisonReference}</span> · {x.institutionNom}
                </span>
                <Link href={`/des/parcours/${x.liaisonId}?onglet=decision`}>{t('parcoursActeur.etablirPv')}</Link>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      {chef && sansConducteur.length ? (
        <Card title={t('parcoursActeur.aDesigner', { count: sansConducteur.length })}>
          <Paginee lignes={sansConducteur} colonnes={colonnesLiaisons(t, formatDate, { echeances: false, geste: designer })} cle={(l) => l.id} vide={t('parcoursActeur.aucuneLiaison')} />
        </Card>
      ) : null}
      <Taches taches={tableau.taches} />
      {ddrm && amont.length ? (
        <Card title={t('parcoursActeur.amont', { count: amont.length })} actions={<Link href="/des/parcours?etape=CADRAGE,LANCEMENT,ATELIERS">{t('tableau.voirTout')}</Link>}>
          <Paginee lignes={amont} colonnes={colonnesLiaisons(t, formatDate, { echeances: false })} cle={(l) => l.id} vide={t('parcoursActeur.aucuneLiaison')} />
        </Card>
      ) : null}
      <Card
        title={t(conducteur ? 'parcoursActeur.mesProjets' : 'parcoursActeur.liaisons', { count: tableau.liaisons.length })}
        actions={<Link href={conducteur ? '/des/parcours?mesProjets=1' : '/des/parcours'}>{t('tableau.voirTout')}</Link>}
      >
        <Paginee lignes={tableau.liaisons} colonnes={colonnesLiaisons(t, formatDate, { echeances: false })} cle={(l) => l.id} vide={t('parcoursActeur.aucuneLiaison')} />
      </Card>
      {tableau.blocages.length ? <Blocages blocages={tableau.blocages} /> : null}
    </div>
  )
}

export function SectionActeur({ fonctions }: { fonctions: Fonction[] }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const tableau = useTableauActeur(fonctions)
  return (
    <section className="gm-col" aria-label={t('parcoursActeur.titre')}>
      <Titre
        actions={
          aPermission(permissions, PERM.liaisonOuvrir) ? (
            <Button variant="primary" icon="plus" onClick={() => void navigate('/des/parcours?formulaire=ouvrir')}>{t('parcoursListe.ouvrir')}</Button>
          ) : undefined
        }
      >
        {t('parcoursActeur.titre')}
      </Titre>
      <Donnees requete={tableau}>{(tb) => <MonTravail tableau={tb} />}</Donnees>
    </section>
  )
}
