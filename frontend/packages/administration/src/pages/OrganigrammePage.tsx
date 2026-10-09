import { useMemo, useRef, useState, type DragEvent, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Card, ConfirmDialog, CrudDialog, DataTable, EmptyState, Icon, KeyValueList, PageHeader, Select, StatusBadge, Tag, cx, type Column } from '@gimac/design-system'
import { aPermission, usePermissions, useLibelleProfil } from '@gimac/socle'
import { useMessageErreur, type Compte } from '@gimac/compte'
import type { Unite } from '@gimac/lot1'
import { cles, useEffet, useLibellesProfils, useMatricePouvoirs, useTousComptes, useUnitesOrganisation } from '../hooks'
import { Annonce } from '../composants/Annonce'
import type { Retour } from '../composants/actes'
import { Donnees } from '../composants/communs'
import { BadgePortee } from '../composants/profils'
import { DialogueUnite } from '../composants/unites'
import { arbreMembres, dansLaChaine, deplacementPossible, effectif, pouvoirsEffectifs, type NoeudMembre } from '../composants/organigramme'
import { enArbre, libelleUnite } from '../composants/rattachement'
import { useChemins } from '../services/useServicesConsole'

// Organigramme (D-86 §3, Organisation › Organigramme) : arbre des unités (chef et effectif sur chaque carte), membres
// d'une unité en arbre des n+1, panneau « Pouvoirs » d'un compte (profils, portée effective de chaque action de la
// matrice et personnes couvertes). Lecture : IAM_ACCOUNT_READ ; gestes : IAM_ACCOUNT_EDIT, sur les routes existantes
// (unités D-76/D-81, rattachement d'administration PUT /comptes/{id}/rattachement, qui reçoit toujours unité et n+1).
// Glisser une unité sur une autre la déplace ; glisser un compte sur un autre en fait son n+1, sur une unité l'y
// rattache, sur la case « Chef d'unité » le désigne chef. Chaque geste passe par une fenêtre de confirmation motivée ;
// la vue liste offre les mêmes gestes au clavier. MS-IAM refait toutes les règles (ORG_CYCLE, ORG_UNITE_INCONNUE…).

const TYPE_GLISSE = 'application/x-gimac-organigramme'
type Glisse = { type: 'unite' | 'compte'; id: string }
type Geste =
  | { type: 'deplacer'; unite: Unite; parent: Unite }
  | { type: 'chef'; unite: Unite; compte: Compte | null }
  | { type: 'nPlus1'; compte: Compte; responsable: Compte }
  | { type: 'rattacher'; compte: Compte; unite: Unite }
  | { type: 'etat'; unite: Unite }
/** Choix au clavier (vue liste et boutons des panneaux) : le geste est complété par une liste déroulante. */
type Choix = { type: 'deplacer'; unite: Unite } | { type: 'chef'; unite: Unite } | { type: 'nPlus1'; compte: Compte } | { type: 'rattacher'; compte: Compte }

