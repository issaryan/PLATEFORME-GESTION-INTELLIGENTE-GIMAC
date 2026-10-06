import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import {
  Button, Checkbox, CrudDialog, DataTable, Dialog, Icon, InlineAlert, KeyValueList, PermissionPicker, Select, StatusBadge, Tabs, TextArea, TextField, type Column, type PermissionGroup,
} from '@gimac/design-system'
import { satisfait } from '@gimac/socle'
import {
  ACTIONS_POUVOIR, CATALOGUE_DROITS, FONCTIONNALITES, GROUPES, basculerDroit, basculerFonctionnalite, completerActions, etatFonctionnalite, permissionPrivilegiee,
  type FonctionnaliteDroits, type ModuleDroits, type Portee,
} from '@gimac/lot1'
import type { Compte } from '@gimac/compte'
import { useComptes, useLibelleProfil, useMatricePouvoirs, usePermissionsRealm, useUnitesOrganisation } from '../hooks'
import { DOMAINES_PERMISSIONS, LIMITES_PROFIL, type Profil } from '../modele/configuration'
import { useChemins } from '../services/useServicesConsole'
import { libelleUnite } from './rattachement'

// Profils composés par fonctionnalité (D-90, docs/lot1/roles-fonctionnalites.md) et présentation des profils (D-86 §2) :
// fiche d'un profil en grande modale (onglets Fonctionnalités, Droits, Pouvoirs, Organisation), arbre Module >
// Fonctionnalité > Droit à cases, aperçu de la barre latérale du profil, permissions brutes en vue avancée, comparaison
// de deux profils, modification du libellé et de la description. L'enregistrement de la composition passe par la
// route D-62 (motif et clé de sécurité) ; le catalogue des droits est local au frontend (registre D-79).

/** Permissions du domaine, groupées par préfixe (HOM, CER, PAR, PIL, COL, NOT, IAM, PLATFORM). */
export function groupesPermissions(permissions: string[], libelle: (d: string) => string): PermissionGroup[] {
  return DOMAINES_PERMISSIONS.map((d) => ({
    key: d,
    label: libelle(d),
    options: permissions.filter((p) => p.startsWith(`${d}_`)).sort().map((p) => ({ value: p, label: p })),
  })).filter((g) => g.options.length > 0)
}

/** Composition présentée (les permissions internes des services ne le sont jamais). */
export const compositionDe = (p: Pick<Profil, 'permissions'>) => p.permissions.filter((x) => !x.startsWith('INTERNAL_')).sort()

// ------------------------------------------------------------------ Arbre des fonctionnalités

function CaseTroisEtats({ id, etat, label, disabled, onBasculer }: { id: string; etat: 'tout' | 'partiel' | 'aucun'; label: string; disabled?: boolean; onBasculer: () => void }) {
  const ref = useRef<HTMLInputElement>(null)
  useEffect(() => {
    if (ref.current) ref.current.indeterminate = etat === 'partiel'
  }, [etat])
  return (
    <label className="gm-check" htmlFor={id}>
      <input ref={ref} id={id} type="checkbox" checked={etat === 'tout'} aria-checked={etat === 'partiel' ? 'mixed' : etat === 'tout'} disabled={disabled} onChange={onBasculer} />
      <span>{label}</span>
    </label>
  )
}

