export const metadata = {
  title: 'Responsible AI Use – YEP',
  description: 'YEP guidelines on responsible and ethical AI use in impact projects and digital literacy activities.',
};

export default function ResponsibleAiPage() {
  return (
    <article className="prose prose-slate dark:prose-invert mx-auto max-w-3xl" aria-label="Responsible AI page">
      <h1>Responsible AI Use</h1>

      <p>
        YEP integrates AI literacy into every exchange. We are committed to transparent, human-centred and
        equitable use of artificial intelligence. This page explains what we expect of participants, mentors,
        facilitators and partners when AI tools are involved.
      </p>

      <section aria-labelledby="principles">
        <h2 id="principles">Our principles</h2>
        <ul>
          <li><strong>Human oversight</strong> – Any AI-generated content on this platform is labelled <em>"AI-generated"</em> and requires human review before it is used or published.</li>
          <li><strong>Transparency</strong> – Participants must declare AI use in their Impact Projects: whether AI was used (yes / no) and, if yes, a plain-language description of how.</li>
          <li><strong>No auto-grading by AI</strong> – Assignments are graded by facilitators or mentors, never by an automated AI system. Quizzes are auto-graded by deterministic scoring rules, not AI inference.</li>
          <li><strong>Privacy by default</strong> – We never feed personal data into third-party AI tools without explicit consent.</li>
          <li><strong>Fairness</strong> – AI tools may amplify bias. We ask all users to critically evaluate AI outputs before acting on them.</li>
        </ul>
      </section>

      <section aria-labelledby="participants">
        <h2 id="participants">For participants</h2>
        <p>
          When you use AI tools (ChatGPT, Gemini, Copilot, Perplexity, or any other) to help with
          your learning or project work:
        </p>
        <ol>
          <li>Disclose it. Use the <em>AI use declaration</em> field on your project submission.</li>
          <li>Understand it. Never submit AI output you do not understand and cannot explain.</li>
          <li>Critique it. AI can be wrong, biased or outdated. Cross-check facts with authoritative sources.</li>
          <li>Add value. Your contribution, judgement and lived experience are what assessors look for.</li>
        </ol>
      </section>

      <section aria-labelledby="mentors">
        <h2 id="mentors">For mentors and facilitators</h2>
        <p>
          When reviewing impact projects, check the AI use declaration. A project that used AI responsibly
          and transparently is not penalised; one that failed to disclose AI assistance may be returned
          for a declaration to be added before final approval.
        </p>
      </section>

      <section aria-labelledby="ai-literacy">
        <h2 id="ai-literacy">AI literacy on YEP</h2>
        <p>
          YEP&rsquo;s Level One programme includes an <em>AI Literacy</em> module. Every AI-literacy activity
          displays this Responsible AI guideline card and links to this page. We update our guidance as AI
          technology evolves; check back regularly.
        </p>
      </section>

      <section aria-labelledby="contact">
        <h2 id="contact">Questions?</h2>
        <p>
          Raise a concern in a discussion thread or contact your facilitator. Serious platform-level
          concerns about AI misuse can be flagged via the post-flagging feature; administrators review
          all flagged content.
        </p>
      </section>
    </article>
  );
}
