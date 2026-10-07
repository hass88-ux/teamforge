import { expect, test } from 'vitest'
import { combineFeed, isGenerated, mergeRankedFeed } from './discoveryFeed'
test('real profiles retain order when browse-only examples are interleaved', () => {
 expect(combineFeed(['real-a', 'real-b'], ['demo-a', 'demo-b', 'demo-c'])).toEqual(['real-a', 'demo-a', 'real-b', 'demo-b', 'demo-c'])
 expect(combineFeed([], ['demo-a'])).toEqual(['demo-a'])
})
test('generated candidates are separated from persisted real decisions', () => {
 expect(isGenerated({ accountType: 'DEMO' })).toBe(true)
 expect(isGenerated({ accountType: 'REAL' })).toBe(false)
})
test('late real ranking does not change a card after someone has started swiping', () => {
 const current = ['demo-a', 'demo-b', 'demo-c']
 expect(mergeRankedFeed(current, ['real-a'], true)[1]).toBe('demo-b')
 expect(mergeRankedFeed(current, ['real-a'], true)).toEqual([...current, 'real-a'])
 expect(mergeRankedFeed(current, ['real-a'], false)).toEqual(['real-a', 'demo-a', 'demo-b', 'demo-c'])
})
