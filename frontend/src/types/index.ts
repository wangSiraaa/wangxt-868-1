export interface Robot {
  id?: number
  robotCode?: string
  robotName?: string
  code?: string
  name?: string
  model?: string
  manufacturer?: string
  hourlyRate?: number
  status?: RobotStatus
}

export enum RobotStatus {
  IDLE = 'IDLE',
  RENTED = 'RENTED',
  MAINTENANCE = 'MAINTENANCE',
  SCRAPPED = 'SCRAPPED'
}

export const RobotStatusText: Record<RobotStatus, string> = {
  [RobotStatus.IDLE]: '空闲',
  [RobotStatus.RENTED]: '租赁中',
  [RobotStatus.MAINTENANCE]: '维修中',
  [RobotStatus.SCRAPPED]: '已报废'
}

export enum RobotStatusColor {
  IDLE = 'green',
  RENTED = 'blue',
  MAINTENANCE = 'orange',
  SCRAPPED = 'default'
}

export interface LeaseOrder {
  id?: number
  orderNo: string
  lesseeFactory: string
  contactPerson: string
  contactPhone: string
  robotId: number
  robot?: Robot
  startTime: string
  endTime?: string
  remark?: string
  status: LeaseOrderStatus
  createTime?: string
  updateTime?: string
}

export enum LeaseOrderStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  SETTLED = 'SETTLED'
}

export const LeaseOrderStatusText: Record<LeaseOrderStatus, string> = {
  [LeaseOrderStatus.DRAFT]: '草稿',
  [LeaseOrderStatus.ACTIVE]: '执行中',
  [LeaseOrderStatus.COMPLETED]: '已完成',
  [LeaseOrderStatus.SETTLED]: '已结算'
}

export enum LeaseOrderStatusColor {
  DRAFT = 'default',
  ACTIVE = 'processing',
  COMPLETED = 'warning',
  SETTLED = 'success'
}

export interface RunningHour {
  id?: number
  leaseOrderId: number
  reportDate: string
  reportedHours: number
  deductionHours: number
  billableHours: number
  remark?: string
  createTime?: string
}

export interface MaintenanceRecord {
  id?: number
  leaseOrderId: number
  startTime: string
  endTime?: string
  downtimeHours: number
  description: string
  maintenanceCost: number
  status: MaintenanceStatus
  createTime?: string
  updateTime?: string
}

export enum MaintenanceStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED'
}

export const MaintenanceStatusText: Record<MaintenanceStatus, string> = {
  [MaintenanceStatus.PENDING]: '待处理',
  [MaintenanceStatus.IN_PROGRESS]: '维修中',
  [MaintenanceStatus.COMPLETED]: '已完成'
}

export enum MaintenanceStatusColor {
  PENDING = 'warning',
  IN_PROGRESS = 'processing',
  COMPLETED = 'success'
}

export interface Settlement {
  id?: number
  leaseOrderId?: number
  settlementNo?: string
  totalBillableHours?: number
  baseRent?: number
  maintenanceTotal?: number
  totalAmount?: number
  status?: SettlementStatus
  reviewTime?: string
  confirmTime?: string
  createTime?: string
  updateTime?: string
}

export enum SettlementStatus {
  DRAFT = 'DRAFT',
  REVIEWED = 'REVIEWED',
  CONFIRMED = 'CONFIRMED'
}

export const SettlementStatusText: Record<SettlementStatus, string> = {
  [SettlementStatus.DRAFT]: '待复核',
  [SettlementStatus.REVIEWED]: '待确认',
  [SettlementStatus.CONFIRMED]: '已结算'
}

export enum SettlementStatusColor {
  DRAFT = 'default',
  REVIEWED = 'warning',
  CONFIRMED = 'success'
}

export interface DashboardStats {
  totalRobots: number
  activeRobots: number
  activeOrders: number
  totalRevenue: number
  monthlyRevenue: number[]
  robotStatusDistribution: {
    status: string
    count: number
  }[]
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
