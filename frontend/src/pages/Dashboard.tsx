import { useEffect, useState } from 'react'
import { Card, Col, Row, Statistic, Spin, message } from 'antd'
import {
  RobotOutlined,
  ThunderboltOutlined,
  ShoppingOutlined,
  DollarOutlined,
} from '@ant-design/icons'
import { dashboardApi } from '@/api'

export default function Dashboard() {
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState<any>({})

  useEffect(() => {
    fetchStats()
  }, [])

  const fetchStats = async () => {
    try {
      setLoading(true)
      const res: any = await dashboardApi.stats()
      setStats(res.data || {})
    } catch (e: any) {
      message.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Spin spinning={loading}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="设备总数"
              value={stats.totalRobots || 0}
              prefix={<RobotOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="租赁中设备"
              value={stats.activeRobots || 0}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="执行中订单"
              value={stats.activeOrders || 0}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="已结算收入（元）"
              value={stats.totalRevenue || 0}
              precision={2}
              prefix={<DollarOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 24 }} title="功能导航">
        <p style={{ marginBottom: 12, color: '#666' }}>
          欢迎使用工业机器人租赁维修结算管理系统。功能使用流程如下：
        </p>
        <ol style={{ paddingLeft: 20, lineHeight: 2.2, color: '#444' }}>
          <li>
            <b>设备管理</b>：录入机器人设备档案（编码、名称、小时费率等）
          </li>
          <li>
            <b>租赁订单</b>：承租工厂提交租赁订单，选择设备、约定租期，激活后开始计费
          </li>
          <li>
            <b>小时上报</b>：每日上报设备实际运行小时数
          </li>
          <li>
            <b>维修登记</b>：登记维修停机时段，系统<b>自动从运行小时中扣减</b>，不计租金
          </li>
          <li>
            <b>费用计算</b>：按计费小时 × 小时费率 + 维修费 = 结算总额
          </li>
          <li>
            <b>复核 → 确认</b>：三级状态流转，确认后<b>维修记录锁定不可修改</b>
          </li>
        </ol>
      </Card>
    </Spin>
  )
}
