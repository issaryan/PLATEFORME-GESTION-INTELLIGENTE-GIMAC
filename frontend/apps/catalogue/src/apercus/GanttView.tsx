import { GanttView } from '@gimac/design-system'

export default function Apercu() {
  return (
    <GanttView
      start="01/09/2026"
      days={70}
      today={27}
      weeks={[{ day: 0, label: 'S36' }, { day: 7, label: 'S37' }, { day: 14, label: 'S38' }, { day: 21, label: 'S39' }, { day: 28, label: 'S40' }, { day: 35, label: 'S41' }, { day: 42, label: 'S42' }, { day: 49, label: 'S43' }, { day: 56, label: 'S44' }, { day: 63, label: 'S45' }]}
      milestones={[
        { key: 1, label: 'Cadrage et descripteurs', owner: 'Chef de projet', start: 0, duration: 14, done: 100, probability: 100 },
        { key: 2, label: 'Composants du catalogue', owner: 'Équipe de réalisation', start: 10, duration: 24, done: 80, critical: true, probability: 85 },
        { key: 3, label: 'Écrans du département, vague 1', owner: 'Équipe de réalisation', start: 30, duration: 21, done: 20, critical: true, probability: 62 },
        { key: 4, label: 'Portail des participants', owner: 'Équipe de réalisation', start: 34, duration: 18, slack: 7, probability: 70 },
        { key: 5, label: "Revue d'ergonomie et accessibilité", owner: 'Testeur', start: 51, duration: 10, critical: true, probability: 44 },
        { key: 6, label: 'Mise en recette', owner: 'Chef du DES', start: 61, duration: 5, critical: true, probability: 44 },
      ]}
    />
  )
}
