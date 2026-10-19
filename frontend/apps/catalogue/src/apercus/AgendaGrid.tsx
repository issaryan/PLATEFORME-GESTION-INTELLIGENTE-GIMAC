import { AgendaGrid, type AgendaEvent } from '@gimac/design-system'

const jourDe = (iso: string) => new Date(new Date(iso).getTime() + 3_600_000).toISOString().slice(0, 10)
const minutesDe = (iso: string) => {
  const d = new Date(new Date(iso).getTime() + 3_600_000)
  return d.getUTCHours() * 60 + d.getUTCMinutes()
}
const heure = (iso: string) => new Date(new Date(iso).getTime() + 3_600_000).toISOString().slice(11, 16)
const jours = ['2026-10-19', '2026-10-20', '2026-10-21', '2026-10-22', '2026-10-23']
const libelles: Record<string, string> = { '2026-10-19': 'lun. 19 oct.', '2026-10-20': 'mar. 20 oct.', '2026-10-21': 'mer. 21 oct.', '2026-10-22': 'jeu. 22 oct.', '2026-10-23': 'ven. 23 oct.' }
const evenements: AgendaEvent[] = [
  { id: '1', titre: 'Revue hebdomadaire des liaisons', debut: '2026-10-19T09:00:00+01:00', fin: '2026-10-19T10:30:00+01:00', categorie: 'Réunion', couleur: 1, lieu: 'Salle 2' },
  { id: '2', titre: 'Atelier interfaces', debut: '2026-10-19T10:00:00+01:00', fin: '2026-10-19T12:00:00+01:00', categorie: 'Activité', couleur: 2 },
  { id: '3', titre: 'Comité de pilotage', debut: '2026-10-20T15:00:00+01:00', fin: '2026-10-20T16:30:00+01:00', categorie: 'Réunion', couleur: 1, enAttente: true },
  { id: '4', titre: 'Mission à Libreville', debut: '2026-10-21T00:00:00+01:00', fin: '2026-10-23T00:00:00+01:00', journeeEntiere: true, categorie: 'Déplacement', couleur: 4 },
  { id: '5', titre: 'Formation sécurité', debut: '2026-10-22T08:00:00+01:00', fin: '2026-10-22T12:00:00+01:00', categorie: 'Formation', couleur: 5 },
]

export default function Apercu() {
  return (
    <AgendaGrid vue="semaine" jours={jours} evenements={evenements} aujourdhui="2026-10-19" maintenant="2026-10-19T11:15:00+01:00" heureDebut={8} heureFin={17}
      label="Semaine 43" libelleJour={(j) => libelles[j] ?? j} heure={heure} jourDe={jourDe} minutesDe={minutesDe} onOuvrir={() => {}} onCreer={() => {}} />
  )
}
