import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import { Button, DataTable, FilterBar, Tabs, PageHeader } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { ETAPES, type StatutParcours } from '../../../modele/parcours'
import type { FiltreLiaisons } from '../../../services/ports'
import { useLiaisonsParcours } from '../../../hooks/parcours'
import { PERM } from '../../../permissions'
import { colonnesLiaisons, useFormatDate } from './communs'
import { DialogueOuverture } from './DialogueOuverture'

// Étape 3, cadrage (/des/parcours, gabarit G2 ; D-57, D-58) : liste paginée côté serveur des liaisons du
// parcours. Les filtres sont portés par l'adresse (les compteurs du tableau de bord y renvoient) ; « Mes
// projets » restreint le conducteur aux liaisons qui lui sont confiées. Tout chiffre vient du service.

const STATUTS: StatutParcours[] = ['CADREE', 'EN_INTEGRATION', 'EN_TEST', 'CERTIFIEE', 'APPROUVEE_SECURITE', 'EN_PRODUCTION', 'SUSPENDUE', 'CLOSE_SANS_SUITE']
const ETAPES_LIAISON = ETAPES.filter((e) => e !== 'PRISE_CONTACT' && e !== 'ADHESION')
const TYPES = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE']
/** Paramètres de l'adresse qui filtrent la liste ; tout changement ramène à la première page. */
const FILTRES = ['statut', 'type', 'etape', 'bloquees', 'sansConducteur'] as const
const TAILLES = [10, 25, 50, 100]

