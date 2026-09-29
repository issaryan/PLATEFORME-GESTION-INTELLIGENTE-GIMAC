import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Card, EmptyState, Link, LinkList, PageHeader, PageSection, Skeleton, StatGrid, StatusBadge, Tag, type StatCardProps } from '@gimac/design-system'
import { aPermission, usePermissions, useSession, type GroupeNavigation } from '@gimac/socle'
import type { Liaison } from '../../modele/homologation'
import { useIndicateursDdrm, useInstitutions } from '../../hooks/participant'
import { useLiaisons } from '../../hooks/liaisons'
import { etatInstitution, etatLiaison } from '../../dictionnaires'
import { NAVIGATION_DES } from '../../navigation'
import { mesProjetsParDefaut } from './liaisons/communs'
import { fonctionsDe } from '../../permissions'
import { SectionActeur } from './tableau/SectionActeur'
import { SectionCockpit } from './tableau/SectionCockpit'

// DES-01 Tableau de bord (/des/tableau-de-bord, gabarit G1 ; D-58, D-61) : chaque acteur voit son
// tableau de bord, composé des seules sections que la matrice des rôles de son jeton ouvre. Les
// compteurs viennent des services ; l'interface n'en calcule aucun. Composition du catalogue (D-63) :
// PageHeader, PageSection, StatGrid, LinkList.

type Tuile = StatCardProps & { key: string }

/** Liaisons d'une liste, en liens vers leur fiche (état et phase à droite). */
function useLiensLiaisons() {
  const { t } = useTranslation()
  return (ls: Liaison[]) =>
    ls.map((l) => ({
      key: l.id,
      href: `/des/liaisons/${l.id}`,
      label: `${l.institutionNom} (${t(`typeProjet.${l.typeProjet}`)})`,
      meta: t(`phase.${l.phaseCourante}`, { defaultValue: l.phaseCourante }),
      status: etatLiaison[l.etat]?.status ?? 'neutral',
      statusLabel: t(etatLiaison[l.etat]?.label ?? l.etat),
    }))
}

