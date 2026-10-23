import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { Banner, Breadcrumb, Button, Card, KeyValueList, Link, StatusBadge, Tabs, PageHeader } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { DossierLiaison } from '../../../modele/parcours'
import { useDossierLiaison } from '../../../hooks/parcours'
import { useOptionsTypesProjet } from '../../../hooks/referentiels'
import { PERM } from '../../../permissions'
import { Donnees } from '../liaisons/communs'
import { BadgeStatut, Jour, useRetour, Valeur } from './communs'
import { DialogueConducteur } from './DialogueConducteur'
import { DialogueAffectation } from './DialogueAffectation'
import { DialogueBlocage, OngletBlocages } from './onglets/OngletBlocages'
import { OngletDecision } from './onglets/OngletDecision'
import { OngletEtapes } from './onglets/OngletEtapes'
import { OngletGouvernance } from './onglets/OngletGouvernance'
import { OngletGraphe, OngletPlanning } from './onglets/OngletGraphePlanning'
import { OngletMiseEnService } from './onglets/OngletMiseEnService'
import { OngletOperations } from './onglets/OngletOperations'
import { OngletPerimetre } from './onglets/OngletPerimetre'
import { OngletChronologie, OngletPreuves, OngletSignatures } from './onglets/OngletPreuves'
import { OngletSandbox } from './onglets/OngletSandbox'

// Dossier numérique de la liaison (/des/parcours/:id, gabarit G3 ; CR Fig. 7.3, D-57) : identification,
// pilotage, puis dix onglets portés par l'adresse (?onglet=). L'écran principal est l'état du périmètre
// contractualisé et ses deux couvertures (CR §7.2.2). Les gestes du dossier (désigner le conducteur,
// affecter les ressources, déclarer un blocage) sont portés par ?action= : les tableaux de bord y mènent.

// D-109 : « Livrables et preuves », « Signatures » et « Chronologie » du dossier (preuves attendues de chaque étape et opération,
// signatures attendues et recueillies, événements réels) ; l'exécution reste dans « Opérations ».
// US-EXI-17 : « Résultats de la sandbox » (DES-09), repris par MS-HOMOLOGATION de SBX-LECTURE, en lecture seule.
const ONGLETS = ['perimetre', 'etapes', 'operations', 'graphe', 'planning', 'preuves', 'signatures', 'chronologie', 'sandbox', 'decision', 'mise-en-service', 'blocages', 'gouvernance'] as const
type Onglet = (typeof ONGLETS)[number]

/** Gestes du dossier : proposés si le service les permet ET si le jeton porte la permission. */
export function useGestesDossier(dossier: DossierLiaison) {
  const permissions = usePermissions()
  const permis = (action: string, ...perms: string[]) => dossier.actionsPermises.includes(action) && aPermission(permissions, ...perms)
  return {
    designer: permis('DESIGNER_CONDUCTEUR', PERM.projetDesigner),
    affecter: permis('AFFECTER_RESSOURCES', PERM.projetDesigner),
    bloquer: permis('DECLARER_BLOCAGE', PERM.etapeConduire),
  }
}

