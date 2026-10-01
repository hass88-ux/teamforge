import { describe, expect, it } from 'vitest'
import { demoReducer, initialDemoState } from './demoState'
import type { Recommendation, CoffeeProposal } from './demoState'
const recommendation: Recommendation = { candidate: { entityType: 'INDIVIDUAL', matchingIntent: 'COLLABORATOR', description: '', neededSkills: [], id: 'demo-1', accountType: 'DEMO', displayName: 'Fictional', role: 'Frontend engineer', headline: 'Demo', skills: ['React'], interests: ['AI'], rolesSought: ['Backend engineer'], weeklyHours: 8, goal: 'Portfolio project', workingStyle: 'Structured', timezone: 'UTC', availability: [20], projects: [] }, compatibility: 90, forwardScore: 90, reverseScore: 90, contributions: {}, evidence: [], overlapHours: 1, modelVersion: 'test' }
const matched = () => demoReducer(initialDemoState, { type: 'decide', recommendation, liked: true })
const proposal: CoffeeProposal = { id: 'coffee', kind: 'Virtual coffee', startsAt: '2026-10-02T18:00:00Z', timezone: 'UTC', note: 'Discuss ideas', status: 'proposed' }

describe('isolated demo collaboration', () => {
  it('requires like and simulated reciprocal interest', () => {
    expect(demoReducer(initialDemoState, { type: 'decide', recommendation, liked: false }).matches).toHaveLength(0)
    expect(demoReducer(initialDemoState, { type: 'decide', recommendation: { ...recommendation, compatibility: 50 }, liked: true }).matches).toHaveLength(0)
    expect(matched().matches).toHaveLength(1)
  })
  it('does not duplicate decisions or matches', () => {
    const state = matched()
    expect(demoReducer(state, { type: 'decide', recommendation, liked: true })).toBe(state)
  })
  it('only adds messages to an existing match and labels the scripted reply', () => {
    const action = { type: 'message' as const, candidateId: 'demo-1', text: ' Hello ', sentAt: '2026-09-30T18:00:00Z', id: 'message-1' }
    expect(demoReducer(initialDemoState, action)).toBe(initialDemoState)
    const state = demoReducer(matched(), action)
    expect(state.matches[0].messages.map(message => message.sender)).toEqual(['you', 'simulation'])
    expect(state.matches[0].messages[0].text).toBe('Hello')
    expect(demoReducer(state, action)).toBe(state)
  })
  it('rejects blank and oversized messages', () => {
    const state = matched()
    for (const text of ['   ', 'a'.repeat(1001)]) expect(demoReducer(state, { type: 'message', candidateId: 'demo-1', text, id: 'x', sentAt: '2026-09-30T18:00:00Z' })).toBe(state)
  })
  it('rejects past proposals, allows future proposals, and finalizes one response', () => {
    const state = matched()
    expect(demoReducer(state, { type: 'propose', candidateId: 'demo-1', proposal, now: '2026-10-03T18:00:00Z' })).toBe(state)
    const pending = demoReducer(state, { type: 'propose', candidateId: 'demo-1', proposal, now: '2026-09-30T18:00:00Z' })
    expect(pending.matches[0].proposals).toHaveLength(1)
    const accepted = demoReducer(pending, { type: 'respond', candidateId: 'demo-1', proposalId: 'coffee', status: 'accepted' })
    const repeated = demoReducer(accepted, { type: 'respond', candidateId: 'demo-1', proposalId: 'coffee', status: 'declined' })
    expect(repeated.matches[0].proposals[0].status).toBe('accepted')
  })
  it('resets all choices, messages and proposals together', () => {
    expect(demoReducer(matched(), { type: 'reset' })).toEqual(initialDemoState)
  })
})
