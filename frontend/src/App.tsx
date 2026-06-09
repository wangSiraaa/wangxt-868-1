import { Layout, Menu, theme } from 'antd'
import { Routes, Route, Link, useLocation, Navigate } from 'react-router-dom'
import {
  RobotOutlined,
  ShoppingCartOutlined,
  DashboardOutlined,
} from '@ant-design/icons'
import Dashboard from '@/pages/Dashboard'
import RobotList from '@/pages/RobotList'
import LeaseOrderList from '@/pages/LeaseOrderList'
import LeaseOrderDetail from '@/pages/LeaseOrderDetail'

const { Header, Sider, Content } = Layout

function App() {
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  const location = useLocation()

  const getSelectedKey = () => {
    if (location.pathname.startsWith('/lease-orders')) return '2'
    if (location.pathname.startsWith('/robots')) return '1'
    return '0'
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible>
        <div
          style={{
            height: 64,
            margin: 16,
            borderRadius: 8,
            background: 'rgba(255,255,255,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontWeight: 600,
            fontSize: 16,
          }}
        >
          机器人租赁结算
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[getSelectedKey()]}
          items={[
            {
              key: '0',
              icon: <DashboardOutlined />,
              label: <Link to="/">工作台</Link>,
            },
            {
              key: '1',
              icon: <RobotOutlined />,
              label: <Link to="/robots">机器人设备</Link>,
            },
            {
              key: '2',
              icon: <ShoppingCartOutlined />,
              label: <Link to="/lease-orders">租赁订单</Link>,
            },
          ]}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: colorBgContainer }}>
          <div
            style={{
              paddingLeft: 24,
              fontSize: 18,
              fontWeight: 500,
              lineHeight: '64px',
            }}
          >
            工业机器人租赁维修结算管理系统
          </div>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            overflow: 'auto',
          }}
        >
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/robots" element={<RobotList />} />
            <Route path="/lease-orders" element={<LeaseOrderList />} />
            <Route path="/lease-orders/:id" element={<LeaseOrderDetail />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}

export default App