function ItemFonctionnalite({ f, permissions, lecture, onChange }: { f: FonctionnaliteDroits; permissions: string[]; lecture: boolean; onChange: (p: string[]) => void }) {
  const { t } = useTranslation()
  const [ouvert, setOuvert] = useState(false)
  const id = useId().replace(/:/g, '')
  const etat = etatFonctionnalite(f, permissions)
  const nom = t(f.libelleKey)
  const n = f.droits.filter((d) => permissions.includes(d.permission)).length
  return (
    <li className="gm-fct-item">
      <div className="gm-fct-ligne">
        <button type="button" className="gm-fct-ouvrir" aria-expanded={ouvert} aria-controls={`gm-fct-${id}`} aria-label={t('console.profils.fct.droitsDe', { fonctionnalite: nom })} onClick={() => setOuvert((x) => !x)}>
          <Icon name="chevron-right" size={16} />
        </button>
        <Icon name={f.icone} size={16} />
        {/* Aucune → la consultation est cochée ; partielle ou complète → la fonctionnalité est décochée. */}
        <CaseTroisEtats id={`gm-fct-case-${id}`} etat={etat} label={nom} disabled={lecture} onBasculer={() => onChange(basculerFonctionnalite(permissions, f, etat === 'aucun'))} />
        <span className="gm-fct-compte">{t('console.profils.fct.compte', { n, total: f.droits.length })}</span>
      </div>
      {ouvert ? (
        <ul className="gm-fct-droits" id={`gm-fct-${id}`} aria-label={t('console.profils.fct.droitsDe', { fonctionnalite: nom })}>
          {f.droits.map((d) => (
            <li key={`${d.nature}-${d.permission}`} className="gm-fct-droit">
              <label className="gm-check">
                <input type="checkbox" checked={permissions.includes(d.permission)} disabled={lecture}
                  aria-label={`${t(`droits.nature.${d.nature}`)} : ${t(`droits.permission.${d.permission}`)} (${d.permission})`}
                  onChange={(e) => onChange(basculerDroit(permissions, f, d, e.target.checked))} />
                <span>
                  <span><span className="gm-fct-nature">{t(`droits.nature.${d.nature}`)}</span> · {t(`droits.permission.${d.permission}`)}</span>
                  <span className="gm-fct-code">{d.permission}{permissionPrivilegiee(d.permission) ? ` · ${t('console.profils.privilegie')}` : ''}</span>
                </span>
              </label>
            </li>
          ))}
        </ul>
      ) : null}
    </li>
  )
}

/** Arbre Module > Fonctionnalité > Droit à cases ; cocher un droit d'écriture coche la consultation. */
export function ArbreFonctionnalites({ permissions, onChange, lecture = false, catalogue = CATALOGUE_DROITS }: {
  permissions: string[]; onChange: (p: string[]) => void; lecture?: boolean; catalogue?: ModuleDroits[]
}) {
  const { t } = useTranslation()
  return (
    <div className="gm-fct-modules" role="group" aria-label={t('console.profils.fct.arbre')}>
      {catalogue.map((m) => (
        <section key={m.cle} className="gm-fct-module" aria-labelledby={`gm-fct-module-${m.cle}`}>
          <h3 id={`gm-fct-module-${m.cle}`}>{t(m.libelleKey)}</h3>
          <ul className="gm-fct-liste">
            {m.fonctionnalites.map((f) => <ItemFonctionnalite key={f.cle} f={f} permissions={permissions} lecture={lecture} onChange={onChange} />)}
          </ul>
        </section>
      ))}
    </div>
  )
}

