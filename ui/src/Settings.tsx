import { useEffect, useState } from 'react'

import {
  EmptyNote,
  Loaded,
  PageHeader,
  Pill,
  Section,
  SettingCard,
  useAsk,
} from 'ratchet-ui/components'

import { useRead } from './useRead.js'

/**
 * WHO THE WIKI IS FOR — the one input to this pipeline a person actually decides.
 *
 * <p>It was four names in a `Map.of` inside the class that runs the sweep. Everything else here was
 * a setting and the thing the whole run is about required editing Java and rebuilding an image.
 *
 * <p>A NAME, AND NOTHING ABOUT WHAT ELSE THE TEXT CALLS THEM. This page had a variants editor —
 * one line per alternative name, seeded from `gold/` — so a sweep would not miss "the orator". It
 * was removed because it makes the reading worse, not merely redundant.
 *
 * <p>Those strings are scoped in the file they came from: "the young man (ch XXIV)", "my friend
 * (ch XXIV, Prince Vasíli's address)". A roster asserts them across all 365 chapters, so "my
 * friend" becomes Pierre wherever anyone says it and "the young man" becomes Pierre wherever there
 * is a young man. The list does not fail to find him, it finds him WHERE HE IS NOT — and a false
 * appearance is worse than a missing one, because nothing downstream can tell it from a real one.
 *
 * <p>The `names` section already does this per chapter, which is the only scope that is true, and
 * keeps a `not` list for forms that mean somebody else there. Asking a person to maintain a worse
 * version of a fold's output was the wrong thing to put on a settings page.
 */
type Person = { slug: string; name: string }

export function Settings() {
  const saved = useRead<Person[]>('/api/roster')
  const [people, setPeople] = useState<Person[] | null>(null)

  // The saved roster is the starting point, once. Re-seeding on every render would discard the
  // reader's edits the moment anything else on the page re-rendered.
  useEffect(() => {
    if (saved.value !== null && people === null) {
      setPeople(saved.value)
    }
  }, [saved.value, people])

  const save = useAsk<Person[], { ok: boolean; why?: string; saved?: number }>({
    send: async (roster) => {
      const answered = await fetch('/api/roster', {
        method: 'PUT',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(roster),
      })
      if (!answered.ok) {
        throw new Error(`the server answered ${answered.status}`)
      }
      return (await answered.json()) as { ok: boolean; why?: string; saved?: number }
    },
    // THE SERVER'S REFUSAL IS THE READER'S SENTENCE. It answers 200 with ok:false for a roster it
    // will not take — an unreadable body, or an empty one that was not meant as empty — and a page
    // that only looked at the status would report a save that did not happen.
    read: (answer) => (answer.ok ? { landed: true } : { landed: false, why: answer.why }),
  })

  const dirty =
    people !== null && saved.value !== null
      && JSON.stringify(people) !== JSON.stringify(saved.value)

  return (
    <>
        <PageHeader
        title="Settings"
        subtitle="The characters this wiki is for, and what the text calls them"
        actions={
          <>
            {save.busy ? <Pill tone="running">saving…</Pill> : null}
            {save.landed && !dirty ? <Pill tone="good">saved</Pill> : null}
            {save.refused !== '' ? (
              <Pill tone="alarm" title={save.refused}>
                not saved
              </Pill>
            ) : null}
            <button
              type="button"
              disabled={!dirty || save.busy}
              onClick={() => people !== null && save.ask(people)}
              style={{
                border: '1px solid var(--accent-action)',
                background: dirty ? 'var(--accent-action)' : 'var(--bg-subtle)',
                color: dirty ? 'var(--accent-on-action)' : 'var(--text-tertiary)',
                padding: '6px 14px',
                cursor: dirty ? 'pointer' : 'default',
                font: 'inherit',
                fontSize: 13,
              }}
            >
              Save roster
            </button>
          </>
        }
      />
      <Loaded
      what="roster"
      failed={saved.failed}
      value={people}
      header={null}
    >
      {(roster) => (
        <>
          <Section title="The roster" gutter="body">
            <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '0 0 14px' }}>
              {roster.length} character{roster.length === 1 ? '' : 's'}. A sweep reads every chapter
              once per character, so this list is the largest single lever on what a run costs.
              What else the text calls them is not set here — the <code>names</code> section works
              that out per chapter, which is the only scope in which it is true.
            </p>
            {save.refused !== '' ? (
              <p style={{ color: 'var(--danger)', fontSize: 13 }}>
                Not saved: {save.refused}
              </p>
            ) : null}
            {roster.length === 0 ? (
              <EmptyNote>
                Nobody is on the roster, so a sweep would read the whole book and write nothing.
              </EmptyNote>
            ) : null}

            <div style={{ display: 'grid', gap: 12 }}>
              {roster.map((person, at) => (
                <SettingCard
                  key={person.slug}
                  title={person.name || person.slug}
                  provenance={person.slug}
                  changed={
                    saved.value === null
                    || JSON.stringify(saved.value[at]) !== JSON.stringify(person)
                  }
                >
                  <div style={{ display: 'grid', gap: 8 }}>
                    <label style={label}>
                      Name a reader sees
                      <input
                        value={person.name}
                        onChange={(e) =>
                          setPeople(edit(roster, at, { ...person, name: e.target.value }))
                        }
                        style={field}
                      />
                    </label>
                    <div>
                      <button
                        type="button"
                        onClick={() => setPeople(roster.filter((_, i) => i !== at))}
                        style={{
                          border: '1px solid var(--border-strong)',
                          background: 'none',
                          color: 'var(--danger)',
                          padding: '4px 10px',
                          cursor: 'pointer',
                          font: 'inherit',
                          fontSize: 12,
                        }}
                      >
                        Remove {person.name || person.slug}
                      </button>
                    </div>
                  </div>
                </SettingCard>
              ))}
            </div>
          </Section>

          <Section title="Add a character" gutter="body">
            <AddCharacter
              taken={roster.map((p) => p.slug)}
              onAdd={(person) => setPeople([...roster, person])}
            />
          </Section>
        </>
      )}
      </Loaded>
    </>
  )
}

