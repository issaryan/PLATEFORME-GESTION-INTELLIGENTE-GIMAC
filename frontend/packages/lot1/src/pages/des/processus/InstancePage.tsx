import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { ReactFlowProvider } from '@xyflow/react'
import {
  Button, ConfirmDialog, CrudDialog, DataTable, FileDropzone, Icon, InlineAlert, KeyValueList, PageHeader, SignDialog, StatusBadge, Timeline, cx, type Column,
} from '@gimac/design-system'
import { EtatsRequete, cleSecuriteDisponible, estApiError, refusReauth, sha256Hex, usePermissions, useSession, type Progression } from '@gimac/socle'
import { ETATS_ETAPE, type EtapeInstance, type InstanceProcessus } from '../../../modele/processus'
import { useActeInstance, useCandidatsEtape, useInstance } from '../../../hooks/processus'
import { useEffet } from '../../../hooks/base'
import { clesPilotage } from '../../../hooks/pilotage'
import { useServices } from '../../../services/ServicesProvider'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { useDates, useLibelleUnite } from '../activite/communs'
import { CanevasProcessus, type CarteEtape } from './CanevasProcessus'
import { ChampValeur, erreurValeur, valeurEnvoyee, valeurSaisie } from './DialogueLancement'
import { BadgeEtape, BadgeInstance, ChoixPersonne, delais, useErreurProcessus } from './communs'
import { ValeursFormulaire } from './FormulaireLancement'
import { DocumentProcessus, tailleLisible, useTypesLisibles } from './DocumentProcessus'
import { telechargerPdfInstance } from './pdf'
import { useCatalogueFonctionnalites } from '../../../catalogue'
import { accesFonctionnalite } from '../../../fonctionnalites'

// Suivi d'une instance de processus d'unité (D-87 §4, /des/processus/instances/:id?etape=CODE) : graphe coloré par l'état
// des étapes (ou liste équivalente), panneau de l'étape choisie (action, exécutant ou tâche à prendre, livrables déposés
// par le dépôt fragmenté de MS-DOCUMENT, champs, signatures FIDO2 de finalité SIGNATURE_PROCESSUS, validation ou
// renvoi, fin de l'étape, réaffectation), suspension, reprise et annulation selon droits et moi, historique.
// D-98 : valeurs du formulaire de lancement visibles de tous (initiateur, intervenants, chef) ; instance en cours relue
// périodiquement (suivi en temps réel) ; l'initiateur revient à « Mes demandes ».

export default function InstancePage() {
  const { id } = useParams()
  const { t } = useTranslation()
  const i = useInstance(id)
  // QUO-03, QUO-12 : un refus de lecture (403) ou une instance inexistante (404) s'explique, sans « Réessayer ».
  const refus = i.isError && estApiError(i.error) && (i.error.status === 403 || i.error.status === 404) ? i.error : null
  if (refus) {
    const introuvable = refus.status === 404
    return (
      <InlineAlert level={introuvable ? 'warn' : 'info'} title={t(introuvable ? 'proc.acces.introuvableTitre' : 'proc.acces.refuseTitre')} correlationId={refus.correlationId || undefined}>
        {t(introuvable ? 'proc.acces.introuvableTexte' : 'proc.acces.refuseTexte')}
      </InlineAlert>
    )
  }
  return (
    <EtatsRequete etat={i} emptyTitle={t('commun.vide')} errorTitle={t('proc.erreur.lectureInstance')} retryLabel={t('commun.reessayer')}>
      {(x) => <ReactFlowProvider><Suivi instance={x} /></ReactFlowProvider>}
    </EtatsRequete>
  )
}

const taille = tailleLisible

/** Empreinte que signera une signature (§5.3) : SHA-256 des lignes CODE:documentId:sha256 des livrables portés déposés. */
export async function empreinteAttendue(instance: InstanceProcessus, porte: string[]): Promise<string> {
  const livrables = instance.etapes.flatMap((e) => e.livrables)
  const lignes = porte.map((c) => livrables.find((l) => l.code === c && l.depot)).filter((l) => !!l).map((l) => `${l!.code}:${l!.depot!.documentId}:${l!.depot!.sha256}\n`).join('')
  return sha256Hex(new TextEncoder().encode(lignes))
}

type Geste = 'terminer' | 'valider' | 'renvoyer' | 'reaffecter' | 'suspendre' | 'reprendre' | 'annuler'

