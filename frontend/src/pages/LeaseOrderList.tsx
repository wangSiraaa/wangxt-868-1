import { useEffect, useRef, useState } from 'react'
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
  Upload,
  Divider,
  Steps,
  Alert,
  Progress,
  Result,
  Typography,
  Row,
  Col,
  Card,
  Statistic,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  StopOutlined,
  UploadOutlined,
  DownloadOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InboxOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import dayjs from 'dayjs'
import type { LeaseOrder, Robot, BatchImportResult } from '@/types'
import { LeaseOrderStatusText, LeaseOrderStatusColor } from '@/types'
import { leaseOrderApi, robotApi } from '@/api'

const { Title, Text, Paragraph } = Typography
const { Dragger } = Upload

export default function LeaseOrderList() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<LeaseOrder[]>([])
  const [robots, setRobots] = useState<Robot[]>([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<LeaseOrder | null>(null)
  const [form] = Form.useForm()

  const [importOpen, setImportOpen] = useState(false)
  const [importStep, setImportStep] = useState(0)
  const [importFile, setImportFile] = useState<File | null>(null)
  const [importResult, setImportResult] = useState<BatchImportResult | null>(null)
  const [importLoading, setImportLoading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

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

  const openImport = () => {
    setImportStep(0)
    setImportFile(null)
    setImportResult(null)
    setImportOpen(true)
  }

  const closeImport = () => {
    if (importLoading) return
    setImportOpen(false)
  }

  const downloadTemplate = async () => {
    try {
      const blob: any = await leaseOrderApi.downloadTemplate()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = '租赁单导入模板.xlsx'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      message.success('模板下载成功')
    } catch (e: any) {
      message.error(e.message || '模板下载失败')
    }
  }

  const beforeUpload = (file: File) => {
    const isExcel = /\.(xlsx|xls)$/i.test(file.name)
    if (!isExcel) {
      message.error('仅支持 .xlsx 或 .xls 格式的文件')
      return false
    }
    const isLt10M = file.size / 1024 / 1024 < 10
    if (!isLt10M) {
      message.error('文件大小不能超过 10MB')
      return false
    }
    setImportFile(file)
    return false
  }

  const removeImportFile = () => {
    setImportFile(null)
  }

  const goNext = () => {
    if (importStep === 0) {
      if (!importFile) {
        message.warning('请先上传导入文件')
        return
      }
      setImportStep(1)
      doImport()
    } else if (importStep === 2) {
      closeImport()
      if (importResult && importResult.successCount > 0) {
        load()
      }
    }
  }

  const goPrev = () => {
    if (importStep === 1) {
      setImportStep(0)
    }
  }

  const doImport = async () => {
    if (!importFile) return
    try {
      setImportLoading(true)
      const res: any = await leaseOrderApi.batchImport(importFile)
      setImportResult(res.data)
      setImportStep(2)
    } catch (e: any) {
      message.error(e.message || '导入失败')
      setImportStep(0)
    } finally {
      setImportLoading(false)
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
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>租赁订单管理</h2>
        <Space>
          <Button icon={<UploadOutlined />} onClick={openImport}>
            批量导入
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建订单
          </Button>
        </Space>
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

      <Modal
        title="批量导入租赁单"
        open={importOpen}
        onCancel={closeImport}
        onOk={goNext}
        okText={importStep === 0 ? '下一步：开始导入' : importStep === 1 ? '导入中...' : '完成并关闭'}
        cancelText={importStep === 0 ? '取消' : importStep === 1 ? '' : ''}
        width={820}
        destroyOnClose
        maskClosable={!importLoading}
        confirmLoading={importLoading}
        cancelButtonProps={{ style: importStep > 0 ? { display: 'none' } : {} }}
        okButtonProps={{ disabled: importStep === 1 }}
      >
        <div style={{ padding: '4px 0 12px' }}>
          <Steps
            current={importStep}
            items={[
              { title: '准备文件', description: '下载模板并填写' },
              { title: '上传导入', description: '校验并写入数据库' },
              { title: '导入结果', description: '查看成功与失败明细' },
            ]}
            style={{ marginBottom: 24 }}
          />

          {importStep === 0 && (
            <div>
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 20 }}
                message="操作说明"
                description={
                  <div>
                    <Paragraph style={{ marginBottom: 4 }}>
                      1. 点击下方按钮下载导入模板（Excel格式）
                    </Paragraph>
                    <Paragraph style={{ marginBottom: 4 }}>
                      2. 按照模板要求填写租赁单数据，其中<span style={{ color: '#cf1322', fontWeight: 600 }}>标 * 号的列为必填</span>
                    </Paragraph>
                    <Paragraph style={{ marginBottom: 4 }}>
                      3. 时间格式必须为 <Text code>yyyy-MM-dd HH:mm</Text>（例如 2025-01-15 08:00）
                    </Paragraph>
                    <Paragraph style={{ margin: 0 }}>
                      4. 设备编码需要先在"设备管理"中登记存在
                    </Paragraph>
                  </div>
                }
              />
              <Space direction="vertical" style={{ width: '100%' }} size="large">
                <div>
                  <Button
                    type="dashed"
                    icon={<DownloadOutlined />}
                    onClick={downloadTemplate}
                    size="large"
                    block
                  >
                    下载 Excel 导入模板
                  </Button>
                </div>
                <div>
                  <Divider orientation="left">上传填写好的文件</Divider>
                  {!importFile ? (
                    <Dragger
                      name="file"
                      multiple={false}
                      accept=".xlsx,.xls"
                      beforeUpload={beforeUpload}
                      showUploadList={false}
                      height={180}
                    >
                      <p className="ant-upload-drag-icon">
                        <InboxOutlined />
                      </p>
                      <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
                      <p className="ant-upload-hint">
                        仅支持 .xlsx 或 .xls 格式，文件大小不超过 10MB
                      </p>
                    </Dragger>
                  ) : (
                    <Alert
                      type="success"
                      showIcon
                      icon={<CheckCircleOutlined />}
                      message="已选择文件"
                      description={
                        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                          <span>📄 {importFile.name} ({(importFile.size / 1024).toFixed(1)} KB)</span>
                          <Button
                            size="small"
                            type="link"
                            danger
                            onClick={removeImportFile}
                            disabled={importLoading}
                          >
                            重新选择
                          </Button>
                        </Space>
                      }
                      style={{ padding: '12px 16px' }}
                    />
                  )}
                </div>
              </Space>
            </div>
          )}

          {importStep === 1 && (
            <div style={{ padding: '40px 0', textAlign: 'center' }}>
              <Spin spinning tip="正在解析并写入数据库，请稍候...">
                <div style={{ padding: 80 }}>
                  <Progress
                    type="circle"
                    percent={60}
                    status="active"
                    strokeColor={{ '0%': '#108ee9', '100%': '#87d068' }}
                  />
                  <div style={{ marginTop: 16 }}>
                    <Text type="secondary">
                      {importFile?.name}
                    </Text>
                  </div>
                </div>
              </Spin>
            </div>
          )}

          {importStep === 2 && importResult && (
            <div>
              <Result
                status={importResult.failCount === 0 ? 'success' : 'warning'}
                title={
                  importResult.failCount === 0
                    ? '全部导入成功！'
                    : `部分导入完成（成功 ${importResult.successCount}，失败 ${importResult.failCount}）`
                }
                subTitle={`共处理 ${importResult.totalCount} 条记录`}
              />
              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                  <Card size="small">
                    <Statistic
                      title="成功导入"
                      value={importResult.successCount}
                      valueStyle={{ color: '#3f8600' }}
                      prefix={<CheckCircleOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card size="small">
                    <Statistic
                      title="导入失败"
                      value={importResult.failCount}
                      valueStyle={{ color: importResult.failCount > 0 ? '#cf1322' : undefined }}
                      prefix={<ExclamationCircleOutlined />}
                    />
                  </Card>
                </Col>
              </Row>
              {importResult.successOrderNos.length > 0 && (
                <div style={{ marginTop: 20 }}>
                  <Title level={5} style={{ marginBottom: 12 }}>✅ 成功生成的订单号</Title>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                    {importResult.successOrderNos.map((no, idx) => (
                      <Tag key={idx} color="green">{no}</Tag>
                    ))}
                  </div>
                </div>
              )}
              {importResult.errors.length > 0 && (
                <div style={{ marginTop: 20 }}>
                  <Title level={5} style={{ marginBottom: 12, color: '#cf1322' }}>❌ 错误明细</Title>
                  <Table
                    size="small"
                    rowKey={(_, idx) => String(idx)}
                    pagination={{ pageSize: 5, size: 'small' }}
                    dataSource={importResult.errors}
                    columns={[
                      { title: '行号', dataIndex: 'rowNum', key: 'rowNum', width: 70, align: 'center' },
                      { title: '字段', dataIndex: 'field', key: 'field', width: 120 },
                      { title: '错误原因', dataIndex: 'message', key: 'message' },
                      { title: '原始值', dataIndex: 'originalValue', key: 'originalValue', width: 160, ellipsis: true },
                    ]}
                  />
                </div>
              )}
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}
