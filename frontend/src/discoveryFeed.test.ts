import { expect, test } from 'vitest'
import { combineFeed, isGenerated } from './discoveryFeed'
test('real profiles retain order when browse-only examples are interleaved', () => {
 expect(combineFeed(['real-a', 'real-b'], ['demo-a', 'demo-b', 'demo-c'])).toEqual(['real-a', 'demo-a', 'real-b', 'demo-b', 'demo-c'])
 expect(combineFeed([], ['demo-a'])).toEqual(['demo-a'])
})
test('generated candidates are separated from persisted real decisions', () => {
 expect(isGenerated({ accountType: 'DEMO' })).toBe(true)
 expect(isGenerated({ accountType: 'REAL' })).toBe(false)
})
