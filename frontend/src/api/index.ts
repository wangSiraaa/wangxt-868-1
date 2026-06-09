import axios from 'axios'
import type { Robot, LeaseOrder, RunningHour, MaintenanceRecord, Settlement, ApiResponse } from '@/types'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const msg = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const robotApi = {
  list: (page = 0, size = 100): Promise<ApiResponse<any>> =>
    request.get(`/robots?page=${page}&size=${size}`),
  get: (id: number): Promise<ApiResponse<Robot>> => request.get(`/robots/${id}`),
  create: (data: Partial<Robot>): Promise<ApiResponse<Robot>> =>
    request.post('/robots', {
      robotCode: data.code || data.robotCode,
      robotName: data.name || data.robotName,
      model: data.model,
      manufacturer: data.manufacturer,
      hourlyRate: data.hourlyRate,
      status: data.status || 'IDLE',
    }),
  update: (id: number, data: Partial<Robot>): Promise<ApiResponse<Robot>> =>
    request.put(`/robots/${id}`, {
      robotCode: data.code || data.robotCode,
      robotName: data.name || data.robotName,
      model: data.model,
      manufacturer: data.manufacturer,
      hourlyRate: data.hourlyRate,
      status: data.status,
    }),
  remove: (id: number): Promise<ApiResponse<void>> => request.delete(`/robots/${id}`),
}

export const leaseOrderApi = {
  list: (page = 0, size = 100): Promise<ApiResponse<any>> =>
    request.get(`/lease-orders?page=${page}&size=${size}`),
  get: (id: number): Promise<ApiResponse<LeaseOrder>> => request.get(`/lease-orders/${id}`),
  create: (data: any): Promise<ApiResponse<LeaseOrder>> => request.post('/lease-orders', data),
  update: (id: number, data: any): Promise<ApiResponse<LeaseOrder>> =>
    request.put(`/lease-orders/${id}`, data),
  remove: (id: number): Promise<ApiResponse<void>> => request.delete(`/lease-orders/${id}`),
  activate: (id: number): Promise<ApiResponse<LeaseOrder>> =>
    request.post(`/lease-orders/${id}/activate`),
  complete: (id: number): Promise<ApiResponse<LeaseOrder>> =>
    request.post(`/lease-orders/${id}/complete`),
}

export const runningHourApi = {
  list: (leaseOrderId: number): Promise<ApiResponse<RunningHour[]>> =>
    request.get(`/lease-orders/${leaseOrderId}/running-hours/all`),
  create: (leaseOrderId: number, data: any): Promise<ApiResponse<RunningHour>> =>
    request.post(`/lease-orders/${leaseOrderId}/running-hours`, data),
  update: (leaseOrderId: number, id: number, data: any): Promise<ApiResponse<RunningHour>> =>
    request.put(`/lease-orders/${leaseOrderId}/running-hours/${id}`, data),
  remove: (leaseOrderId: number, id: number): Promise<ApiResponse<void>> =>
    request.delete(`/lease-orders/${leaseOrderId}/running-hours/${id}`),
}

export const maintenanceApi = {
  list: (leaseOrderId: number): Promise<ApiResponse<MaintenanceRecord[]>> =>
    request.get(`/lease-orders/${leaseOrderId}/maintenance/all`),
  create: (leaseOrderId: number, data: any): Promise<ApiResponse<MaintenanceRecord>> =>
    request.post(`/lease-orders/${leaseOrderId}/maintenance`, data),
  update: (leaseOrderId: number, id: number, data: any): Promise<ApiResponse<MaintenanceRecord>> =>
    request.put(`/lease-orders/${leaseOrderId}/maintenance/${id}`, data),
  remove: (leaseOrderId: number, id: number): Promise<ApiResponse<void>> =>
    request.delete(`/lease-orders/${leaseOrderId}/maintenance/${id}`),
}

export const settlementApi = {
  list: (leaseOrderId: number): Promise<ApiResponse<Settlement[]>> =>
    request.get(`/lease-orders/${leaseOrderId}/settlement`),
  latest: (leaseOrderId: number): Promise<ApiResponse<Settlement>> =>
    request.get(`/lease-orders/${leaseOrderId}/settlement/latest`),
  calculate: (leaseOrderId: number): Promise<ApiResponse<Settlement>> =>
    request.post(`/lease-orders/${leaseOrderId}/settlement/calculate`),
  review: (leaseOrderId: number): Promise<ApiResponse<Settlement>> =>
    request.post(`/lease-orders/${leaseOrderId}/settlement/review`),
  confirm: (leaseOrderId: number): Promise<ApiResponse<Settlement>> =>
    request.post(`/lease-orders/${leaseOrderId}/settlement/confirm`),
  cancelConfirm: (leaseOrderId: number): Promise<ApiResponse<Settlement>> =>
    request.post(`/lease-orders/${leaseOrderId}/settlement/cancel-confirm`),
}

export const dashboardApi = {
  stats: (): Promise<ApiResponse<any>> => request.get('/dashboard/stats'),
}
