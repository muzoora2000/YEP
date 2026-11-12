/**
 * Mirror of the Spring Boot DTO contracts (`/api/v1/**`).
 */

export type Role = 'PARTICIPANT' | 'FACILITATOR' | 'MENTOR' | 'ADMIN' | 'PARTNER';
export type AccountStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED';
export type ActivityLevel = 'ONE' | 'TWO';
export type ActivityStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'COMPLETED';
export type EnrollmentStatus = 'APPLIED' | 'ENROLLED' | 'REJECTED' | 'WAITLISTED' | 'COMPLETED' | 'CANCELLED';
export type TeamStatus = 'OPEN' | 'FULL' | 'LOCKED';
export type TeamMemberRole = 'LEADER' | 'MEMBER';
export type ChallengeArea =
  | 'EMPLOYMENT'
  | 'EDUCATION'
  | 'AGRICULTURE'
  | 'HEALTH'
  | 'CLIMATE_CHANGE'
  | 'DIGITAL_INCLUSION';
export type ProjectStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'CHANGES_REQUESTED' | 'APPROVED' | 'COMPLETED';
export type MilestoneStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';
export type ReviewDecision = 'COMMENT' | 'CHANGES_REQUESTED' | 'APPROVED';
export type SessionStatus = 'SCHEDULED' | 'LIVE' | 'COMPLETED' | 'CANCELLED';
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'EXCUSED';
export type MaterialType = 'VIDEO' | 'PDF' | 'LINK' | 'SLIDES' | 'TEXT';
export type TaskType = 'QUIZ' | 'ASSIGNMENT';
export type AssessmentStatus = 'SUBMITTED' | 'GRADED';
export type NotificationType = 'IN_APP' | 'EMAIL' | 'SMS';
export type OpportunityType = 'JOB' | 'INTERNSHIP' | 'MENTORSHIP' | 'SCHOLARSHIP' | 'TRAINING';
export type OpportunityStatus = 'OPEN' | 'CLOSED';
export type OrganisationType = 'NGO' | 'UNIVERSITY' | 'EMPLOYER' | 'DEVELOPMENT_AGENCY' | 'TECH_COMPANY';
export type SupportType = 'FUNDING' | 'MENTORSHIP' | 'RESOURCES' | 'VISIBILITY';
export type SupportStatus = 'OFFERED' | 'ACCEPTED' | 'DECLINED';

