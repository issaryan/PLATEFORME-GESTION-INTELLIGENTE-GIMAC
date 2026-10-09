import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import { Button, Card, InlineAlert, KeyValueList, Link, LinkList, PageHeader, RadioGroup, Select, StatusBadge, Stepper, type StepState } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { DossierInstitution, Institution, StatutInstitution } from '../../../modele/participant'
import { etatInstitution } from '../../../dictionnaires'
import { useDossierInstitution, useInstitutions } from '../../../hooks/participant'
import { useConducteurs, useLiaisonsParcours, useOuvrirLiaison } from '../../../hooks/parcours'
import { useOptionsTypesProjet } from '../../../hooks/referentiels'
import { PERM } from '../../../permissions'
import { useDescripteurDS } from '../../../ressources/descripteur'
import { creerInstitutionDescriptor } from '../../../ressources/institution'
import { FormulaireInstitution } from '../institutions/formulaires'
import { useErreurService } from '../liaisons/communs'
import { useFormatDate } from '../parcours/communs'

// « Lancer une certification » (D-98 §5, docs/lot1/lancement-processus.md §8) : point d'entrée unique du parcours de
// certification. Il guide, en réutilisant les routes existantes, de l'institution (choisie au registre ou enregistrée)
// à son adhésion (instruite et prononcée sur sa fiche), puis à l'ouverture de la liaison (type de projet servi en données,
// D-91, et conducteur désigné dans le même acte), et ouvre enfin le dossier de la liaison. L'institution choisie est
// portée par l'adresse (?institution=) ; MS-PARTICIPANT et MS-HOMOLOGATION décident, l'écran affiche leurs refus.

/** Statuts conduits vers une certification : candidature et adhésion à mener, ou liaison à ouvrir. */
const STATUTS_LANCEMENT: StatutInstitution[] = ['CANDIDAT', 'ADHERENT', 'EN_HOMOLOGATION']
/** Statuts depuis lesquels une liaison s'ouvre (affichage ; MS-HOMOLOGATION revérifie). */
const OUVRABLES: StatutInstitution[] = ['ADHERENT', 'EN_HOMOLOGATION']
/** Mémoire de l'onglet : l'institution choisie, retrouvée au retour de sa fiche (commodité, jamais une donnée métier). */
const MEMOIRE = 'gm.lancementCertification.institution'

function lireMemoire(): string | null {
  try {
    return sessionStorage.getItem(MEMOIRE)
  } catch {
    return null
  }
}
function ecrireMemoire(id: string | null) {
  try {
    if (id) sessionStorage.setItem(MEMOIRE, id)
    else sessionStorage.removeItem(MEMOIRE)
  } catch {
    // Stockage indisponible : l'adresse porte l'institution.
  }
}

function EtapeInstitution({ institutionId, onChoisir }: { institutionId: string; onChoisir: (id: string) => void }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [creation, setCreation] = useState(false)
  const institutions = useInstitutions({ page: 1, taille: 200, statut: STATUTS_LANCEMENT })
  const lot = useMemo(() => creerInstitutionDescriptor({ t, ouvertureLiaison: false }), [t])
  const descripteur = useDescripteurDS(lot)
  const options = (institutions.data?.contenu ?? []).map((i) => ({ value: i.id, label: t('certif.institution.option', { nom: i.sigle ? `${i.nom} (${i.sigle})` : i.nom, statut: t(etatInstitution[i.statut]?.label ?? i.statut) }) }))
  return (
    <Card title={t('certif.institution.titre')}>
      <div className="gm-col">
        {institutions.isSuccess && !options.length ? <InlineAlert level="info">{t('certif.institution.aucune')}</InlineAlert> : null}
        <div className="gm-row" style={{ alignItems: 'flex-end', gap: 'var(--gm-space-3)', flexWrap: 'wrap' }}>
          <div style={{ flex: '1 1 320px', minWidth: 0 }}>
            <Select name="institution" label={t('certif.institution.choisir')} help={t('certif.institution.choisir.aide')} value={institutionId}
              disabled={institutions.isPending} options={[{ value: '', label: t('certif.ouverture.choisir') }, ...options]} onChange={onChoisir} />
          </div>
          {aPermission(permissions, 'PAR_INSTITUTION_CREATE') ? (
            <Button variant="secondary" icon="plus" onClick={() => setCreation(true)}>{t('certif.institution.nouvelle')}</Button>
          ) : null}
        </div>
      </div>
      {creation ? <FormulaireInstitution descripteur={descripteur} onFermer={() => setCreation(false)} onCree={(i) => { setCreation(false); onChoisir(i.id) }} /> : null}
    </Card>
  )
}

