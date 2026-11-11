import type { ProfileDraft } from './profile'

// Public sample only; never persisted as an account or a real member.
export function tourProfile(): ProfileDraft {
 return {
  entityType: 'INDIVIDUAL', matchingIntent: 'COLLABORATOR',
  displayName: 'Demo builder', role: 'Backend engineer',
  description: 'Demo profile for exploring TeamForge. I bring Java and Python and want to build developer tools with a frontend collaborator.',
  skills: ['Java', 'Spring Boot', 'Python', 'PostgreSQL'], neededSkills: ['React', 'TypeScript'],
  interests: ['Developer tools', 'Education'], rolesSought: ['Frontend engineer', 'Designer'],
  weeklyHours: 8, goal: 'Portfolio project', workingStyle: 'Structured',
  timezone: 'UTC', availability: [18, 19, 42, 43, 66, 67, 138, 139],
 }
}
