export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}