function Dossier({ dossier }: { dossier: DossierLiaison }) {
  const permissions = usePermissions()
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const [retour, annoncer] = useRetour()
  const gestes = useGestesDossier(dossier)
  // D-98 : la tâche « Prendre la fonction » ou « Affecter <fonction> » mène à ?onglet=etapes&fonction=<FONCTION> : l'affectation
  // se fait dans la gouvernance, ouverte avec la fonction demandée.
  const fonctionDemandee = params.get('fonction')
  const demande = (ONGLETS as readonly string[]).includes(params.get('onglet') ?? '') ? (params.get('onglet') as Onglet) : 'perimetre'
  const onglet: Onglet = fonctionDemandee && demande === 'etapes' ? 'gouvernance' : demande
  const action = params.get('action')
  const maj = (m: Record<string, string | null>) =>
    setParams(
      (p) => {
        const n = new URLSearchParams(p)
        Object.entries(m).forEach(([k, v]) => (v ? n.set(k, v) : n.delete(k)))
        return n
      },
      { replace: true },
    )
  const fermer = () => maj({ action: null })
  const blocagesEnCours = dossier.blocages.filter((b) => !b.leveLe).length
  // Recette HOM-09 : type créé dans la plateforme (D-91) nommé par son libellé servi, jamais par son code.
  const types = useOptionsTypesProjet()

  const identification = [
    { key: t('parcoursDossier.reference'), value: dossier.reference, mono: true },
    // D-104 : intitulé du projet et référence d'origine (reprise), s'ils sont renseignés.
    ...(dossier.intituleProjet ? [{ key: t('cadrage.intitule'), value: dossier.intituleProjet }] : []),
    ...(dossier.referenceOrigine ? [{ key: t('cadrage.referenceOrigine'), value: dossier.referenceOrigine, mono: true }] : []),
    { key: t('parcoursDossier.institution'), value: dossier.institutionNom },
    // CER-05 : libellé servi avec le dossier quand le catalogue des types ne le connaît pas.
    { key: t('parcoursDossier.type'), value: types.libelle(dossier.typeProjet) === dossier.typeProjet && dossier.typeProjetLibelle ? dossier.typeProjetLibelle : types.libelle(dossier.typeProjet) },
    { key: t('parcoursDossier.banque'), value: dossier.banquePartenaireNom ?? <span className="gm-field-help">{t('parcoursDossier.sansBanque')}</span> },
    ...(dossier.personnalisateurNom ? [{ key: t('parcoursDossier.personnalisateur'), value: dossier.personnalisateurNom }] : []),
    { key: t('parcoursDossier.versionReferentiel'), value: <Valeur>{dossier.versionReferentiel === null ? null : t('parcoursDossier.version', { v: dossier.versionReferentiel })}</Valeur> },
    ...(dossier.liaisonOrigineId
      ? [{ key: t('parcoursDossier.origine'), value: <Link href={`/des/parcours/${dossier.liaisonOrigineId}`} mono>{dossier.liaisonOrigineId}</Link> }]
      : []),
  ]
  const pilotage = [
    { key: t('parcoursDossier.conducteur'), value: dossier.conducteur?.nom ?? <StatusBadge status="warn" label={t('parcoursListe.aDesigner')} /> },
    // D-93 : responsables lus du RACI ; une fonction à affecter le signale.
    ...(dossier.responsableFonctionnel ? [{ key: t('parcoursDossier.responsableFonctionnel'), value: <Valeur>{dossier.responsableFonctionnel.nom}</Valeur> }] : []),
    ...(dossier.responsableTechnique ? [{ key: t('parcoursDossier.responsableTechnique'), value: <Valeur>{dossier.responsableTechnique.nom}</Valeur> }] : []),
    ...((dossier.intervenants ?? []).some((i) => i.aAffecter)
      ? [{ key: t('parcoursDossier.aAffecter'), value: <Link href={`/des/parcours/${dossier.id}?onglet=gouvernance`}>{t('parcoursDossier.aAffecterN', { count: (dossier.intervenants ?? []).filter((i) => i.aAffecter).length })}</Link> }]
      : []),
    { key: t('parcoursDossier.demarrage'), value: <Jour iso={dossier.dateDemarrage} /> },
    { key: t('parcoursDossier.cible'), value: <Jour iso={dossier.dateCibleProduction} /> },
  ]
  const contenu: Record<Onglet, ReactNode> = {
    perimetre: <OngletPerimetre dossier={dossier} />,
    etapes: <OngletEtapes dossier={dossier} onAffecter={gestes.affecter ? () => maj({ action: 'affecter' }) : undefined} />,
    operations: <OngletOperations dossier={dossier} />,
    graphe: <OngletGraphe dossier={dossier} />,
    planning: <OngletPlanning dossier={dossier} />,
    preuves: <OngletPreuves dossier={dossier} />,
    signatures: <OngletSignatures dossier={dossier} />,
    chronologie: <OngletChronologie dossier={dossier} />,
    sandbox: <OngletSandbox dossier={dossier} />,
    decision: <OngletDecision dossier={dossier} />,
    'mise-en-service': <OngletMiseEnService dossier={dossier} />,
    blocages: <OngletBlocages dossier={dossier} onDeclarer={gestes.bloquer ? () => maj({ action: 'bloquer' }) : undefined} />,
    gouvernance: <OngletGouvernance dossier={dossier} fonction={fonctionDemandee} />,
  }
  const libelleOnglet = (k: Onglet) => (k === 'blocages' && blocagesEnCours > 0 ? t('parcoursDossier.onglet.blocagesN', { count: blocagesEnCours }) : t(`parcoursDossier.onglet.${k}`))

  return (
    <div className="gm-col">
      <Breadcrumb items={[{ label: t('parcoursListe.titre'), href: '/des/parcours' }, { label: dossier.reference }]} />
      {params.get('nouvelle') === '1' ? (
        <Banner level="info" title={t('parcoursDossier.nouvelle.titre', { reference: dossier.reference })} onDismiss={() => maj({ nouvelle: null })}>
          {t('parcoursDossier.nouvelle.texte')}
        </Banner>
      ) : null}
      <PageHeader
        title={dossier.institutionNom}
        meta={<>
          <span className="gm-mono">{dossier.reference}</span>
          <BadgeStatut statut={dossier.statut} />
          <span className="gm-pagehead-user">{t('parcoursDossier.etapeCourante', { etape: dossier.etapesVersion?.find((e) => e.code === dossier.etapeCourante)?.libelle ?? t(`parcours.etape.${dossier.etapeCourante}`) })}</span>
          {blocagesEnCours > 0 ? <StatusBadge status="crit" label={t('parcoursListe.bloquee')} /> : null}
        </>}
        actions={<>
          {/* Messagerie (D-64) : un nouveau message rattaché à cette liaison, qui y est citée d'emblée. */}
          {aPermission(permissions, PERM.messageEnvoyer) ? (
            <Link href={`/des/messagerie?nouveau=1&liaison=${encodeURIComponent(dossier.id)}`}>{t('msg.discuter')}</Link>
          ) : null}
          {gestes.bloquer ? <Button variant="secondary" icon="flag" onClick={() => maj({ action: 'bloquer' })}>{t('parcoursBlocages.declarer')}</Button> : null}
          {gestes.designer ? (
            <Button variant={dossier.conducteur ? 'secondary' : 'primary'} icon="user" onClick={() => maj({ action: 'designer' })}>
              {t(dossier.conducteur ? 'parcoursConducteur.redesigner' : 'parcoursConducteur.designer')}
            </Button>
          ) : null}
          {gestes.affecter ? <Button variant="primary" icon="users" onClick={() => maj({ action: 'affecter' })}>{t('parcoursAffectation.titre')}</Button> : null}
        </>}
      />
      <div className="gm-row" style={{ alignItems: 'stretch', gap: 'var(--gm-space-4)' }}>
        <div style={{ flex: '1 1 320px', minWidth: 0 }}>
          <Card title={t('parcoursDossier.identification')}>
            <KeyValueList items={identification} />
          </Card>
        </div>
        <div style={{ flex: '1 1 320px', minWidth: 0 }}>
          <Card title={t('parcoursDossier.pilotage')}>
            <KeyValueList items={pilotage} />
          </Card>
        </div>
      </div>
      <Tabs items={ONGLETS.map((k) => ({ key: k, label: libelleOnglet(k) }))} value={onglet} onChange={(k) => setParams(k === 'perimetre' ? {} : { onglet: k })} />
      <section aria-label={libelleOnglet(onglet)}>{contenu[onglet]}</section>
      {action === 'designer' && gestes.designer ? (
        <DialogueConducteur
          liaison={dossier}
          onFermer={fermer}
          onDesigne={(nom) => {
            fermer()
            annoncer(t('parcoursConducteur.fait', { nom }))
          }}
        />
      ) : null}
      {action === 'affecter' && gestes.affecter ? (
        <DialogueAffectation
          dossier={dossier}
          onFermer={fermer}
          onAffecte={() => {
            fermer()
            annoncer(t('parcoursAffectation.fait'))
          }}
        />
      ) : null}
      {action === 'bloquer' && gestes.bloquer ? (
        <DialogueBlocage
          dossier={dossier}
          onFermer={fermer}
          onDeclare={() => {
            maj({ action: null, onglet: 'blocages' })
            annoncer(t('parcoursBlocages.fait'))
          }}
        />
      ) : null}
      {retour}
    </div>
  )
}

export default function DossierLiaisonPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const dossier = useDossierLiaison(id)
  return (
    <div className="gm-col">
      <Donnees requete={dossier} vide={t('erreur.introuvable')}>
        {(d) => <Dossier dossier={d} />}
      </Donnees>
    </div>
  )
}
