import { createApi, type BaseQueryFn } from '@reduxjs/toolkit/query/react'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { axiosClient } from './axiosClient'
import type { ApiResponse } from './systemApi'
import type { AuthUserProfile, OrganizationSummary } from '../slices/authSlice'

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

export interface AuthSessionResponse {
  sessionId: string
  accessToken: string
  refreshToken: string
  tokenType: string
  user: AuthUserProfile
}

export interface LoginResult {
  mfaRequired: boolean
  mfaToken: string | null
  session: AuthSessionResponse | null
}

export interface MfaSetupResponse {
  secret: string
  otpAuthUri: string
}

export interface SessionDto {
  id: string
  organizationId: string | null
  userAgent: string | null
  ipAddress: string | null
  createdAt: string
  expiresAt: string
}

export interface BranchDto {
  id: string
  code: string
  name: string
  city: string | null
  country: string | null
  address: string | null
  headquarters: boolean
  status: string
  createdAt: string
}

export interface DepartmentDto {
  id: string
  code: string
  name: string
  description: string | null
  parentDepartmentId: string | null
  status: string
  createdAt: string
}

export interface MemberDto {
  id: string
  userId: string
  email: string
  fullName: string
  employeeCode: string | null
  designation: string | null
  branchId: string | null
  departmentId: string | null
  status: string
  joinedAt: string
  roles: { id: string; code: string; name: string }[]
}

export interface PermissionDto {
  id: string
  moduleCode: string
  permissionKey: string
  description: string
}

export interface RoleDto {
  id: string
  code: string
  name: string
  description: string | null
  systemRole: boolean
  permissionKeys: string[]
}

export interface ApiKeyDto {
  id: string
  name: string
  keyPrefix: string
  scopes: string
  expiresAt: string | null
  lastUsedAt: string | null
  revokedAt: string | null
  createdAt: string
  active: boolean
}

export interface CreatedApiKeyResponse {
  apiKey: ApiKeyDto
  secretKey: string
}

export interface AuditLogDto {
  id: string
  organizationId: string | null
  actorUserId: string | null
  actorEmail: string | null
  action: string
  resourceType: string
  resourceId: string | null
  summary: string
  status: string
  createdAt: string
}