export default function OrganigrammePage() {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const navigate = useNavigate()
  const { base } = useChemins()
  const edition = aPermission(usePermissions(), 'IAM_ACCOUNT_EDIT')
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const unites = useUnitesOrganisation()
  const tous = useTousComptes()
  const matrice = useMatricePouvoirs()
  const modifierUnite = useEffet((s, u: Unite) => s.organisation.modifierUnite(u.code, u), [cles.iam])
  const rattacher = useEffet((s, v: { id: string; unite: string | null; responsableId: string | null }) => s.organisation.rattacher(v.id, { unite: v.unite, responsableId: v.responsableId }), [cles.iam])
  const [vue, setVue] = useState<'arbre' | 'liste'>('arbre')
  const [choisie, setChoisie] = useState<string | undefined>()
  const [compteChoisi, setCompteChoisi] = useState<string | undefined>()
  const [geste, setGeste] = useState<Geste | null>(null)
  const [choix, setChoix] = useState<Choix | null>(null)
  const [dialogueUnite, setDialogueUnite] = useState<{ unite?: Unite; parent?: string } | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const [annonce, setAnnonce] = useState('')
  const [tire, setTire] = useState<Glisse | null>(null)
  const [cible, setCible] = useState<string | null>(null)
  const glisse = useRef<Glisse | null>(null)

  const internes = useMemo(() => (tous.data ?? []).filter((c) => !c.institutionId), [tous.data])
  // Jamais d'identifiant technique pendant le chargement des comptes (recette ADM-08) : « chargement… » à sa place.
  const nom = (id: string | null | undefined) => (!id ? t('console.org.rattachement.aucunResponsable') : tous.isPending ? t('console.organigramme.chargement') : internes.find((c) => c.id === id)?.nomAffiche ?? id)
  const l = unites.data ?? []

  // ------------------------------------------------------------ gestes
  const objet = (g: Geste) => {
    switch (g.type) {
      case 'deplacer': return [{ key: t('console.org.unite.libelle'), value: g.unite.libelle }, { key: t('console.organigramme.parentAvantApres'), value: `${l.find((x) => x.code === g.unite.parent)?.libelle ?? t('console.org.unite.sansParent')} → ${g.parent.libelle}` }]
      case 'chef': return [{ key: t('console.org.unite.libelle'), value: g.unite.libelle }, { key: t('console.organigramme.chef'), value: `${nom(g.unite.responsableId)} → ${g.compte?.nomAffiche ?? t('console.org.unite.sansResponsable')}` }]
      case 'nPlus1': return [{ key: t('console.organigramme.compte'), value: g.compte.nomAffiche }, { key: t('console.org.rattachement.responsable'), value: `${nom(g.compte.responsableId)} → ${g.responsable.nomAffiche}` }]
      case 'rattacher': return [{ key: t('console.organigramme.compte'), value: g.compte.nomAffiche }, { key: t('console.org.rattachement.unite'), value: `${l.find((x) => x.code === g.compte.unite)?.libelle ?? t('console.org.rattachement.aucuneUnite')} → ${g.unite.libelle}` }]
      default: return [{ key: t('console.org.unite.libelle'), value: g.unite.libelle }, { key: t('console.org.unite.code'), value: g.unite.code, mono: true }]
    }
  }
  const titre = (g: Geste) => (g.type === 'etat' ? t(g.unite.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver') : t(`console.organigramme.geste.${g.type}`))
  const executer = async (g: Geste) => {
    setGeste(null)
    setRetour(null)
    try {
      if (g.type === 'deplacer') await modifierUnite.mutateAsync({ ...g.unite, parent: g.parent.code })
      else if (g.type === 'chef') await modifierUnite.mutateAsync({ ...g.unite, responsableId: g.compte?.id ?? null })
      else if (g.type === 'etat') await modifierUnite.mutateAsync({ ...g.unite, active: !g.unite.active })
      // Rattachement d'administration : l'unité et le n+1 sont toujours envoyés (D-76 §1.2).
      else if (g.type === 'nPlus1') await rattacher.mutateAsync({ id: g.compte.id, unite: g.compte.unite ?? null, responsableId: g.responsable.id })
      else await rattacher.mutateAsync({ id: g.compte.id, unite: g.unite.code, responsableId: g.compte.responsableId ?? null })
      const texte = g.type === 'etat' ? t(g.unite.active ? 'console.org.unite.desactivee' : 'console.org.unite.reactivee', { libelle: g.unite.libelle }) : t(`console.organigramme.fait.${g.type}`)
      setRetour({ niveau: 'info', message: { texte } })
    } catch (e) {
      setRetour({ niveau: 'crit', message: message(e) })
    }
  }

  // ------------------------------------------------------------ glisser-déposer (HTML natif)
  const commencer = (e: DragEvent, g: Glisse, libelle: string) => {
    glisse.current = g
    setTire(g)
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData(TYPE_GLISSE, JSON.stringify(g))
    e.dataTransfer.setData('text/plain', libelle)
    setAnnonce(t(g.type === 'unite' ? 'console.organigramme.glisse.unite' : 'console.organigramme.glisse.compte', { nom: libelle }))
  }
  const finir = () => {
    glisse.current = null
    setTire(null)
    setCible(null)
  }
  /** Geste que produirait le dépôt de l'élément saisi sur la cible, ou null s'il n'est pas permis. */
  const gesteSur = (g: Glisse | null, sur: { type: 'unite' | 'compte' | 'chef'; id: string }): Geste | null => {
    if (!g || !edition) return null
    const unite = l.find((u) => u.code === sur.id)
    if (g.type === 'unite') {
      const u = l.find((x) => x.code === g.id)
      return u && unite && sur.type === 'unite' && deplacementPossible(l, u.code, unite.code) ? { type: 'deplacer', unite: u, parent: unite } : null
    }
    const c = internes.find((x) => x.id === g.id)
    if (!c) return null
    if (sur.type === 'unite') return unite && unite.active && c.unite !== unite.code ? { type: 'rattacher', compte: c, unite } : null
    if (sur.type === 'chef') return unite && c.actif && unite.responsableId !== c.id ? { type: 'chef', unite, compte: c } : null
    const r = internes.find((x) => x.id === sur.id)
    return r && r.actif && c.responsableId !== r.id && !dansLaChaine(internes, c.id, r.id) ? { type: 'nPlus1', compte: c, responsable: r } : null
  }
  const cibleDe = (sur: { type: 'unite' | 'compte' | 'chef'; id: string }, libelle: string) => ({
    onDragOver: (e: DragEvent) => {
      if (!gesteSur(glisse.current, sur)) return
      e.preventDefault()
      e.dataTransfer.dropEffect = 'move'
      if (cible !== `${sur.type}:${sur.id}`) {
        setCible(`${sur.type}:${sur.id}`)
        setAnnonce(t('console.organigramme.glisse.cible', { nom: libelle }))
      }
    },
    onDragLeave: () => setCible((x) => (x === `${sur.type}:${sur.id}` ? null : x)),
    onDrop: (e: DragEvent) => {
      e.preventDefault()
      let g = glisse.current
      try {
        g = g ?? (JSON.parse(e.dataTransfer.getData(TYPE_GLISSE)) as Glisse)
      } catch {
        g = null
      }
      const x = gesteSur(g, sur)
      finir()
      if (x) setGeste(x)
    },
    'data-cible': cible === `${sur.type}:${sur.id}` ? 'oui' : undefined,
  })

  // ------------------------------------------------------------ rendu de l'arbre des unités
  const carteUnite = (u: Unite) => {
    const chef = u.responsableId ? nom(u.responsableId) : t('console.organigramme.sansChef')
    const { 'data-cible': estCible, ...zone } = cibleDe({ type: 'unite', id: u.code }, u.libelle)
    return (
      <button type="button" className={cx('gm-org-unite', !u.active && 'gm-org-unite-inactive', estCible && 'gm-org-cible', tire?.type === 'unite' && tire.id === u.code && 'gm-org-tire')}
        aria-current={choisie === u.code ? 'true' : undefined} draggable={edition} data-unite={u.code}
        onDragStart={(e) => commencer(e, { type: 'unite', id: u.code }, u.libelle)} onDragEnd={finir} {...zone}
        onClick={() => { setChoisie(u.code); setCompteChoisi(undefined); setRetour(null) }}>
        {edition ? <span className="gm-org-poignee" aria-hidden="true"><Icon name="grip-vertical" size={16} /></span> : null}
        <span className="gm-org-unite-corps">
          <span className="gm-org-unite-titre">
            <span>{u.libelle}</span>
            <span className="gm-mono gm-type-small">{u.code}</span>
            <StatusBadge status="neutral" icon={null} label={t(`console.org.type.${u.type}`)} />
            {!u.active ? <StatusBadge status="neutral" label={t('console.org.inactive')} /> : null}
          </span>
          <span className="gm-org-unite-ligne" aria-busy={tous.isPending || undefined}>
            {t('console.organigramme.chefDe', { nom: chef })}{tous.isPending ? null : <> · {t('console.organigramme.effectif', { count: effectif(l, internes, u.code) })}</>}
          </span>
        </span>
      </button>
    )
  }
  const branche = (parent: string | null): ReactNode => {
    const enfants = l.filter((u) => (parent === null ? !u.parent || !l.some((x) => x.code === u.parent) : u.parent === parent)).sort((a, b) => a.libelle.localeCompare(b.libelle, 'fr'))
    if (!enfants.length) return null
    return (
      <ul className={parent === null ? 'gm-org-arbre' : undefined} aria-label={parent === null ? t('console.organigramme.arbre') : undefined}>
        {enfants.map((u) => <li key={u.code}>{carteUnite(u)}{branche(u.code)}</li>)}
      </ul>
    )
  }

  // ------------------------------------------------------------ membres d'une unité
  const ligneMembre = (c: Compte, u: Unite | undefined, externe?: string) => {
    const { 'data-cible': estCible, ...zone } = cibleDe({ type: 'compte', id: c.id }, c.nomAffiche)
    return (
      <div className={cx('gm-org-membre', u?.responsableId === c.id && 'gm-org-membre-chef', !c.actif && 'gm-org-membre-inactif', estCible && 'gm-org-cible', tire?.type === 'compte' && tire.id === c.id && 'gm-org-tire')}
        draggable={edition && c.actif} data-compte={c.id} onDragStart={(e) => commencer(e, { type: 'compte', id: c.id }, c.nomAffiche)} onDragEnd={finir} {...zone}>
        {edition && c.actif ? <span className="gm-org-poignee" aria-hidden="true"><Icon name="grip-vertical" size={16} /></span> : null}
        <span className="gm-org-membre-nom">
          <span>{c.nomAffiche}{u?.responsableId === c.id ? <> <StatusBadge status="ok" icon={null} label={t('console.organigramme.chef')} /></> : null}{!c.actif ? <> <StatusBadge status="neutral" label={t('console.org.inactive')} /></> : null}</span>
          <span>{c.fonction ?? c.identifiant}{externe ? ` · ${externe}` : ''}</span>
        </span>
        <Button size="sm" variant="ghost" icon="scale" aria-label={t('console.organigramme.pouvoirsDe', { nom: c.nomAffiche })} onClick={() => setCompteChoisi(c.id)}>{t('console.organigramme.pouvoirs')}</Button>
        {edition ? <Button size="sm" variant="ghost" icon="pen-line" aria-label={t('console.organigramme.nPlus1De', { nom: c.nomAffiche })} onClick={() => setChoix({ type: 'nPlus1', compte: c })}>{t('console.organigramme.nPlus1')}</Button> : null}
      </div>
    )
  }
  const arbre = (n: NoeudMembre[], u: Unite | undefined): ReactNode => (
    <ul>{n.map((x) => <li key={x.compte.id}>{ligneMembre(x.compte, u, x.compte.responsableId && !internes.some((m) => m.id === x.compte.responsableId && m.unite === x.compte.unite) ? t('console.organigramme.nPlus1Externe', { nom: nom(x.compte.responsableId) }) : undefined)}{x.enfants.length ? arbre(x.enfants, u) : null}</li>)}</ul>
  )

  // ------------------------------------------------------------ panneaux
  const u = l.find((x) => x.code === choisie)
  const membres = u ? arbreMembres(internes, u.code) : []
  const sansUnite = internes.filter((c) => !c.unite && c.actif)
  const compte = internes.find((c) => c.id === compteChoisi)

  const panneauUnite = u ? (
    <Card title={u.libelle} actions={edition ? (
      <span className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>
        <Button size="sm" icon="pen-line" onClick={() => setDialogueUnite({ unite: u })}>{t('console.organigramme.renommer')}</Button>
        <Button size="sm" icon="route" onClick={() => setChoix({ type: 'deplacer', unite: u })}>{t('console.organigramme.deplacer')}</Button>
        <Button size="sm" icon="plus" onClick={() => setDialogueUnite({ parent: u.code })}>{t('console.organigramme.sousUnite')}</Button>
        <Button size="sm" variant={u.active ? 'danger' : 'secondary'} onClick={() => setGeste({ type: 'etat', unite: u })}>{t(u.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver')}</Button>
      </span>
    ) : undefined}>
      <div className="gm-col">
        <KeyValueList items={[
          { key: t('console.org.unite.code'), value: u.code, mono: true },
          { key: t('console.org.unite.type'), value: t(`console.org.type.${u.type}`) },
          { key: t('console.org.unite.parent'), value: libelleUnite(l, u.parent) || t('console.org.unite.sansParent') },
          { key: t('console.organigramme.effectifTitre'), value: t('console.organigramme.effectif', { count: effectif(l, internes, u.code) }) },
        ]} />
        {(() => {
          const { 'data-cible': estCible, ...zone } = cibleDe({ type: 'chef', id: u.code }, t('console.organigramme.chef'))
          return (
            <div className={cx('gm-org-zone', estCible && 'gm-org-cible')} {...zone}>
              <div className="gm-row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
                <span><strong>{t('console.organigramme.chef')}</strong> : {u.responsableId ? nom(u.responsableId) : t('console.organigramme.sansChef')}</span>
                {edition ? <Button size="sm" icon="user" onClick={() => setChoix({ type: 'chef', unite: u })}>{t('console.organigramme.designerChef')}</Button> : null}
              </div>
              {edition ? <span className="gm-field-help">{t('console.organigramme.zoneChef')}</span> : null}
            </div>
          )
        })()}
        <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('console.organigramme.membres')}</h3>
        {membres.length ? <div className="gm-org-membres" aria-label={t('console.organigramme.membresDe', { unite: u.libelle })} role="group">{arbre(membres, u)}</div>
          : <p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.aucunMembre')}</p>}
      </div>
    </Card>
  ) : <Card title={t('console.org.detail')}><EmptyState icon="network" title={t('console.organigramme.choisir')} text={t('console.organigramme.choisirTexte')} /></Card>

  const panneauPouvoirs = compte ? (
    <Card title={t('console.organigramme.pouvoirsTitre', { nom: compte.nomAffiche })} actions={<Button size="sm" variant="ghost" icon="x" aria-label={t('console.organigramme.fermerPouvoirs')} onClick={() => setCompteChoisi(undefined)} />}>
      <div className="gm-col" role="region" aria-label={t('console.organigramme.pouvoirsTitre', { nom: compte.nomAffiche })}>
        <div className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>
          <strong>{t('console.organigramme.profils')}</strong>
          {compte.profils.length ? compte.profils.map((p) => (
            <Button key={p} size="sm" variant="ghost" icon="scale" aria-label={t('console.organigramme.ligneMatrice', { profil: libelleProfil(p) })}
              onClick={() => navigate(`${base}/matrice-pouvoirs?profil=${encodeURIComponent(p)}`)}>{libelleProfil(p)}</Button>
          )) : <span>{t('console.organigramme.aucunProfil')}</span>}
        </div>
        <p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.pouvoirsTexte')}</p>
        <div className="gm-org-portee">
          {pouvoirsEffectifs(compte, internes, l, matrice.data?.profils ?? []).map((x) => (
            <PorteeAction key={x.action} libelle={t(`console.matrice.action.${x.action}`)} portee={<BadgePortee portee={x.portee} />} personnes={x.personnes} />
          ))}
        </div>
      </div>
    </Card>
  ) : null

  // ------------------------------------------------------------ vue liste (clavier)
  const colonnesUnites: Column<{ unite: Unite; profondeur: number } & Record<string, unknown>>[] = [
    { key: 'libelle', header: t('console.org.unite.libelle'), render: (x) => <span style={{ paddingLeft: `calc(${x.profondeur} * var(--gm-space-4))` }}>{x.unite.libelle} <span className="gm-mono gm-type-small">{x.unite.code}</span></span> },
    { key: 'type', header: t('console.org.unite.type'), render: (x) => t(`console.org.type.${x.unite.type}`) },
    { key: 'chef', header: t('console.organigramme.chef'), render: (x) => (x.unite.responsableId ? nom(x.unite.responsableId) : t('console.organigramme.sansChef')) },
    { key: 'effectif', header: t('console.organigramme.effectifTitre'), type: 'number', render: (x) => effectif(l, internes, x.unite.code) },
    { key: 'etat', header: t('console.org.unite.etat'), render: (x) => <StatusBadge status={x.unite.active ? 'ok' : 'neutral'} label={t(x.unite.active ? 'console.org.active' : 'console.org.inactive')} /> },
    {
      key: 'actions', header: '', render: (x) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', justifyContent: 'flex-end', flexWrap: 'nowrap' }}>
          <Button size="sm" variant="ghost" aria-label={t('console.organigramme.ouvrirUnite', { unite: x.unite.libelle })} onClick={() => { setChoisie(x.unite.code); setVue('arbre') }}>{t('console.organigramme.ouvrir')}</Button>
          {edition ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.deplacerUnite', { unite: x.unite.libelle })} onClick={() => setChoix({ type: 'deplacer', unite: x.unite })}>{t('console.organigramme.deplacer')}</Button> : null}
          {edition ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.chefDeUnite', { unite: x.unite.libelle })} onClick={() => setChoix({ type: 'chef', unite: x.unite })}>{t('console.organigramme.designerChef')}</Button> : null}
        </span>
      ),
    },
  ]
  const colonnesComptes: Column<Compte>[] = [
    { key: 'nomAffiche', header: t('console.organigramme.compte'), render: (c) => <span className="gm-col" style={{ gap: 0 }}><span>{c.nomAffiche}</span><span className="gm-type-small">{c.fonction ?? c.identifiant}</span></span> },
    { key: 'unite', header: t('console.org.rattachement.unite'), render: (c) => libelleUnite(l, c.unite) || t('console.org.rattachement.aucuneUnite') },
    { key: 'responsableId', header: t('console.org.rattachement.responsable'), render: (c) => nom(c.responsableId) },
    { key: 'profils', header: t('console.organigramme.profils'), render: (c) => <span className="gm-row" style={{ flexWrap: 'wrap', gap: 2 }}>{c.profils.map((p) => <Tag key={p}>{libelleProfil(p)}</Tag>)}</span> },
    {
      key: 'actions', header: '', render: (c) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)', justifyContent: 'flex-end', flexWrap: 'nowrap' }}>
          <Button size="sm" variant="ghost" aria-label={t('console.organigramme.pouvoirsDe', { nom: c.nomAffiche })} onClick={() => setCompteChoisi(c.id)}>{t('console.organigramme.pouvoirs')}</Button>
          {edition && c.actif ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.nPlus1De', { nom: c.nomAffiche })} onClick={() => setChoix({ type: 'nPlus1', compte: c })}>{t('console.organigramme.nPlus1')}</Button> : null}
          {edition && c.actif ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.uniteDe', { nom: c.nomAffiche })} onClick={() => setChoix({ type: 'rattacher', compte: c })}>{t('console.organigramme.changerUnite')}</Button> : null}
        </span>
      ),
    },
  ]

  return (
    <div className="gm-col">
      <PageHeader title={t('console.organigramme.titre')} subtitle={t('console.organigramme.sousTitre')}
        actions={(
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
            <span className="gm-row" role="group" aria-label={t('console.organigramme.vue')} style={{ gap: 0 }}>
              {(['arbre', 'liste'] as const).map((v) => (
                <button key={v} type="button" className={cx('gm-btn', 'gm-btn-sm', vue === v ? 'gm-btn-primary' : 'gm-btn-secondary')} aria-pressed={vue === v} onClick={() => setVue(v)}>
                  <Icon name={v === 'arbre' ? 'network' : 'menu'} size={20} />{t(`console.organigramme.vue.${v}`)}
                </button>
              ))}
            </span>
            {edition ? <Button variant="primary" icon="plus" onClick={() => setDialogueUnite({})}>{t('console.org.unite.nouvelle')}</Button> : null}
          </span>
        )} />
      <Annonce retour={retour} />
      <div className="gm-sr-only" role="status" aria-live="polite">{annonce}</div>
      <Donnees requete={unites} estVide={(x) => x.length === 0} vide={t('console.org.vide')}>
        {() => vue === 'arbre' ? (
          <div className="gm-org">
            <Card title={t('console.organigramme.arbre')}>
              <div className="gm-col">
                {edition ? <p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.aideGlisser')}</p> : null}
                {branche(null)}
                {sansUnite.length ? (
                  <section aria-labelledby="gm-org-sans-unite" className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
                    <h3 id="gm-org-sans-unite" className="gm-type-h3" style={{ margin: 0 }}>{t('console.organigramme.sansUnite')}</h3>
                    <div className="gm-org-membres">{arbre(sansUnite.map((c) => ({ compte: c, enfants: [] })), undefined)}</div>
                  </section>
                ) : null}
              </div>
            </Card>
            <div className="gm-col">
              {panneauUnite}
              {panneauPouvoirs}
            </div>
          </div>
        ) : (
          <div className="gm-col">
            <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('console.organigramme.arbre')}</h2>
            <DataTable columns={colonnesUnites} rows={enArbre(l) as ({ unite: Unite; profondeur: number } & Record<string, unknown>)[]} rowKey={(x) => x.unite.code} density="compact" pagination={false} state="ok" />
            <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('console.organigramme.comptes')}</h2>
            <DataTable columns={colonnesComptes} rows={internes} rowKey={(c) => c.id} density="compact" pageSize={25} state={tous.isPending ? 'loading' : internes.length ? 'ok' : 'empty'} emptyText={t('console.organigramme.aucunMembre')} />
            {panneauPouvoirs}
          </div>
        )}
      </Donnees>
      {geste ? (
        <ConfirmDialog open title={titre(geste)} description={t(geste.type === 'etat' ? (geste.unite.active ? 'console.org.unite.desactiverTexte' : 'console.org.unite.reactiverTexte') : `console.organigramme.texte.${geste.type}`)}
          confirmLabel={titre(geste)} danger={geste.type === 'etat' && geste.unite.active} reason object={objet(geste)}
          extra={<p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.motifAide')}</p>}
          onConfirm={() => void executer(geste)} onClose={() => setGeste(null)} />
      ) : null}
      {choix ? <DialogueChoix choix={choix} unites={l} comptes={internes} onFermer={() => setChoix(null)} onChoisi={(g) => { setChoix(null); setGeste(g) }} /> : null}
      {dialogueUnite ? (
        <DialogueUnite unite={dialogueUnite.unite} parentInitial={dialogueUnite.parent} unites={l} onFermer={() => setDialogueUnite(null)} onFait={(x) => {
          setDialogueUnite(null)
          setChoisie(x.code)
          setRetour({ niveau: 'info', message: { texte: t(dialogueUnite.unite ? 'console.org.unite.modifiee' : 'console.org.unite.creee', { libelle: x.libelle }) } })
        }} />
      ) : null}
    </div>
  )
}

/** Portée d'une action et personnes couvertes, dépliables. */
function PorteeAction({ libelle, portee, personnes }: { libelle: string; portee: ReactNode; personnes: Compte[] }) {
  const { t } = useTranslation()
  return (
    <>
      <span>{libelle}</span>
      <span>{portee}</span>
      {personnes.length ? (
        <details style={{ gridColumn: '1 / -1', marginTop: -4 }}>
          <summary className="gm-field-help">{t('console.organigramme.couvertes', { count: personnes.length })}</summary>
          <span className="gm-row" style={{ flexWrap: 'wrap', gap: 2 }}>{personnes.map((p) => <Tag key={p.id}>{p.nomAffiche}</Tag>)}</span>
        </details>
      ) : null}
    </>
  )
}

/** Geste au clavier : la cible est choisie dans une liste, puis confirmée avec un motif comme un dépôt. */
function DialogueChoix({ choix, unites, comptes, onFermer, onChoisi }: { choix: Choix; unites: Unite[]; comptes: Compte[]; onFermer: () => void; onChoisi: (g: Geste) => void }) {
  const { t } = useTranslation()
  const [valeur, setValeur] = useState('')
  const options = (() => {
    switch (choix.type) {
      case 'deplacer': return enArbre(unites).filter(({ unite: u }) => deplacementPossible(unites, choix.unite.code, u.code)).map(({ unite: u, profondeur }) => ({ value: u.code, label: `${'  '.repeat(profondeur)}${u.libelle}` }))
      case 'rattacher': return enArbre(unites.filter((u) => u.active && u.code !== choix.compte.unite)).map(({ unite: u, profondeur }) => ({ value: u.code, label: `${'  '.repeat(profondeur)}${u.libelle}` }))
      case 'chef': return comptes.filter((c) => c.actif && c.id !== choix.unite.responsableId).map((c) => ({ value: c.id, label: `${c.nomAffiche}${c.fonction ? ` · ${c.fonction}` : ''}` }))
      default: return comptes.filter((c) => c.actif && c.id !== choix.compte.responsableId && !dansLaChaine(comptes, choix.compte.id, c.id)).map((c) => ({ value: c.id, label: `${c.nomAffiche}${c.fonction ? ` · ${c.fonction}` : ''}` }))
    }
  })()
  const sujet = 'unite' in choix ? choix.unite.libelle : choix.compte.nomAffiche
  const geste = (): Geste | null => {
    if (choix.type === 'deplacer') { const p = unites.find((u) => u.code === valeur); return p ? { type: 'deplacer', unite: choix.unite, parent: p } : null }
    if (choix.type === 'rattacher') { const u = unites.find((x) => x.code === valeur); return u ? { type: 'rattacher', compte: choix.compte, unite: u } : null }
    const c = comptes.find((x) => x.id === valeur)
    if (!c) return null
    return choix.type === 'chef' ? { type: 'chef', unite: choix.unite, compte: c } : { type: 'nPlus1', compte: choix.compte, responsable: c }
  }
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-organigramme" title={t(`console.organigramme.geste.${choix.type}`)} subject={sujet} description={t(`console.organigramme.texte.${choix.type}`)}
      onClose={onFermer} submitLabel={t('console.organigramme.continuer')} submitIcon="arrow-right" submitDisabled={!valeur} submitDisabledReason={t('console.organigramme.choisirCible')}
      onSubmit={() => { const g = geste(); if (g) onChoisi(g) }}>
      <Select name="cible" label={t(`console.organigramme.cible.${choix.type}`)} required value={valeur} onChange={setValeur}
        options={[{ value: '', label: t('console.organigramme.choisirCible') }, ...options]} />
    </CrudDialog>
  )
}
