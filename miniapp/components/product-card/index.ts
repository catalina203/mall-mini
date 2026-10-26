interface ProductCard {
  id: number
  name: string
  subtitle: string
  mainImage: string
  priceText: string
  stockText: string
}

Component({
  properties: {
    product: {
      type: Object,
      value: {} as ProductCard,
    },
  },
  methods: {
    onSelect() {
      const product = this.data.product as ProductCard
      if (product && product.id) {
        this.triggerEvent('select', { id: product.id })
      }
    },
  },
})
