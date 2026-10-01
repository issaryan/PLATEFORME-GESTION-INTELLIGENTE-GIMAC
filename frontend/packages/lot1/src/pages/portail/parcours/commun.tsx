import { useCallback, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { PageHeader, StatusBadge, type PageHeaderUser, type Status } from '@gimac/design-system'
import { aPermission, usePermissions, type ResourceActionLot1 } from '@gimac/socle'
import type { EtatEtape, Fonction, Sla, StatutParcours, TacheActeur } from '../../../modele/parcours'
import type { Services } from '../../../services/ports'
import { useExecuteurActions } from '../../../ressources/actions'

// Briques partagées du portail du participant (qualité Externe, CR §3.4) : tons des états, dates dans la
// langue courante, en-tête d'écran, permissions du jeton et lancement des gestes par l'ExecuteurActions.
// Aucune règle n'est évaluée ici : les actions affichées sont celles que le service permet.

export const PERM = { lire: 'HOM_PORTAL_READ', deposer: 'HOM_PORTAL_SUBMIT', lireCertification: 'CER_PORTAL_READ', signer: 'CER_PORTAL_SIGN' } as const

export const tonStatut: Record<StatutParcours, Status> = {
  CADREE: 'info', EN_INTEGRATION: 'info', EN_TEST: 'info', CERTIFIEE: 'ok', APPROUVEE_SECURITE: 'ok', EN_PRODUCTION: 'ok', SUSPENDUE: 'warn', CLOSE_SANS_SUITE: 'neutral',
}
export const tonSla: Record<Sla, Status> = { DANS_LES_DELAIS: 'ok', ECHEANCE_PROCHE: 'warn', DEPASSE: 'crit' }
export const tonEtape: Record<EtatEtape, Status> = { A_VENIR: 'neutral', EN_COURS: 'info', CLOSE: 'ok', BLOQUEE: 'crit', SANS_OBJET: 'neutral' }

/** Date du service (AAAA-MM-JJ ou horodatage) écrite dans la langue de l'interface. */
export function useDate() {
  const { i18n } = useTranslation()
  return useCallback(
    (iso: string | null | undefined) => {
      if (!iso) return ''
      const d = new Date(iso.length === 10 ? `${iso}T12:00:00Z` : iso)
      return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString(i18n.language, { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Africa/Douala' })
    },
    [i18n.language],
  )
}

/** Un intervenant du GIMAC n'est jamais nommé sur le portail : seule sa fonction est affichée. */
export function useFonction() {
  const { t } = useTranslation()
  return useCallback((f: Fonction | string | null | undefined) => (f ? t(`parcours.fonction.${f}`, { defaultValue: t('pp.fonction.gimac') }) : t('pp.nonDesigne')), [t])
}

export function usePermissionPortail(permission: string) {
  return aPermission(usePermissions(), permission)
}

export function SlaBadge({ sla }: { sla: Sla | null }) {
  const { t } = useTranslation()
  return sla ? <StatusBadge status={tonSla[sla]} label={t(`parcours.sla.${sla}`)} /> : null
}

/** Échéance d'une tâche : la date, puis l'état du délai calculé par le service. */
export function Echeance({ date, sla }: { date: string | null; sla: Sla | null }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  return (
    <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
      <span className="gm-num">{date ? ecrire(date) : t('pp.sansEcheance')}</span>
      <SlaBadge sla={sla} />
    </span>
  )
}

/** En-tête d'un écran du portail (PageHeader du catalogue) : retour, titre, sous-titre, personne connectée et état. */
export function EnTete({ titre, sousTitre, retour, etat, utilisateur }: { titre: string; sousTitre?: ReactNode; retour?: { href: string; libelle: string }; etat?: ReactNode; utilisateur?: PageHeaderUser }) {
  return <PageHeader title={titre} subtitle={sousTitre} back={retour ? { href: retour.href, label: retour.libelle } : undefined} meta={etat} user={utilisateur} />
}

export function Section({ titre, action, description, children }: { titre: string; action?: ReactNode; description?: ReactNode; children: ReactNode }) {
  return (
    <section className="gm-col" aria-label={titre} style={{ gap: 'var(--gm-space-3)' }}>
      <div className="gm-row" style={{ justifyContent: 'space-between' }}>
        <h2 className="gm-type-h3" style={{ margin: 0 }}>{titre}</h2>
        {action}
      </div>
      {description ? <p className="gm-section-desc">{description}</p> : null}
      {children}
    </section>
  )
}

/** Libellé d'une tâche du participant : l'opération du référentiel (clé parcours.op.<rang>). */
export function useLibelleTache() {
  const { t } = useTranslation()
  return useCallback((x: Pick<TacheActeur, 'rang'>) => t(`parcours.op.${x.rang}`, { defaultValue: x.rang }), [t])
}

/** Libellé du geste attendu du participant sur une tâche (bouton d'accès à l'écran d'action). */
export const GESTES: Record<string, { cle: string; icone: 'upload' | 'pen-line' | 'send' | 'file-text' }> = {
  COMMUNIQUER_COORDONNEES: { cle: 'pp.geste.coordonnees', icone: 'send' },
  DEPOSER_LIVRABLE: { cle: 'pp.geste.livrable', icone: 'upload' },
  SIGNER_PROCES_VERBAL: { cle: 'pp.geste.signer', icone: 'pen-line' },
  DEPOSER_ATTESTATION: { cle: 'pp.geste.attestation', icone: 'upload' },
  RENSEIGNER_FICHE_SUIVI: { cle: 'pp.geste.fiche', icone: 'file-text' },
}
/** Permission du jeton exigée par chaque geste. */
export const PERMISSION_GESTE: Record<string, string> = {
  COMMUNIQUER_COORDONNEES: PERM.deposer, DEPOSER_LIVRABLE: PERM.deposer, SIGNER_PROCES_VERBAL: PERM.signer, DEPOSER_ATTESTATION: PERM.deposer, RENSEIGNER_FICHE_SUIVI: PERM.deposer,
}
/** Écran de dépôt d'un livrable (/portail/liaisons/:id/livrables/:op). */
export const lienLivrable = (x: Pick<TacheActeur, 'liaisonId' | 'operationId'>) => `/portail/liaisons/${encodeURIComponent(x.liaisonId)}/livrables/${encodeURIComponent(x.operationId)}`
/**
 * Écran où se réalise le geste d'une tâche : un livrable mène toujours à son écran de dépôt, quel que soit
 * le lien servi ; les autres gestes suivent le lien du service.
 */
export const lienGeste = (x: TacheActeur, geste: string | undefined) => (geste === 'DEPOSER_LIVRABLE' ? lienLivrable(x) : x.lien)
/** Premier geste de la tâche que le service permet et que le jeton autorise. */
export function gesteDe(tache: TacheActeur, permissions: string[] | undefined) {
  return (tache.actionsPermises ?? []).find((a) => GESTES[a] && aPermission(permissions, PERMISSION_GESTE[a]))
}

/**
 * Lance un geste engageant par l'ExecuteurActions (ConfirmDialog, ou SignDialog avec réauthentification
 * FIDO2) ; l'effet passe par le hook de données fourni, jamais par le service directement.
 */
export function useGeste() {
  const executeur = useExecuteurActions()
  return <T,>(action: Omit<ResourceActionLot1<T, Services>, 'run' | 'scope'> & { effet: (row: T, ctx: { assertion?: string; motif?: string; navigate: (to: string) => void }) => Promise<unknown> }, row: T, titre: string) => {
    const { effet, ...reste } = action
    executeur.lancer<T>({ ...reste, scope: 'page', run: ({ row: r, assertion, motif, navigate }) => effet(r, { assertion, motif, navigate }) }, row, titre, 'parcours')
  }
}
