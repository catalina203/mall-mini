import { request } from './request'
import type { PageResult, Review, ReviewSummary } from '../types/api'

export function createReview(
  orderNo: string,
  payload: { orderItemId: number; rating: number; content?: string; anonymous?: boolean },
): Promise<Review> {
  return request<Review>({
    url: `/me/orders/${orderNo}/reviews`,
    method: 'POST',
    data: payload,
  })
}

export function listProductReviews(
  productId: number,
  page = 1,
  pageSize = 10,
): Promise<PageResult<Review>> {
  return request<PageResult<Review>>({
    url: `/products/${productId}/reviews?page=${page}&pageSize=${pageSize}`,
  })
}

export function getReviewSummary(productId: number): Promise<ReviewSummary> {
  return request<ReviewSummary>({ url: `/products/${productId}/reviews/summary` })
}
