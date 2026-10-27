import http from './request'

async function download(path: string, filename: string, params?: Record<string, unknown>) {
  const response = await http.get(path, { params, responseType: 'blob' })
  return { blob: response.data as Blob, filename }
}

export function exportOrders(params: Record<string, unknown>) {
  return download('/admin/exports/orders', 'orders.csv', params)
}

export function exportUsers(params: Record<string, unknown>) {
  return download('/admin/exports/users', 'users.csv', params)
}

export function exportStockLogs(params: Record<string, unknown>) {
  return download('/admin/exports/stock-logs', 'stock-logs.csv', params)
}
