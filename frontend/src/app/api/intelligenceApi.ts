import { createApi, type BaseQueryFn } from '@reduxjs/toolkit/query/react'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { axiosClient } from './axiosClient'
import type { ApiResponse } from './systemApi'

interface AxiosBaseQueryArgs {
  url: string
  method?: AxiosRequestConfig['method']
  data?: AxiosRequestConfig['data']
  params?: AxiosRequestConfig['params']
}

interface AxiosBaseQueryError {
  status?: number
  data?: unknown
}

const axiosBaseQuery =
  (): BaseQueryFn<AxiosBaseQueryArgs, unknown, AxiosBaseQueryError> =>
  async ({ url, method = 'GET', data, params }) => {
    try {
      const result = await axiosClient.request({ url, method, data, params })
      return { data: result.data }
    } catch (err) {
      const error = err as AxiosError
      return {
        error: {
          status: error.response?.status,
          data: error.response?.data || error.message,
        },
      }
    }
  }

export interface ReportDataRowDto {
  label: string
  count: number
  amount: number
}

export interface ExecutivePulseDto {
  leadCount: number
  dealCount: number
  pipelineValue: number
  weightedForecast: number
  totalInvoiced: number
  totalReceivable: number
  openTickets: number
  employeeHeadcount: number
  totalTasks: number
  completedTasks: number
  moduleBreakdown: ReportDataRowDto[]
}

export interface BiDashboardDto {
  id: string
  name: string
  description: string | null
  shared: boolean
  layoutJson: string
  createdAt: string
}

export interface BiReportDto {
  id: string
  dashboardId: string | null
  name: string
  dataSource: string
  chartType: string
  metric: string
  dimension: string
  createdAt: string
}

export interface ReportExecutionDto {
  report: BiReportDto
  rows: ReportDataRowDto[]
  executedAt: string
}

export interface CreatorAppDto {
  id: string
  appCode: string
  name: string
  description: string | null
  status: string
  appVersion: number
  createdAt: string
}

export interface FieldDefinitionDto {
  key: string
  label: string
  type: string
  required: boolean
  formula?: string | null
}

export interface CreatorFormDto {
  id: string
  appId: string
  formCode: string
  name: string
  fields: FieldDefinitionDto[]
  createdAt: string
}

export interface CreatorRecordDto {
  id: string
  formId: string
  recordNumber: string
  data: Record<string, unknown>
  submittedBy: string
  createdAt: string
}

export interface WorkflowDto {
  id: string
  name: string
  triggerEvent: string
  conditionExpr: string | null
  actionType: string
  enabled: boolean
  runCount: number
  lastRunAt: string | null
  createdAt: string
}

export interface WorkflowExecutionDto {
  id: string
  workflowId: string
  status: string
  outputSummary: string
  durationMs: number
  executedAt: string
}