/** Barre latérale de l'application interne qu'ouvrirait la composition (registre D-79), comparée à la composition de référence. */
export function ApercuBarreLaterale({ permissions, reference }: { permissions: string[]; reference?: string[] }) {
  const { t } = useTranslation()
  const ouverte = (p: string[] | undefined, f: (typeof FONCTIONNALITES)[number]) => satisfait(p, f.predicat)
  const groupes = GROUPES.map((g) => ({
    g,
    entrees: FONCTIONNALITES.filter((f) => f.groupe === g.cle && (ouverte(permissions, f) || (reference && ouverte(reference, f)))),
  })).filter((x) => x.entrees.length)
  const visibles = FONCTIONNALITES.filter((f) => ouverte(permissions, f)).length
  const id = `gm-nav-apercu-${useId().replace(/:/g, '')}`
  return (
    <aside className="gm-nav-apercu" aria-labelledby={id}>
      <h3 id={id}>{t('console.profils.apercu.titre')}</h3>
      <p className="gm-field-help" style={{ margin: 0 }} role="status">{t('console.profils.apercu.compte', { count: visibles })}</p>
      {groupes.map(({ g, entrees }) => (
        <div key={g.cle}>
          {g.libelleKey ? <p className="gm-nav-apercu-groupe">{t(g.libelleKey)}</p> : null}
          <ul>
            {entrees.map((f) => {
              const maintenant = ouverte(permissions, f)
              const avant = reference ? ouverte(reference, f) : maintenant
              const classe = maintenant && !avant ? 'gm-nav-apercu-nouveau' : !maintenant && avant ? 'gm-nav-apercu-retire' : undefined
              return (
                <li key={f.cle} className={classe}>
                  <Icon name={f.icone} size={16} />
                  <span>{t(f.libelleKey)}</span>
                  {classe ? <span className="gm-sr-only">{t(maintenant ? 'console.profils.apercu.ajoutee' : 'console.profils.apercu.retiree')}</span> : null}
                </li>
              )
            })}
          </ul>
        </div>
      ))}
    </aside>
  )
}

// ------------------------------------------------------------------ Fiche d'un profil

const STATUT_PORTEE: Record<Portee, 'neutral' | 'info' | 'warn' | 'ok'> = { AUCUNE: 'neutral', DIRECTS: 'info', EQUIPE: 'warn', UNITES: 'ok' }
export function BadgePortee({ portee }: { portee: Portee }) {
  const { t } = useTranslation()
  return <StatusBadge status={STATUT_PORTEE[portee]} icon={null} label={t(`console.matrice.portee.${portee}`)} />
}

function OngletPouvoirs({ profil }: { profil: Profil }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { base } = useChemins()
  const libelleProfil = useLibelleProfil()
  const matrice = useMatricePouvoirs()
  const ligne = matrice.data?.profils.find((l) => l.profil === profil.code)
  if (matrice.isPending) return <p className="gm-field-help" role="status">{t('console.profils.chargement')}</p>
  return (
    <div className="gm-col">
      <p className="gm-field-help" style={{ margin: 0 }}>{t('console.profils.onglet.pouvoirsTexte')}</p>
      {ligne ? (
        <KeyValueList items={[
          ...ACTIONS_POUVOIR.map((a) => ({ key: t(`console.matrice.action.${a}`), value: <BadgePortee portee={completerActions(ligne.actions)[a]} /> })),
          { key: t('console.matrice.demandables'), value: ligne.profilsDemandables.length ? ligne.profilsDemandables.map((p) => libelleProfil(p)).join(', ') : t('console.profils.aucunDemandable') },
        ]} />
      ) : <InlineAlert level="info" title={t('console.profils.sansLigne')} />}
      <div><Button size="sm" icon="scale" onClick={() => navigate(`${base}/matrice-pouvoirs?profil=${encodeURIComponent(profil.code)}`)}>{t('console.profils.versMatrice')}</Button></div>
    </div>
  )
}

