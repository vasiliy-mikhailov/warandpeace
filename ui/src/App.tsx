import 'ratchet-ui/tokens.css'
import './palette.css'
import { Chapters } from './Chapters.js'
import { Characters } from './Characters.js'
import { Chrome, usePath } from './Chrome.js'
import { Dashboard } from './Dashboard.js'
import { PageHeader, Section, EmptyNote } from 'ratchet-ui/components'

export function App() {
  const path = usePath()
  return (
    <Chrome path={path}>
      {path === '/' ? (
        <Characters />
      ) : path === '/chapters' ? (
        <Chapters />
      ) : path === '/dashboard' ? (
        <Dashboard />
      ) : (
        <>
          <PageHeader title="No such page" subtitle={path} />
          <Section title="" gutter="body">
            <EmptyNote>
              This wiki serves the characters, the chapters and the run. Nothing is at{' '}
              <code>{path}</code>.
            </EmptyNote>
          </Section>
        </>
      )}
    </Chrome>
  )
}