export default function ParcoursPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const conducteur = aPermission(permissions, PERM.pvEtablir)
  const peutOuvrir = aPermission(permissions, PERM.liaisonOuvrir)
  const mesProjets = conducteur && params.get('mesProjets') === '1'
  const page = Math.max(1, Number(params.get('page')) || 1)
  const taille = TAILLES.includes(Number(params.get('taille'))) ? Number(params.get('taille')) : 25
  const q = params.get('q') ?? ''
  const [saisie, setSaisie] = useState(q)

  // Dernière adresse demandée : le routeur applique l'adresse dans une transition, après la frappe ; une
  // recherche saisie juste après « Réinitialiser » part donc de l'adresse demandée, pas de celle encore affichée.
  const adresse = useRef(params)
  useEffect(() => { adresse.current = params }, [params])
  const modifier = (maj: Record<string, string | null>, garderPage = false) => {
    const suivant = new URLSearchParams(adresse.current)
    Object.entries(maj).forEach(([k, v]) => (v ? suivant.set(k, v) : suivant.delete(k)))
    if (!garderPage) suivant.delete('page')
    adresse.current = suivant
    setParams(suivant, { replace: true })
  }
  // Recherche portée par l'adresse : une navigation (menu, compteur) remet le champ à sa valeur.
  useEffect(() => {
    setSaisie((s) => (s.trim() === q ? s : q))
  }, [q])
  // La recherche libre part au service après une courte pause de saisie.
  useEffect(() => {
    if (saisie.trim() === q) return
    const id = window.setTimeout(() => modifier({ q: saisie.trim() || null }), 300)
    return () => window.clearTimeout(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [saisie])

  const filtre = useMemo<FiltreLiaisons>(
    () => ({
      page, taille, recherche: q || undefined, statut: params.get('statut') || undefined, typeProjet: params.get('type') || undefined, etape: params.get('etape') || undefined,
      bloquees: params.get('bloquees') === '1' || undefined, sansConducteur: params.get('sansConducteur') === '1' || undefined, mesProjets: mesProjets || undefined,
    }),
    [page, taille, q, params, mesProjets],
  )
  const liste = useLiaisonsParcours(filtre)
  const lignes = liste.data?.contenu ?? []

  const libelleEtapes = (v: string) => v.split(',').map((e) => t(`parcours.etape.${e}`)).join(', ')
  const actifs = [
    ...(params.get('statut') ? [{ key: 'statut', label: `${t('parcoursListe.statut')} : ${t(`parcours.statut.${params.get('statut')}`)}` }] : []),
    ...(params.get('type') ? [{ key: 'type', label: `${t('parcoursListe.type')} : ${t(`parcours.typeProjet.${params.get('type')}`)}` }] : []),
    ...(params.get('etape') ? [{ key: 'etape', label: `${t('parcoursListe.etape')} : ${libelleEtapes(params.get('etape') ?? '')}` }] : []),
    ...(params.get('bloquees') === '1' ? [{ key: 'bloquees', label: t('parcoursListe.filtre.bloquees') }] : []),
    ...(params.get('sansConducteur') === '1' ? [{ key: 'sansConducteur', label: t('parcoursListe.filtre.sansConducteur') }] : []),
    ...(q ? [{ key: 'q', label: `${t('parcoursListe.recherche')} : ${q}` }] : []),
  ]
  const filtres = [
    { key: 'statut', label: t('parcoursListe.statut'), value: params.get('statut') ?? '', options: STATUTS.map((s) => ({ value: s, label: t(`parcours.statut.${s}`) })) },
    { key: 'type', label: t('parcoursListe.type'), value: params.get('type') ?? '', options: TYPES.map((s) => ({ value: s, label: t(`parcours.typeProjet.${s}`) })) },
    { key: 'etape', label: t('parcoursListe.etape'), value: params.get('etape') ?? '', options: ETAPES_LIAISON.map((s) => ({ value: s, label: t(`parcours.etape.${s}`) })) },
    { key: 'situation', label: t('parcoursListe.situation'), value: params.get('bloquees') === '1' ? 'bloquees' : params.get('sansConducteur') === '1' ? 'sansConducteur' : '', options: [
      { value: 'bloquees', label: t('parcoursListe.filtre.bloquees') },
      { value: 'sansConducteur', label: t('parcoursListe.filtre.sansConducteur') },
    ] },
  ]
  const surFiltre = (cle: string, v: string) =>
    cle === 'situation' ? modifier({ bloquees: v === 'bloquees' ? '1' : null, sansConducteur: v === 'sansConducteur' ? '1' : null }) : modifier({ [cle]: v || null })
  const retirer = (cle: string) => {
    if (cle === 'q') setSaisie('')
    modifier({ [cle]: null })
  }
  const reinitialiser = () => {
    setSaisie('')
    modifier(Object.fromEntries([...FILTRES, 'q'].map((k) => [k, null])))
  }

  const formatDate = useFormatDate()
  const colonnes = colonnesLiaisons(t, formatDate)

  const vide = actifs.length > 0 || mesProjets
  return (
    <div className="gm-col">
      <PageHeader
        title={t(mesProjets ? 'parcoursListe.titre.mesProjets' : 'parcoursListe.titre')}
        subtitle={t(mesProjets ? 'parcoursListe.intro.mesProjets' : 'parcoursListe.intro')}
        actions={peutOuvrir ? (
          <Button variant="primary" icon="plus" onClick={() => modifier({ formulaire: 'ouvrir' }, true)}>
            {t('parcoursListe.ouvrir')}
          </Button>
        ) : undefined}
      />
      {conducteur ? (
        <Tabs
          items={[{ key: 'toutes', label: t('parcoursListe.onglet.toutes') }, { key: 'mesProjets', label: t('parcoursListe.onglet.mesProjets') }]}
          value={mesProjets ? 'mesProjets' : 'toutes'}
          onChange={(k) => modifier({ mesProjets: k === 'mesProjets' ? '1' : null })}
        />
      ) : null}
      <FilterBar query={saisie} onQuery={setSaisie} filters={filtres} onChange={surFiltre} active={actifs} onRemove={retirer} onReset={reinitialiser} savedLabel={t('parcoursListe.vueEnregistree')} />
      <DataTable
        columns={colonnes}
        rows={lignes}
        rowKey={(l) => l.id}
        total={liste.data?.total}
        page={page}
        pageSize={taille}
        onPage={(p) => modifier({ page: p > 1 ? String(p) : null }, true)}
        onPageSize={(n) => modifier({ taille: n === 25 ? null : String(n) })}
        onRowOpen={(l) => void navigate(`/des/parcours/${l.id}`)}
        state={liste.isPending ? 'loading' : liste.isError ? 'error' : lignes.length ? 'ok' : 'empty'}
        error={liste.error ? { code: liste.error.code, message: liste.error.message, correlationId: liste.error.correlationId || undefined } : undefined}
        onRetry={() => void liste.refetch()}
        emptyText={t(vide ? 'parcoursListe.vide.filtre' : 'parcoursListe.vide')}
        emptyHelp={t(vide ? 'parcoursListe.vide.filtre.aide' : peutOuvrir ? 'parcoursListe.vide.aide.ouvrir' : 'parcoursListe.vide.aide')}
        toolbar={<span className="gm-num">{t('parcoursListe.total', { count: liste.data?.total ?? 0 })}</span>}
      />
      {params.get('formulaire') === 'ouvrir' && peutOuvrir ? <DialogueOuverture onFermer={() => modifier({ formulaire: null }, true)} /> : null}
    </div>
  )
}