function OngletOrganisation({ profil }: { profil: Profil }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { base } = useChemins()
  const comptes = useComptes({ page: 1, taille: 100, profil: profil.code })
  const unites = useUnitesOrganisation()
  const tous = useComptes({ page: 1, taille: 100 })
  const l = comptes.data?.contenu ?? []
  const nom = (id: string | null | undefined) => (id ? tous.data?.contenu.find((c) => c.id === id)?.nomAffiche ?? id : t('console.org.rattachement.aucunResponsable'))
  const colonnes: Column<Compte>[] = [
    { key: 'nomAffiche', header: t('console.profils.org.titulaire'), render: (c) => <span className="gm-col" style={{ gap: 0 }}><span>{c.nomAffiche}</span><span className="gm-mono gm-type-small">{c.identifiant}</span></span> },
    { key: 'unite', header: t('console.org.rattachement.unite'), render: (c) => (c.unite ? libelleUnite(unites.data ?? [], c.unite) : t('console.org.rattachement.aucuneUnite')) },
    { key: 'responsableId', header: t('console.org.rattachement.responsable'), render: (c) => nom(c.responsableId) },
    { key: 'actif', header: t('console.org.unite.etat'), render: (c) => <StatusBadge status={c.actif ? 'ok' : 'neutral'} label={t(c.actif ? 'console.org.active' : 'console.org.inactive')} /> },
  ]
  return (
    <div className="gm-col">
      <p className="gm-field-help" style={{ margin: 0 }}>{t('console.profils.onglet.organisationTexte')}</p>
      <DataTable columns={colonnes} rows={l} rowKey={(c) => c.id} density="compact" pagination={false} state={comptes.isPending ? 'loading' : l.length ? 'ok' : 'empty'} emptyText={t('console.profils.org.aucun')} />
      <div><Button size="sm" icon="network" onClick={() => navigate(`${base}/organigramme`)}>{t('console.profils.versOrganigramme')}</Button></div>
    </div>
  )
}

type OngletFiche = 'fonctionnalites' | 'droits' | 'pouvoirs' | 'organisation'
const ONGLETS_FICHE: OngletFiche[] = ['fonctionnalites', 'droits', 'pouvoirs', 'organisation']

/**
 * Fiche d'un profil : composition par fonctionnalité (arbre et aperçu de la barre latérale), permissions brutes en vue
 * avancée, pouvoirs (ligne de la matrice D-81) et organisation (titulaires, unités, n+1).
 */
export function FicheProfil({ profil, modifiable, ongletInitial = 'fonctionnalites', onFermer, onContinuer }: {
  profil: Profil; modifiable: boolean; ongletInitial?: OngletFiche; onFermer: () => void; onContinuer: (p: string[]) => void
}) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const realm = usePermissionsRealm()
  const initiale = useMemo(() => compositionDe(profil), [profil])
  const [choix, setChoix] = useState<string[]>(initiale)
  const [onglet, setOnglet] = useState<OngletFiche>(ongletInitial)
  const dirty = modifiable && choix.join() !== initiale.join()
  const devientPrivilegie = !profil.privilegie && choix.some(permissionPrivilegiee) && !initiale.some(permissionPrivilegiee)
  const disponibles = [...new Set([...(realm.data ?? []), ...initiale])].filter((p) => !p.startsWith('INTERNAL_'))
  return (
    <Dialog open id="gm-dlg-composition" className="gm-dialog-xl" title={t('console.profils.fiche', { profil: libelleProfil(profil.code, profil.libelle) })}
      subtitle={<span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}><span className="gm-mono">{profil.code}</span>{profil.description ? <span>{profil.description}</span> : null}</span>}
      onClose={onFermer}
      footer={(
        <>
          <Button variant="secondary" onClick={onFermer}>{t(modifiable ? 'commun.annuler' : 'commun.fermer')}</Button>
          {modifiable ? (
            <Button variant="primary" icon="pen-line" disabled={!dirty} disabledReason={t('console.matrice.inchangee')} onClick={() => onContinuer([...choix].sort())}>{t('console.profils.enregistrer')}</Button>
          ) : null}
        </>
      )}>
      <div className="gm-col">
        {!modifiable ? <InlineAlert level="info" title={t('console.profils.composition.lecture')} /> : null}
        {devientPrivilegie ? <InlineAlert level="warn" title={t('console.profils.devientPrivilegie')} /> : null}
        <Tabs items={ONGLETS_FICHE.map((k) => ({ key: k, label: t(`console.profils.onglet.${k}`) }))} value={onglet} onChange={(k) => setOnglet(k as OngletFiche)} />
        <div role="tabpanel" aria-label={t(`console.profils.onglet.${onglet}`)} className="gm-col">
          {onglet === 'fonctionnalites' ? (
            <>
              <p className="gm-field-help" style={{ margin: 0 }}>{t(modifiable ? 'console.profils.fct.texte' : 'console.profils.fct.lecture')}</p>
              <div className="gm-fct">
                <ArbreFonctionnalites permissions={choix} onChange={setChoix} lecture={!modifiable} />
                <ApercuBarreLaterale permissions={choix} reference={dirty ? initiale : undefined} />
              </div>
            </>
          ) : null}
          {onglet === 'droits' ? (
            <>
              <p className="gm-field-help" style={{ margin: 0 }}>{t(modifiable ? 'console.profils.composition.texte' : 'console.profils.fct.lecture')}</p>
              <PermissionPicker label={t('console.profils.permissions')} readOnly={!modifiable} value={choix} onChange={(x) => setChoix([...x].sort())}
                groups={groupesPermissions(disponibles, (d) => t(`console.domaine.${d}`))} />
            </>
          ) : null}
          {onglet === 'pouvoirs' ? <OngletPouvoirs profil={profil} /> : null}
          {onglet === 'organisation' ? <OngletOrganisation profil={profil} /> : null}
        </div>
      </div>
    </Dialog>
  )
}

