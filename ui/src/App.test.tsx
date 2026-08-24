import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'

import { App } from './App.js'

/**
 * THE PAGE ACTUALLY RENDERS, which is a different claim from "it compiles".
 *
 * This project shipped a release where the server type-checked, every Java test was green, the
 * manifest validated, {@code danglingNav()} reported the nav healthy — and the front door answered
 * {@code {"ok":false,"why":"no such endpoint: /"}}. The guard compared two hand-written lists that
 * agreed with each other and neither of them was the routing. Nothing in the tree had ever loaded
 * the site.
 *
 * <p>So these tests mount the real components against the real API shapes and assert what a reader
 * would see. A bundle that builds and throws on mount is the same failure one layer up, and it is
 * the failure a 200 on {@code /} cannot detect.
 */
const MANIFEST = {
  id: 'warandpeace',
  name: 'War and Peace',
  description: 'A fandom wiki built one chapter at a time',
  version: '0.1.0',
  basePath: '',
  assetPrefix: '',
  api: '/api',
  health: '/api/health',
  nav: [
    { label: 'Characters', path: '/', badge: 'characters' },
    { label: 'Chapters', path: '/chapters', badge: 'chapters' },
    { label: 'The run', path: '/dashboard', badge: 'reading' },
  ],
  badges: {
    characters: { endpoint: '/api/badges', field: 'characters' },
    chapters: { endpoint: '/api/badges', field: 'chapters' },
    reading: { endpoint: '/api/badges', field: 'reading' },
  },
}

// The empty state the deployment is actually in: a loaded corpus and no sweep yet.
const EMPTY = {
  '/api/manifest': MANIFEST,
  '/api/badges': { characters: 0, chapters: 365, reading: 0 },
  '/api/health': { ok: true, version: '0.1.0' },
  '/api/characters': [],
  '/api/chapters': [
    { slug: '001-book-one-chi', book: 'BOOK ONE', chapter: 'I', paragraphs: 42 },
    { slug: '002-book-one-chii', book: 'BOOK ONE', chapter: 'II', paragraphs: 18 },
    { slug: '300-book-four-chi', book: 'BOOK FOUR', chapter: 'I', paragraphs: 27 },
  ],
} as const

function serving(answers: Record<string, unknown>) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const path = String(input)
    if (!(path in answers)) {
      return { ok: false, status: 404, json: async () => ({}) } as Response
    }
    return { ok: true, status: 200, json: async () => answers[path] } as Response
  })
}

function at(path: string) {
  window.history.pushState({}, '', path)
}

describe('the wiki renders', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', serving(EMPTY as unknown as Record<string, unknown>))
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the characters page at the front door', async () => {
    at('/')
    render(<App />)

    expect(await screen.findByText('Characters', { selector: 'h1' })).toBeTruthy()
    // THE EMPTY STATE IS STATED, not left as a table of headings a reader takes for a failed load.
    await waitFor(() =>
      expect(screen.getByText(/No character has a page yet/)).toBeTruthy())
    expect(screen.getByText('chapters in the book')).toBeTruthy()
  })

  it('lists the chapters and the books they are in', async () => {
    at('/chapters')
    render(<App />)

    expect(await screen.findByText('Chapters', { selector: 'h1' })).toBeTruthy()
    // BOOK FOUR appears twice on purpose -- once as a filter and once in the table -- so this
    // counts rather than assuming one, which is what the first draft of this assertion did.
    await waitFor(() => expect(screen.getAllByText(/BOOK ONE/).length).toBeGreaterThan(1))
    expect(screen.getAllByText(/BOOK FOUR/).length).toBeGreaterThan(0)
  })

  it('reports the run, and says a zero is the resting state rather than a failure', async () => {
    at('/dashboard')
    render(<App />)

    expect(await screen.findByText('The run', { selector: 'h1' })).toBeTruthy()
    await waitFor(() => expect(screen.getByText(/Nothing has been read yet/)).toBeTruthy())
    expect(screen.getByText(/docker compose run --rm wp-sweep/)).toBeTruthy()
  })

  it('builds its nav from the manifest rather than from a list of its own', async () => {
    at('/')
    render(<App />)

    // The three labels carry their badge counts, which only the server knows. Queried as links,
    // because SectionTabs renders each label more than once and a bare text query cannot say which
    // of them is the nav.
    await waitFor(() =>
      expect(screen.getAllByRole('link', { name: /Chapters 365/ }).length).toBeGreaterThan(0))
    expect(screen.getAllByRole('link', { name: /Characters 0/ }).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', { name: /The run 0/ }).length).toBeGreaterThan(0)
    // AND THE HREFS ARE THE SERVER'S PATHS, which is the half that would catch a nav built here.
    expect(screen.getAllByRole('link', { name: /Chapters 365/ })[0]?.getAttribute('href'))
      .toBe('/chapters')
  })

  it('says so when a page cannot be read, rather than showing an empty frame', async () => {
    vi.stubGlobal('fetch', serving({ '/api/manifest': MANIFEST }))
    at('/')
    render(<App />)

    await waitFor(() =>
      expect(screen.getByText(/character list could not be read|\/api\/characters answered 404/))
        .toBeTruthy())
  })

  it('refuses a path nobody built instead of rendering a blank page', async () => {
    at('/findings')
    render(<App />)

    expect(await screen.findByText('No such page', { selector: 'h1' })).toBeTruthy()
  })
})
