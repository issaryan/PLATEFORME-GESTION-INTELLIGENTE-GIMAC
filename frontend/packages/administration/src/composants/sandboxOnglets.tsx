import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Banner, Button, Card, CheckboxGroup, ConfirmDialog, CrudDialog, DataTable, DateTimeField, Hash, InlineAlert, NumberField, StatusBadge, Tag, TextArea, TextField, type Column, type Status } from '@gimac/design-system'
import { aPermission, usePermissions, type ApiError } from '@gimac/socle'
import { dateHeure, useMessageErreur } from '@gimac/compte'
import { useApprouverPrestataire, useComptesProteges, useDelegations, useDemanderPrestataire, usePerimetresSuspendus, useRebutSandbox, useRefuserPrestataire, useRegistreSandbox, useRevoquerPrestataire } from '../hooks/sandbox'
import { COMPOSANTS_SANDBOX, type CompteProtege, type EntreeRegistreSandbox, type EtatEffectifDelegation, type PageSandbox, type PerimetreSuspendu, type RebutSandbox, type VueDelegation } from '../modele/sandbox'
import { useServicesConsole } from '../services/useServicesConsole'
import { useActes } from './actes'
import { etatListe } from './formats'

// Onglets de l'écran « Sandbox GIMAC » de la console (D-119) : chaque onglet lit une page de MS-IAM.

type Lecture<T> = { data?: PageSandbox<T>; isPending: boolean; isError: boolean; error: ApiError | null; refetch: () => unknown }

/** Table paginée d'un onglet, avec son aide et son texte de liste vide. */
function TableSandbox<T extends object>(p: {
  lire: (page: number, taille: number) => Lecture<T>
  colonnes: Column<T>[]
  cle: (x: T) => string
  aide: { titre: string; texte: string }
  vide: string
  entete?: ReactNode
}) {
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(20)
  const q = p.lire(page, taille)
  const lignes = q.data?.contenu ?? []
  return (
    <div className="gm-col">
      <Banner level="info" title={p.aide.titre}>{p.aide.texte}</Banner>
      {p.entete}
      <DataTable
        columns={p.colonnes}
        rows={lignes}
        rowKey={p.cle}
        total={q.data?.total}
        page={page}
        pageSize={taille}
        onPage={setPage}
        onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }}
        onRetry={() => void q.refetch()}
        emptyText={p.vide}
        density="compact"
        {...etatListe(q, lignes)}
      />
    </div>
  )
}

export function OngletPerimetres() {
  const { t, i18n } = useTranslation()
  const colonnes: Column<PerimetreSuspendu>[] = [
    { key: 'code', header: t('sbx.iam.perimetre.code'), render: (p) => <span className="gm-mono">{p.code}</span> },
    { key: 'etat', header: t('sbx.iam.perimetre.etat'), render: () => <StatusBadge status="crit" label={t('sbx.iam.perimetre.suspendu')} /> },
    { key: 'depuis', header: t('sbx.iam.perimetre.depuis'), render: (p) => dateHeure(p.depuis, i18n.language) },
    { key: 'motif', header: t('sbx.iam.perimetre.motif'), render: (p) => p.motif ?? '-' },
    { key: 'acteurId', header: t('sbx.iam.perimetre.acteur'), render: (p) => <span className="gm-mono">{p.acteurId}</span> },
  ]
  return (
    <TableSandbox lire={usePerimetresSuspendus} colonnes={colonnes} cle={(p) => p.code}
      aide={{ titre: t('sbx.iam.perimetre.aide.titre'), texte: t('sbx.iam.perimetre.aide') }} vide={t('sbx.iam.perimetre.vide')} />
  )
}

export function OngletProtections() {
  const { t, i18n } = useTranslation()
  const colonnes: Column<CompteProtege>[] = [
    { key: 'nom', header: t('sbx.iam.protection.compte'), render: (c) => c.nom },
    { key: 'institutionId', header: t('sbx.iam.protection.institution'), render: (c) => <span className="gm-mono">{c.institutionId}</span> },
    { key: 'etat', header: t('sbx.iam.protection.etat'), render: () => <StatusBadge status="ok" icon="shield-check" label={t('sbx.iam.protection.protege')} /> },
    { key: 'depuis', header: t('sbx.iam.protection.depuis'), render: (c) => dateHeure(c.depuis, i18n.language) },
  ]
  return (
    <TableSandbox lire={useComptesProteges} colonnes={colonnes} cle={(c) => c.compteId}
      aide={{ titre: t('sbx.iam.protection.aide.titre'), texte: t('sbx.iam.protection.aide') }} vide={t('sbx.iam.protection.vide')} />
  )
}

