export const metadata = {
  title: 'Privacy Notice – YEP',
  description: 'How the Youth Empowerment Project collects, uses and protects your personal data.',
};

export default function PrivacyPage() {
  return (
    <article className="prose prose-slate dark:prose-invert mx-auto max-w-3xl" aria-label="Privacy notice">
      <h1>Privacy Notice</h1>
      <p className="lead">Last updated: November 2026</p>

      <section>
        <h2>Who we are</h2>
        <p>
          The Youth Empowerment Project (YEP) is a virtual exchange platform for young people aged 18–35
          in Uganda and East Africa. References to <em>&ldquo;YEP&rdquo;</em>, <em>&ldquo;we&rdquo;</em>,
          <em>&ldquo;us&rdquo;</em> or <em>&ldquo;our&rdquo;</em> in this notice refer to the YEP platform
          operators.
        </p>
      </section>

      <section>
        <h2>What data we collect</h2>
        <p>We collect only the minimum data needed to run the platform:</p>
        <ul>
          <li><strong>Account data</strong> – name, email address, hashed password, role, phone (optional).</li>
          <li><strong>Profile data</strong> – age, country, digital-skills level, institution, bio (all optional except age and country for participants).</li>
          <li><strong>Activity data</strong> – enrolments, attendance, assessment submissions, certificates.</li>
          <li><strong>Project data</strong> – team membership, impact project content, milestone progress, AI-use declarations.</li>
          <li><strong>Communication data</strong> – discussion posts and notifications.</li>
          <li><strong>Audit data</strong> – administrative actions logged for accountability.</li>
        </ul>
      </section>

      <section>
        <h2>How we use your data</h2>
        <ul>
          <li>To provide and personalise your learning experience.</li>
          <li>To assess progress and issue verifiable certificates.</li>
          <li>To send notifications about activities, sessions and project updates.</li>
          <li>To allow partner organisations to find talent (only with your explicit consent).</li>
          <li>To improve the platform and report aggregated, anonymised statistics to funders.</li>
        </ul>
      </section>

      <section>
        <h2>Partner-sharing consent</h2>
        <p>
          Your profile is <strong>never</strong> visible to partner organisations unless you explicitly
          switch on <em>"Share my profile with partners"</em> in your profile settings. Even then, your
          email address and phone number are never shared. You can withdraw consent at any time; your
          profile is immediately hidden from partner searches.
        </p>
      </section>

      <section>
        <h2>Data retention and deletion</h2>
        <p>
          You may request deletion of your account at any time. We anonymise your name and profile;
          discussion posts are shown as <em>"Former participant"</em>. Certificates already issued remain
          publicly verifiable using their certificate number.
        </p>
      </section>

      <section>
        <h2>Security</h2>
        <p>
          Passwords are stored as BCrypt hashes (strength 10). JWTs are signed with HS256. All
          communication uses HTTPS. The database is hosted on a managed, encrypted PostgreSQL service.
          We apply least-privilege access controls throughout.
        </p>
      </section>

      <section>
        <h2>Your rights</h2>
        <p>
          You have the right to access, correct, delete or export your personal data. Contact your
          facilitator or raise a request via your profile page.
        </p>
      </section>
    </article>
  );
}
