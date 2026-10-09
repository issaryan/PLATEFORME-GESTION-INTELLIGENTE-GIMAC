import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate, useSearchParams } from 'react-router'
import {
  Button, Card, DataTable, EmptyState, InlineAlert, KeyValueList, Link, PageHeader, Skeleton, StatusBadge, Tabs, TreeView, type Column, type TreeNode,
} from '@gimac/design-system'
import type { MembreEquipe, Unite } from '../../../modele/pilotage'
import { useSession } from '@gimac/socle'
import { couvre, couvreUnites, rangPortee, type MesPouvoirs, type Subordonne } from '../../../modele/pouvoirs'
import { ajouterJours, aujourdhui } from '../../../modele/jours'
import { useEquipe, useUnites } from '../../../hooks/pilotage'
import { usePouvoirs } from '../../../hooks/pouvoirs'
import { useRetour } from '../parcours/communs'
import { etatListe } from '../liaisons/communs'
import { BadgeRapport, NavigationPeriode, jourDemande, useDates, useErreur, useLibelleUnite } from './communs'
import { MesDemandes } from './demandes'
import { DialogueDemandeCompte, DialogueDemandeProfils, DialogueDemandeStatut, DialogueRattachement } from './dialoguesEquipe'

// Mon équipe (D-76, /des/equipe, PIL_TEAM_READ ; D-81 §7) : trois onglets.
// - « Activité du jour » : les comptes dont l'utilisateur figure dans la chaîne des responsables (MS-IAM), avec,
//   pour le jour choisi, leurs activités, leur rapport du jour, leurs tâches ouvertes et en retard, et leur
//   absence ; accès à leur agenda, leur journée et leurs rapports (compteurs de MS-PILOTAGE).
// - « Organisation » : arbre des subordonnés et des unités dirigées (GET pouvoirs/moi) ; sur chaque personne, les
//   gestes que la matrice des pouvoirs ouvre (confier une tâche, lire agenda, journée et rapports, modifier le
//   rattachement, demander un profil, une désactivation ou une réactivation).
// - « Mes demandes » (/des/equipe/demandes) : demandes adressées à l'administration, état, décision, annulation.
// « Demander un compte » (COMPTE_DEMANDER) ouvre le formulaire en fenêtre modale.

type Vue = 'jour' | 'organisation' | 'demandes'
type Dialogue = { compte: true; unite?: string } | { profils: Subordonne } | { statut: Subordonne } | { rattachement: Subordonne } | null

