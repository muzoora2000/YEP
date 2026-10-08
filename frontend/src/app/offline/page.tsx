import { LinkButton } from '@/components/ui/Button';

export const metadata = {
  title: 'You are offline',
  description: 'The YEP offline shell could not reach the network.',
};

export default function OfflinePage() {
  return (
    <div className="mx-auto max-w-prose py-16 text-center">
      <h1 className="text-2xl font-bold text-slate-900">You are offline</h1>
      <p className="mt-3 text-sm text-slate-600">
        YEP could not reach the network. Your coursework, projects and messages will be here as soon as
        you reconnect.
      </p>
      <div className="mt-6 flex justify-center">
        <LinkButton href="/">Try again</LinkButton>
      </div>
    </div>
  );
}