export interface User {
  userId: number;
  name: string;
  email: string;
  role: Role;
  status: AccountStatus;
  phone: string | null;
  createdAt: string;
  lastLoginAt: string | null;
  age: number | null;
  country: string | null;
  digitalSkills: string | null;
  levelTwoEligible: boolean;
  institution: string | null;
  bio: string | null;
  shareProfileWithPartners: boolean;
  consentDate: string | null;
  specialisation: string | null;
  organisation: string | null;
  expertise: string | null;
  department: string | null;
  organisationName: string | null;
  organisationType: OrganisationType | null;
  website: string | null;
  description: string | null;
  verified: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface RegisterResponse {
  userId: number;
  name: string;
  email: string;
  role: Role;
  status: AccountStatus;
  message: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Activity {
  activityId: number;
  level: ActivityLevel;
  title: string;
  description: string | null;
  startDate: string;
  endDate: string | null;
  category: string | null;
  capacity: number | null;
  status: ActivityStatus;
  passMark: number;
  minAttendancePercent: number;
  minTeamSize: number;
  maxTeamSize: number;
  facilitatorId: number;
  facilitatorName: string;
  partnerId: number | null;
  partnerName: string | null;
  enrolledCount: number;
  waitlistedCount: number;
  levelFeatures: string[];
}

export interface Enrollment {
  enrollmentId: number;
  activityId: number;
  activityTitle: string;
  youthId: number;
  youthName: string;
  youthEmail: string;
  status: EnrollmentStatus;
  enrollmentDate: string;
  completedAt: string | null;
}

export interface Session {
  sessionId: number;
  activityId: number;
  title: string;
  scheduledStart: string;
  durationMinutes: number;
  meetingLink: string;
  status: SessionStatus;
  checkInOpen: boolean;
}

export interface Attendance {
  attendanceId: number;
  sessionId: number;
  youthId: number;
  youthName: string;
  status: AttendanceStatus;
  markedAt: string;
}

export interface Material {
  materialId: number;
  activityId: number;
  title: string;
  materialType: MaterialType;
  url: string;
  description: string | null;
  uploadedByName: string;
  uploadedAt: string;
  published: boolean;
}

export interface Thread {
  threadId: number;
  activityId: number;
  teamId: number | null;
  topic: string;
  createdAt: string;
  postCount: number;
}

export interface Post {
  postId: number;
  threadId: number;
  authorId: number;
  authorName: string;
  content: string;
  postedAt: string;
  flagged: boolean;
  flagReason: string | null;
  hidden: boolean;
}

export interface Task {
  taskId: number;
  activityId: number;
  title: string;
  taskType: TaskType;
  instructions: string | null;
  questions: Record<string, unknown>[] | null;
  maxScore: number;
  dueDate: string | null;
  published: boolean;
}

export interface Assessment {
  assessmentId: number;
  taskId: number;
  taskTitle: string;
  activityId: number;
  youthId: number;
  youthName: string;
  score: number | null;
  maxScore: number;
  percentage: number;
  status: AssessmentStatus;
  feedback: string | null;
  answers: number[] | null;
  submissionText: string | null;
  assessmentDate: string;
  gradedByName: string | null;
}

export interface Certificate {
  certificateId: number;
  certificateNumber: string;
  youthId: number;
  youthName: string;
  activityId: number;
  activityTitle: string;
  achievement: string;
  issueDate: string;
  revoked: boolean;
}

export interface Progress {
  activityId: number;
  activityTitle: string;
  enrollmentStatus: EnrollmentStatus;
  totalSessions: number;
  attendedSessions: number;
  attendancePercent: number | null;
  gradedAssessments: number;
  assessmentAverage: number;
  passMark: number;
  minAttendancePercent: number;
  passedAssessments: boolean;
  passedAttendance: boolean;
  completed: boolean;
  levelTwoEligible: boolean;
  assessments: Assessment[];
  certificates: Certificate[];
}

export interface TeamMember {
  memberId: number;
  youthId: number;
  name: string;
  email: string;
  role: TeamMemberRole;
}

export interface Team {
  teamId: number;
  activityId: number;
  activityTitle: string;
  teamName: string;
  mentorId: number | null;
  mentorName: string | null;
  createdById: number;
  createdByName: string;
  status: TeamStatus;
  memberCount: number;
  minTeamSize: number;
  maxTeamSize: number;
  members: TeamMember[];
  projectId: number | null;
  projectTitle: string | null;
}

export interface Milestone {
  milestoneId: number;
  projectId: number;
  title: string;
  dueDate: string | null;
  status: MilestoneStatus;
  completedAt: string | null;
  overdue: boolean;
}

export interface Review {
  reviewId: number;
  projectId: number;
  mentorId: number;
  mentorName: string;
  decision: ReviewDecision;
  feedback: string;
  createdAt: string;
}

export interface Project {
  projectId: number;
  teamId: number;
  teamName: string;
  activityId: number;
  activityTitle: string;
  title: string;
  description: string | null;
  challengeArea: ChallengeArea;
  status: ProjectStatus;
  aiUsed: boolean;
  aiUsageDeclaration: string | null;
  submittedAt: string | null;
  openForSupport: boolean;
  progressPercent: number;
  overdue: boolean;
  milestones: Milestone[];
  reviews: Review[];
}

export interface ParticipantRow {
  youthId: number;
  name: string;
  country: string | null;
  status: EnrollmentStatus;
  attendancePercent: number | null;
  resultsPercent: number | null;
  lastActivityAt: string | null;
}

export interface Notification {
  notificationId: number;
  type: NotificationType;
  message: string;
  link: string | null;
  dateSent: string;
  read: boolean;
}

export interface UnreadCount {
  unread: number;
}

export interface Opportunity {
  opportunityId: number;
  partnerId: number;
  partnerName: string;
  organisationName: string;
  title: string;
  opportunityType: OpportunityType;
  description: string | null;
  location: string | null;
  deadline: string | null;
  status: OpportunityStatus;
  createdAt: string;
  interestCount: number;
}

export interface Interest {
  interestId: number;
  opportunityId: number;
  opportunityTitle: string;
  youthId: number;
  youthName: string;
  youthEmail: string;
  message: string | null;
  createdAt: string;
}

export interface Support {
  supportId: number;
  projectId: number;
  projectTitle: string;
  partnerId: number;
  partnerName: string;
  organisationName: string;
  supportType: SupportType;
  message: string | null;
  status: SupportStatus;
  createdAt: string;
}

export interface PartnerSummary {
  partnerId: number;
  name: string;
  email: string;
  organisationName: string;
  country: string | null;
  organisationType: OrganisationType | null;
  website: string | null;
  description: string | null;
  verified: boolean;
}

export interface PublicSettings {
  platformName: string;
  countries: string[];
}

export interface Dashboard {
  totalUsers: number;
  participants: number;
  facilitators: number;
  mentors: number;
  partners: number;
  administrators: number;
  pendingUsers: number;
  activeUsers: number;
  activities: number;
  publishedActivities: number;
  enrollments: number;
  teams: number;
  projects: number;
  approvedProjects: number;
  certificates: number;
  opportunities: number;
  verifiedPartners: number;
}

export interface Setting {
  key: string;
  value: string;
  updatedBy: number | null;
  updatedAt: string | null;
}

export interface AuditLog {
  logId: number;
  userId: number | null;
  userName: string | null;
  action: string;
  entity: string;
  entityId: number | null;
  details: string | null;
  createdAt: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string> | null;
}
