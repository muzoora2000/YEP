import { type NextRequest, NextResponse } from 'next/server';

/** Routes that never require authentication. */
const PUBLIC_ROUTES = new Set([
  '/',
  '/login',
  '/register',
  '/forgot-password',
  '/reset-password',
  '/responsible-ai',
  '/privacy',
  '/offline',
  '/certificates/verify',
]);

/** Prefix check for public dynamic routes. */
const PUBLIC_PREFIXES = ['/verify/', '/exchanges', '/opportunities', '/api/'];

/**
 * Middleware: redirect unauthenticated visitors to /login.
 * The backend enforces roles independently — this is a UX guard only.
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Always allow public paths and static assets
  if (
    PUBLIC_ROUTES.has(pathname) ||
    PUBLIC_PREFIXES.some((prefix) => pathname.startsWith(prefix)) ||
    pathname.startsWith('/_next') ||
    pathname.startsWith('/favicon') ||
    pathname.startsWith('/icon') ||
    pathname.startsWith('/manifest') ||
    pathname.startsWith('/sw.js')
  ) {
    return NextResponse.next();
  }

  // Check for JWT token in the yep_token cookie
  const token = request.cookies.get('yep_token')?.value;
  if (!token) {
    // Carry the original destination so login page can redirect back
    const loginUrl = request.nextUrl.clone();
    loginUrl.pathname = '/login';
    loginUrl.searchParams.set('next', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    // Apply to all routes except Next.js internals
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
};
