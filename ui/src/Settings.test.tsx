import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'

import { Settings } from './Settings.js'

/**
 * THE ROSTER IS EDITABLE AND THE EDIT ACTUALLY LEAVES THE PAGE.
 *
 * A settings screen that renders is half a settings screen. The half that matters is the PUT, and
 * the half of THAT which is easy to get wrong is a server answering 200 with `ok:false` — which
 * this one does for a roster it will not take. A page that looked only at the status would report a
 * save that never happened, which is the worst outcome available to a settings screen.
 */
const ROSTER = [
  { slug: 'pierre-bezukhov', name: 'Pierre Bezúkhov' },
  { slug: 'prince-andrew-bolkonski', name: 'Prince Andrew Bolkónski' },
]

function serving(put: (body: string) => unknown) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    if (String(input) === '/api/roster' && init?.method === 'PUT') {
      return { ok: true, status: 200, json: async () => put(String(init.body)) } as Response
    }
    if (String(input) === '/api/roster') {
      return { ok: true, status: 200, json: async () => ROSTER } as Response
    }
    return { ok: false, status: 404, json: async () => ({}) } as Response
  })
}

describe('the roster is editable', () => {
  // Explicit, because vitest runs with globals:false here and testing-library's own
  // afterEach is only installed when the global hooks are.
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })
  beforeEach(() => vi.stubGlobal('fetch', serving(() => ({ ok: true, saved: 2 }))))

  it('shows every character, and offers no place to list their other names', async () => {
    render(<Settings />)

    await waitFor(() => expect(screen.getByDisplayValue('Pierre Bezúkhov')).toBeTruthy())
    expect(screen.getByText('Settings', { selector: 'h1' })).toBeTruthy()
    expect(screen.getByDisplayValue('Prince Andrew Bolkónski')).toBeTruthy()

    // THE ABSENCE IS THE ASSERTION. A variants editor here asserts a form across all 365 chapters,
    // so "my friend" becomes Pierre wherever anyone says it -- it finds him where he is not, and a
    // false appearance is worse than a missing one. The `names` section does it per chapter.
    expect(document.querySelectorAll('textarea').length).toBe(0)
    expect(screen.getByText(/works that out per chapter/)).toBeTruthy()
  })

  it('sends what was edited, and does not send until something changed', async () => {
    const sent: string[] = []
    vi.stubGlobal('fetch', serving((body) => {
      sent.push(body)
      return { ok: true, saved: 2 }
    }))
    render(<Settings />)

    // RE-QUERIED EVERY TIME, never held. The pills beside this button are conditional, so the
    // sibling set changes the moment anything becomes dirty and React hands back a different DOM
    // node; a reference captured before the edit is detached and reports the state it had then.
    const save = () => screen.getByRole('button', { name: 'Save roster' }) as HTMLButtonElement
    await screen.findByDisplayValue('Prince Andrew Bolkónski')
    expect(save().disabled).toBe(true)

    fireEvent.change(screen.getByDisplayValue('Prince Andrew Bolkónski'),
      { target: { value: 'Prince Andrew' } })
    await waitFor(() => expect(save().disabled).toBe(false))
    fireEvent.click(save())

    await waitFor(() => expect(sent.length).toBe(1))
    const put = JSON.parse(sent[0] ?? '[]') as typeof ROSTER
    expect(put[1]?.name).toBe('Prince Andrew')
    expect(put[0]?.name).toBe('Pierre Bezúkhov')
    expect(Object.keys(put[0] ?? {}).sort()).toEqual(['name', 'slug'])
  })

  it('says a refusal out loud instead of showing a save that did not happen', async () => {
    vi.stubGlobal('fetch', serving(() =>
      ({ ok: false, why: 'that roster had no readable characters in it' })))
    render(<Settings />)

    await screen.findByDisplayValue('Pierre Bezúkhov')
    fireEvent.change(screen.getByDisplayValue('Pierre Bezúkhov'), { target: { value: 'Pierre' } })
    await waitFor(() =>
      expect((screen.getByRole('button', { name: 'Save roster' }) as HTMLButtonElement).disabled)
        .toBe(false))
    fireEvent.click(screen.getByRole('button', { name: 'Save roster' }))

    await waitFor(() =>
      expect(screen.getByText(/no readable characters/)).toBeTruthy())
    expect(screen.queryByText('saved')).toBeNull()
  })

  it('adds a character, and shows the slug before it is committed', async () => {
    render(<Settings />)
    await screen.findByDisplayValue('Pierre Bezúkhov')

    fireEvent.change(screen.getByPlaceholderText('Natásha Rostóva'),
      { target: { value: 'Natásha Rostóva' } })
    // THE SLUG IS A PATH AND A JOURNAL KEY, so it is shown rather than discovered later.
    expect(screen.getByText('natasha-rostova')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Add' }))
    await waitFor(() => expect(screen.getByDisplayValue('Natásha Rostóva')).toBeTruthy())
  })

  it('refuses a name whose slug is already on the roster', async () => {
    render(<Settings />)
    await screen.findByDisplayValue('Pierre Bezúkhov')

    fireEvent.change(screen.getByPlaceholderText('Natásha Rostóva'),
      { target: { value: 'Pierre Bezúkhov' } })

    expect(screen.getByText(/already taken/)).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Add' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('removes a character', async () => {
    render(<Settings />)

    fireEvent.click(await screen.findByRole('button', { name: /Remove Prince Andrew/ }))

    await waitFor(() =>
      expect(screen.queryByDisplayValue('Prince Andrew Bolkónski')).toBeNull())
    expect(screen.getByDisplayValue('Pierre Bezúkhov')).toBeTruthy()
  })
})