/** Couleur de l'état effectif d'une délégation. */
export const STATUT_DELEGATION: Record<EtatEffectifDelegation, Status> = {
  ACTIVE: 'ok', PROGRAMMEE: 'info', DEMANDEE: 'warn', ECHUE: 'neutral', REFUSEE: 'neutral', REVOQUEE: 'crit',
}

/** Colonnes communes aux mandats et aux accès de prestataire. */
export function colonnesDelegation(t: (k: string) => string, langue: string, portee: Column<VueDelegation>): Column<VueDelegation>[] {
  return [
    { key: 'nomCompte', header: t('sbx.iam.delegation.titulaire'), render: (v) => v.nomCompte },
    portee,
    { key: 'periode', header: t('sbx.iam.delegation.periode'), render: (v) => `${dateHeure(v.delegation.debut, langue)} → ${dateHeure(v.delegation.fin, langue)}` },
    { key: 'etat', header: t('sbx.iam.delegation.etat'), render: (v) => <StatusBadge status={STATUT_DELEGATION[v.etat]} label={t(`sbx.iam.delegation.etat.${v.etat}`)} /> },
  ]
}

const useMandats = (page: number, taille: number) => useDelegations('MANDAT', page, taille)

export function OngletMandats() {
  const { t, i18n } = useTranslation()
  const portee: Column<VueDelegation> = {
    key: 'institution', header: t('sbx.iam.mandat.mandant'),
    render: (v) => (
      <div className="gm-col" style={{ gap: 4 }}>
        <span className="gm-mono">{v.delegation.institutionId}</span>
        <div className="gm-row" style={{ flexWrap: 'wrap', gap: 4 }}>{v.delegation.permissions.map((p) => <Tag key={p}>{t(`droits.permission.${p}`)}</Tag>)}</div>
      </div>
    ),
  }
  return (
    <TableSandbox lire={useMandats} colonnes={colonnesDelegation(t, i18n.language, portee)} cle={(v) => v.delegation.id}
      aide={{ titre: t('sbx.iam.mandat.aide.titre'), texte: t('sbx.iam.mandat.aide') }} vide={t('sbx.iam.mandat.vide')} />
  )
}

const usePrestataires = (page: number, taille: number) => useDelegations('PRESTATAIRE', page, taille)
/** Permission des gestes sur les accès de prestataire (MS-IAM, IAM_ACCOUNT_SECURITY ; le RSSI approuve). */
export const PERMISSION_PRESTATAIRE = 'IAM_ACCOUNT_SECURITY'

/** Saisie JJ/MM/AAAA et HH:MM à l'heure de Douala (UTC+1) en instant ISO ; null si la saisie n'est pas une date. */
export function instantDouala(date: string, heure: string): string | null {
  const d = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(date.trim())
  const h = /^(\d{2}):(\d{2})$/.exec(heure.trim())
  if (!d || !h) return null
  const t = Date.parse(`${d[3]}-${d[2]}-${d[1]}T${h[1]}:${h[2]}:00+01:00`)
  return Number.isNaN(t) ? null : new Date(t).toISOString().replace(/\.\d{3}Z$/, 'Z')
}