function ActiviteDuJour() {
  const { t } = useTranslation()
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const [params, setParams] = useSearchParams()
  const jour = jourDemande(params.get('jour'))
  const equipe = useEquipe(jour)
  const lignes = equipe.data ?? []
  const aller = (j: string | undefined) => setParams(j ? { jour: j } : {})
  const colonnes: Column<MembreEquipe>[] = [
    {
      key: 'collaborateur', header: t('act.equipe.collaborateur'), render: (m) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <strong>{m.collaborateur.nomAffiche}</strong>
          <span className="gm-field-help">{[m.collaborateur.fonction, libelleUnite(m.collaborateur.unite)].filter(Boolean).join(' · ')}</span>
          {m.collaborateur.niveau > 1 ? <span className="gm-field-help">{t('act.equipe.niveau', { niveau: m.collaborateur.niveau })}</span> : null}
        </span>
      ),
    },
    {
      key: 'activites', header: t('act.equipe.activites'), render: (m) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <span>{t('act.equipe.faites', { count: m.activites.faites, prevues: m.activites.prevues })}</span>
          {m.activites.enCours ? <span className="gm-field-help">{t('act.equipe.enCours', { count: m.activites.enCours })}</span> : null}
        </span>
      ),
    },
    { key: 'rapport', header: t('act.equipe.rapport'), render: (m) => <BadgeRapport etat={m.rapportDuJour} /> },
    {
      key: 'taches', header: t('act.equipe.taches'), render: (m) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
          <span>{m.tachesOuvertes}</span>
          {m.tachesEnRetard ? <StatusBadge status="crit" icon="clock" label={t('act.equipe.enRetard', { count: m.tachesEnRetard })} /> : null}
        </span>
      ),
    },
    { key: 'presence', header: t('act.equipe.presence'), render: (m) => <StatusBadge status={m.absent ? 'warn' : 'ok'} label={t(m.absent ? 'act.equipe.absent' : 'act.equipe.present')} /> },
    {
      key: 'acces', header: t('act.equipe.acces'), render: (m) => (
        // QUO-14 : liens côte à côte (renvoyés à la ligne si besoin), sans déborder de la ligne du tableau.
        <span className="gm-row gm-equipe-acces" style={{ gap: 'var(--gm-space-1) var(--gm-space-3)', flexWrap: 'wrap', padding: 'var(--gm-space-1) 0' }}>
          <Link href={`/des/activite?jour=${jour}&collaborateur=${encodeURIComponent(m.collaborateur.id)}`}>{t('act.equipe.journee')}</Link>
          <Link href={`/des/agenda?jour=${jour}&collaborateur=${encodeURIComponent(m.collaborateur.id)}`}>{t('act.equipe.agenda')}</Link>
          <Link href={`/des/rapports?vue=recus&auteur=${encodeURIComponent(m.collaborateur.id)}`}>{t('act.equipe.rapports')}</Link>
        </span>
      ),
    },
  ]
  return (
    <>
      <NavigationPeriode
        libelle={dates.jourLong(jour)}
        precedent={t('act.jourPrecedent')}
        suivant={t('act.jourSuivant')}
        aujourdhuiLibelle={t('act.aujourdhui')}
        onPrecedent={() => aller(ajouterJours(jour, -1))}
        onSuivant={() => aller(ajouterJours(jour, 1))}
        onAujourdhui={jour !== aujourdhui() ? () => aller(undefined) : undefined}
      />
      <DataTable columns={colonnes} rows={lignes} rowKey={(m) => m.collaborateur.id} emptyText={t('act.equipe.vide')} emptyHelp={t('act.equipe.videAide')}
        {...etatListe(equipe, lignes)} onRetry={() => void equipe.refetch()} />
    </>
  )
}

/** Arbre de la hiérarchie : subordonnés par leur n+1, à partir des directs. */
function arbrePersonnes(l: Subordonne[], prefixe: string, t: (k: string) => string): TreeNode[] {
  const noeud = (s: Subordonne, profondeur: number): TreeNode => ({
    key: `${prefixe}${s.id}`, label: s.nomAffiche, icon: 'user',
    badge: s.actif ? undefined : { status: 'neutral', label: t('pvr.desactive') },
    children: profondeur > 10 ? [] : l.filter((x) => x.niveau >= 1 && x.responsableId === s.id).map((x) => noeud(x, profondeur + 1)),
  })
  return l.filter((s) => s.niveau === 1).map((s) => noeud(s, 1))
}