function EtapeAdhesion({ dossier }: { dossier: DossierInstitution }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const date = useFormatDate()
  const i = dossier.institution
  const id = encodeURIComponent(i.id)
  const enAttente = dossier.transitions.find((x) => x.etat === 'PROPOSEE' && x.vers === 'ADHERENT')
  const reunis = dossier.prerequisAdhesion.filter((p) => p.satisfait).length
  return (
    <Card title={t('certif.adhesion.titre')}>
      <div className="gm-col">
        {OUVRABLES.includes(i.statut) ? <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}><StatusBadge status="ok" icon="check" label={t(etatInstitution[i.statut]?.label ?? i.statut)} /><span>{t('certif.adhesion.faite')}</span></span> : null}
        {!STATUTS_LANCEMENT.includes(i.statut) ? <InlineAlert level="warn">{t('certif.adhesion.ineligible', { statut: t(etatInstitution[i.statut]?.label ?? i.statut) })}</InlineAlert> : null}
        {i.statut === 'CANDIDAT' ? (
          <>
            <p className="gm-section-desc" style={{ margin: 0 }}>{t('certif.adhesion.candidat')}</p>
            <span>{t('certif.adhesion.prerequis', { n: reunis, total: dossier.prerequisAdhesion.length })}</span>
            <ul className="gm-col" style={{ gap: 'var(--gm-space-1)', margin: 0, paddingLeft: 'var(--gm-space-4)' }}>
              {dossier.prerequisAdhesion.map((p) => (
                <li key={p.code}>
                  <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                    <StatusBadge status={p.satisfait ? 'ok' : 'warn'} label={t(p.satisfait ? 'prerequis.satisfait' : 'prerequis.manquant')} />
                    <span>{p.libelle}</span>
                  </span>
                </li>
              ))}
            </ul>
            {enAttente ? (
              <InlineAlert level="info" title={t('certif.adhesion.enAttente', { date: date(enAttente.demandeeLe) })}>
                {aPermission(permissions, PERM.transitionApprouver) ? <Link href={`/des/institutions/${id}?onglet=transitions`}>{t('certif.adhesion.examiner')}</Link> : null}
              </InlineAlert>
            ) : null}
            <div className="gm-row" style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap' }}>
              {aPermission(permissions, PERM.adhesionInstruire) ? <Link href={`/des/institutions/${id}?onglet=pieces`}>{t('certif.adhesion.instruire')}</Link> : null}
              {!enAttente && aPermission(permissions, 'PAR_TRANSITION_REQUEST') ? <Link href={`/des/institutions/${id}`}>{t('certif.adhesion.proposer')}</Link> : null}
            </div>
          </>
        ) : null}
      </div>
    </Card>
  )
}

function EtapeOuverture({ institution }: { institution: Institution }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const peutOuvrir = aPermission(permissions, PERM.liaisonOuvrir)
  const ouvrable = OUVRABLES.includes(institution.statut)
  const types = useOptionsTypesProjet(true)
  const conducteurs = useConducteurs(peutOuvrir && ouvrable)
  const banques = useInstitutions({ page: 1, taille: 200, statut: ['ADHERENT', 'EN_HOMOLOGATION', 'ACTIF'] }, peutOuvrir && ouvrable)
  // Liaisons déjà ouvertes pour l'institution : la recherche du service porte sur le nom de l'institution.
  const liaisons = useLiaisonsParcours({ page: 1, taille: 25, recherche: institution.nom })
  const ouvrir = useOuvrirLiaison()
  const erreur = useErreurService()(ouvrir.error)
  const [typeProjet, setType] = useState('')
  const [conducteurId, setConducteur] = useState('')
  const [banqueId, setBanque] = useState('')
  const [tente, setTente] = useState(false)
  const choisir = { value: '', label: t('certif.ouverture.choisir') }
  const erreurs = { type: !typeProjet ? t('certif.ouverture.erreur.type') : undefined, conducteur: !conducteurId ? t('certif.ouverture.erreur.conducteur') : undefined }
  const soumettre = () => {
    setTente(true)
    if (erreurs.type || erreurs.conducteur) return
    ouvrir.mutate(
      { institutionId: institution.id, typeProjet, chefDeProjetId: conducteurId, banquePartenaireId: banqueId || null },
      { onSuccess: (l) => { ecrireMemoire(null); void navigate(`/des/parcours/${encodeURIComponent(l.id)}?nouvelle=1`) } },
    )
  }
  const siennes = (liaisons.data?.contenu ?? []).filter((l) => l.institutionNom === institution.nom)
  return (
    <Card title={t('certif.ouverture.titre')}>
      <div className="gm-col">
        {siennes.length ? (
          <LinkList label={t('certif.ouverture.liaisons')}
            items={siennes.map((l) => ({ key: l.id, href: `/des/parcours/${encodeURIComponent(l.id)}`, label: `${l.reference} · ${types.libelle(l.typeProjet)}`, meta: t(`parcours.statut.${l.statut}`) }))} />
        ) : null}
        {!ouvrable ? <InlineAlert level="info">{t('certif.ouverture.attente')}</InlineAlert> : null}
        {ouvrable && !peutOuvrir ? <InlineAlert level="info">{t('certif.ouverture.nonPermise')}</InlineAlert> : null}
        {ouvrable && peutOuvrir ? (
          <>
            {erreur ? <InlineAlert level="crit" title={t('certif.ouverture.refus')} correlationId={erreur.correlationId || undefined}>{erreur.message}</InlineAlert> : null}
            <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
              {/* DDR-27 : choix obligatoire signalé par l'astérisque, comme les autres champs. */}
              <RadioGroup name="typeProjet" label={t('certif.ouverture.type')} required value={typeProjet} options={types.options} onChange={setType} />
              <span className="gm-field-help">{t('certif.ouverture.type.aide')}</span>
              {types.pret && !types.options.length ? <InlineAlert level="info">{t('certif.ouverture.aucunType')}</InlineAlert> : null}
              {tente && erreurs.type ? <span className="gm-field-error" role="alert">{erreurs.type}</span> : null}
            </div>
            <Select name="conducteurId" label={t('certif.ouverture.conducteur')} help={t('certif.ouverture.conducteur.aide')} required value={conducteurId}
              disabled={conducteurs.isPending} options={[choisir, ...(conducteurs.data ?? []).map((p) => ({ value: p.id, label: p.nom }))]} onChange={setConducteur}
              error={tente ? erreurs.conducteur : undefined} />
            <Select name="banquePartenaireId" label={t('certif.ouverture.banque')} help={t('certif.ouverture.banque.aide')} value={banqueId}
              options={[{ value: '', label: t('certif.ouverture.sansBanque') }, ...(banques.data?.contenu ?? []).filter((b) => b.categorie === 'BANQUE' && b.id !== institution.id).map((b) => ({ value: b.id, label: b.nom }))]}
              onChange={setBanque} />
            <div className="gm-row">
              <Button variant="primary" icon="route" onClick={soumettre} loading={ouvrir.isPending}>{t('certif.ouverture.valider')}</Button>
            </div>
          </>
        ) : null}
      </div>
    </Card>
  )
}

