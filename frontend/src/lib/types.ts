/**
 * Mirror of the Spring Boot DTO contracts (`/api/v1/**`). Keeping them as
 * plain interfaces means the UI never guesses at a payload shape.
 */

export type Role = 'PARTICIPANT' | 'FACILITATOR' | 'MENTOR' | 'ADMIN';
export type AccountStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING';
export type ExchangeLevel = 'LEVEL_ONE' | 'LEVEL_TWO';
export type ExchangeStatus = 'DRAFT' | 'OPEN' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type LevelOneTopic =
  | 'DIGITAL_LITERACY'
  | 'AI_LITERACY'
  | 'CRITICAL_THINKING'
  | 'MEDIA_LITERACY'
  | 'INTERCULTURAL_COMM';
export type LevelTwoFocusArea =
  | 'EMPLOYMENT'
  | 'EDUCATION'
  | 'AGRICULTURE'
  | 'HEALTH'
  | 'CLIMATE'
  | 'DIGITAL_INCLUSION';
export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type CompletionStatus = 'ENROLLED' | 'IN_PROGRESS' | 'COMPLETED' | 'WITHDRAWN';
export type ProjectStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'COMPLETED'
  | 'REJECTED';
export type AssessmentTargetType = 'PARTICIPANT' | 'TEAM';
export type TeamMemberRole = 'LEAD' | 'MEMBER';
export type AnnouncementScope = 'ALL_USERS' | 'EXCHANGE_COHORT' | 'TEAM';
export type AiModerationStatus = 'PENDING' | 'APPROVED' | 'BLOCKED';

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  country: string | null;
  phone: string | null;
  accountStatus: AccountStatus;
  consentGiven: boolean;
  createdAt: string;
  age: number | null;
  skillLevel: SkillLevel | null;
  expertiseArea: string | null;
  expertiseTopics: string | null;
  sessionsFacilitated: number | null;
  mentorshipAreas: string | null;
  permissions: string | null;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface Exchange {
  id: number;
  title: string;
  description: string | null;
  level: ExchangeLevel;
  summaryLabel: string;
  startDate: string;
  endDate: string | null;
  status: ExchangeStatus;
  topic: LevelOneTopic | null;
  maxParticipants: number | null;
  sessionLink: string | null;
  facilitatorId: number | null;
  focusArea: LevelTwoFocusArea | null;
  maxTeams: number | null;
  applicationsOpen: boolean | null;
  enrolledCount: number;
  teamCount: number;
  canEnroll: boolean;
}

export interface Enrollment {
  id: number;
  participantId: number | null;
  participantName: string | null;
  exchangeId: number | null;
  exchangeTitle: string | null;
  level: ExchangeLevel | null;
  enrolledAt: string;
  completionStatus: CompletionStatus;
  completedAt: string | null;
}

export interface TeamMember {
  participantId: number | null;
  fullName: string | null;
  email: string | null;
  memberRole: TeamMemberRole;
  joinedAt: string;
}

export interface Team {
  id: number;
  name: string;
  description: string | null;
  levelTwoExchangeId: number | null;
  exchangeTitle: string | null;
  mentorId: number | null;
  mentorName: string | null;
  members: TeamMember[];
  memberCount: number;
  impactProjectId: number | null;
  impactProjectTitle: string | null;
  impactProjectStatus: string | null;
  createdAt: string;
}

export interface Project {
  id: number;
  title: string;
  problemStatement: string | null;
  proposedSolution: string | null;
  teamId: number | null;
  teamName: string | null;
  levelTwoExchangeId: number | null;
  focusArea: LevelTwoFocusArea | null;
  status: ProjectStatus;
  submissionDate: string | null;
  aiAssisted: boolean;
  aiUsageNote: string | null;
  reviewFeedback: string | null;
  reviewedAt: string | null;
}

export interface Assessment {
  id: number;
  targetType: AssessmentTargetType;
  targetId: number | null;
  targetLabel: string | null;
  assessorId: number | null;
  assessorName: string | null;
  criteria: string;
  score: number;
  feedback: string | null;
  assessmentDate: string;
  pass: boolean;
}

export interface Certificate {
  id: number;
  title: string;
  recipientId: number | null;
  recipientName: string | null;
  issueDate: string;
  verificationCode: string;
  relatedActivityOrProject: string | null;
}

export interface CertificateVerification {
  valid: boolean;
  message: string;
  title: string | null;
  recipientName: string | null;
  issueDate: string | null;
  relatedActivityOrProject: string | null;
}

export interface Message {
  id: number;
  messageType: string;
  senderId: number | null;
  senderName: string | null;
  recipientId: number | null;
  recipientName: string | null;
  teamId: number | null;
  exchangeId: number | null;
  subject: string | null;
  body: string;
  read: boolean;
  sentAt: string;
  aiGenerated: boolean;
  aiFlagReason: string | null;
  aiModerationStatus: AiModerationStatus | null;
  scope: AnnouncementScope | null;
  published: boolean | null;
  requiresHumanApproval: boolean | null;
}

export interface UnreadCount {
  unread: number;
  pendingApprovals: number;
}

export interface AiSuggestion {
  draft: string;
  aiGenerated: boolean;
  label: string;
  reviewInstructions: string;
}

export interface AiModeration {
  [key: string]: unknown;
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
