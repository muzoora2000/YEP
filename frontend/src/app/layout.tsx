import type { Metadata, Viewport } from 'next';

import { Navbar } from '@/components/Navbar';
import { Providers } from '@/components/Providers';

import './globals.css';

export const metadata: Metadata = {
  title: {
    default: 'YEP - Youth Digital Empowerment and Virtual Exchange Platform',
    template: '%s | YEP',
  },
  description:
    'YEP connects young people in Uganda and East Africa through Level One and Level Two virtual exchanges, impact projects, mentoring and verifiable certificates.',
  applicationName: 'YEP',
  manifest: '/manifest.webmanifest',
  icons: { icon: [{ url: '/icon.svg', type: 'image/svg+xml' }] },
  appleWebApp: { capable: true, title: 'YEP', statusBarStyle: 'default' },
  keywords: ['virtual exchange', 'youth', 'digital literacy', 'East Africa', 'impact projects'],
};

export const viewport: Viewport = {
  themeColor: '#4f46e5',
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="flex min-h-screen flex-col bg-slate-50 text-slate-900 antialiased">
        <Providers>
          <a
            href="#main"
            className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-brand-800 focus:shadow-lg"
          >
            Skip to content
          </a>
          <Navbar />
          <main id="main" className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 sm:py-8">
            {children}
          </main>
          <footer className="border-t border-slate-200 bg-white">
            <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-6 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-6">
              <p>YEP - Youth Digital Empowerment and Virtual Exchange Platform</p>
              <p>
                Responsible AI: AI-drafted content is always labelled and approved by a human facilitator
                before publishing.
              </p>
            </div>
          </footer>
        </Providers>
      </body>
    </html>
  );
}
