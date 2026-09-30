export type ProfileDraft = {
  displayName: string
  role: string
  skills: string[]
  interests: string[]
  rolesSought: string[]
  weeklyHours: number
  goal: string
  workingStyle: string
  timezone: string
  availability: number[]
}

export const roles = ['Backend engineer', 'Frontend engineer', 'Full-stack engineer', 'ML engineer', 'Designer', 'Founder', 'Researcher', 'Student', 'Mobile developer', 'DevOps engineer', 'Data engineer', 'Cybersecurity engineer']
export const skills = ['Java', 'Spring Boot', 'Python', 'PyTorch', 'React', 'TypeScript', 'PostgreSQL', 'AWS', 'Docker', 'UI/UX', 'Product', 'Mobile', 'Security', 'Data engineering']
export const domains = ['AI', 'Education', 'Healthcare', 'Climate', 'Fintech', 'Developer tools', 'Cybersecurity', 'Gaming', 'Robotics', 'Research', 'Consumer apps']
export const goals = ['Hackathon', 'Portfolio project', 'Open source', 'Research collaboration', 'Startup', 'Weekend project']
export const styles = ['Structured', 'Flexible', 'Fast-moving', 'Research-heavy', 'Design-first', 'Engineering-first', 'Product-first']

export function emptyProfile(): ProfileDraft {
  return { displayName: '', role: '', skills: [], interests: [], rolesSought: [], weeklyHours: 8, goal: '', workingStyle: '', timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC', availability: [] }
}

export function stepIsComplete(step: number, profile: ProfileDraft): boolean {
  switch (step) {
    case 0: return profile.displayName.trim().length > 0 && profile.role.length > 0
    case 1: return profile.skills.length > 0
    case 2: return profile.interests.length > 0 && profile.goal.length > 0
    case 3: return profile.rolesSought.length > 0
    case 4: return profile.weeklyHours >= 1 && profile.weeklyHours <= 60 && profile.workingStyle.length > 0
    case 5: return profile.timezone.trim().length > 0 && profile.availability.length > 0
    default: return false
  }
}
