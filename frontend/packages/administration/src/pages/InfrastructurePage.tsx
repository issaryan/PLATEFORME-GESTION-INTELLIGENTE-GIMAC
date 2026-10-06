import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import {
  Button, Card, DataTable, EmptyState, IconButton, InlineAlert, NumberField, PageHeader, Select, Skeleton, StatusBadge, Tag, type Column, type Status,
} from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { dateHeure } from '@gimac/compte'
import { useCharges, useFluxInfrastructure, useGesteInfrastructure, useJournauxCharge, useOperationsInfrastructure, useSeries } from '../hooks'
import { TendanceMetrique } from '../composants/graphiquesSupervision'
import { CarteNoeuds, CarteTaches, FicheCharge } from '../composants/kubernetes'
import { BORNES_INFRASTRUCTURE, DUREES_JOURNAUX, type Charge, type Geste, type Operation } from '../modele/infrastructure'
import { useActes } from '../composants/actes'
import { Annonce } from '../composants/Annonce'
import { Donnees } from '../composants/communs'
import { STATUT_CHARGE } from '../composants/widgetsPlateforme'


// Exploitation de l'infrastructure (D-78, docs/exploitation/operations-infrastructure.md §4) : charges de travail
// Docker ou Kubernetes du périmètre (état, santé, répliques, redémarrages, image, démarrage, protection),
// journaux à la demande, gestes en fenêtres modales (motif, confirmation par la clé de sécurité FIDO2, mécanisme
// des actes sensibles de D-62), historique paginé ; état rafraîchi par le flux temps réel. Servi dans la console
// et dans l'application interne (D-79). Sans PLATFORM_OPS_EXECUTE (RSSI) : lecture seule, aucun geste.

const STATUT_SANTE: Record<Charge['sante'], Status> = { SAINE: 'ok', DEGRADEE: 'warn', INCONNUE: 'neutral' }
const STATUT_RESULTAT: Record<Operation['resultat'], Status> = { REUSSIE: 'ok', ECHOUEE: 'crit', EN_COURS: 'info' }
const ICONE_GESTE = { REDEMARRER: 'rotate-ccw', ARRETER: 'pause', DEMARRER: 'arrow-right', ECHELLE: 'users' } as const
const ORDRE_GESTES: Geste[] = ['REDEMARRER', 'DEMARRER', 'ARRETER', 'ECHELLE']

/** Nombre de répliques voulu, tenu par le champ lui-même (le dialogue garde l'élément tel qu'à son ouverture). */
function ChampRepliques({ initial, surChangement }: { initial: number; surChangement: (v: string) => void }) {
  const { t } = useTranslation()
  const [valeur, setValeur] = useState(String(initial))
  return (
    <NumberField name="repliques" label={t('infra.repliques')} required min={BORNES_INFRASTRUCTURE.repliquesMin} max={BORNES_INFRASTRUCTURE.repliquesMax}
      help={t('infra.repliques.aide')} value={valeur} onChange={(v) => {
        setValeur(v)
        surChangement(v)
      }} />
  )
}

function Journaux({ nom, fermer }: { nom: string; fermer: () => void }) {
  const { t, i18n } = useTranslation()
  const [lignes, setLignes] = useState('200')
  const [depuis, setDepuis] = useState<string>('PT10M')
  const [demande, setDemande] = useState({ lignes: 200, depuis: 'PT10M' })
  const journaux = useJournauxCharge(nom, demande.lignes, demande.depuis)
  const n = Number(lignes)
  const lignesValides = Number.isInteger(n) && n >= 1 && n <= BORNES_INFRASTRUCTURE.lignesMax
  return (
    <Card title={t('infra.journaux.titre', { nom })} actions={<Button variant="ghost" icon="x" onClick={fermer}>{t('infra.journaux.fermer')}</Button>}>
      <div className="gm-row" style={{ alignItems: 'flex-end', gap: 'var(--gm-space-3)' }}>
        <NumberField name="lignes" label={t('infra.journaux.lignes')} min={1} max={BORNES_INFRASTRUCTURE.lignesMax} value={lignes} onChange={setLignes}
          error={lignesValides ? undefined : t('infra.journaux.lignes.invalide')} />
        <Select name="depuis" label={t('infra.journaux.depuis')} value={depuis} onChange={setDepuis}
          options={DUREES_JOURNAUX.map((d) => ({ value: d, label: t(`infra.journaux.duree.${d}`) }))} />
        <Button icon="refresh" disabled={!lignesValides} onClick={() => {
          setDemande({ lignes: n, depuis })
          void journaux.refetch()
        }}>{t('infra.journaux.actualiser')}</Button>
      </div>
      <Donnees requete={journaux} vide={t('infra.journaux.vide')} estVide={(j) => j.lignes.length === 0}>
        {(j) => (
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            {j.tronque ? <span className="gm-field-help">{t('infra.journaux.tronque', { n: j.lignes.length })}</span> : null}
            <ol aria-label={t('infra.journaux.titre', { nom })} className="gm-panel gm-mono" style={{ listStyle: 'none', margin: 0, padding: 'var(--gm-space-3)', maxHeight: 360, overflow: 'auto' }}>
              {j.lignes.map((l, i) => (
                <li key={i} className="gm-row" style={{ gap: 'var(--gm-space-2)', alignItems: 'baseline', flexWrap: 'nowrap' }}>
                  <span className="gm-field-help" style={{ whiteSpace: 'nowrap' }}>{dateHeure(l.quand, i18n.language)}</span>
                  {l.flux === 'stderr' ? <Tag>{t('infra.journaux.stderr')}</Tag> : null}
                  <span style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{l.texte}</span>
                </li>
              ))}
            </ol>
          </div>
        )}
      </Donnees>
    </Card>
  )
}