function edit(roster: Person[], at: number, person: Person): Person[] {
  return roster.map((each, i) => (i === at ? person : each))
}

function AddCharacter({
  taken,
  onAdd,
}: {
  taken: string[]
  onAdd: (person: Person) => void
}) {
  const [name, setName] = useState('')

  // THE SLUG IS DERIVED AND SHOWN BEFORE IT IS COMMITTED, because it is a directory name and a
  // journal key: two characters sharing one do not read as a collision, they read as work already
  // done. The server disambiguates on its own; showing it here means nobody is surprised by it.
  const slug = slugOf(name)
  const clash = taken.includes(slug)

  return (
    <div style={{ display: 'flex', gap: 10, alignItems: 'flex-end', flexWrap: 'wrap' }}>
      <label style={{ ...label, minWidth: 320 }}>
        Name
        <input
          value={name}
          placeholder="Natásha Rostóva"
          onChange={(e) => setName(e.target.value)}
          style={field}
        />
      </label>
      <p style={{ color: 'var(--text-tertiary)', fontSize: 12, margin: '0 0 8px' }}>
        {name.trim() === '' ? ' ' : clash ? `${slug} is already taken` : slug}
      </p>
      <button
        type="button"
        disabled={name.trim() === '' || clash}
        onClick={() => {
          onAdd({ slug, name: name.trim() })
          setName('')
        }}
        style={{
          border: '1px solid var(--border-strong)',
          background: 'var(--bg-subtle)',
          padding: '6px 14px',
          marginBottom: 8,
          cursor: name.trim() === '' || clash ? 'default' : 'pointer',
          font: 'inherit',
          fontSize: 13,
        }}
      >
        Add
      </button>
    </div>
  )
}

/** The same folding the server does: accents folded rather than stripped, parentheticals dropped. */
function slugOf(name: string): string {
  const folded = name.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase()
  const cut = folded
    .replace(/\(.*?\)/g, ' ')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return cut === '' ? 'character' : cut
}

const label = {
  display: 'grid',
  gap: 4,
  fontSize: 12,
  color: 'var(--text-tertiary)',
} as const

const field = {
  border: '1px solid var(--border-soft)',
  background: 'var(--bg-elevated)',
  color: 'var(--text-primary)',
  padding: '6px 8px',
  font: 'inherit',
  fontSize: 14,
} as const