function Organisation({ pouvoirs, ouvrir }: { pouvoirs: MesPouvoirs; ouvrir: (d: Dialogue) => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const libelleUnite = useLibelleUnite()
  const unites = useUnites()
  const moi = useSession()?.sujet
  const [choix, setChoix] = useState<string | undefined>()
  const catalogue = unites.data ?? []
  const dirigees = pouvoirs.unitesDirigees
  const noeudUnite = (code: string): TreeNode => ({
    key: `u:${code}`, label: libelleUnite(code), code, icon: 'landmark',
    count: pouvoirs.subordonnes.filter((s) => s.unite === code).length,
    children: [
      ...catalogue.filter((u: Unite) => u.parent === code && dirigees.includes(u.code)).map((u) => noeudUnite(u.code)),
      ...pouvoirs.subordonnes.filter((s) => s.unite === code).map((s): TreeNode => ({ key: `m:${s.id}`, label: s.nomAffiche, icon: 'user' })),
    ],
  })
  const racines = dirigees.filter((c) => !dirigees.includes(catalogue.find((u) => u.code === c)?.parent ?? ''))
  const hierarchie = arbrePersonnes(pouvoirs.subordonnes, 'p:', t)
  const id = choix?.startsWith('p:') || choix?.startsWith('m:') ? choix.slice(2) : undefined
  const personne = pouvoirs.subordonnes.find((s) => s.id === id)
  const unite = choix?.startsWith('u:') ? choix.slice(2) : undefined
  const a = (action: Parameters<typeof couvre>[1]) => !!personne && couvre(pouvoirs, action, personne)
  const jour = aujourdhui()
  const lien = (chemin: string) => `${chemin}${chemin.includes('?') ? '&' : '?'}collaborateur=${encodeURIComponent(personne!.id)}`
  const gestes = personne ? [
    a('TACHE_CONFIER') && personne.actif ? <Button key="confier" size="sm" variant="primary" icon="plus" onClick={() => navigate(`/des/taches?confier=${encodeURIComponent(personne.id)}`)}>{t('pvr.geste.confier')}</Button> : null,
    a('ACTIVITE_LIRE') ? <Link key="journee" href={lien(`/des/activite?jour=${jour}`)}>{t('act.equipe.journee')}</Link> : null,
    a('ACTIVITE_LIRE') ? <Link key="agenda" href={lien(`/des/agenda?jour=${jour}`)}>{t('act.equipe.agenda')}</Link> : null,
    a('RAPPORT_VISER') ? <Link key="rapports" href={`/des/rapports?vue=recus&auteur=${encodeURIComponent(personne.id)}`}>{t('act.equipe.rapports')}</Link> : null,
    a('RATTACHEMENT_MODIFIER') ? <Button key="rattachement" size="sm" icon="pen-line" onClick={() => ouvrir({ rattachement: personne })}>{t('pvr.geste.rattachement')}</Button> : null,
    a('PROFIL_DEMANDER') ? <Button key="profils" size="sm" icon="shield-check" onClick={() => ouvrir({ profils: personne })}>{t('pvr.geste.profils')}</Button> : null,
    a('STATUT_DEMANDER') ? (
      <Button key="statut" size="sm" variant={personne.actif ? 'danger' : 'secondary'} onClick={() => ouvrir({ statut: personne })}>{t(personne.actif ? 'pvr.geste.desactivation' : 'pvr.geste.reactivation')}</Button>
    ) : null,
  ].filter(Boolean) : []
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-4)', flexWrap: 'wrap' }}>
      <div className="gm-col" style={{ flex: '1 1 320px', minWidth: 0 }}>
        <Card title={t('pvr.hierarchie')}>
          {hierarchie.length ? <TreeView label={t('pvr.hierarchie')} nodes={hierarchie} expanded={pouvoirs.subordonnes.map((s) => `p:${s.id}`)} selected={choix} onSelect={setChoix} />
            : <EmptyState icon="users" title={t('act.equipe.vide')} text={t('act.equipe.videAide')} />}
        </Card>
        {racines.length ? (
          <Card title={t('pvr.unitesDirigees')}>
            <TreeView label={t('pvr.unitesDirigees')} nodes={racines.map(noeudUnite)} expanded={dirigees.map((c) => `u:${c}`)} selected={choix} onSelect={setChoix} />
          </Card>
        ) : null}
      </div>
      <div style={{ flex: '1 1 320px', minWidth: 0 }}>
        {personne ? (
          <Card title={personne.nomAffiche}>
            <div className="gm-col">
              <KeyValueList items={[
                { key: t('pvr.fonction'), value: personne.fonction ?? t('pvr.aucun') },
                { key: t('pvr.compte.unite'), value: personne.unite ? libelleUnite(personne.unite) : t('pvr.aucun') },
                { key: t('pvr.compte.responsable'), value: personne.responsableId === moi ? t('pvr.moiMeme') : pouvoirs.subordonnes.find((s) => s.id === personne.responsableId)?.nomAffiche ?? personne.responsableId ?? t('pvr.aucun') },
                { key: t('pvr.lien'), value: personne.niveau === 0 ? t('pvr.lien.unite') : personne.niveau === 1 ? t('pvr.lien.direct') : t('act.equipe.niveau', { niveau: personne.niveau }) },
                { key: t('pvr.etat'), value: <StatusBadge status={personne.actif ? 'ok' : 'neutral'} label={t(personne.actif ? 'pvr.actif' : 'pvr.desactive')} /> },
              ]} />
              <div role="group" aria-label={t('pvr.gestes')} className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap', alignItems: 'center' }}>
                {gestes.length ? gestes : <span className="gm-field-help">{t('pvr.aucunGeste')}</span>}
              </div>
            </div>
          </Card>
        ) : unite ? (
          <Card title={libelleUnite(unite)}>
            <div className="gm-col">
              <KeyValueList items={[
                { key: t('pvr.code'), value: unite, mono: true },
                { key: t('pvr.membres'), value: String(pouvoirs.subordonnes.filter((s) => s.unite === unite).length) },
              ]} />
              <div role="group" aria-label={t('pvr.gestes')} className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
                {couvreUnites(pouvoirs, 'TACHE_CONFIER') ? <Button size="sm" variant="primary" icon="plus" onClick={() => navigate(`/des/taches?unite=${encodeURIComponent(unite)}`)}>{t('pvr.geste.confierUnite')}</Button> : null}
                {rangPortee(pouvoirs.actions.COMPTE_DEMANDER) > 0 ? <Button size="sm" icon="user" onClick={() => ouvrir({ compte: true, unite })}>{t('pvr.geste.compteUnite')}</Button> : null}
              </div>
            </div>
          </Card>
        ) : (
          <Card title={t('pvr.selection')}><EmptyState icon="users" title={t('pvr.choisir')} text={t('pvr.choisirTexte')} /></Card>
        )}
      </div>
    </div>
  )
}