function Historique() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(10)
  const operations = useOperationsInfrastructure(page - 1, taille)
  const colonnes: Column<Operation>[] = [
    { key: 'demandeeLe', header: t('infra.op.demandeeLe'), render: (o) => dateHeure(o.demandeeLe, i18n.language) },
    { key: 'charge', header: t('infra.charge'), render: (o) => <span className="gm-mono">{o.charge}</span> },
    { key: 'geste', header: t('infra.op.geste'), render: (o) => t(`infra.geste.${o.geste}`) },
    { key: 'acteur', header: t('infra.op.acteur'), render: (o) => o.acteur.nomAffiche ?? o.acteur.id },
    { key: 'motif', header: t('infra.op.motif'), render: (o) => o.motif },
    {
      key: 'resultat', header: t('infra.op.resultat'),
      render: (o) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <StatusBadge status={STATUT_RESULTAT[o.resultat]} label={t(`infra.resultat.${o.resultat}`)} />
          {o.erreur ? <span className="gm-field-help">{o.erreur}</span> : null}
        </span>
      ),
    },
    {
      key: 'etats', header: t('infra.op.avantApres'),
      render: (o) => `${o.avant ? t(`infra.etat.${o.avant}`, { defaultValue: o.avant }) : '…'} → ${o.apres ? t(`infra.etat.${o.apres}`, { defaultValue: o.apres }) : '…'}`,
    },
  ]
  return (
    <Card title={t('infra.historique.titre')}>
      <DataTable columns={colonnes} rows={operations.data?.contenu ?? []} rowKey={(o) => o.id} density="compact"
        state={operations.isPending ? 'loading' : operations.isError ? 'error' : operations.data?.contenu.length ? 'ok' : 'empty'}
        error={operations.error ?? undefined} onRetry={() => void operations.refetch()} emptyText={t('infra.historique.vide')}
        total={operations.data?.total ?? 0} page={page} pageSize={taille} onPage={setPage} onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }} />
    </Card>
  )
}

