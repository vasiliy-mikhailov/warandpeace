import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'

import { SectionTabs } from 'ratchet-ui/components'
import type { SectionTab } from 'ratchet-ui/components'
import { checkManifest } from 'ratchet-ui/check'

import type { Badges, Manifest } from './api.js'
import { useRead } from './useRead.js'

/**
 * THE NAV IS THE SERVER'S, READ FROM THE MANIFEST RATHER THAN WRITTEN HERE.
 *
 * Two copies of a route table is how one of them starts advertising a page that was never built —
 * which is exactly what happened to this project: `Dash.NAV` listed three pages, a second list
 * beside it agreed they were served, health reported ok, and the server answered 404 for all three.
 * Both lists were hand-written and neither was the routing.
 *
 * So this row is built from `/api/manifest` and nothing here restates it. A nav item the server
 * stops publishing disappears from the page; one it adds appears. The page cannot drift from the
 * server because it has no opinion to drift with.
 */
export function Chrome({ path, children }: { path: string; children: ReactNode }) {
  const manifest = useRead<Manifest>('/api/manifest')
  const badges = useRead<Badges>('/api/badges')

  const complaints =
    manifest.value === null ? [] : checkManifest(manifest.value)

  const tabs: SectionTab[] =
    manifest.value === null
      ? []
      : manifest.value.nav.map((item) => ({
          label: countOf(item.badge, badges.value) === null
            ? item.label
            : `${item.label} ${countOf(item.badge, badges.value)}`,
          href: item.path,
          current: item.path === path,
        }))

  return (
    <div style={{ minHeight: '100vh' }}>
      <header
        style={{
          borderBottom: '1px solid var(--border-soft)',
          background: 'var(--bg-panel)',
        }}
      >
        <div style={{ maxWidth: 1100, margin: '0 auto', padding: '18px 24px 0' }}>
          <a
            href="/"
            style={{
              display: 'block',
              fontSize: 26,
              letterSpacing: '-0.01em',
              color: 'var(--text-primary)',
              textDecoration: 'none',
            }}
          >
            War&nbsp;and&nbsp;Peace
          </a>
          <p
            style={{
              margin: '2px 0 12px',
              color: 'var(--text-tertiary)',
              fontSize: 13,
              fontStyle: 'italic',
            }}
          >
            {manifest.value?.description ?? 'A fandom wiki built one chapter at a time'}
          </p>
          {tabs.length > 0 ? <SectionTabs tabs={tabs} label="Sections" /> : null}
        </div>
      </header>

      {/*
        A MANIFEST THAT DOES NOT VALIDATE IS SAID OUT LOUD RATHER THAN RENDERED AROUND. checkManifest
        is the reason ratchet-ui exists at all: the contract is policed at runtime, on the consuming
        side, because a server that publishes a badge nothing defines type-checks perfectly on both
        sides and is still broken.
      */}
      {complaints.length > 0 ? (
        <div
          role="alert"
          style={{
            maxWidth: 1100,
            margin: '12px auto 0',
            padding: '10px 14px',
            border: '1px solid var(--danger)',
            color: 'var(--danger)',
            background: 'var(--bg-card)',
            fontSize: 13,
          }}
        >
          The server's manifest does not satisfy the contract: {complaints.join('; ')}
        </div>
      ) : null}

      <main style={{ maxWidth: 1100, margin: '0 auto', padding: '20px 0 64px' }}>{children}</main>
    </div>
  )
}

function countOf(badge: string | null | undefined, badges: Badges | null): number | null {
  if (badge === null || badge === undefined || badges === null) {
    return null
  }
  const counts: Record<string, number> = {
    characters: badges.characters,
    chapters: badges.chapters,
    reading: badges.reading,
  }
  return counts[badge] ?? null
}

/**
 * CLIENT ROUTING WITHOUT A ROUTER, because three static paths do not need one.
 *
 * A dependency here would be a dependency in the image, in the lockfile and in the CVE feed, for a
 * switch over three strings. Links are ordinary anchors so that a middle-click, a bookmark and a
 * reload all behave; only same-origin left-clicks are intercepted.
 */
export function usePath(): string {
  const [path, setPath] = useState(window.location.pathname)

  useEffect(() => {
    const onPop = () => setPath(window.location.pathname)
    const onClick = (event: MouseEvent) => {
      if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey
          || event.shiftKey || event.altKey) {
        return
      }
      const anchor = (event.target as HTMLElement | null)?.closest('a')
      if (!anchor) {
        return
      }
      const href = anchor.getAttribute('href')
      if (href === null || !href.startsWith('/') || anchor.getAttribute('target') !== null) {
        return
      }
      event.preventDefault()
      window.history.pushState({}, '', href)
      setPath(href)
      window.scrollTo(0, 0)
    }
    window.addEventListener('popstate', onPop)
    document.addEventListener('click', onClick)
    return () => {
      window.removeEventListener('popstate', onPop)
      document.removeEventListener('click', onClick)
    }
  }, [])

  return path
}
