import { useEffect, useState } from 'react'
import {
  Button,
  Table,
  Space,
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  Tag,
  message,
  Popconfirm,
  Spin,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { Robot, RobotStatus } from '@/types'
import { RobotStatusText, RobotStatusColor } from '@/types'
import { robotApi } from '@/api'

export default function RobotList() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<Robot[]>([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Robot | null>(null)
  const [form] = Form.useForm()

  const load = async () => {
    try {
      setLoading(true)
      const res: any = await robotApi.list(0, 100)
      setData(res.data?.content || [])
    } catch (e: any) {
      message.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ status: 'IDLE', hourlyRate: 100 })
    setOpen(true)
  }

  const openEdit = (row: Robot) => {
    setEditing(row)
    form.setFieldsValue({
      robotCode: row.code || (row as any).robotCode,
      robotName: row.name || (row as any).robotName,
      model: row.model,
      manufacturer: row.manufacturer,
      hourlyRate: row.hourlyRate,
      status: row.status,
    })
    setOpen(true)
  }

  const submit = async (values: any) => {
    try {
      if (editing) {
        await robotApi.update(editing.id!, values)
        message.success('修改成功')
      } else {
        await robotApi.create(values)
        message.success('创建成功')
      }
      setOpen(false)
      load()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const remove = async (id: number) => {
    try {
      await robotApi.remove(id)
      message.success('删除成功')
      load()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const columns = [
    { title: '编码', dataIndex: 'code', key: 'code', render: (v: string, r: any) => v || r.robotCode },
    { title: '名称', dataIndex: 'name', key: 'name', render: (v: string, r: any) => v || r.robotName },
    { title: '型号', dataIndex: 'model', key: 'model' },
    { title: '制造商', dataIndex: 'manufacturer', key: 'manufacturer' },
    {
      title: '小时费率（元）',
      dataIndex: 'hourlyRate',
      key: 'hourlyRate',
      render: (v: any) => `¥${Number(v).toFixed(2)}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (s: RobotStatus) => (
        <Tag color={(RobotStatusColor as any)[s] || 'default'}>{RobotStatusText[s]}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_: any, row: any) => (
        <Space>
          <Button
            size="small"
            type="link"
            icon={<EditOutlined />}
            onClick={() => openEdit(row as Robot)}
          >
            编辑
          </Button>
          <Popconfirm title="确认删除？" onConfirm={() => remove((row as Robot).id!)}>
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2 style={{ margin: 0 }}>机器人设备管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增设备
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
        title={editing ? '编辑设备' : '新增设备'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item label="设备编码" name="robotCode" rules={[{ required: true, message: '必填' }]}>
            <Input placeholder="如 RB001" />
          </Form.Item>
          <Form.Item label="设备名称" name="robotName" rules={[{ required: true, message: '必填' }]}>
            <Input placeholder="如 WelderA 焊接机器人" />
          </Form.Item>
          <Form.Item label="型号" name="model">
            <Input placeholder="如 Welder-X100" />
          </Form.Item>
          <Form.Item label="制造商" name="manufacturer">
            <Input placeholder="如 某机器人公司" />
          </Form.Item>
          <Form.Item
            label="小时费率（元/小时）"
            name="hourlyRate"
            rules={[{ required: true, message: '必填' }]}
          >
            <InputNumber min={0} step={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="初始状态" name="status">
            <Select
              options={[
                { value: 'IDLE', label: '空闲' },
                { value: 'MAINTENANCE', label: '维修中' },
                { value: 'SCRAPPED', label: '已报废' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
