'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { titleCase } from '@/lib/format';
import type { Project } from '@/lib/types';
import { Badge } from '@/components/ui/Badge';
import { LinkButton } from '@/components/ui/Button';
import { Card, CardDescription, CardTitle } from '@/components/ui/Card';
import { EmptyState, ErrorState, PageHeader, SpinnerRow } from '@/components/ui/Feedback';
import { Field, Select } from '@/components/ui/Form';

export default function ProjectsPage() {
  const { user } = useAuth();
  const [status, setStatus] = useState('');

  const scope =
    user?.role === 'PARTICIPANT' ? '/api/v1/projects/my' : user?.role === 'MENTOR' ? '/api/v1/projects/mentored' : '/api/v1/projects/showcase';

  const projects = useQuery({
    queryKey: ['projects', scope],
    queryFn: () => api<Project[]>(scope),
    enabled: !!user,
  });

  const visible = (projects.data ?? []).filter((project) => !status || project.status === status);

  return (
    <>
      <PageHeader
        title="Impact projects"
        description={
          user?.role === 'PARTICIPANT'
            ? 'Your teams’ projects. Document a problem, propose a solution, add milestones and submit for human review.'
            : user?.role === 'MENTOR'
              ? 'Projects from teams you mentor. Review drafts and help teams reach approval.'
              : 'Approved impact projects from YEP teams.'
        }
        actions={
          <LinkButton href="/exchanges" size="sm" variant="secondary">
            Level Two activities
          </LinkButton>
        }
      />

      <form className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 sm:grid-cols-3 dark:border-slate-800 dark:bg-slate-900" onSubmit={(event) => event.preventDefault()}>
        <Field label="Status" htmlFor="project-status">
          <Select id="project-status" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="">All statuses</option>
            {['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'COMPLETED'].map((value) => (
              <option key={value} value={value}>
                {titleCase(value)}
              </option>
            ))}
          </Select>
        </Field>
      </form>

      {!user ? (
        <EmptyState title="Sign in to see your projects" description="Your team’s impact project appears here once you create one." />
      ) : projects.isLoading ? (
        <SpinnerRow label="Loading projects…" />
      ) : projects.isError ? (
        <ErrorState
          message={projects.error instanceof ApiError ? projects.error.message : 'Could not load projects.'}
          onRetry={() => projects.refetch()}
        />
      ) : visible.length === 0 ? (
        <EmptyState
          title="No projects here yet"
          description={
            user.role === 'PARTICIPANT'
              ? 'Open one of your teams and create an impact project draft.'
              : 'Projects will appear here once teams start submitting them.'
          }
          action={
            user.role === 'PARTICIPANT' ? (
              <LinkButton href="/teams" size="sm">
                Go to my teams
              </LinkButton>
            ) : undefined
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {visible.map((project) => (
            <Card key={project.projectId} as="article">
              <div className="flex items-start justify-between gap-3">
                <CardTitle>
                  <Link href={`/projects/${project.projectId}`} className="hover:text-brand-700 hover:underline dark:hover:text-brand-300">
                    {project.title}
                  </Link>
                </CardTitle>
                <Badge tone={project.status}>{titleCase(project.status)}</Badge>
              </div>
              <CardDescription>
                Team {project.teamName} · {titleCase(project.challengeArea)}
              </CardDescription>
              <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-slate-300">
                <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{project.progressPercent}% milestones done</span>
                <span className="rounded-full bg-slate-100 px-2.5 py-1 dark:bg-slate-800">{project.milestones.length} milestones</span>
                {project.aiUsed && <Badge tone="DRAFT">AI-assisted draft</Badge>}
                {project.overdue && <Badge tone="CANCELLED">Overdue</Badge>}
              </div>
              <div className="mt-4">
                <LinkButton href={`/projects/${project.projectId}`} size="sm" variant="secondary">
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