export default function InfrastructurePage() {
  const { t, i18n } = useTranslation()
  const permissions = usePermissions()
  const executer = aPermission(permissions, 'PLATFORM_OPS_EXECUTE')
  const charges = useCharges()
  const etatFlux = useFluxInfrastructure()
  const geste = useGesteInfrastructure()
  const actes = useActes()
  const [parametres, setParametres] = useSearchParams()
  const choisie = parametres.get('charge') ?? ''
  const repliques = useRef('1')
  const cpuCharges = useSeries('CHARGE_CPU', 'UNE_HEURE')
  const [courbes, setCourbes] = useState<Charge | null>(null)
  const choisir = (nom: string) => setParametres((p) => {
    const q = new URLSearchParams(p)
    if (nom) q.set('charge', nom)
    else q.delete('charge')
    return q
  }, { replace: true })

  const lancer = (c: Charge, g: Geste) => {
    repliques.current = String(c.repliques.voulues)
    actes.sensible({
      titre: t(`infra.geste.${g}.titre`, { nom: c.nom }),
      objet: [
        { key: t('infra.charge'), value: c.nom, mono: true },
        { key: t('infra.etat'), value: t(`infra.etat.${c.etat}`) },
        { key: t('infra.repliques.colonne'), value: t('infra.widget.repliques', { pretes: c.repliques.pretes, voulues: c.repliques.voulues }) },
      ],
      contenu: { charge: c.nom, geste: g },
      succes: t('infra.geste.accepte', { nom: c.nom }),
      extra: g === 'ECHELLE'
        ? <ChampRepliques initial={c.repliques.voulues} surChangement={(v) => { repliques.current = v }} />
        : <InlineAlert level={g === 'ARRETER' ? 'warn' : 'info'}>{t(`infra.geste.${g}.texte`)}</InlineAlert>,
      executer: async (motif, assertion) => {
        if (motif.length > BORNES_INFRASTRUCTURE.motifMax) throw new Error(t('erreur.OPS_MOTIF_INVALIDE'))
        const r = Number(repliques.current)
        if (g === 'ECHELLE' && (!Number.isInteger(r) || r < BORNES_INFRASTRUCTURE.repliquesMin || r > BORNES_INFRASTRUCTURE.repliquesMax)) throw new Error(t('erreur.OPS_REPLIQUES_INVALIDES'))
        await geste.mutateAsync({ nom: c.nom, geste: g, demande: g === 'ECHELLE' ? { motif, repliques: r } : { motif }, assertion })
      },
    })
  }

  const colonnes: Column<Charge>[] = [
    {
      key: 'nom', header: t('infra.chargeImage'),
      render: (c) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
            <span className="gm-mono">{c.nom}</span>
            {c.protegee ? <span title={t('infra.protegee.aide')}><Tag>{t('infra.protegee')}</Tag></span> : null}
          </span>
          {/* Image de la charge, sous son nom : la table garde ses gestes visibles sans défilement horizontal. */}
          <span className="gm-field-help gm-mono" style={{ overflowWrap: 'anywhere' }} title={t('infra.image')}>{c.image ?? '…'}</span>
        </span>
      ),
    },
    // État et santé dans une même colonne, l'un sous l'autre : la table tient dans la largeur, gestes compris.
    {
      key: 'etat', header: t('infra.etatSante'),
      render: (c) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)', alignItems: 'flex-start' }}>
          <StatusBadge status={STATUT_CHARGE[c.etat]} label={t(`infra.etat.${c.etat}`)} />
          <span title={t('infra.sante')}><StatusBadge status={STATUT_SANTE[c.sante]} label={t(`infra.sante.${c.sante}`)} /></span>
        </span>
      ),
    },
    { key: 'tendance', header: t('graphiques.colonne.cpuCharge'), render: (c) => <TendanceMetrique series={cpuCharges.data} nom={c.nom} libelle={t('graphiques.metrique.CHARGE_CPU') + ' · ' + c.nom} /> },
    { key: 'repliques', header: t('infra.repliques.colonne'), type: 'number', render: (c) => `${c.repliques.pretes} / ${c.repliques.voulues}` },
    { key: 'redemarrages', header: t('infra.redemarrages'), type: 'number', render: (c) => c.redemarrages },
    { key: 'demarreeLe', header: t('infra.demarreeLe'), render: (c) => dateHeure(c.demarreeLe, i18n.language) },
    {
      key: 'actions', header: t('infra.actions'),
      render: (c) => (
        // Boutons à pictogramme (nom accessible complet, info-bulle) : la colonne tient sans défilement horizontal.
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'nowrap' }}>
          <span title={t('infra.courbes.voir', { nom: c.nom })}>
            <IconButton size="sm" variant="ghost" icon="gauge" onClick={() => setCourbes(c)} label={t('infra.courbes.voir', { nom: c.nom })} />
          </span>
          <span title={t('infra.journaux.voir', { nom: c.nom })}>
            <IconButton size="sm" variant="ghost" icon="file-text" onClick={() => choisir(c.nom)} label={t('infra.journaux.voir', { nom: c.nom })} />
          </span>
          {executer ? ORDRE_GESTES.filter((g) => c.gestes.includes(g)).map((g) => (
            <span key={g} title={t(`infra.geste.${g}.titre`, { nom: c.nom })}>
              <IconButton size="sm" variant={g === 'ARRETER' ? 'danger' : 'secondary'} icon={ICONE_GESTE[g]} onClick={() => lancer(c, g)} label={t(`infra.geste.${g}.titre`, { nom: c.nom })} />
            </span>
          )) : null}
        </span>
      ),
    },
  ]

  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <PageHeader title={t('infra.titre')} subtitle={t(executer ? 'infra.sousTitre' : 'infra.sousTitre.lecture')} />
      <Annonce retour={actes.retour} />
      {etatFlux === 'perdu' ? <InlineAlert level="warn" title={t('console.flux.perdu.texte')} /> : null}
      {charges.isPending ? <Skeleton height={160} /> : charges.isError || !charges.data ? (
        <InlineAlert level="crit" title={t('commun.erreur.titre')} correlationId={charges.error?.correlationId || undefined}>
          {charges.error ? t(`erreur.${charges.error.code}`, { defaultValue: charges.error.message || t('commun.erreur.titre') }) : null}
        </InlineAlert>
      ) : charges.data.orchestrateur === 'AUCUN' ? (
        <EmptyState icon="monitor" title={t('infra.nonConfiguree.titre')} text={t('infra.nonConfiguree.texte')} />
      ) : (
        <>
          <Card title={t('infra.charges.titre', { orchestrateur: t(`infra.orchestrateur.${charges.data.orchestrateur}`) })}>
            {!executer ? <InlineAlert level="info">{t('infra.lectureSeule')}</InlineAlert> : null}
            <DataTable columns={colonnes} rows={charges.data.charges} rowKey={(c) => c.nom} density="compact" liveUpdates
              state={charges.data.charges.length ? 'ok' : 'empty'} emptyText={t('infra.charges.vide')} />
          </Card>
          {choisie ? <Journaux key={choisie} nom={choisie} fermer={() => choisir('')} /> : null}
          {courbes ? <FicheCharge charge={courbes} orchestrateur={charges.data.orchestrateur} fermer={() => setCourbes(null)} /> : null}
          {charges.data.orchestrateur === 'KUBERNETES' ? <CarteNoeuds /> : null}
          {charges.data.orchestrateur === 'KUBERNETES' ? <CarteTaches /> : null}
          <Historique />
        </>
      )}
      {actes.dialogue}
    </div>
  )
}