function Suivi({ instance: i }: { instance: InstanceProcessus }) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const session = useSession()
  // L'initiateur suit sa demande : retour à « Mes demandes » ; les autres reviennent aux instances de l'unité.
  const initiateur = !!session && session.sujet === i.lanceur.id
  // Liens de retour selon le catalogue (D-98) : l'instance s'ouvre aussi sans « Processus de l'unité » (lien profond).
  const catalogue = useCatalogueFonctionnalites()
  const permissions = usePermissions()
  const ouverte = (cle: string) => accesFonctionnalite(catalogue, cle, permissions) === 'ouvert'
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const erreur = useErreurProcessus()
  const services = useServices()
  const [params, setParams] = useSearchParams()
  const vue = params.get('vue') === 'liste' ? 'liste' : 'canevas'
  const parDefaut = i.etapes.find((e) => e.moi.executer || e.moi.prendre || e.moi.signer.length || e.moi.valider) ?? i.etapes.find((e) => ['ACTIVE', 'A_SIGNER', 'A_VALIDER'].includes(e.etat)) ?? i.etapes[0]
  // L'étape ouverte par défaut reste celle de l'arrivée : un acte (signature, fin) ne fait pas changer le panneau.
  const arrivee = useRef(parDefaut.code)
  const choisie = i.etapes.find((e) => e.code === (params.get('etape') ?? arrivee.current)) ?? parDefaut
  const [geste, setGeste] = useState<Geste | null>(null)
  const [echec, setEchec] = useState<unknown>(null)
  const [annonce, setAnnonce] = useState('')
  const acte = <V,>(f: (s: ReturnType<typeof useServices>, v: V) => Promise<InstanceProcessus>) => f
  const suspendre = useActeInstance(acte((s, motif: string) => s.processus.suspendre(i.id, motif)))
  const reprendre = useActeInstance(acte((s, motif: string) => s.processus.reprendre(i.id, motif)))
  const annuler = useActeInstance(acte((s, motif: string) => s.processus.annuler(i.id, motif)))
  const choisir = (code: string) => setParams((x) => {
    const n = new URLSearchParams(x)
    n.set('etape', code)
    return n
  })
  const basculerVue = (v: 'canevas' | 'liste') => setParams((x) => {
    const n = new URLSearchParams(x)
    if (v === 'liste') n.set('vue', 'liste')
    else n.delete('vue')
    return n
  })
  const cartes = useMemo<CarteEtape[]>(() => i.etapes.map((e) => ({
    code: e.code, libelle: e.libelle, predecesseurs: e.predecesseurs, position: e.position, etat: e.etat,
    lignes: [
      e.executant ? e.executant.nomAffiche : e.aPrendre ? t('proc.suivi.aPrendreCourt') : '',
      e.echeance ? t('proc.suivi.echeance', { date: dates.jour(e.echeance) }) : delais(t, e.delaiJours, e.delaiMaxJours),
      e.signatures.length ? t('proc.suivi.signatures', { n: e.signatures.filter((s) => s.etat === 'SIGNEE').length, total: e.signatures.length }) : '',
    ].filter(Boolean),
  })), [i.etapes, t, dates])
  const executer = (m: { mutate: (v: string, o: object) => void }, motif: string, message: string) => {
    setEchec(null)
    m.mutate(motif, { onSuccess: () => setAnnonce(message), onError: setEchec })
  }
  const refus = erreur(echec)
  const telecharger = async () => {
    setEchec(null)
    try {
      await telechargerPdfInstance(services.processus, i, (i18n.language || 'fr').slice(0, 2))
      setAnnonce(t('proc.suivi.pdfPret'))
    } catch (e) { setEchec(e) }
  }
  /** Clé d'une désignation (CODE_ETAPE, CODE_ETAPE.validateur, CODE_ETAPE.signature.CODE) en clair. */
  const libelleDesignation = (cle: string) => {
    const [code, role, signature] = cle.split('.')
    const e = i.etapes.find((x) => x.code === code)
    const etape = e?.libelle ?? code
    if (role === 'validateur') return t('proc.lancement.validateur', { etape })
    if (role === 'signature') return t('proc.lancement.signataire', { etape, signature: e?.signatures.find((s) => s.code === signature)?.libelle ?? signature })
    return t('proc.lancement.executant', { etape })
  }
  const colonnes: Column<EtapeInstance & Record<string, unknown>>[] = [
    { key: 'code', header: t('proc.champ.code'), type: 'mono', width: 120 },
    { key: 'libelle', header: t('proc.champ.libelle') },
    { key: 'etat', header: t('proc.champ.etat'), render: (e) => <BadgeEtape etat={e.etat} /> },
    { key: 'executant', header: t('proc.suivi.executant'), render: (e) => e.executant?.nomAffiche ?? (e.aPrendre ? t('proc.suivi.aPrendreCourt') : '') },
    { key: 'echeance', header: t('proc.suivi.echeanceTitre'), render: (e) => (e.echeance ? dates.jour(e.echeance) : '') },
    { key: 'actions', header: '', type: 'actions', render: (e) => <Button size="sm" variant="ghost" aria-label={t('proc.suivi.ouvrirEtape', { code: e.code })} onClick={() => choisir(e.code)}>{t('proc.ouvrir')}</Button> },
  ]

  return (
    <div className="gm-col" data-testid="suivi-instance" style={{ gap: 'var(--gm-space-3)' }}>
      <PageHeader title={i.intitule} subtitle={`${i.processus.libelle} · v${i.processus.version} · ${libelleUnite(i.unite)}`}
        actions={(
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
            <BadgeInstance etat={i.etat} />
            <Button size="sm" variant="secondary" icon="download" onClick={telecharger}>{t('proc.suivi.telechargerPdf')}</Button>
            {i.droits.suspendre ? <Button size="sm" icon="pause" onClick={() => setGeste('suspendre')}>{t('proc.suivi.suspendre')}</Button> : null}
            {i.droits.reprendre ? <Button size="sm" icon="refresh" onClick={() => setGeste('reprendre')}>{t('proc.suivi.reprendre')}</Button> : null}
            {i.droits.annuler ? <Button size="sm" variant="danger" icon="x" onClick={() => setGeste('annuler')}>{t('proc.suivi.annuler')}</Button> : null}
            {initiateur && ouverte('mesDemandes') ? <Button size="sm" variant="ghost" icon="inbox" onClick={() => navigate('/des/demandes')}>{t('dem.mesDemandes.titre')}</Button> : null}
            {ouverte('processus') ? <Button size="sm" variant="ghost" icon="arrow-left" onClick={() => navigate('/des/processus?vue=instances')}>{t('proc.suivi.retour')}</Button> : null}
          </span>
        )} />
      <KeyValueList items={[
        ...(i.reference ? [{ key: t('proc.suivi.reference'), value: <span className="gm-mono">{i.reference}</span> }] : []),
        { key: t('proc.suivi.lanceur'), value: `${i.lanceur.nomAffiche} · ${dates.instant(i.lanceeLe)}` },
        { key: t('proc.instance.avancement'), value: t('proc.instance.avancementDe', { n: i.avancement.terminees, total: i.avancement.total }) },
        ...Object.entries(i.designations).map(([k, p]) => ({ key: libelleDesignation(k), value: p.nomAffiche })),
        ...(i.delais ? [{ key: t('proc.suivi.delaisDemande'), value: <span className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>
          {t('proc.suivi.echeances', { cible: dates.jour(i.delais.echeanceCible), max: dates.jour(i.delais.echeanceMax) })}
          {i.delais.enRetard ? <StatusBadge status="crit" icon="clock" label={t('proc.suivi.enRetard')} /> : i.delais.aLApproche ? <StatusBadge status="warn" icon="clock" label={t('proc.suivi.aLApproche')} /> : null}
        </span> }] : []),
        ...(i.closeLe ? [{ key: t('proc.suivi.closeLe'), value: dates.instant(i.closeLe) }] : []),
        ...(i.motif ? [{ key: t('proc.suivi.motif'), value: i.motif }] : []),
      ]} />
      {i.formulaire?.length ? (
        <section className="gm-proc-bloc" aria-labelledby="gm-proc-formulaire" data-testid="valeurs-lancement">
          <h3 id="gm-proc-formulaire">{t('dem.suivi.formulaire')}</h3>
          <ValeursFormulaire valeurs={i.formulaire} instanceId={i.id} />
        </section>
      ) : null}
      {i.etat === 'SUSPENDUE' ? <InlineAlert level="warn" title={t('proc.suivi.suspendue')} /> : null}
      {refus ? <InlineAlert level="crit" title={refus.titre} correlationId={refus.correlationId} action={<Button size="sm" variant="ghost" icon="x" onClick={() => setEchec(null)}>{t('commun.fermer')}</Button>} /> : null}
      <div className="gm-sr-only" role="status" aria-live="polite">{annonce}</div>
      <div className="gm-row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
        <div className="gm-row" role="group" aria-label={t('proc.vue.titre')} style={{ gap: 0 }}>
          {(['canevas', 'liste'] as const).map((v) => (
            <button key={v} type="button" className={cx('gm-btn', 'gm-btn-sm', vue === v ? 'gm-btn-primary' : 'gm-btn-secondary')} aria-pressed={vue === v} onClick={() => basculerVue(v)}>
              <Icon name={v === 'canevas' ? 'workflow' : 'menu'} size={20} />{t(`proc.vue.${v}`)}
            </button>
          ))}
        </div>
        <div className="gm-proc-legende" role="list" aria-label={t('proc.suivi.legende')}>
          {ETATS_ETAPE.map((e) => <span key={e} role="listitem"><BadgeEtape etat={e} /></span>)}
        </div>
      </div>
      <div className="gm-proc-graphe">
        {vue === 'canevas' ? (
          <CanevasProcessus cartes={cartes} lecture choisie={choisie?.code} libelleOuvrir={t('proc.ouvrir')} libelleEtat={(e) => t(`proc.etape.etat.${e}`)} testId="instance-canevas" onOuvrir={choisir} miniCarte={false} />
        ) : (
          <DataTable columns={colonnes} rows={i.etapes as (EtapeInstance & Record<string, unknown>)[]} rowKey={(e) => e.code} state="ok" density="compact" selected={choisie ? [choisie.code] : []} />
        )}
      </div>
      <div className="gm-proc-suivi">
        {choisie ? <PanneauEtape key={choisie.code} instance={i} etape={choisie} onAnnonce={setAnnonce} /> : <span />}
      <section className="gm-col" aria-labelledby="gm-proc-historique" style={{ gap: 'var(--gm-space-2)' }}>
        <h2 id="gm-proc-historique" className="gm-type-h3" style={{ margin: 0 }}>{t('proc.suivi.historique')}</h2>
        <Timeline events={[...i.historique].reverse().map((h) => ({
          // QUO-07 : l'étape est nommée par son libellé, non par son code.
          event: `${t(`proc.historique.${h.action}`, { defaultValue: h.action })}${h.etape ? ` · ${i.etapes.find((x) => x.code === h.etape)?.libelle ?? h.etape}` : ''}${h.detail ? ` : ${h.detail}` : ''}`,
          at: dates.instant(h.quand), actor: h.acteur?.nomAffiche,
        }))} />
      </section>
      </div>
      {geste === 'suspendre' ? <ConfirmDialog open reason title={t('proc.suivi.suspendre')} description={t('proc.suivi.suspendreTexte')} confirmLabel={t('proc.suivi.suspendre')}
        onClose={() => setGeste(null)} onConfirm={(m) => { setGeste(null); executer(suspendre, m, t('proc.annonce.suspendue')) }} /> : null}
      {geste === 'reprendre' ? <ConfirmDialog open title={t('proc.suivi.reprendre')} description={t('proc.suivi.reprendreTexte')} confirmLabel={t('proc.suivi.reprendre')}
        onClose={() => setGeste(null)} onConfirm={(m) => { setGeste(null); executer(reprendre, m, t('proc.annonce.reprise')) }} /> : null}
      {geste === 'annuler' ? <ConfirmDialog open danger reason title={t('proc.suivi.annuler')} description={t('proc.suivi.annulerTexte')} confirmLabel={t('proc.suivi.annuler')}
        onClose={() => setGeste(null)} onConfirm={(m) => { setGeste(null); executer(annuler, m, t('proc.annonce.annulee')) }} /> : null}
    </div>
  )
}

