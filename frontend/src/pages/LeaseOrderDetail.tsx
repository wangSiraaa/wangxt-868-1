import { useEffect, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Descriptions,
  InputNumber,
  DatePicker,
  Modal,
  Form,
  Input,
  message,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  Tabs,
  Tooltip,
  Alert,
  Divider,
  Result,
  Popconfirm,
} from 'antd'
import {
  ArrowLeftOutlined,
  PlusOutlined,
  CalculatorOutlined,
  EditOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  SafetyOutlined,
  RollbackOutlined,
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import {
  leaseOrderApi,
  maintenanceApi,
  runningHourApi,
  settlementApi,
} from '@/api'
import type {
  LeaseOrder,
  RunningHour,
  MaintenanceRecord,
  Settlement,
  SettlementStatus,
} from '@/types'
import {
  MaintenanceStatusText,
  MaintenanceStatusColor,
  LeaseOrderStatusText,
  LeaseOrderStatusColor,
  SettlementStatusText,
  SettlementStatusColor,
} from '@/types'

const { RangePicker } = DatePicker

export default function LeaseOrderDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const orderId = Number(id!)

  const [loading, setLoading] = useState(false)
  const [order, setOrder] = useState<LeaseOrder | null>(null)
  const [runningHours, setRunningHours] = useState<RunningHour[]>([])
  const [maintenance, setMaintenance] = useState<MaintenanceRecord[]>([])
  const [settlement, setSettlement] = useState<Settlement | null>(null)

  const [rhModal, setRhModal] = useState<{ open: boolean; editing?: RunningHour }>({ open: false })
  const [mtModal, setMtModal] = useState<{ open: boolean; editing?: MaintenanceRecord }>({ open: false })
  const [rhForm] = Form.useForm()
  const [mtForm] = Form.useForm()

  const locked = settlement?.status === 'CONFIRMED'

  const loadAll = async () => {
    try {
      setLoading(true)
      const [oRes, rhRes, mtRes, sRes] = await Promise.all([
        leaseOrderApi.get(orderId),
        runningHourApi.list(orderId),
        maintenanceApi.list(orderId),
        (async () => {
          try {
            return await settlementApi.latest(orderId)
          } catch {
            return { data: null } as any
          }
        })(),
      ])
      setOrder((oRes as any).data)
      setRunningHours((rhRes as any).data || [])
      setMaintenance((mtRes as any).data || [])
      setSettlement((sRes as any).data)
    } catch (e: any) {
      message.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [orderId])

  const openRh = (record?: RunningHour) => {
    setRhModal({ open: true, editing: record })
    rhForm.resetFields()
    if (record) {
      rhForm.setFieldsValue({
        reportDate: record.reportDate ? dayjs(record.reportDate) : null,
        reportedHours: record.reportedHours,
      })
    } else {
      rhForm.setFieldsValue({ reportedHours: 8 })
    }
  }

  const submitRh = async (values: any) => {
    try {
      const payload = {
        reportDate: values.reportDate.format('YYYY-MM-DD'),
        reportedHours: values.reportedHours,
      }
      if (rhModal.editing) {
        await runningHourApi.update(orderId, rhModal.editing.id!, payload)
      } else {
        await runningHourApi.create(orderId, payload)
      }
      message.success('保存成功，已自动重算扣减小时')
      setRhModal({ open: false })
      loadAll()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const removeRh = async (r: RunningHour) => {
    try {
      await runningHourApi.remove(orderId, r.id!)
      message.success('删除成功')
      loadAll()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const openMt = (record?: MaintenanceRecord) => {
    setMtModal({ open: true, editing: record })
    mtForm.resetFields()
    if (record) {
      mtForm.setFieldsValue({
        timeRange: [
          record.startTime ? dayjs(record.startTime) : null,
          record.endTime ? dayjs(record.endTime) : null,
        ],
        description: record.description,
        maintenanceCost: record.maintenanceCost,
        status: record.status,
      })
    } else {
      mtForm.setFieldsValue({ status: 'COMPLETED', maintenanceCost: 0 })
    }
  }

  const submitMt = async (values: any) => {
    try {
      const [startDay, endDay] = values.timeRange || []
      if (!startDay || !endDay) {
        message.error('Please select start and end time')
        return
      }
      if (endDay.isBefore(startDay)) {
        message.error('End time before start')
        return
      }
      const payload: any = {
        startTime: startDay.format('YYYY-MM-DDTHH:mm:ss'),
        endTime: endDay.format('YYYY-MM-DDTHH:mm:ss'),
        description: values.description,
        maintenanceCost: values.maintenanceCost ?? 0,
        status: values.status ?? 'COMPLETED',
      }
      let res: any
      if (mtModal.editing) {
        res = await maintenanceApi.update(orderId, mtModal.editing.id!, payload)
      } else {
        res = await maintenanceApi.create(orderId, payload)
      }
      const dtHours = res?.data?.downtimeHours
      const sfx = dtHours != null ? (", Downtime " + Number(dtHours).toFixed(2) + "h deducted") : ", deduction recalculated"
      const pref = mtModal.editing ? "Updated" : "Registered"
      message.success(pref + " maintenance OK" + sfx)
      setMtModal({ open: false })
      loadAll()
    } catch (e: any) {
      message.error(e.message || 'Save failed')
    }
  }


  const removeMt = async (m: MaintenanceRecord) => {
    try {
      await maintenanceApi.remove(orderId, m.id!)
      message.success('删除成功，运行小时已自动重算')
      loadAll()
    } catch (e: any) {
      message.error(e.message)
    }
  }

  const calculate = async () => {
    Modal.confirm({
      title: '确认重新计算结算单？',
      onOk: async () => {
        try {
          const res: any = await settlementApi.calculate(orderId)
          setSettlement(res.data)
          message.success('费用计算完成')
          loadAll()
        } catch (e: any) {
          message.error(e.message)
        }
      },
    })
  }

  const review = async () => {
    Modal.confirm({
      title: '确认复核？',
      onOk: async () => {
        try {
          const res: any = await settlementApi.review(orderId)
          setSettlement(res.data)
          message.success('已复核，请财务结算')
          loadAll()
        } catch (e: any) {
          message.error(e.message)
        }
      },
    })
  }

  const confirm = async () => {
    Modal.confirm({
      title: '⚠️ 确认结算？',
      content: '结算确认后，所有维修记录、运行小时将被锁定不可修改，是否继续？',
      okText: '确认结算',
      okType: 'danger',
      onOk: async () => {
        try {
          const res: any = await settlementApi.confirm(orderId)
          setSettlement(res.data)
          message.success('结算确认完成，数据已锁定')
          loadAll()
        } catch (e: any) {
          message.error(e.message)
        }
      },
    })
  }

  const cancelConfirm = async () => {
    Modal.confirm({
      title: '取消结算确认？',
      onOk: async () => {
        try {
          const res: any = await settlementApi.cancelConfirm(orderId)
          setSettlement(res.data)
          message.success('已取消确认')
          loadAll()
        } catch (e: any) {
          message.error(e.message)
        }
      },
    })
  }

  if (!order) return <Spin />

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
          返回
        </Button>
      </div>

      {locked && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="结算已确认，所有数据已锁定，不可修改维修记录和运行小时"
          icon={<SafetyOutlined />}
        />
      )}

      <Card
        title="租赁订单"
        style={{ marginBottom: 16 }}
        extra={
          <Tag
            color={
              (LeaseOrderStatusColor as any)[order.status!] || 'default'
            }
          >
            {LeaseOrderStatusText[order.status!]}
          </Tag>
        }
      >
        <Descriptions bordered column={{ xs: 1, sm: 2, md: 3 }} size="small">
          <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
          <Descriptions.Item label="承租工厂">{order.lesseeFactory}</Descriptions.Item>
          <Descriptions.Item label="联系人">
            {order.contactPerson} / {order.contactPhone}
          </Descriptions.Item>
          <Descriptions.Item label="租期开始">
            {order.startTime ? dayjs(order.startTime).format('YYYY-MM-DD HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="租期结束">
            {order.endTime ? dayjs(order.endTime).format('YYYY-MM-DD HH:mm') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="设备ID">{order.robotId}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Tabs
        items={[
          {
            key: '1',
            label: '运行小时上报',
            children: (
              <Card
                title="运行小时（每日上报）"
                extra={
                  <Space>
                    <Tooltip title="录入维修停机时段后会自动扣减当日计费小时">
                      <Tag color="blue">
                        计费小时 = 上报小时 - 维修停机小时
                      </Tag>
                    </Tooltip>
                    <Button
                      type="primary"
                      icon={<PlusOutlined />}
                      onClick={() => openRh()}
                      disabled={locked}
                    >
                      上报运行小时
                    </Button>
                  </Space>
                }
              >
                <Table
                  rowKey="id"
                  size="middle"
                  dataSource={runningHours}
                  pagination={false}
                  columns={[
                    {
                      title: '日期',
                      dataIndex: 'reportDate',
                      key: 'reportDate',
                      width: 140,
                      render: (v) => (v ? dayjs(v).format('YYYY-MM-DD') : '-'),
                    },
                    {
                      title: '上报运行小时',
                      dataIndex: 'reportedHours',
                      key: 'reportedHours',
                      width: 140,
                      render: (v) => <b>{Number(v).toFixed(2)} h</b>,
                    },
                    {
                      title: '维修停机扣减',
                      dataIndex: 'deductionHours',
                      key: 'deductionHours',
                      width: 140,
                      render: (v) =>
                        Number(v) > 0 ? (
                          <Tag color="red">-{Number(v).toFixed(2)} h</Tag>
                        ) : (
                          <span style={{ color: '#888' }}>0 h</span>
                        ),
                    },
                    {
                      title: '计费小时',
                      dataIndex: 'billableHours',
                      key: 'billableHours',
                      width: 140,
                      render: (v: any) => (
                        <span style={{ color: '#1677ff', fontWeight: 600 }}>
                          {Number(v).toFixed(2)} h
                        </span>
                      ),
                    },
                    {
                      title: '操作',
                      key: 'action',
                      width: 160,
                      render: (_any, row: any) => (
                        <Space>
                          <Button
                            size="small"
                            type="link"
                            icon={<EditOutlined />}
                            disabled={locked}
                            onClick={() => openRh(row)}
                          >
                            编辑
                          </Button>
                          <Popconfirm
                            title="确认删除？"
                            onConfirm={() => removeRh(row)}
                          >
                            <Button
                              size="small"
                              type="link"
                              danger
                              icon={<DeleteOutlined />}
                              disabled={locked}
                            >
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </Card>
            ),
          },
          {
            key: '2',
            label: '维修登记',
            children: (
              <Card
                title="维修登记（停机时段）"
                extra={
                  <Space>
                    <Tooltip title="维修登记变动会触发运行小时自动重算">
                      <Tag color="orange">
                        自动重算计费小时
                      </Tag>
                    </Tooltip>
                    <Button
                      type="primary"
                      icon={<PlusOutlined />}
                      onClick={() => openMt()}
                      disabled={locked}
                    >
                      登记维修
                    </Button>
                  </Space>
                }
              >
                <Table
                  rowKey="id"
                  size="middle"
                  dataSource={maintenance}
                  pagination={false}
                  columns={[
                    {
                      title: '开始时间',
                      dataIndex: 'startTime',
                      key: 'startTime',
                      render: (v) =>
                        v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
                    },
                    {
                      title: '结束时间',
                      dataIndex: 'endTime',
                      key: 'endTime',
                      render: (v) =>
                        v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
                    },
                    {
                      title: '停机时长',
                      dataIndex: 'downtimeHours',
                      key: 'downtimeHours',
                      render: (v) => <b>{Number(v).toFixed(2)} h</b>,
                    },
                    {
                      title: '维修费用',
                      dataIndex: 'maintenanceCost',
                      key: 'maintenanceCost',
                      render: (v) =>
                        v ? `¥${Number(v).toFixed(2)}` : '¥0.00',
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      key: 'status',
                      render: (s: any) => (
                        <Tag
                          color={
                            (MaintenanceStatusColor as any)[s] || 'default'
                          }
                        >
                          {(MaintenanceStatusText as any)[s]}
                        </Tag>
                      ),
                    },
                    {
                      title: '描述',
                      dataIndex: 'description',
                      key: 'description',
                      ellipsis: true,
                    },
                    {
                      title: '操作',
                      key: 'action',
                      width: 160,
                      render: (_any, row: any) => (
                        <Space>
                          <Button
                            size="small"
                            type="link"
                            icon={<EditOutlined />}
                            disabled={locked}
                            onClick={() => openMt(row)}
                          >
                            编辑
                          </Button>
                          <Popconfirm
                            title="确认删除？"
                            onConfirm={() => removeMt(row)}
                          >
                            <Button
                              size="small"
                              type="link"
                              danger
                              icon={<DeleteOutlined />}
                              disabled={locked}
                            >
                              删除
                            </Button>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </Card>
            ),
          },
          {
            key: '3',
            label: '费用结算',
            children: (
              <div>
                <Card
                  title="结算单"
                  style={{ marginBottom: 16 }}
                  extra={
                    <Space>
                      {settlement && (
                        <Tag
                          color={
                            (SettlementStatusColor as any)[settlement.status ?? 'DRAFT'] ||
                            'default'
                          }
                        >
                          {(SettlementStatusText as any)[settlement.status ?? 'DRAFT']}
                        </Tag>
                      )}
                      <Button
                        icon={<CalculatorOutlined />}
                        onClick={calculate}
                        disabled={settlement?.status === 'CONFIRMED'}
                      >
                        {settlement ? '重新计算' : '计算费用'}
                      </Button>
                      {settlement?.status === 'DRAFT' && (
                        <Button type="primary" onClick={review}>
                          复核
                        </Button>
                      )}
                      {settlement?.status === 'REVIEWED' && (
                        <Button
                          type="primary"
                          danger
                          icon={<CheckCircleOutlined />}
                          onClick={confirm}
                        >
                          确认结算（锁定）
                        </Button>
                      )}
                      {settlement?.status === 'CONFIRMED' && (
                        <Button
                          icon={<RollbackOutlined />}
                          onClick={cancelConfirm}
                        >
                          取消确认
                        </Button>
                      )}
                    </Space>
                  }
                >
                  {!settlement ? (
                    <Result
                      status="info"
                      title="还未生成结算单"
                      subTitle="点击「计算费用」生成结算单"
                    />
                  ) : (
                    <div>
                      <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small">
                        <Descriptions.Item label="结算单号">
                          {settlement.settlementNo}
                        </Descriptions.Item>
                        <Descriptions.Item label="生成时间">
                          {settlement.createTime
                            ? dayjs(settlement.createTime).format(
                                'YYYY-MM-DD HH:mm'
                              )
                            : '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="复核时间">
                          {settlement.reviewTime
                            ? dayjs(settlement.reviewTime).format(
                                'YYYY-MM-DD HH:mm'
                              )
                            : '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="确认时间">
                          {settlement.confirmTime
                            ? dayjs(settlement.confirmTime).format(
                                'YYYY-MM-DD HH:mm'
                              )
                            : '-'}
                        </Descriptions.Item>
                      </Descriptions>
                      <Divider orientation="left">
                        <b>费用明细</b>
                      </Divider>
                      <Row gutter={[16, 16]}>
                        <Col xs={12} sm={6}>
                          <Card size="small">
                            <Statistic
                              title="总计费运行小时"
                              value={settlement.totalBillableHours}
                              precision={2}
                              suffix="h"
                            />
                          </Card>
                        </Col>
                        <Col xs={12} sm={6}>
                          <Card size="small">
                            <Statistic
                              title="基础租金（小时×费率）"
                              value={settlement.baseRent}
                              precision={2}
                              prefix="¥"
                              valueStyle={{ color: '#1677ff' }}
                            />
                          </Card>
                        </Col>
                        <Col xs={12} sm={6}>
                          <Card size="small">
                            <Statistic
                              title="维修费用"
                              value={settlement.maintenanceTotal}
                              precision={2}
                              prefix="¥"
                              valueStyle={{ color: '#fa8c16' }}
                            />
                          </Card>
                        </Col>
                        <Col xs={12} sm={6}>
                          <Card size="small">
                            <Statistic
                              title="结算总金额"
                              value={settlement.totalAmount}
                              precision={2}
                              prefix="¥"
                              valueStyle={{ color: '#cf1322', fontWeight: 700 }}
                            />
                          </Card>
                        </Col>
                      </Row>
                    </div>
                  )}
                </Card>
              </div>
            ),
          },
        ]}
      />

      <Modal
        title={rhModal.editing ? '编辑运行小时' : '上报运行小时'}
        open={rhModal.open}
        onCancel={() => setRhModal({ open: false })}
        onOk={() => rhForm.submit()}
        destroyOnClose
      >
        <Form form={rhForm} layout="vertical" onFinish={submitRh}>
          <Form.Item
            label="日期"
            name="reportDate"
            rules={[{ required: true, message: '必填' }]}
          >
            <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item
            label="运行小时"
            name="reportedHours"
            rules={[{ required: true, message: '必填' }]}
          >
            <InputNumber
              min={0}
              max={24}
              step={0.5}
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={mtModal.editing ? '编辑维修登记' : '登记维修'}
        open={mtModal.open}
        onCancel={() => setMtModal({ open: false })}
        onOk={() => mtForm.submit()}
        destroyOnClose
      >
        <Form form={mtForm} layout="vertical" onFinish={submitMt}>
          <Form.Item
            label="维修时段（开始时间 ～ 结束时间）"
            name="timeRange"
            rules={[{ required: true, message: '请选择维修开始和结束时间' }]}
          >
            <RangePicker
              showTime={{ format: 'HH:mm' }}
              format="YYYY-MM-DD HH:mm"
              style={{ width: '100%' }}
              placeholder={['维修开始时间', '维修结束时间']}
            />

          </Form.Item>
          <Form.Item label="维修描述" name="description">
            <Input.TextArea rows={3} placeholder="如 减速器更换、故障描述" />
          </Form.Item>
          <Form.Item label="维修费用" name="maintenanceCost">
            <InputNumber
              min={0}
              step={100}
              prefix="¥"
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select
              options={[
                { value: 'PENDING', label: '待处理' },
                { value: 'IN_PROGRESS', label: '维修中' },
                { value: 'COMPLETED', label: '已完成' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Spin>
  )
}
