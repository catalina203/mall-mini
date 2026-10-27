import { request } from './request'
import type { MemberInfo, PageResult, PointsLog } from '../types/api'

export function getMember(): Promise<MemberInfo> {
  return request<MemberInfo>({ url: '/me/member' })
}

export function listPointsLogs(page = 1, pageSize = 20): Promise<PageResult<PointsLog>> {
  return request<PageResult<PointsLog>>({
    url: `/me/points/logs?page=${page}&pageSize=${pageSize}`,
  })
}
