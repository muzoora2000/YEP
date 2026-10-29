import type { NextRequest } from 'next/server';

export const dynamic = 'force-dynamic';
export const runtime = 'nodejs';

const FALLBACK_TARGET =
  process.env.NODE_ENV === 'development' ? 'http://localhost:8080' : 'https://yep-backend.onrender.com';

const FORWARDED_REQUEST_HEADERS = new Set([
  'accept',
  'accept-language',
  'authorization',
  'content-type',
  'cookie',
  'user-agent',
  'x-requested-with',
]);

const SKIPPED_RESPONSE_HEADERS = new Set([
  'connection',
  'content-encoding',
  'content-length',
  'keep-alive',
  'set-cookie',
  'transfer-encoding',
]);

function resolveTarget(): string {
  const configured = process.env.API_PROXY_TARGET ?? process.env.NEXT_PUBLIC_API_URL;
  const target = configured?.trim() || FALLBACK_TARGET;
  return target.replace(/\/+$/, '');
}

function getSetCookies(headers: Headers): string[] {
  const withCookies = headers as Headers & { getSetCookie?: () => string[] };
  return withCookies.getSetCookie?.() ?? [];
}

async function handle(request: NextRequest, context: { params: { path: string[] } }): Promise<Response> {
  const path = context.params.path.map((segment) => encodeURIComponent(segment)).join('/');
  const upstreamUrl = `${resolveTarget()}/api/${path}${request.nextUrl.search}`;

  const headers = new Headers();
  request.headers.forEach((value, key) => {
    if (FORWARDED_REQUEST_HEADERS.has(key.toLowerCase())) {
      headers.set(key, value);
    }
  });

  const method = request.method.toUpperCase();
  const body = method === 'GET' || method === 'HEAD' ? undefined : await request.arrayBuffer();

  let upstream: Response;
  try {
    upstream = await fetch(upstreamUrl, {
      method,
      headers,
      body,
      redirect: 'manual',
      cache: 'no-store',
    });
  } catch {
    return new Response(
      JSON.stringify({ code: 'UPSTREAM_UNAVAILABLE', message: 'Could not reach the YEP API.' }),
      { status: 502, headers: { 'Content-Type': 'application/json' } },
    );
  }

  const responseHeaders = new Headers();
  upstream.headers.forEach((value, key) => {
    if (!SKIPPED_RESPONSE_HEADERS.has(key.toLowerCase())) {
      responseHeaders.append(key, value);
    }
  });

  for (const cookie of getSetCookies(upstream.headers)) {
    responseHeaders.append('set-cookie', cookie);
  }

  return new Response(await upstream.arrayBuffer(), {
    status: upstream.status,
    statusText: upstream.statusText,
    headers: responseHeaders,
  });
}

export {
  handle as DELETE,
  handle as GET,
  handle as HEAD,
  handle as OPTIONS,
  handle as PATCH,
  handle as POST,
  handle as PUT,
};