/** Demande d'accès de prestataire (US-EXI-24) : compte sans institution, composants, début, durée en heures, motif. */
function DialogueDemandePrestataire({ onFermer, onDemande }: { onFermer: () => void; onDemande: (message: string) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const demander = useDemanderPrestataire()
  const [compteId, setCompteId] = useState('')
  const [composants, setComposants] = useState<string[]>([])
  const [debut, setDebut] = useState({ date: '', time: '' })
  const [duree, setDuree] = useState('4')
  const [motif, setMotif] = useState('')
  const heures = Number(duree)
  const instant = instantDouala(debut.date, debut.time)
  const manque = !compteId.trim() ? t('sbx.iam.prestataire.manque.compte') : !composants.length ? t('sbx.iam.prestataire.manque.composants')
    : !instant ? t('sbx.iam.prestataire.manque.debut') : !(heures >= 1 && heures <= 8) ? t('sbx.iam.prestataire.manque.duree') : !motif.trim() ? t('sbx.iam.prestataire.manque.motif') : ''
  const erreur = demander.error ? message(demander.error) : null
  // Envoi bloqué par la boîte de dialogue tant qu'il manque une saisie : l'instant de début est alors connu.
  const envoyer = () => {
    const fin = new Date(Date.parse(instant!) + heures * 3_600_000).toISOString().replace(/\.\d{3}Z$/, 'Z')
    demander.mutate({ compteId: compteId.trim(), composants, debut: instant!, fin, motif: motif.trim() }, { onSuccess: () => onDemande(t('sbx.iam.prestataire.demandee')) })
  }
  return (
    <CrudDialog open context="create" size="md" title={t('sbx.iam.prestataire.demander')} description={t('sbx.iam.prestataire.demander.aide')} onClose={onFermer}
      onSubmit={envoyer} submitLabel={t('sbx.iam.prestataire.demander.envoyer')} submitIcon="send" submitting={demander.isPending} submitDisabled={!!manque} submitDisabledReason={manque || undefined}
      dirty={!!(compteId || composants.length || motif)} error={erreur ? { title: erreur.texte, correlationId: erreur.correlationId } : undefined}>
      <TextField label={t('sbx.iam.prestataire.compte')} help={t('sbx.iam.prestataire.compte.aide')} required value={compteId} onChange={setCompteId} maxLength={64} />
      <CheckboxGroup label={t('sbx.iam.prestataire.composants')} value={composants} onChange={setComposants} options={COMPOSANTS_SANDBOX.map((c) => ({ value: c, label: c }))} />
      <DateTimeField label={t('sbx.iam.prestataire.debut')} required date={debut.date} time={debut.time} onChange={setDebut} />
      <NumberField label={t('sbx.iam.prestataire.duree')} help={t('sbx.iam.prestataire.duree.aide')} unit={t('sbx.iam.prestataire.heures')} min={1} max={8} step={1} value={duree} onChange={setDuree} />
      <TextArea label={t('sbx.iam.prestataire.motif')} required value={motif} onChange={setMotif} maxLength={500} />
    </CrudDialog>
  )
}

export function OngletPrestataires() {
  const { t, i18n } = useTranslation()
  const permissions = usePermissions()
  const peut = aPermission(permissions, PERMISSION_PRESTATAIRE)
  const actes = useActes()
  const message = useMessageErreur()
  const approuver = useApprouverPrestataire()
  const refuser = useRefuserPrestataire()
  const revoquer = useRevoquerPrestataire()
  const [demande, setDemande] = useState(false)
  const [aApprouver, setAApprouver] = useState<VueDelegation | null>(null)
  const objet = (v: VueDelegation) => [
    { key: t('sbx.iam.delegation.titulaire'), value: v.nomCompte },
    { key: t('sbx.iam.prestataire.composants'), value: v.delegation.composants.join(', ') },
    { key: t('sbx.iam.delegation.periode'), value: `${dateHeure(v.delegation.debut, i18n.language)} → ${dateHeure(v.delegation.fin, i18n.language)}` },
  ]
  const portee: Column<VueDelegation> = {
    key: 'composants', header: t('sbx.iam.prestataire.composantsMotif'),
    render: (v) => (
      <div className="gm-col" style={{ gap: 4 }}>
        <div className="gm-row" style={{ flexWrap: 'wrap', gap: 4 }}>{v.delegation.composants.map((c) => <Tag key={c}>{c}</Tag>)}</div>
        {v.delegation.motif ? <span className="gm-field-help">{v.delegation.motif}</span> : null}
      </div>
    ),
  }
  const gestes: Column<VueDelegation> = {
    key: 'gestes', header: t('sbx.iam.prestataire.gestes'),
    render: (v) => (
      <div className="gm-row" style={{ flexWrap: 'wrap', gap: 4 }}>
        {v.etat === 'DEMANDEE' ? (
          <>
            <Button size="sm" variant="primary" icon="check" onClick={() => setAApprouver(v)}>{t('sbx.iam.prestataire.approuver')}</Button>
            <Button size="sm" variant="ghost" icon="x" onClick={() => actes.motive({
              titre: t('sbx.iam.prestataire.refuser.titre'), libelle: t('sbx.iam.prestataire.refuser'), objet: objet(v), succes: t('sbx.iam.prestataire.refusee'), danger: true,
              executer: (motif) => refuser.mutateAsync({ id: v.delegation.id, motif }),
            })}>{t('sbx.iam.prestataire.refuser')}</Button>
          </>
        ) : null}
        {v.etat === 'ACTIVE' || v.etat === 'PROGRAMMEE' ? (
          <Button size="sm" variant="danger" icon="circle-x" onClick={() => actes.motive({
            titre: t('sbx.iam.prestataire.revoquer.titre'), libelle: t('sbx.iam.prestataire.revoquer'), objet: objet(v), succes: t('sbx.iam.prestataire.revoquee'), danger: true,
            description: t('sbx.iam.prestataire.revoquer.aide'), executer: (motif) => revoquer.mutateAsync({ id: v.delegation.id, motif }),
          })}>{t('sbx.iam.prestataire.revoquer')}</Button>
        ) : null}
      </div>
    ),
  }
  const colonnes = [...colonnesDelegation(t, i18n.language, portee), ...(peut ? [gestes] : [])]
  const confirmerApprobation = async () => {
    const v = aApprouver!
    setAApprouver(null)
    try {
      await approuver.mutateAsync(v.delegation.id)
      actes.setRetour({ niveau: 'info', message: { texte: t('sbx.iam.prestataire.approuvee') } })
    } catch (e) {
      actes.setRetour({ niveau: 'crit', message: message(e) })
    }
  }
  const entete = (
    <>
      {actes.retour ? <InlineAlert level={actes.retour.niveau} title={actes.retour.message.texte} correlationId={actes.retour.message.correlationId} /> : null}
      {peut ? <div className="gm-row"><Button variant="primary" icon="plus" onClick={() => setDemande(true)}>{t('sbx.iam.prestataire.demander')}</Button></div> : null}
    </>
  )
  return (
    <>
      <TableSandbox lire={usePrestataires} colonnes={colonnes} cle={(v) => v.delegation.id} entete={entete}
        aide={{ titre: t('sbx.iam.prestataire.aide.titre'), texte: t('sbx.iam.prestataire.aide') }} vide={t('sbx.iam.prestataire.vide')} />
      {demande ? <DialogueDemandePrestataire onFermer={() => setDemande(false)} onDemande={(m) => { setDemande(false); actes.setRetour({ niveau: 'info', message: { texte: m } }) }} /> : null}
      {aApprouver ? (
        <ConfirmDialog open title={t('sbx.iam.prestataire.approuver.titre')} description={t('sbx.iam.prestataire.approuver.aide')} confirmLabel={t('sbx.iam.prestataire.approuver')}
          object={objet(aApprouver)} onConfirm={() => void confirmerApprobation()} onClose={() => setAApprouver(null)} />
      ) : null}
      {actes.dialogue}
    </>
  )
}

/** Enregistre un fichier rendu par le service (lien de téléchargement éphémère). */
function enregistrer(blob: Blob, nom: string) {
  if (typeof URL.createObjectURL !== 'function') return
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nom
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
}

/** Permission de lecture du registre d'audit de la sandbox (MS-AUDIT, US-EXI-10). */
export const PERMISSION_REGISTRE = 'SBX_TRA_CONSULTATION'

/**
 * Registre d'audit de MS-AUDIT filtré sur la source sandbox (US-EXI-10) : événements à effet des onze services, filtre par
 * institution, export au format ouvert (CSV) ; rebut des enveloppes non conformes, alerte au-dessus du seuil.
 */
export function OngletRegistre() {
  const { t, i18n } = useTranslation()
  const services = useServicesConsole()
  const message = useMessageErreur()
  const [saisie, setSaisie] = useState('')
  const [institution, setInstitution] = useState('')
  const [export_, setExport] = useState<{ niveau: 'info' | 'crit'; texte: string; correlationId?: string } | null>(null)
  const lire = (page: number, taille: number) => useRegistreSandbox(institution, page, taille)
  const colonnes: Column<EntreeRegistreSandbox>[] = [
    { key: 'index', header: t('sbx.audit.index'), render: (e) => <span className="gm-mono">{e.index}</span> },
    { key: 'survenueLe', header: t('sbx.audit.survenue'), render: (e) => dateHeure(e.survenueLe, i18n.language) },
    { key: 'type', header: t('sbx.audit.type'), render: (e) => <span className="gm-mono">{e.type}</span> },
    { key: 'institution', header: t('sbx.audit.institution'), render: (e) => (e.institution ? <span className="gm-mono">{e.institution}</span> : '-') },
    { key: 'acteur', header: t('sbx.audit.acteur'), render: (e) => <span className="gm-mono">{e.acteur}</span> },
    { key: 'empreinte', header: t('sbx.audit.empreinte'), render: (e) => <Hash value={e.empreinte} onCopy={() => void navigator.clipboard?.writeText(e.empreinte).catch(() => undefined)} /> },
  ]
  const exporter = async () => {
    try {
      enregistrer(await services.sandbox.exporterRegistre(institution || undefined), `registre-sandbox${institution ? `-${institution}` : ''}.csv`)
      setExport({ niveau: 'info', texte: t('sbx.audit.exporte') })
    } catch (e) {
      const m = message(e)
      setExport({ niveau: 'crit', texte: m.texte, correlationId: m.correlationId })
    }
  }
  const entete = (
    <>
      <form className="gm-row" style={{ alignItems: 'flex-end', gap: 'var(--gm-space-3)', flexWrap: 'wrap' }} onSubmit={(e) => { e.preventDefault(); setInstitution(saisie.trim()) }}>
        <TextField label={t('sbx.audit.filtre')} help={t('sbx.audit.filtre.aide')} value={saisie} onChange={setSaisie} maxLength={64} />
        <Button type="submit" icon="search">{t('sbx.audit.filtrer')}</Button>
        <Button variant="ghost" icon="download" onClick={() => void exporter()}>{t('sbx.audit.exporter')}</Button>
      </form>
      {export_ ? <InlineAlert level={export_.niveau} title={export_.texte} correlationId={export_.correlationId} /> : null}
    </>
  )
  return (
    <div className="gm-col">
      <TableSandbox lire={lire} colonnes={colonnes} cle={(e) => e.eventId} entete={entete}
        aide={{ titre: t('sbx.audit.aide.titre'), texte: t('sbx.audit.aide') }} vide={institution ? t('sbx.audit.videInstitution', { institution }) : t('sbx.audit.vide')} />
      <RebutRegistre />
    </div>
  )
}

function RebutRegistre() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(20)
  const q = useRebutSandbox(page, taille)
  const lignes = q.data?.contenu ?? []
  const colonnes: Column<RebutSandbox>[] = [
    { key: 'recuLe', header: t('sbx.audit.rebut.recu'), render: (r) => dateHeure(r.recuLe, i18n.language) },
    { key: 'sujet', header: t('sbx.audit.rebut.sujet'), render: (r) => <span className="gm-mono">{r.sujet}</span> },
    { key: 'eventId', header: t('sbx.audit.rebut.identifiant'), render: (r) => <span className="gm-mono">{r.eventId ?? '-'}</span> },
    { key: 'motif', header: t('sbx.audit.rebut.motif'), render: (r) => <StatusBadge status="warn" label={t(`sbx.audit.motif.${r.motif}`, { defaultValue: r.motif })} /> },
  ]
  return (
    <Card title={t('sbx.audit.rebut.titre')}>
      <div className="gm-col">
        {q.data?.alerte ? <Banner level="crit" title={t('sbx.audit.rebut.alerte', { total: q.data.total, seuil: q.data.seuil })} /> : null}
        <DataTable columns={colonnes} rows={lignes} rowKey={(r) => String(r.id)} total={q.data?.total} page={page} pageSize={taille} onPage={setPage}
          onPageSize={(n) => { setTaille(n); setPage(1) }} onRetry={() => void q.refetch()} emptyText={t('sbx.audit.rebut.vide')} density="compact" {...etatListe(q, lignes)} />
      </div>
    </Card>
  )
}
