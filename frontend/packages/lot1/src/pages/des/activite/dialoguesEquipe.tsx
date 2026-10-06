import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CheckboxGroup, CrudDialog, Select, TextArea, TextField } from '@gimac/design-system'
import { useSession } from '@gimac/socle'
import { MOTIF_DEMANDE, couverts, type MesPouvoirs, type NouvelleDemande, type Subordonne } from '../../../modele/pouvoirs'
import { useDemander, usePouvoirs, useRattacherSubordonne } from '../../../hooks/pouvoirs'
import { pourDialogue, useErreur, useLibelleUnite } from './communs'

// Demandes adressées à l'administration depuis « Mon équipe » (D-81 §4) : création d'un compte de subordonné,
// ajout ou retrait de profils, désactivation ou réactivation ; rattachement d'un subordonné (RATTACHEMENT_MODIFIER).
// Listes limitées à la portée de l'action dans la matrice des pouvoirs ; MS-IAM refait chaque contrôle
// (ORG_HORS_PERIMETRE, ORG_PROFIL_NON_DEMANDABLE, ORG_DEMANDE_EXISTANTE).

const IDENTIFIANT = /^[a-z][a-z0-9._-]{2,49}$/
const COURRIEL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

/**
 * Libellé d'un profil : celui du serveur (libellesProfils de GET /pouvoirs/moi, D-86 §5), sinon le catalogue de
 * « Mon compte », le code à défaut.
 */
export function useLibelleProfil() {
  const { t } = useTranslation()
  const libelles = usePouvoirs().data?.libellesProfils
  return (code: string) => libelles?.[code] || t(`compte.profil.${code}`, { defaultValue: code })
}

/** Motif de la demande : 5 à 500 caractères. */
function useMotif() {
  const { t } = useTranslation()
  const [motif, setMotif] = useState('')
  const erreur = motif.trim().length < MOTIF_DEMANDE.min || motif.trim().length > MOTIF_DEMANDE.max ? t('pvr.demande.erreur.motif', MOTIF_DEMANDE) : undefined
  const champ = (tente: boolean) => (
    <TextArea name="motif" label={t('pvr.demande.motif')} help={t('pvr.demande.motifAide')} required rows={3} maxLength={MOTIF_DEMANDE.max} value={motif} onChange={setMotif}
      error={tente ? erreur : undefined} />
  )
  return { motif: motif.trim(), erreur, champ, saisi: !!motif }
}

