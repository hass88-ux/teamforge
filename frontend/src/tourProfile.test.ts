import { expect, test } from 'vitest'
import { stepIsComplete, roles, skills, goals, styles, domains } from './profile'
import { tourProfile } from './tourProfile'

test('quick tour has valid selections and isolated profile state', () => {
 const profile = tourProfile()
 for (let step = 0; step < 6; step++) expect(stepIsComplete(step, profile)).toBe(true)
 expect(roles).toContain(profile.role)
 expect(goals).toContain(profile.goal)
 expect(styles).toContain(profile.workingStyle)
 for (const skill of [...profile.skills, ...profile.neededSkills]) expect(skills).toContain(skill)
 for (const domain of profile.interests) expect(domains).toContain(domain)
 expect(profile.availability.every(hour => hour >= 0 && hour < 168)).toBe(true)
 profile.skills.push('React')
 expect(tourProfile().skills).not.toContain('React')
})
