import request from './request'

export interface LeagueTierInfo {
  tierName: string
  tierIcon: string
  tierSortOrder: number
  totalPoints: number
  nextTierName?: string
  nextTierIcon?: string
  nextTierPoints?: number
  pointsToNext?: number
}

export interface RankingItem {
  ranking: number
  userId: number
  nickname: string
  avatar: string
  weeklyPoints: number
}

export interface LeagueRanking {
  groupId: number
  tierName: string
  tierIcon: string
  rankings: RankingItem[]
  myRanking: number
  myPoints: number
  pointsToPromote: number
  promoteCount: number
  demoteCount: number
}

export interface LeagueHistoryItem {
  weekStartDate: string
  tierName: string
  tierIcon: string
  ranking: number
  weeklyPoints: number
  result: string
}

export function getLeagueTier() {
  return request.get<any, LeagueTierInfo>('/v1/league/tier')
}

export function getLeagueRanking() {
  return request.get<any, LeagueRanking>('/v1/league/ranking')
}

export function getLeagueHistory(weeks = 12) {
  return request.get<any, LeagueHistoryItem[]>('/v1/league/history', { params: { weeks } })
}