/** « Demander un compte » : identité, fonction, unité et n+1 dans ma portée, profils demandables, motif. */
export function DialogueDemandeCompte({ pouvoirs, unite, onFermer, onFait }: { pouvoirs: MesPouvoirs; unite?: string; onFermer: () => void; onFait: (m: string) => void }) {
  const { t } = useTranslation()
  const session = useSession()
  const erreur = useErreur()
  const libelleUnite = useLibelleUnite()
  const libelleProfil = useLibelleProfil()
  const demander = useDemander()
  const m = useMotif()
  const moi = session?.sujet ?? ''
  const [v, setV] = useState({ prenom: '', nom: '', identifiant: '', email: '', telephone: '', fonction: '', unite: unite ?? '', responsableId: moi, profils: [] as string[] })
  const [tente, setTente] = useState(false)
  const champ = (k: keyof typeof v) => (x: string | string[]) => setV((o) => ({ ...o, [k]: x }))
  const responsables = [{ id: moi, nomAffiche: t('pvr.moiMeme') }, ...couverts(pouvoirs, 'COMPTE_DEMANDER').filter((s) => s.actif)]
  const erreurs: Record<string, string | undefined> = {
    prenom: !v.prenom.trim() ? t('pvr.demande.erreur.obligatoire') : undefined,
    nom: !v.nom.trim() ? t('pvr.demande.erreur.obligatoire') : undefined,
    identifiant: !IDENTIFIANT.test(v.identifiant.trim()) ? t('pvr.demande.erreur.identifiant') : undefined,
    email: !COURRIEL.test(v.email.trim()) ? t('pvr.demande.erreur.email') : undefined,
    fonction: !v.fonction.trim() ? t('pvr.demande.erreur.obligatoire') : undefined,
    unite: !v.unite ? t('pvr.demande.erreur.unite') : undefined,
    profils: !v.profils.length ? t('pvr.demande.erreur.profils') : undefined,
    motif: m.erreur,
  }
  const e = (k: string) => (tente ? erreurs[k] : undefined)
  const soumettre = () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean)) return
    const d: NouvelleDemande = {
      type: 'CREATION_COMPTE', cibleId: null, motif: m.motif,
      contenu: {
        identifiant: v.identifiant.trim(), prenom: v.prenom.trim(), nom: v.nom.trim(), email: v.email.trim(), telephone: v.telephone.trim() || null, fonction: v.fonction.trim(),
        unite: v.unite, responsableId: v.responsableId, profils: v.profils,
      },
    }
    demander.mutate(d, { onSuccess: () => onFait(t('pvr.demande.deposee')) })
  }
  return (
    <CrudDialog context="create" size="lg" id="gm-dlg-demande-compte" title={t('pvr.compte.titre')} description={t('pvr.compte.texte')} onClose={onFermer} onSubmit={soumettre}
      submitLabel={t('pvr.demande.envoyer')} submitIcon="send" submitting={demander.isPending} dirty={m.saisi || !!(v.prenom || v.nom || v.identifiant || v.email)}
      error={pourDialogue(erreur(demander.error))}>
      <div className="gm-form-grid">
        <div className="gm-form-field"><TextField name="prenom" label={t('pvr.compte.prenom')} required maxLength={80} value={v.prenom} onChange={champ('prenom')} error={e('prenom')} /></div>
        <div className="gm-form-field"><TextField name="nom" label={t('pvr.compte.nom')} required maxLength={80} value={v.nom} onChange={champ('nom')} error={e('nom')} /></div>
        <div className="gm-form-field">
          <TextField name="identifiant" label={t('pvr.compte.identifiant')} help={t('pvr.compte.identifiantAide')} required mono maxLength={50} value={v.identifiant}
            onChange={(x) => champ('identifiant')(x.toLowerCase())} error={e('identifiant')} />
        </div>
        <div className="gm-form-field"><TextField name="email" type="email" label={t('pvr.compte.email')} required maxLength={120} value={v.email} onChange={champ('email')} error={e('email')} /></div>
        <div className="gm-form-field"><TextField name="telephone" label={t('pvr.compte.telephone')} maxLength={20} value={v.telephone} onChange={champ('telephone')} /></div>
        <div className="gm-form-field"><TextField name="fonction" label={t('pvr.compte.fonction')} required maxLength={120} value={v.fonction} onChange={champ('fonction')} error={e('fonction')} /></div>
        <div className="gm-form-field">
          <Select name="unite" label={t('pvr.compte.unite')} help={t('pvr.compte.uniteAide')} required value={v.unite} onChange={champ('unite')} error={e('unite')}
            options={[{ value: '', label: t('pvr.choisirUnite') }, ...pouvoirs.unitesDirigees.map((u) => ({ value: u, label: libelleUnite(u) }))]} />
        </div>
        <div className="gm-form-field">
          <Select name="responsableId" label={t('pvr.compte.responsable')} help={t('pvr.compte.responsableAide')} required value={v.responsableId} onChange={champ('responsableId')}
            options={responsables.map((p) => ({ value: p.id, label: p.nomAffiche }))} />
        </div>
      </div>
      <CheckboxGroup id="gm-demande-profils" label={t('pvr.compte.profils')} value={v.profils} onChange={champ('profils')} error={e('profils')}
        options={pouvoirs.profilsDemandables.map((p) => ({ value: p, label: libelleProfil(p) }))} />
      {m.champ(tente)}
    </CrudDialog>
  )
}

/** Demande d'ajout ou de retrait de profils pour un subordonné (PROFIL_DEMANDER). */
export function DialogueDemandeProfils({ pouvoirs, personne, onFermer, onFait }: { pouvoirs: MesPouvoirs; personne: Subordonne; onFermer: () => void; onFait: (m: string) => void }) {
  const { t } = useTranslation()
  const erreur = useErreur()
  const libelleProfil = useLibelleProfil()
  const demander = useDemander()
  const m = useMotif()
  const [ajouter, setAjouter] = useState<string[]>([])
  const [retirer, setRetirer] = useState<string[]>([])
  const [tente, setTente] = useState(false)
  const choix = !ajouter.length && !retirer.length ? t('pvr.profils.erreur.aucun') : ajouter.some((p) => retirer.includes(p)) ? t('pvr.profils.erreur.double') : undefined
  const options = pouvoirs.profilsDemandables.map((p) => ({ value: p, label: libelleProfil(p) }))
  return (
    <CrudDialog context="create" size="md" id="gm-dlg-demande-profils" title={t('pvr.profils.titre')} subject={personne.nomAffiche} description={t('pvr.profils.texte')} onClose={onFermer}
      onSubmit={() => {
        setTente(true)
        if (choix || m.erreur) return
        demander.mutate({ type: 'PROFILS', cibleId: personne.id, contenu: { ajouter, retirer }, motif: m.motif }, { onSuccess: () => onFait(t('pvr.demande.deposee')) })
      }}
      submitLabel={t('pvr.demande.envoyer')} submitIcon="send" submitting={demander.isPending} dirty={m.saisi || !!ajouter.length || !!retirer.length} error={pourDialogue(erreur(demander.error))}>
      <CheckboxGroup id="gm-profils-ajouter" label={t('pvr.profils.ajouter')} value={ajouter} onChange={setAjouter} options={options} error={tente ? choix : undefined} />
      <CheckboxGroup id="gm-profils-retirer" label={t('pvr.profils.retirer')} value={retirer} onChange={setRetirer} options={options} />
      {m.champ(tente)}
    </CrudDialog>
  )
}