// ------------------------------------------------------------------ Présentation (libellé, description)

/** « Modifier le profil » (D-86 §2) : libellé 1 à 80, description 0 à 500 ; motif et clé de sécurité ensuite. */
export function DialoguePresentation({ profil, onFermer, onContinuer }: { profil: Profil; onFermer: () => void; onContinuer: (p: { libelle: string; description: string }) => void }) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const initiale = { libelle: libelleProfil(profil.code, profil.libelle), description: profil.description ?? '' }
  const [v, setV] = useState(initiale)
  const [tente, setTente] = useState(false)
  const erreurs = {
    libelle: !v.libelle.trim() || v.libelle.trim().length > LIMITES_PROFIL.libelleMax ? t('console.profils.erreur.libelle', { max: LIMITES_PROFIL.libelleMax }) : undefined,
    description: v.description.trim().length > LIMITES_PROFIL.descriptionMax ? t('console.profils.erreur.description', { max: LIMITES_PROFIL.descriptionMax }) : undefined,
  }
  const dirty = v.libelle !== initiale.libelle || v.description !== initiale.description
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-presentation-profil" title={t('console.profils.presentation.titre')} subject={<span className="gm-mono">{profil.code}</span>}
      description={t('console.profils.presentation.texte')} onClose={onFermer} reauth dirty={dirty} submitDisabled={!dirty} submitDisabledReason={t('console.matrice.inchangee')}
      submitLabel={t('console.profils.enregistrer')} submitIcon="pen-line"
      onSubmit={() => {
        setTente(true)
        if (erreurs.libelle || erreurs.description) return
        onContinuer({ libelle: v.libelle.trim(), description: v.description.trim() })
      }}>
      <TextField name="libelle" label={t('console.profils.libelle')} help={t('console.profils.libelleAide')} required maxLength={LIMITES_PROFIL.libelleMax} value={v.libelle}
        onChange={(x) => setV((o) => ({ ...o, libelle: x }))} error={tente ? erreurs.libelle : undefined} />
      <TextArea name="description" label={t('console.profils.description')} help={t('console.profils.descriptionAide')} rows={4} maxLength={LIMITES_PROFIL.descriptionMax} value={v.description}
        onChange={(x) => setV((o) => ({ ...o, description: x }))} error={tente ? erreurs.description : undefined} />
      {profil.privilegie ? <InlineAlert level="warn" title={t('console.profils.presentation.privilegie')} /> : null}
    </CrudDialog>
  )
}

// ------------------------------------------------------------------ Comparaison de deux profils

