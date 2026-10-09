import { useEffect, useLayoutEffect, useMemo, useRef, useState, type CSSProperties, type DragEvent, type PointerEvent as PointeurReact, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import {
  Avatar, Button, Card, Checkbox, ConfirmDialog, CrudDialog, DataTable, EmptyState, Icon, IconButton, InlineAlert, KeyValueList, Menu, PageHeader, RadioGroup, Select, StatusBadge, Tag, cx,
  type Column, type MenuItem,
} from '@gimac/design-system'
import { aPermission, usePermissions, useLibelleProfil, urlAvatar, type ApiError } from '@gimac/socle'
import { useMessageErreur, type Compte } from '@gimac/compte'
import type { Unite } from '@gimac/lot1'
import { cles, useEffet, useLibellesProfils, useMatricePouvoirs, useTousComptes, useUnitesOrganisation } from '../hooks'
import { Annonce } from '../composants/Annonce'
import type { Retour } from '../composants/actes'
import { Donnees } from '../composants/communs'
import { BadgePortee } from '../composants/profils'
import { DialogueUnite } from '../composants/unites'
import {
  ZOOM, ancetres, arbreMembres, borneZoom, dansLaChaine, deplacementPossible, effectif, nomFichierOrganigramme, normaliser, pouvoirsEffectifs, rechercher, responsableParDefaut,
  type NoeudMembre, type ResultatRecherche,
} from '../composants/organigramme'
import { enArbre, libelleUnite } from '../composants/rattachement'
import { useChemins } from '../services/useServicesConsole'
import type { DemandeAffectation, OptionsOrganigrammePdf, ResultatAffectation } from '../modele/comptes'

// Organigramme (D-86 §3, D-102, Organisation › Organigramme) : organigramme descendant (traits de liaison, cartes au type
// d'unité en couleur discrète, chef avec sa photo ou ses initiales, effectif), branches repliables, zoom, déplacement,
// recherche d'une personne ou d'une unité, plein écran, panneau de l'unité choisie (membres en arbre des n+1) et panneau
// « Pouvoirs » d'un compte. Sur petit écran, l'organigramme redevient un arbre vertical indenté (CSS). Lecture :
// IAM_ACCOUNT_READ ; gestes : IAM_ACCOUNT_EDIT. Affecter des personnes à une unité (POST …/unites/{code}/affectations),
// retirer une personne, changer son unité ou son n+1, déplacer une unité, désigner son chef : par glisser-déposer, par le
// menu d'une carte ou au clavier (vue Liste), toujours avec une confirmation motivée dont le motif est journalisé par
// MS-IAM. Téléchargement de l'organigramme en PDF (composé par MS-IAM, D-102 §4). MS-IAM refait toutes les règles.

const TYPE_GLISSE = 'application/x-gimac-organigramme'
type Glisse = { type: 'unite' | 'compte'; id: string }
type Geste =
  | { type: 'deplacer'; unite: Unite; parent: Unite }
  | { type: 'chef'; unite: Unite; compte: Compte | null }
  | { type: 'nPlus1'; compte: Compte; responsable: Compte }
  | { type: 'rattacher'; compte: Compte; unite: Unite }
  | { type: 'retirer'; compte: Compte; unite: Unite }
  | { type: 'etat'; unite: Unite }
/** Choix au clavier (vue liste et boutons des panneaux) : le geste est complété par une liste déroulante. */
type Choix = { type: 'deplacer'; unite: Unite } | { type: 'chef'; unite: Unite } | { type: 'nPlus1'; compte: Compte } | { type: 'rattacher'; compte: Compte }

/** Valeur d'attribut sûre dans un sélecteur CSS (identifiants et codes). */
const echapper = (v: string) => v.replace(/["\\]/g, '\\$&')

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

export default function OrganigrammePage() {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const navigate = useNavigate()
  const { base } = useChemins()
  const couche = base.startsWith('/console') ? 'console' : 'des'
  const edition = aPermission(usePermissions(), 'IAM_ACCOUNT_EDIT')
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const unites = useUnitesOrganisation()
  const tous = useTousComptes()
  const matrice = useMatricePouvoirs()
  const modifierUnite = useEffet((s, v: { u: Unite; motif?: string }) => s.organisation.modifierUnite(v.u.code, v.u, v.motif), [cles.iam])
  const rattacher = useEffet((s, v: { id: string; unite: string | null; responsableId: string | null; motif?: string }) =>
    s.organisation.rattacher(v.id, { unite: v.unite, responsableId: v.responsableId, motif: v.motif }), [cles.iam])
  const retirer = useEffet((s, v: { code: string; id: string; motif: string }) => s.organisation.retirer(v.code, v.id, v.motif), [cles.iam])
  const [vue, setVue] = useState<'arbre' | 'liste'>('arbre')
  const [choisie, setChoisie] = useState<string | undefined>()
  const [compteChoisi, setCompteChoisi] = useState<string | undefined>()
  const [geste, setGeste] = useState<Geste | null>(null)
  const [choix, setChoix] = useState<Choix | null>(null)
  const [dialogueUnite, setDialogueUnite] = useState<{ unite?: Unite; parent?: string } | null>(null)
  const [affectation, setAffectation] = useState<Unite | null>(null)
  const [pdf, setPdf] = useState<{ racine: string } | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const [annonce, setAnnonce] = useState('')
  const [tire, setTire] = useState<Glisse | null>(null)
  const [cible, setCible] = useState<string | null>(null)
  const glisse = useRef<Glisse | null>(null)
  // D-102 : affichage ergonomique (repli, zoom, déplacement, recherche, plein écran, menu d'une carte).
  const [replies, setReplies] = useState<Set<string>>(new Set())
  const [zoom, setZoom] = useState(1)
  const [pleinEcran, setPleinEcran] = useState(false)
  const [recherche, setRecherche] = useState('')
  const [trouve, setTrouve] = useState<{ unite?: string; compte?: string } | null>(null)
  const [menu, setMenu] = useState<string | null>(null)
  const vueRef = useRef<HTMLDivElement>(null)
  const toileRef = useRef<HTMLDivElement>(null)
  const ajusteUneFois = useRef(false)

  const internes = useMemo(() => (tous.data ?? []).filter((c) => !c.institutionId), [tous.data])
  // Jamais d'identifiant technique pendant le chargement des comptes (recette ADM-08) : « chargement… » à sa place.
  const nom = (id: string | null | undefined) => (!id ? t('console.org.rattachement.aucunResponsable') : tous.isPending ? t('console.organigramme.chargement') : internes.find((c) => c.id === id)?.nomAffiche ?? id)
  const photo = (id: string | null | undefined) => {
    const c = id ? internes.find((x) => x.id === id) : undefined
    return c ? urlAvatar(couche, c.id, c.avatarVersion) : undefined
  }
  const l = useMemo(() => unites.data ?? [], [unites.data])
  const enfantsDe = (parent: string | null) =>
    l.filter((u) => (parent === null ? !u.parent || !l.some((x) => x.code === u.parent) : u.parent === parent)).sort((a, b) => a.libelle.localeCompare(b.libelle, 'fr'))

  // ------------------------------------------------------------ gestes
  const objet = (g: Geste) => {
    switch (g.type) {
      case 'deplacer': return [{ key: t('console.org.unite.libelle'), value: g.unite.libelle }, { key: t('console.organigramme.parentAvantApres'), value: `${l.find((x) => x.code === g.unite.parent)?.libelle ?? t('console.org.unite.sansParent')} → ${g.parent.libelle}` }]
      case 'chef': return [{ key: t('console.org.unite.libelle'), value: g.unite.libelle }, { key: t('console.organigramme.chef'), value: `${nom(g.unite.responsableId)} → ${g.compte?.nomAffiche ?? t('console.org.unite.sansResponsable')}` }]
      case 'nPlus1': return [{ key: t('console.organigramme.compte'), value: g.compte.nomAffiche }, { key: t('console.org.rattachement.responsable'), value: `${nom(g.compte.responsableId)} → ${g.responsable.nomAffiche}` }]
      case 'rattacher': return [{ key: t('console.organigramme.compte'), value: g.compte.nomAffiche }, { key: t('console.org.rattachement.unite'), value: `${l.find((x) => x.code === g.compte.unite)?.libelle ?? t('console.org.rattachement.aucuneUnite')} → ${g.unite.libelle}` }]
      case 'retirer': return [{ key: t('console.organigramme.compte'), value: g.compte.nomAffiche }, { key: t('console.org.rattachement.unite'), value: `${g.unite.libelle} → ${t('console.org.rattachement.aucuneUnite')}` }]
      default: return [{ key: t('console.org.unite.libelle'), value: g.unite.libelle }, { key: t('console.org.unite.code'), value: g.unite.code, mono: true }]
    }
  }
  const titre = (g: Geste) => (g.type === 'etat' ? t(g.unite.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver') : t(`console.organigramme.geste.${g.type}`))
  const executer = async (g: Geste, motif: string) => {
    setGeste(null)
    setRetour(null)
    const m = motif.trim() || undefined
    try {
      if (g.type === 'deplacer') await modifierUnite.mutateAsync({ u: { ...g.unite, parent: g.parent.code }, motif: m })
      else if (g.type === 'chef') await modifierUnite.mutateAsync({ u: { ...g.unite, responsableId: g.compte?.id ?? null }, motif: m })
      else if (g.type === 'etat') await modifierUnite.mutateAsync({ u: { ...g.unite, active: !g.unite.active }, motif: m })
      // Rattachement d'administration : l'unité et le n+1 sont toujours envoyés (D-76 §1.2), avec le motif (D-102).
      else if (g.type === 'nPlus1') await rattacher.mutateAsync({ id: g.compte.id, unite: g.compte.unite ?? null, responsableId: g.responsable.id, motif: m })
      else if (g.type === 'retirer') await retirer.mutateAsync({ code: g.unite.code, id: g.compte.id, motif: motif.trim() })
      else await rattacher.mutateAsync({ id: g.compte.id, unite: g.unite.code, responsableId: g.compte.responsableId ?? null, motif: m })
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

  // ------------------------------------------------------------ affichage : repli, zoom, déplacement, recherche
  const avecSousUnites = l.filter((u) => l.some((x) => x.parent === u.code)).map((u) => u.code)
  const basculer = (u: Unite) => {
    const replie = replies.has(u.code)
    setReplies((r) => {
      const n = new Set(r)
      if (replie) n.delete(u.code)
      else n.add(u.code)
      return n
    })
    setAnnonce(t(replie ? 'console.organigramme.deplier' : 'console.organigramme.replier', { unite: u.libelle }))
  }
  const ajuster = () => {
    const v = vueRef.current
    const c = toileRef.current
    if (!v || !c) return
    const r = (c.firstElementChild ?? c).getBoundingClientRect()
    const largeur = r.width / zoom
    const hauteur = r.height / zoom
    if (!largeur || !hauteur || !v.clientWidth) return setZoom(1)
    setZoom(borneZoom(Math.min(1, (v.clientWidth - 16) / largeur, Math.max(360, v.clientHeight - 16) / hauteur)))
    v.scrollTo?.({ left: 0, top: 0 })
  }
  // Première lecture : l'organigramme tient dans la largeur de la zone quand il la dépasse.
  useLayoutEffect(() => {
    if (ajusteUneFois.current || vue !== 'arbre' || !l.length) return
    ajusteUneFois.current = true
    const v = vueRef.current
    const c = toileRef.current
    // Lisibilité d'abord : jamais en deçà de 75 % à l'ouverture (le reste se parcourt en faisant glisser le fond).
    if (v && c && c.scrollWidth > v.clientWidth && v.clientWidth > 0) {
      const z = borneZoom(Math.max(0.75, (v.clientWidth - 16) / c.scrollWidth))
      setZoom(z)
      // La racine au centre de la zone : la largeur restante se parcourt de part et d'autre.
      requestAnimationFrame(() => { if (vueRef.current) vueRef.current.scrollLeft = (vueRef.current.scrollWidth - vueRef.current.clientWidth) / 2 })
    }
  }, [l.length, vue])
  // Plein écran : l'organigramme est ajusté à la nouvelle taille de la zone.
  const premierPleinEcran = useRef(true)
  useEffect(() => {
    if (premierPleinEcran.current) {
      premierPleinEcran.current = false
      return
    }
    ajuster()
  }, [pleinEcran])
  const zoomer = (z: number) => {
    const n = borneZoom(z)
    setZoom(n)
    setAnnonce(t('console.organigramme.zoom', { valeur: Math.round(n * 100) }))
  }
  // Déplacement : glisser le fond de l'organigramme (pas une carte ni un bouton) fait défiler la zone.
  const deplacement = useRef<{ x: number; y: number; l: number; h: number } | null>(null)
  const surPointeurBas = (e: PointeurReact<HTMLDivElement>) => {
    const v = vueRef.current
    if (!v || e.button !== 0 || (e.target as Element).closest('button, a, input, [draggable="true"], .gm-menu')) return
    deplacement.current = { x: e.clientX, y: e.clientY, l: v.scrollLeft, h: v.scrollTop }
    v.setPointerCapture?.(e.pointerId)
  }
  const surPointeurDeplace = (e: PointeurReact<HTMLDivElement>) => {
    const d = deplacement.current
    const v = vueRef.current
    if (!d || !v) return
    v.scrollLeft = d.l - (e.clientX - d.x)
    v.scrollTop = d.h - (e.clientY - d.y)
  }
  const surPointeurHaut = () => {
    deplacement.current = null
  }
  const resultats = useMemo(() => rechercher(l, internes, recherche), [l, internes, recherche])
  const aller = (r: ResultatRecherche) => {
    setRecherche('')
    const code = r.type === 'unite' ? r.unite.code : r.compte.unite ?? undefined
    if (code) {
      const ouvrir = new Set(ancetres(l, code))
      setReplies((x) => new Set([...x].filter((c) => !ouvrir.has(c))))
      setChoisie(code)
    }
    setVue('arbre')
    setTrouve({ unite: code, compte: r.type === 'personne' ? r.compte.id : undefined })
    setAnnonce(r.type === 'unite'
      ? t('console.organigramme.trouveUnite', { nom: r.unite.libelle })
      : code ? t('console.organigramme.trouvePersonne', { nom: r.compte.nomAffiche, unite: l.find((x) => x.code === code)?.libelle ?? code }) : t('console.organigramme.trouvePersonneSansUnite', { nom: r.compte.nomAffiche }))
  }
  // L'élément trouvé est amené au centre de la zone (carte de l'unité, puis ligne de la personne dans le panneau).
  useEffect(() => {
    if (!trouve) return
    const cibleTrouvee = (trouve.compte && document.querySelector(`.gm-org-panneau [data-compte="${echapper(trouve.compte)}"], .gm-org-sans-unite [data-compte="${echapper(trouve.compte)}"]`))
      || (trouve.unite && document.querySelector(`[data-unite="${echapper(trouve.unite)}"]`))
    if (cibleTrouvee instanceof HTMLElement) {
      cibleTrouvee.scrollIntoView?.({ behavior: 'smooth', block: 'center', inline: 'center' })
      const carte = trouve.unite ? document.querySelector(`[data-unite="${echapper(trouve.unite)}"]`) : null
      if (carte instanceof HTMLElement && carte !== cibleTrouvee) carte.scrollIntoView?.({ behavior: 'smooth', block: 'nearest', inline: 'center' })
    }
  }, [trouve])
  // Plein écran : Échap le quitte ; un clic hors du menu d'une carte le ferme.
  useEffect(() => {
    if (!pleinEcran && !menu) return
    const clavier = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return
      if (menu) setMenu(null)
      else setPleinEcran(false)
    }
    const clic = (e: MouseEvent) => {
      if (menu && !(e.target as Element).closest?.('.gm-org-menu')) setMenu(null)
    }
    document.addEventListener('keydown', clavier)
    document.addEventListener('mousedown', clic)
    return () => {
      document.removeEventListener('keydown', clavier)
      document.removeEventListener('mousedown', clic)
    }
  }, [pleinEcran, menu])

  // ------------------------------------------------------------ rendu de l'organigramme des unités
  const actionsUnite = (u: Unite): MenuItem[] => [
    ...(edition && u.active ? [{ key: 'affecter', label: t('console.organigramme.affecter'), icon: 'user-plus' as const, onClick: () => { setMenu(null); setAffectation(u) } }] : []),
    ...(edition ? [
      { key: 'chef', label: t('console.organigramme.designerChef'), icon: 'user' as const, onClick: () => { setMenu(null); setChoix({ type: 'chef', unite: u }) } },
      { key: 'sous', label: t('console.organigramme.sousUnite'), icon: 'plus' as const, onClick: () => { setMenu(null); setDialogueUnite({ parent: u.code }) } },
      { key: 'renommer', label: t('console.organigramme.renommer'), icon: 'pen-line' as const, onClick: () => { setMenu(null); setDialogueUnite({ unite: u }) } },
      { key: 'deplacer', label: t('console.organigramme.deplacer'), icon: 'route' as const, onClick: () => { setMenu(null); setChoix({ type: 'deplacer', unite: u }) } },
      { key: 'sep', separator: true },
    ] : []),
    { key: 'pdf', label: t('console.organigramme.pdf.branche'), icon: 'file-down' as const, onClick: () => { setMenu(null); setPdf({ racine: u.code }) } },
  ]
  const carteUnite = (u: Unite) => {
    const chef = u.responsableId ? nom(u.responsableId) : t('console.organigramme.sansChef')
    const { 'data-cible': estCible, ...zone } = cibleDe({ type: 'unite', id: u.code }, u.libelle)
    return (
      <button type="button" className={cx('gm-org-unite', !u.active && 'gm-org-unite-inactive', estCible && 'gm-org-cible', tire?.type === 'unite' && tire.id === u.code && 'gm-org-tire', trouve?.unite === u.code && 'gm-org-trouve')}
        aria-current={choisie === u.code ? 'true' : undefined} draggable={edition} data-unite={u.code} data-type={u.type}
        onDragStart={(e) => commencer(e, { type: 'unite', id: u.code }, u.libelle)} onDragEnd={finir} {...zone}
        onClick={() => { setChoisie(u.code); setCompteChoisi(undefined); setRetour(null); setTrouve(null) }}>
        {edition ? <span className="gm-org-poignee" aria-hidden="true"><Icon name="grip-vertical" size={16} /></span> : null}
        <span className="gm-org-unite-corps">
          <span className="gm-org-unite-type">{t(`console.org.type.${u.type}`)} <span className="gm-mono">{u.code}</span></span>
          <span className="gm-org-unite-titre">
            <span className="gm-org-unite-libelle">{u.libelle}</span>
            {!u.active ? <StatusBadge status="neutral" label={t('console.org.inactive')} /> : null}
          </span>
          <span className="gm-org-unite-ligne" aria-busy={tous.isPending || undefined}>
            <Avatar name={u.responsableId ? chef : '?'} src={tous.isPending ? undefined : photo(u.responsableId)} size="sm" decorative className={cx(!u.responsableId && 'gm-org-avatar-vide')} />
            <span className="gm-org-unite-chef">
              <span>{t('console.organigramme.chefDe', { nom: chef })}</span>
              {tous.isPending ? null : <><span className="gm-org-sep"> · </span><span className="gm-org-unite-effectif"><Icon name="users" size={16} />{t('console.organigramme.effectif', { count: effectif(l, internes, u.code) })}</span></>}
            </span>
          </span>
        </span>
      </button>
    )
  }
  const branche = (parent: string | null): ReactNode => {
    const enfants = enfantsDe(parent)
    if (!enfants.length) return null
    return (
      <ul className={parent === null ? 'gm-org-arbre' : undefined} aria-label={parent === null ? t('console.organigramme.arbre') : undefined}>
        {enfants.map((u) => {
          const sous = enfantsDe(u.code)
          const replie = replies.has(u.code)
          return (
            <li key={u.code}>
              <div className={cx('gm-org-noeud', menu === u.code && 'gm-org-noeud-menu')}>
                {carteUnite(u)}
                <span className="gm-org-menu">
                  <IconButton variant="ghost" size="sm" icon="ellipsis-vertical" label={t('console.organigramme.menuUnite', { unite: u.libelle })}
                    aria-haspopup="menu" aria-expanded={menu === u.code} onClick={() => setMenu((m) => (m === u.code ? null : u.code))} />
                  {menu === u.code ? <Menu items={actionsUnite(u)} label={t('console.organigramme.menuUnite', { unite: u.libelle })} /> : null}
                </span>
              </div>
              {sous.length ? (
                <button type="button" className="gm-org-replier" aria-expanded={!replie} aria-label={t(replie ? 'console.organigramme.deplier' : 'console.organigramme.replier', { unite: u.libelle })}
                  title={t('console.organigramme.sousUnites', { count: sous.length })} onClick={() => basculer(u)}>
                  <Icon name={replie ? 'chevron-right' : 'chevron-down'} size={16} />
                  {replie ? <span>{sous.length}</span> : null}
                </button>
              ) : null}
              {!replie ? branche(u.code) : null}
            </li>
          )
        })}
      </ul>
    )
  }

  // ------------------------------------------------------------ membres d'une unité
  const ligneMembre = (c: Compte, u: Unite | undefined) => {
    const { 'data-cible': estCible, ...zone } = cibleDe({ type: 'compte', id: c.id }, c.nomAffiche)
    const profil = c.profils[0] ? libelleProfil(c.profils[0]) : null
    return (
      <div className={cx('gm-org-membre', u?.responsableId === c.id && 'gm-org-membre-chef', !c.actif && 'gm-org-membre-inactif', estCible && 'gm-org-cible', tire?.type === 'compte' && tire.id === c.id && 'gm-org-tire', trouve?.compte === c.id && 'gm-org-trouve')}
        draggable={edition && c.actif} data-compte={c.id} onDragStart={(e) => commencer(e, { type: 'compte', id: c.id }, c.nomAffiche)} onDragEnd={finir} {...zone}>
        {edition && c.actif ? <span className="gm-org-poignee" aria-hidden="true"><Icon name="grip-vertical" size={16} /></span> : null}
        <Avatar name={c.nomAffiche} src={urlAvatar(couche, c.id, c.avatarVersion)} size="sm" decorative />
        <span className="gm-org-membre-nom">
          <span>{c.nomAffiche}{u?.responsableId === c.id ? <> <StatusBadge status="ok" icon={null} label={t('console.organigramme.chef')} /></> : null}{!c.actif ? <> <StatusBadge status="neutral" label={t('console.org.inactive')} /></> : null}</span>
          <span>{[c.fonction ?? c.identifiant, profil].filter(Boolean).join(' · ')}</span>
          <span>{c.responsableId ? t('console.organigramme.nPlus1Externe', { nom: nom(c.responsableId) }) : t('console.organigramme.aucunNPlus1')}</span>
        </span>
        <span className="gm-org-membre-actions">
          <Button size="sm" variant="ghost" icon="scale" aria-label={t('console.organigramme.pouvoirsDe', { nom: c.nomAffiche })} onClick={() => setCompteChoisi(c.id)}>{t('console.organigramme.pouvoirs')}</Button>
          {edition && c.actif ? <Button size="sm" variant="ghost" icon="pen-line" aria-label={t('console.organigramme.nPlus1De', { nom: c.nomAffiche })} onClick={() => setChoix({ type: 'nPlus1', compte: c })}>{t('console.organigramme.nPlus1')}</Button> : null}
          {edition && c.actif ? <Button size="sm" variant="ghost" icon="route" aria-label={t('console.organigramme.uniteDe', { nom: c.nomAffiche })} onClick={() => setChoix({ type: 'rattacher', compte: c })}>{t('console.organigramme.changerUnite')}</Button> : null}
          {edition && u && c.unite === u.code && u.responsableId !== c.id ? (
            <IconButton size="sm" variant="ghost" icon="user-minus" label={t('console.organigramme.retirerDe', { nom: c.nomAffiche })} onClick={() => setGeste({ type: 'retirer', compte: c, unite: u })} />
          ) : null}
        </span>
      </div>
    )
  }
  const arbre = (n: NoeudMembre[], u: Unite | undefined): ReactNode => (
    <ul>{n.map((x) => <li key={x.compte.id}>{ligneMembre(x.compte, u)}{x.enfants.length ? arbre(x.enfants, u) : null}</li>)}</ul>
  )

  // ------------------------------------------------------------ panneaux
  const u = l.find((x) => x.code === choisie)
  const membres = u ? arbreMembres(internes, u.code) : []
  const sansUnite = internes.filter((c) => !c.unite && c.actif)
  const compte = internes.find((c) => c.id === compteChoisi)

  const panneauUnite = u ? (
    <Card title={u.libelle} className="gm-org-panneau">
      <div className="gm-col">
        {edition ? (
      <span className="gm-row gm-org-panneau-actions" style={{ gap: 'var(--gm-space-1)', flexWrap: 'wrap' }}>
        {u.active ? <Button size="sm" variant="primary" icon="user-plus" onClick={() => setAffectation(u)}>{t('console.organigramme.affecter')}</Button> : null}
        <Button size="sm" icon="pen-line" onClick={() => setDialogueUnite({ unite: u })}>{t('console.organigramme.renommer')}</Button>
        <Button size="sm" icon="route" onClick={() => setChoix({ type: 'deplacer', unite: u })}>{t('console.organigramme.deplacer')}</Button>
        <Button size="sm" icon="plus" onClick={() => setDialogueUnite({ parent: u.code })}>{t('console.organigramme.sousUnite')}</Button>
        <Button size="sm" variant={u.active ? 'danger' : 'secondary'} onClick={() => setGeste({ type: 'etat', unite: u })}>{t(u.active ? 'console.org.unite.desactiver' : 'console.org.unite.reactiver')}</Button>
      </span>
        ) : null}
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
                <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                  {u.responsableId ? <Avatar name={nom(u.responsableId)} src={photo(u.responsableId)} size="sm" decorative /> : null}
                  <span><strong>{t('console.organigramme.chef')}</strong> : {u.responsableId ? nom(u.responsableId) : t('console.organigramme.sansChef')}</span>
                </span>
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
          {edition && x.unite.active ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.affecterA', { unite: x.unite.libelle })} onClick={() => setAffectation(x.unite)}>{t('console.organigramme.affecter')}</Button> : null}
          {edition ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.deplacerUnite', { unite: x.unite.libelle })} onClick={() => setChoix({ type: 'deplacer', unite: x.unite })}>{t('console.organigramme.deplacer')}</Button> : null}
          {edition ? <Button size="sm" variant="ghost" aria-label={t('console.organigramme.chefDeUnite', { unite: x.unite.libelle })} onClick={() => setChoix({ type: 'chef', unite: x.unite })}>{t('console.organigramme.designerChef')}</Button> : null}
        </span>
      ),
    },
  ]
  const colonnesComptes: Column<Compte>[] = [
    { key: 'nomAffiche', header: t('console.organigramme.compte'), render: (c) => <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'nowrap' }}><Avatar name={c.nomAffiche} src={urlAvatar(couche, c.id, c.avatarVersion)} size="sm" decorative /><span className="gm-col" style={{ gap: 0 }}><span>{c.nomAffiche}</span><span className="gm-type-small">{c.fonction ?? c.identifiant}</span></span></span> },
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

  // ------------------------------------------------------------ barre d'outils de l'organigramme
  const outils = (
    <div className="gm-org-outils" role="toolbar" aria-label={t('console.organigramme.outils')}>
      <div className="gm-org-recherche">
        <div className="gm-input-wrap">
          <input type="search" className="gm-input" aria-label={t('console.organigramme.recherche')} placeholder={t('console.organigramme.recherche')} value={recherche}
            aria-controls={recherche.trim().length >= 2 ? 'gm-org-resultats' : undefined}
            onChange={(e) => setRecherche(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && resultats[0]) { e.preventDefault(); aller(resultats[0]) }
              if (e.key === 'Escape') setRecherche('')
            }} />
          <span className="gm-input-suffix"><Icon name="search" /></span>
        </div>
        {recherche.trim().length >= 2 ? (
          <ul className="gm-org-resultats" id="gm-org-resultats" aria-label={t('console.organigramme.resultats')}>
            {resultats.length ? resultats.map((r) => (
              <li key={r.type === 'unite' ? `u:${r.unite.code}` : `p:${r.compte.id}`}>
                <button type="button" onClick={() => aller(r)}>
                  {r.type === 'unite'
                    ? <><span className="gm-org-pastille" data-type={r.unite.type} aria-hidden="true" /><span className="gm-org-resultat-texte"><span>{r.unite.libelle}</span><span className="gm-type-small">{t('console.organigramme.resultatUnite')} · {t(`console.org.type.${r.unite.type}`)}</span></span></>
                    : <><Avatar name={r.compte.nomAffiche} src={urlAvatar(couche, r.compte.id, r.compte.avatarVersion)} size="sm" decorative /><span className="gm-org-resultat-texte"><span>{r.compte.nomAffiche}</span><span className="gm-type-small">{l.find((x) => x.code === r.compte.unite)?.libelle ?? t('console.org.rattachement.aucuneUnite')}</span></span></>}
                </button>
              </li>
            )) : <li className="gm-field-help">{t('console.organigramme.aucunResultat')}</li>}
          </ul>
        ) : null}
      </div>
      <span className="gm-row gm-org-outils-groupe gm-org-outils-zoom">
        <IconButton size="sm" variant="secondary" icon="zoom-out" label={t('console.organigramme.zoomMoins')} disabled={zoom <= ZOOM.min} onClick={() => zoomer(zoom - ZOOM.pas)} />
        <output className="gm-org-zoom" aria-label={t('console.organigramme.zoom', { valeur: Math.round(zoom * 100) })}>{Math.round(zoom * 100)} %</output>
        <IconButton size="sm" variant="secondary" icon="zoom-in" label={t('console.organigramme.zoomPlus')} disabled={zoom >= ZOOM.max} onClick={() => zoomer(zoom + ZOOM.pas)} />
        <IconButton size="sm" variant="secondary" icon="scan" label={t('console.organigramme.ajuster')} onClick={ajuster} />
      </span>
      <span className="gm-row gm-org-outils-groupe">
        <IconButton size="sm" variant="secondary" icon="chevrons-down-up" label={t('console.organigramme.toutReplier')} onClick={() => { setReplies(new Set(avecSousUnites)); setAnnonce(t('console.organigramme.toutReplier')) }} />
        <IconButton size="sm" variant="secondary" icon="chevrons-up-down" label={t('console.organigramme.toutDeplier')} onClick={() => { setReplies(new Set()); setAnnonce(t('console.organigramme.toutDeplier')) }} />
        <IconButton size="sm" variant="secondary" icon={pleinEcran ? 'minimize-2' : 'maximize-2'} label={t(pleinEcran ? 'console.organigramme.quitterPleinEcran' : 'console.organigramme.pleinEcran')}
          aria-pressed={pleinEcran} onClick={() => setPleinEcran((x) => !x)} />
      </span>
    </div>
  )

  return (
    <div className="gm-col">
      <PageHeader title={t('console.organigramme.titre')} subtitle={t('console.organigramme.sousTitre')}
        actions={(
          <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
            <span className="gm-row" role="group" aria-label={t('console.organigramme.vue')} style={{ gap: 0 }}>
              {(['arbre', 'liste'] as const).map((v) => (
                <button key={v} type="button" className={cx('gm-btn', 'gm-btn-sm', vue === v ? 'gm-btn-primary' : 'gm-btn-secondary')} aria-pressed={vue === v} onClick={() => setVue(v)}>
                  <Icon name={v === 'arbre' ? 'network' : 'menu'} size={20} />{t(`console.organigramme.vue.${v}`)}
                </button>
              ))}
            </span>
            <Button icon="file-down" onClick={() => setPdf({ racine: '' })}>{t('console.organigramme.pdf.bouton')}</Button>
            {edition ? <Button variant="primary" icon="plus" onClick={() => setDialogueUnite({})}>{t('console.org.unite.nouvelle')}</Button> : null}
          </span>
        )} />
      <Annonce retour={retour} />
      <div className="gm-sr-only" role="status" aria-live="polite">{annonce}</div>
      <Donnees requete={unites} estVide={(x) => x.length === 0} vide={t('console.org.vide')}>
        {() => vue === 'arbre' ? (
          <div className={cx('gm-org', pleinEcran && 'gm-org-plein-ecran')} data-plein-ecran={pleinEcran || undefined}>
            <Card title={t('console.organigramme.arbre')} className="gm-org-carte-arbre">
              <div className="gm-col">
                {outils}
                {edition ? <p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.aideGlisser')}</p> : null}
                <div className="gm-org-vue" ref={vueRef} tabIndex={0} role="region" aria-label={t('console.organigramme.deplacement')}
                  onPointerDown={surPointeurBas} onPointerMove={surPointeurDeplace} onPointerUp={surPointeurHaut} onPointerCancel={surPointeurHaut}>
                  <div className="gm-org-toile" ref={toileRef} style={{ zoom, '--gm-org-zoom': zoom } as CSSProperties}>
                    {branche(null)}
                  </div>
                </div>
                {sansUnite.length ? (
                  <section aria-labelledby="gm-org-sans-unite" className="gm-col gm-org-sans-unite" style={{ gap: 'var(--gm-space-1)' }}>
                    <h3 id="gm-org-sans-unite" className="gm-type-h3" style={{ margin: 0 }}>{t('console.organigramme.sansUnite')}</h3>
                    <div className="gm-org-membres">{arbre(sansUnite.map((c) => ({ compte: c, enfants: [] })), undefined)}</div>
                  </section>
                ) : null}
              </div>
            </Card>
            <div className="gm-col gm-org-panneaux">
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
          confirmLabel={titre(geste)} danger={(geste.type === 'etat' && geste.unite.active) || geste.type === 'retirer'} reason object={objet(geste)}
          extra={<p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.motifAide')}</p>}
          onConfirm={(motif) => void executer(geste, motif ?? '')} onClose={() => setGeste(null)} />
      ) : null}
      {choix ? <DialogueChoix choix={choix} unites={l} comptes={internes} onFermer={() => setChoix(null)} onChoisi={(g) => { setChoix(null); setGeste(g) }} /> : null}
      {affectation ? (
        <DialogueAffectation unite={affectation} unites={l} comptes={internes} couche={couche} onFermer={() => setAffectation(null)} onFait={(r) => {
          setAffectation(null)
          setChoisie(affectation.code)
          setRetour({ niveau: 'info', message: { texte: t('console.organigramme.affectation.fait', { count: r.length, unite: affectation.libelle }) } })
        }} />
      ) : null}
      {pdf ? <DialoguePdf racine={pdf.racine} unites={l} onFermer={() => setPdf(null)} onFait={(n) => { setPdf(null); setRetour({ niveau: 'info', message: { texte: t('console.organigramme.pdf.fait', { nom: n }) } }) }} /> : null}
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

/**
 * Affecter des personnes à une unité (D-102 §2) : recherche dans l'annuaire des comptes internes actifs, sélection
 * multiple (50 au plus), n+1 proposé automatiquement, choisi ou aucun, motif obligatoire ; refus d'ensemble restitué
 * personne par personne.
 */
function DialogueAffectation({ unite, unites, comptes, couche, onFermer, onFait }: {
  unite: Unite; unites: Unite[]; comptes: Compte[]; couche: 'console' | 'des'; onFermer: () => void; onFait: (r: ResultatAffectation[]) => void
}) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const affecter = useEffet((s, v: { code: string; d: DemandeAffectation }) => s.organisation.affecter(v.code, v.d), [cles.iam])
  const [q, setQ] = useState('')
  const [choisis, setChoisis] = useState<string[]>([])
  const [mode, setMode] = useState<'auto' | 'choisi' | 'aucun'>('auto')
  const [n1, setN1] = useState('')
  const [erreur, setErreur] = useState<{ texte: string; correlationId?: string; refus: { id: string; code: string }[] } | null>(null)
  const actifs = comptes.filter((c) => c.actif && !c.institutionId)
  const n = normaliser(q)
  const visibles = actifs
    .filter((c) => !n || normaliser(`${c.nomAffiche} ${c.identifiant} ${c.fonction ?? ''}`).includes(n))
    .sort((a, b) => Number(a.unite === unite.code) - Number(b.unite === unite.code) || a.nomAffiche.localeCompare(b.nomAffiche, 'fr'))
  const propose = responsableParDefaut(unites, null, unite.code)
  const basculer = (id: string) => setChoisis((x) => (x.includes(id) ? x.filter((y) => y !== id) : x.length >= 50 ? x : [...x, id]))
  const envoyer = async (motif: string) => {
    setErreur(null)
    const d: DemandeAffectation = { comptes: choisis, motif, ...(mode === 'auto' ? {} : { responsableId: mode === 'aucun' ? null : n1 }) }
    try {
      onFait(await affecter.mutateAsync({ code: unite.code, d }))
    } catch (e) {
      const m = message(e)
      const refus = ((e as ApiError).details as { refus?: { id: string; code: string }[] } | undefined)?.refus ?? []
      setErreur({ texte: m.texte, correlationId: m.correlationId, refus })
    }
  }
  const nomDe = (id: string) => comptes.find((c) => c.id === id)?.nomAffiche ?? id
  return (
    <CrudDialog context="edit" size="lg" id="gm-dlg-affectation" title={t('console.organigramme.affecter')} subject={`${t(`console.org.type.${unite.type}`)} · ${unite.libelle}`}
      description={t('console.organigramme.affectation.texte')} onClose={onFermer} reason dirty={choisis.length > 0}
      submitLabel={t('console.organigramme.affectation.valider', { count: choisis.length })} submitIcon="user-plus" submitting={affecter.isPending}
      submitDisabled={!choisis.length || (mode === 'choisi' && !n1)} submitDisabledReason={!choisis.length ? t('console.organigramme.affectation.choisir') : t('console.organigramme.choisirCible')}
      error={erreur ? { title: erreur.texte, correlationId: erreur.correlationId, text: erreur.refus.length ? (
        <><span>{t('console.organigramme.affectation.refus')}</span><ul className="gm-org-refus">{erreur.refus.map((r) => <li key={r.id}><strong>{nomDe(r.id)}</strong> : {t(`erreur.${r.code}`, { defaultValue: r.code })}</li>)}</ul></>
      ) : undefined } : undefined}
      onSubmit={(motif) => void envoyer(motif)}>
      <div className="gm-col">
        <div className="gm-input-wrap">
          <input type="search" className="gm-input" aria-label={t('console.organigramme.affectation.recherche')} placeholder={t('console.organigramme.affectation.recherche')} value={q} onChange={(e) => setQ(e.target.value)} />
          <span className="gm-input-suffix"><Icon name="search" /></span>
        </div>
        <div className="gm-row" style={{ justifyContent: 'space-between' }}>
          <strong>{t('console.organigramme.affectation.personnes')}</strong>
          <span className="gm-field-help" aria-live="polite">{t('console.organigramme.affectation.selection', { count: choisis.length })}{choisis.length >= 50 ? ` · ${t('console.organigramme.affectation.max')}` : ''}</span>
        </div>
        <div className="gm-org-candidats" role="group" aria-label={t('console.organigramme.affectation.personnes')}>
          {visibles.length ? visibles.map((c) => {
            const membre = c.unite === unite.code
            return (
              <label key={c.id} className={cx('gm-org-candidat', choisis.includes(c.id) && 'gm-org-candidat-choisi', membre && 'gm-org-candidat-membre')}>
                <input type="checkbox" checked={choisis.includes(c.id)} disabled={membre || (!choisis.includes(c.id) && choisis.length >= 50)} onChange={() => basculer(c.id)} />
                <Avatar name={c.nomAffiche} src={urlAvatar(couche, c.id, c.avatarVersion)} size="sm" decorative />
                <span className="gm-org-candidat-texte">
                  <span><strong>{c.nomAffiche}</strong> <span className="gm-mono gm-type-small">{c.identifiant}</span></span>
                  <span className="gm-type-small">{[c.fonction, membre ? t('console.organigramme.affectation.dejaMembre') : t('console.organigramme.affectation.uniteActuelle', { unite: libelleUnite(unites, c.unite) || t('console.organigramme.affectation.sansUnite') })].filter(Boolean).join(' · ')}</span>
                </span>
              </label>
            )
          }) : <p className="gm-field-help" style={{ margin: 0 }}>{t('console.organigramme.affectation.aucun')}</p>}
        </div>
        <RadioGroup name="nPlus1" label={t('console.organigramme.affectation.nPlus1')} value={mode} onChange={(v) => setMode(v as typeof mode)}
          options={[
            { value: 'auto', label: t('console.organigramme.affectation.nPlus1.auto') },
            { value: 'choisi', label: t('console.organigramme.affectation.nPlus1.choisi') },
            { value: 'aucun', label: t('console.organigramme.affectation.nPlus1.aucun') },
          ]}
          help={mode === 'auto' ? (propose ? t('console.organigramme.affectation.nPlus1.propose', { nom: comptes.find((c) => c.id === propose.id)?.nomAffiche ?? propose.id, unite: propose.unite.libelle }) : t('console.organigramme.affectation.nPlus1.proposeAucun')) : undefined} />
        {mode === 'choisi' ? (
          <Select name="responsable" label={t('console.organigramme.affectation.responsable')} required value={n1} onChange={setN1}
            options={[{ value: '', label: t('console.organigramme.choisirCible') }, ...actifs.map((c) => ({ value: c.id, label: `${c.nomAffiche}${c.fonction ? ` · ${c.fonction}` : ''}` }))]} />
        ) : null}
      </div>
    </CrudDialog>
  )
}

/** Télécharger l'organigramme en PDF (D-102 §4) : périmètre, papier, orientation, annexe des membres. */
function DialoguePdf({ racine, unites, onFermer, onFait }: { racine: string; unites: Unite[]; onFermer: () => void; onFait: (nom: string) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const telecharger = useEffet((s, o: OptionsOrganigrammePdf) => s.organisation.organigrammePdf(o), [])
  const [perimetre, setPerimetre] = useState(racine)
  const [format, setFormat] = useState<'A3' | 'A4'>('A3')
  const [orientation, setOrientation] = useState<'paysage' | 'portrait'>('paysage')
  const [membres, setMembres] = useState(false)
  const [erreur, setErreur] = useState<{ title: string; correlationId?: string } | null>(null)
  const envoyer = async () => {
    setErreur(null)
    try {
      const blob = await telecharger.mutateAsync({ racine: perimetre || null, format, orientation, membres })
      const nom = nomFichierOrganigramme()
      enregistrer(blob, nom)
      onFait(nom)
    } catch (e) {
      const m = message(e)
      setErreur({ title: m.texte, correlationId: m.correlationId })
    }
  }
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-organigramme-pdf" title={t('console.organigramme.pdf.titre')} description={t('console.organigramme.pdf.texte')} onClose={onFermer}
      submitLabel={t('console.organigramme.pdf.telecharger')} submitIcon="download" submitting={telecharger.isPending} error={erreur ?? undefined} onSubmit={() => void envoyer()}>
      <div className="gm-col">
        {import.meta.env.VITE_SERVICES === 'simule' ? <InlineAlert level="info">{t('console.organigramme.pdf.demo')}</InlineAlert> : null}
        <Select name="perimetre" label={t('console.organigramme.pdf.perimetre')} value={perimetre} onChange={setPerimetre}
          options={[{ value: '', label: t('console.organigramme.pdf.tout') }, ...enArbre(unites.filter((u) => u.active)).map(({ unite: u, profondeur }) => ({ value: u.code, label: `${'  '.repeat(profondeur)}${u.libelle}` }))]} />
        <RadioGroup name="format" label={t('console.organigramme.pdf.format')} value={format} onChange={(v) => setFormat(v as 'A3' | 'A4')} options={[{ value: 'A3', label: 'A3' }, { value: 'A4', label: 'A4' }]} />
        <RadioGroup name="orientation" label={t('console.organigramme.pdf.orientation')} value={orientation} onChange={(v) => setOrientation(v as 'paysage' | 'portrait')}
          options={[{ value: 'paysage', label: t('console.organigramme.pdf.paysage') }, { value: 'portrait', label: t('console.organigramme.pdf.portrait') }]} />
        <Checkbox name="membres" label={t('console.organigramme.pdf.membres')} checked={membres} onChange={setMembres} />
      </div>
    </CrudDialog>
  )
}