/** Demande de désactivation (ou de réactivation) d'un subordonné (STATUT_DEMANDER). */
export function DialogueDemandeStatut({ personne, onFermer, onFait }: { personne: Subordonne; onFermer: () => void; onFait: (m: string) => void }) {
  const { t } = useTranslation()
  const erreur = useErreur()
  const demander = useDemander()
  const m = useMotif()
  const [tente, setTente] = useState(false)
  const cle = personne.actif ? 'desactivation' : 'reactivation'
  return (
    <CrudDialog context="create" size="md" id="gm-dlg-demande-statut" title={t(`pvr.statut.${cle}`)} subject={personne.nomAffiche} description={t(`pvr.statut.${cle}Texte`)} onClose={onFermer}
      danger={personne.actif}
      onSubmit={() => {
        setTente(true)
        if (m.erreur) return
        demander.mutate({ type: 'STATUT', cibleId: personne.id, contenu: { actif: !personne.actif }, motif: m.motif }, { onSuccess: () => onFait(t('pvr.demande.deposee')) })
      }}
      submitLabel={t('pvr.demande.envoyer')} submitIcon="send" submitting={demander.isPending} dirty={m.saisi} error={pourDialogue(erreur(demander.error))}>
      {m.champ(tente)}
    </CrudDialog>
  )
}

/** Rattachement d'un subordonné : unité parmi mes unités dirigées, n+1 dans mon équipe (RATTACHEMENT_MODIFIER). */
export function DialogueRattachement({ pouvoirs, personne, onFermer, onFait }: { pouvoirs: MesPouvoirs; personne: Subordonne; onFermer: () => void; onFait: (m: string) => void }) {
  const { t } = useTranslation()
  const session = useSession()
  const erreur = useErreur()
  const libelleUnite = useLibelleUnite()
  const rattacher = useRattacherSubordonne()
  const moi = session?.sujet ?? ''
  const initiale = { unite: personne.unite ?? '', responsableId: personne.responsableId ?? '' }
  const [v, setV] = useState(initiale)
  const [motif, setMotif] = useState('')
  const unites = [...new Set([...(personne.unite ? [personne.unite] : []), ...pouvoirs.unitesDirigees])]
  const responsables = [{ id: moi, nomAffiche: t('pvr.moiMeme') }, ...pouvoirs.subordonnes.filter((s) => s.niveau >= 1 && s.id !== personne.id && s.actif)]
  if (personne.responsableId && !responsables.some((r) => r.id === personne.responsableId)) responsables.push({ id: personne.responsableId, nomAffiche: personne.responsableId })
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-rattachement" title={t('pvr.rattachement.titre')} subject={personne.nomAffiche} description={t('pvr.rattachement.texte')} onClose={onFermer}
      onSubmit={() => rattacher.mutate({ id: personne.id, unite: v.unite || null, responsableId: v.responsableId || null, motif: motif.trim() || null }, { onSuccess: () => onFait(t('pvr.rattachement.fait', { nom: personne.nomAffiche })) })}
      submitLabel={t('pvr.rattachement.enregistrer')} submitting={rattacher.isPending} dirty={JSON.stringify(v) !== JSON.stringify(initiale)} error={pourDialogue(erreur(rattacher.error))}>
      <Select name="unite" label={t('pvr.compte.unite')} value={v.unite} onChange={(x) => setV((o) => ({ ...o, unite: x }))}
        options={unites.map((u) => ({ value: u, label: libelleUnite(u) }))} />
      <Select name="responsableId" label={t('pvr.compte.responsable')} value={v.responsableId} onChange={(x) => setV((o) => ({ ...o, responsableId: x }))}
        options={responsables.map((r) => ({ value: r.id, label: r.nomAffiche }))} />
      <TextField name="motif" label={t('pvr.rattachement.motif')} help={t('pvr.rattachement.motifAide')} value={motif} onChange={setMotif} maxLength={MOTIF_DEMANDE.max} />
    </CrudDialog>
  )
}
