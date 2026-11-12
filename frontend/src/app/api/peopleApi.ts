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

export interface EmployeeDto {
  id: string
  employeeCode: string
  fullName: string
  workEmail: string
  designation: string
  departmentName: string | null
  branchName: string | null
  joiningDate: string
  employmentType: string
  status: string
  baseMonthlySalary: number
  annualLeaveBalance: number
}

export interface AttendanceDto {
  id: string
  employeeId: string
  workDate: string
  checkInAt: string
  checkOutAt: string | null
  workMode: string
  latitude: number | null
  longitude: number | null
  checkOutLatitude?: number | null
  checkOutLongitude?: number | null
  locationLabel?: string | null
  workedMinutes: number
  overtimeMinutes: number
  status: string
}

export interface LeaveRequestDto {
  id: string
  employeeId: string
  leaveType: string
  startDate: string
  endDate: string
  daysCount: number
  reason: string | null
  status: string
}

export interface GoalDto {
  id: string
  employeeId: string
  title: string
  targetMetric: string
  progressPercent: number
  reviewStatus: string
}

export interface JobDto {
  id: string
  jobCode: string
  title: string
  department: string | null
  location: string | null
  employmentType: string
  openingsCount: number
  status: string
}

export interface CandidateDto {
  id: string
  jobId: string
  fullName: string
  email: string
  skills: string | null
  experienceYears: number
  stage: string
  rating: number | null
  hiredEmployeeId: string | null
}

export interface PayRunDto {
  id: string
  payMonth: string
  employeesCount: number
  totalGross: number
  totalDeductions: number
  totalNetPay: number
  status: string
}

export interface PayslipDto {
  id: string
  payrollRunId: string
  employeeId: string
  employeeName: string
  payMonth: string
  basicPay: number
  hraAllowance: number
  specialAllowance: number
  grossPay: number
  pfDeduction: number
  esiDeduction: number
  tdsTaxDeduction: number
  totalDeductions: number
  netPay: number
}

