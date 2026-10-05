import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Card, EmptyState, Link, LinkList, PageHeader, PageSection, Skeleton, StatGrid, StatusBadge, type StatCardProps } from '@gimac/design-system'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import { useIndicateursDdrm, useInstitutions } from '../../hooks/participant'
import { etatInstitution } from '../../dictionnaires'
import { fonctionnalitesOuvertes, widgetsOuverts, type CleWidget, type Fonctionnalite } from '../../fonctionnalites'
import { PERM, fonctionsDe } from '../../permissions'
import { SectionActeur } from './tableau/SectionActeur'
import { SectionCockpit } from './tableau/SectionCockpit'
import { SectionAdministration } from './tableau/SectionAdministration'
import { SectionMaJournee } from './tableau/SectionMaJournee'
import { useExtensionsTableauDeBord } from './tableau/extensions'

// DES-01 Tableau de bord (/des/tableau-de-bord, gabarit G1 ; D-58, D-61, D-79) : chaque acteur voit son
// tableau de bord, composé des widgets des fonctionnalités que les permissions de son jeton ouvrent (registre
// fonctionnalites.ts) ; jamais vide : à défaut de widget, les raccourcis des fonctionnalités ouvertes. Les
// compteurs viennent des services ; l'interface n'en calcule aucun. Composition du catalogue (D-63) :
// PageHeader, PageSection, StatGrid, LinkList.

type Tuile = StatCardProps & { key: string }

/** Chargé et chef de la DDRM : étapes 1 et 2 du parcours (prise de contact, adhésion). */
function SectionDdrm({ approbateur, instructeur }: { approbateur: boolean; instructeur: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const indicateurs = useIndicateursDdrm()
  // « Mes dossiers en cours » paginés côté serveur, pagination visible comme celle des tableaux.
  const [pageDossiers, setPageDossiers] = useState({ page: 1, taille: 5 })
  const mesDossiers = useInstitutions({ ...pageDossiers, mesDossiers: true, statut: ['CANDIDAT'] }, instructeur)
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
              page={pageDossiers.page}
              pageSize={pageDossiers.taille}
              total={mesDossiers.data?.total ?? 0}
              onPage={(page) => setPageDossiers((p) => ({ ...p, page }))}
              onPageSize={(taille) => setPageDossiers({ page: 1, taille })}
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

/** Raccourcis vers les écrans que les permissions ouvrent (registre des fonctionnalités), hors tableau de bord. */
function MesAcces({ fonctionnalites }: { fonctionnalites: Fonctionnalite[] }) {
  const { t } = useTranslation()
  return (
    <PageSection title={t('tableau.acces.titre')}>
      <LinkList label={t('tableau.acces.titre')} items={fonctionnalites.map((f) => ({ key: f.cle, href: f.chemin, label: t(f.libelleKey) }))} />
    </PageSection>
  )
}

/** Widget de plateforme sans composant fourni par l'application : raccourci vers son écran. */
function Raccourci({ fonctionnalite }: { fonctionnalite?: Fonctionnalite }) {
  const { t } = useTranslation()
  if (!fonctionnalite) return null
  return (
    <PageSection title={t(fonctionnalite.libelleKey)} description={t(`tableau.widget.${fonctionnalite.cle}`)}
      actions={<Link href={fonctionnalite.chemin}>{t('tableau.widget.ouvrir')}</Link>} />
  )
}

export default function TableauDeBordPage() {
  const { t } = useTranslation()
  const session = useSession()
  const permissions = usePermissions()
  const profils = (session?.roles ?? []).filter((r) => r !== 'ROLE_USER')
  // Registre des fonctionnalités (D-79) : chaque widget s'affiche selon les permissions du jeton, jamais le rôle.
  const ouvertes = fonctionnalitesOuvertes(permissions)
  const widgets = widgetsOuverts(permissions)
  const a = (w: CleWidget) => widgets.includes(w)
  const extensions = useExtensionsTableauDeBord()
  const fonctionnalite = (cle: string) => ouvertes.find((f) => f.cle === cle)
  // Fonctions du parcours portées par la session : paramètre du tableau d'acteur de MS-HOMOLOGATION.
  const fonctions = fonctionsDe(session?.roles ?? [])
  const raccourcis = ouvertes.filter((f) => f.cle !== 'tableauDeBord')
  // Jamais vide : sans autre widget que « Ma journée », les raccourcis des fonctionnalités ouvertes.
  const sansWidget = widgets.every((w) => w === 'maJournee')
  const { administration: Administration, supervision: Supervision, infrastructure: Infrastructure } = extensions
  return (
    <div className="gm-col">
      <PageHeader
        title={t('tableau.titre')}
        subtitle={t('tableau.sousTitre')}
        user={session ? { name: session.nom, roles: profils.map((r) => t(`profil.${r}`, { defaultValue: r })) } : undefined}
      />
      {/* « Ma journée » (D-76 §6) : tout porteur de PIL_ACTIVITY_OWN, avant les widgets métier. */}
      {a('maJournee') ? <SectionMaJournee equipe={aPermission(permissions, PERM.equipeLire)} /> : null}
      {/* Administration des comptes (D-69) : indicateurs et actions de l'application, sinon section d'actions. */}
      {a('administration') ? (Administration ? <Administration /> : <SectionAdministration />) : null}
      {/* Plateforme (D-79) : résumé de la supervision et charges en erreur, fournis par l'application. */}
      {a('supervision') ? (Supervision ? <Supervision /> : <Raccourci fonctionnalite={fonctionnalite('supervision')} />) : null}
      {a('infrastructure') ? (Infrastructure ? <Infrastructure /> : <Raccourci fonctionnalite={fonctionnalite('infrastructure')} />) : null}
      {a('ddrm') ? <SectionDdrm approbateur={aPermission(permissions, PERM.transitionApprouver)} instructeur={aPermission(permissions, PERM.adhesionInstruire)} /> : null}
      {a('cockpit') ? <SectionCockpit /> : null}
      {a('monTravail') ? <SectionActeur fonctions={fonctions} /> : null}
      {sansWidget && raccourcis.length > 0 ? <MesAcces fonctionnalites={raccourcis} /> : null}
      {sansWidget && raccourcis.length === 0 ? <EmptyState icon="gauge" title={t('tableau.vide.titre')} text={t('tableau.vide.texte')} /> : null}
    </div>
  )
}
