import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CrudDialog, KeyValueList, Note, SearchSelect, Select, Tag } from '@gimac/design-system'
import { useMessageErreur, type Compte, type MessageErreur } from '@gimac/compte'
import type { Unite } from '@gimac/lot1'
import { useComptes, useCompte, useEffet, useUnitesOrganisation } from '../hooks'
import { Section } from './communs'

// Rattachement d'un compte (D-76, docs/lot1/mon-activite.md §1.2) : unité la plus fine (pôle, service ou
// département) et responsable (n+1) choisi dans l'annuaire des comptes, en fenêtre modale. MS-IAM refait
// toutes les règles : unité active (ORG_UNITE_INCONNUE), responsable interne actif distinct du titulaire
// (ORG_RESPONSABLE_INVALIDE), aucune boucle hiérarchique (ORG_CYCLE), compte d'institution exclu (ORG_COMPTE_EXTERNE).

/** Libellé d'une unité et le chemin de ses parents (« Pôle réseau, Service Production informatique »). */
export function libelleUnite(unites: Unite[], code: string | null | undefined): string {
  const u = unites.find((x) => x.code === code)
  if (!u) return code ?? ''
  const parent = unites.find((x) => x.code === u.parent)
  return parent ? `${u.libelle} (${parent.libelle})` : u.libelle
}

/** Unités dans l'ordre de l'arbre (parents d'abord), avec leur profondeur. */
export function enArbre(unites: Unite[]): { unite: Unite; profondeur: number }[] {
  const l: { unite: Unite; profondeur: number }[] = []
  const visiter = (parent: string | null, profondeur: number) => unites
    .filter((u) => u.parent === parent || (parent === null && u.parent !== null && !unites.some((x) => x.code === u.parent)))
    .sort((a, b) => a.libelle.localeCompare(b.libelle, 'fr'))
    .forEach((u) => {
      if (l.some((x) => x.unite.code === u.code)) return
      l.push({ unite: u, profondeur })
      visiter(u.code, profondeur + 1)
    })
  visiter(null, 0)
  return l
}

const CHAMP: Record<string, 'unite' | 'responsable'> = { ORG_UNITE_INCONNUE: 'unite', ORG_RESPONSABLE_INVALIDE: 'responsable', ORG_CYCLE: 'responsable' }

