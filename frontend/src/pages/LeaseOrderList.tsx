import { useEffect, useState } from 'react'
import {
  Button,
  Table,
  Space,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  Tag,
  message,
  Popconfirm,
  Spin,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  StopOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import type { LeaseOrder, Robot } from '@/types'
import { LeaseOrderStatusText, LeaseOrderStatusColor } from '@/types'
import { leaseOrderApi, robotApi } from '@/api'

export default function LeaseOrderList() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<LeaseOrder[]>([])
  const [robots, setRobots] = useState<Robot[]>([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<LeaseOrder | null>(null)
  const [form] = Form.useForm()

  const load = async () => {
    try {
      setLoading(true)
      const res: any = await leaseOrderApi.list(0, 100)
      setData(res.data?.content || [])
    } catch (e: any) {
      message.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  const loadRobots = async () => {
    try {
      const res: any = await robotApi.list(0, 100)
      const list = (res.data?.content || []).map((r: any) => ({
        ...r,
        code: r.code || r.robotCode,
        name: r.name || r.robotName,
      }))
      setRobots(list)
    } catch (e: any) {
      message.error(e.message)
    }
  }

  useEffect(() => {
    load()
    loadRobots()
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      status: 'DRAFT',
      startTime: dayjs(),
    })
    setOpen(true)
  }

  const openEdit = (row: any) => {
    setEditing(row)
    form.setFieldsValue({
      orderNo: row.orderNo,
      lesseeFactory: row.lesseeFactory,
      contactPerson: row.contactPerson,
      contactPhone: row.contactPhone,
      robotId: row.robotId,
      startTime: row.startTime ? dayjs(row.startTime) : null,
      endTime: row.endTime ? dayjs(row.endTime) : null,
    })
    setOpen(true)
  }

  const submit = async (values: any) => {
    try {
      const payload = {
        ...values,
        startTime: values.startTime?.toISOString(),
        endTime: values.endTime?.toISOString() || null,
      }
      if (editing) {
        await leaseOrderApi.update(editing.id!, payload)
        message.success('修改成功')
      } else {
        await leaseOrderApi.create(payload)
        message.success('创建成功')
      }
      setOpen(false)
      load()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const activate = async (id: number) => {
    try {
      await leaseOrderApi.activate(id)
      message.success('已激活，开始计费')
      load()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const complete = async (id: number) => {
    try {
      await leaseOrderApi.complete(id)
      message.success('已完成')
      load()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const remove = async (id: number) => {
    try {
      await leaseOrderApi.remove(id)
      message.success('删除成功')
      load()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const getRobotName = (rid: number) => {
    const r = robots.find((x) => x.id === rid)
    return r ? `${r.code || (r as any).robotCode} - ${r.name || (r as any).robotName}` : `ID:${rid}`
  }

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
    { title: '承租工厂', dataIndex: 'lesseeFactory', key: 'lesseeFactory' },
    { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson' },
    { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone' },
    {
      title: '租赁设备',
      dataIndex: 'robotId',
      key: 'robotId',
      render: (rid: number) => getRobotName(rid),
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD') : '-'),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD') : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (s: any) => (
        <Tag color={(LeaseOrderStatusColor as any)[s] || 'default'}>
          {(LeaseOrderStatusText as any)[s]}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 320,
      render: (_: any, row: any) => (
        <Space wrap>
          <Button
            size="small"
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/lease-orders/${row.id}`)}
          >
            详情
          </Button>
          {row.status === 'DRAFT' && (
            <>
              <Button
                size="small"
                type="link"
                icon={<EditOutlined />}
                onClick={() => openEdit(row)}
              >
                编辑
              </Button>
              <Popconfirm title="确认删除？" onConfirm={() => remove(row.id)}>
                <Button size="small" type="link" danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
              <Button
                size="small"
                type="primary"
                ghost
                icon={<PlayCircleOutlined />}
                onClick={() => activate(row.id)}
              >
                激活
              </Button>
            </>
          )}
          {row.status === 'ACTIVE' && (
            <Button
              size="small"
              type="link"
              icon={<StopOutlined />}
              onClick={() => complete(row.id)}
            >
              完成订单
            </Button>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2 style={{ margin: 0 }}>租赁订单管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新建订单
        </Button>
      </div>
      <Spin spinning={loading}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data}
          pagination={{ pageSize: 10 }}
        />
      </Spin>

      <Modal
        title={editing ? '编辑订单' : '新建订单'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={520}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item label="订单号" name="orderNo" rules={[{ required: true, message: '必填' }]}>
            <Input placeholder="如 LO-2025-001" />
          </Form.Item>
          <Form.Item
            label="承租工厂"
            name="lesseeFactory"
            rules={[{ required: true, message: '必填' }]}
          >
            <Input placeholder="如 上海汽配制造有限公司" />
          </Form.Item>
          <Form.Item
            label="联系人"
            name="contactPerson"
            rules={[{ required: true, message: '必填' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            label="联系电话"
            name="contactPhone"
            rules={[{ required: true, message: '必填' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            label="选择设备"
            name="robotId"
            rules={[{ required: true, message: '必选' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={robots.map((r) => ({
                value: r.id,
                label: `${r.code || (r as any).robotCode} - ${r.name || (r as any).robotName}  (¥${r.hourlyRate}/h)`,
              }))}
            />
          </Form.Item>
          <Form.Item
            label="租期开始"
            name="startTime"
            rules={[{ required: true, message: '必填' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="租期结束（可不填）" name="endTime">
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