function PanneauEtape({ instance: i, etape: e, onAnnonce }: { instance: InstanceProcessus; etape: EtapeInstance; onAnnonce: (m: string) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const libelleProfil = useLibelleProfilEquipe()
  const libelleUnite = useLibelleUnite()
  const erreur = useErreurProcessus()
  const [geste, setGeste] = useState<Geste | null>(null)
  const [echec, setEchec] = useState<unknown>(null)
  const [signature, setSignature] = useState<string | null>(null)
  const prendre = useEffet((s, tacheId: string) => s.pilotage.prendre(tacheId), [clesPilotage.tout])
  const terminer = useActeInstance((s) => s.processus.terminer(i.id, e.code))
  const valider = useActeInstance((s, v: { decision: 'VALIDER' | 'RENVOYER'; motif?: string }) => s.processus.valider(i.id, e.code, v.decision, v.motif))
  const reaffecter = useActeInstance((s, v: { personneId: string; motif: string }) => s.processus.reaffecter(i.id, e.code, v.personneId, v.motif))
  const champs = useActeInstance((s, v: Record<string, string | number | null>) => s.processus.saisirChamps(i.id, e.code, v))
  const [valeurs, setValeurs] = useState<Record<string, string>>(() => Object.fromEntries(e.champs.map((c) => [c.code, valeurSaisie(c, c.valeur)])))
  const [tenteChamps, setTenteChamps] = useState(false)
  const attendu = (a: { personne: { nomAffiche: string } | null; profil: string | null; unite: string | null } | null) =>
    !a ? t('proc.aucun') : a.personne ? a.personne.nomAffiche : t('proc.suivi.aPrendre', { profil: a.profil ? libelleProfil(a.profil) : t('proc.suivi.toutMembre'), unite: a.unite ? libelleUnite(a.unite) : t('proc.suivi.toutLeGimac') })
  const ok = (message: string) => () => { setEchec(null); onAnnonce(message) }
  const refus = erreur(echec)
  const details = (echec as { details?: { livrables?: string[]; champs?: string[] } } | null)?.details
  const erreursChamps = Object.fromEntries(e.champs.map((c) => [c.code, erreurValeur(c, valeurs[c.code] ?? '', t)]))
  const champsModifies = e.champs.filter((c) => (valeurs[c.code] ?? '') !== valeurSaisie(c, c.valeur))
  const cleOk = cleSecuriteDisponible()
  return (
    <aside className="gm-col" aria-labelledby="gm-proc-etape-titre" style={{ gap: 'var(--gm-space-3)' }}>
      <div className="gm-row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
        <h2 id="gm-proc-etape-titre" className="gm-type-h3" style={{ margin: 0 }}>{e.libelle} <span className="gm-mono gm-type-small">{e.code}</span></h2>
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
          <BadgeEtape etat={e.etat} />
          {e.enRetard ? <StatusBadge status="crit" icon="clock" label={t('proc.suivi.enRetard')} /> : e.aLApproche ? <StatusBadge status="warn" icon="clock" label={t('proc.suivi.aLApproche')} /> : null}
          {e.renvois ? <StatusBadge status="warn" icon={null} label={t('proc.suivi.renvois', { count: e.renvois })} /> : null}
        </span>
      </div>
      {refus ? (
        <InlineAlert level="crit" title={refus.titre} correlationId={refus.correlationId}>
          {/* QUO-07 : champs et livrables manquants nommés par leur libellé, non par leur code. */}
          {details?.livrables?.length ? t('proc.manquants.livrables', { liste: details.livrables.map((c) => e.livrables.find((l) => l.code === c)?.libelle ?? c).join(', ') })
            : details?.champs?.length ? t('proc.manquants.champs', { liste: details.champs.map((c) => e.champs.find((x) => x.code === c)?.libelle ?? c).join(', ') }) : null}
        </InlineAlert>
      ) : null}
      {e.action ? <p style={{ margin: 0 }}>{e.action}</p> : null}
      <KeyValueList items={[
        { key: t('proc.suivi.executant'), value: e.executant ? e.executant.nomAffiche : e.aPrendre ? attendu({ personne: null, ...e.aPrendre }) : t('proc.suivi.pasEncore') },
        ...(e.validateur ? [{ key: t('proc.intervenant.validateur'), value: attendu(e.validateur) }] : []),
        ...(e.echeanceCible && e.echeanceCible !== e.echeance ? [{ key: t('proc.suivi.echeanceCibleTitre'), value: dates.jour(e.echeanceCible) }] : []),
        ...(e.echeance ? [{ key: e.echeanceCible && e.echeanceCible !== e.echeance ? t('proc.suivi.echeanceMaxTitre') : t('proc.suivi.echeanceTitre'), value: dates.jour(e.echeance) }] : []),
        ...(e.activeeLe ? [{ key: t('proc.suivi.activeeLe'), value: dates.instant(e.activeeLe) }] : []),
        ...(e.termineeLe ? [{ key: t('proc.suivi.termineeLe'), value: dates.instant(e.termineeLe) }] : []),
      ]} />
      {e.moi.prendre && e.tacheId ? (
        <Button variant="primary" icon="user" loading={prendre.isPending} onClick={() => prendre.mutate(e.tacheId!, { onSuccess: ok(t('proc.annonce.prise')), onError: setEchec })}>{t('proc.suivi.prendre')}</Button>
      ) : null}
      {e.livrables.length ? (
        <section className="gm-proc-bloc" aria-label={t('proc.onglet.livrables')}>
          <h3>{t('proc.onglet.livrables')}</h3>
          {e.livrables.map((l) => (
            <div key={l.code} className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
              <span><strong>{l.libelle}</strong> <span className="gm-mono gm-type-small">{l.code}</span>{l.obligatoire ? <span className="gm-field-help"> · {t('proc.livrable.obligatoire')}</span> : null}</span>
              {l.depot ? (
                <span className="gm-col" style={{ gap: 0 }}>
                  <span className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>
                    <DocumentProcessus instanceId={i.id} documentId={l.depot.documentId} nom={l.depot.nomFichier} typeMime={l.depot.typeMime} taille={l.depot.taille} />
                    <span>{l.depot.deposePar ? `· ${l.depot.deposePar.nomAffiche} ` : ''}· {dates.instant(l.depot.deposeLe)}</span>
                  </span>
                  <span className="gm-proc-hash" title="SHA-256">SHA-256 {l.depot.sha256}</span>
                </span>
              ) : <span className="gm-field-help">{t('proc.suivi.nonDepose')}</span>}
              {e.moi.executer ? <DepotLivrable instanceId={i.id} etape={e.code} livrable={l} onDepose={(nom) => { setEchec(null); onAnnonce(t('proc.annonce.depose', { nom })) }} onEchec={setEchec} /> : null}
            </div>
          ))}
        </section>
      ) : null}
      {e.champs.length ? (
        <section className="gm-proc-bloc" aria-label={t('proc.onglet.champs')}>
          <h3>{t('proc.onglet.champs')}</h3>
          {e.moi.executer ? (
            <>
              {e.champs.map((c) => <ChampValeur key={c.code} prefixe="valeurs" c={c} valeur={valeurs[c.code] ?? ''} onChange={(v) => setValeurs((o) => ({ ...o, [c.code]: v }))} erreur={tenteChamps ? erreursChamps[c.code] : undefined} />)}
              <div>
                <Button size="sm" icon="check" disabled={!champsModifies.length} disabledReason={t('proc.suivi.champsInchanges')} loading={champs.isPending}
                  onClick={() => {
                    setTenteChamps(true)
                    if (champsModifies.some((c) => (erreursChamps[c.code] && (valeurs[c.code] ?? '').trim()) || (c.type === 'NOMBRE' && erreursChamps[c.code] && valeurs[c.code]))) return
                    champs.mutate(Object.fromEntries(champsModifies.map((c) => [c.code, valeurEnvoyee(c, valeurs[c.code] ?? '')])), { onSuccess: ok(t('proc.annonce.champs')), onError: setEchec })
                  }}>{t('proc.suivi.enregistrerChamps')}</Button>
              </div>
            </>
          ) : <KeyValueList items={e.champs.map((c) => ({ key: c.libelle, value: c.valeur === null || c.valeur === '' ? t('proc.suivi.nonRenseigne') : valeurSaisie(c, c.valeur) }))} />}
        </section>
      ) : null}
      {e.signatures.length ? (
        <section className="gm-proc-bloc" aria-label={t('proc.onglet.signatures')}>
          <h3>{t('proc.onglet.signatures')}</h3>
          {e.signatures.some((s) => e.moi.signer.includes(s.code)) && !cleOk ? <InlineAlert level="warn" title={t('proc.suivi.httpsTitre')}>{t('proc.suivi.httpsTexte')}</InlineAlert> : null}
          {[...e.signatures].sort((a, b) => a.ordre - b.ordre).map((s) => (
            <div key={s.code} className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
              <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
                <strong>{s.libelle}</strong>
                <StatusBadge status={s.etat === 'SIGNEE' ? 'ok' : 'warn'} label={t(`proc.signature.etat.${s.etat}`)} />
                <span className="gm-field-help">{t('proc.signature.ordreN', { n: s.ordre })} · {t('proc.signature.portes', { liste: s.porte.join(', ') })}</span>
              </span>
              <span className="gm-field-help">{s.etat === 'SIGNEE' && s.signePar ? t('proc.signature.signeePar', { nom: s.signePar.nomAffiche, date: s.signeLe ? dates.instant(s.signeLe) : '' }) : t('proc.signature.attendue', { qui: attendu(s.attendu) })}</span>
              {s.empreinte ? <span className="gm-proc-hash">SHA-256 {s.empreinte}</span> : null}
              {e.moi.signer.includes(s.code) ? (
                <div><Button variant="primary" icon="key-round" disabled={!cleOk} disabledReason={t('proc.suivi.httpsTitre')} onClick={() => setSignature(s.code)}>{t('proc.suivi.signer')}</Button></div>
              ) : null}
            </div>
          ))}
        </section>
      ) : null}
      {e.moi.valider ? (
        <div className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
          <Button variant="primary" icon="check" onClick={() => setGeste('valider')}>{t('proc.suivi.valider')}</Button>
          <Button variant="secondary" icon="rotate-ccw" onClick={() => setGeste('renvoyer')}>{t('proc.suivi.renvoyer')}</Button>
        </div>
      ) : null}
      <div className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
        {e.moi.executer ? <Button variant="primary" icon="check" onClick={() => setGeste('terminer')}>{t('proc.suivi.terminer')}</Button> : null}
        {e.moi.reaffecter ? <Button variant="secondary" icon="user" onClick={() => setGeste('reaffecter')}>{t('proc.suivi.reaffecter')}</Button> : null}
      </div>
      {geste === 'terminer' ? <ConfirmDialog open title={t('proc.suivi.terminer')} description={t(e.signatures.length ? 'proc.suivi.terminerSignatures' : e.validateur ? 'proc.suivi.terminerValidation' : 'proc.suivi.terminerTexte')}
        confirmLabel={t('proc.suivi.terminer')} onClose={() => setGeste(null)} onConfirm={() => { setGeste(null); terminer.mutate(undefined, { onSuccess: ok(t('proc.annonce.terminee')), onError: setEchec }) }} /> : null}
      {geste === 'valider' ? <ConfirmDialog open title={t('proc.suivi.valider')} description={t('proc.suivi.validerTexte')} confirmLabel={t('proc.suivi.valider')} onClose={() => setGeste(null)}
        onConfirm={() => { setGeste(null); valider.mutate({ decision: 'VALIDER' }, { onSuccess: ok(t('proc.annonce.validee')), onError: setEchec }) }} /> : null}
      {geste === 'renvoyer' ? <ConfirmDialog open reason title={t('proc.suivi.renvoyer')} description={t('proc.suivi.renvoyerTexte')} confirmLabel={t('proc.suivi.renvoyer')} onClose={() => setGeste(null)}
        onConfirm={(motif) => { setGeste(null); valider.mutate({ decision: 'RENVOYER', motif }, { onSuccess: ok(t('proc.annonce.renvoyee')), onError: setEchec }) }} /> : null}
      {geste === 'reaffecter' ? <DialogueReaffectation instanceId={i.id} etape={e} enCours={reaffecter.isPending} onFermer={() => setGeste(null)}
        onValider={(v) => reaffecter.mutate(v, { onSuccess: () => { setGeste(null); ok(t('proc.annonce.reaffectee'))() }, onError: (x) => { setGeste(null); setEchec(x) } })} /> : null}
      {signature ? <DialogueSignature instance={i} etape={e} code={signature} onFermer={() => setSignature(null)} onSignee={() => onAnnonce(t('proc.annonce.signee'))} /> : null}
    </aside>
  )
}

/** Dépôt d'un livrable : envoi fragmenté (D-74, finalité PROCESSUS_UNITE, référence : l'instance), puis rattachement à l'étape. */
function DepotLivrable({ instanceId, etape, livrable, onDepose, onEchec }: {
  instanceId: string; etape: string; livrable: EtapeInstance['livrables'][number]; onDepose: (nom: string) => void; onEchec: (e: unknown) => void
}) {
  const { t } = useTranslation()
  const services = useServices()
  const types = useTypesLisibles()
  const rattacher = useActeInstance((s, documentId: string) => s.processus.deposerLivrable(instanceId, etape, livrable.code, documentId))
  const [fichier, setFichier] = useState<File | null>(null)
  const [progression, setProgression] = useState<Progression | null>(null)
  const arret = useRef<AbortController | null>(null)
  useEffect(() => () => arret.current?.abort(), [])
  const deposer = async () => {
    if (!fichier) return
    try {
      arret.current = new AbortController()
      const depot = await services.processus.televerserLivrable(fichier, instanceId, { signal: arret.current.signal, surProgression: setProgression })
      await rattacher.mutateAsync(depot.documentId)
      onDepose(fichier.name)
      setFichier(null)
    } catch (e) {
      if (!(e instanceof DOMException && e.name === 'AbortError')) onEchec(e)
    } finally {
      setProgression(null)
    }
  }
  const pourcent = progression ? Math.floor((progression.envoye / Math.max(progression.total, 1)) * 100) : 0
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
      <FileDropzone name={`livrable-${livrable.code}`} label={t(livrable.depot ? 'proc.suivi.remplacer' : 'proc.suivi.deposer', { livrable: livrable.libelle })} accept={livrable.typesPieces.join(',')}
        help={livrable.typesPieces.length ? t('proc.suivi.types', { types: types(livrable.typesPieces) }) : undefined}
        file={fichier ? { name: fichier.name, size: taille(fichier.size) } : undefined} state={progression ? 'uploading' : fichier ? 'done' : 'idle'} progress={pourcent}
        onFile={setFichier} onRemove={() => setFichier(null)} />
      {progression ? <span className="gm-field-help" aria-live="polite">{t(`proc.depot.${progression.phase}`, { pourcent })}</span> : null}
      {fichier ? <div><Button size="sm" icon="upload" loading={!!progression || rattacher.isPending} onClick={() => void deposer()}>{t('proc.suivi.envoyer', { livrable: livrable.code })}</Button></div> : null}
    </div>
  )
}

