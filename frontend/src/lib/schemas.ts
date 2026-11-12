import { z } from 'zod';

const password = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .max(100, 'Password must be at most 100 characters')
  .regex(/[A-Za-z]/, 'Password must include a letter')
  .regex(/\d/, 'Password must include a number');

export const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export type LoginValues = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    name: z.string().min(2, 'Tell us your full name').max(120),
    email: z.string().email('Enter a valid email address').max(190),
    password,
    confirmPassword: z.string(),
    role: z.enum(['PARTICIPANT', 'FACILITATOR', 'MENTOR', 'PARTNER']),
    country: z.string().max(80).optional(),
    phone: z.string().max(30).optional(),
    age: z.coerce.number().int().min(18, 'Participants must be 18 or older').max(35, 'Participants must be 35 or younger').optional(),
    digitalSkills: z.string().max(80).optional(),
    institution: z.string().max(190).optional(),
    bio: z.string().max(1000).optional(),
    shareProfileWithPartners: z.boolean().optional(),
    specialisation: z.string().max(190).optional(),
    organisation: z.string().max(190).optional(),
    expertise: z.string().max(500).optional(),
    department: z.string().max(190).optional(),
    organisationName: z.string().max(190).optional(),
    organisationType: z.enum(['NGO', 'UNIVERSITY', 'EMPLOYER', 'DEVELOPMENT_AGENCY', 'TECH_COMPANY']).optional(),
    website: z.string().max(300).optional(),
    description: z.string().max(2000).optional(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })
  .refine((values) => values.role !== 'PARTICIPANT' || (values.age !== undefined && values.age !== null), {
    path: ['age'],
    message: 'Age is required for participants',
  })
  .refine((values) => values.role !== 'PARTNER' || !!values.organisationName, {
    path: ['organisationName'],
    message: 'Organisation name is required for partners',
  });

export type RegisterValues = z.infer<typeof registerSchema>;

export const adminUserSchema = z.object({
  name: z.string().min(2, 'Name is required').max(120),
  email: z.string().email('Enter a valid email address').max(190),
  password,
  role: z.enum(['PARTICIPANT', 'FACILITATOR', 'MENTOR', 'PARTNER', 'ADMIN']),
  phone: z.string().max(30).optional(),
  age: z.preprocess(
    (value) => (value === '' || value === null ? undefined : value),
    z.coerce.number().int().min(18).max(35).optional(),
  ),
  country: z.string().max(80).optional(),
  organisationName: z.string().max(190).optional(),
  organisationType: z.enum(['NGO', 'UNIVERSITY', 'EMPLOYER', 'DEVELOPMENT_AGENCY', 'TECH_COMPANY']).optional(),
});

export type AdminUserValues = z.infer<typeof adminUserSchema>;

export const activitySchema = z
  .object({
    level: z.enum(['ONE', 'TWO']),
    title: z.string().min(4, 'Title is too short').max(190),
    description: z.string().max(4000).optional(),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().optional(),
    category: z.string().max(80).optional(),
    capacity: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().min(1).optional(),
    ),
    passMark: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().min(0).max(100).optional(),
    ),
    minAttendancePercent: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().min(0).max(100).optional(),
    ),
    minTeamSize: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().min(1).optional(),
    ),
    maxTeamSize: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().min(1).optional(),
    ),
    facilitatorId: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().positive().optional(),
    ),
    partnerId: z.preprocess(
      (value) => (value === '' || value === null ? undefined : value),
      z.coerce.number().int().positive().optional(),
    ),
  })
  .refine((values) => !values.endDate || values.endDate >= values.startDate, {
    path: ['endDate'],
    message: 'End date cannot be before the start date',
  });

export type ActivityValues = z.infer<typeof activitySchema>;

export const teamSchema = z.object({
  activityId: z.coerce.number().int().positive('Choose a Level Two activity'),
  teamName: z.string().min(2, 'Team name is too short').max(120),
  mentorId: z.coerce.number().int().positive().optional(),
});

export type TeamValues = z.infer<typeof teamSchema>;

export const projectSchema = z.object({
  teamId: z.coerce.number().int().positive(),
  title: z.string().min(4, 'Title is too short').max(190),
  description: z.string().max(6000).optional(),
  challengeArea: z.enum(['EMPLOYMENT', 'EDUCATION', 'AGRICULTURE', 'HEALTH', 'CLIMATE_CHANGE', 'DIGITAL_INCLUSION']),
  aiUsed: z.boolean().optional(),
  aiUsageDeclaration: z.string().max(2000).optional(),
});

export type ProjectValues = z.infer<typeof projectSchema>;

export const milestoneSchema = z.object({
  title: z.string().min(2, 'Milestone title is too short').max(190),
  dueDate: z.string().optional(),
});

export type MilestoneValues = z.infer<typeof milestoneSchema>;

export const reviewSchema = z.object({
  decision: z.enum(['COMMENT', 'CHANGES_REQUESTED', 'APPROVED']),
  feedback: z.string().min(2, 'Feedback is required').max(4000),
});

export type ReviewValues = z.infer<typeof reviewSchema>;

export const taskSchema = z.object({
  title: z.string().min(2, 'Title is required').max(190),
  taskType: z.enum(['QUIZ', 'ASSIGNMENT']),
  instructions: z.string().max(4000).optional(),
  maxScore: z.preprocess(
    (value) => (value === '' || value === null ? undefined : value),
    z.coerce.number().int().min(1).max(1000).optional(),
  ),
  dueDate: z.string().optional(),
  published: z.boolean().optional(),
});

export type TaskValues = z.infer<typeof taskSchema>;

export const submissionSchema = z.object({
  submissionText: z.string().min(1, 'Write your answer').max(6000),
});

export type SubmissionValues = z.infer<typeof submissionSchema>;

export const gradeSchema = z.object({
  score: z.coerce.number().int().min(0),
  feedback: z.string().max(4000).optional(),
});

export type GradeValues = z.infer<typeof gradeSchema>;

export const opportunitySchema = z.object({
  title: z.string().min(4, 'Title is too short').max(190),
  opportunityType: z.enum(['JOB', 'INTERNSHIP', 'MENTORSHIP', 'SCHOLARSHIP', 'TRAINING']),
  description: z.string().max(4000).optional(),
  location: z.string().max(190).optional(),
  deadline: z.string().optional(),
});

export type OpportunityValues = z.infer<typeof opportunitySchema>;

export const supportSchema = z.object({
  supportType: z.enum(['FUNDING', 'MENTORSHIP', 'RESOURCES', 'VISIBILITY']),
  message: z.string().max(2000).optional(),
});

export type SupportValues = z.infer<typeof supportSchema>;
