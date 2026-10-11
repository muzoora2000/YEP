'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Suspense, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { ApiError, api, withQuery } from '@/lib/api';
import { titleCase } from '@/lib/format';
import type { Project, ProjectStatus } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Select } from '@/components/ui/Form';

const statuses: Array<'' | ProjectStatus> = [
  '',
  'DRAFT',
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'COMPLETED',
  'REJECTED',
];

function ProjectsContent() {
  const params = useSearchParams();
  const [status, setStatus] = useState(params.get('status') ?? '');
  const [mine, setMine] = useState(params.get('mine') === 'true');

  const query = withQuery('/api/v1/projects', { status: status || undefined, mine: mine ? 'true' : undefined });
  const projects = useQuery({
    queryKey: ['projects', query],
    queryFn: () => api<Project[]>(query),
    placeholderData: (previous) => previous,
  });

  return (
    <>
      <PageHeader
        title="Impact projects"
        description="Every Level Two team documents a problem, proposes a solution and submits for human review."
        actions={<LinkButton href="/exchanges?level=LEVEL_TWO" size="sm" variant="secondary">Level Two programmes</LinkButton>}
      />

      <form
        className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-3 dark:border-slate-800 dark:bg-slate-900"
        onSubmit={(event) => event.preventDefault()}
        aria-label="Filter projects"
      >
        <Field label="Status" htmlFor="project-status">
          <Select id="project-status" value={status} onChange={(event) => setStatus(event.target.value)}>
            {statuses.map((value) => (
              <option key={value || 'all'} value={value}>
                {value ? titleCase(value) : 'Submitted for review'}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Scope" htmlFor="project-scope">
          <Select id="project-scope" value={mine ? 'mine' : 'all'} onChange={(event) => setMine(event.target.value === 'mine')}>
            <option value="all">All projects</option>
            <option value="mine">My projects</option>
          </Select>
        </Field>
      </form>

      {projects.isLoading ? (
        <SpinnerRow label="Loading projects…" />
      ) : projects.isError ? (
        <ErrorState
          message={projects.error instanceof ApiError ? projects.error.message : 'Could not load projects.'}
          onRetry={() => projects.refetch()}
        />
      ) : (projects.data ?? []).length === 0 ? (
        <EmptyState
          title="No projects here yet"
          description="Teams submit their impact projects for review, so this list fills up as programmes run."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {(projects.data ?? []).map((project) => (
            <Card key={project.id} as="article">
              <div className="flex items-start justify-between gap-3">
                <CardTitle>
                  <Link href={`/projects/${project.id}`} className="hover:text-brand-700 dark:hover:text-brand-300 hover:underline">
                    {project.title}
                  </Link>
                </CardTitle>
                <Badge tone={project.status}>{titleCase(project.status)}</Badge>
              </div>
              <CardDescription>
                {project.teamName ? `Team ${project.teamName}` : 'Unassigned team'}
                {project.focusArea ? ` · ${titleCase(project.focusArea)}` : ''}
              </CardDescription>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
                {project.submissionDate && <span className="rounded-full bg-slate-100 dark:bg-slate-800 px-2.5 py-1">Submitted</span>}
                {project.aiAssisted && <Badge tone="DRAFT">AI-assisted draft</Badge>}
              </div>
              <div className="mt-4">
                <LinkButton href={`/projects/${project.id}`} size="sm" variant="secondary">
                  Open project
                </LinkButton>
              </div>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}

export default function ProjectsPage() {
  return (
    <Suspense fallback={<SpinnerRow label="Loading projects…" />}>
      <ProjectsContent />
    </Suspense>
  );
}