/**
 * Réaffectation d'une étape en cours, motif obligatoire : candidats de l'étape (porteurs du profil exigé dans l'unité, puis
 * ailleurs dans le GIMAC), puis l'annuaire.
 */
function DialogueReaffectation({ instanceId, etape, enCours, onFermer, onValider }: {
  instanceId: string; etape: EtapeInstance; enCours: boolean; onFermer: () => void; onValider: (v: { personneId: string; motif: string }) => void
}) {
  const { t } = useTranslation()
  const [personne, setPersonne] = useState('')
  const candidats = useCandidatsEtape(instanceId, etape.code)
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-reaffectation" title={t('proc.suivi.reaffecter')} subject={`${etape.code} · ${etape.libelle}`} description={t('proc.suivi.reaffecterTexte')} reason
      onClose={onFermer} submitLabel={t('proc.suivi.reaffecter')} submitting={enCours} submitDisabled={!personne} submitDisabledReason={t('proc.personne.choisir')}
      onSubmit={(motif) => onValider({ personneId: personne, motif })}>
      <ChoixPersonne name="personne" label={t('proc.suivi.nouvelExecutant')} required value={personne} onChange={setPersonne} profil={candidats.data?.profil ?? etape.aPrendre?.profil}
        candidats={candidats.data} chargement={candidats.isPending} exclure={etape.executant ? [etape.executant.id] : []} />
    </CrudDialog>
  )
}