/** Comparaison de deux profils côte à côte : droits du catalogue, permissions hors catalogue, barre latérale. */
export function DialogueComparaison({ profils, onFermer }: { profils: Profil[]; onFermer: () => void }) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const [a, setA] = useState(profils[0]?.code ?? '')
  const [b, setB] = useState(profils[1]?.code ?? profils[0]?.code ?? '')
  const [differences, setDifferences] = useState(false)
  const pa = compositionDe(profils.find((p) => p.code === a) ?? { permissions: [] })
  const pb = compositionDe(profils.find((p) => p.code === b) ?? { permissions: [] })
  const nomA = libelleProfil(a)
  const nomB = libelleProfil(b)
  const nbDiff = new Set([...pa.filter((x) => !pb.includes(x)), ...pb.filter((x) => !pa.includes(x))]).size
  const options = profils.map((p) => ({ value: p.code, label: `${libelleProfil(p.code, p.libelle)} (${p.code})` }))
  const marque = (oui: boolean, nom: string) => <td className={oui ? 'gm-cmp-oui' : 'gm-cmp-non'} aria-label={`${nom} : ${t(oui ? 'console.profils.cmp.oui' : 'console.profils.cmp.non')}`}>{oui ? '✓' : '·'}</td>
  return (
    <Dialog open id="gm-dlg-comparaison" className="gm-dialog-xl" title={t('console.profils.cmp.titre')} description={t('console.profils.cmp.texte')} onClose={onFermer}
      footer={<Button variant="secondary" onClick={onFermer}>{t('commun.fermer')}</Button>}>
      <div className="gm-col">
        <div className="gm-form-grid">
          <div className="gm-form-field"><Select name="profilA" label={t('console.profils.cmp.a')} value={a} onChange={setA} options={options} /></div>
          <div className="gm-form-field"><Select name="profilB" label={t('console.profils.cmp.b')} value={b} onChange={setB} options={options} /></div>
        </div>
        <div className="gm-row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
          <span role="status">{t('console.profils.cmp.resume', { count: nbDiff })}</span>
          <Checkbox name="differences" label={t('console.profils.cmp.differences')} checked={differences} onChange={setDifferences} />
        </div>
        <table className="gm-cmp-table">
          <caption className="gm-sr-only">{t('console.profils.cmp.caption', { a: nomA, b: nomB })}</caption>
          <thead><tr><th scope="col">{t('console.profils.cmp.droit')}</th><th scope="col">{nomA}</th><th scope="col">{nomB}</th></tr></thead>
          {CATALOGUE_DROITS.map((m) => {
            const lignes = m.fonctionnalites.flatMap((f) => f.droits.map((d) => ({ f, d, ia: pa.includes(d.permission), ib: pb.includes(d.permission) })))
              .filter((x) => !differences || x.ia !== x.ib)
            if (!lignes.length) return null
            return (
              <tbody key={m.cle}>
                <tr><th scope="rowgroup" colSpan={3}>{t(m.libelleKey)}</th></tr>
                {lignes.map(({ f, d, ia, ib }) => (
                  <tr key={`${f.cle}-${d.permission}`} className={ia !== ib ? 'gm-cmp-diff' : undefined}>
                    <td>{t(f.libelleKey)} · {t(`droits.nature.${d.nature}`)} · {t(`droits.permission.${d.permission}`)} <span className="gm-fct-code">{d.permission}</span></td>
                    {marque(ia, nomA)}
                    {marque(ib, nomB)}
                  </tr>
                ))}
              </tbody>
            )
          })}
        </table>
        <div className="gm-fct" style={{ gridTemplateColumns: 'repeat(2, minmax(0, 1fr))' }}>
          <div><strong>{nomA}</strong><ApercuBarreLaterale permissions={pa} /></div>
          <div><strong>{nomB}</strong><ApercuBarreLaterale permissions={pb} /></div>
        </div>
      </div>
    </Dialog>
  )
}
