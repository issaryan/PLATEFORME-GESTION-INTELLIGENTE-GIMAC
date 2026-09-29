import { Link, PageSection, StatCard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <PageSection title="Homologation" actions={<Link href="#/PageSection">Voir tout</Link>}>
      <StatCard label="Liaisons en cadrage" value={2} status="info" statusLabel="à suivre" />
    </PageSection>
  )
}