/** Signature d'une étape : empreinte des livrables portés, clé de sécurité (finalité SIGNATURE_PROCESSUS), acte signé. */
function DialogueSignature({ instance, etape, code, onFermer, onSignee }: { instance: InstanceProcessus; etape: EtapeInstance; code: string; onFermer: () => void; onSignee: () => void }) {
  const { t } = useTranslation()
  const services = useServices()
  const erreur = useErreurProcessus()
  const s = etape.signatures.find((x) => x.code === code)!
  const signer = useActeInstance((x, assertion: string) => x.processus.signer(instance.id, etape.code, code, assertion))
  const [hash, setHash] = useState('')
  const [etat, setEtat] = useState<'idle' | 'waiting' | 'done' | 'error'>('idle')
  const [message, setMessage] = useState<string | undefined>()
  useEffect(() => {
    let actif = true
    void empreinteAttendue(instance, s.porte).then((h) => actif && setHash(h))
    return () => { actif = false }
  }, [instance, s.porte])
  const executer = async () => {
    setEtat('waiting')
    setMessage(undefined)
    try {
      const assertion = await services.reauth.assertion('SIGNATURE_PROCESSUS')
      await signer.mutateAsync(assertion)
      setEtat('done')
      onSignee()
    } catch (e) {
      const r = refusReauth(e)
      setEtat('error')
      setMessage(r ? t(`reauth.${r}`) : erreur(e)?.titre)
    }
  }
  return (
    <SignDialog open title={t('proc.suivi.signerTitre', { signature: s.libelle })} action={s.libelle} hash={hash} state={etat} error={message}
      object={[{ key: t('proc.champ.etape'), value: `${etape.code} · ${etape.libelle}` }, { key: t('proc.signature.porte'), value: s.porte.join(', '), mono: true }]}
      onSign={() => void executer()} onClose={onFermer} onDone={onFermer} />
  )
}
