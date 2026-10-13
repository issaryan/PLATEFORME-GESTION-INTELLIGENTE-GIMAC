import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { InlineAlert, LinkList, ResourceForm, type ResourceDescriptor } from '@gimac/design-system'
import type { TypeProjet } from '../../../modele/commun'
import type { Institution } from '../../../modele/participant'
import { useLiaisons } from '../../../hooks/liaisons'
// D-98 : même ouverture que le parcours (POST /liaisons, conducteurs habilités), puis le dossier de la liaison.
import { useConducteurs, useOuvrirLiaison } from '../../../hooks/parcours'
import { useInstitutions } from '../../../hooks/participant'
import { etatLiaison } from '../../../dictionnaires'
import { useOptionsTypesProjet } from '../../../hooks/referentiels'
import { useErreurService } from '../liaisons/communs'

// Étape 3 du parcours (D-57, D-58) : ouverture d'une liaison en phase de cadrage depuis la fiche d'une
// institution adhérente, en homologation ou active (D-107), par un chef de département qui détient HOM_LIAISON_OPEN et
// HOM_PROJECT_DESIGNATE. MS-HOMOLOGATION décide (éligibilité, unicité, habilitation du conducteur,
// référentiel publié) ; MS-PARTICIPANT fait ensuite passer l'institution en homologation.

/** Statuts de l'institution depuis lesquels l'ouverture est proposée (affichage ; le service revérifie). */
export const STATUTS_OUVERTURE: Institution['statut'][] = ['ADHERENT', 'EN_HOMOLOGATION', 'ACTIF']

type Valeurs = { typeProjet: string; intituleProjet: string; referenceOrigine: string; chefDeProjetId: string; banquePartenaireId: string }
/** Référence d'origine (D-104) : format accepté par MS-HOMOLOGATION (OuvertureRequest). */
export const REFERENCE_ORIGINE = /^[A-Za-z0-9][A-Za-z0-9._/-]*$/
const texte = (v: unknown) => (typeof v === 'string' ? v : '')

export function FormulaireCadrage({ institution, onFermer }: { institution: Institution; onFermer: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const conducteurs = useConducteurs()
  // Banques partenaires possibles : banques du registre déjà admises.
  const banques = useInstitutions({ page: 1, taille: 100, statut: ['ADHERENT', 'EN_HOMOLOGATION', 'ACTIF'] })
  const ouvrir = useOuvrirLiaison()
  // Types proposés au cadrage (D-91) : servis par MS-HOMOLOGATION.
  const types = useOptionsTypesProjet(true)
  const erreur = useErreurService()
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const descripteur: ResourceDescriptor<Valeurs> = {
    key: 'cadrage',
    label: { singular: t('liaison.singulier'), plural: t('liaison.pluriel'), gender: 'f' },
    idField: 'typeProjet',
    titleField: 'typeProjet',
    endpoints: { list: '', get: '', create: '/bff/des/homologation/liaisons' },
    permissions: { list: 'HOM_LIAISON_LIST', read: 'HOM_LIAISON_READ', create: 'HOM_LIAISON_OPEN' },
    actions: [],
    fields: [
      { name: 'typeProjet', label: t('liaison.typeProjet'), kind: 'radio', required: true, inForm: 'create', options: types.options },
      // D-104 : une institution peut conduire plusieurs projets, y compris du même type ; l'intitulé les distingue.
      { name: 'intituleProjet', label: t('cadrage.intitule'), kind: 'text', inForm: 'create', help: t('cadrage.intitule.aide') },
      { name: 'referenceOrigine', label: t('cadrage.referenceOrigine'), kind: 'mono', inForm: 'create', help: t('cadrage.referenceOrigine.aide') },
      {
        name: 'chefDeProjetId', label: t('cadrage.conducteur'), kind: 'select', required: true, inForm: 'create', help: t('cadrage.conducteur.aide'),
        // Le formulaire du catalogue place lui-même l'option vide « Choisir » en tête de liste.
        options: (conducteurs.data ?? []).map((c) => ({ value: c.id, label: c.nom })),
      },
      {
        name: 'banquePartenaireId', label: t('cadrage.banque'), kind: 'select', inForm: 'create', help: t('cadrage.banque.aide'),
        options: (banques.data?.contenu ?? []).filter((b) => b.categorie === 'BANQUE' && b.id !== institution.id).map((b) => ({ value: b.id, label: b.nom })),
      },
    ],
  }
  return (
    <ResourceForm
      descriptor={descripteur}
      mode="create"
      title={t('cadrage.form.titre', { nom: institution.nom })}
      submitLabel={t('cadrage.action.ouvrir')}
      intro={<InlineAlert level="info">{t(`cadrage.form.intro.${institution.statut === 'EN_HOMOLOGATION' || institution.statut === 'ACTIF' ? institution.statut : 'ADHERENT'}`, { nom: institution.nom })}</InlineAlert>}
      errors={erreurs}
      presentation="dialog"
      submitting={ouvrir.isPending}
      serverError={erreur(ouvrir.error)}
      onCancel={onFermer}
      onSubmit={(v) => {
        const manquants: Record<string, string> = {}
        if (!texte(v.typeProjet)) manquants.typeProjet = t('cadrage.requis.typeProjet')
        if (!texte(v.chefDeProjetId)) manquants.chefDeProjetId = t('cadrage.requis.conducteur')
        const intitule = texte(v.intituleProjet).trim()
        const reference = texte(v.referenceOrigine).trim()
        if (intitule.length > 200) manquants.intituleProjet = t('institution.erreur.longueur', { max: 200 })
        if (reference && (reference.length > 50 || !REFERENCE_ORIGINE.test(reference))) manquants.referenceOrigine = t('cadrage.referenceOrigine.format')
        setErreurs(manquants)
        if (Object.keys(manquants).length) return
        ouvrir.mutate(
          {
            institutionId: institution.id,
            typeProjet: texte(v.typeProjet) as TypeProjet,
            chefDeProjetId: texte(v.chefDeProjetId),
            banquePartenaireId: texte(v.banquePartenaireId) || null,
            intituleProjet: intitule || null,
            referenceOrigine: reference || null,
          },
          // D-98 : la liaison ouverte mène à son dossier (parcours), où chaque acteur suit ses opérations.
          { onSuccess: (l) => void navigate(`/des/parcours/${encodeURIComponent(l.id)}?nouvelle=1`) },
        )
      }}
    />
  )
}

/** Onglet « Liaisons » de la fiche : liaisons de l'institution (filtre institutionId du service). */
export function OngletLiaisonsInstitution({ institution }: { institution: Institution }) {
  const { t } = useTranslation()
  const liaisons = useLiaisons({ page: 1, taille: 25, filtres: { institutionId: institution.id } })
  return (
    <LinkList
      label={t('institution.liaisons.liste')}
      empty={liaisons.isPending ? undefined : t('institution.liaisons.aucune')}
      items={(liaisons.data?.contenu ?? []).map((l) => ({
        key: l.id,
        href: `/des/liaisons/${l.id}`,
        // D-104 : plusieurs liaisons, y compris du même type ; l'intitulé du projet, s'il est donné, les distingue.
        label: `${l.intituleProjet ? `${l.intituleProjet}, ` : ''}${t(`typeProjet.${l.typeProjet}`, { defaultValue: String(l.typeProjet) })} (${l.id})`,
        meta: t(`phase.${l.phaseCourante}`, { defaultValue: l.phaseCourante }),
        status: etatLiaison[l.etat]?.status ?? 'neutral',
        statusLabel: t(etatLiaison[l.etat]?.label ?? l.etat),
      }))}
    />
  )
}