export const intelligenceApi = createApi({
  reducerPath: 'intelligenceApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Pulse', 'Dashboards', 'Reports', 'CreatorApps', 'CreatorForms', 'CreatorRecords', 'Workflows', 'Executions'],
  endpoints: (builder) => ({
    getExecutivePulse: builder.query<ExecutivePulseDto, void>({
      query: () => ({ url: '/analytics/pulse' }),
      transformResponse: (res: ApiResponse<ExecutivePulseDto>) => res.data,
      providesTags: ['Pulse'],
    }),
    getDashboards: builder.query<BiDashboardDto[], void>({
      query: () => ({ url: '/analytics/dashboards' }),
      transformResponse: (res: ApiResponse<BiDashboardDto[]>) => res.data,
      providesTags: ['Dashboards'],
    }),
    createDashboard: builder.mutation<BiDashboardDto, { name: string; description?: string; shared?: boolean }>({
      query: (data) => ({ url: '/analytics/dashboards', method: 'POST', data }),
      transformResponse: (res: ApiResponse<BiDashboardDto>) => res.data,
      invalidatesTags: ['Dashboards'],
    }),
    getReports: builder.query<BiReportDto[], void>({
      query: () => ({ url: '/analytics/reports' }),
      transformResponse: (res: ApiResponse<BiReportDto[]>) => res.data,
      providesTags: ['Reports'],
    }),
    createReport: builder.mutation<BiReportDto, { dashboardId?: string; name: string; dataSource: string; chartType?: string }>({
      query: (data) => ({ url: '/analytics/reports', method: 'POST', data }),
      transformResponse: (res: ApiResponse<BiReportDto>) => res.data,
      invalidatesTags: ['Reports'],
    }),
    runReport: builder.mutation<ReportExecutionDto, string>({
      query: (id) => ({ url: `/analytics/reports/${id}/run`, method: 'POST' }),
      transformResponse: (res: ApiResponse<ReportExecutionDto>) => res.data,
    }),

    getCreatorApps: builder.query<CreatorAppDto[], void>({
      query: () => ({ url: '/creator/apps' }),
      transformResponse: (res: ApiResponse<CreatorAppDto[]>) => res.data,
      providesTags: ['CreatorApps'],
    }),
    createCreatorApp: builder.mutation<CreatorAppDto, { name: string; description?: string }>({
      query: (data) => ({ url: '/creator/apps', method: 'POST', data }),
      transformResponse: (res: ApiResponse<CreatorAppDto>) => res.data,
      invalidatesTags: ['CreatorApps'],
    }),
    publishCreatorApp: builder.mutation<CreatorAppDto, string>({
      query: (id) => ({ url: `/creator/apps/${id}/publish`, method: 'POST' }),
      transformResponse: (res: ApiResponse<CreatorAppDto>) => res.data,
      invalidatesTags: ['CreatorApps'],
    }),
    getCreatorForms: builder.query<CreatorFormDto[], string | void>({
      query: (appId) => ({ url: '/creator/forms', params: appId ? { appId } : undefined }),
      transformResponse: (res: ApiResponse<CreatorFormDto[]>) => res.data,
      providesTags: ['CreatorForms'],
    }),
    createCreatorForm: builder.mutation<CreatorFormDto, { appId: string; name: string; fields: FieldDefinitionDto[] }>({
      query: (data) => ({ url: '/creator/forms', method: 'POST', data }),
      transformResponse: (res: ApiResponse<CreatorFormDto>) => res.data,
      invalidatesTags: ['CreatorForms'],
    }),
    getCreatorRecords: builder.query<CreatorRecordDto[], string>({
      query: (formId) => ({ url: `/creator/forms/${formId}/records` }),
      transformResponse: (res: ApiResponse<CreatorRecordDto[]>) => res.data,
      providesTags: ['CreatorRecords'],
    }),
    submitCreatorRecord: builder.mutation<CreatorRecordDto, { formId: string; data: Record<string, unknown> }>({
      query: ({ formId, data }) => ({ url: `/creator/forms/${formId}/records`, method: 'POST', data: { data } }),
      transformResponse: (res: ApiResponse<CreatorRecordDto>) => res.data,
      invalidatesTags: ['CreatorRecords'],
    }),

    getWorkflows: builder.query<WorkflowDto[], void>({
      query: () => ({ url: '/automation/workflows' }),
      transformResponse: (res: ApiResponse<WorkflowDto[]>) => res.data,
      providesTags: ['Workflows'],
    }),
    createWorkflow: builder.mutation<WorkflowDto, { name: string; triggerEvent: string; conditionExpr?: string; actionType: string }>({
      query: (data) => ({ url: '/automation/workflows', method: 'POST', data }),
      transformResponse: (res: ApiResponse<WorkflowDto>) => res.data,
      invalidatesTags: ['Workflows'],
    }),
    toggleWorkflow: builder.mutation<WorkflowDto, { id: string; enabled: boolean }>({
      query: ({ id, enabled }) => ({ url: `/automation/workflows/${id}/toggle`, method: 'PATCH', data: { enabled } }),
      transformResponse: (res: ApiResponse<WorkflowDto>) => res.data,
      invalidatesTags: ['Workflows'],
    }),
    triggerWorkflow: builder.mutation<WorkflowExecutionDto, { id: string; payload?: Record<string, unknown> }>({
      query: ({ id, payload }) => ({ url: `/automation/workflows/${id}/trigger`, method: 'POST', data: payload ?? {} }),
      transformResponse: (res: ApiResponse<WorkflowExecutionDto>) => res.data,
      invalidatesTags: ['Workflows', 'Executions'],
    }),
    getWorkflowExecutions: builder.query<WorkflowExecutionDto[], string | void>({
      query: (workflowId) => ({ url: '/automation/executions', params: workflowId ? { workflowId } : undefined }),
      transformResponse: (res: ApiResponse<WorkflowExecutionDto[]>) => res.data,
      providesTags: ['Executions'],
    }),
  }),
})

export const {
  useGetExecutivePulseQuery,
  useGetDashboardsQuery,
  useCreateDashboardMutation,
  useGetReportsQuery,
  useCreateReportMutation,
  useRunReportMutation,
  useGetCreatorAppsQuery,
  useCreateCreatorAppMutation,
  usePublishCreatorAppMutation,
  useGetCreatorFormsQuery,
  useCreateCreatorFormMutation,
  useGetCreatorRecordsQuery,
  useSubmitCreatorRecordMutation,
  useGetWorkflowsQuery,
  useCreateWorkflowMutation,
  useToggleWorkflowMutation,
  useTriggerWorkflowMutation,
  useGetWorkflowExecutionsQuery,
} = intelligenceApi