export const peopleApi = createApi({
  reducerPath: 'peopleApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Employees', 'Attendance', 'Leaves', 'Goals', 'Jobs', 'Candidates', 'PayRuns', 'Payslips'],
  endpoints: (builder) => ({
    getEmployees: builder.query<EmployeeDto[], void>({
      query: () => ({ url: '/hr/employees' }),
      transformResponse: (res: ApiResponse<EmployeeDto[]>) => res.data,
      providesTags: ['Employees'],
    }),
    createEmployee: builder.mutation<
      EmployeeDto,
      { fullName: string; workEmail: string; designation: string; departmentName?: string; baseMonthlySalary: number }
    >({
      query: (data) => ({ url: '/hr/employees', method: 'POST', data }),
      transformResponse: (res: ApiResponse<EmployeeDto>) => res.data,
      invalidatesTags: ['Employees'],
    }),
    getAttendance: builder.query<AttendanceDto[], void>({
      query: () => ({ url: '/hr/attendance' }),
      transformResponse: (res: ApiResponse<AttendanceDto[]>) => res.data,
      providesTags: ['Attendance'],
    }),
    checkInAttendance: builder.mutation<
      AttendanceDto,
      { employeeId: string; workMode: string; latitude?: number; longitude?: number; locationLabel?: string }
    >({
      query: (data) => ({ url: '/hr/attendance/check-in', method: 'POST', data }),
      transformResponse: (res: ApiResponse<AttendanceDto>) => res.data,
      invalidatesTags: ['Attendance'],
    }),
    checkOutAttendance: builder.mutation<AttendanceDto, { employeeId: string; latitude?: number; longitude?: number } | string>({
      query: (arg) => {
        const employeeId = typeof arg === 'string' ? arg : arg.employeeId
        const data = typeof arg === 'string' ? undefined : { latitude: arg.latitude, longitude: arg.longitude }
        return { url: `/hr/attendance/${employeeId}/check-out`, method: 'POST', data }
      },
      transformResponse: (res: ApiResponse<AttendanceDto>) => res.data,
      invalidatesTags: ['Attendance'],
    }),
    getLeaves: builder.query<LeaveRequestDto[], void>({
      query: () => ({ url: '/hr/leaves' }),
      transformResponse: (res: ApiResponse<LeaveRequestDto[]>) => res.data,
      providesTags: ['Leaves'],
    }),
    submitLeave: builder.mutation<
      LeaveRequestDto,
      { employeeId: string; leaveType: string; startDate: string; endDate: string; reason?: string }
    >({
      query: (data) => ({ url: '/hr/leaves', method: 'POST', data }),
      transformResponse: (res: ApiResponse<LeaveRequestDto>) => res.data,
      invalidatesTags: ['Leaves', 'Employees'],
    }),
    decideLeave: builder.mutation<LeaveRequestDto, { id: string; approved: boolean }>({
      query: ({ id, approved }) => ({ url: `/hr/leaves/${id}/decision`, method: 'POST', data: { approved } }),
      transformResponse: (res: ApiResponse<LeaveRequestDto>) => res.data,
      invalidatesTags: ['Leaves', 'Employees'],
    }),
    cancelLeave: builder.mutation<LeaveRequestDto, string>({
      query: (id) => ({ url: `/hr/leaves/${id}/cancel`, method: 'POST' }),
      transformResponse: (res: ApiResponse<LeaveRequestDto>) => res.data,
      invalidatesTags: ['Leaves', 'Employees'],
    }),
    getGoals: builder.query<GoalDto[], void>({
      query: () => ({ url: '/hr/goals' }),
      transformResponse: (res: ApiResponse<GoalDto[]>) => res.data,
      providesTags: ['Goals'],
    }),
    createGoal: builder.mutation<GoalDto, { employeeId: string; title: string; targetMetric: string; progressPercent: number }>({
      query: (data) => ({ url: '/hr/goals', method: 'POST', data }),
      transformResponse: (res: ApiResponse<GoalDto>) => res.data,
      invalidatesTags: ['Goals'],
    }),
    updateGoalProgress: builder.mutation<GoalDto, { id: string; progressPercent: number }>({
      query: ({ id, progressPercent }) => ({ url: `/hr/goals/${id}/progress`, method: 'PATCH', data: { progressPercent } }),
      transformResponse: (res: ApiResponse<GoalDto>) => res.data,
      invalidatesTags: ['Goals'],
    }),

    // Recruitment
    getJobs: builder.query<JobDto[], void>({
      query: () => ({ url: '/recruitment/jobs' }),
      transformResponse: (res: ApiResponse<JobDto[]>) => res.data,
      providesTags: ['Jobs'],
    }),
    createJob: builder.mutation<JobDto, { title: string; department?: string; location?: string; openingsCount: number }>({
      query: (data) => ({ url: '/recruitment/jobs', method: 'POST', data }),
      transformResponse: (res: ApiResponse<JobDto>) => res.data,
      invalidatesTags: ['Jobs'],
    }),
    getCandidates: builder.query<CandidateDto[], void>({
      query: () => ({ url: '/recruitment/candidates' }),
      transformResponse: (res: ApiResponse<CandidateDto[]>) => res.data,
      providesTags: ['Candidates'],
    }),
    addCandidate: builder.mutation<
      CandidateDto,
      { jobId: string; fullName: string; email: string; skills?: string; experienceYears: number }
    >({
      query: (data) => ({ url: '/recruitment/candidates', method: 'POST', data }),
      transformResponse: (res: ApiResponse<CandidateDto>) => res.data,
      invalidatesTags: ['Candidates'],
    }),
    hireCandidate: builder.mutation<{ employeeCode: string }, { id: string; baseMonthlySalary: number }>({
      query: ({ id, baseMonthlySalary }) => ({
        url: `/recruitment/candidates/${id}/hire`,
        method: 'POST',
        data: { baseMonthlySalary },
      }),
      transformResponse: (res: ApiResponse<{ employeeCode: string }>) => res.data,
      invalidatesTags: ['Candidates', 'Employees'],
    }),

    // Payroll
    getPayRuns: builder.query<PayRunDto[], void>({
      query: () => ({ url: '/payroll/runs' }),
      transformResponse: (res: ApiResponse<PayRunDto[]>) => res.data,
      providesTags: ['PayRuns'],
    }),
    processPayRun: builder.mutation<PayRunDto, { payMonth: string }>({
      query: (data) => ({ url: '/payroll/runs', method: 'POST', data }),
      transformResponse: (res: ApiResponse<PayRunDto>) => res.data,
      invalidatesTags: ['PayRuns', 'Payslips'],
    }),
    getPayslips: builder.query<PayslipDto[], void>({
      query: () => ({ url: '/payroll/payslips' }),
      transformResponse: (res: ApiResponse<PayslipDto[]>) => res.data,
      providesTags: ['Payslips'],
    }),
  }),
})

export const {
  useGetEmployeesQuery,
  useCreateEmployeeMutation,
  useGetAttendanceQuery,
  useCheckInAttendanceMutation,
  useCheckOutAttendanceMutation,
  useGetLeavesQuery,
  useSubmitLeaveMutation,
  useDecideLeaveMutation,
  useCancelLeaveMutation,
  useGetGoalsQuery,
  useCreateGoalMutation,
  useUpdateGoalProgressMutation,
  useGetJobsQuery,
  useCreateJobMutation,
  useGetCandidatesQuery,
  useAddCandidateMutation,
  useHireCandidateMutation,
  useGetPayRunsQuery,
  useProcessPayRunMutation,
  useGetPayslipsQuery,
} = peopleApi

