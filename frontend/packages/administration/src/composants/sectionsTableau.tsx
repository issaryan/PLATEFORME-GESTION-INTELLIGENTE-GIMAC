import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ActionBar, Button, Card, DataTable, Link, PageSection, StatGrid, Tag, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { dateHeure } from '@gimac/compte'
import { useIndicateursComptes, useJournal, useLibelleProfil } from '../hooks'
import type { EntreeJournal } from '../modele/comptes'
import { useChemins } from '../services/useServicesConsole'
import { Donnees, Section } from './communs'
import { DialogueCreationCompte } from './DialogueCreationCompte'

// Sections d'administration des tableaux de bord (D-62, D-69), communes à la console (/console) et au
// département (/des/administration) : gestion des utilisateurs (actions directes, indicateurs cliquables
// vers la liste filtrée) et derniers actes d'administration. Chemins de l'application courante (useChemins).

/**
 * Gestion des utilisateurs (D-62), en tête du tableau de bord du super administrateur et du RSSI :
 * actions directes (création si IAM_ACCOUNT_CREATE, listes filtrées, profils, journal, politique) et
 * indicateurs des comptes cliquables vers la liste filtrée (?etat=). La création s'ouvre en modale.
 */
export function SectionGestionUtilisateurs({ posture }: { posture: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { base } = useChemins()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil()
  const indicateurs = useIndicateursComptes()
  const [creation, setCreation] = useState(false)
  const peut = (p: string) => aPermission(permissions, p)
  const vers = (chemin: string) => () => void navigate(chemin)
  const alerte = (n: number, s: 'crit' | 'warn', libelle: string) => ({ status: n > 0 ? s : ('ok' as const), statusLabel: t(n > 0 ? libelle : 'console.aJour') })
  return (
    <PageSection title={t('console.gestion.titre')} description={t(posture ? 'console.gestion.texte.rssi' : 'console.gestion.texte')}>
      <ActionBar align="start" label={t('console.gestion.actions')}>
        {peut('IAM_ACCOUNT_CREATE') ? <Button variant="primary" icon="plus" onClick={() => setCreation(true)}>{t('console.comptes.creer')}</Button> : null}
        <Button onClick={vers(`${base}/comptes`)}>{t('console.gestion.tous')}</Button>
        <Button variant="ghost" onClick={vers(`${base}/comptes?etat=SANS_2FA`)}>{t('console.gestion.sans2fa')}</Button>
        <Button variant="ghost" onClick={vers(`${base}/comptes?etat=PREMIER_ACCES`)}>{t('console.gestion.premierAcces')}</Button>
        <Button variant="ghost" onClick={vers(`${base}/profils`)}>{t('console.nav.profils')}</Button>
        {peut('IAM_JOURNAL_READ') ? <Button variant="ghost" onClick={vers(`${base}/journal`)}>{t('console.nav.journal')}</Button> : null}
        {peut('PLATFORM_SECURITY_POLICY_READ') ? <Button variant="ghost" onClick={vers(`${base}/politique-securite`)}>{t('console.nav.politique')}</Button> : null}
      </ActionBar>
      <Donnees requete={indicateurs}>
        {(i) => (
          <div className="gm-col">
            <StatGrid
              label={t(posture ? 'console.posture.titre' : 'console.comptes.indicateurs')}
              items={[
                { key: 'total', label: t('console.indicateur.total'), value: i.total, onClick: vers(`${base}/comptes`) },
                { key: 'actifs', label: t('console.indicateur.actifs'), value: i.actifs, onClick: vers(`${base}/comptes?etat=ACTIF`) },
                { key: 'sans2fa', label: t('console.indicateur.sansSecondFacteur'), value: i.sansSecondFacteur, ...alerte(i.sansSecondFacteur, 'crit', 'console.aTraiter'), onClick: vers(`${base}/comptes?etat=SANS_2FA`) },
                { key: 'premierAcces', label: t('console.indicateur.premierAcces'), value: i.premierAccesEnAttente, ...alerte(i.premierAccesEnAttente, 'warn', 'console.enAttente'), onClick: vers(`${base}/comptes?etat=PREMIER_ACCES`) },
                { key: 'inactifs', label: t('console.indicateur.inactifs'), value: i.inactifs, onClick: vers(`${base}/comptes?etat=INACTIF`) },
                { key: 'privilegies', label: t('console.indicateur.privilegies'), value: i.privilegies, onClick: vers(`${base}/profils`) },
                ...(posture ? [{ key: 'crees30', label: t('console.indicateur.crees30'), value: i.creesDepuis30Jours, onClick: vers(`${base}/comptes`) }] : []),
                { key: 'actes24', label: t('console.indicateur.actes24'), value: i.actesDepuis24Heures, ...(peut('IAM_JOURNAL_READ') ? { onClick: vers(`${base}/journal`) } : {}) },
              ]}
            />
            {posture ? (
              <Card title={t('console.indicateur.parProfil')}>
                <span className="gm-row">
                  {Object.entries(i.parProfil).map(([p, n]) => <Tag key={p}>{`${libelleProfil(p)} : ${n}`}</Tag>)}
                </span>
              </Card>
            ) : null}
          </div>
        )}
      </Donnees>
      {creation ? <DialogueCreationCompte onFermer={() => setCreation(false)} /> : null}
    </PageSection>
  )
}

export function SectionActes() {
  const { t, i18n } = useTranslation()
  const { base } = useChemins()
  const journal = useJournal({ page: 1, taille: 5 })
  const colonnes: Column<EntreeJournal>[] = [
    { key: 'horodatage', header: t('console.journal.horodatage'), render: (j) => dateHeure(j.horodatage, i18n.language) },
    { key: 'acteurNom', header: t('console.journal.acteur'), render: (j) => j.acteurNom ?? j.acteurId },
    { key: 'action', header: t('console.journal.action'), render: (j) => t(`console.journal.action.${j.action}`, { defaultValue: j.action }) },
    { key: 'cibleLibelle', header: t('console.journal.cible'), render: (j) => <span className="gm-mono">{j.cibleLibelle ?? '—'}</span> },
    { key: 'motif', header: t('console.journal.motif'), render: (j) => j.motif ?? '—' },
  ]
  return (
    <Section titre={t('console.actes.titre')} id="gm-actes" action={<Link href={`${base}/journal`}>{t('console.actes.voirTout')}</Link>}>
      <Donnees requete={journal} estVide={(p) => p.contenu.length === 0} vide={t('console.journal.vide')}>
        {(p) => <DataTable columns={colonnes} rows={p.contenu} rowKey={(j) => j.id} density="compact" />}
      </Donnees>
    </Section>
  )
}

/**
 * Tableau de bord d'administration des comptes, tel que le département l'affiche (D-69) : gestion des
 * utilisateurs (posture de sécurité pour le RSSI) puis derniers actes, selon les permissions.
 */
export function TableauAdministrationComptes() {
  const permissions = usePermissions()
  const posture = !aPermission(permissions, 'IAM_ROLE_PRIVILEGED_GRANT')
  return (
    <>
      <SectionGestionUtilisateurs posture={posture} />
      {aPermission(permissions, 'IAM_JOURNAL_READ') ? <SectionActes /> : null}
    </>
  )
}