export default function EquipePage() {
  const { t } = useTranslation()
  const erreur = useErreur()
  const ici = useLocation()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const pouvoirs = usePouvoirs()
  const [dialogue, setDialogue] = useState<Dialogue>(null)
  const [retour, annoncer] = useRetour()
  const surDemandes = ici.pathname.endsWith('/demandes')
  const vue: Vue = surDemandes ? 'demandes' : params.get('vue') === 'organisation' ? 'organisation' : 'jour'
  const choisir = (k: string) => {
    if (k === 'demandes') navigate('/des/equipe/demandes')
    else navigate(k === 'organisation' ? '/des/equipe?vue=organisation' : '/des/equipe')
  }
  const fait = (m: string) => {
    setDialogue(null)
    annoncer(m)
  }
  const p = pouvoirs.data
  return (
    <div className="gm-col">
      <PageHeader
        title={t('act.equipe.titre')}
        subtitle={t('act.equipe.sousTitre')}
        actions={p && rangPortee(p.actions.COMPTE_DEMANDER) > 0 ? <Button variant="primary" icon="plus" onClick={() => setDialogue({ compte: true })}>{t('pvr.compte.demander')}</Button> : undefined}
      />
      <Tabs items={(['jour', 'organisation', 'demandes'] as const).map((k) => ({ key: k, label: t(`pvr.vue.${k}`) }))} value={vue} onChange={choisir} />
      {retour}
      <div role="tabpanel" aria-label={t(`pvr.vue.${vue}`)} className="gm-col">
        {vue === 'jour' ? <ActiviteDuJour /> : null}
        {vue === 'organisation' ? (
          pouvoirs.isError ? <InlineAlert level="crit" correlationId={pouvoirs.error.correlationId || undefined}>{erreur(pouvoirs.error)?.titre}</InlineAlert>
            : !p ? <Skeleton height={240} /> : <Organisation pouvoirs={p} ouvrir={setDialogue} />
        ) : null}
        {vue === 'demandes' ? (
          <MesDemandes ouverte={params.get('demande')} annoncer={annoncer} ouvrir={(id) => setParams(id ? { demande: id } : {})} />
        ) : null}
      </div>
      {p && dialogue && 'compte' in dialogue ? <DialogueDemandeCompte pouvoirs={p} unite={dialogue.unite} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {p && dialogue && 'profils' in dialogue ? <DialogueDemandeProfils pouvoirs={p} personne={dialogue.profils} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'statut' in dialogue ? <DialogueDemandeStatut personne={dialogue.statut} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {p && dialogue && 'rattachement' in dialogue ? <DialogueRattachement pouvoirs={p} personne={dialogue.rattachement} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
    </div>
  )
}