function Parcours({ institutionId, onChoisir }: { institutionId: string; onChoisir: (id: string) => void }) {
  const { t } = useTranslation()
  const dossier = useDossierInstitution(institutionId)
  const d = dossier.data
  const statut = d?.institution.statut
  const etats: StepState[] = !d ? ['current', 'todo', 'todo', 'todo'] : statut === 'CANDIDAT' ? ['done', 'current', 'todo', 'todo'] : OUVRABLES.includes(statut!) ? ['done', 'done', 'current', 'todo'] : ['done', 'error', 'todo', 'todo']
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
      <Stepper steps={['institution', 'adhesion', 'ouverture', 'dossier'].map((k, i) => ({ label: t(`certif.etape.${k}`), state: etats[i] }))} />
      <EtapeInstitution institutionId={institutionId} onChoisir={onChoisir} />
      {d ? (
        <>
          <Card>
            <KeyValueList items={[
              { key: t('certif.institution.choisir'), value: <Link href={`/des/institutions/${encodeURIComponent(d.institution.id)}`}>{d.institution.nom}</Link> },
              { key: t('certif.institution.statut'), value: <StatusBadge status={etatInstitution[d.institution.statut]?.status ?? 'neutral'} label={t(etatInstitution[d.institution.statut]?.label ?? d.institution.statut)} /> },
              { key: t('certif.institution.code'), value: d.institution.codeParticipant ?? <span className="gm-field-help">{t('commun.nonRenseigne')}</span>, mono: true },
            ]} />
          </Card>
          <EtapeAdhesion dossier={d} />
          <EtapeOuverture institution={d.institution} />
        </>
      ) : null}
      {institutionId && dossier.isError ? <InlineAlert level="crit">{t('erreur.introuvable')}</InlineAlert> : null}
      <Card title={t('certif.dossier.titre')}>
        <div className="gm-col">
          <p className="gm-section-desc" style={{ margin: 0 }}>{t('certif.dossier.texte')}</p>
          <Link href="/des/certification/mes-actions">{t('nav.mesActionsCertification')}</Link>
        </div>
      </Card>
    </div>
  )
}

export default function LancerCertificationPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const institutionId = params.get('institution') ?? ''
  // Retour sur l'écran depuis la barre latérale : l'institution choisie dans cet onglet est reprise.
  useEffect(() => {
    if (!institutionId) {
      const garde = lireMemoire()
      if (garde) setParams({ institution: garde }, { replace: true })
    }
  }, [institutionId, setParams])
  const choisir = (id: string) => {
    ecrireMemoire(id || null)
    setParams(id ? { institution: id } : {}, { replace: true })
  }
  return (
    <div className="gm-col">
      <PageHeader title={t('certif.lancer.titre')} subtitle={t('certif.lancer.description')} />
      <Parcours institutionId={institutionId} onChoisir={choisir} />
    </div>
  )
}
