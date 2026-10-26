export function formatPrice(value: number | string): string {
  const amount = Number(value)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '—'
}

export function formatPriceRange(min: number | string, max: number | string): string {
  const minText = formatPrice(min)
  const maxText = formatPrice(max)
  return minText === maxText ? minText : `${minText} ~ ${maxText}`
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (input: number) => String(input).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
