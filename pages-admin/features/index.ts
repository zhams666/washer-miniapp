type FeatureStatus = 'done' | 'partial' | 'todo';

type FeatureItem = {
  key: string;
  title: string;
  status: FeatureStatus;
  statusText: string;
  entryText: string;
  logic: string;
  route?: string;
};

const statusTextMap: Record<FeatureStatus, string> = {
  done: '已接入',
  partial: '部分接入',
  todo: '待接入',
};

const feature = (
  key: string,
  title: string,
  status: FeatureStatus,
  entryText: string,
  logic: string,
  route?: string
): FeatureItem => ({
  key,
  title,
  status,
  statusText: statusTextMap[status],
  entryText,
  logic,
  route,
});

Page({
  data: {
    groups: [
      {
        title: '用户端洗车主流程',
        desc: '用户打开小程序后可以完成找店、扫码、充值、排队、洗车、查看订单和钱包明细。',
        items: [
          feature(
            'home-nearest-store',
            '首页最近门店',
            'done',
            '首页、门店页',
            '首页读取用户定位后请求门店列表，按距离选择最近门店展示；门店页继续展示完整门店列表、价格、地址、工位数量和门店标签。',
            '/pages/home/index'
          ),
          feature(
            'scan-wash',
            '扫码后进入洗车流程',
            'done',
            '首页扫码洗车',
            '扫码结果会解析门店或工位二维码，识别成功后进入门店详情或工位洗车流程；余额不足时引导到充值页，充值完成后可回到洗车流程继续操作。',
            '/pages/home/index'
          ),
          feature(
            'bay-status',
            '工位空闲查看和点击洗车',
            'done',
            '门店详情',
            '门店详情读取工位状态，展示空闲、使用中、停用；用户点击空闲工位后进入对应工位的洗车流程。',
            '/pages/service/index'
          ),
          feature(
            'queue',
            '预约排队',
            'done',
            '门店详情',
            '门店没有空闲工位时显示排队入口；后端按门店生成排队序号，校验用户是否在门店一百米内，超出范围会取消当前排队并让后续用户前移。',
            '/pages/service/index'
          ),
          feature(
            'wallet-detail',
            '余额、充值明细和消费明细',
            'done',
            '我的、钱包',
            '钱包页展示总余额、通用余额、门店余额和流水；门店独立钱包通过 storeId 区分，便于统计每个门店的充值、赠送、消费和未使用余额。',
            '/pages/wallet/index'
          ),
          feature(
            'order-detail',
            '消费订单和洗车订单',
            'done',
            '订单页、我的',
            '用户订单页按用户维度展示洗车订单、支付状态、金额和时间；商户端可按手机号、门店、订单状态继续筛选。',
            '/pages/order/index'
          ),
          feature(
            'card-user',
            '用户次卡购买和查看',
            'partial',
            '次卡购买、我的卡券、钱包',
            '已提供次卡购买入口和用户资产接口基础；还需要进一步完善每张次卡的剩余次数、适用门店、使用记录和退款后取消状态展示。',
            '/pages/card-purchase/index'
          ),
          feature(
            'ranking',
            '充值榜单和时长榜单',
            'partial',
            '底部榜单',
            '榜单入口已放到底部导航；后续需要把充值金额、洗车时长、头像、昵称按榜单类型统一展示。',
            '/pages/ranking/index'
          ),
          feature(
            'invite',
            '老用户邀请新用户奖励',
            'partial',
            '邀请页',
            '已有邀请页入口；完整奖励需要记录邀请关系、被邀请人首单或充值行为、奖励发放流水和防刷规则。',
            '/pages/invitation/index'
          ),
          feature(
            'points-mall',
            '积分商城',
            'partial',
            '积分商城',
            '用户端已有积分商城入口；PC 后台还需要完善商品上架、库存、积分兑换规则和兑换记录。',
            '/pages/points-mall/index'
          ),
          feature(
            'wash-docs',
            '洗车教程和操作步骤',
            'done',
            '洗车教程',
            '教程页用于展示洗车步骤；如果后续轮播图支持点击跳转，可直接跳到该教程页。',
            '/pages/question/index'
          ),
        ] as FeatureItem[],
      },
      {
        title: '商户端移动管理',
        desc: '门店店长、加盟老板和总部人员在小程序内查看门店、订单、设备、资产和经营数据。',
        items: [
          feature(
            'mini-admin-dashboard',
            '商户端经营总览',
            'done',
            '商家管理首页',
            '登录后根据角色返回可管理门店范围，总部看全部，加盟老板看旗下门店，店长看本店；首页展示设备、订单、金额和最近动态。',
            '/pages-admin/home/index'
          ),
          feature(
            'mini-admin-devices',
            '工位和设备管理',
            'partial',
            '商户端设备',
            '设备页展示工位状态和异常；启停、维护备注、录入工位、撤机、柜机、云音响、毛巾柜、车牌识别器需要按硬件协议继续接入。',
            '/pages-admin/devices/index'
          ),
          feature(
            'mini-admin-orders',
            '订单查询',
            'done',
            '商户端订单',
            '订单页按权限范围查询订单，可通过手机号、门店和状态筛选，便于处理卡单、异常订单和用户咨询。',
            '/pages-admin/orders/index'
          ),
          feature(
            'mini-admin-assets',
            '用户资产调整',
            'done',
            '商户端用户资产',
            '商户可对用户做加余额、扣余额、赠送金额、次卡调整；扣款和人工调整要求填写备注，便于追踪扣减说明。',
            '/pages-admin/assets/index'
          ),
          feature(
            'mini-admin-finance',
            '充值明细、收益明细和未核销统计',
            'partial',
            '商户端经营流水',
            '经营流水页展示当前权限范围内的充值、消费、次卡和核销指标；后续需要增加日期筛选、赠送金额、未使用余额和每日洗车量汇总。',
            '/pages-admin/finance/index'
          ),
          feature(
            'mini-admin-profile',
            '人员权限和门店范围',
            'done',
            '商户端我的权限',
            '登录后按总部、加盟老板、店长、员工角色展示权限范围；后续修改密码和人员管理可继续放入该模块。',
            '/pages-admin/profile/index'
          ),
          feature(
            'mini-admin-features',
            '功能完整性检查',
            'done',
            '商户端功能整理',
            '本页把 Word 中的功能拆成已接入、部分接入和待接入，便于试用版验收和后续开发排期。',
            '/pages-admin/features/index'
          ),
          feature(
            'store-notice',
            '门店维护备注和多条公告',
            'partial',
            '门店管理、门店列表',
            '门店已有备注字段可以展示单条维护信息；多条公告、设备损坏公告、生效时间和排序建议新增 store_notice 表后接入。',
            '/pages/service/index'
          ),
        ] as FeatureItem[],
      },
      {
        title: '价格、会员和营销',
        desc: '围绕会员日、分时价格、次卡、团购核销、抽奖和积分兑换的经营配置。',
        items: [
          feature(
            'member-day',
            '会员日设置',
            'todo',
            '价格配置',
            '需要新增会员日规则，可按周一到周日配置；结算时先判断用户会员身份和当前日期，再套用会员日价格或优惠。',
          ),
          feature(
            'time-pricing',
            '不同时间段价格',
            'partial',
            '后端价格规则',
            '后端已有价格规则基础，可支持首段价格和超时单价；还需要增加多时间段、半小时内单价、半小时后单价等阶梯规则配置。',
          ),
          feature(
            'member-price',
            '会员价格',
            'partial',
            '充值、洗车计费',
            '用户充值成为会员后，洗车计费应优先读取会员价格；目前有会员身份和价格字段基础，后续需要在计费服务里统一结算优先级。',
          ),
          feature(
            'carousel-gif',
            '轮播图和 GIF 动画',
            'todo',
            '首页运营位',
            '需要新增轮播图配置：图片或 GIF 地址、跳转类型、排序、上下架时间；点击洗车步骤图时跳转洗车教程。',
          ),
          feature(
            'voucher-platform',
            '美团和抖音团购核销',
            'partial',
            '首页团购核销',
            '首页已有核销入口；完整功能需要平台券规则、核销记录、退款撤销、每日核销统计和平台来源字段。',
            '/pages/voucher-redeem/index'
          ),
          feature(
            'douyin-card',
            '抖音次卡取消和退款',
            'todo',
            '次卡管理',
            '需要在后台记录外部平台次卡订单号、用户手机号、剩余次数和退款状态；客户退款时商户端可把对应次卡置为取消。',
          ),
          feature(
            'card-daily-count',
            '次卡购买次数统计',
            'partial',
            '用户资产、经营流水',
            '次卡订单已有数据基础；后续在商户端按日期统计购买次数、购买金额、使用次数和剩余次数。',
            '/pages-admin/assets/index'
          ),
          feature(
            'lottery',
            '微信抽奖',
            'todo',
            '积分商城、活动中心',
            '需要奖品池、抽奖次数、中奖记录、库存扣减和风控规则；建议和积分商城共用奖品库存。',
          ),
        ] as FeatureItem[],
      },
      {
        title: 'PC 后台、硬件和支付',
        desc: '电脑后台、硬件控制器、支付配置和试用版上传前必须注意的基础条件。',
        items: [
          feature(
            'pc-admin',
            '电脑 PC 后台',
            'done',
            '后台管理页',
            'PC 后台用于管理用户、门店、订单、设备、充值、次卡、加盟联系和统计数据；适合总部人员做批量配置和查询。',
          ),
          feature(
            'cabinet',
            '柜机和控制器对接',
            'todo',
            '设备管理',
            '柜机、控制器、门禁、云音响、毛巾柜需要先确认厂商协议，再把设备编号、控制命令、回调状态接入 Device 模块。',
          ),
          feature(
            'stuck-order',
            '卡单和无法取消订单处理',
            'partial',
            '商户端订单、PC 后台订单',
            '订单查询已具备基础；后续需要补充强制结束、退款、释放工位、控制器状态校准和操作日志，解决控制器卡单问题。',
            '/pages-admin/orders/index'
          ),
          feature(
            'payment-config',
            '平台支付和商家助手序列号',
            'partial',
            'PC 后台配置',
            '支付基础配置已保留；正式试用前需要确认商户号、证书、回调域名、合法域名和商家助手序列号是否与当前环境一致。',
          ),
          feature(
            'security',
            '阿里云安全和试用版上传',
            'partial',
            '服务器、微信开发者工具',
            '试用版上传前要清理无用依赖、确认合法域名、HTTPS、接口可用、定位权限说明、隐私协议和服务器安全组；阿里云需要继续做漏洞扫描和弱口令检查。',
          ),
        ] as FeatureItem[],
      },
    ],
  },

  handleFeatureTap(e: WechatMiniprogram.TouchEvent) {
    const route = String(e.currentTarget.dataset.route || '');
    if (!route) {
      wx.showToast({
        title: '该功能需要继续接入',
        icon: 'none',
      });
      return;
    }
    if (route.indexOf('/pages-admin/') === 0) {
      wx.navigateTo({ url: route });
      return;
    }
    if (
      route === '/pages/home/index' ||
      route === '/pages/service/index' ||
      route === '/pages/ranking/index' ||
      route === '/pages/order/index' ||
      route === '/pages/mine/index'
    ) {
      wx.switchTab({ url: route });
      return;
    }
    wx.navigateTo({ url: route });
  },
});