/** Chargé et chef de la DDRM : étapes 1 et 2 du parcours (prise de contact, adhésion). */
function SectionDdrm({ approbateur, instructeur }: { approbateur: boolean; instructeur: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const indicateurs = useIndicateursDdrm()
  const mesDossiers = useInstitutions({ page: 1, taille: 5, mesDossiers: true, statut: ['CANDIDAT'] }, instructeur)
  const i = indicateurs.data
  const vers = (statut: string) => () => void navigate(`/des/institutions?statut=${statut}`)
  const tuiles: Tuile[] = i
    ? [
        ...(approbateur
          ? [{ key: 'propositions', label: t('tableau.ddrm.propositions'), value: i.propositionsEnAttente, status: i.propositionsEnAttente > 0 ? 'warn' as const : 'ok' as const,
              statusLabel: t(i.propositionsEnAttente > 0 ? 'tableau.aTrancher' : 'tableau.aJour'), onClick: vers('CANDIDAT') }]
          : []),
        { key: 'candidats', label: t('tableau.ddrm.candidats'), value: i.candidats, onClick: vers('CANDIDAT') },
        ...(instructeur
          ? [{ key: 'pieces', label: t('tableau.ddrm.piecesAExaminer'), value: i.piecesAExaminer, status: i.piecesAExaminer > 0 ? 'warn' as const : 'ok' as const,
              statusLabel: t(i.piecesAExaminer > 0 ? 'tableau.aTraiter' : 'tableau.aJour'), onClick: vers('CANDIDAT') }]
          : []),
        { key: 'adherents', label: t('tableau.ddrm.adherents'), value: i.adherents, onClick: vers('ADHERENT') },
        { key: 'enHomologation', label: t('tableau.ddrm.enHomologation'), value: i.enHomologation, onClick: vers('EN_HOMOLOGATION') },
        { key: 'closSansSuite', label: t('tableau.ddrm.closSansSuite'), value: i.closSansSuite, onClick: vers('CLOS_SANS_SUITE') },
      ]
    : []
  return (
    <PageSection title={t('tableau.ddrm.titre')}>
      {indicateurs.isPending ? (
        <Skeleton height={96} />
      ) : indicateurs.isError || !i ? (
        <StatusBadge status="crit" label={t('commun.erreur.titre')} />
      ) : (
        <StatGrid label={t('tableau.ddrm.titre')} items={tuiles} />
      )}
      {instructeur ? (
        <Card title={t('tableau.ddrm.mesDossiers')} actions={<Link href="/des/institutions?onglet=mesDossiers">{t('tableau.voirTout')}</Link>}>
          {mesDossiers.isPending ? (
            <Skeleton height={48} />
          ) : (
            <LinkList
              label={t('tableau.ddrm.mesDossiers')}
              empty={t('tableau.ddrm.aucunDossier')}
              items={(mesDossiers.data?.contenu ?? []).map((d) => ({
                key: d.id, href: `/des/institutions/${d.id}`, label: d.nom, status: etatInstitution[d.statut]?.status ?? 'neutral', statusLabel: t(`institution.statut.${d.statut}`),
              }))}
            />
          )}
        </Card>
      ) : null}
    </PageSection>
  )
}

/** Chef du DES et chef de la DDRM : cadrage des liaisons (étape 3, D-57) ; ils ouvrent et désignent. */
function SectionCadrage({ adherents }: { adherents: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const liens = useLiensLiaisons()
  const enCadrage = useLiaisons({ page: 1, taille: 5, filtres: { etat: 'OUVERTE' } })
  const indicateurs = useIndicateursDdrm(adherents)
  const tuiles: Tuile[] = [
    { key: 'cadrage', label: t('tableau.cadrage.enCadrage'), value: enCadrage.data?.total ?? '…', status: 'info', statusLabel: t('phase.CADRAGE'), onClick: () => void navigate('/des/liaisons?etat=OUVERTE') },
    ...(adherents ? [{ key: 'adherents', label: t('tableau.cadrage.adherents'), value: indicateurs.data?.adherents ?? '…', onClick: () => void navigate('/des/institutions?statut=ADHERENT') }] : []),
  ]
  return (
    <PageSection title={t('tableau.cadrage.titre')} actions={<Link href="/des/institutions?statut=ADHERENT">{t('tableau.cadrage.ouvrir')}</Link>}>
      <StatGrid label={t('tableau.cadrage.titre')} items={tuiles} />
      {enCadrage.isPending ? <Skeleton height={48} /> : <LinkList label={t('tableau.cadrage.enCadrage')} items={liens(enCadrage.data?.contenu ?? [])} />}
    </PageSection>
  )
}

/** Conducteur de projet : « Mes projets », les liaisons dont il est le conducteur. */
function SectionMesProjets() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const liens = useLiensLiaisons()
  const mes = useLiaisons({ page: 1, taille: 10, filtres: { mesProjets: true } })
  const enCadrage = useLiaisons({ page: 1, taille: 1, filtres: { mesProjets: true, etat: 'OUVERTE' } })
  return (
    <PageSection title={t('tableau.projets.titre')} actions={<Link href="/des/liaisons?mesProjets=1">{t('tableau.voirTout')}</Link>}>
      <StatGrid
        label={t('tableau.projets.titre')}
        items={[
          { key: 'total', label: t('tableau.projets.total'), value: mes.data?.total ?? '…', onClick: () => void navigate('/des/liaisons?mesProjets=1') },
          { key: 'cadrage', label: t('tableau.projets.enCadrage'), value: enCadrage.data?.total ?? '…', status: 'info', statusLabel: t('phase.CADRAGE') },
        ]}
      />
      {mes.isPending ? <Skeleton height={48} /> : <LinkList label={t('tableau.projets.liste')} empty={t('tableau.projets.aucun')} items={liens(mes.data?.contenu ?? [])} />}
    </PageSection>
  )
}

/** Raccourcis vers les écrans que la matrice des rôles ouvre (navigation filtrée par permission). */
function MesAcces({ permissions }: { permissions: string[] | undefined }) {
  const { t } = useTranslation()
  const entrees = (NAVIGATION_DES as GroupeNavigation[])
    .flatMap((g) => g.entrees)
    .filter((e) => e.cle !== 'tableauDeBord' && aPermission(permissions, ...(e.permission ? [e.permission] : [])))
  if (entrees.length === 0) return null
  return (
    <PageSection title={t('tableau.acces.titre')}>
      <LinkList label={t('tableau.acces.titre')} items={entrees.map((e) => ({ key: e.cle, href: e.chemin, label: t(e.libelleKey) }))} />
    </PageSection>
  )
}

export default function TableauDeBordPage() {
  const { t } = useTranslation()
  const session = useSession()
  const permissions = usePermissions()
  const lecteurRegistre = aPermission(permissions, 'PAR_INSTITUTION_READ')
  const instructeur = lecteurRegistre && aPermission(permissions, 'PAR_ADHESION_INSTRUCT')
  const approbateur = lecteurRegistre && aPermission(permissions, 'PAR_TRANSITION_APPROVE')
  const ddrm = instructeur || approbateur
  const cadrage = aPermission(permissions, 'HOM_LIAISON_OPEN') && aPermission(permissions, 'HOM_PROJECT_DESIGNATE')
  const projets = mesProjetsParDefaut(session?.roles, permissions)
  const pilotage = aPermission(permissions, 'HOM_DASHBOARD_READ')
  const profils = (session?.roles ?? []).filter((r) => r !== 'ROLE_USER')
  const fonctions = fonctionsDe(session?.roles ?? [])
  const rien = !ddrm && !cadrage && !projets && !pilotage && !lecteurRegistre
  return (
    <div className="gm-col">
      <PageHeader
        title={t('tableau.titre')}
        meta={session ? <><span>{t('tableau.bienvenue', { nom: session.nom })}</span>{profils.map((r) => <Tag key={r}>{t(`profil.${r}`, { defaultValue: r })}</Tag>)}</> : undefined}
      />
      {ddrm ? <SectionDdrm approbateur={approbateur} instructeur={instructeur} /> : null}
      {cadrage ? <SectionCadrage adherents={!ddrm && lecteurRegistre} /> : null}
      {projets ? <SectionMesProjets /> : null}
      {pilotage ? (
        <PageSection title={t('tableau.homologation.titre')}>
          <LinkList label={t('tableau.homologation.titre')} items={[{ key: 'pilotage', href: '/des/liaisons?onglet=pilotage', label: t('tableau.homologation.pilotage') }]} />
        </PageSection>
      ) : null}
      {pilotage && fonctions.some((f) => f === 'DG' || f === 'CHEF_DES' || f === 'CHEF_DDRM') ? <SectionCockpit /> : null}
      {fonctions.length > 0 && aPermission(permissions, 'HOM_LIAISON_LIST') ? <SectionActeur fonctions={fonctions} /> : null}
      <MesAcces permissions={permissions} />
      {rien ? <EmptyState icon="gauge" title={t('tableau.vide.titre')} text={t('tableau.vide.texte')} /> : null}
    </div>
  )
}
