import { z } from 'zod';

const password = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .max(72, 'Password must be at most 72 characters');

export const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export type LoginValues = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    fullName: z.string().min(2, 'Tell us your full name').max(120),
    email: z.string().email('Enter a valid email address').max(190),
    password,
    confirmPassword: z.string(),
    country: z.string().max(80).optional(),
    phone: z.string().max(30).optional(),
    role: z.enum(['PARTICIPANT', 'FACILITATOR', 'MENTOR']),
    consentGiven: z.literal(true, {
      errorMap: () => ({ message: 'You must accept the privacy consent to continue' }),
    }),
    age: z.coerce.number().int().min(16, 'Age must be 16 or older').max(40, 'Age must be 40 or younger').optional(),
    skillLevel: z.enum(['BEGINNER', 'INTERMEDIATE', 'ADVANCED']).optional(),
    expertiseArea: z.string().max(120).optional(),
    expertiseTopics: z.string().max(400).optional(),
    mentorshipAreas: z.string().max(400).optional(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })
  .refine((values) => values.role !== 'PARTICIPANT' || (values.age !== undefined && values.age !== null), {
    path: ['age'],
    message: 'Age is required for participants',
  });

export type RegisterValues = z.infer<typeof registerSchema>;

export const exchangeSchema = z.object({
  title: z.string().min(4, 'Title is too short').max(180),
  description: z.string().max(4000).optional(),
  level: z.enum(['LEVEL_ONE', 'LEVEL_TWO']),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional(),
  status: z.enum(['DRAFT', 'OPEN', 'ACTIVE', 'COMPLETED', 'CANCELLED']).optional(),
  topic: z.enum(['DIGITAL_LITERACY', 'AI_LITERACY', 'CRITICAL_THINKING', 'MEDIA_LITERACY', 'INTERCULTURAL_COMM']).optional(),
  maxParticipants: z.coerce.number().int().min(1).max(100000).optional(),
  sessionLink: z.string().max(500).optional(),
  focusArea: z
    .enum(['EMPLOYMENT', 'EDUCATION', 'AGRICULTURE', 'HEALTH', 'CLIMATE', 'DIGITAL_INCLUSION'])
    .optional(),
  maxTeams: z.coerce.number().int().min(1).max(500).optional(),
  applicationsOpen: z.boolean().optional(),
});

export type ExchangeValues = z.infer<typeof exchangeSchema>;

export const projectSchema = z.object({
  title: z.string().min(4, 'Title is too short').max(180),
  problemStatement: z.string().max(4000).optional(),
  proposedSolution: z.string().max(4000).optional(),
  aiAssisted: z.boolean().optional(),
  aiUsageNote: z
    .string()
    .max(2000)
    .optional()
    .refine((value) => !value || value.trim().length > 0, {
      message: 'Describe how AI was used, or leave the box unticked',
    }),
});

export type ProjectValues = z.infer<typeof projectSchema>;

export const teamSchema = z.object({
  name: z.string().min(2, 'Team name is too short').max(120),
  description: z.string().max(2000).optional(),
  levelTwoExchangeId: z.coerce.number().int().positive('Choose a Level Two programme'),
});

export type TeamValues = z.infer<typeof teamSchema>;

export const assessmentSchema = z.object({
  targetType: z.enum(['PARTICIPANT', 'TEAM']),
  participantId: z.coerce.number().int().positive().optional(),
  teamId: z.coerce.number().int().positive().optional(),
  criteria: z.string().min(2, 'Criteria is required').max(200),
  score: z.coerce.number().int().min(0, 'Score must be 0 or more').max(100, 'Score must be 100 or less'),
  feedback: z.string().max(4000).optional(),
});

export type AssessmentValues = z.infer<typeof assessmentSchema>;

export const messageSchema = z
  .object({
    recipientId: z.coerce.number().int().positive().optional(),
    teamId: z.coerce.number().int().positive().optional(),
    subject: z.string().max(200).optional(),
    body: z.string().min(1, 'Write a message').max(4000),
  })
  .refine((value) => value.recipientId !== undefined || value.teamId !== undefined, {
    path: ['recipientId'],
    message: 'Pick a recipient or a team',
  });

export type MessageValues = z.infer<typeof messageSchema>;

export const announcementSchema = z.object({
  subject: z.string().min(2, 'Subject is required').max(200),
  body: z.string().min(2, 'Body is required').max(4000),
  scope: z.enum(['ALL_USERS', 'EXCHANGE_COHORT', 'TEAM']).optional(),
  exchangeId: z.coerce.number().int().positive().optional(),
  teamId: z.coerce.number().int().positive().optional(),
});

export type AnnouncementValues = z.infer<typeof announcementSchema>;