function DialogueRattachement({ compte, onFermer, onFait }: { compte: Compte; onFermer: () => void; onFait: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const unites = useUnitesOrganisation()
  const rattacher = useEffet((s, r: { unite: string | null; responsableId: string | null }) => s.organisation.rattacher(compte.id, r))
  const actuel = useCompte(compte.responsableId ?? '', !!compte.responsableId)
  const [unite, setUnite] = useState(compte.unite ?? '')
  const [responsable, setResponsable] = useState<{ id: string; nom: string } | null>(null)
  const [recherche, setRecherche] = useState('')
  const [differee, setDifferee] = useState('')
  const [erreurs, setErreurs] = useState<{ unite?: string; responsable?: string }>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  useEffect(() => {
    const d = window.setTimeout(() => setDifferee(recherche.trim()), 250)
    return () => window.clearTimeout(d)
  }, [recherche])
  useEffect(() => {
    if (actuel.data) setResponsable((r) => r ?? { id: actuel.data.id, nom: actuel.data.nomAffiche })
  }, [actuel.data])
  // Annuaire des comptes : internes et actifs, hors du titulaire.
  const annuaire = useComptes({ page: 1, taille: 8, q: differee || undefined, etat: 'ACTIF' })
  const candidats = (annuaire.data?.contenu ?? []).filter((c) => c.id !== compte.id && !c.institutionId)
  const choix = enArbre((unites.data ?? []).filter((u) => u.active || u.code === compte.unite))
  const soumettre = async () => {
    setErreurs({})
    setErreur(null)
    try {
      await rattacher.mutateAsync({ unite: unite || null, responsableId: responsable?.id ?? null })
      onFait()
    } catch (x) {
      const m = message(x)
      const champ = m.code ? CHAMP[m.code] : undefined
      if (champ) setErreurs({ [champ]: m.texte })
      else setErreur(m)
    }
  }
  return (
    <CrudDialog
      context="edit"
      size="md"
      id="gm-dlg-rattachement"
      title={t('console.org.rattachement.modifier')}
      subject={`${compte.identifiant} · ${compte.nomAffiche}`}
      description={t('console.org.rattachement.texte')}
      onClose={onFermer}
      onSubmit={() => void soumettre()}
      submitLabel={t('console.org.rattachement.enregistrer')}
      submitting={rattacher.isPending}
      dirty={unite !== (compte.unite ?? '') || (responsable?.id ?? null) !== (compte.responsableId ?? null)}
      error={erreur ? { title: erreur.texte, correlationId: erreur.correlationId } : undefined}
    >
      <Select name="unite" label={t('console.org.rattachement.unite')} help={t('console.org.rattachement.uniteAide')} value={unite} onChange={setUnite} error={erreurs.unite}
        disabled={unites.isPending}
        options={[{ value: '', label: t('console.org.rattachement.sansUnite') }, ...choix.map(({ unite: u, profondeur }) => ({
          value: u.code, label: `${'  '.repeat(profondeur)}${u.libelle}${u.active ? '' : ` (${t('console.org.inactive')})`}`,
        }))]} />
      <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
        {responsable ? (
          <div className="gm-row" style={{ gap: 'var(--gm-space-2)' }} aria-label={t('console.org.rattachement.responsableChoisi')}>
            <span className="gm-field-label">{t('console.org.rattachement.responsable')}</span>
            <Tag onRemove={() => setResponsable(null)}>{responsable.nom}</Tag>
          </div>
        ) : null}
        <SearchSelect
          name="responsable"
          label={responsable ? t('console.org.rattachement.changerResponsable') : t('console.org.rattachement.responsable')}
          help={t('console.org.rattachement.responsableAide')}
          error={erreurs.responsable}
          query={recherche}
          onQuery={setRecherche}
          loading={annuaire.isFetching}
          options={candidats.map((c) => ({ value: c.id, code: c.identifiant, label: c.nomAffiche }))}
          onChange={(id) => {
            const c = candidats.find((x) => x.id === id)
            if (c) setResponsable({ id: c.id, nom: c.nomAffiche })
            setRecherche('')
          }}
        />
      </div>
    </CrudDialog>
  )
}

/** Bloc « Rattachement » de la fiche d'un compte : lecture, et modification avec IAM_ACCOUNT_EDIT. */
export function SectionRattachement({ compte, peutModifier, onFait }: { compte: Compte; peutModifier: boolean; onFait: () => void }) {
  const { t } = useTranslation()
  const unites = useUnitesOrganisation()
  const responsable = useCompte(compte.responsableId ?? '', !!compte.responsableId)
  const [ouvert, setOuvert] = useState(false)
  const externe = !!compte.institutionId
  return (
    <Section titre={t('console.org.rattachement.titre')} id="gm-rattachement"
      action={peutModifier && !externe ? <Button size="sm" icon="pen-line" onClick={() => setOuvert(true)}>{t('console.org.rattachement.modifier')}</Button> : undefined}>
      {externe ? <Note>{t('console.org.rattachement.externe')}</Note> : (
        <KeyValueList
          items={[
            { key: t('console.org.rattachement.unite'), value: compte.unite ? libelleUnite(unites.data ?? [], compte.unite) : t('console.org.rattachement.aucuneUnite') },
            {
              key: t('console.org.rattachement.responsable'),
              value: compte.responsableId ? (responsable.data ? `${responsable.data.nomAffiche} (${responsable.data.identifiant})` : compte.responsableId) : t('console.org.rattachement.aucunResponsable'),
            },
          ]}
        />
      )}
      {ouvert ? <DialogueRattachement compte={compte} onFermer={() => setOuvert(false)} onFait={() => { setOuvert(false); onFait() }} /> : null}
    </Section>
  )
}