export interface NotificationDto {
  id: string
  title: string
  message: string
  category: string
  linkPath: string | null
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationListResponse {
  unreadCount: number
  items: NotificationDto[]
}

export const adminApi = createApi({
  reducerPath: 'adminApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Profile', 'Orgs', 'Branches', 'Departments', 'Members', 'Roles', 'ApiKeys', 'AuditLogs', 'Notifications', 'Sessions'],
  endpoints: (builder) => ({
    register: builder.mutation<
      AuthSessionResponse,
      {
        fullName: string
        email: string
        password: string
        jobTitle?: string
        organizationName: string
        organizationSlug?: string
        industry?: string
        baseCurrency?: string
        timezone?: string
      }
    >({
      query: (data) => ({ url: '/auth/register', method: 'POST', data }),
      transformResponse: (res: ApiResponse<AuthSessionResponse>) => res.data,
    }),
    login: builder.mutation<LoginResult, { email: string; password: string }>({
      query: (data) => ({ url: '/auth/login', method: 'POST', data }),
      transformResponse: (res: ApiResponse<LoginResult>) => res.data,
    }),
    verifyMfaLogin: builder.mutation<AuthSessionResponse, { mfaToken: string; code: string }>({
      query: (data) => ({ url: '/auth/mfa/verify-login', method: 'POST', data }),
      transformResponse: (res: ApiResponse<AuthSessionResponse>) => res.data,
    }),
    logout: builder.mutation<void, { refreshToken?: string | null }>({
      query: (data) => ({ url: '/auth/logout', method: 'POST', data }),
    }),
    getMe: builder.query<AuthUserProfile, void>({
      query: () => ({ url: '/auth/me' }),
      transformResponse: (res: ApiResponse<AuthUserProfile>) => res.data,
      providesTags: ['Profile'],
    }),
    setupMfa: builder.mutation<MfaSetupResponse, void>({
      query: () => ({ url: '/auth/mfa/setup', method: 'POST' }),
      transformResponse: (res: ApiResponse<MfaSetupResponse>) => res.data,
    }),
    enableMfa: builder.mutation<{ mfaEnabled: boolean }, { code: string }>({
      query: (data) => ({ url: '/auth/mfa/enable', method: 'POST', data }),
      transformResponse: (res: ApiResponse<{ mfaEnabled: boolean }>) => res.data,
      invalidatesTags: ['Profile', 'AuditLogs'],
    }),
    disableMfa: builder.mutation<{ mfaEnabled: boolean }, { code: string }>({
      query: (data) => ({ url: '/auth/mfa/disable', method: 'POST', data }),
      transformResponse: (res: ApiResponse<{ mfaEnabled: boolean }>) => res.data,
      invalidatesTags: ['Profile', 'AuditLogs'],
    }),
    getSessions: builder.query<SessionDto[], void>({
      query: () => ({ url: '/auth/sessions' }),
      transformResponse: (res: ApiResponse<SessionDto[]>) => res.data,
      providesTags: ['Sessions'],
    }),
    revokeSession: builder.mutation<void, string>({
      query: (id) => ({ url: `/auth/sessions/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Sessions'],
    }),

    // Organizations
    getOrganizations: builder.query<OrganizationSummary[], void>({
      query: () => ({ url: '/organizations' }),
      transformResponse: (res: ApiResponse<OrganizationSummary[]>) => res.data,
      providesTags: ['Orgs'],
    }),
    createOrganization: builder.mutation<
      OrganizationSummary,
      { name: string; slug?: string; industry?: string; baseCurrency?: string; timezone?: string }
    >({
      query: (data) => ({ url: '/organizations', method: 'POST', data }),
      transformResponse: (res: ApiResponse<OrganizationSummary>) => res.data,
      invalidatesTags: ['Orgs', 'Profile'],
    }),
    switchOrganization: builder.mutation<AuthSessionResponse, string>({
      query: (orgId) => ({ url: `/organizations/${orgId}/switch`, method: 'POST' }),
      transformResponse: (res: ApiResponse<AuthSessionResponse>) => res.data,
      invalidatesTags: ['Profile', 'Branches', 'Departments', 'Members', 'Roles', 'ApiKeys', 'AuditLogs', 'Notifications'],
    }),

    // Branches
    getBranches: builder.query<ApiResponse<BranchDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/branches', params: params || { page: 0, size: 50 } }),
      providesTags: ['Branches'],
    }),
    createBranch: builder.mutation<
      BranchDto,
      { code: string; name: string; city?: string; country?: string; address?: string; headquarters?: boolean }
    >({
      query: (data) => ({ url: '/branches', method: 'POST', data }),
      transformResponse: (res: ApiResponse<BranchDto>) => res.data,
      invalidatesTags: ['Branches', 'AuditLogs'],
    }),
    archiveBranch: builder.mutation<void, string>({
      query: (id) => ({ url: `/branches/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Branches', 'AuditLogs'],
    }),

    // Departments
    getDepartments: builder.query<ApiResponse<DepartmentDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/departments', params: params || { page: 0, size: 50 } }),
      providesTags: ['Departments'],
    }),
    createDepartment: builder.mutation<
      DepartmentDto,
      { code: string; name: string; description?: string }
    >({
      query: (data) => ({ url: '/departments', method: 'POST', data }),
      transformResponse: (res: ApiResponse<DepartmentDto>) => res.data,
      invalidatesTags: ['Departments', 'AuditLogs'],
    }),
    archiveDepartment: builder.mutation<void, string>({
      query: (id) => ({ url: `/departments/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Departments', 'AuditLogs'],
    }),

    // Members
    getMembers: builder.query<ApiResponse<MemberDto[]>, { q?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/members', params: params || { page: 0, size: 50 } }),
      providesTags: ['Members'],
    }),
    addMember: builder.mutation<
      MemberDto,
      {
        fullName: string
        email: string
        temporaryPassword?: string
        employeeCode?: string
        designation?: string
        branchId?: string
        departmentId?: string
        roleIds?: string[]
      }
    >({
      query: (data) => ({ url: '/members', method: 'POST', data }),
      transformResponse: (res: ApiResponse<MemberDto>) => res.data,
      invalidatesTags: ['Members', 'AuditLogs'],
    }),

    // Roles & Permissions
    getPermissions: builder.query<PermissionDto[], void>({
      query: () => ({ url: '/permissions' }),
      transformResponse: (res: ApiResponse<PermissionDto[]>) => res.data,
    }),
    getRoles: builder.query<RoleDto[], void>({
      query: () => ({ url: '/roles' }),
      transformResponse: (res: ApiResponse<RoleDto[]>) => res.data,
      providesTags: ['Roles'],
    }),
    createRole: builder.mutation<
      RoleDto,
      { code: string; name: string; description?: string; permissionKeys: string[] }
    >({
      query: (data) => ({ url: '/roles', method: 'POST', data }),
      transformResponse: (res: ApiResponse<RoleDto>) => res.data,
      invalidatesTags: ['Roles', 'AuditLogs'],
    }),

    // API Keys
    getApiKeys: builder.query<ApiKeyDto[], void>({
      query: () => ({ url: '/api-keys' }),
      transformResponse: (res: ApiResponse<ApiKeyDto[]>) => res.data,
      providesTags: ['ApiKeys'],
    }),
    createApiKey: builder.mutation<
      CreatedApiKeyResponse,
      { name: string; scopes: string[]; expiresInDays?: number }
    >({
      query: (data) => ({ url: '/api-keys', method: 'POST', data }),
      transformResponse: (res: ApiResponse<CreatedApiKeyResponse>) => res.data,
      invalidatesTags: ['ApiKeys', 'AuditLogs'],
    }),
    revokeApiKey: builder.mutation<void, string>({
      query: (id) => ({ url: `/api-keys/${id}`, method: 'DELETE' }),
      invalidatesTags: ['ApiKeys', 'AuditLogs'],
    }),

    // Audit Logs
    getAuditLogs: builder.query<ApiResponse<AuditLogDto[]>, { q?: string; action?: string; page?: number; size?: number } | void>({
      query: (params) => ({ url: '/audit-logs', params: params || { page: 0, size: 25 } }),
      providesTags: ['AuditLogs'],
    }),

    // Notifications
    getNotifications: builder.query<ApiResponse<NotificationListResponse>, void>({
      query: () => ({ url: '/notifications', params: { page: 0, size: 20 } }),
      providesTags: ['Notifications'],
    }),
    markNotificationRead: builder.mutation<NotificationDto, string>({
      query: (id) => ({ url: `/notifications/${id}/read`, method: 'POST' }),
      transformResponse: (res: ApiResponse<NotificationDto>) => res.data,
      invalidatesTags: ['Notifications'],
    }),
    markAllNotificationsRead: builder.mutation<{ updated: number }, void>({
      query: () => ({ url: '/notifications/read-all', method: 'POST' }),
      transformResponse: (res: ApiResponse<{ updated: number }>) => res.data,
      invalidatesTags: ['Notifications'],
    }),
  }),
})

export const {
  useRegisterMutation,
  useLoginMutation,
  useVerifyMfaLoginMutation,
  useLogoutMutation,
  useGetMeQuery,
  useSetupMfaMutation,
  useEnableMfaMutation,
  useDisableMfaMutation,
  useGetSessionsQuery,
  useRevokeSessionMutation,
  useGetOrganizationsQuery,
  useCreateOrganizationMutation,
  useSwitchOrganizationMutation,
  useGetBranchesQuery,
  useCreateBranchMutation,
  useArchiveBranchMutation,
  useGetDepartmentsQuery,
  useCreateDepartmentMutation,
  useArchiveDepartmentMutation,
  useGetMembersQuery,
  useAddMemberMutation,
  useGetPermissionsQuery,
  useGetRolesQuery,
  useCreateRoleMutation,
  useGetApiKeysQuery,
  useCreateApiKeyMutation,
  useRevokeApiKeyMutation,
  useGetAuditLogsQuery,
  useGetNotificationsQuery,
  useMarkNotificationReadMutation,
  useMarkAllNotificationsReadMutation,
} = adminApi

