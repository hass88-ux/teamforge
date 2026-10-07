export function isGenerated(candidate: { accountType: string }): boolean {
 return candidate.accountType === 'DEMO'
}
export function combineFeed<T>(real: T[], generated: T[]): T[] {
 const feed: T[] = []
 for (let i = 0; i < Math.max(real.length, generated.length); i++) {
  if (i < real.length) feed.push(real[i])
  if (i < generated.length) feed.push(generated[i])
 }
 return feed
}
