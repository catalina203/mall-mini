import { appConfig } from '../../utils/config'

interface Section {
  heading: string
  body: string
}

const USER_AGREEMENT: Section[] = [
  {
    heading: '一、服务说明',
    body: '本页面为商城用户协议示例文本。正式上线前，运营方应结合主体信息、商品类型和实际业务流程，由法务或合规人员审核并替换本页内容。',
  },
  {
    heading: '二、账号与登录',
    body: '用户通过微信授权或开发环境测试账号登录。用户应妥善保管账号，不得出借、转让或用于违法活动。开发登录仅在开发环境提供，不属于真实微信登录。',
  },
  {
    heading: '三、下单与支付',
    body: '商品价格、库存、优惠和运费以下单时服务端计算结果为准。用户提交订单后应按页面提示完成支付；未支付订单可能因超时被取消，库存自动回补。',
  },
  {
    heading: '四、发货与售后',
    body: '商家按页面展示的履约规则发货。符合条件的订单可申请退款或退货退款，具体以订单状态和售后页面提示为准。',
  },
  {
    heading: '五、其他',
    body: '本示例不构成正式法律文件。上线前请补充运营主体、联系方式、争议解决方式、发票与隐私条款，并确保与实际收集和使用的信息一致。',
  },
]

const PRIVACY_NOTICE: Section[] = [
  {
    heading: '一、我们收集的信息',
    body: '为完成登录、下单、支付和履约，我们可能收集微信标识、昵称、头像、收货人姓名、联系电话、收货地址和订单信息。当前脚手架不主动收集与业务无关的个人信息。',
  },
  {
    heading: '二、信息用途',
    body: '收集的信息仅用于账号识别、订单处理、物流配送、售后服务和必要的安全审计，不用于与商城服务无关的用途。',
  },
  {
    heading: '三、信息共享',
    body: '为完成支付和配送，订单所需信息可能提供给支付机构和物流服务商。除法律法规要求或用户授权外，我们不会向其他第三方提供个人信息。',
  },
  {
    heading: '四、信息安全',
    body: '服务端对手机号和地址等敏感信息进行脱敏展示，日志不记录密码、令牌、支付密钥或完整个人信息。生产环境应使用 HTTPS 并限制数据访问权限。',
  },
  {
    heading: '五、用户权利',
    body: '用户可查看和修改收货地址，可在个人中心退出登录。账号注销、数据删除等能力需在上线前按实际业务补充。本页为示例文本，上线前应由法务或合规人员审核。',
  },
]

Page({
  data: {
    brandName: appConfig.brandName,
    title: '用户协议',
    updatedAt: '示例版本，上线前需替换',
    sections: [] as Section[],
  },

  onLoad(options: Record<string, string | undefined>) {
    const type = options.type === 'privacy' ? 'privacy' : 'user'
    const isPrivacy = type === 'privacy'
    this.setData({
      title: isPrivacy ? '隐私说明' : '用户协议',
      sections: isPrivacy ? PRIVACY_NOTICE : USER_AGREEMENT,
    })
    wx.setNavigationBarTitle({ title: isPrivacy ? '隐私说明' : '用户协议' })
  },
})
