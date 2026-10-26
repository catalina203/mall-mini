Component({
  properties: {
    src: {
      type: String,
      value: '',
    },
    mode: {
      type: String,
      value: 'aspectFill',
    },
  },
  data: {
    failed: false,
  },
  observers: {
    src() {
      this.setData({ failed: false })
    },
  },
  methods: {
    onError() {
      this.setData({ failed: true })
    },
  },
})
